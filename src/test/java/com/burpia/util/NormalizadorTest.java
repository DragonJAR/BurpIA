package com.burpia.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Normalizador Tests")
class NormalizadorTest {

    @Nested
    @DisplayName("normalizarTexto")
    class NormalizarTexto {
        @Test
        @DisplayName("Retorna cadena vacía para null")
        void retornaVacioParaNull() {
            assertEquals("", Normalizador.normalizarTexto(null));
        }

        @Test
        @DisplayName("Retorna cadena vacía para entrada vacía")
        void retornaVacioParaEntradaVacia() {
            assertEquals("", Normalizador.normalizarTexto(""));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Maneja null y vacío consistentemente")
        void manejaNullYVacio(String entrada) {
            assertEquals("", Normalizador.normalizarTexto(entrada));
        }

        @Test
        @DisplayName("Reemplaza secuencias de escape")
        void reemplazaSecuenciasEscape() {
            assertEquals("linea1\nlinea2", Normalizador.normalizarTexto("linea1\\nlinea2"));
            assertEquals("col1\tcol2", Normalizador.normalizarTexto("col1\\tcol2"));
            assertEquals("dice \"hola\"", Normalizador.normalizarTexto("dice \\\"hola\\\""));
        }

        @Test
        @DisplayName("Desescapa retorno de carro")
        void desescapaRetornoCarro() {
            assertEquals("linea1\rlinea2", Normalizador.normalizarTexto("linea1\\rlinea2"));
        }

        @Test
        @DisplayName("Desescapa barra invertida")
        void desescapaBarraInvertida() {
            assertEquals("ruta\\archivo", Normalizador.normalizarTexto("ruta\\\\archivo"));
            assertEquals("c:\\path\\to\\file", Normalizador.normalizarTexto("c:\\\\path\\\\to\\\\file"));
        }

        @Test
        @DisplayName("Maneja secuencias mixtas")
        void manejaSecuenciasMixtas() {
            String entrada = "Linea1\\nLinea2\\tcon\\\"comillas\\\" y \\\\barra";
            String esperado = "Linea1\nLinea2\tcon\"comillas\" y \\barra";
            assertEquals(esperado, Normalizador.normalizarTexto(entrada));
        }

        @Test
        @DisplayName("Recorta espacios en blanco")
        void recortaEspacios() {
            assertEquals("texto", Normalizador.normalizarTexto("  texto  "));
        }

        @Test
        @DisplayName("Mantiene secuencias no reconocidas")
        void mantieneSecuenciasNoReconocidas() {
            assertEquals("test\\xvalor", Normalizador.normalizarTexto("test\\xvalor"));
            assertEquals("test\\avalor", Normalizador.normalizarTexto("test\\avalor"));
        }
    }

    @Nested
    @DisplayName("normalizarTextoConControlesEnEspacio")
    class NormalizarTextoConControlesEnEspacio {
        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Maneja null y vacío consistentemente")
        void manejaNullYVacio(String entrada) {
            assertEquals("", Normalizador.normalizarTextoConControlesEnEspacio(entrada));
        }

        @Test
        @DisplayName("Convierte controles en espacios")
        void convierteControlesEnEspacios() {
            assertEquals("linea1 linea2", Normalizador.normalizarTextoConControlesEnEspacio("linea1\\nlinea2"));
            assertEquals("col1 col2", Normalizador.normalizarTextoConControlesEnEspacio("col1\\tcol2"));
            assertEquals("texto1 texto2", Normalizador.normalizarTextoConControlesEnEspacio("texto1\\rtexto2"));
        }

        @Test
        @DisplayName("Maneja secuencias mixtas con controles como espacio")
        void manejaSecuenciasMixtas() {
            String entrada = "Linea1\\nLinea2\\tcon\\\"comillas\\\" y \\\\barra";
            String esperado = "Linea1 Linea2 con\"comillas\" y \\barra";
            assertEquals(esperado, Normalizador.normalizarTextoConControlesEnEspacio(entrada));
        }

        @Test
        @DisplayName("Recorta espacios después de conversión")
        void recortaEspaciosDespuesConversion() {
            assertEquals("a b", Normalizador.normalizarTextoConControlesEnEspacio("  a\\nb  "));
        }

        @Test
        @DisplayName("Mantiene secuencias no reconocidas")
        void mantieneSecuenciasNoReconocidas() {
            assertEquals("test\\xvalor", Normalizador.normalizarTextoConControlesEnEspacio("test\\xvalor"));
        }
    }

    @Nested
    @DisplayName("sanitizarApiKey")
    class SanitizarApiKey {
        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Retorna [NO CONFIGURADA] para null o vacía")
        void retornaNoConfiguradaParaNullOVacia(String apiKey) {
            assertEquals("[NO CONFIGURADA]", Normalizador.sanitizarApiKey(apiKey));
        }

        @Test
        @DisplayName("Enmascara claves largas mostrando inicio y final")
        void enmascaraClavesLargas() {
            String resultado = Normalizador.sanitizarApiKey("sk-1234567890abcdef");
            assertEquals("sk-1****cdef", resultado);
        }

        @Test
        @DisplayName("Enmascara completamente claves cortas (menos de 8 caracteres)")
        void enmascaraClavesCortas() {
            assertEquals("****", Normalizador.sanitizarApiKey("corta"));
            assertEquals("****", Normalizador.sanitizarApiKey("1234567"));
        }

        @Test
        @DisplayName("Enmascara completamente claves de exactamente 8 caracteres")
        void enmascaraClavesExactamente8Caracteres() {
            assertEquals("****", Normalizador.sanitizarApiKey("12345678"));
        }

        @Test
        @DisplayName("Enmascara claves de 9 caracteres mostrando inicio y final")
        void enmascaraClaves9Caracteres() {
            assertEquals("1234****6789", Normalizador.sanitizarApiKey("123456789"));
        }
    }

    @Nested
    @DisplayName("Utilidades de texto")
    class UtilidadesTexto {
        @Nested
        @DisplayName("esVacio")
        class EsVacio {
            @Test
            @DisplayName("Retorna true para null")
            void retornaTrueParaNull() {
                assertTrue(Normalizador.esVacio(null));
            }

            @Test
            @DisplayName("Retorna true para cadena vacía")
            void retornaTrueParaVacia() {
                assertTrue(Normalizador.esVacio(""));
            }

            @Test
            @DisplayName("Retorna true para solo espacios")
            void retornaTrueParaSoloEspacios() {
                assertTrue(Normalizador.esVacio("   "));
                assertTrue(Normalizador.esVacio("\t\n\r"));
            }

            @Test
            @DisplayName("Retorna false para texto con contenido")
            void retornaFalseParaContenido() {
                assertFalse(Normalizador.esVacio("texto"));
                assertFalse(Normalizador.esVacio("  texto  "));
            }
        }

        @Nested
        @DisplayName("noEsVacio")
        class NoEsVacio {
            @Test
            @DisplayName("Es inverso de esVacio para null")
            void esInversoParaNull() {
                assertFalse(Normalizador.noEsVacio(null));
            }

            @Test
            @DisplayName("Es inverso de esVacio para cadena vacía")
            void esInversoParaVacia() {
                assertFalse(Normalizador.noEsVacio(""));
            }

            @Test
            @DisplayName("Es inverso de esVacio para solo espacios")
            void esInversoParaSoloEspacios() {
                assertFalse(Normalizador.noEsVacio("   "));
            }

            @Test
            @DisplayName("Retorna true para texto con contenido")
            void retornaTrueParaContenido() {
                assertTrue(Normalizador.noEsVacio("texto"));
            }
        }
    }
}
