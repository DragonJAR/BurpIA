package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.ProveedorAI;
import com.burpia.i18n.I18nLogs;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Hallazgo;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.ControlCancelacionPausa;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.Normalizador;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

public class GestorMultiProveedor {
    private static final String ORIGEN_LOG = "GestorMultiProveedor";
    private static final long DELAY_ENTRE_PROVEEDORES_MS = 2000L;
    private static final String LINEA_SEPARADORA_PROVEEDOR = "========================================";
    
    private final SolicitudAnalisis solicitud;
    private final ConfiguracionAPI config;
    private final PrintWriter stdout;
    private final PrintWriter stderr;
    private final GestorConsolaGUI gestorConsola;
    private final BooleanSupplier tareaCancelada;
    private final BooleanSupplier tareaPausada;
    private final ControlCancelacionPausa control;
    private final GestorLoggingUnificado gestorLogging;
    private final Object logLock;
    private final ConstructorPrompts constructorPrompt;
    private final ParseadorRespuestasAI parseador;

    public GestorMultiProveedor(SolicitudAnalisis solicitud,
                               ConfiguracionAPI config,
                               PrintWriter stdout,
                               PrintWriter stderr,
                               GestorConsolaGUI gestorConsola,
                               BooleanSupplier tareaCancelada,
                               BooleanSupplier tareaPausada,
                               GestorLoggingUnificado gestorLogging) {
        this.solicitud = solicitud;
        this.config = config != null ? config : new ConfiguracionAPI();
        this.stdout = stdout != null ? stdout : new PrintWriter(OutputStream.nullOutputStream(), true);
        this.stderr = stderr != null ? stderr : new PrintWriter(OutputStream.nullOutputStream(), true);
        this.gestorConsola = gestorConsola;
        this.tareaCancelada = tareaCancelada != null ? tareaCancelada : () -> false;
        this.tareaPausada = tareaPausada != null ? tareaPausada : () -> false;
        this.control = new ControlCancelacionPausa(tareaCancelada, tareaPausada);
        this.gestorLogging = gestorLogging;
        this.logLock = new Object();
        this.constructorPrompt = new ConstructorPrompts(this.config);
        this.parseador = new ParseadorRespuestasAI(this.gestorLogging, this.config.obtenerIdiomaUi());
    }

    public ResultadoAnalisisMultiple ejecutarAnalisisMultiProveedor() throws IOException, InterruptedException {
        List<String> proveedores = config.obtenerProveedoresMultiConsulta();
        
        if (Normalizador.esVacia(proveedores)) {
            registrar(I18nLogs.MultiProveedor.SIN_PROVEEDORES());
            return ejecutarAnalisisProveedorUnico();
        }

        if (proveedores.size() == 1) {
            registrar(I18nLogs.MultiProveedor.UN_PROVEEDOR());
            return ejecutarAnalisisProveedorUnico();
        }

        return ejecutarAnalisisSecuencialProveedores(proveedores);
    }

    private ResultadoAnalisisMultiple ejecutarAnalisisSecuencialProveedores(List<String> proveedores)
            throws IOException, InterruptedException {
        
        List<Hallazgo> todosHallazgos = new ArrayList<>();
        List<String> proveedoresFallidos = new ArrayList<>();
        boolean proveedorEjecutadoPreviamente = false;

        for (String proveedor : proveedores) {
            control.verificarCancelacion();
            control.esperarSiPausada();

            if (Normalizador.esVacio(proveedor)) {
                continue;
            }
            
            if (!ProveedorAI.existeProveedor(proveedor)) {
                registrar(I18nLogs.MultiProveedor.PROVEEDOR_NO_EXISTE(proveedor));
                continue;
            }

            String modelo = config.obtenerModeloParaProveedor(proveedor);
            if (Normalizador.esVacio(modelo)) {
                registrar(I18nLogs.MultiProveedor.PROVEEDOR_SIN_MODELO(proveedor));
                continue;
            }

            if (proveedorEjecutadoPreviamente) {
                long delaySegundos = DELAY_ENTRE_PROVEEDORES_MS / 1000L;
                registrar(I18nLogs.MultiProveedor.ESPERANDO_SIGUIENTE(delaySegundos));
                control.esperarConControl(DELAY_ENTRE_PROVEEDORES_MS);
            }

            registrar(LINEA_SEPARADORA_PROVEEDOR);
            registrar(I18nLogs.MultiProveedor.PROVEEDOR_EJECUTANDO(proveedor, modelo));

            try {
                ResultadoAnalisisMultiple resultado = ejecutarAnalisisProveedor(proveedor, modelo);
                List<Hallazgo> hallazgosProveedor = resultado.obtenerHallazgos();

                registrar(I18nLogs.MultiProveedor.PROVEEDOR_COMPLETADO(proveedor, hallazgosProveedor.size()));
                todosHallazgos.addAll(hallazgosProveedor);

            } catch (InterruptedException ie) {
                // Cancelación del usuario: restaurar flag y propagar para
                // que el orquestador corte el resto de proveedores. Antes
                // estaba siendo tragada por el catch Exception siguiente
                // (M1 audit) → la cancelación no surtía efecto en multi-mode.
                Thread.currentThread().interrupt();
                throw ie;
            } catch (Exception e) {
                registrar(I18nLogs.MultiProveedor.PROVEEDOR_ERROR(proveedor, e.getMessage()));
                proveedoresFallidos.add(proveedor);
            } finally {
                proveedorEjecutadoPreviamente = true;
            }
        }

        if (!proveedoresFallidos.isEmpty()) {
            registrarError(I18nLogs.MultiProveedor.PROVEEDORES_FALLIDOS(
                    proveedoresFallidos.size(), String.join(", ", proveedoresFallidos)));
        }

        registrar(LINEA_SEPARADORA_PROVEEDOR);
        registrar(I18nLogs.MultiProveedor.MULTI_CONSULTA_COMPLETADA(todosHallazgos.size()));

        return new ResultadoAnalisisMultiple(solicitud.obtenerUrl(), todosHallazgos,
                solicitud.obtenerSolicitudHttp(), proveedoresFallidos);
    }

