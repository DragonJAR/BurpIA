package com.burpia.ui;

import com.burpia.config.AgenteTipo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SubmitSequenceFactoryTest {

    @Test
    @DisplayName("AUTO para Claude en Linux/Mac usa CR")
    void autoClaudeCodeUnixUsaCr() {
        SubmitSequenceFactory.SubmitSequence secuencia = SubmitSequenceFactory.construir(
            AgenteTipo.CLAUDE_CODE,
            null,
            SubmitSequenceFactory.Plataforma.LINUX
        );
        assertEquals("\r", secuencia.payload());
        assertEquals(1, secuencia.repeticiones());
    }

    @Test
    @DisplayName("AUTO para Claude en Windows usa CRLF")
    void autoClaudeCodeWindowsUsaCrlf() {
        SubmitSequenceFactory.SubmitSequence secuencia = SubmitSequenceFactory.construir(
            AgenteTipo.CLAUDE_CODE,
            null,
            SubmitSequenceFactory.Plataforma.WINDOWS
        );
        assertEquals("\r\n", secuencia.payload());
        assertEquals(1, secuencia.repeticiones());
    }

    @Test
    @DisplayName("AUTO para Droid usa SMART_FALLBACK (Escalera)")
    void autoFactoryDroidUsaSmartFallback() {
        SubmitSequenceFactory.SubmitSequence secuencia = SubmitSequenceFactory.construir(
            AgenteTipo.FACTORY_DROID,
            null,
            SubmitSequenceFactory.Plataforma.LINUX
        );
        assertEquals("\r", secuencia.payload());
        assertEquals(1, secuencia.repeticiones());

        assertNotNull(secuencia.getFallback());
        assertEquals("\n", secuencia.getFallback().payload());
    }

    @Test
    @DisplayName("Override CRLF fuerza secuencia CRLF")
    void overrideCrlfFunciona() {
        SubmitSequenceFactory.SubmitSequence secuencia = SubmitSequenceFactory.construir(
            AgenteTipo.CLAUDE_CODE,
            "CRLF",
            SubmitSequenceFactory.Plataforma.LINUX
        );
        assertEquals("\r\n", secuencia.payload());
        assertEquals(1, secuencia.repeticiones());
    }

    @Test
    @DisplayName("Override inválido cae en AUTO")
    void overrideInvalidoCaeEnAuto() {
        SubmitSequenceFactory.SubmitSequence secuencia = SubmitSequenceFactory.construir(
            AgenteTipo.CLAUDE_CODE,
            "NO_EXISTE",
            SubmitSequenceFactory.Plataforma.WINDOWS
        );
        assertEquals("\r\n", secuencia.payload());
        assertEquals(1, secuencia.repeticiones());
    }
    
    @Test
    @DisplayName("TRIPLE_ENTER_OS usa el separador de plataforma")
    void tripleEnterOsUsaSeparadorCorrecto() {
        SubmitSequenceFactory.SubmitSequence win = SubmitSequenceFactory.construir(
            AgenteTipo.FACTORY_DROID,
            "TRIPLE_ENTER_OS",
            SubmitSequenceFactory.Plataforma.WINDOWS
        );
        assertEquals("\r\n", win.payload());
        assertEquals(3, win.repeticiones());

        SubmitSequenceFactory.SubmitSequence linux = SubmitSequenceFactory.construir(
            AgenteTipo.FACTORY_DROID,
            "TRIPLE_ENTER_OS",
            SubmitSequenceFactory.Plataforma.LINUX
        );
        assertEquals("\r", linux.payload());
        assertEquals(3, linux.repeticiones());
    }
}
