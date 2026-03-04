package com.burpia.ui;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Tarea;
import com.burpia.util.Normalizador;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static com.burpia.ui.UIUtils.ejecutarEnEdt;

public class ModeloTablaTareas extends DefaultTableModel {
    private static final int TOTAL_COLUMNAS = 4;
    private static final int LIMITE_DEFECTO_TAREAS = 500;
    private final List<Tarea> datos;
    private int limiteFilas;
    private final ReentrantLock lock;
    private final AtomicInteger versionCambios = new AtomicInteger(0);

    public ModeloTablaTareas() {
        this(LIMITE_DEFECTO_TAREAS);
    }

    public ModeloTablaTareas(int limiteFilas) {
        super(I18nUI.Tablas.COLUMNAS_TAREAS(), 0);
        this.datos = new ArrayList<>();
        this.limiteFilas = Math.max(1, limiteFilas);
        this.lock = new ReentrantLock();
    }

    @Override
    public boolean isCellEditable(int fila, int columna) {
        return false;
    }

    /**
     * Agrega una sola tarea a la tabla.
     * 
     * NOTA DE RENDIMIENTO: Para agregar múltiples tareas, usar agregarTareas(List) en su lugar.
     * agregarTareas() es hasta 17x más rápido porque minimiza invocaciones al EDT.
     * 
     * Benchmark: agregar100.individual = 0.593 ms vs agregar100.batch = 0.035 ms
     * 
     * @param tarea Tarea a agregar
     */
    public void agregarTarea(Tarea tarea) {
        agregarTareaYObtenerIdsPurgadas(tarea);
    }

    /**
     * Agrega múltiples tareas en una sola operación batch.
     * Mucho más eficiente que llamar agregarTarea() N veces porque
     * minimiza las invocaciones al EDT y las notificaciones a listeners.
     *
     * @param tareas Lista de tareas a agregar
     */
    public void agregarTareas(List<Tarea> tareas) {
        if (tareas == null || tareas.isEmpty()) {
            return;
        }

        List<Tarea> tareasFiltradas = new ArrayList<>();
        for (Tarea tarea : tareas) {
            if (tarea != null) {
                tareasFiltradas.add(tarea);
            }
        }

        if (tareasFiltradas.isEmpty()) {
            return;
        }

        ejecutarEnEdt(() -> {
            List<String> idsPurgadas;
            lock.lock();
            try {
                boolean huboCambios = false;
                for (Tarea tarea : tareasFiltradas) {
                    int tamañoAnterior = datos.size();
                    datos.add(tarea);
                    if (datos.size() > tamañoAnterior) {
                        huboCambios = true;
                    }
                }
                marcarCambio();
                idsPurgadas = aplicarLimiteFilasEnDatos();
            } finally {
                lock.unlock();
            }

            sincronizarTablaDesdeDatosEnEdt();
        });
    }

    public List<String> agregarTareaYObtenerIdsPurgadas(Tarea tarea) {
        if (tarea == null) {
            return new ArrayList<>();
        }
        List<String> idsPurgadas;
        lock.lock();
        try {
            datos.add(tarea);
            marcarCambio();
            idsPurgadas = aplicarLimiteFilasEnDatos();
            if (!idsPurgadas.isEmpty()) {
                programarSincronizacionTabla();
            } else {
                ejecutarEnEdt(() -> {
                    addRow(tarea.aFilaTabla());
                });
            }
        } finally {
            lock.unlock();
        }
        return idsPurgadas;
    }

    public void actualizarTarea(Tarea tarea) {
        if (tarea == null) {
            return;
        }
        String idTarea = tarea.obtenerId();
        if (Normalizador.esVacio(idTarea)) {
            return;
        }
        int indiceEnDatos = -1;
        lock.lock();
        try {
            indiceEnDatos = buscarIndiceSi(t -> idTarea.equals(t.obtenerId()));
            if (indiceEnDatos >= 0) {
                datos.set(indiceEnDatos, tarea);
                marcarCambio();
            }
        } finally {
            lock.unlock();
        }
        
        if (indiceEnDatos != -1) {
            final int filaUI = indiceEnDatos;
            final Object[] nuevosValores = tarea.aFilaTabla();
            ejecutarEnEdt(() -> {
                if (filaUI < getRowCount()) {
                    for (int col = 0; col < TOTAL_COLUMNAS; col++) {
                        setValueAt(nuevosValores[col], filaUI, col);
                    }
                }
            });
        }
    }

    private List<String> aplicarLimiteFilasEnDatos() {
        List<String> idsPurgadas = new ArrayList<>();
        while (datos.size() > limiteFilas) {
            int indice = buscarIndicePurgablePorLimite();
            if (indice < 0 || indice >= datos.size()) {
                break;
            }
            Tarea tareaPurgada = datos.remove(indice);
            if (tareaPurgada != null) {
                String id = tareaPurgada.obtenerId();
                if (id != null && !id.isEmpty()) {
                    idsPurgadas.add(id);
                }
            }
        }
        return idsPurgadas;
    }

    private int buscarIndicePurgablePorLimite() {
        return buscarIndiceSi(t -> t != null && t.esFinalizada());
    }

    /**
     * Busca el índice de la primera tarea que cumpla con la condición especificada.
     * Debe llamarse dentro de un lock bloqueado.
     *
     * @param condicion Predicado para evaluar cada tarea
     * @return Índice de la primera tarea que cumple la condición, o -1 si no encuentra
     */
    private int buscarIndiceSi(java.util.function.Predicate<Tarea> condicion) {
        for (int i = 0; i < datos.size(); i++) {
            Tarea tarea = datos.get(i);
            if (tarea != null && condicion.test(tarea)) {
                return i;
            }
        }
        return -1;
    }

