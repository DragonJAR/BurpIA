package com.burpia.diagnostics;

import com.burpia.util.Normalizador;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Proceso de diagnóstico para probar la entrada de teclado (Enter key probe).
 * <p>
 * Esta utilidad lee de stdin byte por byte y reporta señales cuando detecta
 * caracteres de nueva línea (CR/LF), útil para diagnosticar problemas de
 * entrada en tests de UI.
 * </p>
 *
 * @see Normalizador
 */
public final class EnterProbeProcess {

    /** Tamaño inicial del buffer para acumular caracteres. */
    private static final int TAMANO_BUFFER_INICIAL = 256;

    /** Valor ASCII del carácter CR (Carriage Return). */
    private static final int ASCII_CR = 13;

    /** Valor ASCII del carácter LF (Line Feed). */
    private static final int ASCII_LF = 10;

    private EnterProbeProcess() {
        // Clase de utilidad, no instanciable
    }

    /**
     * Punto de entrada principal que lee stdin y reporta señales de entrada.
     *
     * @param args argumentos de línea de comandos (no utilizados)
     * @throws Exception si ocurre un error de I/O
     */
    public static void main(String[] args) throws Exception {
        InputStream input = System.in;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(TAMANO_BUFFER_INICIAL);

        System.out.println("ENTER_PROBE_READY os=" + normalizarOs(System.getProperty("os.name")) + " pid=" + ProcessHandle.current().pid());

        int value;
        while ((value = input.read()) != -1) {
            int byteValue = value & 0xFF;
            String hex = String.format(Locale.ROOT, "%02X", byteValue);
            if (byteValue == ASCII_CR || byteValue == ASCII_LF) {
                String text = buffer.toString(StandardCharsets.UTF_8);
                System.out.println("ENTER_PROBE_SIGNAL byte=" + hex + " len=" + text.length());
                System.out.println("ENTER_PROBE_BUFFER text=" + escapar(text));
                buffer.reset();
            } else {
                buffer.write(byteValue);
            }
        }

        System.out.println("ENTER_PROBE_EOF");
    }

    /**
     * Normaliza el nombre del sistema operativo para uso en logs.
     *
     * @param osName el nombre del sistema operativo
     * @return el nombre normalizado en minúsculas con guiones, o "unknown" si es vacío
     */
    private static String normalizarOs(String osName) {
        if (Normalizador.esVacio(osName)) {
            return "unknown";
        }
        return osName.trim().toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    /**
     * Escapa caracteres de control en un texto para visualización segura.
     * <p>
     * Los caracteres ASCII imprimibles (32-126 excepto DEL) se mantienen,
     * los demás se representan como {@code \xNN} en hexadecimal.
     * </p>
     *
     * @param text el texto a escapar
     * @return el texto escapado, o cadena vacía si el input es vacío
     */
    private static String escapar(String text) {
        if (Normalizador.esVacio(text)) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch >= 32 && ch != 127) {
                escaped.append(ch);
            } else {
                escaped.append(String.format(Locale.ROOT, "\\x%02X", (int) ch));
            }
        }
        return escaped.toString();
    }
}
