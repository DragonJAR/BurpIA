package com.burpia.ui;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nUI;
import javax.swing.*;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class FabricaMenuContextual implements ContextMenuItemsProvider {
    private final burp.api.montoya.MontoyaApi api;
    private final ConsumerSolicitud manejadorAnalisis;
    private final com.burpia.config.ConfiguracionAPI config;
    private final java.util.function.Consumer<HttpRequestResponse> manejadorFactoryDroid;
    private final AtomicReference<RegistroClic> ultimoClic;
    private static final long VENTANA_DEBOUNCE_MS = 500L;

    public interface ConsumerSolicitud {
        void analizarSolicitud(HttpRequest solicitud, boolean forzarAnalisis, HttpRequestResponse solicitudRespuestaOriginal);
    }

    public FabricaMenuContextual(MontoyaApi api, ConsumerSolicitud manejadorAnalisis, com.burpia.config.ConfiguracionAPI config, java.util.function.Consumer<HttpRequestResponse> manejadorFactoryDroid) {
        this.api = api;
        this.manejadorAnalisis = manejadorAnalisis;
        this.config = config;
        this.manejadorFactoryDroid = manejadorFactoryDroid;
        this.ultimoClic = new AtomicReference<>();
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent evento) {
        List<Component> itemsMenu = new ArrayList<>();

        if (evento == null || evento.selectedRequestResponses() == null || evento.selectedRequestResponses().isEmpty()) {
            return itemsMenu;
        }

        final HttpRequestResponse solicitudRespuestaSeleccionada = evento.selectedRequestResponses().get(0);
        final HttpRequest solicitudCapturada = solicitudRespuestaSeleccionada.request();

        JMenuItem itemAnalizar = new JMenuItem(I18nUI.Contexto.ITEM_ANALIZAR_SOLICITUD());
        itemAnalizar.setToolTipText(I18nUI.Tooltips.Contexto.ANALIZAR_SOLICITUD());
        itemAnalizar.addActionListener(e -> {
            manejarClicConDebounce(solicitudCapturada, solicitudRespuestaSeleccionada);
        });

        itemsMenu.add(itemAnalizar);

        if (config.agenteFactoryDroidHabilitado()) {
            JMenuItem itemFactoryDroid = new JMenuItem(I18nUI.Contexto.MENU_ENVIAR_FACTORY_DROID());
            itemFactoryDroid.addActionListener(e -> {
                manejadorFactoryDroid.accept(solicitudRespuestaSeleccionada);
            });
            itemsMenu.add(itemFactoryDroid);
        }

        return itemsMenu;
    }

    private void manejarClicConDebounce(HttpRequest solicitud, HttpRequestResponse solicitudRespuestaOriginal) {
        String contenido = solicitud != null ? solicitud.toString() : "null";
        String hash = String.valueOf(contenido.hashCode());
        long ahora = System.currentTimeMillis();

        RegistroClic previo = ultimoClic.get();
        if (previo != null && hash.equals(previo.hashSolicitud) && (ahora - previo.timestampMs) < VENTANA_DEBOUNCE_MS) {
            api.logging().logToOutput(I18nUI.Contexto.LOG_DEBOUNCE_IGNORADO());
            return;
        }

        ultimoClic.set(new RegistroClic(hash, ahora));

        manejadorAnalisis.analizarSolicitud(solicitud, true, solicitudRespuestaOriginal);

        api.logging().logToOutput(I18nUI.Contexto.LOG_ANALISIS_FORZADO());

        SwingUtilities.invokeLater(() -> {
            if (GraphicsEnvironment.isHeadless()) {
                return;
            }
            JOptionPane.showMessageDialog(
                null,
                I18nUI.Contexto.MSG_ANALISIS_INICIADO(),
                I18nUI.Contexto.TITULO_ANALISIS_INICIADO(),
                JOptionPane.INFORMATION_MESSAGE
            );
        });
    }

    private static final class RegistroClic {
        private final String hashSolicitud;
        private final long timestampMs;

        private RegistroClic(String hashSolicitud, long timestampMs) {
            this.hashSolicitud = hashSolicitud;
            this.timestampMs = timestampMs;
        }
    }

}
