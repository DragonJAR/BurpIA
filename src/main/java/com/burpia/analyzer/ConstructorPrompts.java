package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.SolicitudAnalisis;

public class ConstructorPrompts {
    private static final String TOKEN_REQUEST = "{REQUEST}";
    private static final String TOKEN_RESPONSE = "{RESPONSE}";
    private static final int MAX_CARACTERES_CUERPO_REQUEST = 12000;
    private static final int MAX_CARACTERES_CUERPO_RESPONSE = 12000;

    private final ConfiguracionAPI config;

    /**
     * Constructor con configuración.
     * @param config Configuración a usar para obtener el prompt configurable
     */
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

        boolean teniaTokenRequest = promptTemplate.contains(TOKEN_REQUEST);
        boolean teniaTokenResponse = promptTemplate.contains(TOKEN_RESPONSE);

        String promptFinal = promptTemplate
            .replace(TOKEN_REQUEST, requestContenido)
            .replace(TOKEN_RESPONSE, responseContenido);

        if (!teniaTokenRequest) {
            promptFinal += "\n\nREQUEST:\n" + requestContenido;
        }
        if (!teniaTokenResponse) {
            promptFinal += "\n\nRESPONSE:\n" + responseContenido;
        }

        return promptFinal;
    }

    private String construirBloqueRequest(SolicitudAnalisis solicitud) {
        if (solicitud == null) {
            return "[REQUEST NO DISPONIBLE]";
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
                .append(truncarTexto(cuerpo, MAX_CARACTERES_CUERPO_REQUEST, "request body"));
        }

        return requestBuilder.toString();
    }

    private String construirBloqueResponse(SolicitudAnalisis solicitud) {
        if (solicitud == null) {
            return "STATUS: N/A\n[RESPONSE NO DISPONIBLE]";
        }

        int status = solicitud.obtenerCodigoEstadoRespuesta();
        String encabezados = valorNoVacio(solicitud.obtenerEncabezadosRespuesta(), "");
        String cuerpo = valorNoVacio(solicitud.obtenerCuerpoRespuesta(), "");

        if (status < 0 && encabezados.isEmpty() && cuerpo.isEmpty()) {
            return "STATUS: N/A\n[RESPONSE NO DISPONIBLE]";
        }

        StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append("STATUS: ").append(status >= 0 ? status : "N/A").append("\n");

        if (!encabezados.isEmpty()) {
            responseBuilder.append(encabezados);
        } else {
            responseBuilder.append("[HEADERS NO DISPONIBLES]\n");
        }

        if (!cuerpo.isEmpty()) {
            responseBuilder.append("\nBODY:\n")
                .append(truncarTexto(cuerpo, MAX_CARACTERES_CUERPO_RESPONSE, "response body"));
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
            "\n[TRUNCADO " + etiqueta + ": +" + truncados + " caracteres]";
    }

    private String valorNoVacio(String valor, String valorDefecto) {
        if (valor == null) {
            return valorDefecto;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? valorDefecto : valor;
    }
}
