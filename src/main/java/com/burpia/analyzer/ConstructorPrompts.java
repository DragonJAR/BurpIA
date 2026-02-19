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

    public String construirPromptAnalisis(SolicitudAnalisis solicitud) {
        String promptTemplate = config.obtenerPromptConfigurable();

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
}
