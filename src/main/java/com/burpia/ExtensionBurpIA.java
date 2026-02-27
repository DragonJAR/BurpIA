package com.burpia;
import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.BurpSuiteEdition;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.GestorConfiguracion;
import com.burpia.i18n.I18nLogs;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Estadisticas;
import com.burpia.model.Hallazgo;
import com.burpia.ui.ModeloTablaHallazgos;
import com.burpia.ui.ModeloTablaTareas;
import com.burpia.ui.PanelAgente;
import com.burpia.ui.PestaniaPrincipal;
import com.burpia.ui.DialogoConfiguracion;
import com.burpia.ui.FabricaMenuContextual;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorTareas;
import com.burpia.util.LimitadorTasa;
import javax.swing.*;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;

public class ExtensionBurpIA implements BurpExtension {
    private MontoyaApi api;
    private ConfiguracionAPI config;
    private GestorConfiguracion gestorConfig;
    private PestaniaPrincipal pestaniaPrincipal;
    private LimitadorTasa limitador;
    private ManejadorHttpBurpIA manejadorHttp;
    private PrintWriter stdout;
    private PrintWriter stderr;
    private Estadisticas estadisticas;
    private GestorTareas gestorTareas;
    private GestorConsolaGUI gestorConsola;
    private ModeloTablaHallazgos modeloTablaHallazgos;
    private ModeloTablaTareas modeloTablaTareas;
    private FabricaMenuContextual fabricaMenuContextual;
    private boolean esProfessional = false;

    public ExtensionBurpIA() {
    }

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        this.esProfessional = esBurpProfessional(api);

        api.extension().setName("BurpIA");
        api.extension().registerUnloadingHandler(() -> {
            unload();
        });

        this.stdout = new PrintWriter(new OutputStream() {
            private final StringBuilder buffer = new StringBuilder();
            @Override
            public void write(int b) {
                if (b == '\n') {
                    flushBuffer();
                } else {
                    buffer.append((char) b);
                }
            }
            @Override
            public void write(byte[] b, int off, int len) {
                String s = new String(b, off, len, StandardCharsets.UTF_8);
                if (s.contains("\n")) {
                    buffer.append(s);
                    flushBuffer();
                } else {
                    buffer.append(s);
                }
            }
            private void flushBuffer() {
                if (buffer.length() > 0) {
                    api.logging().logToOutput(buffer.toString());
                    buffer.setLength(0);
                }
            }
            @Override
            public void flush() {
                flushBuffer();
            }
        }, true);
        this.stderr = new PrintWriter(new OutputStream() {
            private final StringBuilder buffer = new StringBuilder();
            @Override
            public void write(int b) {
                if (b == '\n') {
                    flushBuffer();
                } else {
                    buffer.append((char) b);
                }
            }
            @Override
            public void write(byte[] b, int off, int len) {
                String s = new String(b, off, len, StandardCharsets.UTF_8);
                if (s.contains("\n")) {
                    buffer.append(s);
                    flushBuffer();
                } else {
                    buffer.append(s);
                }
            }
            private void flushBuffer() {
                if (buffer.length() > 0) {
                    api.logging().logToError(buffer.toString());
                    buffer.setLength(0);
                }
            }
            @Override
            public void flush() {
                flushBuffer();
            }
        }, true);

        gestorConfig = new GestorConfiguracion(stdout, stderr);
        config = gestorConfig.cargarConfiguracion();
        I18nUI.establecerIdioma(config.obtenerIdiomaUi());
        gestorConsola = new GestorConsolaGUI();
        gestorConsola.capturarStreamsOriginales(stdout, stderr);

        registrar("==================================================");
        registrar(" BurpIA v1.0.1 - Complemento de Seguridad con IA");
        registrar("==================================================");
        registrar("Burp Suite: " + (esProfessional ? "Professional" : "Community Edition"));

        actualizarVersionBurp();
        registrar("==================================================");

        registrar("Configuracion cargada:");
        registrar("  - URL de API: " + config.obtenerUrlApi());
        registrar("  - Modelo: " + config.obtenerModelo());
        registrar("  - Timeout AI (global): " + config.obtenerTiempoEsperaAI() + " segundos");
        registrar("  - Timeout AI (modelo activo): " +
            config.obtenerTiempoEsperaParaModelo(config.obtenerProveedorAI(), config.obtenerModelo()) + " segundos");
        registrar("  - Retraso: " + config.obtenerRetrasoSegundos() + " segundos");
        registrar("  - Maximo Concurrente: " + config.obtenerMaximoConcurrente());
        registrar("  - Maximo Hallazgos en Tabla: " + config.obtenerMaximoHallazgosTabla());
        registrar("  - Modo Detallado: " + (config.esDetallado() ? "ACTIVADO" : "desactivado"));
        registrar("  - Agente Activo (" + config.obtenerTipoAgente() + "): " + (config.agenteHabilitado() ? "HABILITADO" : "desactivado"));
        registrar("==================================================");

