package com.burpia.flow;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.burpia.analyzer.AnalizadorAI;
import com.burpia.analyzer.ConstructorPrompts;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.evidence.EvidenceManager;
import com.burpia.model.Hallazgo;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.processor.HttpRequestProcessor;
import com.burpia.ui.ModeloTablaHallazgos;
import com.burpia.util.ControlBackpressureGlobal;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.LimitadorTasa;
import com.burpia.util.Normalizador;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * Manager para análisis de flujo de peticiones HTTP.
 * <p>
 * Combina múltiples peticiones en UNA SOLA consulta al LLM,
 * tratándolas como una secuencia lógica (ej: login → dashboard → profile),
 * en lugar de analizar cada petición de forma aislada.
 * </p>
 * <p>
 * Esto permite detectar vulnerabilidades que solo aparecen en el contexto
 * de un flujo completo, como bypasses de autenticación, escalada de privilegios,
 * y bugs de lógica de negocio.
 * </p>
 */
public class FlowAnalysisManager {
    
    private static final String ORIGEN_LOG = "FlowAnalysisManager";
    
    private final MontoyaApi api;
    private final ConfiguracionAPI config;
    private final GestorLoggingUnificado gestorLogging;
    private final HttpRequestProcessor httpRequestProcessor;
    private final EvidenceManager evidenceManager;
    private final LimitadorTasa limitador;
    private final GestorConsolaGUI gestorConsola;
    private final ModeloTablaHallazgos modeloTablaHallazgos;
    private final AtomicBoolean cancelado;
    private final ControlBackpressureGlobal controlBackpressure;
    private volatile Thread threadAnalisisActivo;
    
    public FlowAnalysisManager(MontoyaApi api,
                                ConfiguracionAPI config,
                                GestorLoggingUnificado gestorLogging,
                                LimitadorTasa limitador,
                                GestorConsolaGUI gestorConsola,
                                ModeloTablaHallazgos modeloTablaHallazgos) {
        if (api == null || config == null || gestorLogging == null) {
            throw new IllegalArgumentException("api, config y gestorLogging son obligatorios");
        }
        this.api = api;
        this.config = config;
        this.gestorLogging = gestorLogging;
        this.httpRequestProcessor = new HttpRequestProcessor(api, config);
        this.evidenceManager = new EvidenceManager(api);
        this.limitador = limitador != null ? limitador : new LimitadorTasa(1);
        this.gestorConsola = gestorConsola;
        this.modeloTablaHallazgos = modeloTablaHallazgos;
        this.cancelado = new AtomicBoolean(false);
        this.controlBackpressure = new ControlBackpressureGlobal();
    }
    
    /**
     * Ejecuta el análisis de flujo sobre una lista de peticiones HTTP.
     * <p>
     * Combina todas las peticiones en un solo prompt y las envía al LLM
     * para análisis contextual.
     * </p>
     *
     * @param peticiones Lista de peticiones HTTP a analizar como flujo
     * @param callback Callback para notificar el resultado
     */
    public void ejecutarAnalisisFlujo(List<HttpRequestResponse> peticiones, FlowAnalysisCallback callback) {
        if (Normalizador.esVacia(peticiones) || peticiones.size() < 2) {
            gestorLogging.error(ORIGEN_LOG, "Se requieren al menos 2 peticiones para análisis de flujo");
            if (callback != null) {
                callback.onError("Se requieren al menos 2 peticiones para análisis de flujo");
            }
            return;
        }
        
        cancelado.set(false);
        int totalPeticiones = peticiones.size();
        
        gestorLogging.info(ORIGEN_LOG, 
            "Iniciando análisis de flujo con " + totalPeticiones + " peticiones");
        
        Thread nuevoThread = new Thread(() -> {
            try {
                ejecutarAnalisisAsync(peticiones, callback);
            } finally {
                threadAnalisisActivo = null;
            }
        }, "BurpIA-FlowAnalysis");
        
        threadAnalisisActivo = nuevoThread;
        nuevoThread.start();
    }
    
