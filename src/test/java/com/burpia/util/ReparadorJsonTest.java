package com.burpia.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ReparadorJson Tests")
class ReparadorJsonTest {

    @Nested
    @DisplayName("repararJson")
    class RepararJson {
        @Test
        @DisplayName("Retorna null para entrada null")
        void retornaNullParaNull() {
            assertNull(ReparadorJson.repararJson(null));
        }

        @Test
        @DisplayName("Retorna null para cadena vacía")
        void retornaNullParaCadenaVacia() {
            assertNull(ReparadorJson.repararJson(""));
            assertNull(ReparadorJson.repararJson("   "));
        }

        @Test
        @DisplayName("Retorna JSON valido sin modificar")
        void retornaJsonValidoSinModificar() {
            String json = "{\"hallazgos\": [{\"titulo\": \"XSS\"}]}";
            assertEquals(json, ReparadorJson.repararJson(json));
        }

        @Test
        @DisplayName("Repara JSON con bloques de codigo markdown")
        void reparaJsonConBloquesMarkdown() {
            String conMarkdown = "```json\n{\"hallazgos\": []}\n```";
            String resultado = ReparadorJson.repararJson(conMarkdown);
            assertNotNull(resultado);
            assertTrue(ReparadorJson.esJsonValido(resultado));
        }

        @Test
        @DisplayName("Repara JSON con bloques de codigo markdown sin lenguaje especificado")
        void reparaJsonConBloquesMarkdownSinLenguaje() {
            String conMarkdown = "```\n{\"hallazgos\": []}\n```   ";
            String resultado = ReparadorJson.repararJson(conMarkdown);
            assertNotNull(resultado);
            assertTrue(ReparadorJson.esJsonValido(resultado));
        }

        @Test
        @DisplayName("Repara JSON con texto alrededor")
        void reparaJsonConTextoAlrededor() {
            String conTexto = "Aqui va mi respuesta: {\"titulo\": \"SQL Injection\"} y nada mas.";
            String resultado = ReparadorJson.repararJson(conTexto);
            assertNotNull(resultado);
            assertTrue(ReparadorJson.esJsonValido(resultado));
        }

        @Test
        @DisplayName("Repara JSON con comas finales")
        void reparaJsonConComasFinales() {
            String conComaFinal = "{\"a\": \"1\", \"b\": \"2\",}";
            String resultado = ReparadorJson.repararJson(conComaFinal);
            assertNotNull(resultado);
            assertTrue(ReparadorJson.esJsonValido(resultado));
        }

        @Test
        @DisplayName("Retorna null para texto sin estructura JSON")
        void retornaNullParaTextoSinJson() {
            assertNull(ReparadorJson.repararJson("esto no es JSON en absoluto"));
        }

        @Test
        @DisplayName("Repara JSON con array envuelto en markdown")
        void reparaJsonArrayConMarkdown() {
            String conMarkdown = "```\n[{\"titulo\": \"XSS\"}, {\"titulo\": \"CSRF\"}]\n```";
            String resultado = ReparadorJson.repararJson(conMarkdown);
            assertNotNull(resultado);
            assertTrue(ReparadorJson.esJsonValido(resultado));
        }

        @Test
        @DisplayName("Repara JSON con contenido extra despues del cierre")
        void reparaJsonConContenidoExtra() {
            String conExtra = "{\"titulo\": \"test\"}\n\nEspero que sea util.";
            String resultado = ReparadorJson.repararJson(conExtra);
            assertNotNull(resultado);
            assertTrue(ReparadorJson.esJsonValido(resultado));
        }

        @Test
        @DisplayName("Extrae pares clave-valor de texto semi-estructurado")
        void extraeParesClaveValor() {
            String semiJson = "\"titulo\": \"XSS\", \"severidad\": \"High\"";
            String resultado = ReparadorJson.repararJson(semiJson);
            assertNotNull(resultado);
            assertTrue(ReparadorJson.esJsonValido(resultado));
        }

        @Test
        @DisplayName("Extrae pares clave-valor con booleanos y numeros")
        void extraeParesClaveValorConBooleanosYNumeros() {
            String semiJson = "\"activo\": true, \"contador\": 42";
            String resultado = ReparadorJson.repararJson(semiJson);
            assertNotNull(resultado);
            assertTrue(ReparadorJson.esJsonValido(resultado));
        }
    }

    @Nested
    @DisplayName("esJsonValido")
    class EsJsonValido {
        @Test
        @DisplayName("Retorna false para null")
        void retornaFalseParaNull() {
            assertFalse(ReparadorJson.esJsonValido(null));
        }

        @Test
        @DisplayName("Retorna false para cadena vacía")
        void retornaFalseParaCadenaVacia() {
            assertFalse(ReparadorJson.esJsonValido(""));
            assertFalse(ReparadorJson.esJsonValido("   "));
        }

        @Test
        @DisplayName("Retorna false para texto plano")
        void retornaFalseParaTextoPlano() {
            assertFalse(ReparadorJson.esJsonValido("esto no es json"));
        }

        @Test
        @DisplayName("Retorna true para objeto JSON valido")
        void retornaTrueParaObjetoJson() {
            assertTrue(ReparadorJson.esJsonValido("{\"clave\": \"valor\"}"));
        }

        @Test
        @DisplayName("Retorna true para arreglo JSON valido")
        void retornaTrueParaArregloJson() {
            assertTrue(ReparadorJson.esJsonValido("[{\"a\": 1}, {\"b\": 2}]"));
        }

