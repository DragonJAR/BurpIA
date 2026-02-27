package com.burpia.util;
import com.burpia.config.ProveedorAI;
import com.burpia.i18n.I18nLogs;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ParserRespuestasAI {
    private static final java.util.regex.Pattern PATRON_BLOQUES_PENSAMIENTO =
        java.util.regex.Pattern.compile("(?is)<\\s*(think|thinking)\\b[^>]*>.*?<\\s*/\\s*\\1\\s*>");
    private static final Logger LOGGER = Logger.getLogger(ParserRespuestasAI.class.getName());

    private static final java.util.regex.Pattern PATRON_CAMPO_TITULO_NO_ESTRICTO = java.util.regex.Pattern.compile(
        "\"(?:titulo|title|name|nombre)\"\\s*:\\s*\"(.*?)(?=\"\\s*(?:,\\s*\"|\\}))",
        java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE
    );
    private static final java.util.regex.Pattern PATRON_CAMPO_DESCRIPCION_NO_ESTRICTO = java.util.regex.Pattern.compile(
        "\"(?:descripcion|description|hallazgo|finding|details|detalle)\"\\s*:\\s*\"(.*?)(?=\"\\s*(?:,\\s*\"|\\}))",
        java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE
    );
    private static final java.util.regex.Pattern PATRON_CAMPO_SEVERIDAD_NO_ESTRICTO = java.util.regex.Pattern.compile(
        "\"(?:severidad|severity|risk|impacto)\"\\s*:\\s*\"(.*?)(?=\"\\s*(?:,\\s*\"|\\}))",
        java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE
    );
    private static final java.util.regex.Pattern PATRON_CAMPO_CONFIANZA_NO_ESTRICTO = java.util.regex.Pattern.compile(
        "\"(?:confianza|confidence|certainty|certeza)\"\\s*:\\s*\"(.*?)(?=\"\\s*(?:,\\s*\"|\\}))",
        java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE
    );
    private static final java.util.regex.Pattern PATRON_CAMPO_EVIDENCIA_NO_ESTRICTO = java.util.regex.Pattern.compile(
        "\"(?:evidencia|evidence|proof|indicator)\"\\s*:\\s*\"(.*?)(?=\"\\s*(?:,\\s*\"|\\}))",
        java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE
    );

    public static String extraerCampoNoEstricto(String campo, String contenido) {
        if (contenido == null || contenido.isEmpty()) {
            return "";
        }
        java.util.regex.Pattern patron;
        switch (normalizarClaveCampoNoEstricto(campo)) {
            case "titulo": patron = PATRON_CAMPO_TITULO_NO_ESTRICTO; break;
            case "descripcion": patron = PATRON_CAMPO_DESCRIPCION_NO_ESTRICTO; break;
            case "severidad": patron = PATRON_CAMPO_SEVERIDAD_NO_ESTRICTO; break;
            case "confianza": patron = PATRON_CAMPO_CONFIANZA_NO_ESTRICTO; break;
            case "evidencia": patron = PATRON_CAMPO_EVIDENCIA_NO_ESTRICTO; break;
            default: return "";
        }
        java.util.regex.Matcher matcher = patron.matcher(contenido);
        if (matcher.find()) {
            return normalizarCampoNoEstricto(matcher.group(1));
        }
        return "";
    }

    private static String normalizarClaveCampoNoEstricto(String campo) {
        if (campo == null) {
            return "";
        }
        String campoNormalizado = campo.trim().toLowerCase(java.util.Locale.ROOT);
        switch (campoNormalizado) {
            case "titulo":
            case "title":
            case "name":
            case "nombre":
                return "titulo";
            case "descripcion":
            case "description":
            case "hallazgo":
            case "finding":
            case "details":
            case "detalle":
                return "descripcion";
            case "severidad":
            case "severity":
            case "risk":
            case "impacto":
                return "severidad";
            case "confianza":
            case "confidence":
            case "certainty":
            case "certeza":
                return "confianza";
            case "evidencia":
            case "evidence":
            case "proof":
            case "indicator":
                return "evidencia";
            default:
                return "";
        }
    }

    public static String normalizarCampoNoEstricto(String valor) {
        return Normalizador.normalizarTexto(valor);
    }

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
                case ProveedorAI.PROVEEDOR_CUSTOM:
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
                contenido = normalizarContenidoExtraido(contenido);
            }

            return limpiarBloquesPensamiento(contenido != null ? contenido : "");

        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, I18nLogs.tr("No se pudo parsear la respuesta JSON del proveedor"), e);
            }
            return limpiarBloquesPensamiento(respuestaJson.trim());
        }
    }

    private static String extraerContenidoOllama(JsonObject raiz) {
        JsonObject message = obtenerObjeto(raiz, "message");
        String contenido = obtenerTexto(message, "content");
        if (!contenido.isEmpty()) {
            return contenido;
        }
        return "";
    }

    private static String extraerContenidoOpenAI(JsonObject raiz) {
        String outputText = obtenerTexto(raiz, "output_text");
        if (!outputText.isEmpty()) {
            return outputText;
        }

        JsonArray output = obtenerArreglo(raiz, "output");
        if (output != null) {
            StringBuilder contenidoCompleto = new StringBuilder();
            for (JsonElement outputItem : output) {
                JsonObject outputObject = obtenerObjeto(outputItem);
                JsonArray contenidos = obtenerArreglo(outputObject, "content");
                if (contenidos == null) {
                    continue;
                }
                anexarTexto(contenidoCompleto, extraerTextoDesdeArreglo(contenidos));
            }
            if (contenidoCompleto.length() > 0) {
                return contenidoCompleto.toString();
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

    private static String extraerContenidoClaude(JsonObject raiz) {
        if (raiz.has("content")) {
            JsonElement content = raiz.get("content");

            if (content.isJsonArray()) {
                var contentArray = content.getAsJsonArray();
                StringBuilder contenidoCompleto = new StringBuilder();
                for (JsonElement item : contentArray) {
                    JsonObject obj = obtenerObjeto(item);
                    if (obj == null) {
                        continue;
                    }
                    String tipo = obtenerTexto(obj, "type");
                    String texto = obtenerTexto(obj, "text");
                    if (!texto.isEmpty() && ("text".equals(tipo) || tipo.isEmpty())) {
                        anexarTexto(contenidoCompleto, texto);
                    }
                }
                if (contenidoCompleto.length() > 0) {
                    return contenidoCompleto.toString();
                }
            }
            else if (content.isJsonPrimitive()) {
                return content.getAsString();
            }
        }
        return "";
    }

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
            String textoCandidato = extraerTextoDesdeArreglo(parts);
            if (!textoCandidato.isEmpty()) {
                return textoCandidato;
            }
        }
        return "";
    }

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

    public static boolean validarRespuestaPrueba(String contenido) {
        if (contenido == null || contenido.trim().isEmpty()) {
            return false;
        }

        String contenidoUpper = contenido.toUpperCase(Locale.ROOT).trim();
        return contenidoUpper.contains("OK") || contenidoUpper.contains("HOLA");
    }

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
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, I18nLogs.tr("No se pudo extraer texto desde elemento JSON"), e);
            }
            return "";
        }
    }

    private static String extraerTextoDesdeArreglo(JsonArray arreglo) {
        if (arreglo == null) {
            return "";
        }
        StringBuilder texto = new StringBuilder();
        for (JsonElement item : arreglo) {
            anexarTexto(texto, extraerTextoDesdeElemento(item));
        }
        return texto.toString();
    }

    private static String extraerTextoDesdeElemento(JsonElement elemento) {
        if (elemento == null) {
            return "";
        }

        String directo = obtenerTexto(elemento);
        if (!directo.isEmpty()) {
            return directo;
        }

        JsonObject objeto = obtenerObjeto(elemento);
        if (objeto == null) {
            return "";
        }

        StringBuilder texto = new StringBuilder();
        anexarTexto(texto, obtenerTexto(objeto, "text"));
        anexarTexto(texto, obtenerTexto(objeto, "content"));
        anexarTexto(texto, obtenerTexto(objeto, "reasoning_content"));
        anexarTexto(texto, extraerTextoDesdeArreglo(obtenerArreglo(objeto, "content")));
        anexarTexto(texto, extraerTextoDesdeArreglo(obtenerArreglo(objeto, "parts")));
        return texto.toString();
    }

    private static void anexarTexto(StringBuilder destino, String texto) {
        if (destino == null || texto == null || texto.isEmpty()) {
            return;
        }
        destino.append(texto);
    }

    private static String normalizarContenidoExtraido(String valor) {
        return Normalizador.normalizarTextoConControlesEnEspacio(valor);
    }
}