    private void ejecutarAnalisisAsync(List<HttpRequestResponse> peticiones, FlowAnalysisCallback callback) {
        boolean permisoAdquirido = false;
        
        try {
            if (cancelado.get()) {
                gestorLogging.info(ORIGEN_LOG, "Análisis de flujo cancelado antes de iniciar");
                if (callback != null) {
                    callback.onCancelled();
                }
                return;
            }
            
            List<SolicitudAnalisis> solicitudes = new ArrayList<>();
            List<String> urlsFlujo = new ArrayList<>();
            
            for (HttpRequestResponse rr : peticiones) {
                if (rr == null || rr.request() == null) {
                    continue;
                }
                SolicitudAnalisis solicitud = httpRequestProcessor.crearSolicitudAnalisisForzada(
                    rr.request(), rr);
                if (solicitud != null) {
                    solicitudes.add(solicitud);
                    urlsFlujo.add(solicitud.obtenerUrl());
                }
            }
            
            if (solicitudes.size() < 2) {
                String error = "No se pudieron procesar suficientes peticiones válidas para análisis de flujo";
                gestorLogging.error(ORIGEN_LOG, error);
                if (callback != null) {
                    callback.onError(error);
                }
                return;
            }
            
            gestorLogging.info(ORIGEN_LOG, 
                "Flujo a analizar: " + String.join(" → ", urlsFlujo));
            
            limitador.adquirir();
            permisoAdquirido = true;
            
            BooleanSupplier estaCancelado = () -> cancelado.get();
            BooleanSupplier estaPausado = () -> false;
            
            SolicitudAnalisis solicitudFlujo = crearSolicitudFlujo(solicitudes);
            
            AnalizadorAI.Callback analizadorCallback = new AnalizadorAI.Callback() {
                @Override
                public void alCompletarAnalisis(ResultadoAnalisisMultiple resultado) {
                    gestorLogging.info(ORIGEN_LOG, 
                        "Análisis de flujo completado. Hallazgos: " + resultado.obtenerHallazgos().size());
                    
                    procesarHallazgos(resultado.obtenerHallazgos(), urlsFlujo);
                    
                    if (callback != null) {
                        callback.onComplete(resultado.obtenerHallazgos(), urlsFlujo);
                    }
                }
                
                @Override
                public void alErrorAnalisis(String error) {
                    gestorLogging.error(ORIGEN_LOG, "Error en análisis de flujo: " + error);
                    if (callback != null) {
                        callback.onError(error);
                    }
                }
                
                @Override
                public void alCanceladoAnalisis() {
                    gestorLogging.info(ORIGEN_LOG, "Análisis de flujo cancelado");
                    if (callback != null) {
                        callback.onCancelled();
                    }
                }
            };
            
            AnalizadorAI analizador = new AnalizadorAI(
                solicitudFlujo,
                config,
                new PrintWriter(System.out, true),
                new PrintWriter(System.err, true),
                limitador,
                analizadorCallback,
                gestorConsola,
                estaCancelado,
                estaPausado
            );
            
            analizador.run();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            gestorLogging.info(ORIGEN_LOG, "Análisis de flujo interrumpido");
            if (callback != null) {
                callback.onCancelled();
            }
        } catch (Exception e) {
            gestorLogging.error(ORIGEN_LOG, "Error inesperado en análisis de flujo: " + e.getMessage(), e);
            if (callback != null) {
                callback.onError("Error inesperado: " + e.getMessage());
            }
        } finally {
            if (permisoAdquirido) {
                limitador.liberar();
            }
        }
    }
    
