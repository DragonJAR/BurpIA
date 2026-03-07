package com.burpia.bulk;

import burp.api.montoya.proxy.ProxyHttpRequestResponse;

/**
 * Filter interface for selecting items from Burp Suite's proxy history.
 * <p>
 * Implementations define criteria for matching proxy history items,
 * enabling flexible filtering strategies (by host, path, method, status code, etc.)
 * </p>
 */
public interface ProxyHistoryFilter {
    
    /**
     * Tests if a proxy history item matches this filter's criteria.
     *
     * @param item the proxy history item to test
     * @return {@code true} if the item matches, {@code false} otherwise
     */
    boolean matches(ProxyHttpRequestResponse item);
    
    /**
     * Returns a human-readable description of this filter's criteria.
     * <p>
     * Used for logging and UI display purposes.
     * </p>
     *
     * @return description of filter criteria
     */
    String getDescription();
}
