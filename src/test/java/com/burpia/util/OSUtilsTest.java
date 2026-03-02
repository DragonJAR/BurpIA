package com.burpia.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.*;

public class OSUtilsTest {

    @Test
    public void testPrepararComandoVacio() {
        assertNull(OSUtils.prepararComando(null));
        assertTrue(OSUtils.prepararComando("").contains(OSUtils.obtenerEolTerminal()));
    }

    @Test
    public void testPrepararComandoYaContieneSaltoDeLinea() {
        String comandoConN = "echo hola\n";
        assertEquals(comandoConN, OSUtils.prepararComando(comandoConN));
        
        String comandoConR = "echo hola\r";
        assertEquals(comandoConR, OSUtils.prepararComando(comandoConR));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void testObtenerEolTerminalWindows() {
        assertEquals("\r\n", OSUtils.obtenerEolTerminal());
        assertTrue(OSUtils.esWindows());
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    public void testObtenerEolTerminalUnix() {
        String eol = OSUtils.obtenerEolTerminal();
        assertEquals("\n", eol);
        assertFalse(OSUtils.esWindows());
    }

    @Test
    public void testExtraerEjecutableComandoClaudeConFlags() {
        assertEquals("claude",
            OSUtils.extraerEjecutableComando("claude --dangerously-skip-permissions"));
    }

    @Test
    public void testExtraerEjecutableComandoConRutaEntreComillasYFlags() {
        assertEquals("/opt/claude/bin/claude",
            OSUtils.extraerEjecutableComando("\"/opt/claude/bin/claude\" --dangerously-skip-permissions"));
    }

    @Test
    public void testResolverEjecutableComandoExpandeRuta() {
        String comando = "~/.local/bin/claude --dangerously-skip-permissions";
        String esperado = System.getProperty("user.home") + "/.local/bin/claude";
        assertEquals(esperado, OSUtils.resolverEjecutableComando(comando));
    }
}
