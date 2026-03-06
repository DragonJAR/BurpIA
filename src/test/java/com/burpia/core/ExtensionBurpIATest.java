package com.burpia.core;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.BurpSuiteEdition;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import com.burpia.ExtensionBurpIA;
import com.burpia.ManejadorHttpBurpIA;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Hallazgo;
import com.burpia.ui.PanelAgente;
import com.burpia.ui.PestaniaPrincipal;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorTareas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ExtensionBurpIA Tests")
class ExtensionBurpIATest {

    /**
     * CONFIABILIDAD: Establecer idioma español antes de cada test para que sean deterministas.
     * Los tests verifican mensajes localizados y deben ser consistentes sin importar
     * el idioma del sistema.
     */
    @BeforeEach
    void setUp() {
        I18nUI.establecerIdioma("es");
    }

    @Nested
    @DisplayName("Tests de ciclo de vida")
    class CicloVidaTests {

        @Test
        @DisplayName("Unload limpia recursos y cierra gestores")
        void testUnloadLimpiaRecursos() throws Exception {
            ExtensionBurpIA extension = new ExtensionBurpIA();
            establecerCampo(extension, "stdout", new PrintWriter(new StringWriter(), true));
            establecerCampo(extension, "stderr", new PrintWriter(new StringWriter(), true));

            ManejadorHttpBurpIA manejador = mock(ManejadorHttpBurpIA.class);
            PestaniaPrincipal pestania = mock(PestaniaPrincipal.class);
            GestorTareas gestorTareas = mock(GestorTareas.class);
            GestorConsolaGUI gestorConsola = mock(GestorConsolaGUI.class);

            establecerCampo(extension, "manejadorHttp", manejador);
            establecerCampo(extension, "pestaniaPrincipal", pestania);
            establecerCampo(extension, "gestorTareas", gestorTareas);
            establecerCampo(extension, "gestorConsola", gestorConsola);

            extension.unload();

            verify(manejador).shutdown();
            verify(pestania).destruir();
            verify(gestorTareas).shutdown();
            verify(gestorConsola).shutdown();

            assertNull(obtenerCampo(extension, "manejadorHttp"));
            assertNull(obtenerCampo(extension, "pestaniaPrincipal"));
            assertNull(obtenerCampo(extension, "gestorTareas"));
            assertNull(obtenerCampo(extension, "gestorConsola"));
        }

        @Test
        @DisplayName("Unload es seguro sin initialize previo")
        void testUnloadSinInitializeNoFalla() {
            ExtensionBurpIA extension = new ExtensionBurpIA();
            assertDoesNotThrow(extension::unload);
        }

        @Test
        @DisplayName("Abrir configuración es seguro sin dependencias inicializadas")
        void testAbrirConfiguracionSinDependenciasNoFalla() throws Exception {
            ExtensionBurpIA extension = new ExtensionBurpIA();
            Method abrirConfiguracion = ExtensionBurpIA.class.getDeclaredMethod("abrirConfiguracion");
            abrirConfiguracion.setAccessible(true);
            assertDoesNotThrow(() -> abrirConfiguracion.invoke(extension));
        }
    }

    @Nested
    @DisplayName("Tests de detección de edición Burp")
    class EsBurpProfessionalTests {

        @Test
        @DisplayName("esBurpProfessional retorna false cuando API es null")
        void testEsBurpProfessionalConApiNull() {
            assertFalse(ExtensionBurpIA.esBurpProfessional(null));
        }

        @Test
        @DisplayName("esBurpProfessional retorna false cuando burpSuite es null")
        void testEsBurpProfessionalConBurpSuiteNull() {
            MontoyaApi api = mock(MontoyaApi.class);
            when(api.burpSuite()).thenReturn(null);
            assertFalse(ExtensionBurpIA.esBurpProfessional(api));
        }

        @Test
        @DisplayName("esBurpProfessional retorna true para Professional Edition")
        void testEsBurpProfessionalProfessionalEdition() {
            MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
            when(api.burpSuite().version().edition()).thenReturn(BurpSuiteEdition.PROFESSIONAL);
            assertTrue(ExtensionBurpIA.esBurpProfessional(api));
        }

