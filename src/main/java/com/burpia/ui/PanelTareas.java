package com.burpia.ui;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nLogs;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Tarea;
import com.burpia.util.ContadorEstadosTareas;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.GestorTareas;
import com.burpia.util.Normalizador;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.burpia.ui.UIUtils.ejecutarEnEdt;

public class PanelTareas extends JPanel {
    private static final String ORIGEN_LOG = "PanelTareas";
    private static final GestorLoggingUnificado GESTOR_LOGGING =
        GestorLoggingUnificado.crearMinimal(null, null);

    private final ModeloTablaTareas modelo;
    private final JTable tabla;
    private JButton botonPausarReanudar;
    private JButton botonCancelar;
    private JButton botonLimpiarCompletadas;
    private JLabel etiquetaEstadisticas;
    private final GestorTareas gestorTareas;
    private Timer timerActualizacion;
    private JPanel panelControles;
    private JPanel panelTablaWrapper;
    private Function<String, Boolean> manejadorReintento;
    private volatile int ultimaVersionTareas = -1;

    private static final int COLUMNA_ID = 0;
    private static final int COLUMNA_URL = 1;
    private static final int COLUMNA_ESTADO = 2;
    private static final int COLUMNA_DURACION = 3;
    private static final int NUMERO_COLUMNAS = 4;

    private static final int ANCHO_COLUMNA_ID = 120;
    private static final int ANCHO_COLUMNA_URL = 400;
    private static final int ANCHO_COLUMNA_ESTADO = 130;
    private static final int ANCHO_COLUMNA_DURACION = 100;

    private static final int INTERVALO_ACTUALIZACION_MS = 1000;
    private static final int UMBRAL_RESPONSIVE = 800;

