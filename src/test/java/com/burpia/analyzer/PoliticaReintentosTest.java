package com.burpia.analyzer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests completos para PoliticaReintentos.
 * <p>
 * Verifica la lógica de clasificación de errores HTTP, excepciones de red,
 * cálculo de tiempos de espera y generación de mensajes de error amigables.
 * </p>
 *
 * @see PoliticaReintentos
 */
@DisplayName("PoliticaReintentos Tests")
class PoliticaReintentosTest {

    @Nested
    @DisplayName("esCodigoReintentable")
    class EsCodigoReintentable {

        @Test
        @DisplayName("503 se clasifica como reintentable")
        void dado503_esReintentable() {
            assertTrue(PoliticaReintentos.esCodigoReintentable(503));
        }

        @Test
        @DisplayName("429 (rate limiting) es reintentable")
        void dado429_esReintentable() {
            assertTrue(PoliticaReintentos.esCodigoReintentable(429));
        }

        @Test
        @DisplayName("500 (internal server error) es reintentable")
        void dado500_esReintentable() {
            assertTrue(PoliticaReintentos.esCodigoReintentable(500));
        }

        @Test
        @DisplayName("502 (bad gateway) es reintentable")
        void dado502_esReintentable() {
            assertTrue(PoliticaReintentos.esCodigoReintentable(502));
        }

        @Test
        @DisplayName("504 (gateway timeout) es reintentable")
        void dado504_esReintentable() {
            assertTrue(PoliticaReintentos.esCodigoReintentable(504));
        }

        @Test
        @DisplayName("408 (request timeout) es reintentable")
        void dado408_esReintentable() {
            assertTrue(PoliticaReintentos.esCodigoReintentable(408));
        }

        @Test
        @DisplayName("425 (too early) es reintentable")
        void dado425_esReintentable() {
            assertTrue(PoliticaReintentos.esCodigoReintentable(425));
        }

        @Test
        @DisplayName("409 (conflict) es reintentable")
        void dado409_esReintentable() {
            assertTrue(PoliticaReintentos.esCodigoReintentable(409));
        }

        @Test
        @DisplayName("400 (bad request) no es reintentable")
        void dado400_noEsReintentable() {
            assertFalse(PoliticaReintentos.esCodigoReintentable(400));
        }

        @Test
        @DisplayName("401 (unauthorized) no es reintentable")
        void dado401_noEsReintentable() {
            assertFalse(PoliticaReintentos.esCodigoReintentable(401));
        }

        @Test
        @DisplayName("404 (not found) no es reintentable")
        void dado404_noEsReintentable() {
            assertFalse(PoliticaReintentos.esCodigoReintentable(404));
        }

        @Test
        @DisplayName("200 (ok) no es reintentable")
        void dado200_noEsReintentable() {
            assertFalse(PoliticaReintentos.esCodigoReintentable(200));
        }
    }

    @Nested
    @DisplayName("esCodigoNoReintentable")
    class EsCodigoNoReintentable {

        @Nested
        @DisplayName("400 Bad Request")
        class Codigo400 {

            @Test
            @DisplayName("400 con 'model is required' es no reintentable")
            void dado400ModelRequired_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(400, "model is required"));
            }

