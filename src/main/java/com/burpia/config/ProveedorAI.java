package com.burpia.config;

import com.burpia.util.Normalizador;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro centralizado de proveedores de IA soportados por BurpIA.
 * <p>
 * Esta clase proporciona configuraciones predefinidas para múltiples proveedores
 * de IA, incluyendo URLs de API, modelos disponibles y parámetros por defecto.
 * </p>
 *
 * @see ConfiguracionProveedor
 * @see Normalizador
 */
public final class ProveedorAI {

    /** Proveedor custom canónico #1 (OpenAI-compatible) */
    public static final String PROVEEDOR_CUSTOM_01 = "-- Custom 01 --";

    /** Proveedor custom canónico #2 (OpenAI-compatible) */
    public static final String PROVEEDOR_CUSTOM_02 = "-- Custom 02 --";

    /** Proveedor custom canónico #3 (OpenAI-compatible) */
    public static final String PROVEEDOR_CUSTOM_03 = "-- Custom 03 --";

    /** URL por defecto para proveedor custom en inglés */
    public static final String URL_CUSTOM_EN = "https://YOUR_OPENAI_COMPATIBLE_BASE_URL/v1";

    /** URL por defecto para proveedor custom en español */
    public static final String URL_CUSTOM_ES = "https://TU_BASE_URL_COMPATIBLE_CON_OPENAI/v1";

    /**
     * Configuración de un proveedor de IA.
     * <p>
     * Encapsula todos los parámetros necesarios para conectar con un proveedor
     * de IA específico.
     * </p>
     */
    public static final class ConfiguracionProveedor {
        private final String urlApi;
        private final String modeloPorDefecto;
        private final List<String> modelosDisponibles;
        private final boolean requiereClaveApi;
        private final int maxTokensPorDefecto;

        /**
         * Construye una configuración de proveedor.
         *
         * @param urlApi URL base de la API del proveedor
         * @param modeloPorDefecto Modelo a usar por defecto
         * @param modelosDisponibles Lista de modelos disponibles
         * @param requiereClaveApi Si el proveedor requiere API key
         * @param maxTokensPorDefecto Límite de tokens por defecto
         */
        public ConfiguracionProveedor(String urlApi, String modeloPorDefecto,
                List<String> modelosDisponibles, boolean requiereClaveApi,
                int maxTokensPorDefecto) {
            this.urlApi = urlApi;
            this.modeloPorDefecto = modeloPorDefecto;
            this.modelosDisponibles = modelosDisponibles != null
                    ? new ArrayList<>(modelosDisponibles)
                    : new ArrayList<>();
            this.requiereClaveApi = requiereClaveApi;
            this.maxTokensPorDefecto = maxTokensPorDefecto;
        }

        /**
         * Obtiene la URL base de la API del proveedor.
         *
         * @return URL de la API
         */
        public String obtenerUrlApi() {
            return urlApi;
        }

        /**
         * Obtiene el modelo por defecto para este proveedor.
         *
         * @return Nombre del modelo por defecto
         */
        public String obtenerModeloPorDefecto() {
            return modeloPorDefecto;
        }

        /**
         * Obtiene la lista de modelos disponibles para este proveedor.
         * <p>
         * Retorna una copia defensiva para preservar la inmutabilidad.
         * </p>
         *
         * @return Copia de la lista de modelos disponibles
         */
        public List<String> obtenerModelosDisponibles() {
            return new ArrayList<>(modelosDisponibles);
        }

        /**
         * Indica si este proveedor requiere API key para autenticación.
         *
         * @return {@code true} si requiere API key, {@code false} en caso contrario
         */
        public boolean requiereClaveApi() {
            return requiereClaveApi;
        }

        /**
         * Obtiene el límite de tokens por defecto para este proveedor.
         *
         * @return Máximo de tokens por defecto
         */
        public int obtenerMaxTokensPorDefecto() {
            return maxTokensPorDefecto;
        }
    }

    /** Registro de proveedores disponibles, ordenado por inserción */
    private static final Map<String, ConfiguracionProveedor> PROVEEDORES = new LinkedHashMap<>();

    static {
        PROVEEDORES.put("Ollama", new ConfiguracionProveedor(
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
                        "codellama"),
                false,
                4096));

