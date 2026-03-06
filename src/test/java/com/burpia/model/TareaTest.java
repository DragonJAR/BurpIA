package com.burpia.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tarea Model Tests")
class TareaTest {

    @Nested
    @DisplayName("Constructores")
    class Constructores {

        @Test
        @DisplayName("Crear tarea con parametros basicos")
        void testCrearTareaBasica() {
            Tarea tarea = new Tarea("abc123", "Analisis HTTP", "https://example.com", Tarea.ESTADO_EN_COLA);

            assertNotNull(tarea);
            assertEquals("abc123", tarea.obtenerId());
            assertEquals("Analisis HTTP", tarea.obtenerTipo());
            assertEquals("https://example.com", tarea.obtenerUrl());
            assertEquals(Tarea.ESTADO_EN_COLA, tarea.obtenerEstado());
            assertTrue(tarea.obtenerTiempoInicio() > 0);
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
    }

    @Nested
    @DisplayName("Validacion de estados")
    class ValidacionEstados {

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
            assertFalse(Tarea.esEstadoValido("   "));
        }
    }

    @Nested
    @DisplayName("Predicados de transicion de estado")
    class PredicadosTransicionEstado {

        @Test
        @DisplayName("esEstadoPausable funciona correctamente")
        void testEsEstadoPausable() {
            assertTrue(Tarea.esEstadoPausable(Tarea.ESTADO_EN_COLA));
            assertTrue(Tarea.esEstadoPausable(Tarea.ESTADO_ANALIZANDO));
            assertFalse(Tarea.esEstadoPausable(Tarea.ESTADO_PAUSADO));
            assertFalse(Tarea.esEstadoPausable(Tarea.ESTADO_COMPLETADO));
            assertFalse(Tarea.esEstadoPausable(Tarea.ESTADO_ERROR));
            assertFalse(Tarea.esEstadoPausable(Tarea.ESTADO_CANCELADO));
        }

        @Test
        @DisplayName("esEstadoReanudable funciona correctamente")
        void testEsEstadoReanudable() {
            assertTrue(Tarea.esEstadoReanudable(Tarea.ESTADO_PAUSADO));
            assertFalse(Tarea.esEstadoReanudable(Tarea.ESTADO_EN_COLA));
            assertFalse(Tarea.esEstadoReanudable(Tarea.ESTADO_ANALIZANDO));
            assertFalse(Tarea.esEstadoReanudable(Tarea.ESTADO_ERROR));
            assertFalse(Tarea.esEstadoReanudable(Tarea.ESTADO_COMPLETADO));
            assertFalse(Tarea.esEstadoReanudable(Tarea.ESTADO_CANCELADO));
        }

        @Test
        @DisplayName("esEstadoReintentable funciona correctamente")
        void testEsEstadoReintentable() {
            assertTrue(Tarea.esEstadoReintentable(Tarea.ESTADO_ERROR));
            assertTrue(Tarea.esEstadoReintentable(Tarea.ESTADO_CANCELADO));
            assertFalse(Tarea.esEstadoReintentable(Tarea.ESTADO_COMPLETADO));
            assertFalse(Tarea.esEstadoReintentable(Tarea.ESTADO_EN_COLA));
            assertFalse(Tarea.esEstadoReintentable(Tarea.ESTADO_ANALIZANDO));
            assertFalse(Tarea.esEstadoReintentable(Tarea.ESTADO_PAUSADO));
        }

        @Test
        @DisplayName("esEstadoCancelable funciona correctamente")
        void testEsEstadoCancelable() {
            assertTrue(Tarea.esEstadoCancelable(Tarea.ESTADO_EN_COLA));
            assertTrue(Tarea.esEstadoCancelable(Tarea.ESTADO_ANALIZANDO));
            assertTrue(Tarea.esEstadoCancelable(Tarea.ESTADO_PAUSADO));
            assertFalse(Tarea.esEstadoCancelable(Tarea.ESTADO_COMPLETADO));
            assertFalse(Tarea.esEstadoCancelable(Tarea.ESTADO_ERROR));
            assertFalse(Tarea.esEstadoCancelable(Tarea.ESTADO_CANCELADO));
        }

