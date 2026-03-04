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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import static com.burpia.ui.UIUtils.ejecutarEnEdt;

public class ModeloTablaHallazgos extends DefaultTableModel {
    private static final int TOTAL_COLUMNAS = 5;
    private final List<Hallazgo> datos;
    private int limiteFilas;
    private final Set<Integer> filasIgnoradas;
    private final ReentrantLock lock;
    private final List<EscuchaCambiosHallazgos> escuchas;

    public ModeloTablaHallazgos() {
        this(1000);
    }

    public ModeloTablaHallazgos(int limiteFilas) {
        super(I18nUI.Tablas.COLUMNAS_HALLAZGOS(), 0);
        this.datos = new ArrayList<>();
        this.limiteFilas = Math.max(1, limiteFilas);
        this.filasIgnoradas = new HashSet<>();
        this.lock = new ReentrantLock();
        this.escuchas = new CopyOnWriteArrayList<>();
    }

    @Override
    public boolean isCellEditable(int fila, int columna) {
        return false;
    }

    public void agregarHallazgo(Hallazgo hallazgo) {
        if (hallazgo == null) {
            return;
        }
        ejecutarEnEdt(() -> {
            lock.lock();
            try {
                datos.add(hallazgo);
                addRow(hallazgo.aFilaTabla());
                if (aplicarLimiteFilas()) {
                    fireTableDataChanged();
                }
            } finally {
                lock.unlock();
            }
            notificarCambios();
        });
    }

    @SuppressWarnings("unchecked")
    public void agregarHallazgos(List<Hallazgo> hallazgos) {
        if (hallazgos == null || hallazgos.isEmpty()) {
            return;
        }

        ejecutarEnEdt(() -> {
            boolean huboCambios = false;
            lock.lock();
            try {
                int agregados = 0;

                for (Hallazgo hallazgo : hallazgos) {
                    if (hallazgo == null) {
                        continue;
                    }

                    // Verificar límite ANTES de agregar para prevenir objetos huérfanos
                    if (datos.size() >= limiteFilas) {
                        // Límite alcanzado, no agregar más
                        break;
                    }

                    datos.add(hallazgo);
                    Object[] rowData = hallazgo.aFilaTabla();
                    addRow(rowData);
                    agregados++;
                    huboCambios = true;
                }

                if (huboCambios) {
                    // Doble protección: asegurar que dataVector también respete límite
                    aplicarLimiteFilas();
                    fireTableDataChanged();
                }
            } finally {
                lock.unlock();
            }
            if (huboCambios) {
                notificarCambios();
            }
        });
    }

    private boolean aplicarLimiteFilas() {
        int rowCount = dataVector.size();
        if (rowCount > limiteFilas) {
            int filasAEliminar = rowCount - limiteFilas;
            
            if (filasAEliminar > 0) {
                datos.subList(0, filasAEliminar).clear();
                dataVector.subList(0, filasAEliminar).clear();
                
                Set<Integer> nuevosIgnorados = new HashSet<>();
                for (Integer idx : filasIgnoradas) {
                    if (idx >= filasAEliminar) nuevosIgnorados.add(idx - filasAEliminar);
                }
                filasIgnoradas.clear();
                filasIgnoradas.addAll(nuevosIgnorados);
                return true;
            }
        }
        return false;
    }

