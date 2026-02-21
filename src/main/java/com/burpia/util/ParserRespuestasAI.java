package com.burpia.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Utilidad para parsear respuestas de APIs de IA de forma robusta.
 * Usa Gson para parsing en lugar de manipulación de strings manual.
 */
public class ParserRespuestasAI {
    private static final java.util.regex.Pattern PATRON_BLOQUES_PENSAMIENTO =
        java.util.regex.Pattern.compile("(?is)<\\s*(think|thinking)\\b[^>]*>.*?<\\s*/\\s*\\1\\s*>");

    /**
     * Extrae el contenido de texto de una respuesta de API de IA.
     * Soporta múltiples formatos: OpenAI, Claude, Gemini, Ollama.
     *
     * @param respuestaJson JSON crudo de la respuesta
     * @param proveedor Nombre del proveedor (OpenAI, Claude, Gemini, Ollama)
     * @return Contenido de texto extraído, o cadena vacía si no se puede extraer
     */
    public static String extraerContenido(String respuestaJson, String proveedor) {
        if (respuestaJson == null || respuestaJson.trim().isEmpty()) {
            return "";
        }

        try {
            JsonElement elemento = JsonParser.parseString(respuestaJson);

            if (!elemento.isJsonObject()) {
                return "";
            }

            JsonObject raiz = elemento.getAsJsonObject();
            String contenido = "";

            switch (proveedor) {
                case "Ollama":
                    contenido = extraerContenidoOllama(raiz);
                    break;

                case "OpenAI":
                case "Z.ai":
                case "minimax":
                    contenido = extraerContenidoOpenAI(raiz);
                    break;

                case "Claude":
                    contenido = extraerContenidoClaude(raiz);
                    break;

                case "Gemini":
                    contenido = extraerContenidoGemini(raiz);
                    break;

                default:
                    // Intentar genérico
                    contenido = extraerContenidoGenerico(raiz);
                    break;
            }

            // Limpiar caracteres de escape
            if (contenido != null && !contenido.isEmpty()) {
                contenido = contenido
                    .replace("\\n", " ")
                    .replace("\\t", " ")
                    .replace("\\\"", "\"")
                    .trim();
            }

            return limpiarBloquesPensamiento(contenido != null ? contenido : "");

        } catch (Exception e) {
            // Si falla el parsing JSON, retornar cadena vacía
            return "";
        }
    }

    /**
     * Formato Ollama: {"message": {"content": "..."}}
     */
    private static String extraerContenidoOllama(JsonObject raiz) {
        if (raiz.has("message")) {
            JsonObject message = raiz.getAsJsonObject("message");
            if (message.has("content")) {
                return message.get("content").getAsString();
            }
        }
        return "";
    }

