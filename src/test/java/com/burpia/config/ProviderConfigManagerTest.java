package com.burpia.config;

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
                modeloListaSeleccionados
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
}