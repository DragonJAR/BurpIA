package com.burpia.execution;

import burp.api.montoya.http.message.HttpRequestResponse;
import com.burpia.analyzer.AnalizadorAI;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.model.Tarea;
import com.burpia.ui.PestaniaPrincipal;
import com.burpia.util.AlmacenEvidenciaHttp;
import com.burpia.util.ControlBackpressureGlobal;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.GestorTareas;
import com.burpia.util.HttpUtils;
import com.burpia.util.LimitadorTasa;
import com.burpia.util.Normalizador;
import com.burpia.util.PoliticaMemoria;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.burpia.ui.UIUtils.ejecutarEnEdt;

public class TaskExecutionManager {
    private static final String ORIGEN_LOG = "TaskExecutionManager";
    private static final long TTL_CONTEXTO_COMPLETADO_MS = 0L;
    private static final long TTL_CONTEXTO_REINTENTABLE_MS = 15 * 60 * 1000L;
    private static final long TTL_CONTEXTO_ACTIVO_MS = Long.MAX_VALUE;
    private static final int MAX_CONTEXTO_REINTENTO = 1000;

    private final ConfiguracionAPI config;
    private final GestorTareas gestorTareas;
    private final GestorConsolaGUI gestorConsola;
    private final PestaniaPrincipal pestaniaPrincipal;
    private final PrintWriter stdout;
    private final PrintWriter stderr;
    private final LimitadorTasa limitador;
    private final ControlBackpressureGlobal controlBackpressure;
    private final AlmacenEvidenciaHttp almacenEvidencia;
    private final GestorLoggingUnificado gestorLogging;

    private final ThreadPoolExecutor executorService;
    private final Map<String, ContextoReintento> contextosReintento;
    private final Map<String, Future<?>> ejecucionesActivas;
    private final Map<String, AnalizadorAI> analizadoresActivos;
    private final Object poolLock = new Object();

    private static final class ContextoReintento {
        private final SolicitudAnalisis solicitudAnalisis;
        private final String evidenciaId;
        private final long creadoMs;

        private ContextoReintento(SolicitudAnalisis solicitudAnalisis, String evidenciaId) {
            this.solicitudAnalisis = solicitudAnalisis;
            this.evidenciaId = evidenciaId;
            this.creadoMs = System.currentTimeMillis();
        }
    }

