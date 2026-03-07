package com.burpia.util;

import java.util.Locale;

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
    public static final String VERSION_ACTUAL = "1.5.0";

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

    /**
     * Normaliza una cadena de versión eliminando caracteres no deseados.
     * <p>
     * Realiza las siguientes operaciones:
     * </p>
     * <ul>
     *   <li>Elimina espacios en blanco al inicio y final</li>
     *   <li>Toma solo la primera línea (hasta el primer salto de línea)</li>
     *   <li>Elimina BOM (Byte Order Mark) si está presente</li>
     *   <li>Elimina prefijo "v" o "V" si existe</li>
     * </ul>
     *
     * @param version la cadena de versión a normalizar, puede ser {@code null}
     * @return la versión normalizada, o cadena vacía si el input es {@code null} o vacío
     */
    public static String normalizarVersion(String version) {
        if (Normalizador.esVacio(version)) {
            return "";
        }

        String limpia = version.trim();

        int salto = limpia.indexOf('\n');
        if (salto >= 0) {
            limpia = limpia.substring(0, salto).trim();
        }

        if (!limpia.isEmpty() && limpia.charAt(0) == '\uFEFF') {
            limpia = limpia.substring(1).trim();
        }

        if (limpia.toLowerCase(Locale.ROOT).startsWith("v")) {
            limpia = limpia.substring(1).trim();
        }
        return limpia;
    }

    /**
     * Determina si dos versiones son diferentes después de normalizarlas.
     * <p>
     * La comparación es insensible a mayúsculas/minúsculas.
     * </p>
     *
     * @param versionLocal  la versión local a comparar
     * @param versionRemota la versión remota a comparar
     * @return {@code true} si las versiones son diferentes, {@code false} si son iguales
     */
    public static boolean sonVersionesDiferentes(String versionLocal, String versionRemota) {
        String local = normalizarVersion(versionLocal);
        String remota = normalizarVersion(versionRemota);
        return !local.equalsIgnoreCase(remota);
    }
}
