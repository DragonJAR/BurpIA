package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.SolicitudAnalisis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ConstructorPrompts Tests")
class ConstructorPromptsTest {

    @Test
    @DisplayName("Prompt de flujo usa el prompt configurable y omite respuesta inexistente")
    void testPromptDeFlujoUsaPromptConfigurableYOmiteRespuestaInexistente() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerIdiomaUi("es");
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
}
