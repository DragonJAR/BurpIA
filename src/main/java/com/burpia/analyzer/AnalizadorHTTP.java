package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nLogs;
import com.burpia.i18n.I18nUI;
import com.burpia.util.ConstructorSolicitudesProveedor;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.ControlCancelacionPausa;
import com.burpia.util.Normalizador;
import okhttp3.*;
import java.io.IOException;
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
    
    private final ConfiguracionAPI config;
    private final BooleanSupplier tareaCancelada;
    private final BooleanSupplier tareaPausada;
    private final ControlCancelacionPausa control;
    private final GestorLoggingUnificado gestorLogging;
    private static final ContextExceededDetector DETECTOR_CONTEXTO = new ContextExceededDetector();
    private volatile Call llamadaHttpActiva;
    
    private static final Map<String, OkHttpClient> CLIENTES_HTTP_POR_TIMEOUT = Collections
            .synchronizedMap(new LinkedHashMap<>(128));

    public AnalizadorHTTP(ConfiguracionAPI config, 
                         BooleanSupplier tareaCancelada, 
                         BooleanSupplier tareaPausada,
                         GestorLoggingUnificado gestorLogging) {
        this.config = config != null ? config : new ConfiguracionAPI();
        this.tareaCancelada = tareaCancelada != null ? tareaCancelada : () -> false;
        this.tareaPausada = tareaPausada != null ? tareaPausada : () -> false;
        this.control = new ControlCancelacionPausa(tareaCancelada, tareaPausada);
        this.gestorLogging = gestorLogging != null ? gestorLogging : 
            GestorLoggingUnificado.crearMinimal(null, null);
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
        long backoffActualMs = PoliticaReintentos.BACKOFF_INICIAL_MS;

        gestorLogging.info(ORIGEN_LOG, I18nLogs.trf("Sistema de retry: hasta %d intentos con backoff exponencial",
                          PoliticaReintentos.MAX_INTENTOS_RETRY));

        for (int intento = 1; intento <= PoliticaReintentos.MAX_INTENTOS_RETRY; intento++) {
            control.verificarCancelacion();
            control.esperarSiPausada();
            
            gestorLogging.info(ORIGEN_LOG, I18nLogs.trf("Intento #%d de %d", intento, PoliticaReintentos.MAX_INTENTOS_RETRY));
            
            try {
                return llamarAPISingle(prompt, intento == 1);
            } catch (NonRetryableApiException e) {
                throw e;
            } catch (ApiHttpException e) {
                ultimaExcepcion = e;
                // Si es error de contexto, lanzar excepción específica (no reintentar aquí)
                if (e.esErrorContextoExcedido()) {
                    int limite = ContextExceededDetector.extraerLimiteTokens(e.obtenerCuerpoError());
                    throw new ContextExceededException(e.getMessage(), e.obtenerCuerpoError(), limite);
                }
                if (PoliticaReintentos.esCodigoNoReintentable(e.obtenerCodigoEstado(), e.obtenerCuerpoError())) {
                    throw new NonRetryableApiException(e.getMessage(), e);
                }
                if (!PoliticaReintentos.esCodigoReintentable(e.obtenerCodigoEstado())) {
                    throw e;
                }
                registrarFalloIntento(intento, e);
                if (intento >= PoliticaReintentos.MAX_INTENTOS_RETRY) {
                    break;
                }
                long esperaMs = PoliticaReintentos.calcularEsperaMs(
                        e.obtenerCodigoEstado(),
                        e.obtenerRetryAfterHeader(),
                        backoffActualMs,
                        intento);
                long esperaSegundos = Math.max(1L, (esperaMs + 999L) / 1000L);
                gestorLogging.info(ORIGEN_LOG, I18nLogs.trf("Esperando %d segundos antes del próximo reintento",
                                 esperaSegundos));
                control.esperarConControl(esperaMs);
                backoffActualMs = Math.min(backoffActualMs * 2L, PoliticaReintentos.BACKOFF_MAXIMO_MS);
            } catch (IOException e) {
                ultimaExcepcion = e;
                if (!PoliticaReintentos.esExcepcionReintentable(e)) {
                    throw e;
                }
                registrarFalloIntento(intento, e);
                if (intento >= PoliticaReintentos.MAX_INTENTOS_RETRY) {
                    break;
                }
                long esperaMs = PoliticaReintentos.calcularEsperaMs(-1, null, backoffActualMs, intento);
                long esperaSegundos = Math.max(1L, (esperaMs + 999L) / 1000L);
                gestorLogging.info(ORIGEN_LOG, I18nLogs.trf("Esperando %d segundos antes del próximo reintento",
                                 esperaSegundos));
                control.esperarConControl(esperaMs);
                backoffActualMs = Math.min(backoffActualMs * 2L, PoliticaReintentos.BACKOFF_MAXIMO_MS);
            }
        }

        gestorLogging.error(ORIGEN_LOG, I18nLogs.trf("Todos los reintentos fallaron después de %d intentos",
                           PoliticaReintentos.MAX_INTENTOS_RETRY));
        gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("SUGERENCIA: Considera cambiar de proveedor de API."));
        
        if (ultimaExcepcion == null) {
            ultimaExcepcion = new IOException(I18nLogs.tr("Fallo de retry sin detalle de excepción"));
        }
        
        gestorLogging.error(ORIGEN_LOG,
                I18nLogs.trf("Último error: %s", ultimaExcepcion.getClass().getSimpleName()),
                ultimaExcepcion);

        throw ultimaExcepcion;
    }

    private String llamarAPISingle(String prompt, boolean registrarDetalleSolicitud) 
            throws IOException, InterruptedException {
        OkHttpClient clienteHttp = obtenerClienteHttp();
        
        control.verificarCancelacion();
        control.esperarSiPausada();

        ConstructorSolicitudesProveedor.SolicitudPreparada preparada = ConstructorSolicitudesProveedor
                .construirSolicitud(config, prompt, clienteHttp);
        Request solicitudHttp = preparada.request;
        
        gestorLogging.info(ORIGEN_LOG, I18nLogs.trf("Llamando a API: %s con modelo: %s",
                          preparada.endpoint, preparada.modeloUsado));
        
        if (Normalizador.noEsVacio(preparada.advertencia)) {
            gestorLogging.info(ORIGEN_LOG, preparada.advertencia);
        }
        
        if (registrarDetalleSolicitud) {
            gestorLogging.info(ORIGEN_LOG,
                I18nLogs.tr("Encabezados de solicitud: Content-Type=application/json, Authorization=Bearer [OCULTO]"));
        }

        Call call = clienteHttp.newCall(solicitudHttp);
        llamadaHttpActiva = call;

        try {
            try (Response respuesta = call.execute()) {
                int codigoRespuesta = respuesta.code();
                gestorLogging.info(ORIGEN_LOG, I18nLogs.trf("Código de respuesta de API: %d", codigoRespuesta));
                gestorLogging.info(ORIGEN_LOG, I18nLogs.trf("Encabezados de respuesta de API: %s", respuesta.headers()));

                if (!respuesta.isSuccessful()) {
                    String cuerpoError = "";
                    ResponseBody bodyError = respuesta.body();
                    if (bodyError != null) {
                        try {
                            cuerpoError = bodyError.string();
                        } catch (IOException e) {
                            gestorLogging.error(ORIGEN_LOG,
                                I18nLogs.tr("No se pudo leer cuerpo de error"), e);
                        }
                    }
                    String retryAfterHeader = respuesta.header("Retry-After");
                    String mensajeError = I18nLogs.trf("Error de API: %d - %s",
                            codigoRespuesta,
                            Normalizador.noEsVacio(cuerpoError) ? cuerpoError : I18nUI.Conexion.DETALLE_SIN_CUERPO());
                    
                    // Detectar error de contexto excedido
                    String proveedor = config.obtenerProveedorAI();
                    boolean esErrorContexto = DETECTOR_CONTEXTO.esErrorContextoExcedido(
                        proveedor, codigoRespuesta, cuerpoError);
                    
                    if (esErrorContexto) {
                        gestorLogging.info(ORIGEN_LOG, I18nLogs.ContextoExcedido.DETECTADO());
                        int limiteTokens = ContextExceededDetector.extraerLimiteTokens(cuerpoError);
                        if (limiteTokens > 0) {
                            gestorLogging.verbose(ORIGEN_LOG, I18nLogs.ContextoExcedido.LIMITE_EXTRAIDO(limiteTokens));
                        }
                    }
                    
                    throw new ApiHttpException(codigoRespuesta, cuerpoError, retryAfterHeader, mensajeError, esErrorContexto);
                }

                ResponseBody cuerpo = respuesta.body();
                if (cuerpo == null) {
                    throw new IOException(I18nLogs.tr("Respuesta de API sin cuerpo (null)"));
                }
                
                String cuerpoRespuesta = cuerpo.string();
                if (cuerpoRespuesta == null) {
                    cuerpoRespuesta = "";
                }
                
                gestorLogging.info(ORIGEN_LOG, I18nLogs.trf("Longitud de respuesta de API: %d caracteres",
                                  cuerpoRespuesta.length()));
                
                return cuerpoRespuesta;
            }
        } catch (ApiHttpException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(I18nUI.General.ERROR_INESPERADO_TIPO(e.getClass().getSimpleName()), e);
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

    /**
     * Cierra y libera todos los OkHttpClient cacheados. Debe llamarse al descargar la extensión
     * para evitar leaks de threads del dispatcher y connections del pool.
     * Idempotente y seguro para llamar múltiples veces.
     */
    public static void limpiarClientes() {
        synchronized (CLIENTES_HTTP_POR_TIMEOUT) {
            for (OkHttpClient cliente : CLIENTES_HTTP_POR_TIMEOUT.values()) {
                try {
                    cliente.dispatcher().executorService().shutdown();
                } catch (Exception ignored) {
                    // Best-effort: el shutdown del executor puede fallar si el JVM ya está cerrando.
                }
                try {
                    cliente.connectionPool().evictAll();
                } catch (Exception ignored) {
                    // Best-effort: evictAll no debería lanzar pero protegemos por si acaso.
                }
            }
            CLIENTES_HTTP_POR_TIMEOUT.clear();
        }
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
            // SECURITY (O3): el bypass de hostname verification se limita a
            // loopback / IP privadas. Para hosts cloud el verifier default
            // aplica normalmente — si un usuario tiene "Ignorar errores SSL"
            // activo para LM Studio local, sus keys cloud NO viajan a través
            // de un MITM por misconfig DNS.
            javax.net.ssl.HostnameVerifier defaultVerifier = javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier();
            builder.hostnameVerifier((hostname, session) -> {
                if (esLoopbackOLan(hostname)) {
                    return true;
                }
                return defaultVerifier.verify(hostname, session);
            });
        } catch (Exception e) {
            gestorLogging.error(ORIGEN_LOG,
                I18nUI.Conexion.LOG_SSL_INSECURE_ERROR(e.getClass().getSimpleName()));
        }
    }

    /**
     * Determina si un hostname corresponde a una IP loopback o de red privada
     * (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16) o el nombre "localhost".
     * Usado para limitar el bypass de hostname verification SSL (O3).
     */
    private static boolean esLoopbackOLan(String hostname) {
        if (hostname == null || hostname.isEmpty()) {
            return false;
        }
        String h = hostname.toLowerCase(java.util.Locale.ROOT);
        if ("localhost".equals(h) || h.endsWith(".localhost") || "127.0.0.1".equals(h) || "::1".equals(h)) {
            return true;
        }
        // IPv4 patterns: 10.x, 172.16-31.x, 192.168.x
        if (h.startsWith("10.")) {
            return true;
        }
        if (h.startsWith("192.168.")) {
            return true;
        }
        if (h.startsWith("172.")) {
            int dot1 = h.indexOf('.', 4);
            if (dot1 > 4) {
                try {
                    int segundo = Integer.parseInt(h.substring(4, dot1));
                    if (segundo >= 16 && segundo <= 31) {
                        return true;
                    }
                } catch (NumberFormatException ignored) {
                    // Not an IP — fall through
                }
            }
        }
        return false;
    }

    private void registrarFalloIntento(int intento, IOException error) {
        String falloMsg = PoliticaReintentos.obtenerMensajeErrorAmigable(error);
        gestorLogging.info(ORIGEN_LOG, I18nLogs.trf("Intento #%d falló: %s - %s",
                intento, error.getClass().getSimpleName(), falloMsg));
        
        if (error instanceof ApiHttpException) {
            ApiHttpException apiError = (ApiHttpException) error;
            String cuerpoError = apiError.obtenerCuerpoError();
            if (Normalizador.noEsVacio(cuerpoError)) {
                // SECURITY (O2): algunos providers embeben partial tokens /
                // session IDs en error bodies. Truncamos antes de loggear
                // para evitar volcar secrets en la consola de Burp.
                String cuerpoTruncado = truncarParaLog(cuerpoError, MAX_LONGITUD_CUERPO_ERROR_LOG);
                gestorLogging.error(ORIGEN_LOG, I18nLogs.trf("Cuerpo de respuesta de error de API: %s", cuerpoTruncado));
            }
        }
    }

    private static final int MAX_LONGITUD_CUERPO_ERROR_LOG = 500;

    /**
     * Trunca un cuerpo de respuesta para logging, agregando un sufijo que
     * indica truncate si aplica. Reduce el riesgo de loggear secrets que
     * algunos providers embeben en error bodies.
     */
    private static String truncarParaLog(String cuerpo, int maxLongitud) {
        if (cuerpo == null || cuerpo.length() <= maxLongitud) {
            return cuerpo;
        }
        return cuerpo.substring(0, maxLongitud) + "… [truncado, +" + (cuerpo.length() - maxLongitud) + " chars]";
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
