package com.burpia.analyzer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nLogs;
import com.burpia.model.Hallazgo;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.ConstructorSolicitudesProveedor;
import com.burpia.util.LimitadorTasa;
import com.burpia.util.ParserRespuestasAI;
import com.burpia.util.ReparadorJson;

import okhttp3.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

public class AnalizadorAI implements Runnable {
    private final SolicitudAnalisis solicitud;
    private final ConfiguracionAPI config;
    private final PrintWriter stdout;
    private final PrintWriter stderr;
    private final LimitadorTasa limitador;
    private final Callback callback;
    private final OkHttpClient clienteHttp;
    private final Gson gson;
    private final ConstructorPrompts constructorPrompt;
    private final GestorConsolaGUI gestorConsola;
    private final BooleanSupplier tareaCancelada;
    private final BooleanSupplier tareaPausada;
    private static final int MAX_CHARS_LOG_DETALLADO = 4000;

    private static final OkHttpClient CLIENTE_COMPARTIDO = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    public interface Callback {
        void alCompletarAnalisis(ResultadoAnalisisMultiple resultado);
        void alErrorAnalisis(String error);
        default void alCanceladoAnalisis() {}
    }

    public AnalizadorAI(SolicitudAnalisis solicitud, ConfiguracionAPI config, PrintWriter stdout, PrintWriter stderr,
                     LimitadorTasa limitador, Callback callback, GestorConsolaGUI gestorConsola,
                     BooleanSupplier tareaCancelada, BooleanSupplier tareaPausada) {
        this.solicitud = solicitud;
        this.config = config != null ? config : new ConfiguracionAPI();
        this.stdout = stdout != null ? stdout : new PrintWriter(OutputStream.nullOutputStream(), true);
        this.stderr = stderr != null ? stderr : new PrintWriter(OutputStream.nullOutputStream(), true);
        this.limitador = limitador != null ? limitador : new LimitadorTasa(1);
        this.callback = callback != null ? callback : new Callback() {
            @Override
            public void alCompletarAnalisis(ResultadoAnalisisMultiple resultado) {
            }

            @Override
            public void alErrorAnalisis(String error) {
            }
        };
        this.gestorConsola = gestorConsola;
        this.clienteHttp = CLIENTE_COMPARTIDO;
        this.gson = new Gson();
        this.constructorPrompt = new ConstructorPrompts(this.config);
        this.tareaCancelada = tareaCancelada != null ? tareaCancelada : () -> false;
        this.tareaPausada = tareaPausada != null ? tareaPausada : () -> false;
    }

    public AnalizadorAI(SolicitudAnalisis solicitud, ConfiguracionAPI config, PrintWriter stdout, PrintWriter stderr,
                     LimitadorTasa limitador, Callback callback) {
        this(solicitud, config, stdout, stderr, limitador, callback, null, null, null);
    }

    public AnalizadorAI(SolicitudAnalisis solicitud, ConfiguracionAPI config, PrintWriter stdout, PrintWriter stderr,
                     LimitadorTasa limitador, Callback callback, GestorConsolaGUI gestorConsola) {
        this(solicitud, config, stdout, stderr, limitador, callback, gestorConsola, null, null);
    }