    public void limpiar() {
        ejecutarEnEdt(() -> {
            lock.lock();
            try {
                datos.clear();
                filasIgnoradas.clear();
                dataVector.clear();
                fireTableDataChanged();
            } finally {
                lock.unlock();
            }
            notificarCambios();
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
        ejecutarEnEdt(() -> fireTableRowsUpdated(fila, fila));
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

    /**
     * Obtiene el número de filas actualmente visibles en la tabla.
     * Cuenta las filas que NO están ignoradas por filtros o marcas manuales.
     *
     * @return Número de filas visibles, o 0 si no hay datos
     */
    public int obtenerFilasVisibles() {
        lock.lock();
        try {
            if (filasIgnoradas == null || filasIgnoradas.isEmpty()) {
                return datos.size();
            }

            int visibles = 0;
            for (int i = 0; i < datos.size(); i++) {
                if (!filasIgnoradas.contains(i)) {
                    visibles++;
                }
            }
            return visibles;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Obtiene el conteo de hallazgos visibles agrupados por severidad.
     * Solo cuenta hallazgos que NO están ignorados por filtros.
     *
     * @return Array de 6 elementos: [total, critical, high, medium, low, info]
     */
    public int[] obtenerEstadisticasVisibles() {
        lock.lock();
        try {
            int[] stats = new int[6]; // [total, critical, high, medium, low, info]

            for (int i = 0; i < datos.size(); i++) {
                if (filasIgnoradas.contains(i)) {
                    continue;
                }

                Hallazgo h = datos.get(i);
                if (h == null) {
                    continue;
                }

                stats[0]++; // total

                String severidad = h.obtenerSeveridad();
                if (severidad != null) {
                    switch (severidad) {
                        case Hallazgo.SEVERIDAD_CRITICAL:
                            stats[1]++;
                            break;
                        case Hallazgo.SEVERIDAD_HIGH:
                            stats[2]++;
                            break;
                        case Hallazgo.SEVERIDAD_MEDIUM:
                            stats[3]++;
                            break;
                        case Hallazgo.SEVERIDAD_LOW:
                            stats[4]++;
                            break;
                        case Hallazgo.SEVERIDAD_INFO:
                            stats[5]++;
                            break;
                    }
                }
            }

            return stats;
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
        ejecutarEnEdt(() -> {
            if (indiceFila >= 0 && indiceFila < getRowCount()) {
                removeRow(indiceFila);
            }
            notificarCambios();
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
        ejecutarEnEdt(() -> {
            lock.lock();
            try {
                limiteFilas = limiteNormalizado;
                if (aplicarLimiteFilas()) {
                    fireTableDataChanged();
                }
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

        ejecutarEnEdt(() -> {
            if (indiceFila < getRowCount()) {
                Object[] filaValores = nuevoHallazgo.aFilaTabla();
                for (int i = 0; i < TOTAL_COLUMNAS; i++) {
                    setValueAt(filaValores[i], indiceFila, i);
                }
                fireTableRowsUpdated(indiceFila, indiceFila);
                notificarCambios();
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
        ejecutarEnEdt(() -> {
            if (indiceActualizado < getRowCount()) {
                Object[] filaValores = nuevoHallazgo.aFilaTabla();
                for (int i = 0; i < TOTAL_COLUMNAS; i++) {
                    setValueAt(filaValores[i], indiceActualizado, i);
                }
                fireTableRowsUpdated(indiceActualizado, indiceActualizado);
                notificarCambios();
            }
        });
        return true;
    }

    public void refrescarColumnasIdioma() {
        ejecutarEnEdt(() -> {
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

    /**
     * Agrega un escucha que será notificado cuando cambien los hallazgos.
     *
     * @param escucha El escucha a agregar
     */
    public void agregarEscucha(EscuchaCambiosHallazgos escucha) {
        if (escucha != null) {
            escuchas.add(escucha);
        }
    }

    /**
     * Elimina un escucha para que deje de recibir notificaciones.
     *
     * @param escucha El escucha a eliminar
     */
    public void eliminarEscucha(EscuchaCambiosHallazgos escucha) {
        escuchas.remove(escucha);
    }

    /**
     * Notifica a todos los escuchas que los hallazgos han cambiado.
     * La notificación se ejecuta en el EDT para garantizar seguridad UI.
     */
    private void notificarCambios() {
        if (escuchas.isEmpty()) {
            return;
        }
        ejecutarEnEdt(() -> {
            for (EscuchaCambiosHallazgos escucha : escuchas) {
                try {
                    escucha.enHallazgosCambiados();
                } catch (Exception e) {
                    // Prevenir que un escucha mal implementado rompa las notificaciones
                    System.err.println("Error en escucha de cambios: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public int getColumnCount() {
        return TOTAL_COLUMNAS;
    }
}
