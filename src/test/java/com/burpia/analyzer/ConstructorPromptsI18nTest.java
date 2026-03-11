package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.SolicitudAnalisis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de internacionalización para ConstructorPrompts.
 * <p>
 * Verifica que los prompts se construyan correctamente en español e inglés,
 * incluyendo la inyección de idioma de salida, placeholders localizados
 * y las instrucciones de formato JSON.
 * </p>
 *
 * @see ConstructorPrompts
 * @see ConfiguracionAPI
 */
@DisplayName("ConstructorPrompts I18n Tests")
class ConstructorPromptsI18nTest {

        @Test
        @DisplayName("Constructor lanza NullPointerException con configuración null")
        void constructorConConfiguracionNull_lanzaExcepcion() {
                assertThrows(NullPointerException.class, () -> new ConstructorPrompts(null));
        }

        @Test
        @DisplayName("Prompt por defecto inyecta idioma de salida en inglés")
        void promptPorDefectoInyectaIdiomaSalidaEnIngles() {
                ConfiguracionAPI config = crearConfiguracionConIdioma("en");
                ConstructorPrompts constructor = new ConstructorPrompts(config);

                String prompt = constructor.construirPromptAnalisis(crearSolicitudBase());

                assertTrue(prompt.contains("OUTPUT LANGUAGE: English"),
                                "Debe contener instrucción de idioma en inglés");
                assertFalse(prompt.contains("{OUTPUT_LANGUAGE}"),
                                "No debe contener token sin reemplazar");
        }

        @Test
        @DisplayName("Prompt por defecto inyecta idioma de salida en español")
        void promptPorDefectoInyectaIdiomaSalidaEnEspanol() {
                ConfiguracionAPI config = crearConfiguracionConIdioma("es");
                ConstructorPrompts constructor = new ConstructorPrompts(config);

                String prompt = constructor.construirPromptAnalisis(crearSolicitudBase());

                assertTrue(prompt.contains("OUTPUT LANGUAGE: Spanish"),
                                "Debe contener instrucción de idioma con valor Spanish");
                assertFalse(prompt.contains("{OUTPUT_LANGUAGE}"),
                                "No debe contener token sin reemplazar");
        }

        @Test
        @DisplayName("Si no hay token de idioma agrega instrucción de fallback en inglés")
        void fallbackIdiomaCuandoNoExisteToken_enIngles() {
                ConfiguracionAPI config = crearConfiguracionConIdioma("en");
                config.establecerPromptConfigurable("Analyze security issues.\n{REQUEST}\n{RESPONSE}");

                ConstructorPrompts constructor = new ConstructorPrompts(config);
                String prompt = constructor.construirPromptAnalisis(crearSolicitudBase());

                assertTrue(prompt.contains("OUTPUT LANGUAGE: English"),
                                "Debe contener instrucción de idioma en inglés");
                assertTrue(prompt.contains("Write \"descripcion\" strictly in OUTPUT LANGUAGE."),
                                "Debe contener instrucción de fallback en inglés");
                assertTrue(prompt.contains("Keep \"severidad\" and \"confianza\" exactly as canonical values."),
                                "Debe contener instrucción sobre valores canónicos");
        }

        @Test
        @DisplayName("Fallback de idioma se localiza al español cuando idioma UI es ES")
        void fallbackIdiomaEnEspanol() {
                ConfiguracionAPI config = crearConfiguracionConIdioma("es");
                config.establecerPromptConfigurable("Analiza seguridad.\n{REQUEST}\n{RESPONSE}");

                ConstructorPrompts constructor = new ConstructorPrompts(config);
                String prompt = constructor.construirPromptAnalisis(crearSolicitudBase());

                assertTrue(prompt.contains("IDIOMA DE SALIDA: Spanish"),
                                "Debe contener instrucción de idioma en español");
                assertTrue(prompt.contains("Escribe \"descripcion\" estrictamente en IDIOMA DE SALIDA."),
                                "Debe contener instrucción de fallback en español");
        }

