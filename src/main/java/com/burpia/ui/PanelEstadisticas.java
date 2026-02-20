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
    private JPanel panelLateral;
    private JPanel panelLineaHallazgos;
    private JPanel panelLineaOperativo;

    private static final int UMBRAL_RESPONSIVE = 900;
    private static final int TAMANIO_FIJO_BOTON = 40;

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
        setLayout(new BorderLayout(8, 2));
        setBorder(BorderFactory.createEmptyBorder(4, 8, 2, 8));

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
        panelLateral = new JPanel(new BorderLayout());
        panelLateral.setOpaque(false);
        panelLateral.add(panelBotones, BorderLayout.NORTH);

        add(panelContenidoCentral, BorderLayout.CENTER);
        add(panelLateral, BorderLayout.EAST);

        timerActualizacion = new Timer(1000, e -> actualizar());
        timerActualizacion.start();
        actualizar();
        SwingUtilities.invokeLater(this::ajustarDimensionBotones);
    }

    private JPanel crearPanelHallazgos() {
        JPanel panel = crearPanelSeccion("🎯 POSIBLES HALLAZGOS Y CRITICIDADES");

        panelLineaHallazgos = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        panelLineaHallazgos.setOpaque(false);
        panelLineaHallazgos.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));

        etiquetaResumenPrincipal.setFont(EstilosUI.FUENTE_MONO_NEGRITA);
        etiquetaResumenPrincipal.setForeground(new Color(0, 102, 204));
        panelLineaHallazgos.add(etiquetaResumenPrincipal);

        JLabel separador = new JLabel(" | ");
        separador.setFont(EstilosUI.FUENTE_MONO_NEGRITA);
        separador.setForeground(new Color(140, 140, 140));
        panelLineaHallazgos.add(separador);

        etiquetaResumenSeveridad.setFont(EstilosUI.FUENTE_MONO);
        etiquetaResumenSeveridad.setForeground(new Color(70, 70, 70));
        panelLineaHallazgos.add(etiquetaResumenSeveridad);

        JLabel separador2 = new JLabel(" | ");
        separador2.setFont(EstilosUI.FUENTE_MONO_NEGRITA);
        separador2.setForeground(new Color(140, 140, 140));
        panelLineaHallazgos.add(separador2);

        etiquetaLimiteHallazgos.setFont(EstilosUI.FUENTE_MONO);
        etiquetaLimiteHallazgos.setForeground(new Color(80, 80, 80));
        panelLineaHallazgos.add(etiquetaLimiteHallazgos);

        JPanel centro = new JPanel(new GridBagLayout());
        centro.setOpaque(false);
        centro.add(panelLineaHallazgos);
        panel.add(centro, BorderLayout.CENTER);

        return panel;
    }

    private JPanel crearPanelOperativo() {
        JPanel panel = crearPanelSeccion("📊 DETALLES OPERATIVOS");

        etiquetaResumenOperativo.setFont(EstilosUI.FUENTE_MONO);
        etiquetaResumenOperativo.setForeground(new Color(90, 90, 90));

        panelLineaOperativo = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panelLineaOperativo.setOpaque(false);
        panelLineaOperativo.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
        panelLineaOperativo.add(etiquetaResumenOperativo);

        JPanel centro = new JPanel(new GridBagLayout());
        centro.setOpaque(false);
        centro.add(panelLineaOperativo);
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
        JPanel panelBotones = new JPanel(new GridLayout(1, 2, 4, 0));
        panelBotones.setOpaque(false);
        panelBotones.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        botonLimpiar.setText("🧹");
        botonLimpiar.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
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
        botonConfiguracion.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
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

        Rectangle franjaBotones = obtenerFranjaObjetivoBotones();
        int ladoBoton = TAMANIO_FIJO_BOTON;

        Dimension tamano = new Dimension(ladoBoton, ladoBoton);
        aplicarDimensionBotonCuadrado(botonLimpiar, tamano);
        aplicarDimensionBotonCuadrado(botonConfiguracion, tamano);
        actualizarTamanoIcono(ladoBoton);

        if (panelLateral != null) {
            int offsetSuperior = Math.max(0, franjaBotones.y);
            panelLateral.setBorder(BorderFactory.createEmptyBorder(offsetSuperior, 0, 0, 0));
        }

        if (panelBotones != null) {
            panelBotones.revalidate();
            panelBotones.repaint();
        }
        if (panelLateral != null) {
            panelLateral.revalidate();
            panelLateral.repaint();
        }
    }

    private void aplicarDimensionBotonCuadrado(JButton boton, Dimension tamano) {
        boton.setPreferredSize(tamano);
        boton.setMinimumSize(tamano);
        boton.setMaximumSize(tamano);
    }

    private Rectangle obtenerFranjaObjetivoBotones() {
        JPanel seccionReferencia = obtenerSeccionReferencia();
        if (seccionReferencia == null) {
            return new Rectangle(0, 0, 0, 0);
        }

        int alturaTotal = seccionReferencia.getHeight() > 0
            ? seccionReferencia.getHeight()
            : seccionReferencia.getPreferredSize().height;
        Insets insets = seccionReferencia.getInsets();
        int yLocalContenido = insets.top;
        int alturaContenido = Math.max(0, alturaTotal - insets.top - insets.bottom);

        Point origenEnPanel = SwingUtilities.convertPoint(seccionReferencia, 0, yLocalContenido, this);
        return new Rectangle(origenEnPanel.x, origenEnPanel.y, 0, alturaContenido);
    }

    private JPanel obtenerSeccionReferencia() {
        if (panelHallazgos != null && panelHallazgos.getParent() != null) {
            return panelHallazgos;
        }
        if (panelOperativo != null && panelOperativo.getParent() != null) {
            return panelOperativo;
        }
        return null;
    }

    private void actualizarTamanoIcono(int ladoBoton) {
        int tamanioFuente = Math.max(18, (int) Math.round(ladoBoton * 0.45));
        Font fuenteIcono = new Font(Font.SANS_SERIF, Font.PLAIN, tamanioFuente);
        botonLimpiar.setFont(fuenteIcono);
        botonConfiguracion.setFont(fuenteIcono);
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
            ajustarDimensionBotones();
        });
    }

    public Estadisticas obtenerEstadisticas() {
        return estadisticas;
    }

    public void destruir() {
        timerActualizacion.stop();
    }
}
