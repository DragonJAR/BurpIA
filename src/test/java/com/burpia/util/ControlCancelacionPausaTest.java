package com.burpia.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ControlCancelacionPausa Tests")
class ControlCancelacionPausaTest {

    private BooleanSupplier canceladaTrue;
    private BooleanSupplier canceladaFalse;
    private BooleanSupplier pausadaTrue;
    private BooleanSupplier pausadaFalse;

    @BeforeEach
    void setUp() {
        canceladaTrue = () -> true;
        canceladaFalse = () -> false;
        pausadaTrue = () -> true;
        pausadaFalse = () -> false;
    }

    @Test
    @DisplayName("Constructor con suppliers válidos retorna valores correctos")
    void testConstructorConSuppliersValidos() {
        ControlCancelacionPausa ctrl = new ControlCancelacionPausa(canceladaTrue, pausadaTrue);
        assertTrue(ctrl.esCancelada(), "assertTrue failed at ControlCancelacionPausaTest.java:30");
        assertTrue(ctrl.esPausada(), "assertTrue failed at ControlCancelacionPausaTest.java:31");
    }

    @Test
    @DisplayName("Constructor con suppliers nulos usa false por defecto sin NPE")
    void testConstructorConSuppliersNulosUsaFalsePorDefecto() {
        ControlCancelacionPausa ctrl = new ControlCancelacionPausa(null, null);
        assertFalse(ctrl.esCancelada(), "assertFalse failed at ControlCancelacionPausaTest.java:37");
        assertFalse(ctrl.esPausada(), "assertFalse failed at ControlCancelacionPausaTest.java:38");
    }

    @Test
    @DisplayName("esCancelada retorna true cuando supplier retorna true")
    void testEstaCanceladaRetornaTrue() {
        ControlCancelacionPausa ctrl = new ControlCancelacionPausa(canceladaTrue, pausadaFalse);
        assertTrue(ctrl.esCancelada(), "assertTrue failed at ControlCancelacionPausaTest.java:44");
    }

    @Test
    @DisplayName("esCancelada retorna false cuando supplier retorna false")
    void testEstaCanceladaRetornaFalse() {
        ControlCancelacionPausa ctrl = new ControlCancelacionPausa(canceladaFalse, pausadaFalse);
        assertFalse(ctrl.esCancelada(), "assertFalse failed at ControlCancelacionPausaTest.java:50");
    }

    @Test
    @DisplayName("esPausada retorna true cuando supplier retorna true")
    void testEstaPausadaRetornaTrue() {
        ControlCancelacionPausa ctrl = new ControlCancelacionPausa(canceladaFalse, pausadaTrue);
        assertTrue(ctrl.esPausada(), "assertTrue failed at ControlCancelacionPausaTest.java:56");
    }

    @Test
    @DisplayName("esPausada retorna false cuando supplier retorna false")
    void testEstaPausadaRetornaFalse() {
        ControlCancelacionPausa ctrl = new ControlCancelacionPausa(canceladaFalse, pausadaFalse);
        assertFalse(ctrl.esPausada(), "assertFalse failed at ControlCancelacionPausaTest.java:62");
    }

    @Test
    @DisplayName("verificarCancelacion lanza InterruptedException cuando está cancelada")
    void testVerificarCancelacionLanzaExcepcionCuandoCancelada() {
        ControlCancelacionPausa ctrl = new ControlCancelacionPausa(canceladaTrue, pausadaFalse);
        InterruptedException thrown = assertThrows(InterruptedException.class,
                ctrl::verificarCancelacion,
                "assertThrows failed at ControlCancelacionPausaTest.java:70");
        assertEquals("Tarea cancelada", thrown.getMessage(),
                "assertEquals failed at ControlCancelacionPausaTest.java:71");
    }

    @Test
    @DisplayName("verificarCancelacion no hace nada cuando no está cancelada")
    void testVerificarCancelacionNoHaceNadaCuandoNoCancelada() {
        ControlCancelacionPausa ctrl = new ControlCancelacionPausa(canceladaFalse, pausadaFalse);
        assertDoesNotThrow(ctrl::verificarCancelacion,
                "assertDoesNotThrow failed at ControlCancelacionPausaTest.java:78");
    }

    @Test
    @DisplayName("esperarSiPausada bloquea cuando está pausada")
    void testEsperarSiPausadaBloqueaCuandoPausada() {
        AtomicBoolean pausada = new AtomicBoolean(true);
        ControlCancelacionPausa ctrl = new ControlCancelacionPausa(() -> false, pausada::get);

        assertTimeoutPreemptively(java.time.Duration.ofMillis(300), () -> {
            Thread worker = new Thread(() -> {
                try {
                    ctrl.esperarSiPausada();
                } catch (InterruptedException e) {
                    // Esperado cuando se forcefully interrupts
                }
            });
            worker.start();

            // El hilo debería estar bloqueado por al menos 250ms (un ciclo de polling)
            Thread.sleep(100);
            assertTrue(worker.isAlive(), "assertTrue failed at ControlCancelacionPausaTest.java:96");

            // Interrumpir y verificar que se detuvo
            worker.interrupt();
            worker.join(500);
        }, "assertTimeoutPreemptively failed at ControlCancelacionPausaTest.java:100");
    }

