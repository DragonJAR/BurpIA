import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class TestAlignment {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 2, 8));

            JPanel panelContenido = new JPanel(new GridLayout(1, 2, 8, 0));
            
            JPanel p1 = new JPanel(new BorderLayout());
            p1.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(Color.BLACK, 1),
                    "TARGET TITLE",
                    TitledBorder.LEFT,
                    TitledBorder.TOP,
                    new Font("SansSerif", Font.BOLD, 12)
            ));
            
            JPanel textLine = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
            textLine.add(new JLabel("Total: 0 | etc | etc"));
            p1.add(textLine, BorderLayout.NORTH);
            
            panelContenido.add(p1);

            JPanel panelLateral = new JPanel(new GridBagLayout());
            JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
            JButton btn = new JButton("B");
            btn.setPreferredSize(new Dimension(30, 30));
            panelBotones.add(btn);

            JPanel contenedor = new JPanel(new BorderLayout());
            contenedor.add(panelBotones, BorderLayout.NORTH);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; gbc.weighty = 1;
            gbc.anchor = GridBagConstraints.FIRST_LINE_START;
            panelLateral.add(contenedor, gbc);

            mainPanel.add(panelContenido, BorderLayout.CENTER);
            mainPanel.add(panelLateral, BorderLayout.EAST);
            
            frame.add(mainPanel);
            frame.pack();

            // Print info
            System.out.println("TitledBorder insets: " + p1.getInsets());
            System.out.println("TextLine Y in P1: " + textLine.getLocation().y);
            System.out.println("TextLine bounds: " + textLine.getBounds());
            
            // Replicate logic
            Point origen = SwingUtilities.convertPoint(p1, textLine.getLocation(), panelLateral);
            System.out.println("Origen in panelLateral: " + origen);
            int offset = origen.y + textLine.getHeight() / 2 - 15;
            System.out.println("Calculated offset: " + offset);
            
            System.exit(0);
        });
    }
}
