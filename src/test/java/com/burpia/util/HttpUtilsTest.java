package com.burpia.util;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("HttpUtils Tests")
class HttpUtilsTest {

    @Nested
    @DisplayName("generarHash")
    class GenerarHash {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Retorna hash SHA-256 de cadena vacía para input null o vacío")
        void retornaHashVacioParaNullOVacio(byte[] input) {
            String expectedEmptyHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
            assertEquals(expectedEmptyHash, HttpUtils.generarHash(input), "assertEquals failed at HttpUtilsTest.java:36");
        }

        @Test
        @DisplayName("Genera hash SHA-256 correcto para input válido")
        void generaHashCorrectoParaInputValido() {
            String expectedHash = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
            assertEquals(expectedHash, HttpUtils.generarHash("hello".getBytes()), "assertEquals failed at HttpUtilsTest.java:43");
        }

        @Test
        @DisplayName("Genera hash consistente para el mismo input")
        void generaHashConsistente() {
            byte[] data = "test data".getBytes();
            String hash1 = HttpUtils.generarHash(data);
            String hash2 = HttpUtils.generarHash(data);
            assertEquals(hash1, hash2, "assertEquals failed at HttpUtilsTest.java:52");
        }

        @Test
        @DisplayName("Genera hashes diferentes para inputs diferentes")
        void generaHashesDiferentesParaInputsDiferentes() {
            String hash1 = HttpUtils.generarHash("input1".getBytes());
            String hash2 = HttpUtils.generarHash("input2".getBytes());
            assertNotEquals(hash1, hash2);
        }
    }

    @Nested
    @DisplayName("generarHashPartes")
    class GenerarHashPartes {

        @Test
        @DisplayName("Retorna hash de cadena vacía para array null")
        void retornaHashVacioParaArrayNull() {
            String expectedEmptyHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
            assertEquals(expectedEmptyHash, HttpUtils.generarHashPartes((String[]) null), "assertEquals failed at HttpUtilsTest.java:72");
        }

        @Test
        @DisplayName("Retorna hash de cadena vacía para array vacío")
        void retornaHashVacioParaArrayVacio() {
            String expectedEmptyHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
            assertEquals(expectedEmptyHash, HttpUtils.generarHashPartes(), "assertEquals failed at HttpUtilsTest.java:79");
        }

        @Test
        @DisplayName("Genera hash determinista para partes válidas")
        void generaHashDeterministaParaPartesValidas() {
            String hash1 = HttpUtils.generarHashPartes("part1", "part2", null, "");
            String hash2 = HttpUtils.generarHashPartes("part1", "part2", null, "");
            assertEquals(hash1, hash2, "El hash debe ser determinista para las mismas partes");
            assertNotNull(hash1, "assertNotNull failed at HttpUtilsTest.java:88");
            assertFalse(hash1.isEmpty(), "assertFalse failed at HttpUtilsTest.java:89");
        }

        @Test
        @DisplayName("Ignora partes null y vacías en el hash")
        void ignoraPartesNullYVacias() {
            String hashConNull = HttpUtils.generarHashPartes("part1", null, "part2");
            String hashSinNull = HttpUtils.generarHashPartes("part1", "part2");
            assertNotEquals(hashConNull, hashSinNull, "Los nulls afectan el hash aunque no se procesen");
        }

        @Test
        @DisplayName("Genera hashes diferentes para diferentes órdenes de partes")
        void generaHashesDiferentesParaDiferentesOrdenes() {
            String hash1 = HttpUtils.generarHashPartes("a", "b");
            String hash2 = HttpUtils.generarHashPartes("b", "a");
            assertNotEquals(hash1, hash2, "El orden de las partes debe afectar el hash");
        }
    }

    @Nested
    @DisplayName("convertirDigestHex")
    class ConvertirDigestHex {

        @Test
        @DisplayName("Retorna cadena vacía para hash null")
        void retornaVacioParaNull() {
            assertEquals("", HttpUtils.convertirDigestHex(null), "assertEquals failed at HttpUtilsTest.java:116");
        }

        @Test
        @DisplayName("Retorna cadena vacía para hash vacío")
        void retornaVacioParaHashVacio() {
            assertEquals("", HttpUtils.convertirDigestHex(new byte[0]), "assertEquals failed at HttpUtilsTest.java:122");
        }

        @Test
        @DisplayName("Convierte bytes a hexadecimal correctamente")
        void convierteBytesAHexadecimal() {
            byte[] hash = {0x00, 0x0f, (byte) 0xff, (byte) 0xab};
            assertEquals("000fffab", HttpUtils.convertirDigestHex(hash), "assertEquals failed at HttpUtilsTest.java:129");
        }

