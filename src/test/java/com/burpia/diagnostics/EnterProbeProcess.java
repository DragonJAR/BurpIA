package com.burpia.diagnostics;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class EnterProbeProcess {

    private EnterProbeProcess() {
    }

    public static void main(String[] args) throws Exception {
        InputStream input = System.in;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(256);

        System.out.println("ENTER_PROBE_READY os=" + normalizarOs(System.getProperty("os.name")) + " pid=" + ProcessHandle.current().pid());

        int value;
        while ((value = input.read()) != -1) {
            int byteValue = value & 0xFF;
            String hex = String.format(Locale.ROOT, "%02X", byteValue);
            if (byteValue == 13 || byteValue == 10) {
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

    private static String normalizarOs(String osName) {
        if (osName == null || osName.isBlank()) {
            return "unknown";
        }
        return osName.trim().toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    private static String escapar(String text) {
        if (text == null || text.isEmpty()) {
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
