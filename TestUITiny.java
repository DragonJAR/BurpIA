import javax.swing.*;
import java.awt.*;
public class TestUITiny {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame();
            JButton b = new JButton("1");
            b.putClientProperty("JButton.buttonType", "square");
            f.add(b, BorderLayout.NORTH);
            f.pack();
            System.out.println(b.getHeight() + ", " + b.getInsets());
            f.dispose();
            System.exit(0);
        });
    }
}
