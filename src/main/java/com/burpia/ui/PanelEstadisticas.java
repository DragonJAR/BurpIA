package com.burpia.ui;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Estadisticas;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.function.IntSupplier;

import static com.burpia.ui.UIUtils.ejecutarEnEdt;

public class PanelEstadisticas extends JPanel {
    private final JLabel etiquetaResumenPrincipal;
    private final JLabel etiquetaResumenSeveridad;
    private final JLabel etiquetaLimiteHallazgos;
    private final JLabel etiquetaResumenOperativo;
    private final Estadisticas estadisticas;
    private final IntSupplier proveedorLimiteHallazgos;
    private final java.util.function.Supplier<int[]> proveedorEstadisticasVisibles;
    private Timer timerActualizacion;
    private final JButton botonConfiguracion;
    private final JButton botonCaptura;
    private Runnable manejadorToggleCaptura;
    private volatile boolean capturaActiva = true;
    private volatile int ultimaVersionEstadisticas = -1;

    private JPanel panelContenidoCentral;
    private JPanel panelHallazgos;
    private JPanel panelOperativo;
    private JPanel panelLateral;
    private JPanel panelLineaHallazgos;
    private JPanel panelLineaOperativo;
    private JLabel etiquetaSeparadorHallazgos;
    private JLabel etiquetaSeparadorLimite;

    private static final int UMBRAL_RESPONSIVE = 900;
    private static final int TAMANIO_FIJO_BOTON = 60;
    private static final int ESPACIADO_BOTONES = 8;
    private static final int AJUSTE_Y_BOTONES = -3;

    @SuppressWarnings("this-escape")
    public PanelEstadisticas(Estadisticas estadisticas,
                             IntSupplier proveedorLimiteHallazgos,
                             PanelHallazgos panelHallazgos) {
        this.estadisticas = estadisticas;
        this.proveedorLimiteHallazgos = proveedorLimiteHallazgos != null ? proveedorLimiteHallazgos : () -> ConfiguracionAPI.MAXIMO_HALLAZGOS_TABLA_DEFECTO;
        this.proveedorEstadisticasVisibles = () -> panelHallazgos != null ? panelHallazgos.obtenerEstadisticasVisibles() : new int[6];
        this.etiquetaResumenPrincipal = new JLabel();
        this.etiquetaResumenSeveridad = new JLabel();
        this.etiquetaLimiteHallazgos = new JLabel();
        this.etiquetaResumenOperativo = new JLabel();
        this.botonConfiguracion = new JButton();
        this.botonCaptura = new JButton();

        // Suscribirse a cambios en el modelo de hallazgos
        if (panelHallazgos != null) {
            ModeloTablaHallazgos modelo = panelHallazgos.obtenerModelo();
            if (modelo != null) {
                modelo.agregarEscucha(() -> actualizar(true));
            }
        }

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
        aplicarTema();
        actualizar();
        ejecutarEnEdt(this::ajustarDimensionBotones);
    }

