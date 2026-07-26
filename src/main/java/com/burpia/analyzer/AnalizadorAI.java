package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nUI;
import com.burpia.i18n.I18nLogs;
import com.burpia.model.ResultadoAnalisisMultiple;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.ControlCancelacionPausa;
import com.burpia.util.LimitadorTasa;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.Normalizador;
import java.io.OutputStream;
import java.io.PrintWriter;
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
    private final ControlCancelacionPausa controlCancelacionPausa;
    private final GestorLoggingUnificado gestorLogging;
    private final OrquestadorAnalisis orquestador;
    private final ParseadorRespuestasAI parseador;
    private final GestorMultiProveedor gestorMulti;

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
     */
    public AnalizadorAI(SolicitudAnalisis solicitud, ConfiguracionAPI config, PrintWriter stdout, PrintWriter stderr,
            LimitadorTasa limitador, Callback callback, Runnable alInicioAnalisis,
            GestorConsolaGUI gestorConsola, BooleanSupplier tareaCancelada, BooleanSupplier tareaPausada) {
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
        this.controlCancelacionPausa = new ControlCancelacionPausa(tareaCancelada, tareaPausada);
        
        this.gestorLogging = GestorLoggingUnificado.crear(gestorConsola, stdout, stderr, null, null);

        this.orquestador = new OrquestadorAnalisis(
            solicitud, this.config, stdout, stderr,
            alInicioAnalisis, gestorConsola, this.controlCancelacionPausa);
            
        this.parseador = new ParseadorRespuestasAI(gestorLogging, 
            config != null ? config.obtenerIdiomaUi() : "es");
        
        this.gestorMulti = new GestorMultiProveedor(
            solicitud, config, stdout, stderr, gestorConsola,
            tareaCancelada, tareaPausada, gestorLogging);

        int timeoutEfectivo = this.config.obtenerTiempoEsperaParaModelo(
                this.config.obtenerProveedorAI(),
                this.config.obtenerModelo());

        gestorLogging.verbose(ORIGEN_LOG, I18nLogs.trf("[%s] Timeout configurado para el cliente HTTP: %ds",
                Thread.currentThread().getName(), timeoutEfectivo));
    }

    public AnalizadorAI(SolicitudAnalisis solicitud, ConfiguracionAPI config, PrintWriter stdout, PrintWriter stderr,
            LimitadorTasa limitador, Callback callback, GestorConsolaGUI gestorConsola,
            BooleanSupplier tareaCancelada, BooleanSupplier tareaPausada) {
        this(solicitud, config, stdout, stderr, limitador, callback, null, gestorConsola, tareaCancelada, tareaPausada);
    }

    public AnalizadorAI(SolicitudAnalisis solicitud, ConfiguracionAPI config, PrintWriter stdout, PrintWriter stderr,
            LimitadorTasa limitador, Callback callback) {
        this(solicitud, config, stdout, stderr, limitador, callback, null, null, null, null);
    }

    public AnalizadorAI(SolicitudAnalisis solicitud, ConfiguracionAPI config, PrintWriter stdout, PrintWriter stderr,
            LimitadorTasa limitador, Callback callback, GestorConsolaGUI gestorConsola) {
        this(solicitud, config, stdout, stderr, limitador, callback, null, gestorConsola, null, null);
    }

    /**
     * Cancela la llamada HTTP activa si existe, en cualquier modo de análisis.
     * Libera inmediatamente el thread y el socket al usar OkHttp Call.cancel().
     *
     * <p>Fan-out DRY: cancela tanto al AnalizadorHTTP del orquestador (modo
     * único) como al del gestor multi-proveedor (modo multi). El que esté
     * inactivo simplemente no tendrá Call activa y su cancelación es no-op, sin
     * necesidad de trackear el modo actual. Antes solo se cancelaba el del
     * orquestador, por lo que la cancelación no llegaba al socket en multi.</p>
     */
    public void cancelarLlamadaHttpActiva() {
        orquestador.cancelarLlamadaHttpActiva();
        gestorMulti.cancelarLlamadaActiva();
    }

    @Override
    public void run() {
        String nombreHilo = Thread.currentThread().getName();
        long tiempoInicio = System.currentTimeMillis();
        boolean permisoAdquirido = false;

        if (solicitud == null) {
            String error = mensajeErrorSolicitudNoDisponible();
            gestorLogging.error(ORIGEN_LOG, I18nLogs.trf("[%s] %s", nombreHilo, error));
            callback.alErrorAnalisis(error);
            return;
        }

        gestorLogging.info(ORIGEN_LOG, I18nLogs.trf("[%s] AnalizadorAI iniciado para URL: %s", nombreHilo, solicitud.obtenerUrl()));
        gestorLogging.verbose(ORIGEN_LOG, I18nLogs.trf("[%s] Hash de solicitud: %s", nombreHilo, solicitud.obtenerHashSolicitud()));

        try {
            controlCancelacionPausa.verificarCancelacion();
            controlCancelacionPausa.esperarSiPausada();
            notificarInicioAnalisis();
            
            String alertaConfiguracion = validarConfiguracionAntesDeConsulta();
            if (Normalizador.noEsVacio(alertaConfiguracion)) {
                gestorLogging.error(ORIGEN_LOG, alertaConfiguracion);
                callback.alErrorAnalisis(alertaConfiguracion);
                return;
            }

            gestorLogging.verbose(ORIGEN_LOG, I18nLogs.trf("[%s] Adquiriendo permiso del limitador (disponibles: %d)",
                    nombreHilo, limitador.permisosDisponibles()));
            limitador.adquirir();
            permisoAdquirido = true;
            gestorLogging.verbose(ORIGEN_LOG, I18nLogs.trf("[%s] Permiso de limitador adquirido", nombreHilo));

            int retrasoSegundos = config.obtenerRetrasoSegundos();
            gestorLogging.verbose(ORIGEN_LOG, I18nLogs.trf("[%s] Durmiendo por %d segundos antes de llamar a la API", nombreHilo, retrasoSegundos));
            controlCancelacionPausa.esperarConControl(retrasoSegundos * 1000L);

            gestorLogging.info(ORIGEN_LOG, I18nLogs.trf("Analizando: %s", solicitud.obtenerUrl()));

            boolean multiHabilitado = config.esMultiProveedorHabilitado();
            java.util.List<String> proveedoresConfig = config.obtenerProveedoresMultiConsulta();
            gestorLogging.verbose(ORIGEN_LOG, I18nLogs.trf("DIAGNOSTICO: multiHabilitado=%s, proveedoresConfig=%s",
                    multiHabilitado,
                    proveedoresConfig != null ? proveedoresConfig.size() + " elementos" : "null"));

            ResultadoAnalisisMultiple resultadoMultiple;
            if (multiHabilitado && proveedoresConfig != null && proveedoresConfig.size() > 1) {
                gestorLogging.verbose(ORIGEN_LOG, I18nLogs.trf("DIAGNOSTICO: Ejecutando multi-proveedor con %d proveedores", proveedoresConfig.size()));
                resultadoMultiple = gestorMulti.ejecutarAnalisisMultiProveedor();
            } else {
                if (multiHabilitado) {
                    gestorLogging.info(ORIGEN_LOG, I18nLogs.trf("PROVEEDOR: Multi-proveedor habilitado pero solo %d proveedor(es) configurado(s). Usando proveedor único: %s",
                            proveedoresConfig != null ? proveedoresConfig.size() : 0,
                            config.obtenerProveedorAI()));
                } else {
                    gestorLogging.info(ORIGEN_LOG, I18nLogs.trf("PROVEEDOR: Usando proveedor único: %s", config.obtenerProveedorAI()));
                }
                resultadoMultiple = orquestador.ejecutarAnalisisCompleto();
            }

            long duracion = System.currentTimeMillis() - tiempoInicio;
            gestorLogging.info(ORIGEN_LOG, I18nLogs.trf("Analisis completado: %s (tomo %dms)", solicitud.obtenerUrl(), duracion));
            gestorLogging.verbose(ORIGEN_LOG, I18nLogs.trf("[%s] Severidad maxima: %s", nombreHilo, resultadoMultiple.obtenerSeveridadMaxima()));

            callback.alCompletarAnalisis(resultadoMultiple);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long duracion = System.currentTimeMillis() - tiempoInicio;
            String causa = Normalizador.noEsVacio(e.getMessage())
                ? e.getMessage()
                : I18nUI.General.ERROR_INESPERADO_TIPO(e.getClass().getSimpleName());

            if (controlCancelacionPausa.esPausada()) {
                gestorLogging.info(ORIGEN_LOG, I18nLogs.trf("[%s] Analisis pausado y liberando hilo (%dms)", nombreHilo, duracion));
                return;
            }

            if (controlCancelacionPausa.esCancelada()) {
                gestorLogging.info(ORIGEN_LOG, I18nLogs.trf("[%s] Analisis cancelado por usuario (%dms)", nombreHilo, duracion));
                callback.alCanceladoAnalisis();
            } else {
                gestorLogging.error(ORIGEN_LOG, I18nLogs.trf("[%s] Analisis interrumpido despues de %dms: %s", nombreHilo, duracion, causa));
                callback.alErrorAnalisis(mensajeAnalisisInterrumpido(causa));
            }
        } catch (Exception e) {
            long duracion = System.currentTimeMillis() - tiempoInicio;
            String falloMsg = Normalizador.noEsVacio(e.getMessage())
                ? e.getMessage()
                : I18nUI.Tareas.MSG_ERROR_DESCONOCIDO();

            gestorLogging.error(ORIGEN_LOG, I18nLogs.trf("[%s] Analisis fallido despues de %dms: %s", nombreHilo, duracion, falloMsg));
            callback.alErrorAnalisis(falloMsg);
        } finally {
            if (permisoAdquirido) {
                limitador.liberar();
                gestorLogging.verbose(ORIGEN_LOG, I18nLogs.trf("[%s] Permiso de limitador liberado (disponibles: %d)",
                        nombreHilo, limitador.permisosDisponibles()));
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
        // config nunca es null: el constructor normaliza a new ConfiguracionAPI()
        String error = config.validarParaConsultaModelo();
        return error != null ? error.trim() : "";
    }

    private static String mensajeErrorSolicitudNoDisponible() {
        return I18nUI.tr("Solicitud de analisis no disponible", "Analysis request is not available");
    }

    private static String mensajeAnalisisInterrumpido(String causa) {
        return I18nUI.trf("Analisis interrumpido: %s", "Analysis interrupted: %s", causa);
    }

    /**
     * Parsea una respuesta JSON usando el parseador interno.
     * Exist for test access only — production code should use the full pipeline.
     */
    ResultadoAnalisisMultiple parsearRespuesta(String respuestaJson) {
        return parseador.parsearRespuesta(respuestaJson, solicitud, config != null ? config.obtenerProveedorAI() : "");
    }
}
