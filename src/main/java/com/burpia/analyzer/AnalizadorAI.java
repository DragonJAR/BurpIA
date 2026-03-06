package com.burpia.analyzer;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.ProveedorAI;
import com.burpia.i18n.I18nLogs;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Hallazgo;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.ControlBackpressureGlobal;
import com.burpia.util.ConstructorSolicitudesProveedor;
import com.burpia.util.LimitadorTasa;
import com.burpia.util.Normalizador;
import com.burpia.util.ParserRespuestasAI;
import com.burpia.util.ReparadorJson;
import okhttp3.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

public class AnalizadorAI implements Runnable {
    private static final String ORIGEN_LOG = "AnalizadorAI";
    private final SolicitudAnalisis solicitud;
    private ConfiguracionAPI config;
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
    private volatile Call llamadaHttpActiva;
    private static final int MAX_CHARS_LOG_DETALLADO = 4000;
    private static final int MAX_INTENTOS_RETRY = 5;
    private static final long BACKOFF_INICIAL_MS = 1000L;
    private static final long BACKOFF_MAXIMO_MS = 8000L;
    private static final long DELAY_ENTRE_PROVEEDORES_MS = 2000L; // 2 segundos entre proveedores
    private static final int CAPACIDAD_EXTRA_BUILDER = 32; // Capacidad extra para StringBuilder de descripción
    private static final int MAX_LONGITUD_TITULO_RESUMIDO = 30; // Longitud máxima para títulos truncados
    private static final int PROFUNDIDAD_MAXIMA_JSON = 5; // Límite de profundidad recursiva en parsing JSON
    private static final String LINEA_SEPARADORA_PROVEEDOR = "========================================";
    private static final int MAX_CLIENTES_HTTP_CACHE = 8;
    private static final Map<String, OkHttpClient> CLIENTES_HTTP_POR_TIMEOUT = 
        Collections.synchronizedMap(new LinkedHashMap<String, OkHttpClient>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, OkHttpClient> eldest) {
                if (size() > MAX_CLIENTES_HTTP_CACHE) {
                    OkHttpClient cliente = eldest.getValue();
                    if (cliente != null) {
                        cliente.dispatcher().executorService().shutdown();
                        cliente.connectionPool().evictAll();
                    }
                    return true;
                }
                return false;
            }
        });
    private static final String[] CAMPOS_TITULO = {"titulo", "title", "name", "nombre"};
    private static final String[] CAMPOS_DESCRIPCION = {"descripcion", "description", "hallazgo", "finding", "detalle", "details"};
    private static final String[] CAMPOS_SEVERIDAD = {"severidad", "severity", "risk", "impacto"};
    private static final String[] CAMPOS_CONFIANZA = {"confianza", "confidence", "certainty", "certeza"};
    private static final String[] CAMPOS_EVIDENCIA = {"evidencia", "evidence", "proof", "indicator"};
    private static final String[] CAMPOS_HALLAZGOS = {"hallazgos", "findings", "issues", "vulnerabilidades"};
    private static final String[] CAMPOS_TEXTO = {"text", "texto", "content", "mensaje", "message", "value", "descripcion", "description"};
    private static final Pattern PATRON_ETIQUETA_TITULO = Pattern.compile("(?i)(título:|title:)");
    private static final Pattern PATRON_ETIQUETA_SEVERIDAD = Pattern.compile("(?i)(severidad:|severity:)");
    private static final Pattern PATRON_ETIQUETA_DESCRIPCION = Pattern.compile("(?i)(vulnerabilidad|descripcion:|description:)");

    /**
     * Interfaz de callback para recibir notificaciones del análisis.
     */
    public interface Callback {
        /**
         * Called when analysis completes successfully.
         * @param resultado The analysis result containing findings
         */
        void alCompletarAnalisis(ResultadoAnalisisMultiple resultado);

        /**
         * Called when analysis encounters an error.
         * @param error Error message describing the failure
         */
        void alErrorAnalisis(String error);

        /**
         * Called when analysis is cancelled by user.
         * Default implementation does nothing.
         */
        default void alCanceladoAnalisis() {}
    }

    private static String mensajeErrorSolicitudNoDisponible() {
        return I18nUI.tr("Solicitud de analisis no disponible", "Analysis request is not available");
    }

    private static String alertaConfiguracionNoDisponible() {
        return I18nUI.tr("ALERTA: Configuracion de IA no disponible", "ALERT: AI configuration is unavailable");
    }

    private static String mensajeAnalisisInterrumpido(String causa) {
        return I18nUI.trf("Analisis interrumpido: %s", "Analysis interrupted: %s", causa);
    }

    private static String tituloPorDefecto() {
        return I18nUI.tr("Sin título", "Untitled");
    }

    private static String descripcionPorDefecto() {
        return I18nUI.tr("Sin descripción", "No description");
    }

    private static String tituloErrorAnalisis() {
        return I18nUI.tr("Error de análisis", "Analysis error");
    }

    private static String descripcionErrorParseo(String detalle) {
        return I18nUI.trf("Error al parsear respuesta: %s", "Error parsing response: %s", detalle);
    }

    private static String tituloHallazgoPlano() {
        return I18nUI.tr("Hallazgo Plano", "Plain Finding");
    }

    /**
     * Constructor principal del analizador AI.
     *
     * @param solicitud          Solicitud HTTP a analizar
     * @param config             Configuración de API (si es null, se crea una por defecto)
     * @param stdout             PrintWriter para salida estándar (si es null, se usa null output)
     * @param stderr             PrintWriter para errores (si es null, se usa null output)
     * @param limitador          Limitador de tasa para controlar concurrencia (si es null, se crea con límite 1)
     * @param callback           Callback para notificar resultados (si es null, se usa callback vacío)
     * @param alInicioAnalisis   Runnable a ejecutar al inicio del análisis (puede ser null)
     * @param gestorConsola      Gestor de consola GUI para logging (puede ser null)
     * @param tareaCancelada     Supplier que indica si la tarea fue cancelada (si es null, siempre false)
     * @param tareaPausada       Supplier que indica si la tarea está pausada (si es null, siempre false)
     * @param controlBackpressure Control global de backpressure para rate limiting (puede ser null)
     */
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
        this.tareaPausada = tareaPausada != null ? tareaPausada : () -> false;
        this.tareaCancelada = tareaCancelada != null ? tareaCancelada : () -> false;
        this.gson = new Gson();
        this.constructorPrompt = new ConstructorPrompts(this.config);
        this.clienteHttp = obtenerClienteHttp(timeoutEfectivo, this.config.ignorarErroresSSL());
        this.controlBackpressure = controlBackpressure;
        this.logLock = new Object();

        rastrear("[" + Thread.currentThread().getName() + "] Timeout configurado para el cliente HTTP: " + timeoutEfectivo + "s");
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

    /**
     * Cancela la llamada HTTP activa si existe.
     * Libera inmediatamente el thread y el socket al usar OkHttp Call.cancel().
     *
     * <p>Este método está diseñado para ser llamado desde un thread externo
     * cuando se pausa o cancela una tarea, permitiendo que la siguiente tarea
     * en cola comience inmediatamente.</p>
     */
    public void cancelarLlamadaHttpActiva() {
        Call call = llamadaHttpActiva;
        if (call != null && !call.isCanceled()) {
            call.cancel();
        }
    }

    @Override
    public void run() {
        String nombreHilo = Thread.currentThread().getName();
        long tiempoInicio = System.currentTimeMillis();
        boolean permisoAdquirido = false;

        if (solicitud == null) {
            String error = mensajeErrorSolicitudNoDisponible();
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
            if (Normalizador.noEsVacio(alertaConfiguracion)) {
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

            boolean multiHabilitado = config.esMultiProveedorHabilitado();
            List<String> proveedoresConfig = config.obtenerProveedoresMultiConsulta();
            rastrear("DIAGNOSTICO: multiHabilitado=" + multiHabilitado + ", proveedoresConfig=" +
                     (proveedoresConfig != null ? proveedoresConfig.size() + " elementos" : "null"));

            ResultadoAnalisisMultiple resultadoMultiple;
            if (multiHabilitado && proveedoresConfig != null && proveedoresConfig.size() > 1) {
                rastrear("DIAGNOSTICO: Ejecutando multi-proveedor con " + proveedoresConfig.size() + " proveedores");
                resultadoMultiple = ejecutarAnalisisMultiProveedorSecuencial();
            } else {
                if (multiHabilitado) {
                    registrar("PROVEEDOR: Multi-proveedor habilitado pero solo " +
                             (proveedoresConfig != null ? proveedoresConfig.size() : 0) +
                             " proveedor(es) configurado(s). Usando proveedor único: " + config.obtenerProveedorAI());
                } else {
                    registrar("PROVEEDOR: Usando proveedor único: " + config.obtenerProveedorAI());
                }
                String respuesta = llamarAPIAIConRetries();
                resultadoMultiple = parsearRespuesta(respuesta);
            }

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
                callback.alErrorAnalisis(mensajeAnalisisInterrumpido(causa));
            }
        } catch (IOException e) {
            long duracion = System.currentTimeMillis() - tiempoInicio;
            String mensaje = e.getMessage();

            // Detectar si es cancelación de OkHttp (Call.cancel())
            if ("Canceled".equals(mensaje) || (mensaje != null && mensaje.contains("Canceled"))) {
                if (esPausada()) {
                    registrar("[" + nombreHilo + "] Análisis pausado (HTTP call cancelada) después de " + duracion + "ms");
                    return;
                }
                if (esCancelada()) {
                    registrar("[" + nombreHilo + "] Análisis cancelado por usuario (HTTP call cancelada) después de " + duracion + "ms");
                    callback.alCanceladoAnalisis();
                    return;
                }
            }

            // Manejo existente de otros errores HTTP
            String falloMsg = PoliticaReintentos.obtenerMensajeErrorAmigable(e);
            registrarError("[" + nombreHilo + "] Análisis fallido después de " + duracion + "ms: " + falloMsg);
            callback.alErrorAnalisis(falloMsg);
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
            return alertaConfiguracionNoDisponible();
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
        if (Normalizador.noEsVacio(preparada.advertencia)) {
            registrar(preparada.advertencia);
        }
        if (registrarDetalleSolicitud) {
            rastrearTecnico("Encabezados de solicitud: Content-Type=application/json, Authorization=Bearer [OCULTO]");
        }

        // Crear llamada HTTP y almacenar referencia para cancelación externa
        Call call = clienteHttp.newCall(solicitudHttp);
        llamadaHttpActiva = call;

        try {
            Response respuesta = call.execute();
            int codigoRespuesta = respuesta.code();
            registrar("Codigo de respuesta de API: " + codigoRespuesta);
            rastrearTecnico("Encabezados de respuesta de API: " + respuesta.headers());

            try {
                if (!respuesta.isSuccessful()) {
                    String cuerpoError = "";
                    ResponseBody bodyError = respuesta.body();
                    if (bodyError != null) {
                        try {
                            cuerpoError = bodyError.string();
                        } catch (IOException e) {
                            rastrear("No se pudo leer cuerpo de error: " + e.getMessage());
                        }
                    }
                    String retryAfterHeader = respuesta.header("Retry-After");
                    String mensajeError = "Error de API: " + codigoRespuesta + " - " +
                        (Normalizador.noEsVacio(cuerpoError) ? cuerpoError : "sin cuerpo");
                    throw new ApiHttpException(codigoRespuesta, cuerpoError, retryAfterHeader, mensajeError);
                }

                ResponseBody cuerpo = respuesta.body();
                if (cuerpo == null) {
                    throw new IOException("Respuesta de API sin cuerpo (null)");
                }
                String cuerpoRespuesta = cuerpo.string();
                if (cuerpoRespuesta == null) {
                    cuerpoRespuesta = "";
                }
                registrar("Longitud de respuesta de API: " + cuerpoRespuesta.length() + " caracteres");
                rastrearTecnico("Respuesta de API (preview):\n" + resumirParaLog(cuerpoRespuesta));
                return cuerpoRespuesta;
            } finally {
                // Cerrar response para liberar recursos
                respuesta.close();
            }
        } catch (ApiHttpException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Error inesperado: " + e.getMessage(), e);
        } finally {
            // Limpiar referencia para permitir GC
            llamadaHttpActiva = null;
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
                    throw new NonRetryableApiException(e.getMessage(), e);
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

    /**
     * Llama a la API AI con una configuración específica del proveedor.
     * <p>
     * Este método está diseñado para el análisis multi-proveedor, donde cada
     * proveedor necesita su propia configuración (API key, modelo, timeout).
     * </p>
     *
     * <p>Usa un bloque try-finally para garantizar que la configuración original
     * se restaura siempre, evitando race conditions en entornos concurrentes.</p>
     *
     * @param configProveedor Configuración específica del proveedor a usar
     * @return Respuesta de la API como string
     * @throws IOException Si la llamada falla o es cancelada
     */
    private String llamarAPIAIConRetriesConConfig(ConfiguracionAPI configProveedor)
            throws IOException {
        // CONFIABILIDAD: Guardar config actual antes de modificar
        ConfiguracionAPI configBackup = this.config;

        try {
            // Establecer config temporal para este proveedor
            this.config = configProveedor;

            // Llamada original (sin modificaciones)
            return llamarAPIAIConRetries();

        } finally {
            // CONFIABILIDAD: Limpieza de recursos garantizada
            // Restaurar config SIEMPRE, sin importar el resultado
            this.config = configBackup;
        }
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
            if (Normalizador.noEsVacio(cuerpoError)) {
                registrarErrorTecnico("Cuerpo de respuesta de error de API: " + cuerpoError);
            }
        }
        if (config.esDetallado() && (intento == 1 || intento == MAX_INTENTOS_RETRY)) {
            rastrearTecnico("Stack trace de la excepción:\n" + obtenerStackTrace(error));
        }
    }

    private static OkHttpClient obtenerClienteHttp(int tiempoEsperaSegundos, boolean ignorarSSL) {
        int timeoutNormalizado = ConfiguracionAPI.normalizarTiempoEspera(tiempoEsperaSegundos);
        String clave = timeoutNormalizado + (ignorarSSL ? "_insecure" : "_secure");

        OkHttpClient existente = CLIENTES_HTTP_POR_TIMEOUT.get(clave);
        if (existente != null) {
            return existente;
        }

        synchronized (CLIENTES_HTTP_POR_TIMEOUT) {
            existente = CLIENTES_HTTP_POR_TIMEOUT.get(clave);
            if (existente != null) {
                return existente;
            }

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(timeoutNormalizado, TimeUnit.SECONDS)
                .readTimeout(timeoutNormalizado, TimeUnit.SECONDS)
                .writeTimeout(timeoutNormalizado, TimeUnit.SECONDS);

            if (ignorarSSL) {
                configurarSslInseguro(builder);
            }

            OkHttpClient nuevo = builder.build();
            CLIENTES_HTTP_POR_TIMEOUT.put(clave, nuevo);
            return nuevo;
        }
    }

    private static void configurarSslInseguro(OkHttpClient.Builder builder) {
        try {
            final javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                new javax.net.ssl.X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
            };

            final javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            builder.sslSocketFactory(sslSocketFactory, (javax.net.ssl.X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            // SSL inseguro no se pudo configurar, se usará configuración por defecto
        }
    }

    private static final class NonRetryableApiException extends IOException {
        private NonRetryableApiException(String message, Throwable cause) {
            super(message, cause);
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

    private ResultadoAnalisisMultiple parsearRespuesta(String respuestaJson) {
        rastrear("Parseando respuesta JSON");
        List<Hallazgo> hallazgos = new ArrayList<>();
        String respuestaOriginal = respuestaJson != null ? respuestaJson : "";

        try {
            String jsonReparado = ReparadorJson.repararJson(respuestaOriginal);
            String respuestaProcesada = respuestaOriginal;
            if (jsonReparado != null && !jsonReparado.equals(respuestaOriginal)) {
                rastrear("JSON reparado exitosamente");
                respuestaProcesada = jsonReparado;
            }

            String proveedor = config.obtenerProveedorAI() != null ? config.obtenerProveedorAI() : "";
            String contenido = ParserRespuestasAI.extraerContenido(respuestaProcesada, proveedor);
            if (Normalizador.esVacio(contenido)) {
                contenido = respuestaProcesada;
            }

            rastrear("Contenido extraído - Longitud: " + contenido.length() + " caracteres");
            rastrearTecnico("Contenido (preview):\n" + resumirParaLog(contenido));

            try {
                String contenidoLimpio = limpiarBloquesMarkdownJson(contenido);
                JsonElement raiz = gson.fromJson(contenidoLimpio, JsonElement.class);
                List<JsonObject> objetosHallazgos = extraerObjetosHallazgos(raiz);

                if (!objetosHallazgos.isEmpty()) {
                    rastrear("Se encontraron " + objetosHallazgos.size() + " hallazgos en JSON");
                    for (JsonObject obj : objetosHallazgos) {
                        agregarHallazgoNormalizado(
                            hallazgos,
                            extraerCampoFlexible(obj, CAMPOS_TITULO),
                            extraerCampoFlexible(obj, CAMPOS_DESCRIPCION),
                            extraerCampoFlexible(obj, CAMPOS_SEVERIDAD),
                            extraerCampoFlexible(obj, CAMPOS_CONFIANZA),
                            extraerCampoFlexible(obj, CAMPOS_EVIDENCIA)
                        );
                    }
                } else {
                    rastrear("JSON sin objetos de hallazgo, intentando parsing de texto plano");
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

            if (!respuestaProcesada.equals(respuestaOriginal)) {
                String contenidoOriginal = ParserRespuestasAI.extraerContenido(respuestaOriginal, proveedor);
                if (Normalizador.esVacio(contenidoOriginal)) {
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

            rastrear("Total de hallazgos parseados: " + hallazgos.size());

            return new ResultadoAnalisisMultiple(
                solicitud.obtenerUrl(),
                hallazgos,
                solicitud.obtenerSolicitudHttp(),
                Collections.emptyList()
            );

        } catch (Exception e) {
            registrarError("Error al parsear respuesta de API: " + e.getMessage());
            rastrearTecnico("JSON fallido (preview):\n" + resumirParaLog(respuestaJson));

            List<Hallazgo> hallazgosError = new ArrayList<>();
            hallazgosError.add(new Hallazgo(
                solicitud.obtenerUrl(),
                tituloErrorAnalisis(),
                descripcionErrorParseo(e.getMessage()),
                Hallazgo.SEVERIDAD_INFO,
                Hallazgo.CONFIANZA_BAJA,
                solicitud.obtenerSolicitudHttp()
            ));

            return new ResultadoAnalisisMultiple(
                solicitud.obtenerUrl(),
                hallazgosError,
                solicitud.obtenerSolicitudHttp(),
                Collections.emptyList()
            );
        }
    }

    /**
     * Parsea hallazgos JSON aplicando múltiples estrategias de forma robusta.
     *
     * Proceso:
     * 1. Método maestro extraerArrayJsonInteligente() intenta parseo + limpieza
     * 2. Si falla, parsearHallazgosCampoPorCampo() como último fallback
     *
     * NOTA: ParserRespuestasAI es responsable de SU PROPIA limpieza de bloques.
     * AnalizadorAI NO sabe sobre "bloques de pensamiento" (encapsulamiento correcto).
     */
    private List<Hallazgo> parsearHallazgosJsonNoEstricto(String contenido) {
        if (Normalizador.esVacio(contenido)) {
            return new ArrayList<>();
        }

        // ESTRATEGIA PRINCIPAL: Método maestro (hace todo: parseo + limpieza + 3 estrategias)
        JsonArray arrayHallazgos = ParserRespuestasAI.extraerArrayJsonInteligente(contenido, gson);

        if (arrayHallazgos != null && arrayHallazgos.size() > 0) {
            return convertirArrayAHallazgos(arrayHallazgos);
        }

        // ÚLTIMA OPCIÓN: Recuperación por delimitadores (JSON extremadamente roto)
        JsonArray arrayRecuperado = ParserRespuestasAI.extraerHallazgosPorDelimitadores(contenido, gson);

        if (arrayRecuperado != null && arrayRecuperado.size() > 0) {
            rastrear("Recuperación extrema: " + arrayRecuperado.size() + " hallazgos");
            return convertirArrayAHallazgos(arrayRecuperado);
        }

        // Fallback final: parseo campo por campo para respuestas extremadamente malformadas
        return parsearHallazgosCampoPorCampo(contenido);
    }

    /**
     * Convierte un JsonArray a lista de Hallazgos.
     * Método reutilizable que elimina duplicación de lógica de conversión.
     */
    private List<Hallazgo> convertirArrayAHallazgos(JsonArray array) {
        List<Hallazgo> hallazgos = new ArrayList<>();
        for (JsonElement item : array) {
            if (item != null && item.isJsonObject()) {
                JsonObject obj = item.getAsJsonObject();
                agregarHallazgoNormalizado(
                    hallazgos,
                    extraerCampoFlexible(obj, CAMPOS_TITULO),
                    extraerCampoFlexible(obj, CAMPOS_DESCRIPCION),
                    extraerCampoFlexible(obj, CAMPOS_SEVERIDAD),
                    extraerCampoFlexible(obj, CAMPOS_CONFIANZA),
                    extraerCampoFlexible(obj, CAMPOS_EVIDENCIA)
                );
            }
        }
        rastrear("Array JSON convertido a " + hallazgos.size() + " hallazgos");
        return hallazgos;
    }

    /**
     * Estrategia 4: Parseo campo por campo para JSON malformado.
     * Fallback cuando las estrategias de extracción de JSON no funcionan.
     * NOTA: Este método es llamado DESPUÉS de que extraerArrayJsonInteligente() falla,
     * por lo que NO reintenta esa estrategia (evita duplicación de logs y procesamiento).
     */
    private List<Hallazgo> parsearHallazgosCampoPorCampo(String contenido) {
        List<Hallazgo> hallazgos = new ArrayList<>();

        // NOTA: NO volvemos a llamar a extraerArrayJsonInteligente() porque ya fue
        // intentado en parsearHallazgosJsonNoEstricto(). Ir directamente al fallback.

        String bloqueHallazgos = extraerBloqueArrayHallazgos(contenido);
        if (Normalizador.esVacio(bloqueHallazgos)) {
            return hallazgos;
        }

        for (String objeto : extraerObjetosNoEstrictos(bloqueHallazgos)) {
            String bloqueHallazgo = normalizarObjetoNoEstricto(objeto);
            if (Normalizador.esVacio(bloqueHallazgo)) {
                continue;
            }

            String titulo = extraerCampoConFallback(CAMPOS_TITULO, bloqueHallazgo);
            String descripcion = extraerCampoConFallback(CAMPOS_DESCRIPCION, bloqueHallazgo);
            String severidad = extraerCampoConFallback(CAMPOS_SEVERIDAD, bloqueHallazgo);
            String confianza = extraerCampoConFallback(CAMPOS_CONFIANZA, bloqueHallazgo);
            String evidencia = extraerCampoConFallback(CAMPOS_EVIDENCIA, bloqueHallazgo);

            agregarHallazgoNormalizado(hallazgos, titulo, descripcion, severidad, confianza, evidencia);
        }

        if (!hallazgos.isEmpty()) {
            rastrear("Parseo campo por campo recuperó " + hallazgos.size() + " hallazgos");
        }

        return hallazgos;
    }

    private String extraerCampoConFallback(String[] campos, String bloque) {
        for (String campo : campos) {
            String valor = ParserRespuestasAI.extraerCampoNoEstricto(campo, bloque);
            if (Normalizador.noEsVacio(valor)) {
                return valor;
            }
        }
        return "";
    }

    private String extraerBloqueArrayHallazgos(String contenido) {
        int indiceHallazgos = -1;
        String[] claves = {"\"hallazgos\"", "\"findings\"", "\"issues\"", "\"vulnerabilidades\""};
        for (String clave : claves) {
            int indice = contenido.indexOf(clave);
            if (indice >= 0 && (indiceHallazgos < 0 || indice < indiceHallazgos)) {
                indiceHallazgos = indice;
            }
        }
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
        if (Normalizador.esVacio(bloque)) {
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

    private List<String> extraerObjetosNoEstrictos(String bloqueHallazgos) {
        List<String> objetos = new ArrayList<>();
        if (Normalizador.esVacio(bloqueHallazgos)) {
            return objetos;
        }
        int inicioObjeto = -1;
        int profundidad = 0;
        boolean enComillas = false;
        boolean escapado = false;
        for (int i = 0; i < bloqueHallazgos.length(); i++) {
            char c = bloqueHallazgos.charAt(i);
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
            if (enComillas) {
                continue;
            }
            if (c == '{') {
                if (profundidad == 0) {
                    inicioObjeto = i;
                }
                profundidad++;
            } else if (c == '}') {
                profundidad--;
                if (profundidad == 0 && inicioObjeto >= 0) {
                    objetos.add(bloqueHallazgos.substring(inicioObjeto, i + 1));
                    inicioObjeto = -1;
                }
            }
        }
        if (objetos.isEmpty()) {
            String[] partes = bloqueHallazgos.split("\\}\\s*,\\s*\\{");
            for (String parte : partes) {
                if (Normalizador.noEsVacio(parte)) {
                    objetos.add(parte);
                }
            }
        }
        return objetos;
    }

    private String etiquetaEvidencia() {
        return "en".equalsIgnoreCase(config.obtenerIdiomaUi()) ? "Evidence" : "Evidencia";
    }

    private String limpiarBloquesMarkdownJson(String contenido) {
        String limpio = contenido != null ? contenido.trim() : "";
        if (limpio.startsWith("```")) {
            limpio = limpio.replaceFirst("^```(?:json)?\\s*", "");
            limpio = limpio.replaceFirst("\\s*```\\s*$", "");
        }
        return limpio.trim();
    }

    private List<JsonObject> extraerObjetosHallazgos(JsonElement raiz) {
        List<JsonObject> objetos = new ArrayList<>();
        if (raiz == null || raiz.isJsonNull()) {
            return objetos;
        }
        if (raiz.isJsonArray()) {
            anexarObjetosDesdeArreglo(raiz.getAsJsonArray(), objetos);
            return objetos;
        }
        if (!raiz.isJsonObject()) {
            return objetos;
        }
        JsonObject objetoRaiz = raiz.getAsJsonObject();
        JsonElement campoHallazgos = extraerPrimerCampoExistente(objetoRaiz, CAMPOS_HALLAZGOS);
        if (campoHallazgos != null) {
            if (campoHallazgos.isJsonArray()) {
                anexarObjetosDesdeArreglo(campoHallazgos.getAsJsonArray(), objetos);
            } else if (campoHallazgos.isJsonObject()) {
                objetos.add(campoHallazgos.getAsJsonObject());
            }
            return objetos;
        }
        if (pareceObjetoHallazgo(objetoRaiz)) {
            objetos.add(objetoRaiz);
        }
        return objetos;
    }

    private void anexarObjetosDesdeArreglo(JsonArray arreglo, List<JsonObject> destino) {
        if (arreglo == null || destino == null) {
            return;
        }
        for (JsonElement elemento : arreglo) {
            if (elemento != null && elemento.isJsonObject()) {
                destino.add(elemento.getAsJsonObject());
            }
        }
    }

    private JsonElement extraerPrimerCampoExistente(JsonObject objeto, String[] campos) {
        if (objeto == null || campos == null) {
            return null;
        }
        for (String campo : campos) {
            if (Normalizador.esVacio(campo)) {
                continue;
            }
            JsonElement valor = objeto.get(campo);
            if (valor != null && !valor.isJsonNull()) {
                return valor;
            }
        }
        return null;
    }

    private boolean pareceObjetoHallazgo(JsonObject objeto) {
        if (objeto == null) {
            return false;
        }
        return Normalizador.noEsVacio(extraerCampoFlexible(objeto, CAMPOS_DESCRIPCION))
            || Normalizador.noEsVacio(extraerCampoFlexible(objeto, CAMPOS_EVIDENCIA));
    }

    private String extraerCampoFlexible(JsonObject objeto, String[] campos) {
        if (objeto == null || campos == null) {
            return "";
        }
        for (String campo : campos) {
            if (Normalizador.esVacio(campo)) {
                continue;
            }
            String valor = extraerCampoComoTexto(objeto.get(campo), 0);
            if (Normalizador.noEsVacio(valor)) {
                return valor;
            }
        }
        return "";
    }

    private String extraerCampoComoTexto(JsonElement elemento, int profundidad) {
        if (elemento == null || elemento.isJsonNull() || profundidad > PROFUNDIDAD_MAXIMA_JSON) {
            return "";
        }
        if (elemento.isJsonPrimitive()) {
            try {
                String valor = elemento.getAsString();
                return valor != null ? valor.trim() : "";
            } catch (Exception e) {
                return "";
            }
        }
        if (elemento.isJsonArray()) {
            StringBuilder acumulado = new StringBuilder();
            for (JsonElement item : elemento.getAsJsonArray()) {
                anexarTexto(acumulado, extraerCampoComoTexto(item, profundidad + 1));
            }
            return acumulado.toString().trim();
        }
        if (elemento.isJsonObject()) {
            JsonObject objeto = elemento.getAsJsonObject();
            for (String campoTexto : CAMPOS_TEXTO) {
                String texto = extraerCampoComoTexto(objeto.get(campoTexto), profundidad + 1);
                if (Normalizador.noEsVacio(texto)) {
                    return texto;
                }
            }
            for (Map.Entry<String, JsonElement> entry : objeto.entrySet()) {
                String texto = extraerCampoComoTexto(entry.getValue(), profundidad + 1);
                if (Normalizador.noEsVacio(texto)) {
                    return texto;
                }
            }
            return "";
        }
        return "";
    }

    private void anexarTexto(StringBuilder destino, String texto) {
        if (destino == null || texto == null || Normalizador.esVacio(texto)) {
            return;
        }
        if (destino.length() > 0) {
            destino.append(' ');
        }
        destino.append(texto.trim());
    }

    private void agregarHallazgoNormalizado(List<Hallazgo> destino,
                                            String tituloRaw,
                                            String descripcionRaw,
                                            String severidadRaw,
                                            String confianzaRaw,
                                            String evidenciaRaw) {
        if (destino == null) {
            return;
        }
        String titulo = normalizarTextoSimple(tituloRaw, tituloPorDefecto());
        String descripcion = normalizarTextoSimple(descripcionRaw, "");
        String evidencia = normalizarTextoSimple(evidenciaRaw, "");
        if (Normalizador.esVacio(descripcion)) {
            descripcion = evidencia;
        } else if (Normalizador.noEsVacio(evidencia) && !descripcion.contains(evidencia)) {
            StringBuilder sb = new StringBuilder(
                descripcion.length() + evidencia.length() + CAPACIDAD_EXTRA_BUILDER);
            sb.append(descripcion).append("\n").append(etiquetaEvidencia()).append(": ").append(evidencia);
            descripcion = sb.toString();
        }
        if (Normalizador.esVacio(descripcion)) {
            descripcion = descripcionPorDefecto();
        }
        String severidad = Hallazgo.normalizarSeveridad(normalizarTextoSimple(severidadRaw, Hallazgo.SEVERIDAD_INFO));
        String confianza = Hallazgo.normalizarConfianza(normalizarTextoSimple(confianzaRaw, Hallazgo.CONFIANZA_BAJA));
        destino.add(new Hallazgo(
            solicitud.obtenerUrl(),
            titulo,
            descripcion,
            severidad,
            confianza,
            solicitud.obtenerSolicitudHttp()
        ));
        if (config.esDetallado()) {
            rastrear("Hallazgo agregado: " + titulo + " (" + severidad + ", " + confianza + ")");
        }
    }

    private String normalizarTextoSimple(String valor, String porDefecto) {
        if (valor == null) {
            return porDefecto != null ? porDefecto : "";
        }
        String normalizado = Normalizador.normalizarTexto(valor);
        if (normalizado.isEmpty()) {
            return porDefecto != null ? porDefecto : "";
        }
        return normalizado;
    }

    private ResultadoAnalisisMultiple ejecutarAnalisisMultiProveedorSecuencial() throws IOException, InterruptedException {
        List<String> proveedores = config.obtenerProveedoresMultiConsulta();
        if (proveedores == null || proveedores.isEmpty()) {
            registrar("Multi-consulta: No hay proveedores seleccionados, usando proveedor único");
            return ejecutarAnalisisProveedorUnico();
        }

        if (proveedores.size() == 1) {
            registrar("Multi-consulta: Solo 1 proveedor seleccionado, usando proveedor único");
            return ejecutarAnalisisProveedorUnico();
        }

        List<Hallazgo> todosHallazgos = new ArrayList<>();
        List<String> proveedoresFallidos = new ArrayList<>();
        ConfiguracionAPI configOriginal = config;

        try {
            for (String proveedor : proveedores) {
                // CONFIABILIDAD: Verificación de estado antes de cada proveedor
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

                // EFICIENCIA: Delay entre proveedores (excepto el primero)
                if (!todosHallazgos.isEmpty()) {
                    long delaySegundos = DELAY_ENTRE_PROVEEDORES_MS / 1000L;
                    registrar("PROVEEDOR: Esperando " + delaySegundos + " segundos antes del siguiente proveedor");
                    esperarConControl(DELAY_ENTRE_PROVEEDORES_MS);
                }

                // CONFIABILIDAD: Línea separadora antes de cada proveedor para claridad visual
                registrar(LINEA_SEPARADORA_PROVEEDOR);
                registrar("PROVEEDOR: " + proveedor + " (" + modelo + ")");

                try {
                    ConfiguracionAPI configProveedor = new ConfiguracionAPI();
                    configProveedor.aplicarDesde(configOriginal);
                    configProveedor.establecerProveedorAI(proveedor);

                    // NUEVO MÉTODO: Llamada con config temporal (evita race condition)
                    String respuesta = llamarAPIAIConRetriesConConfig(configProveedor);
                    ResultadoAnalisisMultiple resultado = parsearRespuestaConEtiqueta(respuesta, proveedor, modelo);

                    List<Hallazgo> hallazgosProveedor = resultado.obtenerHallazgos();
                    registrar("PROVEEDOR: " + proveedor + " completado - " + hallazgosProveedor.size() + " hallazgo(s) encontrado(s)");
                    todosHallazgos.addAll(hallazgosProveedor);

                } catch (Exception e) {
                    registrar("PROVEEDOR: Error con " + proveedor + ": " + e.getMessage());
                    proveedoresFallidos.add(proveedor);
                }
            }

            // Registro visible de errores parciales
            if (!proveedoresFallidos.isEmpty()) {
                registrarError("PROVEEDOR: " + proveedoresFallidos.size() +
                             " proveedor(es) fallaron: " + String.join(", ", proveedoresFallidos));
            }

            registrar(LINEA_SEPARADORA_PROVEEDOR);
            registrar("PROVEEDOR: Multi-consulta completada. Total de hallazgos combinados: " + todosHallazgos.size());

            return new ResultadoAnalisisMultiple(solicitud.obtenerUrl(), todosHallazgos,
                solicitud.obtenerSolicitudHttp(), proveedoresFallidos);

        } finally {
            this.config = configOriginal;
        }
    }

    private ResultadoAnalisisMultiple ejecutarAnalisisProveedorUnico() throws IOException {
        String respuesta = llamarAPIAIConRetries();
        return parsearRespuesta(respuesta);
    }

    private ResultadoAnalisisMultiple parsearRespuestaConEtiqueta(String respuestaJson,
                                                                   String proveedor,
                                                                   String modelo) {
        ResultadoAnalisisMultiple resultado = parsearRespuesta(respuestaJson);
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
                hallazgo.obtenerEvidenciaHttp()
            );

            hallazgosConEtiqueta.add(hallazgoEtiquetado);
        }

        return new ResultadoAnalisisMultiple(
            solicitud.obtenerUrl(),
            hallazgosConEtiqueta,
            solicitud.obtenerSolicitudHttp(),
            Collections.emptyList()
        );
    }

    private List<Hallazgo> parsearTextoPlano(String contenido) {
        List<Hallazgo> hallazgos = new ArrayList<>();
        if (Normalizador.esVacio(contenido)) {
            return hallazgos;
        }

        try {
            String[] lineas = contenido.split("\n");
            StringBuilder descripcion = new StringBuilder();
            String severidad = Hallazgo.SEVERIDAD_INFO;
            String confianza = Hallazgo.CONFIANZA_BAJA;

            for (String linea : lineas) {
                String lineaNormalizada = linea.trim();
                String lineaLower = lineaNormalizada.toLowerCase(Locale.ROOT);

                if (contieneAlguno(lineaLower, "título:", "title:")) {
                    if (descripcion.length() > 0) {
                        agregarHallazgoDesdeDescripcion(hallazgos, descripcion.toString(), severidad, confianza);
                        descripcion.setLength(0);
                    }

                    descripcion
                        .append(PATRON_ETIQUETA_TITULO.matcher(lineaNormalizada).replaceAll("").trim())
                        .append(" - ");
                } else if (contieneAlguno(lineaLower, "severidad:", "severity:")) {
                    String sev = PATRON_ETIQUETA_SEVERIDAD.matcher(lineaNormalizada).replaceAll("").trim();
                    severidad = Hallazgo.normalizarSeveridad(sev);
                } else if (contieneAlguno(lineaLower, "vulnerabilidad", "descripcion:", "description:")) {
                    if (descripcion.length() > 0 && !descripcion.toString().contains(" - ")) {
                        agregarHallazgoDesdeDescripcion(hallazgos, descripcion.toString(), severidad, confianza);
                        descripcion.setLength(0);
                    }
                    descripcion.append(PATRON_ETIQUETA_DESCRIPCION.matcher(lineaNormalizada).replaceAll("").trim());
                } else if (!lineaNormalizada.isEmpty() 
                           && !lineaNormalizada.startsWith("{") 
                           && !lineaNormalizada.startsWith("}")
                           && !lineaNormalizada.startsWith("[")
                           && !lineaNormalizada.startsWith("]")
                           && !lineaNormalizada.matches("^\"[^\"]+\"\\s*:.*")) { // Ignorar lineas de clave-valor JSON
                    descripcion.append(" ").append(lineaNormalizada);
                }
            }

            if (descripcion.length() > 0) {
                String descStr = descripcion.toString().trim();
                String titulo = descStr;
                if (titulo.contains(" - ")) {
                    String[] partes = titulo.split(" - ", 2);
                    titulo = partes[0];
                    descStr = partes[1];
                } else {
                    titulo = descStr.substring(0, Math.min(MAX_LONGITUD_TITULO_RESUMIDO, descStr.length())) + "...";
                }
                hallazgos.add(new Hallazgo(solicitud.obtenerUrl(), titulo, descStr, severidad, confianza, solicitud.obtenerSolicitudHttp()));
            }

            if (hallazgos.isEmpty() && contenido.length() > 0) {
                String sev = extraerSeveridad(contenido);
                hallazgos.add(new Hallazgo(
                    solicitud.obtenerUrl(),
                    tituloHallazgoPlano(),
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

    private boolean contieneAlguno(String textoLower, String... terminos) {
        if (textoLower == null || terminos == null) {
            return false;
        }
        for (String termino : terminos) {
            if (termino != null && textoLower.contains(termino)) {
                return true;
            }
        }
        return false;
    }

    private void agregarHallazgoDesdeDescripcion(List<Hallazgo> hallazgos, String descripcion, String severidad, String confianza) {
        if (hallazgos == null || descripcion == null) {
            return;
        }
        String descripcionLimpia = descripcion.trim();
        if (descripcionLimpia.isEmpty()) {
            return;
        }
        String titulo = descripcionLimpia.substring(0,
            Math.min(MAX_LONGITUD_TITULO_RESUMIDO, descripcionLimpia.length())) + "...";
        hallazgos.add(new Hallazgo(
            solicitud.obtenerUrl(),
            titulo,
            descripcionLimpia,
            severidad,
            confianza,
            solicitud.obtenerSolicitudHttp()
        ));
    }

    /**
     * Extrae y normaliza la severidad del contenido.
     * <p>
     * Si no se puede determinar la severidad (resulta en INFO), se usa LOW como mínimo
     * para texto plano, ya que si hay contenido que no pudo parsearse como JSON,
     * probablemente representa un hallazgo de seguridad al menos de bajo impacto.
     * </p>
     *
     * @param contenido Contenido del cual extraer la severidad
     * @return Severidad normalizada (nunca INFO para texto plano)
     */
    private String extraerSeveridad(String contenido) {
        String severidad = Hallazgo.normalizarSeveridad(contenido);
        if (Hallazgo.SEVERIDAD_INFO.equals(severidad)) {
            severidad = Hallazgo.SEVERIDAD_LOW;
        }
        rastrear("Severidad extraida: " + severidad + " del contenido");
        return severidad;
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
        GestorConsolaGUI consolaActual = this.gestorConsola;

        if (consolaActual != null) {
            if (mensajeTecnico) {
                consolaActual.registrarTecnico(ORIGEN_LOG, mensajeSeguro, tipo);
            } else {
                consolaActual.registrar(ORIGEN_LOG, mensajeSeguro, tipo);
            }
            return;
        }

        String prefijoLocalizado = I18nLogs.tr(prefijoSalida);
        String mensajeLocalizado = mensajeTecnico ? I18nLogs.trTecnico(mensajeSeguro) : I18nLogs.tr(mensajeSeguro);
        PrintWriter destinoStr;

        synchronized (logLock) {
            destinoStr = esError ? stderr : stdout;
            if (destinoStr != null) {
                destinoStr.println(prefijoLocalizado + mensajeLocalizado);
                destinoStr.flush();
            }
        }
    }

    private String resumirParaLog(String texto) {
        if (texto == null) {
            return "";
        }
        if (config.esDetallado()) {
            return texto;
        }
        if (texto.length() <= MAX_CHARS_LOG_DETALLADO) {
            return texto;
        }
        return texto.substring(0, MAX_CHARS_LOG_DETALLADO) +
            "... [truncado " + (texto.length() - MAX_CHARS_LOG_DETALLADO) + " chars]";
    }
}
