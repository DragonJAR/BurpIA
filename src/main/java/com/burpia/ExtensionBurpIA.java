package com.burpia;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.BurpSuiteEdition;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import com.burpia.analyzer.AnalizadorHTTP;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.ConfiguracionAPIRef;
import com.burpia.config.GestorConfiguracion;
import com.burpia.flow.FlowAnalysisConstraints;
import com.burpia.i18n.I18nLogs;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Estadisticas;
import com.burpia.model.Hallazgo;
import com.burpia.processor.HttpRequestProcessor;
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
import com.burpia.util.ProcesadorPromptHTTP;
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
import java.util.concurrent.atomic.AtomicReference;
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
    private ConfiguracionAPIRef configRef;
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
    private HttpRequestProcessor httpRequestProcessor;
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
        configRef = new ConfiguracionAPIRef(config);
        EstilosUI.actualizarFuentes(config);
        I18nUI.establecerIdioma(config.obtenerIdiomaUi());
        gestorConsola = new GestorConsolaGUI();
        gestorConsola.capturarStreamsOriginales(stdout, stderr);

        gestorLogging = GestorLoggingUnificado.crear(gestorConsola, stdout, stderr, api, null);
        httpRequestProcessor = new HttpRequestProcessor(api, configRef, gestorLogging);

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
                api, configRef, pestaniaPrincipal, stdout, stderr, limitador,
                estadisticas, gestorTareas, gestorConsola, modeloTablaHallazgos, httpRequestProcessor);
        if (gestorTareas != null) {
            gestorTareas.establecerManejadorCancelacion(manejadorHttp::cancelarEjecucionActiva);
            gestorTareas.establecerManejadorPausa(manejadorHttp::cancelarEjecucionActiva);
            gestorTareas.establecerManejadorReanudar(manejadorHttp::reencolarTarea);
        }
        if (pestaniaPrincipal != null) {
            pestaniaPrincipal.establecerManejadorReintentoTareas(manejadorHttp::reencolarTarea);
            pestaniaPrincipal.establecerManejadorGuardarIssue(manejadorHttp::guardarHallazgoComoIssue);
            pestaniaPrincipal.establecerManejadorToggleCaptura(this::alternarCapturaDesdeUI);
            pestaniaPrincipal.establecerEstadoCaptura(manejadorHttp.estaCapturaActiva());

            pestaniaPrincipal.establecerManejadorCambioFiltros(
                () -> guardarConfiguracionSilenciosa("cambio-filtros-hallazgos")
            );
        }
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
            HttpRequestResponse solicitudRespuestaOriginal, FabricaMenuContextual.ContextoInvocacion contextoInvocacion) {
        if (forzarAnalisis && manejadorHttp != null) {
            manejadorHttp.analizarSolicitudForzada(solicitud, solicitudRespuestaOriginal, contextoInvocacion);
        }
    }

    private void analizarFlujoManual(List<HttpRequestResponse> solicitudesRespuestaOriginales,
            FabricaMenuContextual.ContextoInvocacion contextoInvocacion) {
        if (manejadorHttp != null) {
            manejadorHttp.analizarFlujoForzado(solicitudesRespuestaOriginales, contextoInvocacion);
        }
    }

    private void registrarMenuContextual() {
        if (fabricaMenuContextual == null) {
            fabricaMenuContextual = new FabricaMenuContextual(
                    api,
                    this::analizarSolicitudManual,
                    this::analizarFlujoManual,
                    configRef.obtener(),
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

    private PanelAgente.ResultadoInyeccion enviarAAgente(HttpRequestResponse solicitudRespuesta,
            FabricaMenuContextual.ContextoInvocacion contextoInvocacion) {
        if (solicitudRespuesta == null) {
            registrarError(I18nLogs.Agente.ERROR_SOLICITUD_NULA());
            return PanelAgente.ResultadoInyeccion.DESCARTADO;
        }
        try {
            registrarInicioContextualDetallado(
                I18nLogs.ContextoMenu.ACCION_ENVIAR_SOLICITUD_AGENTE(),
                contextoInvocacion
            );
            registrarSolicitudContextualDetallada(solicitudRespuesta);
            registrarBypassContextualDetallado(I18nLogs.ContextoMenu.BYPASS_ENVIO_AGENTE());

            String prompt = obtenerPromptAgenteDisponible();
            registrarPromptAgenteDetallado(prompt);
            if (prompt == null) {
                return PanelAgente.ResultadoInyeccion.DESCARTADO;
            }
            boolean usaTokensHttp = contieneAlgunToken(prompt, TOKEN_REQUEST, TOKEN_RESPONSE);
            rastrearContextual(I18nLogs.ContextoMenu.PROMPT_USA_TOKENS_HTTP(usaTokensHttp));
            registrarOmisionesResponseDetalladas(prompt, solicitudRespuesta);
            String request = serializarSolicitudSiNecesario(prompt, solicitudRespuesta);
            String response = serializarRespuestaSiNecesario(prompt, solicitudRespuesta);
            String inputFinal = aplicarTokensPromptAgente(prompt, request, response, config.obtenerIdiomaUi());
            registrarSerializacionAgenteDetallada(
                Normalizador.noEsVacio(request) ? 1 : 0,
                Normalizador.noEsVacio(response) ? 1 : 0,
                contieneToken(prompt, TOKEN_RESPONSE) && !tieneResponseDisponible(solicitudRespuesta) ? 1 : 0
            );
            rastrearContextual(I18nLogs.ContextoMenu.LONGITUD_PAYLOAD_AGENTE(inputFinal.length()));
            PanelAgente.ResultadoInyeccion resultado = enviarPayloadAgente(inputFinal);
            rastrearContextual(I18nLogs.ContextoMenu.RESULTADO_INYECCION_AGENTE(resultado));
            return resultado;
        } catch (Exception e) {
            registrarError(I18nLogs.Agente.ERROR_ENVIO(e.getMessage()));
            return PanelAgente.ResultadoInyeccion.DESCARTADO;
        }
    }

    private PanelAgente.ResultadoInyeccion enviarFlujoAAgente(List<HttpRequestResponse> solicitudesRespuesta,
            FabricaMenuContextual.ContextoInvocacion contextoInvocacion) {
        try {
            registrarInicioContextualDetallado(
                I18nLogs.ContextoMenu.ACCION_ENVIAR_FLUJO_AGENTE(),
                contextoInvocacion
            );
            registrarResumenSeleccionContextualDetallado(solicitudesRespuesta);
            registrarSolicitudesContextualesDetalladas(FlowAnalysisConstraints.filtrarSolicitudesValidas(solicitudesRespuesta));
            registrarBypassContextualDetallado(I18nLogs.ContextoMenu.BYPASS_ENVIO_AGENTE());

            String prompt = obtenerPromptAgenteDisponible();
            registrarPromptAgenteDetallado(prompt);
            if (prompt == null) {
                return PanelAgente.ResultadoInyeccion.DESCARTADO;
            }

            List<HttpRequestResponse> solicitudesValidas = FlowAnalysisConstraints.filtrarSolicitudesValidas(solicitudesRespuesta);
            if (!FlowAnalysisConstraints.tieneMinimoValido(solicitudesRespuesta)) {
                registrarError(I18nUI.Contexto.MSG_FLUJO_REQUIERE_MULTIPLES_VALIDAS());
                return PanelAgente.ResultadoInyeccion.DESCARTADO;
            }
            if (FlowAnalysisConstraints.excedeMaximoValido(solicitudesRespuesta)) {
                registrarError(I18nUI.Contexto.MSG_FLUJO_MAXIMO_PETICIONES(FlowAnalysisConstraints.MAXIMO_PETICIONES_FLUJO));
                return PanelAgente.ResultadoInyeccion.DESCARTADO;
            }

            boolean usaTokensHttp = ProcesadorPromptHTTP.contieneMarcadoresHttp(prompt);
            rastrearContextual(I18nLogs.ContextoMenu.PROMPT_USA_TOKENS_HTTP(usaTokensHttp));
            registrarOmisionesResponseFlujoDetalladas(prompt, solicitudesValidas);
            String inputFinal = construirPromptFlujoAgente(prompt, solicitudesValidas);
            registrarSerializacionAgenteDetallada(
                contarRequestsSerializadasFlujo(prompt, solicitudesValidas),
                contarResponsesSerializadasFlujo(prompt, solicitudesValidas),
                contarResponsesOmitidasFlujo(prompt, solicitudesValidas)
            );
            rastrearContextual(I18nLogs.ContextoMenu.LONGITUD_PAYLOAD_AGENTE(inputFinal.length()));
            PanelAgente.ResultadoInyeccion resultado = enviarPayloadAgente(inputFinal);
            rastrearContextual(I18nLogs.ContextoMenu.RESULTADO_INYECCION_AGENTE(resultado));
            return resultado;
        } catch (Exception e) {
            registrarError(I18nLogs.Agente.ERROR_FLUJO(e.getMessage()));
            return PanelAgente.ResultadoInyeccion.DESCARTADO;
        }
    }

    // PMD no rastrea que this::enviarAAgente (línea 215) resuelve a esta
    // sobrecarga de 1 arg vía PredicateAgenteSolicitud; es un falso positivo.
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private PanelAgente.ResultadoInyeccion enviarAAgente(HttpRequestResponse solicitudRespuesta) {
        return enviarAAgente(solicitudRespuesta, null);
    }

    // PMD no rastrea que this::enviarFlujoAAgente (línea 216) resuelve a esta
    // sobrecarga de 1 arg vía PredicateAgenteFlujo; es un falso positivo.
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private PanelAgente.ResultadoInyeccion enviarFlujoAAgente(List<HttpRequestResponse> solicitudesRespuesta) {
        return enviarFlujoAAgente(solicitudesRespuesta, null);
    }

    private boolean hayAgenteOperativoDisponible() {
        ConfiguracionAPI cfg = configRef.obtener();
        return cfg != null && cfg.hayAlgunAgenteHabilitado();
    }

    private String obtenerTipoAgenteOperativoActual() {
        ConfiguracionAPI cfg = configRef.obtener();
        if (cfg == null) {
            return null;
        }
        String tipoAgenteOperativo = cfg.obtenerTipoAgenteOperativo();
        return Normalizador.noEsVacio(tipoAgenteOperativo) ? tipoAgenteOperativo : cfg.obtenerTipoAgente();
    }

    private PanelAgente.ResultadoInyeccion enviarHallazgoAAgente(Hallazgo hallazgo) {
        if (configRef == null || configRef.obtener() == null) {
            registrarError(I18nLogs.Agente.ERROR_CONFIGURACION_NULA());
            return PanelAgente.ResultadoInyeccion.DESCARTADO;
        }
        if (!hayAgenteOperativoDisponible()) {
            registrar(I18nLogs.Agente.ERROR_DESHABILITADO());
            return PanelAgente.ResultadoInyeccion.DESCARTADO;
        }
        if (hallazgo == null) {
            registrarError(I18nLogs.Agente.ERROR_HALLAZGO_NULO());
            return PanelAgente.ResultadoInyeccion.DESCARTADO;
        }
        try {
            String prompt = obtenerPromptAgenteDisponible();
            if (prompt == null) {
                return PanelAgente.ResultadoInyeccion.DESCARTADO;
            }

            HttpRequestResponse evidencia = resolverEvidenciaIssue(hallazgo, null);
            String request = serializarSolicitudSiNecesario(prompt, evidencia, hallazgo.obtenerUrl());
            String response = serializarRespuestaSiNecesario(prompt, evidencia);
            String tituloValor = Normalizador.valorSeguro(hallazgo.obtenerTitulo());
            String resumenValor = Normalizador.valorSeguro(hallazgo.obtenerHallazgo());
            String urlContextValor = Normalizador.valorSeguro(hallazgo.obtenerUrl());
            boolean usaTitulo = contieneToken(prompt, TOKEN_TITLE);
            boolean usaResumen = contieneAlgunToken(prompt, TOKEN_SUMMARY, TOKEN_DESCRIPTION);
            boolean usaUrl = contieneToken(prompt, TOKEN_URL);
        String titulo = usaTitulo && Normalizador.noEsVacio(tituloValor) ? tituloValor : "";
        String resumen = usaResumen && Normalizador.noEsVacio(resumenValor) ? resumenValor : "";
        String urlContext = usaUrl && Normalizador.noEsVacio(urlContextValor) ? urlContextValor : "";
            String lang = configRef.obtener().obtenerIdiomaUi();

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
            registrarError(I18nLogs.Agente.ERROR_HALLAZGO_ENVIO(e.getMessage()));
            return PanelAgente.ResultadoInyeccion.DESCARTADO;
        }
    }

    private PanelAgente.ResultadoInyeccion enfocarEInyectarEnAgente(PanelAgente panelAgente, String inputFinal) {
        if (panelAgente == null || pestaniaPrincipal == null) {
            return PanelAgente.ResultadoInyeccion.DESCARTADO;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            pestaniaPrincipal.seleccionarPestaniaAgente();
            return panelAgente.inyectarComando(inputFinal, 0);
        }
        AtomicReference<PanelAgente.ResultadoInyeccion> resultado = new AtomicReference<>(PanelAgente.ResultadoInyeccion.DESCARTADO);
        try {
            SwingUtilities.invokeAndWait(() -> {
                if (pestaniaPrincipal == null) {
                    return;
                }
                pestaniaPrincipal.seleccionarPestaniaAgente();
                resultado.set(panelAgente.inyectarComando(inputFinal, 0));
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return PanelAgente.ResultadoInyeccion.DESCARTADO;
        } catch (InvocationTargetException e) {
            return PanelAgente.ResultadoInyeccion.DESCARTADO;
        }
        return resultado.get();
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
        ConfiguracionAPI cfg = configRef.obtener();
        if (cfg == null) {
            registrarError(I18nLogs.Agente.ERROR_CONFIGURACION_NULA());
            return null;
        }
        if (!hayAgenteOperativoDisponible()) {
            registrar(I18nLogs.Agente.ERROR_DESHABILITADO());
            return null;
        }
        return normalizarPromptAgente(cfg.obtenerAgentePrompt());
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
        if (!contieneToken(prompt, TOKEN_REQUEST) || !Normalizador.noEsVacio(urlFallback)) {
            return "";
        }
        try {
            return construirSolicitudGetDesdeUrl(urlFallback);
        } catch (Exception ignored) {
            return "";
        }
    }

    private String construirSolicitudGetDesdeUrl(String url) {
        if (!Normalizador.noEsVacio(url)) {
            return "";
        }
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (Exception ignored) {
            return "";
        }
        String host = uri.getHost();
        if (!Normalizador.noEsVacio(host)) {
            return "";
        }
        String path = uri.getRawPath();
        if (!Normalizador.noEsVacio(path)) {
            path = "/";
        }
        String query = uri.getRawQuery();
        String objetivo = Normalizador.noEsVacio(query) ? path + "?" + query : path;
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
        if (!contieneToken(prompt, TOKEN_RESPONSE) || !tieneResponseDisponible(evidencia)) {
            return "";
        }
        return evidencia.response().toString();
    }

    private String construirPromptFlujoAgente(String prompt, List<HttpRequestResponse> evidencias) {
        if (!ProcesadorPromptHTTP.contieneMarcadoresHttp(prompt) || Normalizador.esVacia(evidencias)) {
            return aplicarTokensPromptAgente(prompt, "", "", configRef.obtener().obtenerIdiomaUi());
        }

        List<String> requests = ProcesadorPromptHTTP.contieneMarcadoresRequest(prompt)
            ? serializarSolicitudesFlujo(evidencias)
            : List.of();
        List<String> responses = ProcesadorPromptHTTP.contieneMarcadoresResponse(prompt)
            ? serializarRespuestasFlujo(evidencias)
            : List.of();

        String promptConHttp = ProcesadorPromptHTTP.reemplazarContenidoFlujo(prompt, requests, responses);
        return aplicarTokensPromptAgente(promptConHttp, "", "", configRef.obtener().obtenerIdiomaUi());
    }

    private List<String> serializarSolicitudesFlujo(List<HttpRequestResponse> evidencias) {
        List<String> serializadas = new ArrayList<>();
        for (HttpRequestResponse evidencia : evidencias) {
            serializadas.add(evidencia != null && evidencia.request() != null
                ? evidencia.request().toString()
                : "");
        }
        return serializadas;
    }

    private List<String> serializarRespuestasFlujo(List<HttpRequestResponse> evidencias) {
        List<String> serializadas = new ArrayList<>();
        for (HttpRequestResponse evidencia : evidencias) {
            serializadas.add(tieneResponseDisponible(evidencia)
                ? evidencia.response().toString()
                : "");
        }
        return serializadas;
    }

    private PanelAgente.ResultadoInyeccion enviarPayloadAgente(String inputFinal) {
        PanelAgente panelAgente = obtenerPanelAgenteDisponible();
        if (panelAgente == null) {
            return PanelAgente.ResultadoInyeccion.DESCARTADO;
        }
        return enfocarEInyectarEnAgente(panelAgente, inputFinal);
    }

    private void agregarLineaSiHayContenido(StringBuilder builder, boolean habilitado, String etiqueta, String valor) {
        if (!habilitado || !Normalizador.noEsVacio(valor)) {
            return;
        }
        builder.append(etiqueta).append(": ").append(valor).append("\n");
    }

    private boolean contieneToken(String prompt, String token) {
        return prompt != null && token != null && prompt.contains(token);
    }

    private boolean tieneResponseDisponible(HttpRequestResponse evidencia) {
        return obtenerProcesadorSolicitudes().tieneResponseDisponible(evidencia);
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

    private PanelAgente obtenerPanelAgenteDisponible() {
        if (pestaniaPrincipal == null) {
            registrarError(I18nLogs.Agente.ERROR_PESTANA_NO_DISPONIBLE());
            return null;
        }
        PanelAgente panelAgente = pestaniaPrincipal.obtenerPanelAgente();
        if (panelAgente == null) {
            registrarError(I18nLogs.Agente.ERROR_PANEL_NO_DISPONIBLE());
            return null;
        }
        return panelAgente;
    }

    private void abrirConfiguracion() {
        registrar(I18nLogs.Extension.ABRIENDO_DIALOGO());

        if (pestaniaPrincipal == null || config == null || gestorConfig == null) {
            registrarError(I18nLogs.Extension.ERROR_ABRIR_DIALOGO());
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
                            gestorConsola.registrarInfo(I18nLogs.Configuracion.GUARDADA_OK());
                        }
                        if (api != null) {
                            api.logging().logToOutput(I18nUI.General.CONFIGURACION_GUARDADA());
                        }

                        registrar(I18nLogs.Extension.CONFIGURACION_ACTUALIZADA(
                                configActual.esDetallado(), configActual.obtenerMaximoConcurrente(),
                                configActual.obtenerRetrasoSegundos(), configActual.obtenerMaximoHallazgosTabla(),
                                configActual.obtenerMaximoTareasTabla()));

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
                registrar(I18nLogs.Agente.LOG_AGENTE_CAMBIADO(obtenerTipoAgenteOperativoActual()));
            });

            api.userInterface().registerSuiteTab(I18nUI.Configuracion.TITULO_APP(), pestaniaPrincipal.obtenerPanel());
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
            throw new RuntimeException(I18nUI.General.ERROR_INICIALIZACION_UI_INTERRUPIDA(), e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(I18nUI.General.ERROR_INICIALIZACION_UI_FALLIDA(), e);
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
        registrar(I18nLogs.trTecnico("BurpIA v") + VersionBurpIA.obtenerVersionActual());
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
                + (hayAgenteOperativoDisponible() ? I18nLogs.Inicializacion.SI() : I18nLogs.Inicializacion.NO()));
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
                    // Construir el orden de ejecución real sin duplicar el
                    // proveedor principal si ya está en la lista multi.
                    List<String> ordenReal = new java.util.ArrayList<>();
                    ordenReal.add(proveedor);
                    for (String p : proveedoresMulti) {
                        if (!ordenReal.contains(p)) {
                            ordenReal.add(p);
                        }
                    }
                    registrar("  " + I18nLogs.Inicializacion.ORDEN_EJECUCION()
                            + String.join(" → ", ordenReal));
                }
            }

            // Performance details
            registrar(I18nLogs.Inicializacion.SECCION_RENDIMIENTO());
            registrar("  " + I18nLogs.Inicializacion.CONCURRENCIA_MAX(String.valueOf(config.obtenerMaximoConcurrente())));
            registrar("  " + I18nLogs.Inicializacion.MAX_TAREAS(String.valueOf(config.obtenerMaximoTareasTabla())));
            registrar("  " + I18nLogs.Inicializacion.RETENCION(String.valueOf(2000)));

            // Agent details
            registrar(I18nLogs.Inicializacion.SECCION_AGENTE());
            registrar("  " + I18nLogs.Inicializacion.AGENTE_HABILITADO(hayAgenteOperativoDisponible()));
            if (hayAgenteOperativoDisponible()) {
                String tipoAgenteOperativo = obtenerTipoAgenteOperativoActual();
                registrar("  " + I18nLogs.Inicializacion.AGENTE_TIPO(tipoAgenteOperativo));
                String rutaBinario = config.obtenerRutaBinarioAgente(tipoAgenteOperativo);
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

    private void rastrearContextual(String mensaje) {
        if (config == null || !config.esDetallado() || Normalizador.esVacio(mensaje)) {
            return;
        }
        if (gestorLogging != null) {
            gestorLogging.verbose(mensaje);
        } else if (stdout != null) {
            stdout.println("[BurpIA] [VERBOSE] " + mensaje);
            stdout.flush();
        }
    }

    private void registrarInicioContextualDetallado(String accion,
            FabricaMenuContextual.ContextoInvocacion contextoInvocacion) {
        if (!debeRegistrarContextoDetallado(contextoInvocacion)) {
            return;
        }
        rastrearContextual(I18nLogs.ContextoMenu.ACCION_INICIADA(
            accion,
            contextoInvocacion.obtenerTipoInvocacion(),
            contextoInvocacion.obtenerTipoHerramienta(),
            contextoInvocacion.obtenerCantidadSeleccionada()
        ));
    }

    private void registrarResumenSeleccionContextualDetallado(List<HttpRequestResponse> solicitudes) {
        if (config == null || !config.esDetallado()) {
            return;
        }
        HttpRequestProcessor procesador = obtenerProcesadorSolicitudes();
        int total = solicitudes != null ? solicitudes.size() : 0;
        int sinRequest = procesador.contarSolicitudesSinRequest(solicitudes);
        int validas = Math.max(0, total - sinRequest);
        int sinResponse = procesador.contarSolicitudesSinResponse(solicitudes);
        rastrearContextual(I18nLogs.ContextoMenu.RESUMEN_SELECCION(total, validas, sinRequest, sinResponse));
    }

    private void registrarSolicitudesContextualesDetalladas(List<HttpRequestResponse> solicitudes) {
        if (config == null || !config.esDetallado() || Normalizador.esVacia(solicitudes)) {
            return;
        }
        for (HttpRequestResponse solicitud : solicitudes) {
            registrarSolicitudContextualDetallada(solicitud);
        }
    }

    private void registrarSolicitudContextualDetallada(HttpRequestResponse solicitud) {
        if (config == null || !config.esDetallado() || solicitud == null) {
            return;
        }
        HttpRequestProcessor.ResumenSolicitudContextual resumen =
            obtenerProcesadorSolicitudes().inspeccionarSolicitudContextual(solicitud);
        if (!resumen.esValida()) {
            return;
        }
        for (String traza : obtenerProcesadorSolicitudes().construirTrazasDetalleContextual(resumen)) {
            rastrearContextual(traza);
        }
    }

    private void registrarBypassContextualDetallado(String mensaje) {
        if (debeRegistrarContextoDetallado(null)) {
            rastrearContextual(mensaje);
        }
    }

    private void registrarPromptAgenteDetallado(String prompt) {
        if (config == null || !config.esDetallado()) {
            return;
        }
        rastrearContextual(I18nLogs.ContextoMenu.PROMPT_AGENTE_DISPONIBLE(Normalizador.noEsVacio(prompt)));
        // Modo detallado: volcar el prompt completo del agente para que el
        // usuario pueda ver qué contexto se le está enviando al CLI agent.
        // Usa verboseTecnico (no rastrearContextual) para no corromper el
        // contenido del prompt con el diccionario i18n.
        if (Normalizador.noEsVacio(prompt) && gestorLogging != null) {
            gestorLogging.verboseTecnico("BurpIA",
                    I18nLogs.tr("=== PROMPT ENVIADO AL AGENTE ===") + "\n" + prompt
                            + "\n" + I18nLogs.tr("=== FIN DEL PROMPT DEL AGENTE ==="));
        }
    }

    private void registrarSerializacionAgenteDetallada(int requestsSerializadas, int responsesSerializadas,
            int responsesOmitidas) {
        if (config == null || !config.esDetallado()) {
            return;
        }
        rastrearContextual(I18nLogs.ContextoMenu.SERIALIZACION_AGENTE(
            requestsSerializadas,
            responsesSerializadas,
            responsesOmitidas
        ));
    }

    private int contarRequestsSerializadasFlujo(String prompt, List<HttpRequestResponse> solicitudesValidas) {
        if (!ProcesadorPromptHTTP.contieneMarcadoresRequest(prompt)) {
            return 0;
        }
        return solicitudesValidas != null ? solicitudesValidas.size() : 0;
    }

    private int contarResponsesSerializadasFlujo(String prompt, List<HttpRequestResponse> solicitudesValidas) {
        if (!ProcesadorPromptHTTP.contieneMarcadoresResponse(prompt) || Normalizador.esVacia(solicitudesValidas)) {
            return 0;
        }
        int total = 0;
        for (HttpRequestResponse solicitud : solicitudesValidas) {
            if (tieneResponseDisponible(solicitud)) {
                total++;
            }
        }
        return total;
    }

    private int contarResponsesOmitidasFlujo(String prompt, List<HttpRequestResponse> solicitudesValidas) {
        if (!ProcesadorPromptHTTP.contieneMarcadoresResponse(prompt) || Normalizador.esVacia(solicitudesValidas)) {
            return 0;
        }
        int total = 0;
        for (HttpRequestResponse solicitud : solicitudesValidas) {
            if (!tieneResponseDisponible(solicitud)) {
                total++;
            }
        }
        return total;
    }

    private boolean debeRegistrarContextoDetallado(FabricaMenuContextual.ContextoInvocacion contextoInvocacion) {
        return config != null && config.esDetallado();
    }

    private HttpRequestProcessor obtenerProcesadorSolicitudes() {
        if (httpRequestProcessor == null) {
            httpRequestProcessor = new HttpRequestProcessor(api, configRef, gestorLogging);
        }
        return httpRequestProcessor;
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
        ConfiguracionAPI snapshot = configRef.obtener();
        snapshot.establecerEscaneoPasivoHabilitado(manejadorHttp.estaCapturaActiva());
        configRef.reemplazar(snapshot);
        guardarConfiguracionSilenciosa("captura");
        pestaniaPrincipal.establecerEstadoCaptura(manejadorHttp.estaCapturaActiva());
        registrar(I18nLogs.trf("Estado de captura actualizado: %s",
                manejadorHttp.estaCapturaActiva() ? I18nLogs.tr("ACTIVA") : I18nLogs.tr("PAUSADA")));
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

    private void registrarOmisionesResponseDetalladas(String prompt, HttpRequestResponse solicitudRespuesta) {
        if (config == null || !config.esDetallado() || !contieneToken(prompt, TOKEN_RESPONSE)) {
            return;
        }
        if (!tieneResponseDisponible(solicitudRespuesta)) {
            String url = resolverUrlContextual(solicitudRespuesta);
            rastrearContextual(I18nLogs.ContextoMenu.RESPONSE_OMITIDA_SERIALIZACION(url));
        }
    }

    private void registrarOmisionesResponseFlujoDetalladas(String prompt, List<HttpRequestResponse> solicitudesValidas) {
        if (config == null || !config.esDetallado()
                || !ProcesadorPromptHTTP.contieneMarcadoresResponse(prompt)
                || Normalizador.esVacia(solicitudesValidas)) {
            return;
        }
        for (HttpRequestResponse solicitud : solicitudesValidas) {
            if (!tieneResponseDisponible(solicitud)) {
                rastrearContextual(I18nLogs.ContextoMenu.RESPONSE_OMITIDA_SERIALIZACION(
                    resolverUrlContextual(solicitud)
                ));
            }
        }
    }

    private String resolverUrlContextual(HttpRequestResponse solicitudRespuesta) {
        if (solicitudRespuesta == null || solicitudRespuesta.request() == null
                || Normalizador.esVacio(solicitudRespuesta.request().url())) {
            return "[URL NULL]";
        }
        return solicitudRespuesta.request().url();
    }

    private void inicializarAgenteSiHabilitado() {
        if (!hayAgenteOperativoDisponible()) {
            registrar(I18nLogs.Agente.ERROR_DESHABILITADO());
            return;
        }

        if (pestaniaPrincipal == null) {
            registrarError(I18nLogs.Agente.ERROR_INICIALIZACION_PESTANA());
            return;
        }

        PanelAgente panelAgente = pestaniaPrincipal.obtenerPanelAgente();
        if (panelAgente == null) {
            registrarError(I18nLogs.Agente.ERROR_INICIALIZACION_PANEL());
            return;
        }

        panelAgente.asegurarConsolaIniciada();
        registrar(I18nLogs.Extension.AGENTE_INICIALIZADO());
    }

    private void guardarConfiguracionSilenciosa(String origen) {
        if (gestorConfig == null || config == null) {
            return;
        }
        StringBuilder mensajeError = new StringBuilder();
        if (!gestorConfig.guardarConfiguracion(config, mensajeError)) {
            String detalle = mensajeError.toString().trim();
            if (Normalizador.esVacio(detalle)) {
                detalle = I18nUI.Tareas.MSG_ERROR_DESCONOCIDO();
            }
            registrarError(I18nUI.Configuracion.MSG_ERROR_PERSISTIR_CONFIG(origen, detalle));
        }
    }

    public void unload() {
        registrar(I18nLogs.Extension.DESCARGANDO());

        this.httpRequestProcessor = null;

        if (fabricaMenuContextual != null) {
            fabricaMenuContextual.marcarDescargado();
        }

        if (manejadorHttp != null) {
            manejadorHttp.shutdown();
            manejadorHttp = null;
            registrar(I18nLogs.Extension.EXECUTOR_CERRADO());
        }

        if (pestaniaPrincipal != null) {
            pestaniaPrincipal.destruir();
            pestaniaPrincipal = null;
        }

        if (gestorConsola != null) {
            gestorConsola.shutdown();
            gestorConsola = null;
        }

        // L9: liberar la referencia del handler de purgado del modelo antes de
        // descartar el gestor. El handler captura una referencia al gestor/UI;
        // sin limpiarlo, el modelo retiene la vieja UI en plugins singleton.
        if (modeloTablaTareas != null) {
            modeloTablaTareas.dispose();
        }

        if (gestorTareas != null) {
            gestorTareas.shutdown();
            gestorTareas = null;
        }

        if (limitador != null) {
            limitador = null;
        }

        // Cerrar dispatchers y connection pools de OkHttpClient cacheados estáticamente,
        // evitando leak de threads al recargar la extensión.
        AnalizadorHTTP.limpiarClientes();

        registrar(I18nLogs.Extension.DESCARGADA_OK());
    }

    public static burp.api.montoya.scanner.audit.issues.AuditIssue crearAuditIssueDesdeHallazgo(Hallazgo hallazgo) {

        if (hallazgo == null) {
            return null;
        }

        if (Normalizador.esVacio(hallazgo.obtenerUrl())) {
            GestorLoggingUnificado.crearMinimal(null, null).warning(
                    "ExtensionBurpIA", I18nLogs.Evidence.HALLAZGO_SIN_URL());
            return null;
        }

        burp.api.montoya.scanner.audit.issues.AuditIssueSeverity severity = convertirSeveridad(
                hallazgo.obtenerSeveridad());
        burp.api.montoya.scanner.audit.issues.AuditIssueConfidence confidence = convertirConfianza(
                hallazgo.obtenerConfianza());

        String remediationDetail = I18nUI.Hallazgos.REMEDIACION_ISSUE();
        String background = I18nUI.Hallazgos.BACKGROUND_ISSUE();
        String remediationBackground = I18nUI.Hallazgos.REMEDIACION_BACKGROUND_ISSUE();

        // El issue se arma SOLO con los 5 campos editables del hallazgo (los mismos del
        // diálogo de doble clic): título, descripción, URL, severidad y confianza. NO se
        // adjunta evidencia HTTP: un request/response reconstruido o sintetizado hace
        // lanzar a siteMap().add (el hallazgo manual, sin evidencia, sí guarda). El texto
        // del LLM se codifica a entidades HTML (guía de PortSwigger) y el baseUrl va CRUDO
        // (Burp lo parsea como URL). La evidencia sigue accesible en la UI de BurpIA.
        return burp.api.montoya.scanner.audit.issues.AuditIssue.auditIssue(
                escaparHtml(hallazgo.obtenerTitulo()),
                escaparHtml(hallazgo.obtenerHallazgo()) + "\n\nURL: " + hallazgo.obtenerUrl(),
                remediationDetail,
                hallazgo.obtenerUrl(),
                severity,
                confidence,
                background,
                remediationBackground,
                severity,
                new HttpRequestResponse[0]);
    }

    /**
     * Traduce texto no confiable (generado por el LLM) a entidades HTML, que es lo que
     * Burp soporta en los campos de texto de un AuditIssue. Evita que metacaracteres
     * rompan el render o inyecten HTML en la vista de issues. El orden importa: '&'
     * debe sustituirse primero. Null -> "" (defensivo: auditIssue no acepta name nulo).
     */
    static String escaparHtml(String texto) {
        if (texto == null) {
            return "";
        }
        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Resuelve el par request/response de un hallazgo: la evidencia directa pasada, si
     * no la almacenada (cache/disco), y si no, sintetiza un par desde el request con
     * response vacía. Lo usa el flujo "Enviar a Agente" para incluir el HTTP en el
     * prompt. NOTA: el issue de Burp ya NO usa evidencia (se arma solo con los campos
     * editables del hallazgo); este método queda para el agente.
     */
    static HttpRequestResponse resolverEvidenciaIssue(Hallazgo hallazgo,
            HttpRequestResponse solicitudRespuestaEvidencia) {
        if (solicitudRespuestaEvidencia != null) {
            return solicitudRespuestaEvidencia;
        }
        if (hallazgo == null) {
            return null;
        }

        HttpRequestResponse evidenciaResuelta = hallazgo.obtenerEvidenciaHttp();
        if (evidenciaResuelta != null) {
            return evidenciaResuelta;
        }

        HttpRequest solicitud = hallazgo.obtenerSolicitudHttp();
        if (solicitud != null) {
            try {
                HttpResponse respuestaVacia = HttpResponse.httpResponse(ByteArray.byteArray(new byte[0]));
                return HttpRequestResponse.httpRequestResponse(solicitud, respuestaVacia);
            } catch (Exception ignored) {
                return null;
            }
        }

        return null;
    }

    public static boolean guardarAuditIssueDesdeHallazgo(MontoyaApi api, Hallazgo hallazgo) {
        if (api == null) {
            GestorLoggingUnificado.crearMinimal(null, null).warning(
                    "ExtensionBurpIA", I18nLogs.Evidence.API_MONTOYA_NO_DISPONIBLE());
            return false;
        }
        // Logger vivo: a partir de aquí api != null, así que los diagnósticos de la
        // ruta de issues llegan a Extensions -> Output/Errors (antes se descartaban
        // con crearMinimal(null,null) => api null => logToBurpApi no escribía nada).
        GestorLoggingUnificado logger = GestorLoggingUnificado.crear(null, null, null, api, null);
        if (!esBurpProfessional(api)) {
            logger.warning("ExtensionBurpIA", I18nLogs.Evidence.ISSUES_SOLO_PRO());
            return false;
        }
        if (api.siteMap() == null) {
            logger.warning("ExtensionBurpIA", I18nLogs.Evidence.SITEMAP_NO_DISPONIBLE());
            return false;
        }
        // URL vacía es la causa más común de "no se creó el issue". El guard interno
        // de crearAuditIssueDesdeHallazgo es static (sin api) y su log queda muerto;
        // lo registramos aquí donde sí hay api para distinguirlo del resto.
        if (hallazgo != null && Normalizador.esVacio(hallazgo.obtenerUrl())) {
            logger.warning("ExtensionBurpIA", I18nLogs.Evidence.HALLAZGO_SIN_URL());
            return false;
        }
        burp.api.montoya.scanner.audit.issues.AuditIssue issue = crearAuditIssueDesdeHallazgo(hallazgo);
        if (issue == null) {
            logger.warning("ExtensionBurpIA", I18nLogs.Evidence.AUDIT_ISSUE_NO_CREADO());
            return false;
        }
        try {
            api.siteMap().add(issue);
        } catch (Exception e) {
            logger.error("ExtensionBurpIA", I18nLogs.Evidence.ERROR_AGREGAR_ISSUE_SITEMAP(), e);
            return false;
        }
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
                if (edicion == BurpSuiteEdition.PROFESSIONAL) {
                    return true;
                }
                // edition() puede devolver null o COMMUNITY: no cortocircuitamos con un
                // return false aquí, porque version() podría no reflejar la edición real
                // en algunos builds. Caemos al fallback de ai().isEnabled().
            }
        // Best-effort: edition probe — Burp CE/Community throws; fallback to defaults
        } catch (Exception ignored) {
        }

        try {
            return api.ai() != null && api.ai().isEnabled();
        // Best-effort: ai().isEnabled() probe — CE/Community may throw
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
        // Best-effort: version probe — Burp CE/Community throws; fallback to defaults
        } catch (Exception ignored) {
        }
        return null;
    }

    public boolean esBurpProfessional() {
        return esProfessional;
    }
}
