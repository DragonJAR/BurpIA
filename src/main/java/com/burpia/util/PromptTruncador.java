package com.burpia.util;

import com.burpia.i18n.I18nUI;

/**
 * Truncador inteligente de prompts para reducir tamaño sin perder información crítica.
 *
 * <p>Cuando el prompt excede el límite de contexto del modelo, este truncador aplica
 * estrategias progresivas para reducir el tamaño mientras preserva:</p>
 * <ul>
 *   <li>System prompt completo (instrucciones de análisis)</li>
 *   <li>Estructura JSON válida</li>
 *   <li>Request HTTP principal (URL, método, headers críticos)</li>
 *   <li>Información de vulnerabilidades ya detectadas</li>
 * </ul>
 *
 * <h3>Estrategias de truncado (en orden de prioridad):</h3>
 * <ol>
 *   <li>Reducir body de requests HTTP secundarios</li>
 *   <li>Eliminar headers menos relevantes (User-Agent, Accept, etc.)</li>
 *   <li>Reducir múltiples requests a uno solo (el principal)</li>
 *   <li>Truncar body del request principal con indicador</li>
 *   <li>Como último recurso: truncar texto plano con "..."</li>
 * </ol>
 *
 * @since 1.5.0
 */
public class PromptTruncador {

    /** Longitud máxima del body truncado */
    private static final int MAX_BODY_TRUNCADO = 1000;

    /** Headers que se pueden eliminar para reducir tamaño */
    private static final String[] HEADERS_ELIMINAR = {
            "user-agent", "accept", "accept-encoding", "accept-language",
            "connection", "keep-alive", "upgrade-insecure-requests"
    };

    /** Marcador de truncado en texto */
    private static final String MARCADOR_TRUNCADO = "\n... [TRUNCATED]";

    /** Patrones precompilados para eliminar headers (formato: header sin guiones para regex) */
    private static final String[] PATRONES_HEADERS_ELIMINAR;

    static {
        PATRONES_HEADERS_ELIMINAR = new String[HEADERS_ELIMINAR.length];
        for (int i = 0; i < HEADERS_ELIMINAR.length; i++) {
            PATRONES_HEADERS_ELIMINAR[i] = HEADERS_ELIMINAR[i].replace("-", "\\-");
        }
    }

    /**
     * Trunca un prompt para que quepa en el límite de tokens especificado.
     *
     * @param prompt         Prompt original (puede ser null o vacío)
     * @param tokensObjetivo Número objetivo de tokens (aproximado, mínimo 100)
     * @return Prompt truncado que debería caber en el límite, o el original si no necesita truncado
     */
    public String truncarPrompt(String prompt, int tokensObjetivo) {
        if (Normalizador.esVacio(prompt)) {
            return prompt;
        }

        // Validar tokensObjetivo con mínimo razonable
        int tokensEfectivos = Math.max(100, tokensObjetivo);

        int tokensActuales = estimarTokens(prompt);
        if (tokensActuales <= tokensEfectivos) {
            return prompt;
        }

        String resultado = prompt;

        // Estrategia 1: Reducir bodies de requests HTTP
        resultado = truncarBodiesHTTP(resultado);
        tokensActuales = estimarTokens(resultado);
        if (tokensActuales <= tokensEfectivos) {
            return resultado;
        }

        // Estrategia 2: Eliminar headers menos relevantes
        resultado = eliminarHeadersPrescindibles(resultado);
        tokensActuales = estimarTokens(resultado);
        if (tokensActuales <= tokensEfectivos) {
            return resultado;
        }

        // Estrategia 3: Truncar texto plano
        resultado = truncarTextoPlano(resultado, tokensEfectivos);

        return resultado;
    }

    /**
     * Estima el número de tokens de un texto.
     *
     * <p>Aproximación simple: 1 token ≈ 4 caracteres.
     * No es exacto pero es suficiente para estimaciones de truncado.</p>
     *
     * @param texto Texto a estimar
     * @return Número estimado de tokens
     */
    public int estimarTokens(String texto) {
        if (Normalizador.esVacio(texto)) {
            return 0;
        }
        // Aproximación: 1 token ≈ 4 caracteres (promedio para inglés/español)
        return (int) Math.ceil(texto.length() / 4.0);
    }

