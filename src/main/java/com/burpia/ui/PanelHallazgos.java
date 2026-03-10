package com.burpia.ui;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.scanner.AuditConfiguration;
import burp.api.montoya.scanner.BuiltInAuditConfiguration;
import burp.api.montoya.scanner.audit.Audit;
import com.burpia.ExtensionBurpIA;
import com.burpia.config.AgenteTipo;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Hallazgo;
import com.burpia.util.Normalizador;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
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
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static com.burpia.ui.UIUtils.ejecutarEnEdt;

public class PanelHallazgos extends JPanel {
    // Constantes para columnas de tabla
    private static final int COLUMNA_HORA = 0;
    private static final int COLUMNA_URL = 1;
    private static final int COLUMNA_TITULO = 2;
    private static final int COLUMNA_SEVERIDAD = 3;
    private static final int COLUMNA_CONFIANZA = 4;
    private static final int NUMERO_COLUMNAS = 5;

    // Constantes para anchos de columna
    private static final int ANCHO_COLUMNA_HORA = 80;
    private static final int ANCHO_COLUMNA_URL = 300;
    private static final int ANCHO_COLUMNA_TITULO = 400;
    private static final int ANCHO_COLUMNA_SEVERIDAD = 100;
    private static final int ANCHO_COLUMNA_CONFIANZA = 100;
    private static final int DELAY_PERSISTENCIA_FILTROS_MS = 300;

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
    private final AtomicBoolean actualizandoEstadoAutoIssues = new AtomicBoolean(false);

    private com.burpia.config.ConfiguracionAPI config;
    private Predicate<Hallazgo> manejadorEnviarAAgente;
    private Runnable manejadorCambioAlertasEnviarA;
    private Runnable manejadorCambioFiltros;
    private UIStateManager uiStateManager;
    private final ExecutorService ejecutorAcciones;
    private final Timer temporizadorPersistenciaFiltros;

    @SuppressWarnings("this-escape")
    public PanelHallazgos(MontoyaApi api) {
        this.api = api;
        this.esBurpProfessional = false;
        this.integracionIssuesDisponible = false;
        this.modelo = new ModeloTablaHallazgos(ConfiguracionAPI.MAXIMO_HALLAZGOS_TABLA_DEFECTO);
        this.tabla = new JTable(modelo);
        this.ejecutorAcciones = crearEjecutorAcciones();
        this.temporizadorPersistenciaFiltros = crearTemporizadorPersistenciaFiltros();
        initComponents();
    }

    public PanelHallazgos(MontoyaApi api, ModeloTablaHallazgos modeloCompartido) {
        this(api, modeloCompartido, false);
    }

