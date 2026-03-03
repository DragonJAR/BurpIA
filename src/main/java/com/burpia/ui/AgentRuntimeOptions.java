package com.burpia.ui;

import com.burpia.config.AgenteTipo;

final class AgentRuntimeOptions {

    private static final int DELAY_SUBMIT_POST_PASTE_MS_POR_DEFECTO = 800;

    private AgentRuntimeOptions() {
    }

    static EnterOptions cargar(String codigoAgente) {
        AgenteTipo tipoAgente = AgenteTipo.desdeCodigo(codigoAgente, AgenteTipo.porDefecto());
        return cargar(tipoAgente);
    }

    static EnterOptions cargar(AgenteTipo tipoAgente) {
        AgenteTipo agenteSeguro = tipoAgente != null ? tipoAgente : AgenteTipo.porDefecto();
        return new EnterOptions(agenteSeguro, DELAY_SUBMIT_POST_PASTE_MS_POR_DEFECTO);
    }

    static final class EnterOptions {
        private final AgenteTipo tipoAgente;
        private final int delaySubmitPostPasteMs;

        EnterOptions(
            AgenteTipo tipoAgente,
            int delaySubmitPostPasteMs
        ) {
            this.tipoAgente = tipoAgente;
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
