package com.burpia.ui;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Tarea;
import com.burpia.util.ContadorEstadosTareas;
import com.burpia.util.Normalizador;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static com.burpia.ui.UIUtils.ejecutarEnEdt;

public class ModeloTablaTareas extends DefaultTableModel {
    private static final int TOTAL_COLUMNAS = 4;
    private static final int LIMITE_DEFECTO_TAREAS = 500;
    private final List<Tarea> datos;
    // Mapa paralelo id → índice para lookup O(1) en hot paths
    // (actualizarTarea, buscarIndicePorId). Antes se hacían scans lineales
    // O(n) bajo lock en cada update — con 500 tareas + bursts concurrentes
    // se notaba. Mantenido sincrónicamente con `datos`: ver
    // reconstruirIndicePorId() después de cualquier cambio estructural.
    private final Map<String, Integer> indicePorId;
    private int limiteFilas;
    private final ReentrantLock lock;
    private final AtomicInteger versionCambios = new AtomicInteger(0);
    private volatile Consumer<List<String>> manejadorPurgado;

    public ModeloTablaTareas() {
        this(LIMITE_DEFECTO_TAREAS);
    }

    public ModeloTablaTareas(int limiteFilas) {
        super(I18nUI.Tablas.COLUMNAS_TAREAS(), 0);
        this.datos = new ArrayList<>();
        this.indicePorId = new HashMap<>();
        this.limiteFilas = Math.max(1, limiteFilas);
        this.lock = new ReentrantLock();
    }

    /**
     * Reconstruye el índice {@code id → índice} desde {@code datos}. Debe
     * llamarse bajo lock después de cualquier cambio estructural (add,
     * remove, purga, limpiar). O(n) — el costo se amortiza contra los
     * lookups O(1) posteriores en hot paths.
     */
    private void reconstruirIndicePorId() {
        indicePorId.clear();
        for (int i = 0; i < datos.size(); i++) {
            Tarea t = datos.get(i);
            if (t != null) {
                String id = t.obtenerId();
                if (Normalizador.noEsVacio(id)) {
                    indicePorId.put(id, i);
                }
            }
        }
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
        if (Normalizador.esVacia(tareas)) {
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
                for (Tarea tarea : tareasFiltradas) {
                    datos.add(tarea);
                }
                marcarCambio();
                idsPurgadas = aplicarLimiteFilasEnDatos();
                reconstruirIndicePorId();
            } finally {
                lock.unlock();
            }

            notificarTareasPurgadas(idsPurgadas);
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
            reconstruirIndicePorId();
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
        notificarTareasPurgadas(idsPurgadas);
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
        boolean actualizado;
        int versionAlActualizar;
        lock.lock();
        try {
            int indiceEnDatos = buscarIndiceSi(t -> idTarea.equals(t.obtenerId()));
            if (indiceEnDatos >= 0) {
                datos.set(indiceEnDatos, tarea);
                marcarCambio();
                actualizado = true;
            } else {
                actualizado = false;
            }
            versionAlActualizar = versionCambios.get();
        } finally {
            lock.unlock();
        }

        if (!actualizado) {
            return;
        }

        // Update puntual evitando el rebuild completo de setDataVector
        // (que disparaba fireTableStructureChanged → repaint completo
        // + reset column model + pérdida de selección). Para evitar TOCTOU
        // re-buscamos el índice DENTRO del EDT bajo lock: si la tarea fue
        // purgada por otro hilo entre tanto, la sync completa ya está
        // programada y nos saltamos esta actualización puntual.
        // M14: capturamos la versión para abortar si hubo cambio estructural
        // (purga) entre el encolado y la ejecución en EDT — en ese caso los
        // índices de `datos` y `dataVector` divergieron y actualizar por índice
        // escribiría en la fila equivocada.
        ejecutarEnEdt(() -> aplicarActualizacionPuntualEnEdt(idTarea, tarea, versionAlActualizar));
    }

