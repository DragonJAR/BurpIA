package com.burpia.util;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utilidad centralizada para la gestión de rutas del sistema de archivos de BurpIA.
 * <p>
 * Esta clase implementa el principio DRY (Don't Repeat Yourself) centralizando
 * todas las operaciones relacionadas con rutas de configuración y evidencias.
 * </p>
 * <p>
 * Todas las rutas se basan en un directorio raíz ubicado en el home del usuario
 * ({@code ~/.burpia/}), lo que garantiza persistencia y aislamiento de datos.
 * </p>
 *
 * @see Normalizador
 * @see <a href="https://github.com/DragonJAR/BurpIA/blob/main/AGENTS.md">Guía de codificación AGENTS.md</a>
 */
public final class RutasBurpIA {

    /** Nombre del directorio base de la aplicación en el home del usuario. */
    private static final String DIRECTORIO_BASE = ".burpia";

    /** Nombre del archivo de configuración JSON. */
    private static final String ARCHIVO_CONFIG = "config.json";

    /** Nombre del directorio para almacenar evidencias HTTP. */
    private static final String DIRECTORIO_EVIDENCIA = "evidence";

    /** Cache del directorio home del usuario para evitar llamadas repetidas a System.getProperty(). */
    private static volatile String cacheHomeUsuario;

    /**
     * Constructor privado para evitar instanciación.
     * <p>
     * Esta es una clase de utilidad con solo métodos estáticos.
     * </p>
     */
    private RutasBurpIA() {
        // Clase de utilidad, no instanciable
    }

    /**
     * Obtiene la ruta al directorio base de BurpIA.
     * <p>
     * El directorio base es {@code ~/.burpia/} donde se almacenan todos los
     * archivos de configuración y datos de la aplicación.
     * </p>
     *
     * @return la ruta absoluta al directorio base de BurpIA
     */
    public static Path obtenerDirectorioBase() {
        return Paths.get(obtenerHomeSeguro(), DIRECTORIO_BASE);
    }

    /**
     * Obtiene la ruta completa al archivo de configuración de BurpIA.
     * <p>
     * El archivo de configuración se encuentra en {@code ~/.burpia/config.json}.
     * </p>
     *
     * @return la ruta absoluta al archivo de configuración JSON
     */
    public static Path obtenerRutaConfig() {
        return obtenerDirectorioBase().resolve(ARCHIVO_CONFIG);
    }

    /**
     * Obtiene la ruta al directorio donde se almacenan las evidencias HTTP.
     * <p>
     * El directorio de evidencias se encuentra en {@code ~/.burpia/evidence/}.
     * </p>
     *
     * @return la ruta absoluta al directorio de evidencias
     */
    public static Path obtenerDirectorioEvidencia() {
        return obtenerDirectorioBase().resolve(DIRECTORIO_EVIDENCIA);
    }

    /**
     * Obtiene el directorio home del usuario de forma segura.
     * <p>
     * Utiliza un mecanismo de fallback: primero intenta {@code user.home},
     * y si no está disponible, usa {@code user.dir} como alternativa.
     * El resultado se cachea para evitar llamadas repetidas al sistema.
     * </p>
     *
     * @return la ruta al directorio home del usuario, nunca {@code null}
     */
    private static String obtenerHomeSeguro() {
        // Double-checked locking pattern para thread-safe lazy initialization
        String home = cacheHomeUsuario;
        if (home == null) {
            synchronized (RutasBurpIA.class) {
                home = cacheHomeUsuario;
                if (home == null) {
                    home = calcularHomeSeguro();
                    cacheHomeUsuario = home;
                }
            }
        }
        return home;
    }

    /**
     * Limpia la caché del directorio home del usuario.
     * <p>
     * Este método está pensado para uso en pruebas unitarias donde el valor de
     * {@code user.home} puede cambiar entre tests.
     * </p>
     */
    public static void limpiarCacheParaTests() {
        synchronized (RutasBurpIA.class) {
            cacheHomeUsuario = null;
        }
    }

    /**
     * Calcula el directorio home del usuario consultando las propiedades del sistema.
     *
     * @return la ruta al directorio home, o el directorio actual como fallback
     */
    private static String calcularHomeSeguro() {
        String userHome = System.getProperty("user.home");
        if (Normalizador.esVacio(userHome)) {
            userHome = System.getProperty("user.dir", ".");
        }
        return userHome;
    }
}
