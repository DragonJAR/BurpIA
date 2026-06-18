package com.burpia.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para ConfiguracionAPIRef.
 * Verifica el comportamiento thread-safe del AtomicReference wrapper.
 */
@DisplayName("ConfiguracionAPIRef Tests")
class ConfiguracionAPIRefTest {

    private ConfiguracionAPIRef ref;

    @BeforeEach
    void setUp() {
        ref = new ConfiguracionAPIRef(new ConfiguracionAPI());
    }

    @Test
    @DisplayName("Configuracion por defecto obtiene snapshot funcional")
    void testConfiguracionPorDefectoObtieneSnapshot() {
        ConfiguracionAPI config = ref.obtener();

        assertNotNull(config, "assertNotNull failed at ConfiguracionAPIRefTest.java:31");
        assertEquals("Ollama", config.obtenerProveedorAI(),
            "assertEquals failed at ConfiguracionAPIRefTest.java:32");
        assertEquals("es", config.obtenerIdiomaUi(),
            "assertEquals failed at ConfiguracionAPIRefTest.java:33");
        assertFalse(config.esDetallado(),
            "assertFalse failed at ConfiguracionAPIRefTest.java:34");
        assertEquals(ConfiguracionAPI.MAXIMO_HALLAZGOS_TABLA_DEFECTO, config.obtenerMaximoHallazgosTabla(),
            "assertEquals failed at ConfiguracionAPIRefTest.java:35");
    }

    @Test
    @DisplayName("Reemplazar actualiza la referencia")
    void testReemplazarActualizaReferencia() {
        ConfiguracionAPI nueva = new ConfiguracionAPI();
        nueva.establecerProveedorAI("OpenAI");
        nueva.establecerMaximoConcurrente(5);
        nueva.establecerDetallado(true);

        ref.reemplazar(nueva);

        ConfiguracionAPI result = ref.obtener();
        assertEquals("OpenAI", result.obtenerProveedorAI(),
            "assertEquals failed at ConfiguracionAPIRefTest.java:49");
        assertEquals(5, result.obtenerMaximoConcurrente(),
            "assertEquals failed at ConfiguracionAPIRefTest.java:50");
        assertTrue(result.esDetallado(),
            "assertTrue failed at ConfiguracionAPIRefTest.java:51");
    }

    @Test
    @DisplayName("Reemplazar es atomico bajo acceso concurrente")
    void testReemplazarEsAtomico() throws InterruptedException {
        int numHilos = 10;
        int actualizacionesPorHilo = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numHilos);
        CountDownLatch latch = new CountDownLatch(numHilos);
        AtomicInteger actualizacionesExitosas = new AtomicInteger(0);

        // Cada hilo reemplaza la configuracion con una nueva instancia
        for (int i = 0; i < numHilos; i++) {
            final int idHilo = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < actualizacionesPorHilo; j++) {
                        ConfiguracionAPI nueva = new ConfiguracionAPI();
                        nueva.establecerMaximoConcurrente(idHilo);
                        nueva.establecerMaximoHallazgosTabla(j);
                        ref.reemplazar(nueva);
                        actualizacionesExitosas.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completodo = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completodo, "assertTrue failed at ConfiguracionAPIRefTest.java:82 - timeout esperando hilos");
        assertEquals(numHilos * actualizacionesPorHilo, actualizacionesExitosas.get(),
            "assertEquals failed at ConfiguracionAPIRefTest.java:83");

