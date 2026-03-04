package com.burpia.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Parser especializado para extraer modelos disponibles desde la API de Ollama.
 * <p>
 * Procesa respuestas JSON del endpoint {@code /api/tags} de Ollama y extrae
 * los nombres de modelos de forma robusta, manejando múltiples formatos de respuesta.
 * </p>
 *
 * @see <a href="https://github.com/DragonJAR/BurpIA/blob/main/AGENTS.md">Guía de codificación AGENTS.md</a>
 */
public final class ParserModelosOllama {

    private ParserModelosOllama() {
        // Clase de utilidad, no instanciable
    }

    /**
     * Extrae la lista de modelos desde una respuesta JSON del endpoint /api/tags de Ollama.
     * <p>
     * El método busca el campo "name" en cada objeto del array "models", con fallback
     * al campo "model" si "name" no está presente. Los modelos se deduplican manteniendo
     * el orden de aparición y se filtran valores inválidos (vacíos, solo ":").
     * </p>
     *
     * @param json la respuesta JSON de Ollama, puede ser {@code null} o vacía
     * @return lista de nombres de modelos válidos, nunca {@code null}
     */
    public static List<String> extraerModelosDesdeTags(String json) {
        if (Normalizador.esVacio(json)) {
            return new ArrayList<>();
        }

        JsonElement raiz;
        try {
            raiz = JsonParser.parseString(json);
        } catch (Exception ignored) {
            return new ArrayList<>();
        }

        if (!raiz.isJsonObject()) {
            return new ArrayList<>();
        }

        JsonArray modelos = raiz.getAsJsonObject().getAsJsonArray("models");
        if (modelos == null) {
            return new ArrayList<>();
        }

        Set<String> unicos = new LinkedHashSet<>();
        for (JsonElement elemento : modelos) {
            if (!elemento.isJsonObject()) {
                continue;
            }
            JsonObject objeto = elemento.getAsJsonObject();
            String nombre = obtenerCampoTexto(objeto, "name");
            if (Normalizador.esVacio(nombre)) {
                nombre = obtenerCampoTexto(objeto, "model");
            }
            if (esModeloValido(nombre)) {
                unicos.add(nombre);
            }
        }

        return new ArrayList<>(unicos);
    }

    /**
     * Obtiene el valor de un campo de texto desde un objeto JSON.
     *
     * @param objeto el objeto JSON fuente
     * @param campo  el nombre del campo a extraer
     * @return el valor del campo normalizado, o cadena vacía si no existe o es inválido
     */
    private static String obtenerCampoTexto(JsonObject objeto, String campo) {
        JsonElement valor = objeto.get(campo);
        if (valor == null || valor.isJsonNull() || !valor.isJsonPrimitive()) {
            return "";
        }
        try {
            return Normalizador.normalizarTexto(valor.getAsString());
        } catch (Exception ignored) {
            return "";
        }
    }

    /**
     * Verifica si un nombre de modelo es válido.
     * <p>
     * Un modelo es válido si no está vacío y no es solo ":".
     * </p>
     *
     * @param modelo el nombre del modelo a validar
     * @return {@code true} si el modelo es válido, {@code false} en caso contrario
     */
    private static boolean esModeloValido(String modelo) {
        if (Normalizador.esVacio(modelo)) {
            return false;
        }
        String limpio = modelo.trim();
        return !Normalizador.esVacio(limpio) && !":".equals(limpio);
    }
}
