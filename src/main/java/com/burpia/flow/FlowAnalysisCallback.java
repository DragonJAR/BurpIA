package com.burpia.flow;

import com.burpia.model.Hallazgo;

import java.util.List;

/**
 * Callback para recibir notificaciones del análisis de flujo.
 */
public interface FlowAnalysisCallback {
    
    /**
     * Called when flow analysis completes successfully.
     *
     * @param hallazgos List of findings detected in the flow
     * @param urlsFlujo URLs that were part of the analyzed flow
     */
    void onComplete(List<Hallazgo> hallazgos, List<String> urlsFlujo);
    
    /**
     * Called when flow analysis encounters an error.
     *
     * @param error Error message describing the failure
     */
    void onError(String error);
    
    /**
     * Called when flow analysis is cancelled by user.
     */
    default void onCancelled() {
    }
}
