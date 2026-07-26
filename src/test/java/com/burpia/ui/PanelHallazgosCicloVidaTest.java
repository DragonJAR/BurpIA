package com.burpia.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.scanner.audit.Audit;
import com.burpia.model.Hallazgo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de ciclo de vida de PanelHallazgos (H3/H4):
 * el TableModelListener se remueve en destruir() y el Audit de Scanner
 * se reutiliza durante la sesión del panel.
 */
@DisplayName("PanelHallazgos Ciclo de Vida Tests")
class PanelHallazgosCicloVidaTest {

    private static final int TIMEOUT_VERIFICACION_MS = 2000;

    private MontoyaApi api;

    @BeforeEach
    void setUp() {
        TestDialogUtils.registrarCapturaDialogos();
        api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
    }

    @AfterEach
    void tearDown() {
        TestDialogUtils.limpiarDialogosPendientes();
        TestDialogUtils.desregistrarCapturaDialogos();
    }

    @Test
    @DisplayName("destruir() remueve el TableModelListener del modelo compartido")
    void testDestruirRemueveListenerDelModelo() throws Exception {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(100);

        PanelHallazgos panel = crearPanel(api, modelo);
        int listenersConPanel = modelo.getTableModelListeners().length;

        panel.destruir();

        assertEquals(listenersConPanel - 1, modelo.getTableModelListeners().length,
            "destruir() debe remover el listener de empty state para no retener el panel");
    }

    @Test
    @DisplayName("Envíos a Scanner reutilizan un único Audit por sesión del panel")
    void testEnviarAScannerReutilizaUnSoloAudit() throws Exception {
        burp.api.montoya.scanner.Scanner scanner = mock(burp.api.montoya.scanner.Scanner.class);
        when(api.scanner()).thenReturn(scanner);
        Audit audit = mock(Audit.class);
        when(scanner.startAudit(any())).thenReturn(audit);

        PanelHallazgos panel = crearPanel(api, new ModeloTablaHallazgos(100));
        try {
            HttpRequest request = mock(HttpRequest.class);
            when(request.url()).thenReturn("https://example.com/scanner");
            agregarHallazgo(panel, request, "https://example.com/scanner");

            invocarEnviarAScanner(panel);
            invocarEnviarAScanner(panel);

            verify(scanner, timeout(TIMEOUT_VERIFICACION_MS).times(1)).startAudit(any());
            verify(audit, timeout(TIMEOUT_VERIFICACION_MS).times(2)).addRequest(any());
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("destruir() libera la referencia al Audit de Scanner sin borrarlo")
    void testDestruirLiberaReferenciaAuditoria() throws Exception {
        burp.api.montoya.scanner.Scanner scanner = mock(burp.api.montoya.scanner.Scanner.class);
        when(api.scanner()).thenReturn(scanner);
        Audit audit = mock(Audit.class);
        when(scanner.startAudit(any())).thenReturn(audit);

        PanelHallazgos panel = crearPanel(api, new ModeloTablaHallazgos(100));
        HttpRequest request = mock(HttpRequest.class);
        when(request.url()).thenReturn("https://example.com/scanner2");
        agregarHallazgo(panel, request, "https://example.com/scanner2");

        invocarEnviarAScanner(panel);
        verify(scanner, timeout(TIMEOUT_VERIFICACION_MS).times(1)).startAudit(any());
        assertNotNull(obtenerCampo(panel, "auditoriaScannerActiva"),
            "El audit debe quedar referenciado tras el primer envío");

        panel.destruir();

        assertNull(obtenerCampo(panel, "auditoriaScannerActiva"),
            "destruir() debe soltar la referencia al audit");
        // No se verifica audit.delete(): borrarlo eliminaría también los
        // resultados ya emitidos en el Scanner de Burp.
    }

    private PanelHallazgos crearPanel(MontoyaApi montoyaApi, ModeloTablaHallazgos modelo) throws Exception {
        assertNotNull(montoyaApi, "La API no puede ser null");
        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() ->
            // El factory estático de AuditConfiguration requiere un Burp en
            // ejecución; en tests se sustituye por un mock.
            holder[0] = new PanelHallazgos(montoyaApi, modelo, true) {
                @Override
                protected burp.api.montoya.scanner.AuditConfiguration crearConfiguracionAuditoriaScanner() {
                    return mock(burp.api.montoya.scanner.AuditConfiguration.class);
                }
            }
        );
        return holder[0];
    }

    private void agregarHallazgo(PanelHallazgos panel, HttpRequest request, String url) throws Exception {
        Hallazgo hallazgo = new Hallazgo(url, "Titulo", "Descripcion", "High", "High", request);
        SwingUtilities.invokeAndWait(() -> panel.agregarHallazgo(hallazgo));
        SwingUtilities.invokeAndWait(() -> {});
    }

    @SuppressWarnings("PMD.UseVarargs")
    private void invocarEnviarAScanner(PanelHallazgos panel) throws Exception {
        Method metodo = PanelHallazgos.class.getDeclaredMethod("enviarAScanner", int[].class);
        metodo.setAccessible(true);
        metodo.invoke(panel, (Object) new int[]{0});
    }

    private Object obtenerCampo(PanelHallazgos panel, String nombreCampo) throws Exception {
        Field campo = PanelHallazgos.class.getDeclaredField(nombreCampo);
        campo.setAccessible(true);
        return campo.get(panel);
    }
}
