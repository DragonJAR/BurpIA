package com.burpia.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReparadorJson {

    public static String repararJson(String jsonPotencial) {
        if (jsonPotencial == null || jsonPotencial.trim().isEmpty()) {
            return null;
        }

        String resultado = jsonPotencial.trim();
        if (esJsonValido(resultado)) {
            return resultado;
        }

        resultado = eliminarMarkdownCodeBlocks(resultado);
        if (esJsonValido(resultado)) {
            return resultado;
        }

        resultado = extraerPrimerObjetoJson(resultado);
        if (esJsonValido(resultado)) {
            return resultado;
        }

        resultado = repararComillasEscapadas(resultado);
        if (esJsonValido(resultado)) {
            return resultado;
        }

        resultado = eliminarContenidoExtra(resultado);
        if (esJsonValido(resultado)) {
            return resultado;
        }

        resultado = repararComas(resultado);
        if (esJsonValido(resultado)) {
            return resultado;
        }

        resultado = extraerParesClaveValor(resultado);
        if (esJsonValido(resultado)) {
            return resultado;
        }

        return null;
    }

    public static boolean esJsonValido(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }

        json = json.trim();
        if (!json.startsWith("{") && !json.startsWith("[")) {
            return false;
        }

        try {
            int longitud = json.length();
            int profundidad = 0;
            boolean enComillas = false;
            boolean escapado = false;

            for (int i = 0; i < longitud; i++) {
                char c = json.charAt(i);

                if (escapado) {
                    escapado = false;
                    continue;
                }

                if (c == '\\') {
                    escapado = true;
                    continue;
                }

                if (c == '"') {
                    enComillas = !enComillas;
                    continue;
                }

                if (!enComillas) {
                    if (c == '{' || c == '[') {
                        profundidad++;
                    } else if (c == '}' || c == ']') {
                        profundidad--;
                    }
                }
            }

            return profundidad == 0 && !enComillas;
        } catch (Exception e) {
            return false;
        }
    }

    private static String eliminarMarkdownCodeBlocks(String texto) {
        texto = texto.replaceAll("(?m)^```json\\s*", "");
        texto = texto.replaceAll("(?m)^```\\s*", "");

        texto = texto.replaceAll("(?m)```$", "");

        return texto.trim();
    }

    private static String extraerPrimerObjetoJson(String texto) {
        int inicio = texto.indexOf('{');
        if (inicio == -1) {
            inicio = texto.indexOf('[');
        }

        if (inicio == -1) {
            return texto;
        }

        StringBuilder resultado = new StringBuilder();
        int profundidad = 0;
        boolean enComillas = false;
        boolean escapado = false;
        char caracterInicio = texto.charAt(inicio);
        char caracterCierre = (caracterInicio == '{') ? '}' : ']';

        for (int i = inicio; i < texto.length(); i++) {
            char c = texto.charAt(i);

            if (escapado) {
                resultado.append(c);
                escapado = false;
                continue;
            }

            if (c == '\\') {
                resultado.append(c);
                escapado = true;
                continue;
            }

            if (c == '"') {
                enComillas = !enComillas;
                resultado.append(c);
                continue;
            }

            if (!enComillas) {
                if (c == caracterInicio) {
                    profundidad++;
                    resultado.append(c);
                } else if (c == caracterCierre) {
                    profundidad--;
                    resultado.append(c);
                    if (profundidad == 0) {
                        break;
                    }
                } else {
                    resultado.append(c);
                }
            } else {
                resultado.append(c);
            }
        }

        return resultado.toString();
    }

    private static String repararComillasEscapadas(String texto) {
        texto = texto.replace("\\\"", "\"");

        Pattern patron = Pattern.compile(": \"([^\"]*)\"");
        Matcher matcher = patron.matcher(texto);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String valor = matcher.group(1)
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
            matcher.appendReplacement(sb, ": \"" + valor + "\"");
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private static String eliminarContenidoExtra(String texto) {
        texto = texto.trim();

        int ultimoCierre = -1;
        for (int i = texto.length() - 1; i >= 0; i--) {
            char c = texto.charAt(i);
            if (c == '}' || c == ']') {
                ultimoCierre = i + 1;
                break;
            }
        }

        if (ultimoCierre > 0) {
            return texto.substring(0, ultimoCierre);
        }

        return texto;
    }

    private static String repararComas(String texto) {
        texto = texto.replaceAll(",\\s*([\\]}])", "$1");

        texto = texto.replaceAll("\"\\s+\"", "\", \"");

        return texto;
    }

    private static String extraerParesClaveValor(String texto) {
        StringBuilder json = new StringBuilder();
        json.append("{");

        Pattern patron = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = patron.matcher(texto);

        List<String> pares = new ArrayList<>();
        while (matcher.find()) {
            String clave = matcher.group(1);
            String valor = matcher.group(2);
            pares.add("\"" + clave + "\": \"" + valor + "\"");
        }

        Pattern patron2 = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(true|false|\\d+)");
        Matcher matcher2 = patron2.matcher(texto);

        while (matcher2.find()) {
            String clave = matcher2.group(1);
            String valor = matcher2.group(2);
            pares.add("\"" + clave + "\": " + valor);
        }

        if (pares.isEmpty()) {
            return texto;
        }

        json.append(String.join(", ", pares));
        json.append("}");

        return json.toString();
    }

    public static List<String> extraerMultiplesObjetosJson(String texto) {
        List<String> objetos = new ArrayList<>();
        String textoRestante = texto;

        while (true) {
            String objeto = extraerPrimerObjetoJson(textoRestante);
            if (objeto == null || objeto.isEmpty()) {
                break;
            }

            if (esJsonValido(objeto)) {
                objetos.add(objeto);
            }

            int longitudObjeto = objeto.length();
            int indice = textoRestante.indexOf(objeto);
            if (indice + longitudObjeto < textoRestante.length()) {
                textoRestante = textoRestante.substring(indice + longitudObjeto);
            } else {
                break;
            }
        }

        return objetos;
    }
}
