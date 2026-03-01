package com.burpia.util;

import com.burpia.model.Hallazgo;
import com.burpia.ui.ModeloTablaHallazgos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Benchmark tests para identificar cuellos de botella en operaciones críticas.
 * Mide tiempo de ejecución y memoria para operaciones frecuentes.
 */
@DisplayName("Performance Benchmarks")
class PerformanceBenchmarkTest {

    private static final int WARMUP_ITERATIONS = 100;
    private static final int MEASUREMENT_ITERATIONS = 1000;
    private static final long NANOS_PER_MS = TimeUnit.MILLISECONDS.toNanos(1);

    private static class BenchmarkResult {
        final String operation;
        final long totalTimeNs;
        final long minTimeNs;
        final long maxTimeNs;
        final double avgTimeNs;

        BenchmarkResult(String operation, long totalTimeNs, long minTimeNs, long maxTimeNs, int iterations) {
            this.operation = operation;
            this.totalTimeNs = totalTimeNs;
            this.minTimeNs = minTimeNs;
            this.maxTimeNs = maxTimeNs;
            this.avgTimeNs = (double) totalTimeNs / iterations;
        }

        @Override
        public String toString() {
            return String.format("%s: avg=%.3f ms, min=%.3f ms, max=%.3f ms, total=%.3f ms",
                operation,
                avgTimeNs / NANOS_PER_MS,
                (double) minTimeNs / NANOS_PER_MS,
                (double) maxTimeNs / NANOS_PER_MS,
                (double) totalTimeNs / NANOS_PER_MS);
        }
    }

    private BenchmarkResult runBenchmark(String name, Runnable operation) {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            operation.run();
        }

        // Measurement
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        long totalTime = 0;

        for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
            long start = System.nanoTime();
            operation.run();
            long elapsed = System.nanoTime() - start;
            totalTime += elapsed;
            minTime = Math.min(minTime, elapsed);
            maxTime = Math.max(maxTime, elapsed);
        }

        return new BenchmarkResult(name, totalTime, minTime, maxTime, MEASUREMENT_ITERATIONS);
    }

    @Test
    @DisplayName("ReparadorJson - JSON valido sin reparacion")
    void benchmarkReparadorJsonValido() {
        String jsonValido = "{\"titulo\":\"SQL Injection\",\"severidad\":\"High\",\"confianza\":\"High\",\"descripcion\":\"Test\",\"evidencia\":\"test\"}";

        BenchmarkResult result = runBenchmark("ReparadorJson.jsonValido", () -> {
            String repaired = ReparadorJson.repararJson(jsonValido);
            assertNotNull(repaired);
        });

        System.out.println(result);
        
        // Umbral: debe ser menor a 1ms en promedio para JSON pequeño válido
        assertTrue(result.avgTimeNs < TimeUnit.MILLISECONDS.toNanos(1),
            "JSON valido debe procesarse en <1ms promedio. " + result);
    }

    @Test
    @DisplayName("ReparadorJson - JSON con markdown")
    void benchmarkReparadorJsonConMarkdown() {
        String jsonConMarkdown = "```json\n{\"titulo\":\"XSS\",\"severidad\":\"Medium\",\"confianza\":\"High\",\"descripcion\":\"Test description\",\"evidencia\":\"evidence\"}\n```";

        BenchmarkResult result = runBenchmark("ReparadorJson.conMarkdown", () -> {
            String repaired = ReparadorJson.repararJson(jsonConMarkdown);
            assertNotNull(repaired);
        });

        System.out.println(result);
    }

    @Test
    @DisplayName("ReparadorJson - JSON danado complejo")
    void benchmarkReparadorJsonDanado() {
        String jsonDanado = "Here is the analysis:\n```json\n{\"hallazgos\":[{\"titulo\":\"SQL Injection in login\",\"severidad\":\"Critical\",\"confianza\":\"High\",\"descripcion\":\"Found SQL injection in /login endpoint with payload: ' OR '1'='1\",\"evidencia\":\"Request: POST /login\\nResponse: Database error\"}]}\n```\nSome extra text after.";

        BenchmarkResult result = runBenchmark("ReparadorJson.danado", () -> {
            String repaired = ReparadorJson.repararJson(jsonDanado);
            assertNotNull(repaired);
        });

        System.out.println(result);
    }

    @Test
    @DisplayName("ParserRespuestasAI - Extraer contenido OpenAI")
    void benchmarkParserRespuestasOpenAI() {
        String respuestaOpenAI = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"{\\\"titulo\\\":\\\"XSS\\\",\\\"severidad\\\":\\\"High\\\"}\"}}]}";

        BenchmarkResult result = runBenchmark("ParserRespuestasAI.OpenAI", () -> {
            String contenido = ParserRespuestasAI.extraerContenido(respuestaOpenAI, "OpenAI");
            assertNotNull(contenido);
        });

        System.out.println(result);
    }

    @Test
    @DisplayName("ParserRespuestasAI - Extraer contenido Claude")
    void benchmarkParserRespuestasClaude() {
        String respuestaClaude = "{\"content\":[{\"type\":\"text\",\"text\":\"This is the analysis result with findings.\"}]}";

        BenchmarkResult result = runBenchmark("ParserRespuestasAI.Claude", () -> {
            String contenido = ParserRespuestasAI.extraerContenido(respuestaClaude, "Claude");
            assertNotNull(contenido);
        });

        System.out.println(result);
    }

    @Test
    @DisplayName("Normalizador - normalizarTexto")
    void benchmarkNormalizador() {
        String texto = "  Linea1\\nLinea2\\tcon\\\"comillas\\\" y \\\\barra  ";

        BenchmarkResult result = runBenchmark("Normalizador.normalizarTexto", () -> {
            String normalizado = Normalizador.normalizarTexto(texto);
            assertNotNull(normalizado);
        });

        System.out.println(result);
    }

    @Test
    @DisplayName("Normalizador - esVacio")
    void benchmarkNormalizadorEsVacio() {
        String texto = "   texto con espacios   ";

        BenchmarkResult result = runBenchmark("Normalizador.esVacio", () -> {
            boolean vacio = Normalizador.esVacio(texto);
            assertFalse(vacio);
        });

        System.out.println(result);
    }

    @Test
    @DisplayName("ModeloTablaHallazgos - agregarHallazgo")
    void benchmarkModeloTablaHallazgos() {
        ModeloTablaHallazgos modelo = new ModeloTablaHallazgos(10000);
        List<Hallazgo> hallazgos = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            hallazgos.add(new Hallazgo(
                "https://example.com/api/" + i,
                "Vulnerabilidad " + i,
                "Descripcion " + i,
                "High",
                "High",
                null
            ));
        }

        BenchmarkResult result = runBenchmark("ModeloTablaHallazgos.agregar", () -> {
            modelo.agregarHallazgos(hallazgos);
        });

        System.out.println(result);

        // Limpiar para siguiente test
        while (modelo.getRowCount() > 0) {
            modelo.eliminarHallazgo(0);
        }
    }

    @Test
    @DisplayName("Reporte de benchmarks")
    void generarReporteBenchmarks() {
        System.out.println("\n========== REPORTE DE BENCHMARKS ==========");
        System.out.println("Iteraciones de calentamiento: " + WARMUP_ITERATIONS);
        System.out.println("Iteraciones de medicion: " + MEASUREMENT_ITERATIONS);
        System.out.println("==========================================\n");
    }
}
