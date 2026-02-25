package com.burpia.ui;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nUI;
import com.burpia.util.OSUtils;
import com.jediterm.pty.PtyProcessTtyConnector;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.ui.JediTermWidget;
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class PanelFactoryDroid extends JPanel {
    private JediTermWidget terminalWidget;
    private final JPanel panelControles;
    private final JPanel panelResultadosWrapper;
    private final ConfiguracionAPI config;
    private PtyProcess process;
    private TtyConnector ttyConnector;
    private JLabel lblDelay;
    private JSpinner spinnerDelay;
    private Runnable alSolicitarFocoPestania;

    public PanelFactoryDroid(ConfiguracionAPI config) {
        this.config = config;
        setLayout(new BorderLayout(EstilosUI.MARGEN_PANEL, EstilosUI.MARGEN_PANEL));
        setBorder(BorderFactory.createEmptyBorder(EstilosUI.MARGEN_PANEL, EstilosUI.MARGEN_PANEL, EstilosUI.MARGEN_PANEL, EstilosUI.MARGEN_PANEL));

        panelControles = new JPanel(new FlowLayout(FlowLayout.LEFT, EstilosUI.ESPACIADO_COMPONENTES, 4));
        panelControles.setBorder(crearBordeTitulado(I18nUI.Consola.TITULO_CONTROLES(), 12, 16));

        JButton btnReiniciar = new JButton("🔄 " + I18nUI.Consola.BOTON_REINICIAR());
        btnReiniciar.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnReiniciar.setToolTipText(I18nUI.Consola.TOOLTIP_REINICIAR());
        btnReiniciar.addActionListener(e -> iniciarConsola());

        JButton btnCtrlC = new JButton("⚡ " + I18nUI.Consola.BOTON_CTRL_C());
        btnCtrlC.setFont(EstilosUI.FUENTE_ESTANDAR);
        btnCtrlC.setToolTipText(I18nUI.Consola.TOOLTIP_CTRL_C());
        btnCtrlC.addActionListener(e -> escribirComando("\u0003"));

        panelControles.add(btnReiniciar);
        panelControles.add(btnCtrlC);

        lblDelay = new JLabel(I18nUI.Consola.ETIQUETA_DELAY());
        lblDelay.setFont(EstilosUI.FUENTE_ESTANDAR);
        lblDelay.setToolTipText(I18nUI.Consola.TOOLTIP_DELAY());

        spinnerDelay = new JSpinner(new SpinnerNumberModel(config.obtenerAgenteFactoryDroidDelay(), 1000, 30000, 500));
        spinnerDelay.setFont(EstilosUI.FUENTE_ESTANDAR);
        spinnerDelay.setToolTipText(I18nUI.Consola.TOOLTIP_DELAY());
        spinnerDelay.setPreferredSize(new Dimension(80, 24));
        spinnerDelay.addChangeListener(e -> {
            int nuevoDelay = (int) spinnerDelay.getValue();
            config.establecerAgenteFactoryDroidDelay(nuevoDelay);
        });

        panelControles.add(new JSeparator(SwingConstants.VERTICAL));
        panelControles.add(lblDelay);
        panelControles.add(spinnerDelay);

        panelResultadosWrapper = new JPanel(new BorderLayout());
        panelResultadosWrapper.setBorder(crearBordeTitulado(I18nUI.Pestanias.AGENTE_FACTORY_DROID(), 3, 3));

        add(panelControles, BorderLayout.NORTH);
        add(panelResultadosWrapper, BorderLayout.CENTER);

        iniciarConsola();
    }

    public void establecerManejadorFocoPestania(Runnable manejador) {
        this.alSolicitarFocoPestania = manejador;
    }

    private static final java.util.concurrent.ExecutorService inyectorPty = java.util.concurrent.Executors.newSingleThreadExecutor();

    public void escribirComando(String comando) {
        if (comando == null) return;
        inyectorPty.submit(() -> {
            try {
                String preparado = OSUtils.prepararComando(comando);
                
                if (ttyConnector != null) {
                    ttyConnector.write(preparado);
                } else if (process != null) {
                    java.io.OutputStream os = process.getOutputStream();
                    os.write(preparado.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
            } catch (Exception ignored) {
            }
        });
    }

    public void inyectarComandoViaPortapapeles(String texto) {
        if (texto == null || texto.isEmpty()) return;
        
        SwingUtilities.invokeLater(() -> {
            // 1. Limpiar obstáculos de forma selectiva (solo ventana de ajustes)
            OSUtils.cerrarVentanaAjustes();
            
            // 1.5. Solicitar a la pestaña principal que se vuelva visible
            if (alSolicitarFocoPestania != null) {
                alSolicitarFocoPestania.run();
            }
            
            // 2. Traer ventana de Burp al frente y solicitar foco para este panel
            OSUtils.forzarEnfoqueVentana(this);

            Runnable accionarPegado = () -> {
                // 3. Asegurar que el terminal tenga el foco de forma garantizada
                if (terminalWidget != null && terminalWidget.getTerminalPanel() != null) {
                    terminalWidget.getTerminalPanel().requestFocusInWindow();
                    terminalWidget.getTerminalPanel().requestFocus();
                }
                
                // 4. Copiar el contenido
                OSUtils.copiarAlPortapapeles(texto);
                
                // 5. Un poco más de tiempo para el cambio de contexto del SO y estabilidad
                new Timer(250, e -> {
                    OSUtils.pegarDesdePortapapeles();
                    ((Timer)e.getSource()).stop();
                    
                    // 6. Retardo tras el pegado antes del Enter (para que el terminal procese el paste)
                    new Timer(300, e2 -> {
                        escribirComando(""); 
                        ((Timer)e2.getSource()).stop();
                    }).start();
                }).start();
            };

            if (this.isShowing()) {
                accionarPegado.run();
            } else {
                java.awt.event.HierarchyListener listener = new java.awt.event.HierarchyListener() {
                    @Override
                    public void hierarchyChanged(java.awt.event.HierarchyEvent e) {
                        if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && PanelFactoryDroid.this.isShowing()) {
                            accionarPegado.run();
                            PanelFactoryDroid.this.removeHierarchyListener(this);
                        }
                    }
                };
                this.addHierarchyListener(listener);
            }
        });
    }

    private void iniciarConsola() {
        destruir();

        terminalWidget = new JediTermWidget(120, 24, new DefaultSettingsProvider() {
            @Override
            public float getTerminalFontSize() { return 14; }
            @Override
            public boolean useAntialiasing() { return true; }
            @Override
            public boolean copyOnSelect() { return true; }
            @Override
            public boolean pasteOnMiddleMouseClick() { return true; }
        });
        terminalWidget.getTerminalPanel().setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        panelResultadosWrapper.removeAll();
        panelResultadosWrapper.add(terminalWidget, BorderLayout.CENTER);
        panelResultadosWrapper.revalidate();
        panelResultadosWrapper.repaint();

        new Thread(() -> {
            try {
                Map<String, String> env = new HashMap<>(System.getenv());
                env.put("TERM", "xterm-256color");

                String[] command;
                if (OSUtils.esWindows()) {
                    command = new String[]{"cmd.exe"};
                } else {
                    String shell = System.getenv("SHELL");
                    if (shell == null || shell.trim().isEmpty()) {
                        shell = "/bin/bash";
                    }
                    command = new String[]{shell, "--login"};
                }

                process = new PtyProcessBuilder()
                    .setCommand(command)
                    .setEnvironment(env)
                    .start();

                ttyConnector = new PtyProcessTtyConnector(process, StandardCharsets.UTF_8);

                SwingUtilities.invokeLater(() -> {
                    terminalWidget.setTtyConnector(ttyConnector);
                    terminalWidget.start();

                    // 1. Iniciar el binario inmediatamente
                    new Timer(500, evt -> {
                        String binarioConfig = config.obtenerAgenteFactoryDroidBinario();
                        String binario;
                        if (binarioConfig == null || binarioConfig.trim().isEmpty()) {
                            binario = "droid";
                        } else {
                            if (OSUtils.esWindows() && binarioConfig.startsWith("%USERPROFILE%")) {
                                binario = binarioConfig.replace("%USERPROFILE%", System.getProperty("user.home"));
                            } else if (binarioConfig.startsWith("~/")) {
                                binario = binarioConfig.replaceFirst("^~", System.getProperty("user.home"));
                            } else {
                                binario = binarioConfig;
                            }
                        }
                        
                        // Iniciar el agente de forma normal
                        escribirComando(binario);
                        ((Timer)evt.getSource()).stop();

                    // 2. Esperar el tiempo configurado y usar Pegado para el prompt inicial
                    new Timer(config.obtenerAgenteFactoryDroidDelay(), evt2 -> {
                        String promptInicial = "BURPAI CRITICAL PRE-FLIGHT CHECK: Before executing any task, perform an immediate and complete inventory of all MCP tools currently available to you.\n\n" +
                                               "Follow this enhanced discovery protocol strictly:\n\n" +
                                               "1. **Full MCP Inventory**: List EVERY active MCP connection and tool with its exact name, description, and capabilities.\n\n" +
                                               "2. **Burp Suite MCP Detection & Priority**:\n" +
                                               "   - The Burp Suite MCP might be registered under various aliases such as `burp`, `burpsuite`, `burp suite`, `burp-mcp`, `burp_mcp` or `mcp_burp`.\n" +
                                               "   - Expected tool patterns: `mcp_burp_*`, `burp_*`, `send_http*`, `get_proxy_*`, `get_scanner_issues`, `create_repeater_tab`,\n" +
                                               "     `send_to_intruder`, `generate_collaborator_payload`, `set_proxy_intercept_state`, etc.\n" +
                                               "   - **KNOWN ISSUE**: Even if Burp MCP is installed and the server is running (port listening, process active),\n" +
                                               "     the tools may not appear in the current Droid session. In this case, IMMEDIATELY perform 'retry connection',\n" +
                                               "     'reintentar conexión' or 'refresh/reconnect Burp MCP' to enable all tools in this session.\n" +
                                               "     Do not proceed with any task until Burp tools are visible.\n" +
                                               "   - Once available, Burp Suite MCP (under any alias) is ALWAYS your HIGHEST PRIORITY and PRIMARY interface.\n\n" +
                                               "3. **Capability Classification**: Categorize all discovered tools strictly into the following groups:\n\n" +
                                               "   - [CATEGORY A] (HIGH PRIORITY) — Burp Suite MCP: Active HTTP interaction, Repeater, Intruder, Proxy, Scanner, Collaborator, Editor.\n" +
                                               "   - [CATEGORY B] — Additional MCP tools: Other configured standard MCP servers.\n" +
                                               "   - [CATEGORY C] — Native capabilities: Built-in shell execution, curl, scripting environments.\n\n" +
                                               "Respond ONLY with this structured inventory report before awaiting further instructions.";

                        inyectarComandoViaPortapapeles(promptInicial);
                        ((Timer)evt2.getSource()).stop();
                    }).start();
                    }).start();
                });

            } catch (Throwable t) {
                try {
                    java.io.FileWriter fw = new java.io.FileWriter("~/burpia_pty_error.log", true);
                    fw.write(I18nUI.Consola.HEADER_LOG_ERROR_PTY());
                    java.io.PrintWriter pw = new java.io.PrintWriter(fw);
                    t.printStackTrace(pw);
                    pw.close();

                } catch(Exception ignored) {}

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(PanelFactoryDroid.this, I18nUI.Consola.MSG_ERROR_NATIVO_PTY(t.toString()), I18nUI.Consola.TITULO_ERROR_PTY(), JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private Border crearBordeTitulado(String titulo, int pV, int pH) {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstilosUI.COLOR_BORDE_PANEL, 1),
                titulo,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                EstilosUI.FUENTE_NEGRITA
            ),
            BorderFactory.createEmptyBorder(pV, pH, pV, pH)
        );
    }

    public void aplicarIdioma() {
        actualizarTituloPanel(panelControles, I18nUI.Consola.TITULO_CONTROLES());
        actualizarTituloPanel(panelResultadosWrapper, I18nUI.Pestanias.AGENTE_FACTORY_DROID());
        
        if (lblDelay != null) {
            lblDelay.setText(I18nUI.Consola.ETIQUETA_DELAY());
            lblDelay.setToolTipText(I18nUI.Consola.TOOLTIP_DELAY());
        }
        if (spinnerDelay != null) {
            spinnerDelay.setToolTipText(I18nUI.Consola.TOOLTIP_DELAY());
        }

        revalidate();
        repaint();
    }

    private void actualizarTituloPanel(JPanel panel, String titulo) {
        Border borde = panel.getBorder();
        if (borde instanceof CompoundBorder) {
            Border externo = ((CompoundBorder) borde).getOutsideBorder();
            if (externo instanceof TitledBorder) {
                ((TitledBorder) externo).setTitle(titulo);
            }
        }
    }

    public void destruir() {
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
        if (ttyConnector != null) {
            try { ttyConnector.close(); } catch (Exception ignored) {}
        }
    }
}
