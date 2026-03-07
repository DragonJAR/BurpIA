package com.burpia.bulk;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.Normalizador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe provider for accessing Burp Suite's proxy history via Montoya API.
 * <p>
 * Provides programmatic access to HTTP request/response history with optional filtering.
 * Falls back to site map if proxy history is unavailable (e.g., Burp Community Edition).
 * </p>
 */
public class HistorialBurpProvider {
    
    private static final String ORIGEN_LOG = "HistorialBurpProvider";
    
    private final MontoyaApi api;
    private final GestorLoggingUnificado gestorLogging;
    private final ReentrantLock candado;
    private volatile boolean proxyDisponible;
    
    /**
     * Creates a new HistorialBurpProvider with the given Montoya API instance.
     *
     * @param api Montoya API instance from Burp Suite
     * @param gestorLogging unified logger for operation logging
     * @throws IllegalArgumentException if api or gestorLogging is null
     */
    public HistorialBurpProvider(MontoyaApi api, GestorLoggingUnificado gestorLogging) {
        if (api == null) {
            throw new IllegalArgumentException("MontoyaApi no puede ser null");
        }
        if (gestorLogging == null) {
            throw new IllegalArgumentException("GestorLoggingUnificado no puede ser null");
        }
        this.api = api;
        this.gestorLogging = gestorLogging;
        this.candado = new ReentrantLock();
        this.proxyDisponible = verificarDisponibilidadProxy();
    }
    
    /**
     * Retrieves complete proxy history without filtering.
     * <p>
     * Returns all items in the proxy history, or falls back to site map if
     * proxy history is unavailable.
     * </p>
     *
     * @return list of proxy history items, or empty list if none available or on error
     */
    public List<ProxyHttpRequestResponse> obtenerHistorialCompleto() {
        candado.lock();
        try {
            if (!proxyDisponible) {
                gestorLogging.info(ORIGEN_LOG, "Proxy history no disponible, usando fallback a site map");
                return Collections.emptyList();
            }
            
            List<ProxyHttpRequestResponse> historial = api.proxy().history();
            if (historial == null) {
                gestorLogging.info(ORIGEN_LOG, "Proxy history retornó null, retornando lista vacía");
                return Collections.emptyList();
            }
            
            gestorLogging.info(ORIGEN_LOG, "Historial obtenido: " + historial.size() + " items");
            return new ArrayList<>(historial);
            
        } catch (Exception e) {
            gestorLogging.error(ORIGEN_LOG, "Error al obtener historial del proxy", e);
            return Collections.emptyList();
        } finally {
            candado.unlock();
        }
    }
    
    /**
     * Retrieves proxy history filtered by the given filter criteria.
     * <p>
     * Applies filtering on Burp's side when possible, otherwise filters in-memory.
     * Returns only items that match the filter's criteria.
     * </p>
     *
     * @param filtro filter to apply to proxy history items
     * @return list of filtered proxy history items, or empty list if none match or on error
     */
    public List<ProxyHttpRequestResponse> obtenerHistorialFiltrado(ProxyHistoryFilter filtro) {
        if (filtro == null) {
            gestorLogging.info(ORIGEN_LOG, "Filtro es null, retornando historial completo");
            return obtenerHistorialCompleto();
        }
        
        List<ProxyHttpRequestResponse> historialCompleto = obtenerHistorialCompleto();
        if (Normalizador.esVacia(historialCompleto)) {
            return Collections.emptyList();
        }
        
        candado.lock();
        try {
            List<ProxyHttpRequestResponse> filtrados = new ArrayList<>();
            for (ProxyHttpRequestResponse item : historialCompleto) {
                if (item != null && filtro.matches(item)) {
                    filtrados.add(item);
                }
            }
            
            gestorLogging.info(ORIGEN_LOG, 
                "Historial filtrado: " + filtrados.size() + "/" + historialCompleto.size() + 
                " items coinciden con filtro: " + filtro.getDescription());
            
            return filtrados;
            
        } catch (Exception e) {
            gestorLogging.error(ORIGEN_LOG, "Error al filtrar historial", e);
            return Collections.emptyList();
        } finally {
            candado.unlock();
        }
    }
    
    /**
     * Retrieves site map as fallback when proxy history is unavailable.
     * <p>
     * Used primarily for Burp Community Edition which may not have proxy history.
     * </p>
     *
     * @return list of HTTP request/responses from site map, or empty list on error
     */
    public List<HttpRequestResponse> obtenerSiteMapComoFallback() {
        candado.lock();
        try {
            List<HttpRequestResponse> siteMap = api.siteMap().requestResponses();
            if (siteMap == null) {
                gestorLogging.info(ORIGEN_LOG, "Site map retornó null, retornando lista vacía");
                return Collections.emptyList();
            }
            
            gestorLogging.info(ORIGEN_LOG, "Site map obtenido como fallback: " + siteMap.size() + " items");
            return new ArrayList<>(siteMap);
            
        } catch (Exception e) {
            gestorLogging.error(ORIGEN_LOG, "Error al obtener site map", e);
            return Collections.emptyList();
        } finally {
            candado.unlock();
        }
    }
    
    /**
     * Checks if proxy history is available in the current Burp Suite edition.
     *
     * @return {@code true} if proxy history is available, {@code false} otherwise
     */
    public boolean estaProxyDisponible() {
        return proxyDisponible;
    }
    
    /**
     * Verifies if proxy history API is available and accessible.
     *
     * @return {@code true} if proxy API is available, {@code false} otherwise
     */
    private boolean verificarDisponibilidadProxy() {
        try {
            api.proxy().history();
            return true;
        } catch (Exception e) {
            gestorLogging.error(ORIGEN_LOG, "Proxy history no disponible en esta edición de Burp", e);
            return false;
        }
    }
}
