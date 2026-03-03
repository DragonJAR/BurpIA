package com.burpia.util;

import burp.api.montoya.MontoyaApi;
import com.burpia.i18n.I18nLogs;

import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Clase unificada para centralizar toda la lógica de logging en la aplicación.
 * Sigue el principio DRY (Don't Repeat Yourself) eliminando duplicación
 * de métodos de logging en múltiples clases.
 *
 * <p>Soporta tres formas de logging:</p>
 * <ul>
 *   <li>Logging a consola GUI (vía GestorConsolaGUI)</li>
 *   <li>Logging a stdout/stderr (vía PrintWriter)</li>
 *   <li>Logging a java.util.logging.Logger</li>
 *   <li>Logging a Burp Suite API (cuando está disponible)</li>
 * </ul>
 *
 * @author BurpIA Team
 * @since 1.0.3
 */
public final class GestorLoggingUnificado {

    private static final String PREFIJO_INFO = "[BurpIA] ";
    private static final String PREFIJO_ERROR = "[BurpIA] [ERROR] ";
    private static final String SEPARADOR_LOG = "==================================================";

    private final GestorConsolaGUI gestorConsola;
    private final PrintWriter stdout;
    private final PrintWriter stderr;
    private final MontoyaApi api;
    private final Logger logger;

    /**
     * Constructor privado para evitar instanciación directa.
     * Usar los métodos estáticos de conveniencia en su lugar.
     */
    private GestorLoggingUnificado(GestorConsolaGUI gestorConsola,
                                   PrintWriter stdout,
                                   PrintWriter stderr,
                                   MontoyaApi api,
                                   Logger logger) {
        this.gestorConsola = gestorConsola;
        this.stdout = stdout;
        this.stderr = stderr;
        this.api = api;
        this.logger = logger;
    }

    /**
     * Crea una nueva instancia del gestor de logging.
     *
     * @param gestorConsola Gestor de consola GUI (puede ser null)
     * @param stdout Stream de salida estándar (puede ser null)
     * @param stderr Stream de error (puede ser null)
     * @param api API de Burp Suite (puede ser null)
     * @param logger Logger de java.util.logging (puede ser null)
     * @return Nueva instancia de GestorLoggingUnificado
     */
    public static GestorLoggingUnificado crear(GestorConsolaGUI gestorConsola,
                                               PrintWriter stdout,
                                               PrintWriter stderr,
                                               MontoyaApi api,
                                               Logger logger) {
        return new GestorLoggingUnificado(gestorConsola, stdout, stderr, api, logger);
    }

    /**
     * Crea una instancia mínima solo con stdout/stderr.
     * Útil para componentes que no tienen acceso a la API completa.
     *
     * @param stdout Stream de salida estándar (puede ser null)
     * @param stderr Stream de error (puede ser null)
     * @return Nueva instancia de GestorLoggingUnificado
     */
    public static GestorLoggingUnificado crearMinimal(PrintWriter stdout, PrintWriter stderr) {
        return new GestorLoggingUnificado(null, stdout, stderr, null, null);
    }

    /**
     * Crea una instancia con Logger de java.util.logging.
     *
     * @param logger Logger de java.util.logging
     * @return Nueva instancia de GestorLoggingUnificado
     */
    public static GestorLoggingUnificado crearConLogger(Logger logger) {
        return new GestorLoggingUnificado(null, null, null, null, logger);
    }

    // ============ MÉTODOS PÚBLICOS DE LOGGING ============

    /**
     * Registra un mensaje informativo.
     *
     * @param origen Origen del mensaje (ej: "BurpIA", nombre de componente)
     * @param mensaje Mensaje a registrar (puede tener marcadores de i18n)
     */
    public void info(String origen, String mensaje) {
        if (mensaje == null) {
            return;
        }

        String mensajeLocalizado = I18nLogs.tr(mensaje);

        // 1. Intentar log a consola GUI
        if (gestorConsola != null) {
            gestorConsola.registrarInfo(origen, mensajeLocalizado);
        }

        // 2. Log a stdout/stderr
        if (stdout != null) {
            String prefijo = origen != null ? "[" + origen + "] " : "";
            stdout.println(prefijo + mensajeLocalizado);
            stdout.flush();
        }

        // 3. Log a Burp API
        if (api != null) {
            try {
                api.logging().logToOutput(prefijoSinCorchetes(origen) + mensajeLocalizado);
            } catch (Exception e) {
                // Ignorar errores de logging a Burp API
            }
        }

        // 4. Log a java.util.logging
        if (logger != null && logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, mensajeLocalizado);
        }
    }

    /**
     * Registra un mensaje de error.
     *
     * @param origen Origen del mensaje
     * @param mensaje Mensaje de error
     */
    public void error(String origen, String mensaje) {
        if (mensaje == null) {
            return;
        }

        String mensajeLocalizado = I18nLogs.tr(mensaje);

        // 1. Intentar log a consola GUI
        if (gestorConsola != null) {
            gestorConsola.registrarError(origen, mensajeLocalizado);
        }

        // 2. Log a stderr
        if (stderr != null) {
            String prefijo = origen != null ? "[" + origen + "] [ERROR] " : "[ERROR] ";
            stderr.println(prefijo + mensajeLocalizado);
            stderr.flush();
        }

        // 3. Log a Burp API
        if (api != null) {
            try {
                api.logging().logToError(prefijoSinCorchetes(origen) + "[ERROR] " + mensajeLocalizado);
            } catch (Exception e) {
                // Ignorar errores de logging a Burp API
            }
        }

        // 4. Log a java.util.logging
        if (logger != null && logger.isLoggable(Level.SEVERE)) {
            logger.log(Level.SEVERE, mensajeLocalizado);
        }
    }

    /**
     * Registra un mensaje de error con excepción.
     *
     * @param origen Origen del mensaje
     * @param mensaje Mensaje de error
     * @param throwable Excepción asociada
     */
    public void error(String origen, String mensaje, Throwable throwable) {
        if (mensaje == null && throwable == null) {
            return;
        }

        String mensajeLocalizado = mensaje != null ? I18nLogs.tr(mensaje) : "";
        String stackTrace = throwable != null ? obtenerStackTrace(throwable) : "";

        // 1. Log a consola GUI
        if (gestorConsola != null) {
            gestorConsola.registrarError(origen, mensajeLocalizado + " - " + stackTrace);
        }

        // 2. Log a stderr
        if (stderr != null) {
            String prefijo = origen != null ? "[" + origen + "] [ERROR] " : "[ERROR] ";
            stderr.println(prefijo + mensajeLocalizado);
            if (throwable != null) {
                throwable.printStackTrace(stderr);
            }
            stderr.flush();
        }

        // 3. Log a java.util.logging
        if (logger != null && logger.isLoggable(Level.SEVERE)) {
            if (throwable != null) {
                logger.log(Level.SEVERE, mensajeLocalizado, throwable);
            } else {
                logger.log(Level.SEVERE, mensajeLocalizado);
            }
        }
    }

    /**
     * Registra un mensaje con nivel específico de java.util.logging.
     *
     * @param nivel Nivel de log (INFO, WARNING, SEVERE, etc.)
     * @param mensaje Mensaje a registrar
     */
    public void log(Level nivel, String mensaje) {
        if (mensaje == null || nivel == null) {
            return;
        }

        // Solo log si el nivel está habilitado
        if (logger != null && logger.isLoggable(nivel)) {
            logger.log(nivel, mensaje);
        } else if (stdout != null) {
            // Fallback a stdout si no hay logger
            String prefijo = nivel == Level.SEVERE ? "[ERROR] " : "[INFO] ";
            stdout.println(prefijo + mensaje);
            stdout.flush();
        }
    }

    /**
     * Registra un mensaje con nivel y excepción.
     *
     * @param nivel Nivel de log
     * @param mensaje Mensaje a registrar
     * @param throwable Excepción asociada
     */
    public void log(Level nivel, String mensaje, Throwable throwable) {
        if (mensaje == null || nivel == null) {
            return;
        }

        if (logger != null && logger.isLoggable(nivel)) {
            logger.log(nivel, mensaje, throwable);
        }
    }

    /**
     * Registra un separador visual en los logs.
     */
    public void separador() {
        if (gestorConsola != null) {
            gestorConsola.registrarInfo("BurpIA", SEPARADOR_LOG);
        }
        if (stdout != null) {
            stdout.println(SEPARADOR_LOG);
        }
    }

    // ============ MÉTODOS DE CONVENIENCIA ============

    /**
     * Método de conveniencia para registrar info sin origen explícito.
     *
     * @param mensaje Mensaje a registrar
     */
    public void info(String mensaje) {
        info("BurpIA", mensaje);
    }

    /**
     * Método de conveniencia para registrar error sin origen explícito.
     *
     * @param mensaje Mensaje de error
     */
    public void error(String mensaje) {
        error("BurpIA", mensaje);
    }

    /**
     * Método de conveniencia para registrar error con excepción.
     *
     * @param mensaje Mensaje de error
     * @param throwable Excepción asociada
     */
    public void error(String mensaje, Throwable throwable) {
        error("BurpIA", mensaje, throwable);
    }

    // ============ MÉTODOS PRIVADOS DE AYUDA ============

    /**
     * Genera el stack trace de una excepción como string.
     */
    private static String obtenerStackTrace(Throwable throwable) {
        if (throwable == null) {
            return "";
        }

        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Genera un prefijo sin corchetes para logging a Burp API.
     */
    private static String prefijoSinCorchetes(String origen) {
        if (origen == null || origen.isEmpty()) {
            return "BurpIA: ";
        }
        return origen + ": ";
    }
}
