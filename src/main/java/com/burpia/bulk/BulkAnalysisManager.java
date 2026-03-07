package com.burpia.bulk;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;
import com.burpia.ManejadorHttpBurpIA;
import com.burpia.model.Hallazgo;
import com.burpia.model.Tarea;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.GestorTareas;
import com.burpia.util.Normalizador;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manager for bulk analysis operations using Observer pattern.
 * <p>
 * Coordinates bulk analysis by:
 * 1. Fetching filtered history from HistorialBurpProvider
 * 2. Creating analysis tasks via existing GestorTareas
 * 3. Tracking progress via BulkProgressObserver
 * 4. Collecting results via BulkAnalysisCallback
 * </p>
 */
public class BulkAnalysisManager {
    
    private static final String ORIGEN_LOG = "BulkAnalysisManager";
    
    private final HistorialBurpProvider provider;
    private final GestorTareas gestorTareas;
    private final ManejadorHttpBurpIA manejadorHttp;
    private final GestorLoggingUnificado gestorLogging;
    private final AtomicBoolean cancelado;
    private final AtomicInteger procesados;
    private final AtomicInteger errores;
    private final List<BulkProgressObserver> observers;  // Thread-safe para observers
    
    public BulkAnalysisManager(HistorialBurpProvider provider,
                                GestorTareas gestorTareas,
                                ManejadorHttpBurpIA manejadorHttp,
                                GestorLoggingUnificado gestorLogging) {
        if (provider == null || gestorTareas == null || manejadorHttp == null || gestorLogging == null) {
            throw new IllegalArgumentException("Todos los parámetros son obligatorios");
        }
        this.provider = provider;
        this.gestorTareas = gestorTareas;
        this.manejadorHttp = manejadorHttp;
        this.gestorLogging = gestorLogging;
        this.cancelado = new AtomicBoolean(false);
        this.procesados = new AtomicInteger(0);
        this.errores = new AtomicInteger(0);
        this.observers = new CopyOnWriteArrayList<>();  // Thread-safe for concurrent access
    }
    
    /**
     * Adds an observer to receive bulk analysis progress updates.
     *
     * @param observer observer to add
     */
    public void agregarObserver(BulkProgressObserver observer) {
        if (observer != null) {
            observers.add(observer);
        }
    }
    
    /**
     * Removes an observer from receiving updates.
     *
     * @param observer observer to remove
     */
    public void removerObserver(BulkProgressObserver observer) {
        observers.remove(observer);
    }
    
    /**
     * Starts bulk analysis with the given filter criteria.
     * <p>
     * Fetches filtered history and creates analysis tasks for each item.
     * Progress updates are sent to all registered observers.
     * </p>
     *
     * @param filtro filter criteria for selecting history items
     * @param callback callback for individual analysis results
     * @return total number of items queued for analysis
     */
    public int iniciarAnalisisBulk(ProxyHistoryFilter filtro, BulkAnalysisCallback callback) {
        cancelado.set(false);
        procesados.set(0);
        errores.set(0);
        
        List<ProxyHttpRequestResponse> items = provider.obtenerHistorialFiltrado(filtro);
        
        if (Normalizador.esVacia(items)) {
            gestorLogging.info(ORIGEN_LOG, "No hay items en el historial para analizar");
            notificarCompletado(0, 0);
            return 0;
        }
        
        int total = items.size();
        gestorLogging.info(ORIGEN_LOG, "Iniciando análisis bulk de " + total + " items");
        
        for (ProxyHttpRequestResponse item : items) {
            if (cancelado.get()) {
                gestorLogging.info(ORIGEN_LOG, "Análisis bulk cancelado por usuario");
                notificarCancelado();
                break;
            }
            
            analizarItem(item, total, callback);
        }
        
        return total;
    }
    
    /**
     * Cancels ongoing bulk analysis.
     */
    public void cancelar() {
        cancelado.set(true);
        gestorLogging.info(ORIGEN_LOG, "Solicitud de cancelación recibida");
    }
    