    @Override
    public void run() {
        String nombreHilo = Thread.currentThread().getName();
        long tiempoInicio = System.currentTimeMillis();
        boolean permisoAdquirido = false;

        registrar("[" + nombreHilo + "] AnalizadorAI iniciado para URL: " + solicitud.obtenerUrl());
        rastrear("[" + nombreHilo + "] Hash de solicitud: " + solicitud.obtenerHashSolicitud());

        try {
            verificarCancelacion();
            esperarSiPausada();

            rastrear("[" + nombreHilo + "] Adquiriendo permiso del limitador (disponibles: " +
                    limitador.permisosDisponibles() + ")");
            limitador.adquirir();
            permisoAdquirido = true;
            rastrear("[" + nombreHilo + "] Permiso de limitador adquirido");

            int retrasoSegundos = config.obtenerRetrasoSegundos();
            rastrear("[" + nombreHilo + "] Durmiendo por " + retrasoSegundos + " segundos antes de llamar a la API");
            esperarConControl(retrasoSegundos * 1000L);

            registrar("Analizando: " + solicitud.obtenerUrl());

            String respuesta = llamarAPIAIConRetries();
            ResultadoAnalisisMultiple resultadoMultiple = parsearRespuesta(respuesta);

            long duracion = System.currentTimeMillis() - tiempoInicio;
            registrar("Analisis completado: " + solicitud.obtenerUrl() + " (tomo " + duracion + "ms)");
            rastrear("[" + nombreHilo + "] Severidad maxima: " + resultadoMultiple.obtenerSeveridadMaxima());

            callback.alCompletarAnalisis(resultadoMultiple);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long duracion = System.currentTimeMillis() - tiempoInicio;
            String causa = e.getMessage() != null ? e.getMessage() : "interrupción";
            if (esCancelada()) {
                registrar("[" + nombreHilo + "] Analisis cancelado por usuario (" + duracion + "ms)");
                callback.alCanceladoAnalisis();
            } else {
                registrarError("[" + nombreHilo + "] Analisis interrumpido despues de " + duracion + "ms: " + causa);
                callback.alErrorAnalisis("Analisis interrumpido: " + causa);
            }
        } catch (Exception e) {
            long duracion = System.currentTimeMillis() - tiempoInicio;
            registrarError("[" + nombreHilo + "] Analisis fallido despues de " + duracion + "ms: " + e.getMessage());
            rastrear("[" + nombreHilo + "] Detalles de solicitud: metodo=" + solicitud.obtenerMetodo() +
                    ", url=" + solicitud.obtenerUrl() + ", longitud_cuerpo=" + solicitud.obtenerCuerpo().length());
            callback.alErrorAnalisis(e.getMessage());
        } finally {
            if (permisoAdquirido) {
                limitador.liberar();
                rastrear("[" + nombreHilo + "] Permiso de limitador liberado (disponibles: " +
                        limitador.permisosDisponibles() + ")");
            }
        }
    }

    private boolean esCancelada() {
        return tareaCancelada.getAsBoolean();
    }

    private boolean esPausada() {
        return tareaPausada.getAsBoolean();
    }

    private void verificarCancelacion() throws InterruptedException {
        if (esCancelada()) {
            throw new InterruptedException("Tarea cancelada por usuario");
        }
    }

