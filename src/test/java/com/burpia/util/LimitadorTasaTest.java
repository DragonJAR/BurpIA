package com.burpia.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para {@link LimitadorTasa}.
 * <p>
 * Verifica el comportamiento del limitador de tasa basado en semáforos,
 * incluyendo normalización de valores inválidos, control de concurrencia,
 * y comportamiento de bloqueo.
 * </p>
 */
@DisplayName("LimitadorTasa Tests")
class LimitadorTasaTest {

    @Nested
    @DisplayName("Constructor y normalización")
    class ConstructorYNormalizacion {

        @Test
        @DisplayName("Normaliza concurrencia cero a un permiso")
        void normalizaConcurrenciaCero() {
            LimitadorTasa limitador = new LimitadorTasa(0);
            assertEquals(1, limitador.permisosDisponibles());
        }

        @Test
        @DisplayName("Normaliza concurrencia negativa a un permiso")
        void normalizaConcurrenciaNegativa() {
            LimitadorTasa limitador = new LimitadorTasa(-5);
            assertEquals(1, limitador.permisosDisponibles());
        }

        @Test
        @DisplayName("Normaliza concurrencia Integer.MIN_VALUE a un permiso")
        void normalizaConcurrenciaMinValue() {
            LimitadorTasa limitador = new LimitadorTasa(Integer.MIN_VALUE);
            assertEquals(1, limitador.permisosDisponibles());
        }

        @Test
        @DisplayName("Mantiene concurrencia valida configurada")
        void mantieneConcurrenciaValida() {
            LimitadorTasa limitador = new LimitadorTasa(3);
            assertEquals(3, limitador.permisosDisponibles());
        }

        @Test
        @DisplayName("Acepta concurrencia de un permiso")
        void aceptaConcurrenciaUno() {
            LimitadorTasa limitador = new LimitadorTasa(1);
            assertEquals(1, limitador.permisosDisponibles());
        }

        @Test
        @DisplayName("Acepta concurrencia alta")
        void aceptaConcurrenciaAlta() {
            LimitadorTasa limitador = new LimitadorTasa(100);
            assertEquals(100, limitador.permisosDisponibles());
        }
    }

    @Nested
    @DisplayName("Adquirir y liberar permisos")
    class AdquirirYLiberarPermisos {

        @Test
        @DisplayName("Adquirir permiso reduce permisos disponibles")
        void adquirirReducePermisos() throws InterruptedException {
            LimitadorTasa limitador = new LimitadorTasa(3);
            assertEquals(3, limitador.permisosDisponibles());

            limitador.adquirir();
            assertEquals(2, limitador.permisosDisponibles());

            limitador.adquirir();
            assertEquals(1, limitador.permisosDisponibles());
        }

        @Test
        @DisplayName("Liberar permiso incrementa permisos disponibles")
        void liberarIncrementaPermisos() throws InterruptedException {
            LimitadorTasa limitador = new LimitadorTasa(2);
            limitador.adquirir();
            assertEquals(1, limitador.permisosDisponibles());

            limitador.liberar();
            assertEquals(2, limitador.permisosDisponibles());
        }

        @Test
        @DisplayName("Ciclo completo adquirir-liberar restaura permisos")
        void cicloCompletoRestauraPermisos() throws InterruptedException {
            LimitadorTasa limitador = new LimitadorTasa(5);
            int permisosIniciales = limitador.permisosDisponibles();

            limitador.adquirir();
            limitador.liberar();

            assertEquals(permisosIniciales, limitador.permisosDisponibles());
        }

        @Test
        @DisplayName("Puede adquirir todos los permisos disponibles")
        void puedeAdquirirTodosLosPermisos() throws InterruptedException {
            LimitadorTasa limitador = new LimitadorTasa(3);

            limitador.adquirir();
            limitador.adquirir();
            limitador.adquirir();

            assertEquals(0, limitador.permisosDisponibles());
        }

