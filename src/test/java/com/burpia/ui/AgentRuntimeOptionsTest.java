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
        assertNull(opciones.estrategiaSubmitOverride());
        assertEquals(800, opciones.delaySubmitPostPasteMs());
    }

    @Test
    @DisplayName("Propiedades runtime de depuracion no alteran opciones del plugin")
    void propiedadesRuntimeNoAfectanOpciones() {
        System.setProperty("burpia.agent.enterDebug", "true");
        System.setProperty("burpia.agent.submit.strategy", "CRLF");
        System.setProperty("burpia.agent.submit.strategy.CLAUDE_CODE", "CTRL_J");
        System.setProperty("burpia.agent.submit.delayMs", "9999");
        System.setProperty("burpia.agent.submit.delayMs.CLAUDE_CODE", "10");

        AgentRuntimeOptions.EnterOptions claude = AgentRuntimeOptions.cargar(AgenteTipo.CLAUDE_CODE);
        AgentRuntimeOptions.EnterOptions factory = AgentRuntimeOptions.cargar(AgenteTipo.FACTORY_DROID);

        assertNull(claude.estrategiaSubmitOverride());
        assertNull(factory.estrategiaSubmitOverride());
        assertEquals(800, claude.delaySubmitPostPasteMs());
        assertEquals(800, factory.delaySubmitPostPasteMs());
    }

    @Test
    @DisplayName("Cargar con agente nulo usa fallback seguro")
    void cargarConAgenteNuloUsaFallback() {
        AgentRuntimeOptions.EnterOptions opciones = AgentRuntimeOptions.cargar((AgenteTipo) null);
        assertEquals(AgenteTipo.FACTORY_DROID, opciones.tipoAgente());
        assertNull(opciones.estrategiaSubmitOverride());
        assertEquals(800, opciones.delaySubmitPostPasteMs());
    }
}
