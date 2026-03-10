package com.burpia.analyzer;

import com.burpia.util.Normalizador;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detector especializado de errores de contexto excedido por proveedor.
 *
 * <p>Cada proveedor AI tiene diferentes mensajes de error cuando el prompt
 * excede el límite de tokens del modelo. Esta clase centraliza la detección
 * para permitir retry con truncado automático.</p>
 *
 * <h3>Ejemplos de errores por proveedor:</h3>
 * <ul>
 *   <li><b>OpenAI:</b> "context_length_exceeded", "This model's maximum context length is X tokens"</li>
 *   <li><b>Claude:</b> "model_context_window_exceeded", "prompt is too long: X tokens"</li>
 *   <li><b>Gemini:</b> "RESOURCE_EXHAUSTED", "Request token count exceeds limit"</li>
 *   <li><b>Z.ai:</b> "Input token length too long"</li>
 *   <li><b>Moonshot:</b> "Input token length too long", "exceeded model token limit"</li>
 *   <li><b>Ollama:</b> Error local sin patrón específico</li>
 * </ul>
 *
 * @since 1.5.0
 */
public class ContextExceededDetector {

    /** HTTP status code típico para errores de contexto */
    private static final int HTTP_BAD_REQUEST = 400;

    /** HTTP status code para errores de recursos agotados (Gemini) */
    private static final int HTTP_RESOURCE_EXHAUSTED = 429;

    /** Patrones comunes para proveedores custom (DRY - reutilizados) */
    private static final Pattern[] PATRONES_CUSTOM = new Pattern[]{
            Pattern.compile("context_length_exceeded", Pattern.CASE_INSENSITIVE),
            Pattern.compile("token.*limit.*exceed", Pattern.CASE_INSENSITIVE)
    };

