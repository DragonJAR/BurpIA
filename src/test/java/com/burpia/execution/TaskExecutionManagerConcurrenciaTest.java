package com.burpia.execution;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.ui.ModeloTablaTareas;
import com.burpia.ui.PestaniaPrincipal;
import com.burpia.util.ControlBackpressureGlobal;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorTareas;
import com.burpia.util.LimitadorTasa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Concurrency tests for TaskExecutionManager class.
 * Validates thread-safety of internal operations and thread naming patterns.
 */
@DisplayName("TaskExecutionManager Concurrency Tests")
class TaskExecutionManagerConcurrenciaTest {

    private GestorTareas gestorTareas;
    private TaskExecutionManager manager;
    private PrintWriter stdout;
    private PrintWriter stderr;

    @BeforeEach
    void setUp() {
        stdout = new PrintWriter(new StringWriter(), true);
        stderr = new PrintWriter(new StringWriter(), true);
    }

    @AfterEach
    void tearDown() {
        if (manager != null) {
            manager.shutdown();
        }
        if (gestorTareas != null) {
            gestorTareas.detener();
        }
    }

    private TaskExecutionManager crearManager() {
        return crearManager(null);
    }

    private TaskExecutionManager crearManager(PestaniaPrincipal pestaniaPrincipal) {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerMaximoConcurrente(5);
        gestorTareas = new GestorTareas(new ModeloTablaTareas(), mensaje -> { });
        return new TaskExecutionManager(
                config,
                gestorTareas,
                null,
                pestaniaPrincipal,
                stdout,
                stderr,
                new LimitadorTasa(5),
                null
        );
    }

