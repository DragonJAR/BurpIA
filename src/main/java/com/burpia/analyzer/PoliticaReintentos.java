package com.burpia.analyzer;

import com.burpia.i18n.I18nUI;
import com.burpia.util.Normalizador;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Set;

/**
 * Política centralizada de reintentos para llamadas a APIs de IA.
 * <p>
 * Esta clase implementa el principio DRY centralizando toda la lógica de:
 * </p>
 * <ul>
 *   <li>Clasificación de códigos HTTP reintentables vs no reintentables</li>
 *   <li>Clasificación de excepciones de red reintentables</li>
 *   <li>Cálculo de tiempos de espera con backoff y jitter</li>
 *   <li>Generación de mensajes de error amigables</li>
 * </ul>
 *
 * @see <a href="https://github.com/DragonJAR/BurpIA/blob/main/AGENTS.md">Guía de codificación AGENTS.md</a>
 */
public final class PoliticaReintentos {

    /** Códigos HTTP que siempre deben reintentarse (errores de servidor y rate limiting). */
    private static final Set<Integer> CODIGOS_REINTENTABLES_SIEMPRE = Set.of(
        408,
        425,
        429,
        500,
        502,
        503,
        504
    );

    /** Códigos HTTP que generalmente no deben reintentarse (errores de cliente). */
    private static final Set<Integer> CODIGOS_NO_REINTENTABLES = Set.of(
        400,
        401,
        403,
        404,
        405,
        410,
        422
    );

    /** Tiempo mínimo de espera entre reintentos en milisegundos. */
    private static final long ESPERA_MINIMA_MS = 1000L;

    private PoliticaReintentos() {
        // Clase de utilidad, no instanciable
    }

    /**
     * Genera mensaje localizado de timeout agotado.
     *
     * @return mensaje de timeout en el idioma configurado
     */
    private static String mensajeTimeoutAgotado() {
        return I18nUI.tr("Tiempo de espera agotado, intenta aumentarlo en los ajustes",
            "Timeout reached, try increasing it in the settings");
    }

    /**
     * Normaliza el cuerpo del error a minúsculas para comparación.
     * <p>
     * Retorna cadena vacía si el cuerpo es null o está vacío.
     * </p>
     *
     * @param cuerpoError cuerpo del error HTTP, puede ser {@code null}
     * @return cuerpo normalizado en minúsculas o cadena vacía
     */
    private static String normalizarCuerpoError(String cuerpoError) {
        if (Normalizador.esVacio(cuerpoError)) {
            return "";
        }
        return cuerpoError.toLowerCase(Locale.ROOT);
    }

    /**
     * Determina si un código de estado HTTP NO debe reintentarse.
     * <p>
     * Algunos códigos de error de cliente (4xx) tienen excepciones donde
     * sí se podría reintentar (ej. rate limiting temporal).
     * </p>
     *
     * @param statusCode código de estado HTTP
     * @param cuerpoError cuerpo de la respuesta de error, puede ser {@code null}
     * @return {@code true} si NO se debe reintentar, {@code false} si se puede reintentar
     */
    public static boolean esCodigoNoReintentable(int statusCode, String cuerpoError) {
        if (CODIGOS_NO_REINTENTABLES.contains(statusCode)) {
            String error = normalizarCuerpoError(cuerpoError);
            // 400 y 404 pueden ser errores de configuración permanentes
            if (statusCode == 400 || statusCode == 404) {
                return error.contains("model is required")
                    || error.contains("not found for api version")
                    || error.contains("invalid_request_error")
                    || error.contains("does not exist");
            }
            return true;
        }

        // 409 Conflict puede ser temporal (rate limit disfrazado)
        if (statusCode == 409) {
            String error = normalizarCuerpoError(cuerpoError);
            // Si contiene indicadores de problema temporal, ES reintentable (retornar false)
            return !(error.contains("rate limit")
                || error.contains("try again")
                || error.contains("temporarily")
                || error.contains("concurrent"));
        }

        return false;
    }

    /**
     * Determina si un código de estado HTTP debe reintentarse.
     *
     * @param statusCode código de estado HTTP
     * @return {@code true} si se debe reintentar, {@code false} en caso contrario
     */
    public static boolean esCodigoReintentable(int statusCode) {
        if (CODIGOS_REINTENTABLES_SIEMPRE.contains(statusCode)) {
            return true;
        }
        return statusCode == 409;
    }

