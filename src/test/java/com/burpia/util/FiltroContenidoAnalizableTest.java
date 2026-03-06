package com.burpia.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FiltroContenidoAnalizable Tests")
class FiltroContenidoAnalizableTest {

    @Nested
    @DisplayName("Por tipo de contenido multimedia")
    class MultimediaTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/svg+xml",
            "image/bmp",
            "image/tiff"
        })
        @DisplayName("Tipos image/* no son analizables")
        void imageTypes_noSonAnalizables(String contentType) {
            assertFalse(FiltroContenidoAnalizable.esAnalizable(contentType, "GET", 200), "assertFalse failed at FiltroContenidoAnalizableTest.java:33");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "video/mp4",
            "video/webm",
            "video/ogg",
            "video/quicktime",
            "video/x-msvideo"
        })
        @DisplayName("Tipos video/* no son analizables")
        void videoTypes_noSonAnalizables(String contentType) {
            assertFalse(FiltroContenidoAnalizable.esAnalizable(contentType, "GET", 200), "assertFalse failed at FiltroContenidoAnalizableTest.java:46");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "audio/mpeg",
            "audio/ogg",
            "audio/wav",
            "audio/flac",
            "audio/aac"
        })
        @DisplayName("Tipos audio/* no son analizables")
        void audioTypes_noSonAnalizables(String contentType) {
            assertFalse(FiltroContenidoAnalizable.esAnalizable(contentType, "GET", 200), "assertFalse failed at FiltroContenidoAnalizableTest.java:59");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "font/woff",
            "font/woff2",
            "font/ttf",
            "font/otf",
            "font/eot"
        })
        @DisplayName("Tipos font/* no son analizables")
        void fontTypes_noSonAnalizables(String contentType) {
            assertFalse(FiltroContenidoAnalizable.esAnalizable(contentType, "GET", 200), "assertFalse failed at FiltroContenidoAnalizableTest.java:72");
        }
    }

    @Nested
    @DisplayName("Por tipo de contenido binario/archivo")
    class BinariosTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "application/octet-stream",
            "application/zip",
            "application/x-gzip",
            "application/gzip",
            "application/pdf",
            "application/java-archive",
            "application/x-rar-compressed",
            "application/vnd.android.package-archive",
            "application/vnd.ms-fontobject",
            "application/x-tar",
            "application/x-7z-compressed",
            "application/x-bzip2",
            "application/x-bzip",
            "application/x-shockwave-flash",
            "application/x-msdownload",
            "application/x-iso9660-image",
            "application/x-msi",
            "application/x-dosexec",
            "application/x-executable",
            "application/x-sharedlib",
            "application/epub+zip",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        })
        @DisplayName("Tipos binarios/archivo no son analizables")
        void binaryTypes_noSonAnalizables(String contentType) {
            assertFalse(FiltroContenidoAnalizable.esAnalizable(contentType, "GET", 200), "assertFalse failed at FiltroContenidoAnalizableTest.java:112");
        }
    }

    @Nested
    @DisplayName("Por código de estado HTTP")
    class CodigoEstadoTests {

        @ParameterizedTest
        @ValueSource(ints = {100, 101, 102, 103})
        @DisplayName("Códigos 1xx (Informational) no son analizables")
        void codigos1xx_noSonAnalizables(int codigo) {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("text/html", "GET", codigo), "assertFalse failed at FiltroContenidoAnalizableTest.java:124");
        }

        @Test
        @DisplayName("Código 204 (No Content) no es analizable")
        void codigo204_noEsAnalizable() {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("text/html", "GET", 204), "assertFalse failed at FiltroContenidoAnalizableTest.java:130");
        }

        @Test
        @DisplayName("Código 304 (Not Modified) no es analizable")
        void codigo304_noEsAnalizable() {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("text/html", "GET", 304), "assertFalse failed at FiltroContenidoAnalizableTest.java:136");
        }

        @ParameterizedTest
        @CsvSource({
            "text/html, GET, 200",
            "text/html, GET, 201",
            "text/html, GET, 301",
            "text/html, GET, 400",
            "application/json, GET, 401",
            "application/json, GET, 403",
            "text/html, GET, 404",
            "application/json, GET, 500",
            "application/json, GET, 502",
            "application/json, GET, 503"
        })
        @DisplayName("Códigos con contenido son analizables")
        void codigosConContenido_sonAnalizables(String contentType, String metodo, int codigo) {
            assertTrue(FiltroContenidoAnalizable.esAnalizable(contentType, metodo, codigo), "assertTrue failed at FiltroContenidoAnalizableTest.java:154");
        }

        @Test
        @DisplayName("Códigos negativos se consideran analizables (sin cuerpo definido)")
        void codigoNegativo_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("text/html", "GET", -1), "assertTrue failed at FiltroContenidoAnalizableTest.java:160");
        }

        @Test
        @DisplayName("Código cero se considera analizable (sin cuerpo definido)")
        void codigoCero_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("text/html", "GET", 0), "assertTrue failed at FiltroContenidoAnalizableTest.java:166");
        }

        @Test
        @DisplayName("Código muy alto se considera analizable")
        void codigoAlto_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("text/html", "GET", 999), "assertTrue failed at FiltroContenidoAnalizableTest.java:172");
        }
    }

    @Nested
    @DisplayName("Por método HTTP")
    class MetodoHttpTests {

        @Test
        @DisplayName("HEAD se omite por ser no analizable")
        void head_seOmite() {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("text/html", "HEAD", 200), "assertFalse failed at FiltroContenidoAnalizableTest.java:183");
        }

        @Test
        @DisplayName("HEAD en minúsculas también se omite")
        void headMinuscula_seOmite() {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("text/html", "head", 200), "assertFalse failed at FiltroContenidoAnalizableTest.java:189");
        }

        @Test
        @DisplayName("HEAD en mixto también se omite")
        void headMixto_seOmite() {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("text/html", "HeAd", 200), "assertFalse failed at FiltroContenidoAnalizableTest.java:195");
        }

        @Test
        @DisplayName("Método null se considera analizable")
        void metodoNull_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("text/html", null, 200), "assertTrue failed at FiltroContenidoAnalizableTest.java:201");
        }

        @Test
        @DisplayName("Método vacío se considera analizable")
        void metodoVacio_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("text/html", "", 200), "assertTrue failed at FiltroContenidoAnalizableTest.java:207");
        }

        @Test
        @DisplayName("Método solo espacios se considera analizable")
        void metodoSoloEspacios_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("text/html", "   ", 200), "assertTrue failed at FiltroContenidoAnalizableTest.java:213");
        }

        @ParameterizedTest
        @CsvSource({
            "text/html, GET",
            "application/json, POST",
            "application/xml, PUT",
            "text/plain, DELETE",
            "application/json, PATCH"
        })
        @DisplayName("Métodos estándar con texto son analizables")
        void metodosEstandar_sonAnalizables(String contentType, String metodo) {
            assertTrue(FiltroContenidoAnalizable.esAnalizable(contentType, metodo, 200), "assertTrue failed at FiltroContenidoAnalizableTest.java:226");
        }
    }

    @Nested
    @DisplayName("Casos edge de Content-Type")
    class EdgeCasesContentTypeTests {

        @Test
        @DisplayName("text/html con charset es analizable")
        void textHtmlConCharset_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("text/html; charset=UTF-8", "GET", 200), "assertTrue failed at FiltroContenidoAnalizableTest.java:237");
        }

        @Test
        @DisplayName("Content-type con múltiples parámetros es analizable")
        void contentTypeConMultiplesParametros_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable(
                "text/html; charset=UTF-8; boundary=something", "GET", 200), "assertTrue failed at FiltroContenidoAnalizableTest.java:243");
        }

        @Test
        @DisplayName("Content-type con espacios alrededor de punto y coma es analizable")
        void contentTypeConEspaciosAlrededorSemicolon_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable(
                "text/html ; charset=UTF-8", "GET", 200), "assertTrue failed at FiltroContenidoAnalizableTest.java:250");
            assertTrue(FiltroContenidoAnalizable.esAnalizable(
                "text/html; charset=UTF-8 ", "GET", 200), "assertTrue failed at FiltroContenidoAnalizableTest.java:252");
        }

        @Test
        @DisplayName("Content-type en mayúsculas se normaliza correctamente")
        void contentTypeMayusculas_seNormaliza() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("TEXT/HTML", "GET", 200), "assertTrue failed at FiltroContenidoAnalizableTest.java:259");
            assertTrue(FiltroContenidoAnalizable.esAnalizable("APPLICATION/JSON", "GET", 200), "assertTrue failed at FiltroContenidoAnalizableTest.java:260");
            assertFalse(FiltroContenidoAnalizable.esAnalizable("IMAGE/JPEG", "GET", 200), "assertFalse failed at FiltroContenidoAnalizableTest.java:261");
            assertFalse(FiltroContenidoAnalizable.esAnalizable("APPLICATION/PDF", "GET", 200), "assertFalse failed at FiltroContenidoAnalizableTest.java:262");
        }

        @Test
        @DisplayName("Content-type mixto mayúsculas/minúsculas se normaliza")
        void contentTypeMixto_seNormaliza() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("Text/Html", "GET", 200), "assertTrue failed at FiltroContenidoAnalizableTest.java:268");
            assertTrue(FiltroContenidoAnalizable.esAnalizable("Application/Json", "GET", 200), "assertTrue failed at FiltroContenidoAnalizableTest.java:269");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Sin content-type se considera analizable")
        void sinContentType_esAnalizable(String contentType) {
            assertTrue(FiltroContenidoAnalizable.esAnalizable(contentType, "GET", 200), "assertTrue failed at FiltroContenidoAnalizableTest.java:276");
        }

        @Test
        @DisplayName("Content-type solo con espacios se considera analizable")
        void contentTypeSoloEspacios_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("   ", "GET", 200), "assertTrue failed at FiltroContenidoAnalizableTest.java:282");
        }

        @Test
        @DisplayName("Content-type con solo punto y coma se considera analizable")
        void contentTypeConSoloSemicolon_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable(";", "GET", 200), "assertTrue failed at FiltroContenidoAnalizableTest.java:288");
        }

        @Test
        @DisplayName("Content-type terminando en punto y coma se maneja correctamente")
        void contentTypeTerminandoEnSemicolon_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("text/html;", "GET", 200), "assertTrue failed at FiltroContenidoAnalizableTest.java:294");
        }
    }

    @Nested
    @DisplayName("Tipos de contenido analizables")
    class TiposAnalizablesTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "application/json",
            "application/xml",
            "text/plain",
            "text/html",
            "text/css",
            "text/javascript",
            "application/javascript",
            "application/x-www-form-urlencoded",
            "multipart/form-data",
            "application/ld+json",
            "application/rss+xml",
            "application/atom+xml",
            "text/xml",
            "text/csv"
        })
        @DisplayName("Tipos de contenido de texto son analizables")
        void textContentTypes_sonAnalizables(String contentType) {
            assertTrue(FiltroContenidoAnalizable.esAnalizable(contentType, "GET", 200), "assertTrue failed at FiltroContenidoAnalizableTest.java:321");
        }

        @Test
        @DisplayName("multipart/form-data es analizable")
        void multipartFormData_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("multipart/form-data", "POST", 200), "assertTrue failed at FiltroContenidoAnalizableTest.java:327");
        }

        @Test
        @DisplayName("application/x-www-form-urlencoded es analizable")
        void urlEncoded_esAnalizable() {
            assertTrue(FiltroContenidoAnalizable.esAnalizable("application/x-www-form-urlencoded", "POST", 200), "assertTrue failed at FiltroContenidoAnalizableTest.java:333");
        }
    }

    @Nested
    @DisplayName("Combinaciones de filtros")
    class CombinacionesTests {

        @Test
        @DisplayName("Binario con código de error sigue siendo no analizable")
        void binarioConError_noEsAnalizable() {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("application/pdf", "GET", 500), "assertFalse failed at FiltroContenidoAnalizableTest.java:344");
        }

        @Test
        @DisplayName("Texto con código sin contenido no es analizable")
        void textoConCodigoSinContenido_noEsAnalizable() {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("text/html", "GET", 204), "assertFalse failed at FiltroContenidoAnalizableTest.java:350");
        }

        @Test
        @DisplayName("HEAD con código de error sigue siendo no analizable")
        void headConError_noEsAnalizable() {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("text/html", "HEAD", 500), "assertFalse failed at FiltroContenidoAnalizableTest.java:356");
        }

        @Test
        @DisplayName("Multimedia con charset sigue siendo no analizable")
        void multimediaConCharset_noEsAnalizable() {
            assertFalse(FiltroContenidoAnalizable.esAnalizable("image/jpeg; charset=binary", "GET", 200), "assertFalse failed at FiltroContenidoAnalizableTest.java:362");
        }
    }
}