        @Test
        @DisplayName("esBurpProfessional retorna false cuando lanza excepción")
        void testEsBurpProfessionalConExcepcion() {
            MontoyaApi api = mock(MontoyaApi.class);
            when(api.burpSuite()).thenThrow(new RuntimeException("Test exception"));
            assertFalse(ExtensionBurpIA.esBurpProfessional(api));
        }

        @Test
        @DisplayName("esBurpProfessional retorna false cuando burpSuite.version es null")
        void testEsBurpProfessionalConVersionNull() {
            MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
            when(api.burpSuite().version()).thenReturn(null);
            assertFalse(ExtensionBurpIA.esBurpProfessional(api));
        }

        @Test
        @DisplayName("esBurpProfessional instancia retorna valor cacheado")
        void testEsBurpProfessionalInstancia() throws Exception {
            ExtensionBurpIA extension = new ExtensionBurpIA();
            establecerCampo(extension, "esProfessional", true);
            assertTrue(extension.esBurpProfessional());

            establecerCampo(extension, "esProfessional", false);
            assertFalse(extension.esBurpProfessional());
        }
    }

    @Nested
    @DisplayName("Tests de creación de AuditIssue")
    class CrearAuditIssueTests {

        @Test
        @DisplayName("crearAuditIssueDesdeHallazgo retorna null cuando hallazgo es null")
        void testCrearAuditIssueConHallazgoNull() {
            assertNull(ExtensionBurpIA.crearAuditIssueDesdeHallazgo(null, null));
        }

        /**
         * CONFIABILIDAD: Los siguientes tests requieren Burp Suite corriendo para crear AuditIssue.
         * La API de Montoya usa ObjectFactoryLocator.FACTORY que solo está disponible en runtime.
         * Se deshabilitan para evitar NullPointerException en entorno de tests.
         */
        @Test
        @Disabled("Requiere Burp Suite corriendo - AuditIssue.auditIssue() necesita ObjectFactoryLocator.FACTORY")
        @DisplayName("crearAuditIssueDesdeHallazgo crea issue válido con hallazgo completo")
        void testCrearAuditIssueConHallazgoCompleto() {
            Hallazgo hallazgo = new Hallazgo("https://example.com/vuln", "XSS Title", "XSS Description", "High", "High");
            var issue = ExtensionBurpIA.crearAuditIssueDesdeHallazgo(hallazgo, null);
            assertNotNull(issue);
            assertEquals("XSS Title", issue.name());
        }

        @Test
        @Disabled("Requiere Burp Suite corriendo - AuditIssue.auditIssue() necesita ObjectFactoryLocator.FACTORY")
        @DisplayName("crearAuditIssueDesdeHallazgo maneja severidad crítica")
        void testCrearAuditIssueSeveridadCritical() {
            Hallazgo hallazgo = new Hallazgo("https://example.com/vuln", "RCE", "Remote Code Execution", "Critical", "High");
            var issue = ExtensionBurpIA.crearAuditIssueDesdeHallazgo(hallazgo, null);
            assertNotNull(issue);
            assertEquals(burp.api.montoya.scanner.audit.issues.AuditIssueSeverity.HIGH, issue.severity());
        }

        @Test
        @Disabled("Requiere Burp Suite corriendo - AuditIssue.auditIssue() necesita ObjectFactoryLocator.FACTORY")
        @DisplayName("crearAuditIssueDesdeHallazgo maneja severidad baja")
        void testCrearAuditIssueSeveridadLow() {
            Hallazgo hallazgo = new Hallazgo("https://example.com/vuln", "Info Leak", "Information Disclosure", "Low", "Medium");
            var issue = ExtensionBurpIA.crearAuditIssueDesdeHallazgo(hallazgo, null);
            assertNotNull(issue);
            assertEquals(burp.api.montoya.scanner.audit.issues.AuditIssueSeverity.LOW, issue.severity());
        }

