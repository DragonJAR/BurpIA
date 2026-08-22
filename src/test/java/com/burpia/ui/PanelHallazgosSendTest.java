package com.burpia.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.BurpSuiteEdition;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.internal.MontoyaObjectFactory;
import burp.api.montoya.internal.ObjectFactoryLocator;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.scanner.AuditConfiguration;
import burp.api.montoya.scanner.audit.Audit;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Hallazgo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import javax.swing.JTable;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para las funciones de envío de PanelHallazgos.
 * <p>
 * Verifica que los métodos de envío a Repeater, Intruder, Scanner, Issues y Agente
 * funcionen correctamente y manejen adecuadamente los casos edge.
 * </p>
 */
@DisplayName("PanelHallazgos Send Tests")
class PanelHallazgosSendTest extends PanelTestBase {

    private static final int TIMEOUT_VERIFICACION_MS = 1000;
    private static final int TIMEOUT_LATCH_SEGUNDOS = 1;
    private static final int MAX_REINTENTOS_ESPERA_FILAS = 25;
    private static final int DELAY_ESPERA_FILAS_MS = 20;

    private PanelHallazgos panel;
    private MontoyaApi api;

    @BeforeEach
    void setUp() throws Exception {
        TestDialogUtils.registrarCapturaDialogos();
        api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        panel = crearPanelHallazgos(api, new ModeloTablaHallazgos(100), true);
    }

    @AfterEach
    void tearDown() {
        // La destrucción del panel la gestiona PanelTestBase.
        TestDialogUtils.desregistrarCapturaDialogos();
    }

    @Test
    @DisplayName("Enviar a Repeater usa la solicitud seleccionada")
    void testEnviarARepeater() throws Exception {
        burp.api.montoya.repeater.Repeater repeater = mock(burp.api.montoya.repeater.Repeater.class);
        when(api.repeater()).thenReturn(repeater);

        HttpRequest request = mock(HttpRequest.class);
        when(request.url()).thenReturn("https://example.com/repeater");
        agregarHallazgoConRequest(panel, request, "https://example.com/repeater");
        assertTrue(panel.obtenerModelo().getRowCount() >= 1, "assertTrue failed at PanelHallazgosSendTest.java:77");

        invocarMetodoPrivado(panel, "enviarARepeater", new int[]{0});

        verify(repeater, timeout(TIMEOUT_VERIFICACION_MS)).sendToRepeater(eq(request), anyString());
    }

    @Test
    @DisplayName("Enviar a Intruder usa la solicitud seleccionada")
    void testEnviarAIntruder() throws Exception {
        burp.api.montoya.intruder.Intruder intruder = mock(burp.api.montoya.intruder.Intruder.class);
        when(api.intruder()).thenReturn(intruder);

        HttpRequest request = mock(HttpRequest.class);
        when(request.url()).thenReturn("https://example.com/intruder");
        agregarHallazgoConRequest(panel, request, "https://example.com/intruder");
        assertTrue(panel.obtenerModelo().getRowCount() >= 1, "assertTrue failed at PanelHallazgosSendTest.java:93");

        invocarMetodoPrivado(panel, "enviarAIntruder", new int[]{0});

        verify(intruder, timeout(TIMEOUT_VERIFICACION_MS)).sendToIntruder(eq(request));
    }

    @Test
    @DisplayName("Enviar a Scanner crea el audit y le encola la solicitud seleccionada")
    void testEnviarAScanner() throws Exception {
        burp.api.montoya.scanner.Scanner scanner = mock(burp.api.montoya.scanner.Scanner.class);
        when(api.scanner()).thenReturn(scanner);
        Audit audit = mock(Audit.class);
        when(scanner.startAudit(any())).thenReturn(audit);

        assertTrue(obtenerCampoBooleano(panel, "esBurpProfessional"), "assertTrue failed at PanelHallazgosSendTest.java:108");
        HttpRequest request = mock(HttpRequest.class);
        when(request.url()).thenReturn("https://example.com/scanner");
        agregarHallazgoConRequest(panel, request, "https://example.com/scanner");
        assertTrue(panel.obtenerModelo().getRowCount() >= 1, "assertTrue failed at PanelHallazgosSendTest.java:112");

        // AuditConfiguration.auditConfiguration(...) delega en ObjectFactoryLocator.FACTORY,
        // que fuera del runtime de Burp es null. mockStatic no alcanza: la acción corre
        // en el executor del panel y los mocks estáticos de Mockito son thread-scoped.
        // Se inyecta la factoría en el campo estático público y se restaura al final.
        MontoyaObjectFactory fabricaMontoya = mock(MontoyaObjectFactory.class);
        AuditConfiguration configScanner = mock(AuditConfiguration.class);
        when(fabricaMontoya.auditConfiguration(any())).thenReturn(configScanner);
        ObjectFactoryLocator.FACTORY = fabricaMontoya;
        try {
            invocarMetodoPrivado(panel, "enviarAScanner", new int[]{0});

            verify(scanner, timeout(TIMEOUT_VERIFICACION_MS).atLeastOnce()).startAudit(any());
            verify(audit, timeout(TIMEOUT_VERIFICACION_MS)).addRequest(eq(request));
        } finally {
            ObjectFactoryLocator.FACTORY = null;
        }
    }

