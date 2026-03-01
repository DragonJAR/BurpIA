package com.burpia.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EstilosUI Tests")
class EstilosUITest {

    @Test
    @DisplayName("esTemaOscuro clasifica correctamente fondos claros y oscuros")
    void testEsTemaOscuroPorLuminancia() {
        assertTrue(EstilosUI.esTemaOscuro(new Color(22, 24, 28)));
        assertFalse(EstilosUI.esTemaOscuro(new Color(242, 243, 245)));
    }

    @Test
    @DisplayName("ajustarParaContrasteMinimo garantiza ratio AA")
    void testAjusteContrasteAA() {
        Color fondo = new Color(240, 240, 240);
        Color base = new Color(180, 180, 180);

        Color ajustado = EstilosUI.ajustarParaContrasteMinimo(base, fondo, EstilosUI.CONTRASTE_AA_NORMAL);

        assertTrue(EstilosUI.ratioContraste(ajustado, fondo) >= EstilosUI.CONTRASTE_AA_NORMAL);
    }

    @Test
    @DisplayName("Colores de enlace y error cumplen AA en tema claro y oscuro")
    void testColoresAccesiblesAA() {
        Color fondoClaro = new Color(248, 248, 248);
        Color fondoOscuro = new Color(34, 36, 41);

        Color enlaceClaro = EstilosUI.colorEnlaceAccesible(fondoClaro);
        Color enlaceOscuro = EstilosUI.colorEnlaceAccesible(fondoOscuro);
        Color errorClaro = EstilosUI.colorErrorAccesible(fondoClaro);
        Color errorOscuro = EstilosUI.colorErrorAccesible(fondoOscuro);

        assertTrue(EstilosUI.ratioContraste(enlaceClaro, fondoClaro) >= EstilosUI.CONTRASTE_AA_NORMAL);
        assertTrue(EstilosUI.ratioContraste(enlaceOscuro, fondoOscuro) >= EstilosUI.CONTRASTE_AA_NORMAL);
        assertTrue(EstilosUI.ratioContraste(errorClaro, fondoClaro) >= EstilosUI.CONTRASTE_AA_NORMAL);
        assertTrue(EstilosUI.ratioContraste(errorOscuro, fondoOscuro) >= EstilosUI.CONTRASTE_AA_NORMAL);
    }
}
