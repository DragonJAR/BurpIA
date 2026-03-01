package com.burpia.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionBurpIATest {

    @Test
    void normalizaVersionConPrefijoYSaltos() {
        assertEquals(VersionBurpIA.VERSION_ACTUAL,
            VersionBurpIA.normalizarVersion(" v" + VersionBurpIA.VERSION_ACTUAL + " \n"));
        assertEquals("2.4.0", VersionBurpIA.normalizarVersion("\uFEFF2.4.0\r\n"));
    }

    @Test
    void comparaVersionesNormalizadas() {
        assertFalse(VersionBurpIA.sonVersionesDiferentes(
            VersionBurpIA.VERSION_ACTUAL,
            "v" + VersionBurpIA.VERSION_ACTUAL
        ));
        assertTrue(VersionBurpIA.sonVersionesDiferentes(VersionBurpIA.VERSION_ACTUAL, "9.9.9"));
        assertTrue(VersionBurpIA.sonVersionesDiferentes(VersionBurpIA.VERSION_ACTUAL, ""));
    }

    @Test
    void versionActualCentralizadaCoincideConFormatoEsperado() {
        assertTrue(VersionBurpIA.VERSION_ACTUAL.matches("\\d+\\.\\d+\\.\\d+"));
        assertEquals(VersionBurpIA.VERSION_ACTUAL, VersionBurpIA.obtenerVersionActual());
    }
}
