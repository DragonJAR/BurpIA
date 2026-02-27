package com.burpia.util;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReparadorJson {

    private ReparadorJson() {
    }

    public static String repararJson(String jsonPotencial) {
        if (jsonPotencial == null || jsonPotencial.trim().isEmpty()) {
            return null;
        }

        String resultado = jsonPotencial.trim();

        if (esJsonValido(resultado)) {
            return resultado;
        }

        resultado = eliminarMarkdownCodeBlocks(resultado);
        if (esJsonValido(resultado)) return resultado;

        resultado = extraerPrimerObjetoJson(resultado);
        if (esJsonValido(resultado)) return resultado;

        resultado = repararComillasEscapadas(resultado);
        if (esJsonValido(resultado)) return resultado;

        resultado = eliminarContenidoExtra(resultado);
        if (esJsonValido(resultado)) return resultado;

        resultado = repararComas(resultado);
        if (esJsonValido(resultado)) return resultado;

        resultado = extraerParesClaveValor(resultado);
        if (esJsonValido(resultado)) return resultado;

        return null;
    }

    public static boolean esJsonValido(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }

        String normalizado = json.trim();

        if (!normalizado.startsWith("{") && !normalizado.startsWith("[")) {
            return false;
        }

        try {
            JsonElement element = JsonParser.parseString(normalizado);
            return element != null && (element.isJsonObject() || element.isJsonArray());
        } catch (Exception ignored) {
            return false;
        }
    }

    private static final Pattern MARKDOWN_CODE_BLOCK_JSON_PATTERN = Pattern.compile("(?m)^```json\\s*");
    private static final Pattern MARKDOWN_CODE_BLOCK_START_PATTERN = Pattern.compile("(?m)^```\\s*");
    private static final Pattern MARKDOWN_CODE_BLOCK_END_PATTERN = Pattern.compile("(?m)```$");

    private static String eliminarMarkdownCodeBlocks(String texto) {
        String res = MARKDOWN_CODE_BLOCK_JSON_PATTERN.matcher(texto).replaceAll("");
        res = MARKDOWN_CODE_BLOCK_START_PATTERN.matcher(res).replaceAll("");
        res = MARKDOWN_CODE_BLOCK_END_PATTERN.matcher(res).replaceAll("");
        return res.trim();
    }

    private static String extraerPrimerObjetoJson(String texto) {
        int inicioObjeto = texto.indexOf('{');
        int inicioArreglo = texto.indexOf('[');

        int inicio = (inicioObjeto != -1 && inicioArreglo != -1)
            ? Math.min(inicioObjeto, inicioArreglo)
            : (inicioObjeto != -1 ? inicioObjeto : inicioArreglo);

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
        if (texto == null) return null;

        String resultado = texto.replace("\\\\\"", "\\\"");

        Pattern patron = Pattern.compile(": \"(.*?)\"(?=\\s*[,\\}])", Pattern.DOTALL);
        Matcher matcher = patron.matcher(resultado);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String valor = matcher.group(1);
            if (valor.contains("\"") && !valor.contains("\\\"")) {
                valor = valor.replace("\"", "\\\"");
            }
            valor = valor.replace("\n", "\\n")
                         .replace("\r", "\\r")
                         .replace("\t", "\\t");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(": \"" + valor + "\""));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private static String eliminarContenidoExtra(String texto) {
        String textoLimpio = texto.trim();

        int ultimoCierre = -1;
        for (int i = textoLimpio.length() - 1; i >= 0; i--) {
            char c = textoLimpio.charAt(i);
            if (c == '}' || c == ']') {
                ultimoCierre = i + 1;
                break;
            }
        }

        if (ultimoCierre > 0) {
            return textoLimpio.substring(0, ultimoCierre);
        }

        return textoLimpio;
    }

    private static final Pattern COMILLA_ESCAPE_PATTERN = Pattern.compile(",\\s*([\\]}])");
    private static final Pattern DOBLE_COMILLA_PATTERN = Pattern.compile("\"\\s+\"");

    private static String repararComas(String texto) {
        String res = COMILLA_ESCAPE_PATTERN.matcher(texto).replaceAll("$1");
        res = DOBLE_COMILLA_PATTERN.matcher(res).replaceAll("\", \"");
        return res;
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
}
