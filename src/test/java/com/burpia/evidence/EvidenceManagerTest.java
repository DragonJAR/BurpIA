package com.burpia.evidence;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.sitemap.SiteMap;
import com.burpia.ExtensionBurpIA;
import com.burpia.model.Hallazgo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class EvidenceManagerTest {

    private MontoyaApi mockApi;
    private SiteMap mockSiteMap;
    private Hallazgo mockHallazgo;

    @BeforeEach
    void setUp() {
        mockApi = mock(MontoyaApi.class);
        mockSiteMap = mock(SiteMap.class);
        mockHallazgo = mock(Hallazgo.class);

        when(mockApi.siteMap()).thenReturn(mockSiteMap);
    }

    @Test
    void guardarHallazgoComoIssue_conBurpProfessional_deberiaGuardar() {
        when(mockHallazgo.obtenerTitulo()).thenReturn("Test Finding");
        when(mockHallazgo.obtenerHallazgo()).thenReturn("Test description");
        when(mockHallazgo.obtenerUrl()).thenReturn("http://example.com");
        when(mockHallazgo.obtenerSeveridad()).thenReturn(Hallazgo.SEVERIDAD_HIGH);
        when(mockHallazgo.obtenerConfianza()).thenReturn(Hallazgo.CONFIANZA_ALTA);

        try (MockedStatic<ExtensionBurpIA> mockedExtension = mockStatic(ExtensionBurpIA.class)) {
            mockedExtension.when(() -> ExtensionBurpIA.esBurpProfessional(any())).thenReturn(true);
            mockedExtension.when(() -> ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(any(), any()))
                          .thenReturn(true);

            EvidenceManager evidenceManagerPro = new EvidenceManager(mockApi);
            assertTrue(evidenceManagerPro.guardarHallazgoComoIssue(mockApi, mockHallazgo));

            mockedExtension.verify(() -> ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(
                any(MontoyaApi.class), any(Hallazgo.class))
            );
        }
    }

    @Test
    void guardarHallazgoComoIssue_sinEvidencia_igualGuarda() {
        when(mockHallazgo.obtenerSolicitudHttp()).thenReturn(null);

        try (MockedStatic<ExtensionBurpIA> mockedExtension = mockStatic(ExtensionBurpIA.class)) {
            mockedExtension.when(() -> ExtensionBurpIA.esBurpProfessional(any())).thenReturn(true);
            mockedExtension.when(() -> ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(any(), any()))
                          .thenReturn(true);

            EvidenceManager evidenceManagerPro = new EvidenceManager(mockApi);
            // Sin evidencia HTTP resoluble: debe enviarse igual (la evidencia es opcional).
            assertTrue(evidenceManagerPro.guardarHallazgoComoIssue(mockApi, mockHallazgo));

            mockedExtension.verify(() -> ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(
                any(MontoyaApi.class), any(Hallazgo.class)));
        }
    }

    @Test
    void guardarHallazgoComoIssue_rutaIssues_logueaPorApiLogging() {
        // Fix A: el logger de EvidenceManager ahora se construye con el api real,
        // así que la ruta de issues escribe a Burp (Extensions -> Output) en vez de
        // descartarse silenciosamente con crearMinimal(null,null).
        Logging mockLogging = mock(Logging.class);
        when(mockApi.logging()).thenReturn(mockLogging);
        when(mockHallazgo.obtenerTitulo()).thenReturn("Test Finding");

        try (MockedStatic<ExtensionBurpIA> mockedExtension = mockStatic(ExtensionBurpIA.class)) {
            mockedExtension.when(() -> ExtensionBurpIA.esBurpProfessional(any())).thenReturn(true);
            mockedExtension.when(() -> ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(any(), any()))
                          .thenReturn(true);

            EvidenceManager evidenceManagerPro = new EvidenceManager(mockApi);
            assertTrue(evidenceManagerPro.guardarHallazgoComoIssue(mockApi, mockHallazgo));

            verify(mockLogging, atLeastOnce()).logToOutput(any());
        }
    }

    @Test
    void guardarHallazgoComoIssue_conBurpCommunity_deberiaRetornarFalso() {
        MontoyaApi mockApiCommunity = mock(MontoyaApi.class);
        when(mockApiCommunity.siteMap()).thenReturn(null);

        EvidenceManager evidenceManagerCommunity = new EvidenceManager(mockApiCommunity);

        boolean resultado = evidenceManagerCommunity.guardarHallazgoComoIssue(mockApiCommunity, mockHallazgo);

        assertFalse(resultado);
    }

    @Test
    void guardarHallazgosComoIssues_conListaValida_deberiaGuardarTodos() {
        try (MockedStatic<ExtensionBurpIA> mockedExtension = mockStatic(ExtensionBurpIA.class)) {
            mockedExtension.when(() -> ExtensionBurpIA.esBurpProfessional(any())).thenReturn(true);
            mockedExtension.when(() -> ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(any(), any()))
                          .thenReturn(true);

            EvidenceManager evidenceManagerPro = new EvidenceManager(mockApi);

            Hallazgo hallazgo1 = mock(Hallazgo.class);
            Hallazgo hallazgo2 = mock(Hallazgo.class);

            when(hallazgo1.obtenerTitulo()).thenReturn("Finding 1");
            when(hallazgo1.obtenerHallazgo()).thenReturn("Description 1");
            when(hallazgo1.obtenerUrl()).thenReturn("http://example.com/1");
            when(hallazgo1.obtenerSeveridad()).thenReturn(Hallazgo.SEVERIDAD_HIGH);
            when(hallazgo1.obtenerConfianza()).thenReturn(Hallazgo.CONFIANZA_ALTA);

            when(hallazgo2.obtenerTitulo()).thenReturn("Finding 2");
            when(hallazgo2.obtenerHallazgo()).thenReturn("Description 2");
            when(hallazgo2.obtenerUrl()).thenReturn("http://example.com/2");
            when(hallazgo2.obtenerSeveridad()).thenReturn(Hallazgo.SEVERIDAD_MEDIUM);
            when(hallazgo2.obtenerConfianza()).thenReturn(Hallazgo.CONFIANZA_MEDIA);

            evidenceManagerPro.guardarHallazgosComoIssues(mockApi, Arrays.asList(hallazgo1, hallazgo2));

            mockedExtension.verify(() -> ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(
                any(MontoyaApi.class), any(Hallazgo.class)), times(2)
            );
        }
    }

    @Test
    void guardarHallazgosComoIssues_conBurpCommunity_noIntentaNingunGuardado() {
        // Early-return con un solo warning: en Community no debe iterar ni
        // invocar el guardado por hallazgo.
        MontoyaApi mockApiCommunity = mock(MontoyaApi.class);
        when(mockApiCommunity.siteMap()).thenReturn(null);

        try (MockedStatic<ExtensionBurpIA> mockedExtension = mockStatic(ExtensionBurpIA.class)) {
            mockedExtension.when(() -> ExtensionBurpIA.esBurpProfessional(any())).thenReturn(false);

            EvidenceManager evidenceManagerCommunity = new EvidenceManager(mockApiCommunity);
            evidenceManagerCommunity.guardarHallazgosComoIssues(mockApiCommunity,
                Arrays.asList(mock(Hallazgo.class), mock(Hallazgo.class), mock(Hallazgo.class)));

            mockedExtension.verify(() -> ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(
                any(MontoyaApi.class), any(Hallazgo.class)), never()
            );
        }
    }

    @Test
    void guardarHallazgosComoIssues_conNullsEnLista_soloIntentaLosValidos() {
        try (MockedStatic<ExtensionBurpIA> mockedExtension = mockStatic(ExtensionBurpIA.class)) {
            mockedExtension.when(() -> ExtensionBurpIA.esBurpProfessional(any())).thenReturn(true);
            mockedExtension.when(() -> ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(any(), any()))
                          .thenReturn(true);

            EvidenceManager evidenceManagerPro = new EvidenceManager(mockApi);

            evidenceManagerPro.guardarHallazgosComoIssues(mockApi,
                Arrays.asList(mock(Hallazgo.class), null, mock(Hallazgo.class)));

            // Solo los 2 hallazgos no-null deben intentarse
            mockedExtension.verify(() -> ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(
                any(MontoyaApi.class), any(Hallazgo.class)), times(2)
            );
        }
    }
}
