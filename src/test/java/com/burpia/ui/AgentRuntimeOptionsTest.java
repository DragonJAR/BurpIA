package com.burpia.ui;

import com.burpia.config.AgenteTipo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentRuntimeOptionsTest {

    private static final String[] PROPIEDADES_BASE = {
        "burpia.agent.enterDebug",
        "burpia.agent.submit.strategy",
        "burpia.agent.submit.delayMs"
    };

    @AfterEach
    void limpiarPropiedades() {
        for (String base : PROPIEDADES_BASE) {
            System.clearProperty(base);
            for (AgenteTipo agente : AgenteTipo.values()) {
                System.clearProperty(base + "." + agente.name());
            }
        }
    }

    @Test
    @DisplayName("Opciones por defecto si no hay propiedades runtime")
    void opcionesPorDefecto() {
        AgentRuntimeOptions.EnterOptions opciones = AgentRuntimeOptions.cargar(AgenteTipo.CLAUDE_CODE);
        assertEquals(AgenteTipo.CLAUDE_CODE, opciones.tipoAgente());
        assertFalse(opciones.enterDebugActivo());
        assertNull(opciones.estrategiaSubmitOverride());
        assertEquals(800, opciones.delaySubmitPostPasteMs());
    }

    @Test
    @DisplayName("Override por agente tiene prioridad sobre global")
    void overridePorAgenteTienePrioridad() {
        System.setProperty("burpia.agent.submit.strategy", "CRLF");
        System.setProperty("burpia.agent.submit.strategy.CLAUDE_CODE", "CTRL_J");

        AgentRuntimeOptions.EnterOptions claude = AgentRuntimeOptions.cargar(AgenteTipo.CLAUDE_CODE);
        AgentRuntimeOptions.EnterOptions factory = AgentRuntimeOptions.cargar(AgenteTipo.FACTORY_DROID);

        assertEquals("CTRL_J", claude.estrategiaSubmitOverride());
        assertEquals("CRLF", factory.estrategiaSubmitOverride());
    }

    @Test
    @DisplayName("Booleanos globales y por agente se resuelven correctamente")
    void booleanosGlobalesYPorAgente() {
        System.setProperty("burpia.agent.enterDebug", "false");
        System.setProperty("burpia.agent.enterDebug.CLAUDE_CODE", "true");

        AgentRuntimeOptions.EnterOptions claude = AgentRuntimeOptions.cargar(AgenteTipo.CLAUDE_CODE);
        AgentRuntimeOptions.EnterOptions factory = AgentRuntimeOptions.cargar(AgenteTipo.FACTORY_DROID);

        assertTrue(claude.enterDebugActivo());
        assertFalse(factory.enterDebugActivo());
    }

    @Test
    @DisplayName("Delay respeta rango y fallback robusto")
    void delayRespetaRango() {
        System.setProperty("burpia.agent.submit.delayMs", "abc");
        AgentRuntimeOptions.EnterOptions invalido = AgentRuntimeOptions.cargar(AgenteTipo.FACTORY_DROID);
        assertEquals(800, invalido.delaySubmitPostPasteMs());

        System.setProperty("burpia.agent.submit.delayMs", "-10");
        AgentRuntimeOptions.EnterOptions minimo = AgentRuntimeOptions.cargar(AgenteTipo.FACTORY_DROID);
        assertEquals(0, minimo.delaySubmitPostPasteMs());

        System.setProperty("burpia.agent.submit.delayMs.CLAUDE_CODE", "999999");
        AgentRuntimeOptions.EnterOptions maximo = AgentRuntimeOptions.cargar(AgenteTipo.CLAUDE_CODE);
        assertEquals(60000, maximo.delaySubmitPostPasteMs());
    }
}
