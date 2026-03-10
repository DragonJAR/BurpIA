package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nLogs;
import com.burpia.util.ConstructorSolicitudesProveedor;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.Normalizador;
import okhttp3.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class AnalizadorHTTP {
    private static final String ORIGEN_LOG = "AnalizadorHTTP";
    private static final int MAX_CLIENTES_HTTP_CACHE = 8;
    private static final int MAX_INTENTOS_RETRY = 5;
    private static final long BACKOFF_INICIAL_MS = 1000L;
    private static final long BACKOFF_MAXIMO_MS = 8000L;
    
    private final ConfiguracionAPI config;
    private final BooleanSupplier tareaCancelada;
    private final BooleanSupplier tareaPausada;
    private final GestorLoggingUnificado gestorLogging;
    private final ContextExceededDetector detectorContexto;
    private volatile Call llamadaHttpActiva;
    
    private static final Map<String, OkHttpClient> CLIENTES_HTTP_POR_TIMEOUT = Collections
            .synchronizedMap(new LinkedHashMap<String, OkHttpClient>(16, 0.75f, true) {
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

    public AnalizadorHTTP(ConfiguracionAPI config, 
                         BooleanSupplier tareaCancelada, 
                         BooleanSupplier tareaPausada,
                         GestorLoggingUnificado gestorLogging) {
        this.config = config != null ? config : new ConfiguracionAPI();
        this.tareaCancelada = tareaCancelada != null ? tareaCancelada : () -> false;
        this.tareaPausada = tareaPausada != null ? tareaPausada : () -> false;
        this.gestorLogging = gestorLogging != null ? gestorLogging : 
            GestorLoggingUnificado.crearMinimal(null, null);
        this.detectorContexto = new ContextExceededDetector();
    }

    public String llamarAPI(String prompt) throws IOException, InterruptedException, ContextExceededException {
        return llamarAPIConRetries(prompt);
    }

    public void cancelarLlamadaActiva() {
        Call call = llamadaHttpActiva;
        if (call != null && !call.isCanceled()) {
            call.cancel();
        }
    }

    private String llamarAPIConRetries(String prompt) throws IOException, InterruptedException, ContextExceededException {
        IOException ultimaExcepcion = null;
        long backoffActualMs = BACKOFF_INICIAL_MS;

        gestorLogging.info(ORIGEN_LOG, "Sistema de retry: hasta " + MAX_INTENTOS_RETRY + 
                          " intentos con backoff exponencial");

        for (int intento = 1; intento <= MAX_INTENTOS_RETRY; intento++) {
            verificarCancelacion();
            esperarSiPausada();
            
            gestorLogging.info(ORIGEN_LOG, "Intento #" + intento + " de " + MAX_INTENTOS_RETRY);
            
            try {
                return llamarAPISingle(prompt, intento == 1);
            } catch (NonRetryableApiException e) {
                throw e;
            } catch (ApiHttpException e) {
                ultimaExcepcion = e;
                // Si es error de contexto, lanzar excepción específica (no reintentar aquí)
                if (e.esErrorContextoExcedido()) {
                    int limite = detectorContexto.extraerLimiteTokens(e.obtenerCuerpoError());
                    throw new ContextExceededException(e.getMessage(), e.obtenerCuerpoError(), limite);
                }
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
                        intento);
                long esperaSegundos = Math.max(1L, (esperaMs + 999L) / 1000L);
                gestorLogging.info(ORIGEN_LOG, "Esperando " + esperaSegundos + 
                                 " segundos antes del próximo reintento");
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
                gestorLogging.info(ORIGEN_LOG, "Esperando " + esperaSegundos + 
                                 " segundos antes del próximo reintento");
                esperarReintento(esperaMs);
                backoffActualMs = Math.min(backoffActualMs * 2L, BACKOFF_MAXIMO_MS);
            }
        }

        gestorLogging.error(ORIGEN_LOG, "Todos los reintentos fallaron después de " + 
                           MAX_INTENTOS_RETRY + " intentos");
        gestorLogging.error(ORIGEN_LOG, "SUGERENCIA: Considera cambiar de proveedor de API.");
        
        if (ultimaExcepcion == null) {
            ultimaExcepcion = new IOException("Fallo de retry sin detalle de excepción");
        }
        
        gestorLogging.error(ORIGEN_LOG, "Último error: " + ultimaExcepcion.getClass().getSimpleName() + 
                           " - " + ultimaExcepcion.getMessage());

        throw ultimaExcepcion;
    }

    private String llamarAPISingle(String prompt, boolean registrarDetalleSolicitud) 
            throws IOException, InterruptedException {
        OkHttpClient clienteHttp = obtenerClienteHttp();
        
        verificarCancelacion();
        esperarSiPausada();

        ConstructorSolicitudesProveedor.SolicitudPreparada preparada = ConstructorSolicitudesProveedor
                .construirSolicitud(config, prompt, clienteHttp);
        Request solicitudHttp = preparada.request;
        
        gestorLogging.info(ORIGEN_LOG, "Llamando a API: " + preparada.endpoint + 
                          " con modelo: " + preparada.modeloUsado);
        
        if (Normalizador.noEsVacio(preparada.advertencia)) {
            gestorLogging.info(ORIGEN_LOG, preparada.advertencia);
        }
        
        if (registrarDetalleSolicitud) {
            gestorLogging.info(ORIGEN_LOG, 
                "Encabezados de solicitud: Content-Type=application/json, Authorization=Bearer [OCULTO]");
        }

        Call call = clienteHttp.newCall(solicitudHttp);
        llamadaHttpActiva = call;

        try {
            try (Response respuesta = call.execute()) {
                int codigoRespuesta = respuesta.code();
                gestorLogging.info(ORIGEN_LOG, "Código de respuesta de API: " + codigoRespuesta);
                gestorLogging.info(ORIGEN_LOG, "Encabezados de respuesta de API: " + respuesta.headers());

                if (!respuesta.isSuccessful()) {
                    String cuerpoError = "";
                    ResponseBody bodyError = respuesta.body();
                    if (bodyError != null) {
                        try {
                            cuerpoError = bodyError.string();
                        } catch (IOException e) {
                            gestorLogging.error(ORIGEN_LOG, 
                                "No se pudo leer cuerpo de error: " + e.getMessage());
                        }
                    }
                    String retryAfterHeader = respuesta.header("Retry-After");
                    String mensajeError = "Error de API: " + codigoRespuesta + " - " +
                            (Normalizador.noEsVacio(cuerpoError) ? cuerpoError : "sin cuerpo");
                    
                    // Detectar error de contexto excedido
                    String proveedor = config.obtenerProveedorAI();
                    boolean esErrorContexto = detectorContexto.esErrorContextoExcedido(
                        proveedor, codigoRespuesta, cuerpoError);
                    
                    if (esErrorContexto) {
                        gestorLogging.info(ORIGEN_LOG, I18nLogs.ContextoExcedido.DETECTADO());
                        int limiteTokens = detectorContexto.extraerLimiteTokens(cuerpoError);
                        if (limiteTokens > 0) {
                            gestorLogging.verbose(ORIGEN_LOG, I18nLogs.ContextoExcedido.LIMITE_EXTRAIDO(limiteTokens));
                        }
                    }
                    
                    throw new ApiHttpException(codigoRespuesta, cuerpoError, retryAfterHeader, mensajeError, esErrorContexto);
                }

                ResponseBody cuerpo = respuesta.body();
                if (cuerpo == null) {
                    throw new IOException("Respuesta de API sin cuerpo (null)");
                }
                
                String cuerpoRespuesta = cuerpo.string();
                if (cuerpoRespuesta == null) {
                    cuerpoRespuesta = "";
                }
                
                gestorLogging.info(ORIGEN_LOG, "Longitud de respuesta de API: " + 
                                  cuerpoRespuesta.length() + " caracteres");
                
                return cuerpoRespuesta;
            }
        } catch (ApiHttpException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Error inesperado: " + e.getMessage(), e);
        } finally {
            llamadaHttpActiva = null;
        }
    }

    protected OkHttpClient obtenerClienteHttp() {
        int timeoutEfectivo = config.obtenerTiempoEsperaParaModelo(
                config.obtenerProveedorAI(),
                config.obtenerModelo());
        return configurarClienteHttp(timeoutEfectivo, config.ignorarErroresSSL());
    }

    private OkHttpClient configurarClienteHttp(int tiempoEsperaSegundos, boolean ignorarSSL) {
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

    private void configurarSslInseguro(OkHttpClient.Builder builder) {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[] {};
                        }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            gestorLogging.error(ORIGEN_LOG, 
                "SSL inseguro no se pudo configurar, se usará configuración por defecto: " + e.getMessage());
        }
    }

    private void verificarCancelacion() throws InterruptedException {
        if (tareaCancelada.getAsBoolean()) {
            throw new InterruptedException("Tarea cancelada por usuario");
        }
    }

    private void esperarSiPausada() throws InterruptedException {
        while (tareaPausada.getAsBoolean() && !tareaCancelada.getAsBoolean()) {
            Thread.sleep(250);
        }
        verificarCancelacion();
    }

    private void esperarReintento(long esperaMs) throws InterruptedException {
        long restante = esperaMs;
        while (restante > 0) {
            verificarCancelacion();
            esperarSiPausada();
            long espera = Math.min(restante, 250);
            Thread.sleep(espera);
            restante -= espera;
        }
    }

    private void registrarFalloIntento(int intento, IOException error) {
        String falloMsg = PoliticaReintentos.obtenerMensajeErrorAmigable(error);
        gestorLogging.info(ORIGEN_LOG, "Intento #" + intento + " falló: " +
                error.getClass().getSimpleName() + " - " + falloMsg);
        
        if (error instanceof ApiHttpException) {
            ApiHttpException apiError = (ApiHttpException) error;
            String cuerpoError = apiError.obtenerCuerpoError();
            if (Normalizador.noEsVacio(cuerpoError)) {
                gestorLogging.error(ORIGEN_LOG, "Cuerpo de respuesta de error de API: " + cuerpoError);
            }
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
        private final boolean esErrorContexto;

        private ApiHttpException(int codigoEstado, String cuerpoError, String retryAfterHeader, 
                                String mensaje, boolean esErrorContexto) {
            super(mensaje);
            this.codigoEstado = codigoEstado;
            this.cuerpoError = cuerpoError != null ? cuerpoError : "";
            this.retryAfterHeader = retryAfterHeader != null ? retryAfterHeader.trim() : "";
            this.esErrorContexto = esErrorContexto;
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

        private boolean esErrorContextoExcedido() {
            return esErrorContexto;
        }
    }
}