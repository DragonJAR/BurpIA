package com.burpia.util;

import java.util.Locale;
import java.util.Set;

public final class FiltroContenidoAnalizable {
    private static final Set<String> CONTENIDOS_NO_ANALIZABLES = Set.of(
        "application/octet-stream",
        "application/zip",
        "application/x-gzip",
        "application/gzip",
        "application/pdf",
        "application/vnd.ms-fontobject"
    );

    private FiltroContenidoAnalizable() {
    }

    public static boolean esAnalizable(String contentType, String metodo, int codigoEstado) {
        if (metodo != null && "HEAD".equalsIgnoreCase(metodo)) {
            return false;
        }
        if (contentType == null || contentType.trim().isEmpty()) {
            return true;
        }

        String normalizado = contentType.toLowerCase(Locale.ROOT).trim();
        int separadorParametros = normalizado.indexOf(';');
        if (separadorParametros >= 0) {
            normalizado = normalizado.substring(0, separadorParametros).trim();
        }
        if (normalizado.isEmpty()) {
            return true;
        }

        if (normalizado.startsWith("image/")
            || normalizado.startsWith("video/")
            || normalizado.startsWith("audio/")
            || normalizado.startsWith("font/")) {
            return false;
        }

        return !CONTENIDOS_NO_ANALIZABLES.contains(normalizado);
    }
}
