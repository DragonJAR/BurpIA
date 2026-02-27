package com.burpia.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    @DisplayName("extraerTextoVisibleEnlace maneja nulos y vacios")
    void testExtraerTextoVisibleEnlaceNuloOVacio() {
        assertEquals("", UIUtils.extraerTextoVisibleEnlace(null));
        assertEquals("", UIUtils.extraerTextoVisibleEnlace("   "));
    }
}
