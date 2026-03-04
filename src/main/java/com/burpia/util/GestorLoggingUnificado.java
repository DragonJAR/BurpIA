package com.burpia.util;

import burp.api.montoya.MontoyaApi;
import com.burpia.i18n.I18nLogs;

import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.burpia.util.Normalizador.esVacio;
import static com.burpia.util.Normalizador.noEsVacio;

/**
 * Clase unificada para centralizar toda la lógica de logging en la aplicación.
 * Sigue el principio DRY (Don't Repeat Yourself) eliminando duplicación
 * de métodos de logging en múltiples clases.
 *
 * <p>Soporta cuatro formas de logging:</p>
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

    private static final String SEPARADOR_LOG = "==================================================";
    private static final String ORIGEN_POR_DEFECTO = "BurpIA";

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
     * @param origen Origen del mensaje (ej: "BurpIA", nombre de componente). Si es null o vacío, se usa "BurpIA".
     * @param mensaje Mensaje a registrar (puede tener marcadores de i18n)
     */
    public void info(String origen, String mensaje) {
        if (esVacio(mensaje)) {
            return;
        }

        String origenNormalizado = normalizarOrigen(origen);
        String mensajeLocalizado = I18nLogs.tr(mensaje);

        // 1. Log a consola GUI
        if (gestorConsola != null) {
            gestorConsola.registrarInfo(origenNormalizado, mensajeLocalizado);
        }

        // 2. Log a stdout
        if (stdout != null) {
            stdout.println(construirPrefijoConsola(origenNormalizado) + mensajeLocalizado);
            stdout.flush();
        }

        // 3. Log a Burp API
        logToBurpApi(origenNormalizado, mensajeLocalizado, false);

        // 4. Log a java.util.logging
        if (logger != null && logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, mensajeLocalizado);
        }
    }

    /**
     * Registra un mensaje de error.
     *
     * @param origen Origen del mensaje. Si es null o vacío, se usa "BurpIA".
     * @param mensaje Mensaje de error
     */
    public void error(String origen, String mensaje) {
        if (esVacio(mensaje)) {
            return;
        }

        String origenNormalizado = normalizarOrigen(origen);
        String mensajeLocalizado = I18nLogs.tr(mensaje);

        // 1. Log a consola GUI
        if (gestorConsola != null) {
            gestorConsola.registrarError(origenNormalizado, mensajeLocalizado);
        }

        // 2. Log a stderr
        if (stderr != null) {
            stderr.println(construirPrefijoConsola(origenNormalizado) + "[ERROR] " + mensajeLocalizado);
            stderr.flush();
        }

        // 3. Log a Burp API
        logToBurpApi(origenNormalizado, "[ERROR] " + mensajeLocalizado, true);

        // 4. Log a java.util.logging
        if (logger != null && logger.isLoggable(Level.SEVERE)) {
            logger.log(Level.SEVERE, mensajeLocalizado);
        }
    }

    /**
     * Registra un mensaje de error con excepción.
     *
     * @param origen Origen del mensaje. Si es null o vacío, se usa "BurpIA".
     * @param mensaje Mensaje de error
     * @param throwable Excepción asociada
     */
    public void error(String origen, String mensaje, Throwable throwable) {
        if (esVacio(mensaje) && throwable == null) {
            return;
        }

        String origenNormalizado = normalizarOrigen(origen);
        String mensajeLocalizado = noEsVacio(mensaje) ? I18nLogs.tr(mensaje) : "";
        String stackTrace = throwable != null ? obtenerStackTrace(throwable) : "";

        // 1. Log a consola GUI
        if (gestorConsola != null) {
            String mensajeCompleto = mensajeLocalizado;
            if (noEsVacio(stackTrace)) {
                mensajeCompleto = mensajeLocalizado + " - " + stackTrace;
            }
            gestorConsola.registrarError(origenNormalizado, mensajeCompleto);
        }

        // 2. Log a stderr
        if (stderr != null) {
            stderr.println(construirPrefijoConsola(origenNormalizado) + "[ERROR] " + mensajeLocalizado);
            if (throwable != null) {
                throwable.printStackTrace(stderr);
            }
            stderr.flush();
        }

        // 3. Log a Burp API
        logToBurpApi(origenNormalizado, "[ERROR] " + mensajeLocalizado + (noEsVacio(stackTrace) ? " - " + stackTrace : ""), true);

        // 4. Log a java.util.logging
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
        if (esVacio(mensaje) || nivel == null) {
            return;
        }

        // Solo log si el nivel está habilitado
        if (logger != null && logger.isLoggable(nivel)) {
            logger.log(nivel, mensaje);
        } else if (stdout != null) {
            // Fallback a stdout si no hay logger
            String prefijo = nivel == Level.SEVERE ? "[ERROR] " : (nivel == Level.WARNING ? "[WARNING] " : "[INFO] ");
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
        if (esVacio(mensaje) || nivel == null) {
            return;
        }

        if (logger != null && logger.isLoggable(nivel)) {
            logger.log(nivel, mensaje, throwable);
        }
    }

    /**
     * Registra un mensaje de tipo verbose/traza.
     *
     * @param origen Origen del mensaje. Si es null o vacío, se usa "BurpIA".
     * @param mensaje Mensaje a registrar
     */
    public void verbose(String origen, String mensaje) {
        if (esVacio(mensaje)) {
            return;
        }

        String origenNormalizado = normalizarOrigen(origen);
        String mensajeLocalizado = I18nLogs.tr(mensaje);

        // 1. Log a consola GUI
        if (gestorConsola != null) {
            gestorConsola.registrarVerbose(origenNormalizado, mensajeLocalizado);
        }

        // 2. Log a stdout (verbose no suele ser crítico)
        if (stdout != null) {
            stdout.println(construirPrefijoConsola(origenNormalizado) + "[VERBOSE] " + mensajeLocalizado);
            stdout.flush();
        }

        // 3. Log a java.util.logging (FINE level)
        if (logger != null && logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, mensajeLocalizado);
        }
    }

    /**
     * Registra un mensaje de advertencia.
     *
     * @param origen Origen del mensaje. Si es null o vacío, se usa "BurpIA".
     * @param mensaje Mensaje a registrar
     */
    public void warning(String origen, String mensaje) {
        if (esVacio(mensaje)) {
            return;
        }

        String origenNormalizado = normalizarOrigen(origen);
        String mensajeLocalizado = I18nLogs.tr(mensaje);

        // 1. Log a consola GUI (como INFO con prefijo WARNING)
        if (gestorConsola != null) {
            gestorConsola.registrarInfo(origenNormalizado, "[WARNING] " + mensajeLocalizado);
        }

        // 2. Log a stdout
        if (stdout != null) {
            stdout.println(construirPrefijoConsola(origenNormalizado) + "[WARNING] " + mensajeLocalizado);
            stdout.flush();
        }

        // 3. Log a Burp API
        logToBurpApi(origenNormalizado, "[WARNING] " + mensajeLocalizado, false);

        // 4. Log a java.util.logging
        if (logger != null && logger.isLoggable(Level.WARNING)) {
            logger.log(Level.WARNING, mensajeLocalizado);
        }
    }

    /**
     * Registra un separador visual en los logs.
     */
    public void separador() {
        if (gestorConsola != null) {
            gestorConsola.registrarInfo(ORIGEN_POR_DEFECTO, SEPARADOR_LOG);
        }
        if (stdout != null) {
            stdout.println(SEPARADOR_LOG);
            stdout.flush();
        }
    }

    // ============ MÉTODOS DE CONVENIENCIA ============

    /**
     * Método de conveniencia para registrar info sin origen explícito.
     *
     * @param mensaje Mensaje a registrar
     */
    public void info(String mensaje) {
        info(ORIGEN_POR_DEFECTO, mensaje);
    }

    /**
     * Método de conveniencia para registrar error sin origen explícito.
     *
     * @param mensaje Mensaje de error
     */
    public void error(String mensaje) {
        error(ORIGEN_POR_DEFECTO, mensaje);
    }

    /**
     * Método de conveniencia para registrar error con excepción.
     *
     * @param mensaje Mensaje de error
     * @param throwable Excepción asociada
     */
    public void error(String mensaje, Throwable throwable) {
        error(ORIGEN_POR_DEFECTO, mensaje, throwable);
    }

    /**
     * Método de conveniencia para registrar verbose sin origen explícito.
     *
     * @param mensaje Mensaje a registrar
     */
    public void verbose(String mensaje) {
        verbose(ORIGEN_POR_DEFECTO, mensaje);
    }

    /**
     * Método de conveniencia para registrar warning sin origen explícito.
     *
     * @param mensaje Mensaje a registrar
     */
    public void warning(String mensaje) {
        warning(ORIGEN_POR_DEFECTO, mensaje);
    }

    // ============ MÉTODOS PRIVADOS DE AYUDA ============

    /**
     * Normaliza el origen, usando el valor por defecto si es null o vacío.
     * Sigue el principio DRY usando Normalizador.esVacio().
     */
    private static String normalizarOrigen(String origen) {
        if (esVacio(origen)) {
            return ORIGEN_POR_DEFECTO;
        }
        return origen.trim();
    }

    /**
     * Construye un prefijo para consola con el formato [Origen].
     */
    private static String construirPrefijoConsola(String origen) {
        return "[" + origen + "] ";
    }

    /**
     * Construye un prefijo para Burp API sin corchetes (formato "Origen: ").
     */
    private static String construirPrefijoBurpApi(String origen) {
        return origen + ": ";
    }

    /**
     * Registra mensaje en Burp API con manejo de errores.
     * Si falla el logging a Burp API, intenta loguear a stderr como fallback.
     *
     * @param origen Origen normalizado
     * @param mensaje Mensaje completo (incluye prefijos de nivel si aplica)
     * @param esError Si es true, usa logToError; si es false, usa logToOutput
     */
    private void logToBurpApi(String origen, String mensaje, boolean esError) {
        if (api == null) {
            return;
        }
        try {
            String mensajeCompleto = construirPrefijoBurpApi(origen) + mensaje;
            if (esError) {
                api.logging().logToError(mensajeCompleto);
            } else {
                api.logging().logToOutput(mensajeCompleto);
            }
        } catch (Exception e) {
            // Fallback: intentar loguear el error de logging a stderr
            if (stderr != null) {
                stderr.println("[BurpIA] [ERROR] Fallo al loguear a Burp API: " + e.getMessage());
                stderr.flush();
            }
        }
    }

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
}