    private void procesarHallazgos(List<Hallazgo> hallazgos, List<String> urlsFlujo) {
        if (Normalizador.esVacia(hallazgos) || modeloTablaHallazgos == null) {
            return;
        }
        
        String urlRepresentativa = urlsFlujo.isEmpty() ? "Flujo de peticiones" : urlsFlujo.get(0);
        String descripcionFlujo = "Flujo: " + String.join(" → ", urlsFlujo);
        
        for (Hallazgo hallazgo : hallazgos) {
            if (hallazgo == null) {
                continue;
            }
            
            Hallazgo hallazgoFlujo = hallazgo.conEvidenciaId(null);
            String descripcionOriginal = hallazgo.obtenerHallazgo();
            String nuevaDescripcion = descripcionOriginal + "\n\n[" + descripcionFlujo + "]";
            
            hallazgoFlujo = hallazgoFlujo.editar(
                urlRepresentativa,
                hallazgo.obtenerTitulo(),
                nuevaDescripcion,
                hallazgo.obtenerSeveridad(),
                hallazgo.obtenerConfianza()
            );
            
            modeloTablaHallazgos.agregarHallazgo(hallazgoFlujo);
        }
    }
    
    private SolicitudAnalisis crearSolicitudFlujo(List<SolicitudAnalisis> solicitudes) {
        if (Normalizador.esVacia(solicitudes)) {
            return null;
        }
        
        SolicitudAnalisis primera = solicitudes.get(0);
        StringBuilder encabezadosBuilder = new StringBuilder();
        StringBuilder cuerpoBuilder = new StringBuilder();
        
        for (int i = 0; i < solicitudes.size(); i++) {
            SolicitudAnalisis s = solicitudes.get(i);
            encabezadosBuilder.append("[PETICIÓN ").append(i + 1).append("]\n");
            encabezadosBuilder.append(s.obtenerMetodo()).append(" ").append(s.obtenerUrl()).append("\n");
            encabezadosBuilder.append(s.obtenerEncabezados()).append("\n\n");
            
            String cuerpo = s.obtenerCuerpo();
            if (!Normalizador.esVacio(cuerpo)) {
                cuerpoBuilder.append("[BODY PETICIÓN ").append(i + 1).append("]\n");
                cuerpoBuilder.append(cuerpo).append("\n\n");
            }
        }
        
        String encabezadosCompletos = encabezadosBuilder.toString();
        String cuerpoCompleto = cuerpoBuilder.toString();
        
        ConstructorPrompts constructorPrompts = new ConstructorPrompts(config);
        String promptFlujo = constructorPrompts.construirPromptFlujo(solicitudes);
        
        return new SolicitudAnalisis(
            primera.obtenerUrl(),
            "FLOW",
            promptFlujo + "\n\n" + encabezadosCompletos,
            cuerpoCompleto,
            "flow-" + System.currentTimeMillis(),
            primera.obtenerSolicitudHttp(),
            -1,
            "",
            ""
        );
    }
    
    /**
     * Cancela el análisis de flujo en curso.
     */
    public void cancelar() {
        cancelado.set(true);
        gestorLogging.info(ORIGEN_LOG, "Solicitud de cancelación recibida");
        
        Thread thread = threadAnalisisActivo;
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
            gestorLogging.info(ORIGEN_LOG, "Thread de análisis interrumpido");
        }
    }
    
    /**
     * Verifica si el análisis está cancelado.
     *
     * @return {@code true} si está cancelado
     */
    public boolean estaCancelado() {
        return cancelado.get();
    }
    
    /**
     * Cierra el manager de forma segura, cancelando cualquier análisis en curso.
     * <p>
     * Debe llamarse al cerrar la aplicación para evitar threads huérfanos.
     * </p>
     */
    public void shutdown() {
        cancelado.set(true);
        
        Thread thread = threadAnalisisActivo;
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
            try {
                thread.join(1000);
                gestorLogging.info(ORIGEN_LOG, "Thread de análisis cerrado correctamente");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                gestorLogging.error(ORIGEN_LOG, "Interrupción durante shutdown del thread de análisis");
            }
        }
        
        threadAnalisisActivo = null;
        gestorLogging.info(ORIGEN_LOG, "FlowAnalysisManager shutdown completado");
    }
}
