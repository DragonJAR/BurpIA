package com.burpia.analyzer;

/**
 * Excepción lanzada cuando el prompt excede el límite de contexto del modelo.
 * 
 * <p>Esta excepción permite al llamador distinguir entre errores de API
 * recuperables (rate limiting, timeouts) y errores de contexto que pueden
 * manejarse truncando el prompt.</p>
 * 
 * @since 1.5.0
 */
public final class ContextExceededException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    private final String cuerpoError;
    private final int limiteTokens;

    /**
     * Crea una nueva excepción de contexto excedido.
     *
     * @param mensaje Mensaje descriptivo del error
     * @param cuerpoError Cuerpo de la respuesta de error del proveedor
     * @param limiteTokens Límite de tokens extraído del error (-1 si no se pudo extraer)
     */
    public ContextExceededException(String mensaje, String cuerpoError, int limiteTokens) {
        super(mensaje);
        this.cuerpoError = cuerpoError != null ? cuerpoError : "";
        this.limiteTokens = limiteTokens;
    }

    /**
     * Obtiene el cuerpo del error de la respuesta del proveedor.
     *
     * @return Cuerpo del error
     */
    public String obtenerCuerpoError() {
        return cuerpoError;
    }

    /**
     * Obtiene el límite de tokens extraído del mensaje de error.
     *
     * @return Límite de tokens, o -1 si no se pudo extraer
     */
    public int obtenerLimiteTokens() {
        return limiteTokens;
    }
}