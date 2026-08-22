package com.burpia.util;

import com.burpia.i18n.I18nUI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Normalizador Tests")
class NormalizadorTest {

    @Nested
    @DisplayName("normalizarTexto")
    class NormalizarTexto {
        @Test
        @DisplayName("Retorna cadena vacía para null")
        void retornaVacioParaNull() {
            assertEquals("", Normalizador.normalizarTexto(null), "assertEquals failed at NormalizadorTest.java:20");
        }

        @Test
        @DisplayName("Retorna cadena vacía para entrada vacía")
        void retornaVacioParaEntradaVacia() {
            assertEquals("", Normalizador.normalizarTexto(""), "assertEquals failed at NormalizadorTest.java:26");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Maneja null y vacío consistentemente")
        void manejaNullYVacio(String entrada) {
            assertEquals("", Normalizador.normalizarTexto(entrada), "assertEquals failed at NormalizadorTest.java:33");
        }

        @Test
        @DisplayName("Reemplaza secuencias de escape")
        void reemplazaSecuenciasEscape() {
            assertEquals("linea1\nlinea2", Normalizador.normalizarTexto("linea1\\nlinea2"), "assertEquals failed at NormalizadorTest.java:39");
            assertEquals("col1\tcol2", Normalizador.normalizarTexto("col1\\tcol2"), "assertEquals failed at NormalizadorTest.java:40");
            assertEquals("dice \"hola\"", Normalizador.normalizarTexto("dice \\\"hola\\\""), "assertEquals failed at NormalizadorTest.java:41");
        }

        @Test
        @DisplayName("Desescapa retorno de carro")
        void desescapaRetornoCarro() {
            assertEquals("linea1\rlinea2", Normalizador.normalizarTexto("linea1\\rlinea2"), "assertEquals failed at NormalizadorTest.java:47");
        }

        @Test
        @DisplayName("Desescapa barra invertida")
        void desescapaBarraInvertida() {
            assertEquals("ruta\\archivo", Normalizador.normalizarTexto("ruta\\\\archivo"), "assertEquals failed at NormalizadorTest.java:53");
            assertEquals("c:\\path\\to\\file", Normalizador.normalizarTexto("c:\\\\path\\\\to\\\\file"), "assertEquals failed at NormalizadorTest.java:54");
        }

        @Test
        @DisplayName("Maneja secuencias mixtas")
        void manejaSecuenciasMixtas() {
            String entrada = "Linea1\\nLinea2\\tcon\\\"comillas\\\" y \\\\barra";
            String esperado = "Linea1\nLinea2\tcon\"comillas\" y \\barra";
            assertEquals(esperado, Normalizador.normalizarTexto(entrada), "assertEquals failed at NormalizadorTest.java:62");
        }

        @Test
        @DisplayName("Recorta espacios en blanco")
        void recortaEspacios() {
            assertEquals("texto", Normalizador.normalizarTexto("  texto  "), "assertEquals failed at NormalizadorTest.java:68");
        }

        @Test
        @DisplayName("Mantiene secuencias no reconocidas")
        void mantieneSecuenciasNoReconocidas() {
            assertEquals("test\\xvalor", Normalizador.normalizarTexto("test\\xvalor"), "assertEquals failed at NormalizadorTest.java:74");
            assertEquals("test\\avalor", Normalizador.normalizarTexto("test\\avalor"), "assertEquals failed at NormalizadorTest.java:75");
        }
    }

    @Nested
    @DisplayName("sanitizarApiKey")
    class SanitizarApiKey {
        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Retorna [NO CONFIGURADA] para null o vacía")
        void retornaNoConfiguradaParaNullOVacia(String apiKey) {
            assertEquals(I18nUI.General.CLAVE_NO_CONFIGURADA(), Normalizador.sanitizarApiKey(apiKey), "assertEquals failed at NormalizadorTest.java:125");
        }

        @Test
        @DisplayName("Enmascara claves largas mostrando inicio y final")
        void enmascaraClavesLargas() {
            String resultado = Normalizador.sanitizarApiKey("sk-1234567890abcdef");
            assertEquals("sk-1****cdef", resultado, "assertEquals failed at NormalizadorTest.java:132");
        }

        @Test
        @DisplayName("Enmascara completamente claves cortas (menos de 8 caracteres)")
        void enmascaraClavesCortas() {
            assertEquals("****", Normalizador.sanitizarApiKey("corta"), "assertEquals failed at NormalizadorTest.java:138");
            assertEquals("****", Normalizador.sanitizarApiKey("1234567"), "assertEquals failed at NormalizadorTest.java:139");
        }

        @Test
        @DisplayName("Enmascara completamente claves de exactamente 8 caracteres")
        void enmascaraClavesExactamente8Caracteres() {
            assertEquals("****", Normalizador.sanitizarApiKey("12345678"), "assertEquals failed at NormalizadorTest.java:145");
        }

        @Test
        @DisplayName("Enmascara completamente claves de 9 a 12 caracteres (casi toda la clave quedaría expuesta)")
        void enmascaraClaves9a12Caracteres() {
            assertEquals("****", Normalizador.sanitizarApiKey("123456789"), "assertEquals failed at NormalizadorTest.java:118");
            assertEquals("****", Normalizador.sanitizarApiKey("123456789012"), "claves de 12 caracteres deben enmascararse por completo");
        }

        @Test
        @DisplayName("Muestra inicio y final a partir de 13 caracteres")
        void muestraInicioYFinalDesde13Caracteres() {
            assertEquals("1234****0abc", Normalizador.sanitizarApiKey("1234567890abc"), "la frontera del enmascaramiento es 12 caracteres");
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
                assertTrue(Normalizador.esVacio(null), "assertTrue failed at NormalizadorTest.java:164");
            }

