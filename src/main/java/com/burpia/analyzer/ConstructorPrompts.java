package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.Normalizador;
import com.burpia.util.PoliticaMemoria;

import java.util.Objects;

/**
 * Constructor de prompts para análisis de seguridad con IA.
 * <p>
 * Esta clase se encarga de construir prompts dinámicos para enviar a los
 * proveedores de IA, incluyendo la solicitud HTTP, respuesta, instrucciones
 * de formato y configuración de idioma.
 * </p>
 *
 * @see ConfiguracionAPI
 * @see SolicitudAnalisis
 */
public class ConstructorPrompts {

    /** Token para reemplazar con el contenido de la solicitud HTTP. */
    private static final String TOKEN_REQUEST = "{REQUEST}";

    /** Token para reemplazar con el contenido de la respuesta HTTP. */
    private static final String TOKEN_RESPONSE = "{RESPONSE}";

    /** Token para reemplazar con el idioma de salida configurado. */
    private static final String TOKEN_OUTPUT_LANGUAGE = "{OUTPUT_LANGUAGE}";

    private final ConfiguracionAPI config;

    /**
     * Crea una nueva instancia del constructor de prompts.
     *
     * @param config la configuración de la API, no puede ser {@code null}
     * @throws NullPointerException si la configuración es {@code null}
     */
    public ConstructorPrompts(ConfiguracionAPI config) {
        this.config = Objects.requireNonNull(config, "ConfiguracionAPI no puede ser null");
    }

    /**
     * Construye un prompt completo para análisis de seguridad.
     * <p>
     * El prompt incluye:
     * </p>
     * <ul>
     *   <li>Template configurable del usuario</li>
     *   <li>Solicitud HTTP (request)</li>
     *   <li>Respuesta HTTP (response)</li>
     *   <li>Instrucciones de idioma de salida</li>
     *   <li>Instrucciones de formato JSON</li>
     * </ul>
     *
     * @param solicitud la solicitud HTTP a analizar, puede ser {@code null}
     * @return el prompt completo construido, nunca {@code null}
     */
    public String construirPromptAnalisis(SolicitudAnalisis solicitud) {
        String promptTemplate = config.obtenerPromptConfigurable();
        if (promptTemplate == null) {
            promptTemplate = "";
        }

        String requestContenido = construirBloqueRequest(solicitud);
        String responseContenido = construirBloqueResponse(solicitud);
        String idiomaSalida = obtenerIdiomaSalida();

        boolean teniaTokenRequest = promptTemplate.contains(TOKEN_REQUEST);
        boolean teniaTokenResponse = promptTemplate.contains(TOKEN_RESPONSE);
        boolean teniaTokenIdioma = promptTemplate.contains(TOKEN_OUTPUT_LANGUAGE);

        String promptFinal = promptTemplate
            .replace(TOKEN_REQUEST, requestContenido)
            .replace(TOKEN_RESPONSE, responseContenido)
            .replace(TOKEN_OUTPUT_LANGUAGE, idiomaSalida);

        if (!teniaTokenRequest) {
            promptFinal += "\n\nREQUEST:\n" + requestContenido;
        }
        if (!teniaTokenResponse) {
            promptFinal += "\n\nRESPONSE:\n" + responseContenido;
        }
        if (!teniaTokenIdioma) {
            promptFinal += "\n\n" + trPrompt("IDIOMA DE SALIDA", "OUTPUT LANGUAGE") + ": " + idiomaSalida +
                "\n" + trPrompt(
                "Escribe \"descripcion\" estrictamente en IDIOMA DE SALIDA. Mantén \"severidad\" y \"confianza\" exactamente con valores canónicos.",
                "Write \"descripcion\" strictly in OUTPUT LANGUAGE. Keep \"severidad\" and \"confianza\" exactly as canonical values."
            );
        }

        if (!promptFinal.contains("json_formatting")) {
            promptFinal += "\n\n" + construirInstruccionesFormatoJson();
        }

        return promptFinal;
    }

