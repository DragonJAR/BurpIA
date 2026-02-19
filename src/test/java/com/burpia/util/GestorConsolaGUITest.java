package com.burpia.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("GestorConsolaGUI Tests")
class GestorConsolaGUITest {

    @Test
    @DisplayName("Genera resumen con contadores numéricos correctos")
    void testGenerarResumen() {
        GestorConsolaGUI gestor = new GestorConsolaGUI();
        gestor.registrarInfo("info");
        gestor.registrarVerbose("verbose");
        gestor.registrarError("error");

        assertEquals("Total: 3 | Info: 1 | Verbose: 1 | Errores: 1", gestor.generarResumen());
    }
}
