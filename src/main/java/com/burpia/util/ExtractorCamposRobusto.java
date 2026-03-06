package com.burpia.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidad genérica para extraer campos de texto estructurado malformado.
 *
 * <p>Diseñado para situaciones donde el texto tiene estructura de campos
 * pero NO es JSON válido por:</p>
 *
 * <ul>
 *   <li>Comillas sin cerrar</li>
 *   <li>Falta de delimitadores ({ })</li>
 *   <li>Campos duplicados</li>
 *   <li>Falta de comas entre objetos</li>
 * </ul>
 *
 * <p><b>Principio de diseño:</b> Genérico y reutilizable.</p>
 *
 * @since 1.0.3
 */
public final class ExtractorCamposRobusto {

    /**
     * Representa un campo extraído con sus variaciones de nombre.
     *
     * <p>Contiene el nombre del campo y sus variaciones en diferentes idiomas
     * o formatos (ej: "titulo", "title", "name").</p>
     */
    public static class Campo {
        private final String nombre;
        private final String[] variaciones;
        private final Pattern patron;

        /**
         * Crea un campo con sus variaciones.
         *
         * @param nombre Nombre principal del campo
         * @param variaciones Variaciones del nombre (i18n, aliases)
         */
        Campo(String nombre, String... variaciones) {
            this.nombre = nombre;
            this.variaciones = variaciones != null ? Arrays.copyOf(variaciones, variaciones.length) : new String[0];
            this.patron = compilarPatronCampo(this.variaciones);
        }

        /**
         * Obtiene el nombre principal del campo.
         *
         * @return Nombre del campo
         */
        public String obtenerNombre() {
            return nombre;
        }

        /**
         * Obtiene el patrón regex compilado para este campo.
         *
         * @return Patrón regex pre-compilado
         */
        public Pattern obtenerPatron() {
            return patron;
        }

        /**
         * Obtiene las variaciones del nombre del campo.
         *
         * @return Array con las variaciones del campo (i18n, aliases)
         */
        public String[] obtenerVariaciones() {
            return variaciones.clone();
        }

        /**
         * Compila el patrón regex para las variaciones del campo.
         * Formato: "campo1|campo2|campo3" (case-insensitive)
         *
         * @param variaciones Variaciones del campo
         * @return Patrón compilado
         */
        private static Pattern compilarPatronCampo(String... variaciones) {
            StringBuilder sb = new StringBuilder("\"(?:");
            for (int i = 0; i < variaciones.length; i++) {
                if (i > 0) {
                    sb.append("|");
                }
                sb.append(Pattern.quote(variaciones[i]));
            }
            sb.append(")\"\\s*:\\s*\"");
            return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
        }
    }

    /**
     * Campos predefinidos para hallazgos de seguridad.
     * Reutilizable en toda la aplicación.
     */
    public static final class CamposHallazgo {
        /** Campo de título/delimitador principal */
        public static final Campo TITULO = new Campo("titulo",
            "titulo", "title", "name", "nombre");

        /** Campo de descripción/detalles */
        public static final Campo DESCRIPCION = new Campo("descripcion",
            "descripcion", "description", "finding", "hallazgo", "details");

        /** Campo de severidad/riesgo */
        public static final Campo SEVERIDAD = new Campo("severidad",
            "severidad", "severity", "risk", "impacto");

        /** Campo de confianza/certeza */
        public static final Campo CONFIANZA = new Campo("confianza",
            "confianza", "confidence", "certainty", "certeza");

        /** Campo de evidencia/prueba */
        public static final Campo EVIDENCIA = new Campo("evidencia",
            "evidencia", "evidence", "proof", "indicator");

        /**
         * Todos los campos en orden de prioridad.
         */
        public static final Campo[] TODOS = {
            TITULO, DESCRIPCION, SEVERIDAD, CONFIANZA, EVIDENCIA
        };

        /**
         * Obtiene las variaciones de un campo por su nombre.
         *
         * @param nombreCampo Nombre del campo
         * @return Array de variaciones o array vacío si no existe
         */
        public static String[] obtenerVariaciones(String nombreCampo) {
            if (Normalizador.esVacio(nombreCampo)) {
                return new String[0];
            }

            String nombreLower = nombreCampo.toLowerCase();
            for (Campo campo : TODOS) {
                if (campo.obtenerNombre().equalsIgnoreCase(nombreLower)) {
                    return campo.obtenerVariaciones();
                }
            }
            return new String[]{nombreCampo};
        }

        private CamposHallazgo() {
        }
    }

    /**
     * Extrae bloques de texto usando un campo como delimitador.
     *
     * <p><b>Estrategia:</b> Busca todas las ocurrencias del campo especificado
     * y extrae el texto desde cada ocurrencia hasta la siguiente.</p>
     *
     * <p><b>Ventaja:</b> No depende de llaves {} balanceadas.</p>
     *
     * @param contenido Texto estructurado malformado
     * @param campoDelimitador Campo que marca el inicio de cada bloque
     * @return Lista de bloques, uno por ocurrencia del campo
     */
    public static List<String> extraerBloquesPorCampo(String contenido, Campo campoDelimitador) {
        if (Normalizador.esVacio(contenido) || campoDelimitador == null) {
            return new ArrayList<>();
        }

        List<String> bloques = new ArrayList<>();
        Pattern patron = campoDelimitador.obtenerPatron();
        Matcher matcher = patron.matcher(contenido);

        List<Integer> posiciones = new ArrayList<>();
        while (matcher.find()) {
            posiciones.add(matcher.start());
        }

        if (posiciones.isEmpty()) {
            return bloques;
        }

        for (int i = 0; i < posiciones.size(); i++) {
            int inicio = posiciones.get(i);
            int fin = (i + 1 < posiciones.size())
                ? posiciones.get(i + 1)
                : contenido.length();

            String bloque = contenido.substring(inicio, fin).trim();
            if (Normalizador.noEsVacio(bloque)) {
                bloques.add(bloque);
            }
        }

        return bloques;
    }

