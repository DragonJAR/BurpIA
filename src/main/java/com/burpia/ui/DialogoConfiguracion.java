package com.burpia.ui;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.GestorConfiguracion;
import com.burpia.config.ProveedorAI;
import com.burpia.util.ConstructorSolicitudesProveedor;
import com.burpia.util.ParserModelosOllama;
import com.burpia.util.ProbadorConexionAI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DialogoConfiguracion extends JDialog {
    private final ConfiguracionAPI config;
    private final GestorConfiguracion gestorConfig;
    private final Runnable alGuardar;

    private JTextField txtUrl;
    private JPasswordField txtClave;
    private JTextField txtRetraso;
    private JTextField txtMaximoConcurrente;
    private JTextField txtMaximoHallazgosTabla;
    private JCheckBox chkDetallado;
    private JTextArea txtPrompt;
    private JButton btnRestaurarPrompt;
    private JLabel lblContadorPrompt;

    private JComboBox<String> comboProveedor;
    private JComboBox<String> comboModelo;
    private JButton btnRefrescarModelos;
    private JButton btnProbarConexion;
    private JTextField txtMaxTokens;

    private static final String MODELO_CUSTOM = "-- Personalizado --";

    public DialogoConfiguracion(Window padre, ConfiguracionAPI config, GestorConfiguracion gestorConfig, Runnable alGuardar) {
        super(padre, "🧠 Ajustes de BurpIA", Dialog.ModalityType.APPLICATION_MODAL);
        this.config = config;
        this.gestorConfig = gestorConfig;
        this.alGuardar = alGuardar;

        inicializarComponentes();
        cargarConfiguracionActual();
    }

    private void inicializarComponentes() {
        setLayout(new BorderLayout(10, 10));
        setSize(800, 650);
        setLocationRelativeTo(getParent());

        JTabbedPane tabbedPane = new JTabbedPane();

        // Pestaña 1: Ajustes General
        JPanel panelGeneral = crearPanelGeneral();
        tabbedPane.addTab("🏢 Proveedor LLM", panelGeneral);

        // Pestaña 2: Ajustes de Prompt
        JPanel panelPrompt = crearPanelPrompt();
        tabbedPane.addTab("📝 Prompt", panelPrompt);

        // Pestaña 3: Acerca de BurpIA
        JPanel panelAcercaDe = crearPanelAcercaDe();
        tabbedPane.addTab("ℹ️ Acerca de", panelAcercaDe);

        JButton btnGuardar = new JButton("💾 Guardar Ajustes");
        btnGuardar.setFont(EstilosUI.FUENTE_BOTON_PRINCIPAL);
        btnGuardar.addActionListener(e -> guardarConfiguracion());

        btnProbarConexion = new JButton("🔍 Probar Conexión");
        btnProbarConexion.setFont(EstilosUI.FUENTE_BOTON_PRINCIPAL);
        btnProbarConexion.setToolTipText("Verificar que la configuración de API es correcta");
        btnProbarConexion.addActionListener(e -> probarConexion());

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panelBotones.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panelBotones.add(btnProbarConexion);
        panelBotones.add(btnGuardar);

        add(tabbedPane, BorderLayout.CENTER);
        add(panelBotones, BorderLayout.SOUTH);
    }

    private JPanel crearPanelGeneral() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel contenido = new JPanel();
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setOpaque(false);

        JPanel panelProveedor = crearPanelProveedor();
        panelProveedor.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelProveedor.setMaximumSize(new Dimension(Integer.MAX_VALUE, panelProveedor.getPreferredSize().height));
        contenido.add(panelProveedor);
        contenido.add(Box.createVerticalStrut(12));

        JPanel panelEjecucion = new JPanel(new GridBagLayout());
        panelEjecucion.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelEjecucion.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
                "⚙️ AJUSTES DE EJECUCIÓN",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtRetraso = new JTextField(10);
        txtRetraso.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtMaximoConcurrente = new JTextField(10);
        txtMaximoConcurrente.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtMaximoHallazgosTabla = new JTextField(10);
        txtMaximoHallazgosTabla.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtMaximoHallazgosTabla.setToolTipText(
            "Límite de hallazgos retenidos en tabla/memoria (" +
                ConfiguracionAPI.MINIMO_HALLAZGOS_TABLA + "-" +
                ConfiguracionAPI.MAXIMO_HALLAZGOS_TABLA + ")"
        );
        chkDetallado = new JCheckBox("Activar registro detallado (recomendado para depuración)");
        chkDetallado.setToolTipText("Cuando está activo, todas las operaciones del complemento se registran con información detallada");

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panelEjecucion.add(new JLabel("Retraso (seg):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panelEjecucion.add(txtRetraso, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panelEjecucion.add(new JLabel("Máximo Concurrente:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panelEjecucion.add(txtMaximoConcurrente, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        panelEjecucion.add(new JLabel("Máx Hallazgos Tabla:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panelEjecucion.add(txtMaximoHallazgosTabla, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        panelEjecucion.add(new JLabel("Modo Detallado:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        panelEjecucion.add(chkDetallado, gbc);

        JLabel etiquetaDescripcion = new JLabel("El modo detallado registra solicitudes HTTP, llamadas API y operaciones internas.");
        etiquetaDescripcion.setFont(EstilosUI.FUENTE_ESTANDAR);
        etiquetaDescripcion.setForeground(new Color(120, 120, 120));
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        panelEjecucion.add(etiquetaDescripcion, gbc);

        panelEjecucion.setMaximumSize(new Dimension(Integer.MAX_VALUE, panelEjecucion.getPreferredSize().height));

        contenido.add(panelEjecucion);
        contenido.add(Box.createVerticalGlue());

        root.add(contenido, BorderLayout.CENTER);
        return root;
    }

    private JPanel crearPanelProveedor() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
                "🤖 CONFIGURACIÓN DE PROVEEDOR AI",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        int fila = 0;

        // Proveedor AI
        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        panel.add(new JLabel("Proveedor AI:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        comboProveedor = new JComboBox<>(ProveedorAI.obtenerNombresProveedores().toArray(new String[0]));
        comboProveedor.setFont(EstilosUI.FUENTE_ESTANDAR);
        comboProveedor.addActionListener(e -> alCambiarProveedor());
        panel.add(comboProveedor, gbc);

        fila++;

        // URL de API
        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        panel.add(new JLabel("URL de API:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        txtUrl = new JTextField(30);
        txtUrl.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        panel.add(txtUrl, gbc);

        fila++;

        // Clave de API
        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        panel.add(new JLabel("Clave de API:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        txtClave = new JPasswordField(30);
        txtClave.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        panel.add(txtClave, gbc);

        fila++;

        // Modelo y botón Refresh
        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        panel.add(new JLabel("Modelo:"), gbc);

        JPanel panelModelo = new JPanel(new BorderLayout(5, 0));
        comboModelo = new JComboBox<>();
        comboModelo.setFont(EstilosUI.FUENTE_ESTANDAR);
        comboModelo.setEditable(true);
        panelModelo.add(comboModelo, BorderLayout.CENTER);

        btnRefrescarModelos = new JButton("🔄 Cargar Modelos");
        btnRefrescarModelos.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnRefrescarModelos.setToolTipText("Actualizar lista de modelos disponibles para el proveedor");
        btnRefrescarModelos.addActionListener(e -> refrescarModelosDesdeAPI());
        panelModelo.add(btnRefrescarModelos, BorderLayout.EAST);

        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(panelModelo, gbc);

        fila++;

        // Max Tokens
        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        JLabel lblMaxTokens = new JLabel("Máximo de Tokens:");
        lblMaxTokens.setToolTipText("Cantidad máxima de tokens en la respuesta del modelo");
        panel.add(lblMaxTokens, gbc);

        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel panelMaxTokens = new JPanel(new BorderLayout(5, 0));
        txtMaxTokens = new JTextField(30);
        txtMaxTokens.setFont(EstilosUI.FUENTE_CAMPO_TEXTO);
        txtMaxTokens.setToolTipText("Rango típico: 4096-8192 tokens según el modelo");
        panelMaxTokens.add(txtMaxTokens, BorderLayout.CENTER);

        JLabel lblInfoTokens = new JLabel("ℹ️");
        lblInfoTokens.setToolTipText("Ventana de contexto: OpenAI 128k, Claude 200k, Gemini 1M");
        panelMaxTokens.add(lblInfoTokens, BorderLayout.EAST);

        panel.add(panelMaxTokens, gbc);

        return panel;
    }

    private JPanel crearPanelPrompt() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel panelInstrucciones = new JPanel(new BorderLayout(0, 8));
        panelInstrucciones.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(120, 140, 255), 1),
                "📝 INSTRUCCIONES",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        panelInstrucciones.setBackground(new Color(244, 248, 255));

        JTextArea txtInstrucciones = new JTextArea();
        txtInstrucciones.setEditable(false);
        txtInstrucciones.setOpaque(false);
        txtInstrucciones.setWrapStyleWord(true);
        txtInstrucciones.setLineWrap(true);
        txtInstrucciones.setFont(EstilosUI.FUENTE_ESTANDAR);
        txtInstrucciones.setText(
            "• El token {REQUEST} se reemplaza automáticamente con la solicitud HTTP analizada.\n" +
            "• Incluye {REQUEST} exactamente donde quieras insertar la petición.\n" +
            "• El prompt debe pedir respuesta JSON estricta con este formato:"
        );
        txtInstrucciones.setBorder(BorderFactory.createEmptyBorder(8, 10, 0, 10));

        JTextArea ejemploJson = new JTextArea();
        ejemploJson.setEditable(false);
        ejemploJson.setFont(EstilosUI.FUENTE_TABLA);
        ejemploJson.setBackground(new Color(255, 255, 255));
        ejemploJson.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 210, 210), 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        ejemploJson.setText("{\"hallazgos\":[{\"descripcion\":\"string\",\"severidad\":\"Critical|High|Medium|Low|Info\",\"confianza\":\"High|Medium|Low\"}]}");

        JPanel bloqueInstrucciones = new JPanel(new BorderLayout(0, 8));
        bloqueInstrucciones.setOpaque(false);
        bloqueInstrucciones.add(txtInstrucciones, BorderLayout.NORTH);
        bloqueInstrucciones.add(ejemploJson, BorderLayout.CENTER);
        bloqueInstrucciones.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        panelInstrucciones.add(bloqueInstrucciones, BorderLayout.CENTER);
        panel.add(panelInstrucciones, BorderLayout.NORTH);

        JPanel panelEditor = new JPanel(new BorderLayout());
        panelEditor.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
                "✍️ PROMPT DE ANÁLISIS",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        // Área de texto para el prompt
        txtPrompt = new JTextArea();
        txtPrompt.setFont(EstilosUI.FUENTE_TABLA);
        txtPrompt.setLineWrap(false);
        txtPrompt.setWrapStyleWord(false);
        txtPrompt.setTabSize(2);

        JScrollPane scrollPrompt = new JScrollPane(txtPrompt);
        scrollPrompt.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPrompt.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        panelEditor.add(scrollPrompt, BorderLayout.CENTER);

        JPanel panelSur = new JPanel(new BorderLayout(10, 6));
        panelSur.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JPanel panelBotonesPrompt = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

        btnRestaurarPrompt = new JButton("🔄 Restaurar Prompt por Defecto");
        btnRestaurarPrompt.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnRestaurarPrompt.setToolTipText("Restaura el prompt original de BurpIA");
        btnRestaurarPrompt.addActionListener(e -> restaurarPromptPorDefecto());
        panelBotonesPrompt.add(btnRestaurarPrompt);

        JPanel panelContador = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        lblContadorPrompt = new JLabel("Caracteres: 0");
        lblContadorPrompt.setFont(EstilosUI.FUENTE_TABLA);
        panelContador.add(lblContadorPrompt);

        JPanel barraAcciones = new JPanel(new BorderLayout());
        barraAcciones.add(panelBotonesPrompt, BorderLayout.WEST);
        barraAcciones.add(panelContador, BorderLayout.EAST);
        panelSur.add(barraAcciones, BorderLayout.NORTH);

        JLabel etiquetaAdvertencia = new JLabel("⚠️ Importante: El token {REQUEST} es obligatorio y debe aparecer exactamente así.");
        etiquetaAdvertencia.setFont(EstilosUI.FUENTE_ESTANDAR);
        etiquetaAdvertencia.setForeground(new Color(139, 0, 0));
        panelSur.add(etiquetaAdvertencia, BorderLayout.SOUTH);

        panelEditor.add(panelSur, BorderLayout.SOUTH);

        panel.add(panelEditor, BorderLayout.CENTER);

        // Listener para actualizar contador y marcar prompt como modificado
        txtPrompt.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                config.establecerPromptModificado(true);
                actualizarContador();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                config.establecerPromptModificado(true);
                actualizarContador();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                config.establecerPromptModificado(true);
                actualizarContador();
            }
        });

        return panel;
    }

    private JPanel crearPanelAcercaDe() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 20));

        JPanel contenedor = new JPanel();
        contenedor.setLayout(new BoxLayout(contenedor, BoxLayout.Y_AXIS));
        contenedor.setOpaque(false);

        JLabel titulo = new JLabel("BurpIA");
        titulo.setFont(new Font(Font.MONOSPACED, Font.BOLD, 26));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        contenedor.add(titulo);

        contenedor.add(Box.createVerticalStrut(6));

        JLabel subtitulo = new JLabel("Análisis de Seguridad con Inteligencia Artificial");
        subtitulo.setFont(EstilosUI.FUENTE_NEGRITA);
        subtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        contenedor.add(subtitulo);

        contenedor.add(Box.createVerticalStrut(14));

        JPanel panelDescripcion = new JPanel(new BorderLayout());
        panelDescripcion.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelDescripcion.setMaximumSize(new Dimension(760, Integer.MAX_VALUE));
        panelDescripcion.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
                "RESUMEN",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JTextArea descripcion = new JTextArea();
        descripcion.setEditable(false);
        descripcion.setWrapStyleWord(true);
        descripcion.setLineWrap(true);
        descripcion.setFont(EstilosUI.FUENTE_ESTANDAR);
        descripcion.setBackground(new Color(245, 250, 255));
        descripcion.setText(
            "BurpIA es una extensión profesional para Burp Suite que aprovecha múltiples " +
            "modelos de Inteligencia Artificial para analizar tráfico HTTP e identificar " +
            "vulnerabilidades de seguridad de forma automatizada.\n\n" +
            "Características principales:\n" +
            "• Compatibilidad con OpenAI, Claude, Gemini, Z.ai, Minimax y Ollama\n" +
            "• De-duplicación inteligente de peticiones para optimizar la cuota de API\n" +
            "• Gestión asíncrona mediante colas de tareas paralelizables\n" +
            "• Integración con site map (Issues), Repeater, Intruder y Scanner Pro\n" +
            "• Prompt totalmente configurable para análisis a medida\n" +
            "• Exportación nativa de reportes de hallazgos a CSV y JSON"
        );
        descripcion.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panelDescripcion.add(descripcion, BorderLayout.CENTER);
        contenedor.add(panelDescripcion);

        contenedor.add(Box.createVerticalStrut(12));

        JPanel panelAutor = new JPanel(new BorderLayout(0, 8));
        panelAutor.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelAutor.setMaximumSize(new Dimension(760, 140));
        panelAutor.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
                "DESARROLLADO POR",
                javax.swing.border.TitledBorder.CENTER,
                javax.swing.border.TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JLabel nombreAutor = new JLabel("Jaime Andrés Restrepo", SwingConstants.CENTER);
        nombreAutor.setFont(EstilosUI.FUENTE_NEGRITA);
        panelAutor.add(nombreAutor, BorderLayout.NORTH);

        JButton btnSitioWeb = new JButton("Visitar DragonJAR.org");
        btnSitioWeb.setFont(EstilosUI.FUENTE_BOTON_PRINCIPAL);
        btnSitioWeb.setToolTipText("https://www.dragonjar.org/contactar-empresa-de-seguridad-informatica");
        btnSitioWeb.addActionListener(e -> {
            try {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().browse(
                        new java.net.URI("https://www.dragonjar.org/contactar-empresa-de-seguridad-informatica")
                    );
                } else {
                    JOptionPane.showMessageDialog(
                        DialogoConfiguracion.this,
                        "URL: https://www.dragonjar.org/contactar-empresa-de-seguridad-informatica",
                        "Enlace",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                    DialogoConfiguracion.this,
                    "Error al abrir el navegador: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        });

        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        panelBoton.setOpaque(false);
        panelBoton.add(btnSitioWeb);
        panelAutor.add(panelBoton, BorderLayout.SOUTH);
        contenedor.add(panelAutor);

        contenedor.add(Box.createVerticalStrut(10));

        JLabel lblVersion = new JLabel("Versión 1.0.0 - Febrero 2026");
        lblVersion.setFont(EstilosUI.FUENTE_ESTANDAR);
        lblVersion.setForeground(new Color(128, 128, 128));
        lblVersion.setAlignmentX(Component.CENTER_ALIGNMENT);
        contenedor.add(lblVersion);

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        wrapper.add(contenedor, gbc);

        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        wrapper.add(Box.createGlue(), gbc);

        JScrollPane scroll = new JScrollPane(wrapper);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void actualizarContador() {
        int longitud = txtPrompt.getText().length();
        lblContadorPrompt.setText("Caracteres: " + longitud);

        if (!txtPrompt.getText().contains("{REQUEST}")) {
            lblContadorPrompt.setText(lblContadorPrompt.getText() + " ⚠️ Falta {REQUEST}");
            lblContadorPrompt.setForeground(Color.RED);
        } else {
            lblContadorPrompt.setForeground(Color.BLACK);
        }
    }

    private void restaurarPromptPorDefecto() {
        int confirmacion = JOptionPane.showConfirmDialog(
            this,
            "¿Estás seguro de que deseas restaurar el prompt por defecto? Se perderán tus cambios personalizados.",
            "Confirmar Restauración",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirmacion == JOptionPane.YES_OPTION) {
            txtPrompt.setText(ConfiguracionAPI.obtenerPromptPorDefecto());
            actualizarContador();
        }
    }

    private void cargarConfiguracionActual() {
        String proveedorActual = config.obtenerProveedorAI();
        if (proveedorActual != null && ProveedorAI.existeProveedor(proveedorActual)) {
            comboProveedor.setSelectedItem(proveedorActual);
        }

        txtRetraso.setText(String.valueOf(config.obtenerRetrasoSegundos()));
        txtMaximoConcurrente.setText(String.valueOf(config.obtenerMaximoConcurrente()));
        txtMaximoHallazgosTabla.setText(String.valueOf(config.obtenerMaximoHallazgosTabla()));
        chkDetallado.setSelected(config.esDetallado());

        if (config.esPromptModificado()) {
            txtPrompt.setText(config.obtenerPromptConfigurable());
        } else {
            txtPrompt.setText(ConfiguracionAPI.obtenerPromptPorDefecto());
        }
        actualizarContador();
        alCambiarProveedor();
    }

    private void guardarConfiguracion() {
        if (!txtPrompt.getText().contains("{REQUEST}")) {
            JOptionPane.showMessageDialog(this,
                    "El prompt debe contener el token {REQUEST} para indicar dónde se insertará la solicitud HTTP.",
                    "Error de Validación",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String proveedorSeleccionado = (String) comboProveedor.getSelectedItem();
        config.establecerProveedorAI(proveedorSeleccionado);

        String claveApi = new String(txtClave.getPassword());
        config.establecerApiKeyParaProveedor(proveedorSeleccionado, claveApi);

        String modeloSeleccionado = obtenerModeloSeleccionado();
        config.establecerModeloParaProveedor(proveedorSeleccionado, modeloSeleccionado);

        String urlBase = txtUrl.getText().trim();
        config.establecerUrlBaseParaProveedor(proveedorSeleccionado, urlBase);

        // Guardar Max Tokens por proveedor
        try {
            int maxTokens = Integer.parseInt(txtMaxTokens.getText().trim());
            config.establecerMaxTokensParaProveedor(proveedorSeleccionado, maxTokens);
        } catch (NumberFormatException e) {
            // Si no es válido, usar el valor por defecto del proveedor
            ProveedorAI.ConfiguracionProveedor configProveedor =
                ProveedorAI.obtenerProveedor(proveedorSeleccionado);
            if (configProveedor != null) {
                config.establecerMaxTokensParaProveedor(proveedorSeleccionado,
                    configProveedor.obtenerMaxTokensPorDefecto());
            }
        }

        config.establecerDetallado(chkDetallado.isSelected());

        String promptActual = txtPrompt.getText();
        String promptPorDefecto = ConfiguracionAPI.obtenerPromptPorDefecto();

        if (promptActual.equals(promptPorDefecto)) {
            config.establecerPromptModificado(false);
            config.establecerPromptConfigurable(promptPorDefecto);
        } else {
            config.establecerPromptModificado(true);
            config.establecerPromptConfigurable(promptActual);
        }

        try {
            config.establecerRetrasoSegundos(Integer.parseInt(txtRetraso.getText()));
            config.establecerMaximoConcurrente(Integer.parseInt(txtMaximoConcurrente.getText()));
            config.establecerMaximoHallazgosTabla(Integer.parseInt(txtMaximoHallazgosTabla.getText()));

            StringBuilder errorMsg = new StringBuilder();
            boolean guardado = gestorConfig.guardarConfiguracion(config, errorMsg);

            if (!guardado) {
                JOptionPane.showMessageDialog(this,
                        "No se pudo guardar la configuracion:\n" + errorMsg.toString() +
                        "\n\nRuta: " + gestorConfig.obtenerRutaConfiguracion(),
                        "Error al Guardar",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            alGuardar.run();
            setVisible(false);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Formato de número inválido en ajustes de ejecución",
                    "Error de Validacion",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void alCambiarProveedor() {
        String proveedorSeleccionado = (String) comboProveedor.getSelectedItem();
        if (proveedorSeleccionado == null) return;
        ProveedorAI.ConfiguracionProveedor configProveedor =
            ProveedorAI.obtenerProveedor(proveedorSeleccionado);

        if (configProveedor == null) {
            return;
        }

        String urlGuardada = config.obtenerUrlBaseParaProveedor(proveedorSeleccionado);
        txtUrl.setText(urlGuardada != null ? urlGuardada : configProveedor.obtenerUrlApi());

        String apiKeyGuardada = config.obtenerApiKeyParaProveedor(proveedorSeleccionado);
        txtClave.setText(apiKeyGuardada != null ? apiKeyGuardada : "");

        Integer maxTokensGuardado = config.obtenerMaxTokensParaProveedor(proveedorSeleccionado);
        txtMaxTokens.setText(String.valueOf(maxTokensGuardado));

        List<String> modelosProveedor = configProveedor.obtenerModelosDisponibles();

        String modeloGuardado = config.obtenerModeloParaProveedor(proveedorSeleccionado);
        if (modeloGuardado == null || modeloGuardado.trim().isEmpty()) {
            modeloGuardado = configProveedor.obtenerModeloPorDefecto();
        }
        cargarModelosEnCombo(modelosProveedor, modeloGuardado);
    }

    private String obtenerModeloSeleccionado() {
        String modeloActual = (String) comboModelo.getSelectedItem();
        if (modeloActual == null) {
            return "";
        }
        if (!MODELO_CUSTOM.equals(modeloActual)) {
            return normalizarModeloSeleccionado(modeloActual);
        }
        Object editorValue = comboModelo.getEditor().getItem();
        return editorValue != null ? normalizarModeloSeleccionado(editorValue.toString()) : "";
    }

    private void refrescarModelosDesdeAPI() {
        String proveedorSeleccionado = (String) comboProveedor.getSelectedItem();
        if (proveedorSeleccionado == null) return;

        btnRefrescarModelos.setEnabled(false);
        btnRefrescarModelos.setText("🔄 Actualizando...");

        SwingWorker<java.util.List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected java.util.List<String> doInBackground() {
                try {
                    String urlApi = txtUrl.getText().trim();

                    if (proveedorSeleccionado.equals("Ollama")) {
                        return obtenerModelosOllama(urlApi);
                    } else {
                        return obtenerModelosDesdeAPI(proveedorSeleccionado);
                    }
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    java.util.List<String> modelos = get();
                    if (modelos != null && !modelos.isEmpty()) {
                        cargarModelosEnCombo(modelos, modelos.get(0));

                        JOptionPane.showMessageDialog(DialogoConfiguracion.this,
                                "Se cargaron " + modelos.size() + " modelos para " + proveedorSeleccionado,
                                "Modelos Actualizados",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(DialogoConfiguracion.this,
                            "Error al procesar modelos: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnRefrescarModelos.setEnabled(true);
                    btnRefrescarModelos.setText("🔄 Cargar Modelos");
                }
            }
        };

        worker.execute();
    }

    private java.util.List<String> obtenerModelosOllama(String urlApi) {
        java.util.List<String> modelos = new java.util.ArrayList<>();

        try {
            String urlBase = ConfiguracionAPI.extraerUrlBase(urlApi);
            if (!urlBase.endsWith("/api/tags")) {
                urlBase = urlBase + "/api/tags";
            }

            java.net.URL url = new java.net.URL(urlBase);
            java.net.HttpURLConnection conexion = (java.net.HttpURLConnection) url.openConnection();
            conexion.setRequestMethod("GET");
            conexion.setRequestProperty("Accept", "application/json");
            conexion.setConnectTimeout(5000);
            conexion.setReadTimeout(5000);

            int codigoRespuesta = conexion.getResponseCode();

            if (codigoRespuesta == 200) {
                StringBuilder respuesta = new StringBuilder();
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conexion.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    String linea;
                    while ((linea = br.readLine()) != null) {
                        respuesta.append(linea);
                    }
                }

                String json = respuesta.toString();
                modelos = ParserModelosOllama.extraerModelosDesdeTags(json);
                if (modelos.isEmpty()) {
                    throw new RuntimeException("Ollama respondió sin modelos válidos en /api/tags");
                }
            } else {
                throw new RuntimeException("Ollama respondió con código HTTP " + codigoRespuesta + ". " +
                    "Asegúrate de que Ollama esté corriendo en " + urlBase.replace("/api/tags", ""));
            }

            conexion.disconnect();
        } catch (java.net.ConnectException e) {
            throw new RuntimeException("No se puede conectar con Ollama. " +
                "Asegúrate de que Ollama esté corriendo en http://localhost:11434");
        } catch (java.net.SocketTimeoutException e) {
            throw new RuntimeException("Tiempo de espera agotado al conectar con Ollama. " +
                "Verifica que Ollama esté corriendo.");
        } catch (Exception e) {
            throw new RuntimeException("Error al conectar con Ollama: " + e.getMessage() +
                ". Asegúrate de que Ollama esté corriendo en http://localhost:11434");
        }

        return modelos;
    }

    private void cargarModelosEnCombo(List<String> modelos, String preferido) {
        comboModelo.removeAllItems();
        comboModelo.addItem(MODELO_CUSTOM);

        List<String> modelosNormalizados = normalizarModelos(modelos);
        for (String modelo : modelosNormalizados) {
            comboModelo.addItem(modelo);
        }

        String preferidoNormalizado = normalizarModeloSeleccionado(preferido);
        if (preferidoNormalizado.isEmpty() && !modelosNormalizados.isEmpty()) {
            preferidoNormalizado = modelosNormalizados.get(0);
        }

        for (int i = 0; i < comboModelo.getItemCount(); i++) {
            if (preferidoNormalizado.equals(comboModelo.getItemAt(i))) {
                comboModelo.setSelectedIndex(i);
                return;
            }
        }

        comboModelo.setSelectedItem(MODELO_CUSTOM);
        comboModelo.getEditor().setItem(preferidoNormalizado);
    }

    private List<String> normalizarModelos(List<String> modelos) {
        Set<String> unicos = new LinkedHashSet<>();
        if (modelos != null) {
            for (String modelo : modelos) {
                String limpio = normalizarModeloSeleccionado(modelo);
                if (!limpio.isEmpty()) {
                    unicos.add(limpio);
                }
            }
        }
        return new ArrayList<>(unicos);
    }

    private String normalizarModeloSeleccionado(String modelo) {
        if (modelo == null) {
            return "";
        }
        String limpio = modelo.trim();
        return ":".equals(limpio) ? "" : limpio;
    }

    private java.util.List<String> obtenerModelosDesdeAPI(String proveedor) {
        if ("Gemini".equals(proveedor)) {
            try {
                okhttp3.OkHttpClient cliente = new okhttp3.OkHttpClient.Builder()
                    .connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
                return ConstructorSolicitudesProveedor.listarModelosGemini(
                    txtUrl.getText().trim(),
                    new String(txtClave.getPassword()).trim(),
                    cliente
                );
            } catch (Exception e) {
                throw new RuntimeException("No se pudieron obtener modelos Gemini: " + e.getMessage(), e);
            }
        }
        return ProveedorAI.obtenerModelosDisponibles(proveedor);
    }

    private void probarConexion() {
        String proveedorSeleccionado = (String) comboProveedor.getSelectedItem();
        if (proveedorSeleccionado == null) {
            JOptionPane.showMessageDialog(
                this,
                "Por favor selecciona un proveedor de AI",
                "Validación",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Validaciones antes de intentar conexión
        String urlApi = txtUrl.getText().trim();
        if (urlApi.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "La URL de API no puede estar vacía.\nPor favor ingresa una URL válida.",
                "Validación",
                JOptionPane.WARNING_MESSAGE
            );
            txtUrl.requestFocus();
            return;
        }

        String claveApi = new String(txtClave.getPassword()).trim();

        // Validar API key según proveedor
        ProveedorAI.ConfiguracionProveedor configProveedor = ProveedorAI.obtenerProveedor(proveedorSeleccionado);
        if (configProveedor != null && configProveedor.requiereClaveApi()) {
            if (claveApi.isEmpty()) {
                JOptionPane.showMessageDialog(
                    this,
                    "La clave de API no puede estar vacía para " + proveedorSeleccionado + ".\n" +
                    "Por favor ingresa tu clave de API.",
                    "Validación",
                    JOptionPane.WARNING_MESSAGE
                );
                txtClave.requestFocus();
                return;
            }
        }

        String modeloAUsar = obtenerModeloSeleccionado();
        if (modeloAUsar.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Por favor selecciona un modelo",
                "Validación",
                JOptionPane.WARNING_MESSAGE
            );
            comboModelo.requestFocus();
            return;
        }

        btnProbarConexion.setEnabled(false);
        btnProbarConexion.setText("🔍 Probando...");

        final String modeloFinal = modeloAUsar;
        SwingWorker<ProbadorConexionAI.ResultadoPrueba, Void> worker = new SwingWorker<>() {
            @Override
            protected ProbadorConexionAI.ResultadoPrueba doInBackground() {
                try {
                    ConfiguracionAPI configTemp = new ConfiguracionAPI();
                    configTemp.establecerProveedorAI(proveedorSeleccionado);
                    configTemp.establecerClaveApi(claveApi);
                    configTemp.establecerModelo(modeloFinal);
                    configTemp.establecerUrlApi(
                        ConfiguracionAPI.construirUrlApiProveedor(
                            proveedorSeleccionado,
                            urlApi,
                            modeloFinal
                        )
                    );

                    ProbadorConexionAI probador = new ProbadorConexionAI(configTemp);
                    return probador.probarConexion();
                } catch (Exception e) {
                    return new ProbadorConexionAI.ResultadoPrueba(
                        false,
                        "Error durante la prueba de conexión:\n\n" + e.getMessage(),
                        null
                    );
                }
            }

            @Override
            protected void done() {
                try {
                    ProbadorConexionAI.ResultadoPrueba resultado = get();
                    int tipoMensaje = resultado.exito ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE;
                    JOptionPane.showMessageDialog(
                        DialogoConfiguracion.this,
                        resultado.mensaje,
                        resultado.exito ? "✅ Conexión Exitosa" : "❌ Error de Conexión",
                        tipoMensaje
                    );
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                        DialogoConfiguracion.this,
                        "Error inesperado durante la prueba:\n\n" + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                } finally {
                    btnProbarConexion.setEnabled(true);
                    btnProbarConexion.setText("🔍 Probar Conexión");
                }
            }
        };

        worker.execute();
    }
}
