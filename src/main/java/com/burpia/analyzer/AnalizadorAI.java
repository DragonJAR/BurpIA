package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nUI;
import com.burpia.i18n.I18nLogs;
import com.burpia.model.Hallazgo;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.ControlBackpressureGlobal;
import com.burpia.util.LimitadorTasa;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.Normalizador;
import okhttp3.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

public class AnalizadorAI implements Runnable {
    private static final String ORIGEN_LOG = "AnalizadorAI";
    private final SolicitudAnalisis solicitud;
    private final ConfiguracionAPI config;
    private final PrintWriter stdout;
    private final PrintWriter stderr;
    private final LimitadorTasa limitador;
    private final Callback callback;
    private final Runnable alInicioAnalisis;
    private final GestorConsolaGUI gestorConsola;
    private final BooleanSupplier tareaCancelada;
    private final BooleanSupplier tareaPausada;
    private final ControlBackpressureGlobal controlBackpressure;
    private final GestorLoggingUnificado gestorLogging;
    private final OrquestadorAnalisis orquestador;
    private final ParseadorRespuestasAI parseador;
    private final GestorMultiProveedor gestorMulti;
    
    // Campos para compatibilidad con tests
    @SuppressWarnings("unused")
    private final AnalizadorHTTP analizadorHTTP;

    /**
     * Interfaz de callback para recibir notificaciones del análisis.
     */
    public interface Callback {
        /**
         * Called when analysis completes successfully.
         * 
         * @param resultado The analysis result containing findings
         */
        void alCompletarAnalisis(ResultadoAnalisisMultiple resultado);

        /**
         * Called when analysis encounters an error.
         * 
         * @param error Error message describing the failure
         */
        void alErrorAnalisis(String error);

        /**
         * Called when analysis is cancelled by user.
         * Default implementation does nothing.
         */
        default void alCanceladoAnalisis() {
        }
    }

