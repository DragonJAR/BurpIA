package com.burpia.flow;

import com.burpia.analyzer.ConstructorPrompts;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.Normalizador;

import java.util.List;

/**
 * Construye una {@link SolicitudAnalisis} única para analizar un flujo completo
 * reutilizando el pipeline normal de tareas.
 */
public final class FlowAnalysisRequestBuilder {
    private FlowAnalysisRequestBuilder() {
    }

    public static SolicitudAnalisis crearSolicitudFlujo(ConfiguracionAPI config, List<SolicitudAnalisis> solicitudes) {
        if (config == null || Normalizador.esVacia(solicitudes)) {
            return null;
        }

        SolicitudAnalisis primera = solicitudes.get(0);
        if (primera == null) {
            return null;
        }

        StringBuilder encabezadosBuilder = new StringBuilder();
        StringBuilder cuerpoBuilder = new StringBuilder();

        for (int i = 0; i < solicitudes.size(); i++) {
            SolicitudAnalisis solicitud = solicitudes.get(i);
            if (solicitud == null) {
                continue;
            }

            encabezadosBuilder.append("[PETICIÓN ").append(i + 1).append("]\n");
            encabezadosBuilder.append(solicitud.obtenerMetodo()).append(" ").append(solicitud.obtenerUrl()).append("\n");
            encabezadosBuilder.append(solicitud.obtenerEncabezados()).append("\n\n");

            String cuerpo = solicitud.obtenerCuerpo();
            if (Normalizador.noEsVacio(cuerpo)) {
                cuerpoBuilder.append("[BODY PETICIÓN ").append(i + 1).append("]\n");
                cuerpoBuilder.append(cuerpo).append("\n\n");
            }
        }

        String promptFlujo = new ConstructorPrompts(config).construirPromptFlujo(solicitudes);
        return new SolicitudAnalisis(
            primera.obtenerUrl(),
            "FLOW",
            encabezadosBuilder.toString(),
            cuerpoBuilder.toString(),
            "flow-" + System.currentTimeMillis(),
            primera.obtenerSolicitudHttp(),
            -1,
            "",
            "",
            promptFlujo
        );
    }
}
