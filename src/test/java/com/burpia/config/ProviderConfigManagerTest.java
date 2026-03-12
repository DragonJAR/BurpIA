package com.burpia.config;

import com.burpia.i18n.I18nUI;
import com.burpia.ui.EstadoProveedorUI;
import com.burpia.util.GestorLoggingUnificado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para ProviderConfigManager.
 *
 * @author BurpIA Team
 * @since 1.0.3
 */
class ProviderConfigManagerTest {

    @Mock
    private ConfiguracionAPI config;

    @Mock
    private GestorLoggingUnificado gestorLogging;

    @Mock
    private JComboBox<String> comboProveedor;

    @Mock
    private JComboBox<String> comboModelo;

    @Mock
    private ComboBoxEditor comboModeloEditor;

    @Mock
    private JTextField txtUrl;

    @Mock
    private JPasswordField txtClave;

    @Mock
    private JTextField txtMaxTokens;

    @Mock
    private JTextField txtTimeoutModelo;

    @Mock
    private JButton btnAgregarProveedor;

    @Mock
    private JButton btnQuitarProveedor;

    @Mock
    private JButton btnSubirProveedor;

    @Mock
    private JButton btnBajarProveedor;

    @Mock
    private JCheckBox chkHabilitarMultiProveedor;

    @Mock
    private JLabel lblEstadoMultiProveedor;

    @Mock
    private JList<String> listaProveedoresDisponibles;

    @Mock
    private JList<String> listaProveedoresSeleccionados;

    private DefaultListModel<String> modeloListaDisponibles;
    private DefaultListModel<String> modeloListaSeleccionados;

    private ProviderConfigManager providerConfigManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        modeloListaDisponibles = new DefaultListModel<>();
        modeloListaSeleccionados = new DefaultListModel<>();

        when(comboModelo.getEditor()).thenReturn(comboModeloEditor);

        providerConfigManager = new ProviderConfigManager(config, gestorLogging);
        

