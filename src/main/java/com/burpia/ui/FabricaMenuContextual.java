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
import java.util.function.Predicate;
import java.util.concurrent.atomic.AtomicReference;

public class FabricaMenuContextual implements ContextMenuItemsProvider {
    private final burp.api.montoya.MontoyaApi api;
    private final ConsumerSolicitud manejadorAnalisis;
    private final ConfiguracionAPI config;
    private final Predicate<HttpRequestResponse> manejadorAgente;
    private final Runnable manejadorCambioAlertasEnviarA;
    private final AtomicReference<RegistroClic> ultimoClic;
    private static final long VENTANA_DEBOUNCE_MS = 500L;

    public interface ConsumerSolicitud {
        void analizarSolicitud(HttpRequest solicitud, boolean forzarAnalisis, HttpRequestResponse solicitudRespuestaOriginal);
    }

    public FabricaMenuContextual(MontoyaApi api,
                                 ConsumerSolicitud manejadorAnalisis,
                                 ConfiguracionAPI config,
                                 Predicate<HttpRequestResponse> manejadorAgente,
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
        final List<HttpRequestResponse> seleccion = new ArrayList<>(evento.selectedRequestResponses());

        JMenuItem itemAnalizar = new JMenuItem(I18nUI.Contexto.ITEM_ANALIZAR_SOLICITUD());
        itemAnalizar.setFont(EstilosUI.FUENTE_ESTANDAR);
        itemAnalizar.setToolTipText(I18nUI.Tooltips.Contexto.ANALIZAR_SOLICITUD());
        itemAnalizar.addActionListener(e -> manejarAnalisisSeleccion(seleccion));

        itemsMenu.add(itemAnalizar);

        if (config != null && config.agenteHabilitado() && manejadorAgente != null) {
            String nombreAgente = AgenteTipo.obtenerNombreVisible(
                config.obtenerTipoAgente(),
                I18nUI.General.AGENTE_GENERICO()
            );
            JMenuItem itemAgente = new JMenuItem(I18nUI.Contexto.MENU_ENVIAR_AGENTE(nombreAgente));
            itemAgente.setFont(EstilosUI.FUENTE_ESTANDAR);
            itemAgente.setToolTipText(I18nUI.Tooltips.Contexto.ENVIAR_A_AGENTE(nombreAgente));
            itemAgente.addActionListener(e -> manejarEnvioAgente(seleccion, nombreAgente));
            itemsMenu.add(itemAgente);
        }

        return itemsMenu;
    }

    private boolean manejarClicConDebounce(HttpRequest solicitud, HttpRequestResponse solicitudRespuestaOriginal) {
        if (solicitud == null || solicitudRespuestaOriginal == null) {
            return false;
        }
        String contenido = solicitud != null ? solicitud.toString() : "null";
        String hash = String.valueOf(contenido.hashCode());
        long ahora = System.currentTimeMillis();

        RegistroClic previo = ultimoClic.get();
        if (previo != null && hash.equals(previo.hashSolicitud) && (ahora - previo.timestampMs) < VENTANA_DEBOUNCE_MS) {
            api.logging().logToOutput(I18nUI.Contexto.LOG_DEBOUNCE_IGNORADO());
            return false;
        }

        ultimoClic.set(new RegistroClic(hash, ahora));

        manejadorAnalisis.analizarSolicitud(solicitud, true, solicitudRespuestaOriginal);

        api.logging().logToOutput(I18nUI.Contexto.LOG_ANALISIS_FORZADO());
        return true;
    }

    private void manejarAnalisisSeleccion(List<HttpRequestResponse> seleccion) {
        if (seleccion == null || seleccion.isEmpty()) {
            return;
        }
        int iniciadas = 0;
        int omitidas = 0;
        for (HttpRequestResponse rr : seleccion) {
            if (rr == null) {
                omitidas++;
                continue;
            }
            HttpRequest solicitud = rr.request();
            if (manejarClicConDebounce(solicitud, rr)) {
                iniciadas++;
            } else {
                omitidas++;
            }
        }

        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        String mensaje = I18nUI.Contexto.MSG_ANALISIS_INICIADO_RESULTADO(iniciadas, seleccion.size(), omitidas);
        if (iniciadas > 0) {
            UIUtils.mostrarInfoConOptOutMenuContextual(
                null,
                I18nUI.Contexto.TITULO_ANALISIS_INICIADO(),
                mensaje,
                alertasEnviarAHabilitadas(),
                this::deshabilitarAlertasEnviarA
            );
        } else {
            UIUtils.mostrarAdvertenciaConOptOutMenuContextual(
                null,
                I18nUI.Contexto.TITULO_ANALISIS_INICIADO(),
                mensaje,
                alertasEnviarAHabilitadas(),
                this::deshabilitarAlertasEnviarA
            );
        }
    }

    private void manejarEnvioAgente(List<HttpRequestResponse> seleccion, String nombreAgente) {
        if (seleccion == null || seleccion.isEmpty() || manejadorAgente == null) {
            return;
        }
        int exitosas = 0;
        int fallidas = 0;
        for (HttpRequestResponse rr : seleccion) {
            if (rr == null) {
                fallidas++;
                continue;
            }
            try {
                boolean enviada = manejadorAgente.test(rr);
                if (enviada) {
                    exitosas++;
                } else {
                    fallidas++;
                }
            } catch (Exception ex) {
                fallidas++;
                api.logging().logToError("[BurpIA] Error enviando solicitud al agente: " + ex.getMessage());
            }
        }

        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        String mensaje = I18nUI.Contexto.MSG_ENVIO_AGENTE_RESULTADO(nombreAgente, exitosas, seleccion.size(), fallidas);
        if (exitosas > 0) {
            UIUtils.mostrarInfoConOptOutMenuContextual(
                null,
                I18nUI.Contexto.TITULO_ENVIO_AGENTE(),
                mensaje,
                alertasEnviarAHabilitadas(),
                this::deshabilitarAlertasEnviarA
            );
        } else {
            UIUtils.mostrarAdvertenciaConOptOutMenuContextual(
                null,
                I18nUI.Contexto.TITULO_ENVIO_AGENTE(),
                mensaje,
                alertasEnviarAHabilitadas(),
                this::deshabilitarAlertasEnviarA
            );
        }
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