    /**
     * Constructor principal del analizador AI.
     *
     * @param solicitud           Solicitud HTTP a analizar
     * @param config              Configuración de API (si es null, se crea una por
     *                            defecto)
     * @param stdout              PrintWriter para salida estándar (si es null, se
     *                            usa null output)
     * @param stderr              PrintWriter para errores (si es null, se usa null
     *                            output)
     * @param limitador           Limitador de tasa para controlar concurrencia (si
     *                            es null, se crea con límite 1)
     * @param callback            Callback para notificar resultados (si es null, se
     *                            usa callback vacío)
     * @param alInicioAnalisis    Runnable a ejecutar al inicio del análisis (puede
     *                            ser null)
     * @param gestorConsola       Gestor de consola GUI para logging (puede ser
     *                            null)
     * @param tareaCancelada      Supplier que indica si la tarea fue cancelada (si
     *                            es null, siempre false)
     * @param tareaPausada        Supplier que indica si la tarea está pausada (si
     *                            es null, siempre false)
     * @param controlBackpressure Control global de backpressure para rate limiting
     *                            (puede ser null)
     */
    public AnalizadorAI(SolicitudAnalisis solicitud, ConfiguracionAPI config, PrintWriter stdout, PrintWriter stderr,
            LimitadorTasa limitador, Callback callback, Runnable alInicioAnalisis,
            GestorConsolaGUI gestorConsola, BooleanSupplier tareaCancelada, BooleanSupplier tareaPausada,
            ControlBackpressureGlobal controlBackpressure) {
        this.solicitud = solicitud;
        this.config = config != null ? config : new ConfiguracionAPI();
        this.stdout = stdout != null ? stdout : new PrintWriter(OutputStream.nullOutputStream(), true);
        this.stderr = stderr != null ? stderr : new PrintWriter(OutputStream.nullOutputStream(), true);
        this.limitador = limitador != null ? limitador : new LimitadorTasa(1);
        this.callback = callback != null ? callback : new Callback() {
            @Override
            public void alCompletarAnalisis(ResultadoAnalisisMultiple resultado) {
            }

            @Override
            public void alErrorAnalisis(String error) {
            }
        };
        this.alInicioAnalisis = alInicioAnalisis;
        this.gestorConsola = gestorConsola;
        this.tareaCancelada = tareaCancelada != null ? tareaCancelada : () -> false;
        this.tareaPausada = tareaPausada != null ? tareaPausada : () -> false;
        this.controlBackpressure = controlBackpressure;
        
        this.gestorLogging = GestorLoggingUnificado.crear(gestorConsola, stdout, stderr, null, null);
        
        // Crear las clases helper
        OrquestadorAnalisis.Callback callbackOrquestador = new OrquestadorAnalisis.Callback() {
            @Override
            public void alCompletarAnalisis(ResultadoAnalisisMultiple resultado) {
                // No se usa directamente aquí, manejamos callbacks en run()
            }
            
            @Override
            public void alErrorAnalisis(String error) {
                // No se usa directamente aquí, manejamos callbacks en run()
            }
        };
        
        this.orquestador = new OrquestadorAnalisis(
            solicitud, config, stdout, stderr, limitador, callbackOrquestador,
            alInicioAnalisis, gestorConsola, tareaCancelada, tareaPausada);
            
        this.parseador = new ParseadorRespuestasAI(gestorLogging, 
            config != null ? config.obtenerIdiomaUi() : "es");
        
        this.gestorMulti = new GestorMultiProveedor(
            solicitud, config, stdout, stderr, gestorConsola,
            tareaCancelada, tareaPausada, gestorLogging);
            
        // Crear analizadorHTTP para compatibilidad con tests
        this.analizadorHTTP = new AnalizadorHTTP(config, tareaCancelada, tareaPausada, gestorLogging);

        int timeoutEfectivo = this.config.obtenerTiempoEsperaParaModelo(
                this.config.obtenerProveedorAI(),
                this.config.obtenerModelo());

        gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("[" + Thread.currentThread().getName() + "] Timeout configurado para el cliente HTTP: "
                + timeoutEfectivo + "s"));
    }

    public AnalizadorAI(SolicitudAnalisis solicitud, ConfiguracionAPI config, PrintWriter stdout, PrintWriter stderr,
            LimitadorTasa limitador, Callback callback, GestorConsolaGUI gestorConsola,
            BooleanSupplier tareaCancelada, BooleanSupplier tareaPausada) {
        this(solicitud, config, stdout, stderr, limitador, callback, null, gestorConsola, tareaCancelada, tareaPausada,
                null);
    }

    public AnalizadorAI(SolicitudAnalisis solicitud, ConfiguracionAPI config, PrintWriter stdout, PrintWriter stderr,
            LimitadorTasa limitador, Callback callback) {
        this(solicitud, config, stdout, stderr, limitador, callback, null, null, null, null, null);
    }

    public AnalizadorAI(SolicitudAnalisis solicitud, ConfiguracionAPI config, PrintWriter stdout, PrintWriter stderr,
            LimitadorTasa limitador, Callback callback, GestorConsolaGUI gestorConsola) {
        this(solicitud, config, stdout, stderr, limitador, callback, null, gestorConsola, null, null, null);
    }

    /**
     * Cancela la llamada HTTP activa si existe.
     * Libera inmediatamente el thread y el socket al usar OkHttp Call.cancel().
     *
     * <p>
     * Este método está diseñado para ser llamado desde un thread externo
     * cuando se pausa o cancela una tarea, permitiendo que la siguiente tarea
     * en cola comience inmediatamente.
     * </p>
     */
    public void cancelarLlamadaHttpActiva() {
        orquestador.cancelarLlamadaHttpActiva();
    }

    @Override
    public void run() {
        String nombreHilo = Thread.currentThread().getName();
        long tiempoInicio = System.currentTimeMillis();
        boolean permisoAdquirido = false;

        if (solicitud == null) {
            String error = mensajeErrorSolicitudNoDisponible();
            gestorLogging.error(ORIGEN_LOG, "[" + nombreHilo + "] " + error);
            callback.alErrorAnalisis(error);
            return;
        }

        gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("[" + nombreHilo + "] AnalizadorAI iniciado para URL: " + solicitud.obtenerUrl()));
        gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("[" + nombreHilo + "] Hash de solicitud: " + solicitud.obtenerHashSolicitud()));

        try {
            verificarCancelacion();
            esperarSiPausada();
            notificarInicioAnalisis();
            
            String alertaConfiguracion = validarConfiguracionAntesDeConsulta();
            if (Normalizador.noEsVacio(alertaConfiguracion)) {
                gestorLogging.error(ORIGEN_LOG, alertaConfiguracion);
                callback.alErrorAnalisis(alertaConfiguracion);
                return;
            }

            gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("[" + nombreHilo + "] Adquiriendo permiso del limitador (disponibles: " +
                    limitador.permisosDisponibles() + ")"));
            limitador.adquirir();
            permisoAdquirido = true;
            gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("[" + nombreHilo + "] Permiso de limitador adquirido"));

            int retrasoSegundos = config.obtenerRetrasoSegundos();
            gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("[" + nombreHilo + "] Durmiendo por " + retrasoSegundos + " segundos antes de llamar a la API"));
            esperarConControl(retrasoSegundos * 1000L);

            gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Analizando: " + solicitud.obtenerUrl()));

            boolean multiHabilitado = config.esMultiProveedorHabilitado();
            java.util.List<String> proveedoresConfig = config.obtenerProveedoresMultiConsulta();
            gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("DIAGNOSTICO: multiHabilitado=" + multiHabilitado + ", proveedoresConfig=" +
                    (proveedoresConfig != null ? proveedoresConfig.size() + " elementos" : "null")));

            ResultadoAnalisisMultiple resultadoMultiple;
            if (multiHabilitado && proveedoresConfig != null && proveedoresConfig.size() > 1) {
                gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("DIAGNOSTICO: Ejecutando multi-proveedor con " + proveedoresConfig.size() + " proveedores"));
                resultadoMultiple = gestorMulti.ejecutarAnalisisMultiProveedor();
            } else {
                if (multiHabilitado) {
                    gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("PROVEEDOR: Multi-proveedor habilitado pero solo " +
                            (proveedoresConfig != null ? proveedoresConfig.size() : 0) +
                            " proveedor(es) configurado(s). Usando proveedor único: " + config.obtenerProveedorAI()));
                } else {
                    gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("PROVEEDOR: Usando proveedor único: " + config.obtenerProveedorAI()));
                }
                resultadoMultiple = orquestador.ejecutarAnalisisCompleto();
            }

            long duracion = System.currentTimeMillis() - tiempoInicio;
            gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Analisis completado: " + solicitud.obtenerUrl() + " (tomo " + duracion + "ms)"));
            gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("[" + nombreHilo + "] Severidad maxima: " + resultadoMultiple.obtenerSeveridadMaxima()));

            callback.alCompletarAnalisis(resultadoMultiple);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long duracion = System.currentTimeMillis() - tiempoInicio;
            String causa = Normalizador.noEsVacio(e.getMessage())
                ? e.getMessage()
                : I18nUI.General.ERROR_INESPERADO_TIPO(e.getClass().getSimpleName());

            if (esPausada()) {
                gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("[" + nombreHilo + "] Analisis pausado y liberando hilo (" + duracion + "ms)"));
                return;
            }

            if (esCancelada()) {
                gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("[" + nombreHilo + "] Analisis cancelado por usuario (" + duracion + "ms)"));
                callback.alCanceladoAnalisis();
            } else {
                gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("[" + nombreHilo + "] Analisis interrumpido despues de " + duracion + "ms: " + causa));
                callback.alErrorAnalisis(mensajeAnalisisInterrumpido(causa));
            }
        } catch (Exception e) {
            long duracion = System.currentTimeMillis() - tiempoInicio;
            String falloMsg = Normalizador.noEsVacio(e.getMessage())
                ? e.getMessage()
                : I18nUI.Tareas.MSG_ERROR_DESCONOCIDO();

            gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("[" + nombreHilo + "] Analisis fallido despues de " + duracion + "ms: " + falloMsg));
            callback.alErrorAnalisis(falloMsg);
        } finally {
            if (permisoAdquirido) {
                limitador.liberar();
                gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("[" + nombreHilo + "] Permiso de limitador liberado (disponibles: " +
                        limitador.permisosDisponibles() + ")"));
            }
        }
    }

    private void notificarInicioAnalisis() {
        if (alInicioAnalisis == null) {
            return;
        }
        try {
            alInicioAnalisis.run();
        } catch (Exception e) {
            gestorLogging.verbose(ORIGEN_LOG, I18nLogs.tr("No se pudo notificar inicio de analisis"));
        }
    }

    private String validarConfiguracionAntesDeConsulta() {
        if (config == null) {
            return alertaConfiguracionNoDisponible();
        }
        String error = config.validarParaConsultaModelo();
        return error != null ? error.trim() : "";
    }

    private boolean esCancelada() {
        return tareaCancelada.getAsBoolean();
    }

    private boolean esPausada() {
        return tareaPausada.getAsBoolean();
    }

    private void verificarCancelacion() throws InterruptedException {
        if (esCancelada()) {
            throw new InterruptedException(I18nUI.Tareas.MSG_CANCELADO_USUARIO());
        }
    }

    private void esperarSiPausada() throws InterruptedException {
        while (esPausada() && !esCancelada()) {
            Thread.sleep(250);
        }
        verificarCancelacion();
    }

    private void esperarConControl(long milisegundos) throws InterruptedException {
        long restante = milisegundos;
        while (restante > 0) {
            verificarCancelacion();
            esperarSiPausada();
            long espera = Math.min(restante, 250);
            Thread.sleep(espera);
            restante -= espera;
        }
    }

    private static String mensajeErrorSolicitudNoDisponible() {
        return I18nUI.tr("Solicitud de analisis no disponible", "Analysis request is not available");
    }

    private static String alertaConfiguracionNoDisponible() {
        return I18nUI.tr("ALERTA: Configuracion de IA no disponible", "ALERT: AI configuration is unavailable");
    }

    private static String mensajeAnalisisInterrumpido(String causa) {
        return I18nUI.trf("Analisis interrumpido: %s", "Analysis interrupted: %s", causa);
    }
    
    /**
     * Método parsearRespuesta para compatibilidad con tests.
     * Ahora delega a ParseadorRespuestasAI.
     */
    private ResultadoAnalisisMultiple parsearRespuesta(String respuestaJson) {
        String proveedor = config != null ? config.obtenerProveedorAI() : "";
        return parseador.parsearRespuesta(respuestaJson, solicitud, proveedor);
    }
}
