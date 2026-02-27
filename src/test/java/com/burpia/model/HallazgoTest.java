package com.burpia.model;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.burpia.i18n.I18nUI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;



@DisplayName("Hallazgo Model Tests")
class HallazgoTest {

    @Test
    @DisplayName("Crear hallazgo con parametros basicos")
    void testCrearHallazgoBasico() {
        Hallazgo hallazgo = new Hallazgo(
            "https://example.com/test",
            "XSS",
            "XSS detectado",
            "High",
            "Medium"
        );

        assertNotNull(hallazgo);
        assertEquals("https://example.com/test", hallazgo.obtenerUrl());
        assertEquals("XSS", hallazgo.obtenerTitulo());
        assertEquals("XSS detectado", hallazgo.obtenerHallazgo());
        assertEquals("High", hallazgo.obtenerSeveridad());
        assertEquals("Medium", hallazgo.obtenerConfianza());
    }

    @Test
    @DisplayName("Validar severidades validas")
    void testSeveridadesValidas() {
        assertTrue(Hallazgo.esSeveridadValida("Critical"));
        assertTrue(Hallazgo.esSeveridadValida("High"));
        assertTrue(Hallazgo.esSeveridadValida("Medium"));
        assertTrue(Hallazgo.esSeveridadValida("Low"));
        assertTrue(Hallazgo.esSeveridadValida("Info"));
    }

    @Test
    @DisplayName("Rechazar severidades invalidas")
    void testSeveridadesInvalidas() {
        assertFalse(Hallazgo.esSeveridadValida("Alta"));
        assertFalse(Hallazgo.esSeveridadValida("alta"));
        assertFalse(Hallazgo.esSeveridadValida("CRITICAL"));
        assertFalse(Hallazgo.esSeveridadValida(null));
        assertFalse(Hallazgo.esSeveridadValida(""));
    }

    @Test
    @DisplayName("Validar confianzas validas")
    void testConfianzasValidas() {
        assertTrue(Hallazgo.esConfianzaValida("High"));
        assertTrue(Hallazgo.esConfianzaValida("Medium"));
        assertTrue(Hallazgo.esConfianzaValida("Low"));
    }

    @Test
    @DisplayName("Rechazar confianzas invalidas")
    void testConfianzasInvalidas() {
        assertFalse(Hallazgo.esConfianzaValida("Alta"));
        assertFalse(Hallazgo.esConfianzaValida("alta"));
        assertFalse(Hallazgo.esConfianzaValida(null));
        assertFalse(Hallazgo.esConfianzaValida(""));
    }

    @Test
    @DisplayName("Obtener peso de severidad correctamente")
    void testPesoSeveridad() {
        assertEquals(5, Hallazgo.obtenerPesoSeveridad("Critical"));
        assertEquals(4, Hallazgo.obtenerPesoSeveridad("High"));
        assertEquals(3, Hallazgo.obtenerPesoSeveridad("Medium"));
        assertEquals(2, Hallazgo.obtenerPesoSeveridad("Low"));
        assertEquals(1, Hallazgo.obtenerPesoSeveridad("Info"));
        assertEquals(0, Hallazgo.obtenerPesoSeveridad("Unknown"));
        assertEquals(0, Hallazgo.obtenerPesoSeveridad(null));
    }

    @Test
    @DisplayName("Prioridad de severidad soporta etiquetas traducidas")
    void testPrioridadSeveridadTraducida() {
        assertEquals(5, Hallazgo.obtenerPrioridadSeveridad("Crítica"));
        assertEquals(4, Hallazgo.obtenerPrioridadSeveridad("Alta"));
        assertEquals(3, Hallazgo.obtenerPrioridadSeveridad("Media"));
        assertEquals(2, Hallazgo.obtenerPrioridadSeveridad("Baja"));
        assertEquals(1, Hallazgo.obtenerPrioridadSeveridad("Información"));
    }

    @Test
    @DisplayName("Prioridad de confianza soporta etiquetas traducidas")
    void testPrioridadConfianzaTraducida() {
        assertEquals(3, Hallazgo.obtenerPrioridadConfianza("Alta"));
        assertEquals(2, Hallazgo.obtenerPrioridadConfianza("Media"));
        assertEquals(1, Hallazgo.obtenerPrioridadConfianza("Baja"));
        assertEquals(0, Hallazgo.obtenerPrioridadConfianza("Desconocida"));
    }

    @Test
    @DisplayName("Convertir a fila de tabla")
    void testAFilaTabla() {
        Hallazgo hallazgo = new Hallazgo(
            "https://example.com/test",
            "SQLi",
            "SQL Injection description",
            "Critical",
            "High"
        );

        Object[] fila = hallazgo.aFilaTabla();

        assertEquals(5, fila.length);
        assertNotNull(fila[0]);
        assertEquals("https://example.com/test", fila[1]);
        assertEquals("SQLi", fila[2]);
        assertEquals(I18nUI.Hallazgos.TRADUCIR_SEVERIDAD(Hallazgo.SEVERIDAD_CRITICAL), fila[3]);
        assertEquals(I18nUI.Hallazgos.TRADUCIR_CONFIANZA(Hallazgo.CONFIANZA_ALTA), fila[4]);
    }

    @Test
    @DisplayName("Editar mantiene evidencia y solicitud HTTP asociada")
    void testEditarMantieneEvidenciaYSolicitud() {
        HttpRequest solicitud = mock(HttpRequest.class);
        HttpRequestResponse evidencia = mock(HttpRequestResponse.class);

        Hallazgo original = new Hallazgo(
            "10:20:30",
            "https://example.com/original",
            "Titulo original",
            "Descripcion original",
            Hallazgo.SEVERIDAD_LOW,
            Hallazgo.CONFIANZA_MEDIA,
            solicitud,
            evidencia
        );

        Hallazgo editado = original.editar(
            "https://example.com/editado",
            "Titulo editado",
            "Descripcion editada",
            Hallazgo.SEVERIDAD_HIGH,
            Hallazgo.CONFIANZA_ALTA
        );

        assertEquals("10:20:30", editado.obtenerHoraDescubrimiento());
        assertSame(solicitud, editado.obtenerSolicitudHttp());
        assertSame(evidencia, editado.obtenerEvidenciaHttp());
        assertEquals("https://example.com/editado", editado.obtenerUrl());
        assertEquals("Titulo editado", editado.obtenerTitulo());
        assertEquals("Descripcion editada", editado.obtenerHallazgo());
    }
}
