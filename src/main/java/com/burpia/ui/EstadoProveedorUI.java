package com.burpia.ui;

import com.burpia.util.Normalizador;

/**
 * Representa el estado temporal de los campos de un proveedor en la UI de configuración.
 * Permite mantener "borradores" cuando el usuario cambia entre proveedores sin guardar.
 *
 * <p>Esta clase es inmutable y thread-safe. Todos los campos se validan en el constructor.</p>
 *
 * @see com.burpia.ui.DialogoConfiguracion
 */
public final class EstadoProveedorUI {

    /** Valor por defecto para maxTokens según configuración de proveedores */
    public static final int MAX_TOKENS_POR_DEFECTO = 4096;

    /** Valor por defecto para timeout en segundos */
    public static final int TIMEOUT_POR_DEFECTO = 120;

    private final String apiKey;
    private final String modelo;
    private final String baseUrl;
    private final int maxTokens;
    private final int timeout;

    /**
     * Crea un nuevo estado de proveedor con los valores especificados.
     *
     * @param apiKey     API key del proveedor (puede ser null o vacío)
     * @param modelo     Nombre del modelo a utilizar (puede ser null o vacío)
     * @param baseUrl    URL base del API del proveedor (puede ser null o vacío)
     * @param maxTokens  Máximo de tokens para respuestas (debe ser positivo, usa valor por defecto si es inválido)
     * @param timeout    Timeout en segundos (debe ser positivo, usa valor por defecto si es inválido)
     */
    public EstadoProveedorUI(String apiKey, String modelo, String baseUrl, int maxTokens, int timeout) {
        // Inmutabilidad defensiva: crear copias de strings
        this.apiKey = apiKey != null ? new String(apiKey) : "";
        this.modelo = modelo != null ? new String(modelo) : "";
        this.baseUrl = baseUrl != null ? new String(baseUrl) : "";

        // Validación de rangos con valores por defecto
        this.maxTokens = maxTokens > 0 ? maxTokens : MAX_TOKENS_POR_DEFECTO;
        this.timeout = timeout > 0 ? timeout : TIMEOUT_POR_DEFECTO;
    }

    /**
     * Obtiene la API key del proveedor.
     *
     * @return API key, nunca null (puede ser string vacío)
     */
    public String obtenerApiKey() {
        return apiKey;
    }

    /**
     * Obtiene el nombre del modelo seleccionado.
     *
     * @return Nombre del modelo, nunca null (puede ser string vacío)
     */
    public String obtenerModelo() {
        return modelo;
    }

    /**
     * Obtiene la URL base del API del proveedor.
     *
     * @return URL base, nunca null (puede ser string vacío)
     */
    public String obtenerBaseUrl() {
        return baseUrl;
    }

    /**
     * Obtiene el máximo de tokens configurado.
     *
     * @return Máximo de tokens (siempre positivo)
     */
    public int obtenerMaxTokens() {
        return maxTokens;
    }

    /**
     * Obtiene el timeout en segundos.
     *
     * @return Timeout en segundos (siempre positivo)
     */
    public int obtenerTimeout() {
        return timeout;
    }

    /**
     * Verifica si la API key está configurada (no vacía).
     *
     * @return true si hay una API key configurada
     */
    public boolean tieneApiKey() {
        return Normalizador.noEsVacio(apiKey);
    }

    /**
     * Verifica si el modelo está seleccionado (no vacío).
     *
     * @return true si hay un modelo seleccionado
     */
    public boolean tieneModelo() {
        return Normalizador.noEsVacio(modelo);
    }

    /**
     * Verifica si la URL base está configurada (no vacía).
     *
     * @return true si hay una URL base configurada
     */
    public boolean tieneBaseUrl() {
        return Normalizador.noEsVacio(baseUrl);
    }

    /**
     * Representación en string para debugging.
     * La API key se sanitiza para evitar exposición en logs.
     *
     * @return String representando el estado (sin exponer API key completa)
     */
    @Override
    public String toString() {
        return String.format("EstadoProveedorUI{modelo='%s', baseUrl='%s', maxTokens=%d, timeout=%d, apiKey=%s}",
                modelo,
                baseUrl,
                maxTokens,
                timeout,
                Normalizador.sanitizarApiKey(apiKey));
    }
}
