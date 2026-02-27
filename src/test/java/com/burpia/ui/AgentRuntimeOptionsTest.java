package com.burpia.ui;

import com.burpia.config.AgenteTipo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentRuntimeOptionsTest {

    @Test
    @DisplayName("Opciones por defecto para agente configurado")
    void opcionesPorDefecto() {
        AgentRuntimeOptions.EnterOptions opciones = AgentRuntimeOptions.cargar(AgenteTipo.CLAUDE_CODE);
        assertEquals(AgenteTipo.CLAUDE_CODE, opciones.tipoAgente());
        assertEquals(800, opciones.delaySubmitPostPasteMs());
    }

    @Test
    @DisplayName("Cargar con agente nulo usa fallback seguro")
    void cargarConAgenteNuloUsaFallback() {
        AgentRuntimeOptions.EnterOptions opciones = AgentRuntimeOptions.cargar((AgenteTipo) null);
        assertEquals(AgenteTipo.FACTORY_DROID, opciones.tipoAgente());
        assertEquals(800, opciones.delaySubmitPostPasteMs());
    }
}