    public void limpiar() {
        lock.lock();
        try {
            datos.clear();
            marcarCambio();
        } finally {
            lock.unlock();
        }
        ejecutarEnEdt(() -> {
            setRowCount(0);
        });
    }

    public Tarea obtenerTarea(int indiceFila) {
        lock.lock();
        try {
            if (indiceFila >= 0 && indiceFila < datos.size()) {
                return datos.get(indiceFila);
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    public String obtenerIdTarea(int indiceFila) {
        Tarea tarea = obtenerTarea(indiceFila);
        return tarea != null ? tarea.obtenerId() : null;
    }

    public void eliminarTarea(int indiceFila) {
        boolean eliminado = false;
        lock.lock();
        try {
            if (indiceFila >= 0 && indiceFila < datos.size()) {
                datos.remove(indiceFila);
                eliminado = true;
            }
        } finally {
            lock.unlock();
        }

        if (!eliminado) {
            return;
        }
        marcarCambio();
        final int filaAEliminar = indiceFila;
        ejecutarEnEdt(() -> {
            if (filaAEliminar < getRowCount()) {
                removeRow(filaAEliminar);
            }
        });
    }

    public int buscarIndicePorId(String idTarea) {
        if (Normalizador.esVacio(idTarea)) {
            return -1;
        }
        lock.lock();
        try {
            return buscarIndiceSi(t -> idTarea.equals(t.obtenerId()));
        } finally {
            lock.unlock();
        }
    }

    public void eliminarTareaPorId(String idTarea) {
        if (Normalizador.esVacio(idTarea)) {
            return;
        }
        int indice = buscarIndicePorId(idTarea);
        if (indice >= 0) {
            eliminarTarea(indice);
        }
    }

    public int obtenerNumeroTareas() {
        lock.lock();
        try {
            return datos.size();
        } finally {
            lock.unlock();
        }
    }

    public int contarPorEstado(String estado) {
        if (Normalizador.esVacio(estado)) {
            return 0;
        }
        lock.lock();
        try {
            return (int) datos.stream()
                .filter(t -> t != null)
                .filter(t -> estado.equals(t.obtenerEstado()))
                .count();
        } finally {
            lock.unlock();
        }
    }

    public void eliminarPorEstado(String... estados) {
        if (estados == null || estados.length == 0) {
            return;
        }
        java.util.Set<String> estadosSet = new java.util.HashSet<>(java.util.Arrays.asList(estados));
        
        // OPTIMIZACIÓN: Usar removeIf() que es O(n) en lugar de múltiples remove(idx) que son O(n²)
        lock.lock();
        try {
            boolean huboCambios = datos.removeIf(tarea -> 
                tarea != null && estadosSet.contains(tarea.obtenerEstado())
            );
            
            if (!huboCambios) {
                return;
            }
            marcarCambio();
        } finally {
            lock.unlock();
        }

        // Sincronizar tabla completa (setDataVector es más eficiente que múltiples removeRow)
        programarSincronizacionTabla();
    }

    public List<Tarea> obtenerTodasLasTareas() {
        lock.lock();
        try {
            return new ArrayList<>(datos);
        } finally {
            lock.unlock();
        }
    }

    public void refrescarColumnasIdioma() {
        ejecutarEnEdt(() -> {
            setColumnIdentifiers(I18nUI.Tablas.COLUMNAS_TAREAS());
            sincronizarTablaDesdeDatosEnEdt();
        });
    }

    @Override
    public int getColumnCount() {
        return TOTAL_COLUMNAS;
    }

    private void programarSincronizacionTabla() {
        ejecutarEnEdt(this::sincronizarTablaDesdeDatosEnEdt);
    }

    public int obtenerVersion() { return versionCambios.get(); }

    private void marcarCambio() {
        versionCambios.incrementAndGet();
    }

    private void sincronizarTablaDesdeDatosEnEdt() {
        List<Object[]> snapshot = new ArrayList<>();
        lock.lock();
        try {
            for (Tarea tarea : datos) {
                if (tarea != null) {
                    snapshot.add(tarea.aFilaTabla());
                }
            }
        } finally {
            lock.unlock();
        }

        // Optimización: setDataVector() hace una sola notificación en lugar de N addRow() separados
        // Esto mejora significativamente el rendimiento cuando hay muchas filas
        setDataVector(
            snapshot.toArray(new Object[0][]),
            I18nUI.Tablas.COLUMNAS_TAREAS()
        );
    }

    public int obtenerLimiteFilas() {
        lock.lock();
        try {
            return limiteFilas;
        } finally {
            lock.unlock();
        }
    }

    public void establecerLimiteFilas(int nuevoLimite) {
        int limiteNormalizado = Math.max(1, nuevoLimite);
        lock.lock();
        try {
            if (this.limiteFilas == limiteNormalizado) {
                return;
            }
            this.limiteFilas = limiteNormalizado;
        } finally {
            lock.unlock();
        }
        // Aplicar el nuevo límite y sincronizar la tabla si se purgaron tareas
        List<String> idsPurgadas = new ArrayList<>();
        lock.lock();
        try {
            idsPurgadas = aplicarLimiteFilasEnDatos();
            marcarCambio();
        } finally {
            lock.unlock();
        }
        if (!idsPurgadas.isEmpty()) {
            programarSincronizacionTabla();
        }
    }
}
