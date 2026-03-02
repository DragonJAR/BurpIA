package com.burpia.ui;

import com.burpia.i18n.I18nUI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("UIUtils Tests")
class UIUtilsTest {

    @Test
    @DisplayName("extraerTextoVisibleEnlace mantiene texto plano")
    void testExtraerTextoVisibleEnlaceTextoPlano() {
        assertEquals("Como instalar Factory Droid?", UIUtils.extraerTextoVisibleEnlace("Como instalar Factory Droid?"));
    }

    @Test
    @DisplayName("extraerTextoVisibleEnlace elimina anchor html")
    void testExtraerTextoVisibleEnlaceConAnchor() {
        String input = "<html><a href='https://example.com'>Como instalar Factory Droid?</a></html>";
        assertEquals("Como instalar Factory Droid?", UIUtils.extraerTextoVisibleEnlace(input));
    }

    @Test
    @DisplayName("extraerTextoVisibleEnlace elimina etiquetas html residuales")
    void testExtraerTextoVisibleEnlaceConEtiquetasHtml() {
        String input = "<b>Texto</b> <i>de enlace</i>";
        assertEquals("Texto de enlace", UIUtils.extraerTextoVisibleEnlace(input));
    }

    @Test
    @DisplayName("extraerTextoVisibleEnlace maneja nulos y vacíos")
    void testExtraerTextoVisibleEnlaceNuloOVacio() {
        assertEquals("", UIUtils.extraerTextoVisibleEnlace(null));
        assertEquals("", UIUtils.extraerTextoVisibleEnlace("   "));
    }

    @Test
    @DisplayName("debeMostrarAlertaConOptOut respeta preferencia")
    void testDebeMostrarAlertaConOptOut() {
        assertTrue(UIUtils.debeMostrarAlertaConOptOut(true));
        assertFalse(UIUtils.debeMostrarAlertaConOptOut(false));
    }

    @Test
    @DisplayName("texto de checkbox no volver a mostrar se localiza")
    void testTextoCheckboxOptOutI18n() {
        I18nUI.establecerIdioma("es");
        assertEquals("No volver a mostrar este mensaje", I18nUI.General.CHECK_NO_VOLVER_MOSTRAR_ALERTA());

        I18nUI.establecerIdioma("en");
        assertEquals("Do not show this message again", I18nUI.General.CHECK_NO_VOLVER_MOSTRAR_ALERTA());

        I18nUI.establecerIdioma("es");
    }

    @Test
    @DisplayName("mensaje de binario inexistente incluye comando completo cuando hay flags")
    void testConstruirMensajeBinarioInexistenteConFlags() {
        I18nUI.establecerIdioma("es");

        String mensaje = UIUtils.construirMensajeBinarioAgenteNoEncontrado(
            "Claude Code",
            "/opt/claude/bin/claude --dangerously-skip-permissions"
        );

        assertEquals(
            "El binario de Claude Code no existe en la ruta actual: /opt/claude/bin/claude\n"
                + "Comando configurado: /opt/claude/bin/claude --dangerously-skip-permissions",
            mensaje
        );
    }

    @Test
    @DisplayName("mensaje de binario inexistente en ingles sin flags no agrega linea extra")
    void testConstruirMensajeBinarioInexistenteSinFlags() {
        I18nUI.establecerIdioma("en");

        String mensaje = UIUtils.construirMensajeBinarioAgenteNoEncontrado(
            "Factory Droid",
            "/tmp/droid"
        );

        assertEquals(
            "The Factory Droid binary does not exist at the current path: /tmp/droid",
            mensaje
        );

        I18nUI.establecerIdioma("es");
    }
}
