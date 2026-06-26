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
 * Esta clase proporciona configuraciones predefinidas para múltiples
 * proveedores
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

    /** URL por defecto para proveedor custom en inglés (endpoint completo — verbatim). */
    public static final String URL_CUSTOM_EN = "https://YOUR_OPENAI_COMPATIBLE_BASE_URL/v1/chat/completions";

    /** URL por defecto para proveedor custom en español (endpoint completo — verbatim). */
    public static final String URL_CUSTOM_ES = "https://TU_BASE_URL_COMPATIBLE_CON_OPENAI/v1/chat/completions";

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
         * @param urlApi              URL base de la API del proveedor
         * @param modeloPorDefecto    Modelo a usar por defecto
         * @param modelosDisponibles  Lista de modelos disponibles
         * @param requiereClaveApi    Si el proveedor requiere API key
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
                "qwen3",
                Arrays.asList(
                        "qwen3",
                        "qwen3-coder",
                        "llama3.3",
                        "gemma3",
                        "deepseek-r1", "deepseek-v3.2",
                        "phi4",
                        "mistral"),
                false,
                4096));

        // Ollama Cloud — mismo formato que Ollama local (/api/chat con
        // messages array, response message.content) pero apunta al
        // servicio cloud (https://ollama.com) y requiere API key vía
        // Authorization: Bearer.
        PROVEEDORES.put("Ollama Cloud", new ConfiguracionProveedor(
                "https://ollama.com",
                "qwen3",
                Arrays.asList(
                        "qwen3",
                        "llama3.3",
                        "gemma3",
                        "deepseek-v3.2",
                        "deepseek-r1",
                        "mistral"),
                true,
                4096));

        PROVEEDORES.put("OpenAI", new ConfiguracionProveedor(
                "https://api.openai.com/v1",
                "gpt-5.5",
                Arrays.asList(
                        "gpt-5.5",
                        "gpt-5.5-pro",
                        "gpt-5.4",
                        "gpt-5.4-mini",
                        "gpt-5.4-nano",
                        "gpt-5.3-codex",
                        "gpt-5.2",
                        "gpt-4o", "gpt-4o-mini"),
                true,
                4096));

        PROVEEDORES.put("Claude", new ConfiguracionProveedor(
                "https://api.anthropic.com/v1",
                "claude-fable-5",
                Arrays.asList(
                        "claude-fable-5",
                        "claude-opus-4-8",
                        "claude-sonnet-4-6",
                        "claude-haiku-4-5-20251001",
                        "claude-3-5-sonnet-20241022",
                        "claude-3-5-haiku-20241022"),
                true,
                8192));

        PROVEEDORES.put("Gemini", new ConfiguracionProveedor(
                "https://generativelanguage.googleapis.com/v1beta",
                "gemini-3.5-flash",
                Arrays.asList(
                        "gemini-3.5-flash",
                        "gemini-3.1-pro-preview",
                        "gemini-3-flash-preview",
                        "gemini-3.1-flash-lite",
                        "gemini-2.5-pro",
                        "gemini-2.5-flash",
                        "gemini-2.5-flash-lite"),
                true,
                8192));

        PROVEEDORES.put("Z.ai", new ConfiguracionProveedor(
                "https://api.z.ai/api/paas/v4",
                "glm-5.2",
                Arrays.asList(
                        "glm-5.2",
                        "glm-5.1",
                        "glm-5-turbo",
                        "glm-5",
                        "glm-4.7",
                        "glm-4.6",
                        "glm-4.5-air",
                        "glm-4-air"),
                true,
                4096));

        PROVEEDORES.put("minimax", new ConfiguracionProveedor(
                "https://api.minimax.io/v1",
                "minimax-m3",
                Arrays.asList(
                        "minimax-m3",
                        "minimax-m2.7",
                        "minimax-m2.5",
                        "minimax-m2.5-highspeed",
                        "minimax-m2.1"),
                true,
                4096));

        PROVEEDORES.put("Moonshot (Kimi)", new ConfiguracionProveedor(
                "https://api.moonshot.ai/v1",
                "kimi-k2.6",
                Arrays.asList(
                        "kimi-k2.7",
                        "kimi-k2.6",
                        "kimi-k2.5",
                        "kimi-latest",
                        "moonshot-v1-128k",
                        "moonshot-v1-32k",
                        "moonshot-v1-8k"),
                true,
                4096));

        // DeepSeek — OpenAI-compatible. Base URL canónica: https://api.deepseek.com
        // Endpoint /chat/completions. Bearer auth.
        PROVEEDORES.put("DeepSeek", new ConfiguracionProveedor(
                "https://api.deepseek.com",
                "deepseek-v4-pro",
                Arrays.asList(
                        "deepseek-v4-pro",
                        "deepseek-v4-flash",
                        "deepseek-chat",
                        "deepseek-reasoner"),
                true,
                4096));

        // xAI Grok — OpenAI-compatible. Base URL canónica: https://api.x.ai/v1
        // Endpoint /chat/completions. Bearer auth.
        PROVEEDORES.put("xAI", new ConfiguracionProveedor(
                "https://api.x.ai/v1",
                "grok-4.3",
                Arrays.asList(
                        "grok-4.3",
                        "grok-4.20-0309-reasoning",
                        "grok-4.20-0309-non-reasoning",
                        "grok-build-0.1"),
                true,
                4096));

        // Sakana Fugu — OpenAI-compatible. Base URL canónica: https://api.sakana.ai/v1
        // Endpoint /chat/completions. Bearer auth. Modelos fugu y fugu-ultra.
        PROVEEDORES.put("Sakana", new ConfiguracionProveedor(
                "https://api.sakana.ai/v1",
                "fugu-ultra",
                Arrays.asList(
                        "fugu-ultra",
                        "fugu"),
                true,
                4096));

        // LM Studio — servidor LLM local (https://lmstudio.ai). Expone un API
        // OpenAI-compatible en http://localhost:1234/v1: endpoint
        // /chat/completions y listado de modelos en /models. No requiere API key
        // (es local). Los modelos dependen de lo que el usuario haya cargado en
        // la app: la carga remota (/models) lista los realmente disponibles, y
        // los defaults de abajo son marcadores de familias comunes (Qwen/Llama/
        // Phi) que LM Studio identifica por el nombre del .gguf cargado.
        // Soporta el endpoint OpenAI-compat (NO el /api/v1/chat nativo, que
        // tiene un formato de request/response distinto).
        PROVEEDORES.put("LM Studio", new ConfiguracionProveedor(
                "http://localhost:1234/v1",
                "qwen3-8b",
                Arrays.asList(
                        "qwen3-8b",
                        "qwen2.5-coder-7b-instruct",
                        "llama-3.3-8b-instruct",
                        "phi-4",
                        "mistral-7b-instruct",
                        "deepseek-r1-distill-qwen-7b"),
                false,
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
     * Indica si el proveedor requiere API key para autenticarse.
     * Consulta el flag {@code requiereClaveApi} del registro central.
     *
     * @param nombreProveedor nombre del proveedor (normalizado o no)
     * @return {@code true} si requiere clave; {@code false} si no, o si es desconocido.
     */
    public static boolean requiereClaveApi(String nombreProveedor) {
        String norm = normalizarProveedor(nombreProveedor);
        if (norm.isEmpty()) {
            return false;
        }
        ConfiguracionProveedor config = PROVEEDORES.get(norm);
        return config != null && config.requiereClaveApi();
    }

    /**
     * Indica si el proveedor requiere una URL base no-vacía para funcionar.
     * Hoy es {@code true} para todos los proveedores soportados (cada uno
     * necesita un endpoint para hablar). Se centraliza acá como abstracción
     * per-provider por si en el futuro algún provider gana URL hardcodeada
     * (ej. SDK con base inmutable) y debiera retornar {@code false}.
     *
     * @param nombreProveedor nombre del proveedor (normalizado o no)
     * @return {@code true} si requiere URL no-vacía; {@code false} si es desconocido.
     */
    public static boolean requiereUrlBase(String nombreProveedor) {
        String norm = normalizarProveedor(nombreProveedor);
        return !norm.isEmpty() && PROVEEDORES.containsKey(norm);
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
     * Indica si el proveedor permite consultar modelos remotamente desde la UI.
     *
     * @param proveedor Nombre del proveedor
     * @return {@code true} si la UI puede cargar modelos dinámicamente
     */
    public static boolean permiteCargaRemotaModelos(String proveedor) {
        String proveedorNormalizado = normalizarProveedor(proveedor);
        if (proveedorNormalizado.isEmpty()) {
            return true;
        }
        return !"minimax".equals(proveedorNormalizado)
                && !"Z.ai".equals(proveedorNormalizado);
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
     * @param idiomaUi        Código de idioma ("es" o "en")
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
     * @return Nombre del modelo por defecto, o {@code null} si el proveedor no
     *         existe
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
     * Conjunto canónico de proveedores cuya API es compatible con el formato
     * de OpenAI (request basado en {@code messages}/{@code input} y respuesta
     * basada en {@code choices[]}/{@code output}).
     * <p>
     * Centraliza la pertenencia a la "familia OpenAI" para aplicar el principio
     * DRY: la construcción de solicitudes, el parseo de respuestas y la
     * detección de contexto excedido consultan este mismo método en lugar de
     * repetir la lista de proveedores en cada sitio (evita que agregar un
     * proveedor nuevo deje rotos algunos caminos, como ocurrió con Moonshot).
     * </p>
     * <p>
     * Nota: OpenAI usa la Responses API ({@code /responses} con campo
     * {@code input}) mientras el resto usa {@code /chat/completions} con
     * {@code messages}; ambos sub-formatos los maneja el extractor de
     * contenido OpenAI, por eso OpenAI también se considera de esta familia.
     * Los proveedores custom canónicos son OpenAI-compatibles por contrato.
     * </p>
     *
     * @param nombreProveedor Nombre de proveedor (normalizado o no)
     * @return {@code true} si el proveedor habla el protocolo OpenAI-compatible
     */
    public static boolean esOpenAICompatible(String nombreProveedor) {
        String proveedorNormalizado = normalizarProveedor(nombreProveedor);
        if (proveedorNormalizado.isEmpty()) {
            return false;
        }
        switch (proveedorNormalizado) {
            case "OpenAI":
            case "Z.ai":
            case "minimax":
            case "DeepSeek":
            case "xAI":
            case "Sakana":
            case "Moonshot (Kimi)":
            case "LM Studio":
                return true;
            default:
                return esProveedorCustom(proveedorNormalizado);
        }
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
        for (String clave : PROVEEDORES.keySet()) {
            if (clave.equalsIgnoreCase(limpio)) {
                return clave;
            }
        }
        return limpio;
    }
}