        @Test
        @Disabled("Requiere Burp Suite corriendo - AuditIssue.auditIssue() necesita ObjectFactoryLocator.FACTORY")
        @DisplayName("crearAuditIssueDesdeHallazgo maneja severidad info")
        void testCrearAuditIssueSeveridadInfo() {
            Hallazgo hallazgo = new Hallazgo("https://example.com/vuln", "Info", "Informational", "Info", "Low");
            var issue = ExtensionBurpIA.crearAuditIssueDesdeHallazgo(hallazgo, null);
            assertNotNull(issue);
            assertEquals(burp.api.montoya.scanner.audit.issues.AuditIssueSeverity.INFORMATION, issue.severity());
        }

        @Test
        @Disabled("Requiere Burp Suite corriendo - AuditIssue.auditIssue() necesita ObjectFactoryLocator.FACTORY")
        @DisplayName("crearAuditIssueDesdeHallazgo mapea confianza alta a CERTAIN")
        void testCrearAuditIssueConfianzaAlta() {
            Hallazgo hallazgo = new Hallazgo("https://example.com/vuln", "SQLi", "SQL Injection", "High", "High");
            var issue = ExtensionBurpIA.crearAuditIssueDesdeHallazgo(hallazgo, null);
            assertNotNull(issue);
            assertEquals(burp.api.montoya.scanner.audit.issues.AuditIssueConfidence.CERTAIN, issue.confidence());
        }

        @Test
        @Disabled("Requiere Burp Suite corriendo - AuditIssue.auditIssue() necesita ObjectFactoryLocator.FACTORY")
        @DisplayName("crearAuditIssueDesdeHallazgo mapea confianza media a FIRM")
        void testCrearAuditIssueConfianzaMedia() {
            Hallazgo hallazgo = new Hallazgo("https://example.com/vuln", "SQLi", "SQL Injection", "High", "Medium");
            var issue = ExtensionBurpIA.crearAuditIssueDesdeHallazgo(hallazgo, null);
            assertNotNull(issue);
            assertEquals(burp.api.montoya.scanner.audit.issues.AuditIssueConfidence.FIRM, issue.confidence());
        }

        @Test
        @Disabled("Requiere Burp Suite corriendo - AuditIssue.auditIssue() necesita ObjectFactoryLocator.FACTORY")
        @DisplayName("crearAuditIssueDesdeHallazgo mapea confianza baja a TENTATIVE")
        void testCrearAuditIssueConfianzaBaja() {
            Hallazgo hallazgo = new Hallazgo("https://example.com/vuln", "SQLi", "SQL Injection", "High", "Low");
            var issue = ExtensionBurpIA.crearAuditIssueDesdeHallazgo(hallazgo, null);
            assertNotNull(issue);
            assertEquals(burp.api.montoya.scanner.audit.issues.AuditIssueConfidence.TENTATIVE, issue.confidence());
        }
    }

    @Nested
    @DisplayName("Tests de guardar AuditIssue")
    class GuardarAuditIssueTests {

        @Test
        @DisplayName("Guardar AuditIssue retorna false cuando API es null")
        void testGuardarAuditIssueConApiNull() {
            Hallazgo hallazgo = new Hallazgo("https://example.com", "SQLi Title", "Possible SQLi", "High", "High");
            boolean guardado = ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(null, hallazgo, null);
            assertFalse(guardado);
        }

        @Test
        @DisplayName("Guardar AuditIssue retorna false cuando SiteMap es null")
        void testGuardarAuditIssueSinSiteMap() {
            MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
            when(api.burpSuite().version().edition()).thenReturn(BurpSuiteEdition.PROFESSIONAL);
            when(api.siteMap()).thenReturn(null);
            Hallazgo hallazgo = new Hallazgo("https://example.com", "SQLi Title", "Possible SQLi", "High", "High");

            boolean guardado = ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(api, hallazgo, null);

            assertFalse(guardado);
        }

