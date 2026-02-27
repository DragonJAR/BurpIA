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
            assertTrue(ProveedorAI.existeProveedor("Ollama"));
            assertTrue(ProveedorAI.existeProveedor("OpenAI"));
            assertTrue(ProveedorAI.existeProveedor("Claude"));
            assertTrue(ProveedorAI.existeProveedor("Gemini"));
            assertTrue(ProveedorAI.existeProveedor("Z.ai"));
            assertTrue(ProveedorAI.existeProveedor("minimax"));
            assertTrue(ProveedorAI.existeProveedor("Moonshot (Kimi)"));
            assertTrue(ProveedorAI.existeProveedor(ProveedorAI.PROVEEDOR_CUSTOM));
        }

        @Test
        @DisplayName("Custom es el ultimo proveedor")
        void customEsUltimo() {
            List<String> nombres = ProveedorAI.obtenerNombresProveedores();
            assertEquals(ProveedorAI.PROVEEDOR_CUSTOM, nombres.get(nombres.size() - 1));
        }

        @Test
        @DisplayName("Proveedor inexistente retorna null")
        void proveedorInexistenteRetornaNull() {
            assertNull(ProveedorAI.obtenerProveedor("NoExiste"));
            assertFalse(ProveedorAI.existeProveedor("NoExiste"));
        }
    }

    @Nested
    @DisplayName("URLs de API")
    class UrlsApi {
        @Test
        @DisplayName("Ollama usa localhost")
        void ollamaUsaLocalhost() {
            String url = ProveedorAI.obtenerUrlApiPorDefecto("Ollama", "es");
            assertTrue(url.contains("localhost:11434"));
        }

        @Test
        @DisplayName("OpenAI usa api.openai.com")
        void openaiUsaApiOpenai() {
            String url = ProveedorAI.obtenerUrlApiPorDefecto("OpenAI", "es");
            assertTrue(url.contains("api.openai.com"));
        }

        @Test
        @DisplayName("Claude usa api.anthropic.com")
        void claudeUsaApiAnthropic() {
            String url = ProveedorAI.obtenerUrlApiPorDefecto("Claude", "es");
            assertTrue(url.contains("api.anthropic.com"));
        }

        @Test
        @DisplayName("Custom devuelve URL en español por defecto")
        void customDevuelveUrlEspanol() {
            String url = ProveedorAI.obtenerUrlApiPorDefecto(ProveedorAI.PROVEEDOR_CUSTOM, "es");
            assertEquals(ProveedorAI.URL_CUSTOM_ES, url);
        }

        @Test
        @DisplayName("Custom devuelve URL en ingles cuando idioma es en")
        void customDevuelveUrlIngles() {
            String url = ProveedorAI.obtenerUrlApiPorDefecto(ProveedorAI.PROVEEDOR_CUSTOM, "en");
            assertEquals(ProveedorAI.URL_CUSTOM_EN, url);
        }

        @Test
        @DisplayName("Proveedor inexistente retorna null para URL")
        void proveedorInexistenteRetornaNullUrl() {
            assertNull(ProveedorAI.obtenerUrlApiPorDefecto("NoExiste", "es"));
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
                List<String> disponibles = ProveedorAI.obtenerModelosDisponibles(nombre);
                assertTrue(disponibles.contains(modeloDefault),
                    nombre + ": modelo por defecto '" + modeloDefault + "' no esta en lista de disponibles");
            }
        }

        @Test
        @DisplayName("Custom no tiene modelos ni modelo por defecto")
        void customNoTieneModelos() {
            List<String> modelos = ProveedorAI.obtenerModelosDisponibles(ProveedorAI.PROVEEDOR_CUSTOM);
            assertTrue(modelos.isEmpty());
            assertEquals("", ProveedorAI.obtenerModeloPorDefecto(ProveedorAI.PROVEEDOR_CUSTOM));
        }

        @Test
        @DisplayName("Proveedor inexistente retorna lista vacia")
        void proveedorInexistenteRetornaListaVacia() {
            List<String> modelos = ProveedorAI.obtenerModelosDisponibles("NoExiste");
            assertTrue(modelos.isEmpty());
        }

        @Test
        @DisplayName("Proveedor inexistente retorna null para modelo por defecto")
        void proveedorInexistenteRetornaNullModelo() {
            assertNull(ProveedorAI.obtenerModeloPorDefecto("NoExiste"));
        }
    }

    @Nested
    @DisplayName("Configuracion de proveedores")
    class ConfiguracionProveedores {
        @Test
        @DisplayName("Ollama no requiere clave API")
        void ollamaNoRequiereClave() {
            ProveedorAI.ConfiguracionProveedor config = ProveedorAI.obtenerProveedor("Ollama");
            assertNotNull(config);
            assertFalse(config.requiereClaveApi());
        }

        @Test
        @DisplayName("OpenAI requiere clave API")
        void openaiRequiereClave() {
            ProveedorAI.ConfiguracionProveedor config = ProveedorAI.obtenerProveedor("OpenAI");
            assertNotNull(config);
            assertTrue(config.requiereClaveApi());
        }

        @Test
        @DisplayName("Claude requiere clave API")
        void claudeRequiereClave() {
            ProveedorAI.ConfiguracionProveedor config = ProveedorAI.obtenerProveedor("Claude");
            assertNotNull(config);
            assertTrue(config.requiereClaveApi());
        }

        @Test
        @DisplayName("Cada proveedor tiene maxTokens mayor a 0")
        void cadaProveedorTieneMaxTokens() {
            List<String> nombres = ProveedorAI.obtenerNombresProveedores();
            for (String nombre : nombres) {
                ProveedorAI.ConfiguracionProveedor config = ProveedorAI.obtenerProveedor(nombre);
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
            assertEquals(tamanoOriginal, modelos2.size());
        }
    }
}
