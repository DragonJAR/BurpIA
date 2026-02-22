package com.burpia.util;

import com.burpia.model.Tarea;
import com.burpia.ui.ModeloTablaTareas;

import javax.swing.*;
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
            modeloTabla.agregarTarea(tarea);
            registrar("Tarea creada: " + tipo + " - " + url);
        } finally {
            candado.unlock();
        }

        aplicarRetencionFinalizadas();
        return tarea;
    }

    public void actualizarTarea(String id, String nuevoEstado, String mensajeInfo) {
        candado.lock();
        try {
            Tarea tarea = tareas.get(id);
            if (tarea != null) {
                tarea.establecerEstado(nuevoEstado);
                if (mensajeInfo != null && !mensajeInfo.isEmpty()) {
                    tarea.establecerMensajeInfo(mensajeInfo);
                }
                actualizarFilaTabla(tarea);
            }
        } finally {
            candado.unlock();
        }
        if (Tarea.ESTADO_COMPLETADO.equals(nuevoEstado) ||
            Tarea.ESTADO_ERROR.equals(nuevoEstado) ||
            Tarea.ESTADO_CANCELADO.equals(nuevoEstado)) {
            aplicarRetencionFinalizadas();
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
            tarea -> Tarea.ESTADO_EN_COLA.equals(tarea.obtenerEstado()) ||
                Tarea.ESTADO_ANALIZANDO.equals(tarea.obtenerEstado()),
            Tarea.ESTADO_PAUSADO
        );
        registrar("Tareas pausadas: " + pausadas);
    }

    public void reanudarTodasPausadas() {
        int reanudadas = actualizarEstadosMasivo(
            tarea -> Tarea.ESTADO_PAUSADO.equals(tarea.obtenerEstado()),
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
                if (estado.equals(Tarea.ESTADO_COMPLETADO) ||
                    estado.equals(Tarea.ESTADO_ERROR) ||
                    estado.equals(Tarea.ESTADO_CANCELADO)) {
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

        candado.lock();
        try {
            long ahora = System.currentTimeMillis();

            for (Tarea tarea : tareas.values()) {
                String estado = tarea.obtenerEstado();
                if (estado.equals(Tarea.ESTADO_ANALIZANDO)) {
                    long duracion = ahora - tarea.obtenerTiempoInicio();
                    if (duracion > TAREA_ATASCADA_MS) {
                        tareasAtascadas.add(tarea);
                    }
                }
            }

            for (Tarea tarea : tareasAtascadas) {
                tarea.establecerEstado(Tarea.ESTADO_ERROR);
                tarea.establecerMensajeInfo(
                    com.burpia.i18n.I18nUI.tr("Tarea atascada - timeout", "Stuck task - timeout")
                );
            }
        } finally {
            candado.unlock();
        }

        for (Tarea tarea : tareasAtascadas) {
            actualizarFilaTabla(tarea);
            registrar(com.burpia.i18n.I18nUI.tr(
                "Tarea atascada detectada: ",
                "Stuck task detected: "
            ) + tarea.obtenerId());
        }
    }

    private void actualizarFilaTabla(Tarea tarea) {
        modeloTabla.actualizarTarea(tarea);
    }

    public Collection<Tarea> obtenerTareas() {
        candado.lock();
        try {
            return new ArrayList<>(tareas.values());
        } finally {
            candado.unlock();
        }
    }

    public Tarea obtenerTarea(String id) {
        candado.lock();
        try {
            return tareas.get(id);
        } finally {
            candado.unlock();
        }
    }

    public int obtenerNumeroTareasActivas() {
        candado.lock();
        try {
            int count = 0;
            for (Tarea tarea : tareas.values()) {
                String estado = tarea.obtenerEstado();
                if (estado.equals(Tarea.ESTADO_EN_COLA) ||
                    estado.equals(Tarea.ESTADO_ANALIZANDO) ||
                    estado.equals(Tarea.ESTADO_PAUSADO)) {
                    count++;
                }
            }
            return count;
        } finally {
            candado.unlock();
        }
    }

    public void pausarTarea(String id) {
        candado.lock();
        try {
            Tarea tarea = tareas.get(id);
            if (tarea != null) {
                if (Tarea.ESTADO_EN_COLA.equals(tarea.obtenerEstado()) ||
                    Tarea.ESTADO_ANALIZANDO.equals(tarea.obtenerEstado())) {
                    tarea.establecerEstado(Tarea.ESTADO_PAUSADO);
                    actualizarFilaTabla(tarea);
                    registrar("Tarea pausada: " + tarea.obtenerUrl());
                }
            }
        } finally {
            candado.unlock();
        }
    }

    public void reanudarTarea(String id) {
        candado.lock();
        try {
            Tarea tarea = tareas.get(id);
            if (tarea != null) {
                if (Tarea.ESTADO_PAUSADO.equals(tarea.obtenerEstado()) ||
                    Tarea.ESTADO_ERROR.equals(tarea.obtenerEstado()) ||
                    Tarea.ESTADO_CANCELADO.equals(tarea.obtenerEstado())) {
                    tarea.establecerEstado(Tarea.ESTADO_EN_COLA);
                    actualizarFilaTabla(tarea);
                    registrar("Tarea reanudada: " + tarea.obtenerUrl());
                }
            }
        } finally {
            candado.unlock();
        }
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
            if (mensajeInfo != null && !mensajeInfo.isEmpty()) {
                tarea.establecerMensajeInfo(mensajeInfo);
            }
            actualizarFilaTabla(tarea);
            return true;
        } finally {
            candado.unlock();
        }
    }

    public void cancelarTarea(String id) {
        boolean cancelada = false;
        candado.lock();
        try {
            Tarea tarea = tareas.get(id);
            if (tarea != null) {
                if (Tarea.ESTADO_EN_COLA.equals(tarea.obtenerEstado()) ||
                    Tarea.ESTADO_ANALIZANDO.equals(tarea.obtenerEstado()) ||
                    Tarea.ESTADO_PAUSADO.equals(tarea.obtenerEstado())) {
                    tarea.establecerEstado(Tarea.ESTADO_CANCELADO);
                    actualizarFilaTabla(tarea);
                    registrar("Tarea cancelada: " + tarea.obtenerUrl());
                    cancelada = true;
                }
            }
        } finally {
            candado.unlock();
        }
        if (cancelada) {
            notificarCancelacion(id);
        }
    }

    public void limpiarTarea(String id) {
        candado.lock();
        try {
            Tarea tarea = tareas.remove(id);
            if (tarea == null) {
                return;
            }
        } finally {
            candado.unlock();
        }

        modeloTabla.eliminarTareaPorId(id);

        registrar("Tarea limpiada: " + id);
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
                return estado.equals(Tarea.ESTADO_CANCELADO);
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
                return estado.equals(Tarea.ESTADO_PAUSADO);
            }
            return false;
        } finally {
            candado.unlock();
        }
    }

    public void establecerManejadorCancelacion(Consumer<String> manejadorCancelacion) {
        this.manejadorCancelacion = manejadorCancelacion;
    }

    private int actualizarEstadosMasivo(java.util.function.Predicate<Tarea> filtro, String nuevoEstado) {
        return actualizarEstadosMasivoConIds(filtro, nuevoEstado).size();
    }

    private List<String> actualizarEstadosMasivoConIds(java.util.function.Predicate<Tarea> filtro, String nuevoEstado) {
        List<String> idsActualizadas = new ArrayList<>();
        candado.lock();
        try {
            for (Tarea tarea : tareas.values()) {
                if (filtro.test(tarea)) {
                    tarea.establecerEstado(nuevoEstado);
                    actualizarFilaTabla(tarea);
                    idsActualizadas.add(tarea.obtenerId());
                }
            }
            return idsActualizadas;
        } finally {
            candado.unlock();
        }
    }

    private void notificarCancelaciones(List<String> idsCanceladas) {
        if (idsCanceladas == null || idsCanceladas.isEmpty()) {
            return;
        }
        for (String id : idsCanceladas) {
            notificarCancelacion(id);
        }
    }

    private void notificarCancelacion(String id) {
        Consumer<String> manejador = this.manejadorCancelacion;
        if (manejador == null || id == null || id.isEmpty()) {
            return;
        }
        try {
            manejador.accept(id);
        } catch (Exception e) {
            registrar("Error en manejador de cancelacion: " + e.getMessage());
        }
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
            return Long.MAX_VALUE;
        }
        long fin = tarea.obtenerTiempoFin();
        if (fin > 0) {
            return fin;
        }
        return tarea.obtenerTiempoInicio();
    }

    private void registrar(String mensaje) {
        try {
            logger.accept(mensaje);
        } catch (Exception ignored) {
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
}
