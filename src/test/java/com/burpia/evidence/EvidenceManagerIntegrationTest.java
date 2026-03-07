package com.burpia.evidence;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.sitemap.SiteMap;
import com.burpia.ExtensionBurpIA;
import com.burpia.model.Hallazgo;
import com.burpia.ui.PanelHallazgos;
import com.burpia.ui.PestaniaPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class EvidenceManagerIntegrationTest {
    
    @TempDir
    Path tempDir;
    
    private EvidenceManager evidenceManager;
    private MontoyaApi mockApi;
    private SiteMap mockSiteMap;
    private PestaniaPrincipal mockPestaniaPrincipal;
    private PanelHallazgos mockPanelHallazgos;
    
    @BeforeEach
    void setUp() {
        mockApi = mock(MontoyaApi.class);
        mockSiteMap = mock(SiteMap.class);
        mockPestaniaPrincipal = mock(PestaniaPrincipal.class);
        mockPanelHallazgos = mock(PanelHallazgos.class);
        
        when(mockApi.siteMap()).thenReturn(mockSiteMap);
        when(mockPestaniaPrincipal.obtenerPanelHallazgos()).thenReturn(mockPanelHallazgos);
        
        evidenceManager = new EvidenceManager(mockApi);
    }
    
    @Test
    void flujoCompleto_deberiaFuncionarCorrectamente() throws Exception {
        HttpRequestResponse mockEvidence = mock(HttpRequestResponse.class);
        HttpRequest mockRequest = mock(HttpRequest.class);
        HttpResponse mockResponse = mock(HttpResponse.class);
        ByteArray mockByteArray = mock(ByteArray.class);
        
        when(mockByteArray.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(mockEvidence.request()).thenReturn(mockRequest);
        when(mockEvidence.response()).thenReturn(mockResponse);
        when(mockRequest.toByteArray()).thenReturn(mockByteArray);
        when(mockResponse.toByteArray()).thenReturn(mockByteArray);
        
        try (MockedStatic<ExtensionBurpIA> mockedExtension = mockStatic(ExtensionBurpIA.class)) {
            mockedExtension.when(() -> ExtensionBurpIA.esBurpProfessional(any())).thenReturn(true);
            mockedExtension.when(() -> ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(any(), any(), any()))
                          .thenReturn(true);
            
            EvidenceManager evidenceManagerPro = new EvidenceManager(mockApi);
            
            String evidenciaId = evidenceManagerPro.almacenarEvidencia(mockEvidence);
            assertNotNull(evidenciaId);
            
            HttpRequestResponse evidenciaRecuperada = evidenceManagerPro.obtenerEvidencia(evidenciaId);
            assertNotNull(evidenciaRecuperada);
            assertEquals(mockRequest, evidenciaRecuperada.request());
            assertEquals(mockResponse, evidenciaRecuperada.response());
            
            Hallazgo hallazgo = new Hallazgo(
                "http://example.com/test",
                "Test Finding",
                "Test description",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA,
                mockRequest
            ).conEvidenciaId(evidenciaId);
            
            boolean guardado = evidenceManagerPro.guardarHallazgoComoIssue(mockApi, hallazgo, evidenciaId);
            assertTrue(guardado);
            
            mockedExtension.verify(() -> ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(
                eq(mockApi), eq(hallazgo), any(HttpRequestResponse.class))
            );
            
            evidenceManagerPro.eliminarEvidencia(evidenciaId);
            HttpRequestResponse evidenciaEliminada = evidenceManagerPro.obtenerEvidencia(evidenciaId);
            assertNull(evidenciaEliminada);
        }
    }
    
    @Test
    void gestionMultiplesHallazgos_deberiaProcesarCorrectamente() throws Exception {
        HttpRequestResponse mockEvidence = mock(HttpRequestResponse.class);
        HttpRequest mockRequest = mock(HttpRequest.class);
        HttpResponse mockResponse = mock(HttpResponse.class);
        ByteArray mockByteArray = mock(ByteArray.class);
        
        when(mockByteArray.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(mockEvidence.request()).thenReturn(mockRequest);
        when(mockEvidence.response()).thenReturn(mockResponse);
        when(mockRequest.toByteArray()).thenReturn(mockByteArray);
        when(mockResponse.toByteArray()).thenReturn(mockByteArray);
        
        try (MockedStatic<ExtensionBurpIA> mockedExtension = mockStatic(ExtensionBurpIA.class)) {
            mockedExtension.when(() -> ExtensionBurpIA.esBurpProfessional(any())).thenReturn(true);
            mockedExtension.when(() -> ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(any(), any(), any()))
                          .thenReturn(true);
            
            EvidenceManager evidenceManagerPro = new EvidenceManager(mockApi);
            String evidenciaId = evidenceManagerPro.almacenarEvidencia(mockEvidence);
        
            List<Hallazgo> hallazgos = Arrays.asList(
                new Hallazgo("http://example.com/1", "Finding 1", "Description 1", 
                             Hallazgo.SEVERIDAD_CRITICAL, Hallazgo.CONFIANZA_ALTA, mockRequest)
                             .conEvidenciaId(evidenciaId),
                new Hallazgo("http://example.com/2", "Finding 2", "Description 2", 
                             Hallazgo.SEVERIDAD_MEDIUM, Hallazgo.CONFIANZA_MEDIA, mockRequest)
                             .conEvidenciaId(evidenciaId),
                new Hallazgo("http://example.com/3", "Finding 3", "Description 3", 
                             Hallazgo.SEVERIDAD_LOW, Hallazgo.CONFIANZA_BAJA, mockRequest)
                             .conEvidenciaId(evidenciaId)
            );
            
            evidenceManagerPro.guardarHallazgosComoIssues(mockApi, hallazgos);
            
            mockedExtension.verify(() -> ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(
                eq(mockApi), any(Hallazgo.class), any(HttpRequestResponse.class)), times(3)
            );
        }
    }
    
    @Test
    void limpiarEvidencias_deberiaMantenerConsistencia() throws Exception {
        HttpRequestResponse mockEvidence = mock(HttpRequestResponse.class);
        HttpRequest mockRequest = mock(HttpRequest.class);
        HttpResponse mockResponse = mock(HttpResponse.class);
        ByteArray mockByteArray = mock(ByteArray.class);
        
        when(mockByteArray.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(mockEvidence.request()).thenReturn(mockRequest);
        when(mockEvidence.response()).thenReturn(mockResponse);
        when(mockRequest.toByteArray()).thenReturn(mockByteArray);
        when(mockResponse.toByteArray()).thenReturn(mockByteArray);
        
        String id1 = evidenceManager.almacenarEvidencia(mockEvidence);
        String id2 = evidenceManager.almacenarEvidencia(mockEvidence);
        String id3 = evidenceManager.almacenarEvidencia(mockEvidence);
        
        assertEquals(3, evidenceManager.obtenerContadorEvidencias());
        
        evidenceManager.eliminarEvidencia(id2);
        assertEquals(2, evidenceManager.obtenerContadorEvidencias());
        assertNull(evidenceManager.obtenerEvidencia(id2));
        
        evidenceManager.limpiarEvidenciasAntiguas();
        assertEquals(2, evidenceManager.obtenerContadorEvidencias());
    }
}