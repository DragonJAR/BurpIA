package com.burpia.ui;

import burp.api.montoya.http.message.requests.HttpRequest;
import com.burpia.model.Hallazgo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Modelo de tabla para hallazgos con thread-safety.
 */
public class ModeloTablaHallazgos extends DefaultTableModel {
    private static final String[] COLUMNAS = {"Hora", "URL", "Hallazgo", "Severidad", "Confianza"};
    private final List<Hallazgo> datos;
    private final int limiteFilas;
    private final Set<Integer> filasIgnoradas;
    private final ReentrantLock lock;

    public ModeloTablaHallazgos() {
        this(1000);
    }

    public ModeloTablaHallazgos(int limiteFilas) {
        super(COLUMNAS, 0);
        this.datos = new ArrayList<>();
        this.limiteFilas = limiteFilas;
        this.filasIgnoradas = new HashSet<>();
        this.lock = new ReentrantLock();
    }

    @Override
    public boolean isCellEditable(int fila, int columna) {
        return false;
    }

    public void agregarHallazgo(Hallazgo hallazgo) {
        lock.lock();
        try {
            datos.add(hallazgo);
        } finally {
            lock.unlock();
        }

        SwingUtilities.invokeLater(() -> {
            Object[] fila = hallazgo.aFilaTabla();
            addRow(fila);
            fireTableDataChanged();
            aplicarLimiteFilas();
        });
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
                    Set<Integer> nuevosIgnorados = new HashSet<>();
                    for (Integer idx : filasIgnoradas) {
                        if (idx > 0) nuevosIgnorados.add(idx - 1);
                    }
                    filasIgnoradas.clear();
                    filasIgnoradas.addAll(nuevosIgnorados);
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
            filasIgnoradas.clear();
        } finally {
            lock.unlock();
        }

        SwingUtilities.invokeLater(() -> {
            setRowCount(0);
            fireTableDataChanged();
        });
    }

    public Hallazgo obtenerHallazgo(int indiceFila) {
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

    public int obtenerNumeroHallazgos() {
        lock.lock();
        try {
            return datos.size();
        } finally {
            lock.unlock();
        }
    }

    public List<Integer> filtrarPorSeveridad(String severidad) {
        lock.lock();
        try {
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < datos.size(); i++) {
                Hallazgo h = datos.get(i);
                if (h.obtenerSeveridad().equals(severidad)) {
                    indices.add(i);
                }
            }
            return indices;
        } finally {
            lock.unlock();
        }
    }

    public List<Integer> buscarPorTexto(String textoBusqueda) {
        lock.lock();
        try {
            List<Integer> indices = new ArrayList<>();
            String textoLower = textoBusqueda.toLowerCase();

            for (int i = 0; i < datos.size(); i++) {
                Hallazgo h = datos.get(i);
                if (h.obtenerHallazgo().toLowerCase().contains(textoLower) ||
                    h.obtenerUrl().toLowerCase().contains(textoLower)) {
                    indices.add(i);
                }
            }
            return indices;
        } finally {
            lock.unlock();
        }
    }

    public void marcarComoIgnorado(int fila) {
        lock.lock();
        try {
            if (fila >= 0 && fila < datos.size()) {
                filasIgnoradas.add(fila);
            }
        } finally {
            lock.unlock();
        }
        SwingUtilities.invokeLater(() -> fireTableRowsUpdated(fila, fila));
    }

    public boolean estaIgnorado(int fila) {
        lock.lock();
        try {
            return filasIgnoradas.contains(fila);
        } finally {
            lock.unlock();
        }
    }

    public int obtenerNumeroIgnorados() {
        lock.lock();
        try {
            return filasIgnoradas.size();
        } finally {
            lock.unlock();
        }
    }

    public List<Hallazgo> obtenerHallazgosNoIgnorados() {
        lock.lock();
        try {
            List<Hallazgo> resultado = new ArrayList<>();
            for (int i = 0; i < datos.size(); i++) {
                if (!filasIgnoradas.contains(i)) {
                    resultado.add(datos.get(i));
                }
            }
            return resultado;
        } finally {
            lock.unlock();
        }
    }

    @Deprecated
    public void marcarComoBorrado(int fila) {
        marcarComoIgnorado(fila);
    }

    @Deprecated
    public boolean estaBorrado(int fila) {
        return estaIgnorado(fila);
    }

    @Deprecated
    public int obtenerNumeroBorrados() {
        return obtenerNumeroIgnorados();
    }

    public HttpRequest obtenerSolicitudHttp(int indiceFila) {
        lock.lock();
        try {
            if (indiceFila >= 0 && indiceFila < datos.size()) {
                Hallazgo h = datos.get(indiceFila);
                return h != null ? h.obtenerSolicitudHttp() : null;
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    public void eliminarHallazgo(int indiceFila) {
        lock.lock();
        try {
            if (indiceFila >= 0 && indiceFila < datos.size()) {
                datos.remove(indiceFila);
                Set<Integer> nuevosIgnorados = new HashSet<>();
                for (Integer idx : filasIgnoradas) {
                    if (idx < indiceFila) {
                        nuevosIgnorados.add(idx);
                    } else if (idx > indiceFila) {
                        nuevosIgnorados.add(idx - 1);
                    }
                }
                filasIgnoradas.clear();
                filasIgnoradas.addAll(nuevosIgnorados);
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

    public List<Hallazgo> obtenerTodosLosHallazgos() {
        lock.lock();
        try {
            return new ArrayList<>(datos);
        } finally {
            lock.unlock();
        }
    }
}
