package com.burpia.util;

import com.burpia.i18n.I18nUI;
import com.burpia.i18n.IdiomaUI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("GestorConsolaGUI Tests")
class GestorConsolaGUITest {

    @Test
    @DisplayName("Genera resumen con contadores numéricos correctos")
    void testGenerarResumen() {
        I18nUI.establecerIdioma(IdiomaUI.ES);
        GestorConsolaGUI gestor = new GestorConsolaGUI();
        gestor.registrarInfo("info");
        gestor.registrarVerbose("verbose");
        gestor.registrarError("error");

        assertEquals("Total: 3 | Info: 1 | Verbose: 1 | Errores: 1", gestor.generarResumen());
    }

    @Test
    @DisplayName("Resumen se localiza a ingles cuando idioma UI es EN")
    void testGenerarResumenEnIngles() {
        I18nUI.establecerIdioma(IdiomaUI.EN);
        GestorConsolaGUI gestor = new GestorConsolaGUI();
        gestor.registrarInfo("info");
        gestor.registrarVerbose("verbose");
        gestor.registrarError("error");

        assertEquals("Total: 3 | Info: 1 | Verbose: 1 | Errors: 1", gestor.generarResumen());
        I18nUI.establecerIdioma(IdiomaUI.ES);
    }
}