        limitador = new LimitadorTasa(config.obtenerMaximoConcurrente());

        estadisticas = new Estadisticas();
        modeloTablaTareas = new ModeloTablaTareas();
        modeloTablaHallazgos = new ModeloTablaHallazgos(config.obtenerMaximoHallazgosTabla());

        gestorTareas = new GestorTareas(modeloTablaTareas,
            mensaje -> {
                if (gestorConsola != null) {
                    gestorConsola.registrarInfo("GestorTareas", mensaje);
                    return;
                }
                if (stdout != null) {
                    stdout.println("[GestorTareas] " + I18nLogs.tr(mensaje));
                    stdout.flush();
                }
            });

        crearYRegistrarPestaniaPrincipal();
        inicializarPreferenciasUsuarioEnUI();


        inicializarAgenteSiHabilitado();

        manejadorHttp = new ManejadorHttpBurpIA(
            api, config, pestaniaPrincipal, stdout, stderr, limitador,
            estadisticas, gestorTareas, gestorConsola, modeloTablaHallazgos
        );
        if (gestorTareas != null) {
            gestorTareas.establecerManejadorCancelacion(manejadorHttp::cancelarEjecucionActiva);
            gestorTareas.establecerManejadorPausa(manejadorHttp::cancelarEjecucionActiva);
            gestorTareas.establecerManejadorReanudar(manejadorHttp::reencolarTarea);
        }
        if (pestaniaPrincipal != null) {
            pestaniaPrincipal.establecerManejadorReintentoTareas(manejadorHttp::reencolarTarea);
        }
        pestaniaPrincipal.establecerManejadorToggleCaptura(this::alternarCapturaDesdeUI);
        pestaniaPrincipal.establecerEstadoCaptura(manejadorHttp.estaCapturaActiva());
        api.http().registerHttpHandler(manejadorHttp);
        registrar("Manejador HTTP registrado exitosamente");

        registrarMenuContextual();
        registrar("Menu contextual de BurpIA registrado exitosamente");