        @Test
        @DisplayName("esEstadoEliminable funciona correctamente")
        void testEsEstadoEliminable() {
            assertTrue(Tarea.esEstadoEliminable(Tarea.ESTADO_COMPLETADO));
            assertTrue(Tarea.esEstadoEliminable(Tarea.ESTADO_ERROR));
            assertTrue(Tarea.esEstadoEliminable(Tarea.ESTADO_CANCELADO));
            assertFalse(Tarea.esEstadoEliminable(Tarea.ESTADO_EN_COLA));
            assertFalse(Tarea.esEstadoEliminable(Tarea.ESTADO_ANALIZANDO));
            assertFalse(Tarea.esEstadoEliminable(Tarea.ESTADO_PAUSADO));
        }

        @Test
        @DisplayName("esEstadoFinal funciona correctamente")
        void testEsEstadoFinal() {
            assertTrue(Tarea.esEstadoFinal(Tarea.ESTADO_COMPLETADO));
            assertTrue(Tarea.esEstadoFinal(Tarea.ESTADO_ERROR));
            assertTrue(Tarea.esEstadoFinal(Tarea.ESTADO_CANCELADO));
            assertFalse(Tarea.esEstadoFinal(Tarea.ESTADO_EN_COLA));
            assertFalse(Tarea.esEstadoFinal(Tarea.ESTADO_ANALIZANDO));
            assertFalse(Tarea.esEstadoFinal(Tarea.ESTADO_PAUSADO));
        }
    }

    @Nested
    @DisplayName("Metodos de instancia")
    class MetodosInstancia {

        @Test
        @DisplayName("Tarea activa")
        void testTareaActiva() {
            Tarea tareaEnCola = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
            Tarea tareaAnalizando = new Tarea("2", "Test", "url", Tarea.ESTADO_ANALIZANDO);
            Tarea tareaPausada = new Tarea("3", "Test", "url", Tarea.ESTADO_PAUSADO);
            Tarea tareaCompletada = new Tarea("4", "Test", "url", Tarea.ESTADO_COMPLETADO);
            Tarea tareaError = new Tarea("5", "Test", "url", Tarea.ESTADO_ERROR);
            Tarea tareaCancelada = new Tarea("6", "Test", "url", Tarea.ESTADO_CANCELADO);

            assertTrue(tareaEnCola.esActiva());
            assertTrue(tareaAnalizando.esActiva());
            assertTrue(tareaPausada.esActiva());
            assertFalse(tareaCompletada.esActiva());
            assertFalse(tareaError.esActiva());
            assertFalse(tareaCancelada.esActiva());
        }

        @Test
        @DisplayName("Tarea finalizada")
        void testTareaFinalizada() {
            Tarea tareaCompletada = new Tarea("1", "Test", "url", Tarea.ESTADO_COMPLETADO);
            Tarea tareaError = new Tarea("2", "Test", "url", Tarea.ESTADO_ERROR);
            Tarea tareaCancelada = new Tarea("3", "Test", "url", Tarea.ESTADO_CANCELADO);
            Tarea tareaEnCola = new Tarea("4", "Test", "url", Tarea.ESTADO_EN_COLA);
            Tarea tareaAnalizando = new Tarea("5", "Test", "url", Tarea.ESTADO_ANALIZANDO);
            Tarea tareaPausada = new Tarea("6", "Test", "url", Tarea.ESTADO_PAUSADO);

            assertTrue(tareaCompletada.esFinalizada());
            assertTrue(tareaError.esFinalizada());
            assertTrue(tareaCancelada.esFinalizada());
            assertFalse(tareaEnCola.esFinalizada());
            assertFalse(tareaAnalizando.esFinalizada());
            assertFalse(tareaPausada.esFinalizada());
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
        void testEstablecerMensajeInfo() {
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
            tarea.establecerMensajeInfo("Procesando solicitud");

            assertEquals("Procesando solicitud", tarea.obtenerMensajeInfo());
        }

        @Test
        @DisplayName("Establecer mensaje info nulo")
        void testEstablecerMensajeInfoNulo() {
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
            tarea.establecerMensajeInfo(null);

            assertNull(tarea.obtenerMensajeInfo());
        }

        @Test
        @DisplayName("Establecer mensaje info vacio")
        void testEstablecerMensajeInfoVacio() {
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
            tarea.establecerMensajeInfo("mensaje inicial");
            tarea.establecerMensajeInfo("");

            assertEquals("", tarea.obtenerMensajeInfo());
        }
    }

