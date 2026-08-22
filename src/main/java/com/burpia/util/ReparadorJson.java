package com.burpia.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidad para reparar JSON potencialmente dañado o mal formado proveniente de respuestas AI.
 * Implementa una cadena de estrategias de reparación para manejar diversos formatos de entrada.
 */
public final class ReparadorJson {

    private static final Pattern MARKDOWN_CODE_BLOCK_START_PATTERN = Pattern.compile("(?m)^```(?:json)?\\s*");
    private static final Pattern MARKDOWN_CODE_BLOCK_END_PATTERN = Pattern.compile("(?m)```\\s*$");
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

        String resultado = colapsarEscapeDobleComilla(texto);

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

    /**
     * Colapsa secuencias {@code \\\"} (backslash escapado + comilla) a {@code \"},
     * pero SOLO cuando la comilla no actúa como cierre de un valor: si el carácter
     * posterior es un delimitador de cierre ({@code ,} {@code }} {@code ]}) o el
     * fin del texto, la secuencia es JSON legítimo (valor terminado en backslash)
     * y colapsarla lo corrompería dejando el string sin cerrar.
     */
    private static String colapsarEscapeDobleComilla(String texto) {
        final String patron = "\\\\\"";
        StringBuilder resultado = new StringBuilder(texto.length());
        int indice = 0;
        while (indice < texto.length()) {
            int pos = texto.indexOf(patron, indice);
            if (pos == -1) {
                resultado.append(texto, indice, texto.length());
                break;
            }
            resultado.append(texto, indice, pos);
            int posSiguiente = pos + patron.length();
            char siguiente = posSiguiente < texto.length() ? texto.charAt(posSiguiente) : '\0';
            if (siguiente == ',' || siguiente == '}' || siguiente == ']' || siguiente == '\0') {
                resultado.append(patron);
            } else {
                resultado.append("\\\"");
            }
            indice = posSiguiente;
        }
        return resultado.toString();
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
            int posCampo = buscarCampo(texto, nombreCampo, indice);
            if (posCampo == -1) {
                resultado.append(texto.substring(indice));
                break;
            }

            // posCampo incluye las comillas del nombre del campo.
            // Buscar ':' después del campo con whitespace OPCIONAL tras los
            // dos puntos (el LLM emite tanto "evidencia": "x" como
            // "evidencia":"x" o "evidencia":  "x").
            int posDosPuntos = -1;
            for (int i = posCampo + nombreCampo.length() + 2; i < texto.length(); i++) {
                if (texto.charAt(i) == ':') {
                    posDosPuntos = i;
                    break;
                }
            }

            if (posDosPuntos == -1) {
                // Sin ':' para este campo: avanzar indice de forma
                // segura sin exceder el límite del texto (evita offsets fuera de
                // rango cuando el campo truncado está al final del payload LLM).
                indice = Math.min(posCampo + nombreCampo.length() + 2, texto.length());
                continue;
            }

            // El valor empieza en el primer no-espacio tras ':'
            int posInicioValor = posDosPuntos + 1;
            while (posInicioValor < texto.length() && Character.isWhitespace(texto.charAt(posInicioValor))) {
                posInicioValor++;
            }

            // Agregar todo hasta el inicio del valor (incluye ':' y su whitespace)
            resultado.append(texto.substring(indice, posInicioValor));

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
        // Locale.ROOT: con el locale por defecto turco/azerí, "EVIDENCE".toLowerCase()
        // produce "evıdence" (I sin punto) y no coincide con "evidence". Consistente
        // con el resto del codebase (p.ej. ParserRespuestasAI usa Locale.ROOT).
        String textoLower = texto.toLowerCase(Locale.ROOT);
        String campoLower = "\"" + campo.toLowerCase(Locale.ROOT) + "\"";
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
                // Secuencia \" ya escapada: preservar ambos caracteres y saltar
                // la comilla para evitar que la próxima iteración la re-escape
                // (lo que produciría \\" y corrompería el JSON al parsear).
                resultado.append("\\\"");
                i++;
            } else {
                resultado.append(c);
            }
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

    /**
     * Repara comas finales ({@code ,} antes de {@code }} o {@code ]}) y comas
     * ausentes entre strings adyacentes ({@code "a" "b"} → {@code "a", "b"}).
     * Usa un scanner de estado en lugar de regex para NO tocar el contenido
     * dentro de strings (donde esas secuencias son texto legítimo).
     */
    private static String repararComas(String texto) {
        return insertarComasEntreStrings(eliminarComasFinales(texto));
    }

    private static String eliminarComasFinales(String texto) {
        StringBuilder resultado = new StringBuilder(texto.length());
        boolean enComillas = false;
        boolean escapado = false;
        for (int i = 0; i < texto.length(); i++) {
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
            if (!enComillas && c == ',') {
                int j = i + 1;
                while (j < texto.length() && Character.isWhitespace(texto.charAt(j))) {
                    j++;
                }
                if (j < texto.length() && (texto.charAt(j) == '}' || texto.charAt(j) == ']')) {
                    continue;
                }
            }
            resultado.append(c);
        }
        return resultado.toString();
    }

    private static String insertarComasEntreStrings(String texto) {
        StringBuilder resultado = new StringBuilder(texto.length() + 16);
        boolean enComillas = false;
        boolean escapado = false;
        boolean huboEspaciosTrasCierre = false;
        for (int i = 0; i < texto.length(); i++) {
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
            if (!enComillas && Character.isWhitespace(c)) {
                if (huboEspaciosTrasCierre || ultimoNoEspacioEsComillaCierre(resultado)) {
                    huboEspaciosTrasCierre = true;
                }
                resultado.append(c);
                continue;
            }
            if (c == '"' && !enComillas && huboEspaciosTrasCierre) {
                // String nuevo tras whitespace que sigue a un cierre de string:
                // falta la coma. Compactar el whitespace ya acumulado a ", ".
                int ultimoNoEspacio = resultado.length() - 1;
                while (ultimoNoEspacio >= 0 && Character.isWhitespace(resultado.charAt(ultimoNoEspacio))) {
                    ultimoNoEspacio--;
                }
                resultado.setLength(ultimoNoEspacio + 1);
                resultado.append(", ");
                enComillas = true;
                huboEspaciosTrasCierre = false;
                resultado.append(c);
                continue;
            }
            if (c == '"') {
                enComillas = !enComillas;
            }
            huboEspaciosTrasCierre = false;
            resultado.append(c);
        }
        return resultado.toString();
    }

    private static boolean ultimoNoEspacioEsComillaCierre(StringBuilder sb) {
        int i = sb.length() - 1;
        while (i >= 0 && Character.isWhitespace(sb.charAt(i))) {
            i--;
        }
        return i >= 0 && sb.charAt(i) == '"';
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
