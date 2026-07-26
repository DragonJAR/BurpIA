package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nUI;
import com.burpia.i18n.IdiomaUI;
import com.burpia.model.SolicitudAnalisis;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ConstructorPrompts Tests")
class ConstructorPromptsTest {

    private IdiomaUI idiomaOriginal;

    @BeforeEach
    void guardarIdiomaOriginal() {
        idiomaOriginal = I18nUI.obtenerIdioma();
    }

    @AfterEach
    void restaurarIdiomaOriginal() {
        I18nUI.establecerIdioma(idiomaOriginal);
    }

    @Test
    @DisplayName("Prompt de flujo usa el prompt configurable y omite respuesta inexistente")
    void testPromptDeFlujoUsaPromptConfigurableYOmiteRespuestaInexistente() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerIdiomaUi("es");
        I18nUI.establecerIdioma(IdiomaUI.ES);
        config.establecerPromptConfigurable("PROMPT FLUJO USUARIO\nREQ={REQUEST}\nRES={RESPONSE}");
        ConstructorPrompts constructor = new ConstructorPrompts(config);

        SolicitudAnalisis primera = new SolicitudAnalisis(
            "https://example.com/login",
            "POST",
            "POST /login HTTP/1.1\nHost: example.com",
            "username=admin",
            "hash-1",
            null,
            302,
            "HTTP/1.1 302 Found\nLocation: /dashboard",
            ""
        );
        SolicitudAnalisis segunda = new SolicitudAnalisis(
            "https://example.com/dashboard",
            "GET",
            "GET /dashboard HTTP/1.1\nHost: example.com",
            "",
            "hash-2",
            null,
            -1,
            "",
            ""
        );

        String prompt = constructor.construirPromptFlujo(List.of(primera, segunda));

        int indiceLogin = prompt.indexOf("=== REQUEST 1 ===");
        int indiceDashboard = prompt.indexOf("=== REQUEST 2 ===");

        assertTrue(prompt.startsWith("PROMPT FLUJO USUARIO"), prompt);
        assertTrue(indiceLogin >= 0, "assertTrue failed at ConstructorPromptsTest.java:47");
        assertTrue(indiceDashboard > indiceLogin, "assertTrue failed at ConstructorPromptsTest.java:48");
        assertTrue(prompt.contains("=== RESPONSE 1 ==="),
            "assertTrue failed at ConstructorPromptsTest.java:50");
        assertFalse(prompt.contains("Eres un experto en seguridad web"),
            "No debe construir un prompt de flujo alterno");
        assertFalse(prompt.contains("=== RESPONSE 2 ==="),
            "El flujo no debe inventar respuestas faltantes");
        assertFalse(prompt.contains("RESPUESTA:\nSTATUS: N/A\n[RESPONSE NO DISPONIBLE]"),
            "El flujo no debe inventar respuestas faltantes");
    }

    @Test
    @DisplayName("Prompt individual agrega bloques fallback cuando faltan tokens")
    void testPromptIndividualAgregaBloquesFallbackCuandoFaltanTokens() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerIdiomaUi("en");
        I18nUI.establecerIdioma(IdiomaUI.EN);
        config.establecerPromptConfigurable("Analyze business logic issues only.");
        ConstructorPrompts constructor = new ConstructorPrompts(config);

        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com/pay",
            "POST",
            "POST /pay HTTP/1.1\nHost: example.com",
            "{\"amount\":10}",
            "hash-3",
            null,
            200,
            "HTTP/1.1 200 OK",
            "{\"status\":\"ok\"}"
        );

        String prompt = constructor.construirPromptAnalisis(solicitud);

        assertTrue(prompt.contains("REQUEST:\nPOST https://example.com/pay"),
            "assertTrue failed at ConstructorPromptsTest.java:76");
        assertTrue(prompt.contains("RESPONSE:\nSTATUS: 200"),
            "assertTrue failed at ConstructorPromptsTest.java:78");
        assertTrue(prompt.contains("OUTPUT LANGUAGE: English"),
            "assertTrue failed at ConstructorPromptsTest.java:80");
    }

    @Test
    @DisplayName("Prompt individual no sustituye tokens literales dentro del body")
    void testPromptIndividualNoSustituyeTokensEnBody() {
        // Regresión H3: si el body contiene el literal "{RESPONSE}", no debe
        // expandirse con el contenido de la respuesta (antes se duplicaba).
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerIdiomaUi("en");
        I18nUI.establecerIdioma(IdiomaUI.EN);
        config.establecerPromptConfigurable("{REQUEST}");
        ConstructorPrompts constructor = new ConstructorPrompts(config);

        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com/echo",
            "POST",
            "POST /echo HTTP/1.1\nHost: example.com",
            "payload con {RESPONSE} literal",
            "hash-4",
            null,
            200,
            "HTTP/1.1 200 OK",
            "{\"ok\":true}"
        );

        String prompt = constructor.construirPromptAnalisis(solicitud);

        assertTrue(prompt.contains("payload con {RESPONSE} literal"),
            "El literal {RESPONSE} dentro del body no debe ser sustituido");
        assertTrue(prompt.contains("RESPONSE:\nSTATUS: 200"),
            "La respuesta real se agrega una sola vez via bloque fallback");
        assertEquals(1, contarOcurrencias(prompt, "{\"ok\":true}"),
            "El contenido de la respuesta no debe duplicarse");
    }

    private static int contarOcurrencias(String texto, String buscado) {
        int total = 0;
        int indice = texto.indexOf(buscado);
        while (indice >= 0) {
            total++;
            indice = texto.indexOf(buscado, indice + 1);
        }
        return total;
    }
}