        @Test
        @DisplayName("Guardar AuditIssue retorna false cuando hallazgo es null")
        void testGuardarAuditIssueConHallazgoNull() {
            MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
            when(api.burpSuite().version().edition()).thenReturn(BurpSuiteEdition.PROFESSIONAL);

            boolean guardado = ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(api, null, null);

            assertFalse(guardado);
        }

        @Test
        @DisplayName("Guardar AuditIssue retorna false cuando no es Professional Edition")
        void testGuardarAuditIssueNoProfessional() {
            MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
            // Simular que no es Professional (burpSuite.version.edition retorna algo distinto de PROFESSIONAL)
            when(api.burpSuite().version().edition()).thenReturn(null);
            Hallazgo hallazgo = new Hallazgo("https://example.com", "SQLi Title", "Possible SQLi", "High", "High");

            boolean guardado = ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(api, hallazgo, null);

            assertFalse(guardado);
        }
    }

    @Nested
    @DisplayName("Tests de envío al Agente")
    class EnviarAAgenteTests {

        @Test
        @DisplayName("Enviar al Agente retorna false cuando config es null")
        void testEnviarAAgenteSinConfigRetornaFalse() throws Exception {
            ExtensionBurpIA extension = new ExtensionBurpIA();
            Method enviarAAgente = ExtensionBurpIA.class.getDeclaredMethod("enviarAAgente", HttpRequestResponse.class);
            enviarAAgente.setAccessible(true);
            Object resultado = enviarAAgente.invoke(extension, new Object[]{null});
            assertFalse((Boolean) resultado);
        }

        @Test
        @DisplayName("Enviar al Agente retorna false cuando agente deshabilitado")
        void testEnviarAAgenteDeshabilitadoRetornaFalse() throws Exception {
            ExtensionBurpIA extension = new ExtensionBurpIA();
            ConfiguracionAPI config = new ConfiguracionAPI();
            config.establecerAgenteHabilitado(false);
            establecerCampo(extension, "config", config);

            Method enviarAAgente = ExtensionBurpIA.class.getDeclaredMethod("enviarAAgente", HttpRequestResponse.class);
            enviarAAgente.setAccessible(true);

            Object resultado = enviarAAgente.invoke(extension, mock(HttpRequestResponse.class));
            assertFalse((Boolean) resultado);
        }

        @Test
        @DisplayName("Enviar al Agente tolera solicitud-respuesta nula")
        void testEnviarAAgenteNuloNoFalla() throws Exception {
            ExtensionBurpIA extension = new ExtensionBurpIA();
            ConfiguracionAPI config = new ConfiguracionAPI();
            config.establecerAgenteHabilitado(true);
            establecerCampo(extension, "config", config);

            Method enviarAAgente = ExtensionBurpIA.class.getDeclaredMethod("enviarAAgente", HttpRequestResponse.class);
            enviarAAgente.setAccessible(true);

            assertDoesNotThrow(() -> enviarAAgente.invoke(extension, new Object[]{null}));
        }

        @Test
        @DisplayName("Enviar al Agente retorna false sin pestaniaPrincipal")
        void testEnviarAAgenteSinPestaniaPrincipalRetornaFalse() throws Exception {
            ExtensionBurpIA extension = new ExtensionBurpIA();
            ConfiguracionAPI config = new ConfiguracionAPI();
            config.establecerAgenteHabilitado(true);
            config.establecerAgentePrompt("REQ={REQUEST}");
            establecerCampo(extension, "config", config);

            Method enviarAAgente = ExtensionBurpIA.class.getDeclaredMethod("enviarAAgente", HttpRequestResponse.class);
            enviarAAgente.setAccessible(true);

            HttpRequestResponse solicitudRespuesta = mock(HttpRequestResponse.class);
            when(solicitudRespuesta.request()).thenReturn(mock(HttpRequest.class));

            Object resultado = enviarAAgente.invoke(extension, solicitudRespuesta);
            assertFalse((Boolean) resultado);
        }