        registrar("Inicialización de BurpIA completada exitosamente");
    }

    private void analizarSolicitudManual(HttpRequest solicitud, boolean forzarAnalisis, HttpRequestResponse solicitudRespuestaOriginal) {
        if (forzarAnalisis && manejadorHttp != null) {
            manejadorHttp.analizarSolicitudForzada(solicitud, solicitudRespuestaOriginal);
        }
    }

    private void registrarMenuContextual() {
        if (fabricaMenuContextual == null) {
            fabricaMenuContextual = new FabricaMenuContextual(api, this::analizarSolicitudManual, config, this::enviarAAgente);
            api.userInterface().registerContextMenuItemsProvider(fabricaMenuContextual);
        }
    }

    private void enviarAAgente(HttpRequestResponse solicitudRespuesta) {
        if (config == null) {
            registrarError("No se puede usar el Agente: configuracion no inicializada");
            return;
        }
        if (!config.agenteHabilitado()) {
            registrar(I18nLogs.Agente.ERROR_DESHABILITADO());
            return;
        }
        if (solicitudRespuesta == null) {
            registrarError("No se puede enviar al Agente: solicitud/respuesta nula");
            return;
        }

        String prompt = config.obtenerAgentePrompt();
        String request = solicitudRespuesta.request() != null ? solicitudRespuesta.request().toString() : "";
        String response = (solicitudRespuesta.response() != null) ? solicitudRespuesta.response().toString() : "";
        String inputFinal = aplicarTokensPromptAgente(prompt, request, response, config.obtenerIdiomaUi());

        PanelAgente panelAgente = obtenerPanelAgenteDisponible();
        if (panelAgente == null) {
            return;
        }
        pestaniaPrincipal.seleccionarPestaniaAgente();
        panelAgente.inyectarComando(inputFinal, 0);
    }

    private void enviarHallazgoAAgente(Hallazgo hallazgo) {
        if (config == null) {
            registrarError("No se puede usar el Agente: configuracion no inicializada");
            return;
        }
        if (!config.agenteHabilitado()) {
            registrar(I18nLogs.Agente.ERROR_DESHABILITADO());
            return;
        }
        if (hallazgo == null) {
            registrarError("No se puede enviar al Agente: hallazgo nulo");
            return;
        }

        String prompt = config.obtenerAgentePrompt();

        HttpRequestResponse evidencia = resolverEvidenciaIssue(hallazgo, null);
        String request = evidencia != null && evidencia.request() != null ? evidencia.request().toString() : "";
        String response = evidencia != null && evidencia.response() != null ? evidencia.response().toString() : "";
        String titulo = hallazgo.obtenerTitulo() != null ? hallazgo.obtenerTitulo() : "";
        String resumen = hallazgo.obtenerHallazgo() != null ? hallazgo.obtenerHallazgo() : "";
        String urlContext = hallazgo.obtenerUrl() != null ? hallazgo.obtenerUrl() : "";
        String lang = config.obtenerIdiomaUi();

        StringBuilder inputBuilder = new StringBuilder();
        if (!prompt.contains("{TITLE}")) {
            inputBuilder.append("Title: ").append(titulo).append("\n");
        }
        if (!prompt.contains("{SUMMARY}") && !prompt.contains("{DESCRIPTION}")) {
            inputBuilder.append("Summary: ").append(resumen).append("\n");
        }
        if (!prompt.contains("{URL}")) {
            inputBuilder.append("URL: ").append(urlContext).append("\n\n");
        }

        String inputFinal = inputBuilder.toString() + aplicarTokensPromptAgente(prompt, request, response, lang, titulo, resumen, urlContext);

        PanelAgente panelAgente = obtenerPanelAgenteDisponible();
        if (panelAgente == null) {
            return;
        }
        pestaniaPrincipal.seleccionarPestaniaAgente();

        panelAgente.inyectarComando(inputFinal, 0);
    }

    private String aplicarTokensPromptAgente(String prompt, String request, String response, String idioma) {
        return aplicarTokensPromptAgente(prompt, request, response, idioma, null, null, null);
    }

    private String aplicarTokensPromptAgente(String prompt, String request, String response, String idioma, 
                                            String titulo, String resumen, String url) {
        String resultado = prompt != null ? prompt : "";
        resultado = resultado.replace("{REQUEST}", request != null ? request : "");
        resultado = resultado.replace("{RESPONSE}", response != null ? response : "");
        resultado = resultado.replace("{OUTPUT_LANGUAGE}", (idioma != null && !idioma.trim().isEmpty()) ? idioma : "es");
        
        if (titulo != null) resultado = resultado.replace("{TITLE}", titulo);
        if (resumen != null) {
            resultado = resultado.replace("{SUMMARY}", resumen);
            resultado = resultado.replace("{DESCRIPTION}", resumen);
        }
        if (url != null) resultado = resultado.replace("{URL}", url);

        return resultado;
    }

    private PanelAgente obtenerPanelAgenteDisponible() {
        if (pestaniaPrincipal == null) {
            registrarError("No se puede usar el Agente: pestaña principal no disponible");
            return null;
        }
        PanelAgente panelAgente = pestaniaPrincipal.obtenerPanelAgente();
        if (panelAgente == null) {
            registrarError("No se puede usar el Agente: panel no disponible");
            return null;
        }
        return panelAgente;
    }

    private void abrirConfiguracion() {
        registrar("Abriendo dialogo de configuracion");

        if (pestaniaPrincipal == null || config == null || gestorConfig == null) {
            registrarError("No se pudo abrir configuracion: dependencias no inicializadas");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            PestaniaPrincipal pestaniaActual = pestaniaPrincipal;
            ConfiguracionAPI configActual = config;
            GestorConfiguracion gestorConfigActual = gestorConfig;

            if (pestaniaActual == null || configActual == null || gestorConfigActual == null) {
                return;
            }

            DialogoConfiguracion dialogo = new DialogoConfiguracion(
                    SwingUtilities.getWindowAncestor(pestaniaActual.obtenerPanel()),
                    configActual,
                    gestorConfigActual,
                    () -> {
                        if (modeloTablaHallazgos != null) {
                            modeloTablaHallazgos.establecerLimiteFilas(configActual.obtenerMaximoHallazgosTabla());
                        }
                        if (manejadorHttp != null) {
                            manejadorHttp.actualizarConfiguracion(configActual);
                        }
                        I18nUI.establecerIdioma(configActual.obtenerIdiomaUi());
                        if (pestaniaPrincipal != null) {
                            pestaniaPrincipal.aplicarIdioma();
                        }
                        if (gestorConsola != null) {
                            gestorConsola.registrarInfo("Configuracion guardada exitosamente");
                        }
                        if (api != null) {
                            api.logging().logToOutput(I18nUI.General.CONFIGURACION_GUARDADA());
                        }

                        registrar("Configuracion actualizada: detallado=" + configActual.esDetallado() +
                                ", maximoConcurrente=" + configActual.obtenerMaximoConcurrente() +
                                ", retraso=" + configActual.obtenerRetrasoSegundos() + "s" +
                        ", maximoHallazgos=" + configActual.obtenerMaximoHallazgosTabla());

                        pestaniaPrincipal.actualizarVisibilidadAgentes();
                    }
            );
            dialogo.setVisible(true);
        });
    }

    private void crearYRegistrarPestaniaPrincipal() {
        Runnable crearUi = () -> {
            pestaniaPrincipal = new PestaniaPrincipal(api, estadisticas, gestorTareas, gestorConsola, modeloTablaTareas, modeloTablaHallazgos, esProfessional, config);
            pestaniaPrincipal.establecerManejadorConfiguracion(this::abrirConfiguracion);
            pestaniaPrincipal.establecerManejadorEnviarAAgente(this::enviarHallazgoAAgente);
            pestaniaPrincipal.establecerManejadorCambioAgente(() -> {
                guardarConfiguracionSilenciosa("cambio-agente-rapido");
                if (manejadorHttp != null) {
                    manejadorHttp.actualizarConfiguracion(config);
                }
                pestaniaPrincipal.actualizarVisibilidadAgentes();
                pestaniaPrincipal.aplicarIdioma();
                registrar("Agente cambiado rápidamente a: " + config.obtenerTipoAgente());
            });

            api.userInterface().registerSuiteTab("BurpIA", pestaniaPrincipal.obtenerPanel());
            registrar("Pestania de UI registrada exitosamente");
        };

        if (SwingUtilities.isEventDispatchThread()) {
            crearUi.run();
            return;
        }

        try {
            SwingUtilities.invokeAndWait(crearUi);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Inicializacion UI interrumpida", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("No se pudo inicializar la UI de BurpIA", e);
        }
    }

    private void registrar(String mensaje) {
        if (gestorConsola != null) {
            gestorConsola.registrarInfo("BurpIA", mensaje);
            return;
        }
        String mensajeLocalizado = I18nLogs.tr(mensaje);
        if (stdout != null) {
            stdout.println("[BurpIA] " + mensajeLocalizado);
            stdout.flush();
            return;
        }
        if (api != null) {
            try {
                api.logging().logToOutput("[BurpIA] " + mensajeLocalizado);
            } catch (Exception ignored) {
            }
        }
    }

    private void registrarError(String mensaje) {
        if (gestorConsola != null) {
            gestorConsola.registrarError("BurpIA", mensaje);
            return;
        }
        String mensajeLocalizado = I18nLogs.tr(mensaje);
        if (stderr != null) {
            stderr.println("[BurpIA] [ERROR] " + mensajeLocalizado);
            stderr.flush();
            return;
        }
        if (api != null) {
            try {
                api.logging().logToError("[BurpIA] [ERROR] " + mensajeLocalizado);
            } catch (Exception ignored) {
            }
        }
    }

    private void alternarCapturaDesdeUI() {
        if (manejadorHttp == null || pestaniaPrincipal == null) {
            return;
        }
        if (manejadorHttp.estaCapturaActiva()) {
            manejadorHttp.pausarCaptura();
        } else {
            manejadorHttp.reanudarCaptura();
        }
        config.establecerEscaneoPasivoHabilitado(manejadorHttp.estaCapturaActiva());
        guardarConfiguracionSilenciosa("captura");
        pestaniaPrincipal.establecerEstadoCaptura(manejadorHttp.estaCapturaActiva());
        registrar("Estado de captura actualizado: " + (manejadorHttp.estaCapturaActiva() ? "ACTIVA" : "PAUSADA"));
    }

    private void inicializarPreferenciasUsuarioEnUI() {
        if (pestaniaPrincipal == null || config == null) {
            return;
        }

        boolean autoGuardadoIssuesPermitido = esProfessional && config.autoGuardadoIssuesHabilitado();
        if (config.autoGuardadoIssuesHabilitado() != autoGuardadoIssuesPermitido) {
            config.establecerAutoGuardadoIssuesHabilitado(autoGuardadoIssuesPermitido);
            guardarConfiguracionSilenciosa("auto-issues-edicion");
        }

        pestaniaPrincipal.establecerGuardadoAutomaticoIssuesActivo(autoGuardadoIssuesPermitido);
        pestaniaPrincipal.establecerAutoScrollConsolaActivo(config.autoScrollConsolaHabilitado());

        pestaniaPrincipal.establecerManejadorAutoGuardadoIssues(activo -> {
            boolean autoGuardadoNormalizado = esProfessional && activo;
            if (config.autoGuardadoIssuesHabilitado() == autoGuardadoNormalizado) {
                return;
            }
            config.establecerAutoGuardadoIssuesHabilitado(autoGuardadoNormalizado);
            guardarConfiguracionSilenciosa("auto-issues");
        });

        pestaniaPrincipal.establecerManejadorAutoScrollConsola(activo -> {
            if (config.autoScrollConsolaHabilitado() == activo) {
                return;
            }
            config.establecerAutoScrollConsolaHabilitado(activo);
            guardarConfiguracionSilenciosa("auto-scroll");
        });

        PanelAgente panelAgente = pestaniaPrincipal.obtenerPanelAgente();
        if (panelAgente != null) {
            panelAgente.establecerManejadorCambioConfiguracion(() -> guardarConfiguracionSilenciosa("agente-delay"));
        }
    }

    private void inicializarAgenteSiHabilitado() {
        if (!config.agenteHabilitado()) {
            registrar("Agente deshabilitado en configuración");
            return;
        }

        if (pestaniaPrincipal == null) {
            registrarError("No se puede inicializar el Agente: pestaniaPrincipal es null");
            return;
        }

        PanelAgente panelAgente = pestaniaPrincipal.obtenerPanelAgente();
        if (panelAgente == null) {
            registrarError("No se puede inicializar el Agente: panel no disponible");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            javax.swing.Timer timer = new javax.swing.Timer(1500, e -> {
                panelAgente.forzarInyeccionPromptInicial();
                registrar("Agente inicializado - prompt inicial inyectado");
                ((javax.swing.Timer) e.getSource()).stop();
            });
            timer.setRepeats(false);
            timer.start();
        });

        registrar("Agente programado para inicialización");
    }

    private void guardarConfiguracionSilenciosa(String origen) {
        if (gestorConfig == null || config == null) {
            return;
        }
        StringBuilder mensajeError = new StringBuilder();
        if (!gestorConfig.guardarConfiguracion(config, mensajeError)) {
            String detalle = mensajeError.toString().trim();
            if (detalle.isEmpty()) {
                detalle = "Error desconocido";
            }
            registrarError("No se pudo persistir configuracion (" + origen + "): " + detalle);
        }
    }

    public void unload() {
        registrar("Descargando extensión BurpIA...");

        if (manejadorHttp != null) {
            manejadorHttp.shutdown();
            manejadorHttp = null;
            registrar("ExecutorService cerrado");
        }

        if (pestaniaPrincipal != null) {
            pestaniaPrincipal.destruir();
            pestaniaPrincipal = null;
        }

        if (gestorConsola != null) {
            gestorConsola.shutdown();
            gestorConsola = null;
        }

        if (gestorTareas != null) {
            gestorTareas.shutdown();
            gestorTareas = null;
        }

        if (limitador != null) {
            limitador = null;
        }

        registrar("Extensión BurpIA descargada correctamente");
    }

    public static burp.api.montoya.scanner.audit.issues.AuditIssue crearAuditIssueDesdeHallazgo(
            Hallazgo hallazgo,
            HttpRequestResponse solicitudRespuestaEvidencia) {

        if (hallazgo == null) {
            return null;
        }

        burp.api.montoya.scanner.audit.issues.AuditIssueSeverity severity = convertirSeveridad(hallazgo.obtenerSeveridad());
        burp.api.montoya.scanner.audit.issues.AuditIssueConfidence confidence = convertirConfianza(hallazgo.obtenerConfianza());

        HttpRequestResponse evidenciaFinal = resolverEvidenciaIssue(hallazgo, solicitudRespuestaEvidencia);

        HttpRequestResponse[] evidencias = evidenciaFinal != null
            ? new HttpRequestResponse[] { evidenciaFinal }
            : new HttpRequestResponse[0];

        String detalleIssue = I18nUI.Hallazgos.DETALLE_ISSUE();
        String remediation = I18nUI.Hallazgos.REMEDIACION_ISSUE();

        return burp.api.montoya.scanner.audit.issues.AuditIssue.auditIssue(
            hallazgo.obtenerTitulo(),
            hallazgo.obtenerHallazgo() + "\n\nURL: " + hallazgo.obtenerUrl(),
            detalleIssue,
            hallazgo.obtenerUrl(),
            severity,
            confidence,
            null,
            remediation,
            null,
            evidencias
        );
    }

    static HttpRequestResponse resolverEvidenciaIssue(Hallazgo hallazgo, HttpRequestResponse solicitudRespuestaEvidencia) {
        if (solicitudRespuestaEvidencia != null) {
            return solicitudRespuestaEvidencia;
        }
        return hallazgo != null ? hallazgo.obtenerEvidenciaHttp() : null;
    }

    public static boolean guardarAuditIssueDesdeHallazgo(
            MontoyaApi api,
            Hallazgo hallazgo,
            HttpRequestResponse solicitudRespuestaEvidencia) {
        if (!esBurpProfessional(api) || api == null || api.siteMap() == null) {
            return false;
        }
        burp.api.montoya.scanner.audit.issues.AuditIssue issue =
            crearAuditIssueDesdeHallazgo(hallazgo, solicitudRespuestaEvidencia);
        if (issue == null) {
            return false;
        }
        api.siteMap().add(issue);
        return true;
    }

    private static burp.api.montoya.scanner.audit.issues.AuditIssueSeverity convertirSeveridad(String severidad) {
        String severidadNormalizada = Hallazgo.normalizarSeveridad(severidad);
        switch (severidadNormalizada) {
            case Hallazgo.SEVERIDAD_CRITICAL:
            case Hallazgo.SEVERIDAD_HIGH:
                return burp.api.montoya.scanner.audit.issues.AuditIssueSeverity.HIGH;
            case Hallazgo.SEVERIDAD_MEDIUM:
                return burp.api.montoya.scanner.audit.issues.AuditIssueSeverity.MEDIUM;
            case Hallazgo.SEVERIDAD_LOW:
                return burp.api.montoya.scanner.audit.issues.AuditIssueSeverity.LOW;
            case Hallazgo.SEVERIDAD_INFO:
                return burp.api.montoya.scanner.audit.issues.AuditIssueSeverity.INFORMATION;
            default:
                return burp.api.montoya.scanner.audit.issues.AuditIssueSeverity.INFORMATION;
        }
    }

    private static burp.api.montoya.scanner.audit.issues.AuditIssueConfidence convertirConfianza(String confianza) {
        String confianzaNormalizada = Hallazgo.normalizarConfianza(confianza);
        switch (confianzaNormalizada) {
            case Hallazgo.CONFIANZA_ALTA:
                return burp.api.montoya.scanner.audit.issues.AuditIssueConfidence.CERTAIN;
            case Hallazgo.CONFIANZA_MEDIA:
                return burp.api.montoya.scanner.audit.issues.AuditIssueConfidence.FIRM;
            case Hallazgo.CONFIANZA_BAJA:
                return burp.api.montoya.scanner.audit.issues.AuditIssueConfidence.TENTATIVE;
            default:
                return burp.api.montoya.scanner.audit.issues.AuditIssueConfidence.TENTATIVE;
        }
    }

    public static boolean esBurpProfessional(MontoyaApi api) {
        if (api == null) {
            return false;
        }
        try {
            if (api.burpSuite() != null && api.burpSuite().version() != null) {
                BurpSuiteEdition edicion = api.burpSuite().version().edition();
                return edicion == BurpSuiteEdition.PROFESSIONAL;
            }
        } catch (Exception ignored) {
        }

        try {
            return api.ai() != null && api.ai().isEnabled();
        } catch (Exception ignored) {
            return false;
        }
    }

    private void actualizarVersionBurp() {
        String version = obtenerVersionBurp(api);
        if (version != null) {
            registrar("Version Burp Suite: " + version);
        }
    }

    private String obtenerVersionBurp(MontoyaApi api) {
        try {
            if (api != null && api.burpSuite() != null && api.burpSuite().version() != null) {
                String version = api.burpSuite().version().toString();
                if (version != null && !version.trim().isEmpty()) {
                    return version;
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public boolean esBurpProfessional() {
        return esProfessional;
    }
}