    /**
     * Obtiene el idioma de salida configurado.
     *
     * @return "English" si el idioma UI es inglés, "Spanish" en caso contrario
     */
    private String obtenerIdiomaSalida() {
        String idiomaUi = config.obtenerIdiomaUi();
        if ("en".equalsIgnoreCase(idiomaUi)) {
            return "English";
        }
        return "Spanish";
    }

    /**
     * Construye el bloque de texto para la solicitud HTTP.
     * <p>
     * Incluye línea inicial (método + URL), encabezados y cuerpo si están disponibles.
     * El cuerpo se trunca si excede el límite configurado.
     * </p>
     *
     * @param solicitud la solicitud HTTP, puede ser {@code null}
     * @return el bloque de request formateado
     */
    private String construirBloqueRequest(SolicitudAnalisis solicitud) {
        if (solicitud == null) {
            return trPrompt("[REQUEST NO DISPONIBLE]", "[REQUEST NOT AVAILABLE]");
        }

        String lineaInicial = valorNoVacio(solicitud.obtenerMetodo(), "[METHOD NULL]") +
            " " + valorNoVacio(solicitud.obtenerUrl(), "[URL NULL]");
        String encabezados = valorNoVacio(solicitud.obtenerEncabezados(), "");

        StringBuilder requestBuilder = new StringBuilder();
        if (encabezados.startsWith(lineaInicial)) {
            requestBuilder.append(encabezados);
        } else if (!encabezados.isEmpty()) {
            requestBuilder.append(lineaInicial).append("\n").append(encabezados);
        } else {
            requestBuilder.append(lineaInicial).append("\n[HEADERS NULL]");
        }

        String cuerpo = valorNoVacio(solicitud.obtenerCuerpo(), "");
        if (!cuerpo.isEmpty()) {
            requestBuilder.append("\nBODY:\n")
                .append(truncarTexto(
                    cuerpo,
                    PoliticaMemoria.MAXIMO_CUERPO_ANALISIS_CARACTERES,
                    trPrompt("cuerpo de solicitud", "request body")
                ));
        }

        return requestBuilder.toString();
    }

    /**
     * Construye el bloque de texto para la respuesta HTTP.
     * <p>
     * Incluye código de estado, encabezados y cuerpo si están disponibles.
     * El cuerpo se trunca si excede el límite configurado.
     * </p>
     *
     * @param solicitud la solicitud HTTP con datos de respuesta, puede ser {@code null}
     * @return el bloque de response formateado
     */
    private String construirBloqueResponse(SolicitudAnalisis solicitud) {
        if (solicitud == null) {
            return "STATUS: N/A\n" + trPrompt("[RESPONSE NO DISPONIBLE]", "[RESPONSE NOT AVAILABLE]");
        }

        int status = solicitud.obtenerCodigoEstadoRespuesta();
        String encabezados = valorNoVacio(solicitud.obtenerEncabezadosRespuesta(), "");
        String cuerpo = valorNoVacio(solicitud.obtenerCuerpoRespuesta(), "");

        if (status < 0 && encabezados.isEmpty() && cuerpo.isEmpty()) {
            return "STATUS: N/A\n" + trPrompt("[RESPONSE NO DISPONIBLE]", "[RESPONSE NOT AVAILABLE]");
        }

        StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append("STATUS: ").append(status >= 0 ? status : "N/A").append("\n");

        if (!encabezados.isEmpty()) {
            responseBuilder.append(encabezados);
        } else {
            responseBuilder.append(trPrompt("[HEADERS NO DISPONIBLES]", "[HEADERS NOT AVAILABLE]")).append("\n");
        }

        if (!cuerpo.isEmpty()) {
            responseBuilder.append("\nBODY:\n")
                .append(truncarTexto(
                    cuerpo,
                    PoliticaMemoria.MAXIMO_CUERPO_ANALISIS_CARACTERES,
                    trPrompt("cuerpo de respuesta", "response body")
                ));
        } else {
            responseBuilder.append("\nBODY:\n[EMPTY]");
        }

        return responseBuilder.toString();
    }

