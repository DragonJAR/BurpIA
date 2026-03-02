package com.burpia.ui;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import com.burpia.config.AgenteTipo;
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
    private final ConfiguracionAPI config;
    private final java.util.function.Consumer<HttpRequestResponse> manejadorAgente;
    private final Runnable manejadorCambioAlertasEnviarA;
    private final AtomicReference<RegistroClic> ultimoClic;
    private static final long VENTANA_DEBOUNCE_MS = 500L;

    public interface ConsumerSolicitud {
        void analizarSolicitud(HttpRequest solicitud, boolean forzarAnalisis, HttpRequestResponse solicitudRespuestaOriginal);
    }

    public FabricaMenuContextual(MontoyaApi api,
                                 ConsumerSolicitud manejadorAnalisis,
                                 ConfiguracionAPI config,
                                 java.util.function.Consumer<HttpRequestResponse> manejadorAgente,
                                 Runnable manejadorCambioAlertasEnviarA) {
        this.api = api;
        this.manejadorAnalisis = manejadorAnalisis;
        this.config = config;
        this.manejadorAgente = manejadorAgente;
        this.manejadorCambioAlertasEnviarA = manejadorCambioAlertasEnviarA;
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
        itemAnalizar.setFont(EstilosUI.FUENTE_ESTANDAR);
        itemAnalizar.setToolTipText(I18nUI.Tooltips.Contexto.ANALIZAR_SOLICITUD());
        itemAnalizar.addActionListener(e -> {
            manejarClicConDebounce(solicitudCapturada, solicitudRespuestaSeleccionada);
        });

        itemsMenu.add(itemAnalizar);

        if (config.agenteHabilitado()) {
            String nombreAgente = AgenteTipo.obtenerNombreVisible(
                config.obtenerTipoAgente(),
                I18nUI.General.AGENTE_GENERICO()
            );
            JMenuItem itemAgente = new JMenuItem(I18nUI.Contexto.MENU_ENVIAR_AGENTE(nombreAgente));
            itemAgente.setFont(EstilosUI.FUENTE_ESTANDAR);
            itemAgente.setToolTipText(I18nUI.Tooltips.Contexto.ENVIAR_A_AGENTE(nombreAgente));
            itemAgente.addActionListener(e -> {
                manejarEnvioAgente(solicitudRespuestaSeleccionada, nombreAgente);
            });
            itemsMenu.add(itemAgente);
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
            UIUtils.mostrarInfoConOptOut(
                null,
                I18nUI.Contexto.TITULO_ANALISIS_INICIADO(),
                I18nUI.Contexto.MSG_ANALISIS_INICIADO(),
                alertasEnviarAHabilitadas(),
                this::deshabilitarAlertasEnviarA
            );
        });
    }

    private void manejarEnvioAgente(HttpRequestResponse solicitudRespuestaSeleccionada, String nombreAgente) {
        try {
            manejadorAgente.accept(solicitudRespuestaSeleccionada);
        } catch (Exception ex) {
            api.logging().logToError("[BurpIA] Error enviando solicitud al agente: " + ex.getMessage());
            return;
        }
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        UIUtils.mostrarInfoConOptOut(
            null,
            I18nUI.Contexto.TITULO_ENVIO_AGENTE(),
            I18nUI.Contexto.MSG_ENVIO_AGENTE(nombreAgente),
            alertasEnviarAHabilitadas(),
            this::deshabilitarAlertasEnviarA
        );
    }

    private boolean alertasEnviarAHabilitadas() {
        return config == null || config.alertasClickDerechoEnviarAHabilitadas();
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

    private static final class RegistroClic {
        private final String hashSolicitud;
        private final long timestampMs;

        private RegistroClic(String hashSolicitud, long timestampMs) {
            this.hashSolicitud = hashSolicitud;
            this.timestampMs = timestampMs;
        }
    }

}
