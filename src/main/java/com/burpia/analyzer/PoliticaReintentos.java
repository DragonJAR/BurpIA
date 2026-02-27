package com.burpia.analyzer;
import com.burpia.i18n.I18nUI;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Set;

public final class PoliticaReintentos {

    private static final Set<Integer> CODIGOS_REINTENTABLES_SIEMPRE = Set.of(
        408,
        425,
        429,
        500,
        502,
        503,
        504
    );

    private static final Set<Integer> CODIGOS_NO_REINTENTABLES = Set.of(
        400,
        401,
        403,
        404,
        405,
        410,
        422
    );
    
    private static final long ESPERA_MINIMA_MS = 1000L;

    private PoliticaReintentos() {
    }

    public static boolean esCodigoNoReintentable(int statusCode, String cuerpoError) {
        if (CODIGOS_NO_REINTENTABLES.contains(statusCode)) {
            String error = cuerpoError != null ? cuerpoError.toLowerCase(Locale.ROOT) : "";
            if (statusCode == 400 || statusCode == 404) {
                return error.contains("model is required")
                    || error.contains("not found for api version")
                    || error.contains("invalid_request_error")
                    || error.contains("does not exist");
            }
            return true;
        }

        if (statusCode == 409) {
            String error = cuerpoError != null ? cuerpoError.toLowerCase(Locale.ROOT) : "";
            return !(error.contains("rate limit") || 
                     error.contains("try again") ||
                     error.contains("temporarily") ||
                     error.contains("concurrent"));
        }
        
        return false;
    }

    public static boolean esCodigoReintentable(int statusCode) {
        if (CODIGOS_REINTENTABLES_SIEMPRE.contains(statusCode)) {
            return true;
        }
        return statusCode == 409;
    }

    public static boolean esExcepcionReintentable(IOException excepcion) {
        if (excepcion == null || Thread.currentThread().isInterrupted()) {
            return false;
        }
        if (excepcion instanceof SocketTimeoutException) {
            return true;
        }
        String mensaje = excepcion.getMessage();
        if (mensaje == null) {
            return true;
        }
        String normalizado = mensaje.toLowerCase(Locale.ROOT);
        return normalizado.contains("timeout")
            || normalizado.contains("connection reset")
            || normalizado.contains("refused")
            || normalizado.contains("temporarily unavailable");
    }

    public static String obtenerMensajeErrorAmigable(Throwable e) {
        if (e == null) {
            return I18nUI.tr("Error desconocido", "Unknown error");
        }

        Throwable actual = e;

        if (actual.getCause() != null && (actual instanceof IOException || actual instanceof RuntimeException)) {
            actual = actual.getCause();
        }

        if (actual instanceof SocketTimeoutException) {
            return I18nUI.tr("Tiempo de espera agotado, intenta aumentarlo en los ajustes",
                "Timeout reached, try increasing it in the settings");
        }

        String msg = actual.getMessage();
        if (msg == null || msg.isEmpty()) {
            return actual.getClass().getSimpleName();
        }

        if (msg.contains("timeout") || msg.contains("timed out")) {
            return I18nUI.tr("Tiempo de espera agotado, intenta aumentarlo en los ajustes",
                "Timeout reached, try increasing it in the settings");
        }

        return msg;
    }

    public static long calcularEsperaMs(int statusCode, String retryAfterHeader, long backoffActualMs, int intentoActual) {
        long backoffNormalizado = Math.max(ESPERA_MINIMA_MS, backoffActualMs);

        if (statusCode == 429) {
            long esperaDesdeHeader = parsearRetryAfterMs(retryAfterHeader, System.currentTimeMillis());
            if (esperaDesdeHeader > 0) {
                return esperaDesdeHeader;
            }
        }

        long jitterDeterministico = Math.min(500L, Math.max(100L, backoffNormalizado / 5L));
        long sesgo = (Math.max(1, intentoActual) % 2 == 0) ? jitterDeterministico : jitterDeterministico / 2L;
        return backoffNormalizado + sesgo;
    }

    static long parsearRetryAfterMs(String retryAfterHeader, long ahoraMs) {
        if (retryAfterHeader == null) {
            return 0L;
        }
        String valor = retryAfterHeader.trim();
        if (valor.isEmpty()) {
            return 0L;
        }

        try {
            long segundos = Long.parseLong(valor);
            if (segundos <= 0L) {
                return 0L;
            }
            return Math.max(ESPERA_MINIMA_MS, segundos * 1000L);
        } catch (NumberFormatException ignored) {
        }

        try {
            ZonedDateTime fecha = ZonedDateTime.parse(valor, DateTimeFormatter.RFC_1123_DATE_TIME);
            long deltaMs = Duration.between(java.time.Instant.ofEpochMilli(ahoraMs), fecha.toInstant()).toMillis();
            return Math.max(0L, deltaMs);
        } catch (DateTimeParseException ignored) {
            return 0L;
        }
    }
}
