package com.burpia.ui;

import com.burpia.model.Tarea;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class ModeloTablaTareas extends DefaultTableModel {
    private static final String[] COLUMNAS = {"Tipo", "URL", "Estado", "Duracion"};
    private final List<Tarea> datos;
    private final int limiteFilas;
    private final ReentrantLock lock;

    public ModeloTablaTareas() {
        this(500);
    }

    public ModeloTablaTareas(int limiteFilas) {
        super(COLUMNAS, 0);
        this.datos = new ArrayList<>();
        this.limiteFilas = limiteFilas;
        this.lock = new ReentrantLock();
    }

    @Override
    public boolean isCellEditable(int fila, int columna) {
        return false;
    }

    public void agregarTarea(Tarea tarea) {
        lock.lock();
        try {
            datos.add(tarea);
        } finally {
            lock.unlock();
        }

        SwingUtilities.invokeLater(() -> {
            addRow(tarea.aFilaTabla());
            fireTableDataChanged();
            aplicarLimiteFilas();
        });
    }

    public void actualizarTarea(Tarea tarea) {
        lock.lock();
        try {
            for (int i = 0; i < datos.size(); i++) {
                if (datos.get(i).obtenerId().equals(tarea.obtenerId())) {
                    datos.set(i, tarea);
                    final int fila = i;
                    SwingUtilities.invokeLater(() -> {
                        Object[] filaDatos = tarea.aFilaTabla();
                        for (int j = 0; j < filaDatos.length; j++) {
                            setValueAt(filaDatos[j], fila, j);
                        }
                        fireTableRowsUpdated(fila, fila);
                    });
                    return;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void aplicarLimiteFilas() {
        lock.lock();
        try {
            if (getRowCount() > limiteFilas) {
                int filasAEliminar = getRowCount() - limiteFilas;
                for (int i = 0; i < filasAEliminar; i++) {
                    removeRow(0);
                    if (!datos.isEmpty()) {
                        datos.remove(0);
                    }
                }
                fireTableDataChanged();
            }
        } finally {
            lock.unlock();
        }
    }

    public void limpiar() {
        lock.lock();
        try {
            datos.clear();
        } finally {
            lock.unlock();
        }

        SwingUtilities.invokeLater(() -> {
            setRowCount(0);
            fireTableDataChanged();
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
        lock.lock();
        try {
            if (indiceFila >= 0 && indiceFila < datos.size()) {
                datos.remove(indiceFila);
            }
        } finally {
            lock.unlock();
        }

        SwingUtilities.invokeLater(() -> {
            if (indiceFila < getRowCount()) {
                removeRow(indiceFila);
            }
            fireTableDataChanged();
        });
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
        lock.lock();
        try {
            int count = 0;
            for (Tarea tarea : datos) {
                if (tarea.obtenerEstado().equals(estado)) {
                    count++;
                }
            }
            return count;
        } finally {
            lock.unlock();
        }
    }

    public void eliminarPorEstado(String... estados) {
        List<Integer> indicesAEliminar = new ArrayList<>();

        lock.lock();
        try {
            for (int i = 0; i < datos.size(); i++) {
                Tarea tarea = datos.get(i);
                for (String estado : estados) {
                    if (tarea.obtenerEstado().equals(estado)) {
                        indicesAEliminar.add(i);
                        break;
                    }
                }
            }

            for (int i = indicesAEliminar.size() - 1; i >= 0; i--) {
                int indice = indicesAEliminar.get(i);
                datos.remove(indice);
            }
        } finally {
            lock.unlock();
        }

        SwingUtilities.invokeLater(() -> {
            for (int i = indicesAEliminar.size() - 1; i >= 0; i--) {
                int indice = indicesAEliminar.get(i);
                if (indice < getRowCount()) {
                    removeRow(indice);
                }
            }
            fireTableDataChanged();
        });
    }

    public List<Tarea> obtenerTodasLasTareas() {
        lock.lock();
        try {
            return new ArrayList<>(datos);
        } finally {
            lock.unlock();
        }
    }
}
