package com.burpia.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PanelFilasResponsive - Layout dinámico")
class PanelFilasResponsiveTest {

    /**
     * Fuerza la propagación de tamaño del frame al panel responsive.
     * Swing no actualiza getWidth() del parent tras setSize() hasta que se
     * valida la jerarquía. doLayout() consulta getParent().getWidth().
     */
    private void redimensionar(JFrame frame, int ancho) {
        frame.setSize(ancho, 200);
        frame.validate();
        frame.repaint();
    }

    @Test
    @DisplayName("Layout horizontal cuando el ancho disponible supera el umbral")
    void layoutHorizontalCuandoAnchoSuperaUmbral() throws Exception {
        int umbral = 400;
        AtomicReference<Boolean> esHorizontal = new AtomicReference<>(null);
        AtomicReference<Long> separadoresRef = new AtomicReference<>(0L);

        SwingUtilities.invokeAndWait(() -> {
            List<List<javax.swing.JComponent>> filas = List.of(
                List.of(new JButton("A"), new JButton("B")),
                List.of(new JButton("C"))
            );
            PanelFilasResponsive panel = new PanelFilasResponsive(umbral, 8, 4, filas);

            JFrame frame = new JFrame();
            frame.setLayout(new BorderLayout());
            frame.add(panel, BorderLayout.CENTER);
            redimensionar(frame, umbral + 100);
            frame.setVisible(true);

            // doLayout() dispara la detección de cambio y reconstrucción
            panel.doLayout();

            esHorizontal.set(panel.getLayout() instanceof FlowLayout);
            separadoresRef.set(java.util.Arrays.stream(panel.getComponents())
                .filter(c -> c instanceof JSeparator)
                .count());

            frame.dispose();
        });

        assertTrue(esHorizontal.get(),
            "El layout debe ser FlowLayout (horizontal) cuando el ancho supera el umbral");
        assertEquals(1, separadoresRef.get(),
            "Debe haber un JSeparator entre las dos filas en modo horizontal");
    }

    @Test
    @DisplayName("Layout vertical cuando el ancho disponible no supera el umbral")
    void layoutVerticalCuandoAnchoNoSuperaUmbral() throws Exception {
        int umbral = 500;
        AtomicReference<Boolean> esVertical = new AtomicReference<>(null);
        AtomicReference<Long> separadoresRef = new AtomicReference<>(0L);

        SwingUtilities.invokeAndWait(() -> {
            List<List<javax.swing.JComponent>> filas = List.of(
                List.of(new JButton("A"), new JButton("B")),
                List.of(new JButton("C"))
            );
            PanelFilasResponsive panel = new PanelFilasResponsive(umbral, 8, 4, filas);

            JFrame frame = new JFrame();
            frame.setLayout(new BorderLayout());
            frame.add(panel, BorderLayout.CENTER);
            redimensionar(frame, umbral - 100);
            frame.setVisible(true);

            panel.doLayout();

            esVertical.set(panel.getLayout() instanceof GridLayout);
            separadoresRef.set(java.util.Arrays.stream(panel.getComponents())
                .filter(c -> c instanceof JSeparator)
                .count());

            frame.dispose();
        });

        assertTrue(esVertical.get(),
            "El layout debe ser GridLayout (vertical) cuando el ancho no supera el umbral");
        assertEquals(0, separadoresRef.get(),
            "No debe haber separadores verticales en modo vertical");
    }

    @Test
    @DisplayName("Transiciona de horizontal a vertical al reducir el ancho")
    void transicionaHorizontalAVerticalAlReducirAncho() throws Exception {
        int umbral = 400;
        AtomicReference<Boolean> cambioDetectado = new AtomicReference<>(false);

        SwingUtilities.invokeAndWait(() -> {
            List<List<javax.swing.JComponent>> filas = List.of(
                List.of(new JButton("Btn1"), new JButton("Btn2"))
            );
            PanelFilasResponsive panel = new PanelFilasResponsive(umbral, 6, 4, filas);

            JFrame frame = new JFrame();
            frame.setLayout(new BorderLayout());
            frame.add(panel, BorderLayout.CENTER);

            // Estado inicial: ancho mayor al umbral → horizontal
            redimensionar(frame, umbral + 200);
            frame.setVisible(true);
            panel.doLayout();
            boolean layoutInicialHorizontal = panel.getLayout() instanceof FlowLayout;

            // Reducir ancho por debajo del umbral → debe cambiar a vertical
            redimensionar(frame, umbral - 100);
            panel.doLayout();
            boolean layoutFinalVertical = panel.getLayout() instanceof GridLayout;

            frame.dispose();

            cambioDetectado.set(layoutInicialHorizontal && layoutFinalVertical);
        });

        assertTrue(cambioDetectado.get(),
            "Debe cambiar de FlowLayout a GridLayout cuando el ancho cruza el umbral hacia abajo");
    }

