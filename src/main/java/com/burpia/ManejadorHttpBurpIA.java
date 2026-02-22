package com.burpia;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.handler.HttpHandler;
import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.handler.RequestToBeSentAction;
import burp.api.montoya.http.handler.ResponseReceivedAction;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.burpia.analyzer.AnalizadorAI;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nLogs;
import com.burpia.model.Estadisticas;
import com.burpia.model.Hallazgo;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.model.Tarea;
import com.burpia.ui.ModeloTablaHallazgos;
import com.burpia.ui.PestaniaPrincipal;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorTareas;
import com.burpia.util.LimitadorTasa;
import com.burpia.util.DeduplicadorSolicitudes;

import javax.swing.*;
import java.io.PrintWriter;
import java.net.URI;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

public class ManejadorHttpBurpIA implements HttpHandler {
    private final MontoyaApi api;
    private final ConfiguracionAPI config;
    private final PestaniaPrincipal pestaniaPrincipal;
    private volatile LimitadorTasa limitador;
    private final DeduplicadorSolicitudes deduplicador;
    private final PrintWriter stdout;
    private final PrintWriter stderr;
    private final ThreadPoolExecutor executorService;
    private final Object logLock;
    private volatile boolean capturaActiva = true;

    private static final Set<String> EXTENSIONES_ESTATICAS;

    static {
        Set<String> extensions = new HashSet<>();
        extensions.add(".js");
        extensions.add(".css");
        extensions.add(".png");
        extensions.add(".jpg");
        extensions.add(".jpeg");
        extensions.add(".gif");
        extensions.add(".svg");
        extensions.add(".ico");
        extensions.add(".woff");
        extensions.add(".woff2");
        extensions.add(".webp");
        extensions.add(".ttf");
        extensions.add(".eot");
        EXTENSIONES_ESTATICAS = java.util.Collections.unmodifiableSet(extensions);
    }

    private final Estadisticas estadisticas;
    private final GestorTareas gestorTareas;
    private final GestorConsolaGUI gestorConsola;
    private final ModeloTablaHallazgos modeloTablaHallazgos;

    public ManejadorHttpBurpIA(MontoyaApi api, ConfiguracionAPI config, PestaniaPrincipal pestaniaPrincipal,
                             PrintWriter stdout, PrintWriter stderr, LimitadorTasa limitador,
                             Estadisticas estadisticas, GestorTareas gestorTareas,
                             GestorConsolaGUI gestorConsola, ModeloTablaHallazgos modeloTablaHallazgos) {
        this.api = api;
        this.config = config;
        this.pestaniaPrincipal = pestaniaPrincipal;
        this.stdout = stdout;
        this.stderr = stderr;
        this.limitador = limitador;
        this.deduplicador = new DeduplicadorSolicitudes();
        this.estadisticas = estadisticas;
        this.gestorTareas = gestorTareas;
        this.gestorConsola = gestorConsola;
        this.modeloTablaHallazgos = modeloTablaHallazgos;

        int maxThreads = config.obtenerMaximoConcurrente() > 0 ? config.obtenerMaximoConcurrente() : 10;
        int capacidadCola = Math.max(50, maxThreads * 20);
        this.executorService = new ThreadPoolExecutor(
            maxThreads,
            maxThreads,
            60L,
            java.util.concurrent.TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(capacidadCola),
            runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.setName("BurpIA-" + thread.getId());
            return thread;
            },
            new ThreadPoolExecutor.AbortPolicy()
        );

        this.logLock = new Object();

