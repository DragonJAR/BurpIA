package com.burpia.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests para {@link ConfigSanitizers} (package-private).
 *
 * <p>Cubre H5: normalización de claves timeout {@code "proveedor::modelo"}
 * cuando el nombre del modelo contiene "::" (variantes custom como
 * "Qwen/Qwen2.5::Instruct"). Antes el {@code indexOf("::")} truncaba el modelo
 * y el timeout configurado se ignoraba silenciosamente.</p>
 */
@DisplayName("ConfigSanitizers Tests")
class ConfigSanitizersTest {

    @Test
    @DisplayName("H5: clave con '::' en el modelo se parsea sin truncar el modelo")
    void testClaveConSeparadorEnModelo() {
        // Usamos un proveedor real del catálogo con un modelo cuyo nombre
        // contiene "::" (variantes custom como "Qwen/Qwen2.5::Instruct").
        String clave = "OpenAI::Qwen/Qwen2.5::Instruct";
        String saneada = ConfigSanitizers.normalizarClaveTimeoutProveedorModelo(clave);
        assertEquals("OpenAI::Qwen/Qwen2.5::Instruct", saneada,
            "El modelo con '::' no debe truncarse: " + saneada);
    }

    @Test
    @DisplayName("clave simple (sin '::' en modelo) sigue funcionando")
    void testClaveSimple() {
        String clave = "OpenAI::gpt-4o";
        String saneada = ConfigSanitizers.normalizarClaveTimeoutProveedorModelo(clave);
        assertEquals("OpenAI::gpt-4o", saneada);
    }

    @Test
    @DisplayName("clave con proveedor en minúsculas se normaliza")
    void testClaveProveedorMinusculas() {
        String saneada = ConfigSanitizers.normalizarClaveTimeoutProveedorModelo("openai::gpt-4o");
        assertEquals("OpenAI::gpt-4o", saneada);
    }

    @Test
    @DisplayName("clave vacía retorna ''")
    void testClaveVacia() {
        assertEquals("", ConfigSanitizers.normalizarClaveTimeoutProveedorModelo(""));
        assertEquals("", ConfigSanitizers.normalizarClaveTimeoutProveedorModelo(null));
    }

    @Test
    @DisplayName("clave sin separador válido retorna ''")
    void testClaveSinSeparadorValido() {
        // Ningún prefijo hasta '::' es proveedor válido.
        assertEquals("", ConfigSanitizers.normalizarClaveTimeoutProveedorModelo("noexiste::model"));
    }

    @Test
    @DisplayName("sanitiza mapa String por proveedor: descarta claves inválidas, normaliza válidas")
    void testSanitizaMapaStringPorProveedor() {
        Map<String, String> mapa = new HashMap<>();
        mapa.put("OpenAI", "sk-123");
        mapa.put("openai", "sk-456"); // duplicado por case → colapsa a "OpenAI"
        mapa.put("ProveedorInexistente", "x"); // descartado
        mapa.put(null, "y"); // descartado

        Map<String, String> saneado = ConfigSanitizers.normalizarMapaStringPorProveedor(mapa);

        assertTrue(saneado.containsKey("OpenAI"), "Debe conservar el proveedor válido");
        assertFalse(saneado.containsKey("ProveedorInexistente"),
            "Debe descartar el proveedor inexistente");
        assertEquals(1, saneado.size(), "Debe colapsar variantes case del mismo proveedor");
    }

    @Test
    @DisplayName("sanitiza mapa Int por proveedor: descarta valores no positivos")
    void testSanitizaMapaIntPorProveedor() {
        Map<String, Integer> mapa = new HashMap<>();
        mapa.put("OpenAI", 4096);
        mapa.put("Claude", 0); // descartado (no positivo)
        mapa.put("Gemini", -1); // descartado

        Map<String, Integer> saneado = ConfigSanitizers.normalizarMapaIntPorProveedor(mapa);

        assertEquals(1, saneado.size());
        assertEquals(4096, saneado.get("OpenAI"));
    }
}
