package com.burpia.i18n;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertEquals("📋 TAREAS", I18nUI.Pestanias.TAREAS(), "assertEquals failed at I18nUITest.java:24");

        I18nUI.establecerIdioma("en");
        assertEquals("📋 TASKS", I18nUI.Pestanias.TAREAS(), "assertEquals failed at I18nUITest.java:27");
    }

    @Test
    @DisplayName("Idioma inválido cae en español")
    void testIdiomaInvalido() {
        I18nUI.establecerIdioma("fr");
        assertEquals(IdiomaUI.ES, I18nUI.obtenerIdioma(), "assertEquals failed at I18nUITest.java:34");
    }

    @Test
    @DisplayName("Logs se mantienen en español cuando idioma es español")
    void testLogsEspanol() {
        I18nUI.establecerIdioma("es");
        String mensaje = "Analisis completado: https://target";
        assertEquals(mensaje, I18nLogs.tr(mensaje), "assertEquals failed at I18nUITest.java:42");
        assertEquals(mensaje, I18nLogs.tr("Analysis completed: https://target"), "assertEquals failed at I18nUITest.java:43");
        assertEquals("Configuracion guardada exitosamente en: /tmp/.burpia/config.json",
            I18nLogs.tr("Configuration saved successfully to: /tmp/.burpia/config.json"), "assertEquals failed at I18nUITest.java:44");
    }

    @Test
    @DisplayName("Logs se traducen a inglés cuando idioma es inglés")
    void testLogsIngles() {
        I18nUI.establecerIdioma("en");
        String traducido = I18nLogs.tr("Analisis completado: https://target");
        assertTrue(traducido.startsWith("Analysis completed"), "assertTrue failed at I18nUITest.java:53");
        assertTrue(I18nLogs.tr("Analizando: https://target").startsWith("Analyzing"), "assertTrue failed at I18nUITest.java:54");
        assertEquals("Configuration saved successfully", I18nLogs.tr("Configuracion guardada exitosamente"), "assertEquals failed at I18nUITest.java:55");
        String cancelado = I18nLogs.tr("Resultado descartado porque la tarea fue cancelada: https://target");
        assertTrue(cancelado.startsWith("Result discarded because task was canceled"), "assertTrue failed at I18nUITest.java:57");
        assertTrue(I18nLogs.tr("Hallazgo sin evidencia HTTP: no se puede crear AuditIssue")
            .startsWith("Finding without HTTP evidence"), "assertTrue failed at I18nUITest.java:58");
        assertEquals("Received response is null", I18nLogs.tr("Respuesta recibida es null"), "assertEquals failed at I18nUITest.java:60");
        assertEquals("Initiating request is null", I18nLogs.tr("Solicitud iniciadora es null"), "assertEquals failed at I18nUITest.java:61");
        assertEquals("Task created: Analisis HTTP - https://target",
            I18nLogs.tr("Tarea creada: Analisis HTTP - https://target"), "assertEquals failed at I18nUITest.java:62");
        assertTrue(I18nLogs.tr("Stack trace de la excepcion:")
            .startsWith("Exception stack trace"), "assertTrue failed at I18nUITest.java:64");
        assertEquals("Configuration saved successfully to: /tmp/.burpia/config.json",
            I18nLogs.tr("Configuracion guardada exitosamente en: /tmp/.burpia/config.json"), "assertEquals failed at I18nUITest.java:66");
    }

    @Test
    @DisplayName("Tooltip de ayuda del agente respeta idioma")
    void testTooltipAyudaAgenteI18n() {
        I18nUI.establecerIdioma("es");
        assertEquals(
            "Abrir guía de instalación/configuración de Claude Code.",
            I18nUI.Tooltips.Agente.GUIA_AGENTE("Claude Code")
        , "assertEquals failed at I18nUITest.java:74");

        I18nUI.establecerIdioma("en");
        assertEquals(
            "Open Claude Code installation/setup guide.",
            I18nUI.Tooltips.Agente.GUIA_AGENTE("Claude Code")
        , "assertEquals failed at I18nUITest.java:80");
    }

    @Test
    @DisplayName("Mensaje fallback de URL de guía respeta idioma")
    void testMensajeUrlGuiaAgenteI18n() {
        String url = "https://example.com/guide";

        I18nUI.establecerIdioma("es");
        assertEquals(
            "No se pudo abrir el navegador.\nURL de la guía:\n" + url,
            I18nUI.Consola.MSG_URL_GUIA_AGENTE(url)
        , "assertEquals failed at I18nUITest.java:92");

        I18nUI.establecerIdioma("en");
        assertEquals(
            "Could not open browser.\nGuide URL:\n" + url,
            I18nUI.Consola.MSG_URL_GUIA_AGENTE(url)
        , "assertEquals failed at I18nUITest.java:98");
    }

    @Test
    @DisplayName("Mensaje de URL en acerca de usa bloque y preserva protocolo")
    void testMensajeUrlAcercaDe() {
        I18nUI.establecerIdioma("es");
        assertEquals(
            "URL:\nhttps://www.dragonjar.org/contactar-empresa-de-seguridad-informatica",
            I18nUI.Configuracion.MSG_URL()
        , "assertEquals failed at I18nUITest.java:108");
        assertEquals(
            "https://www.dragonjar.org/contactar-empresa-de-seguridad-informatica",
            I18nUI.Configuracion.URL_SITIO_WEB()
        , "assertEquals failed at I18nUITest.java:112");
        assertEquals(
            "URL:\nhttps://example.com/docs",
            I18nUI.Configuracion.MSG_URL("https://example.com/docs")
        , "assertEquals failed at I18nUITest.java:116");

        I18nUI.establecerIdioma("en");
        assertEquals(
            "URL:\nhttps://www.dragonjar.org/contactar-empresa-de-seguridad-informatica",
            I18nUI.Configuracion.MSG_URL()
        , "assertEquals failed at I18nUITest.java:122");
    }

    @Test
    @DisplayName("Mensaje de comando configurado para binario respeta idioma")
    void testMensajeComandoConfiguradoI18n() {
        String comando = "claude --dangerously-skip-permissions";

        I18nUI.establecerIdioma("es");
        assertEquals("Comando configurado: " + comando,
            I18nUI.Configuracion.Agentes.MSG_COMANDO_CONFIGURADO(comando), "assertEquals failed at I18nUITest.java:134");

        I18nUI.establecerIdioma("en");
        assertEquals("Configured command: " + comando,
            I18nUI.Configuracion.Agentes.MSG_COMANDO_CONFIGURADO(comando), "assertEquals failed at I18nUITest.java:138");
    }

    @Test
    @DisplayName("Mensajes de resultados de envío/contexto respetan idioma")
    void testMensajesResultadosEnvioContextoI18n() {
        I18nUI.establecerIdioma("es");
        assertEquals(
            "Solicitudes enviadas para análisis forzado: 2/3 (omitidas: 1).",
            I18nUI.Contexto.MSG_ANALISIS_INICIADO_RESULTADO(2, 3, 1)
        , "assertEquals failed at I18nUITest.java:146");
        assertEquals(
            "Solicitudes enviadas a Claude Code: 1/2 (fallidas: 1).",
            I18nUI.Contexto.MSG_ENVIO_AGENTE_RESULTADO("Claude Code", 1, 2, 1)
        , "assertEquals failed at I18nUITest.java:150");

        I18nUI.establecerIdioma("en");
        assertEquals(
            "Requests sent for forced analysis: 2/3 (skipped: 1).",
            I18nUI.Contexto.MSG_ANALISIS_INICIADO_RESULTADO(2, 3, 1)
        , "assertEquals failed at I18nUITest.java:156");
        assertEquals(
            "Requests sent to Claude Code: 1/2 (failed: 1).",
            I18nUI.Contexto.MSG_ENVIO_AGENTE_RESULTADO("Claude Code", 1, 2, 1)
        , "assertEquals failed at I18nUITest.java:160");
    }

    @Test
    @DisplayName("Líneas de estado de alerta no usan emojis en español")
    void testLineasEstadoAlertaSinEmojiEs() {
        I18nUI.establecerIdioma("es");
        String url = "https://example.com/repeater";

        String ok = I18nUI.Hallazgos.LINEA_ESTADO_EXITO_ALERTA(url);
        String aviso = I18nUI.Hallazgos.LINEA_ESTADO_ADVERTENCIA_ALERTA(url + " " + I18nUI.Hallazgos.MSG_SIN_REQUEST());
        String error = I18nUI.Hallazgos.LINEA_ESTADO_ERROR_ALERTA(url + " (error: fallo)");

        assertEquals("[OK] " + url, ok, "assertEquals failed at I18nUITest.java:176");
        assertTrue(aviso.startsWith("[AVISO] "), "assertTrue failed at I18nUITest.java:177");
        assertTrue(error.startsWith("[ERROR] "), "assertTrue failed at I18nUITest.java:178");
        assertFalse(ok.contains("✅"), "assertFalse failed at I18nUITest.java:179");
        assertFalse(aviso.contains("⚠"), "assertFalse failed at I18nUITest.java:180");
        assertFalse(error.contains("❌"), "assertFalse failed at I18nUITest.java:181");
    }

    @Test
    @DisplayName("Líneas de estado de alerta no usan emojis en inglés")
    void testLineasEstadoAlertaSinEmojiEn() {
        I18nUI.establecerIdioma("en");
        String url = "https://example.com/intruder";

        String ok = I18nUI.Hallazgos.LINEA_ESTADO_EXITO_ALERTA(url);
        String aviso = I18nUI.Hallazgos.LINEA_ESTADO_ADVERTENCIA_ALERTA(url + " " + I18nUI.Hallazgos.MSG_SIN_REQUEST());
        String error = I18nUI.Hallazgos.LINEA_ESTADO_ERROR_ALERTA(url + " (error: failed)");

        assertEquals("[OK] " + url, ok, "assertEquals failed at I18nUITest.java:194");
        assertTrue(aviso.startsWith("[WARN] "), "assertTrue failed at I18nUITest.java:195");
        assertTrue(error.startsWith("[ERROR] "), "assertTrue failed at I18nUITest.java:196");
        assertFalse(ok.contains("✅"), "assertFalse failed at I18nUITest.java:197");
        assertFalse(aviso.contains("⚠"), "assertFalse failed at I18nUITest.java:198");
        assertFalse(error.contains("❌"), "assertFalse failed at I18nUITest.java:199");
    }

    @Test
    @DisplayName("Resumen de consola usa base unificada en ambos formatos")
    void testResumenConsolaUnificado() {
        I18nUI.establecerIdioma("es");
        assertEquals(
            "Total: 3 | Info: 1 | Verbose: 1 | Errores: 1",
            I18nUI.Consola.RESUMEN_LOGS(3, 1, 1, 1)
        , "assertEquals failed at I18nUITest.java:206");
        assertEquals(
            "📊 Total: 3 | Info: 1 | Verbose: 1 | Errores: 1",
            I18nUI.Consola.RESUMEN(3, 1, 1, 1)
        , "assertEquals failed at I18nUITest.java:210");

        I18nUI.establecerIdioma("en");
        assertEquals(
            "Total: 3 | Info: 1 | Verbose: 1 | Errors: 1",
            I18nUI.Consola.RESUMEN_LOGS(3, 1, 1, 1)
        , "assertEquals failed at I18nUITest.java:216");
        assertEquals(
            "📊 Total: 3 | Info: 1 | Verbose: 1 | Errors: 1",
            I18nUI.Consola.RESUMEN(3, 1, 1, 1)
        , "assertEquals failed at I18nUITest.java:220");
    }
}
