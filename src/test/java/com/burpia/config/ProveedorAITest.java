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
            assertTrue(ProveedorAI.existeProveedor(ProveedorAI.PROVEEDOR_CUSTOM_01), "Custom 01 deberia existir");
            assertTrue(ProveedorAI.existeProveedor(ProveedorAI.PROVEEDOR_CUSTOM_02), "Custom 02 deberia existir");
            assertTrue(ProveedorAI.existeProveedor(ProveedorAI.PROVEEDOR_CUSTOM_03), "Custom 03 deberia existir");
        }

        @Test
        @DisplayName("Custom 01/02/03 son los ultimos proveedores en orden")
        void customEsUltimo() {
            List<String> nombres = ProveedorAI.obtenerNombresProveedores();
            assertEquals(ProveedorAI.PROVEEDOR_CUSTOM_01, nombres.get(nombres.size() - 3),
                "Custom 01 deberia estar en la antepenultima posicion");
            assertEquals(ProveedorAI.PROVEEDOR_CUSTOM_02, nombres.get(nombres.size() - 2),
                "Custom 02 deberia estar en la penultima posicion");
            assertEquals(ProveedorAI.PROVEEDOR_CUSTOM_03, nombres.get(nombres.size() - 1),
                "Custom 03 deberia ser el ultimo proveedor de la lista");
            assertFalse(nombres.contains("-- Custom --"),
                "No debe aparecer alias antiguo en la lista visual");
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
        @DisplayName("Custom 01/02/03 devuelven URL en español por defecto")
        void customDevuelveUrlEspanol() {
            String url1 = ProveedorAI.obtenerUrlApiPorDefecto(ProveedorAI.PROVEEDOR_CUSTOM_01, "es");
            String url2 = ProveedorAI.obtenerUrlApiPorDefecto(ProveedorAI.PROVEEDOR_CUSTOM_02, "es");
            String url3 = ProveedorAI.obtenerUrlApiPorDefecto(ProveedorAI.PROVEEDOR_CUSTOM_03, "es");
            assertEquals(ProveedorAI.URL_CUSTOM_ES, url1, "Custom 01 en español deberia usar URL_CUSTOM_ES");
            assertEquals(ProveedorAI.URL_CUSTOM_ES, url2, "Custom 02 en español deberia usar URL_CUSTOM_ES");
            assertEquals(ProveedorAI.URL_CUSTOM_ES, url3, "Custom 03 en español deberia usar URL_CUSTOM_ES");
        }

        @Test
        @DisplayName("Custom 01/02/03 devuelven URL en inglés cuando idioma es en")
        void customDevuelveUrlIngles() {
            String url1 = ProveedorAI.obtenerUrlApiPorDefecto(ProveedorAI.PROVEEDOR_CUSTOM_01, "en");
            String url2 = ProveedorAI.obtenerUrlApiPorDefecto(ProveedorAI.PROVEEDOR_CUSTOM_02, "en");
            String url3 = ProveedorAI.obtenerUrlApiPorDefecto(ProveedorAI.PROVEEDOR_CUSTOM_03, "en");
            assertEquals(ProveedorAI.URL_CUSTOM_EN, url1, "Custom 01 en ingles deberia usar URL_CUSTOM_EN");
            assertEquals(ProveedorAI.URL_CUSTOM_EN, url2, "Custom 02 en ingles deberia usar URL_CUSTOM_EN");
            assertEquals(ProveedorAI.URL_CUSTOM_EN, url3, "Custom 03 en ingles deberia usar URL_CUSTOM_EN");
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
                if (!ProveedorAI.esProveedorCustom(nombre)) {
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
                if (ProveedorAI.esProveedorCustom(nombre)) continue;
                String modeloDefault = ProveedorAI.obtenerModeloPorDefecto(nombre);
                assertNotNull(modeloDefault, nombre + " no tiene modelo por defecto");
                List<String> disponibles = ProveedorAI.obtenerModelosDisponibles(nombre);
                assertTrue(disponibles.contains(modeloDefault),
                    nombre + ": modelo por defecto '" + modeloDefault + "' no esta en lista de disponibles");
            }
        }

        @Test
        @DisplayName("Custom 01/02/03 no tienen modelos ni modelo por defecto")
        void customNoTieneModelos() {
            List<String> modelos1 = ProveedorAI.obtenerModelosDisponibles(ProveedorAI.PROVEEDOR_CUSTOM_01);
            List<String> modelos2 = ProveedorAI.obtenerModelosDisponibles(ProveedorAI.PROVEEDOR_CUSTOM_02);
            List<String> modelos3 = ProveedorAI.obtenerModelosDisponibles(ProveedorAI.PROVEEDOR_CUSTOM_03);
            assertTrue(modelos1.isEmpty(), "Custom 01 no deberia tener modelos predefinidos");
            assertTrue(modelos2.isEmpty(), "Custom 02 no deberia tener modelos predefinidos");
            assertTrue(modelos3.isEmpty(), "Custom 03 no deberia tener modelos predefinidos");
            assertEquals("", ProveedorAI.obtenerModeloPorDefecto(ProveedorAI.PROVEEDOR_CUSTOM_01),
                "Custom 01 deberia tener modelo vacio");
            assertEquals("", ProveedorAI.obtenerModeloPorDefecto(ProveedorAI.PROVEEDOR_CUSTOM_02),
                "Custom 02 deberia tener modelo vacio");
            assertEquals("", ProveedorAI.obtenerModeloPorDefecto(ProveedorAI.PROVEEDOR_CUSTOM_03),
                "Custom 03 deberia tener modelo vacio");
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

        @Test
        @DisplayName("MiniMax no permite carga remota de modelos y otros proveedores si")
        void minimaxNoPermiteCargaRemotaModelos() {
            assertFalse(ProveedorAI.permiteCargaRemotaModelos("minimax"),
                "minimax no deberia permitir carga remota de modelos");
            assertTrue(ProveedorAI.permiteCargaRemotaModelos("OpenAI"),
                "OpenAI deberia permitir carga remota de modelos");
            assertTrue(ProveedorAI.permiteCargaRemotaModelos(null),
                "Un proveedor nulo no debe bloquear el boton por defecto");
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
