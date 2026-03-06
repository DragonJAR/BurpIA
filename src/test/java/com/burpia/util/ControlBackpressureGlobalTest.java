package com.burpia.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para {@link ControlBackpressureGlobal}.
 * <p>
 * Verifica el comportamiento del control de backpressure basado en cooldown,
 * incluyendo activación, expiración, extensión automática, y thread-safety.
 * </p>
 */
@DisplayName("ControlBackpressureGlobal Tests")
class ControlBackpressureGlobalTest {

    @Nested
    @DisplayName("Activación de cooldown")
    class ActivacionCooldown {

        @Test
        @DisplayName("Activar cooldown marca estado activo")
        void activarCooldownMarcaEstadoActivo() {
            ControlBackpressureGlobal control = new ControlBackpressureGlobal();
            control.activarCooldown(200L);
            
            assertTrue(control.estaEnCooldown(), "assertTrue failed at ControlBackpressureGlobalTest.java:35");
            assertTrue(control.milisegundosRestantes() > 0L, "assertTrue failed at ControlBackpressureGlobalTest.java:36");
            assertTrue(control.milisegundosRestantes() <= 200L, "assertTrue failed at ControlBackpressureGlobalTest.java:37");
        }

        @Test
        @DisplayName("Activar cooldown con valor negativo es ignorado")
        void activarCooldownNegativoIgnorado() {
            ControlBackpressureGlobal control = new ControlBackpressureGlobal();
            control.activarCooldown(-100L);
            
            assertFalse(control.estaEnCooldown(), "assertFalse failed at ControlBackpressureGlobalTest.java:46");
            assertEquals(0L, control.milisegundosRestantes(), "assertEquals failed at ControlBackpressureGlobalTest.java:47");
        }

        @Test
        @DisplayName("Activar cooldown con cero es ignorado")
        void activarCooldownCeroIgnorado() {
            ControlBackpressureGlobal control = new ControlBackpressureGlobal();
            control.activarCooldown(0L);
            
            assertFalse(control.estaEnCooldown(), "assertFalse failed at ControlBackpressureGlobalTest.java:56");
            assertEquals(0L, control.milisegundosRestantes(), "assertEquals failed at ControlBackpressureGlobalTest.java:57");
        }

        @Test
        @DisplayName("Cooldown largo no causa overflow")
        void cooldownLargoNoCausaOverflow() {
            ControlBackpressureGlobal control = new ControlBackpressureGlobal();
            control.activarCooldown(Long.MAX_VALUE - System.currentTimeMillis() - 1);
            
            assertTrue(control.estaEnCooldown(), "assertTrue failed at ControlBackpressureGlobalTest.java:66");
            assertTrue(control.milisegundosRestantes() > 0L, "assertTrue failed at ControlBackpressureGlobalTest.java:67");
        }
    }

    @Nested
    @DisplayName("Extensión de cooldown")
    class ExtensionCooldown {

        @Test
        @DisplayName("Cooldown mas largo extiende el existente")
        void cooldownMasLargoExtiendeExistente() {
            ControlBackpressureGlobal control = new ControlBackpressureGlobal();
            control.activarCooldown(100L);
            long restantesInicial = control.milisegundosRestantes();
            
            control.activarCooldown(500L);
            long restantesFinal = control.milisegundosRestantes();
            
            assertTrue(restantesFinal > restantesInicial, "assertTrue failed at ControlBackpressureGlobalTest.java:85");
            assertTrue(control.milisegundosRestantes() > 100L, "assertTrue failed at ControlBackpressureGlobalTest.java:86");
        }

        @Test
        @DisplayName("Cooldown mas corto no reduce el existente")
        void cooldownMasCortoNoReduceExistente() {
            ControlBackpressureGlobal control = new ControlBackpressureGlobal();
            control.activarCooldown(500L);
            long restantesInicial = control.milisegundosRestantes();
            
            control.activarCooldown(100L);
            long restantesFinal = control.milisegundosRestantes();
            
            // Debe mantener el cooldown más largo (dentro del margen de tiempo transcurrido)
            assertTrue(restantesFinal >= restantesInicial - 10L, "assertTrue failed at ControlBackpressureGlobalTest.java:100");
        }
    }

    @Nested
    @DisplayName("Limpieza de cooldown")
    class LimpiezaCooldown {

        @Test
        @DisplayName("Limpiar cooldown resetea estado")
        void limpiarCooldownReseteaEstado() {
            ControlBackpressureGlobal control = new ControlBackpressureGlobal();
            control.activarCooldown(5000L);
            assertTrue(control.estaEnCooldown(), "assertTrue failed at ControlBackpressureGlobalTest.java:113");
            
            control.limpiar();
            
            assertFalse(control.estaEnCooldown(), "assertFalse failed at ControlBackpressureGlobalTest.java:117");
            assertEquals(0L, control.milisegundosRestantes(), "assertEquals failed at ControlBackpressureGlobalTest.java:118");
        }