    /**
     * Determina si una excepción de I/O debe reintentarse.
     * <p>
     * Se consideran reintentables las excepciones relacionadas con:
     * </p>
     * <ul>
     *   <li>Timeouts (SocketTimeoutException)</li>
     *   <li>Conexiones rechazadas o reseteadas</li>
     *   <li>Servicios temporalmente no disponibles</li>
     *   <li>Excepciones genéricas sin mensaje (posibles problemas de red)</li>
     * </ul>
     *
     * @param excepcion excepción de I/O, puede ser {@code null}
     * @return {@code true} si se debe reintentar, {@code false} en caso contrario
     */
    public static boolean esExcepcionReintentable(IOException excepcion) {
        if (excepcion == null || Thread.currentThread().isInterrupted()) {
            return false;
        }
        if (excepcion instanceof SocketTimeoutException) {
            return true;
        }
        String mensaje = excepcion.getMessage();
        // Excepciones sin mensaje se asumen como problemas de red transitorios
        if (Normalizador.esVacio(mensaje)) {
            return true;
        }
        String normalizado = mensaje.toLowerCase(Locale.ROOT);
        return normalizado.contains("timeout")
            || normalizado.contains("connection reset")
            || normalizado.contains("refused")
            || normalizado.contains("temporarily unavailable");
    }

    /**
     * Genera un mensaje de error amigable para el usuario.
     * <p>
     * Desenvuelve excepciones anidadas y traduce errores técnicos comunes
     * a mensajes comprensibles.
     * </p>
     *
     * @param e excepción original, puede ser {@code null}
     * @return mensaje amigable para mostrar al usuario
     */
    public static String obtenerMensajeErrorAmigable(Throwable e) {
        if (e == null) {
            return I18nUI.tr("Error desconocido", "Unknown error");
        }

        Throwable actual = e;

        // Desenvolver excepciones wrapper comunes
        if (actual.getCause() != null && (actual instanceof IOException || actual instanceof RuntimeException)) {
            actual = actual.getCause();
        }

        if (actual instanceof SocketTimeoutException) {
            return mensajeTimeoutAgotado();
        }

        String msg = actual.getMessage();
        if (Normalizador.esVacio(msg)) {
            return actual.getClass().getSimpleName();
        }

        if (msg.contains("timeout") || msg.contains("timed out")) {
            return mensajeTimeoutAgotado();
        }

        return msg;
    }

    /**
     * Calcula el tiempo de espera antes del próximo reintento.
     * <p>
     * La estrategia combina:
     * </p>
     * <ul>
     *   <li>Backoff exponencial normalizado (mínimo {@value #ESPERA_MINIMA_MS}ms)</li>
     *   <li>Respeto del header Retry-After para códigos 429</li>
     *   <li>Jitter determinístico basado en el número de intento para evitar tormentas</li>
     * </ul>
     *
     * @param statusCode código de estado HTTP, usar -1 para excepciones de red
     * @param retryAfterHeader valor del header Retry-After, puede ser {@code null}
     * @param backoffActualMs tiempo de backoff actual en milisegundos
     * @param intentoActual número de intento actual (1-based)
     * @return tiempo de espera en milisegundos
     */
    public static long calcularEsperaMs(int statusCode, String retryAfterHeader, long backoffActualMs, int intentoActual) {
        long backoffNormalizado = Math.max(ESPERA_MINIMA_MS, backoffActualMs);

        // Para rate limiting, respetar el header del servidor
        if (statusCode == 429 && Normalizador.noEsVacio(retryAfterHeader)) {
            long esperaDesdeHeader = parsearRetryAfterMs(retryAfterHeader, System.currentTimeMillis());
            if (esperaDesdeHeader > 0) {
                return esperaDesdeHeader;
            }
        }

        // Jitter determinístico: 100-500ms basado en backoff, con sesgo según intento par/impar
        long jitterDeterministico = Math.min(500L, Math.max(100L, backoffNormalizado / 5L));
        long sesgo = (Math.max(1, intentoActual) % 2 == 0) ? jitterDeterministico : jitterDeterministico / 2L;
        return backoffNormalizado + sesgo;
    }

    /**
     * Parsea el header HTTP Retry-After a milisegundos.
     * <p>
     * Soporta dos formatos:
     * </p>
     * <ul>
     *   <li>Entero en segundos (ej. "120")</li>
     *   <li>Fecha HTTP RFC 1123 (ej. "Wed, 21 Oct 2015 07:28:00 GMT")</li>
     * </ul>
     *
     * @param retryAfterHeader valor del header, puede ser {@code null}
     * @param ahoraMs timestamp actual en milisegundos
     * @return tiempo de espera en milisegundos, o 0 si no se puede parsear
     */
    static long parsearRetryAfterMs(String retryAfterHeader, long ahoraMs) {
        if (Normalizador.esVacio(retryAfterHeader)) {
            return 0L;
        }

        String valor = retryAfterHeader.trim();

        try {
            long segundos = Long.parseLong(valor);
            if (segundos <= 0L) {
                return 0L;
            }
            return Math.max(ESPERA_MINIMA_MS, segundos * 1000L);
        } catch (NumberFormatException ignored) {
            // No es un número, intentar parsear como fecha
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
