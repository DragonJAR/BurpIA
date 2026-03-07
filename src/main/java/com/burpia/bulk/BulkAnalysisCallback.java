package com.burpia.bulk;

import com.burpia.model.Hallazgo;
import java.util.List;

/**
 * Callback interface for individual request analysis results during bulk analysis.
 * <p>
 * Receives results for each analyzed request, allowing aggregation of findings
 * and error handling at granular level.
 * </p>
 */
public interface BulkAnalysisCallback {
    
    /**
     * Called when analysis of a single request completes with findings.
     *
     * @param url URL of the analyzed request
     * @param findings list of security findings detected (may be empty)
     */
    void onAnalysisComplete(String url, List<Hallazgo> findings);
    
    /**
     * Called when analysis of a single request fails.
     *
     * @param url URL of the request that failed
     * @param error error message describing the failure
     */
    void onAnalysisError(String url, String error);
    
    /**
     * Called when analysis of a single request is cancelled.
     *
     * @param url URL of the cancelled request
     */
    void onAnalysisCancelled(String url);
}
