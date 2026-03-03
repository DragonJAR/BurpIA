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
            "No se pudo abrir el navegador.\nURL de la guía:\n" + url,
            I18nUI.Consola.MSG_URL_GUIA_AGENTE(url)
        );

        I18nUI.establecerIdioma("en");
        assertEquals(
            "Could not open browser.\nGuide URL:\n" + url,
            I18nUI.Consola.MSG_URL_GUIA_AGENTE(url)
        );
    }

    @Test
    @DisplayName("Mensaje de URL en acerca de usa bloque y preserva protocolo")
    void testMensajeUrlAcercaDe() {
        I18nUI.establecerIdioma("es");
        assertEquals(
            "URL:\nhttps://www.dragonjar.org/contactar-empresa-de-seguridad-informatica",
            I18nUI.Configuracion.MSG_URL()
        );
        assertEquals(
            "https://www.dragonjar.org/contactar-empresa-de-seguridad-informatica",
            I18nUI.Configuracion.URL_SITIO_WEB()
        );
        assertEquals(
            "URL:\nhttps://example.com/docs",
            I18nUI.Configuracion.MSG_URL("https://example.com/docs")
        );

        I18nUI.establecerIdioma("en");
        assertEquals(
            "URL:\nhttps://www.dragonjar.org/contactar-empresa-de-seguridad-informatica",
            I18nUI.Configuracion.MSG_URL()
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

    @Test
    @DisplayName("Mensajes de resultados de envío/contexto respetan idioma")
    void testMensajesResultadosEnvioContextoI18n() {
        I18nUI.establecerIdioma("es");
        assertEquals(
            "Solicitudes enviadas para análisis forzado: 2/3 (omitidas: 1).",
            I18nUI.Contexto.MSG_ANALISIS_INICIADO_RESULTADO(2, 3, 1)
        );
        assertEquals(
            "Solicitudes enviadas a Claude Code: 1/2 (fallidas: 1).",
            I18nUI.Contexto.MSG_ENVIO_AGENTE_RESULTADO("Claude Code", 1, 2, 1)
        );

        I18nUI.establecerIdioma("en");
        assertEquals(
            "Requests sent for forced analysis: 2/3 (skipped: 1).",
            I18nUI.Contexto.MSG_ANALISIS_INICIADO_RESULTADO(2, 3, 1)
        );
        assertEquals(
            "Requests sent to Claude Code: 1/2 (failed: 1).",
            I18nUI.Contexto.MSG_ENVIO_AGENTE_RESULTADO("Claude Code", 1, 2, 1)
        );

        I18nUI.establecerIdioma("es");
    }

    @Test
    @DisplayName("Líneas de estado de alerta no usan emojis en español")
    void testLineasEstadoAlertaSinEmojiEs() {
        I18nUI.establecerIdioma("es");
        String url = "https://example.com/repeater";

        String ok = I18nUI.Hallazgos.LINEA_ESTADO_EXITO_ALERTA(url);
        String aviso = I18nUI.Hallazgos.LINEA_ESTADO_ADVERTENCIA_ALERTA(url + " " + I18nUI.Hallazgos.MSG_SIN_REQUEST());
        String error = I18nUI.Hallazgos.LINEA_ESTADO_ERROR_ALERTA(url + " (error: fallo)");

        assertEquals("[OK] " + url, ok);
        assertTrue(aviso.startsWith("[AVISO] "));
        assertTrue(error.startsWith("[ERROR] "));
        assertFalse(ok.contains("✅"));
        assertFalse(aviso.contains("⚠"));
        assertFalse(error.contains("❌"));
    }

    @Test
    @DisplayName("Líneas de estado de alerta no usan emojis en inglés")
    void testLineasEstadoAlertaSinEmojiEn() {
        I18nUI.establecerIdioma("en");
        String url = "https://example.com/intruder";

        String ok = I18nUI.Hallazgos.LINEA_ESTADO_EXITO_ALERTA(url);
        String aviso = I18nUI.Hallazgos.LINEA_ESTADO_ADVERTENCIA_ALERTA(url + " " + I18nUI.Hallazgos.MSG_SIN_REQUEST());
        String error = I18nUI.Hallazgos.LINEA_ESTADO_ERROR_ALERTA(url + " (error: failed)");

        assertEquals("[OK] " + url, ok);
        assertTrue(aviso.startsWith("[WARN] "));
        assertTrue(error.startsWith("[ERROR] "));
        assertFalse(ok.contains("✅"));
        assertFalse(aviso.contains("⚠"));
        assertFalse(error.contains("❌"));
        I18nUI.establecerIdioma("es");
    }

    @Test
    @DisplayName("Resumen de consola usa base unificada en ambos formatos")
    void testResumenConsolaUnificado() {
        I18nUI.establecerIdioma("es");
        assertEquals(
            "Total: 3 | Info: 1 | Verbose: 1 | Errores: 1",
            I18nUI.Consola.RESUMEN_LOGS(3, 1, 1, 1)
        );
        assertEquals(
            "📊 Total: 3 | Info: 1 | Verbose: 1 | Errores: 1",
            I18nUI.Consola.RESUMEN(3, 1, 1, 1)
        );

        I18nUI.establecerIdioma("en");
        assertEquals(
            "Total: 3 | Info: 1 | Verbose: 1 | Errors: 1",
            I18nUI.Consola.RESUMEN_LOGS(3, 1, 1, 1)
        );
        assertEquals(
            "📊 Total: 3 | Info: 1 | Verbose: 1 | Errors: 1",
            I18nUI.Consola.RESUMEN(3, 1, 1, 1)
        );
    }
}
