package com.burpia.ui;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Hallazgo;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class ModeloTablaHallazgos extends DefaultTableModel {
    private static final int TOTAL_COLUMNAS = 5;
    private final List<Hallazgo> datos;
    private int limiteFilas;
    private final Set<Integer> filasIgnoradas;
    private final ReentrantLock lock;

    public ModeloTablaHallazgos() {
        this(1000);
    }

    public ModeloTablaHallazgos(int limiteFilas) {
        super(I18nUI.Tablas.COLUMNAS_HALLAZGOS(), 0);
        this.datos = new ArrayList<>();
        this.limiteFilas = Math.max(1, limiteFilas);
        this.filasIgnoradas = new HashSet<>();
        this.lock = new ReentrantLock();
    }

    @Override
    public boolean isCellEditable(int fila, int columna) {
        return false;
    }

    public void agregarHallazgo(Hallazgo hallazgo) {
        if (hallazgo == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            lock.lock();
            try {
                datos.add(hallazgo);
                addRow(hallazgo.aFilaTabla());
                aplicarLimiteFilas();
            } finally {
                lock.unlock();
            }
        });
    }

    public void agregarHallazgos(List<Hallazgo> hallazgos) {
        if (hallazgos == null || hallazgos.isEmpty()) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            lock.lock();
            try {
                for (Hallazgo hallazgo : hallazgos) {
                    if (hallazgo == null) {
                        continue;
                    }
                    datos.add(hallazgo);
                    addRow(hallazgo.aFilaTabla());
                }
                aplicarLimiteFilas();
            } finally {
                lock.unlock();
            }
        });
    }

    private void aplicarLimiteFilas() {
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
        }
    }

    public void limpiar() {
        SwingUtilities.invokeLater(() -> {
            lock.lock();
            try {
                datos.clear();
                filasIgnoradas.clear();
                setRowCount(0);
            } finally {
                lock.unlock();
            }
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

    public void marcarComoIgnorado(int fila) {
        boolean filaValida;
        lock.lock();
        try {
            filaValida = fila >= 0 && fila < datos.size();
            if (filaValida) {
                filasIgnoradas.add(fila);
            }
        } finally {
            lock.unlock();
        }
        if (!filaValida) {
            return;
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
        boolean eliminado = false;
        lock.lock();
        try {
            if (indiceFila >= 0 && indiceFila < datos.size()) {
                datos.remove(indiceFila);
                eliminado = true;
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

        if (!eliminado) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            if (indiceFila >= 0 && indiceFila < getRowCount()) {
                removeRow(indiceFila);
            }
        });
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
        SwingUtilities.invokeLater(() -> {
            lock.lock();
            try {
                limiteFilas = limiteNormalizado;
                aplicarLimiteFilas();
            } finally {
                lock.unlock();
            }
        });
    }

    public void actualizarHallazgo(int indiceFila, Hallazgo nuevoHallazgo) {
        if (nuevoHallazgo == null) {
            return;
        }
        lock.lock();
        try {
            if (indiceFila >= 0 && indiceFila < datos.size()) {
                datos.set(indiceFila, nuevoHallazgo);
            } else {
                return;
            }
        } finally {
            lock.unlock();
        }

        SwingUtilities.invokeLater(() -> {
            if (indiceFila < getRowCount()) {
                Object[] filaValores = nuevoHallazgo.aFilaTabla();
                for (int i = 0; i < TOTAL_COLUMNAS; i++) {
                    setValueAt(filaValores[i], indiceFila, i);
                }
                fireTableRowsUpdated(indiceFila, indiceFila);
            }
        });
    }

    public boolean actualizarHallazgo(Hallazgo hallazgoOriginal, Hallazgo nuevoHallazgo) {
        if (hallazgoOriginal == null || nuevoHallazgo == null) {
            return false;
        }

        int indiceFila;
        lock.lock();
        try {
            indiceFila = -1;
            for (int i = 0; i < datos.size(); i++) {
                if (datos.get(i) == hallazgoOriginal) {
                    indiceFila = i;
                    break;
                }
            }
            if (indiceFila < 0) {
                return false;
            }
            datos.set(indiceFila, nuevoHallazgo);
        } finally {
            lock.unlock();
        }

        final int indiceActualizado = indiceFila;
        SwingUtilities.invokeLater(() -> {
            if (indiceActualizado < getRowCount()) {
                Object[] filaValores = nuevoHallazgo.aFilaTabla();
                for (int i = 0; i < TOTAL_COLUMNAS; i++) {
                    setValueAt(filaValores[i], indiceActualizado, i);
                }
                fireTableRowsUpdated(indiceActualizado, indiceActualizado);
            }
        });
        return true;
    }

    public void refrescarColumnasIdioma() {
        SwingUtilities.invokeLater(() -> {
            List<Object[]> snapshot = new ArrayList<>();
            lock.lock();
            try {
                for (Hallazgo hallazgo : datos) {
                    if (hallazgo != null) {
                        snapshot.add(hallazgo.aFilaTabla());
                    }
                }
            } finally {
                lock.unlock();
            }

            setColumnIdentifiers(I18nUI.Tablas.COLUMNAS_HALLAZGOS());
            setRowCount(0);
            for (Object[] fila : snapshot) {
                addRow(fila);
            }
        });
    }

    @Override
    public int getColumnCount() {
        return TOTAL_COLUMNAS;
    }
}