    @Test
    @DisplayName("Enviar a Issues usa el manejador centralizado con la solicitud HTTP")
    void testEnviarAIssuesUsaManejadorCentralizadoConSolicitudHttp() throws Exception {
        burp.api.montoya.sitemap.SiteMap siteMap = mock(burp.api.montoya.sitemap.SiteMap.class);
        when(api.burpSuite().version().edition()).thenReturn(BurpSuiteEdition.PROFESSIONAL);
        when(api.ai().isEnabled()).thenReturn(true);
        when(api.siteMap()).thenReturn(siteMap);

        assertTrue(obtenerCampoBooleano(panel, "integracionIssuesDisponible"), "assertTrue failed at PanelHallazgosSendTest.java:125");
        SwingUtilities.invokeAndWait(() -> panel.establecerGuardadoAutomaticoIssuesActivo(false));

        HttpRequest request = mock(HttpRequest.class);
        when(request.url()).thenReturn("https://example.com/issues");
        agregarHallazgo(panel, new Hallazgo(
            "https://example.com/issues",
            "Titulo",
            "Descripcion",
            "High",
            "High",
            request
        ));
        assertTrue(panel.obtenerModelo().getRowCount() >= 1, "assertTrue failed at PanelHallazgosSendTest.java:131");

        CountDownLatch latch = new CountDownLatch(1);
        panel.establecerManejadorGuardarIssue(hallazgo -> {
            assertSame(request, hallazgo.obtenerSolicitudHttp(),
                "El flujo manual debe entregar la solicitud HTTP al manejador centralizado");
            latch.countDown();
            return true;
        });

        assertDoesNotThrow(() -> invocarMetodoPrivado(panel, "enviarAIssues", new int[]{0}));
        assertTrue(latch.await(TIMEOUT_LATCH_SEGUNDOS, TimeUnit.SECONDS),
            "El manejador centralizado de Issues debe ejecutarse");
    }

    @Test
    @DisplayName("Enviar a Issues reporta fallo cuando el manejador no puede guardar")
    void testEnviarAIssuesReportaFalloSinGuardar() throws Exception {
        HttpRequest request = mock(HttpRequest.class);
        when(request.url()).thenReturn("https://example.com/issues-fail");
        agregarHallazgoConRequest(panel, request, "https://example.com/issues-fail");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger intentos = new AtomicInteger(0);
        panel.establecerManejadorGuardarIssue(hallazgo -> {
            intentos.incrementAndGet();
            latch.countDown();
            return false;
        });

        invocarMetodoPrivado(panel, "enviarAIssues", new int[]{0});

        assertTrue(latch.await(TIMEOUT_LATCH_SEGUNDOS, TimeUnit.SECONDS),
            "Debe intentar guardar antes de reportar fallo");
        assertEquals(1, intentos.get(), "No debe reintentar ni duplicar el guardado fallido");
    }

    @Test
    @DisplayName("Auto-guardado de Issues no se ejecuta cuando esta desactivado")
    void testAutoGuardadoIssuesDesactivadoNoGuarda() throws Exception {
        AtomicInteger guardados = new AtomicInteger(0);
        panel.establecerManejadorGuardarIssue(hallazgo -> {
            guardados.incrementAndGet();
            return true;
        });
        SwingUtilities.invokeAndWait(() -> panel.establecerGuardadoAutomaticoIssuesActivo(false));

        HttpRequest request = mock(HttpRequest.class);
        when(request.url()).thenReturn("https://example.com/auto-off");
        agregarHallazgo(panel, new Hallazgo(
            "https://example.com/auto-off",
            "Titulo",
            "Descripcion",
            "High",
            "High",
            request
        ));
        Thread.sleep(DELAY_ESPERA_FILAS_MS * 2L);

        assertEquals(0, guardados.get(), "El auto-guardado desactivado no debe guardar Issues");
    }

