package com.burpia.ui;

import com.burpia.model.Estadisticas;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;
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

    private JPanel panelContenidoCentral;
    private JPanel panelHallazgos;
    private JPanel panelOperativo;
    private JPanel panelBotones;
    private JPanel panelLateral;
    private JPanel contenedorBotonesLateral;
    private GridBagConstraints restriccionesBotonesLateral;
    private JPanel panelLineaHallazgos;
    private JPanel panelLineaOperativo;

    private static final int UMBRAL_RESPONSIVE = 900;
    private static final int TAMANIO_FIJO_BOTON = 30;
    private static final int AJUSTE_Y_BOTONES = -10;
    private static final int ESPACIADO_BOTONES = 4;

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
        for (ActionListener listener : botonConfiguracion.getActionListeners()) {
            botonConfiguracion.removeActionListener(listener);
        }
        if (manejador != null) {
            botonConfiguracion.addActionListener(e -> manejador.run());
        }
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
                actualizarLayoutBotonesResponsive(getWidth());
                ajustarDimensionBotones();
            }
        });

        panelBotones = crearPanelBotones();
        panelLateral = new JPanel(new GridBagLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension preferido = super.getPreferredSize();
                return new Dimension(preferido.width, 0);
            }

            @Override
            public Dimension getMinimumSize() {
                Dimension minimo = super.getMinimumSize();
                return new Dimension(minimo.width, 0);
            }
        };
        panelLateral.setOpaque(false);
        contenedorBotonesLateral = new JPanel(new BorderLayout());
        contenedorBotonesLateral.setOpaque(false);
        contenedorBotonesLateral.add(panelBotones, BorderLayout.NORTH);

        restriccionesBotonesLateral = new GridBagConstraints();
        restriccionesBotonesLateral.gridx = 0;
        restriccionesBotonesLateral.gridy = 0;
        restriccionesBotonesLateral.weightx = 0;
        restriccionesBotonesLateral.weighty = 1;
        restriccionesBotonesLateral.anchor = GridBagConstraints.FIRST_LINE_START;
        restriccionesBotonesLateral.fill = GridBagConstraints.NONE;
        restriccionesBotonesLateral.insets = new Insets(0, 0, 0, 0);
        panelLateral.add(contenedorBotonesLateral, restriccionesBotonesLateral);

        add(panelContenidoCentral, BorderLayout.CENTER);
        add(panelLateral, BorderLayout.EAST);

        timerActualizacion = new Timer(1000, e -> actualizar());
        timerActualizacion.start();
        actualizar();
        SwingUtilities.invokeLater(() -> {
            actualizarLayoutBotonesResponsive(getWidth());
            ajustarDimensionBotones();
        });
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

        panel.add(panelLineaHallazgos, BorderLayout.NORTH);

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

        panel.add(panelLineaOperativo, BorderLayout.NORTH);

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
        JPanel panelBotones = new JPanel();
        panelBotones.setLayout(new GridLayout(1, 2, ESPACIADO_BOTONES, 0));
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

    private void actualizarLayoutBotonesResponsive(int anchoContenedor) {
        if (panelBotones == null) {
            return;
        }

        boolean layoutVertical = anchoContenedor < UMBRAL_RESPONSIVE;
        if (layoutVertical) {
            aplicarLayoutBotonesVerticalAlineado();
        } else {
            aplicarLayoutBotonesHorizontal();
        }
    }

    private void aplicarLayoutBotonesHorizontal() {
        if (panelBotones.getLayout() instanceof GridLayout) {
            GridLayout grid = (GridLayout) panelBotones.getLayout();
            if (grid.getRows() == 1 && grid.getColumns() == 2) {
                return;
            }
        }

        panelBotones.removeAll();
        panelBotones.setLayout(new GridLayout(1, 2, ESPACIADO_BOTONES, 0));
        panelBotones.add(botonLimpiar);
        panelBotones.add(botonConfiguracion);
        panelBotones.revalidate();
        panelBotones.repaint();
    }

    private void aplicarLayoutBotonesVerticalAlineado() {
        if (panelBotones.getLayout() instanceof GridBagLayout) {
            return;
        }

        panelBotones.removeAll();
        panelBotones.setLayout(new GridBagLayout());

        GridBagConstraints filaSuperior = new GridBagConstraints();
        filaSuperior.gridx = 0;
        filaSuperior.gridy = 0;
        filaSuperior.anchor = GridBagConstraints.FIRST_LINE_START;
        filaSuperior.fill = GridBagConstraints.NONE;
        filaSuperior.weightx = 0;
        filaSuperior.weighty = 0;
        filaSuperior.insets = new Insets(0, 0, ESPACIADO_BOTONES, 0);
        panelBotones.add(botonLimpiar, filaSuperior);

        GridBagConstraints filaInferior = new GridBagConstraints();
        filaInferior.gridx = 0;
        filaInferior.gridy = 1;
        filaInferior.anchor = GridBagConstraints.FIRST_LINE_START;
        filaInferior.fill = GridBagConstraints.NONE;
        filaInferior.weightx = 0;
        filaInferior.weighty = 0;
        filaInferior.insets = new Insets(0, 0, 0, 0);
        panelBotones.add(botonConfiguracion, filaInferior);

        panelBotones.revalidate();
        panelBotones.repaint();
    }

    private void ajustarDimensionBotones() {
        if (botonLimpiar == null || botonConfiguracion == null) {
            return;
        }

        int ladoBoton = TAMANIO_FIJO_BOTON;

        Dimension tamano = new Dimension(ladoBoton, ladoBoton);
        aplicarDimensionBotonCuadrado(botonLimpiar, tamano);
        aplicarDimensionBotonCuadrado(botonConfiguracion, tamano);
        actualizarTamanoIcono(ladoBoton);

        if (panelLateral != null && contenedorBotonesLateral != null && restriccionesBotonesLateral != null) {
            int anchoContenedor = getWidth();
            boolean layoutVertical = anchoContenedor < UMBRAL_RESPONSIVE;
            int offsetSuperior = calcularOffsetSuperiorBotones(obtenerFranjaObjetivoBotones(), ladoBoton);

            if (layoutVertical) {
                int offsetInferior = calcularOffsetSuperiorBotones(
                    obtenerFranjaObjetivoBotonesDesde(panelLineaOperativo),
                    ladoBoton
                );
                restriccionesBotonesLateral.insets = new Insets(offsetSuperior, 0, 0, 0);
                ajustarSeparacionVerticalBotones(offsetInferior - offsetSuperior, ladoBoton);
            } else {
                restriccionesBotonesLateral.insets = new Insets(offsetSuperior, 0, 0, 0);
            }

            GridBagLayout layoutLateral = (GridBagLayout) panelLateral.getLayout();
            layoutLateral.setConstraints(contenedorBotonesLateral, restriccionesBotonesLateral);
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
        return obtenerFranjaObjetivoBotonesDesde(panelLineaHallazgos != null ? panelLineaHallazgos : panelLineaOperativo);
    }

    private Rectangle obtenerFranjaObjetivoBotonesDesde(JPanel lineaReferencia) {
        if (lineaReferencia == null || lineaReferencia.getParent() == null || panelLateral == null) {
            return new Rectangle(0, 0, 0, 0);
        }
        Point origenEnPanel = SwingUtilities.convertPoint(
            lineaReferencia.getParent(),
            lineaReferencia.getLocation(),
            panelLateral
        );
        return new Rectangle(origenEnPanel.x, origenEnPanel.y, lineaReferencia.getWidth(), lineaReferencia.getHeight());
    }

    private int calcularOffsetSuperiorBotones(Rectangle franjaBotones, int ladoBoton) {
        int offsetSuperior = franjaBotones.y + AJUSTE_Y_BOTONES;
        return Math.max(0, offsetSuperior);
    }

    private void ajustarSeparacionVerticalBotones(int diferenciaY, int ladoBoton) {
        if (!(panelBotones.getLayout() instanceof GridBagLayout) || panelBotones.getComponentCount() < 2) {
            return;
        }

        int separacion = Math.max(ESPACIADO_BOTONES, diferenciaY - ladoBoton);
        GridBagLayout layoutBotones = (GridBagLayout) panelBotones.getLayout();
        GridBagConstraints superior = layoutBotones.getConstraints(botonLimpiar);
        superior.insets = new Insets(0, 0, separacion, 0);
        layoutBotones.setConstraints(botonLimpiar, superior);
    }

    private void actualizarTamanoIcono(int ladoBoton) {
        int tamanioFuente = Math.max(18, (int) Math.round(ladoBoton * 0.45));
        Font fuenteIcono = new Font(Font.SANS_SERIF, Font.PLAIN, tamanioFuente);
        botonLimpiar.setFont(fuenteIcono);
        botonConfiguracion.setFont(fuenteIcono);
    }

    public void actualizar() {
        SwingUtilities.invokeLater(() -> {
            actualizarLayoutBotonesResponsive(getWidth());

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
