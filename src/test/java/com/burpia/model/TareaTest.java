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

            assertNotNull(tarea, "assertNotNull failed at TareaTest.java:23");
            assertEquals("abc123", tarea.obtenerId(), "assertEquals failed at TareaTest.java:24");
            assertEquals("Analisis HTTP", tarea.obtenerTipo(), "assertEquals failed at TareaTest.java:25");
            assertEquals("https://example.com", tarea.obtenerUrl(), "assertEquals failed at TareaTest.java:26");
            assertEquals(Tarea.ESTADO_EN_COLA, tarea.obtenerEstado(), "assertEquals failed at TareaTest.java:27");
            assertTrue(tarea.obtenerTiempoInicio() > 0, "assertTrue failed at TareaTest.java:28");
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
            assertEquals(Tarea.ESTADO_ERROR, tarea.obtenerEstado(), "assertEquals failed at TareaTest.java:79");
        }

        @Test
        @DisplayName("Constructor con estado nulo establece ERROR")
        void testConstructorEstadoNulo() {
            Tarea tarea = new Tarea("id", "Test", "url", null);
            assertEquals(Tarea.ESTADO_ERROR, tarea.obtenerEstado(), "assertEquals failed at TareaTest.java:86");
        }
    }

    @Nested
    @DisplayName("Validacion de estados")
    class ValidacionEstados {

        @Test
        @DisplayName("Estados validos")
        void testEstadosValidos() {
            assertTrue(Tarea.esEstadoValido(Tarea.ESTADO_EN_COLA), "assertTrue failed at TareaTest.java:97");
            assertTrue(Tarea.esEstadoValido(Tarea.ESTADO_ANALIZANDO), "assertTrue failed at TareaTest.java:98");
            assertTrue(Tarea.esEstadoValido(Tarea.ESTADO_PAUSADO), "assertTrue failed at TareaTest.java:99");
            assertTrue(Tarea.esEstadoValido(Tarea.ESTADO_COMPLETADO), "assertTrue failed at TareaTest.java:100");
            assertTrue(Tarea.esEstadoValido(Tarea.ESTADO_ERROR), "assertTrue failed at TareaTest.java:101");
            assertTrue(Tarea.esEstadoValido(Tarea.ESTADO_CANCELADO), "assertTrue failed at TareaTest.java:102");
        }

        @Test
        @DisplayName("Estados invalidos")
        void testEstadosInvalidos() {
            assertFalse(Tarea.esEstadoValido("Running"), "assertFalse failed at TareaTest.java:108");
            assertFalse(Tarea.esEstadoValido("Paused"), "assertFalse failed at TareaTest.java:109");
            assertFalse(Tarea.esEstadoValido(null), "assertFalse failed at TareaTest.java:110");
            assertFalse(Tarea.esEstadoValido(""), "assertFalse failed at TareaTest.java:111");
            assertFalse(Tarea.esEstadoValido("   "), "assertFalse failed at TareaTest.java:112");
        }
    }

    @Nested
    @DisplayName("Predicados de transicion de estado")
    class PredicadosTransicionEstado {

        @Test
        @DisplayName("esEstadoPausable funciona correctamente")
        void testEsEstadoPausable() {
            assertTrue(Tarea.esEstadoPausable(Tarea.ESTADO_EN_COLA), "assertTrue failed at TareaTest.java:123");
            assertTrue(Tarea.esEstadoPausable(Tarea.ESTADO_ANALIZANDO), "assertTrue failed at TareaTest.java:124");
            assertFalse(Tarea.esEstadoPausable(Tarea.ESTADO_PAUSADO), "assertFalse failed at TareaTest.java:125");
            assertFalse(Tarea.esEstadoPausable(Tarea.ESTADO_COMPLETADO), "assertFalse failed at TareaTest.java:126");
            assertFalse(Tarea.esEstadoPausable(Tarea.ESTADO_ERROR), "assertFalse failed at TareaTest.java:127");
            assertFalse(Tarea.esEstadoPausable(Tarea.ESTADO_CANCELADO), "assertFalse failed at TareaTest.java:128");
        }

        @Test
        @DisplayName("esEstadoReanudable funciona correctamente")
        void testEsEstadoReanudable() {
            assertTrue(Tarea.esEstadoReanudable(Tarea.ESTADO_PAUSADO), "assertTrue failed at TareaTest.java:134");
            assertFalse(Tarea.esEstadoReanudable(Tarea.ESTADO_EN_COLA), "assertFalse failed at TareaTest.java:135");
            assertFalse(Tarea.esEstadoReanudable(Tarea.ESTADO_ANALIZANDO), "assertFalse failed at TareaTest.java:136");
            assertFalse(Tarea.esEstadoReanudable(Tarea.ESTADO_ERROR), "assertFalse failed at TareaTest.java:137");
            assertFalse(Tarea.esEstadoReanudable(Tarea.ESTADO_COMPLETADO), "assertFalse failed at TareaTest.java:138");
            assertFalse(Tarea.esEstadoReanudable(Tarea.ESTADO_CANCELADO), "assertFalse failed at TareaTest.java:139");
        }

        @Test
        @DisplayName("esEstadoReintentable funciona correctamente")
        void testEsEstadoReintentable() {
            assertTrue(Tarea.esEstadoReintentable(Tarea.ESTADO_ERROR), "assertTrue failed at TareaTest.java:145");
            assertTrue(Tarea.esEstadoReintentable(Tarea.ESTADO_CANCELADO), "assertTrue failed at TareaTest.java:146");
            assertFalse(Tarea.esEstadoReintentable(Tarea.ESTADO_COMPLETADO), "assertFalse failed at TareaTest.java:147");
            assertFalse(Tarea.esEstadoReintentable(Tarea.ESTADO_EN_COLA), "assertFalse failed at TareaTest.java:148");
            assertFalse(Tarea.esEstadoReintentable(Tarea.ESTADO_ANALIZANDO), "assertFalse failed at TareaTest.java:149");
            assertFalse(Tarea.esEstadoReintentable(Tarea.ESTADO_PAUSADO), "assertFalse failed at TareaTest.java:150");
        }

        @Test
        @DisplayName("esEstadoCancelable funciona correctamente")
        void testEsEstadoCancelable() {
            assertTrue(Tarea.esEstadoCancelable(Tarea.ESTADO_EN_COLA), "assertTrue failed at TareaTest.java:156");
            assertTrue(Tarea.esEstadoCancelable(Tarea.ESTADO_ANALIZANDO), "assertTrue failed at TareaTest.java:157");
            assertTrue(Tarea.esEstadoCancelable(Tarea.ESTADO_PAUSADO), "assertTrue failed at TareaTest.java:158");
            assertFalse(Tarea.esEstadoCancelable(Tarea.ESTADO_COMPLETADO), "assertFalse failed at TareaTest.java:159");
            assertFalse(Tarea.esEstadoCancelable(Tarea.ESTADO_ERROR), "assertFalse failed at TareaTest.java:160");
            assertFalse(Tarea.esEstadoCancelable(Tarea.ESTADO_CANCELADO), "assertFalse failed at TareaTest.java:161");
        }

        @Test
        @DisplayName("esEstadoEliminable funciona correctamente")
        void testEsEstadoEliminable() {
            assertTrue(Tarea.esEstadoEliminable(Tarea.ESTADO_COMPLETADO), "assertTrue failed at TareaTest.java:167");
            assertTrue(Tarea.esEstadoEliminable(Tarea.ESTADO_ERROR), "assertTrue failed at TareaTest.java:168");
            assertTrue(Tarea.esEstadoEliminable(Tarea.ESTADO_CANCELADO), "assertTrue failed at TareaTest.java:169");
            assertFalse(Tarea.esEstadoEliminable(Tarea.ESTADO_EN_COLA), "assertFalse failed at TareaTest.java:170");
            assertFalse(Tarea.esEstadoEliminable(Tarea.ESTADO_ANALIZANDO), "assertFalse failed at TareaTest.java:171");
            assertFalse(Tarea.esEstadoEliminable(Tarea.ESTADO_PAUSADO), "assertFalse failed at TareaTest.java:172");
        }

        @Test
        @DisplayName("esEstadoFinal funciona correctamente")
        void testEsEstadoFinal() {
            assertTrue(Tarea.esEstadoFinal(Tarea.ESTADO_COMPLETADO), "assertTrue failed at TareaTest.java:178");
            assertTrue(Tarea.esEstadoFinal(Tarea.ESTADO_ERROR), "assertTrue failed at TareaTest.java:179");
            assertTrue(Tarea.esEstadoFinal(Tarea.ESTADO_CANCELADO), "assertTrue failed at TareaTest.java:180");
            assertFalse(Tarea.esEstadoFinal(Tarea.ESTADO_EN_COLA), "assertFalse failed at TareaTest.java:181");
            assertFalse(Tarea.esEstadoFinal(Tarea.ESTADO_ANALIZANDO), "assertFalse failed at TareaTest.java:182");
            assertFalse(Tarea.esEstadoFinal(Tarea.ESTADO_PAUSADO), "assertFalse failed at TareaTest.java:183");
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

            assertTrue(tareaEnCola.esActiva(), "assertTrue failed at TareaTest.java:201");
            assertTrue(tareaAnalizando.esActiva(), "assertTrue failed at TareaTest.java:202");
            assertTrue(tareaPausada.esActiva(), "assertTrue failed at TareaTest.java:203");
            assertFalse(tareaCompletada.esActiva(), "assertFalse failed at TareaTest.java:204");
            assertFalse(tareaError.esActiva(), "assertFalse failed at TareaTest.java:205");
            assertFalse(tareaCancelada.esActiva(), "assertFalse failed at TareaTest.java:206");
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

            assertTrue(tareaCompletada.esFinalizada(), "assertTrue failed at TareaTest.java:219");
            assertTrue(tareaError.esFinalizada(), "assertTrue failed at TareaTest.java:220");
            assertTrue(tareaCancelada.esFinalizada(), "assertTrue failed at TareaTest.java:221");
            assertFalse(tareaEnCola.esFinalizada(), "assertFalse failed at TareaTest.java:222");
            assertFalse(tareaAnalizando.esFinalizada(), "assertFalse failed at TareaTest.java:223");
            assertFalse(tareaPausada.esFinalizada(), "assertFalse failed at TareaTest.java:224");
        }

        @Test
        @DisplayName("Cambiar estado de tarea")
        void testCambiarEstado() {
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
            tarea.establecerEstado(Tarea.ESTADO_ANALIZANDO);

            assertEquals(Tarea.ESTADO_ANALIZANDO, tarea.obtenerEstado(), "assertEquals failed at TareaTest.java:233");
        }

        @Test
        @DisplayName("Establecer estado invalido establece ERROR")
        void testEstablecerEstadoInvalido() {
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
            tarea.establecerEstado("EstadoInvalido");

            assertEquals(Tarea.ESTADO_ERROR, tarea.obtenerEstado(), "assertEquals failed at TareaTest.java:242");
        }

        @Test
        @DisplayName("Establecer estado nulo establece ERROR")
        void testEstablecerEstadoNulo() {
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
            tarea.establecerEstado(null);

            assertEquals(Tarea.ESTADO_ERROR, tarea.obtenerEstado(), "assertEquals failed at TareaTest.java:251");
        }

        @Test
        @DisplayName("Establecer estado vacio establece ERROR")
        void testEstablecerEstadoVacio() {
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
            tarea.establecerEstado("");

            assertEquals(Tarea.ESTADO_ERROR, tarea.obtenerEstado(), "assertEquals failed at TareaTest.java:260");
        }

        @Test
        @DisplayName("Establecer mensaje info")
        void testEstablecerMensajeInfo() {
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
            tarea.establecerMensajeInfo("Procesando solicitud");

            assertEquals("Procesando solicitud", tarea.obtenerMensajeInfo(), "assertEquals failed at TareaTest.java:269");
        }

        @Test
        @DisplayName("Establecer mensaje info nulo")
        void testEstablecerMensajeInfoNulo() {
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
            tarea.establecerMensajeInfo(null);

            assertNull(tarea.obtenerMensajeInfo(), "assertNull failed at TareaTest.java:278");
        }

        @Test
        @DisplayName("Establecer mensaje info vacio")
        void testEstablecerMensajeInfoVacio() {
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
            tarea.establecerMensajeInfo("mensaje inicial");
            tarea.establecerMensajeInfo("");

            assertEquals("", tarea.obtenerMensajeInfo(), "assertEquals failed at TareaTest.java:288");
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

            assertTrue(tarea.obtenerTiempoInicio() >= antes, "assertTrue failed at TareaTest.java:303");
            assertTrue(tarea.obtenerTiempoInicio() <= despues, "assertTrue failed at TareaTest.java:304");
        }

        @Test
        @DisplayName("obtenerTiempoFin es 0 antes de finalizar")
        void testObtenerTiempoFinInicial() {
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
            assertEquals(0, tarea.obtenerTiempoFin(), "assertEquals failed at TareaTest.java:311");
        }

        @Test
        @DisplayName("obtenerTiempoFin se establece al completar")
        void testObtenerTiempoFinAlCompletar() throws InterruptedException {
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
            Thread.sleep(10); // Pequeña pausa para asegurar diferencia de tiempo
            tarea.establecerEstado(Tarea.ESTADO_COMPLETADO);

            assertTrue(tarea.obtenerTiempoFin() > 0, "assertTrue failed at TareaTest.java:321");
            assertTrue(tarea.obtenerTiempoFin() >= tarea.obtenerTiempoInicio(), "assertTrue failed at TareaTest.java:322");
        }

        @Test
        @DisplayName("obtenerTiempoFin se establece en estado ERROR")
        void testObtenerTiempoFinEnError() throws InterruptedException {
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
            Thread.sleep(10);
            tarea.establecerEstado(Tarea.ESTADO_ERROR);

            assertTrue(tarea.obtenerTiempoFin() > 0, "assertTrue failed at TareaTest.java:332");
        }

        @Test
        @DisplayName("obtenerTiempoFin se establece en estado CANCELADO")
        void testObtenerTiempoFinEnCancelado() throws InterruptedException {
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
            Thread.sleep(10);
            tarea.establecerEstado(Tarea.ESTADO_CANCELADO);

            assertTrue(tarea.obtenerTiempoFin() > 0, "assertTrue failed at TareaTest.java:342");
        }

        @Test
        @DisplayName("obtenerDuracionMilisegundos es 0 al inicio")
        void testObtenerDuracionMilisegundosInicial() {
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
            assertEquals(0, tarea.obtenerDuracionMilisegundos(), "assertEquals failed at TareaTest.java:349");
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
            assertEquals(duracionPausada, tarea.obtenerDuracionMilisegundos(), "assertEquals failed at TareaTest.java:372");
            
            tarea.establecerEstado(Tarea.ESTADO_ANALIZANDO);
            Thread.sleep(20);
            
            long duracionFinal = tarea.obtenerDuracionMilisegundos();
            assertTrue(duracionFinal >= duracionPausada + 20, "assertTrue failed at TareaTest.java:378");
        }
    }

    @Nested
    @DisplayName("Formateo de duracion")
    class FormateoDuracion {

        @Test
        @DisplayName("formatearDuracion estatico con segundos")
        void testFormatearDuracionEstaticoSegundos() {
            assertEquals("0s", Tarea.formatearDuracion(0), "assertEquals failed at TareaTest.java:389");
            assertEquals("0s", Tarea.formatearDuracion(999), "assertEquals failed at TareaTest.java:390");
            assertEquals("1s", Tarea.formatearDuracion(1000), "assertEquals failed at TareaTest.java:391");
            assertEquals("5s", Tarea.formatearDuracion(5500), "assertEquals failed at TareaTest.java:392");
        }

        @Test
        @DisplayName("formatearDuracion estatico con minutos")
        void testFormatearDuracionEstaticoMinutos() {
            assertEquals("1m 0s", Tarea.formatearDuracion(60000), "assertEquals failed at TareaTest.java:398");
            assertEquals("1m 30s", Tarea.formatearDuracion(90000), "assertEquals failed at TareaTest.java:399");
            assertEquals("2m 15s", Tarea.formatearDuracion(135000), "assertEquals failed at TareaTest.java:400");
            assertEquals("10m 0s", Tarea.formatearDuracion(600000), "assertEquals failed at TareaTest.java:401");
        }

        @Test
        @DisplayName("formatearDuracion de instancia")
        void testFormatearDuracionInstancia() throws InterruptedException {
            Tarea tarea = new Tarea("1", "Test", "url", Tarea.ESTADO_EN_COLA);
            String duracion = tarea.formatearDuracion();
            assertNotNull(duracion, "assertNotNull failed at TareaTest.java:409");
            assertEquals("0s", duracion, "assertEquals failed at TareaTest.java:410");
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

            assertEquals(4, fila.length, "assertEquals failed at TareaTest.java:424");
            assertEquals("Analisis", fila[0], "assertEquals failed at TareaTest.java:425");
            assertEquals("https://example.com", fila[1], "assertEquals failed at TareaTest.java:426");
            assertEquals(Tarea.ESTADO_EN_COLA, fila[2], "assertEquals failed at TareaTest.java:427");
            assertEquals("0s", fila[3], "assertEquals failed at TareaTest.java:428");
        }

        @Test
        @DisplayName("Fila de tabla refleja cambio de estado")
        void testAFilaTablaConCambioEstado() throws InterruptedException {
            Tarea tarea = new Tarea("1", "Analisis", "https://example.com", Tarea.ESTADO_ANALIZANDO);
            Thread.sleep(50);
            tarea.establecerEstado(Tarea.ESTADO_COMPLETADO);
            
            Object[] fila = tarea.aFilaTabla();
            assertEquals(Tarea.ESTADO_COMPLETADO, fila[2], "assertEquals failed at TareaTest.java:439");
            // La duracion debe ser >= 50ms
            String duracion = (String) fila[3];
            assertTrue(duracion.contains("s") || duracion.contains("m"), "assertTrue failed at TareaTest.java:442");
        }
    }

    @Nested
    @DisplayName("Colores de estado")
    class ColoresEstado {

        @Test
        @DisplayName("Obtener color estado para estados validos")
        void testObtenerColorEstadoValidos() {
            assertNotNull(Tarea.obtenerColorEstado(Tarea.ESTADO_EN_COLA), "assertNotNull failed at TareaTest.java:453");
            assertNotNull(Tarea.obtenerColorEstado(Tarea.ESTADO_ANALIZANDO), "assertNotNull failed at TareaTest.java:454");
            assertNotNull(Tarea.obtenerColorEstado(Tarea.ESTADO_COMPLETADO), "assertNotNull failed at TareaTest.java:455");
            assertNotNull(Tarea.obtenerColorEstado(Tarea.ESTADO_ERROR), "assertNotNull failed at TareaTest.java:456");
            assertNotNull(Tarea.obtenerColorEstado(Tarea.ESTADO_CANCELADO), "assertNotNull failed at TareaTest.java:457");
            assertNotNull(Tarea.obtenerColorEstado(Tarea.ESTADO_PAUSADO), "assertNotNull failed at TareaTest.java:458");
        }

        @Test
        @DisplayName("Obtener color estado con estado nulo devuelve negro")
        void testObtenerColorEstadoNulo() {
            assertEquals(Color.BLACK, Tarea.obtenerColorEstado(null), "assertEquals failed at TareaTest.java:464");
        }

        @Test
        @DisplayName("Obtener color estado con estado vacio devuelve negro")
        void testObtenerColorEstadoVacio() {
            assertEquals(Color.BLACK, Tarea.obtenerColorEstado(""), "assertEquals failed at TareaTest.java:470");
        }

        @Test
        @DisplayName("Obtener color estado con estado invalido devuelve negro")
        void testObtenerColorEstadoInvalido() {
            assertEquals(Color.BLACK, Tarea.obtenerColorEstado("Invalido"), "assertEquals failed at TareaTest.java:476");
            assertEquals(Color.BLACK, Tarea.obtenerColorEstado("Running"), "assertEquals failed at TareaTest.java:477");
            assertEquals(Color.BLACK, Tarea.obtenerColorEstado("Paused"), "assertEquals failed at TareaTest.java:478");
        }
    }
}
