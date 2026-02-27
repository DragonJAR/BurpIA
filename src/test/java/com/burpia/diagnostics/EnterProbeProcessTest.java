package com.burpia.diagnostics;

import com.burpia.util.OSUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Enter Probe Process Tests")
class EnterProbeProcessTest {

    @Test
    @DisplayName("Detecta CR como Enter y conserva longitud del buffer")
    void detectaCr() throws Exception {
        String salida = ejecutarProbe("hola\r".getBytes(StandardCharsets.UTF_8));
        assertTrue(salida.contains("ENTER_PROBE_SIGNAL byte=0D len=4"));
        assertTrue(salida.contains("ENTER_PROBE_BUFFER text=hola"));
    }

    @Test
    @DisplayName("Detecta LF como Enter y conserva longitud del buffer")
    void detectaLf() throws Exception {
        String salida = ejecutarProbe("hola\n".getBytes(StandardCharsets.UTF_8));
        assertTrue(salida.contains("ENTER_PROBE_SIGNAL byte=0A len=4"));
        assertTrue(salida.contains("ENTER_PROBE_BUFFER text=hola"));
    }

    @Test
    @DisplayName("CRLF genera dos eventos consecutivos")
    void detectaCrLf() throws Exception {
        String salida = ejecutarProbe("hola\r\n".getBytes(StandardCharsets.UTF_8));
        assertTrue(salida.contains("ENTER_PROBE_SIGNAL byte=0D len=4"));
        assertTrue(salida.contains("ENTER_PROBE_SIGNAL byte=0A len=0"));
    }

    private String ejecutarProbe(byte[] payload) throws Exception {
        Process process = iniciarProcesoProbe();

        try (OutputStream os = process.getOutputStream()) {
            os.write(payload);
            os.flush();
        }

        boolean termino = process.waitFor(10, TimeUnit.SECONDS);
        if (!termino) {
            process.destroyForcibly();
            throw new IllegalStateException("EnterProbeProcess no finalizo en 10s");
        }

        String salida = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(), "EnterProbeProcess fallo: " + error + "\nSTDOUT:\n" + salida);
        assertTrue(salida.contains("ENTER_PROBE_READY"));
        assertTrue(salida.contains("ENTER_PROBE_EOF"));
        return salida;
    }

    private Process iniciarProcesoProbe() throws Exception {
        String javaBin = Path.of(
            System.getProperty("java.home"),
            "bin",
            OSUtils.esWindows() ? "java.exe" : "java"
        ).toString();

        ProcessBuilder pb = new ProcessBuilder(
            javaBin,
            "-cp",
            System.getProperty("java.class.path"),
            EnterProbeProcess.class.getName()
        );
        pb.redirectErrorStream(false);
        return pb.start();
    }
}