        @Test
        @DisplayName("Liberar sin adquirir previo aumenta permisos")
        void liberarSinAdquirirAumentaPermisos() {
            LimitadorTasa limitador = new LimitadorTasa(2);
            // Semaphore permite liberar más permisos de los que se adquirieron
            limitador.liberar();
            assertEquals(3, limitador.permisosDisponibles());
        }
    }

    @Nested
    @DisplayName("Comportamiento de bloqueo")
    class ComportamientoBloqueo {

        @Test
        @DisplayName("Adquirir bloquea cuando no hay permisos disponibles")
        void adquirirBloqueaSinPermisos() throws InterruptedException {
            LimitadorTasa limitador = new LimitadorTasa(1);
            limitador.adquirir(); // Agotar permisos

            CountDownLatch bloqueado = new CountDownLatch(1);
            CountDownLatch terminado = new CountDownLatch(1);
            AtomicInteger permisosAlFinal = new AtomicInteger(-1);

            Thread hiloBloqueado = new Thread(() -> {
                bloqueado.countDown();
                try {
                    limitador.adquirir();
                    permisosAlFinal.set(limitador.permisosDisponibles());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    terminado.countDown();
                }
            });

            hiloBloqueado.start();
            bloqueado.await(1, TimeUnit.SECONDS);

            // El hilo debería estar bloqueado, no ha terminado
            assertFalse(terminado.await(100, TimeUnit.MILLISECONDS));

            // Liberar permiso para desbloquear
            limitador.liberar();
            terminado.await(1, TimeUnit.SECONDS);

            // Ahora debería haber terminado
            assertEquals(0, permisosAlFinal.get());
        }

        @Test
        @DisplayName("Múltiples hilos pueden adquirir permisos concurrentes")
        void multiplesHilosAdquierenConcurrentes() throws InterruptedException {
            LimitadorTasa limitador = new LimitadorTasa(3);
            int numHilos = 10;
            CountDownLatch listo = new CountDownLatch(numHilos);
            AtomicInteger maxConcurrentes = new AtomicInteger(0);
            AtomicInteger concurrentesActuales = new AtomicInteger(0);

            Thread[] hilos = new Thread[numHilos];
            for (int i = 0; i < numHilos; i++) {
                hilos[i] = new Thread(() -> {
                    try {
                        limitador.adquirir();
                        int actuales = concurrentesActuales.incrementAndGet();
                        maxConcurrentes.updateAndGet(max -> Math.max(max, actuales));

                        Thread.sleep(50); // Simular trabajo

                        concurrentesActuales.decrementAndGet();
                        limitador.liberar();
                        listo.countDown();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            for (Thread hilo : hilos) {
                hilo.start();
            }

            listo.await(5, TimeUnit.SECONDS);

            // El máximo de concurrentes nunca debe exceder el límite
            assertTrue(maxConcurrentes.get() <= 3);
        }
    }

    @Nested
    @DisplayName("Patrón try-finally")
    class PatronTryFinally {

        @Test
        @DisplayName("Usar en try-finally garantiza liberación de permisos")
        void tryFinallyGarantizaLiberacion() throws InterruptedException {
            LimitadorTasa limitador = new LimitadorTasa(2);
            int permisosIniciales = limitador.permisosDisponibles();

            limitador.adquirir();
            try {
                // Simular operación que podría fallar
                throw new RuntimeException("Error simulado");
            } catch (RuntimeException e) {
                // Capturar excepción
            } finally {
                limitador.liberar();
            }

            assertEquals(permisosIniciales, limitador.permisosDisponibles());
        }

        @Test
        @DisplayName("Múltiples adquisiciones en try-finally anidados")
        void multiplesAdquisicionesTryFinally() throws InterruptedException {
            LimitadorTasa limitador = new LimitadorTasa(5);

            limitador.adquirir();
            try {
                limitador.adquirir();
                try {
                    assertEquals(3, limitador.permisosDisponibles());
                } finally {
                    limitador.liberar();
                }
            } finally {
                limitador.liberar();
            }

            assertEquals(5, limitador.permisosDisponibles());
        }
    }
}
