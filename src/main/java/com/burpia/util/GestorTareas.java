package com.burpia.util;
import com.burpia.model.Tarea;
import com.burpia.ui.ModeloTablaTareas;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class GestorTareas {
    private final Map<String, Tarea> tareas;
    private final ReentrantLock candado;
    private final ScheduledExecutorService monitorVerificacion;
    private final ModeloTablaTareas modeloTabla;
    private final Consumer<String> logger;
    private volatile Consumer<String> manejadorCancelacion;
    private volatile Consumer<String> manejadorPausa;
    private volatile Consumer<String> manejadorReanudar;
    private final int maxTareasFinalizadasRetenidas;

    private static final long INTERVALO_VERIFICACION_MS = 30000;
    private static final long TAREA_ATASCADA_MS = 300000;
    private static final int MAX_TAREAS_FINALIZADAS_RETENIDAS_POR_DEFECTO = 2000;

    public GestorTareas(ModeloTablaTareas modeloTabla, Consumer<String> logger) {
        this(modeloTabla, logger, MAX_TAREAS_FINALIZADAS_RETENIDAS_POR_DEFECTO);
    }

    public GestorTareas(ModeloTablaTareas modeloTabla, Consumer<String> logger, int maxTareasFinalizadasRetenidas) {
        this.tareas = new ConcurrentHashMap<>();
        this.candado = new ReentrantLock();
        this.modeloTabla = modeloTabla;
        this.logger = logger != null ? logger : mensaje -> { };
        this.manejadorCancelacion = null;
        this.manejadorPausa = null;
        this.manejadorReanudar = null;
        this.maxTareasFinalizadasRetenidas = Math.max(1, maxTareasFinalizadasRetenidas);

        this.monitorVerificacion = crearMonitorVerificacion();
        this.monitorVerificacion.scheduleAtFixedRate(
            this::verificarTareasAtascadas,
            INTERVALO_VERIFICACION_MS,
            INTERVALO_VERIFICACION_MS,
            TimeUnit.MILLISECONDS
        );
    }

    public Tarea crearTarea(String tipo, String url, String estado, String mensajeInfo) {
        String id = UUID.randomUUID().toString();
        Tarea tarea = new Tarea(id, tipo, url, estado);
        tarea.establecerMensajeInfo(mensajeInfo);

        candado.lock();
        try {
            tareas.put(id, tarea);
            List<String> idsPurgadas = modeloTabla.agregarTareaYObtenerIdsPurgadas(tarea);
            sincronizarTareasConPurgadoModelo(idsPurgadas, id);
            registrar("Tarea creada: " + tipo + " - " + url);
        } finally {
            candado.unlock();
        }

        return tarea;
    }

    public void actualizarTarea(String id, String nuevoEstado, String mensajeInfo) {
        candado.lock();
        try {
            Tarea tarea = tareas.get(id);
            if (tarea != null) {
                tarea.establecerEstado(nuevoEstado);
                if (Normalizador.noEsVacio(mensajeInfo)) {
                    tarea.establecerMensajeInfo(mensajeInfo);
                }
                actualizarFilaTabla(tarea);
            }
        } finally {
            candado.unlock();
        }
    }

    public void cancelarTodas() {
        List<String> idsCanceladas = actualizarEstadosMasivoConIds(Tarea::esActiva, Tarea.ESTADO_CANCELADO);
        registrar("Tareas canceladas: " + idsCanceladas.size());
        notificarCancelaciones(idsCanceladas);
    }

    public void pausarReanudarTodas() {
        if (obtenerNumeroTareasPausadas() > 0) {
            reanudarTodasPausadas();
        } else {
            pausarTodasActivas();
        }
    }

    public void pausarTodasActivas() {
        int pausadas = actualizarEstadosMasivo(
            tarea -> Tarea.esEstadoPausable(tarea.obtenerEstado()),
            Tarea.ESTADO_PAUSADO
        );
        registrar("Tareas pausadas: " + pausadas);
    }

    public void reanudarTodasPausadas() {
        int reanudadas = actualizarEstadosMasivo(
            tarea -> Tarea.esEstadoReanudable(tarea.obtenerEstado()),
            Tarea.ESTADO_EN_COLA
        );
        registrar("Tareas reanudadas: " + reanudadas);
    }

    public int obtenerNumeroTareasPausadas() {
        candado.lock();
        try {
            int pausadas = 0;
            for (Tarea tarea : tareas.values()) {
                if (Tarea.ESTADO_PAUSADO.equals(tarea.obtenerEstado())) {
                    pausadas++;
                }
            }
            return pausadas;
        } finally {
            candado.unlock();
        }
    }

    public void limpiarCompletadas() {
        candado.lock();
        try {
            List<String> idsAEliminar = new ArrayList<>();

            for (Map.Entry<String, Tarea> entry : tareas.entrySet()) {
                String estado = entry.getValue().obtenerEstado();
                if (Tarea.esEstadoEliminable(estado)) {
                    idsAEliminar.add(entry.getKey());
                }
            }

            for (String id : idsAEliminar) {
                tareas.remove(id);
            }
            modeloTabla.eliminarPorEstado(
                Tarea.ESTADO_COMPLETADO,
                Tarea.ESTADO_ERROR,
                Tarea.ESTADO_CANCELADO
            );

            registrar("Tareas limpiadas: " + idsAEliminar.size());
        } finally {
            candado.unlock();
        }
    }

    private void verificarTareasAtascadas() {
        List<Tarea> tareasAtascadas = new ArrayList<>();
        List<String> idsAInterrumpir = new ArrayList<>();

        candado.lock();
        try {
            for (Tarea tarea : tareas.values()) {
                String estado = tarea.obtenerEstado();
                if (Tarea.ESTADO_ANALIZANDO.equals(estado)) {
                    long duracion = tarea.obtenerDuracionMilisegundos();
                    if (duracion > TAREA_ATASCADA_MS) {
                        tareasAtascadas.add(tarea);
                    }
                }
            }

            for (Tarea tarea : tareasAtascadas) {
                tarea.establecerEstado(Tarea.ESTADO_ERROR);
                tarea.establecerMensajeInfo(
                    com.burpia.i18n.I18nUI.Tareas.ESTADO_TAREA_ATASCADA()
                );
                String id = tarea.obtenerId();
                if (Normalizador.noEsVacio(id)) {
                    idsAInterrumpir.add(id);
                }
            }
        } finally {
            candado.unlock();
        }

        for (Tarea tarea : tareasAtascadas) {
            actualizarFilaTabla(tarea);
            registrar(com.burpia.i18n.I18nUI.Tareas.LOG_TAREA_ATASCADA_DETECTADA() + tarea.obtenerId());
        }
        notificarCancelaciones(idsAInterrumpir);
        

        aplicarRetencionFinalizadas();
    }

    private void actualizarFilaTabla(Tarea tarea) {
        modeloTabla.actualizarTarea(tarea);
    }

    public Tarea obtenerTarea(String id) {
        candado.lock();
        try {
            return tareas.get(id);
        } finally {
            candado.unlock();
        }
    }

    public boolean pausarTarea(String id) {
        boolean pausada = false;
        String url = null;
        Consumer<String> manejador = null;
        candado.lock();
        try {
            Tarea tarea = tareas.get(id);
            if (tarea != null && Tarea.esEstadoPausable(tarea.obtenerEstado())) {
                tarea.establecerEstado(Tarea.ESTADO_PAUSADO);
                actualizarFilaTabla(tarea);
                url = tarea.obtenerUrl();
                pausada = true;
                manejador = this.manejadorPausa;
            }
        } finally {
            candado.unlock();
        }
        if (pausada) {
            registrar("Tarea pausada: " + url);
            notificarManejador(manejador, id, "pausa");
        }
        return pausada;
    }

    public boolean reanudarTarea(String id) {
        boolean reanudada = false;
        String url = null;
        Consumer<String> manejador = null;
        candado.lock();
        try {
            Tarea tarea = tareas.get(id);
            if (tarea != null && esEstadoReencolable(tarea.obtenerEstado())) {
                tarea.establecerEstado(Tarea.ESTADO_EN_COLA);
                actualizarFilaTabla(tarea);
                url = tarea.obtenerUrl();
                reanudada = true;
                manejador = this.manejadorReanudar;
            }
        } finally {
            candado.unlock();
        }
        if (reanudada) {
            registrar("Tarea reanudada: " + url);
            notificarManejador(manejador, id, "reanudar");
        }
        return reanudada;
    }

    public boolean marcarTareaAnalizando(String id, String mensajeInfo) {
        candado.lock();
        try {
            Tarea tarea = tareas.get(id);
            if (tarea == null) {
                return false;
            }
            String estadoActual = tarea.obtenerEstado();
            if (Tarea.ESTADO_CANCELADO.equals(estadoActual)) {
                return false;
            }
            if (Tarea.ESTADO_ANALIZANDO.equals(estadoActual)) {
                return true;
            }
            if (!Tarea.ESTADO_EN_COLA.equals(estadoActual)) {
                return false;
            }
            tarea.establecerEstado(Tarea.ESTADO_ANALIZANDO);
            if (Normalizador.noEsVacio(mensajeInfo)) {
                tarea.establecerMensajeInfo(mensajeInfo);
            }
            actualizarFilaTabla(tarea);
            return true;
        } finally {
            candado.unlock();
        }
    }

    public boolean cancelarTarea(String id) {
        boolean cancelada = false;
        String url = null;
        candado.lock();
        try {
            Tarea tarea = tareas.get(id);
            if (tarea != null && Tarea.esEstadoCancelable(tarea.obtenerEstado())) {
                tarea.establecerEstado(Tarea.ESTADO_CANCELADO);
                actualizarFilaTabla(tarea);
                url = tarea.obtenerUrl();
                cancelada = true;
            }
        } finally {
            candado.unlock();
        }
        if (cancelada) {
            registrar("Tarea cancelada: " + url);
            notificarCancelacion(id);
        }
        return cancelada;
    }

    public boolean limpiarTarea(String id) {
        candado.lock();
        try {
            Tarea tarea = tareas.remove(id);
            if (tarea == null) {
                return false;
            }
        } finally {
            candado.unlock();
        }

        modeloTabla.eliminarTareaPorId(id);
        registrar("Tarea limpiada: " + id);
        return true;
    }

    public void detener() {
        if (monitorVerificacion != null && !monitorVerificacion.isShutdown()) {
            monitorVerificacion.shutdownNow();
        }
        candado.lock();
        try {
            tareas.clear();
            modeloTabla.limpiar();
        } finally {
            candado.unlock();
        }
    }

    public void shutdown() {
        detener();
    }

    public boolean estaTareaCancelada(String id) {
        candado.lock();
        try {
            Tarea tarea = tareas.get(id);
            if (tarea != null) {
                String estado = tarea.obtenerEstado();
                return Tarea.ESTADO_CANCELADO.equals(estado);
            }
            return false;
        } finally {
            candado.unlock();
        }
    }

    public boolean estaTareaPausada(String id) {
        candado.lock();
        try {
            Tarea tarea = tareas.get(id);
            if (tarea != null) {
                String estado = tarea.obtenerEstado();
                return Tarea.ESTADO_PAUSADO.equals(estado);
            }
            return false;
        } finally {
            candado.unlock();
        }
    }

    public void establecerManejadorCancelacion(Consumer<String> manejadorCancelacion) {
        this.manejadorCancelacion = manejadorCancelacion;
    }

    public void establecerManejadorPausa(Consumer<String> manejadorPausa) {
        this.manejadorPausa = manejadorPausa;
    }

    public void establecerManejadorReanudar(Consumer<String> manejadorReanudar) {
        this.manejadorReanudar = manejadorReanudar;
    }

    private int actualizarEstadosMasivo(java.util.function.Predicate<Tarea> filtro, String nuevoEstado) {
        return actualizarEstadosMasivoConIds(filtro, nuevoEstado).size();
    }

    private List<String> actualizarEstadosMasivoConIds(java.util.function.Predicate<Tarea> filtro, String nuevoEstado) {
        List<Tarea> tareasAActualizar = new ArrayList<>();
        List<String> idsActualizadas = new ArrayList<>();

        // FASE 1: Recoger datos con candado (operación rápida)
        candado.lock();
        try {
            for (Tarea tarea : tareas.values()) {
                if (filtro.test(tarea)) {
                    tarea.establecerEstado(nuevoEstado);
                    idsActualizadas.add(tarea.obtenerId());
                    tareasAActualizar.add(tarea);
                }
            }
        } finally {
            candado.unlock();
        }

        // FASE 2: Actualizar UI sin candado (operación lenta pero no bloquea)
        for (Tarea tarea : tareasAActualizar) {
            actualizarFilaTabla(tarea);
        }

        return idsActualizadas;
    }

    private void notificarCancelaciones(List<String> idsCanceladas) {
        if (Normalizador.esVacia(idsCanceladas)) {
            return;
        }
        for (String id : idsCanceladas) {
            if (!Normalizador.esVacio(id)) {
                notificarCancelacion(id);
            }
        }
    }

    private void notificarCancelacion(String id) {
        notificarManejador(this.manejadorCancelacion, id, "cancelacion");
    }

    private void aplicarRetencionFinalizadas() {
        List<String> idsAEliminar = new ArrayList<>();
        candado.lock();
        try {
            List<Tarea> finalizadas = new ArrayList<>();
            for (Tarea tarea : tareas.values()) {
                if (tarea != null && tarea.esFinalizada()) {
                    finalizadas.add(tarea);
                }
            }
            int excedente = finalizadas.size() - maxTareasFinalizadasRetenidas;
            if (excedente <= 0) {
                return;
            }
            finalizadas.sort(Comparator.comparingLong(this::obtenerTimestampFinalizacion));
            for (int i = 0; i < excedente; i++) {
                Tarea tarea = finalizadas.get(i);
                if (tarea != null) {
                    String id = tarea.obtenerId();
                    if (id != null && tareas.remove(id) != null) {
                        idsAEliminar.add(id);
                    }
                }
            }
        } finally {
            candado.unlock();
        }

        for (String id : idsAEliminar) {
            modeloTabla.eliminarTareaPorId(id);
        }
        if (!idsAEliminar.isEmpty()) {
            registrar("Retencion aplicada en tareas finalizadas: " + idsAEliminar.size() + " eliminadas");
        }
    }

    private long obtenerTimestampFinalizacion(Tarea tarea) {
        if (tarea == null) {
            return 0L;
        }
        long fin = tarea.obtenerTiempoFin();
        if (fin > 0) {
            return fin;
        }
        long inicio = tarea.obtenerTiempoInicio();
        return inicio > 0 ? inicio : 0L;
    }

    private void registrar(String mensaje) {
        try {
            logger.accept(mensaje);
        } catch (Exception ignored) {
            // Silenciar errores de logging para evitar cascadas
        }
    }

    private ScheduledExecutorService crearMonitorVerificacion() {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread hilo = new Thread(runnable);
            hilo.setName("BurpIA-GestorTareas");
            hilo.setDaemon(true);
            return hilo;
        });
    }

    private void sincronizarTareasConPurgadoModelo(List<String> idsPurgadas, String idActual) {
        if (Normalizador.esVacia(idsPurgadas)) {
            return;
        }
        for (String idPurgado : idsPurgadas) {
            if (Normalizador.esVacio(idPurgado) || idPurgado.equals(idActual)) {
                continue;
            }
            tareas.remove(idPurgado);
        }
    }

    private boolean esEstadoReencolable(String estado) {
        return Tarea.esEstadoReanudable(estado) || Tarea.esEstadoReintentable(estado);
    }

    private void notificarManejador(Consumer<String> manejador, String id, String tipoOperacion) {
        if (manejador == null || Normalizador.esVacio(id)) {
            return;
        }
        try {
            manejador.accept(id);
        } catch (Exception e) {
            registrar("Error en manejador de " + tipoOperacion + ": " + e.getMessage());
        }
    }
}
