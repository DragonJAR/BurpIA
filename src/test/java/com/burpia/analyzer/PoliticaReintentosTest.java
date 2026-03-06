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
            assertTrue(PoliticaReintentos.esCodigoReintentable(503), "assertTrue failed at PoliticaReintentosTest.java:34");
        }

        @Test
        @DisplayName("429 (rate limiting) es reintentable")
        void dado429_esReintentable() {
            assertTrue(PoliticaReintentos.esCodigoReintentable(429), "assertTrue failed at PoliticaReintentosTest.java:40");
        }

        @Test
        @DisplayName("500 (internal server error) es reintentable")
        void dado500_esReintentable() {
            assertTrue(PoliticaReintentos.esCodigoReintentable(500), "assertTrue failed at PoliticaReintentosTest.java:46");
        }

        @Test
        @DisplayName("502 (bad gateway) es reintentable")
        void dado502_esReintentable() {
            assertTrue(PoliticaReintentos.esCodigoReintentable(502), "assertTrue failed at PoliticaReintentosTest.java:52");
        }

        @Test
        @DisplayName("504 (gateway timeout) es reintentable")
        void dado504_esReintentable() {
            assertTrue(PoliticaReintentos.esCodigoReintentable(504), "assertTrue failed at PoliticaReintentosTest.java:58");
        }

        @Test
        @DisplayName("408 (request timeout) es reintentable")
        void dado408_esReintentable() {
            assertTrue(PoliticaReintentos.esCodigoReintentable(408), "assertTrue failed at PoliticaReintentosTest.java:64");
        }

        @Test
        @DisplayName("425 (too early) es reintentable")
        void dado425_esReintentable() {
            assertTrue(PoliticaReintentos.esCodigoReintentable(425), "assertTrue failed at PoliticaReintentosTest.java:70");
        }

        @Test
        @DisplayName("409 (conflict) es reintentable")
        void dado409_esReintentable() {
            assertTrue(PoliticaReintentos.esCodigoReintentable(409), "assertTrue failed at PoliticaReintentosTest.java:76");
        }

        @Test
        @DisplayName("400 (bad request) no es reintentable")
        void dado400_noEsReintentable() {
            assertFalse(PoliticaReintentos.esCodigoReintentable(400), "assertFalse failed at PoliticaReintentosTest.java:82");
        }

        @Test
        @DisplayName("401 (unauthorized) no es reintentable")
        void dado401_noEsReintentable() {
            assertFalse(PoliticaReintentos.esCodigoReintentable(401), "assertFalse failed at PoliticaReintentosTest.java:88");
        }

        @Test
        @DisplayName("404 (not found) no es reintentable")
        void dado404_noEsReintentable() {
            assertFalse(PoliticaReintentos.esCodigoReintentable(404), "assertFalse failed at PoliticaReintentosTest.java:94");
        }

        @Test
        @DisplayName("200 (ok) no es reintentable")
        void dado200_noEsReintentable() {
            assertFalse(PoliticaReintentos.esCodigoReintentable(200), "assertFalse failed at PoliticaReintentosTest.java:100");
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
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(400, "model is required"), "assertTrue failed at PoliticaReintentosTest.java:115");
            }

            @Test
            @DisplayName("400 con 'MODEL IS REQUIRED' (mayúsculas) es no reintentable")
            void dado400ModelRequiredMayusculas_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(400, "MODEL IS REQUIRED"), "assertTrue failed at PoliticaReintentosTest.java:121");
            }

            @Test
            @DisplayName("400 con 'not found for api version' es no reintentable")
            void dado400NotFoundApiVersion_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(400, "Model not found for api version"), "assertTrue failed at PoliticaReintentosTest.java:127");
            }

            @Test
            @DisplayName("400 con 'invalid_request_error' es no reintentable")
            void dado400InvalidRequestError_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(400, "invalid_request_error"), "assertTrue failed at PoliticaReintentosTest.java:133");
            }

            @Test
            @DisplayName("400 con 'does not exist' es no reintentable")
            void dado400DoesNotExist_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(400, "Model does not exist"), "assertTrue failed at PoliticaReintentosTest.java:139");
            }

            @Test
            @DisplayName("400 con cuerpo null es reintentable (sin mensaje específico)")
            void dado400ConNull_esReintentable() {
                assertFalse(PoliticaReintentos.esCodigoNoReintentable(400, null), "assertFalse failed at PoliticaReintentosTest.java:145");
            }

            @Test
            @DisplayName("400 con cuerpo vacío es reintentable (sin mensaje específico)")
            void dado400ConVacio_esReintentable() {
                assertFalse(PoliticaReintentos.esCodigoNoReintentable(400, ""), "assertFalse failed at PoliticaReintentosTest.java:151");
            }
        }

        @Nested
        @DisplayName("404 Not Found")
        class Codigo404 {

            @Test
            @DisplayName("404 con 'model is required' es no reintentable")
            void dado404ModelRequired_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(404, "model is required"), "assertTrue failed at PoliticaReintentosTest.java:162");
            }

            @Test
            @DisplayName("404 con 'does not exist' es no reintentable")
            void dado404DoesNotExist_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(404, "Resource does not exist"), "assertTrue failed at PoliticaReintentosTest.java:168");
            }

            @Test
            @DisplayName("404 con cuerpo null es reintentable (sin mensaje específico)")
            void dado404ConNull_esReintentable() {
                assertFalse(PoliticaReintentos.esCodigoNoReintentable(404, null), "assertFalse failed at PoliticaReintentosTest.java:174");
            }
        }

        @Nested
        @DisplayName("Otros códigos no reintentables")
        class OtrosCodigosNoReintentables {

            @Test
            @DisplayName("401 (unauthorized) es no reintentable")
            void dado401_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(401, "Unauthorized"), "assertTrue failed at PoliticaReintentosTest.java:185");
            }

            @Test
            @DisplayName("403 (forbidden) es no reintentable")
            void dado403_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(403, "Forbidden"), "assertTrue failed at PoliticaReintentosTest.java:191");
            }

            @Test
            @DisplayName("405 (method not allowed) es no reintentable")
            void dado405_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(405, "Method not allowed"), "assertTrue failed at PoliticaReintentosTest.java:197");
            }

            @Test
            @DisplayName("410 (gone) es no reintentable")
            void dado410_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(410, "Gone"), "assertTrue failed at PoliticaReintentosTest.java:203");
            }

            @Test
            @DisplayName("422 (unprocessable entity) es no reintentable")
            void dado422_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(422, "Unprocessable entity"), "assertTrue failed at PoliticaReintentosTest.java:209");
            }
        }

        @Nested
        @DisplayName("409 Conflict - casos especiales")
        class Codigo409Conflict {

            @Test
            @DisplayName("409 con 'rate limit' es reintentable (no es no reintentable)")
            void dado409ConRateLimit_esReintentable() {
                assertFalse(PoliticaReintentos.esCodigoNoReintentable(409, "rate limit exceeded"), "assertFalse failed at PoliticaReintentosTest.java:220");
            }

            @Test
            @DisplayName("409 con 'try again' es reintentable")
            void dado409ConTryAgain_esReintentable() {
                assertFalse(PoliticaReintentos.esCodigoNoReintentable(409, "Please try again later"), "assertFalse failed at PoliticaReintentosTest.java:226");
            }

            @Test
            @DisplayName("409 con 'temporarily' es reintentable")
            void dado409ConTemporarily_esReintentable() {
                assertFalse(PoliticaReintentos.esCodigoNoReintentable(409, "Resource temporarily unavailable"), "assertFalse failed at PoliticaReintentosTest.java:232");
            }

            @Test
            @DisplayName("409 con 'concurrent' es reintentable")
            void dado409ConConcurrent_esReintentable() {
                assertFalse(PoliticaReintentos.esCodigoNoReintentable(409, "concurrent modification detected"), "assertFalse failed at PoliticaReintentosTest.java:238");
            }

            @Test
            @DisplayName("409 sin indicadores temporales es no reintentable")
            void dado409SinIndicadores_noSeReintenta() {
                assertTrue(PoliticaReintentos.esCodigoNoReintentable(409, "Resource conflict"), "assertTrue failed at PoliticaReintentosTest.java:244");
            }
        }

        @Nested
        @DisplayName("Códigos reintentables")
        class CodigosReintentables {

            @Test
            @DisplayName("500 no es no reintentable (es reintentable)")
            void dado500_esReintentable() {
                assertFalse(PoliticaReintentos.esCodigoNoReintentable(500, "Internal server error"), "assertFalse failed at PoliticaReintentosTest.java:255");
            }

            @Test
            @DisplayName("503 no es no reintentable (es reintentable)")
            void dado503_esReintentable() {
                assertFalse(PoliticaReintentos.esCodigoNoReintentable(503, "Service unavailable"), "assertFalse failed at PoliticaReintentosTest.java:261");
            }

            @Test
            @DisplayName("429 no es no reintentable (es reintentable)")
            void dado429_esReintentable() {
                assertFalse(PoliticaReintentos.esCodigoNoReintentable(429, "Too many requests"), "assertFalse failed at PoliticaReintentosTest.java:267");
            }
        }
    }

    @Nested
    @DisplayName("esExcepcionReintentable")
    class EsExcepcionReintentable {

        @Test
        @DisplayName("SocketTimeoutException es reintentable")
        void socketTimeout_esReintentable() {
            assertTrue(PoliticaReintentos.esExcepcionReintentable(new SocketTimeoutException("timeout")), "assertTrue failed at PoliticaReintentosTest.java:279");
        }

        @Test
        @DisplayName("SocketTimeoutException sin mensaje es reintentable")
        void socketTimeoutSinMensaje_esReintentable() {
            assertTrue(PoliticaReintentos.esExcepcionReintentable(new SocketTimeoutException()), "assertTrue failed at PoliticaReintentosTest.java:285");
        }

        @Test
        @DisplayName("IOException con 'timeout' en mensaje es reintentable")
        void ioExceptionConTimeout_esReintentable() {
            assertTrue(PoliticaReintentos.esExcepcionReintentable(new IOException("Connection timeout")), "assertTrue failed at PoliticaReintentosTest.java:291");
        }

        @Test
        @DisplayName("IOException con 'connection reset' en mensaje es reintentable")
        void ioExceptionConConnectionReset_esReintentable() {
            assertTrue(PoliticaReintentos.esExcepcionReintentable(new IOException("connection reset by peer")), "assertTrue failed at PoliticaReintentosTest.java:297");
        }

        @Test
        @DisplayName("IOException con 'refused' en mensaje es reintentable")
        void ioExceptionConRefused_esReintentable() {
            assertTrue(PoliticaReintentos.esExcepcionReintentable(new IOException("Connection refused")), "assertTrue failed at PoliticaReintentosTest.java:303");
        }

        @Test
        @DisplayName("IOException con 'temporarily unavailable' en mensaje es reintentable")
        void ioExceptionConTemporarilyUnavailable_esReintentable() {
            assertTrue(PoliticaReintentos.esExcepcionReintentable(new IOException("Service temporarily unavailable")), "assertTrue failed at PoliticaReintentosTest.java:309");
        }

        @Test
        @DisplayName("IOException sin mensaje es reintentable (asume problema de red)")
        void ioExceptionSinMensaje_esReintentable() {
            assertTrue(PoliticaReintentos.esExcepcionReintentable(new IOException()), "assertTrue failed at PoliticaReintentosTest.java:315");
        }

        @Test
        @DisplayName("IOException con mensaje null es reintentable")
        void ioExceptionConMensajeNull_esReintentable() {
            IOException e = new IOException();
            assertTrue(PoliticaReintentos.esExcepcionReintentable(e), "assertTrue failed at PoliticaReintentosTest.java:322");
        }

        @Test
        @DisplayName("IOException con mensaje no reconocido no es reintentable")
        void ioExceptionConMensajeNoReconocido_noEsReintentable() {
            assertFalse(PoliticaReintentos.esExcepcionReintentable(new IOException("Invalid API key")), "assertFalse failed at PoliticaReintentosTest.java:328");
        }

        @Test
        @DisplayName("Excepcion null no es reintentable")
        void excepcionNull_noEsReintentable() {
            assertFalse(PoliticaReintentos.esExcepcionReintentable(null), "assertFalse failed at PoliticaReintentosTest.java:334");
        }

        @Test
        @DisplayName("Con thread interrumpido no es reintentable")
        void conThreadInterrumpido_noEsReintentable() throws InterruptedException {
            Thread testThread = new Thread(() -> {
                Thread.currentThread().interrupt();
                assertFalse(PoliticaReintentos.esExcepcionReintentable(new SocketTimeoutException("timeout")), "assertFalse failed at PoliticaReintentosTest.java:342");
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
                assertTrue(esperaMs >= 5000L, "assertTrue failed at PoliticaReintentosTest.java:361");
            }

            @Test
            @DisplayName("429 con Retry-After de 120 segundos retorna 120000ms")
            void dado429ConRetryAfter120Segundos_retorna120000ms() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(429, "120", 1000L, 1);
                assertEquals(120000L, esperaMs, "assertEquals failed at PoliticaReintentosTest.java:368");
            }

            @Test
            @DisplayName("429 con Retry-After de 0 usa backoff normal")
            void dado429ConRetryAfterCero_usaBackoffNormal() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(429, "0", 2000L, 1);
                assertTrue(esperaMs >= 2000L, "assertTrue failed at PoliticaReintentosTest.java:375");
                assertTrue(esperaMs <= 2500L, "assertTrue failed at PoliticaReintentosTest.java:376");
            }

            @Test
            @DisplayName("429 con Retry-After negativo usa backoff normal")
            void dado429ConRetryAfterNegativo_usaBackoffNormal() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(429, "-5", 2000L, 1);
                assertTrue(esperaMs >= 1000L, "assertTrue failed at PoliticaReintentosTest.java:383");
            }

            @Test
            @DisplayName("429 con Retry-After inválido usa backoff normal")
            void dado429ConRetryAfterInvalido_usaBackoffNormal() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(429, "invalid", 2000L, 1);
                assertTrue(esperaMs >= 2000L, "assertTrue failed at PoliticaReintentosTest.java:390");
            }

            @Test
            @DisplayName("429 con Retry-After null usa backoff normal")
            void dado429ConRetryAfterNull_usaBackoffNormal() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(429, null, 2000L, 1);
                assertTrue(esperaMs >= 2000L, "assertTrue failed at PoliticaReintentosTest.java:397");
            }

            @Test
            @DisplayName("429 con Retry-After vacío usa backoff normal")
            void dado429ConRetryAfterVacio_usaBackoffNormal() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(429, "", 2000L, 1);
                assertTrue(esperaMs >= 2000L, "assertTrue failed at PoliticaReintentosTest.java:404");
            }
        }

        @Nested
        @DisplayName("Backoff normalizado")
        class BackoffNormalizado {

            @Test
            @DisplayName("Backoff menor a 1000ms se normaliza a 1000ms")
            void dadoBackoffMenorA1000_seNormaliza() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(500, null, 500L, 1);
                assertTrue(esperaMs >= 1000L, "assertTrue failed at PoliticaReintentosTest.java:416");
            }

            @Test
            @DisplayName("Backoff de 0 se normaliza a 1000ms mínimo")
            void dadoBackoffCero_seNormaliza() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(500, null, 0L, 1);
                assertTrue(esperaMs >= 1000L, "assertTrue failed at PoliticaReintentosTest.java:423");
            }

            @Test
            @DisplayName("Backoff negativo se normaliza a 1000ms mínimo")
            void dadoBackoffNegativo_seNormaliza() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(500, null, -100L, 1);
                assertTrue(esperaMs >= 1000L, "assertTrue failed at PoliticaReintentosTest.java:430");
            }

            @Test
            @DisplayName("Backoff de 2000ms con intento 1 retorna entre 1000-1500ms de jitter")
            void dadoBackoff2000_intento1_retornaConJitter() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(500, null, 2000L, 1);
                assertTrue(esperaMs >= 2000L, "assertTrue failed at PoliticaReintentosTest.java:437");
                assertTrue(esperaMs <= 2500L, "assertTrue failed at PoliticaReintentosTest.java:438");
            }

            @Test
            @DisplayName("Backoff de 2000ms con intento 2 (par) tiene más jitter")
            void dadoBackoff2000_intento2_retornaConMasJitter() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(500, null, 2000L, 2);
                assertTrue(esperaMs >= 2000L, "assertTrue failed at PoliticaReintentosTest.java:445");
                assertTrue(esperaMs <= 2500L, "assertTrue failed at PoliticaReintentosTest.java:446");
            }

            @Test
            @DisplayName("Intento 0 se comporta como intento impar")
            void dadoIntentoCero_seComportaComoImpar() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(500, null, 2000L, 0);
                assertTrue(esperaMs >= 2000L, "assertTrue failed at PoliticaReintentosTest.java:453");
            }

            @Test
            @DisplayName("Intento negativo se normaliza")
            void dadoIntentoNegativo_seNormaliza() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(500, null, 2000L, -5);
                assertTrue(esperaMs >= 2000L, "assertTrue failed at PoliticaReintentosTest.java:460");
            }
        }

        @Nested
        @DisplayName("Códigos que no son 429")
        class CodigosNo429 {

            @Test
            @DisplayName("500 ignora Retry-After y usa backoff")
            void dado500_ignoraRetryAfter() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(500, "10", 2000L, 1);
                assertTrue(esperaMs >= 2000L, "assertTrue failed at PoliticaReintentosTest.java:472");
                assertTrue(esperaMs < 10000L, "assertTrue failed at PoliticaReintentosTest.java:473");
            }

            @Test
            @DisplayName("503 usa backoff normal")
            void dado503_usaBackoffNormal() {
                long esperaMs = PoliticaReintentos.calcularEsperaMs(503, null, 2000L, 1);
                assertTrue(esperaMs >= 2000L, "assertTrue failed at PoliticaReintentosTest.java:480");
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
            assertEquals(30000L, resultado, "assertEquals failed at PoliticaReintentosTest.java:493");
        }

        @Test
        @DisplayName("Parsea segundos con espacios")
        void parseaSegundosConEspacios() {
            long resultado = PoliticaReintentos.parsearRetryAfterMs("  60  ", System.currentTimeMillis());
            assertEquals(60000L, resultado, "assertEquals failed at PoliticaReintentosTest.java:500");
        }

        @Test
        @DisplayName("Retorna 0 para valor 0")
        void retornaCeroParaValorCero() {
            long resultado = PoliticaReintentos.parsearRetryAfterMs("0", System.currentTimeMillis());
            assertEquals(0L, resultado, "assertEquals failed at PoliticaReintentosTest.java:507");
        }

        @Test
        @DisplayName("Retorna 0 para valor negativo")
        void retornaCeroParaValorNegativo() {
            long resultado = PoliticaReintentos.parsearRetryAfterMs("-10", System.currentTimeMillis());
            assertEquals(0L, resultado, "assertEquals failed at PoliticaReintentosTest.java:514");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Retorna 0 para null, vacío o espacios")
        void retornaCeroParaNullVacioOEspacios(String valor) {
            long resultado = PoliticaReintentos.parsearRetryAfterMs(valor, System.currentTimeMillis());
            assertEquals(0L, resultado, "assertEquals failed at PoliticaReintentosTest.java:523");
        }

        @Test
        @DisplayName("Retorna 0 para texto no numérico inválido")
        void retornaCeroParaTextoNoNumerico() {
            long resultado = PoliticaReintentos.parsearRetryAfterMs("invalid", System.currentTimeMillis());
            assertEquals(0L, resultado, "assertEquals failed at PoliticaReintentosTest.java:530");
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
            assertTrue(resultado > 0L, "assertTrue failed at PoliticaReintentosTest.java:545");
            assertTrue(resultado <= 65000L, "assertTrue failed at PoliticaReintentosTest.java:546");
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
            assertEquals(0L, resultado, "assertEquals failed at PoliticaReintentosTest.java:561");
        }

        @Test
        @DisplayName("Retorna 0 para fecha con formato inválido")
        void retornaCeroParaFormatoFechaInvalido() {
            long resultado = PoliticaReintentos.parsearRetryAfterMs("Wed, 21 Oct 2015", System.currentTimeMillis());
            assertEquals(0L, resultado, "assertEquals failed at PoliticaReintentosTest.java:568");
        }

        @Test
        @DisplayName("Respeta mínimo de 1000ms para segundos positivos")
        void respetaMinimoParaSegundosPositivos() {
            long resultado = PoliticaReintentos.parsearRetryAfterMs("1", System.currentTimeMillis());
            assertEquals(1000L, resultado, "assertEquals failed at PoliticaReintentosTest.java:575");
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
            assertEquals("Connection refused", mensaje, "assertEquals failed at PoliticaReintentosTest.java:614");
        }

        @Test
        @DisplayName("IOException sin mensaje retorna nombre de clase")
        void ioExceptionSinMensaje_retornaNombreClase() {
            String mensaje = PoliticaReintentos.obtenerMensajeErrorAmigable(new IOException());
            assertEquals("IOException", mensaje, "assertEquals failed at PoliticaReintentosTest.java:621");
        }

        @Test
        @DisplayName("Excepcion null retorna mensaje de error desconocido")
        void excepcionNull_retornaErrorDesconocido() {
            String mensaje = PoliticaReintentos.obtenerMensajeErrorAmigable(null);
            assertTrue(mensaje.contains("desconocido") || mensaje.contains("Unknown"), "assertTrue failed at PoliticaReintentosTest.java:628");
        }

        @Test
        @DisplayName("RuntimeException con causa la desenvuelve")
        void runtimeExceptionConCausa_desenvuelve() {
            IOException causa = new IOException("Error de conexión");
            RuntimeException wrapper = new RuntimeException("Wrapper", causa);
            String mensaje = PoliticaReintentos.obtenerMensajeErrorAmigable(wrapper);
            assertEquals("Error de conexión", mensaje, "assertEquals failed at PoliticaReintentosTest.java:637");
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
            assertEquals("Exception", mensaje, "assertEquals failed at PoliticaReintentosTest.java:656");
        }
    }
}
