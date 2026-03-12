package com.burpia.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.InvocationType;
import com.burpia.config.AgenteTipo;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.flow.FlowAnalysisConstraints;
import com.burpia.i18n.I18nUI;
import com.burpia.util.Normalizador;

import javax.swing.*;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class FabricaMenuContextual implements ContextMenuItemsProvider {
    private final burp.api.montoya.MontoyaApi api;
    private final ConsumerSolicitud manejadorAnalisisSolicitud;
    private final ConsumerFlujo manejadorAnalisisFlujo;
    private final ConfiguracionAPI config;
    private final PredicateAgenteSolicitud manejadorAgenteSolicitud;
    private final PredicateAgenteFlujo manejadorAgenteFlujo;
    private final Runnable manejadorCambioAlertasEnviarA;
    private final Frame parentFrame;
    private final AtomicReference<RegistroClic> ultimoClic;
    private static final long VENTANA_DEBOUNCE_MS = 500L;

    public interface ConsumerSolicitudSinContexto {
        void analizarSolicitud(HttpRequest solicitud, boolean forzarAnalisis, HttpRequestResponse solicitudRespuestaOriginal);
    }

    public interface ConsumerSolicitud {
        void analizarSolicitud(HttpRequest solicitud, boolean forzarAnalisis, HttpRequestResponse solicitudRespuestaOriginal,
                ContextoInvocacion contextoInvocacion);
    }

    public interface ConsumerFlujo {
        void analizarFlujo(List<HttpRequestResponse> solicitudesRespuestaOriginales, ContextoInvocacion contextoInvocacion);
    }

    public interface PredicateAgenteSolicitud {
        boolean enviar(HttpRequestResponse solicitudRespuesta, ContextoInvocacion contextoInvocacion);
    }

    public interface PredicateAgenteFlujo {
        boolean enviar(List<HttpRequestResponse> solicitudesRespuesta, ContextoInvocacion contextoInvocacion);
    }

    public FabricaMenuContextual(MontoyaApi api,
                                 ConsumerSolicitud manejadorAnalisisSolicitud,
                                 ConsumerFlujo manejadorAnalisisFlujo,
                                 ConfiguracionAPI config,
                                 PredicateAgenteSolicitud manejadorAgenteSolicitud,
                                 PredicateAgenteFlujo manejadorAgenteFlujo,
                                 Runnable manejadorCambioAlertasEnviarA,
                                 Frame parentFrame) {
        this.api = api;
        this.manejadorAnalisisSolicitud = manejadorAnalisisSolicitud;
        this.manejadorAnalisisFlujo = manejadorAnalisisFlujo;
        this.config = config;
        this.manejadorAgenteSolicitud = manejadorAgenteSolicitud;
        this.manejadorAgenteFlujo = manejadorAgenteFlujo;
        this.manejadorCambioAlertasEnviarA = manejadorCambioAlertasEnviarA;
        this.parentFrame = parentFrame;
        this.ultimoClic = new AtomicReference<>();
    }

    public FabricaMenuContextual(MontoyaApi api,
                                 ConsumerSolicitudSinContexto manejadorAnalisisSolicitud,
                                 java.util.function.Consumer<List<HttpRequestResponse>> manejadorAnalisisFlujo,
                                 ConfiguracionAPI config,
                                 java.util.function.Predicate<HttpRequestResponse> manejadorAgenteSolicitud,
                                 java.util.function.Predicate<List<HttpRequestResponse>> manejadorAgenteFlujo,
                                 Runnable manejadorCambioAlertasEnviarA,
                                 Frame parentFrame) {
        this(
            api,
            manejadorAnalisisSolicitud != null
                ? (solicitud, forzarAnalisis, solicitudRespuestaOriginal, contextoInvocacion) ->
                    manejadorAnalisisSolicitud.analizarSolicitud(solicitud, forzarAnalisis, solicitudRespuestaOriginal)
                : null,
            manejadorAnalisisFlujo != null
                ? (solicitudesRespuestaOriginales, contextoInvocacion) ->
                    manejadorAnalisisFlujo.accept(solicitudesRespuestaOriginales)
                : null,
            config,
            manejadorAgenteSolicitud != null
                ? (solicitudRespuesta, contextoInvocacion) -> manejadorAgenteSolicitud.test(solicitudRespuesta)
                : null,
            manejadorAgenteFlujo != null
                ? (solicitudesRespuesta, contextoInvocacion) -> manejadorAgenteFlujo.test(solicitudesRespuesta)
                : null,
            manejadorCambioAlertasEnviarA,
            parentFrame
        );
    }
    @Override
    public List<Component> provideMenuItems(ContextMenuEvent evento) {
        List<Component> itemsMenu = new ArrayList<>();
        if (evento == null || Normalizador.esVacia(evento.selectedRequestResponses())) {
            return itemsMenu;
        }
        final List<HttpRequestResponse> seleccion = new ArrayList<>(evento.selectedRequestResponses());
        int cantidadSeleccionada = seleccion.size();
        final ContextoInvocacion contextoInvocacion = construirContextoInvocacion(evento, cantidadSeleccionada);

        if (cantidadSeleccionada == 1) {
            JMenuItem itemAnalizar = new JMenuItem(I18nUI.Contexto.ITEM_ANALIZAR_SOLICITUD());
            itemAnalizar.setFont(EstilosUI.FUENTE_ESTANDAR);
            itemAnalizar.setToolTipText(I18nUI.Tooltips.Contexto.ANALIZAR_SOLICITUD());
            itemAnalizar.addActionListener(e -> manejarAnalisisSeleccion(seleccion, contextoInvocacion));
            itemsMenu.add(itemAnalizar);
        } else if (cantidadSeleccionada >= 2) {
            JMenuItem itemFlujo = new JMenuItem(I18nUI.Contexto.ITEM_ANALIZAR_FLUJO());
            itemFlujo.setFont(EstilosUI.FUENTE_ESTANDAR);
            itemFlujo.setToolTipText(I18nUI.Tooltips.Contexto.ANALIZAR_FLUJO());
            itemFlujo.addActionListener(e -> manejarAnalisisFlujo(seleccion, contextoInvocacion));
            itemsMenu.add(itemFlujo);
        }

        if (config != null && config.hayAlgunAgenteHabilitado()) {
            String nombreAgente = AgenteTipo.obtenerNombreVisible(
                config.obtenerTipoAgenteOperativo(),
                I18nUI.General.AGENTE_GENERICO()
            );
            if (cantidadSeleccionada == 1 && manejadorAgenteSolicitud != null) {
                JMenuItem itemAgente = new JMenuItem(I18nUI.Contexto.ITEM_ANALIZAR_SOLICITUD_CON_AGENTE(nombreAgente));
                itemAgente.setFont(EstilosUI.FUENTE_ESTANDAR);
                itemAgente.setToolTipText(I18nUI.Tooltips.Contexto.ANALIZAR_SOLICITUD_CON_AGENTE(nombreAgente));
                itemAgente.addActionListener(e -> manejarEnvioAgenteSolicitud(seleccion, nombreAgente, contextoInvocacion));
                itemsMenu.add(itemAgente);
            } else if (cantidadSeleccionada >= 2 && manejadorAgenteFlujo != null) {
                JMenuItem itemAgenteFlujo = new JMenuItem(I18nUI.Contexto.ITEM_ANALIZAR_FLUJO_CON_AGENTE(nombreAgente));
                itemAgenteFlujo.setFont(EstilosUI.FUENTE_ESTANDAR);
                itemAgenteFlujo.setToolTipText(I18nUI.Tooltips.Contexto.ANALIZAR_FLUJO_CON_AGENTE(nombreAgente));
                itemAgenteFlujo.addActionListener(e -> manejarEnvioAgenteFlujo(seleccion, nombreAgente, contextoInvocacion));
                itemsMenu.add(itemAgenteFlujo);
            }
        }

        return itemsMenu;
    }

    private boolean manejarClicConDebounce(HttpRequest solicitud, HttpRequestResponse solicitudRespuestaOriginal,
            ContextoInvocacion contextoInvocacion) {
        if (solicitud == null || solicitudRespuestaOriginal == null) {
            return false;
        }
        String contenido = solicitud.toString();
        String hash = String.valueOf(contenido.hashCode());
        long ahora = System.currentTimeMillis();

        RegistroClic previo = ultimoClic.get();
        if (previo != null && hash.equals(previo.hashSolicitud) && (ahora - previo.timestampMs) < VENTANA_DEBOUNCE_MS) {
            api.logging().logToOutput(I18nUI.Contexto.LOG_DEBOUNCE_IGNORADO());
            return false;
        }

        ultimoClic.set(new RegistroClic(hash, ahora));

        if (manejadorAnalisisSolicitud == null) {
            return false;
        }
        manejadorAnalisisSolicitud.analizarSolicitud(solicitud, true, solicitudRespuestaOriginal, contextoInvocacion);
        return true;
    }

    private void manejarAnalisisSeleccion(List<HttpRequestResponse> seleccion, ContextoInvocacion contextoInvocacion) {
        if (Normalizador.esVacia(seleccion)) {
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
            if (manejarClicConDebounce(solicitud, rr, contextoInvocacion)) {
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

    private void manejarEnvioAgenteSolicitud(List<HttpRequestResponse> seleccion, String nombreAgente,
            ContextoInvocacion contextoInvocacion) {
        if (Normalizador.esVacia(seleccion) || manejadorAgenteSolicitud == null) {
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
                boolean enviada = manejadorAgenteSolicitud.enviar(rr, contextoInvocacion);
                if (enviada) {
                    exitosas++;
                } else {
                    fallidas++;
                }
            } catch (Exception ex) {
                fallidas++;
                api.logging().logToError(I18nUI.Contexto.LOG_ERROR_ENVIO_AGENTE(ex.getMessage()));
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

    private void manejarEnvioAgenteFlujo(List<HttpRequestResponse> seleccion, String nombreAgente,
            ContextoInvocacion contextoInvocacion) {
        if (Normalizador.esVacia(seleccion) || manejadorAgenteFlujo == null) {
            return;
        }
        if (!FlowAnalysisConstraints.tieneMinimoValido(seleccion)) {
            if (!GraphicsEnvironment.isHeadless()) {
                UIUtils.mostrarAdvertenciaConOptOutMenuContextual(
                    null,
                    I18nUI.Contexto.TITULO_FLUJO_REQUIERE_MULTIPLES(),
                    I18nUI.Contexto.MSG_FLUJO_REQUIERE_MULTIPLES_VALIDAS(),
                    alertasEnviarAHabilitadas(),
                    this::deshabilitarAlertasEnviarA
                );
            }
            return;
        }
        if (FlowAnalysisConstraints.excedeMaximoValido(seleccion)) {
            if (!GraphicsEnvironment.isHeadless()) {
                UIUtils.mostrarAdvertenciaConOptOutMenuContextual(
                    null,
                    I18nUI.Contexto.TITULO_FLUJO_REQUIERE_MULTIPLES(),
                    I18nUI.Contexto.MSG_FLUJO_MAXIMO_PETICIONES(FlowAnalysisConstraints.MAXIMO_PETICIONES_FLUJO),
                    alertasEnviarAHabilitadas(),
                    this::deshabilitarAlertasEnviarA
                );
            }
            return;
        }
        boolean enviada;
        try {
            enviada = manejadorAgenteFlujo.enviar(new ArrayList<>(seleccion), contextoInvocacion);
        } catch (Exception ex) {
            api.logging().logToError(I18nUI.Contexto.LOG_ERROR_ENVIO_AGENTE(ex.getMessage()));
            enviada = false;
        }

        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        String mensaje = enviada
            ? I18nUI.Contexto.MSG_ENVIO_AGENTE_FLUJO(nombreAgente, seleccion.size())
            : I18nUI.Contexto.MSG_ENVIO_AGENTE_FLUJO_ERROR(nombreAgente, seleccion.size());
        if (enviada) {
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
    
    private void manejarAnalisisFlujo(List<HttpRequestResponse> seleccion, ContextoInvocacion contextoInvocacion) {
        if (seleccion == null || seleccion.size() < 2) {
            if (GraphicsEnvironment.isHeadless()) {
                return;
            }
            UIUtils.mostrarAdvertenciaConOptOutMenuContextual(
                null,
                I18nUI.Contexto.TITULO_FLUJO_REQUIERE_MULTIPLES(),
                I18nUI.Contexto.MSG_FLUJO_REQUIERE_MULTIPLES(),
                alertasEnviarAHabilitadas(),
                this::deshabilitarAlertasEnviarA
            );
            return;
        }
        
        List<HttpRequestResponse> solicitudesValidas = FlowAnalysisConstraints.filtrarSolicitudesValidas(seleccion);
        if (!FlowAnalysisConstraints.tieneMinimoValido(seleccion)) {
            if (GraphicsEnvironment.isHeadless()) {
                return;
            }
            UIUtils.mostrarAdvertenciaConOptOutMenuContextual(
                null,
                I18nUI.Contexto.TITULO_FLUJO_REQUIERE_MULTIPLES(),
                I18nUI.Contexto.MSG_FLUJO_REQUIERE_MULTIPLES_VALIDAS(),
                alertasEnviarAHabilitadas(),
                this::deshabilitarAlertasEnviarA
            );
            return;
        }
        if (FlowAnalysisConstraints.excedeMaximoValido(seleccion)) {
            if (GraphicsEnvironment.isHeadless()) {
                return;
            }
            UIUtils.mostrarAdvertenciaConOptOutMenuContextual(
                null,
                I18nUI.Contexto.TITULO_FLUJO_REQUIERE_MULTIPLES(),
                I18nUI.Contexto.MSG_FLUJO_MAXIMO_PETICIONES(FlowAnalysisConstraints.MAXIMO_PETICIONES_FLUJO),
                alertasEnviarAHabilitadas(),
                this::deshabilitarAlertasEnviarA
            );
            return;
        }

        if (manejadorAnalisisFlujo == null) {
            return;
        }

        if (!GraphicsEnvironment.isHeadless()) {
            UIUtils.mostrarInfoConOptOutMenuContextual(
                null,
                I18nUI.Contexto.TITULO_FLUJO_INICIADO(),
                I18nUI.Contexto.MSG_FLUJO_INICIADO(solicitudesValidas.size()),
                alertasEnviarAHabilitadas(),
                this::deshabilitarAlertasEnviarA
            );
        }

        manejadorAnalisisFlujo.analizarFlujo(solicitudesValidas, contextoInvocacion);
    }

    private ContextoInvocacion construirContextoInvocacion(ContextMenuEvent evento, int cantidadSeleccionada) {
        InvocationType tipoInvocacion = InvocationType.PROXY_HISTORY;
        ToolType tipoHerramienta = ToolType.PROXY;
        if (evento != null) {
            try {
                if (evento.invocationType() != null) {
                    tipoInvocacion = evento.invocationType();
                }
            } catch (Exception ignored) {
            }
            try {
                if (evento.toolType() != null) {
                    tipoHerramienta = evento.toolType();
                }
            } catch (Exception ignored) {
            }
        }
        return new ContextoInvocacion(tipoInvocacion, tipoHerramienta, cantidadSeleccionada);
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

    private static final class RegistroClic {
        private final String hashSolicitud;
        private final long timestampMs;

        private RegistroClic(String hashSolicitud, long timestampMs) {
            this.hashSolicitud = hashSolicitud;
            this.timestampMs = timestampMs;
        }
    }

    public static final class ContextoInvocacion {
        private final InvocationType tipoInvocacion;
        private final ToolType tipoHerramienta;
        private final int cantidadSeleccionada;

        private ContextoInvocacion(InvocationType tipoInvocacion, ToolType tipoHerramienta, int cantidadSeleccionada) {
            this.tipoInvocacion = tipoInvocacion;
            this.tipoHerramienta = tipoHerramienta;
            this.cantidadSeleccionada = cantidadSeleccionada;
        }

        public InvocationType obtenerTipoInvocacion() {
            return tipoInvocacion;
        }

        public ToolType obtenerTipoHerramienta() {
            return tipoHerramienta;
        }

        public int obtenerCantidadSeleccionada() {
            return cantidadSeleccionada;
        }
    }

}
