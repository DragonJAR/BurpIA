package com.burpia.ui;

import com.burpia.bulk.CompositeProxyHistoryFilter;
import com.burpia.bulk.HistorialBurpProvider;
import com.burpia.util.Normalizador;

import javax.swing.*;
import java.awt.*;

/**
 * Diálogo para configurar filtros de historial antes de análisis bulk.
 */
public class DialogoFiltroHistorial extends JDialog {
    
    private final HistorialBurpProvider provider;
    private JTextField txtHost;
    private JTextField txtPath;
    private JComboBox<String> comboMetodo;
    private JTextField txtStatusMin;
    private JTextField txtStatusMax;
    private JTextField txtContentType;
    private JLabel lblPreview;
    private CompositeProxyHistoryFilter filtroResultante;
    
    public DialogoFiltroHistorial(Frame padre, HistorialBurpProvider provider) {
        super(padre, "Filtrar Historial para Análisis Bulk", true);
        this.provider = provider;
        inicializarUI();
    }
    
    private void inicializarUI() {
        setSize(500, 400);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout(10, 10));
        
        JPanel panelCampos = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        int fila = 0;
        
        gbc.gridx = 0; gbc.gridy = fila;
        panelCampos.add(new JLabel("Host (regex):"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        txtHost = new JTextField(20);
        panelCampos.add(txtHost, gbc);
        
        fila++;
        gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 1;
        panelCampos.add(new JLabel("Path (regex):"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        txtPath = new JTextField(20);
        panelCampos.add(txtPath, gbc);
        
        fila++;
        gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 1;
        panelCampos.add(new JLabel("Método:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        comboMetodo = new JComboBox<>(new String[]{"", "GET", "POST", "PUT", "DELETE", "PATCH"});
        panelCampos.add(comboMetodo, gbc);
        
        fila++;
        gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 1;
        panelCampos.add(new JLabel("Status Min:"), gbc);
        gbc.gridx = 1;
        txtStatusMin = new JTextField(5);
        panelCampos.add(txtStatusMin, gbc);
        gbc.gridx = 2;
        panelCampos.add(new JLabel("Status Max:"), gbc);
        gbc.gridx = 3;
        txtStatusMax = new JTextField(5);
        panelCampos.add(txtStatusMax, gbc);
        
        fila++;
        gbc.gridx = 0; gbc.gridy = fila; gbc.gridwidth = 1;
        panelCampos.add(new JLabel("Content-Type:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        txtContentType = new JTextField(20);
        panelCampos.add(txtContentType, gbc);
        
        add(panelCampos, BorderLayout.CENTER);
        
        JPanel panelBotones = new JPanel(new FlowLayout());
        JButton btnPreview = new JButton("Vista Previa");
        JButton btnOk = new JButton("Aceptar");
        JButton btnCancelar = new JButton("Cancelar");
        
        lblPreview = new JLabel(" ");
        panelBotones.add(lblPreview);
        panelBotones.add(btnPreview);
        panelBotones.add(btnOk);
        panelBotones.add(btnCancelar);
        
        add(panelBotones, BorderLayout.SOUTH);
        
        btnPreview.addActionListener(e -> actualizarPreview());
        btnOk.addActionListener(e -> {
            filtroResultante = construirFiltro();
            dispose();
        });
        btnCancelar.addActionListener(e -> {
            filtroResultante = null;
            dispose();
        });
    }
    
    private void actualizarPreview() {
        CompositeProxyHistoryFilter filtro = construirFiltro();
        int count = provider.obtenerHistorialFiltrado(filtro).size();
        lblPreview.setText(count + " items coinciden");
    }
    
    private CompositeProxyHistoryFilter construirFiltro() {
        CompositeProxyHistoryFilter.Builder builder = CompositeProxyHistoryFilter.builder();
        
        String host = txtHost.getText().trim();
        if (!host.isEmpty()) {
            builder.patronHost(host);
        }
        
        String path = txtPath.getText().trim();
        if (!path.isEmpty()) {
            builder.patronPath(path);
        }
        
        String metodo = (String) comboMetodo.getSelectedItem();
        if (Normalizador.noEsVacio(metodo)) {
            builder.metodo(metodo);
        }
        
        try {
            int statusMin = Integer.parseInt(txtStatusMin.getText().trim());
            int statusMax = Integer.parseInt(txtStatusMax.getText().trim());
            if (statusMin > 0 && statusMax >= statusMin) {
                builder.rangoCodigoEstado(statusMin, statusMax);
            }
        } catch (NumberFormatException e) {
            // Ignorar si no son números válidos
        }
        
        String contentType = txtContentType.getText().trim();
        if (!contentType.isEmpty()) {
            builder.tipoContenido(contentType);
        }
        
        return builder.build();
    }
    
    public CompositeProxyHistoryFilter obtenerFiltro() {
        return filtroResultante;
    }
}
