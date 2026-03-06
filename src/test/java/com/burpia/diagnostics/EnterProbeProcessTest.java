package com.burpia.diagnostics;

import com.burpia.util.OSUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests para el proceso de diagnóstico de entrada de teclado (Enter key probe).
 * <p>
 * Verifica que el proceso detecte correctamente caracteres de nueva línea
 * (CR/LF) y reporte las señales apropiadas.
 * </p>
 */
@DisplayName("Enter Probe Process Tests")
class EnterProbeProcessTest {

    /** Timeout en segundos para esperar a que el proceso termine. */
    private static final int TIMEOUT_SEGUNDOS = 10;

    @Test
    @DisplayName("Detecta CR como Enter y conserva longitud del buffer")
    void detectaCr() throws Exception {
        String salida = ejecutarProbe("hola\r".getBytes(StandardCharsets.UTF_8));
        assertTrue(salida.contains("ENTER_PROBE_SIGNAL byte=0D len=4"),
            "Debe detectar CR con longitud 4");
        assertTrue(salida.contains("ENTER_PROBE_BUFFER text=hola"),
            "Debe reportar buffer con texto 'hola'");
    }

    @Test
    @DisplayName("Detecta LF como Enter y conserva longitud del buffer")
    void detectaLf() throws Exception {
        String salida = ejecutarProbe("hola\n".getBytes(StandardCharsets.UTF_8));
        assertTrue(salida.contains("ENTER_PROBE_SIGNAL byte=0A len=4"),
            "Debe detectar LF con longitud 4");
        assertTrue(salida.contains("ENTER_PROBE_BUFFER text=hola"),
            "Debe reportar buffer con texto 'hola'");
    }

    @Test
    @DisplayName("CRLF genera dos eventos consecutivos")
    void detectaCrLf() throws Exception {
        String salida = ejecutarProbe("hola\r\n".getBytes(StandardCharsets.UTF_8));
        assertTrue(salida.contains("ENTER_PROBE_SIGNAL byte=0D len=4"),
            "Debe detectar CR con longitud 4");
        assertTrue(salida.contains("ENTER_PROBE_SIGNAL byte=0A len=0"),
            "Debe detectar LF con longitud 0 despues del reset");
    }

    /**
     * Ejecuta el proceso probe con el payload especificado y retorna la salida.
     *
     * @param payload los bytes a enviar al proceso via stdin
     * @return la salida estandar del proceso
     * @throws Exception si ocurre un error durante la ejecucion
     */
    private String ejecutarProbe(byte[] payload) throws Exception {
        Process process = null;
        try {
            process = iniciarProcesoProbe();
            enviarPayload(process, payload);
            return esperarYObtenerSalida(process);
        } finally {
            destruirProceso(process);
        }
    }

    /**
     * Inicializa y configura el proceso probe.
     *
     * @return el proceso iniciado
     * @throws IOException si no se puede iniciar el proceso
     */
    private Process iniciarProcesoProbe() throws IOException {
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

    /**
     * Envia el payload al proceso via stdin y cierra el stream.
     *
     * @param process el proceso objetivo
     * @param payload los bytes a enviar
     * @throws IOException si ocurre un error de escritura
     */
    private void enviarPayload(Process process, byte[] payload) throws IOException {
        try (OutputStream os = process.getOutputStream()) {
            os.write(payload);
            os.flush();
        }
    }

    /**
     * Espera a que el proceso termine y retorna su salida estandar.
     * <p>
     * Lee stdout y stderr en paralelo para evitar deadlocks cuando el buffer
     * del sistema se llena.
     * </p>
     *
     * @param process el proceso a esperar
     * @return la salida estandar del proceso
     * @throws Exception si el proceso no termina a tiempo o falla
     */
    private String esperarYObtenerSalida(Process process) throws Exception {
        // Leer streams en paralelo para evitar deadlock cuando el buffer del OS se llena
        final byte[][] stdoutBytes = {new byte[0]};
        final byte[][] stderrBytes = {new byte[0]};

        // Usar threads para leer ambos streams simultaneamente
        Thread stdoutThread = new Thread(() -> {
            try {
                stdoutBytes[0] = process.getInputStream().readAllBytes();
            } catch (IOException e) {
                // Mantener array vacio si hay error
            }
        });

        Thread stderrThread = new Thread(() -> {
            try {
                stderrBytes[0] = process.getErrorStream().readAllBytes();
            } catch (IOException e) {
                // Mantener array vacio si hay error
            }
        });

        stdoutThread.start();
        stderrThread.start();

        boolean termino = process.waitFor(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
        if (!termino) {
            process.destroyForcibly();
            stdoutThread.join(1000);
            stderrThread.join(1000);
            fail("EnterProbeProcess no finalizo en " + TIMEOUT_SEGUNDOS + "s");
        }

        // Esperar a que los threads terminen de leer
        stdoutThread.join(1000);
        stderrThread.join(1000);

        String salida = new String(stdoutBytes[0], StandardCharsets.UTF_8);
        String error = new String(stderrBytes[0], StandardCharsets.UTF_8);

        assertEquals(0, process.exitValue(),
            "EnterProbeProcess fallo: " + error + "\nSTDOUT:\n" + salida);
        assertTrue(salida.contains("ENTER_PROBE_READY"),
            "Debe incluir señal READY");
        assertTrue(salida.contains("ENTER_PROBE_EOF"),
            "Debe incluir señal EOF");

        return salida;
    }

    /**
     * Destruye el proceso de forma segura si existe.
     *
     * @param process el proceso a destruir, puede ser null
     */
    private void destruirProceso(Process process) {
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
    }
}
