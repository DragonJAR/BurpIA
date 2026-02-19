package com.burpia.util;

import com.burpia.model.Tarea;
import com.burpia.ui.ModeloTablaTareas;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class GestorTareas {
    private final Map<String, Tarea> tareas;
    private final ReentrantLock candado;
    private final javax.swing.Timer temporizadorVerificacion;
    private final ModeloTablaTareas modeloTabla;
    private final Consumer<String> logger;

    private static final long INTERVALO_VERIFICACION_MS = 30000;
    private static final long TAREA_ATASCADA_MS = 300000;

    public GestorTareas(ModeloTablaTareas modeloTabla, Consumer<String> logger) {
        this.tareas = new ConcurrentHashMap<>();
        this.candado = new ReentrantLock();
        this.modeloTabla = modeloTabla;
        this.logger = logger;

        this.temporizadorVerificacion = new javax.swing.Timer(
            (int) INTERVALO_VERIFICACION_MS,
            e -> verificarTareasAtascadas()
        );
        this.temporizadorVerificacion.start();
    }

    public Tarea crearTarea(String tipo, String url, String estado, String mensajeInfo) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        Tarea tarea = new Tarea(id, tipo, url, estado);
        tarea.establecerMensajeInfo(mensajeInfo);

        candado.lock();
        try {
            tareas.put(id, tarea);
            modeloTabla.agregarTarea(tarea);
            logger.accept("Tarea creada: " + tipo + " - " + url);
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
                if (mensajeInfo != null && !mensajeInfo.isEmpty()) {
                    tarea.establecerMensajeInfo(mensajeInfo);
                }
                actualizarFilaTabla(tarea);
            }
        } finally {
            candado.unlock();
        }
    }

    public void cancelarTodas() {
        candado.lock();
        try {
            int canceladas = 0;
            for (Tarea tarea : tareas.values()) {
                String estado = tarea.obtenerEstado();
                if (estado.equals(Tarea.ESTADO_EN_COLA) ||
                    estado.equals(Tarea.ESTADO_ANALIZANDO) ||
                    estado.equals(Tarea.ESTADO_PAUSADO)) {
                    tarea.establecerEstado(Tarea.ESTADO_CANCELADO);
                    actualizarFilaTabla(tarea);
                    canceladas++;
                }
            }
            logger.accept("Tareas canceladas: " + canceladas);
        } finally {
            candado.unlock();
        }
    }

    public void pausarReanudarTodas() {
        candado.lock();
        try {
            int pausadas = 0;
            int reanudadas = 0;

            for (Tarea tarea : tareas.values()) {
                String estado = tarea.obtenerEstado();
                if (estado.equals(Tarea.ESTADO_ANALIZANDO)) {
                    tarea.establecerEstado(Tarea.ESTADO_PAUSADO);
                    actualizarFilaTabla(tarea);
                    pausadas++;
                } else if (estado.equals(Tarea.ESTADO_PAUSADO)) {
                    tarea.establecerEstado(Tarea.ESTADO_ANALIZANDO);
                    actualizarFilaTabla(tarea);
                    reanudadas++;
                }
            }

            if (pausadas > 0) {
                logger.accept("Tareas pausadas: " + pausadas);
            } else if (reanudadas > 0) {
                logger.accept("Tareas reanudadas: " + reanudadas);
            }
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

            logger.accept("Tareas limpiadas: " + idsAEliminar.size());
        } finally {
            candado.unlock();
        }
    }

    private void verificarTareasAtascadas() {
        // Primero: recopilar tareas atascadas (con lock)
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
                tarea.establecerMensajeInfo("Tarea atascada - timeout");
            }
        } finally {
            candado.unlock();
        }

        // Segundo: actualizar UI SIN lock (fuera del bloque sincronizado)
        for (Tarea tarea : tareasAtascadas) {
            actualizarFilaTabla(tarea);
            logger.accept("Tarea atascada detectada: " + tarea.obtenerId());
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
                String estado = tarea.obtenerEstado();
                if (estado.equals(Tarea.ESTADO_EN_COLA) ||
                    estado.equals(Tarea.ESTADO_ANALIZANDO)) {
                    tarea.establecerEstado(Tarea.ESTADO_PAUSADO);
                    actualizarFilaTabla(tarea);
                    logger.accept("Tarea pausada: " + tarea.obtenerUrl());
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
                String estado = tarea.obtenerEstado();
                if (estado.equals(Tarea.ESTADO_PAUSADO) ||
                    estado.equals(Tarea.ESTADO_ERROR) ||
                    estado.equals(Tarea.ESTADO_CANCELADO)) {
                    tarea.establecerEstado(Tarea.ESTADO_EN_COLA);
                    actualizarFilaTabla(tarea);
                    logger.accept("Tarea reanudada: " + tarea.obtenerUrl());
                }
            }
        } finally {
            candado.unlock();
        }
    }

    public void cancelarTarea(String id) {
        candado.lock();
        try {
            Tarea tarea = tareas.get(id);
            if (tarea != null) {
                String estado = tarea.obtenerEstado();
                if (estado.equals(Tarea.ESTADO_EN_COLA) ||
                    estado.equals(Tarea.ESTADO_ANALIZANDO) ||
                    estado.equals(Tarea.ESTADO_PAUSADO)) {
                    tarea.establecerEstado(Tarea.ESTADO_CANCELADO);
                    actualizarFilaTabla(tarea);
                    logger.accept("Tarea cancelada: " + tarea.obtenerUrl());
                }
            }
        } finally {
            candado.unlock();
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

        logger.accept("Tarea limpiada: " + id);
    }

    public void detener() {
        if (temporizadorVerificacion != null) {
            temporizadorVerificacion.stop();
        }
        // Limpiar recursos
        candado.lock();
        try {
            tareas.clear();
            modeloTabla.limpiar();
        } finally {
            candado.unlock();
        }
    }

    /**
     * Método shutdown para limpieza de recursos (llamado por ExtensionBurpIA.unload())
     */
    public void shutdown() {
        detener();
    }

    /**
     * Verifica si una tarea está cancelada.
     * @param id ID de la tarea
     * @return true si la tarea está cancelada
     */
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

    /**
     * Verifica si una tarea está pausada.
     * @param id ID de la tarea
     * @return true si la tarea está pausada
     */
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
}
