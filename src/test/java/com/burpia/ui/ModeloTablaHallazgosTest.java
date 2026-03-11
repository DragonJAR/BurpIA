package com.burpia.ui;

import com.burpia.i18n.I18nUI;
import com.burpia.model.Hallazgo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ModeloTablaHallazgos Tests")
class ModeloTablaHallazgosTest {

    @AfterEach
    void resetIdioma() {
        I18nUI.establecerIdioma("es");
    }

    @Nested
    @DisplayName("Operaciones de agregar hallazgos")
    class AgregarHallazgosTests {

        @Test
        @DisplayName("Agregar hallazgo individual correctamente")
        void testAgregarHallazgoIndividual() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);
            Hallazgo hallazgo = crearHallazgo("https://example.com/test", "Test");

            modelo.agregarHallazgo(hallazgo);
            SwingUtilities.invokeAndWait(() -> {});

            assertEquals(1, modelo.getRowCount(), "assertEquals failed at ModeloTablaHallazgosTest.java:46");
            assertEquals(1, modelo.obtenerNumeroHallazgos(), "assertEquals failed at ModeloTablaHallazgosTest.java:47");
            assertNotNull(modelo.obtenerHallazgo(0), "assertNotNull failed at ModeloTablaHallazgosTest.java:48");
        }

        @Test
        @DisplayName("Agregar hallazgo null no altera la tabla")
        void testAgregarHallazgoNullNoAltera() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);

            assertDoesNotThrow(() -> modelo.agregarHallazgo(null));
            SwingUtilities.invokeAndWait(() -> {});

            assertEquals(0, modelo.getRowCount(), "assertEquals failed at ModeloTablaHallazgosTest.java:59");
            assertEquals(0, modelo.obtenerNumeroHallazgos(), "assertEquals failed at ModeloTablaHallazgosTest.java:60");
        }

        @Test
        @DisplayName("Agregar lista de hallazgos en lote")
        void testAgregarHallazgosLote() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(10);
            List<Hallazgo> hallazgos = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                hallazgos.add(crearHallazgo("https://example.com/" + i, "Test " + i));
            }

            modelo.agregarHallazgos(hallazgos);
            SwingUtilities.invokeAndWait(() -> {});

            assertEquals(5, modelo.getRowCount(), "assertEquals failed at ModeloTablaHallazgosTest.java:75");
            assertEquals(5, modelo.obtenerNumeroHallazgos(), "assertEquals failed at ModeloTablaHallazgosTest.java:76");
        }

        @Test
        @DisplayName("Agregar lista vacía no altera la tabla")
        void testAgregarHallazgosListaVacia() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);

            assertDoesNotThrow(() -> modelo.agregarHallazgos(Collections.emptyList()));
            SwingUtilities.invokeAndWait(() -> {});

            assertEquals(0, modelo.getRowCount(), "assertEquals failed at ModeloTablaHallazgosTest.java:87");
        }

        @Test
        @DisplayName("Agregar lista null no altera la tabla")
        void testAgregarHallazgosListaNull() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);

            assertDoesNotThrow(() -> modelo.agregarHallazgos(null));
            SwingUtilities.invokeAndWait(() -> {});

            assertEquals(0, modelo.getRowCount(), "assertEquals failed at ModeloTablaHallazgosTest.java:98");
        }

        @Test
        @DisplayName("Agregar lote respeta límite de filas")
        void testAgregarHallazgosRespetaLimite() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(3);
            List<Hallazgo> hallazgos = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                hallazgos.add(crearHallazgo("https://example.com/" + i, "Test " + i));
            }

            modelo.agregarHallazgos(hallazgos);
            SwingUtilities.invokeAndWait(() -> {});

            assertEquals(3, modelo.getRowCount(), "assertEquals failed at ModeloTablaHallazgosTest.java:113");
        }

        @Test
        @DisplayName("Agregar lote conserva los mismos hallazgos recientes que el agregado individual")
        void testAgregarHallazgosLoteMantieneMismosRecientesQueAgregarIndividual() throws Exception {
            ModeloTablaHallazgos modeloLote = new ModeloTablaHallazgos(3);
            ModeloTablaHallazgos modeloIndividual = new ModeloTablaHallazgos(3);
            List<Hallazgo> hallazgos = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                hallazgos.add(crearHallazgo("https://example.com/" + i, "Test " + i));
            }

            modeloLote.agregarHallazgos(hallazgos);
            for (Hallazgo hallazgo : hallazgos) {
                modeloIndividual.agregarHallazgo(hallazgo);
            }
            SwingUtilities.invokeAndWait(() -> {});

            assertEquals(modeloIndividual.getRowCount(), modeloLote.getRowCount(),
                "Ambas rutas deben respetar el mismo límite");
            for (int i = 0; i < modeloIndividual.getRowCount(); i++) {
                assertEquals(
                    modeloIndividual.obtenerHallazgo(i).obtenerTitulo(),
                    modeloLote.obtenerHallazgo(i).obtenerTitulo(),
                    "El agregado por lote debe conservar el mismo subconjunto reciente que el agregado individual"
                );
            }
        }
    }

    @Nested
    @DisplayName("Gestión de límite de filas")
    class LimiteFilasTests {

        @Test
        @DisplayName("Permite actualizar límite de filas en runtime")
        void testActualizarLimiteFilas() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);

            for (int i = 0; i < 6; i++) {
                modelo.agregarHallazgo(crearHallazgo("https://example.com/" + i, "Test " + i));
            }
            SwingUtilities.invokeAndWait(() -> {});
            assertEquals(5, modelo.getRowCount(), "assertEquals failed at ModeloTablaHallazgosTest.java:130");

            modelo.establecerLimiteFilas(3);
            SwingUtilities.invokeAndWait(() -> {});
            assertEquals(3, modelo.getRowCount(), "assertEquals failed at ModeloTablaHallazgosTest.java:134");
            assertEquals(3, modelo.obtenerLimiteFilas(), "assertEquals failed at ModeloTablaHallazgosTest.java:135");
        }

        @Test
        @DisplayName("Reducir límite notifica a la tabla cuando se purgan filas")
        void testReducirLimiteNotificaTabla() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);
            for (int i = 0; i < 5; i++) {
                modelo.agregarHallazgo(crearHallazgo("https://example.com/" + i, "Test " + i));
            }
            SwingUtilities.invokeAndWait(() -> {});

            AtomicInteger eventos = new AtomicInteger(0);
            modelo.addTableModelListener(evento -> {
                if (evento != null && evento.getType() == TableModelEvent.UPDATE) {
                    eventos.incrementAndGet();
                }
            });

            modelo.establecerLimiteFilas(3);
            SwingUtilities.invokeAndWait(() -> {});

            assertEquals(3, modelo.getRowCount(), "assertEquals failed at ModeloTablaHallazgosTest.java:157");
            assertTrue(eventos.get() > 0, "assertTrue failed at ModeloTablaHallazgosTest.java:158");
        }

        @Test
        @DisplayName("Límite mínimo es 1")
        void testLimiteMinimo() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(0);

            assertEquals(1, modelo.obtenerLimiteFilas(), "assertEquals failed at ModeloTablaHallazgosTest.java:166");

            modelo.establecerLimiteFilas(-5);
            SwingUtilities.invokeAndWait(() -> {});

            assertEquals(1, modelo.obtenerLimiteFilas(), "assertEquals failed at ModeloTablaHallazgosTest.java:171");
        }
    }

    @Nested
    @DisplayName("Operaciones de actualización")
    class ActualizacionTests {

        @Test
        @DisplayName("Actualizar con hallazgo null no altera datos")
        void testActualizarHallazgoNullNoRompe() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);
            modelo.agregarHallazgo(crearHallazgo("https://example.com/x", "Original"));
            SwingUtilities.invokeAndWait(() -> {});

            assertDoesNotThrow(() -> modelo.actualizarHallazgo(0, null));
            SwingUtilities.invokeAndWait(() -> {});
            assertEquals(1, modelo.getRowCount(), "assertEquals failed at ModeloTablaHallazgosTest.java:188");
        }

        @Test
        @DisplayName("Actualiza por referencia para evitar índice desfasado")
        void testActualizarHallazgoPorReferencia() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);
            Hallazgo primero = crearHallazgo("https://example.com/a", "TA");
            Hallazgo segundo = crearHallazgo("https://example.com/b", "TB");
            modelo.agregarHallazgo(primero);
            modelo.agregarHallazgo(segundo);
            SwingUtilities.invokeAndWait(() -> {});

            Hallazgo editado = segundo.editar(
                "https://example.com/b-editado",
                "TB Editado",
                "Hallazgo B Editado",
                "High",
                "High"
            );

            assertTrue(modelo.actualizarHallazgo(segundo, editado), "assertTrue failed at ModeloTablaHallazgosTest.java:209");
            SwingUtilities.invokeAndWait(() -> {});

            Hallazgo filaActualizada = modelo.obtenerHallazgo(1);
            assertEquals("https://example.com/b-editado", filaActualizada.obtenerUrl(), "assertEquals failed at ModeloTablaHallazgosTest.java:213");
            assertEquals("TB Editado", filaActualizada.obtenerTitulo(), "assertEquals failed at ModeloTablaHallazgosTest.java:214");
            assertEquals("Hallazgo B Editado", filaActualizada.obtenerHallazgo(), "assertEquals failed at ModeloTablaHallazgosTest.java:215");

            Hallazgo noExistente = crearHallazgo("https://example.com/c", "TC");
            assertFalse(modelo.actualizarHallazgo(noExistente, editado), "assertFalse failed at ModeloTablaHallazgosTest.java:218");
        }

        @Test
        @DisplayName("Actualizar por referencia con null original retorna false")
        void testActualizarHallazgoPorReferenciaNullOriginal() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);
            modelo.agregarHallazgo(crearHallazgo("https://example.com/a", "Test"));
            SwingUtilities.invokeAndWait(() -> {});

            Hallazgo editado = crearHallazgo("https://example.com/b", "Editado");
            assertFalse(modelo.actualizarHallazgo(null, editado), "assertFalse failed at ModeloTablaHallazgosTest.java:229");
        }
    }

    @Nested
    @DisplayName("Operaciones de eliminación e ignorar")
    class EliminacionIgnorarTests {

        @Test
        @DisplayName("Ignorar y eliminar con índice inválido no rompe la tabla")
        void testOperacionesIndiceInvalido() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);
            modelo.agregarHallazgo(crearHallazgo("https://example.com/x", "Test"));
            SwingUtilities.invokeAndWait(() -> {});
            assertEquals(1, modelo.getRowCount(), "assertEquals failed at ModeloTablaHallazgosTest.java:243");

            assertDoesNotThrow(() -> modelo.marcarComoIgnorado(-1));
            assertDoesNotThrow(() -> modelo.eliminarHallazgo(-1));
            SwingUtilities.invokeAndWait(() -> {});

            assertEquals(1, modelo.getRowCount(), "assertEquals failed at ModeloTablaHallazgosTest.java:249");
            assertEquals(0, modelo.obtenerNumeroIgnorados(), "assertEquals failed at ModeloTablaHallazgosTest.java:250");
        }

        @Test
        @DisplayName("Marcar hallazgo como ignorado correctamente")
        void testMarcarComoIgnorado() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);
            modelo.agregarHallazgo(crearHallazgo("https://example.com/x", "Test"));
            SwingUtilities.invokeAndWait(() -> {});

            modelo.marcarComoIgnorado(0);
            SwingUtilities.invokeAndWait(() -> {});

            assertTrue(modelo.estaIgnorado(0), "assertTrue failed at ModeloTablaHallazgosTest.java:263");
            assertEquals(1, modelo.obtenerNumeroIgnorados(), "assertEquals failed at ModeloTablaHallazgosTest.java:264");
        }

        @Test
        @DisplayName("Eliminar hallazgo reduce el conteo")
        void testEliminarHallazgo() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);
            modelo.agregarHallazgo(crearHallazgo("https://example.com/1", "Test 1"));
            modelo.agregarHallazgo(crearHallazgo("https://example.com/2", "Test 2"));
            SwingUtilities.invokeAndWait(() -> {});
            assertEquals(2, modelo.getRowCount(), "assertEquals failed at ModeloTablaHallazgosTest.java:274");

            modelo.eliminarHallazgo(0);
            SwingUtilities.invokeAndWait(() -> {});

            assertEquals(1, modelo.getRowCount(), "assertEquals failed at ModeloTablaHallazgosTest.java:279");
            assertEquals(1, modelo.obtenerNumeroHallazgos(), "assertEquals failed at ModeloTablaHallazgosTest.java:280");
        }

        @Test
        @DisplayName("Eliminar ajusta índices de filas ignoradas")
        void testEliminarAjustaIndicesIgnorados() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);
            modelo.agregarHallazgo(crearHallazgo("https://example.com/1", "Test 1"));
            modelo.agregarHallazgo(crearHallazgo("https://example.com/2", "Test 2"));
            modelo.agregarHallazgo(crearHallazgo("https://example.com/3", "Test 3"));
            SwingUtilities.invokeAndWait(() -> {});

            modelo.marcarComoIgnorado(2);
            SwingUtilities.invokeAndWait(() -> {});

            modelo.eliminarHallazgo(0);
            SwingUtilities.invokeAndWait(() -> {});

            assertEquals(1, modelo.obtenerNumeroIgnorados(), "assertEquals failed at ModeloTablaHallazgosTest.java:298");
            assertTrue(modelo.estaIgnorado(1), "assertTrue failed at ModeloTablaHallazgosTest.java:299");
        }
    }

    @Nested
    @DisplayName("Operaciones de obtención y estadísticas")
    class ObtencionEstadisticasTests {

        @Test
        @DisplayName("Obtener hallazgo con índice válido")
        void testObtenerHallazgoValido() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);
            Hallazgo hallazgo = crearHallazgo("https://example.com/test", "Test Title");
            modelo.agregarHallazgo(hallazgo);
            SwingUtilities.invokeAndWait(() -> {});

            Hallazgo obtenido = modelo.obtenerHallazgo(0);
            assertNotNull(obtenido, "assertNotNull failed at ModeloTablaHallazgosTest.java:316");
            assertEquals("https://example.com/test", obtenido.obtenerUrl(), "assertEquals failed at ModeloTablaHallazgosTest.java:317");
            assertEquals("Test Title", obtenido.obtenerTitulo(), "assertEquals failed at ModeloTablaHallazgosTest.java:318");
        }

        @Test
        @DisplayName("Obtener hallazgo con índice inválido retorna null")
        void testObtenerHallazgoInvalido() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);

            assertNull(modelo.obtenerHallazgo(-1), "assertNull failed at ModeloTablaHallazgosTest.java:326");
            assertNull(modelo.obtenerHallazgo(0), "assertNull failed at ModeloTablaHallazgosTest.java:327");
            assertNull(modelo.obtenerHallazgo(100), "assertNull failed at ModeloTablaHallazgosTest.java:328");
        }

        @Test
        @DisplayName("Obtener filas visibles excluye ignorados")
        void testObtenerFilasVisibles() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);
            modelo.agregarHallazgo(crearHallazgo("https://example.com/1", "Test 1"));
            modelo.agregarHallazgo(crearHallazgo("https://example.com/2", "Test 2"));
            modelo.agregarHallazgo(crearHallazgo("https://example.com/3", "Test 3"));
            SwingUtilities.invokeAndWait(() -> {});

            assertEquals(3, modelo.obtenerFilasVisibles(), "assertEquals failed at ModeloTablaHallazgosTest.java:340");

            modelo.marcarComoIgnorado(1);
            SwingUtilities.invokeAndWait(() -> {});

            assertEquals(2, modelo.obtenerFilasVisibles(), "assertEquals failed at ModeloTablaHallazgosTest.java:345");
        }

        @Test
        @DisplayName("Obtener estadísticas visibles agrupa por severidad")
        void testObtenerEstadisticasVisibles() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(10);
            modelo.agregarHallazgo(crearHallazgoConSeveridad("https://example.com/1", "Critical", Hallazgo.SEVERIDAD_CRITICAL));
            modelo.agregarHallazgo(crearHallazgoConSeveridad("https://example.com/2", "High", Hallazgo.SEVERIDAD_HIGH));
            modelo.agregarHallazgo(crearHallazgoConSeveridad("https://example.com/3", "High", Hallazgo.SEVERIDAD_HIGH));
            modelo.agregarHallazgo(crearHallazgoConSeveridad("https://example.com/4", "Medium", Hallazgo.SEVERIDAD_MEDIUM));
            modelo.agregarHallazgo(crearHallazgoConSeveridad("https://example.com/5", "Low", Hallazgo.SEVERIDAD_LOW));
            modelo.agregarHallazgo(crearHallazgoConSeveridad("https://example.com/6", "Info", Hallazgo.SEVERIDAD_INFO));
            SwingUtilities.invokeAndWait(() -> {});

            int[] stats = modelo.obtenerEstadisticasVisibles();

            assertEquals(6, stats[0], "assertEquals failed at ModeloTablaHallazgosTest.java:362"); // total
            assertEquals(1, stats[1], "assertEquals failed at ModeloTablaHallazgosTest.java:363"); // critical
            assertEquals(2, stats[2], "assertEquals failed at ModeloTablaHallazgosTest.java:364"); // high
            assertEquals(1, stats[3], "assertEquals failed at ModeloTablaHallazgosTest.java:365"); // medium
            assertEquals(1, stats[4], "assertEquals failed at ModeloTablaHallazgosTest.java:366"); // low
            assertEquals(1, stats[5], "assertEquals failed at ModeloTablaHallazgosTest.java:367"); // info
        }

        @Test
        @DisplayName("Obtener hallazgos no ignorados")
        void testObtenerHallazgosNoIgnorados() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);
            modelo.agregarHallazgo(crearHallazgo("https://example.com/1", "Test 1"));
            modelo.agregarHallazgo(crearHallazgo("https://example.com/2", "Test 2"));
            modelo.agregarHallazgo(crearHallazgo("https://example.com/3", "Test 3"));
            SwingUtilities.invokeAndWait(() -> {});

            modelo.marcarComoIgnorado(1);
            SwingUtilities.invokeAndWait(() -> {});

            List<Hallazgo> noIgnorados = modelo.obtenerHallazgosNoIgnorados();
            assertEquals(2, noIgnorados.size(), "assertEquals failed at ModeloTablaHallazgosTest.java:383");
        }
    }

    @Nested
    @DisplayName("Operaciones de limpieza")
    class LimpiezaTests {

        @Test
        @DisplayName("Limpiar elimina todos los hallazgos")
        void testLimpiar() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);
            modelo.agregarHallazgo(crearHallazgo("https://example.com/1", "Test 1"));
            modelo.agregarHallazgo(crearHallazgo("https://example.com/2", "Test 2"));
            SwingUtilities.invokeAndWait(() -> {});
            assertEquals(2, modelo.getRowCount(), "assertEquals failed at ModeloTablaHallazgosTest.java:398");

            modelo.limpiar();
            SwingUtilities.invokeAndWait(() -> {});

            assertEquals(0, modelo.getRowCount(), "assertEquals failed at ModeloTablaHallazgosTest.java:403");
            assertEquals(0, modelo.obtenerNumeroHallazgos(), "assertEquals failed at ModeloTablaHallazgosTest.java:404");
            assertEquals(0, modelo.obtenerNumeroIgnorados(), "assertEquals failed at ModeloTablaHallazgosTest.java:405");
        }
    }

    @Nested
    @DisplayName("Internacionalización")
    class InternacionalizacionTests {

        @Test
        @DisplayName("Refrescar idioma regenera filas traducidas")
        void testRefrescarColumnasIdiomaRegeneraFilas() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);
            I18nUI.establecerIdioma("es");
            modelo.agregarHallazgo(new Hallazgo("https://example.com/x", "T", "Hallazgo X", "High", "Low"));
            SwingUtilities.invokeAndWait(() -> {});
            assertEquals("Alta", modelo.getValueAt(0, 3), "assertEquals failed at ModeloTablaHallazgosTest.java:420");
            assertEquals("Baja", modelo.getValueAt(0, 4), "assertEquals failed at ModeloTablaHallazgosTest.java:421");

            I18nUI.establecerIdioma("en");
            modelo.refrescarColumnasIdioma();
            SwingUtilities.invokeAndWait(() -> {});

            assertEquals("High", modelo.getValueAt(0, 3), "assertEquals failed at ModeloTablaHallazgosTest.java:427");
            assertEquals("Low", modelo.getValueAt(0, 4), "assertEquals failed at ModeloTablaHallazgosTest.java:428");
        }
    }

    @Nested
    @DisplayName("Escuchas de cambios")
    class EscuchasCambiosTests {

        @Test
        @DisplayName("Agregar escucha y recibir notificación")
        void testAgregarEscuchaYNotificar() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);
            AtomicBoolean notificado = new AtomicBoolean(false);

            modelo.agregarEscucha(() -> notificado.set(true));
            modelo.agregarHallazgo(crearHallazgo("https://example.com/test", "Test"));
            SwingUtilities.invokeAndWait(() -> {});

            assertTrue(notificado.get(), "assertTrue failed at ModeloTablaHallazgosTest.java:446");
        }

        @Test
        @DisplayName("Eliminar escucha deja de recibir notificaciones")
        void testEliminarEscucha() throws Exception {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);
            AtomicInteger contador = new AtomicInteger(0);

            EscuchaCambiosHallazgos escucha = () -> contador.incrementAndGet();
            modelo.agregarEscucha(escucha);

            modelo.agregarHallazgo(crearHallazgo("https://example.com/1", "Test 1"));
            SwingUtilities.invokeAndWait(() -> {});
            assertEquals(1, contador.get(), "assertEquals failed at ModeloTablaHallazgosTest.java:460");

            modelo.eliminarEscucha(escucha);

            modelo.agregarHallazgo(crearHallazgo("https://example.com/2", "Test 2"));
            SwingUtilities.invokeAndWait(() -> {});
            assertEquals(1, contador.get(), "assertEquals failed at ModeloTablaHallazgosTest.java:466");
        }

        @Test
        @DisplayName("Agregar escucha null no causa error")
        void testAgregarEscuchaNull() {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);
            assertDoesNotThrow(() -> modelo.agregarEscucha(null));
        }
    }

    @Nested
    @DisplayName("Propiedades de tabla")
    class PropiedadesTablaTests {

        @Test
        @DisplayName("Celdas no son editables")
        void testCeldasNoEditables() {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);

            assertFalse(modelo.isCellEditable(0, 0), "assertFalse failed at ModeloTablaHallazgosTest.java:486");
            assertFalse(modelo.isCellEditable(100, 4), "assertFalse failed at ModeloTablaHallazgosTest.java:487");
            assertFalse(modelo.isCellEditable(-1, 0), "assertFalse failed at ModeloTablaHallazgosTest.java:488");
        }

        @Test
        @DisplayName("Número de columnas es 5")
        void testNumeroColumnas() {
            ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);
            assertEquals(5, modelo.getColumnCount(), "assertEquals failed at ModeloTablaHallazgosTest.java:495");
        }
    }

    private Hallazgo crearHallazgo(String url, String titulo) {
        return new Hallazgo(url, titulo, "Descripción de " + titulo, "Low", "Low");
    }

    private Hallazgo crearHallazgoConSeveridad(String url, String titulo, String severidad) {
        return new Hallazgo(url, titulo, "Descripción de " + titulo, severidad, "Low");
    }
}
