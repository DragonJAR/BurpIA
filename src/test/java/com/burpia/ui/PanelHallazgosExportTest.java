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

/**
 * Tests unitarios para las funciones de exportación de PanelHallazgos.
 * <p>
 * Verifica que los métodos de escape CSV y JSON manejen correctamente
 * caracteres especiales como comillas, comas, saltos de línea y barras invertidas.
 * </p>
 */
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

        assertEquals("\"10:00:00\nUTC\",\"https://example.com/a,b\"\"c\",Titulo X,\"Linea 1\n\"\"detalle\"\"\",High,Medium", csv, "assertEquals failed at PanelHallazgosExportTest.java:41");
    }

    @Test
    @DisplayName("JSON escapa todos los campos exportados")
    void testConstruirObjetoJsonEscapaTodosLosCampos() throws Exception {
        PanelHallazgos panel = crearPanel();
        Hallazgo hallazgo = new Hallazgo(
            "10:00\"\\\n\t",
            "https://example.com/p?q=\"v\"\n",
            "Titulo \"especial\"\n",
            "Linea1\r\nLinea2\t\\\"",
            "High",
            "Low",
            null
        );

        String json = invocarMetodoPrivado(panel, "construirObjetoJson", hallazgo);

        assertTrue(json.contains("\"hora\": \"10:00\\\"\\\\\\n\\t\""), "assertTrue failed at PanelHallazgosExportTest.java:60");
        assertTrue(json.contains("\"url\": \"https://example.com/p?q=\\\"v\\\"\\n\""), "assertTrue failed at PanelHallazgosExportTest.java:61");
        assertTrue(json.contains("\"titulo\": \"Titulo \\\"especial\\\"\\n\""), "assertTrue failed at PanelHallazgosExportTest.java:62");
        assertTrue(json.contains("\"hallazgo\": \"Linea1\\r\\nLinea2\\t\\\\\\\"\""), "assertTrue failed at PanelHallazgosExportTest.java:63");
        assertTrue(json.contains("\"severidad\": \"High\""), "assertTrue failed at PanelHallazgosExportTest.java:64");
        assertTrue(json.contains("\"confianza\": \"Low\""), "assertTrue failed at PanelHallazgosExportTest.java:65");
    }

    /**
     * Crea una instancia de PanelHallazgos para testing.
     * <p>
     * Utiliza invokeAndWait para asegurar que el componente Swing se crea
     * en el Event Dispatch Thread (EDT).
     * </p>
     *
     * @return PanelHallazgos configurado para testing
     * @throws Exception si ocurre error en la creación del panel
     */
    private PanelHallazgos crearPanel() throws Exception {
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, new ModeloTablaHallazgos(100), false));
        return holder[0];
    }

    /**
     * Invoca un método privado de PanelHallazgos usando reflexión.
     * <p>
     * Permite testear métodos de escape CSV/JSON que son privados.
     * </p>
     *
     * @param panel        Instancia de PanelHallazgos
     * @param nombreMetodo Nombre del método a invocar
     * @param hallazgo     Hallazgo a pasar como parámetro
     * @return Resultado del método invocado
     * @throws Exception si el método no existe o falla la invocación
     */
    private String invocarMetodoPrivado(PanelHallazgos panel, String nombreMetodo, Hallazgo hallazgo) throws Exception {
        Method metodo = PanelHallazgos.class.getDeclaredMethod(nombreMetodo, Hallazgo.class);
        metodo.setAccessible(true);
        return (String) metodo.invoke(panel, hallazgo);
    }
}