        providerConfigManager.inicializarComponentesUI(
                comboProveedor,
                comboModelo,
                txtUrl,
                txtClave,
                txtMaxTokens,
                txtTimeoutModelo,
                btnAgregarProveedor,
                btnQuitarProveedor,
                btnSubirProveedor,
                btnBajarProveedor,
                chkHabilitarMultiProveedor,
                lblEstadoMultiProveedor,
                modeloListaDisponibles,
                modeloListaSeleccionados,
                listaProveedoresDisponibles,
                listaProveedoresSeleccionados
        );
    }

    @Test
    void testConstructorConNulos() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ProviderConfigManager(null, gestorLogging);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new ProviderConfigManager(config, null);
        });
    }

    @Test
    void testObtenerProveedorActual() {
        when(comboProveedor.getSelectedItem()).thenReturn("OpenAI");

        assertEquals("OpenAI", providerConfigManager.obtenerProveedorActual());
        verify(comboProveedor).getSelectedItem();
    }

    @Test
    void testObtenerProveedorActualConComboNulo() {
        ProviderConfigManager manager = new ProviderConfigManager(config, gestorLogging);
        assertNull(manager.obtenerProveedorActual());
    }

    @Test
    void testEsProveedorSeleccionado() {
        when(comboProveedor.getSelectedItem()).thenReturn("OpenAI");

        assertTrue(providerConfigManager.esProveedorSeleccionado("OpenAI"));
        assertFalse(providerConfigManager.esProveedorSeleccionado("Claude"));
    }

    @Test
    void testEsProveedorSeleccionadoConEntradaVacia() {
        assertFalse(providerConfigManager.esProveedorSeleccionado(""));
        assertFalse(providerConfigManager.esProveedorSeleccionado(null));
    }

    @Test
    void testEstablecerProveedorActual() {
        providerConfigManager.establecerProveedorActual("Claude");
        
        verify(comboProveedor).setSelectedItem("Claude");
    }

    @Test
    void testExtraerEstadoActualRapido() {
        when(txtClave.getPassword()).thenReturn("test-key".toCharArray());
        when(txtUrl.getText()).thenReturn("https://test.com");
        when(txtMaxTokens.getText()).thenReturn("2048");
        when(txtTimeoutModelo.getText()).thenReturn("60");
        when(comboModelo.getSelectedItem()).thenReturn("gpt-4");

        EstadoProveedorUI estado = providerConfigManager.extraerEstadoActualRapido();

        assertNotNull(estado);
        assertEquals("test-key", estado.obtenerApiKey());
        assertEquals("https://test.com", estado.obtenerBaseUrl());
        assertEquals(2048, estado.obtenerMaxTokens());
        assertEquals(60, estado.obtenerTimeout());
        assertEquals("gpt-4", estado.obtenerModelo());
    }

    @Test
    void testExtraerEstadoActualRapidoPreservaTextosInvalidosDeBorrador() {
        when(txtClave.getPassword()).thenReturn("test-key".toCharArray());
        when(txtUrl.getText()).thenReturn("https://test.com");
        when(txtMaxTokens.getText()).thenReturn("abc");
        when(txtTimeoutModelo.getText()).thenReturn("999999");
        when(comboModelo.getSelectedItem()).thenReturn("gpt-4");

        EstadoProveedorUI estado = providerConfigManager.extraerEstadoActualRapido();

        assertNotNull(estado);
        assertEquals("abc", estado.obtenerMaxTokensTexto());
        assertEquals("999999", estado.obtenerTimeoutTexto());
        assertEquals(EstadoProveedorUI.MAX_TOKENS_POR_DEFECTO, estado.obtenerMaxTokens());
        assertEquals(999999, estado.obtenerTimeout());
    }

    @Test
    void testObtenerConfiguracionParaGuardar() {
        EstadoProveedorUI estadoExistente = new EstadoProveedorUI("key1", "model1", "url1", 1024, 30);
        providerConfigManager.cambiarProveedor("OpenAI");
        Map<String, EstadoProveedorUI> config = providerConfigManager.obtenerConfiguracionParaGuardar();

        assertNotNull(config);
        assertTrue(config.isEmpty());
    }

    @Test
    void testCambiarProveedorConEntradaVacia() {
        providerConfigManager.cambiarProveedor("");
        providerConfigManager.cambiarProveedor(null);

        verify(comboProveedor, never()).setSelectedItem(anyString());
    }

    @Test
    void testActualizarEstadoMultiProveedorDeshabilitado() {
        when(chkHabilitarMultiProveedor.isSelected()).thenReturn(false);

        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = ProviderConfigManager.class.getDeclaredMethod("actualizarEstadoMultiProveedor");
            method.setAccessible(true);
            method.invoke(providerConfigManager);
        });

        verify(lblEstadoMultiProveedor).setText(I18nUI.Configuracion.TXT_MULTI_PROVEEDOR_DESHABILITADO());
    }

    @Test
    void testActualizarEstadoMultiProveedorHabilitadoSinProveedores() {
        when(chkHabilitarMultiProveedor.isSelected()).thenReturn(true);
        modeloListaSeleccionados.clear();

        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = ProviderConfigManager.class.getDeclaredMethod("actualizarEstadoMultiProveedor");
            method.setAccessible(true);
            method.invoke(providerConfigManager);
        });

        verify(lblEstadoMultiProveedor).setText(
                I18nUI.Configuracion.ALERTA_MULTI_PROVEEDOR_SIN_PROVEEDORES());
    }

    @Test
    void testActualizarEstadoMultiProveedorHabilitadoConUnProveedor() {
        when(chkHabilitarMultiProveedor.isSelected()).thenReturn(true);
        modeloListaSeleccionados.clear();
        modeloListaSeleccionados.addElement("OpenAI");

        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = ProviderConfigManager.class.getDeclaredMethod("actualizarEstadoMultiProveedor");
            method.setAccessible(true);
            method.invoke(providerConfigManager);
        });

        verify(lblEstadoMultiProveedor).setText(I18nUI.Configuracion.ALERTA_MULTI_PROVEEDOR_MENOS_DOS());
    }

    @Test
    void testActualizarEstadoMultiProveedorHabilitadoConMultiplesProveedores() {
        when(chkHabilitarMultiProveedor.isSelected()).thenReturn(true);
        modeloListaSeleccionados.clear();
        modeloListaSeleccionados.addElement("OpenAI");
        modeloListaSeleccionados.addElement("Claude");
        modeloListaSeleccionados.addElement("Gemini");

        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = ProviderConfigManager.class.getDeclaredMethod("actualizarEstadoMultiProveedor");
            method.setAccessible(true);
            method.invoke(providerConfigManager);
        });

        verify(lblEstadoMultiProveedor).setText(I18nUI.Configuracion.TXT_MULTI_PROVEEDOR_HABILITADO("3"));
    }

    @Test
    void testActualizarBotonesMultiProveedorDeshabilitado() {
        when(chkHabilitarMultiProveedor.isSelected()).thenReturn(false);

        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = ProviderConfigManager.class.getDeclaredMethod("actualizarBotonesMultiProveedor");
            method.setAccessible(true);
            method.invoke(providerConfigManager);
        });

        verify(btnAgregarProveedor).setEnabled(false);
        verify(btnQuitarProveedor).setEnabled(false);
    }

    @Test
    void testActualizarBotonesMultiProveedorHabilitadoSinSeleccion() {
        when(chkHabilitarMultiProveedor.isSelected()).thenReturn(true);

        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = ProviderConfigManager.class.getDeclaredMethod("actualizarBotonesMultiProveedor");
            method.setAccessible(true);
            method.invoke(providerConfigManager);
        });

        verify(btnAgregarProveedor).setEnabled(false);
        verify(btnQuitarProveedor).setEnabled(false);
    }

    @Test
    void testActualizarBotonesMultiProveedorHabilitadoConSeleccion() {
        when(chkHabilitarMultiProveedor.isSelected()).thenReturn(true);
        modeloListaDisponibles.addElement("OpenAI");

        JList<String> listaDisponiblesReal = new JList<>(modeloListaDisponibles);
        listaDisponiblesReal.setSelectedIndex(0);

        java.lang.reflect.Field field = null;
        try {
            field = ProviderConfigManager.class.getDeclaredField("listaProveedoresDisponibles");
            field.setAccessible(true);
            field.set(providerConfigManager, listaDisponiblesReal);
        } catch (Exception e) {
            fail("No se pudo inyectar mock de listaProveedoresDisponibles: " + e.getMessage());
        }

        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = ProviderConfigManager.class.getDeclaredMethod("actualizarBotonesMultiProveedor");
            method.setAccessible(true);
            method.invoke(providerConfigManager);
        });

        verify(btnAgregarProveedor).setEnabled(true);
    }

    @Test
    void testSeleccionEnListasActualizaBotonesMultiProveedor() throws Exception {
        ProviderConfigManager manager = new ProviderConfigManager(config, gestorLogging);
        JComboBox<String> comboProveedorReal = new JComboBox<>(new String[] {"OpenAI", "Claude"});
        JComboBox<String> comboModeloReal = new JComboBox<>();
        JTextField txtUrlReal = new JTextField();
        JPasswordField txtClaveReal = new JPasswordField();
        JTextField txtMaxTokensReal = new JTextField();
        JTextField txtTimeoutReal = new JTextField();
        JButton btnAgregarReal = new JButton();
        JButton btnQuitarReal = new JButton();
        JButton btnSubirReal = new JButton();
        JButton btnBajarReal = new JButton();
        JCheckBox chkMultiReal = new JCheckBox();
        chkMultiReal.setSelected(true);
        JLabel lblEstadoReal = new JLabel();
        DefaultListModel<String> disponibles = new DefaultListModel<>();
        disponibles.addElement("OpenAI");
        DefaultListModel<String> seleccionados = new DefaultListModel<>();
        seleccionados.addElement("Claude");
        JList<String> listaDisponiblesReal = new JList<>(disponibles);
        JList<String> listaSeleccionadosReal = new JList<>(seleccionados);

        manager.inicializarComponentesUI(
            comboProveedorReal,
            comboModeloReal,
            txtUrlReal,
            txtClaveReal,
            txtMaxTokensReal,
            txtTimeoutReal,
            btnAgregarReal,
            btnQuitarReal,
            btnSubirReal,
            btnBajarReal,
            chkMultiReal,
            lblEstadoReal,
            disponibles,
            seleccionados,
            listaDisponiblesReal,
            listaSeleccionadosReal
        );

        java.lang.reflect.Method method = ProviderConfigManager.class.getDeclaredMethod("actualizarBotonesMultiProveedor");
        method.setAccessible(true);
        method.invoke(manager);

        assertFalse(btnAgregarReal.isEnabled(), "Sin selección disponible el botón Agregar debe iniciar deshabilitado");
        assertFalse(btnQuitarReal.isEnabled(), "Sin selección en seleccionados el botón Quitar debe iniciar deshabilitado");

        listaDisponiblesReal.setSelectedIndex(0);
        assertTrue(btnAgregarReal.isEnabled(), "Seleccionar un proveedor disponible debe habilitar Agregar");

        listaSeleccionadosReal.setSelectedIndex(0);
        assertTrue(btnQuitarReal.isEnabled(), "Seleccionar un proveedor ya agregado debe habilitar Quitar");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testActualizarBotonesMultiProveedorProtegeProveedorPrincipal() throws Exception {
        ProviderConfigManager manager = crearManagerConComponentesReales();
        JList<String> listaSeleccionados = obtenerCampoPrivado(manager, "listaProveedoresSeleccionados", JList.class);
        JButton btnQuitar = obtenerCampoPrivado(manager, "btnQuitarProveedor", JButton.class);
        JButton btnSubir = obtenerCampoPrivado(manager, "btnSubirProveedor", JButton.class);
        JButton btnBajar = obtenerCampoPrivado(manager, "btnBajarProveedor", JButton.class);
        JCheckBox chkMulti = obtenerCampoPrivado(manager, "chkHabilitarMultiProveedor", JCheckBox.class);
        DefaultListModel<String> seleccionados = obtenerCampoPrivado(manager, "modeloListaSeleccionados", DefaultListModel.class);

        chkMulti.setSelected(true);
        seleccionados.addElement("OpenAI");
        seleccionados.addElement("Claude");
        seleccionados.addElement("Gemini");
        manager.establecerProveedorActual("OpenAI");

        listaSeleccionados.setSelectedIndex(0);
        invocarMetodoPrivado(manager, "actualizarBotonesMultiProveedor");

        assertFalse(btnQuitar.isEnabled(), "El proveedor principal no debe poder quitarse");
        assertFalse(btnSubir.isEnabled(), "El proveedor principal no debe poder moverse");
        assertFalse(btnBajar.isEnabled(), "El proveedor principal no debe poder moverse");

        listaSeleccionados.setSelectedIndex(1);
        invocarMetodoPrivado(manager, "actualizarBotonesMultiProveedor");

        assertTrue(btnQuitar.isEnabled(), "Un proveedor secundario sí debe poder quitarse");
        assertFalse(btnSubir.isEnabled(), "Ningún proveedor secundario debe adelantarse al principal");
        assertTrue(btnBajar.isEnabled(), "Debe poder reordenarse debajo del principal");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCambiarProveedorMantieneMaximoCincoEnMultiProveedor() {
        ConfiguracionAPI configReal = new ConfiguracionAPI();
        ProviderConfigManager manager = crearManagerConComponentesReales(configReal);
        DefaultListModel<String> disponibles = obtenerCampoPrivado(manager, "modeloListaDisponibles", DefaultListModel.class);
        DefaultListModel<String> seleccionados = obtenerCampoPrivado(manager, "modeloListaSeleccionados", DefaultListModel.class);
        JCheckBox chkMulti = obtenerCampoPrivado(manager, "chkHabilitarMultiProveedor", JCheckBox.class);

        chkMulti.setSelected(true);
        seleccionados.addElement("OpenAI");
        seleccionados.addElement("Claude");
        seleccionados.addElement("Gemini");
        seleccionados.addElement("Ollama");
        seleccionados.addElement("Z.ai");
        disponibles.addElement(ProveedorAI.PROVEEDOR_CUSTOM_01);

        manager.establecerProveedorActual("OpenAI");
        manager.cambiarProveedor(ProveedorAI.PROVEEDOR_CUSTOM_01);

        assertEquals(5, seleccionados.getSize(), "La sincronización del proveedor principal no debe superar el máximo");
        assertEquals(ProveedorAI.PROVEEDOR_CUSTOM_01, seleccionados.getElementAt(0),
                "El proveedor principal activo debe quedar primero");
        assertEquals("OpenAI", seleccionados.getElementAt(1),
                "El proveedor principal anterior debe conservarse en la lista");
        assertFalse(contieneElemento(seleccionados, "Z.ai"),
                "Debe expulsarse el último proveedor secundario para respetar el límite");
        assertTrue(contieneElemento(disponibles, "Z.ai"),
                "El proveedor desplazado debe volver a la lista de disponibles");
    }

    @Test
    void testValidarEstadoActualDetectaModeloVacio() {
        when(comboProveedor.getSelectedItem()).thenReturn("OpenAI");
        when(comboModelo.getSelectedItem()).thenReturn("");
        when(txtTimeoutModelo.getText()).thenReturn("60");
        when(txtMaxTokens.getText()).thenReturn("2048");
        when(txtUrl.getText()).thenReturn("https://api.openai.com/v1");

        ProviderConfigManager.ValidationResultEstadoProveedor resultado =
                providerConfigManager.validarEstadoActual(false, false);

        assertFalse(resultado.esValido());
        assertEquals("modelo", resultado.obtenerCampo());
        assertEquals(I18nUI.Configuracion.ERROR_MODELO_REQUERIDO(), resultado.obtenerMensajeError());
    }

    @Test
    void testValidarEstadoActualParaCargaModelosPermiteModeloVacio() {
        when(comboProveedor.getSelectedItem()).thenReturn("OpenAI");
        when(comboModelo.getSelectedItem()).thenReturn("");
        when(txtTimeoutModelo.getText()).thenReturn("60");
        when(txtMaxTokens.getText()).thenReturn("2048");
        when(txtUrl.getText()).thenReturn("https://api.openai.com/v1");
        when(txtClave.getPassword()).thenReturn(("sk-" + "a".repeat(48)).toCharArray());

        ProviderConfigManager.ValidationResultEstadoProveedor resultado =
                providerConfigManager.validarEstadoActualParaCargaModelos(true);

        assertTrue(resultado.esValido());
        assertNotNull(resultado.obtenerEstado());
        assertEquals("", resultado.obtenerEstado().obtenerModelo());
    }

    @Test
    void testValidarEstadoActualDetectaMaxTokensNoNumerico() {
        when(comboProveedor.getSelectedItem()).thenReturn("OpenAI");
        when(comboModelo.getSelectedItem()).thenReturn("gpt-4o");
        when(txtTimeoutModelo.getText()).thenReturn("60");
        when(txtMaxTokens.getText()).thenReturn("abc");
        when(txtUrl.getText()).thenReturn("https://api.openai.com/v1");

        ProviderConfigManager.ValidationResultEstadoProveedor resultado =
                providerConfigManager.validarEstadoActual(false, false);

        assertFalse(resultado.esValido());
        assertEquals("maxTokens", resultado.obtenerCampo());
        assertEquals(
                ConfigValidator.validarMaxTokens(0).obtenerMensajeError(),
                resultado.obtenerMensajeError());
    }

    @Test
    void testValidarEstadoActualParaConexionRequiereUrl() {
        when(comboProveedor.getSelectedItem()).thenReturn("OpenAI");
        when(comboModelo.getSelectedItem()).thenReturn("gpt-4o");
        when(txtTimeoutModelo.getText()).thenReturn("60");
        when(txtMaxTokens.getText()).thenReturn("2048");
        when(txtUrl.getText()).thenReturn("   ");
        when(txtClave.getPassword()).thenReturn(("sk-" + "a".repeat(48)).toCharArray());

        ProviderConfigManager.ValidationResultEstadoProveedor resultado =
                providerConfigManager.validarEstadoActual(true, true);

        assertFalse(resultado.esValido());
        assertEquals("url", resultado.obtenerCampo());
        assertEquals(I18nUI.Configuracion.MSG_URL_VACIA(), resultado.obtenerMensajeError());
    }

    @Test
    void testValidarEstadoActualParaConexionRequiereApiKeyValida() {
        when(comboProveedor.getSelectedItem()).thenReturn("OpenAI");
        when(comboModelo.getSelectedItem()).thenReturn("gpt-4o");
        when(txtTimeoutModelo.getText()).thenReturn("60");
        when(txtMaxTokens.getText()).thenReturn("2048");
        when(txtUrl.getText()).thenReturn("https://api.openai.com/v1");
        when(txtClave.getPassword()).thenReturn("".toCharArray());

        ProviderConfigManager.ValidationResultEstadoProveedor resultado =
                providerConfigManager.validarEstadoActual(true, true);

        assertFalse(resultado.esValido());
        assertEquals("apiKey", resultado.obtenerCampo());
        assertEquals(
                ConfigValidator.validarApiKey("", "OpenAI").obtenerMensajeError(),
                resultado.obtenerMensajeError());
    }

    private ProviderConfigManager crearManagerConComponentesReales() {
        return crearManagerConComponentesReales(new ConfiguracionAPI());
    }

    private ProviderConfigManager crearManagerConComponentesReales(ConfiguracionAPI configReal) {
        ProviderConfigManager manager = new ProviderConfigManager(configReal, gestorLogging);
        JComboBox<String> comboProveedorReal = new JComboBox<>(new String[] {
            "OpenAI",
            "Claude",
            "Gemini",
            "Ollama",
            "Z.ai",
            ProveedorAI.PROVEEDOR_CUSTOM_01
        });
        JComboBox<String> comboModeloReal = new JComboBox<>();
        JTextField txtUrlReal = new JTextField();
        JPasswordField txtClaveReal = new JPasswordField();
        JTextField txtMaxTokensReal = new JTextField();
        JTextField txtTimeoutReal = new JTextField();
        JButton btnAgregarReal = new JButton();
        JButton btnQuitarReal = new JButton();
        JButton btnSubirReal = new JButton();
        JButton btnBajarReal = new JButton();
        JCheckBox chkMultiReal = new JCheckBox();
        JLabel lblEstadoReal = new JLabel();
        DefaultListModel<String> disponibles = new DefaultListModel<>();
        DefaultListModel<String> seleccionados = new DefaultListModel<>();
        JList<String> listaDisponiblesReal = new JList<>(disponibles);
        JList<String> listaSeleccionadosReal = new JList<>(seleccionados);

        manager.inicializarComponentesUI(
                comboProveedorReal,
                comboModeloReal,
                txtUrlReal,
                txtClaveReal,
                txtMaxTokensReal,
                txtTimeoutReal,
                btnAgregarReal,
                btnQuitarReal,
                btnSubirReal,
                btnBajarReal,
                chkMultiReal,
                lblEstadoReal,
                disponibles,
                seleccionados,
                listaDisponiblesReal,
                listaSeleccionadosReal
        );
        return manager;
    }

    @SuppressWarnings("unchecked")
    private <T> T obtenerCampoPrivado(Object target, String nombreCampo, Class<T> tipo) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(nombreCampo);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (ReflectiveOperationException e) {
            fail("No se pudo leer el campo privado " + nombreCampo + ": " + e.getMessage());
            return null;
        }
    }

    private void invocarMetodoPrivado(Object target, String nombreMetodo) {
        try {
            java.lang.reflect.Method method = target.getClass().getDeclaredMethod(nombreMetodo);
            method.setAccessible(true);
            method.invoke(target);
        } catch (ReflectiveOperationException e) {
            fail("No se pudo invocar el método privado " + nombreMetodo + ": " + e.getMessage());
        }
    }

    private boolean contieneElemento(DefaultListModel<String> modelo, String valor) {
        for (int i = 0; i < modelo.size(); i++) {
            if (valor.equals(modelo.getElementAt(i))) {
                return true;
            }
        }
        return false;
    }
}
