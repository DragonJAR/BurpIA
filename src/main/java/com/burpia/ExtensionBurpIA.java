package com.burpia;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.handler.HttpResponseReceived;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.GestorConfiguracion;
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

    public ExtensionBurpIA() {
    }

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;

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
                api.logging().logToOutput(new String(b, off, len));
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
                api.logging().logToError(new String(b, off, len));
            }
            @Override
            public void flush() {}
        }, true);

        registrar("==================================================");
        registrar(" BurpIA v1.0.0 - Complemento de Seguridad con IA Cargando...");
        registrar("==================================================");

        gestorConfig = new GestorConfiguracion(stdout, stderr);
        config = gestorConfig.cargarConfiguracion();

        registrar("Configuracion cargada:");
        registrar("  - URL de API: " + config.obtenerUrlApi());
        registrar("  - Modelo: " + config.obtenerModelo());
        registrar("  - Retraso: " + config.obtenerRetrasoSegundos() + " segundos");
        registrar("  - Maximo Concurrente: " + config.obtenerMaximoConcurrente());
        registrar("  - Modo Detallado: " + (config.esDetallado() ? "ACTIVADO" : "desactivado"));
        registrar("==================================================");

        limitador = new LimitadorTasa(config.obtenerMaximoConcurrente());

        estadisticas = new Estadisticas();
        modeloTablaTareas = new ModeloTablaTareas();
        modeloTablaHallazgos = new ModeloTablaHallazgos();

        gestorTareas = new GestorTareas(modeloTablaTareas,
            mensaje -> stdout.println("[GestorTareas] " + mensaje));

        gestorConsola = new GestorConsolaGUI();
        gestorConsola.capturarStreamsOriginales(stdout, stderr);

        SwingUtilities.invokeLater(() -> {
            pestaniaPrincipal = new PestaniaPrincipal(api, estadisticas, gestorTareas, gestorConsola, modeloTablaTareas, modeloTablaHallazgos);
            pestaniaPrincipal.establecerManejadorConfiguracion(this::abrirConfiguracion);
            api.userInterface().registerSuiteTab("BurpIA", pestaniaPrincipal.obtenerPanel());
            registrar("Pestania de UI registrada exitosamente");
        });

        manejadorHttp = new ManejadorHttpBurpIA(
            api, config, pestaniaPrincipal, stdout, stderr, limitador,
            estadisticas, gestorTareas, gestorConsola, modeloTablaHallazgos
        );
        api.http().registerHttpHandler(manejadorHttp);
        registrar("Manejador HTTP registrado exitosamente");

        fabricaMenuContextual = new FabricaMenuContextual(api, (solicitud, forzarAnalisis) -> {
            if (forzarAnalisis && manejadorHttp != null) {
                manejadorHttp.analizarSolicitudForzada(solicitud);
            }
        });
        api.userInterface().registerContextMenuItemsProvider(fabricaMenuContextual);
        registrar("Menu contextual de BurpIA registrado exitosamente");

        api.logging().logToOutput("==================================================");
        api.logging().logToOutput(" BurpIA v1.0.0 - Complemento de Seguridad con IA Cargado");
        api.logging().logToOutput(" Modo detallado: " + (config.esDetallado() ? "ACTIVADO" : "desactivado"));
        api.logging().logToOutput(" Ve a la pestania 'BurpIA' y configura tu clave de API");
        api.logging().logToOutput("==================================================");

        registrar("Inicialización de BurpIA completada exitosamente");
    }

    private void abrirConfiguracion() {
        registrar("Abriendo dialogo de configuracion");

        SwingUtilities.invokeLater(() -> {
            DialogoConfiguracion dialogo = new DialogoConfiguracion(
                    SwingUtilities.getWindowAncestor(pestaniaPrincipal.obtenerPanel()),
                    config,
                    gestorConfig,
                    () -> {
                        limitador = new LimitadorTasa(config.obtenerMaximoConcurrente());
                        manejadorHttp.actualizarConfiguracion(config);
                        gestorConsola.registrarInfo("Configuracion guardada exitosamente");
                        api.logging().logToOutput("Configuracion guardada");

                        registrar("Configuracion actualizada: detallado=" + config.esDetallado() +
                                ", maximoConcurrente=" + config.obtenerMaximoConcurrente() +
                                ", retraso=" + config.obtenerRetrasoSegundos() + "s");
                    }
            );
            dialogo.setVisible(true);
        });
    }

    private void registrar(String mensaje) {
        stdout.println("[BurpIA] " + mensaje);
        stdout.flush();
    }

    private void registrarError(String mensaje) {
        stderr.println("[BurpIA] [ERROR] " + mensaje);
        stderr.flush();
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
            HttpResponseReceived respuestaRecibida) {

        burp.api.montoya.scanner.audit.issues.AuditIssueSeverity severity = convertirSeveridad(hallazgo.obtenerSeveridad());
        burp.api.montoya.scanner.audit.issues.AuditIssueConfidence confidence = convertirConfianza(hallazgo.obtenerConfianza());

        return burp.api.montoya.scanner.audit.issues.AuditIssue.auditIssue(
            hallazgo.obtenerHallazgo(),
            hallazgo.obtenerHallazgo() + "\n\nURL: " + hallazgo.obtenerUrl(),
            "Revisa la solicitud para confirmar la vulnerabilidad. Este hallazgo se guarda automáticamente para que no se pierda al cerrar Burp, pero requiere validación manual. Haz clic derecho en la pestaña de hallazgos para enviar la petición al Repeater o al Intruder. Nunca confíes ciegamente en los resultados de una IA.",
            hallazgo.obtenerUrl(),
            severity,
            confidence,
            null,
            "Revisa los encabezados y el cuerpo de la solicitud HTTP para confirmar la vulnerabilidad. Este hallazgo se guarda automáticamente para que no se pierda al cerrar Burp, pero requiere validación manual. Haz clic derecho en la pestaña de hallazgos para enviar la petición al Repeater o al Intruder, y nunca confíes ciegamente en los resultados de una IA.",
            null,
            new burp.api.montoya.http.message.HttpRequestResponse[0]
        );
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
}
