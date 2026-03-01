package com.burpia.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Normalizador Tests")
class NormalizadorTest {

    @Nested
    @DisplayName("normalizarTexto")
    class NormalizarTexto {
        @Test
        @DisplayName("Retorna cadena vacia para null")
        void retornaVacioParaNull() {
            assertEquals("", Normalizador.normalizarTexto(null));
        }

        @Test
        @DisplayName("Reemplaza secuencias de escape")
        void reemplazaSecuenciasEscape() {
            assertEquals("linea1\nlinea2", Normalizador.normalizarTexto("linea1\\nlinea2"));
            assertEquals("col1\tcol2", Normalizador.normalizarTexto("col1\\tcol2"));
            assertEquals("dice \"hola\"", Normalizador.normalizarTexto("dice \\\"hola\\\""));
        }

        @Test
        @DisplayName("Recorta espacios en blanco")
        void recortaEspacios() {
            assertEquals("texto", Normalizador.normalizarTexto("  texto  "));
        }
    }

    @Nested
    @DisplayName("normalizarTextoConControlesEnEspacio")
    class NormalizarTextoConControlesEnEspacio {
        @Test
        @DisplayName("Convierte controles en espacios")
        void convierteControlesEnEspacios() {
            assertEquals("linea1 linea2", Normalizador.normalizarTextoConControlesEnEspacio("linea1\\nlinea2"));
            assertEquals("col1 col2", Normalizador.normalizarTextoConControlesEnEspacio("col1\\tcol2"));
        }

        @Test
        @DisplayName("Retorna cadena vacia para null")
        void retornaVacioParaNull() {
            assertEquals("", Normalizador.normalizarTextoConControlesEnEspacio(null));
        }
    }

    @Nested
    @DisplayName("sanitizarApiKey")
    class SanitizarApiKey {
        @Test
        @DisplayName("Retorna indicador para null o vacia")
        void retornaIndicadorParaNullOVacia() {
            assertEquals("[NO CONFIGURADA]", Normalizador.sanitizarApiKey(null));
            assertEquals("[NO CONFIGURADA]", Normalizador.sanitizarApiKey(""));
        }

        @Test
        @DisplayName("Enmascara claves largas mostrando inicio y final")
        void enmascaraClavesLargas() {
            String resultado = Normalizador.sanitizarApiKey("sk-1234567890abcdef");
            assertEquals("sk-1****cdef", resultado);
        }

        @Test
        @DisplayName("Enmascara completamente claves cortas")
        void enmascaraClavesCortas() {
            assertEquals("****", Normalizador.sanitizarApiKey("corta"));
        }
    }

    @Nested
    @DisplayName("Utilidades de texto")
    class UtilidadesTexto {
        @Test
        @DisplayName("esVacio verifica correctamente")
        void esVacio() {
            assertTrue(Normalizador.esVacio(null));
            assertTrue(Normalizador.esVacio(""));
            assertTrue(Normalizador.esVacio("   "));
            assertFalse(Normalizador.esVacio("texto"));
        }

        @Test
        @DisplayName("noEsVacio es inverso de esVacio")
        void noEsVacio() {
            assertFalse(Normalizador.noEsVacio(null));
            assertTrue(Normalizador.noEsVacio("texto"));
        }
    }
}
