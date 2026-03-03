package com.burpia.ui;

/**
 * Representa el estado temporal de los campos de un proveedor en la UI de configuración.
 * Permite mantener "borradores" cuando el usuario cambia entre proveedores sin guardar.
 */
public class EstadoProveedorUI {
    private final String apiKey;
    private final String modelo;
    private final String baseUrl;
    private final int maxTokens;
    private final int timeout;

    public EstadoProveedorUI(String apiKey, String modelo, String baseUrl, int maxTokens, int timeout) {
        this.apiKey = apiKey;
        this.modelo = modelo;
        this.baseUrl = baseUrl;
        this.maxTokens = maxTokens;
        this.timeout = timeout;
    }

    public String getApiKey() { return apiKey; }
    public String getModelo() { return modelo; }
    public String getBaseUrl() { return baseUrl; }
    public int getMaxTokens() { return maxTokens; }
    public int getTimeout() { return timeout; }
}
