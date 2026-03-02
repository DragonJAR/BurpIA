package com.burpia.util;
import com.burpia.i18n.I18nUI;
import com.burpia.i18n.IdiomaUI;
import com.burpia.ui.EstilosUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.Color;
import java.io.PrintWriter;
import java.io.StringWriter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;





@DisplayName("GestorConsolaGUI Tests")
class GestorConsolaGUITest {

    @AfterEach
    void resetIdioma() {
        I18nUI.establecerIdioma(IdiomaUI.ES);
    }

    @Test
    @DisplayName("Genera resumen con contadores numéricos correctos")
    void testGenerarResumen() {
        I18nUI.establecerIdioma(IdiomaUI.ES);
        GestorConsolaGUI gestor = new GestorConsolaGUI();
        gestor.registrarInfo("info");
        gestor.registrarVerbose("verbose");
        gestor.registrarError("error");

        assertEquals("Total: 3 | Info: 1 | Verbose: 1 | Errores: 1", gestor.generarResumen());
    }

    @Test
    @DisplayName("Resumen se localiza a inglés cuando idioma UI es EN")
    void testGenerarResumenEnIngles() {
        I18nUI.establecerIdioma(IdiomaUI.EN);
        GestorConsolaGUI gestor = new GestorConsolaGUI();
        gestor.registrarInfo("info");
        gestor.registrarVerbose("verbose");
        gestor.registrarError("error");

        assertEquals("Total: 3 | Info: 1 | Verbose: 1 | Errors: 1", gestor.generarResumen());
        I18nUI.establecerIdioma(IdiomaUI.ES);
    }

    @Test
    @DisplayName("Preserva origen y nivel al duplicar a stream original")
    void testPreservaOrigenYNivelEnStream() {
        I18nUI.establecerIdioma(IdiomaUI.ES);
        GestorConsolaGUI gestor = new GestorConsolaGUI();
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        gestor.capturarStreamsOriginales(new PrintWriter(out, true), new PrintWriter(err, true));

        gestor.registrar("ManejadorBurpIA", "Evento informativo", GestorConsolaGUI.TipoLog.INFO);
        gestor.registrar("ManejadorBurpIA", "Evento detallado", GestorConsolaGUI.TipoLog.VERBOSE);
        gestor.registrar("ManejadorBurpIA", "Evento de error", GestorConsolaGUI.TipoLog.ERROR);

        String salidaOut = out.toString();
        String salidaErr = err.toString();
        assertTrue(salidaOut.contains("[ManejadorBurpIA]"));
        assertTrue(salidaOut.contains("[RASTREO]"));
        assertTrue(salidaErr.contains("[ManejadorBurpIA] [ERROR]"));
    }

    @Test
    @DisplayName("Etiqueta verbose se localiza a TRACE cuando idioma UI es EN")
    void testEtiquetaVerboseEnIngles() {
        I18nUI.establecerIdioma(IdiomaUI.EN);
        GestorConsolaGUI gestor = new GestorConsolaGUI();
        StringWriter out = new StringWriter();
        gestor.capturarStreamsOriginales(new PrintWriter(out, true), new PrintWriter(new StringWriter(), true));

        gestor.registrar("ManejadorBurpIA", "Evento detallado", GestorConsolaGUI.TipoLog.VERBOSE);

        assertTrue(out.toString().contains("[TRACE]"));
    }

    @Test
    @DisplayName("Conserva backlog previo y lo muestra al establecer consola")
    void testBacklogAntesDeConsola() throws Exception {
        I18nUI.establecerIdioma(IdiomaUI.ES);
        GestorConsolaGUI gestor = new GestorConsolaGUI();
        gestor.registrarInfo("BurpIA", "Log previo 1");
        gestor.registrarError("BurpIA", "Log previo 2");

        JTextPane consola = new JTextPane();
        SwingUtilities.invokeAndWait(() -> gestor.establecerConsola(consola));
        SwingUtilities.invokeAndWait(() -> {
        });

        String texto = consola.getText();
        assertTrue(texto.contains("Log previo 1"));
        assertTrue(texto.contains("Log previo 2"));
    }

    @Test
    @DisplayName("NOTA/ACCION y NOTE/ACTION se resaltan en negrilla")
    void testEtiquetasNotaAccionEnNegrilla() throws Exception {
        I18nUI.establecerIdioma(IdiomaUI.ES);
        GestorConsolaGUI gestor = new GestorConsolaGUI();
        JTextPane consola = new JTextPane();
        SwingUtilities.invokeAndWait(() -> gestor.establecerConsola(consola));
        SwingUtilities.invokeAndWait(() -> {
            gestor.registrarInfo("BurpIA", "NOTA: prueba visual. ACCION: revisar scope.");
            gestor.registrarInfo("BurpIA", "NOTE: visual check. ACTION: validate scope.");
        });
        SwingUtilities.invokeAndWait(() -> {
        });

        String texto = consola.getDocument().getText(0, consola.getDocument().getLength());
        assertTrue(estaEnNegrilla(consola, texto, "NOTA:"));
        assertTrue(estaEnNegrilla(consola, texto, "ACCION:"));
        assertTrue(estaEnNegrilla(consola, texto, "NOTE:"));
        assertTrue(estaEnNegrilla(consola, texto, "ACTION:"));
    }

    @Test
    @DisplayName("Reaplicar tema de consola conserva contadores y mantiene contraste AA")
    void testAplicarTemaConsolaConfiable() throws Exception {
        GestorConsolaGUI gestor = new GestorConsolaGUI();
        JTextPane consola = new JTextPane();

        SwingUtilities.invokeAndWait(() -> {
            consola.setBackground(new Color(30, 32, 36));
            gestor.establecerConsola(consola);
        });
        SwingUtilities.invokeAndWait(() -> {
            gestor.registrarInfo("info inicial");
            gestor.registrarError("error inicial");
        });
        SwingUtilities.invokeAndWait(() -> {
        });

        int totalAntes = gestor.obtenerTotalLogs();
        Color colorInfoAntes = StyleConstants.getForeground(
            consola.getStyledDocument().getStyle("Info")
        );

        SwingUtilities.invokeAndWait(() -> {
            consola.setBackground(new Color(247, 247, 247));
            gestor.aplicarTemaConsola();
        });
        SwingUtilities.invokeAndWait(() -> {
        });

        Color colorInfoDespues = StyleConstants.getForeground(
            consola.getStyledDocument().getStyle("Info")
        );
        assertNotEquals(colorInfoAntes, colorInfoDespues);
        assertEquals(totalAntes, gestor.obtenerTotalLogs());
        assertTrue(EstilosUI.ratioContraste(colorInfoDespues, consola.getBackground()) >= EstilosUI.CONTRASTE_AA_NORMAL);

        SwingUtilities.invokeAndWait(() -> gestor.registrarVerbose("trace posterior"));
        SwingUtilities.invokeAndWait(() -> {
        });
        assertTrue(consola.getText().contains("trace posterior"));
    }

    private boolean estaEnNegrilla(JTextPane consola, String texto, String etiqueta) {
        int idx = texto.indexOf(etiqueta);
        if (idx < 0) {
            return false;
        }
        AttributeSet attrs = consola.getStyledDocument().getCharacterElement(idx).getAttributes();
        return StyleConstants.isBold(attrs);
    }
}
