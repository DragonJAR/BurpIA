package com.burpia.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tarea Model Tests")
class TareaTest {

    @Test
    @DisplayName("Crear tarea con parametros basicos")
    void testCrearTareaBasica() {
        Tarea tarea = new Tarea("abc123", "Analisis HTTP", "https://example.com", Tarea.ESTADO_EN_COLA);

        assertNotNull(tarea);
        assertEquals("abc123", tarea.obtenerId());
        assertEquals("Analisis HTTP", tarea.obtenerTipo());
        assertEquals("https://example.com", tarea.obtenerUrl());
        assertEquals(Tarea.ESTADO_EN_COLA, tarea.obtenerEstado());
    }

    @Test
    @DisplayName("Estados validos")
    void testEstadosValidos() {
        assertTrue(Tarea.esEstadoValido(Tarea.ESTADO_EN_COLA));
        assertTrue(Tarea.esEstadoValido(Tarea.ESTADO_ANALIZANDO));
        assertTrue(Tarea.esEstadoValido(Tarea.ESTADO_PAUSADO));
        assertTrue(Tarea.esEstadoValido(Tarea.ESTADO_COMPLETADO));
        assertTrue(Tarea.esEstadoValido(Tarea.ESTADO_ERROR));
        assertTrue(Tarea.esEstadoValido(Tarea.ESTADO_CANCELADO));
    }

    @Test
    @DisplayName("Estados invalidos")
    void testEstadosInvalidos() {
        assertFalse(Tarea.esEstadoValido("Running"));
        assertFalse(Tarea.esEstadoValido("Paused"));
        assertFalse(Tarea.esEstadoValido(null));
        assertFalse(Tarea.esEstadoValido(""));
    }

    @Test
    @DisplayName("Tarea activa")
    void testTareaActiva() {
        Tarea tareaEnCola = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
        Tarea tareaAnalizando = new Tarea("2", "Test", "url", Tarea.ESTADO_ANALIZANDO);
        Tarea tareaPausada = new Tarea("3", "Test", "url", Tarea.ESTADO_PAUSADO);
        Tarea tareaCompletada = new Tarea("4", "Test", "url", Tarea.ESTADO_COMPLETADO);

        assertTrue(tareaEnCola.esActiva());
        assertTrue(tareaAnalizando.esActiva());
        assertTrue(tareaPausada.esActiva());
        assertFalse(tareaCompletada.esActiva());
    }

    @Test
    @DisplayName("Tarea finalizada")
    void testTareaFinalizada() {
        Tarea tareaCompletada = new Tarea("1", "Test", "url", Tarea.ESTADO_COMPLETADO);
        Tarea tareaError = new Tarea("2", "Test", "url", Tarea.ESTADO_ERROR);
        Tarea tareaCancelada = new Tarea("3", "Test", "url", Tarea.ESTADO_CANCELADO);
        Tarea tareaEnCola = new Tarea("4", "Test", "url", Tarea.ESTADO_EN_COLA);

        assertTrue(tareaCompletada.esFinalizada());
        assertTrue(tareaError.esFinalizada());
        assertTrue(tareaCancelada.esFinalizada());
        assertFalse(tareaEnCola.esFinalizada());
    }

    @Test
    @DisplayName("Cambiar estado de tarea")
    void testCambiarEstado() {
        Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
        tarea.establecerEstado(Tarea.ESTADO_ANALIZANDO);
        
        assertEquals(Tarea.ESTADO_ANALIZANDO, tarea.obtenerEstado());
    }

    @Test
    @DisplayName("Establecer mensaje info")
    void testMensajeInfo() {
        Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
        tarea.establecerMensajeInfo("Procesando solicitud");
        
        assertEquals("Procesando solicitud", tarea.obtenerMensajeInfo());
    }

    @Test
    @DisplayName("Convertir a fila de tabla")
    void testAFilaTabla() {
        Tarea tarea = new Tarea("1", "Analisis", "https://example.com", Tarea.ESTADO_EN_COLA);
        Object[] fila = tarea.aFilaTabla();

        assertEquals(4, fila.length);
        assertEquals("Analisis", fila[0]);
        assertEquals("https://example.com", fila[1]);
        assertEquals(Tarea.ESTADO_EN_COLA, fila[2]);
        assertNotNull(fila[3]); // duracion
    }
}
