package com.burpia.ui;

import com.burpia.config.AgenteTipo;
import java.util.Objects;

/**
 * Opciones de runtime para la ejecucion de agentes CLI.
 * Proporciona configuracion centralizada para el comportamiento de inyeccion de comandos.
 */
final class AgentRuntimeOptions {

    private static final int DELAY_SUBMIT_POST_PASTE_MS_POR_DEFECTO = 800;

    private AgentRuntimeOptions() {
    }

    /**
     * Carga las opciones de runtime a partir del codigo de agente.
     *
     * @param codigoAgente codigo del tipo de agente (puede ser null o vacio)
     * @return opciones de runtime configuradas
     */
    static EnterOptions cargar(String codigoAgente) {
        AgenteTipo tipoAgente = AgenteTipo.desdeCodigo(codigoAgente, AgenteTipo.porDefecto());
        return cargar(tipoAgente);
    }

    /**
     * Carga las opciones de runtime a partir del tipo de agente.
     *
     * @param tipoAgente tipo de agente (puede ser null, se usara el valor por defecto)
     * @return opciones de runtime configuradas
     */
    static EnterOptions cargar(AgenteTipo tipoAgente) {
        AgenteTipo agenteSeguro = Objects.requireNonNullElse(tipoAgente, AgenteTipo.porDefecto());
        return new EnterOptions(agenteSeguro, DELAY_SUBMIT_POST_PASTE_MS_POR_DEFECTO);
    }

    /**
     * Registro inmutable de opciones para la secuencia de entrada del agente.
     */
    static final class EnterOptions {
        private final AgenteTipo tipoAgente;
        private final int delaySubmitPostPasteMs;

        /**
         * Construye las opciones de entrada.
         *
         * @param tipoAgente tipo de agente (no puede ser null)
         * @param delaySubmitPostPasteMs delay en milisegundos despues del paste (debe ser &gt;= 0)
         * @throws IllegalArgumentException si delaySubmitPostPasteMs es negativo
         * @throws NullPointerException si tipoAgente es null
         */
        EnterOptions(AgenteTipo tipoAgente, int delaySubmitPostPasteMs) {
            this.tipoAgente = Objects.requireNonNull(tipoAgente, "tipoAgente no puede ser null");
            if (delaySubmitPostPasteMs < 0) {
                throw new IllegalArgumentException("delaySubmitPostPasteMs debe ser >= 0, recibido: " + delaySubmitPostPasteMs);
            }
            this.delaySubmitPostPasteMs = delaySubmitPostPasteMs;
        }

        AgenteTipo tipoAgente() {
            return tipoAgente;
        }

        int delaySubmitPostPasteMs() {
            return delaySubmitPostPasteMs;
        }
    }
}