        @Test
        @DisplayName("Si existe token de idioma no agrega fallback duplicado")
        void noDuplicaFallbackCuandoExisteToken() {
                ConfiguracionAPI config = crearConfiguracionConIdioma("en");
                config.establecerPromptConfigurable("Only JSON.\nLang={OUTPUT_LANGUAGE}\n{REQUEST}\n{RESPONSE}");

                ConstructorPrompts constructor = new ConstructorPrompts(config);
                String prompt = constructor.construirPromptAnalisis(crearSolicitudBase());

                assertTrue(prompt.contains("Lang=English"),
                                "Debe reemplazar el token con el idioma");
                assertFalse(prompt.contains("Keep \"severidad\" and \"confianza\" exactly as canonical values."),
                                "No debe agregar fallback cuando ya existe token");
        }

        @Test
        @DisplayName("Placeholders de no disponible en inglés")
        void placeholdersNoDisponiblesEnIngles() {
                ConfiguracionAPI config = crearConfiguracionConIdioma("en");
                ConstructorPrompts constructor = new ConstructorPrompts(config);

                String prompt = constructor.construirPromptAnalisis(null);

                assertTrue(prompt.contains("[REQUEST NOT AVAILABLE]"),
                                "Debe contener placeholder de request en inglés");
                assertTrue(prompt.contains("[RESPONSE NOT AVAILABLE]"),
                                "Debe contener placeholder de response en inglés");
        }

        @Test
        @DisplayName("Placeholders de no disponible en español")
        void placeholdersNoDisponiblesEnEspanol() {
                ConfiguracionAPI config = crearConfiguracionConIdioma("es");
                ConstructorPrompts constructor = new ConstructorPrompts(config);

                String prompt = constructor.construirPromptAnalisis(null);

                assertTrue(prompt.contains("[REQUEST NO DISPONIBLE]"),
                                "Debe contener placeholder de request en español");
                assertTrue(prompt.contains("[RESPONSE NO DISPONIBLE]"),
                                "Debe contener placeholder de response en español");
        }

        @Test
        @DisplayName("Prompt incluye instrucciones de formato JSON")
        void promptIncluyeInstruccionesFormatoJson() {
                ConfiguracionAPI config = crearConfiguracionConIdioma("en");
                ConstructorPrompts constructor = new ConstructorPrompts(config);

                String prompt = constructor.construirPromptAnalisis(crearSolicitudBase());

                assertTrue(prompt.contains("<output_rules>"),
                                "Debe contener bloque de instrucciones de salida");
                assertTrue(prompt.contains("CRITICAL: If your evidencia or descripcion contains HTML"),
                                "Debe contener instrucción de escape de comillas en HTML");
        }

        @Test
        @DisplayName("Prompt con template vacío usa prompt por defecto")
        void promptConTemplateVacio_UsaPromptPorDefecto() {
                ConfiguracionAPI config = crearConfiguracionConIdioma("en");
                config.establecerPromptConfigurable("");

                ConstructorPrompts constructor = new ConstructorPrompts(config);
                String prompt = constructor.construirPromptAnalisis(crearSolicitudBase());

                assertTrue(prompt.contains("<http_request>"),
                                "Debe usar template por defecto con tags XML");
                assertTrue(prompt.contains("<http_response>"),
                                "Debe usar template por defecto con tags XML");
                assertTrue(prompt.contains("OUTPUT LANGUAGE: English"),
                                "Debe contener instrucción de idioma reemplazada");
        }

        /**
         * Crea una configuración con el idioma UI especificado.
         *
         * @param idioma el idioma de la UI ("en" o "es")
         * @return la configuración creada
         */
        private ConfiguracionAPI crearConfiguracionConIdioma(String idioma) {
                ConfiguracionAPI config = new ConfiguracionAPI();
                config.establecerIdiomaUi(idioma);
                return config;
        }

        /**
         * Crea una solicitud base para pruebas.
         *
         * @return una solicitud HTTP de ejemplo
         */
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
                                "{\"ok\":true}");
        }
}
