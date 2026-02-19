package com.burpia.ui;

import com.burpia.model.Tarea;
import com.burpia.util.GestorTareas;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PanelTareas extends JPanel {
    private final ModeloTablaTareas modelo;
    private final JTable tabla;
    private JButton botonPausarReanudar;
    private JButton botonCancelar;
    private JButton botonLimpiarCompletadas;
    private JLabel etiquetaEstadisticas;
    private final GestorTareas gestorTareas;
    private Timer timerActualizacion;

    // Umbral de ancho para cambiar de layout (en pixeles)
    private static final int UMBRAL_RESPONSIVE = 800;  // Ajustado para mejor distribución

    public PanelTareas(GestorTareas gestorTareas, ModeloTablaTareas modelo) {
        this.modelo = modelo;
        this.tabla = new JTable(modelo);
        this.gestorTareas = gestorTareas;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel panelControles = new JPanel();
        panelControles.setLayout(new BoxLayout(panelControles, BoxLayout.Y_AXIS));
        panelControles.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
            "🎮 CONTROLES DE TAREAS",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            EstilosUI.FUENTE_NEGRITA
        ));

        JPanel panelTodosControles = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5)) {
            private boolean ultimoLayoutHorizontal = true;

            @Override
            public void doLayout() {
                // Detectar ancho disponible
                int ancho = getWidth() - 40;
                boolean esLayoutHorizontal = ancho >= UMBRAL_RESPONSIVE;

                // Cambiar layout solo si es necesario
                if (esLayoutHorizontal != ultimoLayoutHorizontal) {
                    if (esLayoutHorizontal) {
                        // Espacio suficiente: una fila con botones + estadísticas
                        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
                    } else {
                        // Espacio limitado: botones en vertical, estadísticas abajo
                        setLayout(new GridLayout(2, 1, 0, 5));
                    }
                    ultimoLayoutHorizontal = esLayoutHorizontal;
                }

                super.doLayout();
            }
        };

        botonPausarReanudar = new JButton("⏸️ Pausar Todo");
        botonPausarReanudar.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonPausarReanudar.setToolTipText("Pausar o reanudar todas las tareas en análisis");
        panelTodosControles.add(botonPausarReanudar);

        botonCancelar = new JButton("🛑 Cancelar Todo");
        botonCancelar.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonCancelar.setToolTipText("Cancelar todas las tareas activas");
        panelTodosControles.add(botonCancelar);

        botonLimpiarCompletadas = new JButton("🧹 Limpiar Completadas");
        botonLimpiarCompletadas.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonLimpiarCompletadas.setToolTipText("Eliminar tareas completadas, con error o canceladas");
        panelTodosControles.add(botonLimpiarCompletadas);

        // Estadísticas de tareas (en el mismo panel cuando hay espacio)
        etiquetaEstadisticas = new JLabel("📊 Tareas Activas: 0 | ✅ Completadas: 0 | ❌ Con Errores: 0");
        etiquetaEstadisticas.setFont(EstilosUI.FUENTE_MONO);
        panelTodosControles.add(etiquetaEstadisticas);

        panelControles.add(panelTodosControles);

        JPanel panelTablaWrapper = new JPanel(new BorderLayout());
        panelTablaWrapper.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
            "📋 LISTA DE TAREAS",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            EstilosUI.FUENTE_NEGRITA
        ));

        tabla.setAutoCreateRowSorter(true);
        tabla.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tabla.setRowHeight(EstilosUI.ALTURA_FILA_TABLA);
        tabla.setFont(EstilosUI.FUENTE_TABLA);

        tabla.getColumnModel().getColumn(2).setCellRenderer(new RenderizadorEstado()); // Estado
        tabla.getColumnModel().getColumn(3).setCellRenderer(new RenderizadorCentrado()); // Duración centrada

        // Ajustar anchos de columna
        tabla.getColumnModel().getColumn(0).setPreferredWidth(120); // Tipo
        tabla.getColumnModel().getColumn(1).setPreferredWidth(400); // URL
        tabla.getColumnModel().getColumn(2).setPreferredWidth(130); // Estado
        tabla.getColumnModel().getColumn(3).setPreferredWidth(100); // Duración

        JScrollPane panelDesplazable = new JScrollPane(tabla);
        panelDesplazable.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panelTablaWrapper.add(panelDesplazable, BorderLayout.CENTER);

        botonPausarReanudar.addActionListener(e -> {
            gestorTareas.pausarReanudarTodas();
            actualizarEstadisticas();
        });

        botonCancelar.addActionListener(e -> {
            int activas = modelo.contarPorEstado(Tarea.ESTADO_EN_COLA) +
                         modelo.contarPorEstado(Tarea.ESTADO_ANALIZANDO) +
                         modelo.contarPorEstado(Tarea.ESTADO_PAUSADO);

            if (activas == 0) {
                JOptionPane.showMessageDialog(
                    this,
                    "No hay tareas activas para cancelar.",
                    "Información",
                    JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }

            int confirmacion = JOptionPane.showConfirmDialog(
                this,
                "¿Cancelar " + activas + " tarea(s) activa(s)?",
                "Confirmar cancelación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (confirmacion == JOptionPane.YES_OPTION) {
                gestorTareas.cancelarTodas();
                actualizarEstadisticas();
            }
        });

        botonLimpiarCompletadas.addActionListener(e -> {
            int completadas = modelo.contarPorEstado(Tarea.ESTADO_COMPLETADO) +
                             modelo.contarPorEstado(Tarea.ESTADO_ERROR) +
                             modelo.contarPorEstado(Tarea.ESTADO_CANCELADO);

            if (completadas == 0) {
                JOptionPane.showMessageDialog(
                    this,
                    "No hay tareas completadas para limpiar.",
                    "Información",
                    JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }

            int confirmacion = JOptionPane.showConfirmDialog(
                this,
                "¿Eliminar " + completadas + " tarea(s) completada(s)?",
                "Confirmar limpieza",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );
            if (confirmacion == JOptionPane.YES_OPTION) {
                gestorTareas.limpiarCompletadas();
                modelo.eliminarPorEstado(
                    Tarea.ESTADO_COMPLETADO,
                    Tarea.ESTADO_ERROR,
                    Tarea.ESTADO_CANCELADO
                );
                actualizarEstadisticas();
            }
        });

        // Timer para actualizar estadísticas cada segundo
        timerActualizacion = new Timer(1000, e -> actualizarEstadisticas());
        timerActualizacion.start();

        // Crear menú contextual para tareas individuales
        crearMenuContextual();

        add(panelControles, BorderLayout.NORTH);
        add(panelTablaWrapper, BorderLayout.CENTER);
    }

    private void crearMenuContextual() {
        // Agregar MouseListener a la tabla para mostrar menú contextual dinámico
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int fila = tabla.rowAtPoint(e.getPoint());
                    if (fila >= 0) {
                        if (!tabla.isRowSelected(fila)) {
                            if (!e.isControlDown()) {
                                tabla.setRowSelectionInterval(fila, fila);
                            } else {
                                tabla.addRowSelectionInterval(fila, fila);
                            }
                        }
                        mostrarMenuContextualDinamico(e.getX(), e.getY());
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int fila = tabla.rowAtPoint(e.getPoint());
                    if (fila >= 0) {
                        if (!tabla.isRowSelected(fila)) {
                            if (!e.isControlDown()) {
                                tabla.setRowSelectionInterval(fila, fila);
                            } else {
                                tabla.addRowSelectionInterval(fila, fila);
                            }
                        }
                        mostrarMenuContextualDinamico(e.getX(), e.getY());
                    }
                }
            }
        });
    }

    private void mostrarMenuContextualDinamico(int x, int y) {
        int[] filas = tabla.getSelectedRows();
        if (filas.length == 0) return;

        JPopupMenu menuContextual = new JPopupMenu();

        if (filas.length == 1) {
            // Menú dinámico para una sola tarea
            int filaModelo = tabla.convertRowIndexToModel(filas[0]);
            String estado = (String) modelo.getValueAt(filaModelo, 2); // Índice 2 = Estado
            String tareaId = modelo.obtenerIdTarea(filaModelo);

            crearMenuUnaTarea(menuContextual, tareaId, estado, filaModelo);
        } else {
            // Menú para múltiples tareas
            crearMenuMultipleTareas(menuContextual, filas);
        }

        menuContextual.show(tabla, x, y);
    }

    private void crearMenuUnaTarea(JPopupMenu menu, String tareaId, String estado, int filaModelo) {
        // Ver detalles - solo para tareas con ERROR
        if (Tarea.ESTADO_ERROR.equals(estado)) {
            JMenuItem menuItemVerDetalles = new JMenuItem("📋 Ver Detalles del Error");
            menuItemVerDetalles.setFont(EstilosUI.FUENTE_ESTANDAR);
            menuItemVerDetalles.addActionListener(e -> {
                String url = (String) modelo.getValueAt(filaModelo, 1);
                String duracion = (String) modelo.getValueAt(filaModelo, 3);
                String mensaje = "URL: " + url + "\n" +
                               "Duracion: " + duracion + "\n" +
                               "Estado: " + estado;
                JOptionPane.showMessageDialog(PanelTareas.this, mensaje, "Detalles del Error", JOptionPane.ERROR_MESSAGE);
            });
            menu.add(menuItemVerDetalles);
        }

        // Reintentar - solo para ERROR o CANCELADO
        if (Tarea.ESTADO_ERROR.equals(estado) || Tarea.ESTADO_CANCELADO.equals(estado)) {
            JMenuItem menuItemReintentar = new JMenuItem("🔄 Reintentar");
            menuItemReintentar.setFont(EstilosUI.FUENTE_ESTANDAR);
            menuItemReintentar.addActionListener(e -> {
                if (tareaId != null) {
                    gestorTareas.reanudarTarea(tareaId);
                    actualizarEstadisticas();
                }
            });
            menu.add(menuItemReintentar);
        }

        // Pausar - solo para EN_COLA o ANALIZANDO
        if (Tarea.ESTADO_EN_COLA.equals(estado) || Tarea.ESTADO_ANALIZANDO.equals(estado)) {
            JMenuItem menuItemPausar = new JMenuItem("⏸️ Pausar");
            menuItemPausar.setFont(EstilosUI.FUENTE_ESTANDAR);
            menuItemPausar.addActionListener(e -> {
                if (tareaId != null) {
                    gestorTareas.pausarTarea(tareaId);
                    actualizarEstadisticas();
                }
            });
            menu.add(menuItemPausar);
        }

        // Reanudar - solo para PAUSADO
        if (Tarea.ESTADO_PAUSADO.equals(estado)) {
            JMenuItem menuItemReanudar = new JMenuItem("▶️ Reanudar");
            menuItemReanudar.setFont(EstilosUI.FUENTE_ESTANDAR);
            menuItemReanudar.addActionListener(e -> {
                if (tareaId != null) {
                    gestorTareas.reanudarTarea(tareaId);
                    actualizarEstadisticas();
                }
            });
            menu.add(menuItemReanudar);
        }

        // Cancelar - para tareas activas (EN_COLA, ANALIZANDO, PAUSADO)
        if (Tarea.ESTADO_EN_COLA.equals(estado) || Tarea.ESTADO_ANALIZANDO.equals(estado) || Tarea.ESTADO_PAUSADO.equals(estado)) {
            if (menu.getComponentCount() > 0) menu.addSeparator();

            JMenuItem menuItemCancelar = new JMenuItem("🛑 Cancelar");
            menuItemCancelar.setFont(EstilosUI.FUENTE_ESTANDAR);
            menuItemCancelar.addActionListener(e -> {
                int confirmacion = JOptionPane.showConfirmDialog(
                    PanelTareas.this,
                    "¿Cancelar esta tarea?",
                    "Confirmar cancelacion",
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

        // Eliminar de la lista - para COMPLETADO, ERROR, CANCELADO
        if (Tarea.ESTADO_COMPLETADO.equals(estado) || Tarea.ESTADO_ERROR.equals(estado) || Tarea.ESTADO_CANCELADO.equals(estado)) {
            if (menu.getComponentCount() > 0) menu.addSeparator();

            JMenuItem menuItemLimpiar = new JMenuItem("🧹 Eliminar de la Lista");
            menuItemLimpiar.setFont(EstilosUI.FUENTE_ESTANDAR);
            menuItemLimpiar.addActionListener(e -> {
                if (tareaId != null) {
                    gestorTareas.limpiarTarea(tareaId);
                    modelo.eliminarTarea(filaModelo);
                    actualizarEstadisticas();
                }
            });
            menu.add(menuItemLimpiar);
        }
    }

    private void crearMenuMultipleTareas(JPopupMenu menu, int[] filas) {
        // Contar tareas por estado
        int erroresCanceladas = 0;
        int activas = 0;
        int pausadas = 0;
        int finalizadas = 0;

        for (int fila : filas) {
            int filaModelo = tabla.convertRowIndexToModel(fila);
            String estado = (String) modelo.getValueAt(filaModelo, 2); // Índice 2 = Estado

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

        // Reintentar - si hay errores o canceladas
        if (erroresCanceladas > 0) {
            JMenuItem menuItemReintentar = new JMenuItem("🔄 Reintentar " + erroresCanceladas + " tarea(s) con error/cancelada(s)");
            menuItemReintentar.setFont(EstilosUI.FUENTE_ESTANDAR);
            menuItemReintentar.addActionListener(e -> reintentarTareas(filas));
            menu.add(menuItemReintentar);
        }

        // Pausar - si hay activas
        if (activas > 0) {
            JMenuItem menuItemPausar = new JMenuItem("⏸️ Pausar " + activas + " tarea(s) activa(s)");
            menuItemPausar.setFont(EstilosUI.FUENTE_ESTANDAR);
            menuItemPausar.addActionListener(e -> pausarTareas(filas));
            menu.add(menuItemPausar);
        }

        // Reanudar - si hay pausadas
        if (pausadas > 0) {
            JMenuItem menuItemReanudar = new JMenuItem("▶️ Reanudar " + pausadas + " tarea(s) pausada(s)");
            menuItemReanudar.setFont(EstilosUI.FUENTE_ESTANDAR);
            menuItemReanudar.addActionListener(e -> reanudarTareas(filas));
            menu.add(menuItemReanudar);
        }

        // Cancelar - si hay activas o pausadas
        if (activas > 0 || pausadas > 0) {
            if (menu.getComponentCount() > 0) menu.addSeparator();

            JMenuItem menuItemCancelar = new JMenuItem("🛑 Cancelar " + (activas + pausadas) + " tarea(s) activa(s)");
            menuItemCancelar.setFont(EstilosUI.FUENTE_ESTANDAR);
            menuItemCancelar.addActionListener(e -> cancelarTareas(filas));
            menu.add(menuItemCancelar);
        }

        // Eliminar finalizadas
        int totalFinalizadas = erroresCanceladas + finalizadas;
        if (totalFinalizadas > 0) {
            if (menu.getComponentCount() > 0) menu.addSeparator();

            JMenuItem menuItemLimpiar = new JMenuItem("🧹 Eliminar " + totalFinalizadas + " tarea(s) finalizada(s)");
            menuItemLimpiar.setFont(EstilosUI.FUENTE_ESTANDAR);
            menuItemLimpiar.addActionListener(e -> eliminarTareasSeleccionadas(filas));
            menu.add(menuItemLimpiar);
        }
    }

    private void reintentarTareas(int[] filas) {
        int contador = 0;
        for (int fila : filas) {
            int filaModelo = tabla.convertRowIndexToModel(fila);
            String estado = (String) modelo.getValueAt(filaModelo, 2);
            String tareaId = modelo.obtenerIdTarea(filaModelo);
            if (tareaId != null && (Tarea.ESTADO_ERROR.equals(estado) || Tarea.ESTADO_CANCELADO.equals(estado))) {
                gestorTareas.reanudarTarea(tareaId);
                contador++;
            }
        }
        actualizarEstadisticas();
        mostrarMensaje("Tareas puestas en cola para reintentar: " + contador);
    }

    private void pausarTareas(int[] filas) {
        int contador = 0;
        for (int fila : filas) {
            int filaModelo = tabla.convertRowIndexToModel(fila);
            String estado = (String) modelo.getValueAt(filaModelo, 2);
            String tareaId = modelo.obtenerIdTarea(filaModelo);
            if (tareaId != null && (Tarea.ESTADO_EN_COLA.equals(estado) || Tarea.ESTADO_ANALIZANDO.equals(estado))) {
                gestorTareas.pausarTarea(tareaId);
                contador++;
            }
        }
        actualizarEstadisticas();
        mostrarMensaje("Tareas pausadas: " + contador);
    }

    private void reanudarTareas(int[] filas) {
        int contador = 0;
        for (int fila : filas) {
            int filaModelo = tabla.convertRowIndexToModel(fila);
            String estado = (String) modelo.getValueAt(filaModelo, 2);
            String tareaId = modelo.obtenerIdTarea(filaModelo);
            if (tareaId != null && Tarea.ESTADO_PAUSADO.equals(estado)) {
                gestorTareas.reanudarTarea(tareaId);
                contador++;
            }
        }
        actualizarEstadisticas();
        mostrarMensaje("Tareas reanudadas: " + contador);
    }

    private void cancelarTareas(int[] filas) {
        int total = 0;
        for (int fila : filas) {
            int filaModelo = tabla.convertRowIndexToModel(fila);
            String estado = (String) modelo.getValueAt(filaModelo, 2);
            if (Tarea.ESTADO_EN_COLA.equals(estado) || Tarea.ESTADO_ANALIZANDO.equals(estado) || Tarea.ESTADO_PAUSADO.equals(estado)) {
                total++;
            }
        }

        int confirmacion = JOptionPane.showConfirmDialog(
            this,
            "¿Cancelar " + total + " tarea(s) activa(s)?",
            "Confirmar cancelacion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (confirmacion != JOptionPane.YES_OPTION) return;

        int contador = 0;
        for (int fila : filas) {
            int filaModelo = tabla.convertRowIndexToModel(fila);
            String estado = (String) modelo.getValueAt(filaModelo, 2);
            String tareaId = modelo.obtenerIdTarea(filaModelo);
            if (tareaId != null && (Tarea.ESTADO_EN_COLA.equals(estado) || Tarea.ESTADO_ANALIZANDO.equals(estado) || Tarea.ESTADO_PAUSADO.equals(estado))) {
                gestorTareas.cancelarTarea(tareaId);
                contador++;
            }
        }
        actualizarEstadisticas();
        mostrarMensaje("Tareas canceladas: " + contador);
    }

    private void eliminarTareasSeleccionadas(int[] filas) {
        Integer[] filasOrdenadas = new Integer[filas.length];
        for (int i = 0; i < filas.length; i++) {
            filasOrdenadas[i] = filas[i];
        }
        java.util.Arrays.sort(filasOrdenadas, java.util.Collections.reverseOrder());

        int contador = 0;
        for (int fila : filasOrdenadas) {
            int filaModelo = tabla.convertRowIndexToModel(fila);
            String estado = (String) modelo.getValueAt(filaModelo, 2);
            String tareaId = modelo.obtenerIdTarea(filaModelo);
            if (tareaId != null && (Tarea.ESTADO_COMPLETADO.equals(estado) || Tarea.ESTADO_ERROR.equals(estado) || Tarea.ESTADO_CANCELADO.equals(estado))) {
                gestorTareas.limpiarTarea(tareaId);
                modelo.eliminarTarea(filaModelo);
                contador++;
            }
        }
        actualizarEstadisticas();
        mostrarMensaje("Tareas eliminadas: " + contador);
    }

    private void mostrarMensaje(String mensaje) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(PanelTareas.this, mensaje, "Informacion", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void actualizarEstadisticas() {
        SwingUtilities.invokeLater(() -> {
            int activas = modelo.contarPorEstado(Tarea.ESTADO_EN_COLA) +
                         modelo.contarPorEstado(Tarea.ESTADO_ANALIZANDO) +
                         modelo.contarPorEstado(Tarea.ESTADO_PAUSADO);
            int pausadas = modelo.contarPorEstado(Tarea.ESTADO_PAUSADO);
            int completadas = modelo.contarPorEstado(Tarea.ESTADO_COMPLETADO);
            int errores = modelo.contarPorEstado(Tarea.ESTADO_ERROR);

            etiquetaEstadisticas.setText(
                String.format("📊 Tareas Activas: %d | ✅ Completadas: %d | ❌ Con Errores: %d",
                    activas, completadas, errores)
            );

            // Actualizar botón Pausar/Reanudar según estado
            if (pausadas > 0) {
                botonPausarReanudar.setText("▶️ Reanudar Todo");
                botonPausarReanudar.setToolTipText("Reanudar todas las tareas pausadas");
            } else {
                botonPausarReanudar.setText("⏸️ Pausar Todo");
                botonPausarReanudar.setToolTipText("Pausar todas las tareas en análisis");
            }
        });
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
}
