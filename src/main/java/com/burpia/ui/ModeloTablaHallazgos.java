package com.burpia.ui;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Hallazgo;
import com.burpia.util.GestorLoggingUnificado;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import static com.burpia.ui.UIUtils.ejecutarEnEdt;

public class ModeloTablaHallazgos extends DefaultTableModel {
    private static final Logger LOGGER = Logger.getLogger(ModeloTablaHallazgos.class.getName());
    private static final int TOTAL_COLUMNAS = 5;
    private static final GestorLoggingUnificado gestorLogging = GestorLoggingUnificado.crearConLogger(LOGGER);
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

    /**
     * Verifica si un índice de fila es válido.
     * Método auxiliar para evitar repetición de validación (DRY).
     *
     * @param indice El índice a verificar
     * @return true si el índice está dentro del rango válido
     */
    private boolean esIndiceValido(int indice) {
        return indice >= 0 && indice < datos.size();
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
                for (Hallazgo hallazgo : hallazgos) {
                    if (hallazgo == null) {
                        continue;
                    }

                    if (datos.size() >= limiteFilas) {
                        break;
                    }

                    datos.add(hallazgo);
                    Object[] rowData = hallazgo.aFilaTabla();
                    addRow(rowData);
                    huboCambios = true;
                }

                if (huboCambios) {
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
            if (esIndiceValido(indiceFila)) {
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
            filaValida = esIndiceValido(fila);
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
            if (filasIgnoradas.isEmpty()) {
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
            if (esIndiceValido(indiceFila)) {
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
            if (esIndiceValido(indiceFila)) {
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
            if (!esIndiceValido(indiceFila)) {
                return;
            }
            datos.set(indiceFila, nuevoHallazgo);
        } finally {
            lock.unlock();
        }

        actualizarFilaEnTabla(indiceFila, nuevoHallazgo);
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

        actualizarFilaEnTabla(indiceFila, nuevoHallazgo);
        return true;
    }

    /**
     * Actualiza los valores de una fila en la tabla UI.
     * Método auxiliar para evitar duplicación de código (DRY).
     *
     * @param indice   El índice de la fila a actualizar
     * @param hallazgo El hallazgo con los nuevos valores
     */
    private void actualizarFilaEnTabla(int indice, Hallazgo hallazgo) {
        ejecutarEnEdt(() -> {
            if (indice < getRowCount()) {
                Object[] filaValores = hallazgo.aFilaTabla();
                for (int i = 0; i < TOTAL_COLUMNAS; i++) {
                    setValueAt(filaValores[i], indice, i);
                }
                fireTableRowsUpdated(indice, indice);
                notificarCambios();
            }
        });
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
                    gestorLogging.error("ModeloTablaHallazgos", "Error en escucha de cambios: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public int getColumnCount() {
        return TOTAL_COLUMNAS;
    }
}
