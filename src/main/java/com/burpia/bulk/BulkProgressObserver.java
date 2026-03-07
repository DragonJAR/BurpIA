package com.burpia.bulk;

/**
 * Observer interface for bulk analysis progress updates.
 * <p>
 * Implementations receive real-time notifications about bulk analysis progress,
 * including processed count, current URL, completion, errors, and cancellation.
 * </p>
 */
public interface BulkProgressObserver {
    
    /**
     * Called when progress is made in bulk analysis.
     *
     * @param processed number of requests processed so far
     * @param total total number of requests to process
     * @param currentUrl URL currently being analyzed (may be empty if between requests)
     */
    void onProgress(int processed, int total, String currentUrl);
    
    /**
     * Called when bulk analysis completes successfully.
     *
     * @param totalAnalyzed total number of requests analyzed
     * @param totalErrors total number of errors encountered
     */
    void onComplete(int totalAnalyzed, int totalErrors);
    
    /**
     * Called when a critical error stops bulk analysis.
     *
     * @param error error message describing what went wrong
     */
    void onError(String error);
    
    /**
     * Called when bulk analysis is cancelled by user.
     */
    void onCancelled();
}
