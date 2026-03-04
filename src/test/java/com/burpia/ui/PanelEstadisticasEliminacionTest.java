package com.burpia.ui;

import com.burpia.ExtensionBurpIA;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.Estadisticas;
import com.burpia.model.Hallazgo;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.PersistedObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests de integración para verificar que PanelEstadisticas
 * se actualiza correctamente cuando se eliminan hallazgos.
 *
 * Estos tests verifican el comportamiento end-to-end de:
 * 1. Eliminar todos los hallazgos
 * 2. Eliminar un hallazgo individual
 * 3. Limpiar la tabla
 */
@DisplayName("PanelEstadisticas Integración Eliminación Tests")
class PanelEstadisticasEliminacionTest {

    private ModeloTablaHallazgos modelo;
    private PanelHallazgos panelHallazgos;
    private PanelEstadisticas panelEstadisticas;
    private Estadisticas estadisticas;

    @BeforeEach
    void setUp() {
        modelo = new ModeloTablaHallazgos(1000);
        estadisticas = new Estadisticas();

        // Crear mocks necesarios
        MontoyaApi mockApi = mock(MontoyaApi.class);
        ConfiguracionAPI mockConfig = mock(ConfiguracionAPI.class);
        when(mockConfig.obtenerMaximoHallazgosTabla()).thenReturn(1000);

        panelHallazgos = new PanelHallazgos(mockApi, modelo, false);
        panelHallazgos.establecerConfiguracion(mockConfig);

        panelEstadisticas = new PanelEstadisticas(
            estadisticas,
            () -> 1000,
            panelHallazgos
        );
    }

    @Test
    @DisplayName("Al eliminar todos los hallazgos, estadísticas muestran 0")
    void alEliminarTodosLosHallazgosEstadisticasMuestranCero() throws Exception {
        // Agregar algunos hallazgos
        for (int i = 0; i < 5; i++) {
            Hallazgo hallazgo = crearHallazgoPrueba("Hallazgo " + i);
            modelo.agregarHallazgo(hallazgo);
        }

        // Esperar a que se procesen eventos en EDT
        SwingUtilities.invokeAndWait(() -> {});

        // Verificar que se muestran estadísticas con hallazgos
        int[] statsAntes = panelHallazgos.obtenerEstadisticasVisibles();
        assertEquals(5, statsAntes[0], "Debería haber 5 hallazgos visibles antes de eliminar");

        // Eliminar todos los hallazgos
        modelo.limpiar();

        // Esperar a que se procesen eventos en EDT
        SwingUtilities.invokeAndWait(() -> {});

        // Verificar que las estadísticas se actualizaron a 0
        int[] statsDespues = panelHallazgos.obtenerEstadisticasVisibles();
        assertEquals(0, statsDespues[0], "Debería haber 0 hallazgos visibles después de limpiar");
        assertEquals(0, statsDespues[1], "Debería haber 0 Critical después de limpiar");
        assertEquals(0, statsDespues[2], "Debería haber 0 High después de limpiar");
        assertEquals(0, statsDespues[3], "Debería haber 0 Medium después de limpiar");
    }

    @Test
    @DisplayName("Al eliminar un hallazgo, estadísticas se decrementan")
    void alEliminarUnHallazgoEstadisticasSeDecrementan() throws Exception {
        // Agregar 3 hallazgos con diferentes severidades
        // Array: [total, critical, high, medium, low, info]
        modelo.agregarHallazgo(crearHallazgoPrueba("Hallazgo 1", "Critical")); // stats[1]
        modelo.agregarHallazgo(crearHallazgoPrueba("Hallazgo 2", "High"));    // stats[2]
        modelo.agregarHallazgo(crearHallazgoPrueba("Hallazgo 3", "Medium"));  // stats[3]

        // Esperar a que se procesen eventos en EDT
        SwingUtilities.invokeAndWait(() -> {});

        // Verificar estadísticas iniciales
        int[] statsAntes = panelHallazgos.obtenerEstadisticasVisibles();
        assertEquals(3, statsAntes[0], "Debería haber 3 hallazgos antes de eliminar");
        assertEquals(1, statsAntes[1], "Debería haber 1 Critical antes de eliminar");
        assertEquals(1, statsAntes[2], "Debería haber 1 High antes de eliminar");
        assertEquals(1, statsAntes[3], "Debería haber 1 Medium antes de eliminar");

        // Eliminar el hallazgo Critical (índice 0)
        modelo.eliminarHallazgo(0);

        // Esperar a que se procesen eventos en EDT
        SwingUtilities.invokeAndWait(() -> {});

        // Verificar que las estadísticas se actualizaron
        int[] statsDespues = panelHallazgos.obtenerEstadisticasVisibles();
        assertEquals(2, statsDespues[0], "Debería haber 2 hallazgos después de eliminar");
        assertEquals(0, statsDespues[1], "Debería haber 0 Critical después de eliminar");
        assertEquals(1, statsDespues[2], "Debería seguir habiendo 1 High");
        assertEquals(1, statsDespues[3], "Debería seguir habiendo 1 Medium");
    }

    @Test
    @DisplayName("Al agregar y eliminar múltiples veces, estadísticas son consistentes")
    void alAgregarYEliminarMultiplesVecesEstadisticasSonConsistentes() throws Exception {
        // Ciclo de agregar y eliminar
        for (int ciclo = 0; ciclo < 3; ciclo++) {
            // Agregar 2 hallazgos
            modelo.agregarHallazgo(crearHallazgoPrueba("H" + ciclo + "A", "Critical"));
            modelo.agregarHallazgo(crearHallazgoPrueba("H" + ciclo + "B", "High"));

            SwingUtilities.invokeAndWait(() -> {});

            int[] statsConHallazgos = panelHallazgos.obtenerEstadisticasVisibles();
            assertEquals(2, statsConHallazgos[0], "Ciclo " + ciclo + ": Debería haber 2 hallazgos");

            // Eliminar uno
            modelo.eliminarHallazgo(0);

            SwingUtilities.invokeAndWait(() -> {});

            int[] statsConUno = panelHallazgos.obtenerEstadisticasVisibles();
            assertEquals(1, statsConUno[0], "Ciclo " + ciclo + ": Debería quedar 1 hallazgo");

            // Limpiar todo
            modelo.limpiar();

            SwingUtilities.invokeAndWait(() -> {});

            int[] statsVacios = panelHallazgos.obtenerEstadisticasVisibles();
            assertEquals(0, statsVacios[0], "Ciclo " + ciclo + ": Debería haber 0 hallazgos");
        }
    }

    private Hallazgo crearHallazgoPrueba(String titulo) {
        return crearHallazgoPrueba(titulo, "Medium");
    }

    private Hallazgo crearHallazgoPrueba(String titulo, String severidad) {
        return new Hallazgo(
            "https://example.com/" + titulo.replaceAll(" ", "-").toLowerCase(),
            titulo,
            "Descripción de prueba para " + titulo,
            severidad,
            "Certain"
        );
    }
}