    private JPanel crearPanelHallazgos() {
        JPanel panel = crearPanelSeccion(I18nUI.Estadisticas.TITULO_HALLAZGOS());

        panelLineaHallazgos = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        panelLineaHallazgos.setOpaque(false);

        etiquetaResumenPrincipal.setFont(EstilosUI.FUENTE_MONO_NEGRITA);
        etiquetaResumenPrincipal.setToolTipText(I18nUI.Tooltips.Estadisticas.RESUMEN_TOTAL());
        panelLineaHallazgos.add(etiquetaResumenPrincipal);

        etiquetaSeparadorHallazgos = new JLabel(I18nUI.General.SEPARADOR_ESTADISTICAS());
        etiquetaSeparadorHallazgos.setFont(EstilosUI.FUENTE_MONO_NEGRITA);
        panelLineaHallazgos.add(etiquetaSeparadorHallazgos);

        etiquetaResumenSeveridad.setFont(EstilosUI.FUENTE_MONO);
        etiquetaResumenSeveridad.setToolTipText(I18nUI.Tooltips.Estadisticas.RESUMEN_SEVERIDAD());
        panelLineaHallazgos.add(etiquetaResumenSeveridad);

        etiquetaSeparadorLimite = new JLabel(I18nUI.General.SEPARADOR_ESTADISTICAS());
        etiquetaSeparadorLimite.setFont(EstilosUI.FUENTE_MONO_NEGRITA);
        panelLineaHallazgos.add(etiquetaSeparadorLimite);

        etiquetaLimiteHallazgos.setFont(EstilosUI.FUENTE_MONO);
        etiquetaLimiteHallazgos.setToolTipText(I18nUI.Tooltips.Estadisticas.LIMITE_HALLAZGOS());
        panelLineaHallazgos.add(etiquetaLimiteHallazgos);

        panel.add(panelLineaHallazgos, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelOperativo() {
        JPanel panel = crearPanelSeccion(I18nUI.Estadisticas.TITULO_OPERATIVO());

        etiquetaResumenOperativo.setFont(EstilosUI.FUENTE_MONO);
        etiquetaResumenOperativo.setToolTipText(I18nUI.Tooltips.Estadisticas.RESUMEN_OPERATIVO());

        panelLineaOperativo = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        panelLineaOperativo.setOpaque(false);
        panelLineaOperativo.add(etiquetaResumenOperativo);

        panel.add(panelLineaOperativo, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelSeccion(String titulo) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(UIUtils.crearBordeTitulado(titulo, 12, 16));
        return panel;
    }

    private void configurarBotones() {
        botonCaptura.setFont(EstilosUI.FUENTE_ICONO_GRANDE);
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
        botonConfiguracion.setFont(EstilosUI.FUENTE_ICONO_GRANDE);
        botonConfiguracion.setToolTipText(I18nUI.Tooltips.Estadisticas.CONFIGURACION());
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
                panelLateral);
        return new Rectangle(origenEnPanelLateral.x, origenEnPanelLateral.y, lineaReferencia.getWidth(),
                lineaReferencia.getHeight());
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
        Font fuenteIcono = EstilosUI.FUENTE_ICONO_GRANDE.deriveFont((float) tamanioFuente);
        botonCaptura.setFont(fuenteIcono);
        botonConfiguracion.setFont(fuenteIcono);
    }

    public void actualizar() {
        actualizar(false);
    }

    private void actualizar(boolean forzar) {
        int versionActual = estadisticas.obtenerVersion();
        if (!forzar && versionActual == ultimaVersionEstadisticas) {
            return;
        }
        ultimaVersionEstadisticas = versionActual;
        Runnable actualizarUi = () -> {
            int[] statsVisibles = proveedorEstadisticasVisibles.get();
            if (statsVisibles == null) {
                statsVisibles = new int[6];
            }

            etiquetaResumenPrincipal.setText(
                I18nUI.Estadisticas.RESUMEN_TOTAL(statsVisibles[0])
            );

            etiquetaResumenSeveridad.setText(I18nUI.Estadisticas.RESUMEN_SEVERIDAD(
                    statsVisibles[1],
                    statsVisibles[2],
                    statsVisibles[3],
                    statsVisibles[4],
                    statsVisibles[5]));

            etiquetaLimiteHallazgos.setText(I18nUI.Estadisticas.LIMITE_HALLAZGOS(proveedorLimiteHallazgos.getAsInt()));

            etiquetaResumenOperativo.setText(I18nUI.Estadisticas.RESUMEN_OPERATIVO(
                    estadisticas.obtenerTotalSolicitudes(),
                    estadisticas.obtenerAnalizados(),
                    estadisticas.obtenerTotalOmitidos(),
                    estadisticas.obtenerErrores()));
        };
        ejecutarEnEdt(actualizarUi);
    }

    public void establecerManejadorToggleCaptura(Runnable manejador) {
        this.manejadorToggleCaptura = manejador;
    }

    public void establecerEstadoCaptura(boolean activa) {
        this.capturaActiva = activa;
        ejecutarEnEdt(() -> {
            actualizarEstadoCapturaUI();
            actualizar();
        });
    }

    private void actualizarEstadoCapturaUI() {
        if (capturaActiva) {
            botonCaptura.setText("⏸️");
            botonCaptura.setToolTipText(I18nUI.Tooltips.Estadisticas.CAPTURA_PAUSAR());
        } else {
            botonCaptura.setText("▶️");
            botonCaptura.setToolTipText(I18nUI.Tooltips.Estadisticas.CAPTURA_REANUDAR());
        }
    }

    public void aplicarIdioma() {
        actualizarTituloSeccion(panelHallazgos, I18nUI.Estadisticas.TITULO_HALLAZGOS());
        actualizarTituloSeccion(panelOperativo, I18nUI.Estadisticas.TITULO_OPERATIVO());
        etiquetaResumenPrincipal.setToolTipText(I18nUI.Tooltips.Estadisticas.RESUMEN_TOTAL());
        etiquetaResumenSeveridad.setToolTipText(I18nUI.Tooltips.Estadisticas.RESUMEN_SEVERIDAD());
        etiquetaLimiteHallazgos.setToolTipText(I18nUI.Tooltips.Estadisticas.LIMITE_HALLAZGOS());
        etiquetaResumenOperativo.setToolTipText(I18nUI.Tooltips.Estadisticas.RESUMEN_OPERATIVO());
        botonConfiguracion.setToolTipText(I18nUI.Tooltips.Estadisticas.CONFIGURACION());
        actualizarEstadoCapturaUI();
        aplicarTema();
        actualizar(true);
        revalidate();
        repaint();
    }

    public void aplicarTema() {
        Runnable aplicar = () -> {
            Color fondoPanel = EstilosUI.obtenerFondoPanel();

            setBackground(fondoPanel);
            if (panelHallazgos != null) {
                panelHallazgos.setBorder(UIUtils.crearBordeTitulado(I18nUI.Estadisticas.TITULO_HALLAZGOS(), 12, 16));
            }
            if (panelOperativo != null) {
                panelOperativo.setBorder(UIUtils.crearBordeTitulado(I18nUI.Estadisticas.TITULO_OPERATIVO(), 12, 16));
            }

            Color colorPrincipal = EstilosUI.colorEnlaceAccesible(fondoPanel);
            Color colorSecundario = EstilosUI.colorTextoSecundario(fondoPanel);
            Color colorSeparador = EstilosUI.colorSeparador(fondoPanel);

            etiquetaResumenPrincipal.setForeground(colorPrincipal);
            etiquetaResumenSeveridad.setForeground(colorSecundario);
            etiquetaLimiteHallazgos.setForeground(colorSecundario);
            etiquetaResumenOperativo.setForeground(colorSecundario);

            if (etiquetaSeparadorHallazgos != null) {
                etiquetaSeparadorHallazgos.setForeground(colorSeparador);
            }
            if (etiquetaSeparadorLimite != null) {
                etiquetaSeparadorLimite.setForeground(colorSeparador);
            }

            repaint();
        };

        ejecutarEnEdt(aplicar);
    }

    private void actualizarTituloSeccion(JPanel panel, String titulo) {
        if (panel != null) {
            UIUtils.actualizarTituloPanel(panel, titulo);
        }
    }

    public void destruir() {
        if (timerActualizacion != null) {
            timerActualizacion.stop();
            timerActualizacion = null;
        }
    }
}
