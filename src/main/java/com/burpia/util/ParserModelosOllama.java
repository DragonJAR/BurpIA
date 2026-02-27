package com.burpia.util;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ParserModelosOllama {

    private ParserModelosOllama() {
    }

    public static List<String> extraerModelosDesdeTags(String json) {
        List<String> resultado = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) {
            return resultado;
        }

        JsonElement raiz;
        try {
            raiz = JsonParser.parseString(json);
        } catch (Exception ignored) {
            return resultado;
        }
        if (!raiz.isJsonObject()) {
            return resultado;
        }

        JsonArray modelos = raiz.getAsJsonObject().getAsJsonArray("models");
        if (modelos == null) {
            return resultado;
        }

        Set<String> unicos = new LinkedHashSet<>();
        for (JsonElement elemento : modelos) {
            if (!elemento.isJsonObject()) {
                continue;
            }
            JsonObject objeto = elemento.getAsJsonObject();
            String nombre = obtenerCampoTexto(objeto, "name");
            if (nombre.isEmpty()) {
                nombre = obtenerCampoTexto(objeto, "model");
            }
            if (esModeloValido(nombre)) {
                unicos.add(nombre);
            }
        }

        resultado.addAll(unicos);
        return resultado;
    }

    private static String obtenerCampoTexto(JsonObject objeto, String campo) {
        JsonElement valor = objeto.get(campo);
        if (valor == null || valor.isJsonNull() || !valor.isJsonPrimitive()) {
            return "";
        }
        try {
            String texto = valor.getAsString();
            return texto != null ? texto.trim() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private static boolean esModeloValido(String modelo) {
        if (modelo == null) {
            return false;
        }
        String limpio = modelo.trim();
        return !limpio.isEmpty() && !":".equals(limpio);
    }
}
