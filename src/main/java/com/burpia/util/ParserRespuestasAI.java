package com.burpia.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;

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

            String proveedorNormalizado = proveedor != null ? proveedor : "";

            switch (proveedorNormalizado) {
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
                    contenido = extraerContenidoGenerico(raiz);
                    break;
            }

            if (contenido != null && !contenido.isEmpty()) {
                contenido = contenido
                    .replace("\\n", " ")
                    .replace("\\t", " ")
                    .replace("\\\"", "\"")
                    .trim();
            }

            return limpiarBloquesPensamiento(contenido != null ? contenido : "");

        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Formato Ollama: {"message": {"content": "..."}}
     */
    private static String extraerContenidoOllama(JsonObject raiz) {
        JsonObject message = obtenerObjeto(raiz, "message");
        String contenido = obtenerTexto(message, "content");
        if (!contenido.isEmpty()) {
            return contenido;
        }
        return "";
    }

    /**
     * Formato OpenAI/Z.ai/minimax: {"choices": [{"message": {"content": "..."}}]}
     */
    private static String extraerContenidoOpenAI(JsonObject raiz) {
        String outputText = obtenerTexto(raiz, "output_text");
        if (!outputText.isEmpty()) {
            return outputText;
        }

        JsonArray output = obtenerArreglo(raiz, "output");
        if (output != null) {
            for (JsonElement outputItem : output) {
                JsonObject outputObject = obtenerObjeto(outputItem);
                JsonArray contenidos = obtenerArreglo(outputObject, "content");
                if (contenidos == null) {
                    continue;
                }
                for (JsonElement contenidoItem : contenidos) {
                    JsonObject contenidoObjeto = obtenerObjeto(contenidoItem);
                    String texto = obtenerTexto(contenidoObjeto, "text");
                    if (!texto.isEmpty()) {
                        return texto;
                    }
                }
            }
        }

        JsonArray choices = obtenerArreglo(raiz, "choices");
        if (choices != null) {
            for (JsonElement choiceItem : choices) {
                JsonObject choice = obtenerObjeto(choiceItem);
                if (choice == null) {
                    continue;
                }

                JsonObject message = obtenerObjeto(choice, "message");
                String content = obtenerTexto(message, "content");
                if (!content.isEmpty()) {
                    return content;
                }
                String contentPartes = extraerTextoDesdeArreglo(obtenerArreglo(message, "content"));
                if (!contentPartes.isEmpty()) {
                    return contentPartes;
                }

                String reasoning = obtenerTexto(message, "reasoning_content");
                if (!reasoning.isEmpty()) {
                    return reasoning;
                }

                String text = obtenerTexto(choice, "text");
                if (!text.isEmpty()) {
                    return text;
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

            if (content.isJsonArray()) {
                var contentArray = content.getAsJsonArray();
                for (JsonElement item : contentArray) {
                    JsonObject obj = obtenerObjeto(item);
                    if (obj == null) {
                        continue;
                    }
                    String tipo = obtenerTexto(obj, "type");
                    String texto = obtenerTexto(obj, "text");
                    if (!texto.isEmpty() && ("text".equals(tipo) || tipo.isEmpty())) {
                        return texto;
                    }
                }
            }
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
        JsonArray candidates = obtenerArreglo(raiz, "candidates");
        if (candidates == null) {
            return "";
        }
        for (JsonElement candidateItem : candidates) {
            JsonObject candidate = obtenerObjeto(candidateItem);
            JsonObject content = obtenerObjeto(candidate, "content");
            JsonArray parts = obtenerArreglo(content, "parts");
            if (parts == null) {
                continue;
            }
            for (JsonElement partItem : parts) {
                JsonObject part = obtenerObjeto(partItem);
                String text = obtenerTexto(part, "text");
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return "";
    }

    /**
     * Intento genérico de extraer contenido buscando campos comunes.
     */
    private static String extraerContenidoGenerico(JsonObject raiz) {
        String[] campos = {"content", "text", "message", "response", "output", "reasoning_content"};

        for (String campo : campos) {
            JsonElement elemento = obtenerElemento(raiz, campo);
            if (elemento == null) {
                continue;
            }

            if (elemento.isJsonPrimitive()) {
                String valor = obtenerTexto(elemento);
                if (!valor.isEmpty()) {
                    return valor;
                }
            } else if (elemento.isJsonObject()) {
                JsonObject objeto = elemento.getAsJsonObject();
                String content = obtenerTexto(objeto, "content");
                if (!content.isEmpty()) {
                    return content;
                }
                String text = obtenerTexto(objeto, "text");
                if (!text.isEmpty()) {
                    return text;
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

    private static JsonElement obtenerElemento(JsonObject objeto, String campo) {
        if (objeto == null || campo == null || !objeto.has(campo)) {
            return null;
        }
        JsonElement elemento = objeto.get(campo);
        if (elemento == null || elemento.isJsonNull()) {
            return null;
        }
        return elemento;
    }

    private static JsonObject obtenerObjeto(JsonObject objeto, String campo) {
        return obtenerObjeto(obtenerElemento(objeto, campo));
    }

    private static JsonObject obtenerObjeto(JsonElement elemento) {
        if (elemento == null || !elemento.isJsonObject()) {
            return null;
        }
        return elemento.getAsJsonObject();
    }

    private static JsonArray obtenerArreglo(JsonObject objeto, String campo) {
        return obtenerArreglo(obtenerElemento(objeto, campo));
    }

    private static JsonArray obtenerArreglo(JsonElement elemento) {
        if (elemento == null || !elemento.isJsonArray()) {
            return null;
        }
        return elemento.getAsJsonArray();
    }

    private static String obtenerTexto(JsonObject objeto, String campo) {
        return obtenerTexto(obtenerElemento(objeto, campo));
    }

    private static String obtenerTexto(JsonElement elemento) {
        if (elemento == null || !elemento.isJsonPrimitive()) {
            return "";
        }
        try {
            String valor = elemento.getAsString();
            return valor != null ? valor : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String extraerTextoDesdeArreglo(JsonArray arreglo) {
        if (arreglo == null) {
            return "";
        }
        for (JsonElement item : arreglo) {
            String directo = obtenerTexto(item);
            if (!directo.isEmpty()) {
                return directo;
            }
            JsonObject objeto = obtenerObjeto(item);
            if (objeto == null) {
                continue;
            }
            String text = obtenerTexto(objeto, "text");
            if (!text.isEmpty()) {
                return text;
            }
            String content = obtenerTexto(objeto, "content");
            if (!content.isEmpty()) {
                return content;
            }
        }
        return "";
    }
}
