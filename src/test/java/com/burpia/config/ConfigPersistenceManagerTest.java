package com.burpia.config;

import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.RutasBurpIA;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.io.PrintWriter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para ConfigPersistenceManager.
 * <p>
 * IMPORTANTE: Este test usa directorio temporal para evitar tocar
 * la configuración real del usuario en ~/.burpia/config.json
 * </p>
 */
class ConfigPersistenceManagerTest {

    @TempDir
    Path tempDir;

    private String userHomeOriginal;
    private GestorConfiguracion gestorConfig;
    private GestorLoggingUnificado gestorLogging;
    private ConfigPersistenceManager persistenceManager;

    @BeforeEach
    void setUp() {
        // Guardar user.home original y redirigir a directorio temporal
        userHomeOriginal = System.getProperty("user.home");
        RutasBurpIA.limpiarCacheParaTests();
        System.setProperty("user.home", tempDir.toString());

        gestorConfig = new GestorConfiguracion(new PrintWriter(System.out), new PrintWriter(System.err));
        gestorLogging = GestorLoggingUnificado.crearMinimal(new PrintWriter(System.out), new PrintWriter(System.err));
        persistenceManager = new ConfigPersistenceManager(gestorConfig, gestorLogging);
    }

    @AfterEach
    void tearDown() {
        // Restaurar user.home original
        if (userHomeOriginal != null) {
            System.setProperty("user.home", userHomeOriginal);
        }
        RutasBurpIA.limpiarCacheParaTests();
    }
    
    @Test
    void testConstructorConGestorConfiguracionNuloDebeLanzarExcepcion() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new ConfigPersistenceManager(null, gestorLogging)
        );
        
        assertEquals("GestorConfiguracion no puede ser nulo", exception.getMessage());
    }
    
    @Test
    void testConstructorConGestorLoggingNuloDebeCrearUnoPorDefecto() {
        ConfigPersistenceManager manager = new ConfigPersistenceManager(gestorConfig, null);
        assertNotNull(manager);
    }
    
    @Test
    void testCargarConfiguracion() {
        ConfiguracionAPI config = persistenceManager.cargarConfiguracion();
        
        assertNotNull(config);
        assertTrue(persistenceManager.existeConfiguracionCargada());
        assertEquals(config, persistenceManager.obtenerConfiguracionCargada());
    }
    
    @Test
    void testValidarConfiguracionNula() {
        assertFalse(persistenceManager.validarConfiguracion(null));
        
        Map<String, String> errores = persistenceManager.obtenerErroresValidacion(null);
        assertEquals(1, errores.size());
        assertTrue(errores.containsKey("configuracion"));
    }
    
    @Test
    void testValidarConfiguracionValida() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI("OpenAI");
        config.establecerModelo("gpt-4o"); // Esto establece el modelo para el proveedor actual
        config.establecerApiKeyParaProveedor("OpenAI", "test-key");
        config.establecerRetrasoSegundos(0);
        config.establecerMaximoConcurrente(5);
        config.establecerMaximoHallazgosTabla(1000);
        config.establecerTiempoEsperaAI(120);
        config.establecerMaximoTareasTabla(2000);
        
        Map<String, String> errores = persistenceManager.obtenerErroresValidacion(config);
        assertTrue(persistenceManager.validarConfiguracion(config));
        assertTrue(errores.isEmpty());
    }
    
    @Test
    void testGuardarConfiguracionNula() {
        assertFalse(persistenceManager.guardarConfiguracion(null));
        
        StringBuilder mensajeError = new StringBuilder();
        assertFalse(persistenceManager.guardarConfiguracion(null, mensajeError));
        assertTrue(mensajeError.length() > 0);
    }
    
    @Test
    void testGuardarConfiguracionValida() {
        ConfiguracionAPI config = new ConfiguracionAPI();
        config.establecerProveedorAI("OpenAI");
        config.establecerApiKeyParaProveedor("OpenAI", "test-key");
        config.establecerModelo("gpt-4o");
        config.establecerRetrasoSegundos(0);
        config.establecerMaximoConcurrente(5);
        config.establecerMaximoHallazgosTabla(1000);
        config.establecerTiempoEsperaAI(120);
        config.establecerMaximoTareasTabla(2000);
        
        assertTrue(persistenceManager.guardarConfiguracion(config));
        assertFalse(persistenceManager.hayCambiosPendientes());
    }
    
    @Test
    void testHayCambiosPendientes() {
        ConfiguracionAPI config = persistenceManager.cargarConfiguracion();
        assertFalse(persistenceManager.hayCambiosPendientes());
        
        config.establecerRetrasoSegundos(10);
        assertTrue(persistenceManager.hayCambiosPendientes());
        
        persistenceManager.actualizarEstadoReferencia();
        assertFalse(persistenceManager.hayCambiosPendientes());
    }
    
    @Test
    void testObtenerRutaArchivoConfiguracion() {
        String ruta = persistenceManager.obtenerRutaArchivoConfiguracion();
        assertNotNull(ruta);
        assertFalse(ruta.isEmpty());
    }
}