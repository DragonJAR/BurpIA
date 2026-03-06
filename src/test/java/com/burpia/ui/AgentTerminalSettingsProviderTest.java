package com.burpia.ui;

import com.jediterm.terminal.TextStyle;
import com.jediterm.terminal.emulator.ColorPalette;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AgentTerminalSettingsProvider Tests")
class AgentTerminalSettingsProviderTest {

    @Test
    @DisplayName("Retorna paleta y estilos no nulos con colores validos")
    void testRetornaEstilosNoNulos() {
        AgentTerminalSettingsProvider provider = new AgentTerminalSettingsProvider();

        ColorPalette paleta = provider.getTerminalColorPalette();
        TextStyle estiloDefault = provider.getDefaultStyle();
        TextStyle estiloSeleccion = provider.getSelectionColor();
        TextStyle estiloBusqueda = provider.getFoundPatternColor();
        TextStyle estiloEnlace = provider.getHyperlinkColor();

        assertNotNull(paleta, "La paleta de colores no debe ser nula");
        assertNotNull(estiloDefault, "El estilo por defecto no debe ser nulo");
        assertNotNull(estiloSeleccion, "El estilo de seleccion no debe ser nulo");
        assertNotNull(estiloBusqueda, "El estilo de busqueda no debe ser nulo");
        assertNotNull(estiloEnlace, "El estilo de enlace no debe ser nulo");

        assertNotNull(estiloDefault.getForeground(), "Foreground por defecto no debe ser nulo");
        assertNotNull(estiloDefault.getBackground(), "Background por defecto no debe ser nulo");
        assertNotNull(estiloSeleccion.getForeground(), "Foreground de seleccion no debe ser nulo");
        assertNotNull(estiloSeleccion.getBackground(), "Background de seleccion no debe ser nulo");
        assertNotNull(estiloBusqueda.getForeground(), "Foreground de busqueda no debe ser nulo");
        assertNotNull(estiloBusqueda.getBackground(), "Background de busqueda no debe ser nulo");
        assertNotNull(estiloEnlace.getForeground(), "Foreground de enlace no debe ser nulo");
        assertNotNull(estiloEnlace.getBackground(), "Background de enlace no debe ser nulo");
    }

    @Test
    @DisplayName("Hipervinculo mantiene contraste AA con su fondo")
    void testHyperlinkConContrasteAA() {
        AgentTerminalSettingsProvider provider = new AgentTerminalSettingsProvider();

        TextStyle estiloEnlace = provider.getHyperlinkColor();

        assertNotNull(estiloEnlace.getForeground(), "Foreground de enlace no debe ser nulo");
        assertNotNull(estiloEnlace.getBackground(), "Background de enlace no debe ser nulo");

        Color fondo = estiloEnlace.getBackground().toAwtColor();
        Color enlace = estiloEnlace.getForeground().toAwtColor();

        assertNotNull(fondo, "Color de fondo convertido no debe ser nulo");
        assertNotNull(enlace, "Color de enlace convertido no debe ser nulo");
        assertTrue(
            EstilosUI.ratioContraste(enlace, fondo) >= EstilosUI.CONTRASTE_AA_NORMAL,
            () -> String.format("Contraste %.2f debe ser >= %.2f",
                EstilosUI.ratioContraste(enlace, fondo), EstilosUI.CONTRASTE_AA_NORMAL)
        );
    }

    @Test
    @DisplayName("Estilo por defecto mantiene contraste AA")
    void testEstiloDefectoConContrasteAA() {
        AgentTerminalSettingsProvider provider = new AgentTerminalSettingsProvider();

        TextStyle estiloDefault = provider.getDefaultStyle();

        assertNotNull(estiloDefault.getForeground(), "assertNotNull failed at AgentTerminalSettingsProviderTest.java:73");
        assertNotNull(estiloDefault.getBackground(), "assertNotNull failed at AgentTerminalSettingsProviderTest.java:74");

        Color fondo = estiloDefault.getBackground().toAwtColor();
        Color texto = estiloDefault.getForeground().toAwtColor();

        assertNotNull(fondo, "assertNotNull failed at AgentTerminalSettingsProviderTest.java:79");
        assertNotNull(texto, "assertNotNull failed at AgentTerminalSettingsProviderTest.java:80");
        assertTrue(
            EstilosUI.ratioContraste(texto, fondo) >= EstilosUI.CONTRASTE_AA_NORMAL,
            () -> String.format("Contraste texto/fondo %.2f debe ser >= %.2f",
                EstilosUI.ratioContraste(texto, fondo), EstilosUI.CONTRASTE_AA_NORMAL)
        );
    }

