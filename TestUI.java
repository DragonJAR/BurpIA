package com.burpia.ui;

import com.burpia.model.Estadisticas;
import javax.swing.*;

public class TestUI {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Estadisticas estadisticas = new Estadisticas(); // Assuming empty constructor exists
            PanelEstadisticas panel = new PanelEstadisticas(estadisticas, () -> 100);
            JFrame frame = new JFrame();
            frame.add(panel);
            frame.setSize(1000, 200);
            frame.setVisible(true);
            
            SwingUtilities.invokeLater(() -> {
                System.out.println("After layout:");
                // Use reflection or just public method to trigger or print
            });
        });
    }
}
