package com.burpia.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.BurpSuiteEdition;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.scanner.audit.Audit;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.Hallazgo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import javax.swing.JTable;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PanelHallazgos Send Tests")
class PanelHallazgosSendTest {

    @Test
    @DisplayName("Enviar a Repeater usa la solicitud seleccionada")
    void testEnviarARepeater() throws Exception {
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        burp.api.montoya.repeater.Repeater repeater = mock(burp.api.montoya.repeater.Repeater.class);
        when(api.repeater()).thenReturn(repeater);
        PanelHallazgos panel = crearPanel(api, true);
        try {
            HttpRequest request = mock(HttpRequest.class);
            when(request.url()).thenReturn("https://example.com/repeater");
            agregarHallazgoConRequest(panel, request, "https://example.com/repeater");
            assertTrue(panel.obtenerModelo().getRowCount() >= 1);

            invocarAccionPrivada(panel, "enviarARepeater", new int[]{0});

            verify(repeater, timeout(1000)).sendToRepeater(eq(request), anyString());
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Enviar a Intruder usa la solicitud seleccionada")
    void testEnviarAIntruder() throws Exception {
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        burp.api.montoya.intruder.Intruder intruder = mock(burp.api.montoya.intruder.Intruder.class);
        when(api.intruder()).thenReturn(intruder);
        PanelHallazgos panel = crearPanel(api, true);
        try {
            HttpRequest request = mock(HttpRequest.class);
            when(request.url()).thenReturn("https://example.com/intruder");
            agregarHallazgoConRequest(panel, request, "https://example.com/intruder");
            assertTrue(panel.obtenerModelo().getRowCount() >= 1);

            invocarAccionPrivada(panel, "enviarAIntruder", new int[]{0});

            verify(intruder, timeout(1000)).sendToIntruder(eq(request));
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Enviar a Scanner ejecuta la ruta sin lanzar errores")
    void testEnviarAScanner() throws Exception {
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        burp.api.montoya.scanner.Scanner scanner = mock(burp.api.montoya.scanner.Scanner.class);
        when(api.scanner()).thenReturn(scanner);
        Audit audit = mock(Audit.class);
        when(scanner.startAudit(any())).thenReturn(audit);

        PanelHallazgos panel = crearPanel(api, true);
        try {
            assertTrue(obtenerBooleanoCampo(panel, "esBurpProfessional"));
            HttpRequest request = mock(HttpRequest.class);
            when(request.url()).thenReturn("https://example.com/scanner");
            agregarHallazgoConRequest(panel, request, "https://example.com/scanner");
            assertTrue(panel.obtenerModelo().getRowCount() >= 1);

            assertDoesNotThrow(() -> invocarAccionPrivada(panel, "enviarAScanner", new int[]{0}));
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Enviar a Issues ejecuta la ruta sin lanzar errores")
    void testEnviarAIssues() throws Exception {
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        burp.api.montoya.sitemap.SiteMap siteMap = mock(burp.api.montoya.sitemap.SiteMap.class);
        when(api.burpSuite().version().edition()).thenReturn(BurpSuiteEdition.PROFESSIONAL);
        when(api.ai().isEnabled()).thenReturn(true);
        when(api.siteMap()).thenReturn(siteMap);

        PanelHallazgos panel = crearPanel(api, true);
        try {
            assertTrue(obtenerBooleanoCampo(panel, "integracionIssuesDisponible"));
            SwingUtilities.invokeAndWait(() -> panel.establecerGuardadoAutomaticoIssuesActivo(false));

            HttpRequest request = mock(HttpRequest.class);
            when(request.url()).thenReturn("https://example.com/issues");
            agregarHallazgoConRequest(panel, request, "https://example.com/issues");
            assertTrue(panel.obtenerModelo().getRowCount() >= 1);

            assertDoesNotThrow(() -> invocarAccionPrivada(panel, "enviarAIssues", new int[]{0}));
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Enviar a agente omite hallazgos ignorados")
    void testEnviarAAgenteOmiteIgnorados() throws Exception {
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        PanelHallazgos panel = crearPanel(api, true);
        try {
            ConfiguracionAPI config = new ConfiguracionAPI();
            config.establecerAgenteHabilitado(true);
            panel.establecerConfiguracion(config);

            HttpRequest requestA = mock(HttpRequest.class);
            when(requestA.url()).thenReturn("https://example.com/a");
            HttpRequest requestB = mock(HttpRequest.class);
            when(requestB.url()).thenReturn("https://example.com/b");

            agregarHallazgoConRequest(panel, requestA, "https://example.com/a");
            agregarHallazgoConRequest(panel, requestB, "https://example.com/b");
            panel.obtenerModelo().marcarComoIgnorado(1);
            flushEdt();

            AtomicInteger enviados = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(1);
            panel.establecerManejadorEnviarAAgente(h -> {
                enviados.incrementAndGet();
                latch.countDown();
                return true;
            });

            invocarAccionPrivada(panel, "enviarAAgente", new int[]{0, 1});

            assertTrue(latch.await(1, TimeUnit.SECONDS));
            assertEquals(1, enviados.get());
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Captura no reconstruye request cuando falta evidencia original")
    void testCapturaNoReconstruyeRequestSinEvidenciaOriginal() throws Exception {
        MontoyaApi api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        PanelHallazgos panel = crearPanel(api, true);
        try {
            panel.obtenerModelo().agregarHallazgo(
                new Hallazgo("https://example.com/sin-request", "Titulo", "Descripcion", "Low", "Low")
            );
            esperarFilas(panel, 1);

            Method metodoCaptura = PanelHallazgos.class.getDeclaredMethod("capturarEntradasAccion", int[].class);
            metodoCaptura.setAccessible(true);
            Object captura = metodoCaptura.invoke(panel, (Object) new int[] {0});

            java.lang.reflect.Field campoEntradas = captura.getClass().getDeclaredField("entradas");
            campoEntradas.setAccessible(true);
            java.util.List<?> entradas = (java.util.List<?>) campoEntradas.get(captura);
            assertEquals(1, entradas.size());

            Object entrada = entradas.get(0);
            java.lang.reflect.Field campoSolicitud = entrada.getClass().getDeclaredField("solicitud");
            campoSolicitud.setAccessible(true);
            assertNull(campoSolicitud.get(entrada));
        } finally {
            panel.destruir();
        }
    }

    private PanelHallazgos crearPanel(MontoyaApi api, boolean esBurpProfessional) throws Exception {
        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, new ModeloTablaHallazgos(100), esBurpProfessional));
        return holder[0];
    }

    private void agregarHallazgoConRequest(PanelHallazgos panel, HttpRequest request, String url) throws Exception {
        Hallazgo hallazgo = new Hallazgo(url, "Titulo", "Descripcion", "High", "High", request);
        panel.obtenerModelo().agregarHallazgo(hallazgo);
        esperarFilas(panel, 1);
    }

    private void invocarAccionPrivada(PanelHallazgos panel, String metodo, int[] filas) throws Exception {
        Method method = PanelHallazgos.class.getDeclaredMethod(metodo, int[].class);
        method.setAccessible(true);
        method.invoke(panel, (Object) filas);
    }

    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
    }

    private void esperarFilas(PanelHallazgos panel, int minimo) throws Exception {
        JTable tabla = obtenerTabla(panel);
        for (int i = 0; i < 25; i++) {
            flushEdt();
            if (panel.obtenerModelo().getRowCount() >= minimo && tabla.getRowCount() >= minimo) {
                return;
            }
            Thread.sleep(20);
        }
        flushEdt();
    }

    private JTable obtenerTabla(PanelHallazgos panel) throws Exception {
        java.lang.reflect.Field field = PanelHallazgos.class.getDeclaredField("tabla");
        field.setAccessible(true);
        return (JTable) field.get(panel);
    }

    private boolean obtenerBooleanoCampo(PanelHallazgos panel, String nombre) throws Exception {
        java.lang.reflect.Field field = PanelHallazgos.class.getDeclaredField(nombre);
        field.setAccessible(true);
        return field.getBoolean(panel);
    }
}