    public TaskExecutionManager(ConfiguracionAPI config, GestorTareas gestorTareas,
            GestorConsolaGUI gestorConsola, PestaniaPrincipal pestaniaPrincipal,
            PrintWriter stdout, PrintWriter stderr, LimitadorTasa limitador,
            ControlBackpressureGlobal controlBackpressure) {
        this.config = config != null ? config : new ConfiguracionAPI();
        this.gestorTareas = gestorTareas;
        this.gestorConsola = gestorConsola;
        this.pestaniaPrincipal = pestaniaPrincipal;
        this.stdout = stdout != null ? stdout : new PrintWriter(System.out, true);
        this.stderr = stderr != null ? stderr : new PrintWriter(System.err, true);
        this.limitador = limitador != null ? limitador : new LimitadorTasa(10);
        this.controlBackpressure = controlBackpressure;
        this.almacenEvidencia = new AlmacenEvidenciaHttp();
        this.gestorLogging = GestorLoggingUnificado.crear(gestorConsola, stdout, stderr, null, null);
        this.contextosReintento = new ConcurrentHashMap<>();
        this.ejecucionesActivas = new ConcurrentHashMap<>();
        this.analizadoresActivos = new ConcurrentHashMap<>();

        int maxThreads = this.config.obtenerMaximoConcurrente() > 0 ? this.config.obtenerMaximoConcurrente() : 10;
        int capacidadCola = Math.max(50, maxThreads * 20);
        this.executorService = new ThreadPoolExecutor(
                maxThreads,
                maxThreads,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(capacidadCola),
                r -> {
                    Thread thread = new Thread(r);
                    thread.setDaemon(true);
                    thread.setName("BurpIA-Task-" + UUID.randomUUID().toString().substring(0, 8));
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy());

        gestorLogging.info(ORIGEN_LOG, "TaskExecutionManager inicializado con " + maxThreads + " hilos");
    }

    public String programarAnalisis(SolicitudAnalisis solicitudAnalisis, HttpRequestResponse evidenciaHttp, String tipoTarea) {
        if (solicitudAnalisis == null) {
            gestorLogging.error(ORIGEN_LOG, "No se pudo programar analisis: solicitud null");
            return null;
        }

        depurarContextosHuerfanos();
        final String url = solicitudAnalisis.obtenerUrl();
        final AtomicReference<String> tareaIdRef = new AtomicReference<>();

        if (gestorTareas != null) {
            final String evidenciaId = almacenarEvidenciaSiDisponible(evidenciaHttp);
            Tarea tarea = gestorTareas.crearTarea(
                    tipoTarea,
                    url,
                    Tarea.ESTADO_EN_COLA,
                    "Esperando analisis");
            tareaIdRef.set(tarea.obtenerId());
            contextosReintento.put(
                    tarea.obtenerId(),
                    new ContextoReintento(solicitudAnalisis, evidenciaId));
            ejecutarAnalisisExistente(tarea.obtenerId(), solicitudAnalisis, evidenciaId);
        }
        return tareaIdRef.get();
    }

    public boolean reencolarTarea(String tareaId) {
        if (Normalizador.esVacio(tareaId)) {
            return false;
        }
        depurarContextosHuerfanos();
        ContextoReintento contexto = contextosReintento.get(tareaId);
        if (contexto == null) {
            gestorLogging.error(ORIGEN_LOG, "No existe contexto para reintentar tarea: " + tareaId);
            return false;
        }

        if (gestorTareas != null) {
            gestorTareas.actualizarTarea(tareaId, Tarea.ESTADO_EN_COLA, "Reintentando...");
        }

        ejecutarAnalisisExistente(
                tareaId,
                contexto.solicitudAnalisis,
                contexto.evidenciaId);

        gestorLogging.info(ORIGEN_LOG, "Tarea reencolada: " + tareaId);
        return true;
    }

    public void cancelarEjecucionActiva(String tareaId) {
        if (Normalizador.esVacio(tareaId)) {
            return;
        }

        AnalizadorAI analizador = analizadoresActivos.remove(tareaId);
        if (analizador != null) {
            analizador.cancelarLlamadaHttpActiva();
            gestorLogging.verbose(ORIGEN_LOG, "Llamada HTTP cancelada para tarea: " + tareaId);
        }

        Future<?> future = ejecucionesActivas.remove(tareaId);
        if (future != null) {
            boolean cancelada = future.cancel(true);
            if (cancelada) {
                gestorLogging.verbose(ORIGEN_LOG, "Cancelación activa aplicada para tarea: " + tareaId);
            }
        }
    }

    private void ejecutarAnalisisExistente(String tareaId, SolicitudAnalisis solicitudAnalisis, String evidenciaId) {
        if (Normalizador.esVacio(tareaId) || solicitudAnalisis == null) {
            return;
        }

        final String url = solicitudAnalisis.obtenerUrl();
        final String tareaIdFinal = tareaId;

        ejecutarEnEdt(() -> {
            if (pestaniaPrincipal != null) {
                pestaniaPrincipal.registrar("Iniciando análisis (continuar/reintentar) para: " + url);
            }
        });

        final AtomicReference<String> tareaIdRef = new AtomicReference<>(tareaIdFinal);

        AnalizadorAI analizador = new AnalizadorAI(
                solicitudAnalisis,
                config.crearSnapshot(),
                stdout,
                stderr,
                limitador,
                new ManejadorResultadoAI(tareaIdRef, url, evidenciaId),
                () -> {
                    if (gestorTareas != null && Normalizador.noEsVacio(tareaIdFinal)) {
                        boolean marcada = gestorTareas.marcarTareaAnalizando(tareaIdFinal, "Analizando");
                        if (!marcada) {
                            gestorLogging.verbose(ORIGEN_LOG, "No se pudo marcar tarea como analizando: " + tareaIdFinal);
                        }
                    }
                },
                gestorConsola,
                () -> gestorTareas != null && Normalizador.noEsVacio(tareaIdFinal)
                        && gestorTareas.estaTareaCancelada(tareaIdFinal),
                () -> gestorTareas != null && Normalizador.noEsVacio(tareaIdFinal)
                        && gestorTareas.estaTareaPausada(tareaIdFinal),
                controlBackpressure);

        String id = tareaIdFinal;

        try {
            analizadoresActivos.put(id, analizador);

            Future<?> future = executorService.submit(analizador);
            ejecucionesActivas.put(id, future);
            gestorLogging.info(ORIGEN_LOG, "Hilo de análisis iniciado para: " + url + " (ID: " + id + ")");
            rastrearEstadoCola();
        } catch (RejectedExecutionException ex) {
            analizadoresActivos.remove(id);
            finalizarEjecucionActiva(id);
            contextosReintento.remove(id);
            eliminarEvidenciaSiDisponible(evidenciaId);
            if (gestorTareas != null) {
                gestorTareas.actualizarTarea(id, Tarea.ESTADO_ERROR, "Descartada por saturación de cola");
            }
            gestorLogging.error(ORIGEN_LOG, "Cola de análisis saturada, solicitud descartada: " + url);
        } catch (Exception ex) {
            analizadoresActivos.remove(id);
            if (gestorTareas != null) {
                gestorTareas.actualizarTarea(id, Tarea.ESTADO_ERROR, "Error al iniciar análisis: " + ex.getMessage());
            }
            finalizarEjecucionActiva(id);
            contextosReintento.remove(id);
            eliminarEvidenciaSiDisponible(evidenciaId);
            gestorLogging.error(ORIGEN_LOG, "Error al iniciar análisis para " + url + ": " + ex.getMessage());
        }
    }

    private void finalizarEjecucionActiva(String tareaId) {
        if (Normalizador.esVacio(tareaId)) {
            return;
        }
        ejecucionesActivas.remove(tareaId);
        analizadoresActivos.remove(tareaId);
    }

    private void depurarContextosHuerfanos() {
        if (gestorTareas == null || contextosReintento.isEmpty()) {
            return;
        }
        long ahora = System.currentTimeMillis();
        Iterator<Map.Entry<String, ContextoReintento>> it = contextosReintento.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, ContextoReintento> entry = it.next();
            if (entry == null) {
                continue;
            }

            String tareaId = entry.getKey();
            ContextoReintento contexto = entry.getValue();

            if (Normalizador.esVacio(tareaId) || contexto == null) {
                it.remove();
                continue;
            }

            String estado = null;
            Tarea tarea = gestorTareas.obtenerTarea(tareaId);
            if (tarea != null) {
                estado = tarea.obtenerEstado();
            }

            long ttlAplicable = calcularTTLParaEstado(estado);

            boolean debePurgar = (estado == null) ||
                    (ttlAplicable == 0L) ||
                    (ttlAplicable != Long.MAX_VALUE &&
                            (ahora - contexto.creadoMs) > ttlAplicable);

            if (debePurgar) {
                eliminarEvidenciaSiDisponible(contexto.evidenciaId);
                it.remove();
            }
        }

        if (contextosReintento.size() <= MAX_CONTEXTO_REINTENTO) {
            return;
        }
        List<Map.Entry<String, ContextoReintento>> candidatos = new ArrayList<>(contextosReintento.entrySet());
        candidatos.sort((a, b) -> Long.compare(
                a != null && a.getValue() != null ? a.getValue().creadoMs : 0L,
                b != null && b.getValue() != null ? b.getValue().creadoMs : 0L));
        int excedente = candidatos.size() - MAX_CONTEXTO_REINTENTO;
        for (int i = 0; i < excedente; i++) {
            Map.Entry<String, ContextoReintento> entry = candidatos.get(i);
            if (entry == null) {
                continue;
            }
            ContextoReintento contexto = contextosReintento.remove(entry.getKey());
            if (contexto != null) {
                eliminarEvidenciaSiDisponible(contexto.evidenciaId);
            }
        }
    }