        @Test
        @DisplayName("Enviar al Agente es seguro sin configuración inicializada")
        void testEnviarAAgenteSinConfigNoFalla() throws Exception {
            ExtensionBurpIA extension = new ExtensionBurpIA();
            Method enviarAAgente = ExtensionBurpIA.class.getDeclaredMethod("enviarAAgente", HttpRequestResponse.class);
            enviarAAgente.setAccessible(true);
            assertDoesNotThrow(() -> enviarAAgente.invoke(extension, new Object[]{null}));
        }

        @Test
        @DisplayName("Enviar hallazgo al Agente retorna false cuando config es null")
        void testEnviarHallazgoAAgenteSinConfigRetornaFalse() throws Exception {
            ExtensionBurpIA extension = new ExtensionBurpIA();
            Method enviarHallazgo = ExtensionBurpIA.class.getDeclaredMethod("enviarHallazgoAAgente", Hallazgo.class);
            enviarHallazgo.setAccessible(true);
            Object resultado = enviarHallazgo.invoke(extension, new Hallazgo("url", "t", "d", "High", "High"));
            assertFalse((Boolean) resultado);
        }

        @Test
        @DisplayName("Enviar hallazgo al Agente tolera hallazgo nulo")
        void testEnviarHallazgoAAgenteNuloNoFalla() throws Exception {
            ExtensionBurpIA extension = new ExtensionBurpIA();
            ConfiguracionAPI config = new ConfiguracionAPI();
            config.establecerAgenteHabilitado(true);
            establecerCampo(extension, "config", config);

            Method enviarHallazgo = ExtensionBurpIA.class.getDeclaredMethod("enviarHallazgoAAgente", Hallazgo.class);
            enviarHallazgo.setAccessible(true);

            assertDoesNotThrow(() -> enviarHallazgo.invoke(extension, new Object[]{null}));
        }

        @Test
        @DisplayName("Enviar al Agente evita serializar request/response si prompt no usa tokens")
        void testEnviarAAgenteEvitaSerializacionSinTokens() throws Exception {
            ExtensionBurpIA extension = new ExtensionBurpIA();
            ConfiguracionAPI config = new ConfiguracionAPI();
            config.establecerAgenteHabilitado(true);
            config.establecerAgentePrompt("Analiza la vulnerabilidad y responde en {OUTPUT_LANGUAGE}");
            establecerCampo(extension, "config", config);

            PestaniaPrincipal pestania = mock(PestaniaPrincipal.class);
            PanelAgente panelAgente = mock(PanelAgente.class);
            when(pestania.obtenerPanelAgente()).thenReturn(panelAgente);
            establecerCampo(extension, "pestaniaPrincipal", pestania);

            HttpRequestResponse solicitudRespuesta = mock(HttpRequestResponse.class);
            AtomicInteger contadorRequest = new AtomicInteger(0);
            AtomicInteger contadorResponse = new AtomicInteger(0);
            HttpRequest request = crearProxyContadorToString(HttpRequest.class, contadorRequest, "REQUEST-CONTENT");
            HttpResponse response = crearProxyContadorToString(HttpResponse.class, contadorResponse, "RESPONSE-CONTENT");
            when(solicitudRespuesta.request()).thenReturn(request);
            when(solicitudRespuesta.response()).thenReturn(response);

            Method enviarAAgente = ExtensionBurpIA.class.getDeclaredMethod("enviarAAgente", HttpRequestResponse.class);
            enviarAAgente.setAccessible(true);

            assertDoesNotThrow(() -> enviarAAgente.invoke(extension, solicitudRespuesta));

            assertEquals(0, contadorRequest.get());
            assertEquals(0, contadorResponse.get());
            verify(panelAgente).inyectarComando(anyString(), eq(0));
        }

