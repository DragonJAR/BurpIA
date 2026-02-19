package com.burpia.ui;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.config.GestorConfiguracion;
import com.burpia.config.ProveedorAI;
import com.burpia.util.ProbadorConexionAI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;

public class DialogoConfiguracion extends JDialog {
    private final ConfiguracionAPI config;
    private final GestorConfiguracion gestorConfig;
    private final Runnable alGuardar;

    private JTextField txtUrl;
    private JPasswordField txtClave;
    private JTextField txtRetraso;
    private JTextField txtMaximoConcurrente;
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
        setSize(800, 600);
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
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        int fila = 0;

        // Sección: Proveedor AI
        JPanel panelProveedor = crearPanelProveedor();
        gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(panelProveedor, gbc);

        fila++;

        // Retraso
        gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 1; gbc.weightx = 0;
        panel.add(new JLabel("Retraso (seg):"), gbc);

        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        txtRetraso = new JTextField(10);
        panel.add(txtRetraso, gbc);

        fila++;

        // Máximo Concurrente
        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        panel.add(new JLabel("Máximo Concurrente:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        txtMaximoConcurrente = new JTextField(10);
        panel.add(txtMaximoConcurrente, gbc);

        fila++;

        // Detallado
        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        panel.add(new JLabel("Modo Detallado:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        chkDetallado = new JCheckBox("Activar registro detallado (recomendado para depuración)");
        chkDetallado.setToolTipText("Cuando está activo, todas las operaciones del complemento se registran con información detallada");
        panel.add(chkDetallado, gbc);

        fila++;

        // Etiqueta de descripción
        gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 2;
        JLabel etiquetaDescripcion = new JLabel("El modo detallado registra todas las solicitudes HTTP, llamadas a API y operaciones internas");
        etiquetaDescripcion.setFont(EstilosUI.FUENTE_ESTANDAR);
        etiquetaDescripcion.setForeground(new Color(128, 128, 128));
        panel.add(etiquetaDescripcion, gbc);

        return panel;
    }

    private JPanel crearPanelProveedor() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
            "🤖 CONFIGURACIÓN DE PROVEEDOR AI",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            EstilosUI.FUENTE_NEGRITA
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
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

        btnRefrescarModelos = new JButton("🔄 Refresh");
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

        fila++;

        // URLs de referencia
        gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextArea urlsReferencia = new JTextArea();
        urlsReferencia.setEditable(false);
        urlsReferencia.setBackground(new Color(240, 248, 255));
        urlsReferencia.setFont(EstilosUI.FUENTE_TABLA);
        urlsReferencia.setText(
            "📌 URLs de referencia por proveedor:\n" +
            "   Ollama:   http://localhost:11434\n" +
            "   OpenAI:   https://api.openai.com/v1\n" +
            "   Claude:   https://api.anthropic.com/v1\n" +
            "   Gemini:   https://generativelanguage.googleapis.com/v1beta\n" +
            "   Z.ai:     https://api.z.ai/api/paas/v4\n" +
            "   minimax:  https://api.minimax.io/v1"
        );
        urlsReferencia.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 255), 1));
        panel.add(urlsReferencia, gbc);