        PROVEEDORES.put("OpenAI", new ConfiguracionProveedor(
                "https://api.openai.com/v1",
                "gpt-5.2-pro",
                Arrays.asList(
                        "gpt-5.3-codex",
                        "gpt-5.2-pro",
                        "gpt-5.2-thinking",
                        "gpt-5-mini",
                        "gpt-4o", "gpt-4o-mini",
                        "o1", "o1-mini", "o1-preview",
                        "gpt-4-turbo", "gpt-3.5-turbo"),
                true,
                4096));

        PROVEEDORES.put("Claude", new ConfiguracionProveedor(
                "https://api.anthropic.com/v1",
                "claude-sonnet-4-6",
                Arrays.asList(
                        "claude-sonnet-4-6",
                        "claude-3-sonnet-20260217",
                        "claude-3-opus-20260205",
                        "claude-3-haiku-202511",
                        "claude-3-5-sonnet-20241022",
                        "claude-3-5-haiku-20241022",
                        "claude-3-opus-20240229"),
                true,
                8192));

        PROVEEDORES.put("Gemini", new ConfiguracionProveedor(
                "https://generativelanguage.googleapis.com/v1beta",
                "gemini-1.5-pro-002",
                Arrays.asList(
                        "gemini-1.5-pro-002",
                        "gemini-1.5-flash-002"),
                true,
                8192));

        PROVEEDORES.put("Z.ai", new ConfiguracionProveedor(
                "https://api.z.ai/api/paas/v4",
                "glm-5",
                Arrays.asList(
                        "glm-5",
                        "glm-4.7-thinking",
                        "glm-4.5v",
                        "glm-4-plus", "glm-4-air", "glm-4-flash",
                        "glm-4-long"),
                true,
                4096));

        PROVEEDORES.put("minimax", new ConfiguracionProveedor(
                "https://api.minimax.io/v1",
                "minimax-m2.5",
                Arrays.asList(
                        "minimax-m2.5",
                        "minimax-m2.5-lightning",
                        "minimax-m2.1-codex",
                        "abab6.5s-chat",
                        "abab6-chat"),
                true,
                4096));

        PROVEEDORES.put("Moonshot (Kimi)", new ConfiguracionProveedor(
                "https://api.moonshot.ai/v1",
                "kimi-k2.5",
                Arrays.asList(
                        "kimi-k2.5",
                        "kimi-latest",
                        "kimi-k2-thinking",
                        "kimi-k2-thinking-turbo",
                        "kimi-k2-0905",
                        "kimi-k2-turbo",
                        "moonshot-v1-128k",
                        "moonshot-v1-128k-vision",
                        "moonshot-v1-32k",
                        "moonshot-v1-32k-vision",
                        "moonshot-v1-8k",
                        "moonshot-v1-8k-vision",
                        "moonshot-v1-auto"),
                true,
                4096));

        PROVEEDORES.put(PROVEEDOR_CUSTOM_01, new ConfiguracionProveedor(
                URL_CUSTOM_ES,
                "",
                Collections.emptyList(),
                false,
                4096));

        PROVEEDORES.put(PROVEEDOR_CUSTOM_02, new ConfiguracionProveedor(
                URL_CUSTOM_ES,
                "",
                Collections.emptyList(),
                false,
                4096));

