package com.burpia.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests de ExtractorCamposRobusto")
class ExtractorCamposRobustoTest {

    @Nested
    @DisplayName("extraerBloquesPorCampo")
    class ExtraerBloquesPorCampo {

        @Test
        @DisplayName("Separa correctamente bloques por título")
        void separaBloquesPorTitulo() {
            String jsonRoto = "{\"titulo\":\"A\", \"severidad\":\"High\", " +
                             "\"titulo\":\"B\", \"severidad\":\"Medium\"}";

            List<String> bloques = ExtractorCamposRobusto.extraerBloquesPorCampo(
                jsonRoto,
                ExtractorCamposRobusto.CamposHallazgo.TITULO
            );

            assertEquals(2, bloques.size(), "Debe extraer 2 bloques");
            assertTrue(bloques.get(0).contains("A"), "Primer bloque debe contener 'A'");
            assertTrue(bloques.get(1).contains("B"), "Segundo bloque debe contener 'B'");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Retorna lista vacía si contenido es null o vacío")
        void retornaListaVaciaSiContenidoVacio(String contenido) {
            List<String> bloques = ExtractorCamposRobusto.extraerBloquesPorCampo(
                contenido,
                ExtractorCamposRobusto.CamposHallazgo.TITULO
            );

            assertTrue(bloques.isEmpty(), "Debe retornar lista vacía");
        }

        @Test
        @DisplayName("Retorna lista vacía si campo delimitador es null")
        void retornaListaVaciaSiCampoNull() {
            String contenido = "{\"titulo\":\"Test\", \"severidad\":\"High\"}";

            List<String> bloques = ExtractorCamposRobusto.extraerBloquesPorCampo(
                contenido,
                null
            );

            assertTrue(bloques.isEmpty(), "Debe retornar lista vacía si campo es null");
        }

        @Test
        @DisplayName("Retorna lista vacía si no hay campo delimitador")
        void retornaListaVaciaSiNoHayDelimitador() {
            String sinTitulo = "{\"severidad\":\"High\", \"confianza\":\"Medium\"}";

            List<String> bloques = ExtractorCamposRobusto.extraerBloquesPorCampo(
                sinTitulo,
                ExtractorCamposRobusto.CamposHallazgo.TITULO
            );

            assertTrue(bloques.isEmpty(), "Debe retornar lista vacía si no hay delimitador");
        }

        @Test
        @DisplayName("Maneja contenido con múltiples líneas")
        void manejaContenidoMultilinea() {
            String multilinea = "\"titulo\":\"Test\",\n" +
                               "\"descripcion\":\"Linea1\\nLinea2\",\n" +
                               "\"titulo\":\"Test2\"";

            List<String> bloques = ExtractorCamposRobusto.extraerBloquesPorCampo(
                multilinea,
                ExtractorCamposRobusto.CamposHallazgo.TITULO
            );

            assertEquals(2, bloques.size(), "Debe extraer 2 bloques con contenido multilínea");
        }

        @Test
        @DisplayName("Encuentra bloques usando variaciones i18n del campo")
        void encuentraBloquesConVariacionesI18n() {
            // Usando "title" en lugar de "titulo"
            String contenido = "\"title\":\"A\", \"severity\":\"High\", " +
                              "\"title\":\"B\", \"severity\":\"Medium\"";

            List<String> bloques = ExtractorCamposRobusto.extraerBloquesPorCampo(
                contenido,
                ExtractorCamposRobusto.CamposHallazgo.TITULO
            );

            assertEquals(2, bloques.size(), "Debe encontrar bloques con variación 'title'");
        }
    }

    @Nested
    @DisplayName("extraerCampoDeBloque")
    class ExtraerCampoDeBloque {

        @Test
        @DisplayName("Toma último valor cuando hay duplicados")
        void tomaUltimoValorConDuplicados() {
            String bloque = "\"titulo\":\"Test\", " +
                           "\"descripcion\":\"Vieja\", " +
                           "\"descripcion\":\"Nueva\"";

            String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                ExtractorCamposRobusto.CamposHallazgo.DESCRIPCION,
                bloque
            );

            assertEquals("Nueva", valor, "Debe tomar el último valor");
        }

        @Test
        @DisplayName("Maneja comillas sin cerrar")
        void manejaComillasSinCerrar() {
            String bloque = "\"titulo\":\"SQL\", " +
                           "\"severidad\":\"High\", " +
                           "\"descripcion\":\"Texto sin cerrar";

            String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                ExtractorCamposRobusto.CamposHallazgo.DESCRIPCION,
                bloque
            );

