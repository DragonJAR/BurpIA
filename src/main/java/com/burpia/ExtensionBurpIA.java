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
import com.burpia.ui.EstilosUI;
import com.burpia.ui.DialogoConfiguracion;
import com.burpia.ui.FabricaMenuContextual;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.GestorTareas;
import com.burpia.util.LimitadorTasa;
import com.burpia.util.Normalizador;
import com.burpia.util.VersionBurpIA;
import javax.swing.*;
import java.awt.Frame;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.burpia.ui.UIUtils.ejecutarEnEdt;

public class ExtensionBurpIA implements BurpExtension {
    private static final String TOKEN_REQUEST = "{REQUEST}";
    private static final String TOKEN_RESPONSE = "{RESPONSE}";
    private static final String TOKEN_TITLE = "{TITLE}";
    private static final String TOKEN_SUMMARY = "{SUMMARY}";
    private static final String TOKEN_DESCRIPTION = "{DESCRIPTION}";
    private static final String TOKEN_URL = "{URL}";

    private MontoyaApi api;
    private ConfiguracionAPI config;
    private GestorConfiguracion gestorConfig;
    private PestaniaPrincipal pestaniaPrincipal;
    private LimitadorTasa limitador;
    private ManejadorHttpBurpIA manejadorHttp;
    private PrintWriter stdout;
    private PrintWriter stderr;
    private GestorLoggingUnificado gestorLogging;
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

        this.stdout = crearPrintWriterMontoya(api.logging()::logToOutput);
        this.stderr = crearPrintWriterMontoya(api.logging()::logToError);

        gestorConfig = new GestorConfiguracion(stdout, stderr);
        config = gestorConfig.cargarConfiguracion();
        EstilosUI.actualizarFuentes(config);
        I18nUI.establecerIdioma(config.obtenerIdiomaUi());
        gestorConsola = new GestorConsolaGUI();
        gestorConsola.capturarStreamsOriginales(stdout, stderr);

        gestorLogging = GestorLoggingUnificado.crear(gestorConsola, stdout, stderr, api, null);

        registrarResumenInicio();

        limitador = new LimitadorTasa(config.obtenerMaximoConcurrente());

