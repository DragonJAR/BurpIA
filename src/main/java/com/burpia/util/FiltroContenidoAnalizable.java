package com.burpia.util;

import java.util.Locale;
import java.util.Set;

/**
 * Utilidad para determinar si el contenido de una respuesta HTTP es analizable por AI.
 * Filtra tipos de contenido binarios, multimedia y respuestas sin cuerpo.
 */
public final class FiltroContenidoAnalizable {

    /** Tipos de contenido binarios/archivo que no deben analizarse */
    private static final Set<String> CONTENIDOS_NO_ANALIZABLES = Set.of(
        "application/octet-stream",
        "application/zip",
        "application/x-gzip",
        "application/gzip",
        "application/pdf",
        "application/vnd.ms-fontobject",
        "application/x-tar",
        "application/x-rar-compressed",
        "application/x-7z-compressed",
        "application/x-bzip2",
        "application/x-bzip",
        "application/java-archive",
        "application/x-shockwave-flash",
        "application/x-msdownload",
        "application/vnd.android.package-archive",
        "application/x-iso9660-image",
        "application/x-msi",
        "application/x-dosexec",
        "application/x-executable",
        "application/x-sharedlib",
        "application/epub+zip",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private FiltroContenidoAnalizable() {
    }

    /**
     * Determina si el contenido de una respuesta HTTP es analizable.
     *
     * @param contentType  el header Content-Type de la respuesta (puede ser null)
     * @param metodo       el método HTTP de la solicitud (GET, POST, HEAD, etc.)
     * @param codigoEstado el código de estado HTTP de la respuesta
     * @return true si el contenido puede ser analizado, false en caso contrario
     */
    public static boolean esAnalizable(String contentType, String metodo, int codigoEstado) {
        if (!esEstadoConContenido(codigoEstado)) {
            return false;
        }

        if (metodo != null && "HEAD".equalsIgnoreCase(metodo)) {
            return false;
        }

        if (Normalizador.esVacio(contentType)) {
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

    /**
     * Verifica si el código de estado HTTP implica que hay contenido en el cuerpo.
     * Los códigos 1xx (Informational), 204 (No Content) y 304 (Not Modified)
     * no tienen cuerpo de respuesta.
     *
     * @param codigoEstado el código de estado HTTP
     * @return true si el código implica contenido en el cuerpo, false en caso contrario
     */
    private static boolean esEstadoConContenido(int codigoEstado) {
        if (codigoEstado >= 100 && codigoEstado < 200) {
            return false;
        }
        if (codigoEstado == 204 || codigoEstado == 304) {
            return false;
        }
        return true;
    }
}
