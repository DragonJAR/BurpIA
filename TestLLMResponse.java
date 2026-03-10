import com.burpia.config.ConfiguracionAPI;
import com.burpia.analyzer.ConstructorPrompts;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.GestorLoggingUnificado;

import java.io.PrintWriter;

/**
 * Test simple para verificar qué JSON genera el LLM con el nuevo prompt.
 */
public class TestLLMResponse {
    public static void main(String[] args) {
        System.out.println("=== TESTING PROMPT RESPONSE ===");
        
        String prompt = ConfiguracionAPI.obtenerPromptPorDefecto();
        System.out.println("Prompt contiene 'hallazgo': " + prompt.contains("hallazgo"));
        System.out.println("Prompt contiene 'descripcion': " + prompt.contains("descripcion"));
        System.out.println("Prompt longitud: " + prompt.length());
        System.out.println("\n=== PROMPT COMPLETO ===");
        System.out.println(prompt);
        
        // Simular respuesta del LLM con ambos campos
        String jsonConAmbosCampos = "{\"hallazgos\":[{\"titulo\":\"SQLi\",\"severidad\":\"High\",\"confianza\":\"High\",\"hallazgo\":\"Inyección SQL detectada\",\"descripcion\":\"Descripción del campo\",\"evidencia\":\"parametro username\"}]}";
        String jsonConDescripcionSolo = "{\"hallazgos\":[{\"titulo\":\"SQLi\",\"severidad\":\"High\",\"confianza\":\"High\",\"descripcion\":\"Descripción del campo\",\"evidencia\":\"parametro username\"}]}";
        
        System.out.println("\n=== JSON CON AMBOS CAMPOS ===");
        System.out.println(jsonConAmbosCampos);
        
        System.out.println("\n=== JSON CON SOLO DESCRIPCIÓN ===");
        System.out.println(jsonConDescripcionSolo);
    }
}