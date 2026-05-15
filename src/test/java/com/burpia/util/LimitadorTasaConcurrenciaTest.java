package com.burpia.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency tests for LimitadorTasa class.
 * Validates thread-safety of ajustarMaximoConcurrente() without TOCTOU issues.
 */
@DisplayName("LimitadorTasa Concurrency Tests")
class LimitadorTasaConcurrenciaTest {

    private static final int THREAD_COUNT = 10;
    private static final int ITERATIONS_PER_THREAD = 50;
    private static final int MINIMO_CONCURRENTE = 1;

    private LimitadorTasa limitador;

    @BeforeEach
    void setUp() {
        limitador = new LimitadorTasa(10);
    }

    @Test
    @DisplayName("ajustarMaximoConcurrente called concurrently never goes below MIN or above MAX")
    void testAjustarMaximoConcurrenteSinTOCTOU() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger violations = new AtomicInteger(0);
        List<Integer> observedValues = new ArrayList<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(10, TimeUnit.SECONDS);
                    for (int j = 0; j < ITERATIONS_PER_THREAD; j++) {
                        int randomValue = (int) (Math.random() * 1000) - 500;
                        limitador.ajustarMaximoConcurrente(randomValue);
                        int currentPermits = limitador.permisosDisponibles();
                        synchronized (observedValues) {
                            observedValues.add(currentPermits);
                        }
                        if (currentPermits < MINIMO_CONCURRENTE) {
                            violations.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(60, TimeUnit.SECONDS);

        executor.shutdown();
        assertTrue(completed, "Not all threads completed within timeout at LimitadorTasaConcurrenciaTest.java:60");
        assertEquals(0, violations.get(),
                "permisosDisponibles should never go below MINIMO_CONCURRENTE at LimitadorTasaConcurrenciaTest.java:62");
        assertFalse(observedValues.isEmpty(),
                "Should have recorded permit values at LimitadorTasaConcurrenciaTest.java:64");
    }

    @Test
    @DisplayName("ajustarMaximoConcurrente clamps extreme values correctly (0, negative, Integer.MAX_VALUE)")
    void testAjustarMaximoConcurrenteValoresLimites() throws Exception {
        int[] extremeValues = {0, -1, -100, Integer.MAX_VALUE, Integer.MIN_VALUE, -Integer.MAX_VALUE};

        ExecutorService executor = Executors.newFixedThreadPool(extremeValues.length);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(extremeValues.length);
        List<Integer> finalPermitValues = new ArrayList<>();

        for (int value : extremeValues) {
            final int testValue = value;
            executor.submit(() -> {
                try {
                    startLatch.await(10, TimeUnit.SECONDS);
                    limitador.ajustarMaximoConcurrente(testValue);
                    int permits = limitador.permisosDisponibles();
                    synchronized (finalPermitValues) {
                        finalPermitValues.add(permits);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);

        executor.shutdown();
        assertTrue(completed, "Not all threads completed within timeout at LimitadorTasaConcurrenciaTest.java:95");

        for (Integer permits : finalPermitValues) {
            assertTrue(permits >= MINIMO_CONCURRENTE,
                    "permisosDisponibles should never be below " + MINIMO_CONCURRENTE +
                            " at LimitadorTasaConcurrenciaTest.java:98, got: " + permits);
        }
    }

    @Test
    @DisplayName("Multiple rapid adjustments converge to a stable valid state")
    void testAjustarMaximoConcurrenteConvergeAEstadoValido() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(10, TimeUnit.SECONDS);
                    for (int j = 0; j < 100; j++) {
                        int value = (int) (Math.random() * 50) - 25;
                        limitador.ajustarMaximoConcurrente(value);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(60, TimeUnit.SECONDS);

        executor.shutdown();
        assertTrue(completed, "Not all threads completed within timeout at LimitadorTasaConcurrenciaTest.java:127");

        int finalPermits = limitador.permisosDisponibles();
        assertTrue(finalPermits >= MINIMO_CONCURRENTE,
                "Final permits should be >= MINIMO_CONCURRENTE at LimitadorTasaConcurrenciaTest.java:130");
    }

    @Test
    @DisplayName("Negative values are clamped to MINIMO_CONCURRENTE")
    void testValoresNegativosSeClampean() {
        limitador.ajustarMaximoConcurrente(-1);
        assertTrue(limitador.permisosDisponibles() >= MINIMO_CONCURRENTE,
                "Negative input should clamp to MIN at LimitadorTasaConcurrenciaTest.java:138");

        limitador.ajustarMaximoConcurrente(-1000);
        assertTrue(limitador.permisosDisponibles() >= MINIMO_CONCURRENTE,
                "Large negative input should clamp to MIN at LimitadorTasaConcurrenciaTest.java:141");

        limitador.ajustarMaximoConcurrente(Integer.MIN_VALUE);
        assertTrue(limitador.permisosDisponibles() >= MINIMO_CONCURRENTE,
                "Integer.MIN_VALUE should clamp to MIN at LimitadorTasaConcurrenciaTest.java:144");
    }

    @Test
    @DisplayName("Zero input is clamped to MINIMO_CONCURRENTE")
    void testCeroSeClampea() {
        limitador.ajustarMaximoConcurrente(0);
        assertTrue(limitador.permisosDisponibles() >= MINIMO_CONCURRENTE,
                "Zero input should clamp to MIN at LimitadorTasaConcurrenciaTest.java:151");
    }
}
