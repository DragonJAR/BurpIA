package com.burpia.config;

import java.util.*;

public class ProveedorAI {
    public static final String PROVEEDOR_CUSTOM = "-- Custom --";
    public static final String URL_CUSTOM_EN = "https://YOUR_OPENAI_COMPATIBLE_BASE_URL/v1";
    public static final String URL_CUSTOM_ES = "https://TU_BASE_URL_COMPATIBLE_CON_OPENAI/v1";

    public static class ConfiguracionProveedor {
        private final String nombre;
        private final String urlApi;
        private final String modeloPorDefecto;
        private final List<String> modelosDisponibles;
        private final boolean requiereClaveApi;
        private final int maxTokensPorDefecto;
        private final int maxTokensVentana;

        public ConfiguracionProveedor(String nombre, String urlApi, String modeloPorDefecto,
                                     List<String> modelosDisponibles, boolean requiereClaveApi,
                                     int maxTokensPorDefecto, int maxTokensVentana) {
            this.nombre = nombre;
            this.urlApi = urlApi;
            this.modeloPorDefecto = modeloPorDefecto;
            this.modelosDisponibles = new ArrayList<>(modelosDisponibles);
            this.requiereClaveApi = requiereClaveApi;
            this.maxTokensPorDefecto = maxTokensPorDefecto;
            this.maxTokensVentana = maxTokensVentana;
        }

        public String obtenerNombre() { return nombre; }
        public String obtenerUrlApi() { return urlApi; }
        public String obtenerModeloPorDefecto() { return modeloPorDefecto; }
        public List<String> obtenerModelosDisponibles() { return new ArrayList<>(modelosDisponibles); }
        public boolean requiereClaveApi() { return requiereClaveApi; }
        public int obtenerMaxTokensPorDefecto() { return maxTokensPorDefecto; }
        public int obtenerMaxTokensVentana() { return maxTokensVentana; }
    }

    private static final Map<String, ConfiguracionProveedor> PROVEEDORES = new LinkedHashMap<>();

    static {
        PROVEEDORES.put("Ollama", new ConfiguracionProveedor(
            "Ollama",
            "http://localhost:11434",
            "gemma3:12b",
            Arrays.asList(
                "gemma3:12b",
                "deepseek-v3.2",
                "phi4",
                "llama3.3", "llama3.2",
                "deepseek-r1", "deepseek-coder",
                "qwen2.5", "qwen2.5-coder",
                "mistral", "mixtral",
                "codellama"
            ),
            false,
            4096,
            32768
        ));

        PROVEEDORES.put("OpenAI", new ConfiguracionProveedor(
            "OpenAI",
            "https://api.openai.com/v1",
            "gpt-5.2-pro",
            Arrays.asList(
                "gpt-5.3-codex",
                "gpt-5.2-pro",
                "gpt-5.2-thinking",
                "gpt-5-mini",
                "gpt-4o", "gpt-4o-mini",
                "o1", "o1-mini", "o1-preview",
                "gpt-4-turbo", "gpt-3.5-turbo"
            ),
            true,
            4096,
            128000
        ));

        PROVEEDORES.put("Claude", new ConfiguracionProveedor(
            "Claude",
            "https://api.anthropic.com/v1",
            "claude-sonnet-4-6",
            Arrays.asList(
                "claude-sonnet-4-6",
                "claude-3-sonnet-20260217",
                "claude-3-opus-20260205",
                "claude-3-haiku-202511",
                "claude-3-5-sonnet-20241022",
                "claude-3-5-haiku-20241022",
                "claude-3-opus-20240229"
            ),
            true,
            8192,
            200000
        ));

        PROVEEDORES.put("Gemini", new ConfiguracionProveedor(
            "Gemini",
            "https://generativelanguage.googleapis.com/v1beta",
            "gemini-1.5-pro-002",
            Arrays.asList(
                "gemini-1.5-pro-002",
                "gemini-1.5-flash-002"
            ),
            true,
            8192,
            1000000
        ));

        PROVEEDORES.put("Z.ai", new ConfiguracionProveedor(
            "Z.ai",
            "https://api.z.ai/api/paas/v4",
            "glm-5",
            Arrays.asList(
                "glm-5",
                "glm-4.7-thinking",
                "glm-4.5v",
                "glm-4-plus", "glm-4-air", "glm-4-flash",
                "glm-4-long"
            ),
            true,
            4096,
            128000
        ));

        PROVEEDORES.put("minimax", new ConfiguracionProveedor(
            "minimax",
            "https://api.minimax.io/v1",
            "minimax-m2.5",
            Arrays.asList(
                "minimax-m2.5",
                "minimax-m2.5-lightning",
                "minimax-m2.1-codex",
                "abab6.5s-chat",
                "abab6-chat"
            ),
            true,
            4096,
            32000
        ));

        PROVEEDORES.put(PROVEEDOR_CUSTOM, new ConfiguracionProveedor(
            PROVEEDOR_CUSTOM,
            URL_CUSTOM_ES,
            "",
            Collections.emptyList(),
            false,
            4096,
            128000
        ));
    }

    public static ConfiguracionProveedor obtenerProveedor(String nombre) {
        return PROVEEDORES.get(nombre);
    }

    public static List<String> obtenerNombresProveedores() {
        return new ArrayList<>(PROVEEDORES.keySet());
    }

    public static boolean existeProveedor(String nombre) {
        return PROVEEDORES.containsKey(nombre);
    }

    public static String obtenerUrlApi(String nombreProveedor) {
        return obtenerUrlApiPorDefecto(nombreProveedor, null);
    }

    public static String obtenerUrlApiPorDefecto(String nombreProveedor, String idiomaUi) {
        if (PROVEEDOR_CUSTOM.equals(nombreProveedor)) {
            return "en".equalsIgnoreCase(idiomaUi) ? URL_CUSTOM_EN : URL_CUSTOM_ES;
        }
        ConfiguracionProveedor config = PROVEEDORES.get(nombreProveedor);
        return config != null ? config.obtenerUrlApi() : null;
    }

    public static String obtenerModeloPorDefecto(String nombreProveedor) {
        ConfiguracionProveedor config = PROVEEDORES.get(nombreProveedor);
        return config != null ? config.obtenerModeloPorDefecto() : null;
    }

    public static List<String> obtenerModelosDisponibles(String nombreProveedor) {
        ConfiguracionProveedor config = PROVEEDORES.get(nombreProveedor);
        return config != null ? config.obtenerModelosDisponibles() : new ArrayList<>();
    }
}