        @Test
        @DisplayName("Enviar al Agente serializa request/response cuando prompt usa tokens")
        void testEnviarAAgenteSerializaConTokens() throws Exception {
            ExtensionBurpIA extension = new ExtensionBurpIA();
            ConfiguracionAPI config = new ConfiguracionAPI();
            config.establecerAgenteHabilitado(true);
            config.establecerAgentePrompt("REQ={REQUEST}\\nRES={RESPONSE}");
            establecerCampo(extension, "config", config);

            PestaniaPrincipal pestania = mock(PestaniaPrincipal.class);
            PanelAgente panelAgente = mock(PanelAgente.class);
            when(pestania.obtenerPanelAgente()).thenReturn(panelAgente);
            establecerCampo(extension, "pestaniaPrincipal", pestania);

            HttpRequestResponse solicitudRespuesta = mock(HttpRequestResponse.class);
            AtomicInteger contadorRequest = new AtomicInteger(0);
            AtomicInteger contadorResponse = new AtomicInteger(0);
            HttpRequest request = crearProxyContadorToString(HttpRequest.class, contadorRequest, "REQUEST-CONTENT");
            HttpResponse response = crearProxyContadorToString(HttpResponse.class, contadorResponse, "RESPONSE-CONTENT");
            when(solicitudRespuesta.request()).thenReturn(request);
            when(solicitudRespuesta.response()).thenReturn(response);

            Method enviarAAgente = ExtensionBurpIA.class.getDeclaredMethod("enviarAAgente", HttpRequestResponse.class);
            enviarAAgente.setAccessible(true);

            assertDoesNotThrow(() -> enviarAAgente.invoke(extension, solicitudRespuesta));

            assertTrue(contadorRequest.get() > 0);
            assertTrue(contadorResponse.get() > 0);
            verify(panelAgente).inyectarComando(anyString(), eq(0));
        }

        @Test
        @DisplayName("Enviar hallazgo manual al Agente genera GET desde URL cuando falta evidencia")
        void testEnviarHallazgoManualGeneraGetDesdeUrl() throws Exception {
            ExtensionBurpIA extension = new ExtensionBurpIA();
            ConfiguracionAPI config = new ConfiguracionAPI();
            config.establecerAgenteHabilitado(true);
            config.establecerAgentePrompt("REQ={REQUEST}\\nRES={RESPONSE}\\nTITLE={TITLE}\\nDESC={DESCRIPTION}");
            establecerCampo(extension, "config", config);

            PestaniaPrincipal pestania = mock(PestaniaPrincipal.class);
            PanelAgente panelAgente = mock(PanelAgente.class);
            when(pestania.obtenerPanelAgente()).thenReturn(panelAgente);
            establecerCampo(extension, "pestaniaPrincipal", pestania);

            Hallazgo hallazgoManual = new Hallazgo(
                "https://example.com/login?x=1",
                "Titulo Manual",
                "Descripcion Manual",
                "High",
                "Medium"
            );

            Method enviarHallazgo = ExtensionBurpIA.class.getDeclaredMethod("enviarHallazgoAAgente", Hallazgo.class);
            enviarHallazgo.setAccessible(true);
            assertDoesNotThrow(() -> enviarHallazgo.invoke(extension, hallazgoManual));

            ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
            verify(panelAgente).inyectarComando(payloadCaptor.capture(), eq(0));
            String payload = payloadCaptor.getValue();

            assertNotNull(payload);
            assertTrue(payload.contains("REQ="));
            int inicioReq = payload.indexOf("REQ=") + 4;
            int finReq = payload.indexOf("\nRES=", inicioReq);
            if (finReq < 0) {
                finReq = payload.indexOf("RES=", inicioReq);
            }
            String requestSerializado = finReq > inicioReq ? payload.substring(inicioReq, finReq) : payload.substring(inicioReq);
            assertTrue(!requestSerializado.trim().isEmpty(), payload);
            assertTrue(requestSerializado.toUpperCase().contains("GET"), payload);
            assertTrue(payload.contains("TITLE=Titulo Manual"));
            assertTrue(payload.contains("DESC=Descripcion Manual"));
            assertTrue(payload.contains("RES="));
        }

