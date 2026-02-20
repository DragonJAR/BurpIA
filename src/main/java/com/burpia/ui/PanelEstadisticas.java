package com.burpia.ui;

import com.burpia.model.Estadisticas;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.function.IntSupplier;

public class PanelEstadisticas extends JPanel {
    private final JLabel etiquetaResumenPrincipal;
    private final JLabel etiquetaResumenSeveridad;
    private final JLabel etiquetaLimiteHallazgos;
    private final JLabel etiquetaResumenOperativo;
    private final Estadisticas estadisticas;
    private final IntSupplier proveedorLimiteHallazgos;
    private Timer timerActualizacion;
    private final JButton botonConfiguracion;
    private final JButton botonLimpiar;
    private Runnable manejadorConfiguracion;

    private JPanel panelContenidoCentral;
    private JPanel panelHallazgos;
    private JPanel panelOperativo;
    private JPanel panelBotones;

    private static final int UMBRAL_RESPONSIVE = 900;

    public PanelEstadisticas(Estadisticas estadisticas, IntSupplier proveedorLimiteHallazgos) {
        this.estadisticas = estadisticas;
        this.proveedorLimiteHallazgos = proveedorLimiteHallazgos != null ? proveedorLimiteHallazgos : () -> 1000;
        this.etiquetaResumenPrincipal = new JLabel();
        this.etiquetaResumenSeveridad = new JLabel();
        this.etiquetaLimiteHallazgos = new JLabel();
        this.etiquetaResumenOperativo = new JLabel();
        this.botonConfiguracion = new JButton();
        this.botonLimpiar = new JButton();
        initComponents();
    }