    @Test
    @DisplayName("programarAnalisis called concurrently does not throw ConcurrentModificationException")
    void testProgramarAnalisisConcurrenteNoLanzaExcepcion() throws Exception {
        manager = crearManager();
        int threadCount = 10;
        int analysesPerThread = 5;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger exceptionCount = new AtomicInteger(0);
        List<String> tareaIds = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    startLatch.await(10, TimeUnit.SECONDS);
                    for (int j = 0; j < analysesPerThread; j++) {
                        try {
                            SolicitudAnalisis solicitud = new SolicitudAnalisis(
                                    "http://example.com/" + threadNum + "/" + j,
                                    "GET",
                                    "GET /api HTTP/1.1\nHost: example.com",
                                    "",
                                    "hash-" + threadNum + "-" + j
                            );
                            String tareaId = manager.programarAnalisis(solicitud, null, "test");
                            synchronized (tareaIds) {
                                if (tareaId != null) {
                                    tareaIds.add(tareaId);
                                }
                            }
                        } catch (Exception e) {
                            exceptionCount.incrementAndGet();
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
        assertTrue(completed, "Not all threads completed within timeout at TaskExecutionManagerConcurrenciaTest.java:92");
        assertEquals(0, exceptionCount.get(),
                "No exception should be thrown during concurrent programming at TaskExecutionManagerConcurrenciaTest.java:94");
        assertFalse(tareaIds.isEmpty(),
                "Should have created some tasks at TaskExecutionManagerConcurrenciaTest.java:96");
    }

    @Test
    @DisplayName("Multiple reencolarTarea calls do not cause issues")
    void testReencolarTareaConcurrenteNoCausaProblemas() throws Exception {
        manager = crearManager();

        SolicitudAnalisis solicitud = new SolicitudAnalisis(
                "http://example.com/retry",
                "GET",
                "GET /retry HTTP/1.1\nHost: example.com",
                "",
                "hash-retry"
        );

        String tareaId = manager.programarAnalisis(solicitud, null, "test");

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(10, TimeUnit.SECONDS);
                    for (int j = 0; j < 10; j++) {
                        try {
                            manager.reencolarTarea(tareaId);
                        } catch (Exception e) {
                            exceptionCount.incrementAndGet();
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
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);

        executor.shutdown();
        assertTrue(completed, "Not all threads completed within timeout at TaskExecutionManagerConcurrenciaTest.java:138");
        assertEquals(0, exceptionCount.get(),
                "No exception should be thrown during concurrent reencolar at TaskExecutionManagerConcurrenciaTest.java:140");
    }

    @Test
    @DisplayName("Thread names follow pattern BurpIA-Task-{number}")
    void testThreadNamesSonDeterministic() throws Exception {
        ConfiguracionAPI localConfig = new ConfiguracionAPI();
        localConfig.establecerMaximoConcurrente(10);

        TaskExecutionManager manager1 = new TaskExecutionManager(
                localConfig, null, null, null,
                stdout, stderr, new LimitadorTasa(10), null
        );

        try {
            // Get the executorService via reflection to capture thread names from the ACTUAL pool
            java.lang.reflect.Field executorField = TaskExecutionManager.class.getDeclaredField("executorService");
            executorField.setAccessible(true);
            ExecutorService managerExecutor = (ExecutorService) executorField.get(manager1);

            int targetCount = 5;
            CountDownLatch nameLatch = new CountDownLatch(targetCount);
            List<String> capturedNames = new ArrayList<>();

            // Submit tasks to the ACTUAL TaskExecutionManager executor, not a test executor
            for (int i = 0; i < targetCount; i++) {
                managerExecutor.submit(() -> {
                    Thread currentThread = Thread.currentThread();
                    String name = currentThread.getName();
                    synchronized (capturedNames) {
                        capturedNames.add(name);
                    }
                    nameLatch.countDown();
                });
            }

            boolean completed = nameLatch.await(10, TimeUnit.SECONDS);

            assertTrue(completed, "Not all thread names captured at TaskExecutionManagerConcurrenciaTest.java:185");

            // All captured threads should come from TaskExecutionManager's pool with proper naming
            assertEquals(targetCount, capturedNames.size(),
                    "Should have captured " + targetCount + " thread names at TaskExecutionManagerConcurrenciaTest.java:187");

            for (String name : capturedNames) {
                // TaskExecutionManager names threads as "BurpIA-Task-{number}" via its
                // ThreadFactory using contadorHilos.incrementAndGet(). The counter is
                // static and shared across instances, so exact numbers vary between runs.
                // We check the prefix to validate the naming convention is applied.
                assertTrue(name.startsWith("BurpIA-Task-"),
                        "Thread name '" + name + "' should start with 'BurpIA-Task-' at TaskExecutionManagerConcurrenciaTest.java:189");
            }
        } finally {
            manager1.shutdown();
        }
    }

    @Test
    @DisplayName("shutdown can be called concurrently without issues")
    void testShutdownConcurrenteNoCausaProblemas() throws Exception {
        ConfiguracionAPI localConfig = new ConfiguracionAPI();
        localConfig.establecerMaximoConcurrente(5);

        TaskExecutionManager localManager = new TaskExecutionManager(
                localConfig, null, null, null,
                stdout, stderr, new LimitadorTasa(5), null
        );

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(10, TimeUnit.SECONDS);
                    localManager.shutdown();
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);

        executor.shutdown();
        assertTrue(completed, "Not all threads completed at TaskExecutionManagerConcurrenciaTest.java:218");
        assertEquals(0, exceptionCount.get(),
                "No exception should be thrown during concurrent shutdown at TaskExecutionManagerConcurrenciaTest.java:220");
    }

    @Test
    @DisplayName("cancelarEjecucionActiva can be called while tasks are running")
    void testCancelarEjecucionActivaConcurrente() throws Exception {
        manager = crearManager();

        SolicitudAnalisis solicitud = new SolicitudAnalisis(
                "http://example.com/cancel",
                "GET",
                "GET /cancel HTTP/1.1\nHost: example.com",
                "",
                "hash-cancel"
        );

        String tareaId = manager.programarAnalisis(solicitud, null, "test");

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(10, TimeUnit.SECONDS);
                    for (int j = 0; j < 5; j++) {
                        try {
                            manager.cancelarEjecucionActiva(tareaId);
                        } catch (Exception e) {
                            exceptionCount.incrementAndGet();
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
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);

        executor.shutdown();
        assertTrue(completed, "Not all threads completed at TaskExecutionManagerConcurrenciaTest.java:257");
        assertEquals(0, exceptionCount.get(),
                "No exception should be thrown during concurrent cancellation at TaskExecutionManagerConcurrenciaTest.java:259");
    }
}
