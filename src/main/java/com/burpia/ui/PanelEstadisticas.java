package com.burpia.ui;

import com.burpia.model.Estadisticas;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class PanelEstadisticas extends JPanel {
    private final JLabel etiquetaResumenPrincipal;
    private final JLabel etiquetaResumenSeveridad;
    private final JLabel etiquetaResumenOperativo;
    private final Estadisticas estadisticas;
    private Timer timerActualizacion;
    private final JButton botonConfiguracion;
    private Runnable manejadorConfiguracion;

    private JPanel panelSuperior;
    private JPanel panelInferior;
    private JPanel panelContenidoCentral;

    // Umbral de ancho para cambiar de layout (en pixeles)
    private static final int UMBRAL_RESPONSIVE = 900;

    public PanelEstadisticas(Estadisticas estadisticas) {
        this.estadisticas = estadisticas;
        this.etiquetaResumenPrincipal = new JLabel();
        this.etiquetaResumenSeveridad = new JLabel();
        this.etiquetaResumenOperativo = new JLabel();
        this.botonConfiguracion = new JButton("🧠 Ajustes");
        initComponents();
    }

    public void establecerManejadorConfiguracion(Runnable manejador) {
        this.manejadorConfiguracion = manejador;
        botonConfiguracion.addActionListener(e -> manejador.run());
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        panelSuperior = crearPanelHallazgos();

        panelInferior = crearPanelOperativo();

        panelContenidoCentral = new JPanel(new GridLayout(1, 2, 10, 0)) {
            private boolean ultimoLayoutHorizontal = true;

            @Override
            public void doLayout() {
                // Detectar ancho disponible
                int ancho = getWidth() - 140; // Restar márgenes y panel de botones
                boolean esLayoutHorizontal = ancho >= UMBRAL_RESPONSIVE;

                // Cambiar layout solo si es necesario
                if (esLayoutHorizontal != ultimoLayoutHorizontal) {
                    if (esLayoutHorizontal) {
                        setLayout(new GridLayout(1, 2, 10, 0)); // Horizontal: 1 fila, 2 columnas
                    } else {
                        setLayout(new GridLayout(2, 1, 0, 10)); // Vertical: 2 filas, 1 columna
                    }
                    ultimoLayoutHorizontal = esLayoutHorizontal;
                }

                super.doLayout();
            }
        };

        panelContenidoCentral.add(panelSuperior);
        panelContenidoCentral.add(panelInferior);

        // Listener para detectar cambios de tamaño y ajustar layout
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                panelContenidoCentral.revalidate();
                panelContenidoCentral.repaint();
            }
        });

        JPanel panelBotones = crearPanelBotones();

        add(panelContenidoCentral, BorderLayout.CENTER);
        add(panelBotones, BorderLayout.EAST);

        // Timer para actualizar cada segundo
        timerActualizacion = new Timer(1000, e -> actualizar());
        timerActualizacion.start();

        // Actualización inicial
        actualizar();
    }

    private JPanel crearPanelHallazgos() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
            "🎯 HALLAZGOS Y CRITICIDAD",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            EstilosUI.FUENTE_NEGRITA
        ));

<<<<<<< ours
        JPanel panelHallazgosCompleto = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        panelHallazgosCompleto.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Total de hallazgos (izquierda)
        etiquetaResumenPrincipal.setFont(EstilosUI.FUENTE_MONO);
        etiquetaResumenPrincipal.setForeground(new Color(0, 102, 204)); // Azul destacado
        panelHallazgosCompleto.add(etiquetaResumenPrincipal);

        // Separador visual
        JLabel separador = new JLabel(" | ");
        separador.setFont(EstilosUI.FUENTE_MONO_NEGRITA);
        separador.setForeground(new Color(150, 150, 150));
        panelHallazgosCompleto.add(separador);

        // Niveles de severidad (misma línea)
        etiquetaResumenSeveridad.setFont(EstilosUI.FUENTE_MONO);
        panelHallazgosCompleto.add(etiquetaResumenSeveridad);
=======
        JPanel panelHallazgosCompleto = new JPanel();
        panelHallazgosCompleto.setOpaque(false);
        panelHallazgosCompleto.setLayout(new BoxLayout(panelHallazgosCompleto, BoxLayout.Y_AXIS));
        panelHallazgosCompleto.setBorder(BorderFactory.createEmptyBorder(2, 8, 6, 8));

        JPanel filaPrincipal = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filaPrincipal.setOpaque(false);
        etiquetaResumenPrincipal.setFont(EstilosUI.FUENTE_MONO_NEGRITA);
        etiquetaResumenPrincipal.setForeground(new Color(0, 102, 204)); // Azul destacado
        filaPrincipal.add(etiquetaResumenPrincipal);

        JPanel filaSeveridades = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        filaSeveridades.setOpaque(false);
        etiquetaResumenSeveridad.setFont(EstilosUI.FUENTE_MONO);
        etiquetaResumenSeveridad.setForeground(new Color(70, 70, 70));
        filaSeveridades.add(etiquetaResumenSeveridad);