    @Test
    @DisplayName("Estilo de seleccion mantiene contraste AA")
    void testEstiloSeleccionConContrasteAA() {
        AgentTerminalSettingsProvider provider = new AgentTerminalSettingsProvider();

        TextStyle estiloSeleccion = provider.getSelectionColor();

        assertNotNull(estiloSeleccion.getForeground(), "assertNotNull failed at AgentTerminalSettingsProviderTest.java:95");
        assertNotNull(estiloSeleccion.getBackground(), "assertNotNull failed at AgentTerminalSettingsProviderTest.java:96");

        Color fondo = estiloSeleccion.getBackground().toAwtColor();
        Color texto = estiloSeleccion.getForeground().toAwtColor();

        assertNotNull(fondo, "assertNotNull failed at AgentTerminalSettingsProviderTest.java:101");
        assertNotNull(texto, "assertNotNull failed at AgentTerminalSettingsProviderTest.java:102");
        assertTrue(
            EstilosUI.ratioContraste(texto, fondo) >= EstilosUI.CONTRASTE_AA_NORMAL,
            () -> String.format("Contraste seleccion %.2f debe ser >= %.2f",
                EstilosUI.ratioContraste(texto, fondo), EstilosUI.CONTRASTE_AA_NORMAL)
        );
    }

    @Test
    @DisplayName("Estilo de busqueda mantiene contraste AA")
    void testEstiloBusquedaConContrasteAA() {
        AgentTerminalSettingsProvider provider = new AgentTerminalSettingsProvider();

        TextStyle estiloBusqueda = provider.getFoundPatternColor();

        assertNotNull(estiloBusqueda.getForeground(), "assertNotNull failed at AgentTerminalSettingsProviderTest.java:117");
        assertNotNull(estiloBusqueda.getBackground(), "assertNotNull failed at AgentTerminalSettingsProviderTest.java:118");

        Color fondo = estiloBusqueda.getBackground().toAwtColor();
        Color texto = estiloBusqueda.getForeground().toAwtColor();

        assertNotNull(fondo, "assertNotNull failed at AgentTerminalSettingsProviderTest.java:123");
        assertNotNull(texto, "assertNotNull failed at AgentTerminalSettingsProviderTest.java:124");
        assertTrue(
            EstilosUI.ratioContraste(texto, fondo) >= EstilosUI.CONTRASTE_AA_NORMAL,
            () -> String.format("Contraste busqueda %.2f debe ser >= %.2f",
                EstilosUI.ratioContraste(texto, fondo), EstilosUI.CONTRASTE_AA_NORMAL)
        );
    }

    @Test
    @DisplayName("Retorna tamano de fuente valido")
    void testTamanoFuenteValido() {
        AgentTerminalSettingsProvider provider = new AgentTerminalSettingsProvider();

        float tamano = provider.getTerminalFontSize();

        assertTrue(tamano > 0, "El tamano de fuente debe ser positivo");
        assertTrue(tamano >= 6, "El tamano de fuente debe ser al menos 6pt");
        assertTrue(tamano <= 72, "El tamano de fuente debe ser como maximo 72pt");
    }

    @Test
    @DisplayName("Configuraciones de terminal habilitadas por defecto")
    void testConfiguracionesPorDefecto() {
        AgentTerminalSettingsProvider provider = new AgentTerminalSettingsProvider();

        assertTrue(provider.useAntialiasing(), "Antialiasing debe estar habilitado");
        assertTrue(provider.copyOnSelect(), "Copy on select debe estar habilitado");
        assertTrue(provider.pasteOnMiddleMouseClick(), "Paste on middle mouse click debe estar habilitado");
    }

    @Test
    @DisplayName("Paleta de colores no es nula")
    void testPaletaColoresNoNula() {
        AgentTerminalSettingsProvider provider = new AgentTerminalSettingsProvider();

        ColorPalette paleta = provider.getTerminalColorPalette();
        assertNotNull(paleta, "La paleta de colores no debe ser nula");
    }
}
