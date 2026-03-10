import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.Hallazgo;

public class SimplePromptTest {
    public static void main(String[] args) {
        System.out.println("=== TESTING PROMPT RESPONSE ===");
        
        String prompt = ConfiguracionAPI.obtenerPromptPorDefecto();
        System.out.println("Prompt contiene 'hallazgo': " + prompt.contains("hallazgo"));
        System.out.println("Prompt contiene 'descripcion': " + prompt.contains("descripcion"));
        
        // Verificar que el prompt esté usando el campo correcto
        boolean usaCampoCorrecto = prompt.contains("\"hallazgo\":");
        System.out.println("Prompt usa campo 'hallazgo' correctamente: " + usaCampoCorrecto);
        
        System.out.println("\n=== PROMPT COMPLETO ===");
        System.out.println(prompt);
        
        // JSON con el campo correcto
        String jsonCorrecto = "{\"hallazgos\":[{\"titulo\":\"SQLi\",\"severidad\":\"High\",\"confianza\":\"High\",\"hallazgo\":\"Inyección SQL detectada\",\"evidencia\":\"parametro username\"}]}";
        
        System.out.println("\n=== JSON CON CAMPO 'hallazgo' (CORRECTO) ===");
        System.out.println(jsonCorrecto);
        
        // JSON con campo 'descripcion' (INCORRECTO)
        String jsonIncorrecto = "{\"hallazgos\":[{\"titulo\":\"SQLi\",\"severidad\":\"High\",\"confianza\":\"High\",\"descripcion\":\"Descripción del campo\",\"evidencia\":\"parametro username\"}]}";
        
        System.out.println("\n=== JSON CON CAMPO 'descripcion' (INCORRECTO) ===");
        System.out.println(jsonIncorrecto);
    }
}