        @Test
        @DisplayName("Limita salida con maxBytes")
        void limitaSalidaConMaxBytes() {
            byte[] hash = {0x01, 0x02, 0x03, 0x04};
            assertEquals("0102", HttpUtils.convertirDigestHex(hash, 2), "assertEquals failed at HttpUtilsTest.java:136");
        }

        @Test
        @DisplayName("Maneja maxBytes mayor que longitud del hash")
        void manejaMaxBytesMayorQueHash() {
            byte[] hash = {0x01, 0x02};
            assertEquals("0102", HttpUtils.convertirDigestHex(hash, 10), "assertEquals failed at HttpUtilsTest.java:143");
        }

        @Test
        @DisplayName("Maneja maxBytes cero o negativo")
        void manejaMaxBytesCeroONegativo() {
            byte[] hash = {0x01, 0x02};
            assertEquals("01", HttpUtils.convertirDigestHex(hash, 0), "assertEquals failed at HttpUtilsTest.java:150");
            assertEquals("01", HttpUtils.convertirDigestHex(hash, -1), "assertEquals failed at HttpUtilsTest.java:151");
        }
    }

    @Nested
    @DisplayName("generarHashRapido")
    class GenerarHashRapido {

        @Test
        @DisplayName("Retorna cadena vacía si solicitud es null")
        void retornaVacioSiSolicitudNull() {
            HttpResponse response = mock(HttpResponse.class);
            assertEquals("", HttpUtils.generarHashRapido(null, response), "assertEquals failed at HttpUtilsTest.java:163");
        }

        @Test
        @DisplayName("Retorna cadena vacía si respuesta es null")
        void retornaVacioSiRespuestaNull() {
            HttpRequest request = mock(HttpRequest.class);
            assertEquals("", HttpUtils.generarHashRapido(request, null), "assertEquals failed at HttpUtilsTest.java:170");
        }

        @Test
        @DisplayName("Genera hash para solicitud y respuesta válidas")
        void generaHashParaSolicitudYRespuestaValidas() {
            HttpRequest request = mock(HttpRequest.class, RETURNS_DEEP_STUBS);
            HttpResponse response = mock(HttpResponse.class, RETURNS_DEEP_STUBS);

            when(request.method()).thenReturn("GET");
            when(request.url()).thenReturn("http://example.com");
            when(request.body().length()).thenReturn(100);
            when(response.statusCode()).thenReturn((short) 200);
            when(response.body().length()).thenReturn(500);

            String hash = HttpUtils.generarHashRapido(request, response);
            assertNotNull(hash, "assertNotNull failed at HttpUtilsTest.java:186");
            assertFalse(hash.isEmpty(), "assertFalse failed at HttpUtilsTest.java:187");
        }

        @Test
        @DisplayName("Genera hash consistente para los mismos datos")
        void generaHashConsistente() {
            HttpRequest request = mock(HttpRequest.class, RETURNS_DEEP_STUBS);
            HttpResponse response = mock(HttpResponse.class, RETURNS_DEEP_STUBS);

            when(request.method()).thenReturn("POST");
            when(request.url()).thenReturn("http://api.example.com/users");
            when(request.body().length()).thenReturn(256);
            when(response.statusCode()).thenReturn((short) 201);
            when(response.body().length()).thenReturn(64);

            String hash1 = HttpUtils.generarHashRapido(request, response);
            String hash2 = HttpUtils.generarHashRapido(request, response);
            assertEquals(hash1, hash2, "assertEquals failed at HttpUtilsTest.java:204");
        }

        @Test
        @DisplayName("Maneja excepciones en body() gracefully")
        void manejaExcepcionesEnBody() {
            HttpRequest request = mock(HttpRequest.class, RETURNS_DEEP_STUBS);
            HttpResponse response = mock(HttpResponse.class, RETURNS_DEEP_STUBS);

            when(request.method()).thenReturn("GET");
            when(request.url()).thenReturn("http://example.com");
            when(request.body()).thenThrow(new RuntimeException("Error"));
            when(response.statusCode()).thenReturn((short) 200);
            when(response.body()).thenThrow(new RuntimeException("Error"));

            String hash = HttpUtils.generarHashRapido(request, response);
            assertNotNull(hash, "assertNotNull failed at HttpUtilsTest.java:220");
            assertFalse(hash.isEmpty(), "assertFalse failed at HttpUtilsTest.java:221");
        }
    }

    @Nested
    @DisplayName("extraerEncabezados (HttpRequest)")
    class ExtraerEncabezadosRequest {

