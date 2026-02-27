package com.burpia.util;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;




@DisplayName("FiltroContenidoAnalizable Tests")
class FiltroContenidoAnalizableTest {

    @Test
    @DisplayName("image/jpeg no es analizable")
    void imageJpeg_noEsAnalizable() {
        assertFalse(FiltroContenidoAnalizable.esAnalizable("image/jpeg", "GET", 200));
    }

    @Test
    @DisplayName("application/octet-stream no es analizable")
    void octetStream_noEsAnalizable() {
        assertFalse(FiltroContenidoAnalizable.esAnalizable("application/octet-stream", "GET", 200));
    }

    @Test
    @DisplayName("text/html es analizable")
    void textHtml_esAnalizable() {
        assertTrue(FiltroContenidoAnalizable.esAnalizable("text/html; charset=UTF-8", "GET", 200));
    }

    @Test
    @DisplayName("HEAD se omite por ser no analizable")
    void head_seOmite() {
        assertFalse(FiltroContenidoAnalizable.esAnalizable("text/html", "HEAD", 200));
    }

    @Test
    @DisplayName("Sin content-type se considera analizable")
    void sinContentType_esAnalizable() {
        assertTrue(FiltroContenidoAnalizable.esAnalizable(null, "GET", 200));
    }
}
