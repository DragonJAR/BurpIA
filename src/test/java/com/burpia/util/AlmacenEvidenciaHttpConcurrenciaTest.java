package com.burpia.util;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Concurrency tests for AlmacenEvidenciaHttp.
 * <p>
 * Verifies that the fix for CAT-009 (writing inside the lock) correctly prevents
 * race conditions between disk writes and cache updates.
 * </p>
 *
 * @see AlmacenEvidenciaHttp#guardar(HttpRequestResponse)
 */
@DisplayName("AlmacenEvidenciaHttp Concurrencia Tests")
class AlmacenEvidenciaHttpConcurrenciaTest {

    private Path directorioTemporal;
    private AlmacenEvidenciaHttp almacen;

    @BeforeEach
    void setUp() throws IOException {
        directorioTemporal = Files.createTempDirectory("burpia-evidencia-test");
        almacen = new AlmacenEvidenciaHttp(directorioTemporal);
    }

    @AfterEach
    void tearDown() {
        if (directorioTemporal != null && Files.exists(directorioTemporal)) {
            eliminarDirectorioRecursivamente(directorioTemporal);
        }
    }

    private void eliminarDirectorioRecursivamente(Path dir) {
        try (var stream = Files.walk(dir)) {
            stream.sorted(Collections.reverseOrder())
                  .forEach(p -> {
                      try {
                          Files.deleteIfExists(p);
                      } catch (IOException ignored) {
                      }
                  });
        } catch (IOException ignored) {
        }
    }

    /**
     * Creates a mock HttpRequestResponse with specified request and response bytes.
     */
    private HttpRequestResponse crearEvidenciaMock(byte[] requestBytes, byte[] responseBytes) {
        HttpRequestResponse mockEvidencia = mock(HttpRequestResponse.class);
        HttpRequest mockRequest = mock(HttpRequest.class);
        HttpResponse mockResponse = mock(HttpResponse.class);
        ByteArray mockRequestBytes = mock(ByteArray.class);
        ByteArray mockResponseBytes = mock(ByteArray.class);

        when(mockRequest.toByteArray()).thenReturn(mockRequestBytes);
        when(mockRequestBytes.getBytes()).thenReturn(requestBytes);

        when(mockEvidencia.request()).thenReturn(mockRequest);

        when(mockEvidencia.hasResponse()).thenReturn(responseBytes != null && responseBytes.length > 0);
        when(mockEvidencia.response()).thenReturn(mockResponse);
        when(mockResponse.toByteArray()).thenReturn(mockResponseBytes);
        when(mockResponseBytes.getBytes()).thenReturn(responseBytes);

        return mockEvidencia;
    }

