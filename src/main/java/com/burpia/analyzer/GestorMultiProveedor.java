package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.ProveedorAI;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Hallazgo;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
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
        this.gestorLogging = gestorLogging;
        this.logLock = new Object();
        this.constructorPrompt = new ConstructorPrompts(this.config);
        this.parseador = new ParseadorRespuestasAI(this.gestorLogging, this.config.obtenerIdiomaUi());
    }

    public ResultadoAnalisisMultiple ejecutarAnalisisMultiProveedor() throws IOException, InterruptedException {
        List<String> proveedores = config.obtenerProveedoresMultiConsulta();
        
        if (Normalizador.esVacia(proveedores)) {
            registrar("Multi-consulta: No hay proveedores seleccionados, usando proveedor único");
            return ejecutarAnalisisProveedorUnico();
        }

        if (proveedores.size() == 1) {
            registrar("Multi-consulta: Solo 1 proveedor seleccionado, usando proveedor único");
            return ejecutarAnalisisProveedorUnico();
        }

        return ejecutarAnalisisSecuencialProveedores(proveedores);
    }

    private ResultadoAnalisisMultiple ejecutarAnalisisSecuencialProveedores(List<String> proveedores)
            throws IOException, InterruptedException {
        
        List<Hallazgo> todosHallazgos = new ArrayList<>();
        List<String> proveedoresFallidos = new ArrayList<>();

        try {
            for (String proveedor : proveedores) {
                verificarCancelacion();
                esperarSiPausada();

                if (Normalizador.esVacio(proveedor)) {
                    continue;
                }
                
                if (!ProveedorAI.existeProveedor(proveedor)) {
                    registrar("PROVEEDOR: Proveedor no existe: " + proveedor + ", omitiendo");
                    continue;
                }

                String modelo = config.obtenerModeloParaProveedor(proveedor);
                if (Normalizador.esVacio(modelo)) {
                    registrar("PROVEEDOR: Proveedor " + proveedor + " no tiene modelo configurado, omitiendo");
                    continue;
                }

                if (!todosHallazgos.isEmpty()) {
                    long delaySegundos = DELAY_ENTRE_PROVEEDORES_MS / 1000L;
                    registrar("PROVEEDOR: Esperando " + delaySegundos + " segundos antes del siguiente proveedor");
                    esperarConControl(DELAY_ENTRE_PROVEEDORES_MS);
                }

                registrar(LINEA_SEPARADORA_PROVEEDOR);
                registrar("PROVEEDOR: " + proveedor + " (" + modelo + ")");

                try {
                    ResultadoAnalisisMultiple resultado = ejecutarAnalisisProveedor(proveedor, modelo);
                    List<Hallazgo> hallazgosProveedor = resultado.obtenerHallazgos();
                    
                    registrar("PROVEEDOR: " + proveedor + " completado - " + hallazgosProveedor.size()
                            + " hallazgo(s) encontrado(s)");
                    todosHallazgos.addAll(hallazgosProveedor);

                } catch (Exception e) {
                    registrar("PROVEEDOR: Error con " + proveedor + ": " + e.getMessage());
                    proveedoresFallidos.add(proveedor);
                }
            }

            if (!proveedoresFallidos.isEmpty()) {
                registrarError("PROVEEDOR: " + proveedoresFallidos.size() +
                        " proveedor(es) fallaron: " + String.join(", ", proveedoresFallidos));
            }

            registrar(LINEA_SEPARADORA_PROVEEDOR);
            registrar("PROVEEDOR: Multi-consulta completada. Total de hallazgos combinados: " + todosHallazgos.size());

            return new ResultadoAnalisisMultiple(solicitud.obtenerUrl(), todosHallazgos,
                    solicitud.obtenerSolicitudHttp(), proveedoresFallidos);

        } finally {
        }
    }

    private ResultadoAnalisisMultiple ejecutarAnalisisProveedorUnico() throws IOException, InterruptedException {
        AnalizadorHTTP analizadorHTTP = new AnalizadorHTTP(config, tareaCancelada, tareaPausada, gestorLogging);
        String respuesta = llamarAPIAIConRetries(analizadorHTTP, config);
        return parsearRespuesta(respuesta, config.obtenerProveedorAI());
    }

    private ResultadoAnalisisMultiple ejecutarAnalisisProveedor(String proveedor, String modelo)
            throws IOException, InterruptedException {
        
        ConfiguracionAPI configProveedor = crearConfiguracionParaProveedor(proveedor);
        AnalizadorHTTP analizadorHTTP = new AnalizadorHTTP(configProveedor, tareaCancelada, tareaPausada, gestorLogging);
        String respuesta = llamarAPIAIConRetries(analizadorHTTP, configProveedor);
        ResultadoAnalisisMultiple resultado = parsearRespuesta(respuesta, proveedor);
        
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
        
        verificarCancelacion();
        esperarSiPausada();
        
        String prompt = constructorPrompt.construirPromptAnalisis(solicitud);
        
        try {
            String respuesta = analizadorHTTP.llamarAPI(prompt);
            registrar("Longitud de respuesta de API: " + respuesta.length() + " caracteres");
            return respuesta;
        } catch (ContextExceededException e) {
            // Para multi-proveedor, propagar como IOException
            throw new IOException(I18nUI.ContextoExcedido.MENSAJE_FALLIDO_PROVEEDOR(configActual.obtenerProveedorAI()), e);
        }
    }

    private ResultadoAnalisisMultiple parsearRespuesta(String respuestaJson) {
        return parsearRespuesta(respuestaJson, config != null ? config.obtenerProveedorAI() : "");
    }

    private ResultadoAnalisisMultiple parsearRespuesta(String respuestaJson, String proveedor) {
        return parseador.parsearRespuesta(respuestaJson, solicitud, proveedor);
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

            Hallazgo hallazgoEtiquetado = new Hallazgo(
                    hallazgo.obtenerUrl(),
                    hallazgo.obtenerTitulo(),
                    descripcionConEtiqueta,
                    hallazgo.obtenerSeveridad(),
                    hallazgo.obtenerConfianza(),
                    hallazgo.obtenerSolicitudHttp(),
                    hallazgo.obtenerEvidenciaHttp());

            hallazgosConEtiqueta.add(hallazgoEtiquetado);
        }

        return new ResultadoAnalisisMultiple(
                solicitud.obtenerUrl(),
                hallazgosConEtiqueta,
                solicitud.obtenerSolicitudHttp(),
                Collections.emptyList());
    }

    private void verificarCancelacion() throws InterruptedException {
        if (tareaCancelada.getAsBoolean()) {
            throw new InterruptedException(I18nUI.Tareas.MSG_CANCELADO_USUARIO());
        }
    }

    private void esperarSiPausada() throws InterruptedException {
        while (tareaPausada.getAsBoolean() && !tareaCancelada.getAsBoolean()) {
            Thread.sleep(250);
        }
        verificarCancelacion();
    }

    private void esperarConControl(long milisegundos) throws InterruptedException {
        long restante = milisegundos;
        while (restante > 0) {
            verificarCancelacion();
            esperarSiPausada();
            long espera = Math.min(restante, 250);
            Thread.sleep(espera);
            restante -= espera;
        }
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
}