    private ResultadoAnalisisMultiple ejecutarAnalisisProveedorUnico() throws IOException, InterruptedException {
        AnalizadorHTTP analizadorHTTP = new AnalizadorHTTP(config, tareaCancelada, tareaPausada, gestorLogging);
        String respuesta = llamarAPIAIConRetries(analizadorHTTP, config);
        return parseador.parsearRespuesta(respuesta, solicitud, config.obtenerProveedorAI());
    }

    private ResultadoAnalisisMultiple ejecutarAnalisisProveedor(String proveedor, String modelo)
            throws IOException, InterruptedException {
        
        ConfiguracionAPI configProveedor = crearConfiguracionParaProveedor(proveedor);
        AnalizadorHTTP analizadorHTTP = new AnalizadorHTTP(configProveedor, tareaCancelada, tareaPausada, gestorLogging);
        String respuesta = llamarAPIAIConRetries(analizadorHTTP, configProveedor);
        ResultadoAnalisisMultiple resultado = parseador.parsearRespuesta(respuesta, solicitud, proveedor);
        
        return etiquetarResultado(resultado, proveedor, modelo);
    }

    private ConfiguracionAPI crearConfiguracionParaProveedor(String proveedor) {
        ConfiguracionAPI configProveedor = new ConfiguracionAPI();
        configProveedor.aplicarDesde(config);
        configProveedor.establecerProveedorAI(proveedor);
        return configProveedor;
    }

    private String llamarAPIAIConRetries(AnalizadorHTTP analizadorHTTP, ConfiguracionAPI configActual)
            throws IOException, InterruptedException {
        
        control.verificarCancelacion();
        control.esperarSiPausada();
        
        String prompt = constructorPrompt.construirPromptAnalisis(solicitud);
        
        try {
            String respuesta = analizadorHTTP.llamarAPI(prompt);
            registrar(I18nLogs.MultiProveedor.LONGITUD_RESPUESTA_API(respuesta.length()));
            return respuesta;
        } catch (ContextExceededException e) {
            // Para multi-proveedor, propagar como IOException
            throw new IOException(I18nUI.ContextoExcedido.MENSAJE_FALLIDO_PROVEEDOR(configActual.obtenerProveedorAI()), e);
        }
    }

    private ResultadoAnalisisMultiple etiquetarResultado(ResultadoAnalisisMultiple resultado,
                                                         String proveedor,
                                                         String modelo) {
        List<Hallazgo> hallazgos = resultado.obtenerHallazgos();
        List<Hallazgo> hallazgosConEtiqueta = new ArrayList<>();

        for (Hallazgo hallazgo : hallazgos) {
            String descripcionOriginal = hallazgo.obtenerHallazgo();
            String etiqueta = I18nUI.Configuracion.TXT_DESCUBIERTO_CON(proveedor, modelo);
            String descripcionConEtiqueta = descripcionOriginal + etiqueta;

            Hallazgo hallazgoEtiquetado = hallazgo.editar(
                    hallazgo.obtenerUrl(),
                    hallazgo.obtenerTitulo(),
                    descripcionConEtiqueta,
                    hallazgo.obtenerSeveridad(),
                    hallazgo.obtenerConfianza());

            hallazgosConEtiqueta.add(hallazgoEtiquetado);
        }

        return new ResultadoAnalisisMultiple(
                solicitud.obtenerUrl(),
                hallazgosConEtiqueta,
                solicitud.obtenerSolicitudHttp(),
                Collections.emptyList());
    }

    private void registrar(String mensaje) {
        if (gestorLogging != null) {
            gestorLogging.info(ORIGEN_LOG, mensaje);
        } else {
            registrarInterno(mensaje, GestorConsolaGUI.TipoLog.INFO, false, "[BurpIA] ", false);
        }
    }

    private void registrarError(String mensaje) {
        if (gestorLogging != null) {
            gestorLogging.error(ORIGEN_LOG, mensaje);
        } else {
            registrarInterno(mensaje, GestorConsolaGUI.TipoLog.ERROR, true, "[BurpIA] [ERROR] ", false);
        }
    }

    private void registrarInterno(String mensaje, GestorConsolaGUI.TipoLog tipo, boolean esError, 
                                 String prefijoSalida, boolean mensajeTecnico) {
        String mensajeSeguro = mensaje != null ? mensaje : "";
        GestorConsolaGUI consolaActual = this.gestorConsola;

        if (consolaActual != null) {
            if (mensajeTecnico) {
                consolaActual.registrarTecnico(ORIGEN_LOG, mensajeSeguro, tipo);
            } else {
                consolaActual.registrar(ORIGEN_LOG, mensajeSeguro, tipo);
            }
            return;
        }

        PrintWriter destinoStr;
        synchronized (logLock) {
            destinoStr = esError ? stderr : stdout;
            if (destinoStr != null) {
                destinoStr.println(prefijoSalida + mensajeSeguro);
                destinoStr.flush();
            }
        }
    }

    /**
     * Parsea una respuesta JSON usando el parseador interno.
     * Exist for test access only — production code should use the full pipeline.
     */
    ResultadoAnalisisMultiple parsearRespuesta(String respuestaJson, String proveedor) {
        return parseador.parsearRespuesta(respuestaJson, solicitud, proveedor);
    }
}