    @SuppressWarnings("this-escape")
    public PanelHallazgos(MontoyaApi api, ModeloTablaHallazgos modeloCompartido, boolean esBurpProfessional) {
        this.api = api;
        this.esBurpProfessional = esBurpProfessional;
        this.integracionIssuesDisponible = esBurpProfessional;
        this.modelo = modeloCompartido != null ? modeloCompartido : new ModeloTablaHallazgos(ConfiguracionAPI.MAXIMO_HALLAZGOS_TABLA_DEFECTO);
        this.tabla = new JTable(modelo);
        this.ejecutorAcciones = crearEjecutorAcciones();
        this.temporizadorPersistenciaFiltros = crearTemporizadorPersistenciaFiltros();
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panelFiltros = new JPanel();
        panelFiltros.setLayout(new BoxLayout(panelFiltros, BoxLayout.Y_AXIS));
        panelFiltros.setBorder(UIUtils.crearBordeTitulado(
            I18nUI.Hallazgos.TITULO_FILTROS(), 12, 16));

        JPanel panelTodosControles = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));

        // EFICIENCIA: FILTRADO primero - flujo natural de trabajo
        etiquetaBusqueda = new JLabel(I18nUI.Hallazgos.ETIQUETA_BUSCAR());
        etiquetaBusqueda.setFont(EstilosUI.FUENTE_ESTANDAR);
        etiquetaBusqueda.setToolTipText(I18nUI.Tooltips.Hallazgos.BUSQUEDA());
        panelTodosControles.add(etiquetaBusqueda);

        campoBusqueda = new JTextField(15);
        campoBusqueda.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        campoBusqueda.setToolTipText(I18nUI.Tooltips.Hallazgos.BUSQUEDA());
        panelTodosControles.add(campoBusqueda);

        comboSeveridad = new JComboBox<>(I18nUI.Hallazgos.OPCIONES_FILTRO_SEVERIDAD());
        comboSeveridad.setFont(EstilosUI.FUENTE_ESTANDAR);
        comboSeveridad.setToolTipText(I18nUI.Tooltips.Hallazgos.FILTRO_SEVERIDAD());
        panelTodosControles.add(comboSeveridad);

        botonLimpiarFiltro = new JButton(I18nUI.Hallazgos.BOTON_LIMPIAR());
        botonLimpiarFiltro.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonLimpiarFiltro.setToolTipText(I18nUI.Tooltips.Hallazgos.LIMPIAR_FILTROS());
        panelTodosControles.add(botonLimpiarFiltro);

        // CONFIABILIDAD: Separador visual entre filtrado y exportación
        panelTodosControles.add(new JLabel("  "));

        // EFICIENCIA: EXPORTACIÓN agrupada - después de filtrar, usuario exporta
        botonExportarCSV = new JButton(I18nUI.Hallazgos.BOTON_EXPORTAR_CSV());
        botonExportarCSV.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonExportarCSV.setToolTipText(I18nUI.Tooltips.Hallazgos.EXPORTAR_CSV());
        panelTodosControles.add(botonExportarCSV);

        botonExportarJSON = new JButton(I18nUI.Hallazgos.BOTON_EXPORTAR_JSON());
        botonExportarJSON.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonExportarJSON.setToolTipText(I18nUI.Tooltips.Hallazgos.EXPORTAR_JSON());
        panelTodosControles.add(botonExportarJSON);

        // CONFIABILIDAD: Separador visual antes de acción destructiva
        panelTodosControles.add(new JLabel("  "));

        // CONFIABILIDAD: Acción destructiva AISLADA al final
        botonLimpiarTodo = new JButton(I18nUI.Hallazgos.BOTON_LIMPIAR_TODO());
        botonLimpiarTodo.setFont(EstilosUI.FUENTE_ESTANDAR);
        botonLimpiarTodo.setToolTipText(I18nUI.Tooltips.Hallazgos.LIMPIAR_TODO());
        panelTodosControles.add(botonLimpiarTodo);

        panelFiltros.add(panelTodosControles);

        JPanel panelSuperior = new JPanel(new BorderLayout(10, 0));
        panelSuperior.add(panelFiltros, BorderLayout.CENTER);

        panelGuardarProyecto = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        panelGuardarProyecto.setBorder(UIUtils.crearBordeTitulado(
            I18nUI.Hallazgos.TITULO_GUARDAR_PROYECTO(), 12, 16));
        chkGuardarEnIssues = new JCheckBox(obtenerEtiquetaGuardadoIssues());
        chkGuardarEnIssues.setSelected(true);
        chkGuardarEnIssues.setFont(EstilosUI.FUENTE_ESTANDAR);
        chkGuardarEnIssues.setToolTipText(obtenerTooltipGuardadoIssues());
        chkGuardarEnIssues.addActionListener(e -> {
            if (actualizandoEstadoAutoIssues.get()) {
                return;
            }
            aplicarEstadoGuardadoAutomaticoIssues(chkGuardarEnIssues.isSelected(), true);
        });
        actualizarEstadoControlesIssuesPorEdicion();

        panelGuardarProyecto.add(chkGuardarEnIssues);

        panelSuperior.add(panelGuardarProyecto, BorderLayout.EAST);

        panelTablaWrapper = new JPanel(new BorderLayout());
        panelTablaWrapper.setBorder(UIUtils.crearBordeTitulado(
            I18nUI.Hallazgos.TITULO_TABLA(), 12, 16));

        tabla.setAutoCreateRowSorter(true);
        tabla.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tabla.setRowHeight(EstilosUI.ALTURA_FILA_TABLA);
        tabla.setFillsViewportHeight(true);
        tabla.setFont(EstilosUI.FUENTE_TABLA);
        tabla.setToolTipText(I18nUI.Tooltips.Hallazgos.TABLA());
        sorter = new TableRowSorter<>(modelo);
        configurarSorters();
        configurarColumnasTabla();

        JScrollPane panelDesplazable = new JScrollPane(tabla);
        panelDesplazable.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panelTablaWrapper.add(panelDesplazable, BorderLayout.CENTER);

        campoBusqueda.getDocument().addDocumentListener(UIUtils.crearDocumentListener(this::aplicarFiltros));

        comboSeveridad.addActionListener(e -> aplicarFiltros());

        botonLimpiarFiltro.addActionListener(e -> limpiarFiltros());

        botonExportarCSV.addActionListener(e -> exportarCSV());

        botonExportarJSON.addActionListener(e -> exportarJSON());

        botonLimpiarTodo.addActionListener(e -> {
            int total = modelo.getRowCount();
            if (total == 0) {
                UIUtils.mostrarInfo(this, I18nUI.Hallazgos.TITULO_INFORMACION(), I18nUI.Hallazgos.MSG_SIN_HALLAZGOS_LIMPIAR());
                return;
            }

            boolean confirmacion = UIUtils.confirmarAdvertencia(
                this,
                I18nUI.Hallazgos.TITULO_CONFIRMAR_LIMPIEZA(),
                I18nUI.Hallazgos.MSG_CONFIRMAR_LIMPIEZA(total)
            );
            if (confirmacion) {
                modelo.limpiar();
            }
        });

        crearMenuContextual();
        aplicarIdioma();

        add(panelSuperior, BorderLayout.NORTH);
        add(panelTablaWrapper, BorderLayout.CENTER);
    }

    private String textoBusquedaCacheado = "";
    private String textoBusquedaQuotado = "";
    private String severidadCacheada = "";
    private String severidadQuotada = "";

    private void aplicarFiltros() {
        String textoBusqueda = campoBusqueda.getText().trim();
        String severidadSeleccionada = (String) comboSeveridad.getSelectedItem();

        if (!textoBusqueda.equals(textoBusquedaCacheado)) {
            textoBusquedaCacheado = textoBusqueda;
            textoBusquedaQuotado = textoBusqueda.isEmpty() ? "" : java.util.regex.Pattern.quote(textoBusqueda);
        }

        if (severidadSeleccionada != null && !severidadSeleccionada.equals(severidadCacheada)) {
            severidadCacheada = severidadSeleccionada;
            severidadQuotada = java.util.regex.Pattern.quote(severidadSeleccionada);
        }

        List<RowFilter<Object, Object>> filtros = new ArrayList<>();

        if (Normalizador.noEsVacio(textoBusqueda)) {
            filtros.add(RowFilter.regexFilter("(?i)" + textoBusquedaQuotado, 1, 2));
        }

        if (severidadSeleccionada != null && comboSeveridad.getSelectedIndex() > 0) {
            filtros.add(RowFilter.regexFilter("^" + severidadQuotada + "$", 3));
        }

        if (!filtros.isEmpty()) {
            sorter.setRowFilter(RowFilter.andFilter(filtros));
        } else {
            sorter.setRowFilter(null);
        }

        programarPersistenciaFiltros();
    }

    private void limpiarFiltros() {
        campoBusqueda.setText("");
        comboSeveridad.setSelectedIndex(0);
        sorter.setRowFilter(null);
        // Reset filter cache to ensure fresh state
        textoBusquedaCacheado = "";
        textoBusquedaQuotado = "";
        severidadCacheada = "";
        severidadQuotada = "";
    }

    private void exportarCSV() {
        exportarHallazgos("csv", I18nUI.Hallazgos.DIALOGO_EXPORTAR_CSV(), this::escribirCsv);
    }

    /**
     * Valida que el archivo de exportación sea válido y se puede escribir.
     *
     * @param archivo Archivo a validar
     * @return Mensaje de error si hay validación fallida, null si es válido
     */
    private String validarArchivoExportacion(File archivo) {
        // Validación 1: El archivo existe (advertir sobre sobrescritura)
        if (archivo.exists()) {
            if (!archivo.isFile()) {
                return I18nUI.Hallazgos.MSG_ERROR_RUTA_NO_ES_ARCHIVO(archivo.getAbsolutePath());
            }
            if (!archivo.canWrite()) {
                return I18nUI.Hallazgos.MSG_ERROR_SIN_PERMISO_ESCRITURA(archivo.getAbsolutePath());
            }
        }

        // Validación 2: Directorio padre existe y es escribible
        File directorioPadre = archivo.getParentFile();
        if (directorioPadre == null) {
            return I18nUI.Hallazgos.MSG_ERROR_DIRECTORIO_INVALIDO();
        }
        if (!directorioPadre.exists()) {
            return I18nUI.Hallazgos.MSG_ERROR_DIRECTORIO_NO_EXISTE(directorioPadre.getAbsolutePath());
        }
        if (!directorioPadre.canWrite()) {
            return I18nUI.Hallazgos.MSG_ERROR_SIN_PERMISO_ESCRITURA_DIRECTORIO(directorioPadre.getAbsolutePath());
        }

        // Validación 3: Espacio en disco disponible (estimación: 10 KB por hallazgo)
        long espacioNecesarioBytes = (long) modelo.getRowCount() * 10 * 1024;
        long espacioLibreBytes = directorioPadre.getFreeSpace();
        if (espacioLibreBytes < espacioNecesarioBytes) {
            long espacioNecesarioMB = espacioNecesarioBytes / (1024 * 1024);
            long espacioLibreMB = espacioLibreBytes / (1024 * 1024);
            return I18nUI.Hallazgos.MSG_ERROR_ESPACIO_INSUFICIENTE(espacioNecesarioMB, espacioLibreMB);
        }

        return null; // Todas las validaciones pasaron
    }

    private void exportarJSON() {
        exportarHallazgos("json", I18nUI.Hallazgos.DIALOGO_EXPORTAR_JSON(), this::escribirJson);
    }

    private void exportarHallazgos(String extension,
                                   String tituloDialogo,
                                   EscritorExportacion escritor) {
        JFileChooser selectorArchivos = new JFileChooser();
        selectorArchivos.setDialogTitle(tituloDialogo);
        String nombreArchivo = "burpia_hallazgos_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
            "." + extension;
        selectorArchivos.setSelectedFile(new File(nombreArchivo));

        int resultado = selectorArchivos.showSaveDialog(this);
        if (resultado != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File archivo = selectorArchivos.getSelectedFile();

        // Validaciones antes de exportar
        String errorValidacion = validarArchivoExportacion(archivo);
        if (errorValidacion != null) {
            UIUtils.mostrarError(this, I18nUI.Hallazgos.TITULO_ERROR_EXPORTACION(), errorValidacion);
            return;
        }

        List<Hallazgo> hallazgosExportar = new ArrayList<>(modelo.obtenerHallazgosNoIgnorados());
        int totalEnTabla = modelo.getRowCount();
        int ignorados = Math.max(0, totalEnTabla - hallazgosExportar.size());

        Runnable tareaExportacion = () -> {
            try (BufferedWriter writer = Files.newBufferedWriter(archivo.toPath(), StandardCharsets.UTF_8)) {
                escritor.escribir(writer, hallazgosExportar);
                String mensaje = I18nUI.Hallazgos.MSG_EXPORTACION_EXITOSA(
                    hallazgosExportar.size(),
                    archivo.getName(),
                    ignorados
                );
                ejecutarEnEdt(() -> UIUtils.mostrarInfo(
                    this,
                    I18nUI.Hallazgos.TITULO_EXPORTACION_EXITOSA(),
                    mensaje
                ));
            } catch (Exception e) {
                String detalle = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                ejecutarEnEdt(() -> UIUtils.mostrarError(
                    this,
                    I18nUI.Hallazgos.TITULO_ERROR_EXPORTACION(),
                    I18nUI.Hallazgos.MSG_ERROR_EXPORTAR(detalle)
                ));
            }
        };

        try {
            ejecutorAcciones.execute(tareaExportacion);
        } catch (RejectedExecutionException ex) {
            UIUtils.mostrarError(
                this,
                I18nUI.Hallazgos.TITULO_ERROR_EXPORTACION(),
                I18nUI.Hallazgos.MSG_ERROR_EXPORTAR(I18nUI.Hallazgos.ERROR_PANEL_CERRANDO())
            );
        }
    }

    private void escribirCsv(BufferedWriter writer, List<Hallazgo> hallazgos) throws Exception {
        writer.write(I18nUI.Hallazgos.CSV_HEADER());
        writer.newLine();
        for (Hallazgo h : hallazgos) {
            writer.write(construirLineaCsv(h));
            writer.newLine();
        }
    }

    private void escribirJson(BufferedWriter writer, List<Hallazgo> hallazgos) throws Exception {
        writer.write("{\n  \"hallazgos\": [\n");
        for (int i = 0; i < hallazgos.size(); i++) {
            Hallazgo h = hallazgos.get(i);
            writer.write(construirObjetoJson(h));
            if (i < hallazgos.size() - 1) {
                writer.write(",");
            }
            writer.newLine();
        }
        writer.write("  ]\n}\n");
    }

    /**
     * Obtiene de forma segura un campo de un hallazgo, retornando cadena vacía si es null.
     *
     * @param hallazgo  El hallazgo del que obtener el campo
     * @param extractor Función que extrae el campo específico del hallazgo
     * @return El valor del campo o cadena vacía si el hallazgo es null
     */
    private String obtenerCampoSeguro(Hallazgo hallazgo,
                                       java.util.function.Function<Hallazgo, String> extractor) {
        return hallazgo != null ? extractor.apply(hallazgo) : "";
    }

    /**
     * Actualiza tanto el texto como el tooltip de un componente UI en una sola operación.
     *
     * @param componente El componente a actualizar (JLabel, JButton, etc.)
     * @param texto      El nuevo texto para el componente
     * @param tooltip    El nuevo tooltip para el componente
     */
    private void actualizarTextoYTooltip(JComponent componente, String texto, String tooltip) {
        if (componente instanceof JLabel) {
            ((JLabel) componente).setText(texto);
        } else if (componente instanceof JButton) {
            ((JButton) componente).setText(texto);
        } else if (componente instanceof JCheckBox) {
            ((JCheckBox) componente).setText(texto);
        }
        componente.setToolTipText(tooltip);
    }

    private String construirLineaCsv(Hallazgo hallazgo) {
        String[] valores = {
            obtenerCampoSeguro(hallazgo, Hallazgo::obtenerHoraDescubrimiento),
            obtenerCampoSeguro(hallazgo, Hallazgo::obtenerUrl),
            obtenerCampoSeguro(hallazgo, Hallazgo::obtenerTitulo),
            obtenerCampoSeguro(hallazgo, Hallazgo::obtenerHallazgo),
            obtenerCampoSeguro(hallazgo, Hallazgo::obtenerSeveridad),
            obtenerCampoSeguro(hallazgo, Hallazgo::obtenerConfianza)
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
            + "      \"hora\": \"" + escapeJson(obtenerCampoSeguro(hallazgo, Hallazgo::obtenerHoraDescubrimiento)) + "\",\n"
            + "      \"url\": \"" + escapeJson(obtenerCampoSeguro(hallazgo, Hallazgo::obtenerUrl)) + "\",\n"
            + "      \"titulo\": \"" + escapeJson(obtenerCampoSeguro(hallazgo, Hallazgo::obtenerTitulo)) + "\",\n"
            + "      \"hallazgo\": \"" + escapeJson(obtenerCampoSeguro(hallazgo, Hallazgo::obtenerHallazgo)) + "\",\n"
            + "      \"severidad\": \"" + escapeJson(obtenerCampoSeguro(hallazgo, Hallazgo::obtenerSeveridad)) + "\",\n"
            + "      \"confianza\": \"" + escapeJson(obtenerCampoSeguro(hallazgo, Hallazgo::obtenerConfianza)) + "\"\n"
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

    /**
     * Agrega múltiples hallazgos al panel.
     *
     * @param hallazgos Lista de hallazgos a agregar
     */
    public void agregarHallazgos(List<Hallazgo> hallazgos) {
        modelo.agregarHallazgos(hallazgos);
    }

    public void limpiar() {
        // El modelo ya maneja EDT internamente, no es necesario envolver
        modelo.limpiar();
    }

    private void crearMenuContextual() {
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

    private void abrirDialogoCreacion() {
        Window windowAncestral = SwingUtilities.getWindowAncestor(this);
        DialogoDetalleHallazgo dialogo = new DialogoDetalleHallazgo(windowAncestral, null, nuevoHallazgo -> {
            modelo.agregarHallazgo(nuevoHallazgo);
        });
        dialogo.setVisible(true);
    }

    private void mostrarMenuContextualSiAplica(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        int fila = tabla.rowAtPoint(e.getPoint());
        ajustarSeleccionParaMenuContextual(fila, e.isControlDown());
        JPopupMenu menu = construirMenuContextualDinamico();
        menu.show(tabla, e.getX(), e.getY());
    }

    private void ajustarSeleccionParaMenuContextual(int fila, boolean controlPresionado) {
        if (fila < 0) {
            tabla.clearSelection();
            return;
        }
        if (tabla.isRowSelected(fila)) {
            return;
        }
        if (!controlPresionado) {
            tabla.setRowSelectionInterval(fila, fila);
        } else {
            tabla.addRowSelectionInterval(fila, fila);
        }
    }

    /**
     * Agrega un item de menú contextual para enviar hallazgos a una herramienta de Burp.
     *
     * @param menu    El menú contextual al que agregar el item
     * @param etiqueta Etiqueta del item de menú
     * @param tooltip Tooltip del item de menú
     * @param accion  Acción a ejecutar con las filas seleccionadas
     */
    private void agregarMenuItemEnvio(JPopupMenu menu, String etiqueta, String tooltip,
                                       java.util.function.Consumer<int[]> accion) {
        menu.add(crearMenuItemContextual(etiqueta, tooltip, e -> accion.accept(tabla.getSelectedRows())));
    }

    private JPopupMenu construirMenuContextualDinamico() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(crearMenuItemContextual(
            I18nUI.Hallazgos.MENU_AGREGAR_HALLAZGO(),
            I18nUI.Tooltips.Hallazgos.MENU_AGREGAR_HALLAZGO(),
            e -> abrirDialogoCreacion()
        ));

        boolean tieneSeleccion = tabla.getSelectedRowCount() > 0;
        if (!tieneSeleccion) {
            return menu;
        }

        menu.addSeparator();
        agregarMenuItemEnvio(menu, I18nUI.Hallazgos.MENU_ENVIAR_REPEATER(),
                            I18nUI.Tooltips.Hallazgos.MENU_REPEATER(), this::enviarARepeater);
        agregarMenuItemEnvio(menu, I18nUI.Hallazgos.MENU_ENVIAR_INTRUDER(),
                            I18nUI.Tooltips.Hallazgos.MENU_INTRUDER(), this::enviarAIntruder);

        if (esBurpProfessional) {
            agregarMenuItemEnvio(menu, I18nUI.Hallazgos.MENU_ENVIAR_SCANNER(),
                                I18nUI.Tooltips.Hallazgos.MENU_SCANNER(), this::enviarAScanner);
        }

        if (config != null && config.agenteHabilitado() && manejadorEnviarAAgente != null) {
            String nombreAgente = obtenerNombreAgenteVisible();
            agregarMenuItemEnvio(menu, I18nUI.Hallazgos.MENU_ENVIAR_AGENTE_ROCKET(nombreAgente),
                                I18nUI.Tooltips.Hallazgos.ENVIAR_AGENTE(nombreAgente), this::enviarAAgente);
        }

        if (integracionIssuesDisponible && !guardadoAutomaticoIssuesActivo) {
            agregarMenuItemEnvio(menu, obtenerEtiquetaMenuIssues(),
                                obtenerTooltipMenuIssues(), this::enviarAIssues);
        }

        menu.addSeparator();
        menu.add(crearMenuItemContextual(
            I18nUI.Hallazgos.MENU_IGNORAR(),
            I18nUI.Tooltips.Hallazgos.MENU_IGNORAR(),
            e -> confirmarYEjecutar(
                I18nUI.Hallazgos.MSG_CONFIRMAR_IGNORAR(tabla.getSelectedRowCount()),
                I18nUI.Hallazgos.TITULO_CONFIRMAR_IGNORAR(),
                false,
                () -> ignorarHallazgos(tabla.getSelectedRows())
            )
        ));
        menu.add(crearMenuItemContextual(
            I18nUI.Hallazgos.MENU_BORRAR(),
            I18nUI.Tooltips.Hallazgos.MENU_BORRAR(),
            e -> confirmarYEjecutar(
                I18nUI.Hallazgos.MSG_CONFIRMAR_BORRAR(tabla.getSelectedRowCount()),
                I18nUI.Hallazgos.TITULO_CONFIRMAR_BORRAR(),
                true,
                () -> borrarHallazgosDeTabla(tabla.getSelectedRows())
            )
        ));
        return menu;
    }

    private void confirmarYEjecutar(String mensaje, String titulo, boolean esAdvertencia, Runnable accion) {
        boolean confirmacion = esAdvertencia
            ? UIUtils.confirmarAdvertencia(this, titulo, mensaje)
            : UIUtils.confirmarPregunta(this, titulo, mensaje);
        if (confirmacion) {
            accion.run();
        }
    }

    private void actualizarEstadoControlesIssuesPorEdicion() {
        if (chkGuardarEnIssues != null) {
            chkGuardarEnIssues.setEnabled(integracionIssuesDisponible);
        }
        if (!integracionIssuesDisponible) {
            aplicarEstadoGuardadoAutomaticoIssues(false, false);
        }
    }

    /**
     * Obtiene el texto i18n apropiado según la disponibilidad de integración con Issues.
     *
     * @param textoDisponible Texto a usar cuando la integración está disponible
     * @param textoSoloPro    Texto a usar cuando la integración NO está disponible
     * @return El texto apropiado según el estado de la integración
     */
    private String obtenerTextoI18n(java.util.function.Supplier<String> textoDisponible,
                                     java.util.function.Supplier<String> textoSoloPro) {
        return integracionIssuesDisponible ? textoDisponible.get() : textoSoloPro.get();
    }

    private String obtenerEtiquetaGuardadoIssues() {
        return obtenerTextoI18n(I18nUI.Hallazgos::CHECK_GUARDAR_ISSUES,
                                I18nUI.Hallazgos::CHECK_GUARDAR_ISSUES_SOLO_PRO);
    }

    private String obtenerTooltipGuardadoIssues() {
        return obtenerTextoI18n(I18nUI.Tooltips.Hallazgos::GUARDAR_ISSUES,
                                I18nUI.Tooltips.Hallazgos::GUARDAR_ISSUES_SOLO_PRO);
    }

    private String obtenerEtiquetaMenuIssues() {
        return obtenerTextoI18n(I18nUI.Hallazgos::MENU_ENVIAR_ISSUES,
                                I18nUI.Hallazgos::MENU_ENVIAR_ISSUES_SOLO_PRO);
    }

    private String obtenerTooltipMenuIssues() {
        return obtenerTextoI18n(I18nUI.Tooltips.Hallazgos::MENU_ISSUES,
                                I18nUI.Tooltips.Hallazgos::MENU_ISSUES_SOLO_PRO);
    }

    private void enviarAAgente(int... filas) {
        if (manejadorEnviarAAgente == null) {
            return;
        }
        final String nombreAgente = obtenerNombreAgenteVisible();
        ejecutarAccionBurp(
            filas,
            "BurpIA-Agente",
            I18nUI.Hallazgos.TITULO_ACCION_AGENTE(),
            I18nUI.Hallazgos.RESUMEN_ACCION_AGENTE(nombreAgente),
            false,
            false,
            (solicitud, hallazgo) -> {
                if (hallazgo == null) {
                    throw new IllegalStateException(I18nUI.Hallazgos.ERROR_HALLAZGO_NO_DISPONIBLE());
                }
                boolean enviado = manejadorEnviarAAgente.test(hallazgo);
                if (!enviado) {
                    throw new IllegalStateException(I18nUI.Hallazgos.ERROR_ENVIO_AGENTE(nombreAgente));
                }
                String url = resolverUrlReferencia(hallazgo);
                return I18nUI.Hallazgos.LINEA_ESTADO_EXITO_ALERTA(
                    url + " " + I18nUI.Hallazgos.SUFIJO_ENVIADO_AGENTE(nombreAgente)
                );
            }
        );
    }

    private void enviarARepeater(int... filas) {
        ejecutarAccionBurp(
            filas,
            "BurpIA-Repeater",
            I18nUI.Hallazgos.TITULO_ACCION_REPEATER(),
            I18nUI.Hallazgos.RESUMEN_ACCION_REPEATER(),
            true,
            false,
            (solicitud, hallazgo) -> {
                String nombreTab = "BurpIA-" + (hallazgo != null ? hallazgo.obtenerSeveridad() : I18nUI.General.HALLAZGO_GENERICO());
                api.repeater().sendToRepeater(solicitud, nombreTab);
                return I18nUI.Hallazgos.LINEA_ESTADO_EXITO_ALERTA(solicitud.url());
            }
        );
    }

    private void enviarAIntruder(int... filas) {
        ejecutarAccionBurp(
            filas,
            "BurpIA-Intruder",
            I18nUI.Hallazgos.TITULO_ACCION_INTRUDER(),
            I18nUI.Hallazgos.RESUMEN_ACCION_INTRUDER(),
            true,
            false,
            (solicitud, hallazgo) -> {
                api.intruder().sendToIntruder(solicitud);
                return I18nUI.Hallazgos.LINEA_ESTADO_EXITO_ALERTA(
                    solicitud.url() + " " + I18nUI.Hallazgos.SUFIJO_ENVIADO_INTRUDER()
                );
            }
        );
    }

    private void enviarAScanner(int... filas) {
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
                        throw new IllegalStateException(I18nUI.Hallazgos.ERROR_SCANNER_SOLO_PRO());
                    }
                    if (auditoriaActiva == null) {
                        AuditConfiguration configScanner = AuditConfiguration.auditConfiguration(
                            BuiltInAuditConfiguration.LEGACY_ACTIVE_AUDIT_CHECKS
                        );
                        auditoriaActiva = api.scanner().startAudit(configScanner);
                    }
                    auditoriaActiva.addRequest(solicitud);
                    api.logging().logToOutput(I18nUI.Hallazgos.LOG_PETICION_ENVIADA_SCANNER() + solicitud.url());
                    return I18nUI.Hallazgos.LINEA_ESTADO_EXITO_ALERTA(
                        solicitud.url() + " " + I18nUI.Hallazgos.SUFIJO_ENVIADO_SCANNER()
                    );
                }
            }
        );
    }

    private void enviarAIssues(int... filas) {
        if (!integracionIssuesDisponible) {
            mostrarInfoEnviarA(I18nUI.Hallazgos.TITULO_INFORMACION(), I18nUI.Hallazgos.MSG_ISSUES_SOLO_PRO());
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

                String url = resolverUrlReferencia(hallazgo);
                return I18nUI.Hallazgos.LINEA_ESTADO_EXITO_ALERTA(
                    url + " " + I18nUI.Hallazgos.SUFIJO_ISSUE_GUARDADO()
                );
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
                mostrarInfoEnviarA(I18nUI.Hallazgos.TITULO_INFORMACION(), I18nUI.Hallazgos.MSG_ACCION_SOLO_IGNORADOS(captura.totalIgnorados));
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
                    String advertencia = entrada.urlReferencia + " " + I18nUI.Hallazgos.MSG_SIN_REQUEST();
                    detalle.append(I18nUI.Hallazgos.LINEA_ESTADO_ADVERTENCIA_ALERTA(advertencia)).append("\n");
                    continue;
                }

                try {
                    String resultado = accion.ejecutar(solicitud, hallazgo);
                    exitosos++;
                    detalle.append(resultado).append("\n");
                } catch (Exception ex) {
                    String mensaje = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                    String url = normalizarUrlTexto(solicitud != null ? solicitud.url() : entrada.urlReferencia);
                    String detalleError = url + I18nUI.Hallazgos.SUFIJO_ERROR_INLINE() + mensaje + ")";
                    detalle.append(I18nUI.Hallazgos.LINEA_ESTADO_ERROR_ALERTA(detalleError)).append("\n");
                    if (registrarErroresScanner && api != null) {
                        api.logging().logToError(I18nUI.Hallazgos.LOG_ERROR_ENVIAR_SCANNER() + mensaje);
                    }
                }
            }

            final int totalExitosos = exitosos;
            final int totalSinRequest = sinRequest;
            final int totalSeleccionados = captura.totalSeleccionados;
            final int totalIgnorados = captura.totalIgnorados;
            final String detalleFinal = detalle.toString();

            ejecutarEnEdt(() -> {
                final String mensajeFinal = I18nUI.Hallazgos.MSG_RESULTADO_ACCION(
                    resumen,
                    totalExitosos,
                    totalSeleccionados,
                    totalSinRequest,
                    totalIgnorados,
                    detalleFinal
                );
                if (totalExitosos > 0) {
                    mostrarInfoEnviarA(titulo, mensajeFinal);
                } else {
                    mostrarAdvertenciaEnviarA(titulo, mensajeFinal);
                }
            });
        };

        try {
            ejecutorAcciones.execute(() -> {
                Thread.currentThread().setName(nombreHilo);
                tarea.run();
            });
        } catch (RejectedExecutionException ex) {
            mostrarAdvertenciaEnviarA(titulo, I18nUI.Hallazgos.ERROR_PANEL_CERRANDO());
        }
    }

    @FunctionalInterface
    private interface AccionSobreSolicitud {
        String ejecutar(HttpRequest solicitud, Hallazgo hallazgo) throws Exception;
    }

    private void ignorarHallazgos(int... filas) {
        int[] filasModelo = convertirFilasVistaAModelo(filas);
        for (int filaModelo : filasModelo) {
            modelo.marcarComoIgnorado(filaModelo);
        }

        final int ignorados = filasModelo.length;
        UIUtils.mostrarInfo(PanelHallazgos.this, I18nUI.Hallazgos.TITULO_HALLAZGOS_IGNORADOS(), I18nUI.Hallazgos.MSG_HALLAZGOS_IGNORADOS(ignorados));
    }

    private void borrarHallazgosDeTabla(int... filas) {
        int[] filasModeloOrdenadasDesc = convertirFilasVistaAModeloOrdenDesc(filas);
        int contadorBorrados = 0;

        for (int filaModelo : filasModeloOrdenadasDesc) {
            if (filaModelo >= 0) {
                modelo.eliminarHallazgo(filaModelo);
                contadorBorrados++;
            }
        }

        UIUtils.mostrarInfo(PanelHallazgos.this, I18nUI.Hallazgos.TITULO_HALLAZGOS_BORRADOS(),
            I18nUI.Hallazgos.MSG_HALLAZGOS_BORRADOS(contadorBorrados));
    }

    private int[] convertirFilasVistaAModelo(int... filasVista) {
        if (filasVista == null || filasVista.length == 0) {
            return new int[0];
        }

        return IntStream.of(filasVista)
            .filter(f -> f >= 0 && f < tabla.getRowCount())
            .map(tabla::convertRowIndexToModel)
            .filter(f -> f >= 0)
            .distinct()
            .toArray();
    }

    private int[] convertirFilasVistaAModeloOrdenDesc(int... filasVista) {
        int[] filas = convertirFilasVistaAModelo(filasVista);
        java.util.Arrays.sort(filas);

        return IntStream.range(0, filas.length)
            .map(i -> filas[filas.length - 1 - i])
            .toArray();
    }

    private ResultadoCapturaAccion capturarEntradasAccion(int... filasVista) {
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
            String urlReferencia = resolverUrlReferencia(hallazgo);
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
        actualizarTextoYTooltip(etiquetaBusqueda, I18nUI.Hallazgos.ETIQUETA_BUSCAR(),
                                I18nUI.Tooltips.Hallazgos.BUSQUEDA());
        actualizarTextoYTooltip(botonLimpiarFiltro, I18nUI.Hallazgos.BOTON_LIMPIAR(),
                                I18nUI.Tooltips.Hallazgos.LIMPIAR_FILTROS());
        actualizarTextoYTooltip(botonExportarCSV, I18nUI.Hallazgos.BOTON_EXPORTAR_CSV(),
                                I18nUI.Tooltips.Hallazgos.EXPORTAR_CSV());
        actualizarTextoYTooltip(botonExportarJSON, I18nUI.Hallazgos.BOTON_EXPORTAR_JSON(),
                                I18nUI.Tooltips.Hallazgos.EXPORTAR_JSON());
        actualizarTextoYTooltip(botonLimpiarTodo, I18nUI.Hallazgos.BOTON_LIMPIAR_TODO(),
                                I18nUI.Tooltips.Hallazgos.LIMPIAR_TODO());
        actualizarTextoYTooltip(chkGuardarEnIssues, obtenerEtiquetaGuardadoIssues(),
                                obtenerTooltipGuardadoIssues());

        campoBusqueda.setToolTipText(I18nUI.Tooltips.Hallazgos.BUSQUEDA());
        comboSeveridad.setToolTipText(I18nUI.Tooltips.Hallazgos.FILTRO_SEVERIDAD());
        tabla.setToolTipText(I18nUI.Tooltips.Hallazgos.TABLA());

        actualizarOpcionesSeveridadIdioma();

        UIUtils.actualizarTituloPanel(panelFiltros, I18nUI.Hallazgos.TITULO_FILTROS());
        UIUtils.actualizarTituloPanel(panelGuardarProyecto, I18nUI.Hallazgos.TITULO_GUARDAR_PROYECTO());
        UIUtils.actualizarTituloPanel(panelTablaWrapper, I18nUI.Hallazgos.TITULO_TABLA());
        actualizarEstadoControlesIssuesPorEdicion();

        modelo.refrescarColumnasIdioma();
        ejecutarEnEdt(() -> {
            configurarSorters();
            configurarColumnasTabla();
            aplicarFiltros();
        });
        revalidate();
        repaint();
    }

    private void actualizarOpcionesSeveridadIdioma() {
        int indiceSeleccionado = comboSeveridad.getSelectedIndex();
        DefaultComboBoxModel<String> nuevoModelo =
            new DefaultComboBoxModel<>(I18nUI.Hallazgos.OPCIONES_FILTRO_SEVERIDAD());
        comboSeveridad.setModel(nuevoModelo);

        int indiceSeguro = Math.max(0, Math.min(indiceSeleccionado, nuevoModelo.getSize() - 1));
        comboSeveridad.setSelectedIndex(indiceSeguro);
    }

    private void configurarSorters() {
        if (sorter == null || tabla == null) return;

        sorter.setComparator(3, (o1, o2) -> {
            String s1 = o1 != null ? o1.toString() : "";
            String s2 = o2 != null ? o2.toString() : "";
            return Integer.compare(Hallazgo.obtenerPrioridadSeveridad(s1), Hallazgo.obtenerPrioridadSeveridad(s2));
        });

        sorter.setComparator(4, (o1, o2) -> {
            String c1 = o1 != null ? o1.toString() : "";
            String c2 = o2 != null ? o2.toString() : "";
            return Integer.compare(Hallazgo.obtenerPrioridadConfianza(c1), Hallazgo.obtenerPrioridadConfianza(c2));
        });

        tabla.setRowSorter(sorter);
    }

    private void configurarColumnasTabla() {
        if (tabla.getColumnModel().getColumnCount() < NUMERO_COLUMNAS) {
            return;
        }
        tabla.getColumnModel().getColumn(COLUMNA_HORA).setCellRenderer(
            new RenderizadorHallazgoBorrado(new RenderizadorCentrado(), tabla, modelo)
        );
        tabla.getColumnModel().getColumn(COLUMNA_URL).setCellRenderer(
            new RenderizadorHallazgoBorrado(new DefaultTableCellRenderer(), tabla, modelo)
        );
        tabla.getColumnModel().getColumn(COLUMNA_TITULO).setCellRenderer(
            new RenderizadorHallazgoBorrado(new DefaultTableCellRenderer(), tabla, modelo)
        );
        tabla.getColumnModel().getColumn(COLUMNA_SEVERIDAD).setCellRenderer(
            new RenderizadorHallazgoBorrado(new RenderizadorSeveridad(), tabla, modelo)
        );
        tabla.getColumnModel().getColumn(COLUMNA_CONFIANZA).setCellRenderer(
            new RenderizadorHallazgoBorrado(new RenderizadorConfianza(), tabla, modelo)
        );

        tabla.getColumnModel().getColumn(COLUMNA_HORA).setPreferredWidth(ANCHO_COLUMNA_HORA);
        tabla.getColumnModel().getColumn(COLUMNA_URL).setPreferredWidth(ANCHO_COLUMNA_URL);
        tabla.getColumnModel().getColumn(COLUMNA_TITULO).setPreferredWidth(ANCHO_COLUMNA_TITULO);
        tabla.getColumnModel().getColumn(COLUMNA_SEVERIDAD).setPreferredWidth(ANCHO_COLUMNA_SEVERIDAD);
        tabla.getColumnModel().getColumn(COLUMNA_CONFIANZA).setPreferredWidth(ANCHO_COLUMNA_CONFIANZA);
    }

    public ModeloTablaHallazgos obtenerModelo() {
        return modelo;
    }

    /**
     * Obtiene el número de hallazgos actualmente visibles en la tabla.
     * Considera los filtros activos y las filas ignoradas.
     *
     * @return Número de hallazgos visibles, o 0 si no hay modelo
     */
    public int obtenerHallazgosVisibles() {
        if (modelo == null) {
            return 0;
        }
        return modelo.obtenerFilasVisibles();
    }

    /**
     * Obtiene las estadísticas de hallazgos visibles agrupadas por severidad.
     * Solo cuenta hallazgos que NO están filtrados.
     *
     * @return Array de 6 elementos: [total, critical, high, medium, low, info] o array de ceros si error
     */
    public int[] obtenerEstadisticasVisibles() {
        if (modelo == null) {
            return new int[6];
        }
        return modelo.obtenerEstadisticasVisibles();
    }

    public void establecerManejadorCambioGuardadoIssues(Consumer<Boolean> manejadorCambioGuardadoIssues) {
        this.manejadorCambioGuardadoIssues = manejadorCambioGuardadoIssues;
    }

    public void establecerConfiguracion(com.burpia.config.ConfiguracionAPI config) {
        this.config = config;
        // Actualizar el límite de filas del modelo con el valor configurable
        int limiteMaximoHallazgos = config.obtenerMaximoHallazgosTabla();
        modelo.establecerLimiteFilas(limiteMaximoHallazgos);
        restaurarEstadoFiltros();
    }

    private void restaurarEstadoFiltros() {
        if (config == null) {
            return;
        }
        ejecutarEnEdt(() -> {
            String textoGuardado = config.persistirFiltroBusquedaHallazgos()
                ? config.obtenerTextoFiltroHallazgos()
                : "";
            campoBusqueda.setText(Normalizador.noEsVacio(textoGuardado) ? textoGuardado : "");

            String severidadGuardada = config.persistirFiltroSeveridadHallazgos()
                ? config.obtenerFiltroSeveridadHallazgos()
                : "";
            if (Normalizador.noEsVacio(severidadGuardada)) {
                DefaultComboBoxModel<String> modelo = (DefaultComboBoxModel<String>) comboSeveridad.getModel();
                for (int i = 0; i < modelo.getSize(); i++) {
                    if (severidadGuardada.equals(modelo.getElementAt(i))) {
                        comboSeveridad.setSelectedIndex(i);
                        break;
                    }
                }
            } else if (comboSeveridad.getItemCount() > 0) {
                comboSeveridad.setSelectedIndex(0);
            }
            aplicarFiltros();
        });
    }

    private Timer crearTemporizadorPersistenciaFiltros() {
        Timer temporizador = new Timer(
            DELAY_PERSISTENCIA_FILTROS_MS,
            e -> guardarEstadoFiltrosAhora()
        );
        temporizador.setRepeats(false);
        return temporizador;
    }

    private void programarPersistenciaFiltros() {
        if (config == null) {
            return;
        }
        temporizadorPersistenciaFiltros.restart();
    }

    private void guardarEstadoFiltrosAhora() {
        if (config == null) {
            return;
        }

        String textoActual = campoBusqueda.getText().trim();
        String severidadActual = (String) comboSeveridad.getSelectedItem();

        // DRY: Solo guardar si el flag está habilitado y hubo cambio
        boolean textoCambio = !textoActual.equals(config.obtenerTextoFiltroHallazgos());
        boolean severidadCambio = severidadActual != null &&
            !severidadActual.equals(config.obtenerFiltroSeveridadHallazgos());

        boolean debeGuardarTexto = textoCambio && config.persistirFiltroBusquedaHallazgos();
        boolean debeGuardarSeveridad = severidadCambio && config.persistirFiltroSeveridadHallazgos();

        if (!debeGuardarTexto && !debeGuardarSeveridad) {
            return;
        }

        if (debeGuardarTexto) {
            config.establecerTextoFiltroHallazgos(textoActual);
        }
        if (debeGuardarSeveridad) {
            config.establecerFiltroSeveridadHallazgos(severidadActual != null ? severidadActual : "");
        }

        // Guardar estado adicional con UIStateManager si está disponible
        if (uiStateManager != null) {
            uiStateManager.guardarEstadoFiltrosHallazgos(textoActual, severidadActual);
        }

        if (manejadorCambioFiltros != null) {
            manejadorCambioFiltros.run();
        }
    }

    public void establecerManejadorEnviarAAgente(Predicate<Hallazgo> manejador) {
        this.manejadorEnviarAAgente = manejador;
    }

    public void establecerManejadorCambioAlertasEnviarA(Runnable manejador) {
        this.manejadorCambioAlertasEnviarA = manejador;
    }

    public void establecerManejadorCambioFiltros(Runnable manejador) {
        this.manejadorCambioFiltros = manejador;
    }

    public void establecerGuardadoAutomaticoIssuesActivo(boolean activo) {
        UIUtils.ejecutarEnEdtYEsperar(() -> aplicarEstadoGuardadoAutomaticoIssues(activo, false));
    }

    public boolean isGuardadoAutomaticoIssuesActivo() {
        return guardadoAutomaticoIssuesActivo;
    }

    private void aplicarEstadoGuardadoAutomaticoIssues(boolean activo, boolean notificarCambio) {
        boolean activoNormalizado = integracionIssuesDisponible && activo;
        boolean estadoActual = guardadoAutomaticoIssuesActivo;

        guardadoAutomaticoIssuesActivo = activoNormalizado;

        if (chkGuardarEnIssues != null && chkGuardarEnIssues.isSelected() != activoNormalizado) {
            if (actualizandoEstadoAutoIssues.compareAndSet(false, true)) {
                try {
                    chkGuardarEnIssues.setSelected(activoNormalizado);
                } finally {
                    actualizandoEstadoAutoIssues.set(false);
                }
            }
        }

        if (notificarCambio && manejadorCambioGuardadoIssues != null && estadoActual != activoNormalizado) {
            manejadorCambioGuardadoIssues.accept(activoNormalizado);
        }
    }

    /**
     * Obtiene el hallazgo actualmente seleccionado en la tabla.
     *
     * @return El hallazgo seleccionado, o null si no hay selección
     */
    public Hallazgo obtenerHallazgoSeleccionado() {
        int filaSeleccionada = tabla.getSelectedRow();
        if (filaSeleccionada < 0) {
            return null;
        }

        int indiceModelo = tabla.convertRowIndexToModel(filaSeleccionada);
        return modelo.obtenerHallazgo(indiceModelo);
    }

    public void destruir() {
        ejecutorAcciones.shutdownNow();
    }

    private String resolverUrlReferencia(Hallazgo hallazgo) {
        return normalizarUrlTexto(hallazgo != null ? hallazgo.obtenerUrl() : null);
    }

    private String normalizarUrlTexto(String url) {
        return Normalizador.esVacio(url) ? I18nUI.Hallazgos.URL_DESCONOCIDA() : url;
    }

    @FunctionalInterface
    private interface EscritorExportacion {
        void escribir(BufferedWriter writer, List<Hallazgo> hallazgos) throws Exception;
    }

    private ExecutorService crearEjecutorAcciones() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable);
            // Usar nombre constante para consistencia en logs y debugging
            thread.setName("BurpIA-PanelHallazgos");
            thread.setDaemon(true);
            return thread;
        });
    }

    private String obtenerNombreAgenteVisible() {
        if (config == null) {
            return I18nUI.General.AGENTE_GENERICO();
        }
        return AgenteTipo.obtenerNombreVisible(
            config.obtenerTipoAgente(),
            I18nUI.General.AGENTE_GENERICO()
        );
    }

    private void mostrarInfoEnviarA(String titulo, String mensaje) {
        UIUtils.mostrarInfoConOptOut(
            this,
            titulo,
            mensaje,
            alertasEnviarAHabilitadas(),
            this::deshabilitarAlertasEnviarA
        );
    }

    private void mostrarAdvertenciaEnviarA(String titulo, String mensaje) {
        UIUtils.mostrarAdvertenciaConOptOut(
            this,
            titulo,
            mensaje,
            alertasEnviarAHabilitadas(),
            this::deshabilitarAlertasEnviarA
        );
    }

    private boolean alertasEnviarAHabilitadas() {
        return config == null
            || (config.alertasHabilitadas() && config.alertasClickDerechoEnviarAHabilitadas());
    }

    private void deshabilitarAlertasEnviarA() {
        if (config == null || !config.alertasClickDerechoEnviarAHabilitadas()) {
            return;
        }
        config.establecerAlertasClickDerechoEnviarAHabilitadas(false);
        if (manejadorCambioAlertasEnviarA != null) {
            manejadorCambioAlertasEnviarA.run();
        }
    }

    /**
     * Establece el texto del campo de búsqueda.
     * Método público para UI State Persistence.
     *
     * @param texto Texto a establecer en el campo de búsqueda
     */
    public void establecerTextoFiltro(String texto) {
        if (campoBusqueda != null && texto != null) {
            ejecutarEnEdt(() -> {
                campoBusqueda.setText(texto);
                aplicarFiltros();
            });
        }
    }

    /**
     * Establece el filtro de severidad seleccionado.
     * Método público para UI State Persistence.
     *
     * @param severidad Severidad a establecer en el combo
     */
    public void establecerFiltroSeveridad(String severidad) {
        if (comboSeveridad != null && severidad != null) {
            ejecutarEnEdt(() -> {
                comboSeveridad.setSelectedItem(severidad);
                aplicarFiltros();
            });
        }
    }

    /**
     * Obtiene la tabla de hallazgos.
     * Método público para UI State Persistence.
     *
     * @return La tabla de hallazgos
     */
    public JTable obtenerTabla() {
        return tabla;
    }

    /**
     * Establece el gestor de estado UI.
     * Método para integrar con UI State Persistence.
     *
     * @param uiStateManager Gestor de estado UI
     */
    public void establecerUIStateManager(UIStateManager uiStateManager) {
        this.uiStateManager = uiStateManager;
    }
}
