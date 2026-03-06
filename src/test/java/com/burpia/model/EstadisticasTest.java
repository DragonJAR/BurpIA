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
            assertEquals(0, stats.obtenerHallazgosCreados(), "assertEquals failed at EstadisticasTest.java:34");
            assertEquals(0, stats.obtenerErrores(), "assertEquals failed at EstadisticasTest.java:35");
            assertEquals(0, stats.obtenerTotalOmitidos(), "assertEquals failed at EstadisticasTest.java:36");
            assertEquals(0, stats.obtenerHallazgosCritical(), "assertEquals failed at EstadisticasTest.java:37");
            assertEquals(0, stats.obtenerHallazgosHigh(), "assertEquals failed at EstadisticasTest.java:38");
            assertEquals(0, stats.obtenerHallazgosMedium(), "assertEquals failed at EstadisticasTest.java:39");
            assertEquals(0, stats.obtenerHallazgosLow(), "assertEquals failed at EstadisticasTest.java:40");
            assertEquals(0, stats.obtenerHallazgosInfo(), "assertEquals failed at EstadisticasTest.java:41");
            assertEquals(0, stats.obtenerHallazgosDesconocidos(), "assertEquals failed at EstadisticasTest.java:42");
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
    @DisplayName("Hallazgos por severidad")
    class HallazgosPorSeveridad {
        @Test
        @DisplayName("Incrementa Critical correctamente")
        void incrementaCritical() {
            stats.incrementarHallazgoSeveridad(Hallazgo.SEVERIDAD_CRITICAL);
            assertEquals(1, stats.obtenerHallazgosCritical(), "assertEquals failed at EstadisticasTest.java:94");
            assertEquals(1, stats.obtenerHallazgosCreados(), "assertEquals failed at EstadisticasTest.java:95");
        }

        @Test
        @DisplayName("Incrementa High correctamente")
        void incrementaHigh() {
            stats.incrementarHallazgoSeveridad(Hallazgo.SEVERIDAD_HIGH);
            assertEquals(1, stats.obtenerHallazgosHigh(), "assertEquals failed at EstadisticasTest.java:102");
        }

        @Test
        @DisplayName("Incrementa Medium correctamente")
        void incrementaMedium() {
            stats.incrementarHallazgoSeveridad(Hallazgo.SEVERIDAD_MEDIUM);
            assertEquals(1, stats.obtenerHallazgosMedium(), "assertEquals failed at EstadisticasTest.java:109");
        }

        @Test
        @DisplayName("Incrementa Low correctamente")
        void incrementaLow() {
            stats.incrementarHallazgoSeveridad(Hallazgo.SEVERIDAD_LOW);
            assertEquals(1, stats.obtenerHallazgosLow(), "assertEquals failed at EstadisticasTest.java:116");
        }

        @Test
        @DisplayName("Incrementa Info correctamente")
        void incrementaInfo() {
            stats.incrementarHallazgoSeveridad(Hallazgo.SEVERIDAD_INFO);
            assertEquals(1, stats.obtenerHallazgosInfo(), "assertEquals failed at EstadisticasTest.java:123");
        }

        @Test
        @DisplayName("Severidad desconocida incrementa creados y desconocidos")
        void severidadDesconocida() {
            stats.incrementarHallazgoSeveridad("Unknown");
            assertEquals(0, stats.obtenerHallazgosCritical(), "assertEquals failed at EstadisticasTest.java:130");
            assertEquals(0, stats.obtenerHallazgosHigh(), "assertEquals failed at EstadisticasTest.java:131");
            assertEquals(0, stats.obtenerHallazgosMedium(), "assertEquals failed at EstadisticasTest.java:132");
            assertEquals(0, stats.obtenerHallazgosLow(), "assertEquals failed at EstadisticasTest.java:133");
            assertEquals(0, stats.obtenerHallazgosInfo(), "assertEquals failed at EstadisticasTest.java:134");
            assertEquals(1, stats.obtenerHallazgosCreados(), "assertEquals failed at EstadisticasTest.java:135");
            assertEquals(1, stats.obtenerHallazgosDesconocidos(), "assertEquals failed at EstadisticasTest.java:136");
        }

        @Test
        @DisplayName("Severidad null incrementa creados y desconocidos")
        void severidadNull() {
            stats.incrementarHallazgoSeveridad(null);
            assertEquals(1, stats.obtenerHallazgosCreados(), "assertEquals failed at EstadisticasTest.java:143");
            assertEquals(1, stats.obtenerHallazgosDesconocidos(), "assertEquals failed at EstadisticasTest.java:144");
        }

        @Test
        @DisplayName("Severidad vacia incrementa creados y desconocidos")
        void severidadVacia() {
            stats.incrementarHallazgoSeveridad("");
            assertEquals(1, stats.obtenerHallazgosCreados(), "assertEquals failed at EstadisticasTest.java:151");
            assertEquals(1, stats.obtenerHallazgosDesconocidos(), "assertEquals failed at EstadisticasTest.java:152");
        }

        @Test
        @DisplayName("Severidad con solo espacios incrementa creados y desconocidos")
        void severidadSoloEspacios() {
            stats.incrementarHallazgoSeveridad("   ");
            assertEquals(1, stats.obtenerHallazgosCreados(), "assertEquals failed at EstadisticasTest.java:159");
            assertEquals(1, stats.obtenerHallazgosDesconocidos(), "assertEquals failed at EstadisticasTest.java:160");
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

            assertEquals(1, stats.obtenerHallazgosCritical(), "assertEquals failed at EstadisticasTest.java:174");
            assertEquals(2, stats.obtenerHallazgosHigh(), "assertEquals failed at EstadisticasTest.java:175");
            assertEquals(1, stats.obtenerHallazgosMedium(), "assertEquals failed at EstadisticasTest.java:176");
            assertEquals(1, stats.obtenerHallazgosLow(), "assertEquals failed at EstadisticasTest.java:177");
            assertEquals(2, stats.obtenerHallazgosInfo(), "assertEquals failed at EstadisticasTest.java:178");
            assertEquals(7, stats.obtenerHallazgosCreados(), "assertEquals failed at EstadisticasTest.java:179");
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
            assertTrue(resumen.contains("Solicitudes: 1"), "assertTrue failed at EstadisticasTest.java:196");
            assertTrue(resumen.contains("Analizados: 1"), "assertTrue failed at EstadisticasTest.java:197");
            assertTrue(resumen.contains("Omitidos: 1"), "assertTrue failed at EstadisticasTest.java:198");
            assertTrue(resumen.contains("Hallazgos: 1"), "assertTrue failed at EstadisticasTest.java:199");
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
            stats.incrementarHallazgoSeveridad(Hallazgo.SEVERIDAD_CRITICAL);
            stats.incrementarHallazgoSeveridad(Hallazgo.SEVERIDAD_HIGH);
            stats.incrementarHallazgoSeveridad("Unknown");
            stats.incrementarOmitidosDuplicado();
            stats.incrementarOmitidosBajaConfianza();

            stats.reiniciar();

            assertEquals(0, stats.obtenerTotalSolicitudes(), "assertEquals failed at EstadisticasTest.java:232");
            assertEquals(0, stats.obtenerAnalizados(), "assertEquals failed at EstadisticasTest.java:233");
            assertEquals(0, stats.obtenerHallazgosCreados(), "assertEquals failed at EstadisticasTest.java:234");
            assertEquals(0, stats.obtenerErrores(), "assertEquals failed at EstadisticasTest.java:235");
            assertEquals(0, stats.obtenerTotalOmitidos(), "assertEquals failed at EstadisticasTest.java:236");
            assertEquals(0, stats.obtenerHallazgosCritical(), "assertEquals failed at EstadisticasTest.java:237");
            assertEquals(0, stats.obtenerHallazgosHigh(), "assertEquals failed at EstadisticasTest.java:238");
            assertEquals(0, stats.obtenerHallazgosMedium(), "assertEquals failed at EstadisticasTest.java:239");
            assertEquals(0, stats.obtenerHallazgosLow(), "assertEquals failed at EstadisticasTest.java:240");
            assertEquals(0, stats.obtenerHallazgosInfo(), "assertEquals failed at EstadisticasTest.java:241");
            assertEquals(0, stats.obtenerHallazgosDesconocidos(), "assertEquals failed at EstadisticasTest.java:242");
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
            
            versionActual = stats.obtenerVersion();
            stats.incrementarHallazgoSeveridad(Hallazgo.SEVERIDAD_HIGH);
            assertTrue(stats.obtenerVersion() > versionActual, "Version debe incrementar con incrementarHallazgoSeveridad");
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

        @Test
        @DisplayName("Hallazgos por severidad son atomicos bajo concurrencia")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
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
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor debe terminar en tiempo");

            assertEquals(hilos * incrementosPorHilo, stats.obtenerHallazgosHigh(), "assertEquals failed at EstadisticasTest.java:341");
            assertEquals(hilos * incrementosPorHilo, stats.obtenerHallazgosCreados(), "assertEquals failed at EstadisticasTest.java:342");
        }

        @Test
        @DisplayName("Reiniciar durante incrementos concurrentes no causa inconsistencias")
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void reiniciarDuranteIncrementosConcurrentes() throws InterruptedException {
            int hilosIncrementadores = 6;
            int incrementosPorHilo = 500;
            ExecutorService executor = Executors.newFixedThreadPool(hilosIncrementadores + 1);
            CountDownLatch latchInicio = new CountDownLatch(1);
            CountDownLatch latchFin = new CountDownLatch(hilosIncrementadores + 1);

            // Hilos que incrementan
            for (int i = 0; i < hilosIncrementadores; i++) {
                executor.submit(() -> {
                    try {
                        latchInicio.await();
                        for (int j = 0; j < incrementosPorHilo; j++) {
                            stats.incrementarTotalSolicitudes();
                            stats.incrementarHallazgoSeveridad(Hallazgo.SEVERIDAD_MEDIUM);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latchFin.countDown();
                    }
                });
            }

            // Hilo que reinicia periodicamente
            executor.submit(() -> {
                try {
                    latchInicio.await();
                    for (int i = 0; i < 10; i++) {
                        Thread.sleep(5);
                        stats.reiniciar();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latchFin.countDown();
                }
            });

            latchInicio.countDown(); // Iniciar todos los hilos
            latchFin.await();
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor debe terminar en tiempo");

            // Verificar que los contadores son consistentes (hallazgosCreados >= suma de severidades especificas)
            int sumaSeveridades = stats.obtenerHallazgosCritical() + stats.obtenerHallazgosHigh() +
                                  stats.obtenerHallazgosMedium() + stats.obtenerHallazgosLow() +
                                  stats.obtenerHallazgosInfo() + stats.obtenerHallazgosDesconocidos();
            assertEquals(sumaSeveridades, stats.obtenerHallazgosCreados(), 
                "La suma de hallazgos por severidad debe igualar el total de hallazgos creados");
            assertTrue(stats.obtenerVersion() >= 0, "Version debe ser no negativa");
        }
    }
}
