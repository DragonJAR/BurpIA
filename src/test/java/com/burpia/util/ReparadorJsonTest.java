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
            assertNull(ReparadorJson.repararJson(null), "assertNull failed at ReparadorJsonTest.java:18");
        }

        @Test
        @DisplayName("Retorna null para cadena vacía")
        void retornaNullParaCadenaVacia() {
            assertNull(ReparadorJson.repararJson(""), "assertNull failed at ReparadorJsonTest.java:24");
            assertNull(ReparadorJson.repararJson("   "), "assertNull failed at ReparadorJsonTest.java:25");
        }

        @Test
        @DisplayName("Retorna JSON valido sin modificar")
        void retornaJsonValidoSinModificar() {
            String json = "{\"hallazgos\": [{\"titulo\": \"XSS\"}]}";
            assertEquals(json, ReparadorJson.repararJson(json), "assertEquals failed at ReparadorJsonTest.java:32");
        }

        @Test
        @DisplayName("Repara JSON con bloques de codigo markdown")
        void reparaJsonConBloquesMarkdown() {
            String conMarkdown = "```json\n{\"hallazgos\": []}\n```";
            String resultado = ReparadorJson.repararJson(conMarkdown);
            assertNotNull(resultado, "assertNotNull failed at ReparadorJsonTest.java:40");
            assertTrue(ReparadorJson.esJsonValido(resultado), "assertTrue failed at ReparadorJsonTest.java:41");
        }

        @Test
        @DisplayName("Repara JSON con bloques de codigo markdown sin lenguaje especificado")
        void reparaJsonConBloquesMarkdownSinLenguaje() {
            String conMarkdown = "```\n{\"hallazgos\": []}\n```   ";
            String resultado = ReparadorJson.repararJson(conMarkdown);
            assertNotNull(resultado, "assertNotNull failed at ReparadorJsonTest.java:49");
            assertTrue(ReparadorJson.esJsonValido(resultado), "assertTrue failed at ReparadorJsonTest.java:50");
        }

        @Test
        @DisplayName("Repara JSON con texto alrededor")
        void reparaJsonConTextoAlrededor() {
            String conTexto = "Aqui va mi respuesta: {\"titulo\": \"SQL Injection\"} y nada mas.";
            String resultado = ReparadorJson.repararJson(conTexto);
            assertNotNull(resultado, "assertNotNull failed at ReparadorJsonTest.java:58");
            assertTrue(ReparadorJson.esJsonValido(resultado), "assertTrue failed at ReparadorJsonTest.java:59");
        }

        @Test
        @DisplayName("Repara JSON con comas finales")
        void reparaJsonConComasFinales() {
            String conComaFinal = "{\"a\": \"1\", \"b\": \"2\",}";
            String resultado = ReparadorJson.repararJson(conComaFinal);
            assertNotNull(resultado, "assertNotNull failed at ReparadorJsonTest.java:67");
            assertTrue(ReparadorJson.esJsonValido(resultado), "assertTrue failed at ReparadorJsonTest.java:68");
        }

        @Test
        @DisplayName("Retorna null para texto sin estructura JSON")
        void retornaNullParaTextoSinJson() {
            assertNull(ReparadorJson.repararJson("esto no es JSON en absoluto"), "assertNull failed at ReparadorJsonTest.java:74");
        }

        @Test
        @DisplayName("Repara JSON con array envuelto en markdown")
        void reparaJsonArrayConMarkdown() {
            String conMarkdown = "```\n[{\"titulo\": \"XSS\"}, {\"titulo\": \"CSRF\"}]\n```";
            String resultado = ReparadorJson.repararJson(conMarkdown);
            assertNotNull(resultado, "assertNotNull failed at ReparadorJsonTest.java:82");
            assertTrue(ReparadorJson.esJsonValido(resultado), "assertTrue failed at ReparadorJsonTest.java:83");
        }

        @Test
        @DisplayName("Repara JSON con contenido extra despues del cierre")
        void reparaJsonConContenidoExtra() {
            String conExtra = "{\"titulo\": \"test\"}\n\nEspero que sea util.";
            String resultado = ReparadorJson.repararJson(conExtra);
            assertNotNull(resultado, "assertNotNull failed at ReparadorJsonTest.java:91");
            assertTrue(ReparadorJson.esJsonValido(resultado), "assertTrue failed at ReparadorJsonTest.java:92");
        }

        @Test
        @DisplayName("Extrae pares clave-valor de texto semi-estructurado")
        void extraeParesClaveValor() {
            String semiJson = "\"titulo\": \"XSS\", \"severidad\": \"High\"";
            String resultado = ReparadorJson.repararJson(semiJson);
            assertNotNull(resultado, "assertNotNull failed at ReparadorJsonTest.java:100");
            assertTrue(ReparadorJson.esJsonValido(resultado), "assertTrue failed at ReparadorJsonTest.java:101");
        }

        @Test
        @DisplayName("Extrae pares clave-valor con booleanos y numeros")
        void extraeParesClaveValorConBooleanosYNumeros() {
            String semiJson = "\"activo\": true, \"contador\": 42";
            String resultado = ReparadorJson.repararJson(semiJson);
            assertNotNull(resultado, "assertNotNull failed at ReparadorJsonTest.java:109");
            assertTrue(ReparadorJson.esJsonValido(resultado), "assertTrue failed at ReparadorJsonTest.java:110");
        }
    }

    @Nested
    @DisplayName("esJsonValido")
    class EsJsonValido {
        @Test
        @DisplayName("Retorna false para null")
        void retornaFalseParaNull() {
            assertFalse(ReparadorJson.esJsonValido(null), "assertFalse failed at ReparadorJsonTest.java:120");
        }

        @Test
        @DisplayName("Retorna false para cadena vacía")
        void retornaFalseParaCadenaVacia() {
            assertFalse(ReparadorJson.esJsonValido(""), "assertFalse failed at ReparadorJsonTest.java:126");
            assertFalse(ReparadorJson.esJsonValido("   "), "assertFalse failed at ReparadorJsonTest.java:127");
        }

        @Test
        @DisplayName("Retorna false para texto plano")
        void retornaFalseParaTextoPlano() {
            assertFalse(ReparadorJson.esJsonValido("esto no es json"), "assertFalse failed at ReparadorJsonTest.java:133");
        }

        @Test
        @DisplayName("Retorna true para objeto JSON valido")
        void retornaTrueParaObjetoJson() {
            assertTrue(ReparadorJson.esJsonValido("{\"clave\": \"valor\"}"), "assertTrue failed at ReparadorJsonTest.java:139");
        }

        @Test
        @DisplayName("Retorna true para arreglo JSON valido")
        void retornaTrueParaArregloJson() {
            assertTrue(ReparadorJson.esJsonValido("[{\"a\": 1}, {\"b\": 2}]"), "assertTrue failed at ReparadorJsonTest.java:145");
        }

        @Test
        @DisplayName("Retorna false para JSON incompleto")
        void retornaFalseParaJsonIncompleto() {
            assertFalse(ReparadorJson.esJsonValido("{\"clave\": \"valor\""), "assertFalse failed at ReparadorJsonTest.java:151");
        }

        @Test
        @DisplayName("Retorna false para cadena JSON tipo string")
        void retornaFalseParaCadenaString() {
            assertFalse(ReparadorJson.esJsonValido("\"solo una cadena\""), "assertFalse failed at ReparadorJsonTest.java:157");
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
            assertNotNull(resultado, "assertNotNull failed at ReparadorJsonTest.java:169");
            assertTrue(ReparadorJson.esJsonValido(resultado), "assertTrue failed at ReparadorJsonTest.java:170");
            assertTrue(resultado.contains("XSS Reflejado"), "assertTrue failed at ReparadorJsonTest.java:171");
        }

        @Test
        @DisplayName("Repara respuesta con objeto JSON anidado complejo")
        void reparaJsonAnidadoComplejo() {
            String json = "{\"hallazgos\": [{\"titulo\": \"IDOR\", \"detalles\": {\"endpoint\": \"/api/users\", \"parametro\": \"id\"}}]}";
            String resultado = ReparadorJson.repararJson(json);
            assertNotNull(resultado, "assertNotNull failed at ReparadorJsonTest.java:179");
            assertTrue(ReparadorJson.esJsonValido(resultado), "assertTrue failed at ReparadorJsonTest.java:180");
        }

        @Test
        @DisplayName("Maneja JSON vacio correctamente")
        void manejaJsonVacio() {
            String json = "{\"hallazgos\": []}";
            assertEquals(json, ReparadorJson.repararJson(json), "assertEquals failed at ReparadorJsonTest.java:187");
        }

        @Test
        @DisplayName("Maneja multiples bloques de codigo markdown")
        void manejaMultiplesBloquesMarkdown() {
            String respuesta = "```json\n{\"a\": 1}\n```\n\nY tambien:\n```\n{\"b\": 2}\n```";
            String resultado = ReparadorJson.repararJson(respuesta);
            assertNotNull(resultado, "assertNotNull failed at ReparadorJsonTest.java:195");
            assertTrue(ReparadorJson.esJsonValido(resultado), "assertTrue failed at ReparadorJsonTest.java:196");
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
            assertNotNull(resultado, "assertNotNull failed at ReparadorJsonTest.java:208");
            assertTrue(ReparadorJson.esJsonValido(resultado), "assertTrue failed at ReparadorJsonTest.java:209");
        }

        @Test
        @DisplayName("Maneja caracteres Unicode en JSON")
        void manejaCaracteresUnicode() {
            String conUnicode = "{\"titulo\": \"Inyección SQL\", \"descripcion\": \"Caracteres: ñáéíóú 日本語\"}";
            String resultado = ReparadorJson.repararJson(conUnicode);
            assertNotNull(resultado, "assertNotNull failed at ReparadorJsonTest.java:217");
            assertTrue(ReparadorJson.esJsonValido(resultado), "assertTrue failed at ReparadorJsonTest.java:218");
        }

        @Test
        @DisplayName("Maneja caracteres especiales escapados en valores")
        void manejaCaracteresEspecialesEscapados() {
            String conEspeciales = "{\"descripcion\": \"Linea1\\nLinea2\\tTab\"}";
            String resultado = ReparadorJson.repararJson(conEspeciales);
            assertNotNull(resultado, "assertNotNull failed at ReparadorJsonTest.java:226");
            assertTrue(ReparadorJson.esJsonValido(resultado), "assertTrue failed at ReparadorJsonTest.java:227");
        }

        @Test
        @DisplayName("Maneja comillas anidadas en valores JSON")
        void manejaComillasAnidadas() {
            String conComillas = "{\"mensaje\": \"El usuario dijo \\\"hola\\\"\"}";
            String resultado = ReparadorJson.repararJson(conComillas);
            assertNotNull(resultado, "assertNotNull failed at ReparadorJsonTest.java:235");
            assertTrue(ReparadorJson.esJsonValido(resultado), "assertTrue failed at ReparadorJsonTest.java:236");
        }

        @Test
        @DisplayName("Maneja JSON profundamente anidado")
        void manejaJsonProfundamenteAnidado() {
            String anidado = "{\"a\": {\"b\": {\"c\": {\"d\": {\"e\": \"valor\"}}}}}";
            String resultado = ReparadorJson.repararJson(anidado);
            assertNotNull(resultado, "assertNotNull failed at ReparadorJsonTest.java:244");
            assertTrue(ReparadorJson.esJsonValido(resultado), "assertTrue failed at ReparadorJsonTest.java:245");
        }

        @Test
        @DisplayName("Maneja campo evidence en ingles con HTML")
        void manejaEvidenceEnInglesConHtml() {
            String conHtml = "{\"title\": \"XSS\", \"evidence\": \"<div class=\"test\">content</div>\"}";
            String resultado = ReparadorJson.repararJson(conHtml);
            assertNotNull(resultado, "assertNotNull failed at ReparadorJsonTest.java:253");
            assertTrue(ReparadorJson.esJsonValido(resultado), "assertTrue failed at ReparadorJsonTest.java:254");
        }

        @Test
        @DisplayName("Maneja JSON con saltos de linea literales en valores")
        void manejaSaltosDeLineaLiterales() {
            String conSaltos = "{\"titulo\": \"XSS\", \"descripcion\": \"Linea1\nLinea2\"}";
            String resultado = ReparadorJson.repararJson(conSaltos);
            assertNotNull(resultado, "assertNotNull failed at ReparadorJsonTest.java:262");
            assertTrue(ReparadorJson.esJsonValido(resultado), "assertTrue failed at ReparadorJsonTest.java:263");
        }
    }
}
