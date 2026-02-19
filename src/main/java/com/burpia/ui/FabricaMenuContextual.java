package com.burpia.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;

import javax.swing.*;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class FabricaMenuContextual implements ContextMenuItemsProvider {
    private final MontoyaApi api;
    private final ConsumerSolicitud manejadorAnalisis;
    private final ConcurrentHashMap<String, Long> ultimoClic;

    public interface ConsumerSolicitud {
        void analizarSolicitud(String solicitud, boolean forzarAnalisis);
    }

    public FabricaMenuContextual(MontoyaApi api, ConsumerSolicitud manejadorAnalisis) {
        this.api = api;
        this.manejadorAnalisis = manejadorAnalisis;
        this.ultimoClic = new ConcurrentHashMap<>();
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent evento) {
        List<Component> itemsMenu = new ArrayList<>();

        if (evento.selectedRequestResponses().isEmpty()) {
            return itemsMenu;
        }

        // Capturar la solicitud ANTES de crear el ActionListener para evitar problemas de scope
        final String solicitudCapturada = evento.selectedRequestResponses().get(0).request().toString();

        JMenuItem itemAnalizar = new JMenuItem("Analizar Solicitud con BurpIA");
        itemAnalizar.addActionListener(e -> {
            manejarClicConDebounce(solicitudCapturada);
        });

        itemsMenu.add(itemAnalizar);

        return itemsMenu;
    }

    private void manejarClicConDebounce(String solicitud) {
        String hash = String.valueOf(solicitud.hashCode());
        long ahora = System.currentTimeMillis();

        Long ultimoClicTime = ultimoClic.get(hash);
        if (ultimoClicTime != null && (ahora - ultimoClicTime) < 500) {
            api.logging().logToOutput("[BurpIA] Debounce: ignorando clic duplicado");
            return;
        }

        ultimoClic.put(hash, ahora);

        // Llamar al manejador con forzarAnalisis=true
        manejadorAnalisis.analizarSolicitud(solicitud, true);

        api.logging().logToOutput("[BurpIA] Analizando solicitud desde menu contextual (forzado)");

        // Mostrar dialogo opcional
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                null,
                "Solicitud enviada para analisis forzado.\n" +
                "Esto puede tomar unos segundos dependiendo de la respuesta de la AI.",
                "BurpIA - Análisis Iniciado",
                JOptionPane.INFORMATION_MESSAGE
            );
        });
    }

    public void limpiarDebounceAntiguo() {
        long ahora = System.currentTimeMillis();
        ultimoClic.entrySet().removeIf(entry -> {
            Long tiempo = entry.getValue();
            return tiempo == null || (ahora - tiempo) > 60000; // 1 minuto
        });
    }
}
