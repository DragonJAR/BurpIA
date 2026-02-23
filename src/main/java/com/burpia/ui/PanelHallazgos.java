package com.burpia.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.scanner.AuditConfiguration;
import burp.api.montoya.scanner.BuiltInAuditConfiguration;
import burp.api.montoya.scanner.audit.Audit;
import com.burpia.ExtensionBurpIA;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

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
    private final boolean integracionIssuesDisponible;
    private JPanel panelFiltros;
    private JPanel panelGuardarProyecto;
    private JPanel panelTablaWrapper;
    private JLabel etiquetaBusqueda;

    private TableRowSorter<ModeloTablaHallazgos> sorter;
    private JCheckBox chkGuardarEnIssues;
    private volatile boolean guardadoAutomaticoIssuesActivo = true;
    private Consumer<Boolean> manejadorCambioGuardadoIssues;
    private boolean actualizandoEstadoAutoIssues = false;
    private JPopupMenu menuContextual;
    private JMenuItem menuItemRepeater;
    private JMenuItem menuItemIntruder;
    private JMenuItem menuItemScanner;
    private JMenuItem menuItemIssues;
    private JMenuItem menuItemIgnorar;
    private JMenuItem menuItemBorrar;
    private final ExecutorService ejecutorAcciones;

    public PanelHallazgos(MontoyaApi api) {
        this.api = api;
        this.esBurpProfessional = false;
        this.integracionIssuesDisponible = false;
        this.modelo = new ModeloTablaHallazgos(1000);
        this.tabla = new JTable(modelo);
        this.ejecutorAcciones = crearEjecutorAcciones();
        initComponents();
    }

    public PanelHallazgos(MontoyaApi api, ModeloTablaHallazgos modeloCompartido) {
        this(api, modeloCompartido, false);
    }

    public PanelHallazgos(MontoyaApi api, ModeloTablaHallazgos modeloCompartido, boolean esBurpProfessional) {
        this.api = api;
        this.esBurpProfessional = esBurpProfessional;
        this.integracionIssuesDisponible = esBurpProfessional;
        this.modelo = modeloCompartido != null ? modeloCompartido : new ModeloTablaHallazgos(1000);
        this.tabla = new JTable(modelo);
        this.ejecutorAcciones = crearEjecutorAcciones();
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
        chkGuardarEnIssues = new JCheckBox(obtenerEtiquetaGuardadoIssues());
        chkGuardarEnIssues.setSelected(true);
        chkGuardarEnIssues.setFont(EstilosUI.FUENTE_ESTANDAR);
        chkGuardarEnIssues.setToolTipText(obtenerTooltipGuardadoIssues());
        chkGuardarEnIssues.addActionListener(e -> {
            if (actualizandoEstadoAutoIssues) {
                return;
            }
            aplicarEstadoGuardadoAutomaticoIssues(chkGuardarEnIssues.isSelected(), true);
        });
        actualizarEstadoControlesIssuesPorEdicion();

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
                    writer.write(construirLineaCsv(h));
                    writer.newLine();
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
                    writer.write(construirObjetoJson(h));
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

    private String construirLineaCsv(Hallazgo hallazgo) {
        String[] valores = {
            hallazgo != null ? hallazgo.obtenerHoraDescubrimiento() : "",
            hallazgo != null ? hallazgo.obtenerUrl() : "",
            hallazgo != null ? hallazgo.obtenerHallazgo() : "",
            hallazgo != null ? hallazgo.obtenerSeveridad() : "",
            hallazgo != null ? hallazgo.obtenerConfianza() : ""
        };

        StringBuilder linea = new StringBuilder();
        for (int i = 0; i < valores.length; i++) {
            linea.append(escapeCsv(valores[i]));
            if (i < valores.length - 1) {
                linea.append(",");
            }
        }
        return linea.toString();
    }

    private String construirObjetoJson(Hallazgo hallazgo) {
        return "    {\n"
            + "      \"hora\": \"" + escapeJson(hallazgo != null ? hallazgo.obtenerHoraDescubrimiento() : "") + "\",\n"
            + "      \"url\": \"" + escapeJson(hallazgo != null ? hallazgo.obtenerUrl() : "") + "\",\n"
            + "      \"hallazgo\": \"" + escapeJson(hallazgo != null ? hallazgo.obtenerHallazgo() : "") + "\",\n"
            + "      \"severidad\": \"" + escapeJson(hallazgo != null ? hallazgo.obtenerSeveridad() : "") + "\",\n"
            + "      \"confianza\": \"" + escapeJson(hallazgo != null ? hallazgo.obtenerConfianza() : "") + "\"\n"
            + "    }";
    }

    private String escapeCsv(String texto) {
        if (texto == null) {
            return "";
        }
        boolean requiereComillas = texto.contains(",")
            || texto.contains("\"")
            || texto.contains("\n")
            || texto.contains("\r");
        if (!requiereComillas) {
            return texto;
        }
        return "\"" + texto.replace("\"", "\"\"") + "\"";
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
        menuContextual = new JPopupMenu();

        menuItemRepeater = crearMenuItemContextual(
            I18nUI.Hallazgos.MENU_ENVIAR_REPEATER(),
            TooltipsUI.Hallazgos.MENU_REPEATER(),
            e -> enviarARepeater(tabla.getSelectedRows())
        );
        menuContextual.add(menuItemRepeater);

        menuItemIntruder = crearMenuItemContextual(
            I18nUI.Hallazgos.MENU_ENVIAR_INTRUDER(),
            TooltipsUI.Hallazgos.MENU_INTRUDER(),
            e -> enviarAIntruder(tabla.getSelectedRows())
        );
        menuContextual.add(menuItemIntruder);

        if (esBurpProfessional) {
            menuItemScanner = crearMenuItemContextual(
                I18nUI.Hallazgos.MENU_ENVIAR_SCANNER(),
                TooltipsUI.Hallazgos.MENU_SCANNER(),
                e -> enviarAScanner(tabla.getSelectedRows())
            );
            menuContextual.add(menuItemScanner);
        } else {
            menuItemScanner = null;
        }

        menuItemIssues = crearMenuItemContextual(
            obtenerEtiquetaMenuIssues(),
            obtenerTooltipMenuIssues(),
            e -> enviarAIssues(tabla.getSelectedRows())
        );
        menuItemIssues.setEnabled(integracionIssuesDisponible);
        menuContextual.add(menuItemIssues);

        menuContextual.addSeparator();

        menuItemIgnorar = crearMenuItemContextual(
            I18nUI.Hallazgos.MENU_IGNORAR(),
            TooltipsUI.Hallazgos.MENU_IGNORAR(),
            e -> {
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
            }
        );
        menuContextual.add(menuItemIgnorar);

        menuItemBorrar = crearMenuItemContextual(
            I18nUI.Hallazgos.MENU_BORRAR(),
            TooltipsUI.Hallazgos.MENU_BORRAR(),
            e -> {
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
            }
        );
        menuContextual.add(menuItemBorrar);
        actualizarVisibilidadMenuIssues();

        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && !e.isPopupTrigger()) {
                    int filaVista = tabla.rowAtPoint(e.getPoint());
                    if (filaVista >= 0) {
                        abrirDialogoEdicion(filaVista);
                    }
                }
                mostrarMenuContextualSiAplica(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mostrarMenuContextualSiAplica(e);
            }
        });
    }

    private JMenuItem crearMenuItemContextual(String texto, String tooltip, ActionListener accion) {
        JMenuItem menuItem = new JMenuItem(texto);
        menuItem.setFont(EstilosUI.FUENTE_ESTANDAR);
        menuItem.setToolTipText(tooltip);
        menuItem.addActionListener(accion);
        return menuItem;
    }

    private void abrirDialogoEdicion(int filaVista) {
        int filaModelo = tabla.convertRowIndexToModel(filaVista);
        Hallazgo hallazgoOriginal = modelo.obtenerHallazgo(filaModelo);
        if (hallazgoOriginal != null) {
            Window windowAncestral = SwingUtilities.getWindowAncestor(this);
            DialogoDetalleHallazgo dialogo = new DialogoDetalleHallazgo(windowAncestral, hallazgoOriginal, hallazgoEditado -> {
                modelo.actualizarHallazgo(hallazgoOriginal, hallazgoEditado);
            });
            dialogo.setVisible(true);
        }
    }

    private void mostrarMenuContextualSiAplica(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        if (menuContextual == null) {
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
        actualizarVisibilidadMenuIssues();
        menuContextual.show(tabla, e.getX(), e.getY());
    }

    private void actualizarVisibilidadMenuIssues() {
        if (menuItemIssues != null) {
            menuItemIssues.setVisible(!integracionIssuesDisponible || !guardadoAutomaticoIssuesActivo);
            menuItemIssues.setEnabled(integracionIssuesDisponible);
        }
    }

    private void actualizarEstadoControlesIssuesPorEdicion() {
        if (chkGuardarEnIssues != null) {
            chkGuardarEnIssues.setEnabled(integracionIssuesDisponible);
        }
        if (!integracionIssuesDisponible) {
            aplicarEstadoGuardadoAutomaticoIssues(false, false);
        }
        actualizarVisibilidadMenuIssues();
    }

    private String obtenerEtiquetaGuardadoIssues() {
        return integracionIssuesDisponible
            ? I18nUI.Hallazgos.CHECK_GUARDAR_ISSUES()
            : I18nUI.Hallazgos.CHECK_GUARDAR_ISSUES_SOLO_PRO();
    }

    private String obtenerTooltipGuardadoIssues() {
        return integracionIssuesDisponible
            ? TooltipsUI.Hallazgos.GUARDAR_ISSUES()
            : TooltipsUI.Hallazgos.GUARDAR_ISSUES_SOLO_PRO();
    }

    private String obtenerEtiquetaMenuIssues() {
        return integracionIssuesDisponible
            ? I18nUI.Hallazgos.MENU_ENVIAR_ISSUES()
            : I18nUI.Hallazgos.MENU_ENVIAR_ISSUES_SOLO_PRO();
    }

    private String obtenerTooltipMenuIssues() {
        return integracionIssuesDisponible
            ? TooltipsUI.Hallazgos.MENU_ISSUES()
            : TooltipsUI.Hallazgos.MENU_ISSUES_SOLO_PRO();
    }

    private void enviarARepeater(int[] filas) {
        ejecutarAccionBurp(
            filas,
            "BurpIA-Repeater",
            I18nUI.Hallazgos.TITULO_ACCION_REPEATER(),
            I18nUI.Hallazgos.RESUMEN_ACCION_REPEATER(),
            true,
            false,
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
            true,
            false,
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
            true,
            true,
            new AccionSobreSolicitud() {
                private Audit auditoriaActiva;

                @Override
                public String ejecutar(HttpRequest solicitud, Hallazgo hallazgo) {
                    if (!esBurpProfessional) {
                        throw new IllegalStateException(I18nUI.tr(
                            "Scanner solo disponible en Burp Professional",
                            "Scanner only available in Burp Professional"
                        ));
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

    private void enviarAIssues(int[] filas) {
        if (!integracionIssuesDisponible) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                PanelHallazgos.this,
                I18nUI.Hallazgos.MSG_ISSUES_SOLO_PRO(),
                I18nUI.Hallazgos.TITULO_INFORMACION(),
                JOptionPane.INFORMATION_MESSAGE
            ));
            return;
        }
        ejecutarAccionBurp(
            filas,
            "BurpIA-Issues",
            I18nUI.Hallazgos.TITULO_ACCION_ISSUES(),
            I18nUI.Hallazgos.RESUMEN_ACCION_ISSUES(),
            false,
            false,
            (solicitud, hallazgo) -> {
                if (hallazgo == null) {
                    throw new IllegalStateException(I18nUI.Hallazgos.ERROR_HALLAZGO_NO_DISPONIBLE());
                }
                if (api == null || api.siteMap() == null) {
                    throw new IllegalStateException(I18nUI.Hallazgos.ERROR_SITEMAP_NO_DISPONIBLE());
                }
                HttpRequestResponse evidencia = hallazgo.obtenerEvidenciaHttp();
                boolean guardado = ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(api, hallazgo, evidencia);
                if (!guardado) {
                    throw new IllegalStateException(I18nUI.Hallazgos.ERROR_GUARDAR_ISSUE());
                }

                String url = hallazgo.obtenerUrl() != null && !hallazgo.obtenerUrl().isBlank()
                    ? hallazgo.obtenerUrl()
                    : I18nUI.tr("URL desconocida", "Unknown URL");
                return "✅ " + url + " " + I18nUI.Hallazgos.SUFIJO_ISSUE_GUARDADO();
            }
        );
    }

    private void ejecutarAccionBurp(int[] filas,
                                    String nombreHilo,
                                    String titulo,
                                    String resumen,
                                    boolean requiereRequest,
                                    boolean registrarErroresScanner,
                                    AccionSobreSolicitud accion) {
        ResultadoCapturaAccion captura = capturarEntradasAccion(filas);
        List<EntradaAccion> entradas = captura.entradas;
        if (entradas.isEmpty()) {
            if (captura.totalIgnorados > 0) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    PanelHallazgos.this,
                    I18nUI.Hallazgos.MSG_ACCION_SOLO_IGNORADOS(captura.totalIgnorados),
                    I18nUI.Hallazgos.TITULO_INFORMACION(),
                    JOptionPane.INFORMATION_MESSAGE
                ));
            }
            return;
        }
        Runnable tarea = () -> {
            int exitosos = 0;
            int sinRequest = 0;
            StringBuilder detalle = new StringBuilder();

            for (EntradaAccion entrada : entradas) {
                HttpRequest solicitud = entrada.solicitud;
                Hallazgo hallazgo = entrada.hallazgo;

                if (requiereRequest && solicitud == null) {
                    sinRequest++;
                    detalle.append("⚠️ ").append(entrada.urlReferencia)
                        .append(" ").append(I18nUI.Hallazgos.MSG_SIN_REQUEST()).append("\n");
                    continue;
                }

                try {
                    String resultado = accion.ejecutar(solicitud, hallazgo);
                    exitosos++;
                    detalle.append(resultado).append("\n");
                } catch (Exception ex) {
                    String mensaje = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                    String url = solicitud != null ? solicitud.url() : entrada.urlReferencia;
                    detalle.append("❌ ").append(url)
                        .append(I18nUI.tr(" (error: ", " (error: "))
                        .append(mensaje)
                        .append(")\n");
                    if (registrarErroresScanner && api != null) {
                        api.logging().logToError(I18nUI.tr(
                            "[BurpIA] Error al enviar a Scanner Pro: ",
                            "[BurpIA] Error sending to Scanner Pro: "
                        ) + mensaje);
                    }
                }
            }

            final int totalExitosos = exitosos;
            final int totalSinRequest = sinRequest;
            final int totalSeleccionados = captura.totalSeleccionados;
            final int totalIgnorados = captura.totalIgnorados;
            final String detalleFinal = detalle.toString();

            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                    PanelHallazgos.this,
                    I18nUI.Hallazgos.MSG_RESULTADO_ACCION(
                        resumen,
                        totalExitosos,
                        totalSeleccionados,
                        totalSinRequest,
                        totalIgnorados,
                        detalleFinal
                    ),
                    titulo,
                    totalExitosos > 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE
                );
            });
        };

        try {
            ejecutorAcciones.execute(() -> {
                Thread.currentThread().setName(nombreHilo);
                tarea.run();
            });
        } catch (RejectedExecutionException ex) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                PanelHallazgos.this,
                I18nUI.tr(
                    "No se pudo ejecutar la accion: el panel se esta cerrando.",
                    "Action could not be executed: the panel is shutting down."
                ),
                titulo,
                JOptionPane.WARNING_MESSAGE
            ));
        }
    }

    @FunctionalInterface
    private interface AccionSobreSolicitud {
        String ejecutar(HttpRequest solicitud, Hallazgo hallazgo) throws Exception;
    }

    private void ignorarHallazgos(int[] filas) {
        int[] filasModelo = convertirFilasVistaAModelo(filas);
        for (int filaModelo : filasModelo) {
            modelo.marcarComoIgnorado(filaModelo);
        }

        final int ignorados = filasModelo.length;
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
        int[] filasModeloOrdenadasDesc = convertirFilasVistaAModeloOrdenDesc(filas);
        final int[] borrados = {0};
        for (int filaModelo : filasModeloOrdenadasDesc) {
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

    private int[] convertirFilasVistaAModelo(int[] filasVista) {
        if (filasVista == null || filasVista.length == 0) {
            return new int[0];
        }
        List<Integer> filasModelo = new ArrayList<>();
        Set<Integer> vistos = new HashSet<>();
        for (int filaVista : filasVista) {
            if (filaVista < 0 || filaVista >= tabla.getRowCount()) {
                continue;
            }
            int filaModelo = tabla.convertRowIndexToModel(filaVista);
            if (filaModelo >= 0 && vistos.add(filaModelo)) {
                filasModelo.add(filaModelo);
            }
        }
        int[] resultado = new int[filasModelo.size()];
        for (int i = 0; i < filasModelo.size(); i++) {
            resultado[i] = filasModelo.get(i);
        }
        return resultado;
    }

    private int[] convertirFilasVistaAModeloOrdenDesc(int[] filasVista) {
        int[] filasModelo = convertirFilasVistaAModelo(filasVista);
        java.util.Arrays.sort(filasModelo);
        for (int i = 0, j = filasModelo.length - 1; i < j; i++, j--) {
            int tmp = filasModelo[i];
            filasModelo[i] = filasModelo[j];
            filasModelo[j] = tmp;
        }
        return filasModelo;
    }

    private ResultadoCapturaAccion capturarEntradasAccion(int[] filasVista) {
        int[] filasModelo = convertirFilasVistaAModelo(filasVista);
        List<EntradaAccion> entradas = new ArrayList<>(filasModelo.length);
        int ignorados = 0;
        for (int filaModelo : filasModelo) {
            if (modelo.estaIgnorado(filaModelo)) {
                ignorados++;
                continue;
            }
            Hallazgo hallazgo = modelo.obtenerHallazgo(filaModelo);
            HttpRequest solicitud = modelo.obtenerSolicitudHttp(filaModelo);
            String urlReferencia = hallazgo != null
                ? hallazgo.obtenerUrl()
                : I18nUI.tr("URL desconocida", "Unknown URL");
            entradas.add(new EntradaAccion(solicitud, hallazgo, urlReferencia));
        }
        return new ResultadoCapturaAccion(entradas, filasModelo.length, ignorados);
    }

    private static final class EntradaAccion {
        private final HttpRequest solicitud;
        private final Hallazgo hallazgo;
        private final String urlReferencia;

        private EntradaAccion(HttpRequest solicitud, Hallazgo hallazgo, String urlReferencia) {
            this.solicitud = solicitud;
            this.hallazgo = hallazgo;
            this.urlReferencia = urlReferencia;
        }
    }

    private static final class ResultadoCapturaAccion {
        private final List<EntradaAccion> entradas;
        private final int totalSeleccionados;
        private final int totalIgnorados;

        private ResultadoCapturaAccion(List<EntradaAccion> entradas, int totalSeleccionados, int totalIgnorados) {
            this.entradas = entradas;
            this.totalSeleccionados = totalSeleccionados;
            this.totalIgnorados = totalIgnorados;
        }
    }

    public void aplicarIdioma() {
        etiquetaBusqueda.setText(I18nUI.Hallazgos.ETIQUETA_BUSCAR());
        botonLimpiarFiltro.setText(I18nUI.Hallazgos.BOTON_LIMPIAR());
        botonExportarCSV.setText(I18nUI.Hallazgos.BOTON_EXPORTAR_CSV());
        botonExportarJSON.setText(I18nUI.Hallazgos.BOTON_EXPORTAR_JSON());
        botonLimpiarTodo.setText(I18nUI.Hallazgos.BOTON_LIMPIAR_TODO());
        chkGuardarEnIssues.setText(obtenerEtiquetaGuardadoIssues());
        actualizarPrimeraOpcionSeveridad();

        etiquetaBusqueda.setToolTipText(TooltipsUI.Hallazgos.BUSQUEDA());
        campoBusqueda.setToolTipText(TooltipsUI.Hallazgos.BUSQUEDA());
        comboSeveridad.setToolTipText(TooltipsUI.Hallazgos.FILTRO_SEVERIDAD());
        botonLimpiarFiltro.setToolTipText(TooltipsUI.Hallazgos.LIMPIAR_FILTROS());
        botonExportarCSV.setToolTipText(TooltipsUI.Hallazgos.EXPORTAR_CSV());
        botonExportarJSON.setToolTipText(TooltipsUI.Hallazgos.EXPORTAR_JSON());
        botonLimpiarTodo.setToolTipText(TooltipsUI.Hallazgos.LIMPIAR_TODO());
        chkGuardarEnIssues.setToolTipText(obtenerTooltipGuardadoIssues());
        tabla.setToolTipText(TooltipsUI.Hallazgos.TABLA());
        if (menuItemRepeater != null) {
            menuItemRepeater.setText(I18nUI.Hallazgos.MENU_ENVIAR_REPEATER());
            menuItemRepeater.setToolTipText(TooltipsUI.Hallazgos.MENU_REPEATER());
        }
        if (menuItemIntruder != null) {
            menuItemIntruder.setText(I18nUI.Hallazgos.MENU_ENVIAR_INTRUDER());
            menuItemIntruder.setToolTipText(TooltipsUI.Hallazgos.MENU_INTRUDER());
        }
        if (menuItemScanner != null) {
            menuItemScanner.setText(I18nUI.Hallazgos.MENU_ENVIAR_SCANNER());
            menuItemScanner.setToolTipText(TooltipsUI.Hallazgos.MENU_SCANNER());
        }
        if (menuItemIssues != null) {
            menuItemIssues.setText(obtenerEtiquetaMenuIssues());
            menuItemIssues.setToolTipText(obtenerTooltipMenuIssues());
        }
        if (menuItemIgnorar != null) {
            menuItemIgnorar.setText(I18nUI.Hallazgos.MENU_IGNORAR());
            menuItemIgnorar.setToolTipText(TooltipsUI.Hallazgos.MENU_IGNORAR());
        }
        if (menuItemBorrar != null) {
            menuItemBorrar.setText(I18nUI.Hallazgos.MENU_BORRAR());
            menuItemBorrar.setToolTipText(TooltipsUI.Hallazgos.MENU_BORRAR());
        }
        actualizarVisibilidadMenuIssues();

        actualizarTituloPanel(panelFiltros, I18nUI.Hallazgos.TITULO_FILTROS());
        actualizarTituloPanel(panelGuardarProyecto, I18nUI.Hallazgos.TITULO_GUARDAR_PROYECTO());
        actualizarTituloPanel(panelTablaWrapper, I18nUI.Hallazgos.TITULO_TABLA());
        actualizarEstadoControlesIssuesPorEdicion();

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

    public void establecerManejadorCambioGuardadoIssues(Consumer<Boolean> manejadorCambioGuardadoIssues) {
        this.manejadorCambioGuardadoIssues = manejadorCambioGuardadoIssues;
    }

    public void establecerGuardadoAutomaticoIssuesActivo(boolean activo) {
        aplicarEstadoGuardadoAutomaticoIssues(activo, false);
    }

    public boolean isGuardadoAutomaticoIssuesActivo() {
        return guardadoAutomaticoIssuesActivo;
    }

    private void aplicarEstadoGuardadoAutomaticoIssues(boolean activo, boolean notificarCambio) {
        boolean activoNormalizado = integracionIssuesDisponible && activo;
        guardadoAutomaticoIssuesActivo = activoNormalizado;
        if (chkGuardarEnIssues != null && chkGuardarEnIssues.isSelected() != activoNormalizado) {
            actualizandoEstadoAutoIssues = true;
            try {
                chkGuardarEnIssues.setSelected(activoNormalizado);
            } finally {
                actualizandoEstadoAutoIssues = false;
            }
        }
        actualizarVisibilidadMenuIssues();
        if (notificarCambio && manejadorCambioGuardadoIssues != null) {
            manejadorCambioGuardadoIssues.accept(activoNormalizado);
        }
    }

    public void destruir() {
        ejecutorAcciones.shutdownNow();
    }

    private ExecutorService crearEjecutorAcciones() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("BurpIA-Hallazgos");
            thread.setDaemon(true);
            return thread;
        });
    }
}
