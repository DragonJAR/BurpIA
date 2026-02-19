package com.burpia.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.burpia.model.Hallazgo;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PanelHallazgos extends JPanel {
    private final ModeloTablaHallazgos modelo;
    private final JTable tabla;
    private JTextField campoBusqueda;
    private JComboBox<String> comboSeveridad;
    private JButton botonLimpiarFiltro;
    private JButton botonExportarCSV;
    private JButton botonExportarJSON;
    private JButton botonLimpiarTodo;
    private final MontoyaApi api;

    private TableRowSorter<ModeloTablaHallazgos> sorter;

    // Umbral de ancho para cambiar de layout (en pixeles)
    private static final int UMBRAL_RESPONSIVE = 900;  // Aumentado para mejor uso del espacio

    public PanelHallazgos(MontoyaApi api) {
        this.api = api;
        this.modelo = new ModeloTablaHallazgos(1000);
        this.tabla = new JTable(modelo);
        initComponents();
    }

    public PanelHallazgos(MontoyaApi api, ModeloTablaHallazgos modeloCompartido) {
        this.api = api;
        this.modelo = modeloCompartido;
        this.tabla = new JTable(modelo);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel panelFiltros = new JPanel();
        panelFiltros.setLayout(new BoxLayout(panelFiltros, BoxLayout.Y_AXIS));
        panelFiltros.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
            "🔭 FILTROS Y BÚSQUEDA",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            EstilosUI.FUENTE_NEGRITA
        ));

        JPanel panelTodosControles = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5)) {
            private boolean ultimoLayoutHorizontal = true;

            @Override
            public void doLayout() {
                int ancho = getWidth() - 40;
                boolean esLayoutHorizontal = ancho >= UMBRAL_RESPONSIVE;

                if (esLayoutHorizontal != ultimoLayoutHorizontal) {
                    if (esLayoutHorizontal) {
                        // Espacio suficiente: una sola fila con todos los elementos
                        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
                    } else {
                        // Espacio limitado: 2 filas con 3 elementos cada una
                        setLayout(new GridLayout(2, 3, 5, 5));
                    }
                    ultimoLayoutHorizontal = esLayoutHorizontal;
                }

                super.doLayout();
            }
        };

        // Primera fila: búsqueda, severidad, limpiar filtros
        JLabel etiquetaBusqueda = new JLabel("🔎 Buscar:");
        etiquetaBusqueda.setFont(EstilosUI.FUENTE_ESTANDAR);
        panelTodosControles.add(etiquetaBusqueda);
        campoBusqueda = new JTextField(30);
        campoBusqueda.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        campoBusqueda.setToolTipText("Buscar por texto en URL o hallazgo");
        panelTodosControles.add(campoBusqueda);

        JLabel etiquetaSeveridad = new JLabel("⚡ Severidad:");
        etiquetaSeveridad.setFont(EstilosUI.FUENTE_ESTANDAR);
        panelTodosControles.add(etiquetaSeveridad);
        comboSeveridad = new JComboBox<>(new String[]{
            "Todas", "Critical", "High", "Medium", "Low", "Info"
        });
        comboSeveridad.setFont(EstilosUI.FUENTE_ESTANDAR);
        panelTodosControles.add(comboSeveridad);

        botonLimpiarFiltro = new JButton("❌ Limpiar Filtros");
        botonLimpiarFiltro.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonLimpiarFiltro.setToolTipText("Quitar todos los filtros activos");
        panelTodosControles.add(botonLimpiarFiltro);

        // Segunda fila: exportar CSV, exportar JSON, limpiar todo
        botonExportarCSV = new JButton("📄 Exportar CSV");
        botonExportarCSV.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonExportarCSV.setToolTipText("Exportar hallazgos a archivo CSV");
        panelTodosControles.add(botonExportarCSV);

        botonExportarJSON = new JButton("📋 Exportar JSON");
        botonExportarJSON.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonExportarJSON.setToolTipText("Exportar hallazgos a archivo JSON");
        panelTodosControles.add(botonExportarJSON);

        botonLimpiarTodo = new JButton("🗑️ Limpiar Todo");
        botonLimpiarTodo.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonLimpiarTodo.setToolTipText("Eliminar todos los hallazgos");
        panelTodosControles.add(botonLimpiarTodo);

        panelFiltros.add(panelTodosControles);

        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.add(panelFiltros, BorderLayout.NORTH);

        JPanel panelTablaWrapper = new JPanel(new BorderLayout());
        panelTablaWrapper.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
            "💎 HALLAZGOS DE SEGURIDAD",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            EstilosUI.FUENTE_NEGRITA
        ));

        tabla.setAutoCreateRowSorter(true);
        tabla.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tabla.setRowHeight(EstilosUI.ALTURA_FILA_TABLA);
        tabla.setFont(EstilosUI.FUENTE_TABLA);
        sorter = new TableRowSorter<>(modelo);
        tabla.setRowSorter(sorter);

        // Configurar renderizadores con wrapper para hallazgos borrados
        tabla.getColumnModel().getColumn(0).setCellRenderer(
            new RenderizadorHallazgoBorrado(new RenderizadorCentrado(), tabla, modelo)
        );
        tabla.getColumnModel().getColumn(1).setCellRenderer(
            new RenderizadorHallazgoBorrado(new DefaultTableCellRenderer(), tabla, modelo)
        );
        tabla.getColumnModel().getColumn(2).setCellRenderer(
            new RenderizadorHallazgoBorrado(new DefaultTableCellRenderer(), tabla, modelo)
        );
        tabla.getColumnModel().getColumn(3).setCellRenderer(
            new RenderizadorHallazgoBorrado(new RenderizadorSeveridad(), tabla, modelo)
        );
        tabla.getColumnModel().getColumn(4).setCellRenderer(
            new RenderizadorHallazgoBorrado(new RenderizadorConfianza(), tabla, modelo)
        );

        // Ajustar anchos de columna
        tabla.getColumnModel().getColumn(0).setPreferredWidth(80);  // Hora
        tabla.getColumnModel().getColumn(1).setPreferredWidth(300); // URL
        tabla.getColumnModel().getColumn(2).setPreferredWidth(400); // Hallazgo
        tabla.getColumnModel().getColumn(3).setPreferredWidth(100); // Severidad
        tabla.getColumnModel().getColumn(4).setPreferredWidth(100); // Confianza

        JScrollPane panelDesplazable = new JScrollPane(tabla);
        panelDesplazable.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panelTablaWrapper.add(panelDesplazable, BorderLayout.CENTER);

        // Manejadores de eventos
        campoBusqueda.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { aplicarFiltros(); }
            public void insertUpdate(DocumentEvent e) { aplicarFiltros(); }
            public void removeUpdate(DocumentEvent e) { aplicarFiltros(); }
        });

        comboSeveridad.addActionListener(e -> aplicarFiltros());

        botonLimpiarFiltro.addActionListener(e -> limpiarFiltros());

        botonExportarCSV.addActionListener(e -> exportarCSV());

        botonExportarJSON.addActionListener(e -> exportarJSON());

        botonLimpiarTodo.addActionListener(e -> {
            int total = modelo.getRowCount();
            if (total == 0) {
                JOptionPane.showMessageDialog(this,
                    "No hay hallazgos para limpiar.",
                    "Información",
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            int confirmacion = JOptionPane.showConfirmDialog(
                this,
                "¿Estás seguro de que deseas limpiar todos los hallazgos (" + total + ")?",
                "Confirmar limpieza",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (confirmacion == JOptionPane.YES_OPTION) {
                modelo.limpiar();
            }
        });

        // Crear menú contextual para hallazgos individuales
        crearMenuContextual();

        add(panelSuperior, BorderLayout.NORTH);
        add(panelTablaWrapper, BorderLayout.CENTER);
    }

    private void aplicarFiltros() {
        String textoBusqueda = campoBusqueda.getText().trim().toLowerCase();
        String severidadSeleccionada = (String) comboSeveridad.getSelectedItem();

        List<RowFilter<Object, Object>> filtros = new ArrayList<>();

        // Filtro de texto
        if (!textoBusqueda.isEmpty()) {
            filtros.add(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(textoBusqueda), 1, 2)); // URL y Hallazgo
        }

        // Filtro de severidad
        if (severidadSeleccionada != null && !severidadSeleccionada.equals("Todas")) {
            filtros.add(RowFilter.regexFilter("^" + severidadSeleccionada + "$", 3));
        }

        if (!filtros.isEmpty()) {
            sorter.setRowFilter(RowFilter.andFilter(filtros));
        } else {
            sorter.setRowFilter(null);
        }
    }

    private void limpiarFiltros() {
        campoBusqueda.setText("");
        comboSeveridad.setSelectedItem("Todas");
        sorter.setRowFilter(null);
    }

    private void exportarCSV() {
        JFileChooser selectorArchivos = new JFileChooser();
        selectorArchivos.setDialogTitle("Exportar hallazgos a CSV");
        String nombreArchivo = "burpia_hallazgos_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
            ".csv";
        selectorArchivos.setSelectedFile(new File(nombreArchivo));

        int resultado = selectorArchivos.showSaveDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File archivo = selectorArchivos.getSelectedFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo))) {
                writer.write("Hora,URL,Hallazgo,Severidad,Confianza\n");

                // Exportar solo hallazgos NO ignorados
                List<Hallazgo> hallazgosExportar = modelo.obtenerHallazgosNoIgnorados();
                int totalEnTabla = modelo.getRowCount();
                int ignorados = totalEnTabla - hallazgosExportar.size();

                for (Hallazgo h : hallazgosExportar) {
                    String[] valores = {
                        h.obtenerHoraDescubrimiento(),
                        h.obtenerUrl(),
                        h.obtenerHallazgo(),
                        h.obtenerSeveridad(),
                        h.obtenerConfianza()
                    };

                    for (int j = 0; j < valores.length; j++) {
                        String texto = valores[j] != null ? valores[j] : "";
                        if (texto.contains(",") || texto.contains("\"")) {
                            texto = "\"" + texto.replace("\"", "\"\"") + "\"";
                        }
                        writer.write(texto);
                        if (j < valores.length - 1) {
                            writer.write(",");
                        }
                    }
                    writer.write("\n");
                }

                String mensaje = "Se exportaron " + hallazgosExportar.size() + " hallazgos a " + archivo.getName();
                if (ignorados > 0) {
                    mensaje += "\n(Ignorados y no exportados: " + ignorados + ")";
                }
                JOptionPane.showMessageDialog(this, mensaje, "Exportacion exitosa", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Error al exportar: " + e.getMessage(),
                    "Error de exportacion",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportarJSON() {
        JFileChooser selectorArchivos = new JFileChooser();
        selectorArchivos.setDialogTitle("Exportar hallazgos a JSON");
        String nombreArchivo = "burpia_hallazgos_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
            ".json";
        selectorArchivos.setSelectedFile(new File(nombreArchivo));

        int resultado = selectorArchivos.showSaveDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File archivo = selectorArchivos.getSelectedFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(archivo))) {
                writer.write("{\n  \"hallazgos\": [\n");

                // Exportar solo hallazgos NO ignorados
                List<Hallazgo> hallazgosExportar = modelo.obtenerHallazgosNoIgnorados();
                int totalEnTabla = modelo.getRowCount();
                int ignorados = totalEnTabla - hallazgosExportar.size();

                for (int i = 0; i < hallazgosExportar.size(); i++) {
                    Hallazgo h = hallazgosExportar.get(i);
                    writer.write("    {\n");
                    writer.write("      \"hora\": \"" + h.obtenerHoraDescubrimiento() + "\",\n");
                    writer.write("      \"url\": \"" + escapeJson(h.obtenerUrl()) + "\",\n");
                    writer.write("      \"hallazgo\": \"" + escapeJson(h.obtenerHallazgo()) + "\",\n");
                    writer.write("      \"severidad\": \"" + h.obtenerSeveridad() + "\",\n");
                    writer.write("      \"confianza\": \"" + h.obtenerConfianza() + "\"\n");
                    writer.write("    }");
                    if (i < hallazgosExportar.size() - 1) {
                        writer.write(",");
                    }
                    writer.write("\n");
                }

                writer.write("  ]\n}\n");

                String mensaje = "Se exportaron " + hallazgosExportar.size() + " hallazgos a " + archivo.getName();
                if (ignorados > 0) {
                    mensaje += "\n(Ignorados y no exportados: " + ignorados + ")";
                }
                JOptionPane.showMessageDialog(this, mensaje, "Exportacion exitosa", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Error al exportar: " + e.getMessage(),
                    "Error de exportacion",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String escapeJson(String texto) {
        return texto.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    public void agregarHallazgo(Hallazgo hallazgo) {
        SwingUtilities.invokeLater(() -> {
            modelo.agregarHallazgo(hallazgo);
        });
    }

    public void limpiar() {
        SwingUtilities.invokeLater(() -> {
            modelo.limpiar();
        });
    }

    private void crearMenuContextual() {
        JPopupMenu menuContextual = new JPopupMenu();

        // === ACCIONES DE BURP SUITE ===
        JMenuItem menuItemRepeater = new JMenuItem("📤 Enviar al Repeater");
        menuItemRepeater.setFont(EstilosUI.FUENTE_ESTANDAR);
        menuItemRepeater.setToolTipText("Enviar la peticion al Repeater de Burp Suite para validacion manual");
        menuItemRepeater.addActionListener(e -> enviarARepeater(tabla.getSelectedRows()));
        menuContextual.add(menuItemRepeater);

        JMenuItem menuItemIntruder = new JMenuItem("🔍 Enviar a Intruder");
        menuItemIntruder.setFont(EstilosUI.FUENTE_ESTANDAR);
        menuItemIntruder.setToolTipText("Enviar la peticion a Intruder para pruebas de fuzzing");
        menuItemIntruder.addActionListener(e -> enviarAScanner(tabla.getSelectedRows()));
        menuContextual.add(menuItemIntruder);

        menuContextual.addSeparator();

        // === GESTION DE HALLAZGOS ===
        JMenuItem menuItemIgnorar = new JMenuItem("🚫 Ignorar");
        menuItemIgnorar.setFont(EstilosUI.FUENTE_ESTANDAR);
        menuItemIgnorar.setToolTipText("Marcar como ignorado (no se incluye en exportaciones)");
        menuItemIgnorar.addActionListener(e -> {
            int[] filas = tabla.getSelectedRows();
            if (filas.length > 0) {
                int confirmacion = JOptionPane.showConfirmDialog(
                    PanelHallazgos.this,
                    filas.length == 1 ? "¿Ignorar este hallazgo?" : "¿Ignorar " + filas.length + " hallazgos?",
                    "Confirmar ignorar",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );
                if (confirmacion == JOptionPane.YES_OPTION) {
                    ignorarHallazgos(filas);
                }
            }
        });
        menuContextual.add(menuItemIgnorar);

        JMenuItem menuItemBorrar = new JMenuItem("🗑️ Borrar");
        menuItemBorrar.setFont(EstilosUI.FUENTE_ESTANDAR);
        menuItemBorrar.setToolTipText("Eliminar hallazgos de la tabla");
        menuItemBorrar.addActionListener(e -> {
            int[] filas = tabla.getSelectedRows();
            if (filas.length > 0) {
                int confirmacion = JOptionPane.showConfirmDialog(
                    PanelHallazgos.this,
                    filas.length == 1 ? "¿Borrar este hallazgo de la tabla?" : "¿Borrar " + filas.length + " hallazgos de la tabla?",
                    "Confirmar borrado",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                if (confirmacion == JOptionPane.YES_OPTION) {
                    borrarHallazgosDeTabla(filas);
                }
            }
        });
        menuContextual.add(menuItemBorrar);

        // === MOUSE LISTENER ===
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
                        menuContextual.show(tabla, e.getX(), e.getY());
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
                        menuContextual.show(tabla, e.getX(), e.getY());
                    }
                }
            }
        });
    }

    private void enviarARepeater(int[] filas) {
        // CRÍTICO: NO llamar api.repeater() desde EDT (Montoya API rule)
        // Ejecutar en thread separado para evitar deadlock de UI
        new Thread(() -> {
            int exitosos = 0;
            int sinRequest = 0;
            StringBuilder detalle = new StringBuilder();

            for (int fila : filas) {
                int filaModelo = tabla.convertRowIndexToModel(fila);
                HttpRequest solicitud = modelo.obtenerSolicitudHttp(filaModelo);
                Hallazgo hallazgo = modelo.obtenerHallazgo(filaModelo);

                if (solicitud == null) {
                    sinRequest++;
                    detalle.append("⚠️ ").append(hallazgo != null ? hallazgo.obtenerUrl() : "URL desconocida")
                        .append(" (sin request)\n");
                    continue;
                }

                try {
                    String nombreTab = "BurpIA-" + (hallazgo != null ? hallazgo.obtenerSeveridad() : "Hallazgo");
                    api.repeater().sendToRepeater(solicitud, nombreTab);
                    exitosos++;
                    detalle.append("✅ ").append(solicitud.url()).append("\n");
                } catch (Exception ex) {
                    detalle.append("❌ ").append(solicitud.url()).append(" (error: ").append(ex.getMessage()).append(")\n");
                }
            }

            final int totalExitosos = exitosos;
            final int totalSinRequest = sinRequest;
            final String detalleFinal = detalle.toString();

            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                    PanelHallazgos.this,
                    "Enviados al Repeater: " + totalExitosos + "/" + filas.length +
                    (totalSinRequest > 0 ? "\nSin request original: " + totalSinRequest : "") +
                    "\n\n" + detalleFinal,
                    "Enviar al Repeater",
                    totalExitosos > 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE
                );
            });
        }, "BurpIA-Repeater").start();
    }

    private void enviarAScanner(int[] filas) {
        new Thread(() -> {
            int exitosos = 0;
            int sinRequest = 0;
            int noDisponible = 0;
            StringBuilder detalle = new StringBuilder();

            for (int fila : filas) {
                int filaModelo = tabla.convertRowIndexToModel(fila);
                HttpRequest solicitud = modelo.obtenerSolicitudHttp(filaModelo);
                Hallazgo hallazgo = modelo.obtenerHallazgo(filaModelo);

                if (solicitud == null) {
                    sinRequest++;
                    detalle.append("⚠️ ").append(hallazgo != null ? hallazgo.obtenerUrl() : "URL desconocida")
                        .append(" (sin request)\n");
                    continue;
                }

                try {
                    api.intruder().sendToIntruder(solicitud);
                    exitosos++;
                    detalle.append("✅ ").append(solicitud.url()).append(" (enviado a Intruder)\n");
                } catch (Exception ex) {
                    detalle.append("❌ ").append(solicitud.url()).append(" (error: ").append(ex.getMessage()).append(")\n");
                }
            }

            final int totalExitosos = exitosos;
            final int totalSinRequest = sinRequest;
            final String detalleFinal = detalle.toString();

            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                    PanelHallazgos.this,
                    "Enviados a Intruder: " + totalExitosos + "/" + filas.length +
                    (totalSinRequest > 0 ? "\nSin request original: " + totalSinRequest : "") +
                    "\n\n" +
                    detalleFinal,
                    "Enviar a Intruder",
                    totalExitosos > 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE
                );
            });
        }, "BurpIA-Scanner").start();
    }

    private void ignorarHallazgos(int[] filas) {
        for (int fila : filas) {
            int filaModelo = tabla.convertRowIndexToModel(fila);
            modelo.marcarComoIgnorado(filaModelo);
        }

        final int ignorados = filas.length;
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                PanelHallazgos.this,
                "Hallazgos ignorados: " + ignorados + "\n\nNo se incluiran en exportaciones CSV/JSON.",
                "Hallazgos ignorados",
                JOptionPane.INFORMATION_MESSAGE
            );
        });
    }

    private void borrarHallazgosDeTabla(int[] filas) {
        // Ordenar de mayor a menor para no afectar índices
        Integer[] filasOrdenadas = new Integer[filas.length];
        for (int i = 0; i < filas.length; i++) {
            filasOrdenadas[i] = filas[i];
        }
        java.util.Arrays.sort(filasOrdenadas, java.util.Collections.reverseOrder());

        final int[] borrados = {0};
        for (int fila : filasOrdenadas) {
            int filaModelo = tabla.convertRowIndexToModel(fila);
            if (filaModelo >= 0) {
                modelo.eliminarHallazgo(filaModelo);
                borrados[0]++;
            }
        }

        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                PanelHallazgos.this,
                "Hallazgos eliminados: " + borrados[0],
                "Hallazgos borrados",
                JOptionPane.INFORMATION_MESSAGE
            );
        });
    }

    public ModeloTablaHallazgos obtenerModelo() {
        return modelo;
    }
}
