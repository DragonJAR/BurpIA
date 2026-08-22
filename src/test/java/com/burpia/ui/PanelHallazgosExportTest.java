package com.burpia.ui;

import com.burpia.i18n.I18nUI;
import com.burpia.i18n.IdiomaUI;
import com.burpia.model.Hallazgo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unitarios para las funciones de exportación de PanelHallazgos.
 * <p>
 * Verifica que los métodos de escape CSV y JSON manejen correctamente
 * caracteres especiales como comillas, comas, saltos de línea y barras invertidas.
 * </p>
 */
@DisplayName("PanelHallazgos Export Tests")
class PanelHallazgosExportTest extends PanelTestBase {

    private IdiomaUI idiomaPrevio;

    @BeforeEach
    void fijarIdiomaIngles() {
        // Los valores esperados en estos tests usan los nombres canonical
        // ("High", "Medium", "Low"). Tras el fix de i18n en CSV/JSON export,
        // el valor de severidad/confianza se traduce al idioma de la UI.
        // Fijamos EN para que los valores traducidos coincidan con los
        // canonical English usados en las aserciones.
        idiomaPrevio = I18nUI.obtenerIdioma();
        I18nUI.establecerIdioma("en");
    }

    @AfterEach
    void restaurarIdioma() {
        if (idiomaPrevio != null) {
            I18nUI.establecerIdioma(idiomaPrevio.codigo());
        }
    }

    @Test
    @DisplayName("CSV escapa comillas, comas y saltos de linea")
    void testConstruirLineaCsvEscapaCamposEspeciales() throws Exception {
        PanelHallazgos panel = crearPanelHallazgos(false);
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
        PanelHallazgos panel = crearPanelHallazgos(false);
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

    @Test
    @DisplayName("CSV traduce severidad/confianza al idioma de la UI (regresión J3)")
    void testCsvTraduceSeveridadYConfianzaAlIdiomaUi() throws Exception {
        // Cambia a español para verificar que los valores se traducen
        // (el setUp del test fijó EN; acá lo cambiamos puntualmente).
        I18nUI.establecerIdioma("es");

        PanelHallazgos panel = crearPanelHallazgos(false);
        Hallazgo hallazgo = new Hallazgo(
            "10:00:00",
            "https://example.com/a",
            "Titulo X",
            "Detalle",
            "High",
            "Medium",
            null
        );

        String csv = invocarMetodoPrivado(panel, "construirLineaCsv", hallazgo);

        assertTrue(csv.contains("Alta"),
            "En español, el valor canonical 'High' debe traducirse a 'Alta' en el CSV. CSV: " + csv);
        assertTrue(csv.contains("Media") || csv.contains("Medio"),
            "En español, el valor canonical 'Medium' debe traducirse a 'Media/Medio' en el CSV. CSV: " + csv);
    }

    @Test
    @DisplayName("Validación de exportación acepta nombres de archivo relativos")
    void testValidarArchivoExportacionAceptaRutaRelativa() throws Exception {
        PanelHallazgos panel = crearPanelHallazgos(false);
        Method metodo = PanelHallazgos.class.getDeclaredMethod("validarArchivoExportacion", java.io.File.class);
        metodo.setAccessible(true);

        String resultado = (String) metodo.invoke(panel, new java.io.File("hallazgos-export.csv"));

        assertEquals(null, resultado, "assertEquals failed at PanelHallazgosExportTest.java:76");
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
