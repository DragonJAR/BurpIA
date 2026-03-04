package com.burpia.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FiltroContenidoAnalizable Tests")
class FiltroContenidoAnalizableTest {

    @Nested
    @DisplayName("Por tipo de contenido multimedia")
    class MultimediaTests {

        @Test
        @DisplayName("image/jpeg no es analizable")
        void imageJpeg_noEsAnalizable() {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("image/jpeg", "GET", 200));
        }

        @Test
        @DisplayName("video/mp4 no es analizable")
        void videoMp4_noEsAnalizable() {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("video/mp4", "GET", 200));
        }

        @Test
        @DisplayName("audio/mpeg no es analizable")
        void audioMpeg_noEsAnalizable() {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("audio/mpeg", "GET", 200));
        }

        @Test
        @DisplayName("font/woff2 no es analizable")
        void fontWoff2_noEsAnalizable() {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("font/woff2", "GET", 200));
        }
    }

    @Nested
    @DisplayName("Por tipo de contenido binario/archivo")
    class BinariosTests {

        @Test
        @DisplayName("application/octet-stream no es analizable")
        void octetStream_noEsAnalizable() {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("application/octet-stream", "GET", 200));
        }

        @Test
        @DisplayName("application/zip no es analizable")
        void zip_noEsAnalizable() {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("application/zip", "GET", 200));
        }

        @Test
        @DisplayName("application/pdf no es analizable")
        void pdf_noEsAnalizable() {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("application/pdf", "GET", 200));
        }

        @Test
        @DisplayName("application/java-archive no es analizable")
        void jar_noEsAnalizable() {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("application/java-archive", "GET", 200));
        }

        @Test
        @DisplayName("application/x-rar-compressed no es analizable")
        void rar_noEsAnalizable() {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("application/x-rar-compressed", "GET", 200));
        }

        @Test
        @DisplayName("application/vnd.android.package-archive no es analizable")
        void apk_noEsAnalizable() {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("application/vnd.android.package-archive", "GET", 200));
        }
    }

    @Nested
    @DisplayName("Por código de estado HTTP")
    class CodigoEstadoTests {

        @ParameterizedTest
        @ValueSource(ints = {100, 101, 102, 103})
        @DisplayName("Códigos 1xx (Informational) no son analizables")
        void codigos1xx_noSonAnalizables(int codigo) {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("text/html", "GET", codigo));
        }

        @Test
        @DisplayName("Código 204 (No Content) no es analizable")
        void codigo204_noEsAnalizable() {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("text/html", "GET", 204));
        }

        @Test
        @DisplayName("Código 304 (Not Modified) no es analizable")
        void codigo304_noEsAnalizable() {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("text/html", "GET", 304));
        }

        @Test
        @DisplayName("Código 200 (OK) con texto es analizable")
        void codigo200_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("text/html", "GET", 200));
        }

        @Test
        @DisplayName("Código 404 (Not Found) con texto es analizable")
        void codigo404_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("text/html", "GET", 404));
        }

        @Test
        @DisplayName("Código 500 (Internal Server Error) con texto es analizable")
        void codigo500_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("application/json", "GET", 500));
        }
    }

    @Nested
    @DisplayName("Por método HTTP")
    class MetodoHttpTests {

        @Test
        @DisplayName("HEAD se omite por ser no analizable")
        void head_seOmite() {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("text/html", "HEAD", 200));
        }

        @Test
        @DisplayName("HEAD en minúsculas también se omite")
        void headMinuscula_seOmite() {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("text/html", "head", 200));
        }

        @Test
        @DisplayName("GET con texto es analizable")
        void get_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("text/html", "GET", 200));
        }

        @Test
        @DisplayName("POST con JSON es analizable")
        void post_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("application/json", "POST", 200));
        }
    }

    @Nested
    @DisplayName("Casos edge")
    class EdgeCasesTests {

        @Test
        @DisplayName("text/html con charset es analizable")
        void textHtmlConCharset_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("text/html; charset=UTF-8", "GET", 200));
        }

        @Test
        @DisplayName("Sin content-type se considera analizable")
        void sinContentType_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable(null, "GET", 200));
        }

        @Test
        @DisplayName("Content-type vacío se considera analizable")
        void contentTypeVacio_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("", "GET", 200));
        }

        @Test
        @DisplayName("Content-type solo con espacios se considera analizable")
        void contentTypeSoloEspacios_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("   ", "GET", 200));
        }

        @Test
        @DisplayName("application/json es analizable")
        void json_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("application/json", "GET", 200));
        }

        @Test
        @DisplayName("application/xml es analizable")
        void xml_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("application/xml", "GET", 200));
        }

        @Test
        @DisplayName("text/plain es analizable")
        void textPlain_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("text/plain", "GET", 200));
        }

        @Test
        @DisplayName("multipart/form-data es analizable")
        void multipartFormData_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("multipart/form-data", "POST", 200));
        }

        @Test
        @DisplayName("application/x-www-form-urlencoded es analizable")
        void urlEncoded_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("application/x-www-form-urlencoded", "POST", 200));
        }
    }
}
