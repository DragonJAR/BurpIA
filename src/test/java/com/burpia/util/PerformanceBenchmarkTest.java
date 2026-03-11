package com.burpia.util;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.GestorConfiguracion;
import com.burpia.analyzer.ParseadorRespuestasAI;
import com.burpia.flow.FlowAnalysisConstraints;
import com.burpia.flow.FlowAnalysisRequestBuilder;
import com.burpia.model.Hallazgo;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.model.Tarea;
import com.burpia.execution.TaskExecutionManager;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.burpia.ui.ModeloTablaHallazgos;
import com.burpia.ui.ModeloTablaTareas;
import com.burpia.util.RutasBurpIA;
import com.burpia.util.GestorTareas;
import com.burpia.util.PoliticaMemoria;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import javax.swing.SwingUtilities;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Benchmark tests para identificar cuellos de botella en operaciones críticas.
 * Mide tiempo de ejecución y memoria para operaciones frecuentes.
 *
 * @Tag benchmark permite excluir estos tests de builds normales.
 * Ejecutar con: ./gradlew test --tests "*Benchmark*" -Djunit.platform.conditions.include.tags=benchmark
 * O con: ./gradlew benchmarkTest
 */
@Tag("benchmark")
@DisplayName("Performance Benchmarks")
class PerformanceBenchmarkTest {

    private static final int WARMUP_ITERATIONS = 100;
    private static final int MEASUREMENT_ITERATIONS = 1000;
    private static final long NANOS_PER_MS = TimeUnit.MILLISECONDS.toNanos(1);
    private static final int CAPACIDAD_MODELO_HALLAZGOS = 10000;
    private static final int CAPACIDAD_MODELO_TAREAS = 1000;
    private static final int CANTIDAD_ELEMENTOS_BATCH = 100;
    private static final long UMBRAL_MS_JSON_VALIDO = 1;
    private static final long UMBRAL_MS_GUARDAR_CONFIG = 100;
    private static final long UMBRAL_MS_CARGAR_CONFIG = 50;
    private static final long UMBRAL_MS_BATCH_TAREAS = 5;
    private static final long UMBRAL_MS_PROMPT_FLUJO = 10;
    private static final long UMBRAL_MS_SOLICITUD_FLUJO = 10;
    private static final long UMBRAL_MS_PROMPT_TRUNCADO = 5;
    private static final long UMBRAL_MS_PARSEO_AI = 5;
    private static final long UMBRAL_MS_PROGRAMAR_TAREA = 10;
    private static final long UMBRAL_MS_CANCELAR_TODAS = 10;

    private String userHomeOriginal;
    private Path tempDirActual;
    private ModeloTablaHallazgos modeloHallazgos;
    private ModeloTablaTareas modeloTareas;

    @BeforeAll
    static void mostrarConfiguracionBenchmark() {
        System.out.println("\n========== REPORTE DE BENCHMARKS ==========");
        System.out.println("Iteraciones de calentamiento: " + WARMUP_ITERATIONS);
        System.out.println("Iteraciones de medicion: " + MEASUREMENT_ITERATIONS);
        System.out.println("==========================================\n");
    }

    @BeforeEach
    void setUp() {
        userHomeOriginal = System.getProperty("user.home");
        modeloHallazgos = null;
        modeloTareas = null;
        tempDirActual = null;
    }

    @AfterEach
    void tearDown() throws Exception {
        if (userHomeOriginal != null) {
            System.setProperty("user.home", userHomeOriginal);
        }
        // CRÍTICO: Limpiar caché de RutasBurpIA para evitar que apunte al temp dir
        RutasBurpIA.limpiarCacheParaTests();
        RutasBurpIA.establecerSuffixConfig(null);
        limpiarModelosSwing();
        limpiarDirectorioTemporal();
    }

    private void limpiarModelosSwing() throws Exception {
        if (modeloHallazgos != null || modeloTareas != null) {
            SwingUtilities.invokeAndWait(() -> {
                if (modeloHallazgos != null) {
                    modeloHallazgos.limpiar();
                }
                if (modeloTareas != null) {
                    modeloTareas.limpiar();
                }
            });
        }
    }

    private void limpiarDirectorioTemporal() {
        if (tempDirActual != null && Files.exists(tempDirActual)) {
            eliminarDirectorioRecursivo(tempDirActual);
        }
    }