        @Test
        @DisplayName("Retorna mensaje de solicitud null para input null")
        void retornaMensajeNullParaInputNull() {
            assertEquals("[SOLICITUD NULL]", HttpUtils.extraerEncabezados((HttpRequest) null), "assertEquals failed at HttpUtilsTest.java:232");
        }

        @Test
        @DisplayName("Extrae encabezados correctamente de solicitud válida")
        void extraeEncabezadosCorrectamente() {
            HttpRequest request = mock(HttpRequest.class);
            when(request.method()).thenReturn("GET");
            when(request.url()).thenReturn("http://example.com");

            HttpHeader header1 = mock(HttpHeader.class);
            when(header1.name()).thenReturn("Host");
            when(header1.value()).thenReturn("example.com");

            HttpHeader header2 = mock(HttpHeader.class);
            when(header2.name()).thenReturn(null);
            when(header2.value()).thenReturn(null);

            when(request.headers()).thenReturn(Arrays.asList(header1, header2, null));

            String headers = HttpUtils.extraerEncabezados(request);
            assertTrue(headers.contains("GET http://example.com"), "assertTrue failed at HttpUtilsTest.java:253");
            assertTrue(headers.contains("Host: example.com"), "assertTrue failed at HttpUtilsTest.java:254");
            assertTrue(headers.contains("[NAME NULL]: [VALUE NULL]"), "assertTrue failed at HttpUtilsTest.java:255");
        }

        @Test
        @DisplayName("Maneja campos null en solicitud")
        void manejaCamposNull() {
            HttpRequest request = mock(HttpRequest.class);
            when(request.method()).thenReturn(null);
            when(request.url()).thenReturn(null);
            when(request.headers()).thenReturn(null);
            String headers = HttpUtils.extraerEncabezados(request);
            assertTrue(headers.contains("[METHOD NULL] [URL NULL]"), "assertTrue failed at HttpUtilsTest.java:266");
            assertTrue(headers.contains("[HEADERS NULL]"), "assertTrue failed at HttpUtilsTest.java:267");
        }
    }

    @Nested
    @DisplayName("extraerEncabezados (HttpResponse)")
    class ExtraerEncabezadosResponse {

        @Test
        @DisplayName("Retorna mensaje de respuesta null para input null")
        void retornaMensajeNullParaInputNull() {
            assertEquals("[RESPUESTA NULL]", HttpUtils.extraerEncabezados((HttpResponse) null), "assertEquals failed at HttpUtilsTest.java:278");
        }

        @Test
        @DisplayName("Extrae encabezados correctamente de respuesta válida")
        void extraeEncabezadosCorrectamente() {
            HttpResponse response = mock(HttpResponse.class);
            when(response.httpVersion()).thenReturn("HTTP/2");
            when(response.statusCode()).thenReturn((short) 200);
            when(response.reasonPhrase()).thenReturn("OK");

            HttpHeader header = mock(HttpHeader.class);
            when(header.name()).thenReturn("Content-Type");
            when(header.value()).thenReturn("text/html");

            when(response.headers()).thenReturn(Collections.singletonList(header));

            String headers = HttpUtils.extraerEncabezados(response);
            assertTrue(headers.contains("HTTP/2 200 OK"), "assertTrue failed at HttpUtilsTest.java:296");
            assertTrue(headers.contains("Content-Type: text/html"), "assertTrue failed at HttpUtilsTest.java:297");
        }

        @Test
        @DisplayName("Maneja campos null en respuesta")
        void manejaCamposNull() {
            HttpResponse response = mock(HttpResponse.class);
            when(response.httpVersion()).thenReturn(null);
            when(response.statusCode()).thenReturn((short) 500);
            when(response.reasonPhrase()).thenReturn(" ");
            when(response.headers()).thenReturn(null);

            String headers = HttpUtils.extraerEncabezados(response);
            assertTrue(headers.contains("HTTP/1.1 500"), "assertTrue failed at HttpUtilsTest.java:310");
            assertTrue(headers.contains("[HEADERS NULL]"), "assertTrue failed at HttpUtilsTest.java:311");
        }
    }

    @Nested
    @DisplayName("extraerCuerpo (HttpRequest)")
    class ExtraerCuerpoRequest {

        @Test
        @DisplayName("Retorna cadena vacía para solicitud null")
        void retornaVacioParaNull() {
            assertEquals("", HttpUtils.extraerCuerpo((HttpRequest) null), "assertEquals failed at HttpUtilsTest.java:322");
        }

