package com.burpia.ui;

import com.burpia.i18n.I18nUI;
import com.burpia.model.Hallazgo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RenderizadorConfianza Tests")
class RenderizadorConfianzaTest {

    private RenderizadorConfianza renderizador;
    private JTable tabla;

    @BeforeEach
    void setUp() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            renderizador = new RenderizadorConfianza();
            DefaultTableModel modelo = new DefaultTableModel(5, 4);
            tabla = new JTable(modelo);
        });
        flushEdt();
    }

    @Test
    @DisplayName("getTableCellRendererComponent retorna componente válido para confianza Alta")
    void testRenderConfianzaAlta() throws Exception {
        String confianzaAlta = I18nUI.Hallazgos.CONFIANZA_ALTA();
        
        Component c = renderizador.getTableCellRendererComponent(
            tabla, confianzaAlta, false, false, 0, 0
        );
        
        assertNotNull(c, "assertNotNull failed at RenderizadorConfianzaTest.java:41");
        assertInstanceOf(RenderizadorConfianza.class, c);
    }

    @Test
    @DisplayName("getTableCellRendererComponent retorna componente válido para confianza Media")
    void testRenderConfianzaMedia() throws Exception {
        String confianzaMedia = I18nUI.Hallazgos.CONFIANZA_MEDIA();
        
        Component c = renderizador.getTableCellRendererComponent(
            tabla, confianzaMedia, false, false, 0, 0
        );
        
        assertNotNull(c, "assertNotNull failed at RenderizadorConfianzaTest.java:55");
    }

    @Test
    @DisplayName("getTableCellRendererComponent retorna componente válido para confianza Baja")
    void testRenderConfianzaBaja() throws Exception {
        String confianzaBaja = I18nUI.Hallazgos.CONFIANZA_BAJA();
        
        Component c = renderizador.getTableCellRendererComponent(
            tabla, confianzaBaja, false, false, 0, 0
        );
        
        assertNotNull(c, "assertNotNull failed at RenderizadorConfianzaTest.java:67");
    }

    @Test
    @DisplayName("Maneja valor null sin error")
    void testRenderValorNull() throws Exception {
        Component c = renderizador.getTableCellRendererComponent(
            tabla, null, false, false, 0, 0
        );
        
        assertNotNull(c, "assertNotNull failed at RenderizadorConfianzaTest.java:78");
        assertDoesNotThrow(() -> {
            flushEdt();
        });
    }

    @Test
    @DisplayName("Maneja string vacío sin error")
    void testRenderStringVacio() throws Exception {
        Component c = renderizador.getTableCellRendererComponent(
            tabla, "", false, false, 0, 0
        );
        
        assertNotNull(c, "assertNotNull failed at RenderizadorConfianzaTest.java:91");
    }

    @Test
    @DisplayName("Maneja valor desconocido sin error")
    void testRenderValorDesconocido() throws Exception {
        Component c = renderizador.getTableCellRendererComponent(
            tabla, "Desconocido", false, false, 0, 0
        );
        
        assertNotNull(c, "assertNotNull failed at RenderizadorConfianzaTest.java:102");
    }

    @Test
    @DisplayName("Detecta atributo STRIKETHROUGH cuando la fuente lo tiene")
    void testDetectaStrikethrough() throws Exception {
        Font fuenteConStrikethrough = tabla.getFont().deriveFont(
            java.util.Map.of(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON)
        );
        tabla.setFont(fuenteConStrikethrough);
        
        Component c = renderizador.getTableCellRendererComponent(
            tabla, I18nUI.Hallazgos.CONFIANZA_ALTA(), false, false, 0, 0
        );
        
        assertNotNull(c, "assertNotNull failed at RenderizadorConfianzaTest.java:118");
        Font fuenteResultado = c.getFont();
        assertTrue(
            fuenteResultado.getAttributes().containsKey(TextAttribute.STRIKETHROUGH),
            "assertTrue failed at RenderizadorConfianzaTest.java:122"
        );
    }

    @Test
    @DisplayName("No detecta STRIKETHROUGH cuando la fuente no lo tiene")
    void testNoDetectaStrikethrough() throws Exception {
        Component c = renderizador.getTableCellRendererComponent(
            tabla, I18nUI.Hallazgos.CONFIANZA_MEDIA(), false, false, 0, 0
        );
        
        assertNotNull(c, "assertNotNull failed at RenderizadorConfianzaTest.java:133");
        Font fuenteResultado = c.getFont();
        assertFalse(
            fuenteResultado.getAttributes().containsKey(TextAttribute.STRIKETHROUGH),
            "assertFalse failed at RenderizadorConfianzaTest.java:137"
        );
    }

    @Test
    @DisplayName("Constructor establece alineación CENTER")
    void testConstructorAlineacion() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            RenderizadorConfianza r = new RenderizadorConfianza();
            assertEquals(javax.swing.SwingConstants.CENTER, r.getHorizontalAlignment(), "assertEquals failed at RenderizadorConfianzaTest.java:238");
        });
        flushEdt();
    }

    @Test
    @DisplayName("Maneja múltiples llamadas consecutivas sin error")
    void testMultiplesLlamadasConsecutivas() throws Exception {
        for (int i = 0; i < 100; i++) {
            Component c = renderizador.getTableCellRendererComponent(
                tabla, 
                i % 3 == 0 ? I18nUI.Hallazgos.CONFIANZA_ALTA() : 
                i % 3 == 1 ? I18nUI.Hallazgos.CONFIANZA_MEDIA() : 
                I18nUI.Hallazgos.CONFIANZA_BAJA(),
                false, false, i % 5, 0
            );
            assertNotNull(c, "assertNotNull failed at RenderizadorConfianzaTest.java:255 - iteración " + i);
        }
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
    }
}
