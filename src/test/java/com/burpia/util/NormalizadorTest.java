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

        @Test
        @DisplayName("Usa valor por defecto cuando resultado esta vacio")
        void usaValorPorDefecto() {
            assertEquals("default", Normalizador.normalizarTexto(null, "default"));
            assertEquals("default", Normalizador.normalizarTexto("", "default"));
            assertEquals("default", Normalizador.normalizarTexto("   ", "default"));
        }
    }

    @Nested
    @DisplayName("truncarParaVisualizacion")
    class TruncarParaVisualizacion {
        @Test
        @DisplayName("Retorna cadena vacia para null")
        void retornaVacioParaNull() {
            assertEquals("", Normalizador.truncarParaVisualizacion(null, 50));
        }

        @Test
        @DisplayName("Retorna texto sin modificar si cabe")
        void retornaTextoSinModificar() {
            assertEquals("corto", Normalizador.truncarParaVisualizacion("corto", 50));
        }

        @Test
        @DisplayName("Trunca texto largo con elipsis")
        void truncaTextoLargoConElipsis() {
            String largo = "a".repeat(100);
            String resultado = Normalizador.truncarParaVisualizacion(largo, 50);
            assertTrue(resultado.endsWith("..."));
            assertTrue(resultado.length() <= 53);
        }

        @Test
        @DisplayName("Retorna vacio para longitud maxima 0")
        void retornaVacioParaLongitudCero() {
            assertEquals("", Normalizador.truncarParaVisualizacion("algo", 0));
        }
    }

    @Nested
    @DisplayName("truncarUrl")
    class TruncarUrl {
        @Test
        @DisplayName("Retorna cadena vacia para null")
        void retornaVacioParaNull() {
            assertEquals("", Normalizador.truncarUrl(null, 50));
        }

        @Test
        @DisplayName("Retorna URL sin modificar si cabe")
        void retornaUrlSinModificar() {
            String url = "https://example.com/api";
            assertEquals(url, Normalizador.truncarUrl(url, 50));
        }

        @Test
        @DisplayName("Trunca URL larga preservando dominio")
        void truncaUrlLargaPreservandoDominio() {
            String url = "https://example.com/api/v1/users/123/posts/456/comments?page=1&limit=100";
            String resultado = Normalizador.truncarUrl(url, 50);
            assertTrue(resultado.contains("example.com"));
            assertTrue(resultado.length() <= 53);
        }

        @Test
        @DisplayName("Usa longitud por defecto 50 cuando es 0")
        void usaLongitudPorDefecto() {
            String url = "https://example.com/" + "a".repeat(200);
            String resultado = Normalizador.truncarUrl(url, 0);
            assertNotNull(resultado);
        }
    }

    @Nested
    @DisplayName("sanitizarParaJson")
    class SanitizarParaJson {
        @Test
        @DisplayName("Retorna cadena vacia para null")
        void retornaVacioParaNull() {
            assertEquals("", Normalizador.sanitizarParaJson(null));
        }

        @Test
        @DisplayName("Escapa caracteres especiales para JSON")
        void escapaCaracteresEspeciales() {
            String resultado = Normalizador.sanitizarParaJson("linea1\nlinea2\ttab \"comillas\" \\barra");
            assertFalse(resultado.contains("\n"));
            assertFalse(resultado.contains("\t"));
            assertTrue(resultado.contains("\\n"));
            assertTrue(resultado.contains("\\t"));
            assertTrue(resultado.contains("\\\""));
        }
    }

    @Nested
    @DisplayName("sanitizarParaLog")
    class SanitizarParaLog {
        @Test
        @DisplayName("Retorna cadena vacia para null")
        void retornaVacioParaNull() {
            assertEquals("", Normalizador.sanitizarParaLog(null));
        }

        @Test
        @DisplayName("Preserva caracteres imprimibles y whitespace valido")
        void preservaCaracteresValidos() {
            assertEquals("texto normal\nnueva linea", Normalizador.sanitizarParaLog("texto normal\nnueva linea"));
        }

        @Test
        @DisplayName("Reemplaza caracteres de control con formato hex")
        void reemplazaCaracteresControl() {
            String resultado = Normalizador.sanitizarParaLog("abc\u0001def");
            assertTrue(resultado.contains("\\x01"));
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

        @Test
        @DisplayName("aMinusculas maneja null")
        void aMinusculasManejaNull() {
            assertEquals("", Normalizador.aMinusculas(null));
            assertEquals("texto", Normalizador.aMinusculas("TEXTO"));
        }

        @Test
        @DisplayName("aMayusculas maneja null")
        void aMayusculasManejaNull() {
            assertEquals("", Normalizador.aMayusculas(null));
            assertEquals("TEXTO", Normalizador.aMayusculas("texto"));
        }

        @Test
        @DisplayName("primeroNoVacio retorna primer valor no vacio")
        void primeroNoVacio() {
            assertEquals("", Normalizador.primeroNoVacio((String[]) null));
            assertEquals("b", Normalizador.primeroNoVacio(null, "", "  ", "b", "c"));
            assertEquals("a", Normalizador.primeroNoVacio("a", "b"));
        }

        @Test
        @DisplayName("valorOalternativo funciona correctamente")
        void valorOalternativo() {
            assertEquals("original", Normalizador.valorOalternativo("original", "alt"));
            assertEquals("alt", Normalizador.valorOalternativo(null, "alt"));
            assertEquals("alt", Normalizador.valorOalternativo("", "alt"));
        }
    }

    @Nested
    @DisplayName("normalizarRango")
    class NormalizarRango {
        @Test
        @DisplayName("Limita int dentro del rango")
        void limitaIntDentroDelRango() {
            assertEquals(5, Normalizador.normalizarRango(5, 1, 10));
            assertEquals(1, Normalizador.normalizarRango(-5, 1, 10));
            assertEquals(10, Normalizador.normalizarRango(99, 1, 10));
        }

        @Test
        @DisplayName("Limita long dentro del rango")
        void limitaLongDentroDelRango() {
            assertEquals(5L, Normalizador.normalizarRango(5L, 1L, 10L));
            assertEquals(1L, Normalizador.normalizarRango(-5L, 1L, 10L));
            assertEquals(10L, Normalizador.normalizarRango(99L, 1L, 10L));
        }
    }

    @Nested
    @DisplayName("truncar y truncarConIndicador")
    class Truncar {
        @Test
        @DisplayName("truncar retorna original si cabe")
        void truncarRetornaOriginalSiCabe() {
            assertEquals("corto", Normalizador.truncar("corto", 50));
        }

        @Test
        @DisplayName("truncar corta texto largo con elipsis")
        void truncarCortaTextoLargo() {
            String resultado = Normalizador.truncar("a".repeat(100), 10);
            assertEquals("aaaaaaaaaa...", resultado);
        }

        @Test
        @DisplayName("truncar retorna vacio para null")
        void truncarRetornaVacioParaNull() {
            assertEquals("", Normalizador.truncar(null, 10));
        }

        @Test
        @DisplayName("truncarConIndicador incluye etiqueta de truncamiento")
        void truncarConIndicadorIncluyeEtiqueta() {
            String resultado = Normalizador.truncarConIndicador("a".repeat(100), 10, "Prompt");
            assertTrue(resultado.contains("[TRUNCADO Prompt:"));
        }

        @Test
        @DisplayName("truncarConIndicador retorna original si cabe")
        void truncarConIndicadorRetornaOriginal() {
            assertEquals("corto", Normalizador.truncarConIndicador("corto", 50, "X"));
        }
    }

    @Nested
    @DisplayName("normalizarParaLog")
    class NormalizarParaLog {
        @Test
        @DisplayName("Usa valor por defecto para null")
        void usaValorPorDefectoParaNull() {
            assertEquals("[NULL]", Normalizador.normalizarParaLog(null, null));
            assertEquals("N/A", Normalizador.normalizarParaLog(null, "N/A"));
        }

        @Test
        @DisplayName("Usa valor por defecto para cadena vacia")
        void usaValorPorDefectoParaVacia() {
            assertEquals("[VACIO]", Normalizador.normalizarParaLog("", null));
            assertEquals("[VACIO]", Normalizador.normalizarParaLog("   ", null));
        }

        @Test
        @DisplayName("Retorna texto limpio si hay contenido")
        void retornaTextoLimpio() {
            assertEquals("valor", Normalizador.normalizarParaLog("  valor  ", "default"));
        }
    }
}