    @Test
    @DisplayName("Transiciona de vertical a horizontal al aumentar el ancho")
    void transicionaVerticalAHorizontalAlAumentarAncho() throws Exception {
        int umbral = 400;
        AtomicReference<Boolean> cambioDetectado = new AtomicReference<>(false);

        SwingUtilities.invokeAndWait(() -> {
            List<List<javax.swing.JComponent>> filas = List.of(
                List.of(new JButton("Btn1"), new JButton("Btn2"))
            );
            PanelFilasResponsive panel = new PanelFilasResponsive(umbral, 6, 4, filas);

            JFrame frame = new JFrame();
            frame.setLayout(new BorderLayout());
            frame.add(panel, BorderLayout.CENTER);

            // Estado inicial: ancho menor al umbral → vertical
            redimensionar(frame, umbral - 100);
            frame.setVisible(true);
            panel.doLayout();
            boolean layoutInicialVertical = panel.getLayout() instanceof GridLayout;

            // Aumentar ancho por encima del umbral → debe cambiar a horizontal
            redimensionar(frame, umbral + 200);
            panel.doLayout();
            boolean layoutFinalHorizontal = panel.getLayout() instanceof FlowLayout;

            frame.dispose();

            cambioDetectado.set(layoutInicialVertical && layoutFinalHorizontal);
        });

        assertTrue(cambioDetectado.get(),
            "Debe cambiar de GridLayout a FlowLayout cuando el ancho cruza el umbral hacia arriba");
    }

    @Test
    @DisplayName("Mantiene todos los componentes visibles tras multiples cambios de layout")
    void mantieneComponentesTrasMultiplesCambiosDeLayout() throws Exception {
        int umbral = 300;
        AtomicReference<Integer> conteoFinal = new AtomicReference<>(0);

        SwingUtilities.invokeAndWait(() -> {
            JButton btn1 = new JButton("Btn1");
            JButton btn2 = new JButton("Btn2");
            JButton btn3 = new JButton("Btn3");

            List<List<javax.swing.JComponent>> filas = List.of(
                List.of(btn1, btn2),
                List.of(btn3)
            );
            PanelFilasResponsive panel = new PanelFilasResponsive(umbral, 8, 4, filas);

            JFrame frame = new JFrame();
            frame.setLayout(new BorderLayout());
            frame.add(panel, BorderLayout.CENTER);

            // Ciclo: horizontal → vertical → horizontal → vertical
            for (int ciclo = 0; ciclo < 4; ciclo++) {
                int ancho = (ciclo % 2 == 0) ? umbral + 200 : umbral - 100;
                redimensionar(frame, ancho);
                panel.doLayout();
            }

            // Contar botones en cualquier nivel de anidamiento (modo vertical
            // envuelve cada fila en un JPanel)
            int botones = contarBotonesRecursivo(panel);

            frame.dispose();
            conteoFinal.set(botones);
        });

        assertEquals(3, conteoFinal.get(),
            "Los 3 botones deben seguir presentes tras múltiples transiciones de layout");
    }

    @Test
    @DisplayName("Tolera filas null y componentes null sin lanzar NPE")
    void toleraFilasYComponentesNulos() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JButton btn = new JButton("X");
            List<List<javax.swing.JComponent>> filas = new java.util.ArrayList<>();
            List<javax.swing.JComponent> filaConNull = new java.util.ArrayList<>();
            filaConNull.add(btn);
            filaConNull.add(null);
            filas.add(filaConNull);
            filas.add(null);

            // List.copyOf lanzaría NPE con estos nulls; la copia debe filtrarlos.
            PanelFilasResponsive panel = new PanelFilasResponsive(300, 8, 4, filas);

            assertEquals(1, contarBotonesRecursivo(panel),
                "Solo el componente no nulo debe quedar agregado al panel");
        });
    }

    private int contarBotonesRecursivo(java.awt.Container contenedor) {
        int count = 0;
        for (java.awt.Component c : contenedor.getComponents()) {
            if (c instanceof JButton) {
                count++;
            }
            if (c instanceof java.awt.Container sub) {
                count += contarBotonesRecursivo(sub);
            }
        }
        return count;
    }
}