            assertEquals("Texto sin cerrar", valor, "Debe extraer valor aunque falte comilla de cierre");
        }

        @Test
        @DisplayName("Extrae título correctamente")
        void extraeTituloCorrectamente() {
            String bloque = "\"titulo\":\"XSS en input\", " +
                           "\"severidad\":\"Medium\"";

            String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                ExtractorCamposRobusto.CamposHallazgo.TITULO,
                bloque
            );

            assertEquals("XSS en input", valor, "Debe extraer título");
        }

        @Test
        @DisplayName("Extrae severidad correctamente")
        void extraeSeveridadCorrectamente() {
            String bloque = "\"titulo\":\"Test\", " +
                           "\"severidad\":\"Critical\"";

            String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                ExtractorCamposRobusto.CamposHallazgo.SEVERIDAD,
                bloque
            );

            assertEquals("Critical", valor, "Debe extraer severidad");
        }

        @Test
        @DisplayName("Extrae confianza correctamente")
        void extraeConfianzaCorrectamente() {
            String bloque = "\"titulo\":\"Test\", " +
                           "\"confianza\":\"High\"";

            String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                ExtractorCamposRobusto.CamposHallazgo.CONFIANZA,
                bloque
            );

            assertEquals("High", valor, "Debe extraer confianza");
        }

        @Test
        @DisplayName("Extrae evidencia correctamente")
        void extraeEvidenciaCorrectamente() {
            String bloque = "\"titulo\":\"Test\", " +
                           "\"evidencia\":\"POST /api/users\"";

            String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                ExtractorCamposRobusto.CamposHallazgo.EVIDENCIA,
                bloque
            );

            assertEquals("POST /api/users", valor, "Debe extraer evidencia");
        }

        @Test
        @DisplayName("Retorna vacío si bloque es null")
        void retornaVacioSiBloqueNull() {
            String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                ExtractorCamposRobusto.CamposHallazgo.TITULO,
                null
            );

            assertTrue(Normalizador.esVacio(valor), "Debe retornar vacío si bloque es null");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Retorna vacío si bloque es vacío o solo espacios")
        void retornaVacioSiBloqueVacio(String bloque) {
            String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                ExtractorCamposRobusto.CamposHallazgo.TITULO,
                bloque
            );

            assertTrue(Normalizador.esVacio(valor), "Debe retornar vacío para bloque vacío o espacios");
        }

        @Test
        @DisplayName("Retorna vacío si campo es null")
        void retornaVacioSiCampoNull() {
            String bloque = "\"titulo\":\"Test\", \"severidad\":\"High\"";

            String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                null,
                bloque
            );

            assertTrue(Normalizador.esVacio(valor), "Debe retornar vacío si campo es null");
        }

        @Test
        @DisplayName("Retorna vacío si campo no existe en bloque")
        void retornaVacioSiCampoNoExiste() {
            String bloque = "\"titulo\":\"Test\", \"severidad\":\"High\"";

            String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                ExtractorCamposRobusto.CamposHallazgo.EVIDENCIA,
                bloque
            );

            assertTrue(Normalizador.esVacio(valor), "Debe retornar vacío si campo no existe");
        }

        @Test
        @DisplayName("Maneja valores con caracteres especiales")
        void manejaValoresConCaracteresEspeciales() {
            String bloque = "\"titulo\":\"SQLi: ' OR 1=1 --\", \"severidad\":\"Critical\"";

            String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                ExtractorCamposRobusto.CamposHallazgo.TITULO,
                bloque
            );

            assertEquals("SQLi: ' OR 1=1 --", valor, "Debe manejar caracteres especiales SQL");
        }

        @Test
        @DisplayName("Maneja valores con caracteres Unicode")
        void manejaValoresConUnicode() {
            String bloque = "\"titulo\":\"XSS in 评论功能\", \"severidad\":\"High\"";

            String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                ExtractorCamposRobusto.CamposHallazgo.TITULO,
                bloque
            );

            assertEquals("XSS in 评论功能", valor, "Debe manejar caracteres Unicode");
        }

        @Nested
        @DisplayName("Variaciones i18n de título")
        class VariacionesTitulo {
            @Test
            @DisplayName("Reconoce 'title' en inglés")
            void reconoceTitleIngles() {
                String bloque = "\"title\":\"SQL Injection\", \"severity\":\"High\"";
                String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                    ExtractorCamposRobusto.CamposHallazgo.TITULO,
                    bloque
                );
                assertEquals("SQL Injection", valor, "Debe reconocer 'title'");
            }

            @Test
            @DisplayName("Reconoce 'nombre' en español")
            void reconoceNombreEspanol() {
                String bloque = "\"nombre\":\"XSS\", \"severidad\":\"Medium\"";
                String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                    ExtractorCamposRobusto.CamposHallazgo.TITULO,
                    bloque
                );
                assertEquals("XSS", valor, "Debe reconocer 'nombre'");
            }

            @Test
            @DisplayName("Reconoce 'name' como alias")
            void reconoceNameAlias() {
                String bloque = "\"name\":\"IDOR\", \"severity\":\"Medium\"";
                String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                    ExtractorCamposRobusto.CamposHallazgo.TITULO,
                    bloque
                );
                assertEquals("IDOR", valor, "Debe reconocer 'name'");
            }
        }

        @Nested
        @DisplayName("Variaciones i18n de severidad")
        class VariacionesSeveridad {
            @Test
            @DisplayName("Reconoce 'risk' en inglés")
            void reconoceRiskIngles() {
                String bloque = "\"titulo\":\"Test\", \"risk\":\"High\"";
                String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                    ExtractorCamposRobusto.CamposHallazgo.SEVERIDAD,
                    bloque
                );
                assertEquals("High", valor, "Debe reconocer 'risk'");
            }

            @Test
            @DisplayName("Reconoce 'impacto' en español")
            void reconoceImpactoEspanol() {
                String bloque = "\"titulo\":\"Test\", \"impacto\":\"Medium\"";
                String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                    ExtractorCamposRobusto.CamposHallazgo.SEVERIDAD,
                    bloque
                );
                assertEquals("Medium", valor, "Debe reconocer 'impacto'");
            }

            @Test
            @DisplayName("Reconoce 'severity' en inglés")
            void reconoceSeverityIngles() {
                String bloque = "\"titulo\":\"Test\", \"severity\":\"Critical\"";
                String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                    ExtractorCamposRobusto.CamposHallazgo.SEVERIDAD,
                    bloque
                );
                assertEquals("Critical", valor, "Debe reconocer 'severity'");
            }
        }

        @Nested
        @DisplayName("Variaciones i18n de descripción")
        class VariacionesDescripcion {
            @Test
            @DisplayName("Reconoce 'description' en inglés")
            void reconoceDescriptionIngles() {
                String bloque = "\"titulo\":\"Test\", \"description\":\"Detalle en inglés\"";
                String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                    ExtractorCamposRobusto.CamposHallazgo.DESCRIPCION,
                    bloque
                );
                assertEquals("Detalle en inglés", valor, "Debe reconocer 'description'");
            }

            @Test
            @DisplayName("Reconoce 'finding' como alias")
            void reconoceFindingAlias() {
                String bloque = "\"titulo\":\"Test\", \"finding\":\"Hallazgo importante\"";
                String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                    ExtractorCamposRobusto.CamposHallazgo.DESCRIPCION,
                    bloque
                );
                assertEquals("Hallazgo importante", valor, "Debe reconocer 'finding'");
            }

            @Test
            @DisplayName("Reconoce 'details' como alias")
            void reconoceDetailsAlias() {
                String bloque = "\"titulo\":\"Test\", \"details\":\"Detalles técnicos\"";
                String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                    ExtractorCamposRobusto.CamposHallazgo.DESCRIPCION,
                    bloque
                );
                assertEquals("Detalles técnicos", valor, "Debe reconocer 'details'");
            }
        }

        @Nested
        @DisplayName("Variaciones i18n de confianza")
        class VariacionesConfianza {
            @Test
            @DisplayName("Reconoce 'confidence' en inglés")
            void reconoceConfidenceIngles() {
                String bloque = "\"titulo\":\"Test\", \"confidence\":\"High\"";
                String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                    ExtractorCamposRobusto.CamposHallazgo.CONFIANZA,
                    bloque
                );
                assertEquals("High", valor, "Debe reconocer 'confidence'");
            }

            @Test
            @DisplayName("Reconoce 'certainty' como alias")
            void reconoceCertaintyAlias() {
                String bloque = "\"titulo\":\"Test\", \"certainty\":\"Medium\"";
                String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                    ExtractorCamposRobusto.CamposHallazgo.CONFIANZA,
                    bloque
                );
                assertEquals("Medium", valor, "Debe reconocer 'certainty'");
            }

            @Test
            @DisplayName("Reconoce 'certeza' en español")
            void reconoceCertezaEspanol() {
                String bloque = "\"titulo\":\"Test\", \"certeza\":\"Low\"";
                String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                    ExtractorCamposRobusto.CamposHallazgo.CONFIANZA,
                    bloque
                );
                assertEquals("Low", valor, "Debe reconocer 'certeza'");
            }
        }

        @Nested
        @DisplayName("Variaciones i18n de evidencia")
        class VariacionesEvidencia {
            @Test
            @DisplayName("Reconoce 'evidence' en inglés")
            void reconoceEvidenceIngles() {
                String bloque = "\"titulo\":\"Test\", \"evidence\":\"GET /api/data\"";
                String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                    ExtractorCamposRobusto.CamposHallazgo.EVIDENCIA,
                    bloque
                );
                assertEquals("GET /api/data", valor, "Debe reconocer 'evidence'");
            }

            @Test
            @DisplayName("Reconoce 'proof' como alias")
            void reconoceProofAlias() {
                String bloque = "\"titulo\":\"Test\", \"proof\":\"curl -X POST\"";
                String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                    ExtractorCamposRobusto.CamposHallazgo.EVIDENCIA,
                    bloque
                );
                assertEquals("curl -X POST", valor, "Debe reconocer 'proof'");
            }

            @Test
            @DisplayName("Reconoce 'indicator' como alias")
            void reconoceIndicatorAlias() {
                String bloque = "\"titulo\":\"Test\", \"indicator\":\"HTTP 500\"";
                String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
                    ExtractorCamposRobusto.CamposHallazgo.EVIDENCIA,
                    bloque
                );
                assertEquals("HTTP 500", valor, "Debe reconocer 'indicator'");
            }
        }
    }

    @Nested
    @DisplayName("CamposHallazgo")
    class CamposHallazgoTests {

        @Test
        @DisplayName("Tiene todos los campos predefinidos")
        void tieneTodosLosCamposPredefinidos() {
            assertNotNull(ExtractorCamposRobusto.CamposHallazgo.TITULO, "TITULO no debe ser null");
            assertNotNull(ExtractorCamposRobusto.CamposHallazgo.DESCRIPCION, "DESCRIPCION no debe ser null");
            assertNotNull(ExtractorCamposRobusto.CamposHallazgo.SEVERIDAD, "SEVERIDAD no debe ser null");
            assertNotNull(ExtractorCamposRobusto.CamposHallazgo.CONFIANZA, "CONFIANZA no debe ser null");
            assertNotNull(ExtractorCamposRobusto.CamposHallazgo.EVIDENCIA, "EVIDENCIA no debe ser null");
        }

        @Test
        @DisplayName("TODOS contiene todos los campos esperados")
        void todosContieneTodosLosCampos() {
            ExtractorCamposRobusto.Campo[] todos = ExtractorCamposRobusto.CamposHallazgo.TODOS;

            assertTrue(todos.length >= 5, "TODOS debe contener al menos 5 campos");

            // Verificar que los campos específicos están presentes
            boolean tieneTitulo = false;
            boolean tieneDescripcion = false;
            boolean tieneSeveridad = false;
            boolean tieneConfianza = false;
            boolean tieneEvidencia = false;

            for (ExtractorCamposRobusto.Campo campo : todos) {
                String nombre = campo.obtenerNombre();
                if ("titulo".equals(nombre)) tieneTitulo = true;
                if ("descripcion".equals(nombre)) tieneDescripcion = true;
                if ("severidad".equals(nombre)) tieneSeveridad = true;
                if ("confianza".equals(nombre)) tieneConfianza = true;
                if ("evidencia".equals(nombre)) tieneEvidencia = true;
            }

            assertTrue(tieneTitulo, "TODOS debe contener TITULO");
            assertTrue(tieneDescripcion, "TODOS debe contener DESCRIPCION");
            assertTrue(tieneSeveridad, "TODOS debe contener SEVERIDAD");
            assertTrue(tieneConfianza, "TODOS debe contener CONFIANZA");
            assertTrue(tieneEvidencia, "TODOS debe contener EVIDENCIA");
        }

        @Nested
        @DisplayName("obtenerVariaciones")
        class ObtenerVariaciones {

            @Test
            @DisplayName("Retorna variaciones correctas para 'titulo'")
            void retornaVariacionesParaTitulo() {
                String[] variaciones = ExtractorCamposRobusto.CamposHallazgo.obtenerVariaciones("titulo");

                assertTrue(variaciones.length > 0, "Debe tener variaciones");
                assertTrue(contiene(variaciones, "titulo"), "Debe contener 'titulo'");
                assertTrue(contiene(variaciones, "title"), "Debe contener 'title'");
            }

            @Test
            @DisplayName("Retorna variaciones correctas para 'severidad'")
            void retornaVariacionesParaSeveridad() {
                String[] variaciones = ExtractorCamposRobusto.CamposHallazgo.obtenerVariaciones("severidad");

                assertTrue(variaciones.length > 0, "Debe tener variaciones");
                assertTrue(contiene(variaciones, "severidad"), "Debe contener 'severidad'");
                assertTrue(contiene(variaciones, "severity"), "Debe contener 'severity'");
            }

            @Test
            @DisplayName("Retorna array vacío si nombre es null")
            void retornaVacioSiNombreNull() {
                String[] variaciones = ExtractorCamposRobusto.CamposHallazgo.obtenerVariaciones(null);

                assertEquals(0, variaciones.length, "Debe retornar array vacío para null");
            }

            @Test
            @DisplayName("Retorna array vacío si nombre es vacío")
            void retornaVacioSiNombreVacio() {
                String[] variaciones = ExtractorCamposRobusto.CamposHallazgo.obtenerVariaciones("");

                assertEquals(0, variaciones.length, "Debe retornar array vacío para string vacío");
            }

            @Test
            @DisplayName("Retorna array con el nombre original si no es campo conocido")
            void retornaNombreOriginalSiNoEsCampoConocido() {
                String[] variaciones = ExtractorCamposRobusto.CamposHallazgo.obtenerVariaciones("campoDesconocido");

                assertEquals(1, variaciones.length, "Debe retornar array con un elemento");
                assertEquals("campoDesconocido", variaciones[0], "Debe contener el nombre original");
            }

            @Test
            @DisplayName("Es case-insensitive")
            void esCaseInsensitive() {
                String[] variaciones = ExtractorCamposRobusto.CamposHallazgo.obtenerVariaciones("TITULO");

                assertTrue(variaciones.length > 0, "Debe tener variaciones");
                assertTrue(contiene(variaciones, "titulo"), "Debe encontrar campo ignorando mayúsculas");
            }
        }
    }

    @Nested
    @DisplayName("Campo")
    class CampoTests {

        @Test
        @DisplayName("obtenerNombre retorna el nombre correcto")
        void obtenerNombreRetornaNombreCorrecto() {
            assertEquals("titulo", ExtractorCamposRobusto.CamposHallazgo.TITULO.obtenerNombre(), "assertEquals failed at ExtractorCamposRobustoTest.java:566");
            assertEquals("severidad", ExtractorCamposRobusto.CamposHallazgo.SEVERIDAD.obtenerNombre(), "assertEquals failed at ExtractorCamposRobustoTest.java:567");
        }

        @Test
        @DisplayName("obtenerPatron retorna patrón no null")
        void obtenerPatronRetornaPatronNoNull() {
            assertNotNull(ExtractorCamposRobusto.CamposHallazgo.TITULO.obtenerPatron(), "assertNotNull failed at ExtractorCamposRobustoTest.java:573");
            assertNotNull(ExtractorCamposRobusto.CamposHallazgo.SEVERIDAD.obtenerPatron(), "assertNotNull failed at ExtractorCamposRobustoTest.java:574");
        }

        @Test
        @DisplayName("obtenerVariaciones retorna copia del array")
        void obtenerVariacionesRetornaCopia() {
            String[] variaciones1 = ExtractorCamposRobusto.CamposHallazgo.TITULO.obtenerVariaciones();
            String[] variaciones2 = ExtractorCamposRobusto.CamposHallazgo.TITULO.obtenerVariaciones();

            assertNotSame(variaciones1, variaciones2, "Debe retornar copia diferente del array");
        }
    }

    // Helper method
    private boolean contiene(String[] array, String valor) {
        for (String s : array) {
            if (s.equals(valor)) {
                return true;
            }
        }
        return false;
    }
}