    /**
     * Patrones de error por proveedor.
     * Clave: nombre normalizado del proveedor (Title Case)
     * Valor: array de patrones regex (case-insensitive)
     */
    private static final Map<String, Pattern[]> PATRONES_ERROR = Map.ofEntries(
            // OpenAI: múltiples variantes de mensaje
            Map.entry("OpenAI", new Pattern[]{
                    Pattern.compile("context_length_exceeded", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("maximum context length", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("this model.*maximum context length.*tokens", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("prompt is too long.*tokens", Pattern.CASE_INSENSITIVE)
            }),

            // Claude/Anthropic
            Map.entry("Claude", new Pattern[]{
                    Pattern.compile("model_context_window_exceeded", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("prompt is too long", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("context window", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("max_context_window", Pattern.CASE_INSENSITIVE)
            }),

            // Gemini (Google)
            Map.entry("Gemini", new Pattern[]{
                    Pattern.compile("RESOURCE_EXHAUSTED", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("token count exceeds limit", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("request.*too large", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("context.*exceeded", Pattern.CASE_INSENSITIVE)
            }),

            // Z.ai (GLM)
            Map.entry("Z.ai", new Pattern[]{
                    Pattern.compile("Input token length too long", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("token limit", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("exceed.*limit", Pattern.CASE_INSENSITIVE)
            }),

            // MiniMax
            Map.entry("Minimax", new Pattern[]{
                    Pattern.compile("token.*limit.*exceed", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("context.*too.*long", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("input.*too.*large", Pattern.CASE_INSENSITIVE)
            }),

            // Moonshot (Kimi)
            Map.entry("Moonshot (Kimi)", new Pattern[]{
                    Pattern.compile("Input token length too long", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("exceeded model token limit", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("token.*exceed", Pattern.CASE_INSENSITIVE)
            }),

            // Ollama (local, mensajes variables)
            Map.entry("Ollama", new Pattern[]{
                    Pattern.compile("context.*length.*exceed", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("prompt.*too.*long", Pattern.CASE_INSENSITIVE),
                    Pattern.compile("token.*limit", Pattern.CASE_INSENSITIVE)
            }),

            // Custom providers (usan patrones comunes - DRY)
            Map.entry("Custom 01", PATRONES_CUSTOM),
            Map.entry("Custom 02", PATRONES_CUSTOM),
            Map.entry("Custom 03", PATRONES_CUSTOM)
    );

    /**
     * Patrón para extraer el límite de tokens del mensaje de error.
     * Busca números seguidos de "tokens" o "context length".
     */
    private static final Pattern PATRON_EXTRAER_LIMITE = 
            Pattern.compile("(\\d+)\\s*(?:tokens?|context)", Pattern.CASE_INSENSITIVE);

    /**
     * Determina si un error HTTP es de contexto excedido.
     *
     * @param proveedor    Nombre del proveedor (ej: "OpenAI", "Claude")
     * @param statusCode   Código HTTP del error
     * @param mensajeError Mensaje de error del proveedor
     * @return {@code true} si es un error de contexto excedido, {@code false} en caso contrario
     */
    public boolean esErrorContextoExcedido(String proveedor, int statusCode, String mensajeError) {
        if (Normalizador.esVacio(mensajeError)) {
            return false;
        }

        // Verificar status code (típicamente 400 o 429)
        if (statusCode != HTTP_BAD_REQUEST && statusCode != HTTP_RESOURCE_EXHAUSTED) {
            return false;
        }

        String proveedorNormalizado = normalizarProveedor(proveedor);
        Pattern[] patrones = PATRONES_ERROR.get(proveedorNormalizado);

        if (patrones == null) {
            // Proveedor desconocido: buscar patrones genéricos
            return contienePatronGenerico(mensajeError);
        }

        for (Pattern patron : patrones) {
            if (patron.matcher(mensajeError).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extrae el límite de tokens del mensaje de error si está disponible.
     *
     * @param mensajeError Mensaje de error del proveedor
     * @return Límite de tokens extraído, o -1 si no se puede extraer
     */
    public int extraerLimiteTokens(String mensajeError) {
        if (Normalizador.esVacio(mensajeError)) {
            return -1;
        }

        Matcher matcher = PATRON_EXTRAER_LIMITE.matcher(mensajeError);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        return -1;
    }

    /**
     * Busca patrones genéricos de error de contexto para proveedores desconocidos.
     *
     * @param mensajeError Mensaje de error a analizar
     * @return {@code true} si contiene patrones genéricos de contexto excedido
     */
    private boolean contienePatronGenerico(String mensajeError) {
        if (Normalizador.esVacio(mensajeError)) {
            return false;
        }

        String mensajeLower = mensajeError.toLowerCase();
        return (mensajeLower.contains("context") && mensajeLower.contains("exceed"))
                || (mensajeLower.contains("token") && mensajeLower.contains("limit"))
                || (mensajeLower.contains("prompt") && mensajeLower.contains("too long"))
                || mensajeLower.contains("context_length_exceeded")
                || mensajeLower.contains("resource_exhausted");
    }

    /**
     * Normaliza el nombre del proveedor para coincidir con las claves del mapa.
     *
     * <p>Convierte a Title Case y maneja variantes comunes de nombres.</p>
     *
     * @param proveedor Nombre del proveedor (ej: "openai", "CLAUDE", "custom 01")
     * @return Nombre normalizado (ej: "OpenAI", "Claude", "Custom 01") o string vacío si es null
     */
    private String normalizarProveedor(String proveedor) {
        if (Normalizador.esVacio(proveedor)) {
            return "";
        }

        String normalizado = proveedor.trim();

        // Manejar variantes de nombres conocidas
        if (normalizado.equalsIgnoreCase("anthropic") || normalizado.equalsIgnoreCase("claude")) {
            return "Claude";
        }
        if (normalizado.equalsIgnoreCase("google") || normalizado.equalsIgnoreCase("gemini")) {
            return "Gemini";
        }
        if (normalizado.equalsIgnoreCase("openai")) {
            return "OpenAI";
        }
        if (normalizado.equalsIgnoreCase("ollama")) {
            return "Ollama";
        }
        if (normalizado.equalsIgnoreCase("minimax")) {
            return "Minimax";
        }
        if (normalizado.equalsIgnoreCase("z.ai") || normalizado.equalsIgnoreCase("zai")) {
            return "Z.ai";
        }
        if (normalizado.equalsIgnoreCase("moonshot") || normalizado.equalsIgnoreCase("moonshot (kimi)")) {
            return "Moonshot (Kimi)";
        }

        // Custom providers: capitalizar correctamente (custom 01 → Custom 01)
        String lower = normalizado.toLowerCase();
        if (lower.startsWith("custom")) {
            return capitalizarCustomProvider(normalizado);
        }

        // Para proveedores desconocidos, retornar Title Case simple
        return capitalizarPrimeraLetra(normalizado);
    }

    /**
     * Capitaliza un proveedor Custom en formato "Custom NN".
     * Ejemplos: "custom 01" → "Custom 01", "CUSTOM 02" → "Custom 02"
     */
    private String capitalizarCustomProvider(String proveedor) {
        String lower = proveedor.toLowerCase();
        if (lower.startsWith("custom ")) {
            String numero = lower.substring(7).trim(); // Después de "custom "
            return "Custom " + numero;
        }
        return "Custom";
    }

    /**
     * Capitaliza la primera letra de un string.
     */
    private String capitalizarPrimeraLetra(String texto) {
        if (Normalizador.esVacio(texto)) {
            return texto;
        }
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }
}