>>>>>>> theirs

        panelHallazgosCompleto.add(filaPrincipal);
        panelHallazgosCompleto.add(Box.createVerticalStrut(4));
        panelHallazgosCompleto.add(filaSeveridades);
        panel.add(panelHallazgosCompleto);
        return panel;
    }

    private JPanel crearPanelOperativo() {
<<<<<<< ours
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
=======
        JPanel panel = crearPanelSeccion("📊 DETALLES OPERATIVOS");
        JPanel panelOperativo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        panelOperativo.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        panelOperativo.setOpaque(false);

        etiquetaResumenOperativo.setFont(EstilosUI.FUENTE_MONO_NEGRITA);
        etiquetaResumenOperativo.setForeground(new Color(100, 100, 100)); // Gris suave
        panelOperativo.add(etiquetaResumenOperativo);
        panel.add(panelOperativo);

        return panel;
    }

    private JPanel crearPanelSeccion(String titulo) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
>>>>>>> theirs
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
            "📊 DETALLES OPERATIVOS",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            EstilosUI.FUENTE_NEGRITA
        ));

        JPanel panelOperativo = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        etiquetaResumenOperativo.setFont(EstilosUI.FUENTE_MONO);
        etiquetaResumenOperativo.setForeground(new Color(100, 100, 100)); // Gris suave
        panelOperativo.add(etiquetaResumenOperativo);
        panel.add(panelOperativo);

        return panel;
    }

    private JPanel crearPanelBotones() {
        JPanel panelBotones = new JPanel();
        panelBotones.setLayout(new BoxLayout(panelBotones, BoxLayout.Y_AXIS));

        // Dimensiones iguales para ambos botones
        Dimension tamanoBoton = new Dimension(120, 28);

        botonConfiguracion.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonConfiguracion.setToolTipText("Abrir ajustes de API");
        botonConfiguracion.setPreferredSize(tamanoBoton);
        botonConfiguracion.setMinimumSize(tamanoBoton);
        botonConfiguracion.setMaximumSize(tamanoBoton);
        panelBotones.add(botonConfiguracion);
        panelBotones.add(Box.createVerticalStrut(5));

        JButton botonReiniciar = new JButton("🧹 Limpiar");
        botonReiniciar.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonReiniciar.setToolTipText("Limpiar todos los contadores");
        botonReiniciar.setPreferredSize(tamanoBoton);
        botonReiniciar.setMinimumSize(tamanoBoton);
        botonReiniciar.setMaximumSize(tamanoBoton);
        botonReiniciar.addActionListener(e -> {
            int confirmacion = JOptionPane.showConfirmDialog(
                this,
                "¿Estás seguro de que deseas limpiar todas las estadísticas?",
                "Confirmar limpieza",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            if (confirmacion == JOptionPane.YES_OPTION) {
                estadisticas.reiniciar();
                actualizar();
            }
        });

        panelBotones.add(botonReiniciar);
        return panelBotones;
    }

    public void actualizar() {
        SwingUtilities.invokeLater(() -> {
            // Total de hallazgos (compacto, misma línea que severidades)
            etiquetaResumenPrincipal.setText(
<<<<<<< ours
                String.format("🔍 Total: %d",
=======
                String.format("🔎 Total hallazgos: %d",
>>>>>>> theirs
                    estadisticas.obtenerHallazgosCreados())
            );

            // Niveles de severidad (misma línea que total de hallazgos)
            etiquetaResumenSeveridad.setText(
<<<<<<< ours
                String.format("🟣 %d | 🔴 %d | 🟠 %d | 🟢 %d | 🔵 %d",
=======
                String.format("🟣 Critical: %d   |   🔴 High: %d   |   🟠 Medium: %d   |   🟢 Low: %d   |   🔵 Info: %d",
>>>>>>> theirs
                    estadisticas.obtenerHallazgosCritical(),
                    estadisticas.obtenerHallazgosHigh(),
                    estadisticas.obtenerHallazgosMedium(),
                    estadisticas.obtenerHallazgosLow(),
                    estadisticas.obtenerHallazgosInfo())
            );

            // LÍNEA OPERATIVA (abajo, más pequeña y gris)
            // Detalles de procesamiento
            etiquetaResumenOperativo.setText(
                String.format("📥 Solicitudes: %d | ✅ Analizados: %d | ⏭️ Omitidos: %d | ❌ Errores: %d",
                    estadisticas.obtenerTotalSolicitudes(),
                    estadisticas.obtenerAnalizados(),
                    estadisticas.obtenerTotalOmitidos(),
                    estadisticas.obtenerErrores())
            );
        });
    }

    public Estadisticas obtenerEstadisticas() {
        return estadisticas;
    }

    public void destruir() {
        timerActualizacion.stop();
    }
}
