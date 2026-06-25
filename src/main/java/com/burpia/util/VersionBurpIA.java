package com.burpia.util;

/**
 * Utilidad para gestión centralizada de versiones de BurpIA.
 * <p>
 * Proporciona métodos para obtener la versión actual, normalizar cadenas de versión
 * y comparar versiones locales con remotas para detectar actualizaciones.
 * </p>
 *
 * @see <a href="https://github.com/DragonJAR/BurpIA/blob/main/AGENTS.md">Guía de codificación AGENTS.md</a>
 */
public final class VersionBurpIA {

    /** Versión actual de BurpIA. */
    public static final String VERSION_ACTUAL = "1.7.0";

    /** URL del archivo VERSION.txt en el repositorio remoto. */
    public static final String URL_VERSION_REMOTA =
        "https://raw.githubusercontent.com/DragonJAR/BurpIA/refs/heads/main/VERSION.txt";

    /** URL de descarga del proyecto en GitHub. */
    public static final String URL_DESCARGA =
        "https://github.com/dragonJAR/burpIA/";

    private VersionBurpIA() {
        // Clase de utilidad, no instanciable
    }

    /**
     * Obtiene la versión actual de BurpIA.
     *
     * @return la cadena de versión actual (ej: "1.0.2")
     */
    public static String obtenerVersionActual() {
        return VERSION_ACTUAL;
    }
}