    /**
     * Trunca un texto si excede el número máximo de caracteres.
     * <p>
     * Si el texto se trunca, se agrega un mensaje indicando cuántos caracteres
     * fueron omitidos.
     * </p>
     *
     * @param texto el texto a truncar, puede ser {@code null}
     * @param maxCaracteres el número máximo de caracteres permitidos
     * @param etiqueta la etiqueta para el mensaje de truncado (ej: "request body")
     * @return el texto truncado con mensaje si aplica, o cadena vacía si el input es {@code null}
     */
    private String truncarTexto(String texto, int maxCaracteres, String etiqueta) {
        if (texto == null) {
            return "";
        }
        if (texto.length() <= maxCaracteres) {
            return texto;
        }
        int truncados = texto.length() - maxCaracteres;
        return texto.substring(0, maxCaracteres) +
            "\n" + trPrompt(
            "[TRUNCADO " + etiqueta + ": +" + truncados + " caracteres]",
            "[TRUNCATED " + etiqueta + ": +" + truncados + " characters]"
        );
    }

    /**
     * Traduce un texto según el idioma de la UI configurado.
     * <p>
     * Este método es específico para traducir texto dentro de los prompts,
     * no para la UI de la aplicación.
     * </p>
     *
     * @param textoEs texto en español
     * @param textoEn texto en inglés
     * @return el texto en el idioma configurado (español por defecto)
     */
    private String trPrompt(String textoEs, String textoEn) {
        return "en".equalsIgnoreCase(config.obtenerIdiomaUi()) ? textoEn : textoEs;
    }

    /**
     * Retorna el valor si no está vacío, o un valor por defecto en caso contrario.
     * <p>
     * Utiliza {@link Normalizador#esVacio(String)} para la validación siguiendo
     * el principio DRY.
     * </p>
     *
     * @param valor el valor a verificar, puede ser {@code null}
     * @param valorDefecto el valor por defecto si el valor está vacío
     * @return el valor trimado si tiene contenido, o el valor por defecto
     */
    private String valorNoVacio(String valor, String valorDefecto) {
        if (Normalizador.esVacio(valor)) {
            return valorDefecto;
        }
        return valor.trim();
    }

    /**
     * Construye las instrucciones de formato JSON para el prompt.
     * <p>
     * Las instrucciones incluyen reglas críticas para el escape correcto de
     * caracteres especiales en contenido HTML dentro de JSON.
     * </p>
     *
     * @return las instrucciones de formato JSON en el idioma configurado
     */
    private String construirInstruccionesFormatoJson() {
        return trPrompt(
            "<json_formatting>" +
            "CRITICAL: When generating JSON responses with HTML content in 'evidencia' field:" +
            "1. Escape ALL double quotes inside HTML tags as backslash-double-quote (\\\") " +
            "2. Examples of CORRECT escaping:" +
            "   - \"evidencia\": \"<img src=\\\"logo.gif\\\">\" " +
            "   - \"evidencia\": \"<form action=\\\"submit.php\\\">\" " +
            "   - \"evidencia\": \"<embed src=\\\"file.swf\\\">\" " +
            "3. NEVER include unescaped quotes inside JSON string values" +
            "4. This prevents JSON parsing errors" +
            "</json_formatting>",

            "<json_formatting>" +
            "CRITICAL: When generating JSON responses with HTML content in 'evidence' field:" +
            "1. Escape ALL double quotes inside HTML tags as backslash-double-quote (\\\") " +
            "2. Examples of CORRECT escaping:" +
            "   - \"evidence\": \"<img src=\\\"logo.gif\\\">\" " +
            "   - \"evidence\": \"<form action=\\\"submit.php\\\">\" " +
            "   - \"evidence\": \"<embed src=\\\"file.swf\\\">\" " +
            "3. NEVER include unescaped quotes inside JSON string values" +
            "4. This prevents JSON parsing errors" +
            "</json_formatting>"
        );
    }
}