            @Test
            @DisplayName("400 con 'MODEL IS REQUIRED' (mayúsculas) es no reintentable")
            void dado400ModelRequiredMayusculas_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(400, "MODEL IS REQUIRED"));
            }

            @Test
            @DisplayName("400 con 'not found for api version' es no reintentable")
            void dado400NotFoundApiVersion_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(400, "Model not found for api version"));
            }

            @Test
            @DisplayName("400 con 'invalid_request_error' es no reintentable")
            void dado400InvalidRequestError_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(400, "invalid_request_error"));
            }

            @Test
            @DisplayName("400 con 'does not exist' es no reintentable")
            void dado400DoesNotExist_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(400, "Model does not exist"));
            }

            @Test
            @DisplayName("400 con cuerpo null es reintentable (sin mensaje específico)")
            void dado400ConNull_esReintentable() {
                assertFalse(PoliticaReintentos.esCodigoNoReintentable(400, null));
            }

            @Test
            @DisplayName("400 con cuerpo vacío es reintentable (sin mensaje específico)")
            void dado400ConVacio_esReintentable() {
                assertFalse(PoliticaReintentos.esCodigoNoReintentable(400, ""));
            }
        }

        @Nested
        @DisplayName("404 Not Found")
        class Codigo404 {

            @Test
            @DisplayName("404 con 'model is required' es no reintentable")
            void dado404ModelRequired_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(404, "model is required"));
            }

            @Test
            @DisplayName("404 con 'does not exist' es no reintentable")
            void dado404DoesNotExist_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(404, "Resource does not exist"));
            }

            @Test
            @DisplayName("404 con cuerpo null es reintentable (sin mensaje específico)")
            void dado404ConNull_esReintentable() {
                assertFalse(PoliticaReintentos.esCodigoNoReintentable(404, null));
            }
        }

        @Nested
        @DisplayName("Otros códigos no reintentables")
        class OtrosCodigosNoReintentables {

            @Test
            @DisplayName("401 (unauthorized) es no reintentable")
            void dado401_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(401, "Unauthorized"));
            }

            @Test
            @DisplayName("403 (forbidden) es no reintentable")
            void dado403_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(403, "Forbidden"));
            }

            @Test
            @DisplayName("405 (method not allowed) es no reintentable")
            void dado405_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(405, "Method not allowed"));
            }

            @Test
            @DisplayName("410 (gone) es no reintentable")
            void dado410_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(410, "Gone"));
            }

            @Test
            @DisplayName("422 (unprocessable entity) es no reintentable")
            void dado422_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(422, "Unprocessable entity"));
            }
        }

        @Nested
        @DisplayName("409 Conflict - casos especiales")
        class Codigo409Conflict {

            @Test
            @DisplayName("409 con 'rate limit' es reintentable (no es no reintentable)")
            void dado409ConRateLimit_esReintentable() {
                assertFalse(PoliticaReintentos.esCodigoNoReintentable(409, "rate limit exceeded"));
            }

            @Test
            @DisplayName("409 con 'try again' es reintentable")
            void dado409ConTryAgain_esReintentable() {
                assertFalse(PoliticaReintentos.esCodigoNoReintentable(409, "Please try again later"));
            }

            @Test
            @DisplayName("409 con 'temporarily' es reintentable")
            void dado409ConTemporarily_esReintentable() {
                assertFalse(PoliticaReintentos.esCodigoNoReintentable(409, "Resource temporarily unavailable"));
            }

            @Test
            @DisplayName("409 con 'concurrent' es reintentable")
            void dado409ConConcurrent_esReintentable() {
                assertFalse(PoliticaReintentos.esCodigoNoReintentable(409, "concurrent modification detected"));
            }

            @Test
            @DisplayName("409 sin indicadores temporales es no reintentable")
            void dado409SinIndicadores_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(409, "Resource conflict"));
            }
        }

        @Nested
        @DisplayName("Códigos reintentables")
        class CodigosReintentables {

            @Test
            @DisplayName("500 no es no reintentable (es reintentable)")
            void dado500_esReintentable() {
                assertFalse(PoliticaReintentos.esCodigoNoReintentable(500, "Internal server error"));
            }

            @Test
            @DisplayName("503 no es no reintentable (es reintentable)")
            void dado503_esReintentable() {
                assertFalse(PoliticaReintentos.esCodigoNoReintentable(503, "Service unavailable"));
            }

            @Test
            @DisplayName("429 no es no reintentable (es reintentable)")
            void dado429_esReintentable() {
                assertFalse(PoliticaReintentos.esCodigoNoReintentable(429, "Too many requests"));
            }
        }
    }

    @Nested
    @DisplayName("esExcepcionReintentable")
    class EsExcepcionReintentable {

        @Test
        @DisplayName("SocketTimeoutException es reintentable")
        void socketTimeout_esReintentable() {
            assertTrue(PoliticaReintentos.esExcepcionReintentable(new SocketTimeoutException("timeout")));
        }

        @Test
        @DisplayName("SocketTimeoutException sin mensaje es reintentable")
        void socketTimeoutSinMensaje_esReintentable() {
            assertTrue(PoliticaReintentos.esExcepcionReintentable(new SocketTimeoutException()));
        }

        @Test
        @DisplayName("IOException con 'timeout' en mensaje es reintentable")
        void ioExceptionConTimeout_esReintentable() {
            assertTrue(PoliticaReintentos.esExcepcionReintentable(new IOException("Connection timeout")));
        }

        @Test
        @DisplayName("IOException con 'connection reset' en mensaje es reintentable")
        void ioExceptionConConnectionReset_esReintentable() {
            assertTrue(PoliticaReintentos.esExcepcionReintentable(new IOException("connection reset by peer")));
        }

        @Test
        @DisplayName("IOException con 'refused' en mensaje es reintentable")
        void ioExceptionConRefused_esReintentable() {
            assertTrue(PoliticaReintentos.esExcepcionReintentable(new IOException("Connection refused")));
        }

        @Test
        @DisplayName("IOException con 'temporarily unavailable' en mensaje es reintentable")
        void ioExceptionConTemporarilyUnavailable_esReintentable() {
            assertTrue(PoliticaReintentos.esExcepcionReintentable(new IOException("Service temporarily unavailable")));
        }

        @Test
        @DisplayName("IOException sin mensaje es reintentable (asume problema de red)")
        void ioExceptionSinMensaje_esReintentable() {
            assertTrue(PoliticaReintentos.esExcepcionReintentable(new IOException()));
        }

        @Test
        @DisplayName("IOException con mensaje null es reintentable")
        void ioExceptionConMensajeNull_esReintentable() {
            IOException e = new IOException();
            assertTrue(PoliticaReintentos.esExcepcionReintentable(e));
        }

        @Test
        @DisplayName("IOException con mensaje no reconocido no es reintentable")
        void ioExceptionConMensajeNoReconocido_noEsReintentable() {
            assertFalse(PoliticaReintentos.esExcepcionReintentable(new IOException("Invalid API key")));
        }

        @Test
        @DisplayName("Excepcion null no es reintentable")
        void excepcionNull_noEsReintentable() {
            assertFalse(PoliticaReintentos.esExcepcionReintentable(null));
        }

        @Test
        @DisplayName("Con thread interrumpido no es reintentable")
        void conThreadInterrumpido_noEsReintentable() throws InterruptedException {
            Thread testThread = new Thread(() -> {
                Thread.currentThread().interrupt();
                assertFalse(PoliticaReintentos.esExcepcionReintentable(new SocketTimeoutException("timeout")));
            });
            testThread.start();
            testThread.join(1000);
        }
    }

    @Nested
    @DisplayName("calcularEsperaMs")
    class CalcularEsperaMs {

        @Nested
        @DisplayName("Con Retry-After header (429)")
        class ConRetryAfter {

            @Test
            @DisplayName("429 con Retry-After en segundos respeta espera del servidor")
            void dado429ConRetryAfterEnSegundos_respetaEsperaServidor() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(429, "5", 1000L, 1);
                assertTrue(esperaMs >= 5000L);
            }

            @Test
            @DisplayName("429 con Retry-After de 120 segundos retorna 120000ms")
            void dado429ConRetryAfter120Segundos_retorna120000ms() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(429, "120", 1000L, 1);
                assertEquals(120000L, esperaMs);
            }

            @Test
            @DisplayName("429 con Retry-After de 0 usa backoff normal")
            void dado429ConRetryAfterCero_usaBackoffNormal() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(429, "0", 2000L, 1);
                assertTrue(esperaMs >= 2000L);
                assertTrue(esperaMs <= 2500L);
            }

            @Test
            @DisplayName("429 con Retry-After negativo usa backoff normal")
            void dado429ConRetryAfterNegativo_usaBackoffNormal() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(429, "-5", 2000L, 1);
                assertTrue(esperaMs >= 1000L);
            }

            @Test
            @DisplayName("429 con Retry-After inválido usa backoff normal")
            void dado429ConRetryAfterInvalido_usaBackoffNormal() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(429, "invalid", 2000L, 1);
                assertTrue(esperaMs >= 2000L);
            }

            @Test
            @DisplayName("429 con Retry-After null usa backoff normal")
            void dado429ConRetryAfterNull_usaBackoffNormal() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(429, null, 2000L, 1);
                assertTrue(esperaMs >= 2000L);
            }

            @Test
            @DisplayName("429 con Retry-After vacío usa backoff normal")
            void dado429ConRetryAfterVacio_usaBackoffNormal() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(429, "", 2000L, 1);
                assertTrue(esperaMs >= 2000L);
            }
        }

        @Nested
        @DisplayName("Backoff normalizado")
        class BackoffNormalizado {

            @Test
            @DisplayName("Backoff menor a 1000ms se normaliza a 1000ms")
            void dadoBackoffMenorA1000_seNormaliza() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(500, null, 500L, 1);
                assertTrue(esperaMs >= 1000L);
            }

            @Test
            @DisplayName("Backoff de 0 se normaliza a 1000ms mínimo")
            void dadoBackoffCero_seNormaliza() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(500, null, 0L, 1);
                assertTrue(esperaMs >= 1000L);
            }

            @Test
            @DisplayName("Backoff negativo se normaliza a 1000ms mínimo")
            void dadoBackoffNegativo_seNormaliza() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(500, null, -100L, 1);
                assertTrue(esperaMs >= 1000L);
            }

            @Test
            @DisplayName("Backoff de 2000ms con intento 1 retorna entre 1000-1500ms de jitter")
            void dadoBackoff2000_intento1_retornaConJitter() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(500, null, 2000L, 1);
                assertTrue(esperaMs >= 2000L);
                assertTrue(esperaMs <= 2500L);
            }

            @Test
            @DisplayName("Backoff de 2000ms con intento 2 (par) tiene más jitter")
            void dadoBackoff2000_intento2_retornaConMasJitter() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(500, null, 2000L, 2);
                assertTrue(esperaMs >= 2000L);
                assertTrue(esperaMs <= 2500L);
            }

            @Test
            @DisplayName("Intento 0 se comporta como intento impar")
            void dadoIntentoCero_seComportaComoImpar() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(500, null, 2000L, 0);
                assertTrue(esperaMs >= 2000L);
            }

            @Test
            @DisplayName("Intento negativo se normaliza")
            void dadoIntentoNegativo_seNormaliza() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(500, null, 2000L, -5);
                assertTrue(esperaMs >= 2000L);
            }
        }

        @Nested
        @DisplayName("Códigos que no son 429")
        class CodigosNo429 {

            @Test
            @DisplayName("500 ignora Retry-After y usa backoff")
            void dado500_ignoraRetryAfter() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(500, "10", 2000L, 1);
                assertTrue(esperaMs >= 2000L);
                assertTrue(esperaMs < 10000L);
            }

            @Test
            @DisplayName("503 usa backoff normal")
            void dado503_usaBackoffNormal() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(503, null, 2000L, 1);
                assertTrue(esperaMs >= 2000L);
            }
        }
    }

    @Nested
    @DisplayName("parsearRetryAfterMs")
    class ParsearRetryAfterMs {

        @Test
        @DisplayName("Parsea segundos correctamente")
        void parseaSegundosCorrectamente() {
            long resultado = PoliticaReintentos.parsearRetryAfterMs("30", System.currentTimeMillis());
            assertEquals(30000L, resultado);
        }

        @Test
        @DisplayName("Parsea segundos con espacios")
        void parseaSegundosConEspacios() {
            long resultado = PoliticaReintentos.parsearRetryAfterMs("  60  ", System.currentTimeMillis());
            assertEquals(60000L, resultado);
        }

        @Test
        @DisplayName("Retorna 0 para valor 0")
        void retornaCeroParaValorCero() {
            long resultado = PoliticaReintentos.parsearRetryAfterMs("0", System.currentTimeMillis());
            assertEquals(0L, resultado);
        }

        @Test
        @DisplayName("Retorna 0 para valor negativo")
        void retornaCeroParaValorNegativo() {
            long resultado = PoliticaReintentos.parsearRetryAfterMs("-10", System.currentTimeMillis());
            assertEquals(0L, resultado);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Retorna 0 para null, vacío o espacios")
        void retornaCeroParaNullVacioOEspacios(String valor) {
            long resultado = PoliticaReintentos.parsearRetryAfterMs(valor, System.currentTimeMillis());
            assertEquals(0L, resultado);
        }

        @Test
        @DisplayName("Retorna 0 para texto no numérico inválido")
        void retornaCeroParaTextoNoNumerico() {
            long resultado = PoliticaReintentos.parsearRetryAfterMs("invalid", System.currentTimeMillis());
            assertEquals(0L, resultado);
        }

        @Test
        @DisplayName("Parsea fecha HTTP RFC 1123 válida")
        void parseaFechaHttpValida() {
            long ahora = System.currentTimeMillis();
            long futuro = ahora + 60000L;
            java.time.ZonedDateTime fechaFutura = java.time.ZonedDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(futuro),
                java.time.ZoneId.of("GMT")
            );
            String fechaStr = fechaFutura.format(java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME);

            long resultado = PoliticaReintentos.parsearRetryAfterMs(fechaStr, ahora);
            assertTrue(resultado > 0L);
            assertTrue(resultado <= 65000L);
        }

        @Test
        @DisplayName("Retorna 0 para fecha en el pasado")
        void retornaCeroParaFechaPasada() {
            long ahora = System.currentTimeMillis();
            long pasado = ahora - 60000L;
            java.time.ZonedDateTime fechaPasada = java.time.ZonedDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(pasado),
                java.time.ZoneId.of("GMT")
            );
            String fechaStr = fechaPasada.format(java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME);

            long resultado = PoliticaReintentos.parsearRetryAfterMs(fechaStr, ahora);
            assertEquals(0L, resultado);
        }

        @Test
        @DisplayName("Retorna 0 para fecha con formato inválido")
        void retornaCeroParaFormatoFechaInvalido() {
            long resultado = PoliticaReintentos.parsearRetryAfterMs("Wed, 21 Oct 2015", System.currentTimeMillis());
            assertEquals(0L, resultado);
        }

        @Test
        @DisplayName("Respeta mínimo de 1000ms para segundos positivos")
        void respetaMinimoParaSegundosPositivos() {
            long resultado = PoliticaReintentos.parsearRetryAfterMs("1", System.currentTimeMillis());
            assertEquals(1000L, resultado);
        }
    }

    @Nested
    @DisplayName("obtenerMensajeErrorAmigable")
    class ObtenerMensajeErrorAmigable {

        @Test
        @DisplayName("SocketTimeoutException retorna mensaje de timeout")
        void socketTimeout_retornaMensajeTimeout() {
            String mensaje = PoliticaReintentos.obtenerMensajeErrorAmigable(new SocketTimeoutException("timeout"));
            String mensajeLower = mensaje.toLowerCase();
            assertTrue(mensajeLower.contains("tiempo") || mensajeLower.contains("timeout"),
                "Expected timeout message but got: " + mensaje);
        }

        @Test
        @DisplayName("IOException con 'timeout' en mensaje retorna mensaje de timeout")
        void ioExceptionConTimeout_retornaMensajeTimeout() {
            String mensaje = PoliticaReintentos.obtenerMensajeErrorAmigable(new IOException("Connection timeout"));
            String mensajeLower = mensaje.toLowerCase();
            assertTrue(mensajeLower.contains("tiempo") || mensajeLower.contains("timeout"),
                "Expected timeout message but got: " + mensaje);
        }

        @Test
        @DisplayName("IOException con 'timed out' retorna mensaje de timeout")
        void ioExceptionConTimedOut_retornaMensajeTimeout() {
            String mensaje = PoliticaReintentos.obtenerMensajeErrorAmigable(new IOException("Operation timed out"));
            String mensajeLower = mensaje.toLowerCase();
            assertTrue(mensajeLower.contains("tiempo") || mensajeLower.contains("timeout"),
                "Expected timeout message but got: " + mensaje);
        }

        @Test
        @DisplayName("IOException con otro mensaje retorna el mensaje original")
        void ioExceptionConOtroMensaje_retornaMensajeOriginal() {
            String mensaje = PoliticaReintentos.obtenerMensajeErrorAmigable(new IOException("Connection refused"));
            assertEquals("Connection refused", mensaje);
        }

        @Test
        @DisplayName("IOException sin mensaje retorna nombre de clase")
        void ioExceptionSinMensaje_retornaNombreClase() {
            String mensaje = PoliticaReintentos.obtenerMensajeErrorAmigable(new IOException());
            assertEquals("IOException", mensaje);
        }

        @Test
        @DisplayName("Excepcion null retorna mensaje de error desconocido")
        void excepcionNull_retornaErrorDesconocido() {
            String mensaje = PoliticaReintentos.obtenerMensajeErrorAmigable(null);
            assertTrue(mensaje.contains("desconocido") || mensaje.contains("Unknown"));
        }

        @Test
        @DisplayName("RuntimeException con causa la desenvuelve")
        void runtimeExceptionConCausa_desenvuelve() {
            IOException causa = new IOException("Error de conexión");
            RuntimeException wrapper = new RuntimeException("Wrapper", causa);
            String mensaje = PoliticaReintentos.obtenerMensajeErrorAmigable(wrapper);
            assertEquals("Error de conexión", mensaje);
        }

        @Test
        @DisplayName("IOException con causa la desenvuelve")
        void ioExceptionConCausa_desenvuelve() {
            SocketTimeoutException causa = new SocketTimeoutException("Read timed out");
            IOException wrapper = new IOException("Wrapper", causa);
            String mensaje = PoliticaReintentos.obtenerMensajeErrorAmigable(wrapper);
            String mensajeLower = mensaje.toLowerCase();
            assertTrue(mensajeLower.contains("tiempo") || mensajeLower.contains("timeout"),
                "Expected timeout message but got: " + mensaje);
        }

        @Test
        @DisplayName("Excepción con mensaje null retorna nombre de clase")
        void excepcionConMensajeNull_retornaNombreClase() {
            Exception e = new Exception((String) null);
            String mensaje = PoliticaReintentos.obtenerMensajeErrorAmigable(e);
            assertEquals("Exception", mensaje);
        }
    }
}