        // Verificar que la referencia final es consistente (no null, no corrupta)
        // Los valores fueron normalizados por ConfiguracionAPI al establecerse:
        // - maximoConcurrente normalizado a rango [1, 10]
        // - maximoHallazgosTabla normalizado a rango [100, 50000]
        ConfiguracionAPI finalConfig = ref.obtener();
        assertNotNull(finalConfig, "assertNotNull failed at ConfiguracionAPIRefTest.java:99");
        assertTrue(finalConfig.obtenerMaximoConcurrente() >= ConfiguracionAPI.MINIMO_MAXIMO_CONCURRENTE &&
                   finalConfig.obtenerMaximoConcurrente() <= ConfiguracionAPI.MAXIMO_MAXIMO_CONCURRENTE,
            "assertTrue failed at ConfiguracionAPIRefTest.java:101 - valor normalizado fuera de rango");
        assertTrue(finalConfig.obtenerMaximoHallazgosTabla() >= ConfiguracionAPI.MINIMO_HALLAZGOS_TABLA &&
                   finalConfig.obtenerMaximoHallazgosTabla() <= ConfiguracionAPI.MAXIMO_HALLAZGOS_TABLA,
            "assertTrue failed at ConfiguracionAPIRefTest.java:103 - valor normalizado fuera de rango");
    }

    // Tests del CAS reemplazarSiEsperada (M3): reintroducido para evitar
    // lost-update entre el EDT y los handlers HTTP que mutan la config.

    @Test
    @DisplayName("M3: reemplazarSiEsperada succeeds cuando la ref no cambió")
    void testReemplazarSiEsperadaSuccess() {
        ConfiguracionAPI actual = ref.obtener();
        ConfiguracionAPI modificada = actual.crearSnapshot();
        modificada.establecerProveedorAI("OpenAI");

        boolean aplicado = ref.reemplazarSiEsperada(actual, modificada);

        assertTrue(aplicado, "El CAS debe aplicar si la ref sigue siendo la esperada");
        assertEquals("OpenAI", ref.obtener().obtenerProveedorAI());
    }

    @Test
    @DisplayName("M3: reemplazarSiEsperada falla si la ref cambió concurrentemente (lost-update evitado)")
    void testReemplazarSiEsperadaFallaSiCambioConcurrente() {
        ConfiguracionAPI actual = ref.obtener();
        // Simula que el EDT (u otro writer) actualizó la ref entre nuestro
        // obtener() y nuestro CAS. El CAS debe fallar para no pisar su cambio.
        ConfiguracionAPI escrituraConcurrente = new ConfiguracionAPI();
        escrituraConcurrente.establecerProveedorAI("Gemini");
        ref.reemplazar(escrituraConcurrente);

        ConfiguracionAPI modificada = actual.crearSnapshot();
        modificada.establecerProveedorAI("Claude");

        boolean aplicado = ref.reemplazarSiEsperada(actual, modificada);

        assertFalse(aplicado, "El CAS debe fallar si la ref cambió concurrentemente");
        assertEquals("Gemini", ref.obtener().obtenerProveedorAI(),
            "La escritura concurrente debe preservarse (no perderse por lost-update)");
    }

    @Test
    @DisplayName("M3: reemplazarSiEsperada rechaza args null")
    void testReemplazarSiEsperadaRechazaNull() {
        assertFalse(ref.reemplazarSiEsperada(null, new ConfiguracionAPI()));
        assertFalse(ref.reemplazarSiEsperada(ref.obtener(), null));
    }

    @Test
    @DisplayName("Reemplazar null no modifica la referencia")
    void testReemplazarNullNoModificaReferencia() {
        ConfiguracionAPI original = ref.obtener();

        ref.reemplazar(null);

        assertSame(original, ref.obtener(),
            "assertSame failed at ConfiguracionAPIRefTest.java:131");
        assertNotNull(ref.obtener(), "assertNotNull failed at ConfiguracionAPIRefTest.java:132");
    }

    @Test
    @DisplayName("ConfiguracionAPIRef null crea configuracion por defecto")
    void testConstructorNullCreaConfiguracionPorDefecto() {
        ConfiguracionAPIRef refNull = new ConfiguracionAPIRef(null);

        assertNotNull(refNull.obtener(), "assertNotNull failed at ConfiguracionAPIRefTest.java:141");
        assertEquals("Ollama", refNull.obtener().obtenerProveedorAI(),
            "assertEquals failed at ConfiguracionAPIRefTest.java:142");
    }

    @Test
    @DisplayName("Obtener retorna la referencia almacenada")
    void testObtenerRetornaReferenciaAlmacenada() {
        ConfiguracionAPI config1 = ref.obtener();
        ConfiguracionAPI config2 = ref.obtener();

        // Debe ser la misma instancia (no es un snapshot)
        assertSame(config1, config2, "assertSame failed at ConfiguracionAPIRefTest.java:152");
    }

    @Test
    @DisplayName("Modificaciones al objeto obtenido afecta la referencia almacenada")
    void testModificacionesAlObtenidoAfectaReferenciaAlmacenada() {
        ConfiguracionAPI obtenido = ref.obtener();

        // Modificar el objeto obtenido (usar proveedor valido para evitar normalizacion)
        obtenido.establecerProveedorAI("OpenAI");

        // Verificar que la referencia almacenada tambien cambio
        // obtener() retorna la referencia viva, no un snapshot
        assertEquals("OpenAI", ref.obtener().obtenerProveedorAI(),
            "assertEquals failed at ConfiguracionAPIRefTest.java:185 - obtener() retorna referencia viva, modificacion debe reflejarse");
    }

    @Test
    @DisplayName("Multiples reemplazos mantienen consistencia")
    void testMultiplesReemplazosMantienenConsistencia() {
        ConfiguracionAPI[] configs = new ConfiguracionAPI[5];
        for (int i = 0; i < configs.length; i++) {
            configs[i] = new ConfiguracionAPI();
            configs[i].establecerMaximoTareasTabla(100 * (i + 1));
        }

        // Reemplazos secuenciales
        for (ConfiguracionAPI config : configs) {
            ref.reemplazar(config);
        }

        assertEquals(500, ref.obtener().obtenerMaximoTareasTabla(),
            "assertEquals failed at ConfiguracionAPIRefTest.java:182");
    }

    @Test
    @DisplayName("Thread-safety con lectores y escritores simultaneos")
    void testThreadSafetyLectoresEscritoresSimultaneos() throws InterruptedException {
        int numEscritores = 5;
        int numLectores = 5;
        int operacionesPorHilo = 50;
        ExecutorService executor = Executors.newFixedThreadPool(numEscritores + numLectores);
        CountDownLatch latch = new CountDownLatch(numEscritores + numLectores);
        AtomicInteger lecturasExitosas = new AtomicInteger(0);
        AtomicInteger escriturasExitosas = new AtomicInteger(0);

        // Escritores
        for (int i = 0; i < numEscritores; i++) {
            final int idEscritor = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operacionesPorHilo; j++) {
                        ConfiguracionAPI nueva = new ConfiguracionAPI();
                        nueva.establecerMaximoConcurrente(idEscritor);
                        ref.reemplazar(nueva);
                        escriturasExitosas.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Lectores
        for (int i = 0; i < numLectores; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operacionesPorHilo; j++) {
                        ConfiguracionAPI config = ref.obtener();
                        if (config != null && config.obtenerMaximoConcurrente() >= 0) {
                            lecturasExitosas.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completodo = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completodo, "assertTrue failed at ConfiguracionAPIRefTest.java:224 - timeout");
        assertEquals(numEscritores * operacionesPorHilo, escriturasExitosas.get(),
            "assertEquals failed at ConfiguracionAPIRefTest.java:225");
        assertEquals(numLectores * operacionesPorHilo, lecturasExitosas.get(),
            "assertEquals failed at ConfiguracionAPIRefTest.java:226");

        // Verificar que al final la referencia es valida
        assertNotNull(ref.obtener(), "assertNotNull failed at ConfiguracionAPIRefTest.java:228");
    }

    @Test
    @DisplayName("Reemplazar con nueva instancia a pesar de referencias externas")
    void testReemplazarInstanciaNuevaAislada() {
        // Obtener referencia externa
        ConfiguracionAPI externa = ref.obtener();
        assertEquals("Ollama", externa.obtenerProveedorAI(),
            "assertEquals failed at ConfiguracionAPIRefTest.java:239");

        // Reemplazar con nueva configuracion
        ConfiguracionAPI nueva = new ConfiguracionAPI();
        nueva.establecerProveedorAI("OpenAI");
        ref.reemplazar(nueva);

        // Verificar que la referencia almacenada cambio
        assertEquals("OpenAI", ref.obtener().obtenerProveedorAI(),
            "assertEquals failed at ConfiguracionAPIRefTest.java:246");

        // Verificar que la referencia externa sigue siendo la original
        assertEquals("Ollama", externa.obtenerProveedorAI(),
            "assertEquals failed at ConfiguracionAPIRefTest.java:249");
    }
}