        @Test
        @DisplayName("Retorna false para JSON incompleto")
        void retornaFalseParaJsonIncompleto() {
            assertFalse(ReparadorJson.esJsonValido("{\"clave\": \"valor\""));
        }

        @Test
        @DisplayName("Retorna false para cadena JSON tipo string")
        void retornaFalseParaCadenaString() {
            assertFalse(ReparadorJson.esJsonValido("\"solo una cadena\""));
        }
    }

    @Nested
    @DisplayName("Escenarios reales de respuesta AI")
    class EscenariosReales {
        @Test
        @DisplayName("Repara respuesta tipica de ChatGPT con markdown")
        void reparaRespuestaChatGpt() {
            String respuesta = "Aquí están los hallazgos:\n\n```json\n{\"hallazgos\": [{\"titulo\": \"XSS Reflejado\", \"severidad\": \"High\", \"descripcion\": \"Se detectó XSS\"}]}\n```\n\n¿Necesitas más detalles?";
            String resultado = ReparadorJson.repararJson(respuesta);
            assertNotNull(resultado);
            assertTrue(ReparadorJson.esJsonValido(resultado));
            assertTrue(resultado.contains("XSS Reflejado"));
        }

        @Test
        @DisplayName("Repara respuesta con objeto JSON anidado complejo")
        void reparaJsonAnidadoComplejo() {
            String json = "{\"hallazgos\": [{\"titulo\": \"IDOR\", \"detalles\": {\"endpoint\": \"/api/users\", \"parametro\": \"id\"}}]}";
            String resultado = ReparadorJson.repararJson(json);
            assertNotNull(resultado);
            assertTrue(ReparadorJson.esJsonValido(resultado));
        }

        @Test
        @DisplayName("Maneja JSON vacio correctamente")
        void manejaJsonVacio() {
            String json = "{\"hallazgos\": []}";
            assertEquals(json, ReparadorJson.repararJson(json));
        }

        @Test
        @DisplayName("Maneja multiples bloques de codigo markdown")
        void manejaMultiplesBloquesMarkdown() {
            String respuesta = "```json\n{\"a\": 1}\n```\n\nY tambien:\n```\n{\"b\": 2}\n```";
            String resultado = ReparadorJson.repararJson(respuesta);
            assertNotNull(resultado);
            assertTrue(ReparadorJson.esJsonValido(resultado));
        }
    }

    @Nested
    @DisplayName("Casos especiales y edge cases")
    class CasosEspeciales {
        @Test
        @DisplayName("Maneja HTML en campo evidencia escapando comillas")
        void manejaHtmlEnEvidencia() {
            String conHtml = "{\"titulo\": \"XSS\", \"evidencia\": \"<a href=\"test\">link</a>\"}";
            String resultado = ReparadorJson.repararJson(conHtml);
            assertNotNull(resultado);
            assertTrue(ReparadorJson.esJsonValido(resultado));
        }

        @Test
        @DisplayName("Maneja caracteres Unicode en JSON")
        void manejaCaracteresUnicode() {
            String conUnicode = "{\"titulo\": \"Inyección SQL\", \"descripcion\": \"Caracteres: ñáéíóú 日本語\"}";
            String resultado = ReparadorJson.repararJson(conUnicode);
            assertNotNull(resultado);
            assertTrue(ReparadorJson.esJsonValido(resultado));
        }

        @Test
        @DisplayName("Maneja caracteres especiales escapados en valores")
        void manejaCaracteresEspecialesEscapados() {
            String conEspeciales = "{\"descripcion\": \"Linea1\\nLinea2\\tTab\"}";
            String resultado = ReparadorJson.repararJson(conEspeciales);
            assertNotNull(resultado);
            assertTrue(ReparadorJson.esJsonValido(resultado));
        }

        @Test
        @DisplayName("Maneja comillas anidadas en valores JSON")
        void manejaComillasAnidadas() {
            String conComillas = "{\"mensaje\": \"El usuario dijo \\\"hola\\\"\"}";
            String resultado = ReparadorJson.repararJson(conComillas);
            assertNotNull(resultado);
            assertTrue(ReparadorJson.esJsonValido(resultado));
        }

        @Test
        @DisplayName("Maneja JSON profundamente anidado")
        void manejaJsonProfundamenteAnidado() {
            String anidado = "{\"a\": {\"b\": {\"c\": {\"d\": {\"e\": \"valor\"}}}}}";
            String resultado = ReparadorJson.repararJson(anidado);
            assertNotNull(resultado);
            assertTrue(ReparadorJson.esJsonValido(resultado));
        }

        @Test
        @DisplayName("Maneja campo evidence en ingles con HTML")
        void manejaEvidenceEnInglesConHtml() {
            String conHtml = "{\"title\": \"XSS\", \"evidence\": \"<div class=\"test\">content</div>\"}";
            String resultado = ReparadorJson.repararJson(conHtml);
            assertNotNull(resultado);
            assertTrue(ReparadorJson.esJsonValido(resultado));
        }

        @Test
        @DisplayName("Maneja JSON con saltos de linea literales en valores")
        void manejaSaltosDeLineaLiterales() {
            String conSaltos = "{\"titulo\": \"XSS\", \"descripcion\": \"Linea1\nLinea2\"}";
            String resultado = ReparadorJson.repararJson(conSaltos);
            assertNotNull(resultado);
            assertTrue(ReparadorJson.esJsonValido(resultado));
        }
    }
}
