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
import com.burpia.util.ControlBackpressureGlobal;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;





public class AnalizadorAI implements Runnable {
    private static final String ORIGEN_LOG = "AnalizadorAI";
    private final SolicitudAnalisis solicitud;
    private final ConfiguracionAPI config;
    private final PrintWriter stdout;
    private final PrintWriter stderr;
    private final LimitadorTasa limitador;
    private final Callback callback;
    private final Runnable alInicioAnalisis;
    private final OkHttpClient clienteHttp;
    private final Gson gson;
    private final ConstructorPrompts constructorPrompt;
    private final GestorConsolaGUI gestorConsola;
    private final BooleanSupplier tareaCancelada;
    private final BooleanSupplier tareaPausada;
    private final ControlBackpressureGlobal controlBackpressure;
    private final Object logLock;
    private static final int MAX_CHARS_LOG_DETALLADO = 4000;
    private static final int TIEMPO_ESPERA_MIN_SEGUNDOS = 10;
    private static final int TIEMPO_ESPERA_MAX_SEGUNDOS = 300;
    private static final int MAX_INTENTOS_RETRY = 5;
    private static final long BACKOFF_INICIAL_MS = 1000L;
    private static final long BACKOFF_MAXIMO_MS = 8000L;
    private static final Map<Integer, OkHttpClient> CLIENTES_HTTP_POR_TIMEOUT = new ConcurrentHashMap<>();

    public interface Callback {
        void alCompletarAnalisis(ResultadoAnalisisMultiple resultado);
        void alErrorAnalisis(String error);
        default void alCanceladoAnalisis() {}
    }

    public AnalizadorAI(SolicitudAnalisis solicitud, ConfiguracionAPI config, PrintWriter stdout, PrintWriter stderr,
                     LimitadorTasa limitador, Callback callback, Runnable alInicioAnalisis,
                     GestorConsolaGUI gestorConsola, BooleanSupplier tareaCancelada, BooleanSupplier tareaPausada,
                     ControlBackpressureGlobal controlBackpressure) {
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
        this.alInicioAnalisis = alInicioAnalisis;
        this.gestorConsola = gestorConsola;
        int timeoutEfectivo = this.config.obtenerTiempoEsperaParaModelo(
            this.config.obtenerProveedorAI(),
            this.config.obtenerModelo()
        );
        this.clienteHttp = obtenerClienteHttp(timeoutEfectivo);
        this.gson = new Gson();
        this.constructorPrompt = new ConstructorPrompts(this.config);
        this.tareaCancelada = tareaCancelada != null ? tareaCancelada : () -> false;
        this.tareaPausada = tareaPausada != null ? tareaPausada : () -> false;
        this.controlBackpressure = controlBackpressure;
        this.logLock = new Object();
    }

    public AnalizadorAI(SolicitudAnalisis solicitud, ConfiguracionAPI config, PrintWriter stdout, PrintWriter stderr,
                     LimitadorTasa limitador, Callback callback, GestorConsolaGUI gestorConsola,
                     BooleanSupplier tareaCancelada, BooleanSupplier tareaPausada) {
        this(solicitud, config, stdout, stderr, limitador, callback, null, gestorConsola, tareaCancelada, tareaPausada, null);
    }

    public AnalizadorAI(SolicitudAnalisis solicitud, ConfiguracionAPI config, PrintWriter stdout, PrintWriter stderr,
                     LimitadorTasa limitador, Callback callback) {
        this(solicitud, config, stdout, stderr, limitador, callback, null, null, null, null, null);
    }

    public AnalizadorAI(SolicitudAnalisis solicitud, ConfiguracionAPI config, PrintWriter stdout, PrintWriter stderr,
                     LimitadorTasa limitador, Callback callback, GestorConsolaGUI gestorConsola) {
        this(solicitud, config, stdout, stderr, limitador, callback, null, gestorConsola, null, null, null);
    }

