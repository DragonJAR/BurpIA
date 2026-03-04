package com.burpia.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests de ExtractorCamposRobusto")
class ExtractorCamposRobustoTest {

    @Test
    @DisplayName("extraerBloquesPorCampo separa correctamente bloques por título")
    void testExtraerBloquesPorCampo_JSONMalformado() {
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

    @Test
    @DisplayName("extraerBloquesPorCampo retorna lista vacía si contenido es vacío")
    void testExtraerBloquesPorCampo_ContenidoVacio() {
        List<String> bloques = ExtractorCamposRobusto.extraerBloquesPorCampo(
            "",
            ExtractorCamposRobusto.CamposHallazgo.TITULO
        );

        assertTrue(bloques.isEmpty(), "Debe retornar lista vacía");
    }

    @Test
    @DisplayName("extraerBloquesPorCampo retorna lista vacía si no hay campo delimitador")
    void testExtraerBloquesPorCampo_SinDelimitador() {
        String sinTitulo = "{\"severidad\":\"High\", \"confianza\":\"Medium\"}";

        List<String> bloques = ExtractorCamposRobusto.extraerBloquesPorCampo(
            sinTitulo,
            ExtractorCamposRobusto.CamposHallazgo.TITULO
        );

        assertTrue(bloques.isEmpty(), "Debe retornar lista vacía si no hay delimitador");
    }

    @Test
    @DisplayName("extraerCampoDeBloque toma último valor cuando hay duplicados")
    void testExtraerCampoDeBloque_CamposDuplicados() {
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
    @DisplayName("extraerCampoDeBloque maneja comillas sin cerrar")
    void testExtraerCampoDeBloque_ComillasSinCerrar() {
        String bloque = "\"titulo\":\"SQL\", " +
                       "\"severidad\":\"High\", " +
                       "\"descripcion\":\"Texto sin cerrar";

        String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
            ExtractorCamposRobusto.CamposHallazgo.DESCRIPCION,
            bloque
        );

        assertEquals("Texto sin cerrar", valor);
    }

    @Test
    @DisplayName("extraerCampoDeBloque extrae título correctamente")
    void testExtraerCampoDeBloque_Titulo() {
        String bloque = "\"titulo\":\"XSS en input\", " +
                       "\"severidad\":\"Medium\"";

        String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
            ExtractorCamposRobusto.CamposHallazgo.TITULO,
            bloque
        );

        assertEquals("XSS en input", valor);
    }

    @Test
    @DisplayName("extraerCampoDeBloque extrae severidad correctamente")
    void testExtraerCampoDeBloque_Severidad() {
        String bloque = "\"titulo\":\"Test\", " +
                       "\"severidad\":\"Critical\"";

        String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
            ExtractorCamposRobusto.CamposHallazgo.SEVERIDAD,
            bloque
        );

        assertEquals("Critical", valor);
    }

    @Test
    @DisplayName("extraerCampoDeBloque extrae confianza correctamente")
    void testExtraerCampoDeBloque_Confianza() {
        String bloque = "\"titulo\":\"Test\", " +
                       "\"confianza\":\"High\"";

        String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
            ExtractorCamposRobusto.CamposHallazgo.CONFIANZA,
            bloque
        );

        assertEquals("High", valor);
    }

    @Test
    @DisplayName("extraerCampoDeBloque extrae evidencia correctamente")
    void testExtraerCampoDeBloque_Evidencia() {
        String bloque = "\"titulo\":\"Test\", " +
                       "\"evidencia\":\"POST /api/users\"";

        String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
            ExtractorCamposRobusto.CamposHallazgo.EVIDENCIA,
            bloque
        );

        assertEquals("POST /api/users", valor);
    }

    @Test
    @DisplayName("extraerCampoDeBloque retorna vacío si bloque es null")
    void testExtraerCampoDeBloque_BloqueNull() {
        String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
            ExtractorCamposRobusto.CamposHallazgo.TITULO,
            null
        );

        assertTrue(Normalizador.esVacio(valor), "Debe retornar vacío");
    }

    @Test
    @DisplayName("extraerCampoDeBloque retorna vacío si campo no existe")
    void testExtraerCampoDeBloque_CampoInexistente() {
        String bloque = "\"titulo\":\"Test\", " +
                       "\"severidad\":\"High\"";

        String valor = ExtractorCamposRobusto.extraerCampoDeBloque(
            ExtractorCamposRobusto.CamposHallazgo.EVIDENCIA,
            bloque
        );

        assertTrue(Normalizador.esVacio(valor), "Debe retornar vacío");
    }

    @Test
    @DisplayName("CamposHallazgo tiene todos los campos predefinidos")
    void testCamposHallazgo_Predefinidos() {
        assertNotNull(ExtractorCamposRobusto.CamposHallazgo.TITULO);
        assertNotNull(ExtractorCamposRobusto.CamposHallazgo.DESCRIPCION);
        assertNotNull(ExtractorCamposRobusto.CamposHallazgo.SEVERIDAD);
        assertNotNull(ExtractorCamposRobusto.CamposHallazgo.CONFIANZA);
        assertNotNull(ExtractorCamposRobusto.CamposHallazgo.EVIDENCIA);
        assertEquals(5, ExtractorCamposRobusto.CamposHallazgo.TODOS.length);
    }

    @Test
    @DisplayName("extraerCampoDeBloque soporta variaciones i18n de título")
    void testExtraerCampoDeBloque_TituloVariaciones() {
        // Test con "title" en inglés
        String bloque1 = "\"title\":\"SQL Injection\", \"severity\":\"High\"";
        String valor1 = ExtractorCamposRobusto.extraerCampoDeBloque(
            ExtractorCamposRobusto.CamposHallazgo.TITULO,
            bloque1
        );
        assertEquals("SQL Injection", valor1);

        // Test con "nombre" en español
        String bloque2 = "\"nombre\":\"XSS\", \"severidad\":\"Medium\"";
        String valor2 = ExtractorCamposRobusto.extraerCampoDeBloque(
            ExtractorCamposRobusto.CamposHallazgo.TITULO,
            bloque2
        );
        assertEquals("XSS", valor2);
    }

    @Test
    @DisplayName("extraerCampoDeBloque soporta variaciones i18n de severidad")
    void testExtraerCampoDeBloque_SeveridadVariaciones() {
        // Test con "risk" en inglés
        String bloque1 = "\"titulo\":\"Test\", \"risk\":\"High\"";
        String valor1 = ExtractorCamposRobusto.extraerCampoDeBloque(
            ExtractorCamposRobusto.CamposHallazgo.SEVERIDAD,
            bloque1
        );
        assertEquals("High", valor1);

        // Test con "impacto" en español
        String bloque2 = "\"titulo\":\"Test\", \"impacto\":\"Medium\"";
        String valor2 = ExtractorCamposRobusto.extraerCampoDeBloque(
            ExtractorCamposRobusto.CamposHallazgo.SEVERIDAD,
            bloque2
        );
        assertEquals("Medium", valor2);
    }
}
