package com.burpia.bulk;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("CompositeProxyHistoryFilter Tests")
class CompositeProxyHistoryFilterTest {
    
    @Mock private ProxyHttpRequestResponse mockItem;
    @Mock private HttpRequest mockRequest;
    @Mock private HttpResponse mockResponse;
    @Mock private burp.api.montoya.http.HttpService mockService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockItem.request()).thenReturn(mockRequest);
        when(mockItem.response()).thenReturn(mockResponse);
        when(mockRequest.httpService()).thenReturn(mockService);
    }
    
    @Nested
    @DisplayName("Builder Pattern")
    class BuilderPattern {
        
        @Test
        @DisplayName("Creates empty filter")
        void createsEmptyFilter() {
            CompositeProxyHistoryFilter filter = CompositeProxyHistoryFilter.builder().build();
            assertTrue(filter.matches(mockItem));
            assertEquals("Sin filtros", filter.getDescription());
        }
        
        @Test
        @DisplayName("Chains multiple criteria")
        void chainsCriteria() {
            when(mockRequest.method()).thenReturn("POST");
            when(mockResponse.statusCode()).thenReturn((short) 200);
            
            CompositeProxyHistoryFilter filter = CompositeProxyHistoryFilter.builder()
                .metodo("POST")
                .codigoEstado(200)
                .build();
            
            assertTrue(filter.matches(mockItem));
            assertTrue(filter.getDescription().contains("método=POST"));
            assertTrue(filter.getDescription().contains("status=200"));
        }
    }
    
    @Nested
    @DisplayName("Method Filter")
    class MethodFilter {
        
        @Test
        @DisplayName("Matches exact method")
        void matchesExactMethod() {
            when(mockRequest.method()).thenReturn("POST");
            
            CompositeProxyHistoryFilter filter = CompositeProxyHistoryFilter.builder()
                .metodo("POST")
                .build();
            
            assertTrue(filter.matches(mockItem));
        }
        
        @Test
        @DisplayName("Rejects different method")
        void rejectsDifferentMethod() {
            when(mockRequest.method()).thenReturn("GET");
            
            CompositeProxyHistoryFilter filter = CompositeProxyHistoryFilter.builder()
                .metodo("POST")
                .build();
            
            assertFalse(filter.matches(mockItem));
        }
    }
    
    @Nested
    @DisplayName("Host Pattern Filter")
    class HostPatternFilter {
        
        @Test
        @DisplayName("Matches host regex pattern")
        void matchesHostPattern() {
            when(mockService.host()).thenReturn("api.example.com");
            
            CompositeProxyHistoryFilter filter = CompositeProxyHistoryFilter.builder()
                .patronHost(".*\\.example\\.com")
                .build();
            
            assertTrue(filter.matches(mockItem));
        }
        
        @Test
        @DisplayName("Rejects non-matching host")
        void rejectsNonMatchingHost() {
            when(mockService.host()).thenReturn("other.com");
            
            CompositeProxyHistoryFilter filter = CompositeProxyHistoryFilter.builder()
                .patronHost(".*\\.example\\.com")
                .build();
            
            assertFalse(filter.matches(mockItem));
        }
    }
    
    @Nested
    @DisplayName("Status Code Filter")
    class StatusCodeFilter {
        
        @Test
        @DisplayName("Matches exact status code")
        void matchesExactCode() {
            when(mockResponse.statusCode()).thenReturn((short) 404);
            
            CompositeProxyHistoryFilter filter = CompositeProxyHistoryFilter.builder()
                .codigoEstado(404)
                .build();
            
            assertTrue(filter.matches(mockItem));
        }
        
        @Test
        @DisplayName("Matches status code range")
        void matchesRange() {
            when(mockResponse.statusCode()).thenReturn((short) 403);
            
            CompositeProxyHistoryFilter filter = CompositeProxyHistoryFilter.builder()
                .rangoCodigoEstado(400, 499)
                .build();
            
            assertTrue(filter.matches(mockItem));
        }
        
        @Test
        @DisplayName("Rejects code outside range")
        void rejectsOutsideRange() {
            when(mockResponse.statusCode()).thenReturn((short) 200);
            
            CompositeProxyHistoryFilter filter = CompositeProxyHistoryFilter.builder()
                .rangoCodigoEstado(400, 499)
                .build();
            
            assertFalse(filter.matches(mockItem));
        }
    }
    
    @Nested
    @DisplayName("Content Type Filter")
    class ContentTypeFilter {
        
        @Test
        @DisplayName("Matches content type")
        void matchesContentType() {
            when(mockResponse.headerValue("Content-Type")).thenReturn("application/json; charset=utf-8");
            
            CompositeProxyHistoryFilter filter = CompositeProxyHistoryFilter.builder()
                .tipoContenido("application/json")
                .build();
            
            assertTrue(filter.matches(mockItem));
        }
        
        @Test
        @DisplayName("Rejects different content type")
        void rejectsDifferentContentType() {
            when(mockResponse.headerValue("Content-Type")).thenReturn("text/html");
            
            CompositeProxyHistoryFilter filter = CompositeProxyHistoryFilter.builder()
                .tipoContenido("application/json")
                .build();
            
            assertFalse(filter.matches(mockItem));
        }
    }
    
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {
        
        @Test
        @DisplayName("Handles null item")
        void handlesNullItem() {
            CompositeProxyHistoryFilter filter = CompositeProxyHistoryFilter.builder()
                .metodo("GET")
                .build();
            
            assertFalse(filter.matches(null));
        }
        
        @Test
        @DisplayName("Handles null request")
        void handlesNullRequest() {
            when(mockItem.request()).thenReturn(null);
            
            CompositeProxyHistoryFilter filter = CompositeProxyHistoryFilter.builder()
                .metodo("GET")
                .build();
            
            assertFalse(filter.matches(mockItem));
        }
        
        @Test
        @DisplayName("Handles null response for status code filter")
        void handlesNullResponseForStatus() {
            when(mockItem.response()).thenReturn(null);
            
            CompositeProxyHistoryFilter filter = CompositeProxyHistoryFilter.builder()
                .codigoEstado(200)
                .build();
            
            assertFalse(filter.matches(mockItem));
        }
    }
}