        @Test
        @DisplayName("Enviar hallazgo manual con URL invalida mantiene titulo y resumen")
        void testEnviarHallazgoManualUrlInvalidaMantieneTituloResumen() throws Exception {
            ExtensionBurpIA extension = new ExtensionBurpIA();
            ConfiguracionAPI config = new ConfiguracionAPI();
            config.establecerAgenteHabilitado(true);
            config.establecerAgentePrompt("REQ={REQUEST}\\nRES={RESPONSE}");
            establecerCampo(extension, "config", config);

            PestaniaPrincipal pestania = mock(PestaniaPrincipal.class);
            PanelAgente panelAgente = mock(PanelAgente.class);
            when(pestania.obtenerPanelAgente()).thenReturn(panelAgente);
            establecerCampo(extension, "pestaniaPrincipal", pestania);

            Hallazgo hallazgoManual = new Hallazgo(
                "://url-invalida",
                "Titulo Manual",
                "Descripcion Manual",
                "Low",
                "Low"
            );

            Method enviarHallazgo = ExtensionBurpIA.class.getDeclaredMethod("enviarHallazgoAAgente", Hallazgo.class);
            enviarHallazgo.setAccessible(true);
            assertDoesNotThrow(() -> enviarHallazgo.invoke(extension, hallazgoManual));

            ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
            verify(panelAgente).inyectarComando(payloadCaptor.capture(), eq(0));
            String payload = payloadCaptor.getValue();

            assertNotNull(payload);
            assertTrue(payload.contains("Title: Titulo Manual"));
            assertTrue(payload.contains("Summary: Descripcion Manual"));
            assertTrue(payload.contains("REQ="));
        }

        @Test
        @DisplayName("Enviar hallazgo usa token URL correctamente")
        void testEnviarHallazgoConTokenUrl() throws Exception {
            ExtensionBurpIA extension = new ExtensionBurpIA();
            ConfiguracionAPI config = new ConfiguracionAPI();
            config.establecerAgenteHabilitado(true);
            config.establecerAgentePrompt("URL={URL}");
            establecerCampo(extension, "config", config);

            PestaniaPrincipal pestania = mock(PestaniaPrincipal.class);
            PanelAgente panelAgente = mock(PanelAgente.class);
            when(pestania.obtenerPanelAgente()).thenReturn(panelAgente);
            establecerCampo(extension, "pestaniaPrincipal", pestania);

            Hallazgo hallazgo = new Hallazgo(
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                "High",
                "High"
            );

            Method enviarHallazgo = ExtensionBurpIA.class.getDeclaredMethod("enviarHallazgoAAgente", Hallazgo.class);
            enviarHallazgo.setAccessible(true);
            assertDoesNotThrow(() -> enviarHallazgo.invoke(extension, hallazgo));

            ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
            verify(panelAgente).inyectarComando(payloadCaptor.capture(), eq(0));
            String payload = payloadCaptor.getValue();

            assertTrue(payload.contains("URL=https://example.com/test"));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T crearProxyContadorToString(Class<T> tipo, AtomicInteger contador, String retornoToString) {
        return (T) Proxy.newProxyInstance(
            tipo.getClassLoader(),
            new Class<?>[]{tipo},
            (proxy, method, args) -> {
                if ("toString".equals(method.getName())) {
                    contador.incrementAndGet();
                    return retornoToString;
                }
                Class<?> retorno = method.getReturnType();
                if (!retorno.isPrimitive()) {
                    return null;
                }
                if (retorno == boolean.class) return false;
                if (retorno == byte.class) return (byte) 0;
                if (retorno == short.class) return (short) 0;
                if (retorno == int.class) return 0;
                if (retorno == long.class) return 0L;
                if (retorno == float.class) return 0f;
                if (retorno == double.class) return 0d;
                if (retorno == char.class) return '\0';
                return null;
            }
        );
    }

    private static void establecerCampo(Object objetivo, String nombre, Object valor) throws Exception {
        Field field = ExtensionBurpIA.class.getDeclaredField(nombre);
        field.setAccessible(true);
        field.set(objetivo, valor);
    }

    private static Object obtenerCampo(Object objetivo, String nombre) throws Exception {
        Field field = ExtensionBurpIA.class.getDeclaredField(nombre);
        field.setAccessible(true);
        return field.get(objetivo);
    }
}
