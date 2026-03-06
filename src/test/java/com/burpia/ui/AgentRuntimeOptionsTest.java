package com.burpia.ui;

import com.burpia.config.AgenteTipo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AgentRuntimeOptions Tests")
class AgentRuntimeOptionsTest {

    @Test
    @DisplayName("Opciones por defecto para agente configurado")
    void opcionesPorDefecto() {
        AgentRuntimeOptions.EnterOptions opciones = AgentRuntimeOptions.cargar(AgenteTipo.CLAUDE_CODE);
        assertEquals(AgenteTipo.CLAUDE_CODE, opciones.tipoAgente(), "assertEquals failed at AgentRuntimeOptionsTest.java:16");
        assertEquals(800, opciones.delaySubmitPostPasteMs(), "assertEquals failed at AgentRuntimeOptionsTest.java:17");
    }

    @Test
    @DisplayName("Cargar con agente nulo usa fallback seguro")
    void cargarConAgenteNuloUsaFallback() {
        AgentRuntimeOptions.EnterOptions opciones = AgentRuntimeOptions.cargar((AgenteTipo) null);
        assertEquals(AgenteTipo.FACTORY_DROID, opciones.tipoAgente(), "assertEquals failed at AgentRuntimeOptionsTest.java:24");
        assertEquals(800, opciones.delaySubmitPostPasteMs(), "assertEquals failed at AgentRuntimeOptionsTest.java:25");
    }

    @Test
    @DisplayName("Cargar con codigo de agente valido")
    void cargarConCodigoAgenteValido() {
        AgentRuntimeOptions.EnterOptions opciones = AgentRuntimeOptions.cargar("GEMINI_CLI");
        assertEquals(AgenteTipo.GEMINI_CLI, opciones.tipoAgente(), "assertEquals failed at AgentRuntimeOptionsTest.java:32");
        assertEquals(800, opciones.delaySubmitPostPasteMs(), "assertEquals failed at AgentRuntimeOptionsTest.java:33");
    }

    @Test
    @DisplayName("Cargar con codigo de agente con espacios lo normaliza")
    void cargarConCodigoAgenteConEspaciosLoNormaliza() {
        AgentRuntimeOptions.EnterOptions opciones = AgentRuntimeOptions.cargar("  CLAUDE_CODE  ");
        assertEquals(AgenteTipo.CLAUDE_CODE, opciones.tipoAgente(), "assertEquals failed at AgentRuntimeOptionsTest.java:40");
    }

    @Test
    @DisplayName("Cargar con codigo de agente nulo usa fallback")
    void cargarConCodigoAgenteNuloUsaFallback() {
        AgentRuntimeOptions.EnterOptions opciones = AgentRuntimeOptions.cargar((String) null);
        assertEquals(AgenteTipo.FACTORY_DROID, opciones.tipoAgente(), "assertEquals failed at AgentRuntimeOptionsTest.java:47");
    }

    @Test
    @DisplayName("Cargar con codigo de agente vacio usa fallback")
    void cargarConCodigoAgenteVacioUsaFallback() {
        AgentRuntimeOptions.EnterOptions opciones = AgentRuntimeOptions.cargar("");
        assertEquals(AgenteTipo.FACTORY_DROID, opciones.tipoAgente(), "assertEquals failed at AgentRuntimeOptionsTest.java:54");
    }

    @Test
    @DisplayName("Cargar con codigo de agente invalido usa fallback")
    void cargarConCodigoAgenteInvalidoUsaFallback() {
        AgentRuntimeOptions.EnterOptions opciones = AgentRuntimeOptions.cargar("AGENTE_INEXISTENTE");
        assertEquals(AgenteTipo.FACTORY_DROID, opciones.tipoAgente(), "assertEquals failed at AgentRuntimeOptionsTest.java:61");
    }

    @Test
    @DisplayName("EnterOptions rechaza tipoAgente nulo")
    void enterOptionsRechazaTipoAgenteNulo() {
        NullPointerException ex = assertThrows(
            NullPointerException.class,
            () -> new AgentRuntimeOptions.EnterOptions(null, 800)
        );
        assertTrue(ex.getMessage().contains("tipoAgente"), "assertTrue failed at AgentRuntimeOptionsTest.java:71");
    }

    @Test
    @DisplayName("EnterOptions rechaza delay negativo")
    void enterOptionsRechazaDelayNegativo() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> new AgentRuntimeOptions.EnterOptions(AgenteTipo.CLAUDE_CODE, -1)
        );
        assertTrue(ex.getMessage().contains("delaySubmitPostPasteMs"), "assertTrue failed at AgentRuntimeOptionsTest.java:81");
    }

    @Test
    @DisplayName("EnterOptions acepta delay cero")
    void enterOptionsAceptaDelayCero() {
        AgentRuntimeOptions.EnterOptions opciones = new AgentRuntimeOptions.EnterOptions(AgenteTipo.CLAUDE_CODE, 0);
        assertEquals(0, opciones.delaySubmitPostPasteMs(), "assertEquals failed at AgentRuntimeOptionsTest.java:88");
    }
}