    public void establecerManejadorConfiguracion(Runnable manejador) {
        this.manejadorConfiguracion = manejador;
        botonConfiguracion.addActionListener(e -> manejador.run());
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 4));
        setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        panelHallazgos = crearPanelHallazgos();
        panelOperativo = crearPanelOperativo();

        panelContenidoCentral = new JPanel(new GridLayout(1, 2, 8, 0)) {
            private boolean ultimoLayoutHorizontal = true;

            @Override
            public void doLayout() {
                int ancho = getWidth() - 132;
                boolean esLayoutHorizontal = ancho >= UMBRAL_RESPONSIVE;

                if (esLayoutHorizontal != ultimoLayoutHorizontal) {
                    if (esLayoutHorizontal) {
                        setLayout(new GridLayout(1, 2, 8, 0));
                    } else {
                        setLayout(new GridLayout(2, 1, 0, 6));
                    }
                    ultimoLayoutHorizontal = esLayoutHorizontal;
                }

                super.doLayout();
            }
        };

        panelContenidoCentral.add(panelHallazgos);
        panelContenidoCentral.add(panelOperativo);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                panelContenidoCentral.revalidate();
                panelContenidoCentral.repaint();
                ajustarDimensionBotones();
            }
        });

        panelBotones = crearPanelBotones();
        JPanel panelLateral = new JPanel(new GridBagLayout());
        panelLateral.setOpaque(false);
        panelLateral.add(panelBotones);

        add(panelContenidoCentral, BorderLayout.CENTER);
        add(panelLateral, BorderLayout.EAST);

        timerActualizacion = new Timer(1000, e -> actualizar());
        timerActualizacion.start();
        actualizar();
        SwingUtilities.invokeLater(this::ajustarDimensionBotones);
    }

    private JPanel crearPanelHallazgos() {
        JPanel panel = crearPanelSeccion("🎯 POSIBLES HALLAZGOS Y CRITICIDADES");

        JPanel linea = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        linea.setOpaque(false);

        etiquetaResumenPrincipal.setFont(EstilosUI.FUENTE_MONO_NEGRITA);
        etiquetaResumenPrincipal.setForeground(new Color(0, 102, 204));
        linea.add(etiquetaResumenPrincipal);

        JLabel separador = new JLabel(" | ");
        separador.setFont(EstilosUI.FUENTE_MONO_NEGRITA);
        separador.setForeground(new Color(140, 140, 140));
        linea.add(separador);

        etiquetaResumenSeveridad.setFont(EstilosUI.FUENTE_MONO);
        etiquetaResumenSeveridad.setForeground(new Color(70, 70, 70));
        linea.add(etiquetaResumenSeveridad);

        JLabel separador2 = new JLabel(" | ");
        separador2.setFont(EstilosUI.FUENTE_MONO_NEGRITA);
        separador2.setForeground(new Color(140, 140, 140));
        linea.add(separador2);

        etiquetaLimiteHallazgos.setFont(EstilosUI.FUENTE_MONO);
        etiquetaLimiteHallazgos.setForeground(new Color(80, 80, 80));
        linea.add(etiquetaLimiteHallazgos);

        JPanel centro = new JPanel(new GridBagLayout());
        centro.setOpaque(false);
        centro.add(linea);
        panel.add(centro, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearPanelOperativo() {
        JPanel panel = crearPanelSeccion("📊 DETALLES OPERATIVOS");

        etiquetaResumenOperativo.setFont(EstilosUI.FUENTE_MONO);
        etiquetaResumenOperativo.setForeground(new Color(90, 90, 90));

        JPanel linea = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        linea.setOpaque(false);
        linea.add(etiquetaResumenOperativo);

        JPanel centro = new JPanel(new GridBagLayout());
        centro.setOpaque(false);
        centro.add(linea);
        panel.add(centro, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearPanelSeccion(String titulo) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
            titulo,
            TitledBorder.LEFT,
            TitledBorder.TOP,
            EstilosUI.FUENTE_NEGRITA
        ));
        return panel;
    }

    private JPanel crearPanelBotones() {
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        panelBotones.setOpaque(false);
        panelBotones.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        botonLimpiar.setText("🧹");
        botonLimpiar.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        botonLimpiar.setToolTipText("Limpiar estadísticas");
        botonLimpiar.setFocusable(false);
        botonLimpiar.addActionListener(e -> {
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

        botonConfiguracion.setText("⚙️");
        botonConfiguracion.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        botonConfiguracion.setToolTipText("Abrir ajustes");
        botonConfiguracion.setFocusable(false);

        // Orden requerido: primero Limpiar, luego Ajustes
        panelBotones.add(botonLimpiar);
        panelBotones.add(botonConfiguracion);
        return panelBotones;
    }

    private void ajustarDimensionBotones() {
        if (botonLimpiar == null || botonConfiguracion == null) {
            return;
        }

        int alturaReferencia = 0;
        if (panelHallazgos != null && panelHallazgos.getHeight() > 0) {
            alturaReferencia = panelHallazgos.getHeight();
        } else if (panelOperativo != null && panelOperativo.getHeight() > 0) {
            alturaReferencia = panelOperativo.getHeight();
        } else if (panelHallazgos != null) {
            alturaReferencia = panelHallazgos.getPreferredSize().height;
        }

        if (alturaReferencia <= 0) {
            return;
        }

        Dimension tamano = new Dimension(alturaReferencia, alturaReferencia);
        aplicarDimensionBotonCuadrado(botonLimpiar, tamano);
        aplicarDimensionBotonCuadrado(botonConfiguracion, tamano);

        if (panelBotones != null) {
            panelBotones.revalidate();
            panelBotones.repaint();
        }
    }

    private void aplicarDimensionBotonCuadrado(JButton boton, Dimension tamano) {
        boton.setPreferredSize(tamano);
        boton.setMinimumSize(tamano);
        boton.setMaximumSize(tamano);
    }

    public void actualizar() {
        SwingUtilities.invokeLater(() -> {
            etiquetaResumenPrincipal.setText(String.format("🔎 Total: %d", estadisticas.obtenerHallazgosCreados()));

            etiquetaResumenSeveridad.setText(
                String.format("🟣 %d   🔴 %d   🟠 %d   🟢 %d   🔵 %d",
                    estadisticas.obtenerHallazgosCritical(),
                    estadisticas.obtenerHallazgosHigh(),
                    estadisticas.obtenerHallazgosMedium(),
                    estadisticas.obtenerHallazgosLow(),
                    estadisticas.obtenerHallazgosInfo())
            );

            etiquetaLimiteHallazgos.setText(
                String.format("🧮 Límite Hallazgos: %d", proveedorLimiteHallazgos.getAsInt())
            );

            etiquetaResumenOperativo.setText(
                String.format("📥 Solicitudes: %d   |   ✅ Analizados: %d   |   ⏭ Omitidos: %d   |   ❌ Errores: %d",
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
