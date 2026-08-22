package com.burpia.ui;

import burp.api.montoya.MontoyaApi;
import com.burpia.model.Estadisticas;
import com.burpia.util.GestorConsolaGUI;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Answers;

import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * Base compartida para tests de paneles Swing de BurpIA.
 * <p>
 * Centraliza los helpers que antes se copiaban en cada clase de test:
 * creación de paneles en el EDT con registro de destrucción automática,
 * acceso a campos privados por reflexión, sincronización con el EDT y
 * verificación de tooltips de encabezados de tabla.
 * </p>
 * <p>
 * Los paneles creados con los métodos {@code crearPanel*} quedan registrados
 * y se destruyen en {@link #destruirPanelesRegistrados()} al final de cada
 * test, porque arrancan Timers y executors que de otro modo fugarían hilos.
 * </p>
 */
abstract class PanelTestBase {

    private final List<Runnable> destruccionesPendientes = new ArrayList<>();

    @AfterEach
    void destruirPanelesRegistrados() {
        for (Runnable destruir : destruccionesPendientes) {
            destruir.run();
        }
        destruccionesPendientes.clear();
    }

    /**
     * Registra una acción de destrucción que se ejecutará al final del test.
     * Pensado para paneles construidos a mano fuera de los {@code crearPanel*}.
     */
    protected final void registrarDestruccion(Runnable destruir) {
        destruccionesPendientes.add(destruir);
    }

    /**
     * Crea un PanelHallazgos con MontoyaApi mock (deep stubs) y un modelo
     * fresco de 100 filas, construido en el EDT y registrado para destrucción.
     */
    protected final PanelHallazgos crearPanelHallazgos(boolean esBurpProfessional) throws Exception {
        MontoyaApi api = mock(MontoyaApi.class, Answers.RETURNS_DEEP_STUBS);
        return crearPanelHallazgos(api, new ModeloTablaHallazgos(100), esBurpProfessional);
    }

    /**
     * Crea un PanelHallazgos con la API y el modelo indicados, construido en
     * el EDT y registrado para destrucción al final del test.
     */
    protected final PanelHallazgos crearPanelHallazgos(MontoyaApi api, ModeloTablaHallazgos modelo,
            boolean esBurpProfessional) throws Exception {
        final PanelHallazgos[] holder = new PanelHallazgos[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelHallazgos(api, modelo, esBurpProfessional));
        PanelHallazgos panel = holder[0];
        assertNotNull(panel, "El panel debe haberse creado correctamente");
        registrarDestruccion(panel::destruir);
        return panel;
    }

    /**
     * Crea un PanelConsola con su gestor fresco, construido en el EDT y
     * registrado para destrucción al final del test.
     */
    protected final PanelConsola crearPanelConsola() throws Exception {
        final PanelConsola[] holder = new PanelConsola[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelConsola(new GestorConsolaGUI()));
        PanelConsola panel = holder[0];
        assertNotNull(panel, "El panel debe haberse creado correctamente");
        registrarDestruccion(panel::destruir);
        return panel;
    }

    /**
     * Crea un PanelEstadisticas con estadísticas frescas y límite fijo de
     * 1000 hallazgos, construido en el EDT y registrado para destrucción.
     */
    protected final PanelEstadisticas crearPanelEstadisticas() throws Exception {
        final PanelEstadisticas[] holder = new PanelEstadisticas[1];
        SwingUtilities.invokeAndWait(() ->
            holder[0] = new PanelEstadisticas(new Estadisticas(), () -> 1000, null));
        PanelEstadisticas panel = holder[0];
        assertNotNull(panel, "El panel debe haberse creado correctamente");
        registrarDestruccion(panel::destruir);
        return panel;
    }

    /**
     * Obtiene el valor de un campo privado mediante reflexión.
     * Uso legítimo en tests, donde el caller controla el tipo esperado.
     */
    protected static <T> T obtenerCampo(Object target, String nombreCampo, Class<T> tipoEsperado) throws Exception {
        assertNotNull(target, "El objeto no puede ser null");
        Field field = target.getClass().getDeclaredField(nombreCampo);
        field.setAccessible(true);
        return tipoEsperado.cast(field.get(target));
    }

    /**
     * Espera a que todos los eventos pendientes en el EDT sean procesados.
     * El cuerpo vacío del Runnable es intencional: invokeAndWait bloquea hasta
     * drenar la cola del EDT, sincronizando el estado de la UI.
     */
    protected static void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
    }

    /**
     * Verifica que el tooltip del encabezado de una columna coincide con el
     * esperado, simulando el mouseMoved que Swing usa para resolver tooltips.
     */
    protected static void assertTooltipEncabezado(JTableHeader encabezado, int columnaVista,
            String esperado) throws Exception {
        Rectangle rect = encabezado.getHeaderRect(columnaVista);
        MouseEvent evento = new MouseEvent(
                encabezado,
                MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(),
                0,
                rect.x + Math.max(1, rect.width / 2),
                rect.y + Math.max(1, rect.height / 2),
                0,
                false);
        SwingUtilities.invokeAndWait(() -> {
            for (MouseMotionListener listener : encabezado.getMouseMotionListeners()) {
                listener.mouseMoved(evento);
            }
        });
        assertEquals(esperado, encabezado.getToolTipText(),
            "El tooltip del encabezado de la columna " + columnaVista + " debe coincidir con el esperado");
    }
}
