package com.burpia.ui;

import com.burpia.i18n.I18nUI;
import com.burpia.model.Estadisticas;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
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
    private final JButton botonCaptura;
    private Runnable manejadorToggleCaptura;
    private volatile boolean capturaActiva = true;

    private JPanel panelContenidoCentral;
    private JPanel panelHallazgos;
    private JPanel panelOperativo;
    private JPanel panelLateral;
    private JPanel panelLineaHallazgos;
    private JPanel panelLineaOperativo;

    private static final int UMBRAL_RESPONSIVE = 900;
    private static final int TAMANIO_FIJO_BOTON = 60;
    private static final int ESPACIADO_BOTONES = 8;
    private static final int AJUSTE_Y_BOTONES = -3;

    public PanelEstadisticas(Estadisticas estadisticas, IntSupplier proveedorLimiteHallazgos) {
        this.estadisticas = estadisticas;
        this.proveedorLimiteHallazgos = proveedorLimiteHallazgos != null ? proveedorLimiteHallazgos : () -> 1000;
        this.etiquetaResumenPrincipal = new JLabel();
        this.etiquetaResumenSeveridad = new JLabel();
        this.etiquetaLimiteHallazgos = new JLabel();
        this.etiquetaResumenOperativo = new JLabel();
        this.botonConfiguracion = new JButton();
        this.botonCaptura = new JButton();
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
        setLayout(new BorderLayout(12, 6));
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        panelHallazgos = crearPanelHallazgos();
        panelOperativo = crearPanelOperativo();

        panelContenidoCentral = new JPanel(new GridLayout(1, 2, 8, 0)) {
            private boolean ultimoLayoutHorizontal = true;

            @Override
            public void doLayout() {
                int ancho = getWidth() - calcularAnchoLateralPreferido();
                boolean esLayoutHorizontal = ancho >= UMBRAL_RESPONSIVE;

                if (esLayoutHorizontal != ultimoLayoutHorizontal) {
                    if (esLayoutHorizontal) {
                        setLayout(new GridLayout(1, 2, 12, 0));
                    } else {
                        setLayout(new GridLayout(2, 1, 0, 10));
                    }
                    ultimoLayoutHorizontal = esLayoutHorizontal;
                }

                super.doLayout();
            }
        };

        panelContenidoCentral.add(panelHallazgos);
        panelContenidoCentral.add(panelOperativo);

        configurarBotones();

        panelLateral = new JPanel(null) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(calcularAnchoLateralPreferido(), 0);
            }

            @Override
            public Dimension getMinimumSize() {
                return new Dimension(calcularAnchoLateralPreferido(), 0);
            }
        };
        panelLateral.setOpaque(false);
        panelLateral.add(botonCaptura);
        panelLateral.add(botonConfiguracion);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                panelContenidoCentral.revalidate();
                panelContenidoCentral.repaint();
                ajustarDimensionBotones();
            }
        });

        add(panelContenidoCentral, BorderLayout.CENTER);
        add(panelLateral, BorderLayout.EAST);

        timerActualizacion = new Timer(1000, e -> actualizar());
        timerActualizacion.start();
        aplicarIdioma();
        actualizar();
        SwingUtilities.invokeLater(this::ajustarDimensionBotones);
    }

    private JPanel crearPanelHallazgos() {
        JPanel panel = crearPanelSeccion(I18nUI.Estadisticas.TITULO_HALLAZGOS());

        panelLineaHallazgos = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        panelLineaHallazgos.setOpaque(false);

        etiquetaResumenPrincipal.setFont(EstilosUI.FUENTE_MONO_NEGRITA);
        etiquetaResumenPrincipal.setForeground(new Color(0, 102, 204));
        etiquetaResumenPrincipal.setToolTipText(TooltipsUI.Estadisticas.RESUMEN_TOTAL());
        panelLineaHallazgos.add(etiquetaResumenPrincipal);

        JLabel separador = new JLabel(" | ");
        separador.setFont(EstilosUI.FUENTE_MONO_NEGRITA);
        separador.setForeground(new Color(140, 140, 140));
        panelLineaHallazgos.add(separador);

        etiquetaResumenSeveridad.setFont(EstilosUI.FUENTE_MONO);
        etiquetaResumenSeveridad.setForeground(new Color(70, 70, 70));
        etiquetaResumenSeveridad.setToolTipText(TooltipsUI.Estadisticas.RESUMEN_SEVERIDAD());
        panelLineaHallazgos.add(etiquetaResumenSeveridad);

        JLabel separador2 = new JLabel(" | ");
        separador2.setFont(EstilosUI.FUENTE_MONO_NEGRITA);
        separador2.setForeground(new Color(140, 140, 140));
        panelLineaHallazgos.add(separador2);

        etiquetaLimiteHallazgos.setFont(EstilosUI.FUENTE_MONO);
        etiquetaLimiteHallazgos.setForeground(new Color(80, 80, 80));
        etiquetaLimiteHallazgos.setToolTipText(TooltipsUI.Estadisticas.LIMITE_HALLAZGOS());
        panelLineaHallazgos.add(etiquetaLimiteHallazgos);

        panel.add(panelLineaHallazgos, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelOperativo() {
        JPanel panel = crearPanelSeccion(I18nUI.Estadisticas.TITULO_OPERATIVO());

        etiquetaResumenOperativo.setFont(EstilosUI.FUENTE_MONO);
        etiquetaResumenOperativo.setForeground(new Color(90, 90, 90));
        etiquetaResumenOperativo.setToolTipText(TooltipsUI.Estadisticas.RESUMEN_OPERATIVO());

        panelLineaOperativo = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        panelLineaOperativo.setOpaque(false);
        panelLineaOperativo.add(etiquetaResumenOperativo);

        panel.add(panelLineaOperativo, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelSeccion(String titulo) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
                titulo,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        return panel;
    }

    private void configurarBotones() {
        botonCaptura.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        botonCaptura.setFocusable(false);
        botonCaptura.setMargin(new Insets(0, 0, 0, 0));
        botonCaptura.putClientProperty("JButton.buttonType", "square");
        botonCaptura.addActionListener(e -> {
            if (manejadorToggleCaptura != null) {
                manejadorToggleCaptura.run();
            }
        });
        actualizarEstadoCapturaUI();

        botonConfiguracion.setText("⚙️");
        botonConfiguracion.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        botonConfiguracion.setToolTipText(TooltipsUI.Estadisticas.CONFIGURACION());
        botonConfiguracion.setFocusable(false);
        botonConfiguracion.setMargin(new Insets(0, 0, 0, 0));
        botonConfiguracion.putClientProperty("JButton.buttonType", "square");
    }

    private void ajustarDimensionBotones() {
        if (panelLateral == null) {
            return;
        }

        boolean layoutVertical = getWidth() < UMBRAL_RESPONSIVE;
        int ladoBoton = TAMANIO_FIJO_BOTON;

        Dimension tamano = new Dimension(ladoBoton, ladoBoton);
        aplicarDimensionBotonCuadrado(botonCaptura, tamano);
        aplicarDimensionBotonCuadrado(botonConfiguracion, tamano);
        actualizarTamanoIcono(ladoBoton);

        Rectangle franjaHallazgos = obtenerFranjaObjetivoBotonesDesde(panelLineaHallazgos);
        Rectangle franjaOperativo = obtenerFranjaObjetivoBotonesDesde(panelLineaOperativo);

        int yHallazgos = calcularOffsetSuperiorBotones(franjaHallazgos);
        if (layoutVertical) {
            int yOperativo = calcularOffsetSuperiorBotones(franjaOperativo);
            botonCaptura.setBounds(0, yHallazgos, ladoBoton, ladoBoton);
            botonConfiguracion.setBounds(0, yOperativo, ladoBoton, ladoBoton);
        } else {
            botonCaptura.setBounds(0, yHallazgos, ladoBoton, ladoBoton);
            botonConfiguracion.setBounds(ladoBoton + ESPACIADO_BOTONES, yHallazgos, ladoBoton, ladoBoton);
        }

        panelLateral.revalidate();
        panelLateral.repaint();
    }

    private Rectangle obtenerFranjaObjetivoBotonesDesde(JPanel lineaReferencia) {
        if (lineaReferencia == null || lineaReferencia.getParent() == null || panelLateral == null) {
            return new Rectangle(0, 0, 0, 0);
        }
        Point origenEnPanelLateral = SwingUtilities.convertPoint(
            lineaReferencia.getParent(),
            lineaReferencia.getLocation(),
            panelLateral
        );
        return new Rectangle(origenEnPanelLateral.x, origenEnPanelLateral.y, lineaReferencia.getWidth(), lineaReferencia.getHeight());
    }

    private int calcularOffsetSuperiorBotones(Rectangle franjaBotones) {
        return Math.max(0, franjaBotones.y + (franjaBotones.height - TAMANIO_FIJO_BOTON) / 2 + AJUSTE_Y_BOTONES);
    }

    private int calcularAnchoLateralPreferido() {
        boolean layoutVertical = getWidth() < UMBRAL_RESPONSIVE;
        if (layoutVertical) {
            return TAMANIO_FIJO_BOTON;
        }
        return (TAMANIO_FIJO_BOTON * 2) + ESPACIADO_BOTONES;
    }

    private void aplicarDimensionBotonCuadrado(JButton boton, Dimension tamano) {
        boton.setPreferredSize(tamano);
        boton.setMinimumSize(tamano);
        boton.setMaximumSize(tamano);
    }

    private void actualizarTamanoIcono(int ladoBoton) {
        int tamanioFuente = Math.max(18, (int) Math.round(ladoBoton * 0.45));
        Font fuenteIcono = new Font(Font.SANS_SERIF, Font.PLAIN, tamanioFuente);
        botonCaptura.setFont(fuenteIcono);
        botonConfiguracion.setFont(fuenteIcono);
    }

    public void actualizar() {
        SwingUtilities.invokeLater(() -> {
            etiquetaResumenPrincipal.setText(I18nUI.Estadisticas.RESUMEN_TOTAL(estadisticas.obtenerHallazgosCreados()));

            etiquetaResumenSeveridad.setText(I18nUI.Estadisticas.RESUMEN_SEVERIDAD(
                    estadisticas.obtenerHallazgosCritical(),
                    estadisticas.obtenerHallazgosHigh(),
                    estadisticas.obtenerHallazgosMedium(),
                    estadisticas.obtenerHallazgosLow(),
                    estadisticas.obtenerHallazgosInfo()
            ));

            etiquetaLimiteHallazgos.setText(I18nUI.Estadisticas.LIMITE_HALLAZGOS(proveedorLimiteHallazgos.getAsInt()));

            etiquetaResumenOperativo.setText(I18nUI.Estadisticas.RESUMEN_OPERATIVO(
                    estadisticas.obtenerTotalSolicitudes(),
                    estadisticas.obtenerAnalizados(),
                    estadisticas.obtenerTotalOmitidos(),
                    estadisticas.obtenerErrores()
            ));

            ajustarDimensionBotones();
        });
    }

    public void establecerManejadorToggleCaptura(Runnable manejador) {
        this.manejadorToggleCaptura = manejador;
    }

    public void establecerEstadoCaptura(boolean activa) {
        this.capturaActiva = activa;
        SwingUtilities.invokeLater(() -> {
            actualizarEstadoCapturaUI();
            actualizar();
        });
    }

    private void actualizarEstadoCapturaUI() {
        if (capturaActiva) {
            botonCaptura.setText("⏸️");
            botonCaptura.setToolTipText(TooltipsUI.Estadisticas.CAPTURA_PAUSAR());
        } else {
            botonCaptura.setText("▶️");
            botonCaptura.setToolTipText(TooltipsUI.Estadisticas.CAPTURA_REANUDAR());
        }
    }

    public void aplicarIdioma() {
        actualizarTituloSeccion(panelHallazgos, I18nUI.Estadisticas.TITULO_HALLAZGOS());
        actualizarTituloSeccion(panelOperativo, I18nUI.Estadisticas.TITULO_OPERATIVO());
        etiquetaResumenPrincipal.setToolTipText(TooltipsUI.Estadisticas.RESUMEN_TOTAL());
        etiquetaResumenSeveridad.setToolTipText(TooltipsUI.Estadisticas.RESUMEN_SEVERIDAD());
        etiquetaLimiteHallazgos.setToolTipText(TooltipsUI.Estadisticas.LIMITE_HALLAZGOS());
        etiquetaResumenOperativo.setToolTipText(TooltipsUI.Estadisticas.RESUMEN_OPERATIVO());
        botonConfiguracion.setToolTipText(TooltipsUI.Estadisticas.CONFIGURACION());
        actualizarEstadoCapturaUI();
        revalidate();
        repaint();
    }

    private void actualizarTituloSeccion(JPanel panel, String titulo) {
        if (panel == null) {
            return;
        }
        Border borde = panel.getBorder();
        if (!(borde instanceof CompoundBorder)) {
            return;
        }
        Border bordeExterno = ((CompoundBorder) borde).getOutsideBorder();
        if (bordeExterno instanceof TitledBorder) {
            ((TitledBorder) bordeExterno).setTitle(titulo);
        }
    }

    public void destruir() {
        timerActualizacion.stop();
    }
}