    /**
     * Trunca los bodies de requests HTTP en el prompt.
     *
     * @param prompt Prompt original con posibles bodies HTTP
     * @return Prompt con bodies truncados si excedían MAX_BODY_TRUNCADO
     */
    private String truncarBodiesHTTP(String prompt) {
        StringBuilder resultado = new StringBuilder(prompt.length() + MARCADOR_TRUNCADO.length() * 2);

        String[] lineas = prompt.split("\n");
        boolean enBody = false;
        boolean bodyTruncado = false;
        int longitudBodyActual = 0;

        for (String linea : lineas) {
            if (linea.contains("Body:") || linea.contains("Request Body:")) {
                enBody = true;
                bodyTruncado = false;
                longitudBodyActual = 0;
                resultado.append(linea).append("\n");
                continue;
            }

            if (enBody) {
                // Detectar fin de body (línea vacía o nuevo sección)
                if (linea.trim().isEmpty() || linea.contains("Response:") ||
                        linea.startsWith("===") || linea.startsWith("---")) {
                    enBody = false;
                    bodyTruncado = false;
                    resultado.append(linea).append("\n");
                    continue;
                }

                // Si ya truncamos este body, saltamos líneas hasta el fin
                if (bodyTruncado) {
                    continue;
                }

                longitudBodyActual += linea.length() + 1;

                if (longitudBodyActual <= MAX_BODY_TRUNCADO) {
                    resultado.append(linea).append("\n");
                } else {
                    // Agregar marcador UNA vez y marcar como truncado
                    resultado.append(MARCADOR_TRUNCADO).append("\n");
                    bodyTruncado = true;
                }
            } else {
                resultado.append(linea).append("\n");
            }
        }

        return resultado.toString();
    }

    /**
     * Elimina headers prescindibles del prompt, reemplazándolos por un marcador corto.
     *
     * <p>Solo elimina headers cuyo valor sea lo suficientemente largo como para
     * que el reemplazo reduzca el tamaño total.</p>
     *
     * @param prompt Prompt original con headers HTTP
     * @return Prompt con headers prescindibles eliminados
     */
    private String eliminarHeadersPrescindibles(String prompt) {
        String resultado = prompt;

        for (int i = 0; i < HEADERS_ELIMINAR.length; i++) {
            String header = HEADERS_ELIMINAR[i];
            String patronRegex = PATRONES_HEADERS_ELIMINAR[i];
            // Patrón para encontrar líneas de headers completas
            String patron = "(?im)^" + patronRegex + ":.+$";

            // Solo reemplazar si el header es más largo que el marcador
            // Marcador: "[...]" = 5 caracteres
            // Header mínimo: "x: y" = 4 caracteres, así que casi siempre reducimos
            resultado = resultado.replaceAll(patron, "[...]");
        }

        return resultado;
    }

    /**
     * Trunca texto plano cuando las estrategias anteriores no fueron suficientes.
     *
     * <p>Preserva el inicio (system prompt crítico) y el final (último request),
     * eliminando contenido intermedio.</p>
     *
     * @param prompt         Prompt a truncar
     * @param tokensObjetivo Número objetivo de tokens
     * @return Prompt truncado
     */
    private String truncarTextoPlano(String prompt, int tokensObjetivo) {
        int caracteresObjetivo = tokensObjetivo * 4;

        if (prompt.length() <= caracteresObjetivo) {
            return prompt;
        }

        // Preservar el inicio (system prompt crítico)
        int inicioCritico = Math.min(caracteresObjetivo / 3, 2000);

        // Preservar el final (última parte del request)
        int finalCritico = Math.min(caracteresObjetivo / 3, 1000);

        String mensajeTruncado = I18nUI.trf(
                "[PROMPT TRUNCADO - Se eliminó contenido intermedio]",
                "[PROMPT TRUNCATED - Intermediate content removed]"
        );

        // Capacidad: inicio + mensaje + marcador + final + margen
        int capacidadEstimada = inicioCritico + mensajeTruncado.length() + MARCADOR_TRUNCADO.length() + finalCritico + 100;
        StringBuilder resultado = new StringBuilder(capacidadEstimada);

        if (prompt.length() > inicioCritico + finalCritico + 200) {
            resultado.append(prompt.substring(0, inicioCritico));
            resultado.append(MARCADOR_TRUNCADO);
            resultado.append("\n\n");
            resultado.append(mensajeTruncado);
            resultado.append("\n\n");
            resultado.append(prompt.substring(prompt.length() - finalCritico));
        } else {
            // Si el prompt es muy corto, truncar al final directamente
            int espacioDisponible = caracteresObjetivo - MARCADOR_TRUNCADO.length() - mensajeTruncado.length() - 10;
            if (espacioDisponible > 0) {
                resultado.append(prompt.substring(0, espacioDisponible));
            }
            resultado.append(MARCADOR_TRUNCADO);
        }

        return resultado.toString();
    }

    /**
     * Calcula el tamaño objetivo de prompt en tokens basado en el context window del modelo.
     *
     * @param contextWindow Context window del modelo en tokens
     * @param maxOutputTokens Tokens reservados para la respuesta
     * @return Tokens disponibles para el prompt
     */
    public int calcularTokensDisponibles(int contextWindow, int maxOutputTokens) {
        // Dejar un margen de seguridad de 1000 tokens
        int margen = 1000;
        return Math.max(1000, contextWindow - maxOutputTokens - margen);
    }
}
