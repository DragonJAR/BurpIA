package com.burpia.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Estadisticas Tests")
class EstadisticasTest {

    private Estadisticas stats;

    @BeforeEach
    void setUp() {
        stats = new Estadisticas();
    }

    @Nested
    @DisplayName("Contadores iniciales")
    class ContadoresIniciales {
        @Test
        @DisplayName("Todos los contadores inician en cero")
        void todosLosContadoresInicianEnCero() {
            assertEquals(0, stats.obtenerTotalSolicitudes());
            assertEquals(0, stats.obtenerAnalizados());
            assertEquals(0, stats.obtenerHallazgosCreados());
            assertEquals(0, stats.obtenerErrores());
            assertEquals(0, stats.obtenerTotalOmitidos());
            assertEquals(0, stats.obtenerHallazgosCritical());
            assertEquals(0, stats.obtenerHallazgosHigh());
            assertEquals(0, stats.obtenerHallazgosMedium());
            assertEquals(0, stats.obtenerHallazgosLow());
            assertEquals(0, stats.obtenerHallazgosInfo());
        }
    }

    @Nested
    @DisplayName("Incrementos basicos")
    class IncrementosBasicos {
        @Test
        @DisplayName("Incrementa solicitudes")
        void incrementaSolicitudes() {
            stats.incrementarTotalSolicitudes();
            stats.incrementarTotalSolicitudes();
            assertEquals(2, stats.obtenerTotalSolicitudes());
        }

        @Test
        @DisplayName("Incrementa analizados")
        void incrementaAnalizados() {
            stats.incrementarAnalizados();
            assertEquals(1, stats.obtenerAnalizados());
        }

        @Test
        @DisplayName("Incrementa errores")
        void incrementaErrores() {
            stats.incrementarErrores();
            stats.incrementarErrores();
            stats.incrementarErrores();
            assertEquals(3, stats.obtenerErrores());
        }
    }

    @Nested
    @DisplayName("Omitidos")
    class Omitidos {
        @Test
        @DisplayName("Total omitidos suma duplicados y baja confianza")
        void totalOmitidosSumaTodos() {
            stats.incrementarOmitidosDuplicado();
            stats.incrementarOmitidosDuplicado();
            stats.incrementarOmitidosBajaConfianza();
            assertEquals(3, stats.obtenerTotalOmitidos());
        }
    }

    @Nested
    @DisplayName("Hallazgos por severidad")
    class HallazgosPorSeveridad {
        @Test
        @DisplayName("Incrementa Critical correctamente")
        void incrementaCritical() {
            stats.incrementarHallazgoSeveridad(Hallazgo.SEVERIDAD_CRITICAL);
            assertEquals(1, stats.obtenerHallazgosCritical());
            assertEquals(1, stats.obtenerHallazgosCreados());
        }

        @Test
        @DisplayName("Incrementa High correctamente")
        void incrementaHigh() {
            stats.incrementarHallazgoSeveridad(Hallazgo.SEVERIDAD_HIGH);
            assertEquals(1, stats.obtenerHallazgosHigh());
        }

        @Test
        @DisplayName("Incrementa Medium correctamente")
        void incrementaMedium() {
            stats.incrementarHallazgoSeveridad(Hallazgo.SEVERIDAD_MEDIUM);
            assertEquals(1, stats.obtenerHallazgosMedium());
        }

        @Test
        @DisplayName("Incrementa Low correctamente")
        void incrementaLow() {
            stats.incrementarHallazgoSeveridad(Hallazgo.SEVERIDAD_LOW);
            assertEquals(1, stats.obtenerHallazgosLow());
        }

        @Test
        @DisplayName("Incrementa Info correctamente")
        void incrementaInfo() {
            stats.incrementarHallazgoSeveridad(Hallazgo.SEVERIDAD_INFO);
            assertEquals(1, stats.obtenerHallazgosInfo());
        }

        @Test
        @DisplayName("Severidad desconocida solo incrementa creados")
        void severidadDesconocida() {
            stats.incrementarHallazgoSeveridad("Unknown");
            assertEquals(0, stats.obtenerHallazgosCritical());
            assertEquals(0, stats.obtenerHallazgosHigh());
            assertEquals(0, stats.obtenerHallazgosMedium());
            assertEquals(0, stats.obtenerHallazgosLow());
            assertEquals(0, stats.obtenerHallazgosInfo());
            assertEquals(1, stats.obtenerHallazgosCreados());
        }

