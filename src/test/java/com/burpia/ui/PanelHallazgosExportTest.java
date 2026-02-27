package com.burpia.ui;
import burp.api.montoya.MontoyaApi;
import com.burpia.model.Hallazgo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;





@DisplayName("PanelHallazgos Export Tests")
class PanelHallazgosExportTest {

    @Test
    @DisplayName("CSV escapa comillas, comas y saltos de linea")
    void testConstruirLineaCsvEscapaCamposEspeciales() throws Exception {
        PanelHallazgos panel = crearPanel();
        Hallazgo hallazgo = new Hallazgo(
            "10:00:00\nUTC",
            "https://example.com/a,b\"c",
            "Titulo X",
            "Linea 1\n\"detalle\"",
            "High",
            "Medium",
            null
        );

        String csv = invocarMetodoPrivado(panel, "construirLineaCsv", hallazgo);

        assertEquals("\"10:00:00\nUTC\",\"https://example.com/a,b\"\"c\",Titulo X,\"Linea 1\n\"\"detalle\"\"\",High,Medium", csv);
    }

    @Test
    @DisplayName("JSON escapa todos los campos exportados")
    void testConstruirObjetoJsonEscapaTodosLosCampos() throws Exception {
        PanelHallazgos panel = crearPanel();
        Hallazgo hallazgo = new Hallazgo(
            "10:00\"\\\n\t",
            "https://example.com/p?q=\"v\"\n",
            "Titulo especial",
            "Linea1\r\nLinea2\t\\\"",
            "High\"\n",
            "Low\\",
            null
        );

        String json = invocarMetodoPrivado(panel, "construirObjetoJson", hallazgo);

        assertTrue(json.contains("\"hora\": \"10:00\\\"\\\\\\n\\t\""));
        assertTrue(json.contains("\"url\": \"https://example.com/p?q=\\\"v\\\"\\n\""));
        assertTrue(json.contains("\"titulo\": \"Titulo especial\""));
        assertTrue(json.contains("\"hallazgo\": \"Linea1\\r\\nLinea2\\t\\\\\\\"\""));
        assertTrue(json.contains("\"severidad\": \"High\\\"\\n\""));
        assertTrue(json.contains("\"confianza\": \"Low\\\\\""));
    }

    private PanelHallazgos crearPanel() throws Exception {
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, new ModeloTablaHallazgos(100), false));
        return holder[0];
    }

    private String invocarMetodoPrivado(PanelHallazgos panel, String nombreMetodo, Hallazgo hallazgo) throws Exception {
        Method metodo = PanelHallazgos.class.getDeclaredMethod(nombreMetodo, Hallazgo.class);
        metodo.setAccessible(true);
        return (String) metodo.invoke(panel, hallazgo);
    }
}