    @SuppressWarnings("this-escape")
    public PanelTareas(GestorTareas gestorTareas, ModeloTablaTareas modelo) {
        this.modelo = modelo;
        this.tabla = new JTable(modelo);
        this.gestorTareas = gestorTareas;
        this.manejadorReintento = null;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panelControles = new JPanel();
        panelControles.setLayout(new BoxLayout(panelControles, BoxLayout.Y_AXIS));
        panelControles.setBorder(UIUtils.crearBordeTitulado(
                I18nUI.Tareas.TITULO_CONTROLES(), 12, 16));

        JPanel panelTodosControles = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5)) {
            private boolean ultimoLayoutHorizontal = true;

            @Override
            public void doLayout() {
                int ancho = getWidth() - 40;
                boolean esLayoutHorizontal = ancho >= UMBRAL_RESPONSIVE;

                if (esLayoutHorizontal != ultimoLayoutHorizontal) {
                    if (esLayoutHorizontal) {
                        setLayout(new FlowLayout(FlowLayout.LEFT, 12, 4));
                    } else {
                        setLayout(new GridLayout(2, 1, 0, 10));
                    }
                    ultimoLayoutHorizontal = esLayoutHorizontal;
                }

                super.doLayout();
            }
        };

        etiquetaEstadisticas = new JLabel(I18nUI.Tareas.ESTADISTICAS(0, 0, 0));
        etiquetaEstadisticas.setFont(EstilosUI.FUENTE_MONO);
        etiquetaEstadisticas.setToolTipText(I18nUI.Tooltips.Tareas.ESTADISTICAS());
        panelTodosControles.add(etiquetaEstadisticas);

        panelTodosControles.add(Box.createHorizontalStrut(20));

        botonPausarReanudar = new JButton(I18nUI.Tareas.BOTON_PAUSAR_TODO());
        botonPausarReanudar.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonPausarReanudar.setToolTipText(I18nUI.Tooltips.Tareas.PAUSAR_REANUDAR());
        panelTodosControles.add(botonPausarReanudar);

        botonCancelar = new JButton(I18nUI.Tareas.BOTON_CANCELAR_TODO());
        botonCancelar.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonCancelar.setToolTipText(I18nUI.Tooltips.Tareas.CANCELAR());
        panelTodosControles.add(botonCancelar);

        botonLimpiarCompletadas = new JButton(I18nUI.Tareas.BOTON_LIMPIAR());
        botonLimpiarCompletadas.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonLimpiarCompletadas.setToolTipText(I18nUI.Tooltips.Tareas.LIMPIAR());
        panelTodosControles.add(botonLimpiarCompletadas);

        panelControles.add(panelTodosControles);

        panelTablaWrapper = new JPanel(new BorderLayout());
        panelTablaWrapper.setBorder(UIUtils.crearBordeTitulado(
                I18nUI.Tareas.TITULO_LISTA(), 12, 16));

        tabla.setAutoCreateRowSorter(true);
        tabla.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tabla.setRowHeight(EstilosUI.ALTURA_FILA_TABLA);
        tabla.setFont(EstilosUI.FUENTE_TABLA);
        tabla.setToolTipText(I18nUI.Tooltips.Tareas.TABLA());
        configurarColumnasTabla();

        JScrollPane panelDesplazable = new JScrollPane(tabla);
        panelDesplazable.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panelTablaWrapper.add(panelDesplazable, BorderLayout.CENTER);

        botonPausarReanudar.addActionListener(e -> {
            ContadorEstadosTareas estadisticas = obtenerEstadisticasSeguras();
            if (estadisticas.obtenerPausadas() > 0) {
                gestorTareas.reanudarTodasPausadas();
            } else if (estadisticas.obtenerActivasSinPausadas() > 0) {
                gestorTareas.pausarTodasActivas();
            } else {
                UIUtils.mostrarInfo(this, I18nUI.Tareas.TITULO_INFORMACION(),
                        I18nUI.Tareas.INFO_SIN_TAREAS_GESTIONAR());
            }
            actualizarEstadisticas();
        });

        botonCancelar.addActionListener(e -> {
            ContadorEstadosTareas estadisticas = obtenerEstadisticasSeguras();
            int activas = estadisticas.obtenerActivas();

            if (activas == 0) {
                UIUtils.mostrarInfo(this, I18nUI.Tareas.TITULO_INFORMACION(), I18nUI.Tareas.INFO_SIN_TAREAS_CANCELAR());
                return;
            }

            boolean confirmacion = UIUtils.confirmarAdvertencia(
                    this,
                    I18nUI.Tareas.TITULO_CONFIRMAR_CANCELACION(),
                    I18nUI.Tareas.MSG_CONFIRMAR_CANCELAR_TAREAS(activas));
            if (confirmacion) {
                gestorTareas.cancelarTodas();
                actualizarEstadisticas();
            }
        });

        botonLimpiarCompletadas.addActionListener(e -> {
            ContadorEstadosTareas estadisticas = obtenerEstadisticasSeguras();
            int completadas = estadisticas.obtenerFinalizadas();

            if (completadas == 0) {
                UIUtils.mostrarInfo(this, I18nUI.Tareas.TITULO_INFORMACION(), I18nUI.Tareas.INFO_SIN_TAREAS_LIMPIAR());
                return;
            }

            boolean confirmacion = UIUtils.confirmarPregunta(
                    this,
                    I18nUI.Tareas.TITULO_CONFIRMAR_LIMPIEZA(),
                    I18nUI.Tareas.MSG_CONFIRMAR_LIMPIAR_COMPLETADAS(completadas));
            if (confirmacion) {
                gestorTareas.limpiarCompletadas();
                actualizarEstadisticas();
            }
        });

        timerActualizacion = new Timer(INTERVALO_ACTUALIZACION_MS, e -> actualizarEstadisticas());
        timerActualizacion.start();
        aplicarIdioma();

        crearMenuContextual();

        add(panelControles, BorderLayout.NORTH);
        add(panelTablaWrapper, BorderLayout.CENTER);
    }

    private void crearMenuContextual() {
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mostrarMenuContextualSiAplica(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mostrarMenuContextualSiAplica(e);
            }
        });
    }

    private void mostrarMenuContextualSiAplica(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }

        int fila = tabla.rowAtPoint(e.getPoint());
        if (fila < 0) {
            return;
        }
        if (!tabla.isRowSelected(fila)) {
            if (!e.isControlDown()) {
                tabla.setRowSelectionInterval(fila, fila);
            } else {
                tabla.addRowSelectionInterval(fila, fila);
            }
        }
        mostrarMenuContextualDinamico(e.getX(), e.getY());
    }

    /**
     * Crea un JMenuItem con el formato estándar del menú contextual.
     *
     * @param texto   Texto del item de menú
     * @param tooltip Tooltip del item de menú
     * @param accion  ActionListener a ejecutar
     * @return JMenuItem configurado con el estilo estándar
     */
    private JMenuItem crearMenuItemContextual(String texto, String tooltip, java.awt.event.ActionListener accion) {
        JMenuItem menuItem = new JMenuItem(texto);
        menuItem.setFont(EstilosUI.FUENTE_ESTANDAR);
        menuItem.setToolTipText(tooltip);
        menuItem.addActionListener(accion);
        return menuItem;
    }

    /**
     * Actualiza el texto y tooltip del botón Pausar/Reanudar según el estado de
     * tareas.
     *
     * @param tareasPausadas Número de tareas pausadas
     */
    private void actualizarBotonPausarReanudar(int tareasPausadas) {
        if (tareasPausadas > 0) {
            UIUtils.actualizarTextoYTooltip(botonPausarReanudar,
                    I18nUI.Tareas.BOTON_REANUDAR_TODO(),
                    I18nUI.Tooltips.Tareas.REANUDAR_TODO());
        } else {
            UIUtils.actualizarTextoYTooltip(botonPausarReanudar,
                    I18nUI.Tareas.BOTON_PAUSAR_TODO(),
                    I18nUI.Tooltips.Tareas.PAUSAR_TODO());
        }
    }

    private void mostrarMenuContextualDinamico(int x, int y) {
        int[] filas = tabla.getSelectedRows();
        if (filas.length == 0)
            return;

        List<TareaSeleccionada> seleccion = capturarSeleccion(filas);
        if (seleccion.isEmpty()) {
            return;
        }

        JPopupMenu menuContextual = new JPopupMenu();

        if (seleccion.size() == 1) {
            crearMenuUnaTarea(menuContextual, seleccion.get(0));
        } else {
            crearMenuMultipleTareas(menuContextual, seleccion);
        }

        menuContextual.show(tabla, x, y);
    }

    private void crearMenuUnaTarea(JPopupMenu menu, TareaSeleccionada seleccion) {
        String tareaId = seleccion.tareaId;
        String estado = seleccion.estado;

        if (Normalizador.esVacio(tareaId)) {
            return;
        }

        if (Tarea.ESTADO_ERROR.equals(estado)) {
            menu.add(crearMenuItemContextual(
                    I18nUI.Tareas.MENU_VER_DETALLES_ERROR(),
                    I18nUI.Tooltips.Tareas.MENU_VER_DETALLES_ERROR(),
                    e -> {
                        Tarea tarea = gestorTareas.obtenerTarea(tareaId);
                        String url = (tarea != null) ? tarea.obtenerUrl() : seleccion.url;
                        String errorDetails = (tarea != null) ? tarea.obtenerMensajeInfo() : "";

                        UIUtils.ejecutarEnEdt(() -> {
                            UIUtils.mostrarDetallesErrorAvanzado(PanelTareas.this, url, errorDetails);
                        });
                    }));
        }

        if (Tarea.ESTADO_ERROR.equals(estado) || Tarea.ESTADO_CANCELADO.equals(estado)) {
            menu.add(crearMenuItemContextual(
                    I18nUI.Tareas.MENU_REINTENTAR(),
                    I18nUI.Tooltips.Tareas.MENU_REINTENTAR_UNA(),
                    e -> {
                        reencolarTarea(tareaId);
                        actualizarEstadisticas();
                    }));
        }

        if (Tarea.ESTADO_EN_COLA.equals(estado) || Tarea.ESTADO_ANALIZANDO.equals(estado)) {
            menu.add(crearMenuItemContextual(
                    I18nUI.Tareas.MENU_PAUSAR(),
                    I18nUI.Tooltips.Tareas.MENU_PAUSAR_UNA(),
                    e -> {
                        if (gestorTareas.pausarTarea(tareaId)) {
                            actualizarEstadisticas();
                        }
                    }));
        }

        if (Tarea.ESTADO_PAUSADO.equals(estado)) {
            menu.add(crearMenuItemContextual(
                    I18nUI.Tareas.MENU_REANUDAR(),
                    I18nUI.Tooltips.Tareas.MENU_REANUDAR_UNA(),
                    e -> {
                        if (gestorTareas.reanudarTarea(tareaId)) {
                            actualizarEstadisticas();
                        }
                    }));
        }

        if (Tarea.ESTADO_EN_COLA.equals(estado) || Tarea.ESTADO_ANALIZANDO.equals(estado)
                || Tarea.ESTADO_PAUSADO.equals(estado)) {
            if (menu.getComponentCount() > 0)
                menu.addSeparator();

            menu.add(crearMenuItemContextual(
                    I18nUI.Tareas.MENU_CANCELAR(),
                    I18nUI.Tooltips.Tareas.MENU_CANCELAR_UNA(),
                    e -> {
                        boolean confirmacion = UIUtils.confirmarPregunta(
                                PanelTareas.this,
                                I18nUI.Tareas.TITULO_CONFIRMAR_CANCELACION(),
                                I18nUI.Tareas.MSG_CONFIRMAR_CANCELAR_UNA_TAREA());
                        if (confirmacion && gestorTareas.cancelarTarea(tareaId)) {
                            actualizarEstadisticas();
                        }
                    }));
        }

        if (Tarea.ESTADO_COMPLETADO.equals(estado) || Tarea.ESTADO_ERROR.equals(estado)
                || Tarea.ESTADO_CANCELADO.equals(estado)) {
            if (menu.getComponentCount() > 0)
                menu.addSeparator();

            menu.add(crearMenuItemContextual(
                    I18nUI.Tareas.MENU_ELIMINAR_LISTA(),
                    I18nUI.Tooltips.Tareas.MENU_ELIMINAR_UNA(),
                    e -> {
                        if (gestorTareas.limpiarTarea(tareaId)) {
                            actualizarEstadisticas();
                        }
                    }));
        }
    }

    private void crearMenuMultipleTareas(JPopupMenu menu, List<TareaSeleccionada> seleccion) {
        int erroresCanceladas = 0;
        int activas = 0;
        int pausadas = 0;
        int finalizadas = 0;

        for (TareaSeleccionada tarea : seleccion) {
            String estado = tarea.estado;

            if (Tarea.ESTADO_ERROR.equals(estado) || Tarea.ESTADO_CANCELADO.equals(estado)) {
                erroresCanceladas++;
            } else if (Tarea.ESTADO_EN_COLA.equals(estado) || Tarea.ESTADO_ANALIZANDO.equals(estado)) {
                activas++;
            } else if (Tarea.ESTADO_PAUSADO.equals(estado)) {
                pausadas++;
            } else if (Tarea.ESTADO_COMPLETADO.equals(estado)) {
                finalizadas++;
            }
        }

        if (erroresCanceladas > 0) {
            menu.add(crearMenuItemContextual(
                    I18nUI.Tareas.MENU_REINTENTAR_MULTIPLES(erroresCanceladas),
                    I18nUI.Tooltips.Tareas.MENU_REINTENTAR_MULTIPLES(),
                    e -> reintentarTareas(seleccion)));
        }

        if (activas > 0) {
            menu.add(crearMenuItemContextual(
                    I18nUI.Tareas.MENU_PAUSAR_MULTIPLES(activas),
                    I18nUI.Tooltips.Tareas.MENU_PAUSAR_MULTIPLES(),
                    e -> pausarTareas(seleccion)));
        }

        if (pausadas > 0) {
            menu.add(crearMenuItemContextual(
                    I18nUI.Tareas.MENU_REANUDAR_MULTIPLES(pausadas),
                    I18nUI.Tooltips.Tareas.MENU_REANUDAR_MULTIPLES(),
                    e -> reanudarTareas(seleccion)));
        }

        if (activas > 0 || pausadas > 0) {
            if (menu.getComponentCount() > 0)
                menu.addSeparator();

            menu.add(crearMenuItemContextual(
                    I18nUI.Tareas.MENU_CANCELAR_MULTIPLES(activas + pausadas),
                    I18nUI.Tooltips.Tareas.MENU_CANCELAR_MULTIPLES(),
                    e -> cancelarTareas(seleccion)));
        }

        int totalFinalizadas = erroresCanceladas + finalizadas;
        if (totalFinalizadas > 0) {
            if (menu.getComponentCount() > 0)
                menu.addSeparator();

            menu.add(crearMenuItemContextual(
                    I18nUI.Tareas.MENU_ELIMINAR_MULTIPLES(totalFinalizadas),
                    I18nUI.Tooltips.Tareas.MENU_ELIMINAR_MULTIPLES(),
                    e -> eliminarTareasSeleccionadas(seleccion)));
        }
    }

    private void reintentarTareas(List<TareaSeleccionada> seleccion) {
        int contador = procesarSeleccion(seleccion, Tarea::esEstadoReintentable, this::reencolarTarea);
        actualizarEstadisticas();
        mostrarMensaje(I18nUI.Tareas.MSG_REINTENTOS(contador));
    }

    public void establecerManejadorReintento(Function<String, Boolean> manejadorReintento) {
        this.manejadorReintento = manejadorReintento;
    }

    private boolean reencolarTarea(String tareaId) {
        if (Normalizador.esVacio(tareaId)) {
            return false;
        }
        Function<String, Boolean> manejador = this.manejadorReintento;
        if (manejador != null) {
            try {
                return Boolean.TRUE.equals(manejador.apply(tareaId));
            } catch (Exception ex) {
                GESTOR_LOGGING.error(ORIGEN_LOG, I18nLogs.tr("Error al reencolar tarea desde el manejador configurado"), ex);
                return false;
            }
        } else {
            return gestorTareas.reanudarTarea(tareaId);
        }
    }

    private void pausarTareas(List<TareaSeleccionada> seleccion) {
        int contador = procesarSeleccion(seleccion, Tarea::esEstadoPausable, gestorTareas::pausarTarea);
        actualizarEstadisticas();
        mostrarMensaje(I18nUI.Tareas.MSG_PAUSADAS(contador));
    }

    private void reanudarTareas(List<TareaSeleccionada> seleccion) {
        int contador = procesarSeleccion(seleccion, Tarea::esEstadoReanudable, gestorTareas::reanudarTarea);
        actualizarEstadisticas();
        mostrarMensaje(I18nUI.Tareas.MSG_REANUDADAS(contador));
    }

    private void cancelarTareas(List<TareaSeleccionada> seleccion) {
        int total = contarSeleccion(seleccion, Tarea::esEstadoCancelable);
        if (total <= 0) {
            UIUtils.mostrarInfo(this, I18nUI.Tareas.TITULO_INFORMACION(), I18nUI.Tareas.INFO_SIN_TAREAS_CANCELAR());
            return;
        }

        boolean confirmacion = UIUtils.confirmarPregunta(
                this,
                I18nUI.Tareas.TITULO_CONFIRMAR_CANCELACION(),
                I18nUI.Tareas.MSG_CONFIRMAR_CANCELAR_TAREAS(total));
        if (!confirmacion)
            return;

        int contador = procesarSeleccion(seleccion, Tarea::esEstadoCancelable, gestorTareas::cancelarTarea);
        actualizarEstadisticas();
        mostrarMensaje(I18nUI.Tareas.MSG_CANCELADAS(contador));
    }

    private void eliminarTareasSeleccionadas(List<TareaSeleccionada> seleccion) {
        int total = contarSeleccion(seleccion, Tarea::esEstadoEliminable);
        if (total <= 0) {
            UIUtils.mostrarInfo(this, I18nUI.Tareas.TITULO_INFORMACION(), I18nUI.Tareas.INFO_SIN_TAREAS_LIMPIAR());
            return;
        }

        boolean confirmacion = UIUtils.confirmarPregunta(
                this,
                I18nUI.Tareas.TITULO_CONFIRMAR_LIMPIEZA(),
                I18nUI.Tareas.MSG_CONFIRMAR_LIMPIAR_COMPLETADAS(total));
        if (!confirmacion)
            return;

        int contador = procesarSeleccion(seleccion, Tarea::esEstadoEliminable, gestorTareas::limpiarTarea);
        actualizarEstadisticas();
        mostrarMensaje(I18nUI.Tareas.MSG_ELIMINADAS(contador));
    }

    private int contarSeleccion(List<TareaSeleccionada> seleccion, Predicate<String> estadoPermitido) {
        if (Normalizador.esVacia(seleccion) || estadoPermitido == null) {
            return 0;
        }
        int total = 0;
        for (TareaSeleccionada tarea : seleccion) {
            if (tarea == null || tarea.tareaId == null) {
                continue;
            }
            if (estadoPermitido.test(tarea.estado)) {
                total++;
            }
        }
        return total;
    }

    private int procesarSeleccion(List<TareaSeleccionada> seleccion,
            Predicate<String> estadoPermitido,
            Function<String, Boolean> accionPorId) {
        if (Normalizador.esVacia(seleccion) || estadoPermitido == null || accionPorId == null) {
            return 0;
        }
        int contador = 0;
        for (TareaSeleccionada tarea : seleccion) {
            if (tarea == null || tarea.tareaId == null || !estadoPermitido.test(tarea.estado)) {
                continue;
            }
            if (Boolean.TRUE.equals(accionPorId.apply(tarea.tareaId))) {
                contador++;
            }
        }
        return contador;
    }

    private List<TareaSeleccionada> capturarSeleccion(int... filasVista) {
        if (filasVista == null || filasVista.length == 0) {
            return new ArrayList<>();
        }

        return java.util.stream.IntStream.of(filasVista)
                .filter(f -> f >= 0 && f < tabla.getRowCount())
                .map(tabla::convertRowIndexToModel)
                .filter(f -> f >= 0 && f < modelo.getRowCount())
                .distinct()
                .mapToObj(f -> new TareaSeleccionada(
                        modelo.obtenerIdTarea(f),
                        valorCeldaTexto(f, COLUMNA_ESTADO),
                        valorCeldaTexto(f, COLUMNA_URL)))
                .collect(Collectors.toList());
    }

    private String valorCeldaTexto(int filaModelo, int columna) {
        Object valor = modelo.getValueAt(filaModelo, columna);
        return valor != null ? valor.toString() : "";
    }

    private void mostrarMensaje(String mensaje) {
        UIUtils.mostrarInfo(this, I18nUI.Tareas.TITULO_INFORMACION(), mensaje);
    }

    private void actualizarEstadisticas() {
        actualizarEstadisticas(false);
    }

    private void actualizarEstadisticas(boolean forzar) {
        int versionActual = modelo != null ? modelo.obtenerVersion() : -1;
        if (!forzar && versionActual == ultimaVersionTareas) {
            return;
        }

        ContadorEstadosTareas estadisticas = obtenerEstadisticasSeguras();
        ultimaVersionTareas = versionActual;

        Runnable actualizarUi = () -> {
            etiquetaEstadisticas.setText(I18nUI.Tareas.ESTADISTICAS(
                estadisticas.obtenerActivas(),
                estadisticas.obtenerCompletadas(),
                estadisticas.obtenerErrores()));

            actualizarBotonPausarReanudar(estadisticas.obtenerPausadas());
        };
        ejecutarEnEdt(actualizarUi);
    }

    /**
     * DRY: Obtiene estadísticas de forma segura, manejando null en modelo o en el resultado.
     */
    private ContadorEstadosTareas obtenerEstadisticasSeguras() {
        if (modelo == null) {
            return ContadorEstadosTareas.vacio();
        }
        ContadorEstadosTareas resultado = modelo.contarEstados();
        return resultado != null ? resultado : ContadorEstadosTareas.vacio();
    }

    public void aplicarIdioma() {
        botonCancelar.setText(I18nUI.Tareas.BOTON_CANCELAR_TODO());
        botonLimpiarCompletadas.setText(I18nUI.Tareas.BOTON_LIMPIAR());
        botonCancelar.setToolTipText(I18nUI.Tooltips.Tareas.CANCELAR());
        botonLimpiarCompletadas.setToolTipText(I18nUI.Tooltips.Tareas.LIMPIAR());
        etiquetaEstadisticas.setToolTipText(I18nUI.Tooltips.Tareas.ESTADISTICAS());
        tabla.setToolTipText(I18nUI.Tooltips.Tareas.TABLA());
        UIUtils.actualizarTituloPanel(panelControles, I18nUI.Tareas.TITULO_CONTROLES());
        UIUtils.actualizarTituloPanel(panelTablaWrapper, I18nUI.Tareas.TITULO_LISTA());
        modelo.refrescarColumnasIdioma();
        ejecutarEnEdt(this::configurarColumnasTabla);
        actualizarEstadisticas(true);
        revalidate();
        repaint();
    }

    public void aplicarTema() {
        Runnable aplicar = () -> {
            Color fondoPanel = EstilosUI.obtenerFondoPanel();

            setBackground(fondoPanel);
            panelControles.setBackground(fondoPanel);
            panelTablaWrapper.setBackground(fondoPanel);

            UIUtils.actualizarTituloPanel(panelControles, I18nUI.Tareas.TITULO_CONTROLES());
            UIUtils.actualizarTituloPanel(panelTablaWrapper, I18nUI.Tareas.TITULO_LISTA());

            Color colorTexto = EstilosUI.colorTextoPrimario(fondoPanel);
            etiquetaEstadisticas.setForeground(colorTexto);

            revalidate();
            repaint();
        };

        ejecutarEnEdt(aplicar);
    }

    private void configurarColumnasTabla() {
        if (tabla.getColumnModel().getColumnCount() < NUMERO_COLUMNAS) {
            return;
        }
        tabla.getColumnModel().getColumn(COLUMNA_ESTADO).setCellRenderer(new RenderizadorEstado());
        tabla.getColumnModel().getColumn(COLUMNA_DURACION).setCellRenderer(new RenderizadorCentrado());

        tabla.getColumnModel().getColumn(COLUMNA_ID).setPreferredWidth(ANCHO_COLUMNA_ID);
        tabla.getColumnModel().getColumn(COLUMNA_URL).setPreferredWidth(ANCHO_COLUMNA_URL);
        tabla.getColumnModel().getColumn(COLUMNA_ESTADO).setPreferredWidth(ANCHO_COLUMNA_ESTADO);
        tabla.getColumnModel().getColumn(COLUMNA_DURACION).setPreferredWidth(ANCHO_COLUMNA_DURACION);
        UIUtils.instalarTooltipsEncabezadoTabla(tabla,
                I18nUI.Tooltips.Tareas.COLUMNA_TIPO(),
                I18nUI.Tooltips.Tareas.COLUMNA_URL(),
                I18nUI.Tooltips.Tareas.COLUMNA_ESTADO(),
                I18nUI.Tooltips.Tareas.COLUMNA_DURACION());
    }

    public void agregarTarea(Tarea tarea) {
        ejecutarEnEdt(() -> {
            modelo.agregarTarea(tarea);
            actualizarEstadisticas();
        });
    }

    public void actualizarTarea(Tarea tarea) {
        ejecutarEnEdt(() -> {
            modelo.actualizarTarea(tarea);
            actualizarEstadisticas();
        });
    }

    public ModeloTablaTareas obtenerModelo() {
        return modelo;
    }

    public void destruir() {
        timerActualizacion.stop();
    }

    public void establecerConfiguracion(ConfiguracionAPI config) {
        if (config == null) {
            return;
        }
        modelo.establecerLimiteFilas(config.obtenerMaximoTareasTabla());
    }

    /**
     * Obtiene la tabla de tareas.
     * Método público para UI State Persistence.
     *
     * @return La tabla de tareas
     */
    public JTable obtenerTabla() {
        return tabla;
    }

    private static final class TareaSeleccionada {
        private final String tareaId;
        private final String estado;
        private final String url;

        private TareaSeleccionada(String tareaId, String estado, String url) {
            this.tareaId = tareaId != null ? tareaId : "";
            this.estado = estado != null ? estado : "";
            this.url = url != null ? url : "";
        }
    }
}
