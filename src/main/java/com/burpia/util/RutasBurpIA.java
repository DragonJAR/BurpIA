package com.burpia.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class RutasBurpIA {
    private static final String DIRECTORIO_BASE = ".burpia";
    private static final String ARCHIVO_CONFIG = "config.json";
    private static final String DIRECTORIO_EVIDENCIA = "evidence";

    private RutasBurpIA() {
    }

    public static Path obtenerDirectorioBase() {
        return Paths.get(obtenerHomeSeguro(), DIRECTORIO_BASE);
    }

    public static Path obtenerRutaConfig() {
        return obtenerDirectorioBase().resolve(ARCHIVO_CONFIG);
    }

    public static Path obtenerDirectorioEvidencia() {
        return obtenerDirectorioBase().resolve(DIRECTORIO_EVIDENCIA);
    }

    private static String obtenerHomeSeguro() {
        String userHome = System.getProperty("user.home");
        if (userHome == null || userHome.trim().isEmpty()) {
            userHome = System.getProperty("user.dir", ".");
        }
        return userHome;
    }
}
