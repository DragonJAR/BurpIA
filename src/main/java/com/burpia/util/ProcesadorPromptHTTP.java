package com.burpia.util;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nUI;
import com.burpia.model.SolicitudAnalisis;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.burpia.util.Normalizador.esVacia;
import static com.burpia.util.Normalizador.esVacio;
import static com.burpia.util.Normalizador.noEsVacia;
import static com.burpia.util.Normalizador.noEsVacio;

/**
 * Procesa prompts con variables HTTP dinámicas.
 * 
 * <p>Solo reemplaza variables: {REQUEST}, {RESPONSE}, {OUTPUT_LANGUAGE}</p>
 * <p>NO modifica el contenido del prompt del usuario.</p>
 * 
 * <p>Patrones soportados:</p>
 * <ul>
 *   <li>{REQUEST} - Request HTTP individual</li>
 *   <li>{RESPONSE} - Response HTTP individual</li>
 *   <li>{OUTPUT_LANGUAGE} - Idioma de salida</li>
 *   <li>{REQUEST_N} - Request N-ésimo en flujo</li>
 *   <li>{RESPONSE_N} - Response N-ésimo en flujo</li>
 * </ul>
 * 
 * @see PoliticaMemoria#MAXIMO_CUERPO_ANALISIS_CARACTERES
 */
public final class ProcesadorPromptHTTP {

    private static final String TOKEN_REQUEST = "{REQUEST}";
    private static final String TOKEN_RESPONSE = "{RESPONSE}";
    private static final String TOKEN_OUTPUT_LANGUAGE = "{OUTPUT_LANGUAGE}";
    private static final Pattern PATRON_REQUEST_NUMERADO = Pattern.compile("\\{REQUEST_(\\d+)\\}");
    private static final Pattern PATRON_RESPONSE_NUMERADO = Pattern.compile("\\{RESPONSE_(\\d+)\\}");

    private ProcesadorPromptHTTP() {
        throw new UnsupportedOperationException(
            "ProcesadorPromptHTTP es una clase de utilidad y no puede instanciarse"
        );
    }
    
    /**
     * Procesa prompt para análisis individual.
     * 
     * @param promptBase Prompt del usuario con variables (puede ser null/vacío, se usa default)
     * @param solicitud Datos HTTP a analizar (no puede ser null)
     * @param config Configuración actual (no puede ser null)
     * @return Prompt procesado con variables reemplazadas
     * @throws IllegalArgumentException si solicitud o config son null
     */
    public static String procesarIndividual(String promptBase, SolicitudAnalisis solicitud, ConfiguracionAPI config) {
        validarParametrosIndividuales(solicitud, config);
        
        String prompt = obtenerPromptEfectivo(promptBase);
        
        return prompt
            .replace(TOKEN_REQUEST, construirRequest(solicitud))
            .replace(TOKEN_RESPONSE, construirResponse(solicitud))
            .replace(TOKEN_OUTPUT_LANGUAGE, obtenerIdiomaSalida(config));
    }
    
    /**
     * Procesa prompt para análisis de flujo (múltiples transacciones).
     * 
     * <p>Si la lista de solicitudes está vacía, retorna el prompt con solo
     * {OUTPUT_LANGUAGE} reemplazado, sin bloques de transacción.</p>
     * 
     * @param promptBase Prompt del usuario (puede ser null/vacío, se usa default)
     * @param solicitudes Lista de solicitudes HTTP (no puede ser null, puede estar vacía)
     * @param config Configuración actual (no puede ser null)
     * @return Prompt procesado con bloques de transacción
     * @throws IllegalArgumentException si solicitudes es null o config es null
     */
    public static String procesarFlujo(String promptBase, List<SolicitudAnalisis> solicitudes, ConfiguracionAPI config) {
        validarParametrosFlujo(solicitudes, config);
        
        String prompt = obtenerPromptEfectivo(promptBase);
        String resultado = prompt;

        if (noEsVacia(solicitudes)) {
            resultado = reemplazarContenidoFlujo(
                resultado,
                construirRequestsFlujo(solicitudes),
                construirResponsesFlujo(solicitudes)
            );
        }
        
        resultado = resultado.replace(TOKEN_OUTPUT_LANGUAGE, obtenerIdiomaSalida(config));
        
        return resultado;
    }

    public static boolean contieneMarcadoresRequest(String prompt) {
        return noEsVacio(prompt)
            && (prompt.contains(TOKEN_REQUEST) || PATRON_REQUEST_NUMERADO.matcher(prompt).find());
    }

    public static boolean contieneMarcadoresResponse(String prompt) {
        return noEsVacio(prompt)
            && (prompt.contains(TOKEN_RESPONSE) || PATRON_RESPONSE_NUMERADO.matcher(prompt).find());
    }