        return panel;
    }

    private JPanel crearPanelPrompt() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel panelInstrucciones = new JPanel(new BorderLayout());
        panelInstrucciones.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 255), 1),
            "📝 INSTRUCCIONES",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            EstilosUI.FUENTE_NEGRITA
        ));

        JTextPane txtInstrucciones = new JTextPane();
        txtInstrucciones.setEditable(false);
        txtInstrucciones.setBackground(new Color(240, 248, 255));
        txtInstrucciones.setContentType("text/html");
        txtInstrucciones.setText(
            "<html><body style='font-family: monospace; font-size: 9px; padding: 5px;'>" +
            "<div style='margin-bottom: 8px;'>• El token <b>{REQUEST}</b> será reemplazado automáticamente con la solicitud HTTP analizada.</div>" +
            "<div style='margin-bottom: 8px;'>• Asegúrate de incluir <b>{REQUEST}</b> exactamente donde quieras que se inserte la petición.</div>" +
            "<div>• El prompt debe pedir una respuesta JSON con este formato:</div>" +
            "<div style='background: white; padding: 5px; margin-top: 5px; font-family: monospace; border: 1px solid #ccc;'>" +
            "{\"hallazgos\": [{\"descripcion\": \"string\", \"severidad\": \"Critical|High|Medium|Low|Info\", \"confianza\": \"High|Medium|Low\"}]}" +
            "</div></body></html>"
        );
        JScrollPane scrollInstrucciones = new JScrollPane(txtInstrucciones);
        scrollInstrucciones.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panelInstrucciones.add(scrollInstrucciones, BorderLayout.CENTER);
        panel.add(panelInstrucciones, BorderLayout.NORTH);

        JPanel panelEditor = new JPanel(new BorderLayout());
        panelEditor.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
            "✍️ PROMPT DE ANÁLISIS",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            EstilosUI.FUENTE_NEGRITA
        ));

        // Área de texto para el prompt
        txtPrompt = new JTextArea();
        txtPrompt.setFont(EstilosUI.FUENTE_TABLA);
        txtPrompt.setLineWrap(true);  // Habilitar line wrap para que no se salga horizontalmente
        txtPrompt.setWrapStyleWord(true);  // Cortar por palabras completas
        txtPrompt.setTabSize(2);

        JScrollPane scrollPrompt = new JScrollPane(txtPrompt);
        scrollPrompt.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPrompt.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);  // Sin scroll horizontal
        panelEditor.add(scrollPrompt, BorderLayout.CENTER);

        JPanel panelSur = new JPanel(new BorderLayout(10, 5));

        JPanel panelBotonesPrompt = new JPanel(new FlowLayout(FlowLayout.LEFT));

        btnRestaurarPrompt = new JButton("🔄 Restaurar Prompt por Defecto");
        btnRestaurarPrompt.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnRestaurarPrompt.setToolTipText("Restaura el prompt original de BurpIA");
        btnRestaurarPrompt.addActionListener(e -> restaurarPromptPorDefecto());
        panelBotonesPrompt.add(btnRestaurarPrompt);

        // Contador de caracteres
        lblContadorPrompt = new JLabel("Caracteres: 0");
        lblContadorPrompt.setFont(EstilosUI.FUENTE_TABLA);
        panelBotonesPrompt.add(lblContadorPrompt);

        panelSur.add(panelBotonesPrompt, BorderLayout.NORTH);

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
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 10, 30));

        JPanel panelCentral = new JPanel();
        panelCentral.setLayout(new BoxLayout(panelCentral, BoxLayout.Y_AXIS));
        panelCentral.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titulo = new JLabel("BurpIA");
        titulo.setFont(new Font(Font.MONOSPACED, Font.BOLD, 24));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelCentral.add(titulo);

        panelCentral.add(Box.createVerticalStrut(10));

        JLabel subtitulo = new JLabel("BurpIA - Análisis de Seguridad con Inteligencia Artificial");
        subtitulo.setFont(EstilosUI.FUENTE_NEGRITA);
        subtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelCentral.add(subtitulo);

        panelCentral.add(Box.createVerticalStrut(30));

        JPanel panelDescripcion = new JPanel(new BorderLayout(10, 10));
        panelDescripcion.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));

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
            "• Análisis automático con múltiples proveedores de IA\n" +
            "• Compatibilidad con OpenAI, Claude, Gemini, Z.ai, Minimax y Ollama\n" +
            "• Detección pasiva de vulnerabilidades en aplicaciones web\n" +
            "• Prompt configurable para análisis personalizados\n" +
            "• Interfaz intuitiva con estadísticas en tiempo real\n\n" +
            "BurpIA permite a auditores de seguridad, pentesters y desarrolladores " +
            "identificar debilidades con mayor eficiencia, potenciado por el uso " +
            "avanzado de modelos de lenguaje."
        );
        descripcion.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panelDescripcion.add(descripcion, BorderLayout.CENTER);
        panelCentral.add(panelDescripcion);

        panelCentral.add(Box.createVerticalStrut(40)); // Aumentado de 30 a 40 para más espaciado armónico

        JPanel panelAutor = new JPanel(new BorderLayout(10, 10));
        panelAutor.setMaximumSize(new Dimension(500, 100));
        panelAutor.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
            "👤 DESARROLLADO POR",
            javax.swing.border.TitledBorder.CENTER,
            javax.swing.border.TitledBorder.TOP,
            EstilosUI.FUENTE_NEGRITA
        ));

        JPanel panelInfoAutor = new JPanel();
        panelInfoAutor.setLayout(new BoxLayout(panelInfoAutor, BoxLayout.Y_AXIS));

        JLabel nombreAutor = new JLabel("Jaime Andrés Restrepo");
        nombreAutor.setFont(EstilosUI.FUENTE_NEGRITA);
        nombreAutor.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelInfoAutor.add(nombreAutor);

        JLabel organizacion = new JLabel("DragonJAR.org");
        organizacion.setFont(EstilosUI.FUENTE_ESTANDAR);
        organizacion.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelInfoAutor.add(organizacion);

        panelInfoAutor.add(Box.createVerticalStrut(10));

        JButton btnSitioWeb = new JButton("🌐 Visitar DragonJAR.org");
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
        btnSitioWeb.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelInfoAutor.add(btnSitioWeb);

        panelAutor.add(panelInfoAutor, BorderLayout.CENTER);
        panelCentral.add(panelAutor);

        panelCentral.add(Box.createVerticalStrut(30));

        JLabel lblVersion = new JLabel("Versión 1.0.0 - Febrero 2026");
        lblVersion.setFont(EstilosUI.FUENTE_ESTANDAR);
        lblVersion.setForeground(new Color(128, 128, 128));
        lblVersion.setAlignmentX(Component.CENTER_ALIGNMENT);
        panelCentral.add(lblVersion);

        panel.add(panelCentral, BorderLayout.CENTER);

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

        String urlApi = config.obtenerUrlApi();
        if (urlApi != null) {
            String urlBase = ConfiguracionAPI.extraerUrlBase(urlApi);
            txtUrl.setText(urlBase);
        }

        String claveApi = config.obtenerClaveApi();
        if (claveApi != null) {
            txtClave.setText(claveApi);
        }

        // Cargar Max Tokens del proveedor actual
        if (proveedorActual != null) {
            int maxTokens = config.obtenerMaxTokensParaProveedor(proveedorActual);
            txtMaxTokens.setText(String.valueOf(maxTokens));
        }

        String modeloActual = config.obtenerModelo();
        if (modeloActual != null) {
            boolean modeloEncontrado = false;
            for (int i = 0; i < comboModelo.getItemCount(); i++) {
                if (modeloActual.equals(comboModelo.getItemAt(i))) {
                    comboModelo.setSelectedIndex(i);
                    modeloEncontrado = true;
                    break;
                }
            }

            if (!modeloEncontrado) {
                comboModelo.addItem(modeloActual);
                comboModelo.setSelectedItem(modeloActual);
            }
        }

        txtRetraso.setText(String.valueOf(config.obtenerRetrasoSegundos()));
        txtMaximoConcurrente.setText(String.valueOf(config.obtenerMaximoConcurrente()));
        chkDetallado.setSelected(config.esDetallado());

        if (config.esPromptModificado()) {
            txtPrompt.setText(config.obtenerPromptConfigurable());
        } else {
            txtPrompt.setText(ConfiguracionAPI.obtenerPromptPorDefecto());
        }
        actualizarContador();
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
        config.establecerClaveApi(claveApi);
        config.establecerApiKeyParaProveedor(proveedorSeleccionado, claveApi);

        String modeloSeleccionado = (String) comboModelo.getSelectedItem();
        if (modeloSeleccionado != null && !modeloSeleccionado.equals(MODELO_CUSTOM)) {
            config.establecerModelo(modeloSeleccionado);
        } else {
            config.establecerModelo(comboModelo.getEditor().getItem().toString().trim());
        }

        String urlBase = txtUrl.getText().trim();
        String urlCompleta = ConfiguracionAPI.construirUrlApiProveedor(
            proveedorSeleccionado,
            urlBase,
            config.obtenerModelo()
        );
        config.establecerUrlApi(urlCompleta);
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
                    "Formato de numero invalido para retraso o solicitudes concurrentes",
                    "Error de Validacion",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void alCambiarProveedor() {
        String proveedorSeleccionado = (String) comboProveedor.getSelectedItem();
        if (proveedorSeleccionado == null) return;

        SwingUtilities.invokeLater(() -> {
            ProveedorAI.ConfiguracionProveedor configProveedor =
                ProveedorAI.obtenerProveedor(proveedorSeleccionado);

            if (configProveedor != null) {
                // URL base: usar guardada en config, si no hay usar default del proveedor
                String urlGuardada = config.obtenerUrlBaseParaProveedor(proveedorSeleccionado);
                if (urlGuardada != null && !urlGuardada.isEmpty()) {
                    txtUrl.setText(urlGuardada);
                } else {
                    // Usar URL por defecto del proveedor
                    txtUrl.setText(configProveedor.obtenerUrlApi());
                }

                // API Key: usar guardada, si no hay dejar vacío
                String apiKeyGuardada = config.obtenerApiKeyParaProveedor(proveedorSeleccionado);
                if (apiKeyGuardada != null && !apiKeyGuardada.isEmpty()) {
                    txtClave.setText(apiKeyGuardada);
                } else {
                    txtClave.setText("");
                }

                // Max Tokens: usar guardado, si no hay usar default del proveedor
                Integer maxTokensGuardado = config.obtenerMaxTokensParaProveedor(proveedorSeleccionado);
                if (maxTokensGuardado != null && maxTokensGuardado > 0) {
                    txtMaxTokens.setText(String.valueOf(maxTokensGuardado));
                } else {
                    txtMaxTokens.setText(String.valueOf(configProveedor.obtenerMaxTokensPorDefecto()));
                }

                // Actualizar lista de modelos
                comboModelo.removeAllItems();
                comboModelo.addItem(MODELO_CUSTOM);

                for (String modelo : configProveedor.obtenerModelosDisponibles()) {
                    comboModelo.addItem(modelo);
                }

                // Seleccionar primer modelo disponible (no el custom)
                if (comboModelo.getItemCount() > 1) {
                    comboModelo.setSelectedIndex(1);
                } else {
                    comboModelo.setSelectedItem(MODELO_CUSTOM);
                }
            }
        });
    }

    private void refrescarModelosDesdeAPI() {
        String proveedorSeleccionado = (String) comboProveedor.getSelectedItem();
        if (proveedorSeleccionado == null) return;

        btnRefrescarModelos.setEnabled(false);
        btnRefrescarModelos.setText("🔄 Refreshing...");

        SwingWorker<java.util.List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected java.util.List<String> doInBackground() {
                try {
                    String urlApi = txtUrl.getText().trim();
                    String claveApi = new String(txtClave.getPassword());

                    ConfiguracionAPI configTemp = new ConfiguracionAPI();
                    configTemp.establecerProveedorAI(proveedorSeleccionado);
                    configTemp.establecerUrlApi(urlApi);
                    configTemp.establecerClaveApi(claveApi);

                    if (proveedorSeleccionado.equals("Ollama")) {
                        return obtenerModelosOllama(urlApi);
                    } else {
                        return obtenerModelosDesdeAPI(proveedorSeleccionado, urlApi, claveApi);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(DialogoConfiguracion.this,
                            "Error al obtener modelos: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    java.util.List<String> modelos = get();
                    if (modelos != null && !modelos.isEmpty()) {
                        comboModelo.removeAllItems();
                        comboModelo.addItem(MODELO_CUSTOM);

                        for (String modelo : modelos) {
                            comboModelo.addItem(modelo);
                        }

                        if (comboModelo.getItemCount() > 1) {
                            comboModelo.setSelectedIndex(1);
                        }

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
                    btnRefrescarModelos.setText("🔄 Refresh");
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
                if (json.contains("\"models\"")) {
                    int inicio = json.indexOf("[");
                    int fin = json.lastIndexOf("]");
                    if (inicio > 0 && fin > inicio) {
                        String modelosJson = json.substring(inicio, fin + 1);
                        String[] partes = modelosJson.split("\"name\"");
                        for (int i = 1; i < partes.length; i++) {
                            int finNombre = partes[i].indexOf("\"");
                            if (finNombre > 0) {
                                String nombre = partes[i].substring(0, finNombre).replace(":\"", "").replace("\":\"", "").trim();
                                if (!nombre.isEmpty()) {
                                    modelos.add(nombre);
                                }
                            }
                        }
                    }
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

    private java.util.List<String> obtenerModelosDesdeAPI(String proveedor, String urlApi, String claveApi) {
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

        String modeloActual = (String) comboModelo.getSelectedItem();

        if (modeloActual == null || modeloActual.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Por favor selecciona un modelo",
                "Validación",
                JOptionPane.WARNING_MESSAGE
            );
            comboModelo.requestFocus();
            return;
        }

        // Determinar qué modelo usar: el seleccionado o el personalizado
        final String modeloAUsar;
        if (modeloActual.equals(MODELO_CUSTOM)) {
            String modeloPersonalizado = comboModelo.getEditor().getItem().toString().trim();
            if (modeloPersonalizado.isEmpty()) {
                JOptionPane.showMessageDialog(
                    this,
                    "El modelo personalizado no puede estar vacío.\n" +
                    "Por favor ingresa un nombre de modelo válido.",
                    "Validación",
                    JOptionPane.WARNING_MESSAGE
                );
                comboModelo.requestFocus();
                return;
            }
            modeloAUsar = modeloPersonalizado;
        } else {
            modeloAUsar = modeloActual;
        }

        btnProbarConexion.setEnabled(false);
        btnProbarConexion.setText("🔍 Probando...");

        SwingWorker<ProbadorConexionAI.ResultadoPrueba, Void> worker = new SwingWorker<>() {
            @Override
            protected ProbadorConexionAI.ResultadoPrueba doInBackground() {
                try {
                    ConfiguracionAPI configTemp = new ConfiguracionAPI();
                    configTemp.establecerProveedorAI(proveedorSeleccionado);
                    configTemp.establecerClaveApi(claveApi);
                    configTemp.establecerModelo(modeloAUsar);
                    configTemp.establecerUrlApi(
                        ConfiguracionAPI.construirUrlApiProveedor(
                            proveedorSeleccionado,
                            urlApi,
                            modeloAUsar
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