        @Test
        @DisplayName("Multiples severidades se acumulan correctamente")
        void multiplesSeveridades() {
            stats.incrementarHallazgoSeveridad(Hallazgo.SEVERIDAD_CRITICAL);
            stats.incrementarHallazgoSeveridad(Hallazgo.SEVERIDAD_HIGH);
            stats.incrementarHallazgoSeveridad(Hallazgo.SEVERIDAD_HIGH);
            stats.incrementarHallazgoSeveridad(Hallazgo.SEVERIDAD_MEDIUM);
            stats.incrementarHallazgoSeveridad(Hallazgo.SEVERIDAD_LOW);
            stats.incrementarHallazgoSeveridad(Hallazgo.SEVERIDAD_INFO);
            stats.incrementarHallazgoSeveridad(Hallazgo.SEVERIDAD_INFO);

            assertEquals(1, stats.obtenerHallazgosCritical());
            assertEquals(2, stats.obtenerHallazgosHigh());
            assertEquals(1, stats.obtenerHallazgosMedium());
            assertEquals(1, stats.obtenerHallazgosLow());
            assertEquals(2, stats.obtenerHallazgosInfo());
            assertEquals(7, stats.obtenerHallazgosCreados());
        }
    }

    @Nested
    @DisplayName("Resumen")
    class Resumen {
        @Test
        @DisplayName("Genera resumen con formato correcto")
        void generaResumenConFormato() {
            stats.incrementarTotalSolicitudes();
            stats.incrementarAnalizados();
            stats.incrementarErrores();
            stats.incrementarHallazgoSeveridad(Hallazgo.SEVERIDAD_HIGH);
            stats.incrementarOmitidosDuplicado();

            String resumen = stats.generarResumen();
            assertTrue(resumen.contains("Solicitudes: 1"));
            assertTrue(resumen.contains("Analizados: 1"));
            assertTrue(resumen.contains("Omitidos: 1"));
            assertTrue(resumen.contains("Hallazgos: 1"));
            assertTrue(resumen.contains("Errores: 1"));
        }
    }

    @Nested
    @DisplayName("Concurrencia")
    class Concurrencia {
        @Test
        @DisplayName("Incrementos concurrentes son atomicos")
        void incrementosConcurrentesSonAtomicos() throws InterruptedException {
            int hilos = 10;
            int incrementosPorHilo = 1000;
            ExecutorService executor = Executors.newFixedThreadPool(hilos);
            CountDownLatch latch = new CountDownLatch(hilos);

            for (int i = 0; i < hilos; i++) {
                executor.submit(() -> {
                    for (int j = 0; j < incrementosPorHilo; j++) {
                        stats.incrementarTotalSolicitudes();
                        stats.incrementarAnalizados();
                        stats.incrementarErrores();
                    }
                    latch.countDown();
                });
            }

            latch.await();
            executor.shutdown();

            assertEquals(hilos * incrementosPorHilo, stats.obtenerTotalSolicitudes());
            assertEquals(hilos * incrementosPorHilo, stats.obtenerAnalizados());
            assertEquals(hilos * incrementosPorHilo, stats.obtenerErrores());
        }

        @Test
        @DisplayName("Hallazgos por severidad son atomicos bajo concurrencia")
        void hallazgosConcurrentesSonAtomicos() throws InterruptedException {
            int hilos = 8;
            int incrementosPorHilo = 500;
            ExecutorService executor = Executors.newFixedThreadPool(hilos);
            CountDownLatch latch = new CountDownLatch(hilos);

            for (int i = 0; i < hilos; i++) {
                executor.submit(() -> {
                    for (int j = 0; j < incrementosPorHilo; j++) {
                        stats.incrementarHallazgoSeveridad(Hallazgo.SEVERIDAD_HIGH);
                    }
                    latch.countDown();
                });
            }

            latch.await();
            executor.shutdown();

            assertEquals(hilos * incrementosPorHilo, stats.obtenerHallazgosHigh());
            assertEquals(hilos * incrementosPorHilo, stats.obtenerHallazgosCreados());
        }
    }
}