            @Test
            @DisplayName("Retorna true para cadena vacía")
            void retornaTrueParaVacia() {
                assertTrue(Normalizador.esVacio(""), "assertTrue failed at NormalizadorTest.java:170");
            }

            @Test
            @DisplayName("Retorna true para solo espacios")
            void retornaTrueParaSoloEspacios() {
                assertTrue(Normalizador.esVacio("   "), "assertTrue failed at NormalizadorTest.java:176");
                assertTrue(Normalizador.esVacio("\t\n\r"), "assertTrue failed at NormalizadorTest.java:177");
            }

            @Test
            @DisplayName("Retorna false para texto con contenido")
            void retornaFalseParaContenido() {
                assertFalse(Normalizador.esVacio("texto"), "assertFalse failed at NormalizadorTest.java:183");
                assertFalse(Normalizador.esVacio("  texto  "), "assertFalse failed at NormalizadorTest.java:184");
            }
        }

        @Nested
        @DisplayName("noEsVacio")
        class NoEsVacio {
            @Test
            @DisplayName("Es inverso de esVacio para null")
            void esInversoParaNull() {
                assertFalse(Normalizador.noEsVacio(null), "assertFalse failed at NormalizadorTest.java:194");
            }

            @Test
            @DisplayName("Es inverso de esVacio para cadena vacía")
            void esInversoParaVacia() {
                assertFalse(Normalizador.noEsVacio(""), "assertFalse failed at NormalizadorTest.java:200");
            }

            @Test
            @DisplayName("Es inverso de esVacio para solo espacios")
            void esInversoParaSoloEspacios() {
                assertFalse(Normalizador.noEsVacio("   "), "assertFalse failed at NormalizadorTest.java:206");
            }

            @Test
            @DisplayName("Retorna true para texto con contenido")
            void retornaTrueParaContenido() {
                assertTrue(Normalizador.noEsVacio("texto"), "assertTrue failed at NormalizadorTest.java:212");
            }
        }
    }

    @Nested
    @DisplayName("Colecciones")
    class Colecciones {
        @Test
        @DisplayName("esVacia - retorna true para null")
        void retornaTrueParaNull() {
            assertTrue(Normalizador.esVacia((List<String>) null), "assertTrue failed at NormalizadorTest.java:225");
        }

        @Test
        @DisplayName("esVacia - retorna true para lista vacía")
        void retornaTrueParaVacia() {
            List<String> lista = new ArrayList<>();
            assertTrue(Normalizador.esVacia(lista), "assertTrue failed at NormalizadorTest.java:231");
        }

        @Test
        @DisplayName("esVacia - retorna false para lista con elementos")
        void retornaFalseParaConElementos() {
            List<String> lista = new ArrayList<>();
            lista.add("elemento");
            assertFalse(Normalizador.esVacia(lista), "assertFalse failed at NormalizadorTest.java:238");
        }

        @Test
        @DisplayName("noEsVacia - retorna false para null")
        void retornaFalseParaNull() {
            assertFalse(Normalizador.noEsVacia((List<String>) null), "assertFalse failed at NormalizadorTest.java:243");
        }

        @Test
        @DisplayName("noEsVacia - retorna false para lista vacía")
        void retornaFalseParaVacia() {
            List<String> lista = new ArrayList<>();
            assertFalse(Normalizador.noEsVacia(lista), "assertFalse failed at NormalizadorTest.java:249");
        }

        @Test
        @DisplayName("noEsVacia - retorna true para lista con elementos")
        void retornaTrueParaConElementos() {
            List<String> lista = new ArrayList<>();
            lista.add("elemento");
            assertTrue(Normalizador.noEsVacia(lista), "assertTrue failed at NormalizadorTest.java:256");
        }
    }

    @Nested
    @DisplayName("Mapas")
    class Mapas {
        @Test
        @DisplayName("esVacia - retorna true para null")
        void retornaTrueParaNull() {
            assertTrue(Normalizador.esVacia((Map<String, String>) null), "assertTrue failed at NormalizadorTest.java:265");
        }

        @Test
        @DisplayName("esVacia - retorna true para mapa vacío")
        void retornaTrueParaVacio() {
            Map<String, String> mapa = new HashMap<>();
            assertTrue(Normalizador.esVacia(mapa), "assertTrue failed at NormalizadorTest.java:271");
        }

        @Test
        @DisplayName("esVacia - retorna false para mapa con entradas")
        void retornaFalseParaConEntradas() {
            Map<String, String> mapa = new HashMap<>();
            mapa.put("clave", "valor");
            assertFalse(Normalizador.esVacia(mapa), "assertFalse failed at NormalizadorTest.java:278");
        }

        @Test
        @DisplayName("noEsVacia - retorna false para null")
        void retornaFalseParaNull() {
            assertFalse(Normalizador.noEsVacia((Map<String, String>) null), "assertFalse failed at NormalizadorTest.java:283");
        }

        @Test
        @DisplayName("noEsVacia - retorna false para mapa vacío")
        void retornaFalseParaVacio() {
            Map<String, String> mapa = new HashMap<>();
            assertFalse(Normalizador.noEsVacia(mapa), "assertFalse failed at NormalizadorTest.java:289");
        }

        @Test
        @DisplayName("noEsVacia - retorna true para mapa con entradas")
        void retornaTrueParaConEntradas() {
            Map<String, String> mapa = new HashMap<>();
            mapa.put("clave", "valor");
            assertTrue(Normalizador.noEsVacia(mapa), "assertTrue failed at NormalizadorTest.java:296");
        }
    }
}
