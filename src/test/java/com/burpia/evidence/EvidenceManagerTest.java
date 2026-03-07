package com.burpia.evidence;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.scanner.audit.issues.AuditIssue;
import burp.api.montoya.sitemap.SiteMap;
import com.burpia.ExtensionBurpIA;
import com.burpia.model.Hallazgo;
import com.burpia.util.AlmacenEvidenciaHttp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class EvidenceManagerTest {
    
    @TempDir
    Path tempDir;
    
    private EvidenceManager evidenceManager;
    private MontoyaApi mockApi;
    private SiteMap mockSiteMap;
    private HttpRequestResponse mockEvidence;
    private HttpRequest mockRequest;
    private HttpResponse mockResponse;
    private Hallazgo mockHallazgo;
    
    @BeforeEach
    void setUp() {
        mockApi = mock(MontoyaApi.class);
        mockSiteMap = mock(SiteMap.class);
        mockEvidence = mock(HttpRequestResponse.class);
        mockRequest = mock(HttpRequest.class);
        mockResponse = mock(HttpResponse.class);
        mockHallazgo = mock(Hallazgo.class);
        
        when(mockApi.siteMap()).thenReturn(mockSiteMap);
        when(mockEvidence.request()).thenReturn(mockRequest);
        when(mockEvidence.response()).thenReturn(mockResponse);
        
        try (MockedStatic<ExtensionBurpIA> mockedExtension = mockStatic(ExtensionBurpIA.class)) {
            mockedExtension.when(() -> ExtensionBurpIA.esBurpProfessional(any())).thenReturn(true);
            evidenceManager = new EvidenceManager(mockApi);
        }
    }
    
    @Test
    void almacenarEvidencia_conEvidenciaValida_deberiaRetornarId() {
        ByteArray mockByteArray = mock(ByteArray.class);
        when(mockByteArray.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(mockRequest.toByteArray()).thenReturn(mockByteArray);
        when(mockResponse.toByteArray()).thenReturn(mockByteArray);
        
        String evidenciaId = evidenceManager.almacenarEvidencia(mockEvidence);
        
        assertNotNull(evidenciaId);
        assertFalse(evidenciaId.isEmpty());
    }
    
    @Test
    void almacenarEvidencia_conEvidenciaNula_deberiaRetornarNulo() {
        String evidenciaId = evidenceManager.almacenarEvidencia(null);
        
        assertNull(evidenciaId);
    }
    
    @Test
    void obtenerEvidencia_conIdValido_deberiaRetornarEvidencia() {
        ByteArray mockByteArray = mock(ByteArray.class);
        when(mockByteArray.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(mockRequest.toByteArray()).thenReturn(mockByteArray);
        when(mockResponse.toByteArray()).thenReturn(mockByteArray);
        
        String evidenciaId = evidenceManager.almacenarEvidencia(mockEvidence);
        HttpRequestResponse resultado = evidenceManager.obtenerEvidencia(evidenciaId);
        
        assertNotNull(resultado);
    }
    
    @Test
    void obtenerEvidencia_conIdInvalido_deberiaRetornarNulo() {
        HttpRequestResponse resultado = evidenceManager.obtenerEvidencia("id-invalido");
        
        assertNull(resultado);
    }
    
    @Test
    void obtenerEvidencia_conIdNulo_deberiaRetornarNulo() {
        HttpRequestResponse resultado = evidenceManager.obtenerEvidencia(null);
        
        assertNull(resultado);
    }
    
    @Test
    void eliminarEvidencia_conIdValido_deberiaEliminarEvidencia() {
        ByteArray mockByteArray = mock(ByteArray.class);
        when(mockByteArray.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(mockRequest.toByteArray()).thenReturn(mockByteArray);
        when(mockResponse.toByteArray()).thenReturn(mockByteArray);
        
        String evidenciaId = evidenceManager.almacenarEvidencia(mockEvidence);
        evidenceManager.eliminarEvidencia(evidenciaId);
        
        HttpRequestResponse resultado = evidenceManager.obtenerEvidencia(evidenciaId);
        
        assertNull(resultado);
    }
    
    @Test
    void eliminarEvidencia_conIdInvalido_noDeberiaLanzarExcepcion() {
        assertDoesNotThrow(() -> {
            evidenceManager.eliminarEvidencia("id-invalido");
        });
    }
    
    @Test
    void guardarHallazgoComoIssue_conBurpProfessional_deberiaGuardar() {
        ByteArray mockByteArray = mock(ByteArray.class);
        when(mockByteArray.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(mockRequest.toByteArray()).thenReturn(mockByteArray);
        when(mockResponse.toByteArray()).thenReturn(mockByteArray);
        when(mockHallazgo.obtenerTitulo()).thenReturn("Test Finding");
        when(mockHallazgo.obtenerHallazgo()).thenReturn("Test description");
        when(mockHallazgo.obtenerUrl()).thenReturn("http://example.com");
        when(mockHallazgo.obtenerSeveridad()).thenReturn(Hallazgo.SEVERIDAD_HIGH);
        when(mockHallazgo.obtenerConfianza()).thenReturn(Hallazgo.CONFIANZA_ALTA);
        when(mockHallazgo.obtenerEvidenciaHttp()).thenReturn(null);
        when(mockHallazgo.obtenerEvidenciaId()).thenReturn(null);
        
        try (MockedStatic<ExtensionBurpIA> mockedExtension = mockStatic(ExtensionBurpIA.class)) {
            mockedExtension.when(() -> ExtensionBurpIA.esBurpProfessional(any())).thenReturn(true);
            mockedExtension.when(() -> ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(any(), any(), any()))
                          .thenReturn(true);
            
            EvidenceManager evidenceManagerPro = new EvidenceManager(mockApi);
            String evidenciaId = evidenceManagerPro.almacenarEvidencia(mockEvidence);
            assertTrue(evidenceManagerPro.guardarHallazgoComoIssue(mockApi, mockHallazgo, evidenciaId));
            
            mockedExtension.verify(() -> ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(
                any(MontoyaApi.class), any(Hallazgo.class), any(HttpRequestResponse.class))
            );
        }
    }
    
    @Test
    void guardarHallazgoComoIssue_conBurpCommunity_deberiaRetornarFalso() {
        MontoyaApi mockApiCommunity = mock(MontoyaApi.class);
        when(mockApiCommunity.siteMap()).thenReturn(null);
        
        EvidenceManager evidenceManagerCommunity = new EvidenceManager(mockApiCommunity);
        
        boolean resultado = evidenceManagerCommunity.guardarHallazgoComoIssue(mockApiCommunity, mockHallazgo, "test-id");
        
        assertFalse(resultado);
    }
    
    @Test
    void guardarHallazgosComoIssues_conListaValida_deberiaGuardarTodos() {
        ByteArray mockByteArray = mock(ByteArray.class);
        when(mockByteArray.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(mockRequest.toByteArray()).thenReturn(mockByteArray);
        when(mockResponse.toByteArray()).thenReturn(mockByteArray);
        
        try (MockedStatic<ExtensionBurpIA> mockedExtension = mockStatic(ExtensionBurpIA.class)) {
            mockedExtension.when(() -> ExtensionBurpIA.esBurpProfessional(any())).thenReturn(true);
            mockedExtension.when(() -> ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(any(), any(), any()))
                          .thenReturn(true);
            
            EvidenceManager evidenceManagerPro = new EvidenceManager(mockApi);
            String evidenciaId = evidenceManagerPro.almacenarEvidencia(mockEvidence);
            
            Hallazgo hallazgo1 = mock(Hallazgo.class);
            Hallazgo hallazgo2 = mock(Hallazgo.class);
            
            when(hallazgo1.obtenerTitulo()).thenReturn("Finding 1");
            when(hallazgo1.obtenerHallazgo()).thenReturn("Description 1");
            when(hallazgo1.obtenerUrl()).thenReturn("http://example.com/1");
            when(hallazgo1.obtenerSeveridad()).thenReturn(Hallazgo.SEVERIDAD_HIGH);
            when(hallazgo1.obtenerConfianza()).thenReturn(Hallazgo.CONFIANZA_ALTA);
            when(hallazgo1.obtenerEvidenciaHttp()).thenReturn(null);
            when(hallazgo1.obtenerEvidenciaId()).thenReturn(evidenciaId);
            
            when(hallazgo2.obtenerTitulo()).thenReturn("Finding 2");
            when(hallazgo2.obtenerHallazgo()).thenReturn("Description 2");
            when(hallazgo2.obtenerUrl()).thenReturn("http://example.com/2");
            when(hallazgo2.obtenerSeveridad()).thenReturn(Hallazgo.SEVERIDAD_MEDIUM);
            when(hallazgo2.obtenerConfianza()).thenReturn(Hallazgo.CONFIANZA_MEDIA);
            when(hallazgo2.obtenerEvidenciaHttp()).thenReturn(null);
            when(hallazgo2.obtenerEvidenciaId()).thenReturn(evidenciaId);
            
            evidenceManagerPro.guardarHallazgosComoIssues(mockApi, Arrays.asList(hallazgo1, hallazgo2));
            
            mockedExtension.verify(() -> ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(
                any(MontoyaApi.class), any(Hallazgo.class), any(HttpRequestResponse.class)), times(2)
            );
        }
    }
    
    @Test
    void limpiarEvidenciasAntiguas_noDeberiaLanzarExcepcion() {
        assertDoesNotThrow(() -> {
            evidenceManager.limpiarEvidenciasAntiguas();
        });
    }
    
    @Test
    void obtenerContadorEvidencias_deberiaRetornarContadorCorrecto() {
        ByteArray mockByteArray = mock(ByteArray.class);
        when(mockByteArray.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(mockRequest.toByteArray()).thenReturn(mockByteArray);
        when(mockResponse.toByteArray()).thenReturn(mockByteArray);
        
        assertEquals(0, evidenceManager.obtenerContadorEvidencias());
        
        evidenceManager.almacenarEvidencia(mockEvidence);
        assertEquals(1, evidenceManager.obtenerContadorEvidencias());
        
        String id = evidenceManager.almacenarEvidencia(mockEvidence);
        assertEquals(2, evidenceManager.obtenerContadorEvidencias());
        
        evidenceManager.eliminarEvidencia(id);
        assertEquals(1, evidenceManager.obtenerContadorEvidencias());
    }
}