    @Test
    @DisplayName("esperarSiPausada retorna inmediatamente cuando no está pausada")
    void testEsperarSiPausadaRetornaCuandoDespausada() {
        AtomicBoolean pausada = new AtomicBoolean(false);
        ControlCancelacionPausa ctrl = new ControlCancelacionPausa(() -> false, pausada::get);

        long inicio = System.currentTimeMillis();
        assertDoesNotThrow(ctrl::esperarSiPausada,
                "assertDoesNotThrow failed at ControlCancelacionPausaTest.java:110");
        long duracion = System.currentTimeMillis() - inicio;

        assertTrue(duracion < 100, "assertTrue failed at ControlCancelacionPausaTest.java:112 - debería retornar casi inmediatamente");
    }

    @Test
    @DisplayName("esperarSiPausada retorna cuando se despausa durante espera")
    void testEsperarSiPausadaRetornaCuandoDespausadaDuranteEspera() throws InterruptedException {
        AtomicBoolean pausada = new AtomicBoolean(true);
        ControlCancelacionPausa ctrl = new ControlCancelacionPausa(() -> false, pausada::get);

        Thread worker = new Thread(() -> {
            try {
                ctrl.esperarSiPausada();
            } catch (InterruptedException e) {
                // No es error - el test lo interrumpe
            }
        });
        worker.start();

        // Esperar a que el hilo esté bloqueado en esperarSiPausada
        Thread.sleep(150);
        assertTrue(pausada.get(), "assertTrue failed at ControlCancelacionPausaTest.java:128");

        // Despausar
        pausada.set(false);

        // Esperar a que continúe ( polling interval es 250ms)
        worker.join(500);
        assertFalse(worker.isAlive(), "assertFalse failed at ControlCancelacionPausaTest.java:133");
    }

    @Test
    @DisplayName("esperarConControl combina pausa y cancelación")
    void testEsperarConControlCombinaPausaYCancelacion() throws InterruptedException {
        AtomicBoolean pausada = new AtomicBoolean(false);
        AtomicBoolean cancelada = new AtomicBoolean(false);
        AtomicReference<Throwable> errorCapturado = new AtomicReference<>();

        ControlCancelacionPausa ctrl = new ControlCancelacionPausa(cancelada::get, pausada::get);

        Thread worker = new Thread(() -> {
            try {
                ctrl.esperarConControl(500);
            } catch (InterruptedException e) {
                errorCapturado.set(e);
            }
        });
        worker.start();

        // Pausar a mitad del wait
        Thread.sleep(100);
        pausada.set(true);

        // Despausar antes de que termine
        Thread.sleep(200);
        pausada.set(false);

        // Cancelar antes de que termine el todo el tiempo
        Thread.sleep(150);
        cancelada.set(true);

        worker.join(500);

        if (worker.isAlive()) {
            worker.interrupt();
        }

        // Debería haber lanzado InterruptedException por cancelación
        assertNotNull(errorCapturado.get(), "assertNotNull failed at ControlCancelacionPausaTest.java:163");
        assertTrue(errorCapturado.get() instanceof InterruptedException,
                "assertInstanceOf failed at ControlCancelacionPausaTest.java:164");
    }

    @Test
    @DisplayName("Thread safety - lecturas concurrentes sin excepciones")
    void testThreadSafetyLecturasConcurrentes() throws InterruptedException {
        AtomicBoolean cancelada = new AtomicBoolean(false);
        AtomicBoolean pausada = new AtomicBoolean(false);
        ControlCancelacionPausa ctrl = new ControlCancelacionPausa(cancelada::get, pausada::get);

        int hilos = 10;
        int iteraciones = 1000;
        Thread[] threads = new Thread[hilos];
        Exception[] excepciones = new Exception[hilos];

        for (int i = 0; i < hilos; i++) {
            final int id = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < iteraciones; j++) {
                        ctrl.esCancelada();
                        ctrl.esPausada();
                        // Alternar estados para max stress
                        if (j % 100 == 0) {
                            cancelada.set(id % 2 == 0);
                            pausada.set(id % 3 == 0);
                        }
                    }
                } catch (Exception e) {
                    excepciones[id] = e;
                }
            });
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            t.join(5000);
        }

        for (int i = 0; i < hilos; i++) {
            assertNull(excepciones[i], "assertNull failed at ControlCancelacionPausaTest.java:195 - hilo " + i + " lanzó: " + excepciones[i]);
        }
    }
}
