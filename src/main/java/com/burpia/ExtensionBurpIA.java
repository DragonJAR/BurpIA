package com.burpia;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.BurpSuiteEdition;
import burp.api.montoya.http.message.HttpRequestResponse;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.GestorConfiguracion;
import com.burpia.i18n.I18nLogs;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Estadisticas;
import com.burpia.model.Hallazgo;
import com.burpia.ui.ModeloTablaHallazgos;
import com.burpia.ui.ModeloTablaTareas;
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
        this.esProfessional = detectarBurpProfessional(api);

        api.extension().setName("BurpIA");
        api.extension().registerUnloadingHandler(() -> {
            unload();
        });

        this.stdout = new PrintWriter(new OutputStream() {
            @Override
            public void write(int b) {
                api.logging().logToOutput(String.valueOf((char) b));
            }
            @Override
            public void write(byte[] b, int off, int len) {
                api.logging().logToOutput(new String(b, off, len, StandardCharsets.UTF_8));
            }
            @Override
            public void flush() {}
        }, true);
        this.stderr = new PrintWriter(new OutputStream() {
            @Override
            public void write(int b) {
                api.logging().logToError(String.valueOf((char) b));
            }
            @Override
            public void write(byte[] b, int off, int len) {
                api.logging().logToError(new String(b, off, len, StandardCharsets.UTF_8));
            }
            @Override
            public void flush() {}
        }, true);

        gestorConfig = new GestorConfiguracion(stdout, stderr);
        config = gestorConfig.cargarConfiguracion();
        I18nUI.establecerIdioma(config.obtenerIdiomaUi());

        registrar("==================================================");
        registrar(" BurpIA v1.0.0 - Complemento de Seguridad con IA");
        registrar("==================================================");
        registrar("Burp Suite: " + (esProfessional ? "Professional" : "Community Edition"));

        String versionBurp = obtenerVersionBurp(api);
        if (versionBurp != null) {
            registrar("Version Burp Suite: " + versionBurp);
        }
        registrar("==================================================");

        registrar("Configuracion cargada:");
        registrar("  - URL de API: " + config.obtenerUrlApi());
        registrar("  - Modelo: " + config.obtenerModelo());
        registrar("  - Retraso: " + config.obtenerRetrasoSegundos() + " segundos");
        registrar("  - Maximo Concurrente: " + config.obtenerMaximoConcurrente());
        registrar("  - Maximo Hallazgos en Tabla: " + config.obtenerMaximoHallazgosTabla());
        registrar("  - Modo Detallado: " + (config.esDetallado() ? "ACTIVADO" : "desactivado"));
        registrar("==================================================");

        limitador = new LimitadorTasa(config.obtenerMaximoConcurrente());

        estadisticas = new Estadisticas();
        modeloTablaTareas = new ModeloTablaTareas();
        modeloTablaHallazgos = new ModeloTablaHallazgos(config.obtenerMaximoHallazgosTabla());

        gestorTareas = new GestorTareas(modeloTablaTareas,
            mensaje -> stdout.println("[GestorTareas] " + I18nLogs.tr(mensaje)));

        gestorConsola = new GestorConsolaGUI();
        gestorConsola.capturarStreamsOriginales(stdout, stderr);

        crearYRegistrarPestaniaPrincipal();
        inicializarPreferenciasUsuarioEnUI();

        manejadorHttp = new ManejadorHttpBurpIA(
            api, config, pestaniaPrincipal, stdout, stderr, limitador,
            estadisticas, gestorTareas, gestorConsola, modeloTablaHallazgos
        );
        pestaniaPrincipal.establecerManejadorToggleCaptura(this::alternarCapturaDesdeUI);
        pestaniaPrincipal.establecerEstadoCaptura(manejadorHttp.estaCapturaActiva());
        api.http().registerHttpHandler(manejadorHttp);
        registrar("Manejador HTTP registrado exitosamente");

        fabricaMenuContextual = new FabricaMenuContextual(api, (solicitud, forzarAnalisis, solicitudRespuestaOriginal) -> {
            if (forzarAnalisis && manejadorHttp != null) {
                manejadorHttp.analizarSolicitudForzada(solicitud, solicitudRespuestaOriginal);
            }
        });
        api.userInterface().registerContextMenuItemsProvider(fabricaMenuContextual);
        registrar("Menu contextual de BurpIA registrado exitosamente");

        api.logging().logToOutput("==================================================");
        api.logging().logToOutput(I18nUI.tr(
            " BurpIA v1.0.0 - Complemento de Seguridad con IA Cargado",
            " BurpIA v1.0.0 - AI Security Plugin Loaded"
        ));
        api.logging().logToOutput(I18nUI.tr(
            " Modo detallado: ",
            " Verbose mode: "
        ) + (config.esDetallado() ? I18nUI.tr("ACTIVADO", "ENABLED") : I18nUI.tr("desactivado", "disabled")));
        api.logging().logToOutput(I18nUI.tr(
            " Ve a la pestania 'BurpIA' y configura tu clave de API",
            " Go to the 'BurpIA' tab and configure your API key"
        ));
        api.logging().logToOutput("==================================================");

        registrar("Inicialización de BurpIA completada exitosamente");
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
                            api.logging().logToOutput(I18nUI.tr("Configuracion guardada", "Configuration saved"));
                        }

                        registrar("Configuracion actualizada: detallado=" + configActual.esDetallado() +
                                ", maximoConcurrente=" + configActual.obtenerMaximoConcurrente() +
                                ", retraso=" + configActual.obtenerRetrasoSegundos() + "s" +
                                ", maximoHallazgos=" + configActual.obtenerMaximoHallazgosTabla());
                    }
            );
            dialogo.setVisible(true);
        });
    }

    private void crearYRegistrarPestaniaPrincipal() {
        Runnable crearUi = () -> {
            pestaniaPrincipal = new PestaniaPrincipal(
                api,
                estadisticas,
                gestorTareas,
                gestorConsola,
                modeloTablaTareas,
                modeloTablaHallazgos,
                esProfessional
            );
            pestaniaPrincipal.establecerManejadorConfiguracion(this::abrirConfiguracion);
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

        pestaniaPrincipal.establecerGuardadoAutomaticoIssuesActivo(config.autoGuardadoIssuesHabilitado());
        pestaniaPrincipal.establecerAutoScrollConsolaActivo(config.autoScrollConsolaHabilitado());

        pestaniaPrincipal.establecerManejadorAutoGuardadoIssues(activo -> {
            if (config.autoGuardadoIssuesHabilitado() == activo) {
                return;
            }
            config.establecerAutoGuardadoIssuesHabilitado(activo);
            guardarConfiguracionSilenciosa("auto-issues");
        });

        pestaniaPrincipal.establecerManejadorAutoScrollConsola(activo -> {
            if (config.autoScrollConsolaHabilitado() == activo) {
                return;
            }
            config.establecerAutoScrollConsolaHabilitado(activo);
            guardarConfiguracionSilenciosa("auto-scroll");
        });
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

        HttpRequestResponse[] evidencias = solicitudRespuestaEvidencia != null
            ? new HttpRequestResponse[] { solicitudRespuestaEvidencia }
            : new HttpRequestResponse[0];

        String detalleIssue = I18nUI.tr(
            "Revisa la solicitud para confirmar la vulnerabilidad. Este hallazgo se guarda automáticamente para que no se pierda al cerrar Burp, pero requiere validación manual. Haz clic derecho en la pestaña de hallazgos para enviar la petición al Repeater o al Intruder. Nunca confíes ciegamente en los resultados de una IA.",
            "Review the request to confirm the vulnerability. This finding is saved automatically so it is not lost when Burp closes, but it requires manual validation. Right-click on the findings tab to send the request to Repeater or Intruder. Never blindly trust AI results."
        );
        String remediation = I18nUI.tr(
            "Revisa los encabezados y el cuerpo de la solicitud HTTP para confirmar la vulnerabilidad. Este hallazgo se guarda automáticamente para que no se pierda al cerrar Burp, pero requiere validación manual. Haz clic derecho en la pestaña de hallazgos para enviar la petición al Repeater o al Intruder, y nunca confíes ciegamente en los resultados de una IA.",
            "Review HTTP request headers and body to confirm the vulnerability. This finding is saved automatically so it is not lost when Burp closes, but it requires manual validation. Right-click on the findings tab to send the request to Repeater or Intruder, and never blindly trust AI results."
        );

        return burp.api.montoya.scanner.audit.issues.AuditIssue.auditIssue(
            hallazgo.obtenerHallazgo(),
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

    public static boolean guardarAuditIssueDesdeHallazgo(
            MontoyaApi api,
            Hallazgo hallazgo,
            HttpRequestResponse solicitudRespuestaEvidencia) {
        if (api == null || api.siteMap() == null) {
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
        switch (severidad) {
            case "Critical":
            case "High":
                return burp.api.montoya.scanner.audit.issues.AuditIssueSeverity.HIGH;
            case "Medium":
                return burp.api.montoya.scanner.audit.issues.AuditIssueSeverity.MEDIUM;
            case "Low":
            case "Info":
                return burp.api.montoya.scanner.audit.issues.AuditIssueSeverity.LOW;
            default:
                return burp.api.montoya.scanner.audit.issues.AuditIssueSeverity.LOW;
        }
    }

    private static burp.api.montoya.scanner.audit.issues.AuditIssueConfidence convertirConfianza(String confianza) {
        switch (confianza) {
            case "High":
                return burp.api.montoya.scanner.audit.issues.AuditIssueConfidence.CERTAIN;
            case "Medium":
                return burp.api.montoya.scanner.audit.issues.AuditIssueConfidence.FIRM;
            case "Low":
                return burp.api.montoya.scanner.audit.issues.AuditIssueConfidence.TENTATIVE;
            default:
                return burp.api.montoya.scanner.audit.issues.AuditIssueConfidence.TENTATIVE;
        }
    }

    private boolean detectarBurpProfessional(MontoyaApi api) {
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
