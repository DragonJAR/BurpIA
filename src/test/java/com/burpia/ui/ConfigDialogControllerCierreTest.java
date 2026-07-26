package com.burpia.ui;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.GestorConfiguracion;
import com.burpia.i18n.I18nUI;
import com.burpia.util.ConnectionTester;
import com.burpia.util.RutasBurpIA;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Tests de regresión para el cierre del diálogo de configuración (H2) y la
 * restauración del botón de refresco de modelos con callbacks stale (H1).
 */
@DisplayName("ConfigDialogController - Cierre y callbacks async")
class ConfigDialogControllerCierreTest {

    private static final int LIMITE_CERRAR_NO_BLOQUEANTE_MS = 1500;
    private static final int TIMEOUT_VERIFICACION_MS = 3000;

    @TempDir
    Path tempDir;

    private String originalUserHome;

    @BeforeEach
    void setUp() throws Exception {
        RutasBurpIA.limpiarCacheParaTests();
        TestDialogUtils.registrarCapturaDialogos();

        originalUserHome = System.getProperty("user.home");
        Path homeAislado = Files.createTempDirectory(tempDir, "home-");
        Files.createDirectories(homeAislado.resolve(".burpia"));
        System.setProperty("user.home", homeAislado.toString());
    }

    @AfterEach
    void tearDown() {
        TestDialogUtils.limpiarDialogosPendientes();
        TestDialogUtils.desregistrarCapturaDialogos();
        RutasBurpIA.limpiarCacheParaTests();

        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }
    }

    @Test
    @DisplayName("cerrar() no bloquea el hilo llamante aunque el tester tarde en cerrar (H2a)")
    void testCerrarNoBloqueaHiloLlamante() throws Exception {
        DialogoConfiguracion dialogo = crearDialogo();
        try {
            ConfigDialogController controller = obtenerController(dialogo);
            CountDownLatch bloqueo = new CountDownLatch(1);
            AtomicReference<Thread> hiloCierre = new AtomicReference<>();
            ConnectionTester tester = mock(ConnectionTester.class);
            doAnswer(inv -> {
                hiloCierre.set(Thread.currentThread());
                bloqueo.await(TIMEOUT_VERIFICACION_MS, TimeUnit.MILLISECONDS);
                return null;
            }).when(tester).cerrar();
            reemplazarConnectionTester(controller, tester);

            long inicio = System.nanoTime();
            controller.cerrar();
            long duracionMs = (System.nanoTime() - inicio) / 1_000_000L;

            assertTrue(duracionMs < LIMITE_CERRAR_NO_BLOQUEANTE_MS,
                "cerrar() no debe esperar al cierre del tester en el hilo llamante (EDT)");

            bloqueo.countDown();
            verify(tester, timeout(TIMEOUT_VERIFICACION_MS).atLeastOnce()).cerrar();
            Thread hilo = hiloCierre.get();
            assertNotNull(hilo, "El cierre del tester debe correr en un hilo dedicado");
            assertTrue(hilo.isDaemon(), "El hilo de cierre debe ser daemon");
        } finally {
            dialogo.dispose();
        }
    }

    @Test
    @DisplayName("Callback stale de modelos restaura el texto del botón refrescar (H1)")
    void testCallbackStaleRestauraBotonRefrescar() throws Exception {
        DialogoConfiguracion dialogo = crearDialogo();
        try {
            ConfigDialogController controller = obtenerController(dialogo);
            AtomicReference<ConnectionTester.CallbackModelos> callback =
                    capturarCallbackModelos(controller);

            JButton btnRefrescar = dialogo.obtenerBtnRefrescarModelos();
            assertNotNull(btnRefrescar, "El diálogo debe exponer el botón de refresco");
            SwingUtilities.invokeAndWait(btnRefrescar::doClick);
            assertNotNull(callback.get(), "El refresco debe haber pedido modelos al tester");
            SwingUtilities.invokeAndWait(() ->
                assertEquals(I18nUI.Configuracion.BOTON_ACTUALIZANDO_MODELOS(), btnRefrescar.getText(),
                    "Durante la carga el botón muestra el estado de actualización"));

            // Simula el cambio de proveedor con la carga en vuelo: invalida la secuencia.
            AtomicLong secuencia = (AtomicLong) obtenerCampoController(controller, "secuenciaRefrescoModelos");
            secuencia.incrementAndGet();

            callback.get().alExito(List.of("modelo-stale"));
            SwingUtilities.invokeAndWait(() -> {});

            SwingUtilities.invokeAndWait(() -> {
                assertEquals(I18nUI.Configuracion.BOTON_CARGAR_MODELOS(), btnRefrescar.getText(),
                    "Un callback stale debe restaurar el texto del botón");
                assertTrue(btnRefrescar.isEnabled(),
                    "El botón debe quedar habilitado según el proveedor actual");
            });
        } finally {
            SwingUtilities.invokeAndWait(dialogo::dispose);
        }
    }

    @Test
    @DisplayName("Callbacks async se ignoran cuando el diálogo ya no es displayable (H2b)")
    void testCallbackIgnoradoTrasDispose() throws Exception {
        DialogoConfiguracion dialogo = crearDialogo();
        ConfigDialogController controller = obtenerController(dialogo);
        AtomicReference<ConnectionTester.CallbackModelos> callback =
                capturarCallbackModelos(controller);

        JButton btnRefrescar = dialogo.obtenerBtnRefrescarModelos();
        SwingUtilities.invokeAndWait(btnRefrescar::doClick);
        assertNotNull(callback.get(), "El refresco debe haber pedido modelos al tester");

        SwingUtilities.invokeAndWait(dialogo::dispose);
        assertFalse(dialogo.isDisplayable(), "Tras dispose el diálogo no debe ser displayable");
        TestDialogUtils.reiniciarDialogosMensajeCapturados();

        callback.get().alExito(List.of("modelo-post-dispose"));
        SwingUtilities.invokeAndWait(() -> {});
        // Margen para que un hipotético diálogo huérfano llegara a mostrarse.
        Thread.sleep(150);
        SwingUtilities.invokeAndWait(() -> {});

        assertFalse(TestDialogUtils.seCapturoDialogoMensaje(),
            "Tras dispose no debe mostrarse ningún diálogo desde callbacks async");
    }

    private DialogoConfiguracion crearDialogo() {
        GestorConfiguracion gestor = new GestorConfiguracion();
        return new DialogoConfiguracion(null, new ConfiguracionAPI(), gestor, () -> {});
    }

    private ConfigDialogController obtenerController(DialogoConfiguracion dialogo) throws Exception {
        return (ConfigDialogController) obtenerCampo(dialogo, DialogoConfiguracion.class, "controller");
    }

    private Object obtenerCampoController(ConfigDialogController controller, String nombreCampo) throws Exception {
        return obtenerCampo(controller, ConfigDialogController.class, nombreCampo);
    }

    private Object obtenerCampo(Object objetivo, Class<?> clase, String nombreCampo) throws Exception {
        Field campo = clase.getDeclaredField(nombreCampo);
        campo.setAccessible(true);
        return campo.get(objetivo);
    }

    private void reemplazarConnectionTester(ConfigDialogController controller, ConnectionTester tester) throws Exception {
        Field campo = ConfigDialogController.class.getDeclaredField("connectionTester");
        campo.setAccessible(true);
        campo.set(controller, tester);
    }

    private AtomicReference<ConnectionTester.CallbackModelos> capturarCallbackModelos(
            ConfigDialogController controller) throws Exception {
        AtomicReference<ConnectionTester.CallbackModelos> callback = new AtomicReference<>();
        ConnectionTester tester = mock(ConnectionTester.class);
        doAnswer(inv -> {
            callback.set(inv.getArgument(1));
            return null;
        }).when(tester).obtenerModelosDisponibles(any(ConfiguracionAPI.class), any());
        reemplazarConnectionTester(controller, tester);
        return callback;
    }
}
