package com.burpia.ui;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Tarea;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class ModeloTablaTareas extends DefaultTableModel {
    private static final int TOTAL_COLUMNAS = 4;
    private final List<Tarea> datos;
    private final int limiteFilas;
    private final ReentrantLock lock;
    private final AtomicInteger versionCambios = new AtomicInteger(0);

    public ModeloTablaTareas() {
        this(500);
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

    public void agregarTarea(Tarea tarea) {
        if (tarea == null) {
            return;
        }
        lock.lock();
        try {
            datos.add(tarea);
            marcarCambio();
            boolean purgaRealizada = aplicarLimiteFilasEnDatos();
            if (purgaRealizada) {
                programarSincronizacionTabla();
            } else {
                SwingUtilities.invokeLater(() -> {
                    addRow(tarea.aFilaTabla());
                });
            }
        } finally {
            lock.unlock();
        }
    }

    public void actualizarTarea(Tarea tarea) {
        if (tarea == null) {
            return;
        }
        String idTarea = tarea.obtenerId();
        if (idTarea == null || idTarea.isEmpty()) {
            return;
        }
        int indiceEnDatos = -1;
        lock.lock();
        try {
            for (int i = 0; i < datos.size(); i++) {
                Tarea tareaActual = datos.get(i);
                if (tareaActual != null && idTarea.equals(tareaActual.obtenerId())) {
                    datos.set(i, tarea);
                    indiceEnDatos = i;
                    marcarCambio();
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
        
        if (indiceEnDatos != -1) {
            final int filaUI = indiceEnDatos;
            final Object[] nuevosValores = tarea.aFilaTabla();
            SwingUtilities.invokeLater(() -> {
                if (filaUI < getRowCount()) {
                    for (int col = 0; col < TOTAL_COLUMNAS; col++) {
                        setValueAt(nuevosValores[col], filaUI, col);
                    }
                }
            });
        }
    }

    private boolean aplicarLimiteFilasEnDatos() {
        boolean huboPurga = false;
        while (datos.size() > limiteFilas) {
            int indice = buscarIndicePurgablePorLimite();
            if (indice < 0 || indice >= datos.size()) {
                break;
            }
            datos.remove(indice);
            huboPurga = true;
        }
        return huboPurga;
    }

    private int buscarIndicePurgablePorLimite() {
        for (int i = 0; i < datos.size(); i++) {
            Tarea tarea = datos.get(i);
            if (tarea != null && tarea.esFinalizada()) {
                return i;
            }
        }
        if (datos.isEmpty()) {
            return -1;
        }
        return 0;
    }

    public void limpiar() {
        lock.lock();
        try {
            datos.clear();
            marcarCambio();
        } finally {
            lock.unlock();
        }
        SwingUtilities.invokeLater(() -> {
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
        SwingUtilities.invokeLater(() -> {
            if (filaAEliminar < getRowCount()) {
                removeRow(filaAEliminar);
            }
        });
    }

    public int buscarIndicePorId(String idTarea) {
        if (idTarea == null || idTarea.isEmpty()) {
            return -1;
        }
        lock.lock();
        try {
            for (int i = 0; i < datos.size(); i++) {
                Tarea tarea = datos.get(i);
                if (tarea != null && idTarea.equals(tarea.obtenerId())) {
                    return i;
                }
            }
            return -1;
        } finally {
            lock.unlock();
        }
    }

    public void eliminarTareaPorId(String idTarea) {
        if (idTarea == null || idTarea.isEmpty()) {
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
        if (estado == null || estado.isEmpty()) {
            return 0;
        }
        lock.lock();
        try {
            int count = 0;
            for (Tarea tarea : datos) {
                if (tarea != null && estado.equals(tarea.obtenerEstado())) {
                    count++;
                }
            }
            return count;
        } finally {
            lock.unlock();
        }
    }

    public void eliminarPorEstado(String... estados) {
        if (estados == null || estados.length == 0) {
            return;
        }
        boolean huboCambios = false;
        lock.lock();
        try {
            for (int i = datos.size() - 1; i >= 0; i--) {
                Tarea tarea = datos.get(i);
                for (String estado : estados) {
                    if (tarea != null && estado != null && estado.equals(tarea.obtenerEstado())) {
                        datos.remove(i);
                        huboCambios = true;
                        break;
                    }
                }
            }
        } finally {
            lock.unlock();
        }
        if (huboCambios) {
            marcarCambio();
            programarSincronizacionTabla();
        }
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
        SwingUtilities.invokeLater(() -> {
            setColumnIdentifiers(I18nUI.Tablas.COLUMNAS_TAREAS());
            sincronizarTablaDesdeDatosEnEdt();
        });
    }

    @Override
    public int getColumnCount() {
        return TOTAL_COLUMNAS;
    }

    private void programarSincronizacionTabla() {
        SwingUtilities.invokeLater(this::sincronizarTablaDesdeDatosEnEdt);
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

        setRowCount(0);
        for (Object[] fila : snapshot) {
            addRow(fila);
        }
    }
}
