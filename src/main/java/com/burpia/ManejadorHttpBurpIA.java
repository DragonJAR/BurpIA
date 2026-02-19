package com.burpia;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.handler.HttpHandler;
import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.handler.RequestToBeSentAction;
import burp.api.montoya.http.handler.ResponseReceivedAction;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import com.burpia.analyzer.AnalizadorAI;
import com.burpia.config.ConfiguracionAPI;
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
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class ManejadorHttpBurpIA implements HttpHandler {
    private final MontoyaApi api;
    private final ConfiguracionAPI config;
    private final PestaniaPrincipal pestaniaPrincipal;
    private final LimitadorTasa limitador;
    private final DeduplicadorSolicitudes deduplicador;
    private final PrintWriter stdout;
    private final PrintWriter stderr;
    private final ExecutorService executorService;
    private final Object logLock;

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
        this.executorService = Executors.newFixedThreadPool(maxThreads, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.setName("BurpIA-Analysis-" + thread.getId());
            return thread;
        });

        this.logLock = new Object();

        registrar("ManejadorHttpBurpIA inicializado (maximoConcurrente=" + config.obtenerMaximoConcurrente() +
            ", retraso=" + config.obtenerRetrasoSegundos() + "s, detallado=" + config.esDetallado() + ")");
    }

    public ManejadorHttpBurpIA(MontoyaApi api, ConfiguracionAPI config, PestaniaPrincipal pestaniaPrincipal,
                             PrintWriter stdout, PrintWriter stderr, LimitadorTasa limitador) {
        this(api, config, pestaniaPrincipal, stdout, stderr, limitador,
             null, null, null, null);
    }

    public void actualizarConfiguracion(ConfiguracionAPI nuevaConfig) {
        registrar("Configuracion actualizada: nuevo maximoConcurrente=" + nuevaConfig.obtenerMaximoConcurrente());
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent solicitudAEnviar) {
        rastrear("Solicitud a enviar: " + solicitudAEnviar.method() + " " + solicitudAEnviar.url());
        return RequestToBeSentAction.continueWith(solicitudAEnviar);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived respuestaRecibida) {
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
        registrar("Procesando: " + metodo + " " + url);

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

        String hashSolicitud = generarHash(cadenaSolicitud.getBytes());
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
        String cuerpo = "";
        try {
            byte[] bodyBytes = respuestaRecibida.initiatingRequest().body().getBytes();
            if (bodyBytes != null && bodyBytes.length > 0) {
                cuerpo = respuestaRecibida.initiatingRequest().bodyToString();
                if (cuerpo == null) {
                    cuerpo = "";
                }
            }
        } catch (Exception e) {
            cuerpo = "";
        }

        int numEncabezados = 0;
        if (respuestaRecibida.initiatingRequest().headers() != null) {
            numEncabezados = respuestaRecibida.initiatingRequest().headers().size();
        }

        rastrear("Detalles de solicitud: Metodo=" + metodo + ", URL=" + url +
                ", Encabezados=" + numEncabezados + ", Longitud cuerpo=" + cuerpo.length());

        SolicitudAnalisis solicitudAnalisis = new SolicitudAnalisis(
            url, metodo, encabezados, cuerpo, hashSolicitud, respuestaRecibida.initiatingRequest()
        );

        final AtomicReference<String> tareaIdRef = new AtomicReference<>();
        if (gestorTareas != null) {
            Tarea tarea = gestorTareas.crearTarea(
                "Analisis HTTP",
                url,
                Tarea.ESTADO_EN_COLA,
                "Esperando análisis"
            );
            tareaIdRef.set(tarea.obtenerId());
        }

        SwingUtilities.invokeLater(() -> {
            pestaniaPrincipal.establecerEstado("Analizando: " + url);
            pestaniaPrincipal.registrar("Iniciando analisis para: " + url);
        });

        AnalizadorAI analizador = new AnalizadorAI(solicitudAnalisis, config, stdout, stderr, limitador,
                new AnalizadorAI.Callback() {
                    @Override
                    public void alCompletarAnalisis(ResultadoAnalisisMultiple resultado) {
                        if (estadisticas != null) {
                            estadisticas.incrementarAnalizados();
                        }

                        String tareaId = tareaIdRef.get();
                        if (gestorTareas != null && tareaId != null) {
                            gestorTareas.actualizarTarea(
                                tareaId,
                                Tarea.ESTADO_COMPLETADO,
                                "Completado: " + resultado.obtenerNumeroHallazgos() + " hallazgos"
                            );
                        }

                        if (resultado != null && resultado.obtenerHallazgos() != null) {
                            for (Hallazgo hallazgo : resultado.obtenerHallazgos()) {
                                if (hallazgo == null) {
                                    continue;
                                }

                                if (modeloTablaHallazgos != null) {
                                    SwingUtilities.invokeLater(() -> {
                                        modeloTablaHallazgos.agregarHallazgo(hallazgo);
                                    });
                                }

                                if (estadisticas != null) {
                                    String severidad = hallazgo.obtenerSeveridad();
                                    if (severidad != null) {
                                        estadisticas.incrementarHallazgoSeveridad(severidad);
                                    }
                                }

                                executorService.submit(() -> {
                                    try {
                                        if (respuestaCapturada != null) {
                                            AuditIssue auditIssue = ExtensionBurpIA.crearAuditIssueDesdeHallazgo(
                                                hallazgo,
                                                respuestaCapturada
                                            );
                                            if (auditIssue != null && api != null && api.siteMap() != null) {
                                                api.siteMap().add(auditIssue);
                                                rastrear("AuditIssue creado en Burp Suite para: " + hallazgo.obtenerHallazgo());
                                            }
                                        }
                                    } catch (Exception e) {
                                        registrarError("Error al crear AuditIssue en Burp Suite: " + e.getMessage());
                                        rastrear("Stack trace:", e);
                                    }
                                });
                            }
                        }

                        String resultadoUrl = resultado != null ? resultado.obtenerUrl() : "[URL NULL]";
                        String severidadMax = resultado != null ? resultado.obtenerSeveridadMaxima() : "N/A";
                        int numHallazgos = resultado != null ? resultado.obtenerNumeroHallazgos() : 0;

                        registrar("Analisis completado: " + resultadoUrl +
                            " (severidad maxima: " + severidadMax +
                            ", hallazgos: " + numHallazgos + ")");

                        SwingUtilities.invokeLater(() -> {
                            if (pestaniaPrincipal != null) {
                                pestaniaPrincipal.actualizarEstadisticas();
                                pestaniaPrincipal.establecerEstado("Analisis completado");
                            }
                        });
                    }

                    @Override
                    public void alErrorAnalisis(String error) {
                        if (estadisticas != null) {
                            estadisticas.incrementarErrores();
                        }

                        String tareaId = tareaIdRef.get();
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
                                pestaniaPrincipal.establecerEstado("Analisis fallido");
                                pestaniaPrincipal.actualizarEstadisticas();
                            }
                        });
                    }
                },
                gestorConsola
        );

        executorService.submit(analizador);
        registrar("Hilo de analisis iniciado para: " + url);

        return ResponseReceivedAction.continueWith(respuestaRecibida);
    }

    private boolean esRecursoEstatico(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        int ultimoPunto = url.lastIndexOf('.');
        if (ultimoPunto == -1) {
            return false;
        }

        String extension = url.substring(ultimoPunto);
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

    private void registrar(String mensaje) {
        synchronized (logLock) {
            if (gestorConsola != null) {
                gestorConsola.registrarInfo(mensaje);
            }
            stdout.println("[ManejadorBurpIA] " + mensaje);
            stdout.flush();
        }
    }

    private void rastrear(String mensaje) {
        if (config.esDetallado()) {
            synchronized (logLock) {
                if (gestorConsola != null) {
                    gestorConsola.registrarVerbose(mensaje);
                }
                stdout.println("[ManejadorBurpIA] [RASTREO] " + mensaje);
                stdout.flush();
            }
        }
    }

    private void registrarError(String mensaje) {
        synchronized (logLock) {
            if (gestorConsola != null) {
                gestorConsola.registrarError(mensaje);
            }
            stderr.println("[ManejadorBurpIA] [ERROR] " + mensaje);
            stderr.flush();
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
                    registrar("ExecutorService no terminó en 5 segundos, forzando shutdown...");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                registrarError("Error al esperar terminación de ExecutorService: " + e.getMessage());
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