        @Test
        @DisplayName("Extrae cuerpo correctamente de solicitud válida")
        void extraeCuerpoCorrectamente() {
            HttpRequest request = mock(HttpRequest.class);
            ByteArray body = mock(ByteArray.class);
            when(body.length()).thenReturn(12);
            when(body.toString()).thenReturn("body content");
            when(request.body()).thenReturn(body);
            assertEquals("body content", HttpUtils.extraerCuerpo(request), "assertEquals failed at HttpUtilsTest.java:333");
        }

        @Test
        @DisplayName("Retorna cadena vacía si body es null")
        void retornaVacioSiBodyNull() {
            HttpRequest request = mock(HttpRequest.class);
            when(request.body()).thenReturn(null);
            assertEquals("", HttpUtils.extraerCuerpo(request), "assertEquals failed at HttpUtilsTest.java:341");
        }

        @Test
        @DisplayName("Maneja excepciones gracefully")
        void manejaExcepciones() {
            HttpRequest request = mock(HttpRequest.class);
            when(request.body()).thenThrow(new RuntimeException("Error"));
            assertEquals("", HttpUtils.extraerCuerpo(request), "assertEquals failed at HttpUtilsTest.java:349");
        }

        @Test
        @DisplayName("Limita cuerpo con maxCaracteres")
        void limitaCuerpoConMaxCaracteres() {
            HttpRequest request = mock(HttpRequest.class);
            ByteArray body = mock(ByteArray.class);
            ByteArray partialBody = mock(ByteArray.class);
            when(body.length()).thenReturn(1000);
            when(body.toString()).thenReturn("a".repeat(1000));
            // maxBytes = Math.min(1000, Math.max(64, 10 * 4)) = Math.min(1000, 64) = 64
            when(body.subArray(0, 64)).thenReturn(partialBody);
            when(partialBody.toString()).thenReturn("a".repeat(10));
            when(request.body()).thenReturn(body);

            String result = HttpUtils.extraerCuerpo(request, 10);
            assertEquals(10, result.length(), "assertEquals failed at HttpUtilsTest.java:366");
            assertEquals("aaaaaaaaaa", result, "assertEquals failed at HttpUtilsTest.java:367");
        }

        @Test
        @DisplayName("Retorna cuerpo completo si es menor que maxCaracteres")
        void retornaCuerpoCompletoSiMenorQueMax() {
            HttpRequest request = mock(HttpRequest.class);
            ByteArray body = mock(ByteArray.class);
            ByteArray partialBody = mock(ByteArray.class);
            when(body.length()).thenReturn(5);
            when(body.toString()).thenReturn("short");
            // maxBytes = Math.min(5, Math.max(64, 100 * 4)) = Math.min(5, 400) = 5
            when(body.subArray(0, 5)).thenReturn(partialBody);
            when(partialBody.toString()).thenReturn("short");
            when(request.body()).thenReturn(body);

            String result = HttpUtils.extraerCuerpo(request, 100);
            assertEquals("short", result, "assertEquals failed at HttpUtilsTest.java:384");
        }
    }

    @Nested
    @DisplayName("extraerCuerpo (HttpResponse)")
    class ExtraerCuerpoResponse {

        @Test
        @DisplayName("Retorna cadena vacía para respuesta null")
        void retornaVacioParaNull() {
            assertEquals("", HttpUtils.extraerCuerpo((HttpResponse) null), "assertEquals failed at HttpUtilsTest.java:395");
        }

        @Test
        @DisplayName("Extrae cuerpo correctamente de respuesta válida")
        void extraeCuerpoCorrectamente() {
            HttpResponse response = mock(HttpResponse.class);
            ByteArray body = mock(ByteArray.class);
            when(body.length()).thenReturn(13);
            when(body.toString()).thenReturn("response body");
            when(response.body()).thenReturn(body);
            assertEquals("response body", HttpUtils.extraerCuerpo(response), "assertEquals failed at HttpUtilsTest.java:406");
        }

        @Test
        @DisplayName("Retorna cadena vacía si body es null")
        void retornaVacioSiBodyNull() {
            HttpResponse response = mock(HttpResponse.class);
            when(response.body()).thenReturn(null);
            assertEquals("", HttpUtils.extraerCuerpo(response), "assertEquals failed at HttpUtilsTest.java:414");
        }

        @Test
        @DisplayName("Maneja excepciones gracefully")
        void manejaExcepciones() {
            HttpResponse response = mock(HttpResponse.class);
            when(response.body()).thenThrow(new RuntimeException("Error"));
            assertEquals("", HttpUtils.extraerCuerpo(response), "assertEquals failed at HttpUtilsTest.java:422");
        }

