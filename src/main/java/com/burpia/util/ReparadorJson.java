package com.burpia.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidad para reparar JSON potencialmente dañado o mal formado proveniente de respuestas AI.
 * Implementa una cadena de estrategias de reparación para manejar diversos formatos de entrada.
 */
public final class ReparadorJson {

    private static final Pattern MARKDOWN_CODE_BLOCK_START_PATTERN = Pattern.compile("(?m)^```(?:json)?\\s*");
    private static final Pattern MARKDOWN_CODE_BLOCK_END_PATTERN = Pattern.compile("(?m)```\\s*$");
    private static final Pattern COMILLA_ESCAPE_PATTERN = Pattern.compile(",\\s*([\\]}])");
    private static final Pattern DOBLE_COMILLA_PATTERN = Pattern.compile("\"\\s+\"");
    private static final Pattern PATRON_VALOR_CAMPO = Pattern.compile(": \"(.*?)\"(?=\\s*[,\\}])", Pattern.DOTALL);
    private static final Pattern PATRON_PAR_STRING = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern PATRON_PAR_BOOLEANO_NUMERO = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(true|false|\\d+)");

    private ReparadorJson() {
    }

    /**
     * Intenta reparar un JSON potencialmente dañado aplicando múltiples estrategias de reparación.
     * Las estrategias se aplican en orden: markdown, extracción de objeto, comillas, contenido extra,
     * comas finales, y extracción de pares clave-valor.
     *
     * @param jsonPotencial el texto que puede contener JSON dañado o mal formado
     * @return el JSON reparado y válido, o null si no se puede reparar
     */
    public static String repararJson(String jsonPotencial) {
        if (Normalizador.esVacio(jsonPotencial)) {
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

    /**
     * Verifica si una cadena es un JSON válido (objeto o arreglo).
     *
     * @param json la cadena a validar
     * @return true si es un JSON válido, false en caso contrario
     */
    public static boolean esJsonValido(String json) {
        if (json == null) {
            return false;
        }

        String normalizado = json.trim();
        if (normalizado.isEmpty()) {
            return false;
        }

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

    private static String eliminarMarkdownCodeBlocks(String texto) {
        String res = MARKDOWN_CODE_BLOCK_START_PATTERN.matcher(texto).replaceAll("");
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

        resultado = repararCamposEvidenciaConHtml(resultado);

        Matcher matcher = PATRON_VALOR_CAMPO.matcher(resultado);
        StringBuilder sb = new StringBuilder();

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

    private static String repararCamposEvidenciaConHtml(String texto) {
        if (Normalizador.esVacio(texto)) {
            return texto;
        }

        // El LLM frecuentemente genera JSON con comillas sin escapar en campos evidencia
        // que contienen HTML. Ejemplo: "evidencia": "<!-- InstanceBegin template="/Templates/..."
        // donde las comillas en los atributos HTML rompen el JSON.
        //
        // Solución: Parser manual que encuentra campos "evidencia" y re-escapea comillas
        // dentro de los valores.

        String[] clavesABuscar = {"evidencia", "evidence", "prueba"};

        for (String clave : clavesABuscar) {
            texto = reparadorCampoHtml(texto, clave);
        }

        return texto;
    }

    /**
     * Repara un campo específico que puede contener comillas sin escapar.
     * Parser manual que encuentra ": valor" y re-escapea comillas dentro del valor.
     */
    private static String reparadorCampoHtml(String texto, String nombreCampo) {
        if (Normalizador.esVacio(texto) || nombreCampo == null) {
            return texto;
        }

        StringBuilder resultado = new StringBuilder(texto.length() + 64);
        int indice = 0;

        while (indice < texto.length()) {
            // Buscar el nombre del campo (case insensitive)
            int posCampo = buscarCampo(texto, nombreCampo, indice);
            if (posCampo == -1) {
                resultado.append(texto.substring(indice));
                break;
            }

            // posCampo incluye las comillas del nombre del campo
            // Ejemplo: "evidencia" está en posCampo, el valor empieza después de ": "
            // Buscar ": " después del campo
            int posDosPuntos = -1;
            for (int i = posCampo + nombreCampo.length() + 2; i < texto.length() - 1; i++) {
                if (texto.charAt(i) == ':' && texto.charAt(i + 1) == ' ') {
                    posDosPuntos = i;
                    break;
                }
            }

            if (posDosPuntos == -1) {
                indice = posCampo + nombreCampo.length() + 2;
                continue;
            }

            // Agregar todo hasta después de ": "
            resultado.append(texto.substring(indice, posDosPuntos + 2));
            
            // El valor empieza en posDosPuntos + 2 (después de ": ")
            int posInicioValor = posDosPuntos + 2;

            // Verificar que hay una comilla de apertura
            if (posInicioValor >= texto.length() || texto.charAt(posInicioValor) != '"') {
                indice = posInicioValor;
                continue;
            }

            // Saltar la comilla de apertura
            int posDespuesApertura = posInicioValor + 1;

            // Encontrar el final del valor (la comilla de cierre que NO está escapada)
            int posFinValor = encontrarFinValorJson(texto, posDespuesApertura);

            if (posFinValor == -1) {
                // No se pudo encontrar un cierre válido, intentar reparar comillas en lo que sigue
                String resto = texto.substring(posDespuesApertura);
                String restoReparado = reparadorComillasEnHtml(resto);
                resultado.append(restoReparado);
                break;
            }

            // Extraer el valor original (sin las comillas de apertura/cierre)
            String valorOriginal = texto.substring(posDespuesApertura, posFinValor);

            // Reparar comillas sin escapar dentro del valor
            String valorReparado = reparadorComillasEnHtml(valorOriginal);

            // Agregar el valor reparado con comillas
            resultado.append('"').append(valorReparado).append('"');

            indice = posFinValor + 1;
        }

        return resultado.toString();
    }

    /**
     * Busca un campo en el texto (case insensitive)
     */
    private static int buscarCampo(String texto, String campo, int desde) {
        String textoLower = texto.toLowerCase();
        String campoLower = "\"" + campo.toLowerCase() + "\"";
        return textoLower.indexOf(campoLower, desde);
    }

    /**
     * Encuentra la posición de la comilla de cierre que cierra el valor JSON.
     * Maneja correctamente comillas escapadas (\") vs. comillas literales (").
     */
    private static int encontrarFinValorJson(String texto, int inicio) {
        boolean escapado = false;

        for (int i = inicio; i < texto.length(); i++) {
            char c = texto.charAt(i);

            if (escapado) {
                escapado = false;
                continue;
            }

            if (c == '\\') {
                escapado = true;
                continue;
            }

            if (c == '"') {
                // Encontramos la comilla de cierre
                return i;
            }
        }

        return -1; // No se encontró cierre
    }

    /**
     * Repara comillas sin escapar dentro de un valor que contiene HTML.
     * El LLM genera: "valor con "texto" dentro" (roto)
     * Debe convertirse a: "valor con \"texto\" dentro" (válido)
     */
    private static String reparadorComillasEnHtml(String valor) {
        if (Normalizador.esVacio(valor)) {
            return valor;
        }

        StringBuilder resultado = new StringBuilder(valor.length() + 32);
        boolean dentroDeTag = false;

        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);

            if (c == '<') {
                dentroDeTag = true;
                resultado.append(c);
            } else if (c == '>') {
                dentroDeTag = false;
                resultado.append(c);
            } else if (c == '"' && dentroDeTag) {
                // Comilla sin escapar dentro de tag HTML - escapar
                resultado.append("\\\"");
            } else if (c == '\\' && i + 1 < valor.length() && valor.charAt(i + 1) == '"') {
                // Backslash antes de comilla - mantenerlo
                resultado.append(c);
            } else {
                resultado.append(c);
            }
        }

        return resultado.toString();
    }

    private static String escaparComillasEnHtml(String html) {
        if (Normalizador.esVacio(html)) {
            return html;
        }

        if (!html.contains("<") || !html.contains(">")) {
            return html;
        }

        StringBuilder resultado = new StringBuilder(html.length() + 32);
        boolean dentroDeTag = false;

        int indice = 0;
        while (indice < html.length()) {
            char c = html.charAt(indice);

            if (c == '<') {
                dentroDeTag = true;
                resultado.append(c);
            } else if (c == '>') {
                dentroDeTag = false;
                resultado.append(c);
            } else if (c == '"' && dentroDeTag) {
                resultado.append("\\\"");
            } else if (c == '\\' && indice + 1 < html.length() && html.charAt(indice + 1) == '"') {
                resultado.append("\\\\\"");
                indice += 2;
                continue;
            } else {
                resultado.append(c);
            }
            indice++;
        }

        return resultado.toString();
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

    private static String repararComas(String texto) {
        String res = COMILLA_ESCAPE_PATTERN.matcher(texto).replaceAll("$1");
        res = DOBLE_COMILLA_PATTERN.matcher(res).replaceAll("\", \"");
        return res;
    }

    private static String extraerParesClaveValor(String texto) {
        StringBuilder json = new StringBuilder();
        json.append("{");

        Matcher matcher = PATRON_PAR_STRING.matcher(texto);

        List<String> pares = new ArrayList<>();
        while (matcher.find()) {
            String clave = matcher.group(1);
            String valor = matcher.group(2);
            pares.add("\"" + clave + "\": \"" + valor + "\"");
        }

        Matcher matcher2 = PATRON_PAR_BOOLEANO_NUMERO.matcher(texto);

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
