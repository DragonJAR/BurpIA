package com.burpia.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para {@link VersionBurpIA}.
 * <p>
 * Verifica la gestión centralizada de versiones, incluyendo normalización
 * de cadenas de versión, comparación de versiones locales y remotas,
 * y detección de actualizaciones.
 * </p>
 */
@DisplayName("VersionBurpIA Tests")
class VersionBurpIATest {

    @Nested
    @DisplayName("normalizarVersion")
    class NormalizarVersion {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Retorna cadena vacía para null o vacío")
        void retornaVacioParaNullOVacio(String entrada) {
            assertEquals("", VersionBurpIA.normalizarVersion(entrada));
        }

        @Test
        @DisplayName("Retorna cadena vacía para solo espacios")
        void retornaVacioParaSoloEspacios() {
            assertEquals("", VersionBurpIA.normalizarVersion("   "));
            assertEquals("", VersionBurpIA.normalizarVersion("\t\n\r"));
        }

        @Test
        @DisplayName("Normaliza versión con prefijo 'v' minúscula")
        void normalizaVersionConPrefijoVMinuscula() {
            assertEquals("1.0.2", VersionBurpIA.normalizarVersion("v1.0.2"));
            assertEquals("2.4.0", VersionBurpIA.normalizarVersion("v2.4.0"));
        }

        @Test
        @DisplayName("Normaliza versión con prefijo 'V' mayúscula")
        void normalizaVersionConPrefijoVMayuscula() {
            assertEquals("1.0.2", VersionBurpIA.normalizarVersion("V1.0.2"));
            assertEquals("2.4.0", VersionBurpIA.normalizarVersion("V2.4.0"));
        }

        @Test
        @DisplayName("Normaliza versión con espacios y saltos de línea")
        void normalizaVersionConEspaciosYSaltos() {
            assertEquals(VersionBurpIA.VERSION_ACTUAL,
                VersionBurpIA.normalizarVersion(" v" + VersionBurpIA.VERSION_ACTUAL + " \n"));
            assertEquals("2.4.0", VersionBurpIA.normalizarVersion("  2.4.0  "));
        }

        @Test
        @DisplayName("Normaliza versión con BOM (Byte Order Mark)")
        void normalizaVersionConBOM() {
            assertEquals("2.4.0", VersionBurpIA.normalizarVersion("\uFEFF2.4.0"));
            assertEquals("2.4.0", VersionBurpIA.normalizarVersion("\uFEFF2.4.0\r\n"));
        }

        @Test
        @DisplayName("Toma solo la primera línea de versión multilinea")
        void tomaSoloPrimeraLinea() {
            assertEquals("1.0.2", VersionBurpIA.normalizarVersion("1.0.2\n2.0.0"));
            assertEquals("1.0.2", VersionBurpIA.normalizarVersion("1.0.2\r\n2.0.0"));
            assertEquals("1.0.2", VersionBurpIA.normalizarVersion("v1.0.2\nextra"));
        }

        @Test
        @DisplayName("Mantiene versión ya normalizada")
        void mantieneVersionNormalizada() {
            assertEquals("1.0.2", VersionBurpIA.normalizarVersion("1.0.2"));
            assertEquals("2.4.0", VersionBurpIA.normalizarVersion("2.4.0"));
        }

        @Test
        @DisplayName("Combina múltiples normalizaciones")
        void combinaMultiplesNormalizaciones() {
            // BOM + espacio + prefijo v + versión + espacio + salto
            assertEquals("1.0.2", VersionBurpIA.normalizarVersion("\uFEFF v1.0.2 \n"));
            // Espacio + prefijo V mayúscula + versión + salto + texto extra
            assertEquals("2.4.0", VersionBurpIA.normalizarVersion(" V2.4.0\r\nextra"));
        }
    }

    @Nested
    @DisplayName("sonVersionesDiferentes")
    class SonVersionesDiferentes {

        @Test
        @DisplayName("Retorna false para versiones idénticas")
        void retornaFalseParaVersionesIdenticas() {
            assertFalse(VersionBurpIA.sonVersionesDiferentes("1.0.2", "1.0.2"));
            assertFalse(VersionBurpIA.sonVersionesDiferentes(
                VersionBurpIA.VERSION_ACTUAL,
                VersionBurpIA.VERSION_ACTUAL
            ));
        }

