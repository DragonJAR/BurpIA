package com.burpia.analyzer;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.PoliticaMemoria;

public class ConstructorPrompts {
    private static final String TOKEN_REQUEST = "{REQUEST}";
    private static final String TOKEN_RESPONSE = "{RESPONSE}";
    private static final String TOKEN_OUTPUT_LANGUAGE = "{OUTPUT_LANGUAGE}";

    private final ConfiguracionAPI config;

    public ConstructorPrompts(ConfiguracionAPI config) {
        if (config == null) {
            throw new IllegalArgumentException("ConfiguracionAPI no puede ser null");
        }
        this.config = config;
    }

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

    private String obtenerIdiomaSalida() {
        String idiomaUi = config.obtenerIdiomaUi();
        if ("en".equalsIgnoreCase(idiomaUi)) {
            return "English";
        }
        return "Spanish";
    }

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

    private String trPrompt(String textoEs, String textoEn) {
        return "en".equalsIgnoreCase(config.obtenerIdiomaUi()) ? textoEn : textoEs;
    }

    private String valorNoVacio(String valor, String valorDefecto) {
        if (valor == null) {
            return valorDefecto;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? valorDefecto : valor;
    }

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