    public static boolean contieneMarcadoresHttp(String prompt) {
        return contieneMarcadoresRequest(prompt) || contieneMarcadoresResponse(prompt);
    }

    public static String reemplazarContenidoFlujo(String promptBase, List<String> requests, List<String> responses) {
        String prompt = promptBase != null ? promptBase : "";
        if (!contieneMarcadoresHttp(prompt)) {
            return prompt;
        }
        if (esVacia(requests) && esVacia(responses)) {
            return prompt;
        }

        String resultado = reemplazarMarcadoresNumerados(prompt, requests, responses);

        if (resultado.contains(TOKEN_REQUEST)) {
            resultado = resultado.replace(TOKEN_REQUEST, construirBloquesEnumerados("REQUEST", requests));
        }
        if (resultado.contains(TOKEN_RESPONSE)) {
            resultado = resultado.replace(TOKEN_RESPONSE, construirBloquesEnumerados("RESPONSE", responses));
        }

        return resultado;
    }
    
    /**
     * Detecta el modo de análisis basado en los datos.
     * 
     * @param solicitud Solicitud a analizar (no puede ser null)
     * @return "SINGLE" si hay response, "REQUEST_ONLY" si no
     * @throws IllegalArgumentException si solicitud es null
     */
    public static String detectarModo(SolicitudAnalisis solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException(I18nUI.General.ERROR_SOLICITUD_NULA());
        }
        
        boolean tieneResponse = solicitud.obtenerCodigoEstadoRespuesta() > 0 || 
                                noEsVacio(solicitud.obtenerCuerpoRespuesta()) ||
                                noEsVacio(solicitud.obtenerEncabezadosRespuesta());
        
