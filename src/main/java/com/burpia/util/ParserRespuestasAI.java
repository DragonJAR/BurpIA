package com.burpia.util;
import com.burpia.config.ProveedorAI;
import com.burpia.i18n.I18nLogs;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import java.util.Locale;

public class ParserRespuestasAI {
    private static final java.util.regex.Pattern PATRON_BLOQUES_PENSAMIENTO =
        java.util.regex.Pattern.compile("(?is)<\\s*(think|thinking)\\b[^>]*>.*?<\\s*/\\s*\\1\\s*>");

    private static final java.util.regex.Pattern PATRON_CAMPO_TITULO_NO_ESTRICTO = java.util.regex.Pattern.compile(
        "\"(?:titulo|title|name|nombre)\"\\s*:\\s*\"(.*?)(?=\"\\s*(?:,\\s*\"[a-zA-Z0-9_]+\"\\s*:|\\}))",
        java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE
    );
    private static final java.util.regex.Pattern PATRON_CAMPO_DESCRIPCION_NO_ESTRICTO = java.util.regex.Pattern.compile(
        "\"(?:descripcion|description|hallazgo|finding|details|detalle)\"\\s*:\\s*\"(.*?)(?=\"\\s*(?:,\\s*\"[a-zA-Z0-9_]+\"\\s*:|\\}))",
        java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE
    );
    private static final java.util.regex.Pattern PATRON_CAMPO_SEVERIDAD_NO_ESTRICTO = java.util.regex.Pattern.compile(
        "\"(?:severidad|severity|risk|impacto)\"\\s*:\\s*\"(.*?)(?=\"\\s*(?:,\\s*\"[a-zA-Z0-9_]+\"\\s*:|\\}))",
        java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE
    );
    private static final java.util.regex.Pattern PATRON_CAMPO_CONFIANZA_NO_ESTRICTO = java.util.regex.Pattern.compile(
        "\"(?:confianza|confidence|certainty|certeza)\"\\s*:\\s*\"(.*?)(?=\"\\s*(?:,\\s*\"[a-zA-Z0-9_]+\"\\s*:|\\}))",
        java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE
    );
    private static final java.util.regex.Pattern PATRON_CAMPO_EVIDENCIA_NO_ESTRICTO = java.util.regex.Pattern.compile(
        "\"(?:evidencia|evidence|proof|indicator)\"\\s*:\\s*\"(.*?)(?=\"\\s*(?:,\\s*\"[a-zA-Z0-9_]+\"\\s*:|\\}))",
        java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE
    );

