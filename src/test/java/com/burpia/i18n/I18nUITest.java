package com.burpia.i18n;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;




@DisplayName("I18nUI Tests")
class I18nUITest {

    @AfterEach
    void resetIdioma() {
        I18nUI.establecerIdioma("es");
    }

    @Test
    @DisplayName("Usa español por defecto y cambia a inglés")
    void testIdiomaActual() {
        I18nUI.establecerIdioma("es");
        assertEquals("📋 TAREAS", I18nUI.Pestanias.TAREAS());

        I18nUI.establecerIdioma("en");
        assertEquals("📋 TASKS", I18nUI.Pestanias.TAREAS());
    }

    @Test
    @DisplayName("Idioma inválido cae en español")
    void testIdiomaInvalido() {
        I18nUI.establecerIdioma("fr");
        assertEquals(IdiomaUI.ES, I18nUI.obtenerIdioma());
    }

    @Test
    @DisplayName("Logs se mantienen en español cuando idioma es español")
    void testLogsEspanol() {
        I18nUI.establecerIdioma("es");
        String mensaje = "Analisis completado: https://target";
        assertEquals(mensaje, I18nLogs.tr(mensaje));
        assertEquals("Analisis completado: https://target",
            I18nLogs.tr("Analysis completed: https://target"));
        assertEquals("Configuracion guardada exitosamente en: /tmp/.burpia/config.json",
            I18nLogs.tr("Configuration saved successfully to: /tmp/.burpia/config.json"));
    }

    @Test
    @DisplayName("Logs se traducen a inglés cuando idioma es inglés")
    void testLogsIngles() {
        I18nUI.establecerIdioma("en");
        String traducido = I18nLogs.tr("Analisis completado: https://target");
        assertTrue(traducido.startsWith("Analysis completed"));
        assertTrue(I18nLogs.tr("Analizando: https://target").startsWith("Analyzing"));
        assertEquals("Configuration saved successfully", I18nLogs.tr("Configuracion guardada exitosamente"));
        String cancelado = I18nLogs.tr("Resultado descartado porque la tarea fue cancelada: https://target");
        assertTrue(cancelado.startsWith("Result discarded because task was canceled"));
        assertTrue(I18nLogs.tr("Hallazgo sin evidencia HTTP: no se puede crear AuditIssue")
            .startsWith("Finding without HTTP evidence"));
        assertEquals("Received response is null", I18nLogs.tr("Respuesta recibida es null"));
        assertEquals("Initiating request is null", I18nLogs.tr("Solicitud iniciadora es null"));
        assertEquals("Task created: Analisis HTTP - https://target",
            I18nLogs.tr("Tarea creada: Analisis HTTP - https://target"));
        assertTrue(I18nLogs.tr("Stack trace de la excepcion:")
            .startsWith("Exception stack trace"));
        assertEquals("Configuration saved successfully to: /tmp/.burpia/config.json",
            I18nLogs.tr("Configuracion guardada exitosamente en: /tmp/.burpia/config.json"));
    }

    @Test
    @DisplayName("Tooltip de ayuda del agente respeta idioma")
    void testTooltipAyudaAgenteI18n() {
        I18nUI.establecerIdioma("es");
        assertEquals(
            "Abrir guía de instalación/configuración de Claude Code.",
            I18nUI.Tooltips.Agente.GUIA_AGENTE("Claude Code")
        );

        I18nUI.establecerIdioma("en");
        assertEquals(
            "Open Claude Code installation/setup guide.",
            I18nUI.Tooltips.Agente.GUIA_AGENTE("Claude Code")
        );
    }

    @Test
    @DisplayName("Mensaje fallback de URL de guía respeta idioma")
    void testMensajeUrlGuiaAgenteI18n() {
        String url = "https://example.com/guide";

        I18nUI.establecerIdioma("es");
        assertEquals(
            "No se pudo abrir el navegador. URL de la guía: " + url,
            I18nUI.Consola.MSG_URL_GUIA_AGENTE(url)
        );

        I18nUI.establecerIdioma("en");
        assertEquals(
            "Could not open browser. Guide URL: " + url,
            I18nUI.Consola.MSG_URL_GUIA_AGENTE(url)
        );
    }

    @Test
    @DisplayName("Mensaje de comando configurado para binario respeta idioma")
    void testMensajeComandoConfiguradoI18n() {
        String comando = "claude --dangerously-skip-permissions";

        I18nUI.establecerIdioma("es");
        assertEquals("Comando configurado: " + comando,
            I18nUI.Configuracion.Agentes.MSG_COMANDO_CONFIGURADO(comando));

        I18nUI.establecerIdioma("en");
        assertEquals("Configured command: " + comando,
            I18nUI.Configuracion.Agentes.MSG_COMANDO_CONFIGURADO(comando));
    }
}