    /**
     * Formato OpenAI/Z.ai/minimax: {"choices": [{"message": {"content": "..."}}]}
     */
    private static String extraerContenidoOpenAI(JsonObject raiz) {
        // OpenAI Responses API: {"output_text":"..."} o {"output":[{"content":[{"type":"output_text","text":"..."}]}]}
        if (raiz.has("output_text")) {
            String outputText = raiz.get("output_text").getAsString();
            if (outputText != null && !outputText.trim().isEmpty()) {
                return outputText;
            }
        }
        if (raiz.has("output")) {
            JsonElement outputElement = raiz.get("output");
            if (outputElement.isJsonArray()) {
                var output = outputElement.getAsJsonArray();
                if (output.size() > 0 && output.get(0).isJsonObject()) {
                    JsonObject first = output.get(0).getAsJsonObject();
                    if (first.has("content") && first.get("content").isJsonArray()) {
                        var content = first.getAsJsonArray("content");
                        if (content.size() > 0 && content.get(0).isJsonObject()) {
                            JsonObject firstContent = content.get(0).getAsJsonObject();
                            if (firstContent.has("text")) {
                                String text = firstContent.get("text").getAsString();
                                if (text != null && !text.trim().isEmpty()) {
                                    return text;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (raiz.has("choices")) {
            var choices = raiz.getAsJsonArray("choices");
            if (choices != null && choices.size() > 0) {
                JsonObject firstChoice = choices.get(0).getAsJsonObject();
                if (firstChoice.has("message")) {
                    JsonObject message = firstChoice.getAsJsonObject("message");
                    if (message.has("content")) {
                        String content = message.get("content").getAsString();
                        if (content != null && !content.trim().isEmpty()) {
                            return content;
                        }
                    }
                    // Algunos proveedores compatibles OpenAI (ej. Z.ai) pueden responder aquí
                    if (message.has("reasoning_content")) {
                        String reasoning = message.get("reasoning_content").getAsString();
                        if (reasoning != null && !reasoning.trim().isEmpty()) {
                            return reasoning;
                        }
                    }
                }

                if (firstChoice.has("text")) {
                    String text = firstChoice.get("text").getAsString();
                    if (text != null && !text.trim().isEmpty()) {
                        return text;
                    }
                }
            }
        }
        return "";
    }

    /**
     * Formato Claude: {"content": [{"type": "text", "text": "..."}]}
     * O formato alternativo: {"content": "..."}
     */
    private static String extraerContenidoClaude(JsonObject raiz) {
        if (raiz.has("content")) {
            JsonElement content = raiz.get("content");

            // Si es un array, buscar el primer bloque de texto
            if (content.isJsonArray()) {
                var contentArray = content.getAsJsonArray();
                for (JsonElement item : contentArray) {
                    if (item.isJsonObject()) {
                        JsonObject obj = item.getAsJsonObject();
                        if ("text".equals(obj.get("type").getAsString()) && obj.has("text")) {
                            return obj.get("text").getAsString();
                        }
                    }
                }
            }
            // Si es un string directo
            else if (content.isJsonPrimitive()) {
                return content.getAsString();
            }
        }
        return "";
    }

    /**
     * Formato Gemini: {"candidates": [{"content": {"parts": [{"text": "..."}]}}]}
     */
    private static String extraerContenidoGemini(JsonObject raiz) {
        if (raiz.has("candidates")) {
            var candidates = raiz.getAsJsonArray("candidates");
            if (candidates != null && candidates.size() > 0) {
                JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                if (firstCandidate.has("content")) {
                    JsonObject content = firstCandidate.getAsJsonObject("content");
                    if (content.has("parts")) {
                        var parts = content.getAsJsonArray("parts");
                        if (parts != null && parts.size() > 0) {
                            JsonObject firstPart = parts.get(0).getAsJsonObject();
                            if (firstPart.has("text")) {
                                return firstPart.get("text").getAsString();
                            }
                        }
                    }
                }
            }
        }
        return "";
    }

    /**
     * Intento genérico de extraer contenido buscando campos comunes.
     */
    private static String extraerContenidoGenerico(JsonObject raiz) {
        // Buscar campos comunes en orden de probabilidad
        String[] campos = {"content", "text", "message", "response", "output", "reasoning_content"};

        for (String campo : campos) {
            if (raiz.has(campo)) {
                JsonElement elemento = raiz.get(campo);
                if (elemento.isJsonPrimitive()) {
                    return elemento.getAsString();
                }
            }
        }

        return "";
    }

    /**
     * Valida si la respuesta contiene "OK" o "Hola" (case-insensitive).
     *
     * @param contenido Contenido de texto a validar
     * @return true si contiene "OK" o "Hola", false en caso contrario
     */
    public static boolean validarRespuestaPrueba(String contenido) {
        if (contenido == null || contenido.trim().isEmpty()) {
            return false;
        }

        String contenidoUpper = contenido.toUpperCase().trim();
        return contenidoUpper.contains("OK") || contenidoUpper.contains("HOLA");
    }

    /**
     * Validación flexible para pruebas de conexión:
     * si hay contenido no vacío, la conexión y el parseo son considerados correctos.
     */
    public static boolean validarRespuestaConexion(String contenido) {
        return contenido != null && !contenido.trim().isEmpty();
    }

    private static String limpiarBloquesPensamiento(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        String limpio = PATRON_BLOQUES_PENSAMIENTO.matcher(texto).replaceAll(" ");
        return limpio.replaceAll("\\s+", " ").trim();
    }
}