        @Test
        @DisplayName("Limita cuerpo con maxCaracteres")
        void limitaCuerpoConMaxCaracteres() {
            HttpResponse response = mock(HttpResponse.class);
            ByteArray body = mock(ByteArray.class);
            ByteArray partialBody = mock(ByteArray.class);
            when(body.length()).thenReturn(1000);
            when(body.toString()).thenReturn("b".repeat(1000));
            when(body.subArray(0, 80)).thenReturn(partialBody);
            when(partialBody.toString()).thenReturn("b".repeat(20));
            when(response.body()).thenReturn(body);

            String result = HttpUtils.extraerCuerpo(response, 20);
            assertEquals(20, result.length(), "assertEquals failed at HttpUtilsTest.java:438");
        }
    }

    @Nested
    @DisplayName("esRecursoEstatico")
    class EsRecursoEstatico {

        @ParameterizedTest
        @ValueSource(strings = {
            "http://example.com/style.css",
            "http://example.com/image.PNG",
            "http://example.com/script.js?v=1.2",
            "http://example.com/font.woff2#anchor",
            "http://example.com/path/to/image.jpg",
            "https://cdn.example.com/bundle.min.js",
            "http://example.com/icon.ico",
            "http://example.com/logo.svg",
            "http://example.com/font.ttf",
            "http://example.com/image.webp"
        })
        @DisplayName("Retorna true para recursos estáticos")
        void retornaTrueParaRecursosEstaticos(String url) {
            assertTrue(HttpUtils.esRecursoEstatico(url), "assertTrue failed at HttpUtilsTest.java:461");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "http://example.com/",
            "http://example.com/api/data",
            "http://example.com/page.html",
            "http://example.com/users.json",
            "http://example.com/feed.xml",
            "http://example.com/document.pdf"
        })
        @DisplayName("Retorna false para recursos no estáticos")
        void retornaFalseParaRecursosNoEstaticos(String url) {
            assertFalse(HttpUtils.esRecursoEstatico(url), "assertFalse failed at HttpUtilsTest.java:475");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Retorna false para URL null o vacía")
        void retornaFalseParaNullOVacio(String url) {
            assertFalse(HttpUtils.esRecursoEstatico(url), "assertFalse failed at HttpUtilsTest.java:482");
        }

        @Test
        @DisplayName("Usa set de extensiones personalizado")
        void usaSetDeExtensionesPersonalizado() {
            Set<String> customExtensions = new HashSet<>();
            customExtensions.add(".custom");
            customExtensions.add(".special");

            assertTrue(HttpUtils.esRecursoEstatico("http://example.com/file.custom", customExtensions), "assertTrue failed at HttpUtilsTest.java:492");
            assertTrue(HttpUtils.esRecursoEstatico("http://example.com/file.special", customExtensions), "assertTrue failed at HttpUtilsTest.java:493");
            assertFalse(HttpUtils.esRecursoEstatico("http://example.com/file.css", customExtensions), "assertFalse failed at HttpUtilsTest.java:494");
        }

        @Test
        @DisplayName("Retorna false si set de extensiones es null")
        void retornaFalseSiSetDeExtensionesNull() {
            assertFalse(HttpUtils.esRecursoEstatico("http://example.com/file.css", null), "assertFalse failed at HttpUtilsTest.java:500");
        }

        @Test
        @DisplayName("Es case-insensitive para extensiones")
        void esCaseInsensitiveParaExtensiones() {
            assertTrue(HttpUtils.esRecursoEstatico("http://example.com/file.CSS"), "assertTrue failed at HttpUtilsTest.java:506");
            assertTrue(HttpUtils.esRecursoEstatico("http://example.com/file.Js"), "assertTrue failed at HttpUtilsTest.java:507");
            assertTrue(HttpUtils.esRecursoEstatico("http://example.com/file.PnG"), "assertTrue failed at HttpUtilsTest.java:508");
        }

        @Test
        @DisplayName("Ignora query string y fragment")
        void ignoraQueryStringYFragment() {
            assertTrue(HttpUtils.esRecursoEstatico("http://example.com/app.js?version=1.0&debug=true"), "assertTrue failed at HttpUtilsTest.java:514");
            assertTrue(HttpUtils.esRecursoEstatico("http://example.com/style.css#section"), "assertTrue failed at HttpUtilsTest.java:515");
            assertFalse(HttpUtils.esRecursoEstatico("http://example.com/page.html?param=value"), "assertFalse failed at HttpUtilsTest.java:516");
        }
    }
}
