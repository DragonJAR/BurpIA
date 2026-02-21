package com.burpia.ui;

import com.burpia.i18n.I18nUI;
import com.burpia.model.Tarea;
import com.burpia.util.GestorTareas;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PanelTareas extends JPanel {
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

    private static final int UMBRAL_RESPONSIVE = 800;

    public PanelTareas(GestorTareas gestorTareas, ModeloTablaTareas modelo) {
        this.modelo = modelo;
        this.tabla = new JTable(modelo);
        this.gestorTareas = gestorTareas;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panelControles = new JPanel();
        panelControles.setLayout(new BoxLayout(panelControles, BoxLayout.Y_AXIS));
        panelControles.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
                I18nUI.Tareas.TITULO_CONTROLES(),
                TitledBorder.LEFT,
                TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

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

        botonPausarReanudar = new JButton(I18nUI.Tareas.BOTON_PAUSAR_TODO());
        botonPausarReanudar.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonPausarReanudar.setToolTipText(TooltipsUI.Tareas.PAUSAR_REANUDAR());
        panelTodosControles.add(botonPausarReanudar);

        botonCancelar = new JButton(I18nUI.Tareas.BOTON_CANCELAR_TODO());
        botonCancelar.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonCancelar.setToolTipText(TooltipsUI.Tareas.CANCELAR());
        panelTodosControles.add(botonCancelar);

        botonLimpiarCompletadas = new JButton(I18nUI.Tareas.BOTON_LIMPIAR());
        botonLimpiarCompletadas.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonLimpiarCompletadas.setToolTipText(TooltipsUI.Tareas.LIMPIAR());
        panelTodosControles.add(botonLimpiarCompletadas);

        etiquetaEstadisticas = new JLabel(I18nUI.Tareas.ESTADISTICAS(0, 0, 0));
        etiquetaEstadisticas.setFont(EstilosUI.FUENTE_MONO);
        etiquetaEstadisticas.setToolTipText(TooltipsUI.Tareas.ESTADISTICAS());
        panelTodosControles.add(etiquetaEstadisticas);

        panelControles.add(panelTodosControles);

        panelTablaWrapper = new JPanel(new BorderLayout());
        panelTablaWrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
                I18nUI.Tareas.TITULO_LISTA(),
                TitledBorder.LEFT,
                TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        tabla.setAutoCreateRowSorter(true);
        tabla.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tabla.setRowHeight(EstilosUI.ALTURA_FILA_TABLA);
        tabla.setFont(EstilosUI.FUENTE_TABLA);
        tabla.setToolTipText(TooltipsUI.Tareas.TABLA());
        configurarColumnasTabla();

        JScrollPane panelDesplazable = new JScrollPane(tabla);
        panelDesplazable.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panelTablaWrapper.add(panelDesplazable, BorderLayout.CENTER);

        botonPausarReanudar.addActionListener(e -> {
            EstadisticasTareas estadisticas = calcularEstadisticasTareas();
            if (estadisticas.pausadas > 0) {
                gestorTareas.reanudarTodasPausadas();
            } else if (estadisticas.activasSinPausadas > 0) {
                gestorTareas.pausarTodasActivas();
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    I18nUI.Tareas.INFO_SIN_TAREAS_GESTIONAR(),
                    I18nUI.Tareas.TITULO_INFORMACION(),
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
            actualizarEstadisticas();
        });

        botonCancelar.addActionListener(e -> {
            EstadisticasTareas estadisticas = calcularEstadisticasTareas();
            int activas = estadisticas.activas;

            if (activas == 0) {
                JOptionPane.showMessageDialog(
                    this,
                    I18nUI.Tareas.INFO_SIN_TAREAS_CANCELAR(),
                    I18nUI.Tareas.TITULO_INFORMACION(),
                    JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }

            int confirmacion = JOptionPane.showConfirmDialog(
                this,
                I18nUI.Tareas.MSG_CONFIRMAR_CANCELAR_TAREAS(activas),
                I18nUI.Tareas.TITULO_CONFIRMAR_CANCELACION(),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (confirmacion == JOptionPane.YES_OPTION) {
                gestorTareas.cancelarTodas();
                actualizarEstadisticas();
            }
        });

        botonLimpiarCompletadas.addActionListener(e -> {
            EstadisticasTareas estadisticas = calcularEstadisticasTareas();
            int completadas = estadisticas.finalizadas;

            if (completadas == 0) {
                JOptionPane.showMessageDialog(
                    this,
                    I18nUI.Tareas.INFO_SIN_TAREAS_LIMPIAR(),
                    I18nUI.Tareas.TITULO_INFORMACION(),
                    JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }

            int confirmacion = JOptionPane.showConfirmDialog(
                this,
                I18nUI.Tareas.MSG_CONFIRMAR_LIMPIAR_COMPLETADAS(completadas),
                I18nUI.Tareas.TITULO_CONFIRMAR_LIMPIEZA(),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            if (confirmacion == JOptionPane.YES_OPTION) {
                gestorTareas.limpiarCompletadas();
                actualizarEstadisticas();
            }
        });

        timerActualizacion = new Timer(1000, e -> actualizarEstadisticas());
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

    private void mostrarMenuContextualDinamico(int x, int y) {
        int[] filas = tabla.getSelectedRows();
        if (filas.length == 0) return;

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
        if (Tarea.ESTADO_ERROR.equals(estado)) {
            JMenuItem menuItemVerDetalles = new JMenuItem(I18nUI.Tareas.MENU_VER_DETALLES_ERROR());
            menuItemVerDetalles.setFont(EstilosUI.FUENTE_ESTANDAR);
            menuItemVerDetalles.setToolTipText(TooltipsUI.Tareas.MENU_VER_DETALLES_ERROR());
            menuItemVerDetalles.addActionListener(e -> {
                String mensaje = I18nUI.Tareas.MSG_DETALLES_ERROR(seleccion.url, seleccion.duracion, estado);
                JOptionPane.showMessageDialog(
                    PanelTareas.this,
                    mensaje,
                    I18nUI.Tareas.TITULO_DETALLES_ERROR(),
                    JOptionPane.ERROR_MESSAGE
                );
            });
            menu.add(menuItemVerDetalles);
        }

        if (Tarea.ESTADO_ERROR.equals(estado) || Tarea.ESTADO_CANCELADO.equals(estado)) {
            JMenuItem menuItemReintentar = new JMenuItem(I18nUI.Tareas.MENU_REINTENTAR());
            menuItemReintentar.setFont(EstilosUI.FUENTE_ESTANDAR);
            menuItemReintentar.setToolTipText(TooltipsUI.Tareas.MENU_REINTENTAR_UNA());
            menuItemReintentar.addActionListener(e -> {
                if (tareaId != null) {
                    gestorTareas.reanudarTarea(tareaId);
                    actualizarEstadisticas();
                }
            });
            menu.add(menuItemReintentar);
        }

        if (Tarea.ESTADO_EN_COLA.equals(estado) || Tarea.ESTADO_ANALIZANDO.equals(estado)) {
            JMenuItem menuItemPausar = new JMenuItem(I18nUI.Tareas.MENU_PAUSAR());
            menuItemPausar.setFont(EstilosUI.FUENTE_ESTANDAR);
            menuItemPausar.setToolTipText(TooltipsUI.Tareas.MENU_PAUSAR_UNA());
            menuItemPausar.addActionListener(e -> {
                if (tareaId != null) {
                    gestorTareas.pausarTarea(tareaId);
                    actualizarEstadisticas();
                }
            });
            menu.add(menuItemPausar);
        }

        if (Tarea.ESTADO_PAUSADO.equals(estado)) {
            JMenuItem menuItemReanudar = new JMenuItem(I18nUI.Tareas.MENU_REANUDAR());
            menuItemReanudar.setFont(EstilosUI.FUENTE_ESTANDAR);
            menuItemReanudar.setToolTipText(TooltipsUI.Tareas.MENU_REANUDAR_UNA());
            menuItemReanudar.addActionListener(e -> {
                if (tareaId != null) {
                    gestorTareas.reanudarTarea(tareaId);
                    actualizarEstadisticas();
                }
            });
            menu.add(menuItemReanudar);
        }

        if (Tarea.ESTADO_EN_COLA.equals(estado) || Tarea.ESTADO_ANALIZANDO.equals(estado) || Tarea.ESTADO_PAUSADO.equals(estado)) {
            if (menu.getComponentCount() > 0) menu.addSeparator();

            JMenuItem menuItemCancelar = new JMenuItem(I18nUI.Tareas.MENU_CANCELAR());
            menuItemCancelar.setFont(EstilosUI.FUENTE_ESTANDAR);
            menuItemCancelar.setToolTipText(TooltipsUI.Tareas.MENU_CANCELAR_UNA());
            menuItemCancelar.addActionListener(e -> {
                int confirmacion = JOptionPane.showConfirmDialog(
                    PanelTareas.this,
                    I18nUI.Tareas.MSG_CONFIRMAR_CANCELAR_UNA_TAREA(),
                    I18nUI.Tareas.TITULO_CONFIRMAR_CANCELACION(),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );
                if (confirmacion == JOptionPane.YES_OPTION && tareaId != null) {
                    gestorTareas.cancelarTarea(tareaId);
                    actualizarEstadisticas();
                }
            });
            menu.add(menuItemCancelar);
        }

        if (Tarea.ESTADO_COMPLETADO.equals(estado) || Tarea.ESTADO_ERROR.equals(estado) || Tarea.ESTADO_CANCELADO.equals(estado)) {
            if (menu.getComponentCount() > 0) menu.addSeparator();

            JMenuItem menuItemLimpiar = new JMenuItem(I18nUI.Tareas.MENU_ELIMINAR_LISTA());
            menuItemLimpiar.setFont(EstilosUI.FUENTE_ESTANDAR);
            menuItemLimpiar.setToolTipText(TooltipsUI.Tareas.MENU_ELIMINAR_UNA());
            menuItemLimpiar.addActionListener(e -> {
                if (tareaId != null) {
                    gestorTareas.limpiarTarea(tareaId);
                    actualizarEstadisticas();
                }
            });
            menu.add(menuItemLimpiar);
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
            JMenuItem menuItemReintentar = new JMenuItem(I18nUI.Tareas.MENU_REINTENTAR_MULTIPLES(erroresCanceladas));
            menuItemReintentar.setFont(EstilosUI.FUENTE_ESTANDAR);
            menuItemReintentar.setToolTipText(TooltipsUI.Tareas.MENU_REINTENTAR_MULTIPLES());
            menuItemReintentar.addActionListener(e -> reintentarTareas(seleccion));
            menu.add(menuItemReintentar);
        }

        if (activas > 0) {
            JMenuItem menuItemPausar = new JMenuItem(I18nUI.Tareas.MENU_PAUSAR_MULTIPLES(activas));
            menuItemPausar.setFont(EstilosUI.FUENTE_ESTANDAR);
            menuItemPausar.setToolTipText(TooltipsUI.Tareas.MENU_PAUSAR_MULTIPLES());
            menuItemPausar.addActionListener(e -> pausarTareas(seleccion));
            menu.add(menuItemPausar);
        }

        if (pausadas > 0) {
            JMenuItem menuItemReanudar = new JMenuItem(I18nUI.Tareas.MENU_REANUDAR_MULTIPLES(pausadas));
            menuItemReanudar.setFont(EstilosUI.FUENTE_ESTANDAR);
            menuItemReanudar.setToolTipText(TooltipsUI.Tareas.MENU_REANUDAR_MULTIPLES());
            menuItemReanudar.addActionListener(e -> reanudarTareas(seleccion));
            menu.add(menuItemReanudar);
        }

        if (activas > 0 || pausadas > 0) {
            if (menu.getComponentCount() > 0) menu.addSeparator();

            JMenuItem menuItemCancelar = new JMenuItem(I18nUI.Tareas.MENU_CANCELAR_MULTIPLES(activas + pausadas));
            menuItemCancelar.setFont(EstilosUI.FUENTE_ESTANDAR);
            menuItemCancelar.setToolTipText(TooltipsUI.Tareas.MENU_CANCELAR_MULTIPLES());
            menuItemCancelar.addActionListener(e -> cancelarTareas(seleccion));
            menu.add(menuItemCancelar);
        }

        int totalFinalizadas = erroresCanceladas + finalizadas;
        if (totalFinalizadas > 0) {
            if (menu.getComponentCount() > 0) menu.addSeparator();

            JMenuItem menuItemLimpiar = new JMenuItem(I18nUI.Tareas.MENU_ELIMINAR_MULTIPLES(totalFinalizadas));
            menuItemLimpiar.setFont(EstilosUI.FUENTE_ESTANDAR);
            menuItemLimpiar.setToolTipText(TooltipsUI.Tareas.MENU_ELIMINAR_MULTIPLES());
            menuItemLimpiar.addActionListener(e -> eliminarTareasSeleccionadas(seleccion));
            menu.add(menuItemLimpiar);
        }
    }

    private void reintentarTareas(List<TareaSeleccionada> seleccion) {
        int contador = 0;
        for (TareaSeleccionada tarea : seleccion) {
            String estado = tarea.estado;
            String tareaId = tarea.tareaId;
            if (tareaId != null && (Tarea.ESTADO_ERROR.equals(estado) || Tarea.ESTADO_CANCELADO.equals(estado))) {
                gestorTareas.reanudarTarea(tareaId);
                contador++;
            }
        }
        actualizarEstadisticas();
        mostrarMensaje(I18nUI.Tareas.MSG_REINTENTOS(contador));
    }

    private void pausarTareas(List<TareaSeleccionada> seleccion) {
        int contador = 0;
        for (TareaSeleccionada tarea : seleccion) {
            String estado = tarea.estado;
            String tareaId = tarea.tareaId;
            if (tareaId != null && (Tarea.ESTADO_EN_COLA.equals(estado) || Tarea.ESTADO_ANALIZANDO.equals(estado))) {
                gestorTareas.pausarTarea(tareaId);
                contador++;
            }
        }
        actualizarEstadisticas();
        mostrarMensaje(I18nUI.Tareas.MSG_PAUSADAS(contador));
    }

    private void reanudarTareas(List<TareaSeleccionada> seleccion) {
        int contador = 0;
        for (TareaSeleccionada tarea : seleccion) {
            String estado = tarea.estado;
            String tareaId = tarea.tareaId;
            if (tareaId != null && Tarea.ESTADO_PAUSADO.equals(estado)) {
                gestorTareas.reanudarTarea(tareaId);
                contador++;
            }
        }
        actualizarEstadisticas();
        mostrarMensaje(I18nUI.Tareas.MSG_REANUDADAS(contador));
    }

    private void cancelarTareas(List<TareaSeleccionada> seleccion) {
        int total = 0;
        for (TareaSeleccionada tarea : seleccion) {
            String estado = tarea.estado;
            if (Tarea.ESTADO_EN_COLA.equals(estado) || Tarea.ESTADO_ANALIZANDO.equals(estado) || Tarea.ESTADO_PAUSADO.equals(estado)) {
                total++;
            }
        }

        int confirmacion = JOptionPane.showConfirmDialog(
            this,
            I18nUI.Tareas.MSG_CONFIRMAR_CANCELAR_TAREAS(total),
            I18nUI.Tareas.TITULO_CONFIRMAR_CANCELACION(),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (confirmacion != JOptionPane.YES_OPTION) return;

        int contador = 0;
        for (TareaSeleccionada tarea : seleccion) {
            String estado = tarea.estado;
            String tareaId = tarea.tareaId;
            if (tareaId != null && (Tarea.ESTADO_EN_COLA.equals(estado) || Tarea.ESTADO_ANALIZANDO.equals(estado) || Tarea.ESTADO_PAUSADO.equals(estado))) {
                gestorTareas.cancelarTarea(tareaId);
                contador++;
            }
        }
        actualizarEstadisticas();
        mostrarMensaje(I18nUI.Tareas.MSG_CANCELADAS(contador));
    }

    private void eliminarTareasSeleccionadas(List<TareaSeleccionada> seleccion) {
        List<String> idsAEliminar = new ArrayList<>();

        for (TareaSeleccionada tarea : seleccion) {
            String estado = tarea.estado;
            if (Tarea.ESTADO_COMPLETADO.equals(estado) ||
                Tarea.ESTADO_ERROR.equals(estado) ||
                Tarea.ESTADO_CANCELADO.equals(estado)) {
                String tareaId = tarea.tareaId;
                if (tareaId != null) {
                    idsAEliminar.add(tareaId);
                }
            }
        }

        int contador = 0;
        for (String tareaId : idsAEliminar) {
            gestorTareas.limpiarTarea(tareaId);
            contador++;
        }

        actualizarEstadisticas();
        mostrarMensaje(I18nUI.Tareas.MSG_ELIMINADAS(contador));
    }

    private List<TareaSeleccionada> capturarSeleccion(int[] filasVista) {
        if (filasVista == null || filasVista.length == 0) {
            return new ArrayList<>();
        }
        List<TareaSeleccionada> seleccion = new ArrayList<>();
        Set<Integer> filasModeloVistas = new HashSet<>();

        for (int filaVista : filasVista) {
            if (filaVista < 0 || filaVista >= tabla.getRowCount()) {
                continue;
            }
            int filaModelo = tabla.convertRowIndexToModel(filaVista);
            if (filaModelo < 0 || filaModelo >= modelo.getRowCount() || !filasModeloVistas.add(filaModelo)) {
                continue;
            }
            String tareaId = modelo.obtenerIdTarea(filaModelo);
            String estado = valorCeldaTexto(filaModelo, 2);
            String url = valorCeldaTexto(filaModelo, 1);
            String duracion = valorCeldaTexto(filaModelo, 3);
            seleccion.add(new TareaSeleccionada(tareaId, estado, url, duracion));
        }

        return seleccion;
    }

    private String valorCeldaTexto(int filaModelo, int columna) {
        Object valor = modelo.getValueAt(filaModelo, columna);
        return valor != null ? valor.toString() : "";
    }

    private void mostrarMensaje(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                PanelTareas.this,
                mensaje,
                I18nUI.Tareas.TITULO_INFORMACION(),
                JOptionPane.INFORMATION_MESSAGE
            );
        });
    }

    private void actualizarEstadisticas() {
        SwingUtilities.invokeLater(() -> {
            EstadisticasTareas estadisticas = calcularEstadisticasTareas();

            etiquetaEstadisticas.setText(I18nUI.Tareas.ESTADISTICAS(
                estadisticas.activas,
                estadisticas.completadas,
                estadisticas.errores
            ));

            if (estadisticas.pausadas > 0) {
                botonPausarReanudar.setText(I18nUI.Tareas.BOTON_REANUDAR_TODO());
                botonPausarReanudar.setToolTipText(TooltipsUI.Tareas.REANUDAR_TODO());
            } else {
                botonPausarReanudar.setText(I18nUI.Tareas.BOTON_PAUSAR_TODO());
                botonPausarReanudar.setToolTipText(TooltipsUI.Tareas.PAUSAR_TODO());
            }
        });
    }

    public void aplicarIdioma() {
        botonCancelar.setText(I18nUI.Tareas.BOTON_CANCELAR_TODO());
        botonLimpiarCompletadas.setText(I18nUI.Tareas.BOTON_LIMPIAR());
        botonPausarReanudar.setToolTipText(TooltipsUI.Tareas.PAUSAR_REANUDAR());
        botonCancelar.setToolTipText(TooltipsUI.Tareas.CANCELAR());
        botonLimpiarCompletadas.setToolTipText(TooltipsUI.Tareas.LIMPIAR());
        etiquetaEstadisticas.setToolTipText(TooltipsUI.Tareas.ESTADISTICAS());
        tabla.setToolTipText(TooltipsUI.Tareas.TABLA());
        actualizarTituloPanel(panelControles, I18nUI.Tareas.TITULO_CONTROLES());
        actualizarTituloPanel(panelTablaWrapper, I18nUI.Tareas.TITULO_LISTA());
        modelo.refrescarColumnasIdioma();
        SwingUtilities.invokeLater(this::configurarColumnasTabla);
        actualizarEstadisticas();
        revalidate();
        repaint();
    }

    private void configurarColumnasTabla() {
        if (tabla.getColumnModel().getColumnCount() < 4) {
            return;
        }
        tabla.getColumnModel().getColumn(2).setCellRenderer(new RenderizadorEstado());
        tabla.getColumnModel().getColumn(3).setCellRenderer(new RenderizadorCentrado());

        tabla.getColumnModel().getColumn(0).setPreferredWidth(120);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(400);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(130);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(100);
    }

    private void actualizarTituloPanel(JPanel panel, String titulo) {
        if (panel == null) {
            return;
        }
        Border borde = panel.getBorder();
        if (!(borde instanceof CompoundBorder)) {
            return;
        }
        Border externo = ((CompoundBorder) borde).getOutsideBorder();
        if (externo instanceof TitledBorder) {
            ((TitledBorder) externo).setTitle(titulo);
        }
    }

    public void agregarTarea(Tarea tarea) {
        SwingUtilities.invokeLater(() -> {
            modelo.agregarTarea(tarea);
            actualizarEstadisticas();
        });
    }

    public void actualizarTarea(Tarea tarea) {
        SwingUtilities.invokeLater(() -> {
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

    private EstadisticasTareas calcularEstadisticasTareas() {
        int enCola = modelo.contarPorEstado(Tarea.ESTADO_EN_COLA);
        int analizando = modelo.contarPorEstado(Tarea.ESTADO_ANALIZANDO);
        int pausadas = modelo.contarPorEstado(Tarea.ESTADO_PAUSADO);
        int completadas = modelo.contarPorEstado(Tarea.ESTADO_COMPLETADO);
        int errores = modelo.contarPorEstado(Tarea.ESTADO_ERROR);
        int canceladas = modelo.contarPorEstado(Tarea.ESTADO_CANCELADO);
        int activasSinPausadas = enCola + analizando;
        int activas = activasSinPausadas + pausadas;
        int finalizadas = completadas + errores + canceladas;
        return new EstadisticasTareas(
            activas,
            activasSinPausadas,
            pausadas,
            completadas,
            errores,
            finalizadas
        );
    }

    private static final class EstadisticasTareas {
        private final int activas;
        private final int activasSinPausadas;
        private final int pausadas;
        private final int completadas;
        private final int errores;
        private final int finalizadas;

        private EstadisticasTareas(int activas,
                                   int activasSinPausadas,
                                   int pausadas,
                                   int completadas,
                                   int errores,
                                   int finalizadas) {
            this.activas = activas;
            this.activasSinPausadas = activasSinPausadas;
            this.pausadas = pausadas;
            this.completadas = completadas;
            this.errores = errores;
            this.finalizadas = finalizadas;
        }
    }

    private static final class TareaSeleccionada {
        private final String tareaId;
        private final String estado;
        private final String url;
        private final String duracion;

        private TareaSeleccionada(String tareaId, String estado, String url, String duracion) {
            this.tareaId = tareaId;
            this.estado = estado;
            this.url = url != null ? url : "";
            this.duracion = duracion != null ? duracion : "";
        }
    }
}