    @Override
    public void run() {
        String nombreHilo = Thread.currentThread().getName();
        long tiempoInicio = System.currentTimeMillis();
        boolean permisoAdquirido = false;

        if (solicitud == null) {
            String error = "Solicitud de analisis no disponible";
            registrarError("[" + nombreHilo + "] " + error);
            callback.alErrorAnalisis(error);
            return;
        }

        registrar("[" + nombreHilo + "] AnalizadorAI iniciado para URL: " + solicitud.obtenerUrl());
        rastrear("[" + nombreHilo + "] Hash de solicitud: " + solicitud.obtenerHashSolicitud());

        try {
            verificarCancelacion();
            esperarSiPausada();
            notificarInicioAnalisis();
            String alertaConfiguracion = validarConfiguracionAntesDeConsulta();
            if (!alertaConfiguracion.isEmpty()) {
                registrarError(alertaConfiguracion);
                callback.alErrorAnalisis(alertaConfiguracion);
                return;
            }

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

            if (esPausada()) {
                registrar("[" + nombreHilo + "] Analisis pausado y liberando hilo (" + duracion + "ms)");

                return;
            }

            if (esCancelada()) {
                registrar("[" + nombreHilo + "] Analisis cancelado por usuario (" + duracion + "ms)");
                callback.alCanceladoAnalisis();
            } else {
                registrarError("[" + nombreHilo + "] Analisis interrumpido despues de " + duracion + "ms: " + causa);
                callback.alErrorAnalisis("Analisis interrumpido: " + causa);
            }
        } catch (Exception e) {
            long duracion = System.currentTimeMillis() - tiempoInicio;
            String falloMsg = PoliticaReintentos.obtenerMensajeErrorAmigable(e);

            registrarError("[" + nombreHilo + "] Analisis fallido despues de " + duracion + "ms: " + falloMsg);
            String cuerpoSolicitud = solicitud.obtenerCuerpo();
            int longitudCuerpo = cuerpoSolicitud != null ? cuerpoSolicitud.length() : 0;
            rastrear("[" + nombreHilo + "] Detalles de solicitud: metodo=" + solicitud.obtenerMetodo() +
                    ", url=" + solicitud.obtenerUrl() + ", longitud_cuerpo=" + longitudCuerpo);
            callback.alErrorAnalisis(falloMsg);
        } finally {
            if (permisoAdquirido) {
                limitador.liberar();
                rastrear("[" + nombreHilo + "] Permiso de limitador liberado (disponibles: " +
                        limitador.permisosDisponibles() + ")");
            }
        }
    }

    private void notificarInicioAnalisis() {
        if (alInicioAnalisis == null) {
            return;
        }
        try {
            alInicioAnalisis.run();
        } catch (Exception e) {
            rastrear("No se pudo notificar inicio de analisis: " + e.getMessage());
        }
    }

