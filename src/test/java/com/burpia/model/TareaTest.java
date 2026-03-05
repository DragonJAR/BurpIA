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
    @DisplayName("Constructor con ID nulo lanza excepcion")
    void testConstructorIdNulo() {
        assertThrows(IllegalArgumentException.class, () ->
                new Tarea(null, "Test", "url", Tarea.ESTADO_EN_COLA));
    }

    @Test
    @DisplayName("Constructor con ID vacio lanza excepcion")
    void testConstructorIdVacio() {
        assertThrows(IllegalArgumentException.class, () ->
                new Tarea("", "Test", "url", Tarea.ESTADO_EN_COLA));
        assertThrows(IllegalArgumentException.class, () ->
                new Tarea("   ", "Test", "url", Tarea.ESTADO_EN_COLA));
    }

    @Test
    @DisplayName("Constructor con tipo nulo lanza excepcion")
    void testConstructorTipoNulo() {
        assertThrows(IllegalArgumentException.class, () ->
                new Tarea("id", null, "url", Tarea.ESTADO_EN_COLA));
    }

    @Test
    @DisplayName("Constructor con tipo vacio lanza excepcion")
    void testConstructorTipoVacio() {
        assertThrows(IllegalArgumentException.class, () ->
                new Tarea("id", "", "url", Tarea.ESTADO_EN_COLA));
    }

    @Test
    @DisplayName("Constructor con URL nula lanza excepcion")
    void testConstructorUrlNula() {
        assertThrows(IllegalArgumentException.class, () ->
                new Tarea("id", "Test", null, Tarea.ESTADO_EN_COLA));
    }

    @Test
    @DisplayName("Constructor con URL vacia lanza excepcion")
    void testConstructorUrlVacia() {
        assertThrows(IllegalArgumentException.class, () ->
                new Tarea("id", "Test", "", Tarea.ESTADO_EN_COLA));
    }

    @Test
    @DisplayName("Constructor con estado invalido establece ERROR")
    void testConstructorEstadoInvalido() {
        Tarea tarea = new Tarea("id", "Test", "url", "EstadoInvalido");
        assertEquals(Tarea.ESTADO_ERROR, tarea.obtenerEstado());
    }

    @Test
    @DisplayName("Constructor con estado nulo establece ERROR")
    void testConstructorEstadoNulo() {
        Tarea tarea = new Tarea("id", "Test", "url", null);
        assertEquals(Tarea.ESTADO_ERROR, tarea.obtenerEstado());
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
    @DisplayName("Predicados de transición de estado son consistentes")
    void testPredicadosTransicionEstado() {
        assertTrue(Tarea.esEstadoPausable(Tarea.ESTADO_EN_COLA));
        assertTrue(Tarea.esEstadoPausable(Tarea.ESTADO_ANALIZANDO));
        assertFalse(Tarea.esEstadoPausable(Tarea.ESTADO_PAUSADO));

        assertTrue(Tarea.esEstadoReanudable(Tarea.ESTADO_PAUSADO));
        assertFalse(Tarea.esEstadoReanudable(Tarea.ESTADO_ERROR));

        assertTrue(Tarea.esEstadoReintentable(Tarea.ESTADO_ERROR));
        assertTrue(Tarea.esEstadoReintentable(Tarea.ESTADO_CANCELADO));
        assertFalse(Tarea.esEstadoReintentable(Tarea.ESTADO_COMPLETADO));

        assertTrue(Tarea.esEstadoCancelable(Tarea.ESTADO_EN_COLA));
        assertTrue(Tarea.esEstadoCancelable(Tarea.ESTADO_ANALIZANDO));
        assertTrue(Tarea.esEstadoCancelable(Tarea.ESTADO_PAUSADO));
        assertFalse(Tarea.esEstadoCancelable(Tarea.ESTADO_COMPLETADO));

        assertTrue(Tarea.esEstadoEliminable(Tarea.ESTADO_COMPLETADO));
        assertTrue(Tarea.esEstadoEliminable(Tarea.ESTADO_ERROR));
        assertTrue(Tarea.esEstadoEliminable(Tarea.ESTADO_CANCELADO));
        assertFalse(Tarea.esEstadoEliminable(Tarea.ESTADO_EN_COLA));
    }

    @Test
    @DisplayName("Estados inválidos")
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
    @DisplayName("Establecer estado invalido establece ERROR")
    void testEstablecerEstadoInvalido() {
        Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
        tarea.establecerEstado("EstadoInvalido");

        assertEquals(Tarea.ESTADO_ERROR, tarea.obtenerEstado());
    }

    @Test
    @DisplayName("Establecer estado nulo establece ERROR")
    void testEstablecerEstadoNulo() {
        Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
        tarea.establecerEstado(null);

        assertEquals(Tarea.ESTADO_ERROR, tarea.obtenerEstado());
    }

    @Test
    @DisplayName("Establecer estado vacio establece ERROR")
    void testEstablecerEstadoVacio() {
        Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
        tarea.establecerEstado("");

        assertEquals(Tarea.ESTADO_ERROR, tarea.obtenerEstado());
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
        assertNotNull(fila[3]);
    }

    @Test
    @DisplayName("Obtener color estado con estado nulo")
    void testObtenerColorEstadoNulo() {
        assertEquals(java.awt.Color.BLACK, Tarea.obtenerColorEstado(null));
    }

    @Test
    @DisplayName("Obtener color estado con estado vacio")
    void testObtenerColorEstadoVacio() {
        assertEquals(java.awt.Color.BLACK, Tarea.obtenerColorEstado(""));
    }

    @Test
    @DisplayName("Obtener color estado con estado invalido")
    void testObtenerColorEstadoInvalido() {
        assertEquals(java.awt.Color.BLACK, Tarea.obtenerColorEstado("Invalido"));
    }
}