        registrar("ManejadorHttpBurpIA inicializado (maximoConcurrente=" + config.obtenerMaximoConcurrente() +
            ", retraso=" + config.obtenerRetrasoSegundos() + "s, detallado=" + config.esDetallado() + ")");
        registrar("NOTA: Solo se analizaran solicitudes DENTRO del SCOPE de Burp Suite");
    }

    public ManejadorHttpBurpIA(MontoyaApi api, ConfiguracionAPI config, PestaniaPrincipal pestaniaPrincipal,
                             PrintWriter stdout, PrintWriter stderr, LimitadorTasa limitador) {
        this(api, config, pestaniaPrincipal, stdout, stderr, limitador,
             null, null, null, null);
    }

    public void actualizarConfiguracion(ConfiguracionAPI nuevaConfig) {
        int nuevoMaximoConcurrente = nuevaConfig.obtenerMaximoConcurrente() > 0
            ? nuevaConfig.obtenerMaximoConcurrente()
            : 10;

        this.limitador = new LimitadorTasa(nuevoMaximoConcurrente);
        actualizarPoolEjecucion(nuevoMaximoConcurrente);

        registrar("Configuracion actualizada: nuevo maximoConcurrente=" + nuevoMaximoConcurrente +
            ", nuevo retraso=" + nuevaConfig.obtenerRetrasoSegundos() + "s");
    }

    private void actualizarPoolEjecucion(int nuevoMaximoConcurrente) {
        synchronized (executorService) {
            int maxActual = executorService.getMaximumPoolSize();
            if (nuevoMaximoConcurrente == maxActual) {
                return;
            }

            if (nuevoMaximoConcurrente > maxActual) {
                executorService.setMaximumPoolSize(nuevoMaximoConcurrente);
                executorService.setCorePoolSize(nuevoMaximoConcurrente);
            } else {
                executorService.setCorePoolSize(nuevoMaximoConcurrente);
                executorService.setMaximumPoolSize(nuevoMaximoConcurrente);
            }
        }
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent solicitudAEnviar) {
        rastrear("Solicitud a enviar: " + solicitudAEnviar.method() + " " + solicitudAEnviar.url());
        return RequestToBeSentAction.continueWith(solicitudAEnviar);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived respuestaRecibida) {
        if (!capturaActiva) {
            return ResponseReceivedAction.continueWith(respuestaRecibida);
        }

        if (respuestaRecibida == null) {
            registrarError("Respuesta recibida es null");
            return ResponseReceivedAction.continueWith(respuestaRecibida);
        }

        if (respuestaRecibida.initiatingRequest() == null) {
            registrarError("Solicitud iniciadora es null");
            return ResponseReceivedAction.continueWith(respuestaRecibida);
        }

        final HttpResponseReceived respuestaCapturada = respuestaRecibida;

        String tempUrl = respuestaRecibida.initiatingRequest().url();
        final String url = tempUrl != null ? tempUrl : "[URL NULL]";

        String tempMetodo = respuestaRecibida.initiatingRequest().method();
        final String metodo = tempMetodo != null ? tempMetodo : "[METHOD NULL]";

        int codigoEstado = respuestaRecibida.statusCode();

        if (estadisticas != null) {
            estadisticas.incrementarTotalSolicitudes();
        }

        rastrear("Respuesta recibida: " + metodo + " " + url + " (estado: " + codigoEstado + ")");

        if (!estaEnScope(respuestaRecibida.initiatingRequest())) {
            rastrear("FUERA DE SCOPE - Omitiendo: " + url);
            return ResponseReceivedAction.continueWith(respuestaRecibida);
        }

        registrar("DENTRO DE SCOPE - Procesando: " + metodo + " " + url);

        if (esRecursoEstatico(url)) {
            if (estadisticas != null) {
                estadisticas.incrementarOmitidosBajaConfianza();
            }
            registrar("Omitiendo recurso estatico: " + url);
            return ResponseReceivedAction.continueWith(respuestaRecibida);
        }

        String cadenaSolicitud = respuestaRecibida.initiatingRequest().toString();
        if (cadenaSolicitud == null || cadenaSolicitud.isEmpty()) {
            cadenaSolicitud = metodo + " " + url;
        }

        String hashSolicitud = generarHash(cadenaSolicitud.getBytes(StandardCharsets.UTF_8));
        rastrear("Hash de solicitud: " + hashSolicitud.substring(0, Math.min(8, hashSolicitud.length())) + "...");

        if (deduplicador.esDuplicadoYAgregar(hashSolicitud)) {
            if (estadisticas != null) {
                estadisticas.incrementarOmitidosDuplicado();
            }
            registrar("Solicitud duplicada omitida: " + url);
            return ResponseReceivedAction.continueWith(respuestaRecibida);
        }

        registrar("Nueva solicitud registrada: " + url + " (hash: " + hashSolicitud.substring(0, Math.min(8, hashSolicitud.length())) + "...)");

        String encabezados = extraerEncabezados(respuestaRecibida.initiatingRequest());
        String cuerpo = extraerCuerpoSolicitud(respuestaRecibida.initiatingRequest());
        String encabezadosRespuesta = extraerEncabezadosRespuesta(respuestaRecibida);
        String cuerpoRespuesta = extraerCuerpoRespuesta(respuestaRecibida);

        int numEncabezados = 0;
        if (respuestaRecibida.initiatingRequest().headers() != null) {
            numEncabezados = respuestaRecibida.initiatingRequest().headers().size();
        }

        rastrear("Detalles de solicitud: Metodo=" + metodo + ", URL=" + url +
                ", Encabezados=" + numEncabezados + ", Longitud cuerpo=" + cuerpo.length() +
                ", Codigo respuesta=" + codigoEstado + ", Longitud cuerpo respuesta=" + cuerpoRespuesta.length());

        SolicitudAnalisis solicitudAnalisis = new SolicitudAnalisis(
            url,
            metodo,
            encabezados,
            cuerpo,
            hashSolicitud,
            respuestaRecibida.initiatingRequest(),
            codigoEstado,
            encabezadosRespuesta,
            cuerpoRespuesta
        );
        programarAnalisis(
            solicitudAnalisis,
            construirEvidenciaHttp(respuestaCapturada.initiatingRequest(), respuestaCapturada),
            "Analisis HTTP"
        );

        return ResponseReceivedAction.continueWith(respuestaRecibida);
    }

    public void analizarSolicitudForzada(HttpRequest solicitud) {
        analizarSolicitudForzada(solicitud, null);
    }

    public void analizarSolicitudForzada(HttpRequest solicitud, HttpRequestResponse solicitudRespuestaOriginal) {
        if (solicitud == null) {
            registrarError("No se pudo analizar solicitud forzada: request null");
            return;
        }

        String url = solicitud.url() != null ? solicitud.url() : "[URL NULL]";
        String metodo = solicitud.method() != null ? solicitud.method() : "[METHOD NULL]";
        String cadenaSolicitud = solicitud.toString() != null ? solicitud.toString() : metodo + " " + url;
        String hashSolicitud = generarHash(cadenaSolicitud.getBytes(StandardCharsets.UTF_8));
        String encabezados = extraerEncabezados(solicitud);
        String cuerpo = extraerCuerpoSolicitud(solicitud);
        int codigoEstadoRespuesta = -1;
        String encabezadosRespuesta = "";
        String cuerpoRespuesta = "";
        if (solicitudRespuestaOriginal != null) {
            try {
                if (solicitudRespuestaOriginal.hasResponse() && solicitudRespuestaOriginal.response() != null) {
                    codigoEstadoRespuesta = solicitudRespuestaOriginal.response().statusCode();
                    encabezadosRespuesta = extraerEncabezadosRespuesta(solicitudRespuestaOriginal.response());
                    cuerpoRespuesta = extraerCuerpoRespuesta(solicitudRespuestaOriginal.response());
                }
            } catch (Exception e) {
                rastrear("No se pudo capturar la respuesta para analisis manual", e);
            }
        }

        SolicitudAnalisis solicitudAnalisis = new SolicitudAnalisis(
            url,
            metodo,
            encabezados,
            cuerpo,
            hashSolicitud,
            solicitud,
            codigoEstadoRespuesta,
            encabezadosRespuesta,
            cuerpoRespuesta
        );
        registrar("Analisis forzado solicitado desde menu contextual: " + metodo + " " + url);
        programarAnalisis(
            solicitudAnalisis,
            normalizarEvidenciaManual(solicitud, solicitudRespuestaOriginal),
            "Analisis Manual"
        );
    }

    private void programarAnalisis(SolicitudAnalisis solicitudAnalisis,
                                   HttpRequestResponse evidenciaHttp,
                                   String tipoTarea) {
        final String url = solicitudAnalisis.obtenerUrl();
        final AtomicReference<String> tareaIdRef = new AtomicReference<>();

        if (gestorTareas != null) {
            Tarea tarea = gestorTareas.crearTarea(
                tipoTarea,
                url,
                Tarea.ESTADO_EN_COLA,
                "Esperando analisis"
            );
            tareaIdRef.set(tarea.obtenerId());
        }

        SwingUtilities.invokeLater(() -> {
            if (pestaniaPrincipal != null) {
                pestaniaPrincipal.registrar("Iniciando analisis para: " + url);
            }
        });

        AnalizadorAI analizador = new AnalizadorAI(
            solicitudAnalisis,
            config.crearSnapshot(),
            stdout,
            stderr,
            limitador,
            new AnalizadorAI.Callback() {
                @Override
                public void alCompletarAnalisis(ResultadoAnalisisMultiple resultado) {
                    String tareaId = tareaIdRef.get();
                    boolean cancelada = gestorTareas != null && tareaId != null && gestorTareas.estaTareaCancelada(tareaId);
                    if (cancelada) {
                        registrar("Resultado descartado porque la tarea fue cancelada: " + url);
                        return;
                    }

                    if (estadisticas != null) {
                        estadisticas.incrementarAnalizados();
                    }

                    if (gestorTareas != null && tareaId != null) {
                        gestorTareas.actualizarTarea(
                            tareaId,
                            Tarea.ESTADO_COMPLETADO,
                            "Completado: " + (resultado != null ? resultado.obtenerNumeroHallazgos() : 0) + " hallazgos"
                        );
                    }

                    if (resultado != null && resultado.obtenerHallazgos() != null) {
                        List<Hallazgo> hallazgosValidos = new ArrayList<>();
                        for (Hallazgo hallazgo : resultado.obtenerHallazgos()) {
                            if (hallazgo == null) {
                                continue;
                            }

                            hallazgosValidos.add(hallazgo);

                            if (estadisticas != null) {
                                String severidad = hallazgo.obtenerSeveridad();
                                if (severidad != null) {
                                    estadisticas.incrementarHallazgoSeveridad(severidad);
                                }
                            }

                            guardarHallazgoEnIssuesSiAplica(hallazgo, evidenciaHttp);
                        }

                        if (modeloTablaHallazgos != null && !hallazgosValidos.isEmpty()) {
                            modeloTablaHallazgos.agregarHallazgos(hallazgosValidos);
                        }
                    }

                    String severidadMax = resultado != null ? resultado.obtenerSeveridadMaxima() : "N/A";
                    int numHallazgos = resultado != null ? resultado.obtenerNumeroHallazgos() : 0;
                    registrar("Analisis completado: " + url +
                        " (severidad maxima: " + severidadMax +
                        ", hallazgos: " + numHallazgos + ")");

                    SwingUtilities.invokeLater(() -> {
                        if (pestaniaPrincipal != null) {
                            pestaniaPrincipal.actualizarEstadisticas();
                        }
                    });
                }

                @Override
                public void alErrorAnalisis(String error) {
                    String tareaId = tareaIdRef.get();
                    boolean cancelada = gestorTareas != null && tareaId != null && gestorTareas.estaTareaCancelada(tareaId);

                    if (cancelada) {
                        registrar("Analisis detenido por cancelacion de usuario: " + url);
                        return;
                    }

                    if (estadisticas != null) {
                        estadisticas.incrementarErrores();
                    }

                    if (gestorTareas != null && tareaId != null) {
                        gestorTareas.actualizarTarea(
                            tareaId,
                            Tarea.ESTADO_ERROR,
                            "Error: " + (error != null ? error : "Error desconocido")
                        );
                    }

                    String errorMsg = error != null ? error : "Error desconocido";
                    registrarError("Analisis fallido para " + url + ": " + errorMsg);

                    SwingUtilities.invokeLater(() -> {
                        if (pestaniaPrincipal != null) {
                            pestaniaPrincipal.registrar("Error de analisis: " + errorMsg);
                            pestaniaPrincipal.actualizarEstadisticas();
                        }
                    });
                }

                @Override
                public void alCanceladoAnalisis() {
                    String tareaId = tareaIdRef.get();
                    if (gestorTareas != null && tareaId != null) {
                        gestorTareas.actualizarTarea(tareaId, Tarea.ESTADO_CANCELADO, "Cancelado por usuario");
                    }
                    registrar("Analisis cancelado por usuario: " + url);
                }
            },
            gestorConsola,
            () -> {
                String tareaId = tareaIdRef.get();
                return gestorTareas != null && tareaId != null && gestorTareas.estaTareaCancelada(tareaId);
            },
            () -> {
                String tareaId = tareaIdRef.get();
                return gestorTareas != null && tareaId != null && gestorTareas.estaTareaPausada(tareaId);
            }
        );

        try {
            executorService.submit(analizador);
            registrar("Hilo de analisis iniciado para: " + url);
        } catch (RejectedExecutionException ex) {
            String tareaId = tareaIdRef.get();
            if (gestorTareas != null && tareaId != null) {
                gestorTareas.actualizarTarea(tareaId, Tarea.ESTADO_ERROR, "Descartada por saturación de cola");
            }
            if (estadisticas != null) {
                estadisticas.incrementarErrores();
            }
            registrarError("Cola de análisis saturada, solicitud descartada: " + url);
        }
    }

    private HttpRequestResponse construirEvidenciaHttp(HttpRequest solicitud, burp.api.montoya.http.message.responses.HttpResponse respuesta) {
        if (solicitud == null || respuesta == null) {
            return null;
        }
        try {
            return HttpRequestResponse.httpRequestResponse(solicitud, respuesta);
        } catch (Exception e) {
            rastrear("No se pudo construir HttpRequestResponse para evidencia de Issue", e);
            return null;
        }
    }

    private HttpRequestResponse normalizarEvidenciaManual(HttpRequest solicitud, HttpRequestResponse solicitudRespuestaOriginal) {
        if (solicitudRespuestaOriginal == null) {
            rastrear("Analisis manual sin request/response original: se registraran hallazgos, pero no Issue.");
            return null;
        }

        try {
            if (!solicitudRespuestaOriginal.hasResponse()) {
                rastrear("Analisis manual sin response asociada: se registraran hallazgos, pero no Issue.");
                return null;
            }
            if (solicitudRespuestaOriginal.request() != null && solicitudRespuestaOriginal.response() != null) {
                return solicitudRespuestaOriginal;
            }
        } catch (Exception e) {
            rastrear("No se pudo reutilizar la evidencia original del analisis manual", e);
        }

        try {
            return construirEvidenciaHttp(solicitud, solicitudRespuestaOriginal.response());
        } catch (Exception e) {
            rastrear("No se pudo construir evidencia desde analisis manual", e);
            return null;
        }
    }

    private void guardarHallazgoEnIssuesSiAplica(Hallazgo hallazgo, HttpRequestResponse evidenciaHttp) {
        if (hallazgo == null) {
            return;
        }
        if (evidenciaHttp == null) {
            rastrear("Hallazgo sin evidencia HTTP: no se puede crear AuditIssue");
            return;
        }

        boolean enviarIssues = pestaniaPrincipal != null
            && pestaniaPrincipal.obtenerPanelHallazgos() != null
            && pestaniaPrincipal.obtenerPanelHallazgos().isGuardadoAutomaticoIssuesActivo();
        if (!enviarIssues) {
            rastrear("Hallazgo omitido en Issues (Autoguardado deshabilitado): " + hallazgo.obtenerHallazgo());
            return;
        }

        try {
            if (api == null || api.siteMap() == null) {
                registrarError("No se pudo guardar AuditIssue: SiteMap API no disponible");
                return;
            }
            boolean guardado = ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(api, hallazgo, evidenciaHttp);
            if (!guardado) {
                rastrear("AuditIssue no creado: hallazgo sin datos suficientes");
                return;
            }
            rastrear("AuditIssue creado en Burp Suite para: " + hallazgo.obtenerHallazgo());
        } catch (Exception e) {
            registrarError("Error al crear AuditIssue en Burp Suite: " + e.getMessage());
            rastrear("Stack trace:", e);
        }
    }

    /**
     * Verifica si la solicitud esta dentro del scope de Burp Suite.
     * Esto es CRITICO para asegurar que solo se analicen objetivos autorizados.
     *
     * @param solicitud La solicitud HTTP a verificar
     * @return true si esta en scope, false si esta fuera de scope
     */
    private boolean estaEnScope(HttpRequest solicitud) {
        if (solicitud == null) {
            rastrear("Solicitud null, no se puede verificar scope");
            return false;
        }

        try {
            String url = solicitud.url();
            if (url == null || url.isEmpty()) {
                rastrear("URL null o vacia, no se puede verificar scope");
                return false;
            }

            if (api != null && api.scope() != null) {
                boolean enScope = api.scope().isInScope(url);

                if (!enScope) {
                    rastrear("URL fuera de scope: " + url);
                } else {
                    rastrear("URL dentro de scope: " + url);
                }

                return enScope;
            }

            rastrear("API de scope no disponible, omitiendo solicitud por seguridad");
            return false;

        } catch (Exception e) {
            registrarError("Error al verificar scope: " + e.getMessage());
            return false;
        }
    }

    private boolean esRecursoEstatico(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        String ruta = url;
        try {
            URI uri = URI.create(url);
            if (uri.getPath() != null && !uri.getPath().isEmpty()) {
                ruta = uri.getPath();
            }
        } catch (IllegalArgumentException ignored) {
            // Mantener la URL original para evaluación best-effort
        }

        int ultimoPunto = ruta.lastIndexOf('.');
        if (ultimoPunto == -1) {
            return false;
        }

        String extension = ruta.substring(ultimoPunto).toLowerCase(Locale.ROOT);
        boolean esEstatico = EXTENSIONES_ESTATICAS.contains(extension);

        if (esEstatico && config.esDetallado()) {
            rastrear("Recurso coincidio con filtro estatico: " + url);
        }

        return esEstatico;
    }

    private String generarHash(byte[] datos) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(datos);
            StringBuilder cadenaHexadecimal = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) cadenaHexadecimal.append('0');
                cadenaHexadecimal.append(hex);
            }
            return cadenaHexadecimal.toString();
        } catch (Exception e) {
            registrarError("Error al generar hash: " + e.getMessage());
            throw new RuntimeException("No se pudo generar hash SHA-256", e);
        }
    }

    private String extraerEncabezados(HttpRequest solicitud) {
        if (solicitud == null) {
            return "[SOLICITUD NULL]";
        }

        StringBuilder encabezados = new StringBuilder();
        String metodo = solicitud.method();
        String url = solicitud.url();

        encabezados.append(metodo != null ? metodo : "[METHOD NULL]")
                   .append(" ")
                   .append(url != null ? url : "[URL NULL]")
                   .append("\n");

        if (solicitud.headers() != null) {
            solicitud.headers().forEach(encabezado -> {
                if (encabezado != null) {
                    encabezados.append(encabezado.name() != null ? encabezado.name() : "[NAME NULL]")
                               .append(": ")
                               .append(encabezado.value() != null ? encabezado.value() : "[VALUE NULL]")
                               .append("\n");
                }
            });
        } else {
            encabezados.append("[HEADERS NULL]\n");
        }

        return encabezados.toString();
    }

    private String extraerCuerpoSolicitud(HttpRequest solicitud) {
        if (solicitud == null) {
            return "";
        }
        try {
            byte[] bodyBytes = solicitud.body() != null ? solicitud.body().getBytes() : null;
            if (bodyBytes == null || bodyBytes.length == 0) {
                return "";
            }
            String cuerpo = solicitud.bodyToString();
            return cuerpo != null ? cuerpo : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private String extraerEncabezadosRespuesta(burp.api.montoya.http.message.responses.HttpResponse respuesta) {
        if (respuesta == null) {
            return "[RESPUESTA NULL]";
        }

        StringBuilder encabezados = new StringBuilder();
        try {
            String version = respuesta.httpVersion() != null ? respuesta.httpVersion() : "HTTP/1.1";
            encabezados.append(version)
                .append(" ")
                .append(respuesta.statusCode());

            String reason = respuesta.reasonPhrase();
            if (reason != null && !reason.trim().isEmpty()) {
                encabezados.append(" ").append(reason.trim());
            }
            encabezados.append("\n");

            if (respuesta.headers() != null) {
                respuesta.headers().forEach(encabezado -> {
                    if (encabezado != null) {
                        encabezados.append(encabezado.name() != null ? encabezado.name() : "[NAME NULL]")
                            .append(": ")
                            .append(encabezado.value() != null ? encabezado.value() : "[VALUE NULL]")
                            .append("\n");
                    }
                });
            } else {
                encabezados.append("[HEADERS NULL]\n");
            }
        } catch (Exception e) {
            registrarError("Error al extraer encabezados de respuesta: " + e.getMessage());
            return "[ERROR AL EXTRAER ENCABEZADOS DE RESPUESTA]";
        }

        return encabezados.toString();
    }

    private String extraerCuerpoRespuesta(burp.api.montoya.http.message.responses.HttpResponse respuesta) {
        if (respuesta == null) {
            return "";
        }
        try {
            byte[] bodyBytes = respuesta.body() != null ? respuesta.body().getBytes() : null;
            if (bodyBytes == null || bodyBytes.length == 0) {
                return "";
            }
            String cuerpo = respuesta.bodyToString();
            return cuerpo != null ? cuerpo : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private void registrar(String mensaje) {
        String mensajeLocalizado = I18nLogs.tr(mensaje);
        synchronized (logLock) {
            if (gestorConsola != null) {
                gestorConsola.registrarInfo(mensajeLocalizado);
            }
            escribirSalida(false, "[ManejadorBurpIA] " + mensajeLocalizado);
        }
    }

    private void rastrear(String mensaje) {
        if (config.esDetallado()) {
            String mensajeLocalizado = I18nLogs.tr(mensaje);
            synchronized (logLock) {
                if (gestorConsola != null) {
                    gestorConsola.registrarVerbose(mensajeLocalizado);
                }
                escribirSalida(false, "[ManejadorBurpIA] [RASTREO] " + mensajeLocalizado);
            }
        }
    }

    private void registrarError(String mensaje) {
        String mensajeLocalizado = I18nLogs.tr(mensaje);
        synchronized (logLock) {
            if (gestorConsola != null) {
                gestorConsola.registrarError(mensajeLocalizado);
            }
            escribirSalida(true, "[ManejadorBurpIA] [ERROR] " + mensajeLocalizado);
        }
    }

    private void escribirSalida(boolean error, String mensaje) {
        PrintWriter destino = error ? stderr : stdout;
        if (destino != null) {
            destino.println(mensaje);
            destino.flush();
            return;
        }

        if (api != null) {
            try {
                if (error) {
                    api.logging().logToError(mensaje);
                } else {
                    api.logging().logToOutput(mensaje);
                }
            } catch (Exception ignored) {
                // Evita romper flujo principal por fallos de logging
            }
        }
    }

    private void rastrear(String mensaje, Throwable e) {
        if (config.esDetallado()) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            rastrear(mensaje + "\n" + sw.toString());
        }
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            registrar("Deteniendo ExecutorService de ManejadorHttpBurpIA...");
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    registrar("ExecutorService no termino en 5 segundos, forzando shutdown...");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                registrarError("Error al esperar terminacion de ExecutorService: " + e.getMessage());
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void pausarCaptura() {
        capturaActiva = false;
        registrar("Captura pausada por usuario");
    }

    public void reanudarCaptura() {
        capturaActiva = true;
        registrar("Captura reanudada por usuario");
    }

    public boolean estaCapturaActiva() {
        return capturaActiva;
    }
}
