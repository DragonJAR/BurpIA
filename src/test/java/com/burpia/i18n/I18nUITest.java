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
    @DisplayName("Usa espanol por defecto y cambia a ingles")
    void testIdiomaActual() {
        I18nUI.establecerIdioma("es");
        assertEquals("📋 TAREAS", I18nUI.Pestanias.TAREAS());

        I18nUI.establecerIdioma("en");
        assertEquals("📋 TASKS", I18nUI.Pestanias.TAREAS());
    }

    @Test
    @DisplayName("Idioma invalido cae en espanol")
    void testIdiomaInvalido() {
        I18nUI.establecerIdioma("fr");
        assertEquals(IdiomaUI.ES, I18nUI.obtenerIdioma());
    }

    @Test
    @DisplayName("Logs se mantienen en espanol cuando idioma es espanol")
    void testLogsEspanol() {
        I18nUI.establecerIdioma("es");
        String mensaje = "Analisis completado: https://target";
        assertEquals(mensaje, I18nLogs.tr(mensaje));
        assertEquals("Analisis completado: https://target",
            I18nLogs.tr("Analysis completed: https://target"));
        assertEquals("Configuracion guardada exitosamente en: /tmp/.burpia.json",
            I18nLogs.tr("Configuration saved successfully to: /tmp/.burpia.json"));
    }

    @Test
    @DisplayName("Logs se traducen a ingles cuando idioma es ingles")
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
        assertEquals("Configuration saved successfully to: /tmp/.burpia.json",
            I18nLogs.tr("Configuracion guardada exitosamente en: /tmp/.burpia.json"));
    }
}
