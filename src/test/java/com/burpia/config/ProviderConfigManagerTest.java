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
        assertEquals("test-key", estado.getApiKey());
        assertEquals("https://test.com", estado.getBaseUrl());
        assertEquals(2048, estado.getMaxTokens());
        assertEquals(60, estado.getTimeout());
        assertEquals("gpt-4", estado.getModelo());
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
    void testValidarEstadoActualDetectaModeloVacio() {
        when(comboProveedor.getSelectedItem()).thenReturn("OpenAI");
        when(comboModelo.getSelectedItem()).thenReturn("");
        when(txtTimeoutModelo.getText()).thenReturn("60");
        when(txtMaxTokens.getText()).thenReturn("2048");
        when(txtUrl.getText()).thenReturn("https://api.openai.com/v1");

        ProviderConfigManager.ValidationResultEstadoProveedor resultado =
                providerConfigManager.validarEstadoActual(false, false);

        assertFalse(resultado.esValido());
        assertEquals("modelo", resultado.getCampo());
        assertEquals(I18nUI.Configuracion.ERROR_MODELO_REQUERIDO(), resultado.getMensajeError());
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
        assertEquals("maxTokens", resultado.getCampo());
        assertEquals(
                ConfigValidator.validarMaxTokens(0).getMensajeError(),
                resultado.getMensajeError());
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
        assertEquals("url", resultado.getCampo());
        assertEquals(I18nUI.Configuracion.MSG_URL_VACIA(), resultado.getMensajeError());
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
        assertEquals("apiKey", resultado.getCampo());
        assertEquals(
                ConfigValidator.validarApiKey("", "OpenAI").getMensajeError(),
                resultado.getMensajeError());
    }
}
