package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.SolicitudAnalisis;

public class ConstructorPrompts {

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

    /**
     * Constructor por defecto con prompt legacy.
     * @deprecated Usar {@link #ConstructorPrompts(ConfiguracionAPI)} en su lugar.
     * Este constructor será eliminado en v2.0.
     */
    @Deprecated
    public ConstructorPrompts() {
        this.config = new ConfiguracionAPI(); // Crear config por defecto en lugar de null
    }

    public String construirPromptAnalisis(SolicitudAnalisis solicitud) {
        String promptTemplate = (config != null) ? config.obtenerPromptConfigurable() : obtenerPromptLegacy();

        // Construir la parte de la solicitud HTTP
        StringBuilder requestBuilder = new StringBuilder();
        requestBuilder.append(solicitud.obtenerMetodo()).append(" ").append(solicitud.obtenerUrl()).append("\n");
        requestBuilder.append(solicitud.obtenerEncabezados());

        if (!solicitud.obtenerCuerpo().isEmpty()) {
            requestBuilder.append("\nBODY:\n").append(solicitud.obtenerCuerpo());
        }

        // Reemplazar {REQUEST} con la solicitud HTTP actual
        return promptTemplate.replace("{REQUEST}", requestBuilder.toString());
    }

    private String obtenerPromptLegacy() {
        return "Act as an offensive security expert. Analyze this HTTP request " +
               "for OWASP Top 10 vulnerabilities. Respond in Spanish.\n\n" +
               "REQUEST:\n" +
               "{REQUEST}\n\n" +
               "IMPORTANT: You must respond ONLY with a valid JSON, no additional text. " +
               "Use this exact format:\n" +
               "{\"hallazgos\": [\n" +
               "  {\"descripcion\": \"Short vulnerability description\", \"severidad\": \"Critical|High|Medium|Low|Info\", \"confianza\": \"High|Medium|Low\"},\n" +
               "  {\"descripcion\": \"Another vulnerability found\", \"severidad\": \"High\", \"confianza\": \"Medium\"}\n" +
               "]}\n\n" +
               "If no vulnerabilities are found, return an empty array: {\"hallazgos\": []}";
    }
}