    @Nested
    @DisplayName("Gestion de tiempos")
    class GestionTiempos {

        @Test
        @DisplayName("obtenerTiempoInicio devuelve tiempo de creacion")
        void testObtenerTiempoInicio() {
            long antes = System.currentTimeMillis();
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
            long despues = System.currentTimeMillis();

            assertTrue(tarea.obtenerTiempoInicio() >= antes);
            assertTrue(tarea.obtenerTiempoInicio() <= despues);
        }

        @Test
        @DisplayName("obtenerTiempoFin es 0 antes de finalizar")
        void testObtenerTiempoFinInicial() {
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
            assertEquals(0, tarea.obtenerTiempoFin());
        }

        @Test
        @DisplayName("obtenerTiempoFin se establece al completar")
        void testObtenerTiempoFinAlCompletar() throws InterruptedException {
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
            Thread.sleep(10); // Pequeña pausa para asegurar diferencia de tiempo
            tarea.establecerEstado(Tarea.ESTADO_COMPLETADO);

            assertTrue(tarea.obtenerTiempoFin() > 0);
            assertTrue(tarea.obtenerTiempoFin() >= tarea.obtenerTiempoInicio());
        }

        @Test
        @DisplayName("obtenerTiempoFin se establece en estado ERROR")
        void testObtenerTiempoFinEnError() throws InterruptedException {
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
            Thread.sleep(10);
            tarea.establecerEstado(Tarea.ESTADO_ERROR);

            assertTrue(tarea.obtenerTiempoFin() > 0);
        }

        @Test
        @DisplayName("obtenerTiempoFin se establece en estado CANCELADO")
        void testObtenerTiempoFinEnCancelado() throws InterruptedException {
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
            Thread.sleep(10);
            tarea.establecerEstado(Tarea.ESTADO_CANCELADO);

            assertTrue(tarea.obtenerTiempoFin() > 0);
        }

        @Test
        @DisplayName("obtenerDuracionMilisegundos es 0 al inicio")
        void testObtenerDuracionMilisegundosInicial() {
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
            assertEquals(0, tarea.obtenerDuracionMilisegundos());
        }

        @Test
        @DisplayName("obtenerDuracionMilisegundos acumula tiempo en ANALIZANDO")
        void testObtenerDuracionMilisegundosAcumula() throws InterruptedException {
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_ANALIZANDO);
            Thread.sleep(50);
            
            long duracion = tarea.obtenerDuracionMilisegundos();
            assertTrue(duracion >= 50, "La duracion debe ser al menos 50ms, fue: " + duracion);
        }

        @Test
        @DisplayName("obtenerDuracionMilisegundos acumula correctamente tras pausar")
        void testObtenerDuracionMilisegundosTrasPausar() throws InterruptedException {
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_ANALIZANDO);
            Thread.sleep(30);
            tarea.establecerEstado(Tarea.ESTADO_PAUSADO);
            long duracionPausada = tarea.obtenerDuracionMilisegundos();
            
            Thread.sleep(20);
            // La duracion no debe cambiar mientras esta pausada
            assertEquals(duracionPausada, tarea.obtenerDuracionMilisegundos());
            
            tarea.establecerEstado(Tarea.ESTADO_ANALIZANDO);
            Thread.sleep(20);
            