    private void esperarSiPausada() throws InterruptedException {
        while (esPausada() && !esCancelada()) {
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

    private String llamarAPIAI() throws IOException {
        try {
            verificarCancelacion();
            esperarSiPausada();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Analisis cancelado/interrumpido por usuario", e);
        }

        rastrear("Construyendo prompt para URL: " + solicitud.obtenerUrl());

        String prompt = constructorPrompt.construirPromptAnalisis(solicitud);
        rastrear("Longitud de prompt: " + prompt.length() + " caracteres");
        rastrear("Prompt (preview):\n" + resumirParaLog(prompt));

        ConstructorSolicitudesProveedor.SolicitudPreparada preparada =
            ConstructorSolicitudesProveedor.construirSolicitud(config, prompt, clienteHttp);
        Request solicitudHttp = preparada.request;
        registrar("Llamando a API: " + preparada.endpoint + " con modelo: " + preparada.modeloUsado);
        if (preparada.advertencia != null && !preparada.advertencia.isEmpty()) {
            registrar(preparada.advertencia);
        }

        rastrear("Encabezados de solicitud: Content-Type=application/json, Authorization=Bearer [OCULTO]");

        try (Response respuesta = clienteHttp.newCall(solicitudHttp).execute()) {
            registrar("Codigo de respuesta de API: " + respuesta.code());
            rastrear("Encabezados de respuesta de API: " + respuesta.headers());

            if (!respuesta.isSuccessful()) {
                String cuerpoError = respuesta.body() != null ? respuesta.body().string() : "null";
                registrarError("Cuerpo de respuesta de error de API: " + cuerpoError);
                String mensajeError = "Error de API: " + respuesta.code() + " - " + cuerpoError;
                if (esErrorNoRecuperable(respuesta.code(), cuerpoError)) {
                    throw new NonRetryableApiException(mensajeError);
                }
                throw new IOException(mensajeError);
            }

            ResponseBody cuerpo = respuesta.body();
            String cuerpoRespuesta = cuerpo != null ? cuerpo.string() : "";
            registrar("Longitud de respuesta de API: " + cuerpoRespuesta.length() + " caracteres");
            rastrear("Respuesta de API (preview):\n" + resumirParaLog(cuerpoRespuesta));

            return cuerpoRespuesta;
        } catch (IOException e) {
            registrarError("Solicitud HTTP fallida: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            rastrear("Stack trace de la excepción:", e);
            throw e;
        } catch (Exception e) {
            registrarError("Error inesperado en llamada API: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            rastrear("Stack trace de la excepción:", e);
            throw new IOException("Error inesperado: " + e.getMessage(), e);
        }
    }

    /**
     * Sistema de retry con backoff exponencial para llamadas a la API.
     * Estrategia:
     * 1. 3 intentos inmediatos (sin espera entre ellos)
     * 2. Si fallan, esperar 30 segundos y reintentar
     * 3. Si falla, esperar 60 segundos y reintentar
     * 4. Si falla, esperar 90 segundos y reintentar
     * 5. Si falla, preguntar al usuario si desea cambiar de proveedor
     */
    private String llamarAPIAIConRetries() throws IOException {
        IOException ultimaExcepcion = null;

        registrar("Sistema de retry: Iniciando 3 intentos inmediatos...");
        for (int i = 0; i < 3; i++) {
            try {
                verificarCancelacion();
                esperarSiPausada();
                registrar("Intento inmediato #" + (i + 1) + " de 3");
                return llamarAPIAI();
            } catch (NonRetryableApiException e) {
                throw e;
            } catch (IOException e) {
                ultimaExcepcion = e;
                registrar("Intento #" + (i + 1) + " falló: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Sistema de retry cancelado/interrumpido", e);
            }
        }

        int[] esperasSegundos = {30, 60, 90};

        for (int i = 0; i < esperasSegundos.length; i++) {
            int esperaSegundos = esperasSegundos[i];
            registrar("Todos los intentos inmediatos fallaron. Esperando " + esperaSegundos +
                     " segundos antes del próximo reintento (intentos: " + (3 + i + 1) + " de 6)");

            try {
                esperarConControl(esperaSegundos * 1000L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IOException("Sistema de retry interrumpido", ie);
            }

            try {
                verificarCancelacion();
                esperarSiPausada();
                registrar("Reintento #" + (4 + i) + " después de esperar " + esperaSegundos + " segundos");
                return llamarAPIAI();
            } catch (NonRetryableApiException e) {
                throw e;
            } catch (IOException e) {
                ultimaExcepcion = e;
                registrar("Reintento #" + (4 + i) + " falló: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Sistema de retry cancelado/interrumpido", e);
            }
        }

        registrar("Todos los reintentos fallaron despues de 6 intentos");
        registrarError("SUGERENCIA: Considera cambiar de proveedor de API.");
        if (ultimaExcepcion == null) {
            ultimaExcepcion = new IOException("Fallo de retry sin detalle de excepción");
        }
        registrarError("Ultimo error: " + ultimaExcepcion.getClass().getSimpleName() + " - " +
            ultimaExcepcion.getMessage());

        throw ultimaExcepcion;
    }

    private boolean esErrorNoRecuperable(int statusCode, String cuerpoError) {
        if (statusCode == 400 || statusCode == 404) {
            String error = cuerpoError != null ? cuerpoError.toLowerCase() : "";
            return error.contains("model is required") ||
                error.contains("not found for api version") ||
                error.contains("invalid_request_error") ||
                error.contains("does not exist");
        }
        return false;
    }

    private static final class NonRetryableApiException extends IOException {
        private NonRetryableApiException(String message) {
            super(message);
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

    private ResultadoAnalisisMultiple parsearRespuesta(String respuestaJson) {
        rastrear("Parseando respuesta JSON");
        List<Hallazgo> hallazgos = new ArrayList<>();

        try {
            String jsonReparado = ReparadorJson.repararJson(respuestaJson);
            if (jsonReparado != null && !jsonReparado.equals(respuestaJson)) {
                rastrear("JSON reparado exitosamente");
                respuestaJson = jsonReparado;
            }

            String proveedor = config.obtenerProveedorAI() != null ? config.obtenerProveedorAI() : "";
            String contenido = ParserRespuestasAI.extraerContenido(respuestaJson, proveedor);
            if (contenido == null || contenido.trim().isEmpty()) {
                contenido = respuestaJson != null ? respuestaJson : "";
            }

            rastrear("Contenido extraído - Longitud: " + contenido.length() + " caracteres");
            rastrear("Contenido (preview):\n" + resumirParaLog(contenido));

            try {
                String contenidoLimpio = contenido.trim();

                if (contenidoLimpio.contains("```json")) {
                    contenidoLimpio = contenidoLimpio.replaceAll("```json\\s*", "");
                    contenidoLimpio = contenidoLimpio.replaceAll("```\\s*$", "");
                    rastrear("Eliminados bloques de código markdown JSON");
                } else if (contenidoLimpio.contains("```")) {
                    contenidoLimpio = contenidoLimpio.replaceAll("```\\w*\\s*", "");
                    contenidoLimpio = contenidoLimpio.replaceAll("```\\s*$", "");
                    rastrear("Eliminados bloques de código markdown genéricos");
                }

                contenidoLimpio = contenidoLimpio.trim();

                if (contenidoLimpio.length() > 0 && !contenidoLimpio.equals(contenido)) {
                    rastrear("Contenido limpio para parsing (preview):\n" + resumirParaLog(contenidoLimpio));
                }

                JsonObject jsonHallazgos = gson.fromJson(contenidoLimpio, JsonObject.class);

                if (jsonHallazgos != null && jsonHallazgos.has("hallazgos")) {
                    JsonArray arrayHallazgos = jsonHallazgos.getAsJsonArray("hallazgos");

                    rastrear("Se encontraron " + arrayHallazgos.size() + " hallazgos en JSON");

                    for (JsonElement elemento : arrayHallazgos) {
                        if (elemento == null || !elemento.isJsonObject()) {
                            rastrear("Elemento de hallazgo invalido, se omite");
                            continue;
                        }
                        JsonObject obj = elemento.getAsJsonObject();
                        String descripcion = obtenerCampoTexto(obj, "descripcion", "Sin descripción");
                        String severidad = obtenerCampoTexto(obj, "severidad", "Info");
                        String confianza = obtenerCampoTexto(obj, "confianza", "Low");

                        severidad = normalizarSeveridad(severidad);
                        confianza = normalizarConfianza(confianza);

                        Hallazgo hallazgo = new Hallazgo(solicitud.obtenerUrl(), descripcion, severidad, confianza, solicitud.obtenerSolicitudHttp());
                        hallazgos.add(hallazgo);

                        rastrear("Hallazgo agregado: " + descripcion + " (" + severidad + ", " + confianza + ")");
                    }
                } else {
                    rastrear("JSON no contiene campo 'hallazgos', intentando parsing de texto plano");
                    hallazgos.addAll(parsearTextoPlano(contenido));
                }
            } catch (Exception e) {
                rastrear("No se pudo parsear como JSON de hallazgos: " + e.getMessage());
                rastrear("Intentando parsing de texto plano como fallback");
                hallazgos.addAll(parsearTextoPlano(contenido));
            }

            String marcaTiempo = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("HH:mm:ss")
            );

            rastrear("Total de hallazgos parseados: " + hallazgos.size());

            return new ResultadoAnalisisMultiple(solicitud.obtenerUrl(), marcaTiempo, hallazgos, solicitud.obtenerSolicitudHttp());

        } catch (Exception e) {
            registrarError("Error al parsear respuesta de API: " + e.getMessage());
            rastrear("JSON fallido (preview):\n" + resumirParaLog(respuestaJson));

            String marcaTiempo = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("HH:mm:ss")
            );
            List<Hallazgo> hallazgosError = new ArrayList<>();
            hallazgosError.add(new Hallazgo(
                solicitud.obtenerUrl(),
                "Error al parsear respuesta: " + e.getMessage(),
                "Info",
                "Low",
                solicitud.obtenerSolicitudHttp()
            ));

            return new ResultadoAnalisisMultiple(solicitud.obtenerUrl(), marcaTiempo, hallazgosError, solicitud.obtenerSolicitudHttp());
        }
    }

    private List<Hallazgo> parsearTextoPlano(String contenido) {
        List<Hallazgo> hallazgos = new ArrayList<>();
        if (contenido == null || contenido.trim().isEmpty()) {
            return hallazgos;
        }

        try {
            String[] lineas = contenido.split("\n");
            StringBuilder descripcion = new StringBuilder();
            String severidad = "Info";
            String confianza = "Low";

            for (String linea : lineas) {
                linea = linea.trim();

                if (linea.toLowerCase().contains("severidad:") ||
                    linea.toLowerCase().contains("severity:")) {
                    String sev = linea.replaceAll("(?i)(severidad:|severity:)", "").trim();
                    severidad = normalizarSeveridad(sev);
                } else if (linea.toLowerCase().contains("vulnerabilidad") ||
                           linea.toLowerCase().contains("descripcion:") ||
                           linea.toLowerCase().contains("description:")) {
                    if (descripcion.length() > 0) {
                        hallazgos.add(new Hallazgo(solicitud.obtenerUrl(), descripcion.toString(), severidad, confianza, solicitud.obtenerSolicitudHttp()));
                        descripcion = new StringBuilder();
                    }
                    descripcion.append(linea.replaceAll("(?i)(vulnerabilidad|descripcion:|description:)", "").trim());
                } else if (linea.length() > 0 && !linea.startsWith("{") && !linea.startsWith("}")) {
                    descripcion.append(" ").append(linea);
                }
            }

            if (descripcion.length() > 0) {
                hallazgos.add(new Hallazgo(solicitud.obtenerUrl(), descripcion.toString().trim(), severidad, confianza, solicitud.obtenerSolicitudHttp()));
            }

            if (hallazgos.isEmpty() && contenido.length() > 0) {
                String sev = extraerSeveridad(contenido);
                hallazgos.add(new Hallazgo(solicitud.obtenerUrl(), contenido.substring(0, Math.min(200, contenido.length())), sev, "Low", solicitud.obtenerSolicitudHttp()));
            }

        } catch (Exception e) {
            rastrear("Error al parsear texto plano: " + e.getMessage());
        }

        return hallazgos;
    }

    private String normalizarSeveridad(String severidad) {
        if (severidad == null) return "Info";

        String s = severidad.toLowerCase();
        if (s.contains("critica") || s.contains("critical")) return "Critical";
        if (s.contains("alta") || s.contains("high")) return "High";
        if (s.contains("media") || s.contains("medium") || s.contains("moderada")) return "Medium";
        if (s.contains("baja") || s.contains("low")) return "Low";
        return "Info";
    }

    private String normalizarConfianza(String confianza) {
        if (confianza == null) return "Medium";

        String c = confianza.toLowerCase();
        if (c.contains("alta") || c.contains("high")) return "High";
        if (c.contains("baja") || c.contains("low")) return "Low"; // Correcto: baja → Low
        return "Medium";
    }

    private String extraerSeveridad(String contenido) {
        String minusculas = contenido.toLowerCase();
        String severidad;

        if (minusculas.contains("alta") || minusculas.contains("high") || minusculas.contains("critica") ||
                minusculas.contains("critical") || minusculas.contains("severa")) {
            severidad = "High";
        } else if (minusculas.contains("media") || minusculas.contains("medium") ||
                minusculas.contains("moderada")) {
            severidad = "Medium";
        } else {
            severidad = "Low";
        }

        rastrear("Severidad extraida: " + severidad + " del contenido");
        return severidad;
    }

    private String obtenerCampoTexto(JsonObject obj, String campo, String porDefecto) {
        if (obj == null || campo == null || campo.trim().isEmpty()) {
            return porDefecto;
        }
        try {
            JsonElement valor = obj.get(campo);
            if (valor == null || valor.isJsonNull()) {
                return porDefecto;
            }
            String texto = valor.getAsString();
            return texto != null && !texto.trim().isEmpty() ? texto : porDefecto;
        } catch (Exception ignored) {
            return porDefecto;
        }
    }

    private void registrar(String mensaje) {
        String mensajeLocalizado = I18nLogs.tr(mensaje);
        if (gestorConsola != null) {
            gestorConsola.registrarInfo(mensajeLocalizado);
        }
        stdout.println("[BurpIA] " + mensajeLocalizado);
        stdout.flush();
    }

    private void rastrear(String mensaje) {
        if (config.esDetallado()) {
            String mensajeLocalizado = I18nLogs.tr(mensaje);
            if (gestorConsola != null) {
                gestorConsola.registrarVerbose(mensajeLocalizado);
            }
            stdout.println("[BurpIA] [RASTREO] " + mensajeLocalizado);
            stdout.flush();
        }
    }

    private void registrarError(String mensaje) {
        String mensajeLocalizado = I18nLogs.tr(mensaje);
        if (gestorConsola != null) {
            gestorConsola.registrarError(mensajeLocalizado);
        }
        stderr.println("[BurpIA] [ERROR] " + mensajeLocalizado);
        stderr.flush();
    }

    private String resumirParaLog(String texto) {
        if (texto == null) {
            return "";
        }
        if (texto.length() <= MAX_CHARS_LOG_DETALLADO) {
            return texto;
        }
        return texto.substring(0, MAX_CHARS_LOG_DETALLADO) +
            "... [truncado " + (texto.length() - MAX_CHARS_LOG_DETALLADO) + " chars]";
    }
}
