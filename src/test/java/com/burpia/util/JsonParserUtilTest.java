package com.burpia.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonParserUtil Tests")
class JsonParserUtilTest {

    private static JsonObject parsearObjeto(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    @Nested
    @DisplayName("extraerObjetosHallazgos")
    class ExtraerObjetosHallazgos {

        @Test
        @DisplayName("Extrae hallazgos desde campo array")
        void extraeDesdeCampoArray() {
            JsonObject raiz = parsearObjeto("{\"hallazgos\": [{\"titulo\": \"XSS\"}]}");
            List<JsonObject> hallazgos = JsonParserUtil.extraerObjetosHallazgos(raiz, JsonParserUtil.CAMPOS_HALLAZGOS);
            assertEquals(1, hallazgos.size(), "Debe extraer el hallazgo del array");
        }

        @Test
        @DisplayName("Cae a la raiz cuando el campo de hallazgos no es array ni objeto")
        void caeALaRaizConCampoNoEstructural() {
            // El campo "hallazgos" existe pero es un string: antes retornaba lista
            // vacía sin evaluar la raíz, perdiendo un hallazgo válido.
            JsonObject raiz = parsearObjeto(
                "{\"hallazgos\": \"sin hallazgos\", \"descripcion\": \"XSS reflejado en parametro q\"}");
            List<JsonObject> hallazgos = JsonParserUtil.extraerObjetosHallazgos(raiz, JsonParserUtil.CAMPOS_HALLAZGOS);
            assertEquals(1, hallazgos.size(), "La raíz debe evaluarse como hallazgo cuando el campo no aporta objetos");
            assertEquals("XSS reflejado en parametro q",
                hallazgos.get(0).get("descripcion").getAsString(), "Debe conservar la descripción de la raíz");
        }

        @Test
        @DisplayName("Cae a la raiz cuando el campo de hallazgos es un array vacio")
        void caeALaRaizConArrayVacio() {
            JsonObject raiz = parsearObjeto(
                "{\"findings\": [], \"evidencia\": \"Header X-Debug expuesto\"}");
            List<JsonObject> hallazgos = JsonParserUtil.extraerObjetosHallazgos(raiz, JsonParserUtil.CAMPOS_HALLAZGOS);
            assertEquals(1, hallazgos.size(), "Un array vacío no debe impedir evaluar la raíz como hallazgo");
        }

        @Test
        @DisplayName("No duplica la raiz cuando el campo aporta hallazgos")
        void noDuplicaRaizConCampoValido() {
            JsonObject raiz = parsearObjeto(
                "{\"hallazgos\": [{\"titulo\": \"SQLi\"}], \"descripcion\": \"resumen general\"}");
            List<JsonObject> hallazgos = JsonParserUtil.extraerObjetosHallazgos(raiz, JsonParserUtil.CAMPOS_HALLAZGOS);
            assertEquals(1, hallazgos.size(), "Con hallazgos en el campo no debe agregar también la raíz");
        }

        @Test
        @DisplayName("Retorna vacio si ni el campo ni la raiz parecen hallazgo")
        void retornaVacioSinHallazgos() {
            JsonObject raiz = parsearObjeto("{\"hallazgos\": 42, \"otro\": \"nada\"}");
            List<JsonObject> hallazgos = JsonParserUtil.extraerObjetosHallazgos(raiz, JsonParserUtil.CAMPOS_HALLAZGOS);
            assertTrue(hallazgos.isEmpty(), "Sin contenido de hallazgo no debe inventar objetos");
        }
    }
}