        return tieneResponse ? "SINGLE" : "REQUEST_ONLY";
    }
    
    // ============ VALIDACIONES ============
    
    private static void validarParametrosIndividuales(SolicitudAnalisis solicitud, ConfiguracionAPI config) {
        if (solicitud == null) {
            throw new IllegalArgumentException(I18nUI.General.ERROR_SOLICITUD_NULA());
        }
        if (config == null) {
            throw new IllegalArgumentException(I18nUI.General.ERROR_CONFIGURACION_NULA_ARGUMENTO());
        }
    }
    
    private static void validarParametrosFlujo(List<SolicitudAnalisis> solicitudes, ConfiguracionAPI config) {
        if (solicitudes == null) {
            throw new IllegalArgumentException(I18nUI.General.ERROR_LISTA_SOLICITUDES_NULA());
        }
        if (config == null) {
            throw new IllegalArgumentException(I18nUI.General.ERROR_CONFIGURACION_NULA_ARGUMENTO());
        }
        // Lista vacía es tolerada: retorna prompt sin bloques de transacción
    }
    
    private static String obtenerPromptEfectivo(String promptBase) {
        return esVacio(promptBase) ? ConfiguracionAPI.obtenerPromptPorDefecto() : promptBase;
    }
    
    // ============ MÉTODOS PRIVADOS DE CONSTRUCCIÓN ============
    
    private static String reemplazarMarcadoresNumerados(String prompt, List<String> requests, List<String> responses) {
        String resultado = prompt;

        int totalPasos = Math.max(
            requests != null ? requests.size() : 0,
            responses != null ? responses.size() : 0
        );

        for (int i = 0; i < totalPasos; i++) {
            String marcadorRequest = "{REQUEST_" + (i + 1) + "}";
            String marcadorResponse = "{RESPONSE_" + (i + 1) + "}";

            String request = requests != null && i < requests.size() ? normalizarContenido(requests.get(i)) : "";
            String response = responses != null && i < responses.size() ? normalizarContenido(responses.get(i)) : "";
            resultado = resultado.replace(marcadorRequest, request);
            resultado = resultado.replace(marcadorResponse, response);
        }

        return resultado;
    }

    private static String construirBloquesEnumerados(String etiqueta, List<String> contenidos) {
        if (esVacia(contenidos)) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < contenidos.size(); i++) {
            String contenido = normalizarContenido(contenidos.get(i));
            if (esVacio(contenido)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append("=== ").append(etiqueta).append(" ").append(i + 1).append(" ===\n");
            builder.append(contenido);
        }
        return builder.toString();
    }

    private static List<String> construirRequestsFlujo(List<SolicitudAnalisis> solicitudes) {
        List<String> requests = new ArrayList<>();
        for (SolicitudAnalisis solicitud : solicitudes) {
            requests.add(solicitud != null ? construirRequest(solicitud) : "");
        }
        return requests;
    }

    private static List<String> construirResponsesFlujo(List<SolicitudAnalisis> solicitudes) {
        List<String> responses = new ArrayList<>();
        for (SolicitudAnalisis solicitud : solicitudes) {
            responses.add(construirResponseFlujo(solicitud));
        }
        return responses;
    }

    private static String construirRequest(SolicitudAnalisis solicitud) {
        if (solicitud == null) {
            return "[REQUEST NOT AVAILABLE]";
        }

        String encabezados = valorSeguro(solicitud.obtenerEncabezados());
        String cuerpo = valorSeguro(solicitud.obtenerCuerpo());
        String lineaInicial = construirLineaRequest(solicitud);
        StringBuilder request = new StringBuilder();

        if (noEsVacio(encabezados)) {
            if (encabezados.startsWith(lineaInicial) || encabezados.startsWith(obtenerPrefijoRequest(solicitud))) {
                request.append(encabezados);
            } else {
                request.append(lineaInicial).append("\n").append(encabezados);
            }
        } else {
            request.append(lineaInicial);
        }

        if (noEsVacio(cuerpo)) {
            request.append("\n").append(limitarCuerpo(cuerpo));
        }

        return request.toString();
    }

    private static String construirResponse(SolicitudAnalisis solicitud) {
        if (solicitud == null) {
            return "HTTP/1.1 0";
        }

        String encabezadosRespuesta = valorSeguro(solicitud.obtenerEncabezadosRespuesta());
        String cuerpoRespuesta = valorSeguro(solicitud.obtenerCuerpoRespuesta());
        String lineaEstado = construirLineaEstado(solicitud.obtenerCodigoEstadoRespuesta());
        StringBuilder response = new StringBuilder();

        if (noEsVacio(encabezadosRespuesta)) {
            if (encabezadosRespuesta.startsWith("HTTP/")) {
                response.append(encabezadosRespuesta);
            } else {
                response.append(lineaEstado).append("\n").append(encabezadosRespuesta);
            }
        }

        if (response.length() == 0) {
            response.append(lineaEstado);
        }
        if (noEsVacio(cuerpoRespuesta)) {
            response.append("\n").append(limitarCuerpo(cuerpoRespuesta));
        }

        return response.toString();
    }

    private static String construirResponseFlujo(SolicitudAnalisis solicitud) {
        if (solicitud == null) {
            return "";
        }

        String encabezadosRespuesta = valorSeguro(solicitud.obtenerEncabezadosRespuesta());
        String cuerpoRespuesta = valorSeguro(solicitud.obtenerCuerpoRespuesta());
        int codigoEstado = solicitud.obtenerCodigoEstadoRespuesta();
        if (esVacio(encabezadosRespuesta) && esVacio(cuerpoRespuesta) && codigoEstado < 0) {
            return "";
        }

        StringBuilder response = new StringBuilder();
        if (noEsVacio(encabezadosRespuesta)) {
            if (encabezadosRespuesta.startsWith("HTTP/")) {
                response.append(encabezadosRespuesta);
            } else {
                response.append(construirLineaEstado(codigoEstado)).append("\n").append(encabezadosRespuesta);
            }
        } else if (codigoEstado >= 0) {
            response.append(construirLineaEstado(codigoEstado));
        }

        if (noEsVacio(cuerpoRespuesta)) {
            if (response.length() > 0) {
                response.append("\n");
            }
            response.append(limitarCuerpo(cuerpoRespuesta));
        }

        return response.toString();
    }

    private static String obtenerIdiomaSalida(ConfiguracionAPI config) {
        String idiomaUi = config.obtenerIdiomaUi();
        return "es".equalsIgnoreCase(idiomaUi) ? "Spanish" : "English";
    }

    private static String normalizarContenido(String contenido) {
        return contenido != null ? contenido : "";
    }

    private static String valorSeguro(String valor) {
        return valor != null ? valor : "";
    }

    private static String construirLineaRequest(SolicitudAnalisis solicitud) {
        return obtenerPrefijoRequest(solicitud) + " HTTP/1.1";
    }

    private static String obtenerPrefijoRequest(SolicitudAnalisis solicitud) {
        return valorSeguro(solicitud.obtenerMetodo()) + " " + valorSeguro(solicitud.obtenerUrl());
    }

    private static String construirLineaEstado(int codigoEstado) {
        return "HTTP/1.1 " + codigoEstado;
    }

    private static String limitarCuerpo(String cuerpo) {
        if (esVacio(cuerpo)) {
            return "";
        }
        
        if (cuerpo.length() > PoliticaMemoria.MAXIMO_CUERPO_ANALISIS_CARACTERES) {
            return cuerpo.substring(0, PoliticaMemoria.MAXIMO_CUERPO_ANALISIS_CARACTERES) 
                + "\n... [TRUNCATED - Body exceeds " + PoliticaMemoria.MAXIMO_CUERPO_ANALISIS_CARACTERES + " chars]";
        }
        return cuerpo;
    }
}
