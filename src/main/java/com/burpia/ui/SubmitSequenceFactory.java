package com.burpia.ui;

import com.burpia.config.AgenteTipo;
import com.burpia.util.OSUtils;

final class SubmitSequenceFactory {

    private SubmitSequenceFactory() {
    }

    static SubmitSequence construir(AgenteTipo tipoAgente, String estrategiaOverride) {
        return construir(tipoAgente, estrategiaOverride, Plataforma.desdeSistemaActual());
    }

    static SubmitSequence construir(AgenteTipo tipoAgente, String estrategiaOverride, Plataforma plataforma) {
        EstrategiaSubmit estrategia = EstrategiaSubmit.desdeValor(estrategiaOverride, EstrategiaSubmit.AUTO);
        AgenteTipo agente = tipoAgente != null ? tipoAgente : AgenteTipo.FACTORY_DROID;
        Plataforma p = plataforma != null ? plataforma : Plataforma.LINUX;

        if (estrategia == EstrategiaSubmit.AUTO) {
            return construirAuto(agente, p);
        }
        return construirFija(estrategia, p);
    }

    private static SubmitSequence construirAuto(AgenteTipo tipoAgente, Plataforma plataforma) {
        if (tipoAgente == AgenteTipo.CLAUDE_CODE) {
            return construirFija(plataforma == Plataforma.WINDOWS ? EstrategiaSubmit.CRLF : EstrategiaSubmit.CR, plataforma);
        }
        return construirFija(EstrategiaSubmit.SMART_FALLBACK, plataforma);
    }

    private static SubmitSequence construirFija(EstrategiaSubmit estrategia, Plataforma plataforma) {
        String sep = (plataforma == Plataforma.WINDOWS) ? "\r\n" : "\r";

        switch (estrategia) {
            case CTRL_J:
            case LF:
                return new SubmitSequence("\n", 1, 0, estrategia);
            
            case CTRL_M:
            case CR:
                return new SubmitSequence("\r", 1, 0, estrategia);
            
            case CRLF:
                return new SubmitSequence("\r\n", 1, 0, estrategia);
            
            case TRIPLE_CRLF:
                return new SubmitSequence("\r\n", 3, 80, estrategia);
            
            case TRIPLE_ENTER_OS:
                return new SubmitSequence(sep, 3, 100, estrategia);
            
            case SMART_FALLBACK:
                return new SubmitSequence("\r", 1, 0, estrategia)
                    .conFallback("\n", 1, 100)
                    .conFallback("\r\n", 1, 100);
            
            case AUTO:
            default:
                return new SubmitSequence(sep, 1, 0, EstrategiaSubmit.AUTO);
        }
    }

    static final class SubmitSequence {
        private final String payload;
        private final int repeticiones;
        private final int delayEntreEnviosMs;
        private final EstrategiaSubmit estrategia;
        private SubmitSequence fallback;

        SubmitSequence(String payload, int repeticiones, int delayEntreEnviosMs, EstrategiaSubmit estrategia) {
            this.payload = payload;
            this.repeticiones = Math.max(1, repeticiones);
            this.delayEntreEnviosMs = Math.max(0, delayEntreEnviosMs);
            this.estrategia = estrategia != null ? estrategia : EstrategiaSubmit.AUTO;
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
            return estrategia.name() + " x" + repeticiones;
        }
    }

    enum EstrategiaSubmit {
        AUTO, CTRL_J, CTRL_M, LF, CR, CRLF, TRIPLE_ENTER_OS, TRIPLE_CRLF, SMART_FALLBACK;

        static EstrategiaSubmit desdeValor(String valor, EstrategiaSubmit porDefecto) {
            if (valor == null || valor.trim().isEmpty()) return porDefecto;
            try {
                return valueOf(valor.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return porDefecto;
            }
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
