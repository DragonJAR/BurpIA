package com.burpia.ui;

import com.burpia.config.AgenteTipo;
import com.burpia.util.Normalizador;
import com.burpia.util.OSUtils;

/**
 * Factory para construir secuencias de envío (submit sequences) para agentes CLI.
 * Las secuencias definen cómo se envía el carácter "Enter" a diferentes terminales.
 */
final class SubmitSequenceFactory {

    private static final String ESTRATEGIA_AUTO_CLAUDE = "AUTO_CLAUDE";
    private static final String ESTRATEGIA_AUTO_DROID = "AUTO_DROID";
    private static final String ESTRATEGIA_AUTO = "AUTO";

    private SubmitSequenceFactory() {
    }

    /**
     * Construye una secuencia de submit basada en el tipo de agente y la plataforma actual.
     *
     * @param tipoAgente tipo de agente (puede ser null, usa valor por defecto)
     * @return secuencia de submit configurada
     */
    static SubmitSequence construir(AgenteTipo tipoAgente) {
        return construir(tipoAgente, Plataforma.desdeSistemaActual());
    }

    /**
     * Construye una secuencia de submit basada en el tipo de agente y la plataforma especificada.
     *
     * @param tipoAgente tipo de agente (puede ser null, usa valor por defecto)
     * @param plataforma plataforma destino (puede ser null, usa LINUX como fallback)
     * @return secuencia de submit configurada
     */
    static SubmitSequence construir(AgenteTipo tipoAgente, Plataforma plataforma) {
        AgenteTipo agente = tipoAgente != null ? tipoAgente : AgenteTipo.porDefecto();
        Plataforma p = plataforma != null ? plataforma : Plataforma.LINUX;
        if (agente == AgenteTipo.CLAUDE_CODE) {
            String submit = (p == Plataforma.WINDOWS) ? "\r\n" : "\r";
            return new SubmitSequence(submit, 1, 0, ESTRATEGIA_AUTO_CLAUDE);
        }
        return new SubmitSequence("\r", 1, 0, ESTRATEGIA_AUTO_DROID)
            .conFallback("\n", 1, 100)
            .conFallback("\r\n", 1, 100);
    }

    /**
     * Representa una secuencia de caracteres para enviar como "Enter" a un terminal.
     * Soporta una cadena de fallbacks para probar diferentes secuencias si la primera falla.
     */
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
            this.estrategia = Normalizador.noEsVacio(estrategia)
                ? estrategia
                : ESTRATEGIA_AUTO;
        }

        /**
         * Agrega un fallback a esta secuencia. Si el payload actual no funciona,
         * se probará el fallback con el nuevo payload.
         *
         * @param payload      caracteres a enviar como fallback (no debe ser null)
         * @param repeticiones número de veces a enviar
         * @param delay        milisegundos de espera entre envíos
         * @return esta instancia para encadenamiento
         */
        SubmitSequence conFallback(String payload, int repeticiones, int delay) {
            if (payload == null) {
                return this;
            }
            if (this.fallback == null) {
                this.fallback = new SubmitSequence(payload, repeticiones, delay, this.estrategia);
            } else {
                this.fallback = this.fallback.conFallback(payload, repeticiones, delay);
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
