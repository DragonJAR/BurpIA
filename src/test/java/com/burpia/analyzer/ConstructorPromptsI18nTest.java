package com.burpia.analyzer;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.SolicitudAnalisis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;




@DisplayName("ConstructorPrompts I18n Tests")
class ConstructorPromptsI18nTest {

    @Test
    @DisplayName("Prompt por defecto inyecta idioma de salida en ingles y espanol")
    void testPromptPorDefectoInyectaIdiomaSalida() {
        ConfiguracionAPI configEn = new ConfiguracionAPI();
        configEn.establecerIdiomaUi("en");
        ConstructorPrompts constructorEn = new ConstructorPrompts(configEn);

        String promptEn = constructorEn.construirPromptAnalisis(crearSolicitudBase());
        assertTrue(promptEn.contains("OUTPUT LANGUAGE: English"));
        assertFalse(promptEn.contains("{OUTPUT_LANGUAGE}"));

        ConfiguracionAPI configEs = new ConfiguracionAPI();
        configEs.establecerIdiomaUi("es");
        ConstructorPrompts constructorEs = new ConstructorPrompts(configEs);

        String promptEs = constructorEs.construirPromptAnalisis(crearSolicitudBase());
        assertTrue(promptEs.contains("OUTPUT LANGUAGE: Spanish"));
        assertFalse(promptEs.contains("{OUTPUT_LANGUAGE}"));
    }

    @Test
    @DisplayName("Si no hay token de idioma agrega instruccion de fallback")
    void testFallbackIdiomaCuandoNoExisteToken() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerIdiomaUi("en");
        config.establecerPromptConfigurable("Analyze security issues.\n{REQUEST}\n{RESPONSE}");

        ConstructorPrompts constructor = new ConstructorPrompts(config);
        String prompt = constructor.construirPromptAnalisis(crearSolicitudBase());

        assertTrue(prompt.contains("OUTPUT LANGUAGE: English"));
        assertTrue(prompt.contains("Write \"descripcion\" strictly in OUTPUT LANGUAGE."));
        assertTrue(prompt.contains("Keep \"severidad\" and \"confianza\" exactly as canonical values."));
    }

    @Test
    @DisplayName("Fallback de idioma se localiza al espanol cuando idioma UI es ES")
    void testFallbackIdiomaEnEspanol() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerIdiomaUi("es");
        config.establecerPromptConfigurable("Analiza seguridad.\n{REQUEST}\n{RESPONSE}");

        ConstructorPrompts constructor = new ConstructorPrompts(config);
        String prompt = constructor.construirPromptAnalisis(crearSolicitudBase());

        assertTrue(prompt.contains("IDIOMA DE SALIDA: Spanish"));
        assertTrue(prompt.contains("Escribe \"descripcion\" estrictamente en IDIOMA DE SALIDA."));
    }

    @Test
    @DisplayName("Si existe token de idioma no agrega fallback duplicado")
    void testNoDuplicaFallbackCuandoExisteToken() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerIdiomaUi("en");
        config.establecerPromptConfigurable("Only JSON.\nLang={OUTPUT_LANGUAGE}\n{REQUEST}\n{RESPONSE}");

        ConstructorPrompts constructor = new ConstructorPrompts(config);
        String prompt = constructor.construirPromptAnalisis(crearSolicitudBase());

        assertTrue(prompt.contains("Lang=English"));
        assertFalse(prompt.contains("Keep \"severidad\" and \"confianza\" exactly as canonical values."));
    }

    @Test
    @DisplayName("Placeholders de no disponible respetan idioma UI")
    void testPlaceholdersNoDisponiblesPorIdioma() {
        ConfiguracionAPI configEn = new ConfiguracionAPI();
        configEn.establecerIdiomaUi("en");
        ConstructorPrompts constructorEn = new ConstructorPrompts(configEn);
        String promptEn = constructorEn.construirPromptAnalisis(null);
        assertTrue(promptEn.contains("[REQUEST NOT AVAILABLE]"));
        assertTrue(promptEn.contains("[RESPONSE NOT AVAILABLE]"));

        ConfiguracionAPI configEs = new ConfiguracionAPI();
        configEs.establecerIdiomaUi("es");
        ConstructorPrompts constructorEs = new ConstructorPrompts(configEs);
        String promptEs = constructorEs.construirPromptAnalisis(null);
        assertTrue(promptEs.contains("[REQUEST NO DISPONIBLE]"));
        assertTrue(promptEs.contains("[RESPONSE NO DISPONIBLE]"));
    }

    private SolicitudAnalisis crearSolicitudBase() {
        return new SolicitudAnalisis(
            "https://example.com/test",
            "GET",
            "GET /test HTTP/1.1\nHost: example.com",
            "",
            "hash-test",
            null,
            200,
            "HTTP/1.1 200 OK",
            "{\"ok\":true}"
        );
    }
}
