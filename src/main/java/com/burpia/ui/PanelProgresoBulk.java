package com.burpia.ui;

import com.burpia.bulk.BulkProgressObserver;

import javax.swing.*;
import java.awt.*;

/**
 * Non-modal progress panel for bulk analysis operations.
 * <p>
 * Implements BulkProgressObserver to receive real-time updates and display
 * progress bar, current URL, and statistics during bulk analysis.
 * </p>
 */
public class PanelProgresoBulk extends JDialog implements BulkProgressObserver {
    
    private final JProgressBar progressBar;
    private final JLabel lblCurrentUrl;
    private final JLabel lblStats;
    private final JButton btnCancel;
    private volatile Runnable cancelAction;
    
    public PanelProgresoBulk(Frame padre) {
        super(padre, "Progreso Análisis Bulk", false);
        progressBar = new JProgressBar();
        lblCurrentUrl = new JLabel("Iniciando...");
        lblStats = new JLabel("0/0 procesados");
        btnCancel = new JButton("Cancelar");
        inicializarUI();
    }
    
    private void inicializarUI() {
        setSize(600, 200);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout(10, 10));
        
        JPanel panelCentro = new JPanel(new BorderLayout(5, 5));
        panelCentro.add(progressBar, BorderLayout.NORTH);
        panelCentro.add(new JScrollPane(lblCurrentUrl), BorderLayout.CENTER);
        panelCentro.add(lblStats, BorderLayout.SOUTH);
        
        add(panelCentro, BorderLayout.CENTER);
        add(btnCancel, BorderLayout.SOUTH);
        
        btnCancel.addActionListener(e -> {
            if (cancelAction != null) {
                cancelAction.run();
            }
        });
        
        progressBar.setStringPainted(true);
    }
    
    @Override
    public void onProgress(int processed, int total, String currentUrl) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setMaximum(total);
            progressBar.setValue(processed);
            lblCurrentUrl.setText(currentUrl != null ? currentUrl : "");
            lblStats.setText(processed + "/" + total + " procesados");
        });
    }
    
    @Override
    public void onComplete(int totalAnalyzed, int totalErrors) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(progressBar.getMaximum());
            lblCurrentUrl.setText("Completado");
            lblStats.setText("Total: " + totalAnalyzed + " | Errores: " + totalErrors);
            btnCancel.setEnabled(false);
        });
    }
    
    @Override
    public void onError(String error) {
        SwingUtilities.invokeLater(() -> {
            lblCurrentUrl.setText("Error: " + error);
            btnCancel.setEnabled(false);
        });
    }
    
    @Override
    public void onCancelled() {
        SwingUtilities.invokeLater(() -> {
            lblCurrentUrl.setText("Cancelado");
            btnCancel.setEnabled(false);
        });
    }
    
    public void establecerCancelAction(Runnable action) {
        this.cancelAction = action;
    }
}
