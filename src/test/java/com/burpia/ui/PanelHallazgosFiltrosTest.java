package com.burpia.ui;
import burp.api.montoya.MontoyaApi;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Hallazgo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;





@DisplayName("PanelHallazgos Filtros Tests")
class PanelHallazgosFiltrosTest {

    @AfterEach
    void resetIdioma() {
        I18nUI.establecerIdioma("es");
    }

    @Test
    @DisplayName("Volver a 'Todas las Criticidades' no deja filtro residual")
    void testFiltroTodasLasCriticidades() throws Exception {
        I18nUI.establecerIdioma("es");
        
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(100);
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);

        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, modelo, true));
        PanelHallazgos panel = holder[0];

        modelo.agregarHallazgos(List.of(
            new Hallazgo("https://example.com/a", "TA", "Hallazgo A", "High", "High"),
            new Hallazgo("https://example.com/b", "TB", "Hallazgo B", "Low", "Medium")
        ));
        flushEdt();

        JTable tabla = obtenerCampo(panel, "tabla", JTable.class);
        JComboBox<?> comboSeveridad = obtenerCampo(panel, "comboSeveridad", JComboBox.class);

        assertEquals(2, tabla.getRowCount(), "assertEquals failed at PanelHallazgosFiltrosTest.java:56");

        SwingUtilities.invokeAndWait(() -> comboSeveridad.setSelectedItem(I18nUI.Hallazgos.SEVERIDAD_HIGH()));
        flushEdt();
        assertEquals(1, tabla.getRowCount(), "assertEquals failed at PanelHallazgosFiltrosTest.java:60");

        SwingUtilities.invokeAndWait(() -> comboSeveridad.setSelectedItem(I18nUI.Hallazgos.OPCION_TODAS_CRITICIDADES()));
        flushEdt();
        assertEquals(2, tabla.getRowCount(), "assertEquals failed at PanelHallazgosFiltrosTest.java:64");
    }

    @Test
    @DisplayName("Búsqueda por texto consulta también la descripción del hallazgo")
    void testBusquedaIncluyeDescripcionHallazgo() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(100);
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);

        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, modelo, true));
        PanelHallazgos panel = holder[0];

        modelo.agregarHallazgos(List.of(
            new Hallazgo("https://example.com/login", "Pantalla Login", "Fuga de stack trace", "Low", "Medium"),
            new Hallazgo("https://example.com/search", "Buscador", "SQL injection en parámetro q", "High", "High")
        ));
        flushEdt();

        JTextField campoBusqueda = obtenerCampo(panel, "campoBusqueda", JTextField.class);
        JTable tabla = obtenerCampo(panel, "tabla", JTable.class);

        SwingUtilities.invokeAndWait(() -> campoBusqueda.setText("sql injection"));
        flushEdt();

        assertEquals(1, tabla.getRowCount(), "La búsqueda debe encontrar coincidencias en la descripción");
        assertEquals("Buscador", tabla.getValueAt(0, 2), "Debe conservar el hallazgo cuya descripción coincide");
    }

    @Test
    @DisplayName("Estado de guardado automático en Issues se sincroniza con el checkbox")
    void testEstadoGuardadoAutomaticoIssues() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(100);
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);

        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, modelo, true));
        PanelHallazgos panel = holder[0];

        JCheckBox checkbox = obtenerCampo(panel, "chkGuardarEnIssues", JCheckBox.class);

        assertFalse(panel.isGuardadoAutomaticoIssuesActivo(), "assertFalse failed at PanelHallazgosFiltrosTest.java:79");

        SwingUtilities.invokeAndWait(checkbox::doClick);
        flushEdt();
        assertTrue(panel.isGuardadoAutomaticoIssuesActivo(), "assertTrue failed at PanelHallazgosFiltrosTest.java:83");

        SwingUtilities.invokeAndWait(checkbox::doClick);
        flushEdt();
        assertFalse(panel.isGuardadoAutomaticoIssuesActivo(), "assertFalse failed at PanelHallazgosFiltrosTest.java:87");
    }

    @Test
    @DisplayName("Modelo marca y consulta hallazgos ignorados correctamente")
    void testModeloIgnorados() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(100);

        modelo.agregarHallazgos(List.of(
            new Hallazgo("https://example.com/a", "TA", "Hallazgo A", "High", "High"),
            new Hallazgo("https://example.com/b", "TB", "Hallazgo B", "Low", "Medium")
        ));
        flushEdt();

        assertEquals(2, modelo.obtenerNumeroHallazgos(), "assertEquals failed at PanelHallazgosFiltrosTest.java:101");
        assertFalse(modelo.estaIgnorado(0), "assertFalse failed at PanelHallazgosFiltrosTest.java:102");
        assertFalse(modelo.estaIgnorado(1), "assertFalse failed at PanelHallazgosFiltrosTest.java:103");
        assertEquals(0, modelo.obtenerNumeroIgnorados(), "assertEquals failed at PanelHallazgosFiltrosTest.java:104");

        modelo.marcarComoIgnorado(0);
        flushEdt();

        assertTrue(modelo.estaIgnorado(0), "assertTrue failed at PanelHallazgosFiltrosTest.java:109");
        assertFalse(modelo.estaIgnorado(1), "assertFalse failed at PanelHallazgosFiltrosTest.java:110");
        assertEquals(1, modelo.obtenerNumeroIgnorados(), "assertEquals failed at PanelHallazgosFiltrosTest.java:111");

        List<Hallazgo> noIgnorados = modelo.obtenerHallazgosNoIgnorados();
        assertEquals(1, noIgnorados.size(), "assertEquals failed at PanelHallazgosFiltrosTest.java:114");
        assertEquals("TB", noIgnorados.get(0).obtenerTitulo(), "assertEquals failed at PanelHallazgosFiltrosTest.java:115");

        modelo.marcarComoIgnorado(1);
        flushEdt();

        assertEquals(2, modelo.obtenerNumeroIgnorados(), "assertEquals failed at PanelHallazgosFiltrosTest.java:120");
        assertTrue(modelo.obtenerHallazgosNoIgnorados().isEmpty(), "assertTrue failed at PanelHallazgosFiltrosTest.java:121");
    }

    @Test
    @DisplayName("Eliminar hallazgo actualiza índices de ignorados")
    void testEliminarActualizaIgnorados() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(100);

        modelo.agregarHallazgos(List.of(
            new Hallazgo("https://example.com/a", "TA", "Hallazgo A", "High", "High"),
            new Hallazgo("https://example.com/b", "TB", "Hallazgo B", "Low", "Medium"),
            new Hallazgo("https://example.com/c", "TC", "Hallazgo C", "Medium", "Low")
        ));
        flushEdt();

        modelo.marcarComoIgnorado(1);
        assertTrue(modelo.estaIgnorado(1), "assertTrue failed at PanelHallazgosFiltrosTest.java:137");

        modelo.eliminarHallazgo(0);
        flushEdt();

        assertEquals(2, modelo.obtenerNumeroHallazgos(), "assertEquals failed at PanelHallazgosFiltrosTest.java:142");
        assertTrue(modelo.estaIgnorado(0), "assertTrue failed at PanelHallazgosFiltrosTest.java:143");
        assertFalse(modelo.estaIgnorado(1), "assertFalse failed at PanelHallazgosFiltrosTest.java:144");

        assertEquals(1, modelo.obtenerNumeroIgnorados(), "assertEquals failed at PanelHallazgosFiltrosTest.java:146");
    }

    @Test
    @DisplayName("aplicarIdioma actualiza todas las opciones del combo de severidad")
    void testAplicarIdiomaActualizaOpcionesSeveridad() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(100);
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);

        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, modelo, true));
        PanelHallazgos panel = holder[0];

        JComboBox<?> comboSeveridad = obtenerCampo(panel, "comboSeveridad", JComboBox.class);
        SwingUtilities.invokeAndWait(() -> {
            comboSeveridad.setSelectedIndex(2);
            I18nUI.establecerIdioma("en");
            panel.aplicarIdioma();
        });
        flushEdt();

        assertEquals("All Severities", comboSeveridad.getItemAt(0), "assertEquals failed at PanelHallazgosFiltrosTest.java:167");
        assertEquals("Critical", comboSeveridad.getItemAt(1), "assertEquals failed at PanelHallazgosFiltrosTest.java:168");
        assertEquals("High", comboSeveridad.getItemAt(2), "assertEquals failed at PanelHallazgosFiltrosTest.java:169");
        assertEquals("Medium", comboSeveridad.getItemAt(3), "assertEquals failed at PanelHallazgosFiltrosTest.java:170");
        assertEquals("Low", comboSeveridad.getItemAt(4), "assertEquals failed at PanelHallazgosFiltrosTest.java:171");
        assertEquals("Info", comboSeveridad.getItemAt(5), "assertEquals failed at PanelHallazgosFiltrosTest.java:172");
        assertEquals(2, comboSeveridad.getSelectedIndex(), "assertEquals failed at PanelHallazgosFiltrosTest.java:173");
    }

    @Test
    @DisplayName("Tabla y acción de limpieza exponen tooltips coherentes")
    void testTooltipsTablaYLimpiarTodo() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(100);
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);

        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, modelo, true));
        PanelHallazgos panel = holder[0];

        JTable tabla = obtenerCampo(panel, "tabla", JTable.class);
        JTableHeader encabezado = tabla.getTableHeader();
        JButton botonLimpiarTodo = obtenerCampo(panel, "botonLimpiarTodo", JButton.class);

        assertTooltipEncabezado(encabezado, 0, I18nUI.Tooltips.Hallazgos.COLUMNA_HORA());
        assertTooltipEncabezado(encabezado, 1, I18nUI.Tooltips.Hallazgos.COLUMNA_URL());
        assertTooltipEncabezado(encabezado, 2, I18nUI.Tooltips.Hallazgos.COLUMNA_HALLAZGO());
        assertTooltipEncabezado(encabezado, 3, I18nUI.Tooltips.Hallazgos.COLUMNA_SEVERIDAD());
        assertTooltipEncabezado(encabezado, 4, I18nUI.Tooltips.Hallazgos.COLUMNA_CONFIANZA());
        assertEquals(I18nUI.Tooltips.Hallazgos.LIMPIAR_TODO(), botonLimpiarTodo.getToolTipText(),
                "El tooltip de limpiar debe describir el comportamiento real");
    }

    @Test
    @DisplayName("Captura de acciones tolera URL invalida sin request")
    void testCapturaEntradasAccionToleraUrlInvalida() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(100);
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, modelo, true));
        PanelHallazgos panel = holder[0];

        modelo.agregarHallazgo(new Hallazgo("://url-invalida", "T", "H", "Low", "Low"));
        flushEdt();

        Method metodo = PanelHallazgos.class.getDeclaredMethod("capturarEntradasAccion", int[].class);
        metodo.setAccessible(true);

        assertDoesNotThrow(() -> metodo.invoke(panel, (Object) new int[]{0}));
    }

    @Test
    @DisplayName("Conteos visibles respetan filtros activos e ignorados")
    void testConteosVisiblesRespetanFiltrosEIgnorados() throws Exception {
        I18nUI.establecerIdioma("es");
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(100);
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, modelo, true));
        PanelHallazgos panel = holder[0];

        modelo.agregarHallazgos(List.of(
            new Hallazgo("https://example.com/a", "TA", "Hallazgo A", "High", "High"),
            new Hallazgo("https://example.com/b", "TB", "Hallazgo B", "High", "Medium"),
            new Hallazgo("https://example.com/c", "TC", "Hallazgo C", "Low", "Low")
        ));
        flushEdt();

        modelo.marcarComoIgnorado(0);
        flushEdt();

        JComboBox<?> comboSeveridad = obtenerCampo(panel, "comboSeveridad", JComboBox.class);
        JTable tabla = obtenerCampo(panel, "tabla", JTable.class);

        SwingUtilities.invokeAndWait(() -> comboSeveridad.setSelectedItem(I18nUI.Hallazgos.SEVERIDAD_HIGH()));
        flushEdt();

        assertEquals(2, tabla.getRowCount(), "La vista filtrada debe mostrar los dos hallazgos High");
        assertEquals(1, panel.obtenerHallazgosVisibles(),
            "Los conteos visibles deben excluir hallazgos ignorados aunque sigan visibles en la tabla");

        int[] estadisticas = panel.obtenerEstadisticasVisibles();
        assertEquals(1, estadisticas[0], "Solo debe contarse un hallazgo visible y no ignorado");
        assertEquals(0, estadisticas[1], "No debe haber Critical visibles");
        assertEquals(1, estadisticas[2], "Debe haber un High visible");
        assertEquals(0, estadisticas[3], "No debe haber Medium visibles");
        assertEquals(0, estadisticas[4], "El Low quedó fuera por el filtro");
        assertEquals(0, estadisticas[5], "No debe haber Info visibles");
    }

    @Test
    @DisplayName("Popup fuera de filas limpia selección previa para evitar acciones residuales")
    void testPopupFueraFilasLimpiaSeleccionPrevia() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(100);
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, modelo, true));
        PanelHallazgos panel = holder[0];

        modelo.agregarHallazgos(List.of(
            new Hallazgo("https://example.com/a", "TA", "Hallazgo A", "High", "High"),
            new Hallazgo("https://example.com/b", "TB", "Hallazgo B", "Low", "Medium")
        ));
        flushEdt();

        JTable tabla = obtenerCampo(panel, "tabla", JTable.class);
        SwingUtilities.invokeAndWait(() -> tabla.setRowSelectionInterval(0, 0));
        assertEquals(1, tabla.getSelectedRowCount(), "assertEquals failed at PanelHallazgosFiltrosTest.java:211");

        Method ajustarSeleccion = PanelHallazgos.class.getDeclaredMethod(
            "ajustarSeleccionParaMenuContextual", int.class, boolean.class
        );
        ajustarSeleccion.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try {
                ajustarSeleccion.invoke(panel, -1, false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        flushEdt();

        assertEquals(0, tabla.getSelectedRowCount(), "assertEquals failed at PanelHallazgosFiltrosTest.java:226");
    }

    @Test
    @DisplayName("Restaurar filtros respeta flags de persistencia deshabilitados")
    void testRestaurarFiltrosRespetaFlagsPersistencia() throws Exception {
        I18nUI.establecerIdioma("es");
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(100);
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);

        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, modelo, true));
        PanelHallazgos panel = holder[0];

        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerTextoFiltroHallazgos("sql");
        config.establecerFiltroSeveridadHallazgos(I18nUI.Hallazgos.SEVERIDAD_HIGH());
        config.establecerPersistirFiltroBusquedaHallazgos(false);
        config.establecerPersistirFiltroSeveridadHallazgos(false);

        SwingUtilities.invokeAndWait(() -> panel.establecerConfiguracion(config));
        flushEdt();

        JTextField campoBusqueda = obtenerCampo(panel, "campoBusqueda", JTextField.class);
        JComboBox<?> comboSeveridad = obtenerCampo(panel, "comboSeveridad", JComboBox.class);

        assertEquals("", campoBusqueda.getText(), "No debe restaurar texto cuando la persistencia está deshabilitada");
        assertEquals(0, comboSeveridad.getSelectedIndex(),
            "No debe restaurar severidad cuando la persistencia está deshabilitada");
    }

    @Test
    @DisplayName("Restaurar severidad localizada heredada sigue funcionando al cambiar idioma")
    void testRestaurarFiltroSeveridadLocalizadaTrasCambioIdioma() throws Exception {
        I18nUI.establecerIdioma("es");
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(100);
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);

        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, modelo, true));
        PanelHallazgos panel = holder[0];

        modelo.agregarHallazgos(List.of(
            new Hallazgo("https://example.com/a", "TA", "Hallazgo A", "High", "High"),
            new Hallazgo("https://example.com/b", "TB", "Hallazgo B", "Low", "Medium")
        ));
        flushEdt();

        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerFiltroSeveridadHallazgos(I18nUI.Hallazgos.SEVERIDAD_HIGH());
        config.establecerPersistirFiltroSeveridadHallazgos(true);

        I18nUI.establecerIdioma("en");
        SwingUtilities.invokeAndWait(() -> {
            panel.aplicarIdioma();
            panel.establecerConfiguracion(config);
        });
        flushEdt();

        JComboBox<?> comboSeveridad = obtenerCampo(panel, "comboSeveridad", JComboBox.class);
        JTable tabla = obtenerCampo(panel, "tabla", JTable.class);

        assertEquals("High", comboSeveridad.getSelectedItem(),
            "La severidad heredada debe mapearse al idioma actual");
        assertEquals(1, tabla.getRowCount(),
            "El filtro restaurado debe seguir aplicándose tras cambiar idioma");
    }

    @Test
    @DisplayName("Guardar severidad persiste valor canónico independiente del idioma")
    void testGuardarFiltroSeveridadPersisteValorCanonico() throws Exception {
        I18nUI.establecerIdioma("es");
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(100);
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);

        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, modelo, true));
        PanelHallazgos panel = holder[0];

        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerPersistirFiltroSeveridadHallazgos(true);
        SwingUtilities.invokeAndWait(() -> panel.establecerConfiguracion(config));
        flushEdt();

        JComboBox<?> comboSeveridad = obtenerCampo(panel, "comboSeveridad", JComboBox.class);
        Method guardarEstado = PanelHallazgos.class.getDeclaredMethod("guardarEstadoFiltrosAhora");
        guardarEstado.setAccessible(true);

        SwingUtilities.invokeAndWait(() -> {
            comboSeveridad.setSelectedItem(I18nUI.Hallazgos.SEVERIDAD_HIGH());
            try {
                guardarEstado.invoke(panel);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        flushEdt();

        assertEquals(Hallazgo.SEVERIDAD_HIGH, config.obtenerFiltroSeveridadHallazgos(),
            "La configuración debe almacenar la severidad con un valor estable");
    }

    @SuppressWarnings({"unchecked", "PMD.UnusedFormalParameter"})
    private <T> T obtenerCampo(Object target, String nombre, Class<T> tipo) throws Exception {
        Field field = target.getClass().getDeclaredField(nombre);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    private void assertTooltipEncabezado(JTableHeader encabezado, int columnaVista, String esperado) throws Exception {
        Rectangle rect = encabezado.getHeaderRect(columnaVista);
        MouseEvent evento = new MouseEvent(
                encabezado,
                MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(),
                0,
                rect.x + Math.max(1, rect.width / 2),
                rect.y + Math.max(1, rect.height / 2),
                0,
                false);
        SwingUtilities.invokeAndWait(() -> {
            for (var listener : encabezado.getMouseMotionListeners()) {
                listener.mouseMoved(evento);
            }
        });
        assertEquals(esperado, encabezado.getToolTipText(), "assertEquals failed at PanelHallazgosFiltrosTest.java:430");
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
    }
}
