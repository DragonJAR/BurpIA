package com.burpia.model;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import org.junit.jupiter.api.AfterEach;
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
import static org.mockito.Mockito.mock;

/**
 * Concurrency tests for Hallazgo class.
 * Validates thread-safety of double-checked locking in obtenerEvidenciaHttp().
 */
@DisplayName("Hallazgo Concurrency Tests")
class HallazgoConcurrenciaTest {

    private static final int THREAD_COUNT = 10;
    private static final int ITERATIONS_PER_THREAD = 100;

    @AfterEach
    void tearDown() {
        Hallazgo.establecerResolutorEvidencia(null);
    }

    @Test
    @DisplayName("Multiple threads calling obtenerEvidenciaHttp() do not throw ConcurrentModificationException")
    void testObtenerEvidenciaHttpThreadSafety() throws Exception {
        HttpRequestResponse mockEvidencia = mock(HttpRequestResponse.class);

        AtomicInteger resolverCalls = new AtomicInteger(0);

        Hallazgo.establecerResolutorEvidencia((evidenciaId) -> {
            resolverCalls.incrementAndGet();
            return mockEvidencia;
        });

        Hallazgo hallazgo = new Hallazgo(
                "10:00:00",
                "http://example.com",
                "Test Title",
                "Test Finding",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA,
                null,
                null,
                "evidencia-123"
        );

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger exceptionCount = new AtomicInteger(0);
        List<HttpRequestResponse> results = new ArrayList<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(10, TimeUnit.SECONDS);
                    for (int j = 0; j < ITERATIONS_PER_THREAD; j++) {
                        try {
                            HttpRequestResponse result = hallazgo.obtenerEvidenciaHttp();
                            synchronized (results) {
                                results.add(result);
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
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);

        executor.shutdown();
        assertTrue(completed, "Not all threads completed within timeout at HallazgoConcurrenciaTest.java:73");
        assertEquals(0, exceptionCount.get(),
                "No ConcurrentModificationException should be thrown at HallazgoConcurrenciaTest.java:75");
        assertFalse(results.isEmpty(), "Results list should not be empty at HallazgoConcurrenciaTest.java:76");
        assertEquals(THREAD_COUNT * ITERATIONS_PER_THREAD, results.size(),
                "All results should be recorded at HallazgoConcurrenciaTest.java:78");
    }

    @Test
    @DisplayName("obtenerEvidenciaHttp returns evidence from multiple threads after set, none get null")
    void testObtenerEvidenciaHttpNoDevuelveNullDespuesDeSet() throws Exception {
        HttpRequestResponse mockEvidencia = mock(HttpRequestResponse.class);

        Hallazgo hallazgo = new Hallazgo(
                "10:00:00",
                "http://test.com",
                "Test Title",
                "Test Finding",
                Hallazgo.SEVERIDAD_CRITICAL,
                Hallazgo.CONFIANZA_MEDIA,
                null,
                mockEvidencia,
                null
        );

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger nullCount = new AtomicInteger(0);
        AtomicInteger nonNullCount = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(10, TimeUnit.SECONDS);
                    for (int j = 0; j < ITERATIONS_PER_THREAD; j++) {
                        HttpRequestResponse result = hallazgo.obtenerEvidenciaHttp();
                        if (result == null) {
                            nullCount.incrementAndGet();
                        } else {
                            nonNullCount.incrementAndGet();
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
        assertTrue(completed, "Not all threads completed within timeout at HallazgoConcurrenciaTest.java:118");
        assertEquals(0, nullCount.get(),
                "No thread should receive null after evidencia was set at HallazgoConcurrenciaTest.java:120");
        assertEquals(THREAD_COUNT * ITERATIONS_PER_THREAD, nonNullCount.get(),
                "All threads should receive the evidencia at HallazgoConcurrenciaTest.java:122");
    }

    @Test
    @DisplayName("obtenerEvidenciaHttp returns same instance when called concurrently after resolution")
    void testObtenerEvidenciaHttpDevuelveMismaInstancia() throws Exception {
        HttpRequestResponse mockEvidencia = mock(HttpRequestResponse.class);

        AtomicInteger resolverCalls = new AtomicInteger(0);

        Hallazgo.establecerResolutorEvidencia((evidenciaId) -> {
            resolverCalls.incrementAndGet();
            return mockEvidencia;
        });

        Hallazgo hallazgo = new Hallazgo(
                "10:00:00",
                "http://same.com",
                "Test",
                "Finding",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA,
                null,
                null,
                "same-evidencia-id"
        );

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        List<HttpRequestResponse> allResults = new ArrayList<>();

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(10, TimeUnit.SECONDS);
                    for (int j = 0; j < ITERATIONS_PER_THREAD; j++) {
                        HttpRequestResponse result = hallazgo.obtenerEvidenciaHttp();
                        synchronized (allResults) {
                            allResults.add(result);
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
        assertTrue(completed, "Not all threads completed within timeout at HallazgoConcurrenciaTest.java:164");
        assertEquals(1, resolverCalls.get(),
                "Resolver should be called exactly once at HallazgoConcurrenciaTest.java:166");
        for (HttpRequestResponse result : allResults) {
            assertNotNull(result,
                    "No result should be null at HallazgoConcurrenciaTest.java:169");
            assertSame(mockEvidencia, result,
                    "All threads should get the same instance at HallazgoConcurrenciaTest.java:171");
        }
    }
}
