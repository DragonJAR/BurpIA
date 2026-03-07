package com.burpia.bulk;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.proxy.Proxy;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;
import burp.api.montoya.sitemap.SiteMap;
import com.burpia.util.GestorLoggingUnificado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HistorialBurpProvider.
 */
@DisplayName("HistorialBurpProvider Tests")
class HistorialBurpProviderTest {
    
    @Mock
    private MontoyaApi mockApi;
    
    @Mock
    private GestorLoggingUnificado mockLogger;
    
    @Mock
    private Proxy mockProxy;
    
    @Mock
    private SiteMap mockSiteMap;
    
    @Mock
    private ProxyHttpRequestResponse mockItem1;
    
    @Mock
    private ProxyHttpRequestResponse mockItem2;
    
    @Mock
    private HttpRequestResponse mockSiteMapItem;
    
    private HistorialBurpProvider provider;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockApi.proxy()).thenReturn(mockProxy);
        when(mockApi.siteMap()).thenReturn(mockSiteMap);
        doNothing().when(mockLogger).info(anyString(), anyString());
        doNothing().when(mockLogger).error(anyString(), anyString());
        doNothing().when(mockLogger).error(anyString(), anyString(), any(Throwable.class));
    }
    
    @Nested
    @DisplayName("Constructor Validation")
    class ConstructorValidation {
        
        @Test
        @DisplayName("Throws exception when api is null")
        void throwsWhenApiIsNull() {
            assertThrows(IllegalArgumentException.class, () -> 
                new HistorialBurpProvider(null, mockLogger)
            );
        }
        
        @Test
        @DisplayName("Throws exception when logger is null")
        void throwsWhenLoggerIsNull() {
            assertThrows(IllegalArgumentException.class, () -> 
                new HistorialBurpProvider(mockApi, null)
            );
        }
        
        @Test
        @DisplayName("Creates successfully with valid parameters")
        void createsWithValidParams() {
            when(mockProxy.history()).thenReturn(Collections.emptyList());
            
            assertDoesNotThrow(() -> 
                new HistorialBurpProvider(mockApi, mockLogger)
            );
        }
    }
    
    @Nested
    @DisplayName("obtenerHistorialCompleto")
    class ObtenerHistorialCompleto {
        
        @Test
        @DisplayName("Returns empty list when proxy history is null")
        void returnsEmptyWhenHistoryIsNull() {
            when(mockProxy.history()).thenReturn(null);
            provider = new HistorialBurpProvider(mockApi, mockLogger);
            
            List<ProxyHttpRequestResponse> result = provider.obtenerHistorialCompleto();
            
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(mockLogger).info(anyString(), contains("retornó null"));
        }
        
        @Test
        @DisplayName("Returns complete history when available")
        void returnsCompleteHistory() {
            List<ProxyHttpRequestResponse> historial = new ArrayList<>();
            historial.add(mockItem1);
            historial.add(mockItem2);
            when(mockProxy.history()).thenReturn(historial);
            provider = new HistorialBurpProvider(mockApi, mockLogger);
            
            List<ProxyHttpRequestResponse> result = provider.obtenerHistorialCompleto();
            
            assertNotNull(result);
            assertEquals(2, result.size());
            verify(mockLogger).info(anyString(), contains("2 items"));
        }
        
        @Test
        @DisplayName("Returns empty list on exception")
        void returnsEmptyOnException() {
            when(mockProxy.history()).thenThrow(new RuntimeException("Test error"));
            provider = new HistorialBurpProvider(mockApi, mockLogger);
            
            List<ProxyHttpRequestResponse> result = provider.obtenerHistorialCompleto();
            
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(mockLogger).error(anyString(), anyString(), any(RuntimeException.class));
        }
    }
    
    @Nested
    @DisplayName("obtenerHistorialFiltrado")
    class ObtenerHistorialFiltrado {
        
        @Test
        @DisplayName("Returns complete history when filter is null")
        void returnsCompleteWhenFilterIsNull() {
            List<ProxyHttpRequestResponse> historial = new ArrayList<>();
            historial.add(mockItem1);
            when(mockProxy.history()).thenReturn(historial);
            provider = new HistorialBurpProvider(mockApi, mockLogger);
            
            List<ProxyHttpRequestResponse> result = provider.obtenerHistorialFiltrado(null);
            
            assertNotNull(result);
            assertEquals(1, result.size());
        }
        
        @Test
        @DisplayName("Returns filtered items matching filter")
        void returnsFilteredItems() {
            List<ProxyHttpRequestResponse> historial = new ArrayList<>();
            historial.add(mockItem1);
            historial.add(mockItem2);
            when(mockProxy.history()).thenReturn(historial);
            
            ProxyHistoryFilter filtro = new ProxyHistoryFilter() {
                @Override
                public boolean matches(ProxyHttpRequestResponse item) {
                    return item == mockItem1;
                }
                
                @Override
                public String getDescription() {
                    return "Test filter";
                }
            };
            
            provider = new HistorialBurpProvider(mockApi, mockLogger);
            
            List<ProxyHttpRequestResponse> result = provider.obtenerHistorialFiltrado(filtro);
            
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(mockItem1, result.get(0));
        }
        
        @Test
        @DisplayName("Returns empty list when no items match")
        void returnsEmptyWhenNoMatch() {
            List<ProxyHttpRequestResponse> historial = new ArrayList<>();
            historial.add(mockItem1);
            when(mockProxy.history()).thenReturn(historial);
            
            ProxyHistoryFilter filtro = new ProxyHistoryFilter() {
                @Override
                public boolean matches(ProxyHttpRequestResponse item) {
                    return false;
                }
                
                @Override
                public String getDescription() {
                    return "Reject all filter";
                }
            };
            
            provider = new HistorialBurpProvider(mockApi, mockLogger);
            
            List<ProxyHttpRequestResponse> result = provider.obtenerHistorialFiltrado(filtro);
            
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }
    
    @Nested
    @DisplayName("obtenerSiteMapComoFallback")
    class ObtenerSiteMapComoFallback {
        
        @Test
        @DisplayName("Returns site map when available")
        void returnsSiteMapWhenAvailable() {
            List<HttpRequestResponse> siteMap = new ArrayList<>();
            siteMap.add(mockSiteMapItem);
            when(mockSiteMap.requestResponses()).thenReturn(siteMap);
            when(mockProxy.history()).thenReturn(Collections.emptyList());
            provider = new HistorialBurpProvider(mockApi, mockLogger);
            
            List<HttpRequestResponse> result = provider.obtenerSiteMapComoFallback();
            
            assertNotNull(result);
            assertEquals(1, result.size());
            verify(mockLogger).info(anyString(), contains("1 items"));
        }
        
        @Test
        @DisplayName("Returns empty list when site map is null")
        void returnsEmptyWhenSiteMapIsNull() {
            when(mockSiteMap.requestResponses()).thenReturn(null);
            when(mockProxy.history()).thenReturn(Collections.emptyList());
            provider = new HistorialBurpProvider(mockApi, mockLogger);
            
            List<HttpRequestResponse> result = provider.obtenerSiteMapComoFallback();
            
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }
    
    @Nested
    @DisplayName("estaProxyDisponible")
    class EstaProxyDisponible {
        
        @Test
        @DisplayName("Returns true when proxy history is available")
        void returnsTrueWhenAvailable() {
            when(mockProxy.history()).thenReturn(Collections.emptyList());
            provider = new HistorialBurpProvider(mockApi, mockLogger);
            
            assertTrue(provider.estaProxyDisponible());
        }
        
        @Test
        @DisplayName("Returns false when proxy history throws exception")
        void returnsFalseOnException() {
            when(mockProxy.history()).thenThrow(new RuntimeException("Proxy not available"));
            provider = new HistorialBurpProvider(mockApi, mockLogger);
            
            assertFalse(provider.estaProxyDisponible());
        }
    }
}
