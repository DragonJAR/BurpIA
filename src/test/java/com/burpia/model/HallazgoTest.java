package com.burpia.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.awt.Color;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Hallazgo Model Tests")
class HallazgoTest {

    @Test
    @DisplayName("Crear hallazgo con parametros basicos")
    void testCrearHallazgoBasico() {
        Hallazgo hallazgo = new Hallazgo(
            "https://example.com/test",
            "XSS detectado",
            "High",
            "Medium"
        );

        assertNotNull(hallazgo);
        assertEquals("https://example.com/test", hallazgo.obtenerUrl());
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
    @DisplayName("Convertir a fila de tabla")
    void testAFilaTabla() {
        Hallazgo hallazgo = new Hallazgo(
            "https://example.com/test",
            "SQL Injection",
            "Critical",
            "High"
        );

        Object[] fila = hallazgo.aFilaTabla();
        
        assertEquals(5, fila.length);
        assertNotNull(fila[0]);
        assertEquals("https://example.com/test", fila[1]);
        assertEquals("SQL Injection", fila[2]);
        assertEquals("Critical", fila[3]);
        assertEquals("High", fila[4]);
    }

    @Test
    @DisplayName("Colores de severidad y confianza toleran null")
    void testColoresToleranNull() {
        assertEquals(Color.GRAY, Hallazgo.obtenerColorSeveridad(null));
        assertEquals(Color.GRAY, Hallazgo.obtenerColorConfianza(null));
    }

    @Test
    @DisplayName("Normaliza severidad y confianza a valores canonicos")
    void testNormalizacionCanonica() {
        assertEquals(Hallazgo.SEVERIDAD_HIGH, Hallazgo.normalizarSeveridad("alta"));
        assertEquals(Hallazgo.SEVERIDAD_CRITICAL, Hallazgo.normalizarSeveridad("critical"));
        assertEquals(Hallazgo.SEVERIDAD_INFO, Hallazgo.normalizarSeveridad(null));
        assertEquals(Hallazgo.CONFIANZA_ALTA, Hallazgo.normalizarConfianza("certain"));
        assertEquals(Hallazgo.CONFIANZA_BAJA, Hallazgo.normalizarConfianza("low"));
        assertEquals(Hallazgo.CONFIANZA_MEDIA, Hallazgo.normalizarConfianza(null));
    }
}
