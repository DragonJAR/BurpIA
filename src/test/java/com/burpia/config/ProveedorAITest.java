package com.burpia.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProveedorAI Tests")
class ProveedorAITest {

    @Nested
    @DisplayName("Proveedores registrados")
    class ProveedoresRegistrados {
        @Test
        @DisplayName("Contiene todos los proveedores esperados")
        void contieneProveedoresEsperados() {
            assertTrue(ProveedorAI.existeProveedor("Ollama"), "Ollama deberia existir");
            assertTrue(ProveedorAI.existeProveedor("OpenAI"), "OpenAI deberia existir");
            assertTrue(ProveedorAI.existeProveedor("Claude"), "Claude deberia existir");
            assertTrue(ProveedorAI.existeProveedor("Gemini"), "Gemini deberia existir");
            assertTrue(ProveedorAI.existeProveedor("Z.ai"), "Z.ai deberia existir");
            assertTrue(ProveedorAI.existeProveedor("minimax"), "minimax deberia existir");
            assertTrue(ProveedorAI.existeProveedor("Moonshot (Kimi)"), "Moonshot (Kimi) deberia existir");
            assertTrue(ProveedorAI.existeProveedor(ProveedorAI.PROVEEDOR_CUSTOM), "Custom deberia existir");
        }

        @Test
        @DisplayName("Custom es el ultimo proveedor")
        void customEsUltimo() {
            List<String> nombres = ProveedorAI.obtenerNombresProveedores();
            assertEquals(ProveedorAI.PROVEEDOR_CUSTOM, nombres.get(nombres.size() - 1),
                "Custom deberia ser el ultimo proveedor de la lista");
        }

        @Test
        @DisplayName("Proveedor inexistente retorna null")
        void proveedorInexistenteRetornaNull() {
            assertNull(ProveedorAI.obtenerProveedor("NoExiste"), "Proveedor inexistente deberia retornar null");
            assertFalse(ProveedorAI.existeProveedor("NoExiste"), "Proveedor inexistente no deberia existir");
        }
    }

    @Nested
    @DisplayName("URLs de API")
    class UrlsApi {
        @Test
        @DisplayName("Ollama usa localhost")
        void ollamaUsaLocalhost() {
            String url = ProveedorAI.obtenerUrlApiPorDefecto("Ollama", "es");
            assertTrue(url.contains("localhost:11434"), "Ollama deberia usar localhost:11434");
        }

        @Test
        @DisplayName("OpenAI usa api.openai.com")
        void openaiUsaApiOpenai() {
            String url = ProveedorAI.obtenerUrlApiPorDefecto("OpenAI", "es");
            assertTrue(url.contains("api.openai.com"), "OpenAI deberia usar api.openai.com");
        }

        @Test
        @DisplayName("Claude usa api.anthropic.com")
        void claudeUsaApiAnthropic() {
            String url = ProveedorAI.obtenerUrlApiPorDefecto("Claude", "es");
            assertTrue(url.contains("api.anthropic.com"), "Claude deberia usar api.anthropic.com");
        }

        @Test
        @DisplayName("Custom devuelve URL en español por defecto")
        void customDevuelveUrlEspanol() {
            String url = ProveedorAI.obtenerUrlApiPorDefecto(ProveedorAI.PROVEEDOR_CUSTOM, "es");
            assertEquals(ProveedorAI.URL_CUSTOM_ES, url, "Custom en español deberia usar URL_CUSTOM_ES");
        }

        @Test
        @DisplayName("Custom devuelve URL en inglés cuando idioma es en")
        void customDevuelveUrlIngles() {
            String url = ProveedorAI.obtenerUrlApiPorDefecto(ProveedorAI.PROVEEDOR_CUSTOM, "en");
            assertEquals(ProveedorAI.URL_CUSTOM_EN, url, "Custom en ingles deberia usar URL_CUSTOM_EN");
        }

        @Test
        @DisplayName("Proveedor inexistente retorna null para URL")
        void proveedorInexistenteRetornaNullUrl() {
            assertNull(ProveedorAI.obtenerUrlApiPorDefecto("NoExiste", "es"),
                "Proveedor inexistente deberia retornar null");
        }
    }

    @Nested
    @DisplayName("Modelos")
    class Modelos {
        @Test
        @DisplayName("Cada proveedor tiene al menos un modelo")
        void cadaProveedorTieneModelos() {
            List<String> nombres = ProveedorAI.obtenerNombresProveedores();
            for (String nombre : nombres) {
                if (!ProveedorAI.PROVEEDOR_CUSTOM.equals(nombre)) {
                    List<String> modelos = ProveedorAI.obtenerModelosDisponibles(nombre);
                    assertFalse(modelos.isEmpty(), nombre + " no tiene modelos");
                }
            }
        }