        @Test
        @DisplayName("Retorna false para versiones con diferente formato pero iguales")
        void retornaFalseParaDiferenteFormatoPeroIguales() {
            assertFalse(VersionBurpIA.sonVersionesDiferentes(
                VersionBurpIA.VERSION_ACTUAL,
                "v" + VersionBurpIA.VERSION_ACTUAL
            ));
            assertFalse(VersionBurpIA.sonVersionesDiferentes("1.0.2", " V1.0.2 "));
            assertFalse(VersionBurpIA.sonVersionesDiferentes("1.0.2", "\uFEFFv1.0.2"));
        }

        @Test
        @DisplayName("Retorna false para comparación case-insensitive")
        void retornaFalseParaComparacionCaseInsensitive() {
            // Las versiones numéricas no tienen caso, pero el método usa equalsIgnoreCase
            assertFalse(VersionBurpIA.sonVersionesDiferentes("1.0.2", "1.0.2"));
        }

        @Test
        @DisplayName("Retorna true para versiones diferentes")
        void retornaTrueParaVersionesDiferentes() {
            assertTrue(VersionBurpIA.sonVersionesDiferentes(VersionBurpIA.VERSION_ACTUAL, "9.9.9"));
            assertTrue(VersionBurpIA.sonVersionesDiferentes("1.0.0", "2.0.0"));
            assertTrue(VersionBurpIA.sonVersionesDiferentes("1.0.2", "1.0.3"));
        }

        @Test
        @DisplayName("Retorna true cuando una versión está vacía")
        void retornaTrueCuandoUnaVersionVacia() {
            assertTrue(VersionBurpIA.sonVersionesDiferentes(VersionBurpIA.VERSION_ACTUAL, ""));
            assertTrue(VersionBurpIA.sonVersionesDiferentes("", VersionBurpIA.VERSION_ACTUAL));
        }

        @Test
        @DisplayName("Retorna false cuando ambas versiones están vacías")
        void retornaFalseCuandoAmbasVacias() {
            assertFalse(VersionBurpIA.sonVersionesDiferentes("", ""));
            assertFalse(VersionBurpIA.sonVersionesDiferentes("", "  "));
        }

        @Test
        @DisplayName("Maneja null en versión local")
        void manejaNullEnVersionLocal() {
            assertTrue(VersionBurpIA.sonVersionesDiferentes(null, "1.0.2"));
            assertFalse(VersionBurpIA.sonVersionesDiferentes(null, ""));
        }

        @Test
        @DisplayName("Maneja null en versión remota")
        void manejaNullEnVersionRemota() {
            assertTrue(VersionBurpIA.sonVersionesDiferentes("1.0.2", null));
            assertFalse(VersionBurpIA.sonVersionesDiferentes("", null));
        }

        @Test
        @DisplayName("Retorna false cuando ambas versiones son null")
        void retornaFalseCuandoAmbasNull() {
            assertFalse(VersionBurpIA.sonVersionesDiferentes(null, null));
        }
    }

    @Nested
    @DisplayName("obtenerVersionActual")
    class ObtenerVersionActual {

        @Test
        @DisplayName("Retorna la versión actual configurada")
        void retornaVersionActualConfigurada() {
            assertEquals(VersionBurpIA.VERSION_ACTUAL, VersionBurpIA.obtenerVersionActual());
        }

        @Test
        @DisplayName("La versión actual coincide con formato semver")
        void versionActualCoincideConFormatoSemver() {
            assertTrue(VersionBurpIA.VERSION_ACTUAL.matches("\\d+\\.\\d+\\.\\d+"));
        }

        @Test
        @DisplayName("La versión actual no está vacía")
        void versionActualNoVacia() {
            assertFalse(VersionBurpIA.VERSION_ACTUAL.isEmpty());
            assertNotNull(VersionBurpIA.obtenerVersionActual());
        }
    }

    @Nested
    @DisplayName("Constantes de URL")
    class ConstantesUrl {

        @Test
        @DisplayName("URL de versión remota está configurada")
        void urlVersionRemotaConfigurada() {
            assertNotNull(VersionBurpIA.URL_VERSION_REMOTA);
            assertFalse(VersionBurpIA.URL_VERSION_REMOTA.isEmpty());
            assertTrue(VersionBurpIA.URL_VERSION_REMOTA.startsWith("https://"));
        }

        @Test
        @DisplayName("URL de descarga está configurada")
        void urlDescargaConfigurada() {
            assertNotNull(VersionBurpIA.URL_DESCARGA);
            assertFalse(VersionBurpIA.URL_DESCARGA.isEmpty());
            assertTrue(VersionBurpIA.URL_DESCARGA.startsWith("https://"));
        }
    }
}