    private String validarConfiguracionAntesDeConsulta() {
        if (config == null) {
            return "ALERTA: Configuracion de IA no disponible";
        }
        String error = config.validarParaConsultaModelo();
        return error != null ? error.trim() : "";
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

    private String construirPromptAnalisis() {
        rastrear("Construyendo prompt para URL: " + solicitud.obtenerUrl());
        String prompt = constructorPrompt.construirPromptAnalisis(solicitud);
        rastrear("Longitud de prompt: " + prompt.length() + " caracteres");
        rastrearTecnico("Prompt (preview):\n" + resumirParaLog(prompt));
        return prompt;
    }

    private String llamarAPIAI(String prompt, boolean registrarDetalleSolicitud) throws IOException {
        try {
            verificarCancelacion();
            esperarSiPausada();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Analisis cancelado/interrumpido por usuario", e);
        }

        ConstructorSolicitudesProveedor.SolicitudPreparada preparada =
            ConstructorSolicitudesProveedor.construirSolicitud(config, prompt, clienteHttp);
        Request solicitudHttp = preparada.request;
        registrar("Llamando a API: " + preparada.endpoint + " con modelo: " + preparada.modeloUsado);
        if (preparada.advertencia != null && !preparada.advertencia.isEmpty()) {
            registrar(preparada.advertencia);
        }
        if (registrarDetalleSolicitud) {
            rastrearTecnico("Encabezados de solicitud: Content-Type=application/json, Authorization=Bearer [OCULTO]");
        }

        try (Response respuesta = clienteHttp.newCall(solicitudHttp).execute()) {
            registrar("Codigo de respuesta de API: " + respuesta.code());
            rastrearTecnico("Encabezados de respuesta de API: " + respuesta.headers());

            if (!respuesta.isSuccessful()) {
                String cuerpoError = respuesta.body() != null ? respuesta.body().string() : "null";
                String retryAfterHeader = respuesta.header("Retry-After");
                String mensajeError = "Error de API: " + respuesta.code() + " - " + cuerpoError;
                throw new ApiHttpException(respuesta.code(), cuerpoError, retryAfterHeader, mensajeError);
            }

            ResponseBody cuerpo = respuesta.body();
            String cuerpoRespuesta = cuerpo != null ? cuerpo.string() : "";
            registrar("Longitud de respuesta de API: " + cuerpoRespuesta.length() + " caracteres");
            rastrearTecnico("Respuesta de API (preview):\n" + resumirParaLog(cuerpoRespuesta));
            return cuerpoRespuesta;
        } catch (ApiHttpException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Error inesperado: " + e.getMessage(), e);
        }
    }

    private String llamarAPIAIConRetries() throws IOException {
        IOException ultimaExcepcion = null;
        long backoffActualMs = BACKOFF_INICIAL_MS;
        String prompt = construirPromptAnalisis();

        registrar("Sistema de retry: hasta " + MAX_INTENTOS_RETRY + " intentos con backoff exponencial");
        for (int intento = 1; intento <= MAX_INTENTOS_RETRY; intento++) {
            try {
                verificarCancelacion();
                esperarSiPausada();
                esperarCooldownGlobalSiAplica();
                registrar("Intento #" + intento + " de " + MAX_INTENTOS_RETRY);
                return llamarAPIAI(prompt, intento == 1);
            } catch (NonRetryableApiException e) {
                throw e;
            } catch (ApiHttpException e) {
                ultimaExcepcion = e;
                if (PoliticaReintentos.esCodigoNoReintentable(e.obtenerCodigoEstado(), e.obtenerCuerpoError())) {
                    throw new NonRetryableApiException(e.getMessage());
                }
                if (!PoliticaReintentos.esCodigoReintentable(e.obtenerCodigoEstado())) {
                    throw e;
                }
                registrarFalloIntento(intento, e);
                if (intento >= MAX_INTENTOS_RETRY) {
                    break;
                }
                long esperaMs = PoliticaReintentos.calcularEsperaMs(
                    e.obtenerCodigoEstado(),
                    e.obtenerRetryAfterHeader(),
                    backoffActualMs,
                    intento
                );
                activarCooldownGlobalSiAplica(e.obtenerCodigoEstado(), esperaMs);
                long esperaSegundos = Math.max(1L, (esperaMs + 999L) / 1000L);
                registrar("Esperando " + esperaSegundos + " segundos antes del próximo reintento");
                esperarReintento(esperaMs);
                backoffActualMs = Math.min(backoffActualMs * 2L, BACKOFF_MAXIMO_MS);
            } catch (IOException e) {
                ultimaExcepcion = e;
                if (!PoliticaReintentos.esExcepcionReintentable(e)) {
                    throw e;
                }
                registrarFalloIntento(intento, e);
                if (intento >= MAX_INTENTOS_RETRY) {
                    break;
                }
                long esperaMs = PoliticaReintentos.calcularEsperaMs(-1, null, backoffActualMs, intento);
                long esperaSegundos = Math.max(1L, (esperaMs + 999L) / 1000L);
                registrar("Esperando " + esperaSegundos + " segundos antes del próximo reintento");
                esperarReintento(esperaMs);
                backoffActualMs = Math.min(backoffActualMs * 2L, BACKOFF_MAXIMO_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Sistema de retry cancelado/interrumpido", e);
            }
        }

        registrar("Todos los reintentos fallaron despues de " + MAX_INTENTOS_RETRY + " intentos");
        registrarError("SUGERENCIA: Considera cambiar de proveedor de API.");
        if (ultimaExcepcion == null) {
            ultimaExcepcion = new IOException("Fallo de retry sin detalle de excepción");
        }
        registrarError("Ultimo error: " + ultimaExcepcion.getClass().getSimpleName() + " - " +
            ultimaExcepcion.getMessage());

        throw ultimaExcepcion;
    }

    private void esperarReintento(long esperaMs) throws IOException {
        try {
            esperarConControl(esperaMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Sistema de retry cancelado/interrumpido", e);
        }
    }

    private void esperarCooldownGlobalSiAplica() throws InterruptedException {
        if (controlBackpressure == null) {
            return;
        }
        long restante = controlBackpressure.milisegundosRestantes();
        if (restante <= 0L) {
            return;
        }
        long segundos = Math.max(1L, (restante + 999L) / 1000L);
        registrar("Rate limit global activo, esperando " + segundos + " segundos");
        while ((restante = controlBackpressure.milisegundosRestantes()) > 0L) {
            esperarConControl(Math.min(restante, 250L));
        }
    }

    private void activarCooldownGlobalSiAplica(int statusCode, long esperaMs) {
        if (controlBackpressure == null || statusCode != 429 || esperaMs <= 0L) {
            return;
        }
        controlBackpressure.activarCooldown(esperaMs);
    }

    private void registrarFalloIntento(int intento, IOException error) {
        String falloMsg = PoliticaReintentos.obtenerMensajeErrorAmigable(error);
        registrar("Intento #" + intento + " falló: " +
            error.getClass().getSimpleName() + " - " + falloMsg);
        if (error instanceof ApiHttpException) {
            ApiHttpException apiError = (ApiHttpException) error;
            String cuerpoError = apiError.obtenerCuerpoError();
            if (cuerpoError != null && !cuerpoError.trim().isEmpty()) {
                registrarErrorTecnico("Cuerpo de respuesta de error de API: " + cuerpoError);
            }
        }
        if (config.esDetallado() && (intento == 1 || intento == MAX_INTENTOS_RETRY)) {
            rastrearTecnico("Stack trace de la excepción:\n" + obtenerStackTrace(error));
        }
    }

    private static OkHttpClient obtenerClienteHttp(int tiempoEsperaSegundos) {
        int timeoutNormalizado = normalizarTiempoEsperaSegundos(tiempoEsperaSegundos);
        return CLIENTES_HTTP_POR_TIMEOUT.computeIfAbsent(timeoutNormalizado,
            timeout -> new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
        );
    }

    private static int normalizarTiempoEsperaSegundos(int valor) {
        if (valor < TIEMPO_ESPERA_MIN_SEGUNDOS) {
            return TIEMPO_ESPERA_MIN_SEGUNDOS;
        }
        if (valor > TIEMPO_ESPERA_MAX_SEGUNDOS) {
            return TIEMPO_ESPERA_MAX_SEGUNDOS;
        }
        return valor;
    }

    private static final class NonRetryableApiException extends IOException {
        private NonRetryableApiException(String message) {
            super(message);
        }
    }

    private static final class ApiHttpException extends IOException {
        private final int codigoEstado;
        private final String cuerpoError;
        private final String retryAfterHeader;

        private ApiHttpException(int codigoEstado, String cuerpoError, String retryAfterHeader, String mensaje) {
            super(mensaje);
            this.codigoEstado = codigoEstado;
            this.cuerpoError = cuerpoError != null ? cuerpoError : "";
            this.retryAfterHeader = retryAfterHeader != null ? retryAfterHeader.trim() : "";
        }

        private int obtenerCodigoEstado() {
            return codigoEstado;
        }

        private String obtenerCuerpoError() {
            return cuerpoError;
        }

        private String obtenerRetryAfterHeader() {
            return retryAfterHeader;
        }
    }

    private void rastrear(String mensaje, Throwable e) {
        if (config.esDetallado()) {
            rastrearTecnico(mensaje + "\n" + obtenerStackTrace(e));
        }
    }

    private ResultadoAnalisisMultiple parsearRespuesta(String respuestaJson) {
        rastrear("Parseando respuesta JSON");
        List<Hallazgo> hallazgos = new ArrayList<>();
        String respuestaOriginal = respuestaJson != null ? respuestaJson : "";

        try {
            String jsonReparado = ReparadorJson.repararJson(respuestaOriginal);
            if (jsonReparado != null && !jsonReparado.equals(respuestaOriginal)) {
                rastrear("JSON reparado exitosamente");
                respuestaJson = jsonReparado;
            } else {
                respuestaJson = respuestaOriginal;
            }

            String proveedor = config.obtenerProveedorAI() != null ? config.obtenerProveedorAI() : "";
            String contenido = ParserRespuestasAI.extraerContenido(respuestaJson, proveedor);
            if (contenido == null || contenido.trim().isEmpty()) {
                contenido = respuestaJson != null ? respuestaJson : "";
            }

            rastrear("Contenido extraído - Longitud: " + contenido.length() + " caracteres");
            rastrearTecnico("Contenido (preview):\n" + resumirParaLog(contenido));

            try {
                String contenidoLimpio = contenido.trim();

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
                        String titulo = obtenerCampoTexto(obj, "titulo", "Sin título");
                        String descripcion = obtenerCampoTexto(obj, "descripcion", "Sin descripción");
                        String severidad = Hallazgo.normalizarSeveridad(
                            obtenerCampoTexto(obj, "severidad", Hallazgo.SEVERIDAD_INFO)
                        );
                        String confianza = Hallazgo.normalizarConfianza(
                            obtenerCampoTexto(obj, "confianza", Hallazgo.CONFIANZA_BAJA)
                        );

                        Hallazgo hallazgo = new Hallazgo(solicitud.obtenerUrl(), titulo, descripcion, severidad, confianza, solicitud.obtenerSolicitudHttp());
                        hallazgos.add(hallazgo);

                        rastrear("Hallazgo agregado: " + titulo + " (" + severidad + ", " + confianza + ")");
                    }
                } else {
                    rastrear("JSON no contiene campo 'hallazgos', intentando parsing de texto plano");
                    hallazgos.addAll(parsearTextoPlano(contenido));
                }
            } catch (Exception e) {
                rastrear("No se pudo parsear como JSON de hallazgos: " + e.getMessage());
                List<Hallazgo> hallazgosNoEstrictos = parsearHallazgosJsonNoEstricto(contenido);
                if (!hallazgosNoEstrictos.isEmpty()) {
                    rastrear("Fallback JSON no estricto recupero " + hallazgosNoEstrictos.size() + " hallazgos");
                    hallazgos.addAll(hallazgosNoEstrictos);
                } else {
                    rastrear("Intentando parsing de texto plano como fallback");
                    hallazgos.addAll(parsearTextoPlano(contenido));
                }
            }

            if (!respuestaJson.equals(respuestaOriginal)) {
                String contenidoOriginal = ParserRespuestasAI.extraerContenido(respuestaOriginal, proveedor);
                if (contenidoOriginal == null || contenidoOriginal.trim().isEmpty()) {
                    contenidoOriginal = respuestaOriginal;
                }

                List<Hallazgo> hallazgosOriginalesNoEstrictos = parsearHallazgosJsonNoEstricto(contenidoOriginal);
                if (hallazgosOriginalesNoEstrictos.size() > hallazgos.size()) {
                    rastrear(
                        "Se detecto perdida de hallazgos tras reparacion JSON; " +
                        "se conserva parseo no estricto del payload original (" +
                        hallazgosOriginalesNoEstrictos.size() + " > " + hallazgos.size() + ")"
                    );
                    hallazgos.clear();
                    hallazgos.addAll(hallazgosOriginalesNoEstrictos);
                }
            }

            String marcaTiempo = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("HH:mm:ss")
            );

            rastrear("Total de hallazgos parseados: " + hallazgos.size());

            return new ResultadoAnalisisMultiple(solicitud.obtenerUrl(), marcaTiempo, hallazgos, solicitud.obtenerSolicitudHttp());

        } catch (Exception e) {
            registrarError("Error al parsear respuesta de API: " + e.getMessage());
            rastrearTecnico("JSON fallido (preview):\n" + resumirParaLog(respuestaJson));

            String marcaTiempo = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("HH:mm:ss")
            );
            List<Hallazgo> hallazgosError = new ArrayList<>();
            hallazgosError.add(new Hallazgo(
                solicitud.obtenerUrl(),
                "Error de análisis",
                "Error al parsear respuesta: " + e.getMessage(),
                Hallazgo.SEVERIDAD_INFO,
                Hallazgo.CONFIANZA_BAJA,
                solicitud.obtenerSolicitudHttp()
            ));

            return new ResultadoAnalisisMultiple(solicitud.obtenerUrl(), marcaTiempo, hallazgosError, solicitud.obtenerSolicitudHttp());
        }
    }

    private List<Hallazgo> parsearHallazgosJsonNoEstricto(String contenido) {
        List<Hallazgo> hallazgos = new ArrayList<>();
        if (contenido == null || contenido.trim().isEmpty()) {
            return hallazgos;
        }

        String bloqueHallazgos = extraerBloqueArrayHallazgos(contenido);
        if (bloqueHallazgos.isEmpty()) {
            return hallazgos;
        }

        String[] objetos = bloqueHallazgos.split("\\}\\s*,\\s*\\{");
        for (String objeto : objetos) {
            String bloqueHallazgo = normalizarObjetoNoEstricto(objeto);
            if (bloqueHallazgo.isEmpty()) {
                continue;
            }
            String titulo = ParserRespuestasAI.extraerCampoNoEstricto("titulo", bloqueHallazgo);
            if (titulo.isEmpty()) {
                titulo = "Sin título";
            }

            String descripcion = ParserRespuestasAI.extraerCampoNoEstricto("descripcion", bloqueHallazgo);
            if (descripcion.isEmpty()) {
                continue;
            }

            String severidad = Hallazgo.normalizarSeveridad(ParserRespuestasAI.extraerCampoNoEstricto("severidad", bloqueHallazgo));
            String confianza = Hallazgo.normalizarConfianza(ParserRespuestasAI.extraerCampoNoEstricto("confianza", bloqueHallazgo));
            String evidencia = ParserRespuestasAI.extraerCampoNoEstricto("evidencia", bloqueHallazgo);

            String descripcionFinal = evidencia.isEmpty()
                ? descripcion
                : descripcion + "\n" + etiquetaEvidencia() + ": " + evidencia;

            hallazgos.add(new Hallazgo(
                solicitud.obtenerUrl(),
                titulo,
                descripcionFinal,
                severidad,
                confianza,
                solicitud.obtenerSolicitudHttp()
            ));
        }
        return hallazgos;
    }

    private String extraerBloqueArrayHallazgos(String contenido) {
        int indiceHallazgos = contenido.indexOf("\"hallazgos\"");
        if (indiceHallazgos < 0) {
            return "";
        }
        int inicioArray = contenido.indexOf('[', indiceHallazgos);
        if (inicioArray < 0) {
            return "";
        }

        int profundidad = 0;
        boolean enComillas = false;
        boolean escapado = false;
        for (int i = inicioArray; i < contenido.length(); i++) {
            char c = contenido.charAt(i);
            if (escapado) {
                escapado = false;
                continue;
            }
            if (c == '\\') {
                escapado = true;
                continue;
            }
            if (c == '"') {
                enComillas = !enComillas;
                continue;
            }
            if (!enComillas) {
                if (c == '[') {
                    profundidad++;
                } else if (c == ']') {
                    profundidad--;
                    if (profundidad == 0) {
                        return contenido.substring(inicioArray + 1, i);
                    }
                }
            }
        }
        return "";
    }

    private String normalizarObjetoNoEstricto(String objeto) {
        if (objeto == null) {
            return "";
        }
        String bloque = objeto.trim();
        if (bloque.isEmpty()) {
            return "";
        }
        if (!bloque.startsWith("{")) {
            bloque = "{" + bloque;
        }
        if (!bloque.endsWith("}")) {
            bloque = bloque + "}";
        }
        return bloque;
    }


    private String etiquetaEvidencia() {
        return "en".equalsIgnoreCase(config.obtenerIdiomaUi()) ? "Evidence" : "Evidencia";
    }

    private List<Hallazgo> parsearTextoPlano(String contenido) {
        List<Hallazgo> hallazgos = new ArrayList<>();
        if (contenido == null || contenido.trim().isEmpty()) {
            return hallazgos;
        }

        try {
            String[] lineas = contenido.split("\n");
            StringBuilder descripcion = new StringBuilder();
            String severidad = Hallazgo.SEVERIDAD_INFO;
            String confianza = Hallazgo.CONFIANZA_BAJA;

            for (String linea : lineas) {
                linea = linea.trim();

                if (linea.toLowerCase().contains("título:") ||
                    linea.toLowerCase().contains("title:")) {

                    if (descripcion.length() > 0) {
                        String t = descripcion.substring(0, Math.min(30, descripcion.length())) + "...";
                        hallazgos.add(new Hallazgo(solicitud.obtenerUrl(), t, descripcion.toString().trim(), severidad, confianza, solicitud.obtenerSolicitudHttp()));
                        descripcion = new StringBuilder();
                    }

                    descripcion.append(linea.replaceAll("(?i)(título:|title:)", "").trim()).append(" - ");
                } else if (linea.toLowerCase().contains("severidad:") ||
                    linea.toLowerCase().contains("severity:")) {
                    String sev = linea.replaceAll("(?i)(severidad:|severity:)", "").trim();
                    severidad = Hallazgo.normalizarSeveridad(sev);
                } else if (linea.toLowerCase().contains("vulnerabilidad") ||
                           linea.toLowerCase().contains("descripcion:") ||
                           linea.toLowerCase().contains("description:")) {
                    if (descripcion.length() > 0 && !descripcion.toString().contains(" - ")) {
                        String t = descripcion.substring(0, Math.min(30, descripcion.length())) + "...";
                        hallazgos.add(new Hallazgo(solicitud.obtenerUrl(), t, descripcion.toString().trim(), severidad, confianza, solicitud.obtenerSolicitudHttp()));
                        descripcion = new StringBuilder();
                    }
                    descripcion.append(linea.replaceAll("(?i)(vulnerabilidad|descripcion:|description:)", "").trim());
                } else if (linea.length() > 0 && !linea.startsWith("{") && !linea.startsWith("}")) {
                    descripcion.append(" ").append(linea);
                }
            }

            if (descripcion.length() > 0) {
                String descStr = descripcion.toString().trim();
                String t = descStr;
                if (t.contains(" - ")) {
                    String[] partes = t.split(" - ", 2);
                    t = partes[0];
                    descStr = partes[1];
                } else {
                    t = descStr.substring(0, Math.min(30, descStr.length())) + "...";
                }
                hallazgos.add(new Hallazgo(solicitud.obtenerUrl(), t, descStr, severidad, confianza, solicitud.obtenerSolicitudHttp()));
            }

            if (hallazgos.isEmpty() && contenido.length() > 0) {
                String sev = extraerSeveridad(contenido);
                String t = contenido.substring(0, Math.min(30, contenido.length())) + "...";
                hallazgos.add(new Hallazgo(
                    solicitud.obtenerUrl(),
                    "Hallazgo Plano",
                    contenido.trim(),
                    sev,
                    Hallazgo.CONFIANZA_BAJA,
                    solicitud.obtenerSolicitudHttp()
                ));
            }

        } catch (Exception e) {
            rastrear("Error al parsear texto plano: " + e.getMessage());
        }

        return hallazgos;
    }

    private String extraerSeveridad(String contenido) {
        String severidad = Hallazgo.normalizarSeveridad(contenido);
        if (Hallazgo.SEVERIDAD_INFO.equals(severidad)) {
            severidad = Hallazgo.SEVERIDAD_LOW;
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
        registrarInterno(mensaje, GestorConsolaGUI.TipoLog.INFO, false, "[BurpIA] ", false);
    }

    private void rastrear(String mensaje) {
        if (config.esDetallado()) {
            registrarInterno(mensaje, GestorConsolaGUI.TipoLog.VERBOSE, false, "[BurpIA] [RASTREO] ", false);
        }
    }

    private void registrarError(String mensaje) {
        registrarInterno(mensaje, GestorConsolaGUI.TipoLog.ERROR, true, "[BurpIA] [ERROR] ", false);
    }

    private void registrarErrorTecnico(String mensaje) {
        registrarInterno(mensaje, GestorConsolaGUI.TipoLog.ERROR, true, "[BurpIA] [ERROR] ", true);
    }

    private void rastrearTecnico(String mensaje) {
        if (config.esDetallado()) {
            registrarInterno(mensaje, GestorConsolaGUI.TipoLog.VERBOSE, false, "[BurpIA] [RASTREO] ", true);
        }
    }

    private String obtenerStackTrace(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    private void registrarInterno(String mensaje, GestorConsolaGUI.TipoLog tipo, boolean esError, String prefijoSalida,
                                  boolean mensajeTecnico) {
        String mensajeSeguro = mensaje != null ? mensaje : "";
        synchronized (logLock) {
            if (gestorConsola != null) {
                if (mensajeTecnico) {
                    gestorConsola.registrarTecnico(ORIGEN_LOG, mensajeSeguro, tipo);
                } else {
                    gestorConsola.registrar(ORIGEN_LOG, mensajeSeguro, tipo);
                }
                return;
            }

            String prefijoLocalizado = I18nLogs.tr(prefijoSalida);
            String mensajeLocalizado = mensajeTecnico ? I18nLogs.trTecnico(mensajeSeguro) : I18nLogs.tr(mensajeSeguro);
            PrintWriter destino = esError ? stderr : stdout;
            destino.println(prefijoLocalizado + mensajeLocalizado);
            destino.flush();
        }
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
