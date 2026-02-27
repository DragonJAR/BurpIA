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
            SubmitSequenceFactory.Plataforma.LINUX
        );
        assertEquals("\r", secuencia.payload());
        assertEquals(1, secuencia.repeticiones());

        assertNotNull(secuencia.getFallback());
        assertEquals("\n", secuencia.getFallback().payload());
        assertNotNull(secuencia.getFallback().getFallback());
        assertEquals("\r\n", secuencia.getFallback().getFallback().payload());
    }
}
