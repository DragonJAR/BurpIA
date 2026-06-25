package com.burpia.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para {@link VersionBurpIA}.
 * <p>
 * Verifica la gestión centralizada de versiones, incluyendo la versión
 * actual expuesta y las constantes de URL del proyecto.
 * </p>
 */
@DisplayName("VersionBurpIA Tests")
class VersionBurpIATest {

    @Nested
    @DisplayName("obtenerVersionActual")
    class ObtenerVersionActual {

        @Test
        @DisplayName("La versión actual coincide con formato semver")
        void versionActualCoincideConFormatoSemver() {
            assertTrue(VersionBurpIA.VERSION_ACTUAL.matches("\\d+\\.\\d+\\.\\d+"), "assertTrue failed at VersionBurpIATest.java:183");
        }

        @Test
        @DisplayName("La versión actual coincide con VERSION.txt")
        void versionActualCoincideConVersionTxt() throws IOException {
            String versionRelease = Files.readString(Path.of("VERSION.txt")).trim();

            assertEquals(versionRelease, VersionBurpIA.VERSION_ACTUAL,
                "VERSION.txt debe permanecer alineado con VersionBurpIA.VERSION_ACTUAL");
        }

        @Test
        @DisplayName("La versión actual no está vacía")
        void versionActualNoVacia() {
            assertFalse(VersionBurpIA.VERSION_ACTUAL.isEmpty(), "assertFalse failed at VersionBurpIATest.java:189");
            assertNotNull(VersionBurpIA.obtenerVersionActual(), "assertNotNull failed at VersionBurpIATest.java:190");
        }
    }

    @Nested
    @DisplayName("Constantes de URL")
    class ConstantesUrl {

        @Test
        @DisplayName("URL de versión remota está configurada")
        void urlVersionRemotaConfigurada() {
            assertNotNull(VersionBurpIA.URL_VERSION_REMOTA, "assertNotNull failed at VersionBurpIATest.java:201");
            assertFalse(VersionBurpIA.URL_VERSION_REMOTA.isEmpty(), "assertFalse failed at VersionBurpIATest.java:202");
            assertTrue(VersionBurpIA.URL_VERSION_REMOTA.startsWith("https://"), "assertTrue failed at VersionBurpIATest.java:203");
        }

        @Test
        @DisplayName("URL de descarga está configurada")
        void urlDescargaConfigurada() {
            assertNotNull(VersionBurpIA.URL_DESCARGA, "assertNotNull failed at VersionBurpIATest.java:209");
            assertFalse(VersionBurpIA.URL_DESCARGA.isEmpty(), "assertFalse failed at VersionBurpIATest.java:210");
            assertTrue(VersionBurpIA.URL_DESCARGA.startsWith("https://"), "assertTrue failed at VersionBurpIATest.java:211");
        }
    }
}
