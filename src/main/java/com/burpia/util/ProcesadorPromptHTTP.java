package com.burpia.util;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.SolicitudAnalisis;

import java.util.List;
import java.util.regex.Matcher;
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
    
    private static final Pattern PATRON_REQUEST_NUMERADO = Pattern.compile("\\{REQUEST_(\\d+)\\}");
    private static final Pattern PATRON_RESPONSE_NUMERADO = Pattern.compile("\\{RESPONSE_(\\d+)\\}");
    
    /**
     * Sección de ejemplo en prompts que debe ser reemplazada por transacciones reales.
     * Busca desde "For a single transaction" hasta el cierre de http_transaction.
     */
    private static final Pattern PATRON_SECCION_EJEMPLO = Pattern.compile(
        "For a single transaction, use:.*?For a flow, use one block per step:.*?</http_transaction>",
        Pattern.DOTALL
    );
    
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
            .replace("{REQUEST}", construirRequest(solicitud))
            .replace("{RESPONSE}", construirResponse(solicitud))
            .replace("{OUTPUT_LANGUAGE}", obtenerIdiomaSalida(config));
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
        
        // Si hay solicitudes, procesar bloques de transacción
        if (noEsVacia(solicitudes)) {
            boolean tieneMarcadoresNumerados = PATRON_REQUEST_NUMERADO.matcher(prompt).find();
            
            if (tieneMarcadoresNumerados) {
                resultado = reemplazarMarcadoresNumerados(resultado, solicitudes);
            } else {
                resultado = construirBloquesTransaccion(resultado, solicitudes);
            }
        }
        
        resultado = resultado.replace("{OUTPUT_LANGUAGE}", obtenerIdiomaSalida(config));
        
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
            throw new IllegalArgumentException("La solicitud no puede ser null");
        }
        
        boolean tieneResponse = solicitud.obtenerCodigoEstadoRespuesta() > 0 || 
                                noEsVacio(solicitud.obtenerCuerpoRespuesta()) ||
                                noEsVacio(solicitud.obtenerEncabezadosRespuesta());
        
        return tieneResponse ? "SINGLE" : "REQUEST_ONLY";
    }
    
    // ============ VALIDACIONES ============
    
    private static void validarParametrosIndividuales(SolicitudAnalisis solicitud, ConfiguracionAPI config) {
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no puede ser null");
        }
        if (config == null) {
            throw new IllegalArgumentException("La configuración no puede ser null");
        }
    }
    
    private static void validarParametrosFlujo(List<SolicitudAnalisis> solicitudes, ConfiguracionAPI config) {
        if (solicitudes == null) {
            throw new IllegalArgumentException("La lista de solicitudes no puede ser null");
        }
        if (config == null) {
            throw new IllegalArgumentException("La configuración no puede ser null");
        }
        // Lista vacía es tolerada: retorna prompt sin bloques de transacción
    }
    
    private static String obtenerPromptEfectivo(String promptBase) {
        return esVacio(promptBase) ? ConfiguracionAPI.obtenerPromptPorDefecto() : promptBase;
    }
    
    // ============ MÉTODOS PRIVADOS DE CONSTRUCCIÓN ============
    
    private static String reemplazarMarcadoresNumerados(String prompt, List<SolicitudAnalisis> solicitudes) {
        String resultado = prompt;
        
        for (int i = 0; i < solicitudes.size(); i++) {
            String marcadorRequest = "{REQUEST_" + (i + 1) + "}";
            String marcadorResponse = "{RESPONSE_" + (i + 1) + "}";
            
            SolicitudAnalisis solicitud = solicitudes.get(i);
            resultado = resultado.replace(marcadorRequest, construirRequest(solicitud));
            resultado = resultado.replace(marcadorResponse, construirResponse(solicitud));
        }
        
        return resultado;
    }
    
    private static String construirBloquesTransaccion(String prompt, List<SolicitudAnalisis> solicitudes) {
        StringBuilder bloques = new StringBuilder();
        
        for (int i = 0; i < solicitudes.size(); i++) {
            SolicitudAnalisis solicitud = solicitudes.get(i);
            
            bloques.append("<http_transaction id=\"").append(i + 1).append("\">\n");
            bloques.append("<http_request>\n");
            bloques.append(construirRequest(solicitud));
            bloques.append("\n</http_request>\n");
            bloques.append("<http_response>\n");
            bloques.append(construirResponse(solicitud));
            bloques.append("\n</http_response>\n");
            bloques.append("</http_transaction>");
            
            if (i < solicitudes.size() - 1) {
                bloques.append("\n\n");
            }
        }
        
        // Reemplazar sección de ejemplo con bloques reales
        Matcher matcher = PATRON_SECCION_EJEMPLO.matcher(prompt);
        if (matcher.find()) {
            // Si encuentra la sección de ejemplo, reemplazarla
            return matcher.replaceFirst(bloques.toString());
        } else {
            // Si no encuentra la sección, agregar bloques al final del prompt
            return prompt + "\n\n" + bloques.toString();
        }
    }
    
    private static String construirRequest(SolicitudAnalisis solicitud) {
        StringBuilder request = new StringBuilder();
        
        // Línea inicial
        request.append(solicitud.obtenerMetodo())
               .append(" ")
               .append(solicitud.obtenerUrl())
               .append(" HTTP/1.1\n");
        
        // Headers
        String encabezados = solicitud.obtenerEncabezados();
        if (noEsVacio(encabezados)) {
            request.append(encabezados).append("\n");
        }
        
        // Body
        String cuerpo = solicitud.obtenerCuerpo();
        if (noEsVacio(cuerpo)) {
            request.append("\n").append(limitarCuerpo(cuerpo));
        }
        
        return request.toString();
    }
    
    private static String construirResponse(SolicitudAnalisis solicitud) {
        StringBuilder response = new StringBuilder();
        
        // Status line
        response.append("HTTP/1.1 ")
                .append(solicitud.obtenerCodigoEstadoRespuesta())
                .append("\n");
        
        // Headers
        String encabezadosRespuesta = solicitud.obtenerEncabezadosRespuesta();
        if (noEsVacio(encabezadosRespuesta)) {
            response.append(encabezadosRespuesta).append("\n");
        }
        
        // Body
        String cuerpoRespuesta = solicitud.obtenerCuerpoRespuesta();
        if (noEsVacio(cuerpoRespuesta)) {
            response.append("\n").append(limitarCuerpo(cuerpoRespuesta));
        }
        
        return response.toString();
    }
    
    private static String obtenerIdiomaSalida(ConfiguracionAPI config) {
        String idiomaUi = config.obtenerIdiomaUi();
        return "es".equalsIgnoreCase(idiomaUi) ? "Spanish" : "English";
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
