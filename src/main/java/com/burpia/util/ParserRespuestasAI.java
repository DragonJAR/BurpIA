package com.burpia.util;
import com.burpia.config.ProveedorAI;
import com.burpia.i18n.I18nLogs;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import java.util.List;
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

            if ("Ollama".equals(proveedorNormalizado)) {
                contenido = extraerContenidoOllama(raiz);
            } else if ("OpenAI".equals(proveedorNormalizado)
                    || "Z.ai".equals(proveedorNormalizado)
                    || "minimax".equals(proveedorNormalizado)
                    || ProveedorAI.esProveedorCustom(proveedorNormalizado)) {
                contenido = extraerContenidoOpenAI(raiz);
            } else if ("Claude".equals(proveedorNormalizado)) {
                contenido = extraerContenidoClaude(raiz);
            } else if ("Gemini".equals(proveedorNormalizado)) {
                contenido = extraerContenidoGemini(raiz);
            } else {
                contenido = extraerContenidoGenerico(raiz);
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
        // Fast path: output_text directo
        JsonElement outputTextElement = raiz.get("output_text");
        if (outputTextElement != null && outputTextElement.isJsonPrimitive()) {
            String outputText = outputTextElement.getAsString();
            if (!outputText.isEmpty()) {
                return outputText;
            }
        }

        // Fast path: output array
        JsonArray output = obtenerArreglo(raiz, "output");
        if (output != null && output.size() > 0) {
            StringBuilder contenidoCompleto = new StringBuilder(output.size() * 100);
            for (JsonElement outputItem : output) {
                JsonObject outputObject = obtenerObjeto(outputItem);
                if (outputObject == null) {
                    continue;
                }

                JsonArray contenidos = obtenerArreglo(outputObject, "content");
                if (contenidos != null) {
                    String texto = extraerTextoDesdeArreglo(contenidos);
                    if (texto != null && !texto.isEmpty()) {
                        contenidoCompleto.append(texto);
                    }
                }
            }
            if (contenidoCompleto.length() > 0) {
                return contenidoCompleto.toString();
            }
        }

        // Path más complejo: choices
        JsonArray choices = obtenerArreglo(raiz, "choices");
        if (choices != null && choices.size() > 0) {
            for (JsonElement choiceItem : choices) {
                JsonObject choice = obtenerObjeto(choiceItem);
                if (choice == null) {
                    continue;
                }

                JsonObject message = obtenerObjeto(choice, "message");
                if (message != null) {
                    // Try content field
                    JsonElement contentElement = message.get("content");
                    if (contentElement != null) {
                        if (contentElement.isJsonPrimitive()) {
                            String content = contentElement.getAsString();
                            if (!content.isEmpty()) {
                                return content;
                            }
                        } else if (contentElement.isJsonArray()) {
                            String contentPartes = extraerTextoDesdeArreglo(contentElement.getAsJsonArray());
                            if (contentPartes != null && !contentPartes.isEmpty()) {
                                return contentPartes;
                            }
                        }
                    }

                    // Try reasoning_content
                    String reasoning = obtenerTexto(message, "reasoning_content");
                    if (!reasoning.isEmpty()) {
                        return reasoning;
                    }
                }

                // Try text field on choice
                String text = obtenerTexto(choice, "text");
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return "";
    }

    private static String extraerContenidoClaude(JsonObject raiz) {
        if (!raiz.has("content")) {
            return "";
        }

        JsonElement content = raiz.get("content");

        // Fast path: primitivo
        if (content.isJsonPrimitive()) {
            return content.getAsString();
        }

        // Fast path: no es array
        if (!content.isJsonArray()) {
            return "";
        }

        var contentArray = content.getAsJsonArray();
        StringBuilder contenidoCompleto = new StringBuilder(contentArray.size() * 100);

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

        return contenidoCompleto.length() > 0 ? contenidoCompleto.toString() : "";
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
        boolean escapando = false;

        for (int i = posicionInicio; i < texto.length(); i++) {
            char c = texto.charAt(i);

            if (enString) {
                if (escapando) {
                    escapando = false;
                    continue;
                }
                if (c == '\\') {
                    escapando = true;
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
     * EXTRA DE JSON INTELIGENTE - Método Maestro ÚNICO.
     *
     * Aplica múltiples estrategias de extracción de JSON de forma secuencial.
     * Cada estrategia es responsable de SU PROPIA limpieza y parseo.
     *
     * Este método es completamente genérico y reutilizable para cualquier tipo de respuesta JSON.
     *
     * Estrategias aplicadas en orden:
     * 1. Parseo directo como JSON (limpia bloques internamente)
     * 2. Extracción de JSON de texto libre (limpia bloques internamente)
     * 3. Búsqueda de arrays con claves comunes (limpia bloques internamente)
     *
     * @param contenido Contenido que puede contener JSON (con o sin noise previo)
     * @param gson Instancia de Gson para parseo
     * @return JsonArray si alguna estrategia tiene éxito, null en caso contrario
     */
    public static JsonArray extraerArrayJsonInteligente(String contenido, com.google.gson.Gson gson) {
        if (Normalizador.esVacio(contenido)) {
            return null;
        }

        // ESTRATEGIA 1: Parseo directo con limpieza interna
        JsonArray resultado = intentarParseoDirectoConLimpieza(contenido, gson);
        if (resultado != null) {
            return resultado;
        }

        // ESTRATEGIA 2: JSON de texto libre con limpieza interna (thinking models)
        resultado = extraerJsonDeTextoLibreConLimpieza(contenido, gson);
        if (resultado != null) {
            return resultado;
        }

        // ESTRATEGIA 3: Arrays con claves comunes con limpieza interna
        resultado = extraerArrayConClavesComunesConLimpieza(contenido, gson);
        if (resultado != null) {
            return resultado;
        }

        return null;
    }

    /**
     * Estrategia 1: Parseo directo con limpieza propia.
     * Limpia bloques de pensamiento antes de intentar parsear.
     */
    private static JsonArray intentarParseoDirectoConLimpieza(String contenido, com.google.gson.Gson gson) {
        String limpio = limpiarBloquesPensamiento(contenido);

        try {
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
     * Estrategia 2: Extraer JSON de texto libre con limpieza propia.
     * Para modelos con "thinking process" antes del JSON.
     */
    private static JsonArray extraerJsonDeTextoLibreConLimpieza(String contenido, com.google.gson.Gson gson) {
        String limpio = limpiarBloquesPensamiento(contenido);

        // Buscar el primer '[' que inicia un array JSON
        int inicioArray = limpio.indexOf('[');
        if (inicioArray == -1) {
            return null;
        }

        // Buscar el cierre del array JSON
        int finArray = encontrarCierreJson(limpio, inicioArray);
        if (finArray == -1) {
            return null;
        }

        String jsonExtraido = limpio.substring(inicioArray, finArray + 1);

        try {
            JsonElement elemento = gson.fromJson(jsonExtraido, JsonElement.class);
            if (elemento != null && elemento.isJsonArray()) {
                return elemento.getAsJsonArray();
            }
        } catch (Exception e) {
            // JSON inválido, retornar null
        }

        return null;
    }

    /**
     * Estrategia 3: Buscar arrays con claves comunes, con limpieza propia.
     */
    private static JsonArray extraerArrayConClavesComunesConLimpieza(String contenido, com.google.gson.Gson gson) {
        String limpio = limpiarBloquesPensamiento(contenido);

        String[] clavesComunes = {"\"hallazgos\"", "\"findings\"", "\"issues\"", "\"vulnerabilidades\"",
                                   "\"results\"", "\"data\"", "\"items\"", "\"objects\""};

        for (String clave : clavesComunes) {
            int indice = limpio.indexOf(clave);
            if (indice >= 0) {
                // Buscar el array después de la clave
                int inicioArray = limpio.indexOf('[', indice);
                if (inicioArray >= 0) {
                    int finArray = encontrarCierreJson(limpio, inicioArray);
                    if (finArray >= 0) {
                        String arrayStr = limpio.substring(inicioArray, finArray + 1);
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

    /**
     * Extrae hallazgos de texto estructurado EXTREMADAMENTE malformado.
     *
     * <p><b>ÚLTIMA OPCIÓN</b> en la cadena de fallback.</p>
     *
     * <p>Usa {@link ExtractorCamposRobusto} para extraer campos sin depender
     * de JSON válido. Estrategia:</p>
     *
     * <ol>
     *   <li>Usa "titulo" como delimitador (no depende de { })</li>
     *   <li>Aplica 3 estrategias de extracción por campo</li>
     *   <li>Maneja campos duplicados (último valor gana)</li>
     * </ol>
     *
     * <p><b>Casos de uso:</b></p>
     * <ul>
     *   <li>Modelos con JSON roto (dragon-security, modelos locales)</li>
     *   <li>LLMs que generan formato inconsistente</li>
     *   <li>Respuestas con thinking process mezclado</li>
     * </ul>
     *
     * @param contenido Texto estructurado malformado
     * @param gson Instancia de Gson
     * @return JsonArray con hallazgos recuperados, null si falla todo
     * @since 1.0.3
     */
    public static JsonArray extraerHallazgosPorDelimitadores(String contenido, com.google.gson.Gson gson) {
        if (Normalizador.esVacio(contenido)) {
            return null;
        }

        try {
            List<String> bloques = ExtractorCamposRobusto.extraerBloquesPorCampo(
                contenido,
                ExtractorCamposRobusto.CamposHallazgo.TITULO
            );

            if (bloques.isEmpty()) {
                return null;
            }

            JsonArray arrayHallazgos = new JsonArray();

            for (String bloque : bloques) {
                JsonObject hallazgo = new JsonObject();

                String titulo = extraerCampoConExtractor(
                    ExtractorCamposRobusto.CamposHallazgo.TITULO, bloque);
                String descripcion = extraerCampoConExtractor(
                    ExtractorCamposRobusto.CamposHallazgo.DESCRIPCION, bloque);
                String severidad = extraerCampoConExtractor(
                    ExtractorCamposRobusto.CamposHallazgo.SEVERIDAD, bloque);
                String confianza = extraerCampoConExtractor(
                    ExtractorCamposRobusto.CamposHallazgo.CONFIANZA, bloque);
                String evidencia = extraerCampoConExtractor(
                    ExtractorCamposRobusto.CamposHallazgo.EVIDENCIA, bloque);

                if (Normalizador.noEsVacio(titulo)) {
                    hallazgo.addProperty("titulo", titulo);
                    if (Normalizador.noEsVacio(descripcion)) {
                        hallazgo.addProperty("descripcion", descripcion);
                    }
                    if (Normalizador.noEsVacio(severidad)) {
                        hallazgo.addProperty("severidad", severidad);
                    }
                    if (Normalizador.noEsVacio(confianza)) {
                        hallazgo.addProperty("confianza", confianza);
                    }
                    if (Normalizador.noEsVacio(evidencia)) {
                        hallazgo.addProperty("evidencia", evidencia);
                    }

                    arrayHallazgos.add(hallazgo);
                }
            }

            return arrayHallazgos.size() > 0 ? arrayHallazgos : null;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Helper para extraer campo usando ExtractorCamposRobusto.
     *
     * @param campo Campo a extraer
     * @param bloque Bloque de texto
     * @return Valor extraído o vacío
     */
    private static String extraerCampoConExtractor(ExtractorCamposRobusto.Campo campo, String bloque) {
        return ExtractorCamposRobusto.extraerCampoDeBloque(campo, bloque);
    }
}