    @Test
    @DisplayName("Auto-guardado de Issues usa el manejador centralizado cuando esta activo")
    void testAutoGuardadoIssuesActivoGuardaConManejadorCentralizado() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger guardados = new AtomicInteger(0);
        panel.establecerManejadorGuardarIssue(hallazgo -> {
            guardados.incrementAndGet();
            latch.countDown();
            return true;
        });
        SwingUtilities.invokeAndWait(() -> panel.establecerGuardadoAutomaticoIssuesActivo(true));

        HttpRequest request = mock(HttpRequest.class);
        when(request.url()).thenReturn("https://example.com/auto-on");
        agregarHallazgo(panel, new Hallazgo(
            "https://example.com/auto-on",
            "Titulo",
            "Descripcion",
            "High",
            "High",
            request
        ));

        assertTrue(latch.await(TIMEOUT_LATCH_SEGUNDOS, TimeUnit.SECONDS),
            "El auto-guardado activo debe invocar el manejador centralizado");
        assertEquals(1, guardados.get(), "Debe guardar exactamente una vez");
    }

    @Test
    @DisplayName("Auto-guardado de Issues registra fallos del manejador centralizado")
    void testAutoGuardadoIssuesActivoRegistraFallosDelManejador() throws Exception {
        Logging logging = mock(Logging.class);
        when(api.logging()).thenReturn(logging);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger intentos = new AtomicInteger(0);
        panel.establecerManejadorGuardarIssue(hallazgo -> {
            intentos.incrementAndGet();
            latch.countDown();
            return false;
        });
        SwingUtilities.invokeAndWait(() -> panel.establecerGuardadoAutomaticoIssuesActivo(true));

        HttpRequest request = mock(HttpRequest.class);
        when(request.url()).thenReturn("https://example.com/auto-fail");
        agregarHallazgo(panel, new Hallazgo(
            "https://example.com/auto-fail",
            "Titulo",
            "Descripcion",
            "High",
            "High",
            request
        ));

        assertTrue(latch.await(TIMEOUT_LATCH_SEGUNDOS, TimeUnit.SECONDS),
            "El auto-guardado debe intentar guardar el hallazgo");
        assertEquals(1, intentos.get(), "El auto-guardado fallido no debe duplicar intentos");
        verify(logging, timeout(TIMEOUT_VERIFICACION_MS))
            .logToError(contains("Auto-guardado de Issues incompleto"));
    }

    @Test
    @DisplayName("Auto-guardado de Issues convierte excepciones del manejador en fallo agregado")
    void testAutoGuardadoIssuesActivoCapturaExcepcionesDelManejador() throws Exception {
        Logging logging = mock(Logging.class);
        when(api.logging()).thenReturn(logging);
        CountDownLatch latch = new CountDownLatch(1);
        panel.establecerManejadorGuardarIssue(hallazgo -> {
            latch.countDown();
            throw new IllegalStateException("fallo controlado");
        });
        SwingUtilities.invokeAndWait(() -> panel.establecerGuardadoAutomaticoIssuesActivo(true));

        HttpRequest request = mock(HttpRequest.class);
        when(request.url()).thenReturn("https://example.com/auto-exception");
        agregarHallazgo(panel, new Hallazgo(
            "https://example.com/auto-exception",
            "Titulo",
            "Descripcion",
            "High",
            "High",
            request
        ));

        assertTrue(latch.await(TIMEOUT_LATCH_SEGUNDOS, TimeUnit.SECONDS),
            "El auto-guardado debe ejecutar el manejador aunque falle");
        verify(logging, timeout(TIMEOUT_VERIFICACION_MS))
            .logToError(contains("Auto-guardado de Issues incompleto"));
    }

    @Test
    @DisplayName("Community no activa auto-guardado de Issues")
    void testCommunityNoActivaAutoGuardadoIssues() throws Exception {
        PanelHallazgos panelCommunity = crearPanelHallazgos(api, new ModeloTablaHallazgos(100), false);
        AtomicInteger guardados = new AtomicInteger(0);
        panelCommunity.establecerManejadorGuardarIssue(hallazgo -> {
            guardados.incrementAndGet();
            return true;
        });
        SwingUtilities.invokeAndWait(() -> panelCommunity.establecerGuardadoAutomaticoIssuesActivo(true));
        assertFalse(panelCommunity.isGuardadoAutomaticoIssuesActivo(),
            "Community no debe activar el auto-guardado de Issues");

        HttpRequest request = mock(HttpRequest.class);
        when(request.url()).thenReturn("https://example.com/community");
        agregarHallazgo(panelCommunity, new Hallazgo(
            "https://example.com/community",
            "Titulo",
            "Descripcion",
            "High",
            "High",
            request
        ));
        Thread.sleep(DELAY_ESPERA_FILAS_MS * 2L);

        assertEquals(0, guardados.get(), "Community no debe intentar guardar Issues");
    }

    @Test
    @DisplayName("Enviar a agente omite hallazgos ignorados")
    void testEnviarAAgenteOmiteIgnorados() throws Exception {
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
            return PanelAgente.ResultadoInyeccion.INYECTADO;
        });

        invocarMetodoPrivado(panel, "enviarAAgente", new int[]{0, 1});

        assertTrue(latch.await(TIMEOUT_LATCH_SEGUNDOS, TimeUnit.SECONDS), "assertTrue failed at PanelHallazgosSendTest.java:163");
        assertEquals(1, enviados.get(), "assertEquals failed at PanelHallazgosSendTest.java:164");
    }

    @Test
    @DisplayName("Enviar a agente procesa hallazgo no ignorado")
    void testEnviarAAgenteProcesaNoIgnorado() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerAgenteHabilitado(true);
        panel.establecerConfiguracion(config);

        HttpRequest request = mock(HttpRequest.class);
        when(request.url()).thenReturn("https://example.com/test");
        agregarHallazgoConRequest(panel, request, "https://example.com/test");
        flushEdt();

        AtomicInteger enviados = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);
        panel.establecerManejadorEnviarAAgente(h -> {
            enviados.incrementAndGet();
            assertNotNull(h, "El hallazgo no debe ser null");
            assertEquals("https://example.com/test", h.obtenerUrl(), "assertEquals failed at PanelHallazgosSendTest.java:184");
            latch.countDown();
            return PanelAgente.ResultadoInyeccion.INYECTADO;
        });

        invocarMetodoPrivado(panel, "enviarAAgente", new int[]{0});

        assertTrue(latch.await(TIMEOUT_LATCH_SEGUNDOS, TimeUnit.SECONDS), "assertTrue failed at PanelHallazgosSendTest.java:191");
        assertEquals(1, enviados.get(), "assertEquals failed at PanelHallazgosSendTest.java:192");
    }

    @Test
    @DisplayName("Enviar a agente con terminal no lista (ENCOLADO) no reporta fallo")
    void testEnviarAAgenteEncoladoNoReportaFallo() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerAgenteHabilitado(true);
        panel.establecerConfiguracion(config);

        HttpRequest request = mock(HttpRequest.class);
        when(request.url()).thenReturn("https://example.com/queued");
        agregarHallazgoConRequest(panel, request, "https://example.com/queued");
        flushEdt();

        AtomicInteger invocaciones = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);
        // El manejador devuelve ENCOLADO: la terminal del agente aún no está lista.
        // El handler debe tratarlo como aceptado (no lanzar IllegalStateException),
        // para no reportar un fallo falso cuando el payload quedó encolado.
        panel.establecerManejadorEnviarAAgente(h -> {
            invocaciones.incrementAndGet();
            latch.countDown();
            return PanelAgente.ResultadoInyeccion.ENCOLADO;
        });

        invocarMetodoPrivado(panel, "enviarAAgente", new int[]{0});

        assertTrue(latch.await(TIMEOUT_LATCH_SEGUNDOS, TimeUnit.SECONDS),
            "El manejador debe invocarse aunque la terminal no esté lista");
        assertEquals(1, invocaciones.get(), "El hallazgo debe procesarse una sola vez");
    }

    @Test
    @DisplayName("Menu contextual usa el agente operativo visible cuando el seleccionado esta deshabilitado")
    void testMenuContextualUsaAgenteOperativoVisible() throws Exception {
        ConfiguracionAPI config = mock(ConfiguracionAPI.class);
        doReturn(100).when(config).obtenerMaximoHallazgosTabla();
        when(config.persistirFiltroBusquedaHallazgos()).thenReturn(false);
        when(config.persistirFiltroSeveridadHallazgos()).thenReturn(false);
        when(config.hayAlgunAgenteHabilitado()).thenReturn(true);
        when(config.agenteHabilitado()).thenReturn(false);
        when(config.obtenerTipoAgenteOperativo()).thenReturn("OPEN_CODE");
        when(config.obtenerTipoAgente()).thenReturn("FACTORY_DROID");
        panel.establecerConfiguracion(config);
        panel.establecerManejadorEnviarAAgente(h -> PanelAgente.ResultadoInyeccion.INYECTADO);

        HttpRequest request = mock(HttpRequest.class);
        when(request.url()).thenReturn("https://example.com/menu");
        agregarHallazgoConRequest(panel, request, "https://example.com/menu");
        flushEdt();

        JTable tabla = obtenerTabla(panel);
        SwingUtilities.invokeAndWait(() -> tabla.setRowSelectionInterval(0, 0));

        Method metodo = PanelHallazgos.class.getDeclaredMethod("construirMenuContextualDinamico");
        metodo.setAccessible(true);
        javax.swing.JPopupMenu menu = (javax.swing.JPopupMenu) metodo.invoke(panel);
        assertNotNull(menu, "assertNotNull failed at PanelHallazgosSendTest.java:216");

        boolean encontrado = false;
        for (java.awt.Component componente : menu.getComponents()) {
            if (componente instanceof javax.swing.JMenuItem) {
                String texto = ((javax.swing.JMenuItem) componente).getText();
                if (texto != null && texto.contains("Open Code")) {
                    encontrado = true;
                    break;
                }
            }
        }

        assertTrue(encontrado, "assertTrue failed at PanelHallazgosSendTest.java:227");
    }

    @Test
    @DisplayName("Clic derecho sobre una fila muestra el menú contextual con las acciones de envío")
    void testClicDerechoMuestraMenuContextualConAcciones() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        panel.establecerConfiguracion(config);

        HttpRequest request = mock(HttpRequest.class);
        when(request.url()).thenReturn("https://example.com/right-click");
        agregarHallazgoConRequest(panel, request, "https://example.com/right-click");
        flushEdt();

        JTable tabla = obtenerTabla(panel);
        // El menú necesita la tabla visible (JPopupMenu.show lo requiere).
        javax.swing.JFrame frame = new javax.swing.JFrame();
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame.add(tabla);
                frame.pack();
                frame.setVisible(true);
            });
            flushEdt();

            // Sin selección previa: el clic derecho debe seleccionar la fila y construir el menú.
            SwingUtilities.invokeAndWait(() -> tabla.clearSelection());
            flushEdt();
            assertEquals(0, tabla.getSelectedRowCount(), "No debe haber selección inicial");

            // Simular el clic derecho (popup trigger) sobre la fila 0, como lo haría
            // UIUtils.instalarMenuContextualTabla en el listener real.
            Rectangle celda = tabla.getCellRect(0, 0, false);
            SwingUtilities.invokeAndWait(() -> {
                MouseEvent popup = new MouseEvent(
                    tabla, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                    java.awt.event.InputEvent.BUTTON3_DOWN_MASK,
                    celda.x + 2, celda.y + 2, 1, true);
                for (java.awt.event.MouseListener listener : tabla.getMouseListeners()) {
                    listener.mousePressed(popup);
                }
            });
            flushEdt();

            // Tras el clic derecho, la fila debe quedar seleccionada.
            assertEquals(1, tabla.getSelectedRowCount(),
                "El clic derecho debe seleccionar la fila bajo el cursor");

            // Y el menú contextual debe construirse con las acciones de envío (no solo "Agregar").
            Method metodo = PanelHallazgos.class.getDeclaredMethod("construirMenuContextualDinamico");
            metodo.setAccessible(true);
            javax.swing.JPopupMenu menu = (javax.swing.JPopupMenu) metodo.invoke(panel);
            assertNotNull(menu, "El menú contextual no debe ser null");

            int items = 0;
            for (java.awt.Component componente : menu.getComponents()) {
                if (componente instanceof javax.swing.JMenuItem) {
                    items++;
                }
            }
            // Con selección debe haber al menos: Agregar + Repeater + Intruder (+ otros).
            assertTrue(items >= 3,
                "El menú tras clic derecho debe incluir varias acciones, encontradas: " + items);
        } finally {
            SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }

    @Test
    @DisplayName("Clic derecho en espacio vacío limpia selección pero sigue mostrando 'Agregar hallazgo'")
    void testClicDerechoEspacioVacioMuestraMenuConAgregar() throws Exception {
        ConfiguracionAPI config = new ConfiguracionAPI();
        panel.establecerConfiguracion(config);

        HttpRequest request = mock(HttpRequest.class);
        when(request.url()).thenReturn("https://example.com/row");
        agregarHallazgoConRequest(panel, request, "https://example.com/row");
        flushEdt();

        JTable tabla = obtenerTabla(panel);
        javax.swing.JFrame frame = new javax.swing.JFrame();
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame.add(tabla);
                frame.pack();
                frame.setVisible(true);
            });
            flushEdt();

            // Selección previa para verificar que se limpia.
            SwingUtilities.invokeAndWait(() -> tabla.setRowSelectionInterval(0, 0));
            flushEdt();
            assertEquals(1, tabla.getSelectedRowCount());

            // Clic derecho DEBAJO de la última fila (espacio vacío): rowAtPoint < 0.
            int yFueraFilas = tabla.getHeight() + 50;
            SwingUtilities.invokeAndWait(() -> {
                MouseEvent popup = new MouseEvent(
                    tabla, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                    java.awt.event.InputEvent.BUTTON3_DOWN_MASK,
                    5, yFueraFilas, 1, true);
                for (java.awt.event.MouseListener listener : tabla.getMouseListeners()) {
                    listener.mousePressed(popup);
                }
            });
            flushEdt();

            // La selección previa debe quedar limpia.
            assertEquals(0, tabla.getSelectedRowCount(),
                "El clic en espacio vacío debe limpiar la selección previa");

            // Pero el menú debe seguir construyéndose con la opción 'Agregar hallazgo',
            // ya que esa opción no depende de selección (regresión del refactor D4).
            Method metodo = PanelHallazgos.class.getDeclaredMethod("construirMenuContextualDinamico");
            metodo.setAccessible(true);
            javax.swing.JPopupMenu menu = (javax.swing.JPopupMenu) metodo.invoke(panel);
            assertNotNull(menu, "El menú debe construirse aún sin selección");

            boolean tieneAgregar = false;
            for (java.awt.Component componente : menu.getComponents()) {
                if (componente instanceof javax.swing.JMenuItem) {
                    String texto = ((javax.swing.JMenuItem) componente).getText();
                    if (texto != null && texto.equals(I18nUI.Hallazgos.MENU_AGREGAR_HALLAZGO())) {
                        tieneAgregar = true;
                        break;
                    }
                }
            }
            assertTrue(tieneAgregar,
                "El menú en espacio vacío debe incluir 'Agregar hallazgo'");
        } finally {
            SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }

    @Test
    @DisplayName("Captura construye el request desde la URL cuando no hay request en memoria")
    void testCapturaConstruyeRequestDesdeUrlSinRequestEnMemoria() throws Exception {
        panel.obtenerModelo().agregarHallazgo(
            new Hallazgo("https://example.com/sin-request", "Titulo", "Descripcion", "Low", "Low")
        );
        esperarFilas(panel, 1);

        HttpRequest construido = mock(HttpRequest.class);
        try (org.mockito.MockedStatic<HttpRequest> mockedReq = org.mockito.Mockito.mockStatic(HttpRequest.class)) {
            mockedReq.when(() -> HttpRequest.httpRequestFromUrl("https://example.com/sin-request"))
                .thenReturn(construido);

            Object captura = invocarMetodoPrivadoRetorno(panel, "capturarEntradasAccion", new int[]{0});
            assertNotNull(captura, "La captura no debe ser null");

            List<?> entradas = obtenerCampoLista(captura, "entradas");
            assertEquals(1, entradas.size(), "Debe haber una entrada");

            Object entrada = entradas.get(0);
            HttpRequest solicitud = obtenerCampo(entrada, "solicitud", HttpRequest.class);
            assertSame(construido, solicitud,
                "Sin request en memoria, la solicitud se construye desde la URL del hallazgo");
        }
    }

    /**
     * Agrega un hallazgo con request HTTP asociado al panel.
     *
     * @param panel   Panel donde agregar el hallazgo
     * @param request Request HTTP asociado
     * @param url     URL del hallazgo
     * @throws Exception si ocurre error al agregar el hallazgo
     */
    private void agregarHallazgoConRequest(PanelHallazgos panel, HttpRequest request, String url) throws Exception {
        assertNotNull(panel, "El panel no puede ser null");
        assertNotNull(request, "El request no puede ser null");
        Hallazgo hallazgo = new Hallazgo(url, "Titulo", "Descripcion", "High", "High", request);
        agregarHallazgo(panel, hallazgo);
    }

    private void agregarHallazgo(PanelHallazgos panel, Hallazgo hallazgo) throws Exception {
        assertNotNull(panel, "El panel no puede ser null");
        SwingUtilities.invokeAndWait(() -> panel.agregarHallazgo(hallazgo));
        esperarFilas(panel, 1);
    }

    /**
     * Invoca un método privado que retorna void.
     *
     * @param panel        Instancia de PanelHallazgos
     * @param nombreMetodo Nombre del método a invocar
     * @param filas        Parámetro int[] a pasar al método
     * @throws Exception si el método no existe o falla la invocación
     */
    @SuppressWarnings("PMD.UseVarargs")
    private void invocarMetodoPrivado(PanelHallazgos panel, String nombreMetodo, int[] filas) throws Exception {
        assertNotNull(panel, "El panel no puede ser null");
        Method metodo = PanelHallazgos.class.getDeclaredMethod(nombreMetodo, int[].class);
        metodo.setAccessible(true);
        metodo.invoke(panel, (Object) filas);
    }

    /**
     * Invoca un método privado con retorno.
     *
     * @param panel        Instancia de PanelHallazgos
     * @param nombreMetodo Nombre del método a invocar
     * @param filas        Parámetro int[] a pasar al método
     * @return Resultado del método invocado
     * @throws Exception si el método no existe o falla la invocación
     */
    @SuppressWarnings("PMD.UseVarargs")
    private Object invocarMetodoPrivadoRetorno(PanelHallazgos panel, String nombreMetodo, int[] filas) throws Exception {
        assertNotNull(panel, "El panel no puede ser null");
        Method metodo = PanelHallazgos.class.getDeclaredMethod(nombreMetodo, int[].class);
        metodo.setAccessible(true);
        return metodo.invoke(panel, (Object) filas);
    }

    /**
     * Espera a que el panel tenga al menos el número mínimo de filas.
     *
     * @param panel  Panel a verificar
     * @param minimo Número mínimo de filas esperadas
     * @throws Exception si ocurre error durante la espera
     */
    private void esperarFilas(PanelHallazgos panel, int minimo) throws Exception {
        assertNotNull(panel, "El panel no puede ser null");
        JTable tabla = obtenerTabla(panel);
        for (int i = 0; i < MAX_REINTENTOS_ESPERA_FILAS; i++) {
            flushEdt();
            if (panel.obtenerModelo().getRowCount() >= minimo && tabla.getRowCount() >= minimo) {
                return;
            }
            Thread.sleep(DELAY_ESPERA_FILAS_MS);
        }
        flushEdt();
    }

    /**
     * Obtiene la tabla interna del panel mediante reflexión.
     *
     * @param panel Panel del cual obtener la tabla
     * @return JTable interna del panel
     * @throws Exception si ocurre error al acceder al campo
     */
    private JTable obtenerTabla(PanelHallazgos panel) throws Exception {
        return obtenerCampo(panel, "tabla", JTable.class);
    }

    /**
     * Obtiene un campo booleano del panel mediante reflexión.
     *
     * @param panel       Panel del cual obtener el campo
     * @param nombreCampo Nombre del campo
     * @return Valor del campo como boolean
     * @throws Exception si ocurre error al acceder al campo
     */
    private boolean obtenerCampoBooleano(PanelHallazgos panel, String nombreCampo) throws Exception {
        return obtenerCampo(panel, nombreCampo, Boolean.class);
    }

    /**
     * Obtiene un campo de tipo List mediante reflexión.
     *
     * @param objeto      Objeto del cual obtener el campo
     * @param nombreCampo Nombre del campo
     * @return Valor del campo como List
     * @throws Exception si ocurre error al acceder al campo
     */
    private List<?> obtenerCampoLista(Object objeto, String nombreCampo) throws Exception {
        return obtenerCampo(objeto, nombreCampo, List.class);
    }
}
