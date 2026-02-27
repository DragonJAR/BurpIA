package com.burpia.ui;

import com.burpia.config.AgenteTipo;
import com.burpia.util.OSUtils;

final class SubmitSequenceFactory {

    private SubmitSequenceFactory() {
    }

    static SubmitSequence construir(AgenteTipo tipoAgente) {
        return construir(tipoAgente, Plataforma.desdeSistemaActual());
    }

    static SubmitSequence construir(AgenteTipo tipoAgente, Plataforma plataforma) {
        AgenteTipo agente = tipoAgente != null ? tipoAgente : AgenteTipo.FACTORY_DROID;
        Plataforma p = plataforma != null ? plataforma : Plataforma.LINUX;
        if (agente == AgenteTipo.CLAUDE_CODE) {
            String submit = (p == Plataforma.WINDOWS) ? "\r\n" : "\r";
            return new SubmitSequence(submit, 1, 0, "AUTO_CLAUDE");
        }
        return new SubmitSequence("\r", 1, 0, "AUTO_DROID")
            .conFallback("\n", 1, 100)
            .conFallback("\r\n", 1, 100);
    }

    static final class SubmitSequence {
        private final String payload;
        private final int repeticiones;
        private final int delayEntreEnviosMs;
        private final String estrategia;
        private SubmitSequence fallback;

        SubmitSequence(String payload, int repeticiones, int delayEntreEnviosMs, String estrategia) {
            this.payload = payload;
            this.repeticiones = Math.max(1, repeticiones);
            this.delayEntreEnviosMs = Math.max(0, delayEntreEnviosMs);
            this.estrategia = estrategia != null && !estrategia.trim().isEmpty()
                ? estrategia
                : "AUTO";
        }

        SubmitSequence conFallback(String payload, int repeticiones, int delay) {
            if (this.fallback == null) {
                this.fallback = new SubmitSequence(payload, repeticiones, delay, this.estrategia);
            } else {
                this.fallback.conFallback(payload, repeticiones, delay);
            }
            return this;
        }

        SubmitSequence getFallback() {
            return fallback;
        }

        String payload() {
            return payload;
        }

        int repeticiones() {
            return repeticiones;
        }

        int delayEntreEnviosMs() {
            return delayEntreEnviosMs;
        }

        String descripcion() {
            return estrategia + " x" + repeticiones;
        }
    }

    enum Plataforma {
        WINDOWS, MAC, LINUX, OTHER;

        static Plataforma desdeSistemaActual() {
            if (OSUtils.esWindows()) return WINDOWS;
            if (OSUtils.esMac()) return MAC;
            if (OSUtils.esLinux()) return LINUX;
            return OTHER;
        }
    }
}
