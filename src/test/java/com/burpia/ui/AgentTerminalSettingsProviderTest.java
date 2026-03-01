package com.burpia.ui;

import com.jediterm.terminal.TextStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AgentTerminalSettingsProvider Tests")
class AgentTerminalSettingsProviderTest {

    @Test
    @DisplayName("Retorna paleta y estilos no nulos")
    void testRetornaEstilosNoNulos() {
        AgentTerminalSettingsProvider provider = new AgentTerminalSettingsProvider();

        assertNotNull(provider.getTerminalColorPalette());
        assertNotNull(provider.getDefaultStyle());
        assertNotNull(provider.getSelectionColor());
        assertNotNull(provider.getFoundPatternColor());
        assertNotNull(provider.getHyperlinkColor());
    }

    @Test
    @DisplayName("Hipervínculo de terminal mantiene contraste AA con el fondo por defecto")
    void testHyperlinkConContrasteAA() {
        AgentTerminalSettingsProvider provider = new AgentTerminalSettingsProvider();

        TextStyle estiloDefault = provider.getDefaultStyle();
        TextStyle estiloLink = provider.getHyperlinkColor();

        assertNotNull(estiloDefault.getBackground());
        assertNotNull(estiloLink.getForeground());

        Color fondo = estiloDefault.getBackground().toAwtColor();
        Color enlace = estiloLink.getForeground().toAwtColor();

        assertNotNull(fondo);
        assertNotNull(enlace);
        assertTrue(EstilosUI.ratioContraste(enlace, fondo) >= EstilosUI.CONTRASTE_AA_NORMAL);
    }
}