    /**
     * Returns the number of items processed so far.
     *
     * @return number of processed items
     */
    public int obtenerProcesados() {
        return procesados.get();
    }
    
    /**
     * Returns the number of errors encountered.
     *
     * @return number of errors
     */
    public int obtenerErrores() {
        return errores.get();
    }
    
    /**
     * Checks if bulk analysis is cancelled.
     *
     * @return {@code true} if cancelled, {@code false} otherwise
     */
    public boolean estaCancelado() {
        return cancelado.get();
    }
    
    private void analizarItem(ProxyHttpRequestResponse item, int total, BulkAnalysisCallback callback) {
        try {
            HttpRequest request = item.request();
            if (request == null) {
                errores.incrementAndGet();
                if (callback != null) {
                    callback.onAnalysisError("[URL desconocida]", "Request null");
                }
                return;
            }
            
            String url = request.url();
            manejadorHttp.analizarSolicitudForzada(request);
            
            procesados.incrementAndGet();
            notificarProgreso(procesados.get(), total, url);
            
            if (callback != null) {
                callback.onAnalysisComplete(url, new ArrayList<>());
            }
            
        } catch (Exception e) {
            errores.incrementAndGet();
            String url = item.request() != null ? item.request().url() : "[URL desconocida]";
            gestorLogging.error(ORIGEN_LOG, "Error analizando item: " + url, e);
            
            if (callback != null) {
                callback.onAnalysisError(url, e.getMessage());
            }
        }
    }
    
    private void notificarProgreso(int procesados, int total, String urlActual) {
        for (BulkProgressObserver observer : observers) {
            try {
                observer.onProgress(procesados, total, urlActual);
            } catch (Exception e) {
                gestorLogging.error(ORIGEN_LOG, "Error notificando progreso a observer", e);
            }
        }
    }
    
    private void notificarCompletado(int totalAnalizados, int totalErrores) {
        for (BulkProgressObserver observer : observers) {
            try {
                observer.onComplete(totalAnalizados, totalErrores);
            } catch (Exception e) {
                gestorLogging.error(ORIGEN_LOG, "Error notificando completado a observer", e);
            }
        }
    }
    
    private void notificarCancelado() {
        for (BulkProgressObserver observer : observers) {
            try {
                observer.onCancelled();
            } catch (Exception e) {
                gestorLogging.error(ORIGEN_LOG, "Error notificando cancelación a observer", e);
            }
        }
    }
    
    /**
     * Executes bulk analysis with the given filter and progress observer.
     *
     * @param filtro filter to apply to history items
     * @param progressObserver observer to receive progress updates
     * @param callback optional callback for individual results
     */
    public void ejecutarAnalisisBulk(CompositeProxyHistoryFilter filtro, 
                                      BulkProgressObserver progressObserver,
                                      BulkAnalysisCallback callback) {
        if (filtro == null) {
            filtro = CompositeProxyHistoryFilter.builder().build();
        }
        
        if (progressObserver != null) {
            observers.add(progressObserver);
        }
        
        ejecutarAnalisis(filtro, callback);
    }
    
    /**
     * Internal method to execute bulk analysis.
     *
     * @param filtro filter to apply
     * @param callback callback for results
     */
    private void ejecutarAnalisis(CompositeProxyHistoryFilter filtro, BulkAnalysisCallback callback) {
        cancelado.set(false);
        procesados.set(0);
        errores.set(0);
        
        List<ProxyHttpRequestResponse> items = provider.obtenerHistorialFiltrado(filtro);
        int total = items.size();
        
        gestorLogging.info(ORIGEN_LOG, "Iniciando análisis bulk de " + total + " items");
        
        if (total == 0) {
            notificarCompletado(0, 0);
            return;
        }
        
        for (ProxyHttpRequestResponse item : items) {
            if (cancelado.get()) {
                notificarCancelado();
                break;
            }
            
            analizarItem(item, total, callback);
        }
        
        if (!cancelado.get()) {
            notificarCompletado(procesados.get(), errores.get());
        }
    }
}
