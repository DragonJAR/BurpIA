package com.burpia.ui;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;

/**
 * Utilidades para tests que involucran diálogos Swing.
 *
 * <p>Problema: Los tests que muestran diálogos (JDialog, UIUtils.confirmarPregunta, etc.)
 * se quedan bloqueados esperando interacción del usuario.</p>
 *
 * <p>Solución: Esta clase proporciona métodos para:</p>
 * <ul>
 *   <li>Ejecutar código que muestra diálogos</li>
 *   <li>Validar el comportamiento del diálogo</li>
   *   <li>Cerrar automáticamente todos los diálogos abiertos después</li>
 * </ul>
 *
 * <p>Uso típico:</p>
 * <pre>{@code
 * // En @BeforeEach:
 * TestDialogUtils.registrarCapturaDialogos();
 *
 * // En test:
 * TestDialogUtils.ejecutarConDialogoAutoCerrado(() -> {
 *     UIUtils.confirmarPregunta(panel, "Título", "Mensaje");
 * }, 100);
 *
 * // En @AfterEach:
 * TestDialogUtils.limpiarDialogosPendientes();
 * TestDialogUtils.desregistrarCapturaDialogos();
 * }</pre>
 */
public class TestDialogUtils {

    private static final List<Window> ventanasCapturadas = new CopyOnWriteArrayList<>();
    private static final AtomicBoolean capturaActiva = new AtomicBoolean(false);
    private static volatile Thread backgroundCleaner;

    /**
     * Registra listener para capturar todas las ventanas abiertas durante los tests.
     * Debe llamarse en @BeforeEach.
     */
    public static void registrarCapturaDialogos() {
        if (capturaActiva.get()) {
            return;
        }
        capturaActiva.set(true);
        ventanasCapturadas.clear();

        // Iniciar thread en background que cierra diálogos automáticamente
        iniciarBackgroundCleaner();
    }

    /**
     * Inicia un thread demonio que cierra ventanas modales automáticamente.
     */
    private static void iniciarBackgroundCleaner() {
        backgroundCleaner = new Thread(() -> {
            while (capturaActiva.get()) {
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        Window[] ventanas = Window.getWindows();
                        for (Window ventana : ventanas) {
                            if (ventana.isVisible() && ventana instanceof JDialog) {
                                // Rastrear ventana capturada
                                if (!ventanasCapturadas.contains(ventana)) {
                                    ventanasCapturadas.add(ventana);
                                }
                                // Cerrar diálogos modales automáticamente
                                ventana.dispose();
                            }
                        }
                    });
                    Thread.sleep(50); // Revisar cada 50ms
                } catch (Exception e) {
                    // Ignorar errores y continuar
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                }
            }
        });
        backgroundCleaner.setDaemon(true);
        backgroundCleaner.setName("TestDialogUtils-Cleaner");
        backgroundCleaner.start();
    }

    /**
     * Desregistra captura de diálogos y limpia ventanas pendientes.
     * Debe llamarse en @AfterEach.
     */
    public static void desregistrarCapturaDialogos() {
        capturaActiva.set(false);

        // Detener thread background
        if (backgroundCleaner != null) {
            backgroundCleaner.interrupt();
            try {
                backgroundCleaner.join(1000); // Esperar máximo 1 segundo
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            backgroundCleaner = null;
        }

        limpiarDialogosPendientes();
        ventanasCapturadas.clear();
    }

    /**
     * Ejecuta una accion que puede mostrar dialogos, cierra automaticamente
     * todos los dialogos abiertos despues del tiempo especificado.
     *
     * @param accion Accion a ejecutar que puede mostrar dialogos
     * @param delayMs Milisegundos a esperar antes de cerrar dialogos (default: 100ms)
     */
    public static void ejecutarConDialogoAutoCerrado(Runnable accion, long delayMs) {
        if (!capturaActiva.get()) {
            throw new IllegalStateException("Debe llamar registrarCapturaDialogos() primero en @BeforeEach");
        }

        if (accion == null) {
            throw new IllegalArgumentException("La accion no puede ser null");
        }

        try {
            // Ejecutar la accion
            accion.run();

            // Esperar un momento para que los dialogos se muestren y procesen
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Cerrar todas las ventanas abiertas durante la accion
            limpiarDialogosPendientes();

        } catch (Exception e) {
            // Asegurarnos de limpiar incluso si hay excepcion
            limpiarDialogosPendientes();
            throw new RuntimeException(e);
        }
    }

    /**
     * Ejecuta una acción con diálogos y los cierra rápidamente (default 50ms).
     */
    public static void ejecutarConDialogoAutoCerrado(Runnable accion) {
        ejecutarConDialogoAutoCerrado(accion, 50);
    }

    /**
     * Cierra todos los dialogos/ventanas abiertas durante los tests.
     * Llamar en @AfterEach o manualmente cuando sea necesario.
     */
    public static void limpiarDialogosPendientes() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                // Cerrar TODAS las ventanas visibles (JDialog, JOptionPane, etc.)
                Window[] todasLasVentanas = Window.getWindows();

                // Cerrar en orden inverso para evitar problemas con dialogos modales
                for (int i = todasLasVentanas.length - 1; i >= 0; i--) {
                    Window ventana = todasLasVentanas[i];
                    if (ventana != null && ventana.isVisible()) {
                        // Cerrar cualquier tipo de dialogo o alerta
                        ventana.dispose();
                    }
                }
            });
        } catch (Exception e) {
            // Ignorar errores en cierre, pero registrar
            System.err.println("TestDialogUtils: Error cerrando ventanas: " + e.getMessage());
        } finally {
            ventanasCapturadas.clear();
        }
    }

    /**
     * Verifica si hay ventanas pendientes por cerrar.
     * Util para debugging de tests.
     *
     * @return Numero de ventanas capturadas durante el test
     */
    public static int contarVentanasPendientes() {
        return ventanasCapturadas.size();
    }

    /**
     * Cuenta el numero de ventanas visibles actualmente.
     * Util para debugging y validacion de limpieza.
     *
     * @return Numero de ventanas visibles
     */
    public static int contarVentanasVisibles() {
        int count = 0;
        for (Window ventana : Window.getWindows()) {
            if (ventana != null && ventana.isVisible()) {
                count++;
            }
        }
        return count;
    }
}
