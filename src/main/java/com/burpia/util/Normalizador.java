package com.burpia.util;

/**
 * Utilidad centralizada para normalización y validación de cadenas de texto.
 * <p>
 * Esta clase implementa el principio DRY (Don't Repeat Yourself) centralizando
 * todas las operaciones comunes de validación y normalización de strings.
 * </p>
 *
 * @see <a href="https://github.com/DragonJAR/BurpIA/blob/main/AGENTS.md">Guía de codificación AGENTS.md</a>
 */
public final class Normalizador {

    private Normalizador() {
        // Clase de utilidad, no instanciable
    }

    /**
     * Normaliza un texto desescapando secuencias de escape básicas y eliminando
     * espacios en blanco al inicio y final.
     * <p>
     * Secuencias de escape soportadas:
     * </p>
     * <ul>
     *   <li>{@code \n} - Nueva línea</li>
     *   <li>{@code \r} - Retorno de carro</li>
     *   <li>{@code \t} - Tabulador</li>
     *   <li>{@code \"} - Comilla doble</li>
     *   <li>{@code \\} - Barra invertida</li>
     * </ul>
     *
     * @param valor el texto a normalizar, puede ser {@code null}
     * @return el texto normalizado con secuencias desescapadas y sin espacios extremos,
     *         o cadena vacía si el input es {@code null}
     */
    public static String normalizarTexto(String valor) {
        return desescaparSecuenciasBasicas(valor, false).trim();
    }

    /**
     * Normaliza un texto convirtiendo caracteres de control en espacios y eliminando
     * espacios en blanco al inicio y final.
     * <p>
     * A diferencia de {@link #normalizarTexto(String)}, este método convierte las
     * secuencias de control ({@code \n}, {@code \r}, {@code \t}) en espacios en lugar
     * de sus caracteres de control correspondientes.
     * </p>
     *
     * @param valor el texto a normalizar, puede ser {@code null}
     * @return el texto normalizado con controles convertidos a espacios y sin espacios extremos,
     *         o cadena vacía si el input es {@code null}
     */
    public static String normalizarTextoConControlesEnEspacio(String valor) {
        return desescaparSecuenciasBasicas(valor, true).trim();
    }

    /**
     * Verifica si una cadena está vacía o es {@code null}.
     * <p>
     * Una cadena se considera vacía si es {@code null} o si solo contiene
     * espacios en blanco.
     * </p>
     *
     * @param valor la cadena a verificar
     * @return {@code true} si la cadena es {@code null} o está vacía/blank,
     *         {@code false} en caso contrario
     */
    public static boolean esVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    /**
     * Verifica si una cadena tiene contenido (no está vacía ni es {@code null}).
     * <p>
     * Este método es la inversa de {@link #esVacio(String)}.
     * </p>
     *
     * @param valor la cadena a verificar
     * @return {@code true} si la cadena tiene contenido,
     *         {@code false} si es {@code null} o está vacía/blank
     */
    public static boolean noEsVacio(String valor) {
        return !esVacio(valor);
    }

    /**
     * Sanitiza una API key para visualización segura, ocultando la mayoría del contenido.
     * <p>
     * El formato de salida es:
     * </p>
     * <ul>
     *   <li>Para claves {@code null} o vacías: {@code [NO CONFIGURADA]}</li>
     *   <li>Para claves de 8 caracteres o menos: {@code ****}</li>
     *   <li>Para claves más largas: primeros 4 caracteres + {@code ****} + últimos 4 caracteres</li>
     * </ul>
     *
     * @param apiKey la API key a sanitizar
     * @return la API key sanitizada para visualización segura
     */
    public static String sanitizarApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "[NO CONFIGURADA]";
        }
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * Desescapa secuencias de escape básicas en un texto.
     * <p>
     * Secuencias soportadas: {@code \n}, {@code \r}, {@code \t}, {@code \"}, {@code \\}
     * </p>
     *
     * @param valor el texto con secuencias de escape
     * @param controlesComoEspacio si es {@code true}, convierte \n, \r, \t en espacios
     * @return el texto con secuencias desescapadas
     */
    private static String desescaparSecuenciasBasicas(String valor, boolean controlesComoEspacio) {
        if (valor == null) {
            return "";
        }
        int len = valor.length();
        StringBuilder sb = new StringBuilder(len);
        int indice = 0;
        while (indice < len) {
            char actual = valor.charAt(indice);
            if (actual == '\\' && indice + 1 < len) {
                char siguiente = valor.charAt(indice + 1);
                switch (siguiente) {
                    case 'n':
                        sb.append(controlesComoEspacio ? ' ' : '\n');
                        indice += 2;
                        continue;
                    case 'r':
                        sb.append(controlesComoEspacio ? ' ' : '\r');
                        indice += 2;
                        continue;
                    case 't':
                        sb.append(controlesComoEspacio ? ' ' : '\t');
                        indice += 2;
                        continue;
                    case '"':
                        sb.append('"');
                        indice += 2;
                        continue;
                    case '\\':
                        sb.append('\\');
                        indice += 2;
                        continue;
                    default:
                        // Secuencia no reconocida, mantener el carácter actual
                        break;
                }
            }
            sb.append(actual);
            indice++;
        }
        return sb.toString();
    }
}