    private void eliminarDirectorioRecursivo(Path directorio) {
        try {
            Files.walk(directorio)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        System.err.println("No se pudo eliminar: " + path);
                    }
                });
        } catch (IOException e) {
            System.err.println("Error limpiando directorio temporal: " + e.getMessage());
        }
    }

    private static class BenchmarkResult {
        final String operation;
        final long totalTimeNs;
        final long minTimeNs;
        final long maxTimeNs;
        final double avgTimeNs;

        BenchmarkResult(String operation, long totalTimeNs, long minTimeNs, long maxTimeNs, int iterations) {
            if (Normalizador.esVacio(operation)) {
                throw new IllegalArgumentException("El nombre de la operacion no puede estar vacio");
            }
            this.operation = operation;
            this.totalTimeNs = totalTimeNs;
            this.minTimeNs = minTimeNs;
            this.maxTimeNs = maxTimeNs;
            this.avgTimeNs = iterations > 0 ? (double) totalTimeNs / iterations : 0;
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
        if (operation == null) {
            throw new IllegalArgumentException("La operacion no puede ser null");
        }

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

    private List<Hallazgo> crearHallazgosDePrueba(int cantidad) {
        List<Hallazgo> hallazgos = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            hallazgos.add(new Hallazgo(
                "https://example.com/api/" + i,
                "Vulnerabilidad " + i,
                "Descripcion " + i,
                "High",
                "High",
                null
            ));
        }
        return hallazgos;
    }

    private List<Tarea> crearTareasDePrueba(int cantidad) {
        List<Tarea> tareas = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            tareas.add(new Tarea(
                "tarea-" + i,
                "analisis",
                "https://example.com/" + i,
                "En proceso"
            ));
        }
        return tareas;
    }

    private Path crearDirectorioTemporalConfig() throws IOException {
        tempDirActual = Files.createTempDirectory("burpia-benchmark");
        Path configDir = tempDirActual.resolve(".burpia");
        Files.createDirectories(configDir);
        System.setProperty("user.home", tempDirActual.toString());
        return tempDirActual;
    }

    private GestorConfiguracion crearGestorConfiguracionSilencioso() {
        PrintWriter nullOut = new PrintWriter(OutputStream.nullOutputStream(), true);
        return new GestorConfiguracion(nullOut, nullOut);
    }

    @Test
    @DisplayName("ReparadorJson - JSON valido sin reparacion")
    void benchmarkReparadorJsonValido() {
        String jsonValido = "{\"titulo\":\"SQL Injection\",\"severidad\":\"High\",\"confianza\":\"High\",\"descripcion\":\"Test\",\"evidencia\":\"test\"}";

        BenchmarkResult result = runBenchmark("ReparadorJson.jsonValido", () -> {
            String repaired = ReparadorJson.repararJson(jsonValido);
            assertEquals(jsonValido, repaired, "assertEquals failed at PerformanceBenchmarkTest.java:205");
        });

        System.out.println(result);

        assertTrue(result.avgTimeNs < TimeUnit.MILLISECONDS.toNanos(UMBRAL_MS_JSON_VALIDO),
            "JSON valido debe procesarse en <" + UMBRAL_MS_JSON_VALIDO + "ms promedio. " + result);
    }

    @Test
    @DisplayName("ReparadorJson - JSON con markdown")
    void benchmarkReparadorJsonConMarkdown() {
        String jsonConMarkdown = "```json\n{\"titulo\":\"XSS\",\"severidad\":\"Medium\",\"confianza\":\"High\",\"descripcion\":\"Test description\",\"evidencia\":\"evidence\"}\n```";

        BenchmarkResult result = runBenchmark("ReparadorJson.conMarkdown", () -> {
            String repaired = ReparadorJson.repararJson(jsonConMarkdown);
            assertTrue(repaired.contains("\"titulo\":\"XSS\""),
                "assertTrue failed at PerformanceBenchmarkTest.java:221");
        });

        System.out.println(result);

        assertTrue(result.avgTimeNs < TimeUnit.MILLISECONDS.toNanos(2),
            "JSON con markdown debe procesarse en <2ms promedio. " + result);
    }

    @Test
    @DisplayName("ReparadorJson - JSON danado complejo")
    void benchmarkReparadorJsonDanado() {
        String jsonDanado = "Here is the analysis:\n```json\n{\"hallazgos\":[{\"titulo\":\"SQL Injection in login\",\"severidad\":\"Critical\",\"confianza\":\"High\",\"descripcion\":\"Found SQL injection in /login endpoint with payload: ' OR '1'='1\",\"evidencia\":\"Request: POST /login\\nResponse: Database error\"}]}\n```\nSome extra text after.";

        BenchmarkResult result = runBenchmark("ReparadorJson.danado", () -> {
            String repaired = ReparadorJson.repararJson(jsonDanado);
            assertTrue(repaired.contains("\"hallazgos\""),
                "assertTrue failed at PerformanceBenchmarkTest.java:237");
        });

        System.out.println(result);

        assertTrue(result.avgTimeNs < TimeUnit.MILLISECONDS.toNanos(3),
            "JSON danado complejo debe procesarse en <3ms promedio. " + result);
    }

    @Test
    @DisplayName("ParserRespuestasAI - Extraer contenido OpenAI")
    void benchmarkParserRespuestasOpenAI() {
        String respuestaOpenAI = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"{\\\"titulo\\\":\\\"XSS\\\",\\\"severidad\\\":\\\"High\\\"}\"}}]}";

        BenchmarkResult result = runBenchmark("ParserRespuestasAI.OpenAI", () -> {
            String contenido = ParserRespuestasAI.extraerContenido(respuestaOpenAI, "OpenAI");
            assertTrue(contenido.contains("\"titulo\":\"XSS\""),
                "assertTrue failed at PerformanceBenchmarkTest.java:254");
        });

        System.out.println(result);

        assertTrue(result.avgTimeNs < TimeUnit.MILLISECONDS.toNanos(UMBRAL_MS_JSON_VALIDO),
            "Parseo OpenAI debe ser <" + UMBRAL_MS_JSON_VALIDO + "ms promedio. " + result);
    }

    @Test
    @DisplayName("ParserRespuestasAI - Extraer contenido Claude")
    void benchmarkParserRespuestasClaude() {
        String respuestaClaude = "{\"content\":[{\"type\":\"text\",\"text\":\"This is the analysis result with findings.\"}]}";

        BenchmarkResult result = runBenchmark("ParserRespuestasAI.Claude", () -> {
            String contenido = ParserRespuestasAI.extraerContenido(respuestaClaude, "Claude");
            assertEquals("This is the analysis result with findings.", contenido,
                "assertEquals failed at PerformanceBenchmarkTest.java:271");
        });

        System.out.println(result);

        assertTrue(result.avgTimeNs < TimeUnit.MILLISECONDS.toNanos(UMBRAL_MS_JSON_VALIDO),
            "Parseo Claude debe ser <" + UMBRAL_MS_JSON_VALIDO + "ms promedio. " + result);
    }

    @Test
    @DisplayName("Normalizador - normalizarTexto")
    void benchmarkNormalizador() {
        String texto = "  Linea1\\nLinea2\\tcon\\\"comillas\\\" y \\\\barra  ";

        BenchmarkResult result = runBenchmark("Normalizador.normalizarTexto", () -> {
            String normalizado = Normalizador.normalizarTexto(texto);
            assertTrue(normalizado.contains("Linea1"),
                "assertTrue failed at PerformanceBenchmarkTest.java:288");
        });

        System.out.println(result);

        assertTrue(result.avgTimeNs < TimeUnit.MILLISECONDS.toNanos(UMBRAL_MS_JSON_VALIDO),
            "Normalizar texto debe ser <" + UMBRAL_MS_JSON_VALIDO + "ms promedio. " + result);
    }

    @Test
    @DisplayName("Normalizador - esVacio")
    void benchmarkNormalizadorEsVacio() {
        String texto = "   texto con espacios   ";

        BenchmarkResult result = runBenchmark("Normalizador.esVacio", () -> {
            boolean vacio = Normalizador.esVacio(texto);
            assertFalse(vacio, "assertFalse failed at PerformanceBenchmarkTest.java:310");
        });

        System.out.println(result);

        assertTrue(result.avgTimeNs < TimeUnit.MILLISECONDS.toNanos(1),
            "esVacio debe ser <1ms promedio (deberia ser microsegundos). " + result);
    }

    @Test
    @DisplayName("ModeloTablaHallazgos - agregarHallazgo")
    void benchmarkModeloTablaHallazgos() throws Exception {
        modeloHallazgos = new ModeloTablaHallazgos(CAPACIDAD_MODELO_HALLAZGOS);
        List<Hallazgo> hallazgos = crearHallazgosDePrueba(CANTIDAD_ELEMENTOS_BATCH);

        AtomicReference<BenchmarkResult> resultRef = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            BenchmarkResult result = runBenchmark("ModeloTablaHallazgos.agregar", () -> {
                modeloHallazgos.agregarHallazgos(hallazgos);
                modeloHallazgos.limpiar();
            });
            resultRef.set(result);
            System.out.println(result);
        });

        assertNotNull(resultRef.get(), "assertNotNull failed at PerformanceBenchmarkTest.java:336");
        assertTrue(resultRef.get().avgTimeNs < TimeUnit.MILLISECONDS.toNanos(10),
            "Agregar 100 hallazgos debe ser <10ms promedio. " + resultRef.get());
    }

    @Test
    @DisplayName("I/O Configuration - Guardar configuracion")
    void benchmarkGuardarConfiguracion() throws Exception {
        crearDirectorioTemporalConfig();

        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI("OpenAI");
        config.establecerModeloParaProveedor("OpenAI", "gpt-4");
        config.establecerApiKeyParaProveedor("OpenAI", "sk-test-key");
        config.establecerTiempoEsperaAI(60);
        config.establecerRetrasoSegundos(5);
        config.establecerMaximoConcurrente(10);
        config.establecerIdiomaUi("es");

        BenchmarkResult result = runBenchmark("I/O.guardarConfiguracion", () -> {
            GestorConfiguracion gestor = crearGestorConfiguracionSilencioso();
            gestor.guardarConfiguracion(config);
        });

        System.out.println(result);

        assertTrue(result.avgTimeNs < TimeUnit.MILLISECONDS.toNanos(UMBRAL_MS_GUARDAR_CONFIG),
            "Guardar configuracion debe ser <" + UMBRAL_MS_GUARDAR_CONFIG + "ms promedio. " + result);
    }

    @Test
    @DisplayName("I/O Configuration - Cargar configuracion")
    void benchmarkCargarConfiguracion() throws Exception {
        crearDirectorioTemporalConfig();

        ConfiguracionAPI configOriginal = new ConfiguracionAPI();
        configOriginal.establecerProveedorAI("OpenAI");
        configOriginal.establecerModeloParaProveedor("OpenAI", "gpt-4");
        configOriginal.establecerApiKeyParaProveedor("OpenAI", "sk-test-key");
        configOriginal.establecerTiempoEsperaAI(60);
        configOriginal.establecerRetrasoSegundos(5);
        configOriginal.establecerMaximoConcurrente(10);
        configOriginal.establecerIdiomaUi("es");

        GestorConfiguracion gestorSetup = new GestorConfiguracion();
        gestorSetup = crearGestorConfiguracionSilencioso();
        gestorSetup.guardarConfiguracion(configOriginal);

        BenchmarkResult result = runBenchmark("I/O.cargarConfiguracion", () -> {
            GestorConfiguracion gestor = crearGestorConfiguracionSilencioso();
            ConfiguracionAPI loaded = gestor.cargarConfiguracion();
            assertEquals("OpenAI", loaded.obtenerProveedorAI(),
                "assertEquals failed at PerformanceBenchmarkTest.java:382");
        });

        System.out.println(result);

        assertTrue(result.avgTimeNs < TimeUnit.MILLISECONDS.toNanos(UMBRAL_MS_CARGAR_CONFIG),
            "Cargar configuracion debe ser <" + UMBRAL_MS_CARGAR_CONFIG + "ms promedio. " + result);
    }

    @Test
    @DisplayName("ModeloTablaTareas - Agregar multiples tareas (individual)")
    void benchmarkModeloTablaTareasAgregarIndividual() throws Exception {
        modeloTareas = new ModeloTablaTareas(CAPACIDAD_MODELO_TAREAS);
        List<Tarea> tareas = crearTareasDePrueba(CANTIDAD_ELEMENTOS_BATCH);

        AtomicReference<BenchmarkResult> resultRef = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            BenchmarkResult result = runBenchmark("ModeloTablaTareas.agregar100.individual", () -> {
                for (Tarea tarea : tareas) {
                    modeloTareas.agregarTarea(tarea);
                }
                modeloTareas.limpiar();
            });
            resultRef.set(result);
            System.out.println(result);
        });

        assertNotNull(resultRef.get(), "assertNotNull failed at PerformanceBenchmarkTest.java:414");
    }

    @Test
    @DisplayName("ModeloTablaTareas - Agregar multiples tareas (batch)")
    void benchmarkModeloTablaTareasAgregarBatch() throws Exception {
        modeloTareas = new ModeloTablaTareas(CAPACIDAD_MODELO_TAREAS);
        List<Tarea> tareas = crearTareasDePrueba(CANTIDAD_ELEMENTOS_BATCH);

        AtomicReference<BenchmarkResult> resultRef = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            BenchmarkResult result = runBenchmark("ModeloTablaTareas.agregar100.batch", () -> {
                modeloTareas.agregarTareas(tareas);
                modeloTareas.limpiar();
            });
            resultRef.set(result);
            System.out.println(result);
        });

        assertNotNull(resultRef.get(), "assertNotNull failed at PerformanceBenchmarkTest.java:434");
        assertTrue(resultRef.get().avgTimeNs < TimeUnit.MILLISECONDS.toNanos(UMBRAL_MS_BATCH_TAREAS),
            "Agregar 100 tareas en batch debe ser <" + UMBRAL_MS_BATCH_TAREAS + "ms promedio. " + resultRef.get());
    }

    @Test
    @DisplayName("ModeloTablaTareas - Eliminar por estado")
    void benchmarkModeloTablaTareasEliminarPorEstado() throws Exception {
        modeloTareas = new ModeloTablaTareas(CAPACIDAD_MODELO_TAREAS);

        AtomicReference<BenchmarkResult> resultRef = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            BenchmarkResult result = runBenchmark("ModeloTablaTareas.eliminarPorEstado", () -> {
                // Pre-poblar DENTRO del benchmark (incluido en la medicion)
                for (int i = 0; i < CANTIDAD_ELEMENTOS_BATCH; i++) {
                    modeloTareas.agregarTarea(new Tarea(
                        "tarea-" + i,
                        "analisis",
                        "https://example.com/" + i,
                        i % 2 == 0 ? "Completada" : "En proceso"
                    ));
                }
                // Medir eliminacion
                modeloTareas.eliminarPorEstado("Completada");
                modeloTareas.limpiar();
            });
            resultRef.set(result);
            System.out.println(result);
        });

        assertNotNull(resultRef.get(), "assertNotNull failed at PerformanceBenchmarkTest.java:465");
    }

    @Test
    @DisplayName("ConstructorPrompts - construir prompt de flujo")
    void benchmarkConstruirPromptFlujo() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerIdiomaUi("es");
        com.burpia.analyzer.ConstructorPrompts constructor =
            new com.burpia.analyzer.ConstructorPrompts(config);
        List<SolicitudAnalisis> solicitudes = crearSolicitudesDeFlujo();

        BenchmarkResult result = runBenchmark("ConstructorPrompts.construirPromptFlujo", () -> {
            String prompt = constructor.construirPromptFlujo(solicitudes);
            assertTrue(prompt.contains("SECUENCIA DE PETICIONES HTTP"),
                "assertTrue failed at PerformanceBenchmarkTest.java:478");
            assertTrue(prompt.contains("POST https://example.com/login"),
                "assertTrue failed at PerformanceBenchmarkTest.java:480");
        });

        System.out.println(result);

        assertTrue(result.avgTimeNs < TimeUnit.MILLISECONDS.toNanos(UMBRAL_MS_PROMPT_FLUJO),
            "Construir prompt de flujo debe ser <" + UMBRAL_MS_PROMPT_FLUJO + "ms promedio. " + result);
    }

    @Test
    @DisplayName("ConstructorPrompts - prompt individual con truncado")
    void benchmarkConstruirPromptIndividualConTruncado() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerIdiomaUi("en");
        com.burpia.analyzer.ConstructorPrompts constructor =
            new com.burpia.analyzer.ConstructorPrompts(config);
        String cuerpoGrande = "A".repeat(PoliticaMemoria.MAXIMO_CUERPO_ANALISIS_CARACTERES + 2000);
        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com/upload",
            "POST",
            "POST /upload HTTP/1.1\nHost: example.com",
            cuerpoGrande,
            "hash-truncado",
            null,
            200,
            "HTTP/1.1 200 OK",
            "{\"ok\":true}"
        );

        BenchmarkResult result = runBenchmark("ConstructorPrompts.construirPromptAnalisis.truncado", () -> {
            String prompt = constructor.construirPromptAnalisis(solicitud);
            assertTrue(prompt.contains("[TRUNCATED request body: +2000 characters]"),
                "assertTrue failed at PerformanceBenchmarkTest.java:522");
            assertTrue(prompt.contains("POST https://example.com/upload"),
                "assertTrue failed at PerformanceBenchmarkTest.java:524");
        });

        System.out.println(result);

        assertTrue(result.avgTimeNs < TimeUnit.MILLISECONDS.toNanos(UMBRAL_MS_PROMPT_TRUNCADO),
            "Construir prompt truncado debe ser <" + UMBRAL_MS_PROMPT_TRUNCADO + "ms promedio. " + result);
    }

    @Test
    @DisplayName("FlowAnalysisRequestBuilder - crear solicitud de flujo")
    void benchmarkCrearSolicitudFlujo() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        List<SolicitudAnalisis> solicitudes = crearSolicitudesDeFlujo();

        BenchmarkResult result = runBenchmark("FlowAnalysisRequestBuilder.crearSolicitudFlujo", () -> {
            SolicitudAnalisis flujo = FlowAnalysisRequestBuilder.crearSolicitudFlujo(config, solicitudes);
            assertNotNull(flujo, "assertNotNull failed at PerformanceBenchmarkTest.java:497");
            assertTrue(flujo.obtenerPromptPreconstruido().contains("POST https://example.com/login"),
                "assertTrue failed at PerformanceBenchmarkTest.java:498");
        });

        System.out.println(result);

        assertTrue(result.avgTimeNs < TimeUnit.MILLISECONDS.toNanos(UMBRAL_MS_SOLICITUD_FLUJO),
            "Crear solicitud de flujo debe ser <" + UMBRAL_MS_SOLICITUD_FLUJO + "ms promedio. " + result);
    }

    @Test
    @DisplayName("FlowAnalysisConstraints - contar requests validas")
    void benchmarkContarRequestsValidasFlujo() {
        HttpRequest request = mock(HttpRequest.class);
        HttpRequestResponse valida1 = mock(HttpRequestResponse.class);
        HttpRequestResponse valida2 = mock(HttpRequestResponse.class);
        HttpRequestResponse invalida = mock(HttpRequestResponse.class);
        when(valida1.request()).thenReturn(request);
        when(valida2.request()).thenReturn(request);
        when(invalida.request()).thenReturn(null);
        List<HttpRequestResponse> seleccion = Arrays.asList(valida1, valida2, invalida, null);

        BenchmarkResult result = runBenchmark("FlowAnalysisConstraints.contarSolicitudesValidas", () -> {
            int total = FlowAnalysisConstraints.contarSolicitudesValidas(seleccion);
            assertEquals(2, total, "assertEquals failed at PerformanceBenchmarkTest.java:518");
        });

        System.out.println(result);

        assertTrue(result.avgTimeNs < TimeUnit.MILLISECONDS.toNanos(1),
            "Contar requests válidas debe ser <1ms promedio. " + result);
    }

    @Test
    @DisplayName("ParseadorRespuestasAI - parsear respuesta no estricta")
    void benchmarkParseadorRespuestasAiNoEstricta() {
        ParseadorRespuestasAI parseador = new ParseadorRespuestasAI(null, "es");
        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com/banner",
            "GET",
            "",
            "",
            "hash-parseo"
        );
        String respuesta = "{\"hallazgos\":["
            + "{\"titulo\":\"Uno\",\"descripcion\":\"Detalle 1\",\"severidad\":\"Low\",\"confianza\":\"High\",\"evidencia\":\"a\"},"
            + "{\"titulo\":\"Dos\",\"descripcion\":\"Detalle 2\",\"severidad\":\"Medium\",\"confianza\":\"Medium\",\"evidencia\":\"<div style=\"background-color:lightgray\"><b>Warning</b>: demo</div>\"}"
            + "]}";

        BenchmarkResult result = runBenchmark("ParseadorRespuestasAI.parsearRespuesta", () -> {
            var resultado = parseador.parsearRespuesta(respuesta, solicitud, "OpenAI");
            assertEquals(2, resultado.obtenerNumeroHallazgos(),
                "assertEquals failed at PerformanceBenchmarkTest.java:568");
            assertTrue(resultado.obtenerHallazgos().get(1).obtenerHallazgo().contains("Warning"),
                "assertTrue failed at PerformanceBenchmarkTest.java:570");
        });

        System.out.println(result);

        assertTrue(result.avgTimeNs < TimeUnit.MILLISECONDS.toNanos(UMBRAL_MS_PARSEO_AI),
            "Parsear respuesta AI debe ser <" + UMBRAL_MS_PARSEO_AI + "ms promedio. " + result);
    }

    @Test
    @DisplayName("TaskExecutionManager - programar analisis")
    void benchmarkProgramarAnalisisTaskExecutionManager() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerMaximoConcurrente(1);
        SolicitudAnalisis solicitud = new SolicitudAnalisis(
            "https://example.com/api",
            "GET",
            "GET /api HTTP/1.1\nHost: example.com",
            "",
            "hash-benchmark-task"
        );

        try (MockedConstruction<com.burpia.analyzer.AnalizadorAI> construccion = mockConstruction(com.burpia.analyzer.AnalizadorAI.class)) {
            BenchmarkResult result = runBenchmark("TaskExecutionManager.programarAnalisis", () -> {
                GestorTareas gestor = new GestorTareas(new ModeloTablaTareas(), mensaje -> { });
                TaskExecutionManager manager = new TaskExecutionManager(
                    config,
                    gestor,
                    null,
                    null,
                    new PrintWriter(OutputStream.nullOutputStream(), true),
                    new PrintWriter(OutputStream.nullOutputStream(), true),
                    new LimitadorTasa(1),
                    null
                );
                String tareaId = manager.programarAnalisis(solicitud, null, "Analisis HTTP");
                assertNotNull(tareaId, "assertNotNull failed at PerformanceBenchmarkTest.java:599");
                assertNotNull(gestor.obtenerTarea(tareaId), "assertNotNull failed at PerformanceBenchmarkTest.java:600");
                manager.shutdown();
                gestor.detener();
            });

            System.out.println(result);

            assertTrue(construccion.constructed().size() >= MEASUREMENT_ITERATIONS,
                "Debe construir analizadores durante el benchmark");
            assertTrue(result.avgTimeNs < TimeUnit.MILLISECONDS.toNanos(UMBRAL_MS_PROGRAMAR_TAREA),
                "Programar análisis debe ser <" + UMBRAL_MS_PROGRAMAR_TAREA + "ms promedio. " + result);
        }
    }

    @Test
    @DisplayName("GestorTareas - cancelar todas las activas")
    void benchmarkGestorTareasCancelarTodas() {
        BenchmarkResult result = runBenchmark("GestorTareas.cancelarTodas", () -> {
            ModeloTablaTareas modelo = new ModeloTablaTareas(1000);
            GestorTareas gestor = new GestorTareas(modelo, mensaje -> { });
            try {
                for (int i = 0; i < CANTIDAD_ELEMENTOS_BATCH; i++) {
                    gestor.crearTarea(
                        "analisis",
                        "https://example.com/" + i,
                        i % 2 == 0 ? Tarea.ESTADO_EN_COLA : Tarea.ESTADO_ANALIZANDO,
                        "activa"
                    );
                }
                gestor.cancelarTodas();
                assertTrue(gestor.estaTareaCancelada(modelo.obtenerIdTarea(0)),
                    "assertTrue failed at PerformanceBenchmarkTest.java:626");
            } finally {
                gestor.detener();
            }
        });

        System.out.println(result);

        assertTrue(result.avgTimeNs < TimeUnit.MILLISECONDS.toNanos(UMBRAL_MS_CANCELAR_TODAS),
            "Cancelar todas debe ser <" + UMBRAL_MS_CANCELAR_TODAS + "ms promedio. " + result);
    }

    private List<SolicitudAnalisis> crearSolicitudesDeFlujo() {
        return List.of(
            new SolicitudAnalisis(
                "https://example.com/login",
                "POST",
                "POST /login HTTP/1.1\nHost: example.com",
                "username=admin",
                "hash-flow-1",
                null,
                302,
                "HTTP/1.1 302 Found\nLocation: /dashboard",
                ""
            ),
            new SolicitudAnalisis(
                "https://example.com/dashboard",
                "GET",
                "GET /dashboard HTTP/1.1\nHost: example.com",
                "",
                "hash-flow-2",
                null,
                200,
                "HTTP/1.1 200 OK",
                "{\"role\":\"user\"}"
            )
        );
    }
}
