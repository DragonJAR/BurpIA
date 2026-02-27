package com.burpia.ui;

import com.burpia.config.AgenteTipo;
import com.burpia.util.Normalizador;

final class AgentRuntimeOptions {

    private static final int DELAY_SUBMIT_POST_PASTE_MS_POR_DEFECTO = 800;
    private static final int DELAY_SUBMIT_POST_PASTE_MS_MAXIMO = 60000;

    private static final String PROP_ENTER_DEBUG_GLOBAL = "burpia.agent.enterDebug";
    private static final String PROP_ENTER_DEBUG_PREFIJO = "burpia.agent.enterDebug.";
    private static final String PROP_ENTER_PROBE_GLOBAL = "burpia.agent.enterProbe";
    private static final String PROP_ENTER_PROBE_PREFIJO = "burpia.agent.enterProbe.";
    private static final String PROP_SUBMIT_STRATEGY_GLOBAL = "burpia.agent.submit.strategy";
    private static final String PROP_SUBMIT_STRATEGY_PREFIJO = "burpia.agent.submit.strategy.";
    private static final String PROP_SUBMIT_DELAY_GLOBAL = "burpia.agent.submit.delayMs";
    private static final String PROP_SUBMIT_DELAY_PREFIJO = "burpia.agent.submit.delayMs.";
    private static final String PROP_PROBE_MODE_GLOBAL = "burpia.agent.probeMode";
    private static final String PROP_PROBE_MODE_PREFIJO = "burpia.agent.probeMode.";

    private AgentRuntimeOptions() {
    }

    static EnterOptions cargar(String codigoAgente) {
        AgenteTipo tipoAgente = AgenteTipo.desdeCodigo(codigoAgente, AgenteTipo.FACTORY_DROID);
        return cargar(tipoAgente);
    }

    static EnterOptions cargar(AgenteTipo tipoAgente) {
        AgenteTipo agenteSeguro = tipoAgente != null ? tipoAgente : AgenteTipo.FACTORY_DROID;
        String debugRaw = resolverValorConOverridePorAgente(
            PROP_ENTER_DEBUG_GLOBAL,
            PROP_ENTER_DEBUG_PREFIJO,
            agenteSeguro
        );
        String probeRaw = resolverValorConOverridePorAgente(
            PROP_ENTER_PROBE_GLOBAL,
            PROP_ENTER_PROBE_PREFIJO,
            agenteSeguro
        );
        String estrategiaOverride = resolverValorConOverridePorAgente(
            PROP_SUBMIT_STRATEGY_GLOBAL,
            PROP_SUBMIT_STRATEGY_PREFIJO,
            agenteSeguro
        );
        String delayRaw = resolverValorConOverridePorAgente(
            PROP_SUBMIT_DELAY_GLOBAL,
            PROP_SUBMIT_DELAY_PREFIJO,
            agenteSeguro
        );
        String probeModeRaw = resolverValorConOverridePorAgente(
            PROP_PROBE_MODE_GLOBAL,
            PROP_PROBE_MODE_PREFIJO,
            agenteSeguro
        );
        int delaySubmitPostPasteMs = parseEnteroEnRango(
            delayRaw,
            DELAY_SUBMIT_POST_PASTE_MS_POR_DEFECTO,
            0,
            DELAY_SUBMIT_POST_PASTE_MS_MAXIMO
        );

        return new EnterOptions(
            agenteSeguro,
            parseBoolean(debugRaw, false),
            parseBoolean(probeRaw, false),
            Normalizador.esVacio(estrategiaOverride) ? null : estrategiaOverride.trim(),
            delaySubmitPostPasteMs,
            ProbeMode.desdeValor(probeModeRaw)
        );
    }

    private static String resolverValorConOverridePorAgente(
        String propiedadGlobal,
        String propiedadPrefijo,
        AgenteTipo agente
    ) {
        if (agente != null && !Normalizador.esVacio(propiedadPrefijo)) {
            String porAgente = System.getProperty(propiedadPrefijo + agente.name());
            if (!Normalizador.esVacio(porAgente)) {
                return porAgente;
            }
        }
        return System.getProperty(propiedadGlobal);
    }

    private static boolean parseBoolean(String valor, boolean porDefecto) {
        if (Normalizador.esVacio(valor)) {
            return porDefecto;
        }
        return Boolean.parseBoolean(valor.trim());
    }

    private static int parseEnteroEnRango(String valor, int porDefecto, int minimo, int maximo) {
        if (Normalizador.esVacio(valor)) {
            return porDefecto;
        }
        try {
            int numero = Integer.parseInt(valor.trim());
            if (numero < minimo) {
                return minimo;
            }
            if (numero > maximo) {
                return maximo;
            }
            return numero;
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    static final class EnterOptions {
        private final AgenteTipo tipoAgente;
        private final boolean enterDebugActivo;
        private final boolean probeSubmitActivo;
        private final String estrategiaSubmitOverride;
        private final int delaySubmitPostPasteMs;
        private final ProbeMode probeMode;

        EnterOptions(
            AgenteTipo tipoAgente,
            boolean enterDebugActivo,
            boolean probeSubmitActivo,
            String estrategiaSubmitOverride,
            int delaySubmitPostPasteMs,
            ProbeMode probeMode
        ) {
            this.tipoAgente = tipoAgente;
            this.enterDebugActivo = enterDebugActivo;
            this.probeSubmitActivo = probeSubmitActivo;
            this.estrategiaSubmitOverride = estrategiaSubmitOverride;
            this.delaySubmitPostPasteMs = delaySubmitPostPasteMs;
            this.probeMode = probeMode != null ? probeMode : ProbeMode.OFF;
        }

        AgenteTipo tipoAgente() {
            return tipoAgente;
        }

        boolean enterDebugActivo() {
            return enterDebugActivo;
        }

        boolean probeSubmitActivo() {
            return probeSubmitActivo;
        }

        String estrategiaSubmitOverride() {
            return estrategiaSubmitOverride;
        }

        int delaySubmitPostPasteMs() {
            return delaySubmitPostPasteMs;
        }

        ProbeMode probeMode() {
            return probeMode;
        }
    }

    enum ProbeMode {
        OFF,
        POSIX,
        WINDOWS;

        static ProbeMode desdeValor(String valor) {
            if (Normalizador.esVacio(valor)) {
                return OFF;
            }
            try {
                return valueOf(valor.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return OFF;
            }
        }
    }
}
