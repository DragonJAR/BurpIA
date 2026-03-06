package com.burpia.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.BurpSuiteEdition;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.scanner.audit.Audit;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.Hallazgo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import javax.swing.JTable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
class PanelHallazgosSendTest {

    private static final int TIMEOUT_VERIFICACION_MS = 1000;
    private static final int TIMEOUT_LATCH_SEGUNDOS = 1;
    private static final int MAX_REINTENTOS_ESPERA_FILAS = 25;
    private static final int DELAY_ESPERA_FILAS_MS = 20;

    private PanelHallazgos panel;
    private MontoyaApi api;

    @BeforeEach
    void setUp() throws Exception {
        api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        panel = crearPanel(api, true);
    }

    @AfterEach
    void tearDown() {
        if (panel != null) {
            panel.destruir();
            panel = null;
        }
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
    @DisplayName("Enviar a Scanner ejecuta la ruta sin lanzar errores")
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

        assertDoesNotThrow(() -> invocarMetodoPrivado(panel, "enviarAScanner", new int[]{0}));
    }

    @Test
    @DisplayName("Enviar a Issues ejecuta la ruta sin lanzar errores")
    void testEnviarAIssues() throws Exception {
        burp.api.montoya.sitemap.SiteMap siteMap = mock(burp.api.montoya.sitemap.SiteMap.class);
        when(api.burpSuite().version().edition()).thenReturn(BurpSuiteEdition.PROFESSIONAL);
        when(api.ai().isEnabled()).thenReturn(true);
        when(api.siteMap()).thenReturn(siteMap);

        assertTrue(obtenerCampoBooleano(panel, "integracionIssuesDisponible"), "assertTrue failed at PanelHallazgosSendTest.java:125");
        SwingUtilities.invokeAndWait(() -> panel.establecerGuardadoAutomaticoIssuesActivo(false));

        HttpRequest request = mock(HttpRequest.class);
        when(request.url()).thenReturn("https://example.com/issues");
        agregarHallazgoConRequest(panel, request, "https://example.com/issues");
        assertTrue(panel.obtenerModelo().getRowCount() >= 1, "assertTrue failed at PanelHallazgosSendTest.java:131");

        assertDoesNotThrow(() -> invocarMetodoPrivado(panel, "enviarAIssues", new int[]{0}));
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
            return true;
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
            return true;
        });

        invocarMetodoPrivado(panel, "enviarAAgente", new int[]{0});

        assertTrue(latch.await(TIMEOUT_LATCH_SEGUNDOS, TimeUnit.SECONDS), "assertTrue failed at PanelHallazgosSendTest.java:191");
        assertEquals(1, enviados.get(), "assertEquals failed at PanelHallazgosSendTest.java:192");
    }

    @Test
    @DisplayName("Captura no reconstruye request cuando falta evidencia original")
    void testCapturaNoReconstruyeRequestSinEvidenciaOriginal() throws Exception {
        panel.obtenerModelo().agregarHallazgo(
            new Hallazgo("https://example.com/sin-request", "Titulo", "Descripcion", "Low", "Low")
        );
        esperarFilas(panel, 1);

        Object captura = invocarMetodoPrivadoRetorno(panel, "capturarEntradasAccion", new int[]{0});
        assertNotNull(captura, "La captura no debe ser null");

        List<?> entradas = obtenerCampoLista(captura, "entradas");
        assertEquals(1, entradas.size(), "assertEquals failed at PanelHallazgosSendTest.java:207");

        Object entrada = entradas.get(0);
        HttpRequest solicitud = obtenerCampo(entrada, "solicitud", HttpRequest.class);
        assertNull(solicitud, "La solicitud debe ser null cuando no hay evidencia original");
    }

    /**
     * Crea una instancia de PanelHallazgos para testing.
     *
     * @param api                 MontoyaApi mockeado
     * @param esBurpProfessional  Si es Burp Professional
     * @return PanelHallazgos configurado para testing
     * @throws Exception si ocurre error en la creación del panel
     */
    private PanelHallazgos crearPanel(MontoyaApi api, boolean esBurpProfessional) throws Exception {
        assertNotNull(api, "La API no puede ser null");
        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> 
            holder[0] = new PanelHallazgos(api, new ModeloTablaHallazgos(100), esBurpProfessional)
        );
        return holder[0];
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
        panel.obtenerModelo().agregarHallazgo(hallazgo);
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
     * Fuerza la ejecución de todas las tareas pendientes en el EDT.
     *
     * @throws Exception si ocurre error al sincronizar con el EDT
     */
    private void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
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
    @SuppressWarnings("unchecked")
    private List<?> obtenerCampoLista(Object objeto, String nombreCampo) throws Exception {
        assertNotNull(objeto, "El objeto no puede ser null");
        Field field = objeto.getClass().getDeclaredField(nombreCampo);
        field.setAccessible(true);
        return (List<?>) field.get(objeto);
    }

    /**
     * Obtiene un campo de un objeto mediante reflexión.
     *
     * @param objeto      Objeto del cual obtener el campo
     * @param nombreCampo Nombre del campo
     * @param tipoEsperado Clase del tipo esperado
     * @param <T> Tipo del campo
     * @return Valor del campo
     * @throws Exception si ocurre error al acceder al campo
     */
    @SuppressWarnings({"unchecked", "PMD.UnusedFormalParameter"})
    private <T> T obtenerCampo(Object objeto, String nombreCampo, Class<T> tipoEsperado) throws Exception {
        assertNotNull(objeto, "El objeto no puede ser null");
        Field field = objeto.getClass().getDeclaredField(nombreCampo);
        field.setAccessible(true);
        return (T) field.get(objeto);
    }
}