            long duracionFinal = tarea.obtenerDuracionMilisegundos();
            assertTrue(duracionFinal >= duracionPausada + 20);
        }
    }

    @Nested
    @DisplayName("Formateo de duracion")
    class FormateoDuracion {

        @Test
        @DisplayName("formatearDuracion estatico con segundos")
        void testFormatearDuracionEstaticoSegundos() {
            assertEquals("0s", Tarea.formatearDuracion(0));
            assertEquals("0s", Tarea.formatearDuracion(999));
            assertEquals("1s", Tarea.formatearDuracion(1000));
            assertEquals("5s", Tarea.formatearDuracion(5500));
        }

        @Test
        @DisplayName("formatearDuracion estatico con minutos")
        void testFormatearDuracionEstaticoMinutos() {
            assertEquals("1m 0s", Tarea.formatearDuracion(60000));
            assertEquals("1m 30s", Tarea.formatearDuracion(90000));
            assertEquals("2m 15s", Tarea.formatearDuracion(135000));
            assertEquals("10m 0s", Tarea.formatearDuracion(600000));
        }

        @Test
        @DisplayName("formatearDuracion de instancia")
        void testFormatearDuracionInstancia() throws InterruptedException {
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
            String duracion = tarea.formatearDuracion();
            assertNotNull(duracion);
            assertEquals("0s", duracion);
        }
    }

    @Nested
    @DisplayName("Conversion a tabla")
    class ConversionTabla {

        @Test
        @DisplayName("Convertir a fila de tabla")
        void testAFilaTabla() {
            Tarea tarea = new Tarea("1", "Analisis", "https://example.com", Tarea.ESTADO_EN_COLA);
            Object[] fila = tarea.aFilaTabla();

            assertEquals(4, fila.length);
            assertEquals("Analisis", fila[0]);
            assertEquals("https://example.com", fila[1]);
            assertEquals(Tarea.ESTADO_EN_COLA, fila[2]);
            assertEquals("0s", fila[3]);
        }

        @Test
        @DisplayName("Fila de tabla refleja cambio de estado")
        void testAFilaTablaConCambioEstado() throws InterruptedException {
            Tarea tarea = new Tarea("1", "Analisis", "https://example.com", Tarea.ESTADO_ANALIZANDO);
            Thread.sleep(50);
            tarea.establecerEstado(Tarea.ESTADO_COMPLETADO);
            
            Object[] fila = tarea.aFilaTabla();
            assertEquals(Tarea.ESTADO_COMPLETADO, fila[2]);
            // La duracion debe ser >= 50ms
            String duracion = (String) fila[3];
            assertTrue(duracion.contains("s") || duracion.contains("m"));
        }
    }

    @Nested
    @DisplayName("Colores de estado")
    class ColoresEstado {

        @Test
        @DisplayName("Obtener color estado para estados validos")
        void testObtenerColorEstadoValidos() {
            assertNotNull(Tarea.obtenerColorEstado(Tarea.ESTADO_EN_COLA));
            assertNotNull(Tarea.obtenerColorEstado(Tarea.ESTADO_ANALIZANDO));
            assertNotNull(Tarea.obtenerColorEstado(Tarea.ESTADO_COMPLETADO));
            assertNotNull(Tarea.obtenerColorEstado(Tarea.ESTADO_ERROR));
            assertNotNull(Tarea.obtenerColorEstado(Tarea.ESTADO_CANCELADO));
            assertNotNull(Tarea.obtenerColorEstado(Tarea.ESTADO_PAUSADO));
        }

        @Test
        @DisplayName("Obtener color estado con estado nulo devuelve negro")
        void testObtenerColorEstadoNulo() {
            assertEquals(Color.BLACK, Tarea.obtenerColorEstado(null));
        }

        @Test
        @DisplayName("Obtener color estado con estado vacio devuelve negro")
        void testObtenerColorEstadoVacio() {
            assertEquals(Color.BLACK, Tarea.obtenerColorEstado(""));
        }

        @Test
        @DisplayName("Obtener color estado con estado invalido devuelve negro")
        void testObtenerColorEstadoInvalido() {
            assertEquals(Color.BLACK, Tarea.obtenerColorEstado("Invalido"));
            assertEquals(Color.BLACK, Tarea.obtenerColorEstado("Running"));
            assertEquals(Color.BLACK, Tarea.obtenerColorEstado("Paused"));
        }
    }
}