    private long calcularTTLParaEstado(String estado) {
        if (estado == null) {
            return TTL_CONTEXTO_COMPLETADO_MS;
        }

        if (Tarea.ESTADO_COMPLETADO.equals(estado)) {
            return TTL_CONTEXTO_COMPLETADO_MS;
        }

        if (Tarea.esEstadoReintentable(estado)) {
            return TTL_CONTEXTO_REINTENTABLE_MS;
        }

        return TTL_CONTEXTO_ACTIVO_MS;
    }

    private void rastrearEstadoCola() {
        if (!config.esDetallado()) {
            return;
        }
        gestorLogging.verbose(ORIGEN_LOG,
                "Estado cola analisis: activos=" + executorService.getActiveCount() +
                        ", enCola=" + executorService.getQueue().size() +
                        ", completadas=" + executorService.getCompletedTaskCount());
    }

    private String almacenarEvidenciaSiDisponible(HttpRequestResponse evidenciaHttp) {
        if (evidenciaHttp == null) {
            return null;
        }
        try {
            return almacenEvidencia.guardar(evidenciaHttp);
        } catch (Exception e) {
            gestorLogging.error(ORIGEN_LOG, "No se pudo persistir evidencia HTTP: " + e.getMessage());
            return null;
        }
    }

    private void eliminarEvidenciaSiDisponible(String evidenciaId) {
        if (Normalizador.esVacio(evidenciaId)) {
            return;
        }
        try {
            almacenEvidencia.eliminar(evidenciaId);
        } catch (Exception e) {
            gestorLogging.error(ORIGEN_LOG, "No se pudo eliminar evidencia HTTP: " + e.getMessage());
        }
    }

