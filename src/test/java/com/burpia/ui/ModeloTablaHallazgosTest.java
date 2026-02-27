package com.burpia.ui;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Hallazgo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;





@DisplayName("ModeloTablaHallazgos Tests")
class ModeloTablaHallazgosTest {

    @AfterEach
    void resetIdioma() {
        I18nUI.establecerIdioma("es");
    }

    @Test
    @DisplayName("Permite actualizar limite de filas en runtime")
    void testActualizarLimiteFilas() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);

        for (int i = 0; i < 6; i++) {
            modelo.agregarHallazgo(new Hallazgo(
                "https://example.com/" + i,
                "Titulo " + i,
                "Hallazgo " + i,
                "Low",
                "Low"
            ));
        }
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(5, modelo.getRowCount());

        modelo.establecerLimiteFilas(3);
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(3, modelo.getRowCount());
        assertEquals(3, modelo.obtenerLimiteFilas());
    }

    @Test
    @DisplayName("Ignorar y eliminar con indice invalido no rompe la tabla")
    void testOperacionesIndiceInvalido() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);
        modelo.agregarHallazgo(new Hallazgo("https://example.com/x", "T", "Hallazgo X", "Low", "Low"));
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(1, modelo.getRowCount());

        assertDoesNotThrow(() -> modelo.marcarComoIgnorado(-1));
        assertDoesNotThrow(() -> modelo.eliminarHallazgo(-1));
        SwingUtilities.invokeAndWait(() -> {});

        assertEquals(1, modelo.getRowCount());
        assertEquals(0, modelo.obtenerNumeroIgnorados());
    }

    @Test
    @DisplayName("Actualizar con hallazgo null no altera datos")
    void testActualizarHallazgoNullNoRompe() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);
        modelo.agregarHallazgo(new Hallazgo("https://example.com/x", "T", "Hallazgo X", "Low", "Low"));
        SwingUtilities.invokeAndWait(() -> {});

        assertDoesNotThrow(() -> modelo.actualizarHallazgo(0, null));
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals(1, modelo.getRowCount());
    }

    @Test
    @DisplayName("Refrescar idioma regenera filas traducidas")
    void testRefrescarColumnasIdiomaRegeneraFilas() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);
        I18nUI.establecerIdioma("es");
        modelo.agregarHallazgo(new Hallazgo("https://example.com/x", "T", "Hallazgo X", "High", "Low"));
        SwingUtilities.invokeAndWait(() -> {});
        assertEquals("Alta", modelo.getValueAt(0, 3));
        assertEquals("Baja", modelo.getValueAt(0, 4));

        I18nUI.establecerIdioma("en");
        modelo.refrescarColumnasIdioma();
        SwingUtilities.invokeAndWait(() -> {});

        assertEquals("High", modelo.getValueAt(0, 3));
        assertEquals("Low", modelo.getValueAt(0, 4));
    }

    @Test
    @DisplayName("Actualiza por referencia para evitar indice desfasado")
    void testActualizarHallazgoPorReferencia() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(5);
        Hallazgo primero = new Hallazgo("https://example.com/a", "TA", "Hallazgo A", "Low", "Low");
        Hallazgo segundo = new Hallazgo("https://example.com/b", "TB", "Hallazgo B", "Low", "Low");
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

        assertTrue(modelo.actualizarHallazgo(segundo, editado));
        SwingUtilities.invokeAndWait(() -> {});

        Hallazgo filaActualizada = modelo.obtenerHallazgo(1);
        assertEquals("https://example.com/b-editado", filaActualizada.obtenerUrl());
        assertEquals("TB Editado", filaActualizada.obtenerTitulo());
        assertEquals("Hallazgo B Editado", filaActualizada.obtenerHallazgo());

        Hallazgo noExistente = new Hallazgo("https://example.com/c", "TC", "Hallazgo C", "Low", "Low");
        assertFalse(modelo.actualizarHallazgo(noExistente, editado));
    }
}
