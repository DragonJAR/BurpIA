package com.burpia.config;

import com.burpia.util.RutasBurpIA;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base test class para tests que necesitan configuración aislada.
 * Establece el sufijo "test" para usar config-test.json en lugar de config.json.
 * 
 * IMPORTANTE: También aísla user.home para evitar contaminar la configuración
 * del usuario. Alarga y restaura el valor original después de cada test.
 */
public abstract class ConfigTestBase {
    
    private String userHomeOriginal;
    
    @BeforeEach
    void setUpConfigTest() {
        // Guardar user.home original antes de cualquier modificación
        this.userHomeOriginal = System.getProperty("user.home");
        
        // Limpiar caché y establecer sufijo de test
        RutasBurpIA.limpiarCacheParaTests();
        RutasBurpIA.establecerSuffixConfig("test");
    }
    
    @AfterEach
    void tearDownConfigTest() {
        // Restaurar user.home original (CRÍTICO para evitar pérdida de config)
        if (userHomeOriginal != null) {
            System.setProperty("user.home", userHomeOriginal);
        } else {
            System.clearProperty("user.home");
        }
        
        // Limpiar caché de RutasBurpIA para que use el user.home restaurado
        RutasBurpIA.limpiarCacheParaTests();
        
        // Resetear sufijo de configuración
        RutasBurpIA.establecerSuffixConfig(null);
    }
}