        @Test
        @DisplayName("Modelo por defecto esta en la lista de modelos disponibles")
        void modeloPorDefectoEstaEnLista() {
            List<String> nombres = ProveedorAI.obtenerNombresProveedores();
            for (String nombre : nombres) {
                if (ProveedorAI.PROVEEDOR_CUSTOM.equals(nombre)) continue;
                String modeloDefault = ProveedorAI.obtenerModeloPorDefecto(nombre);
                assertNotNull(modeloDefault, nombre + " no tiene modelo por defecto");
                List<String> disponibles = ProveedorAI.obtenerModelosDisponibles(nombre);
                assertTrue(disponibles.contains(modeloDefault),
                    nombre + ": modelo por defecto '" + modeloDefault + "' no esta en lista de disponibles");
            }
        }

        @Test
        @DisplayName("Custom no tiene modelos ni modelo por defecto")
        void customNoTieneModelos() {
            List<String> modelos = ProveedorAI.obtenerModelosDisponibles(ProveedorAI.PROVEEDOR_CUSTOM);
            assertTrue(modelos.isEmpty(), "Custom no deberia tener modelos predefinidos");
            String modeloDefault = ProveedorAI.obtenerModeloPorDefecto(ProveedorAI.PROVEEDOR_CUSTOM);
            assertEquals("", modeloDefault, "Custom deberia tener modelo vacio");
        }

        @Test
        @DisplayName("Proveedor inexistente retorna lista vacía")
        void proveedorInexistenteRetornaListaVacia() {
            List<String> modelos = ProveedorAI.obtenerModelosDisponibles("NoExiste");
            assertTrue(modelos.isEmpty(), "Proveedor inexistente deberia retornar lista vacia");
        }

        @Test
        @DisplayName("Proveedor inexistente retorna null para modelo por defecto")
        void proveedorInexistenteRetornaNullModelo() {
            assertNull(ProveedorAI.obtenerModeloPorDefecto("NoExiste"),
                "Proveedor inexistente deberia retornar null para modelo por defecto");
        }
    }

    @Nested
    @DisplayName("Configuracion de proveedores")
    class ConfiguracionProveedores {
        @Test
        @DisplayName("Ollama no requiere clave API")
        void ollamaNoRequiereClave() {
            ProveedorAI.ConfiguracionProveedor config = ProveedorAI.obtenerProveedor("Ollama");
            assertNotNull(config, "Ollama deberia tener configuracion");
            assertFalse(config.requiereClaveApi(), "Ollama no deberia requerir clave API");
        }

        @Test
        @DisplayName("OpenAI requiere clave API")
        void openaiRequiereClave() {
            ProveedorAI.ConfiguracionProveedor config = ProveedorAI.obtenerProveedor("OpenAI");
            assertNotNull(config, "OpenAI deberia tener configuracion");
            assertTrue(config.requiereClaveApi(), "OpenAI deberia requerir clave API");
        }

        @Test
        @DisplayName("Claude requiere clave API")
        void claudeRequiereClave() {
            ProveedorAI.ConfiguracionProveedor config = ProveedorAI.obtenerProveedor("Claude");
            assertNotNull(config, "Claude deberia tener configuracion");
            assertTrue(config.requiereClaveApi(), "Claude deberia requerir clave API");
        }

        @Test
        @DisplayName("Cada proveedor tiene maxTokens mayor a 0")
        void cadaProveedorTieneMaxTokens() {
            List<String> nombres = ProveedorAI.obtenerNombresProveedores();
            for (String nombre : nombres) {
                ProveedorAI.ConfiguracionProveedor config = ProveedorAI.obtenerProveedor(nombre);
                assertNotNull(config, nombre + " no tiene configuracion");
                assertTrue(config.obtenerMaxTokensPorDefecto() > 0,
                    nombre + " tiene maxTokens <= 0");
            }
        }

        @Test
        @DisplayName("La lista de modelos es una copia defensiva")
        void listaModelosEsCopiaDefensiva() {
            List<String> modelos1 = ProveedorAI.obtenerModelosDisponibles("OpenAI");
            int tamanoOriginal = modelos1.size();
            modelos1.add("modelo-falso");
            List<String> modelos2 = ProveedorAI.obtenerModelosDisponibles("OpenAI");
            assertEquals(tamanoOriginal, modelos2.size(),
                "Modificar la lista retornada no deberia afectar el origen");
        }
    }
}
