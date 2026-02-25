package com.burpia.ui;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Tarea;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class ModeloTablaTareas extends DefaultTableModel {
    private static final int TOTAL_COLUMNAS = 4;
    private final List<Tarea> datos;
    private final int limiteFilas;
    private final ReentrantLock lock;

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
            aplicarLimiteFilasEnDatos();
        } finally {
            lock.unlock();
        }
        programarSincronizacionTabla();
    }

    public void actualizarTarea(Tarea tarea) {
        if (tarea == null) {
            return;
        }
        String idTarea = tarea.obtenerId();
        if (idTarea == null || idTarea.isEmpty()) {
            return;
        }
        boolean actualizada = false;
        lock.lock();
        try {
            for (int i = 0; i < datos.size(); i++) {
                Tarea tareaActual = datos.get(i);
                if (tareaActual != null && idTarea.equals(tareaActual.obtenerId())) {
                    datos.set(i, tarea);
                    actualizada = true;
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
        if (actualizada) {
            programarSincronizacionTabla();
        }
    }

    private void aplicarLimiteFilasEnDatos() {
        while (datos.size() > limiteFilas) {
            int indice = buscarIndicePurgablePorLimite();
            if (indice < 0 || indice >= datos.size()) {
                break;
            }
            datos.remove(indice);
        }
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
        } finally {
            lock.unlock();
        }
        programarSincronizacionTabla();
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
        programarSincronizacionTabla();
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
        boolean cambio = false;

        lock.lock();
        try {
            for (int i = datos.size() - 1; i >= 0; i--) {
                Tarea tarea = datos.get(i);
                for (String estado : estados) {
                    if (tarea != null && estado != null && estado.equals(tarea.obtenerEstado())) {
                        datos.remove(i);
                        cambio = true;
                        break;
                    }
                }
            }
        } finally {
            lock.unlock();
        }
        if (cambio) {
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

    private void sincronizarTablaDesdeDatosEnEdt() {
        List<Object[]> snapshot = new ArrayList<>();
        lock.lock();
        try {
            aplicarLimiteFilasEnDatos();
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