        estadisticas = new Estadisticas();
        modeloTablaTareas = new ModeloTablaTareas(config.obtenerMaximoTareasTabla());
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
                estadisticas, gestorTareas, gestorConsola, modeloTablaHallazgos);
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

        pestaniaPrincipal.establecerManejadorCambioFiltros(
            () -> guardarConfiguracionSilenciosa("cambio-filtros-hallazgos")
        );
        api.http().registerHttpHandler(manejadorHttp);
        if (config.esDetallado()) {
            registrar("Manejador HTTP registrado exitosamente");
        }

        registrarMenuContextual();
        if (config.esDetallado()) {
            registrar("Menu contextual de BurpIA registrado exitosamente");
        }

        registrar(I18nLogs.Inicializacion.INICIALIZACION_COMPLETA());
    }

    private PrintWriter crearPrintWriterMontoya(Consumer<String> sink) {
        return new PrintWriter(new OutputStream() {
            private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            @Override
            public void write(int b) {
                if (b == '\n') {
                    flushBuffer();
                    return;
                }
                buffer.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) {
                String texto = new String(b, off, len, StandardCharsets.UTF_8);
                buffer.write(b, off, len);
                if (texto.contains("\n")) {
                    flushBuffer();
                }
            }

            private void flushBuffer() {
                if (buffer.size() == 0) {
                    return;
                }
                sink.accept(buffer.toString(StandardCharsets.UTF_8));
                buffer.reset();
            }

            @Override
            public void flush() {
                flushBuffer();
            }
        }, true);
    }

    private void analizarSolicitudManual(HttpRequest solicitud, boolean forzarAnalisis,
            HttpRequestResponse solicitudRespuestaOriginal) {
        if (forzarAnalisis && manejadorHttp != null) {
            manejadorHttp.analizarSolicitudForzada(solicitud, solicitudRespuestaOriginal);
        }
    }

    private void registrarMenuContextual() {
        if (fabricaMenuContextual == null) {
            fabricaMenuContextual = new FabricaMenuContextual(
                    api,
                    this::analizarSolicitudManual,
                    null,
                    config,
                    this::enviarAAgente,
                    this::enviarFlujoAAgente,
                    () -> guardarConfiguracionSilenciosa("alertas-enviar-a-contexto"),
                    obtenerFramePadre());
            api.userInterface().registerContextMenuItemsProvider(fabricaMenuContextual);
        }
    }
    
    private Frame obtenerFramePadre() {
        return pestaniaPrincipal != null ? 
            (Frame) SwingUtilities.getWindowAncestor(pestaniaPrincipal) : null;
    }

    private boolean enviarAAgente(HttpRequestResponse solicitudRespuesta) {
        if (solicitudRespuesta == null) {
            registrarError("No se puede enviar al Agente: solicitud/respuesta nula");
            return false;
        }
        try {
            String prompt = obtenerPromptAgenteDisponible();
            if (prompt == null) {
                return false;
            }
            String request = serializarSolicitudSiNecesario(prompt, solicitudRespuesta);
            String response = serializarRespuestaSiNecesario(prompt, solicitudRespuesta);
            String inputFinal = aplicarTokensPromptAgente(prompt, request, response, config.obtenerIdiomaUi());
            return enviarPayloadAgente(inputFinal);
        } catch (Exception e) {
            registrarError("No se pudo enviar al Agente: " + e.getMessage());
            return false;
        }
    }

    private boolean enviarFlujoAAgente(List<HttpRequestResponse> solicitudesRespuesta) {
        try {
            String prompt = obtenerPromptAgenteDisponible();
            if (prompt == null) {
                return false;
            }

            List<HttpRequestResponse> solicitudesValidas = filtrarSolicitudesConRequest(solicitudesRespuesta);
            if (solicitudesValidas.size() < 2) {
                registrarError(I18nUI.Contexto.MSG_FLUJO_REQUIERE_MULTIPLES_VALIDAS());
                return false;
            }

            String requests = serializarSolicitudesFlujoSiNecesario(prompt, solicitudesValidas);
            String responses = serializarRespuestasFlujoSiNecesario(prompt, solicitudesValidas);
            String inputFinal = aplicarTokensPromptAgente(prompt, requests, responses, config.obtenerIdiomaUi());
            return enviarPayloadAgente(inputFinal);
        } catch (Exception e) {
            registrarError("No se pudo enviar flujo al Agente: " + e.getMessage());
            return false;
        }
    }

    private boolean enviarHallazgoAAgente(Hallazgo hallazgo) {
        if (config == null) {
            registrarError("No se puede usar el Agente: configuracion no inicializada");
            return false;
        }
        if (!config.agenteHabilitado()) {
            registrar(I18nLogs.Agente.ERROR_DESHABILITADO());
            return false;
        }
        if (hallazgo == null) {
            registrarError("No se puede enviar al Agente: hallazgo nulo");
            return false;
        }
        try {
            String prompt = obtenerPromptAgenteDisponible();
            if (prompt == null) {
                return false;
            }

            HttpRequestResponse evidencia = resolverEvidenciaIssue(hallazgo, null);
            String request = serializarSolicitudSiNecesario(prompt, evidencia, hallazgo.obtenerUrl());
            String response = serializarRespuestaSiNecesario(prompt, evidencia);
            String tituloValor = valorSeguro(hallazgo.obtenerTitulo());
            String resumenValor = valorSeguro(hallazgo.obtenerHallazgo());
            String urlContextValor = valorSeguro(hallazgo.obtenerUrl());
            boolean usaTitulo = contieneToken(prompt, TOKEN_TITLE);
            boolean usaResumen = contieneAlgunToken(prompt, TOKEN_SUMMARY, TOKEN_DESCRIPTION);
            boolean usaUrl = contieneToken(prompt, TOKEN_URL);
            String titulo = usaTitulo && tieneContenido(tituloValor) ? tituloValor : "";
            String resumen = usaResumen && tieneContenido(resumenValor) ? resumenValor : "";
            String urlContext = usaUrl && tieneContenido(urlContextValor) ? urlContextValor : "";
            String lang = config.obtenerIdiomaUi();

            StringBuilder inputBuilder = new StringBuilder();
            agregarLineaSiHayContenido(inputBuilder, !usaTitulo, "Title", tituloValor);
            agregarLineaSiHayContenido(inputBuilder, !usaResumen, "Summary", resumenValor);
            agregarLineaSiHayContenido(inputBuilder, !usaUrl, "URL", urlContextValor);
            if (inputBuilder.length() > 0) {
                inputBuilder.append("\n");
            }

            String inputFinal = inputBuilder.toString()
                    + aplicarTokensPromptAgente(prompt, request, response, lang, titulo, resumen, urlContext);

            return enviarPayloadAgente(inputFinal);
        } catch (Exception e) {
            registrarError("No se pudo enviar hallazgo al Agente: " + e.getMessage());
            return false;
        }
    }

    private boolean enfocarEInyectarEnAgente(PanelAgente panelAgente, String inputFinal) {
        if (panelAgente == null || pestaniaPrincipal == null) {
            return false;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            pestaniaPrincipal.seleccionarPestaniaAgente();
            panelAgente.inyectarComando(inputFinal, 0);
            return true;
        }
        AtomicBoolean enviado = new AtomicBoolean(false);
        try {
            SwingUtilities.invokeAndWait(() -> {
                if (pestaniaPrincipal == null) {
                    return;
                }
                pestaniaPrincipal.seleccionarPestaniaAgente();
                panelAgente.inyectarComando(inputFinal, 0);
                enviado.set(true);
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (InvocationTargetException e) {
            return false;
        }
        return enviado.get();
    }

    private String aplicarTokensPromptAgente(String prompt, String request, String response, String idioma) {
        return aplicarTokensPromptAgente(prompt, request, response, idioma, null, null, null);
    }

    private String aplicarTokensPromptAgente(String prompt, String request, String response, String idioma,
            String titulo, String resumen, String url) {
        String resultado = prompt != null ? prompt : "";
        resultado = resultado.replace(TOKEN_REQUEST, request != null ? request : "");
        resultado = resultado.replace(TOKEN_RESPONSE, response != null ? response : "");
        resultado = resultado.replace("{OUTPUT_LANGUAGE}",
                Normalizador.noEsVacio(idioma) ? idioma : "es");

        if (titulo != null)
            resultado = resultado.replace(TOKEN_TITLE, titulo);
        if (resumen != null) {
            resultado = resultado.replace(TOKEN_SUMMARY, resumen);
            resultado = resultado.replace(TOKEN_DESCRIPTION, resumen);
        }
        if (url != null)
            resultado = resultado.replace(TOKEN_URL, url);

        return resultado;
    }

    private String normalizarPromptAgente(String prompt) {
        return prompt != null ? prompt : "";
    }

    private String obtenerPromptAgenteDisponible() {
        if (config == null) {
            registrarError("No se puede usar el Agente: configuracion no inicializada");
            return null;
        }
        if (!config.agenteHabilitado()) {
            registrar(I18nLogs.Agente.ERROR_DESHABILITADO());
            return null;
        }
        return normalizarPromptAgente(config.obtenerAgentePrompt());
    }

    private String serializarSolicitudSiNecesario(String prompt, HttpRequestResponse evidencia) {
        return serializarSolicitudSiNecesario(prompt, evidencia, null);
    }

    private String serializarSolicitudSiNecesario(String prompt, HttpRequestResponse evidencia, String urlFallback) {
        if (!contieneToken(prompt, TOKEN_REQUEST) || evidencia == null || evidencia.request() == null) {
            return serializarSolicitudFallbackDesdeUrl(prompt, urlFallback);
        }
        return evidencia.request().toString();
    }

    private String serializarSolicitudFallbackDesdeUrl(String prompt, String urlFallback) {
        if (!contieneToken(prompt, TOKEN_REQUEST) || !tieneContenido(urlFallback)) {
            return "";
        }
        try {
            return construirSolicitudGetDesdeUrl(urlFallback);
        } catch (Exception ignored) {
            return "";
        }
    }

    private String construirSolicitudGetDesdeUrl(String url) {
        if (!tieneContenido(url)) {
            return "";
        }
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (Exception ignored) {
            return "";
        }
        String host = uri.getHost();
        if (!tieneContenido(host)) {
            return "";
        }
        String path = uri.getRawPath();
        if (!tieneContenido(path)) {
            path = "/";
        }
        String query = uri.getRawQuery();
        String objetivo = tieneContenido(query) ? path + "?" + query : path;
        String hostHeader = host;
        int port = uri.getPort();
        String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase() : "";
        boolean puertoPorDefecto = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        if (port > 0 && !puertoPorDefecto) {
            hostHeader = host + ":" + port;
        }
        return "GET " + objetivo + " HTTP/1.1\r\n"
                + "Host: " + hostHeader + "\r\n"
                + "User-Agent: BurpIA/" + VersionBurpIA.obtenerVersionActual() + "\r\n"
                + "Accept: */*\r\n"
                + "Connection: close\r\n\r\n";
    }

    private String serializarRespuestaSiNecesario(String prompt, HttpRequestResponse evidencia) {
        if (!contieneToken(prompt, TOKEN_RESPONSE) || evidencia == null || evidencia.response() == null) {
            return "";
        }
        return evidencia.response().toString();
    }

    private String serializarSolicitudesFlujoSiNecesario(String prompt, List<HttpRequestResponse> evidencias) {
        if (!contieneToken(prompt, TOKEN_REQUEST) || Normalizador.esVacia(evidencias)) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        int indice = 1;
        for (HttpRequestResponse evidencia : evidencias) {
            if (evidencia == null || evidencia.request() == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append("=== REQUEST ").append(indice).append(" ===\n");
            builder.append(evidencia.request());
            indice++;
        }
        return builder.toString();
    }

    private String serializarRespuestasFlujoSiNecesario(String prompt, List<HttpRequestResponse> evidencias) {
        if (!contieneToken(prompt, TOKEN_RESPONSE) || Normalizador.esVacia(evidencias)) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        int indice = 1;
        for (HttpRequestResponse evidencia : evidencias) {
            if (evidencia == null || evidencia.request() == null) {
                continue;
            }
            if (evidencia.response() != null) {
                if (builder.length() > 0) {
                    builder.append("\n\n");
                }
                builder.append("=== RESPONSE ").append(indice).append(" ===\n");
                builder.append(evidencia.response());
            }
            indice++;
        }
        return builder.toString();
    }

    private List<HttpRequestResponse> filtrarSolicitudesConRequest(List<HttpRequestResponse> solicitudesRespuesta) {
        List<HttpRequestResponse> solicitudesValidas = new ArrayList<>();
        if (Normalizador.esVacia(solicitudesRespuesta)) {
            return solicitudesValidas;
        }
        for (HttpRequestResponse solicitudRespuesta : solicitudesRespuesta) {
            if (solicitudRespuesta != null && solicitudRespuesta.request() != null) {
                solicitudesValidas.add(solicitudRespuesta);
            }
        }
        return solicitudesValidas;
    }

    private boolean enviarPayloadAgente(String inputFinal) {
        PanelAgente panelAgente = obtenerPanelAgenteDisponible();
        if (panelAgente == null) {
            return false;
        }
        return enfocarEInyectarEnAgente(panelAgente, inputFinal);
    }

    private void agregarLineaSiHayContenido(StringBuilder builder, boolean habilitado, String etiqueta, String valor) {
        if (!habilitado || !tieneContenido(valor)) {
            return;
        }
        builder.append(etiqueta).append(": ").append(valor).append("\n");
    }

    private boolean contieneToken(String prompt, String token) {
        return prompt != null && token != null && prompt.contains(token);
    }

    private boolean contieneAlgunToken(String prompt, String... tokens) {
        if (tokens == null || tokens.length == 0) {
            return false;
        }
        for (String token : tokens) {
            if (contieneToken(prompt, token)) {
                return true;
            }
        }
        return false;
    }

    private boolean tieneContenido(String texto) {
        return Normalizador.noEsVacio(texto);
    }

    private String valorSeguro(String texto) {
        return texto != null ? texto : "";
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

        ejecutarEnEdt(() -> {
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
                        if (modeloTablaTareas != null) {
                            modeloTablaTareas.establecerLimiteFilas(configActual.obtenerMaximoTareasTabla());
                        }
                        if (manejadorHttp != null) {
                            manejadorHttp.actualizarConfiguracion(configActual);
                        }
                        I18nUI.establecerIdioma(configActual.obtenerIdiomaUi());
                        if (pestaniaActual != null) {
                            pestaniaActual.aplicarIdioma();
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
                                ", maximoHallazgos=" + configActual.obtenerMaximoHallazgosTabla() +
                                ", maximoTareas=" + configActual.obtenerMaximoTareasTabla());

                        pestaniaActual.actualizarVisibilidadAgentes();
                    });
            dialogo.setVisible(true);
        });
    }

    private void crearYRegistrarPestaniaPrincipal() {
        Runnable crearUi = () -> {
            pestaniaPrincipal = new PestaniaPrincipal(api, estadisticas, gestorTareas, gestorConsola, modeloTablaTareas,
                    modeloTablaHallazgos, esProfessional, config, gestorLogging);
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
            // CONFIABILIDAD: Log compacto con información de versión
            registrar(I18nLogs.Inicializacion.UI_REGISTRADA_EN(
                    esProfessional ? "Burp Suite Professional" : "Burp Suite",
                    api.burpSuite().version().toString()));
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
        if (gestorLogging != null) {
            gestorLogging.info(mensaje);
        } else if (stdout != null) {
            stdout.println(mensaje);
        }
    }

    private void registrarResumenInicio() {
        // CONFIABILIDAD: Logs optimizados y diferenciados por modo (normal vs detallado)
        boolean detallado = config.esDetallado();
        String proveedor = config.obtenerProveedorAI();
        String modelo = config.obtenerModelo();
        int timeoutEfectivo = config.obtenerTiempoEsperaParaModelo(proveedor, modelo);

        // Header siempre presente
        gestorLogging.separador();
        registrar(" BurpIA v" + VersionBurpIA.obtenerVersionActual());
        gestorLogging.separador();

        // Sección [Configuration] - MODO NORMAL
        registrar(I18nLogs.Inicializacion.SECCION_CONFIGURACION());
        registrar("  " + I18nLogs.Inicializacion.PROVEEDOR_PRINCIPAL()
                + proveedor + " (" + modelo + "), " + I18nLogs.Inicializacion.TIMEOUT_SEGUNDOS(String.valueOf(timeoutEfectivo)));

        // Multi-proveedor (condicional)
        if (config.esMultiProveedorHabilitado()) {
            List<String> proveedoresMulti = config.obtenerProveedoresMultiConsulta();
            if (Normalizador.noEsVacia(proveedoresMulti)) {
                registrar("  " + I18nLogs.Inicializacion.MULTI_PROVEEDOR()
                        + String.join(", ", proveedoresMulti));
            }
        }

        // Concurrencia y rendimiento
        registrar("  " + I18nLogs.Inicializacion.CONCURRENCIA()
                + I18nLogs.Inicializacion.TAREAS(String.valueOf(config.obtenerMaximoConcurrente()))
                + ", " + I18nLogs.Inicializacion.RETRASO_SEGUNDOS(String.valueOf(config.obtenerRetrasoSegundos()))
                + ", " + I18nLogs.Inicializacion.MAX_HALLAZGOS(String.valueOf(config.obtenerMaximoHallazgosTabla())));

        // Flags binarios
        registrar("  " + I18nLogs.Inicializacion.MODO_DETALLADO()
                + (detallado ? I18nLogs.Inicializacion.SI() : I18nLogs.Inicializacion.NO())
                + " | " + I18nLogs.Inicializacion.AGENTE()
                + (config.agenteHabilitado() ? I18nLogs.Inicializacion.SI() : I18nLogs.Inicializacion.NO()));
        gestorLogging.separador();

        // Sección [Environment] - SOLO MODO DETALLADO
        if (detallado) {
            registrar(I18nLogs.Inicializacion.SECCION_ENTORNO());
            registrar("  " + I18nLogs.Inicializacion.ENTORNO_BURP_SUITE(
                    esProfessional ? "Professional" : "Community Edition",
                    obtenerVersionBurp(api)));

            // Detalles técnicos de IA
            registrar("  " + I18nLogs.Inicializacion.URL_API(config.obtenerUrlApi()));
            registrar("  " + I18nLogs.Inicializacion.API_KEY(
                    com.burpia.util.Normalizador.sanitizarApiKey(config.obtenerClaveApi())));
            registrar("  " + I18nLogs.Inicializacion.TIMEOUT_GLOBAL(
                    String.valueOf(config.obtenerTiempoEsperaAI()),
                    String.valueOf(timeoutEfectivo),
                    String.valueOf(timeoutEfectivo)));
            registrar("  " + I18nLogs.Inicializacion.SSL_VERIFICACION(!config.ignorarErroresSSL()));
            registrar("  " + I18nLogs.Inicializacion.MODO_SOLO_PROXY(config.soloProxy()));
            registrar("  " + I18nLogs.Inicializacion.IDIOMA(
                    "es".equals(config.obtenerIdiomaUi()) ? "Español" : "English",
                    config.obtenerIdiomaUi()));

            // Multi-provider details
            if (config.esMultiProveedorHabilitado()) {
                registrar(I18nLogs.Inicializacion.SECCION_MULTI_PROVEEDOR());
                registrar("  " + I18nLogs.Inicializacion.MULTI_HABILITADO(true));
                List<String> proveedoresMulti = config.obtenerProveedoresMultiConsulta();
                if (Normalizador.noEsVacia(proveedoresMulti)) {
                    StringBuilder sb = new StringBuilder(I18nLogs.Inicializacion.PROVEEDORES());
                    for (String prov : proveedoresMulti) {
                        String provModelo = config.obtenerModeloParaProveedor(prov);
                        int provTimeout = config.obtenerTiempoEsperaParaModelo(prov, provModelo);
                        if (sb.length() > I18nLogs.Inicializacion.PROVEEDORES().length()) {
                            sb.append(", ");
                        }
                        sb.append(prov).append(" (").append(provModelo).append(", timeout ")
                          .append(provTimeout).append("s)");
                    }
                    registrar("  " + sb);
                    registrar("  " + I18nLogs.Inicializacion.ORDEN_EJECUCION()
                            + proveedor + " → " + String.join(" → ", proveedoresMulti));
                }
            }

            // Performance details
            registrar(I18nLogs.Inicializacion.SECCION_RENDIMIENTO());
            registrar("  " + I18nLogs.Inicializacion.CONCURRENCIA_MAX(String.valueOf(config.obtenerMaximoConcurrente())));
            registrar("  " + I18nLogs.Inicializacion.MAX_TAREAS(String.valueOf(config.obtenerMaximoTareasTabla())));
            registrar("  " + I18nLogs.Inicializacion.RETENCION(String.valueOf(2000)));

            // Agent details
            registrar(I18nLogs.Inicializacion.SECCION_AGENTE());
            registrar("  " + I18nLogs.Inicializacion.AGENTE_HABILITADO(config.agenteHabilitado()));
            if (config.agenteHabilitado()) {
                registrar("  " + I18nLogs.Inicializacion.AGENTE_TIPO(config.obtenerTipoAgente()));
                String rutaBinario = config.obtenerRutaBinarioAgente(config.obtenerTipoAgente());
                if (Normalizador.noEsVacio(rutaBinario)) {
                    registrar("  " + I18nLogs.Inicializacion.AGENTE_BINARIO(rutaBinario));
                }
            }
            gestorLogging.separador();
        }
    }

    private void registrarError(String mensaje) {
        if (gestorLogging != null) {
            gestorLogging.error(mensaje);
        } else if (stderr != null) {
            stderr.println(mensaje);
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

        pestaniaPrincipal.establecerGuardadoAutomaticoIssuesActivo(autoGuardadoIssuesPermitido);
        pestaniaPrincipal.establecerAutoScrollConsolaActivo(config.autoScrollConsolaHabilitado());

        pestaniaPrincipal.establecerManejadorAutoGuardadoIssues(activo -> {
            if (!esProfessional) {
                return;
            }
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
        pestaniaPrincipal.establecerManejadorAlertasEnviarA(() -> guardarConfiguracionSilenciosa("alertas-enviar-a"));

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

        panelAgente.asegurarConsolaIniciada();
        registrar("Agente inicializado - secuencia automática de arranque activa");
    }

    private void guardarConfiguracionSilenciosa(String origen) {
        if (gestorConfig == null || config == null) {
            return;
        }
        StringBuilder mensajeError = new StringBuilder();
        if (!gestorConfig.guardarConfiguracion(config, mensajeError)) {
            String detalle = mensajeError.toString().trim();
            if (Normalizador.esVacio(detalle)) {
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

        burp.api.montoya.scanner.audit.issues.AuditIssueSeverity severity = convertirSeveridad(
                hallazgo.obtenerSeveridad());
        burp.api.montoya.scanner.audit.issues.AuditIssueConfidence confidence = convertirConfianza(
                hallazgo.obtenerConfianza());

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
                evidencias);
    }

    static HttpRequestResponse resolverEvidenciaIssue(Hallazgo hallazgo,
            HttpRequestResponse solicitudRespuestaEvidencia) {
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
        burp.api.montoya.scanner.audit.issues.AuditIssue issue = crearAuditIssueDesdeHallazgo(hallazgo,
                solicitudRespuestaEvidencia);
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

    private String obtenerVersionBurp(MontoyaApi api) {
        try {
            if (api != null && api.burpSuite() != null && api.burpSuite().version() != null) {
                String version = api.burpSuite().version().toString();
                if (Normalizador.noEsVacio(version)) {
                    return version;
                }
            }
        } catch (Exception ignored) {
            // La version de Burp no es critica; si falla, devolvemos null
        }
        return null;
    }

    public boolean esBurpProfessional() {
        return esProfessional;
    }
}