    @Test
    @DisplayName("testGuardarConcurrenteNoGeneraInconsistencia — multiple threads saving different IDs should not throw ConcurrentModificationException")
    void testGuardarConcurrenteNoGeneraInconsistencia() throws Exception {
        int threadCount = 10;
        int iterationsPerThread = 20;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicReference<Throwable> firstError = new AtomicReference<>();
        AtomicInteger successCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadIndex = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterationsPerThread; i++) {
                        String uniqueId = "thread-" + threadIndex + "-iter-" + i;
                        byte[] req = ("GET /test/" + uniqueId + " HTTP/1.1\r\nHost: example.com\r\n\r\n").getBytes();
                        byte[] res = ("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\nOK").getBytes();
                        HttpRequestResponse evidencia = crearEvidenciaMock(req, res);

                        try {
                            String id = almacen.guardar(evidencia);
                            if (id != null) {
                                successCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            firstError.compareAndSet(null, e);
                            break;
                        }
                    }
                } catch (Throwable e) {
                    errorCount.incrementAndGet();
                    firstError.compareAndSet(null, e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS),
            "All threads should complete within timeout");

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS),
            "Executor should shutdown");

        assertEquals(0, errorCount.get(),
            "No thread should throw exception. First error: " + firstError.get());
        assertEquals(threadCount * iterationsPerThread, successCount.get(),
            "All writes should succeed");
    }

    @Test
    @DisplayName("testGuardarYObtenerConcurrenteEsConsistente — reader gets null or complete evidence, never partial state")
    void testGuardarYObtenerConcurrenteEsConsistente() throws Exception {
        byte[] req = ("GET /shared HTTP/1.1\r\nHost: example.com\r\n\r\n").getBytes();
        byte[] res = ("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\nOK").getBytes();
        HttpRequestResponse sharedEvidence = crearEvidenciaMock(req, res);

        AtomicReference<String> savedId = new AtomicReference<>();
        AtomicInteger readerNullCount = new AtomicInteger(0);
        AtomicInteger readerValidCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicReference<Throwable> firstError = new AtomicReference<>();

        int writerIterations = 5;
        int readerIterations = 50;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        // Writer thread
        Thread writerThread = new Thread(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < writerIterations; i++) {
                    String id = almacen.guardar(sharedEvidence);
                    savedId.set(id);
                    Thread.sleep(10);
                }
            } catch (Throwable e) {
                errorCount.incrementAndGet();
                firstError.compareAndSet(null, e);
            } finally {
                doneLatch.countDown();
            }
        });

        // Reader thread
        Thread readerThread = new Thread(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < readerIterations; i++) {
                    String id = savedId.get();
                    if (id != null) {
                        HttpRequestResponse result = almacen.obtener(id);
                        if (result == null) {
                            readerNullCount.incrementAndGet();
                        } else {
                            // Verify we can call methods on result without exception
                            result.hasResponse();
                            readerValidCount.incrementAndGet();
                        }
                    }
                    Thread.sleep(5);
                }
            } catch (Throwable e) {
                errorCount.incrementAndGet();
                firstError.compareAndSet(null, e);
            } finally {
                doneLatch.countDown();
            }
        });

        writerThread.start();
        readerThread.start();
        startLatch.countDown();

        assertTrue(doneLatch.await(30, TimeUnit.SECONDS),
            "Both threads should complete within timeout");
        writerThread.join(2000);
        readerThread.join(2000);

        assertEquals(0, errorCount.get(),
            "No exception should occur. First error: " + firstError.get());
        assertTrue(readerNullCount.get() + readerValidCount.get() > 0,
            "Reader should have attempted at least one read");
    }

    @Test
    @DisplayName("testGuardarConcurrenteConMismoIdEsSeguro — multiple threads saving should not crash, final state is consistent")
    void testGuardarConcurrenteConMismoIdEsSeguro() throws Exception {
        // Note: guardar() generates a new UUID each time, so this test verifies
        // that concurrent calls don't corrupt internal state even when happening
        // at the exact same time
        int threadCount = 8;
        int iterationsPerThread = 10;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicReference<Throwable> firstError = new AtomicReference<>();
        AtomicInteger writesCompleted = new AtomicInteger(0);

        byte[] req = ("GET /concurrent HTTP/1.1\r\nHost: example.com\r\n\r\n").getBytes();
        byte[] res = ("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\nOK").getBytes();
        HttpRequestResponse evidencia = crearEvidenciaMock(req, res);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterationsPerThread; i++) {
                        try {
                            String id = almacen.guardar(evidencia);
                            if (id != null) {
                                writesCompleted.incrementAndGet();
                            }
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            firstError.compareAndSet(null, e);
                            break;
                        }
                    }
                } catch (Throwable e) {
                    errorCount.incrementAndGet();
                    firstError.compareAndSet(null, e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS),
            "All threads should complete within timeout");

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS),
            "Executor should shutdown");

        assertEquals(0, errorCount.get(),
            "No thread should throw exception. First error: " + firstError.get());
        assertEquals(threadCount * iterationsPerThread, writesCompleted.get(),
            "All concurrent writes should complete successfully");
    }

    @Test
    @DisplayName("testLimpiarNoInterfiereConGuardarConcurrente — limpiar while saving should not cause ConcurrentModificationException")
    void testLimpiarNoInterfiereConGuardarConcurrente() throws Exception {
        int writerCount = 5;
        int iterationsPerWriter = 15;
        int cleanCycles = 20;

        ExecutorService executor = Executors.newFixedThreadPool(writerCount + 1);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(writerCount + 1);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicReference<Throwable> firstError = new AtomicReference<>();
        AtomicInteger successfulWrites = new AtomicInteger(0);

        byte[] req = ("GET /clean-test HTTP/1.1\r\nHost: example.com\r\n\r\n").getBytes();
        byte[] res = ("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\nOK").getBytes();

        // Writer threads
        for (int w = 0; w < writerCount; w++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < iterationsPerWriter; i++) {
                        HttpRequestResponse evidencia = crearEvidenciaMock(req, res);
                        try {
                            String id = almacen.guardar(evidencia);
                            if (id != null) {
                                successfulWrites.incrementAndGet();
                            }
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            firstError.compareAndSet(null, e);
                        }
                    }
                } catch (Throwable e) {
                    errorCount.incrementAndGet();
                    firstError.compareAndSet(null, e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Cleaner thread
        executor.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < cleanCycles; i++) {
                    try {
                        almacen.limpiarCacheMemoria();
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        firstError.compareAndSet(null, e);
                    }
                    Thread.sleep(5);
                }
            } catch (Throwable e) {
                errorCount.incrementAndGet();
                firstError.compareAndSet(null, e);
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS),
            "All threads should complete within timeout");

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS),
            "Executor should shutdown");

        assertEquals(0, errorCount.get(),
            "No exception should occur during concurrent write and clean. First error: " + firstError.get());
        assertEquals(writerCount * iterationsPerWriter, successfulWrites.get(),
            "All writes should complete despite concurrent cleaning");
    }
}