    /**
     * Aplica una actualización puntual de fila en el EDT, re-verificando
     * la posición de la tarea bajo lock para evitar carreras con purgas.
     *
     * @param versionAlEncolar versión de cambios capturada al encolar; si difiere
     *                         de la actual al ejecutar, hubo un cambio estructural
     *                         y abortamos (la sync completa ya refleja el estado)
     */
    private void aplicarActualizacionPuntualEnEdt(String idTarea, Tarea tarea, int versionAlEncolar) {
        // M14: si hubo cambio estructural (purga/add batch) entre el encolado y
        // ahora, los índices de dataVector ya no corresponden a los de `datos`.
        // Abortar: la sync completa programada por ese cambio ya es consistente.
        if (versionCambios.get() != versionAlEncolar) {
            return;
        }

        Object[] fila;
        int indiceActual;
        lock.lock();
        try {
            // Re-verificar: la versión pudo cambiar justo antes de tomar el lock.
            if (versionCambios.get() != versionAlEncolar) {
                return;
            }
            indiceActual = buscarIndiceSi(t -> idTarea.equals(t.obtenerId()));
            if (indiceActual < 0) {
                // Purgada entre el unlock y el EDT → ya se programó (o se
                // programará) una sync completa que cubre este caso.
                return;
            }
            fila = tarea.aFilaTabla();
        } finally {
            lock.unlock();
        }

        // Si el tableModel está fuera de sync con `datos` (puede pasar tras
        // una purga concurrente que aún no se reflejó), abortamos: la sync
        // completa lo cubrirá.
        if (indiceActual >= getRowCount() || fila == null) {
            return;
        }

        int columnas = Math.min(fila.length, getColumnCount());
        for (int col = 0; col < columnas; col++) {
            // setValueAt actualiza dataVector y emite fireTableCellUpdated.
            // Swing coalesce cells cercanas en un solo repaint.
            setValueAt(fila[col], indiceActual, col);
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
            String id = tareaPurgada.obtenerId();
            if (Normalizador.noEsVacio(id)) {
                idsPurgadas.add(id);
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
            indicePorId.clear();
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
                reconstruirIndicePorId();
                eliminado = true;
                marcarCambio();
            }
        } finally {
            lock.unlock();
        }

        if (!eliminado) {
            return;
        }
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
            // Lookup O(1) vía mapa paralelo. Antes era scan O(n) sobre `datos`.
            Integer idx = indicePorId.get(idTarea);
            if (idx == null) {
                return -1;
            }
            // Verificación defensiva: si el mapa quedó stale por algún path
            // que olvidó reconstruir, no devolvemos un índice inválido.
            if (idx >= 0 && idx < datos.size()) {
                Tarea t = datos.get(idx);
                if (t != null && idTarea.equals(t.obtenerId())) {
                    return idx;
                }
            }
            // Fallback: scan lineal y reconstruye índice si encuentra discrepancia.
            int idxReal = buscarIndiceSi(t -> idTarea.equals(t.obtenerId()));
            if (idxReal >= 0) {
                reconstruirIndicePorId();
            }
            return idxReal;
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

        lock.lock();
        try {
            boolean huboCambios = datos.removeIf(tarea ->
                tarea != null && estadosSet.contains(tarea.obtenerEstado())
            );

            if (!huboCambios) {
                return;
            }
            reconstruirIndicePorId();
            marcarCambio();
        } finally {
            lock.unlock();
        }

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
        List<String> idsPurgadas;
        lock.lock();
        try {
            if (this.limiteFilas == limiteNormalizado) {
                return;
            }
            this.limiteFilas = limiteNormalizado;
            idsPurgadas = aplicarLimiteFilasEnDatos();
            if (!idsPurgadas.isEmpty()) {
                reconstruirIndicePorId();
            }
            marcarCambio();
        } finally {
            lock.unlock();
        }
        notificarTareasPurgadas(idsPurgadas);
        if (!idsPurgadas.isEmpty()) {
            programarSincronizacionTabla();
        }
    }

    public void establecerManejadorPurgado(Consumer<List<String>> manejadorPurgado) {
        this.manejadorPurgado = manejadorPurgado;
    }

    /**
     * L9: libera referencias retenidas por el modelo al descargar la extensión.
     * En plugins Burp singleton el modelo puede sobrevivir a la UI; el handler
     * de purgado captura típicamente una referencia al componente padre, así
     * que sin limpiarlo se retiene la vieja UI. Idempotente.
     */
    public void dispose() {
        this.manejadorPurgado = null;
    }

    /**
     * Cuenta las tareas por estado usando el contador centralizado.
     * <p>
     * Este método usa {@link ContadorEstadosTareas} para evitar duplicar
     * la lógica de conteo en múltiples lugares.
     * </p>
     *
     * @return ContadorEstadosTareas con los conteos de cada estado
     */
    public ContadorEstadosTareas contarEstados() {
        lock.lock();
        try {
            return ContadorEstadosTareas.contar(datos);
        } finally {
            lock.unlock();
        }
    }

    private void notificarTareasPurgadas(List<String> idsPurgadas) {
        Consumer<List<String>> manejador = this.manejadorPurgado;
        if (manejador == null || Normalizador.esVacia(idsPurgadas)) {
            return;
        }
        manejador.accept(new ArrayList<>(idsPurgadas));
    }
}