        @Test
        @DisplayName("Limpiar cooldown sin cooldown activo es seguro")
        void limpiarSinCooldownActivoEsSeguro() {
            ControlBackpressureGlobal control = new ControlBackpressureGlobal();
            
            control.limpiar();
            
            assertFalse(control.estaEnCooldown(), "assertFalse failed at ControlBackpressureGlobalTest.java:128");
            assertEquals(0L, control.milisegundosRestantes(), "assertEquals failed at ControlBackpressureGlobalTest.java:129");
        }
    }

    @Nested
    @DisplayName("Thread-safety")
    class ThreadSafety {

        @Test
        @DisplayName("Activacion concurrente de cooldowns es thread-safe")
        void activacionConcurrenteEsThreadSafe() throws InterruptedException {
            ControlBackpressureGlobal control = new ControlBackpressureGlobal();
            int numThreads = 10;
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CountDownLatch latch = new CountDownLatch(numThreads);
            AtomicInteger errores = new AtomicInteger(0);

            for (int i = 0; i < numThreads; i++) {
                final long duracion = 100L + (i * 10L);
                executor.submit(() -> {
                    try {
                        control.activarCooldown(duracion);
                    } catch (Exception e) {
                        errores.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(5, TimeUnit.SECONDS);
            executor.shutdown();

            assertEquals(0, errores.get(), "assertEquals failed at ControlBackpressureGlobalTest.java:162");
            assertTrue(control.estaEnCooldown(), "assertTrue failed at ControlBackpressureGlobalTest.java:163");
        }

        @Test
        @DisplayName("Lectura y escritura concurrente es thread-safe")
        void lecturaEscrituraConcurrenteEsThreadSafe() throws InterruptedException {
            ControlBackpressureGlobal control = new ControlBackpressureGlobal();
            int numThreads = 20;
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CountDownLatch latch = new CountDownLatch(numThreads);
            AtomicInteger errores = new AtomicInteger(0);

            // Mitad de threads activando cooldown, mitad leyendo estado
            for (int i = 0; i < numThreads; i++) {
                final boolean esEscritor = (i % 2 == 0);
                executor.submit(() -> {
                    try {
                        if (esEscritor) {
                            control.activarCooldown(100L);
                        } else {
                            control.estaEnCooldown();
                            control.milisegundosRestantes();
                        }
                    } catch (Exception e) {
                        errores.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(5, TimeUnit.SECONDS);
            executor.shutdown();

            assertEquals(0, errores.get(), "assertEquals failed at ControlBackpressureGlobalTest.java:197");
        }
    }

    @Nested
    @DisplayName("Consultas de estado")
    class ConsultasEstado {

        @Test
        @DisplayName("Obtener expiracion de cooldown devuelve timestamp valido")
        void obtenerExpiracionCooldownDevuelveTimestampValido() {
            ControlBackpressureGlobal control = new ControlBackpressureGlobal();
            long antes = System.currentTimeMillis();
            control.activarCooldown(1000L);
            long despues = System.currentTimeMillis();
            
            long expiracion = control.obtenerExpiracionCooldown();
            
            assertTrue(expiracion >= antes + 1000L, "assertTrue failed at ControlBackpressureGlobalTest.java:215");
            assertTrue(expiracion <= despues + 1000L, "assertTrue failed at ControlBackpressureGlobalTest.java:216");
        }

        @Test
        @DisplayName("Obtener expiracion sin cooldown devuelve cero")
        void obtenerExpiracionSinCooldownDevuelveCero() {
            ControlBackpressureGlobal control = new ControlBackpressureGlobal();
            
            assertEquals(0L, control.obtenerExpiracionCooldown(), "assertEquals failed at ControlBackpressureGlobalTest.java:224");
        }

        @Test
        @DisplayName("ToString sin cooldown es descriptivo")
        void toStringSinCooldownEsDescriptivo() {
            ControlBackpressureGlobal control = new ControlBackpressureGlobal();
            
            String resultado = control.toString();
            
            assertTrue(resultado.contains("sin cooldown activo"), "assertTrue failed at ControlBackpressureGlobalTest.java:234");
        }

        @Test
        @DisplayName("ToString con cooldown muestra milisegundos restantes")
        void toStringConCooldownMuestraMilisegundos() {
            ControlBackpressureGlobal control = new ControlBackpressureGlobal();
            control.activarCooldown(1000L);
            
            String resultado = control.toString();
            
            assertTrue(resultado.contains("cooldown:"), "assertTrue failed at ControlBackpressureGlobalTest.java:245");
            assertTrue(resultado.contains("ms restantes"), "assertTrue failed at ControlBackpressureGlobalTest.java:246");
        }
    }
}
