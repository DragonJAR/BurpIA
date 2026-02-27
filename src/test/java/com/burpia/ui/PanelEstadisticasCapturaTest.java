package com.burpia.ui;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Estadisticas;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;





@DisplayName("PanelEstadisticas Captura Tests")
class PanelEstadisticasCapturaTest {

    @AfterEach
    void resetIdioma() {
        I18nUI.establecerIdioma("es");
    }

    @Test
    @DisplayName("Boton captura alterna icono y ejecuta callback")
    void testBotonCapturaToggle() throws Exception {
        PanelEstadisticas panel = crearPanel();
        try {
            JButton boton = obtenerBotonCaptura(panel);
            AtomicInteger clicks = new AtomicInteger();
            panel.establecerManejadorToggleCaptura(clicks::incrementAndGet);

            assertEquals("⏸️", boton.getText());

            SwingUtilities.invokeAndWait(boton::doClick);
            assertEquals(1, clicks.get());

            panel.establecerEstadoCaptura(false);
            flushEdt();
            assertEquals("▶️", boton.getText());

            panel.establecerEstadoCaptura(true);
            flushEdt();
            assertEquals("⏸️", boton.getText());
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("aplicarIdioma refresca resumen operativo aunque no cambien estadisticas")
    void testAplicarIdiomaRefrescaResumenes() throws Exception {
        I18nUI.establecerIdioma("es");
        PanelEstadisticas panel = crearPanel();
        try {
            JLabel limite = obtenerEtiquetaLimite(panel);
            flushEdt();
            assertTrue(limite.getText().contains("Límite Hallazgos"));

            SwingUtilities.invokeAndWait(() -> {
                I18nUI.establecerIdioma("en");
                panel.aplicarIdioma();
            });
            flushEdt();

            assertTrue(limite.getText().contains("Findings Limit"));
        } finally {
            panel.destruir();
        }
    }

    private PanelEstadisticas crearPanel() throws Exception {
        final PanelEstadisticas[] holder = new PanelEstadisticas[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelEstadisticas(new Estadisticas(), () -> 1000));
        return holder[0];
    }

    private JButton obtenerBotonCaptura(PanelEstadisticas panel) throws Exception {
        Field field = PanelEstadisticas.class.getDeclaredField("botonCaptura");
        field.setAccessible(true);
        return (JButton) field.get(panel);
    }

    private JLabel obtenerEtiquetaLimite(PanelEstadisticas panel) throws Exception {
        Field field = PanelEstadisticas.class.getDeclaredField("etiquetaLimiteHallazgos");
        field.setAccessible(true);
        return (JLabel) field.get(panel);
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
    }
}