    public static String extraerCampoNoEstricto(String campo, String contenido) {
        if (Normalizador.esVacio(contenido)) {
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
        if (Normalizador.esVacio(respuestaJson)) {
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

            if (Normalizador.noEsVacio(contenido)) {
                contenido = normalizarContenidoExtraido(contenido);
            }

            return limpiarBloquesPensamiento(contenido != null ? contenido : "");

        } catch (Exception e) {
            // Error de parseo no crítico, se intenta con texto plano
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
        if (Normalizador.esVacio(contenido)) {
            return false;
        }

        String contenidoUpper = contenido.toUpperCase(Locale.ROOT).trim();
        return contenidoUpper.contains("OK") || contenidoUpper.contains("HOLA");
    }

    public static boolean validarRespuestaConexion(String contenido) {
        return Normalizador.noEsVacio(contenido);
    }

    private static String limpiarBloquesPensamiento(String texto) {
        if (Normalizador.esVacio(texto)) {
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
            // Error al extraer texto, se retorna vacío
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
        if (destino == null || Normalizador.esVacio(texto)) {
            return;
        }
        destino.append(texto);
    }

    private static String normalizarContenidoExtraido(String valor) {
        return Normalizador.normalizarTextoConControlesEnEspacio(valor);
    }

    /**
     * Extrae un array JSON de una respuesta que puede contener texto antes/después.
     * Busca patrones como: "[{...}]" o cualquier estructura JSON válida.
     *
     * @param contenido Texto que puede contener JSON rodeado de texto
     * @return JsonArray si se encuentra JSON válido, null en caso contrario
     */
    public static JsonArray extraerJsonDeTextoLibre(String contenido) {
        if (Normalizador.esVacio(contenido)) {
            return null;
        }

        // Buscar el primer '[' que inicia un array JSON
        int inicioArray = contenido.indexOf('[');
        if (inicioArray == -1) {
            return null;
        }

        // Buscar el cierre del array JSON
        int finArray = encontrarCierreJson(contenido, inicioArray);
        if (finArray == -1) {
            return null;
        }

        String jsonExtraido = contenido.substring(inicioArray, finArray + 1);

        try {
            JsonElement elemento = JsonParser.parseString(jsonExtraido);
            if (elemento.isJsonArray()) {
                return elemento.getAsJsonArray();
            }
        } catch (Exception e) {
            // JSON inválido, retornar null
        }

        return null;
    }

    /**
     * Hace público el método de limpieza de bloques de pensamiento
     * para que pueda ser usado por AnalizadorAI.
     *
     * @param texto Texto que puede contener bloques <thinking> o ```
     * @return Texto limpio sin bloques de pensamiento
     */
    public static String limpiarBloquesPensamientoParaAnalisis(String texto) {
        return limpiarBloquesPensamiento(texto);
    }

    /**
     * Encuentra la posición del carácter que cierra un array JSON,
     * contando corchetes anidados y manejando strings correctamente.
     *
     * @param texto Texto completo
     * @param posicionInicio Posición del '[' inicial
     * @return Posición del ']' de cierre, o -1 si no se encuentra
     */
    private static int encontrarCierreJson(String texto, int posicionInicio) {
        int profundidad = 0;
        boolean enString = false;
        char caracterEscape = '\0';

        for (int i = posicionInicio; i < texto.length(); i++) {
            char c = texto.charAt(i);

            if (enString) {
                if (caracterEscape == '\\') {
                    caracterEscape = c;
                    continue;
                }
                if (c == '\\') {
                    caracterEscape = c;
                    continue;
                }
                if (c == '"') {
                    enString = false;
                }
                continue;
            }

            if (c == '"') {
                enString = true;
                continue;
            }

            if (c == '[') {
                profundidad++;
            } else if (c == ']') {
                profundidad--;
                if (profundidad == 0) {
                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * Intenta extraer un array JSON aplicando múltiples estrategias de forma secuencial.
     * Este método es genérico y reutilizable para cualquier tipo de respuesta JSON.
     *
     * Estrategias aplicadas en orden:
     * 1. Parseo directo como JSON completo
     * 2. Extracción de JSON de texto libre (busca [{...}] en medio del texto)
     * 3. Búsqueda de arrays dentro de objetos JSON (hallazgos, findings, issues, etc.)
     *
     * @param contenido Contenido que puede contener JSON
     * @param gson Instancia de Gson para parseo
     * @return JsonArray si alguna estrategia tiene éxito, null en caso contrario
     */
    public static JsonArray extraerArrayConMultiplesEstrategias(String contenido, com.google.gson.Gson gson) {
        if (Normalizador.esVacio(contenido)) {
            return null;
        }

        // Estrategia 1: Parseo directo como JSON completo
        JsonArray resultado = intentarParseoDirecto(contenido, gson);
        if (resultado != null) {
            return resultado;
        }

        // Estrategia 2: Extraer JSON de texto libre (para modelos con thinking process)
        resultado = extraerJsonDeTextoLibre(contenido);
        if (resultado != null) {
            return resultado;
        }

        // Estrategia 3: Buscar arrays dentro de objetos con claves comunes
        resultado = extraerArrayDeObjetoConClavesComunes(contenido, gson);
        if (resultado != null) {
            return resultado;
        }

        return null;
    }

    /**
     * Estrategia 1: Intenta parsear el contenido directamente como JSON.
     * Retorna un array si el contenido es un array JSON o si contiene un array como raíz.
     */
    private static JsonArray intentarParseoDirecto(String contenido, com.google.gson.Gson gson) {
        try {
            String limpio = limpiarBloquesPensamiento(contenido);
            JsonElement elemento = gson.fromJson(limpio, JsonElement.class);

            if (elemento == null) {
                return null;
            }

            // Si es directamente un array
            if (elemento.isJsonArray()) {
                return elemento.getAsJsonArray();
            }

            // Si es un objeto, buscar el primer array dentro
            if (elemento.isJsonObject()) {
                return buscarPrimerArrayEnObjeto(elemento.getAsJsonObject());
            }
        } catch (Exception e) {
            // No es JSON válido, retornar null
        }
        return null;
    }

    /**
     * Busca recursivamente el primer array dentro de un objeto JSON.
     */
    private static JsonArray buscarPrimerArrayEnObjeto(JsonObject objeto) {
        for (String key : objeto.keySet()) {
            JsonElement elemento = objeto.get(key);
            if (elemento != null && elemento.isJsonArray()) {
                return elemento.getAsJsonArray();
            }
            if (elemento != null && elemento.isJsonObject()) {
                JsonArray resultado = buscarPrimerArrayEnObjeto(elemento.getAsJsonObject());
                if (resultado != null) {
                    return resultado;
                }
            }
        }
        return null;
    }

    /**
     * Estrategia 3: Busca arrays dentro de objetos con claves comunes de hallazgos.
     */
    private static JsonArray extraerArrayDeObjetoConClavesComunes(String contenido, com.google.gson.Gson gson) {
        String[] clavesComunes = {"\"hallazgos\"", "\"findings\"", "\"issues\"", "\"vulnerabilidades\"",
                                   "\"results\"", "\"data\"", "\"items\"", "\"objects\""};

        for (String clave : clavesComunes) {
            int indice = contenido.indexOf(clave);
            if (indice >= 0) {
                // Buscar el array después de la clave
                int inicioArray = contenido.indexOf('[', indice);
                if (inicioArray >= 0) {
                    int finArray = encontrarCierreJson(contenido, inicioArray);
                    if (finArray >= 0) {
                        String arrayStr = contenido.substring(inicioArray, finArray + 1);
                        try {
                            JsonElement elemento = gson.fromJson(arrayStr, JsonElement.class);
                            if (elemento != null && elemento.isJsonArray()) {
                                return elemento.getAsJsonArray();
                            }
                        } catch (Exception e) {
                            // Continuar con siguiente clave
                        }
                    }
                }
            }
        }
        return null;
    }
}