    public void shutdown() {
        for (AnalizadorAI analizador : analizadoresActivos.values()) {
            if (analizador != null) {
                analizador.cancelarLlamadaHttpActiva();
            }
        }
        analizadoresActivos.clear();

        for (Map.Entry<String, Future<?>> entry : ejecucionesActivas.entrySet()) {
            if (entry != null && entry.getValue() != null) {
                entry.getValue().cancel(true);
            }
        }
        ejecucionesActivas.clear();
        contextosReintento.clear();
        almacenEvidencia.limpiarCacheMemoria();

        if (executorService != null && !executorService.isShutdown()) {
            gestorLogging.info(ORIGEN_LOG, "Deteniendo ExecutorService de TaskExecutionManager...");
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    gestorLogging.error(ORIGEN_LOG, "ExecutorService no terminó en 5 segundos, forzando shutdown...");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                gestorLogging.error(ORIGEN_LOG, "Error al esperar terminación de ExecutorService: " + e.getMessage());
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void actualizarConfiguracion(ConfiguracionAPI nuevaConfig) {
        if (nuevaConfig == null) {
            gestorLogging.error(ORIGEN_LOG, "No se pudo actualizar configuración: objeto nulo");
            return;
        }

        int nuevoMaximoConcurrente = nuevaConfig.obtenerMaximoConcurrente() > 0
                ? nuevaConfig.obtenerMaximoConcurrente()
                : 1;

        limitador.ajustarMaximoConcurrente(nuevoMaximoConcurrente);
        actualizarPoolEjecucion(nuevoMaximoConcurrente);

        gestorLogging.info(ORIGEN_LOG, "Configuración actualizada: maxConcurrente=" + nuevoMaximoConcurrente);
    }

    private void actualizarPoolEjecucion(int nuevoMaximoConcurrente) {
        synchronized (poolLock) {
            int maxActual = executorService.getMaximumPoolSize();
            if (nuevoMaximoConcurrente == maxActual) {
                return;
            }

            if (nuevoMaximoConcurrente > maxActual) {
                executorService.setMaximumPoolSize(nuevoMaximoConcurrente);
                executorService.setCorePoolSize(nuevoMaximoConcurrente);
            } else {
                executorService.setCorePoolSize(nuevoMaximoConcurrente);
                executorService.setMaximumPoolSize(nuevoMaximoConcurrente);
            }
        }
    }

    public int obtenerTareasActivas() {
        return executorService.getActiveCount();
    }

    public int obtenerTareasEnCola() {
        return executorService.getQueue().size();
    }

    public long obtenerTareasCompletadas() {
        return executorService.getCompletedTaskCount();
    }

    private class ManejadorResultadoAI implements AnalizadorAI.Callback {
        private final AtomicReference<String> tareaIdRef;
        private final String url;
        private final String evidenciaId;

        ManejadorResultadoAI(AtomicReference<String> tareaIdRef, String url, String evidenciaId) {
            this.tareaIdRef = tareaIdRef;
            this.url = url;
            this.evidenciaId = evidenciaId;
        }

        @Override
        public void alCompletarAnalisis(com.burpia.model.ResultadoAnalisisMultiple resultado) {
            final String id = tareaIdRef.get();

            try {
                if (gestorTareas != null && Normalizador.noEsVacio(id)) {
                    gestorTareas.actualizarTarea(id, Tarea.ESTADO_COMPLETADO,
                            "Completado: " + (resultado != null ? resultado.obtenerNumeroHallazgos() : 0)
                                    + " hallazgos");
                }

                gestorLogging.info(ORIGEN_LOG, "Análisis completado: " + url);

                ejecutarEnEdt(() -> {
                    if (pestaniaPrincipal != null) {
                        pestaniaPrincipal.actualizarEstadisticas();
                    }
                });
            } finally {
                limpiarRecursosTarea(id);
            }
        }

        @Override
        public void alErrorAnalisis(String error) {
            final String id = tareaIdRef.get();

            try {
                if (gestorTareas != null && Normalizador.noEsVacio(id)) {
                    gestorTareas.actualizarTarea(id, Tarea.ESTADO_ERROR,
                            "Error: " + (error != null ? error : "Error desconocido"));
                }
                gestorLogging.error(ORIGEN_LOG, "Análisis fallido para " + url + ": " + (error != null ? error : "Error desconocido"));

                ejecutarEnEdt(() -> {
                    if (pestaniaPrincipal != null) {
                        pestaniaPrincipal.actualizarEstadisticas();
                    }
                });
            } finally {
                limpiarRecursosTarea(id);
            }
        }

        @Override
        public void alCanceladoAnalisis() {
            final String id = tareaIdRef.get();

            try {
                if (gestorTareas != null && Normalizador.noEsVacio(id)) {
                    gestorTareas.actualizarTarea(id, Tarea.ESTADO_CANCELADO, "Cancelado por usuario");
                }
                gestorLogging.info(ORIGEN_LOG, "Análisis cancelado: " + url);
            } finally {
                limpiarRecursosTarea(id);
            }
        }

        private void limpiarRecursosTarea(String id) {
            finalizarEjecucionActiva(id);

            if (gestorTareas != null && Normalizador.noEsVacio(id)) {
                Tarea tarea = gestorTareas.obtenerTarea(id);
                if (tarea != null && !Tarea.ESTADO_ERROR.equals(tarea.obtenerEstado())
                        && !Tarea.ESTADO_CANCELADO.equals(tarea.obtenerEstado())) {
                    contextosReintento.remove(id);
                }
            } else {
                contextosReintento.remove(id);
            }
        }
    }
}