    /**
     * Extrae el valor de un campo de un bloque usando 3 estrategias secuenciales.
     *
     * <p>Estrategias (en orden):</p>
     * <ol>
     *   <li>ParserRespuestasAI.extraerCampoNoEstricto() - JSON casi válido</li>
     *   <li>Regex permisivo - comillas sin cerrar</li>
     *   <li>Búsqueda greedy - hasta próximo campo conocido</li>
     * </ol>
     *
     * @param campo Campo a extraer
     * @param bloque Bloque de texto del hallazgo
     * @return Valor extraído o vacío si no se encuentra
     */
    public static String extraerCampoDeBloque(Campo campo, String bloque) {
        if (campo == null || Normalizador.esVacio(bloque)) {
            return "";
        }

        String valor = extraerCampoEstricto(campo, bloque);
        if (Normalizador.noEsVacio(valor)) {
            return valor;
        }

        valor = extraerCampoPermisivo(campo, bloque);
        if (Normalizador.noEsVacio(valor)) {
            return valor;
        }

        valor = extraerCampoGreedy(campo, bloque);
        if (Normalizador.noEsVacio(valor)) {
            return valor;
        }

        return "";
    }

    /**
     * Estrategia 1: Método estándar para JSON casi válido.
     * Busca TODAS las ocurrencias del campo y toma la última.
     *
     * @param campo Campo a extraer
     * @param bloque Bloque de texto
     * @return Valor extraído o vacío
     */
    private static String extraerCampoEstricto(Campo campo, String bloque) {
        String ultimoValor = "";
        String[] variaciones = campo.obtenerVariaciones();

        for (String variacion : variaciones) {
            String patronStr = "\"" + Pattern.quote(variacion) + "\"\\s*:\\s*\"([^\"]*)";
            Pattern patron = Pattern.compile(patronStr, Pattern.CASE_INSENSITIVE);
            Matcher matcher = patron.matcher(bloque);

            while (matcher.find()) {
                String valor = matcher.group(1);
                if (Normalizador.noEsVacio(valor)) {
                    ultimoValor = valor;
                }
            }
        }

        return ultimoValor;
    }

    /**
     * Estrategia 2: Regex permisivo para comillas sin cerrar.
     *
     * @param campo Campo a extraer
     * @param bloque Bloque de texto
     * @return Valor extraído o vacío
     */
    private static String extraerCampoPermisivo(Campo campo, String bloque) {
        Pattern patron = campo.obtenerPatron();
        Matcher matcher = patron.matcher(bloque);

        if (matcher.find()) {
            int inicioValor = matcher.end();
            int finValor = encontrarFinDeValor(bloque, inicioValor);
            String valor = bloque.substring(inicioValor, finValor);
            valor = valor.replaceAll("[\",\\}]+$", "").trim();

            if (Normalizador.noEsVacio(valor)) {
                return valor;
            }
        }

        return "";
    }

    /**
     * Estrategia 3: Búsqueda greedy hasta próximo campo.
     *
     * @param campo Campo a extraer
     * @param bloque Bloque de texto
     * @return Valor extraído o vacío
     */
    private static String extraerCampoGreedy(Campo campo, String bloque) {
        Pattern patron = campo.obtenerPatron();
        Matcher matcher = patron.matcher(bloque);

        if (!matcher.find()) {
            return "";
        }

        int inicioValor = matcher.end();
        int finValor = encontrarProximoCampo(bloque, inicioValor);
        String valor = bloque.substring(inicioValor, finValor);

        valor = valor.replaceAll("[^\\p{Print}]", " ");
        valor = valor.trim();
        valor = valor.replaceAll("[\",\\}]+$", "");

        return valor;
    }

    /**
     * Encuentra el fin de un valor buscando cierre de comillas o siguiente campo.
     *
     * @param texto Texto completo
     * @param inicio Posición desde donde buscar
     * @return Posición donde termina el valor
     */
    private static int encontrarFinDeValor(String texto, int inicio) {
        if (Normalizador.esVacio(texto) || inicio < 0 || inicio >= texto.length()) {
            return texto != null ? texto.length() : 0;
        }

        int finComilla = texto.indexOf('"', inicio);
        if (finComilla >= 0) {
            return finComilla;
        }

        return encontrarProximoCampo(texto, inicio);
    }

    /**
     * Encuentra la posición del siguiente campo conocido.
     *
     * @param texto Texto completo
     * @param inicio Posición desde donde buscar
     * @return Posición del próximo campo o fin del texto
     */
    private static int encontrarProximoCampo(String texto, int inicio) {
        if (Normalizador.esVacio(texto) || inicio < 0 || inicio >= texto.length()) {
            return texto != null ? texto.length() : 0;
        }

        int posicionMasCercana = texto.length();

        for (Campo campo : CamposHallazgo.TODOS) {
            Pattern patron = campo.obtenerPatron();
            Matcher matcher = patron.matcher(texto);
            matcher.region(inicio, texto.length());

            if (matcher.find()) {
                int pos = matcher.start();
                if (pos < posicionMasCercana) {
                    posicionMasCercana = pos;
                }
            }
        }

        return posicionMasCercana;
    }

    private ExtractorCamposRobusto() {
    }
}
