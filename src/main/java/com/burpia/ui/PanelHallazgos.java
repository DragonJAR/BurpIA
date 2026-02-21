package com.burpia.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.scanner.AuditConfiguration;
import burp.api.montoya.scanner.BuiltInAuditConfiguration;
import burp.api.montoya.scanner.audit.Audit;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Hallazgo;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
    private final boolean esBurpProfessional;
    private JPanel panelFiltros;
    private JPanel panelGuardarProyecto;
    private JPanel panelTablaWrapper;
    private JLabel etiquetaBusqueda;

    private TableRowSorter<ModeloTablaHallazgos> sorter;
    private JCheckBox chkGuardarEnIssues;
    private volatile boolean guardadoAutomaticoIssuesActivo = true;

    public PanelHallazgos(MontoyaApi api) {
        this.api = api;
        this.esBurpProfessional = false;
        this.modelo = new ModeloTablaHallazgos(1000);
        this.tabla = new JTable(modelo);
        initComponents();
    }

    public PanelHallazgos(MontoyaApi api, ModeloTablaHallazgos modeloCompartido) {
        this(api, modeloCompartido, false);
    }

    public PanelHallazgos(MontoyaApi api, ModeloTablaHallazgos modeloCompartido, boolean esBurpProfessional) {
        this.api = api;
        this.esBurpProfessional = esBurpProfessional;
        this.modelo = modeloCompartido;
        this.tabla = new JTable(modelo);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panelFiltros = new JPanel();
        panelFiltros.setLayout(new BoxLayout(panelFiltros, BoxLayout.Y_AXIS));
        panelFiltros.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
                I18nUI.Hallazgos.TITULO_FILTROS(),
                TitledBorder.LEFT,
                TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JPanel panelTodosControles = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));

        etiquetaBusqueda = new JLabel(I18nUI.Hallazgos.ETIQUETA_BUSCAR());
        etiquetaBusqueda.setFont(EstilosUI.FUENTE_ESTANDAR);
        etiquetaBusqueda.setToolTipText(TooltipsUI.Hallazgos.BUSQUEDA());
        panelTodosControles.add(etiquetaBusqueda);
        campoBusqueda = new JTextField(15);
        campoBusqueda.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        campoBusqueda.setToolTipText(TooltipsUI.Hallazgos.BUSQUEDA());
        panelTodosControles.add(campoBusqueda);

        comboSeveridad = new JComboBox<>(new String[]{
            I18nUI.Hallazgos.OPCION_TODAS_CRITICIDADES(), "Critical", "High", "Medium", "Low", "Info"
        });
        comboSeveridad.setFont(EstilosUI.FUENTE_ESTANDAR);
        comboSeveridad.setToolTipText(TooltipsUI.Hallazgos.FILTRO_SEVERIDAD());
        panelTodosControles.add(comboSeveridad);

        botonLimpiarFiltro = new JButton(I18nUI.Hallazgos.BOTON_LIMPIAR());
        botonLimpiarFiltro.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonLimpiarFiltro.setToolTipText(TooltipsUI.Hallazgos.LIMPIAR_FILTROS());
        panelTodosControles.add(botonLimpiarFiltro);

        botonExportarCSV = new JButton(I18nUI.Hallazgos.BOTON_EXPORTAR_CSV());
        botonExportarCSV.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonExportarCSV.setToolTipText(TooltipsUI.Hallazgos.EXPORTAR_CSV());
        panelTodosControles.add(botonExportarCSV);

        botonExportarJSON = new JButton(I18nUI.Hallazgos.BOTON_EXPORTAR_JSON());
        botonExportarJSON.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonExportarJSON.setToolTipText(TooltipsUI.Hallazgos.EXPORTAR_JSON());
        panelTodosControles.add(botonExportarJSON);

        botonLimpiarTodo = new JButton(I18nUI.Hallazgos.BOTON_LIMPIAR_TODO());
        botonLimpiarTodo.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonLimpiarTodo.setToolTipText(TooltipsUI.Hallazgos.LIMPIAR_TODO());
        panelTodosControles.add(botonLimpiarTodo);

        panelFiltros.add(panelTodosControles);

        JPanel panelSuperior = new JPanel(new BorderLayout(10, 0));
        panelSuperior.add(panelFiltros, BorderLayout.CENTER);

        panelGuardarProyecto = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        panelGuardarProyecto.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
                I18nUI.Hallazgos.TITULO_GUARDAR_PROYECTO(),
                TitledBorder.LEFT,
                TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        chkGuardarEnIssues = new JCheckBox(I18nUI.Hallazgos.CHECK_GUARDAR_ISSUES());
        chkGuardarEnIssues.setSelected(true);
        chkGuardarEnIssues.setFont(EstilosUI.FUENTE_ESTANDAR);
        chkGuardarEnIssues.setToolTipText(TooltipsUI.Hallazgos.GUARDAR_ISSUES());
        chkGuardarEnIssues.addActionListener(e -> guardadoAutomaticoIssuesActivo = chkGuardarEnIssues.isSelected());

        panelGuardarProyecto.add(chkGuardarEnIssues);

        panelSuperior.add(panelGuardarProyecto, BorderLayout.EAST);

        panelTablaWrapper = new JPanel(new BorderLayout());
        panelTablaWrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
                I18nUI.Hallazgos.TITULO_TABLA(),
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
        tabla.setToolTipText(TooltipsUI.Hallazgos.TABLA());
        sorter = new TableRowSorter<>(modelo);
        tabla.setRowSorter(sorter);

        configurarColumnasTabla();

        JScrollPane panelDesplazable = new JScrollPane(tabla);
        panelDesplazable.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panelTablaWrapper.add(panelDesplazable, BorderLayout.CENTER);

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
                    I18nUI.Hallazgos.MSG_SIN_HALLAZGOS_LIMPIAR(),
                    I18nUI.Hallazgos.TITULO_INFORMACION(),
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            int confirmacion = JOptionPane.showConfirmDialog(
                this,
                I18nUI.Hallazgos.MSG_CONFIRMAR_LIMPIEZA(total),
                I18nUI.Hallazgos.TITULO_CONFIRMAR_LIMPIEZA(),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (confirmacion == JOptionPane.YES_OPTION) {
                modelo.limpiar();
            }
        });

        crearMenuContextual();
        aplicarIdioma();

        add(panelSuperior, BorderLayout.NORTH);
        add(panelTablaWrapper, BorderLayout.CENTER);
    }

    private void aplicarFiltros() {
        String textoBusqueda = campoBusqueda.getText().trim().toLowerCase();
        String severidadSeleccionada = (String) comboSeveridad.getSelectedItem();

        List<RowFilter<Object, Object>> filtros = new ArrayList<>();

        if (!textoBusqueda.isEmpty()) {
            filtros.add(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(textoBusqueda), 1, 2));
        }

        if (severidadSeleccionada != null && comboSeveridad.getSelectedIndex() > 0) {
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
        comboSeveridad.setSelectedIndex(0);
        sorter.setRowFilter(null);
    }

    private void exportarCSV() {
        JFileChooser selectorArchivos = new JFileChooser();
        selectorArchivos.setDialogTitle(I18nUI.Hallazgos.DIALOGO_EXPORTAR_CSV());
        String nombreArchivo = "burpia_hallazgos_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
            ".csv";
        selectorArchivos.setSelectedFile(new File(nombreArchivo));

        int resultado = selectorArchivos.showSaveDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File archivo = selectorArchivos.getSelectedFile();
            try (BufferedWriter writer = Files.newBufferedWriter(archivo.toPath(), StandardCharsets.UTF_8)) {
                writer.write(I18nUI.Hallazgos.CSV_HEADER() + "\n");

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

                String mensaje = I18nUI.Hallazgos.MSG_EXPORTACION_EXITOSA(
                    hallazgosExportar.size(),
                    archivo.getName(),
                    ignorados
                );
                JOptionPane.showMessageDialog(
                    this,
                    mensaje,
                    I18nUI.Hallazgos.TITULO_EXPORTACION_EXITOSA(),
                    JOptionPane.INFORMATION_MESSAGE
                );

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    I18nUI.Hallazgos.MSG_ERROR_EXPORTAR(e.getMessage()),
                    I18nUI.Hallazgos.TITULO_ERROR_EXPORTACION(),
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportarJSON() {
        JFileChooser selectorArchivos = new JFileChooser();
        selectorArchivos.setDialogTitle(I18nUI.Hallazgos.DIALOGO_EXPORTAR_JSON());
        String nombreArchivo = "burpia_hallazgos_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
            ".json";
        selectorArchivos.setSelectedFile(new File(nombreArchivo));

        int resultado = selectorArchivos.showSaveDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File archivo = selectorArchivos.getSelectedFile();
            try (BufferedWriter writer = Files.newBufferedWriter(archivo.toPath(), StandardCharsets.UTF_8)) {
                writer.write("{\n  \"hallazgos\": [\n");

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

                String mensaje = I18nUI.Hallazgos.MSG_EXPORTACION_EXITOSA(
                    hallazgosExportar.size(),
                    archivo.getName(),
                    ignorados
                );
                JOptionPane.showMessageDialog(
                    this,
                    mensaje,
                    I18nUI.Hallazgos.TITULO_EXPORTACION_EXITOSA(),
                    JOptionPane.INFORMATION_MESSAGE
                );

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    I18nUI.Hallazgos.MSG_ERROR_EXPORTAR(e.getMessage()),
                    I18nUI.Hallazgos.TITULO_ERROR_EXPORTACION(),
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String escapeJson(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    public void agregarHallazgo(Hallazgo hallazgo) {
        modelo.agregarHallazgo(hallazgo);
    }

    public void limpiar() {
        SwingUtilities.invokeLater(() -> {
            modelo.limpiar();
        });
    }

    private void crearMenuContextual() {
        JPopupMenu menuContextual = new JPopupMenu();

        JMenuItem menuItemRepeater = new JMenuItem(I18nUI.Hallazgos.MENU_ENVIAR_REPEATER());
        menuItemRepeater.setFont(EstilosUI.FUENTE_ESTANDAR);
        menuItemRepeater.setToolTipText(TooltipsUI.Hallazgos.MENU_REPEATER());
        menuItemRepeater.addActionListener(e -> enviarARepeater(tabla.getSelectedRows()));
        menuContextual.add(menuItemRepeater);

        JMenuItem menuItemIntruder = new JMenuItem(I18nUI.Hallazgos.MENU_ENVIAR_INTRUDER());
        menuItemIntruder.setFont(EstilosUI.FUENTE_ESTANDAR);
        menuItemIntruder.setToolTipText(TooltipsUI.Hallazgos.MENU_INTRUDER());
        menuItemIntruder.addActionListener(e -> enviarAIntruder(tabla.getSelectedRows()));
        menuContextual.add(menuItemIntruder);

        if (esBurpProfessional) {
            JMenuItem menuItemScanner = new JMenuItem(I18nUI.Hallazgos.MENU_ENVIAR_SCANNER());
            menuItemScanner.setFont(EstilosUI.FUENTE_ESTANDAR);
            menuItemScanner.setToolTipText(TooltipsUI.Hallazgos.MENU_SCANNER());
            menuItemScanner.addActionListener(e -> enviarAScanner(tabla.getSelectedRows()));
            menuContextual.add(menuItemScanner);
        }

        menuContextual.addSeparator();

        JMenuItem menuItemIgnorar = new JMenuItem(I18nUI.Hallazgos.MENU_IGNORAR());
        menuItemIgnorar.setFont(EstilosUI.FUENTE_ESTANDAR);
        menuItemIgnorar.setToolTipText(TooltipsUI.Hallazgos.MENU_IGNORAR());
        menuItemIgnorar.addActionListener(e -> {
            int[] filas = tabla.getSelectedRows();
            if (filas.length > 0) {
                int confirmacion = JOptionPane.showConfirmDialog(
                    PanelHallazgos.this,
                    I18nUI.Hallazgos.MSG_CONFIRMAR_IGNORAR(filas.length),
                    I18nUI.Hallazgos.TITULO_CONFIRMAR_IGNORAR(),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );
                if (confirmacion == JOptionPane.YES_OPTION) {
                    ignorarHallazgos(filas);
                }
            }
        });
        menuContextual.add(menuItemIgnorar);

        JMenuItem menuItemBorrar = new JMenuItem(I18nUI.Hallazgos.MENU_BORRAR());
        menuItemBorrar.setFont(EstilosUI.FUENTE_ESTANDAR);
        menuItemBorrar.setToolTipText(TooltipsUI.Hallazgos.MENU_BORRAR());
        menuItemBorrar.addActionListener(e -> {
            int[] filas = tabla.getSelectedRows();
            if (filas.length > 0) {
                int confirmacion = JOptionPane.showConfirmDialog(
                    PanelHallazgos.this,
                    I18nUI.Hallazgos.MSG_CONFIRMAR_BORRAR(filas.length),
                    I18nUI.Hallazgos.TITULO_CONFIRMAR_BORRAR(),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                if (confirmacion == JOptionPane.YES_OPTION) {
                    borrarHallazgosDeTabla(filas);
                }
            }
        });
        menuContextual.add(menuItemBorrar);

        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int filaVista = tabla.rowAtPoint(e.getPoint());
                    if (filaVista >= 0) {
                        abrirDialogoEdicion(filaVista);
                    }
                } else {
                    mostrarMenuContextualSiAplica(e, menuContextual);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mostrarMenuContextualSiAplica(e, menuContextual);
            }
        });
    }

    private void abrirDialogoEdicion(int filaVista) {
        int filaModelo = tabla.convertRowIndexToModel(filaVista);
        Hallazgo hallazgo = modelo.obtenerHallazgo(filaModelo);
        if (hallazgo != null) {
            Window windowAncestral = SwingUtilities.getWindowAncestor(this);
            DialogoDetalleHallazgo dialogo = new DialogoDetalleHallazgo(windowAncestral, hallazgo, hallazgoEditado -> {
                modelo.actualizarHallazgo(filaModelo, hallazgoEditado);
            });
            dialogo.setVisible(true);
        }
    }

    private void mostrarMenuContextualSiAplica(MouseEvent e, JPopupMenu menuContextual) {
        if (!SwingUtilities.isRightMouseButton(e)) {
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
        menuContextual.show(tabla, e.getX(), e.getY());
    }

    private void enviarARepeater(int[] filas) {
        ejecutarAccionBurp(
            filas,
            "BurpIA-Repeater",
            I18nUI.Hallazgos.TITULO_ACCION_REPEATER(),
            I18nUI.Hallazgos.RESUMEN_ACCION_REPEATER(),
            (solicitud, hallazgo) -> {
                String nombreTab = "BurpIA-" + (hallazgo != null ? hallazgo.obtenerSeveridad() : "Hallazgo");
                api.repeater().sendToRepeater(solicitud, nombreTab);
                return "✅ " + solicitud.url();
            }
        );
    }

    private void enviarAIntruder(int[] filas) {
        ejecutarAccionBurp(
            filas,
            "BurpIA-Intruder",
            I18nUI.Hallazgos.TITULO_ACCION_INTRUDER(),
            I18nUI.Hallazgos.RESUMEN_ACCION_INTRUDER(),
            (solicitud, hallazgo) -> {
                api.intruder().sendToIntruder(solicitud);
                return "✅ " + solicitud.url() + " " + I18nUI.tr("(enviado a Intruder)", "(sent to Intruder)");
            }
        );
    }

    private void enviarAScanner(int[] filas) {
        ejecutarAccionBurp(
            filas,
            "BurpIA-Scanner",
            I18nUI.Hallazgos.TITULO_ACCION_SCANNER(),
            I18nUI.Hallazgos.RESUMEN_ACCION_SCANNER(),
            new AccionSobreSolicitud() {
                private Audit auditoriaActiva;

                @Override
                public String ejecutar(HttpRequest solicitud, Hallazgo hallazgo) {
                    if (!esBurpProfessional) {
                        throw new IllegalStateException("Scanner solo disponible en Burp Professional");
                    }
                    if (auditoriaActiva == null) {
                        AuditConfiguration configScanner = AuditConfiguration.auditConfiguration(
                            BuiltInAuditConfiguration.LEGACY_ACTIVE_AUDIT_CHECKS
                        );
                        auditoriaActiva = api.scanner().startAudit(configScanner);
                    }
                    auditoriaActiva.addRequest(solicitud);
                    api.logging().logToOutput(I18nUI.tr(
                        "[BurpIA] Peticion enviada a Scanner Pro: ",
                        "[BurpIA] Request sent to Scanner Pro: "
                    ) + solicitud.url());
                    return "✅ " + solicitud.url() + " " + I18nUI.tr("(enviado a Scanner Pro)", "(sent to Scanner Pro)");
                }
            }
        );
    }

    private void ejecutarAccionBurp(int[] filas,
                                    String nombreHilo,
                                    String titulo,
                                    String resumen,
                                    AccionSobreSolicitud accion) {
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
                    detalle.append("⚠️ ").append(hallazgo != null ? hallazgo.obtenerUrl() : I18nUI.tr("URL desconocida", "Unknown URL"))
                        .append(" ").append(I18nUI.Hallazgos.MSG_SIN_REQUEST()).append("\n");
                    continue;
                }

                try {
                    String resultado = accion.ejecutar(solicitud, hallazgo);
                    exitosos++;
                    detalle.append(resultado).append("\n");
                } catch (Exception ex) {
                    String mensaje = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                    detalle.append("❌ ").append(solicitud.url()).append(" (error: ").append(mensaje).append(")\n");
                    if (titulo.contains("Scanner")) {
                        api.logging().logToError(I18nUI.tr(
                            "[BurpIA] Error al enviar a Scanner Pro: ",
                            "[BurpIA] Error sending to Scanner Pro: "
                        ) + mensaje);
                    }
                }
            }

            final int totalExitosos = exitosos;
            final int totalSinRequest = sinRequest;
            final String detalleFinal = detalle.toString();

            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                    PanelHallazgos.this,
                    I18nUI.Hallazgos.MSG_RESULTADO_ACCION(
                        resumen,
                        totalExitosos,
                        filas.length,
                        totalSinRequest,
                        detalleFinal
                    ),
                    titulo,
                    totalExitosos > 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE
                );
            });
        }, nombreHilo).start();
    }

    @FunctionalInterface
    private interface AccionSobreSolicitud {
        String ejecutar(HttpRequest solicitud, Hallazgo hallazgo) throws Exception;
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
                I18nUI.Hallazgos.MSG_HALLAZGOS_IGNORADOS(ignorados),
                I18nUI.Hallazgos.TITULO_HALLAZGOS_IGNORADOS(),
                JOptionPane.INFORMATION_MESSAGE
            );
        });
    }

    private void borrarHallazgosDeTabla(int[] filas) {
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
                I18nUI.Hallazgos.MSG_HALLAZGOS_BORRADOS(borrados[0]),
                I18nUI.Hallazgos.TITULO_HALLAZGOS_BORRADOS(),
                JOptionPane.INFORMATION_MESSAGE
            );
        });
    }

    public void aplicarIdioma() {
        etiquetaBusqueda.setText(I18nUI.Hallazgos.ETIQUETA_BUSCAR());
        botonLimpiarFiltro.setText(I18nUI.Hallazgos.BOTON_LIMPIAR());
        botonExportarCSV.setText(I18nUI.Hallazgos.BOTON_EXPORTAR_CSV());
        botonExportarJSON.setText(I18nUI.Hallazgos.BOTON_EXPORTAR_JSON());
        botonLimpiarTodo.setText(I18nUI.Hallazgos.BOTON_LIMPIAR_TODO());
        chkGuardarEnIssues.setText(I18nUI.Hallazgos.CHECK_GUARDAR_ISSUES());
        actualizarPrimeraOpcionSeveridad();

        etiquetaBusqueda.setToolTipText(TooltipsUI.Hallazgos.BUSQUEDA());
        campoBusqueda.setToolTipText(TooltipsUI.Hallazgos.BUSQUEDA());
        comboSeveridad.setToolTipText(TooltipsUI.Hallazgos.FILTRO_SEVERIDAD());
        botonLimpiarFiltro.setToolTipText(TooltipsUI.Hallazgos.LIMPIAR_FILTROS());
        botonExportarCSV.setToolTipText(TooltipsUI.Hallazgos.EXPORTAR_CSV());
        botonExportarJSON.setToolTipText(TooltipsUI.Hallazgos.EXPORTAR_JSON());
        botonLimpiarTodo.setToolTipText(TooltipsUI.Hallazgos.LIMPIAR_TODO());
        chkGuardarEnIssues.setToolTipText(TooltipsUI.Hallazgos.GUARDAR_ISSUES());
        tabla.setToolTipText(TooltipsUI.Hallazgos.TABLA());

        actualizarTituloPanel(panelFiltros, I18nUI.Hallazgos.TITULO_FILTROS());
        actualizarTituloPanel(panelGuardarProyecto, I18nUI.Hallazgos.TITULO_GUARDAR_PROYECTO());
        actualizarTituloPanel(panelTablaWrapper, I18nUI.Hallazgos.TITULO_TABLA());

        modelo.refrescarColumnasIdioma();
        SwingUtilities.invokeLater(() -> {
            sorter = new TableRowSorter<>(modelo);
            tabla.setRowSorter(sorter);
            configurarColumnasTabla();
            aplicarFiltros();
        });
        revalidate();
        repaint();
    }

    private void actualizarPrimeraOpcionSeveridad() {
        boolean estabaSeleccionadoPrimero = comboSeveridad.getSelectedIndex() == 0;
        comboSeveridad.removeItemAt(0);
        comboSeveridad.insertItemAt(I18nUI.Hallazgos.OPCION_TODAS_CRITICIDADES(), 0);
        if (estabaSeleccionadoPrimero || comboSeveridad.getSelectedIndex() < 0) {
            comboSeveridad.setSelectedIndex(0);
        }
    }

    private void configurarColumnasTabla() {
        if (tabla.getColumnModel().getColumnCount() < 5) {
            return;
        }
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

        tabla.getColumnModel().getColumn(0).setPreferredWidth(80);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(300);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(400);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(100);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(100);
    }

    private void actualizarTituloPanel(JPanel panel, String titulo) {
        Border borde = panel.getBorder();
        if (borde instanceof CompoundBorder) {
            Border bordeExterno = ((CompoundBorder) borde).getOutsideBorder();
            if (bordeExterno instanceof TitledBorder) {
                ((TitledBorder) bordeExterno).setTitle(titulo);
            }
        }
    }

    public ModeloTablaHallazgos obtenerModelo() {
        return modelo;
    }

    public boolean isGuardadoAutomaticoIssuesActivo() {
        return guardadoAutomaticoIssuesActivo;
    }
}
