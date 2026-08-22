package com.burpia.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
            assertEquals(0, stats.obtenerTotalSolicitudes(), "assertEquals failed at EstadisticasTest.java:32");
            assertEquals(0, stats.obtenerAnalizados(), "assertEquals failed at EstadisticasTest.java:33");
            assertEquals(0, stats.obtenerErrores(), "assertEquals failed at EstadisticasTest.java:35");
            assertEquals(0, stats.obtenerTotalOmitidos(), "assertEquals failed at EstadisticasTest.java:36");
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
            assertEquals(2, stats.obtenerTotalSolicitudes(), "assertEquals failed at EstadisticasTest.java:54");
        }

        @Test
        @DisplayName("Incrementa analizados")
        void incrementaAnalizados() {
            stats.incrementarAnalizados();
            assertEquals(1, stats.obtenerAnalizados(), "assertEquals failed at EstadisticasTest.java:61");
        }

        @Test
        @DisplayName("Incrementa errores")
        void incrementaErrores() {
            stats.incrementarErrores();
            stats.incrementarErrores();
            stats.incrementarErrores();
            assertEquals(3, stats.obtenerErrores(), "assertEquals failed at EstadisticasTest.java:70");
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
            assertEquals(3, stats.obtenerTotalOmitidos(), "assertEquals failed at EstadisticasTest.java:83");
        }
    }

    @Nested
    @DisplayName("Hallazgos creados")
    class HallazgosCreados {
        @Test
        @DisplayName("Incrementa hallazgos creados y se refleja en el resumen")
        void incrementaHallazgosCreados() {
            stats.incrementarHallazgosCreados();
            stats.incrementarHallazgosCreados();
            assertEquals(2, stats.obtenerHallazgosCreados(), "El contador de hallazgos debe incrementar");
            assertTrue(stats.generarResumen().contains("Hallazgos: 2"),
                "El resumen debe reflejar los hallazgos creados");
        }

        @Test
        @DisplayName("Reiniciar pone hallazgos creados a cero")
        void reiniciarPoneHallazgosACero() {
            stats.incrementarHallazgosCreados();
            stats.reiniciar();
            assertEquals(0, stats.obtenerHallazgosCreados(), "reiniciar debe limpiar hallazgosCreados");
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
            stats.incrementarOmitidosDuplicado();

            String resumen = stats.generarResumen();
            assertTrue(resumen.contains("Solicitudes: 1"), "assertTrue failed at EstadisticasTest.java:196");
            assertTrue(resumen.contains("Analizados: 1"), "assertTrue failed at EstadisticasTest.java:197");
            assertTrue(resumen.contains("Omitidos: 1"), "assertTrue failed at EstadisticasTest.java:198");
            assertTrue(resumen.contains("Errores: 1"), "assertTrue failed at EstadisticasTest.java:200");
        }

        @Test
        @DisplayName("Genera resumen con todos los valores en cero")
        void generaResumenConCeros() {
            String resumen = stats.generarResumen();
            assertTrue(resumen.contains("Solicitudes: 0"), "assertTrue failed at EstadisticasTest.java:207");
            assertTrue(resumen.contains("Analizados: 0"), "assertTrue failed at EstadisticasTest.java:208");
            assertTrue(resumen.contains("Omitidos: 0"), "assertTrue failed at EstadisticasTest.java:209");
            assertTrue(resumen.contains("Hallazgos: 0"), "assertTrue failed at EstadisticasTest.java:210");
            assertTrue(resumen.contains("Errores: 0"), "assertTrue failed at EstadisticasTest.java:211");
        }
    }

    @Nested
    @DisplayName("Reinicio")
    class Reinicio {
        @Test
        @DisplayName("Reiniciar pone todos los contadores a cero")
        void reiniciarPoneContadoresACero() {
            stats.incrementarTotalSolicitudes();
            stats.incrementarAnalizados();
            stats.incrementarErrores();
            stats.incrementarOmitidosDuplicado();
            stats.incrementarOmitidosBajaConfianza();

            stats.reiniciar();

            assertEquals(0, stats.obtenerTotalSolicitudes(), "assertEquals failed at EstadisticasTest.java:232");
            assertEquals(0, stats.obtenerAnalizados(), "assertEquals failed at EstadisticasTest.java:233");
            assertEquals(0, stats.obtenerErrores(), "assertEquals failed at EstadisticasTest.java:235");
            assertEquals(0, stats.obtenerTotalOmitidos(), "assertEquals failed at EstadisticasTest.java:236");
        }

        @Test
        @DisplayName("Reiniciar incrementa version")
        void reiniciarIncrementaVersion() {
            int versionInicial = stats.obtenerVersion();
            stats.reiniciar();
            assertTrue(stats.obtenerVersion() > versionInicial, "assertTrue failed at EstadisticasTest.java:250");
        }
    }

    @Nested
    @DisplayName("Versionado")
    class Versionado {
        @Test
        @DisplayName("Version incrementa con cada operacion de incremento")
        void versionIncrementaConOperaciones() {
            int versionInicial = stats.obtenerVersion();

            stats.incrementarTotalSolicitudes();
            assertTrue(stats.obtenerVersion() > versionInicial, "Version debe incrementar con incrementarTotalSolicitudes");

            int versionActual = stats.obtenerVersion();
            stats.incrementarAnalizados();
            assertTrue(stats.obtenerVersion() > versionActual, "Version debe incrementar con incrementarAnalizados");

            versionActual = stats.obtenerVersion();
            stats.incrementarErrores();
            assertTrue(stats.obtenerVersion() > versionActual, "Version debe incrementar con incrementarErrores");

            versionActual = stats.obtenerVersion();
            stats.incrementarOmitidosDuplicado();
            assertTrue(stats.obtenerVersion() > versionActual, "Version debe incrementar con incrementarOmitidosDuplicado");

            versionActual = stats.obtenerVersion();
            stats.incrementarOmitidosBajaConfianza();
            assertTrue(stats.obtenerVersion() > versionActual, "Version debe incrementar con incrementarOmitidosBajaConfianza");
        }
    }

    @Nested
    @DisplayName("Concurrencia")
    class Concurrencia {
        @Test
        @DisplayName("Incrementos concurrentes son atomicos")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
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
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor debe terminar en tiempo");

            assertEquals(hilos * incrementosPorHilo, stats.obtenerTotalSolicitudes(), "assertEquals failed at EstadisticasTest.java:314");
            assertEquals(hilos * incrementosPorHilo, stats.obtenerAnalizados(), "assertEquals failed at EstadisticasTest.java:315");
            assertEquals(hilos * incrementosPorHilo, stats.obtenerErrores(), "assertEquals failed at EstadisticasTest.java:316");
        }
    }
}