        PROVEEDORES.put(PROVEEDOR_CUSTOM_03, new ConfiguracionProveedor(
                URL_CUSTOM_ES,
                "",
                Collections.emptyList(),
                false,
                4096));
    }

    /**
     * Constructor privado para evitar instanciación.
     * <p>
     * Esta es una clase de utilidad con solo métodos estáticos.
     * </p>
     */
    private ProveedorAI() {
        // Clase de utilidad, no instanciable
    }

    /**
     * Obtiene la configuración completa de un proveedor por su nombre.
     *
     * @param nombre Nombre del proveedor (ej: "OpenAI", "Claude", "Gemini")
     * @return Configuración del proveedor, o {@code null} si no existe
     */
    public static ConfiguracionProveedor obtenerProveedor(String nombre) {
        String proveedorNormalizado = normalizarProveedor(nombre);
        if (proveedorNormalizado.isEmpty()) {
            return null;
        }
        return PROVEEDORES.get(proveedorNormalizado);
    }

    /**
     * Obtiene la lista de nombres de todos los proveedores disponibles.
     * <p>
     * Los proveedores custom canónicos ({@link #PROVEEDOR_CUSTOM_01},
     * {@link #PROVEEDOR_CUSTOM_02}, {@link #PROVEEDOR_CUSTOM_03})
     * siempre se publican al final y en ese orden.
     * </p>
     *
     * @return Lista de nombres de proveedores
     */
    public static List<String> obtenerNombresProveedores() {
        return new ArrayList<>(PROVEEDORES.keySet());
    }

    /**
     * Verifica si existe un proveedor con el nombre especificado.
     *
     * @param nombre Nombre del proveedor a verificar
     * @return {@code true} si el proveedor existe, {@code false} en caso contrario
     */
    public static boolean existeProveedor(String nombre) {
        String proveedorNormalizado = normalizarProveedor(nombre);
        if (proveedorNormalizado.isEmpty()) {
            return false;
        }
        return PROVEEDORES.containsKey(proveedorNormalizado);
    }

    /**
     * Obtiene la URL de API por defecto para un proveedor.
     * <p>
     * Para el proveedor custom, retorna la URL según el idioma de UI especificado.
     * </p>
     *
     * @param nombreProveedor Nombre del proveedor
     * @param idiomaUi Código de idioma ("es" o "en")
     * @return URL de API por defecto, o {@code null} si el proveedor no existe
     */
    public static String obtenerUrlApiPorDefecto(String nombreProveedor, String idiomaUi) {
        String proveedorNormalizado = normalizarProveedor(nombreProveedor);
        if (proveedorNormalizado.isEmpty()) {
            return null;
        }
        if (esProveedorCustom(proveedorNormalizado)) {
            return "en".equalsIgnoreCase(idiomaUi) ? URL_CUSTOM_EN : URL_CUSTOM_ES;
        }
        ConfiguracionProveedor config = PROVEEDORES.get(proveedorNormalizado);
        return config != null ? config.obtenerUrlApi() : null;
    }

    /**
     * Obtiene el modelo por defecto para un proveedor.
     *
     * @param nombreProveedor Nombre del proveedor
     * @return Nombre del modelo por defecto, o {@code null} si el proveedor no existe
     */
    public static String obtenerModeloPorDefecto(String nombreProveedor) {
        String proveedorNormalizado = normalizarProveedor(nombreProveedor);
        if (proveedorNormalizado.isEmpty()) {
            return null;
        }
        ConfiguracionProveedor config = PROVEEDORES.get(proveedorNormalizado);
        return config != null ? config.obtenerModeloPorDefecto() : null;
    }

    /**
     * Obtiene la lista de modelos disponibles para un proveedor.
     * <p>
     * Retorna una lista vacía inmutable si el proveedor no existe.
     * </p>
     *
     * @param nombreProveedor Nombre del proveedor
     * @return Lista de modelos disponibles, o lista vacía si no existe
     */
    public static List<String> obtenerModelosDisponibles(String nombreProveedor) {
        String proveedorNormalizado = normalizarProveedor(nombreProveedor);
        if (proveedorNormalizado.isEmpty()) {
            return Collections.emptyList();
        }
        ConfiguracionProveedor config = PROVEEDORES.get(proveedorNormalizado);
        return config != null ? config.obtenerModelosDisponibles() : Collections.emptyList();
    }

    /**
     * Indica si un nombre corresponde a cualquier proveedor custom soportado.
     *
     * @param nombreProveedor Nombre de proveedor
     * @return {@code true} si es un proveedor custom
     */
    public static boolean esProveedorCustom(String nombreProveedor) {
        String proveedorNormalizado = normalizarProveedor(nombreProveedor);
        if (proveedorNormalizado.isEmpty()) {
            return false;
        }
        return PROVEEDOR_CUSTOM_01.equals(proveedorNormalizado)
            || PROVEEDOR_CUSTOM_02.equals(proveedorNormalizado)
            || PROVEEDOR_CUSTOM_03.equals(proveedorNormalizado);
    }

    /**
     * Normaliza el nombre de proveedor recibido desde UI/config.
     *
     * @param nombreProveedor Nombre recibido desde UI/config
     * @return nombre canónico, o cadena vacía si entrada inválida
     */
    public static String normalizarProveedor(String nombreProveedor) {
        if (Normalizador.esVacio(nombreProveedor)) {
            return "";
        }
        String limpio = nombreProveedor.trim();
        return limpio;
    }
}
