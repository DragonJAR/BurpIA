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

import java.awt.Component;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import javax.swing.JMenuItem;
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
    private volatile boolean descargado = false;
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
        PanelAgente.ResultadoInyeccion enviar(HttpRequestResponse solicitudRespuesta, ContextoInvocacion contextoInvocacion);
    }

    public interface PredicateAgenteFlujo {
        PanelAgente.ResultadoInyeccion enviar(List<HttpRequestResponse> solicitudesRespuesta, ContextoInvocacion contextoInvocacion);
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
                    ? PanelAgente.ResultadoInyeccion.INYECTADO
                    : PanelAgente.ResultadoInyeccion.DESCARTADO
                : null,
            manejadorAgenteFlujo != null
                ? (solicitudesRespuesta, contextoInvocacion) -> manejadorAgenteFlujo.test(solicitudesRespuesta)
                    ? PanelAgente.ResultadoInyeccion.INYECTADO
                    : PanelAgente.ResultadoInyeccion.DESCARTADO
                : null,
            manejadorCambioAlertasEnviarA,
            parentFrame
        );
    }
    /**
     * Marca esta fábrica como descargada para que ignore futuras invocaciones.
     * La Montoya API no permite desregistrar un ContextMenuItemsProvider,
     * por lo que este flag evita invocaciones sobre recursos ya liberados.
     */
    public void marcarDescargado() {
        descargado = true;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent evento) {
        // Burp traga las excepciones de provideMenuItems y muestra el menú vacío sin avisar.
        // Las registramos para no quedar a ciegas (p.ej. por qué no sale en Repeater).
        try {
            return construirItemsMenu(evento);
        } catch (RuntimeException ex) {
            if (api != null && api.logging() != null) {
                api.logging().logToError(
                        "[FabricaMenuContextual] provideMenuItems lanzó; menú no mostrado: " + ex.getMessage(), ex);
            }
            return List.of();
        }
    }

    private List<Component> construirItemsMenu(ContextMenuEvent evento) {
        if (descargado) {
            return List.of();
        }

        List<Component> itemsMenu = new ArrayList<>();
        // Fuente de selección: preferimos la selección tabular; si no hay (p.ej. en un editor
        // individual de Repeater sin selección de filas), usamos el request del editor activo.
        List<HttpRequestResponse> baseSeleccion = evento != null ? evento.selectedRequestResponses() : null;
        if (Normalizador.esVacia(baseSeleccion) && evento != null
                && evento.messageEditorRequestResponse() != null
                && evento.messageEditorRequestResponse().isPresent()) {
            baseSeleccion = java.util.Collections.singletonList(
                    evento.messageEditorRequestResponse().get().requestResponse());
        }
        if (Normalizador.esVacia(baseSeleccion)) {
            // Diagnóstico: por qué no se produce menú (p.ej. Repeater). Best-effort.
            // Solo en modo registro detallado para no ensuciar el Output en uso normal.
            if (config != null && config.esDetallado() && api != null && api.logging() != null) {
                try {
                    int sel = evento != null && evento.selectedRequestResponses() != null
                            ? evento.selectedRequestResponses().size() : -1;
                    boolean editor = evento != null && evento.messageEditorRequestResponse() != null
                            && evento.messageEditorRequestResponse().isPresent();
                    api.logging().logToOutput("[FabricaMenuContextual] sin seleccion -> menu vacio."
                            + " selectedRequestResponses=" + sel
                            + " editorPresente=" + editor
                            + " tool=" + (evento != null ? String.valueOf(evento.toolType()) : "null")
                            + " invocation=" + (evento != null ? String.valueOf(evento.invocationType()) : "null"));
                } catch (RuntimeException diagEx) {
                    api.logging().logToOutput(
                            "[FabricaMenuContextual] sin seleccion -> menu vacio (diag err: " + diagEx.getMessage() + ")");
                }
            }
            return itemsMenu;
        }
        final List<HttpRequestResponse> seleccion = new ArrayList<>(baseSeleccion);
        int cantidadSeleccionada = seleccion.size();
        final ContextoInvocacion contextoInvocacion = construirContextoInvocacion(evento, cantidadSeleccionada);

        if (cantidadSeleccionada == 1) {
            itemsMenu.add(UIUtils.crearMenuItemContextual(
                I18nUI.Contexto.ITEM_ANALIZAR_SOLICITUD(),
                I18nUI.Tooltips.Contexto.ANALIZAR_SOLICITUD(),
                e -> manejarAnalisisSeleccion(seleccion, contextoInvocacion)
            ));
        } else if (cantidadSeleccionada >= 2) {
            itemsMenu.add(UIUtils.crearMenuItemContextual(
                I18nUI.Contexto.ITEM_ANALIZAR_FLUJO(),
                I18nUI.Tooltips.Contexto.ANALIZAR_FLUJO(),
                e -> manejarAnalisisFlujo(seleccion, contextoInvocacion)
            ));
        }

        boolean agenteHabilitado = config != null && config.hayAlgunAgenteHabilitado();
        if (agenteHabilitado) {
            String nombreAgente = AgenteTipo.obtenerNombreVisible(config, I18nUI.General.AGENTE_GENERICO());
            if (cantidadSeleccionada == 1 && manejadorAgenteSolicitud != null) {
                itemsMenu.add(UIUtils.crearMenuItemContextual(
                    I18nUI.Contexto.ITEM_ANALIZAR_SOLICITUD_CON_AGENTE(nombreAgente),
                    I18nUI.Tooltips.Contexto.ANALIZAR_SOLICITUD_CON_AGENTE(nombreAgente),
                    e -> manejarEnvioAgenteSolicitud(seleccion, nombreAgente, contextoInvocacion)
                ));
            } else if (cantidadSeleccionada >= 2 && manejadorAgenteFlujo != null) {
                itemsMenu.add(UIUtils.crearMenuItemContextual(
                    I18nUI.Contexto.ITEM_ANALIZAR_FLUJO_CON_AGENTE(nombreAgente),
                    I18nUI.Tooltips.Contexto.ANALIZAR_FLUJO_CON_AGENTE(nombreAgente),
                    e -> manejarEnvioAgenteFlujo(seleccion, nombreAgente, contextoInvocacion)
                ));
            }
        } else if (config != null) {
            // Con config presente pero sin agente habilitado: ítem deshabilitado (no oculto) con
            // tooltip explicativo, para que el usuario descubra la función y sepa cómo activarla.
            // (Cuando config es null —caso degenerado— se omite, manteniendo el contrato existente.)
            JMenuItem itemDeshabilitado = UIUtils.crearMenuItemContextual(
                I18nUI.Contexto.ITEM_AGENTE_DESHABILITADO(),
                I18nUI.Contexto.TOOLTIP_AGENTE_DESHABILITADO(),
                null
            );
            itemDeshabilitado.setEnabled(false);
            itemsMenu.add(itemDeshabilitado);
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
        // hashCode() tiene colisiones conocidas (ej: "Aa" vs "BB"), así que
        // además del hash verificamos equals del contenido completo antes de
        // descartar un clic como duplicado. Antes, dos requests distintos con
        // hashCode colisionante dentro de la ventana de debounce se trataban
        // erróneamente como duplicados.
        if (previo != null && hash.equals(previo.hashSolicitud)
                && previo.contenido != null && previo.contenido.equals(contenido)
                && (ahora - previo.timestampMs) < VENTANA_DEBOUNCE_MS) {
            api.logging().logToOutput(I18nUI.Contexto.LOG_DEBOUNCE_IGNORADO());
            return false;
        }

        ultimoClic.set(new RegistroClic(hash, contenido, ahora));

        if (manejadorAnalisisSolicitud == null) {
            return false;
        }
        manejadorAnalisisSolicitud.analizarSolicitud(solicitud, true, solicitudRespuestaOriginal, contextoInvocacion);
        return true;
    }

    private void manejarAnalisisSeleccion(List<HttpRequestResponse> seleccion, ContextoInvocacion contextoInvocacion) {
        int iniciadas;
        int omitidas;
        try {
            if (Normalizador.esVacia(seleccion)) {
                return;
            }
            iniciadas = 0;
            omitidas = 0;
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
        } catch (RuntimeException ex) {
            // El handler corre en el EDT; sin este catch la excepción escaparía
            // sin informar al usuario ni registrar el stack.
            api.logging().logToError(I18nUI.Contexto.MSG_ERROR_ANALISIS(ex.getMessage()), ex);
            if (!GraphicsEnvironment.isHeadless()) {
                UIUtils.mostrarError(parentFrame,
                    I18nUI.Contexto.TITULO_ERROR_ANALISIS(),
                    I18nUI.Contexto.MSG_ERROR_ANALISIS(ex.getMessage()));
            }
            return;
        }

        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        String mensaje = I18nUI.Contexto.MSG_ANALISIS_INICIADO_RESULTADO(iniciadas, seleccion.size(), omitidas);
        if (iniciadas > 0) {
            UIUtils.mostrarInfoConOptOutMenuContextual(
                parentFrame,
                I18nUI.Contexto.TITULO_ANALISIS_INICIADO(),
                mensaje,
                alertasEnviarAHabilitadas(),
                this::deshabilitarAlertasEnviarA
            );
        } else {
            UIUtils.mostrarAdvertenciaConOptOutMenuContextual(
                parentFrame,
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
        // Confirmar antes de enviar a un agente (ejecuta binarios externos sobre la solicitud).
        if (!UIUtils.confirmarAdvertencia(parentFrame,
                I18nUI.Contexto.TITULO_CONFIRMAR_ENVIO_AGENTE(),
                I18nUI.Contexto.MSG_CONFIRMAR_ENVIO_AGENTE_SOLICITUD(nombreAgente))) {
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
                PanelAgente.ResultadoInyeccion resultado = manejadorAgenteSolicitud.enviar(rr, contextoInvocacion);
                if (resultado != PanelAgente.ResultadoInyeccion.DESCARTADO) {
                    exitosas++;
                } else {
                    fallidas++;
                }
            } catch (Exception ex) {
                fallidas++;
                api.logging().logToError(I18nUI.Contexto.LOG_ERROR_ENVIO_AGENTE(ex.getMessage()), ex);
            }
        }

        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        String mensaje = I18nUI.Contexto.MSG_ENVIO_AGENTE_RESULTADO(nombreAgente, exitosas, seleccion.size(), fallidas);
        if (exitosas > 0) {
            UIUtils.mostrarInfoConOptOutMenuContextual(
                parentFrame,
                I18nUI.Contexto.TITULO_ENVIO_AGENTE(),
                mensaje,
                alertasEnviarAHabilitadas(),
                this::deshabilitarAlertasEnviarA
            );
        } else {
            // Las advertencias de fallo no son silenciables: el usuario no debe perder señales de error.
            UIUtils.mostrarAdvertencia(
                parentFrame,
                I18nUI.Contexto.TITULO_ENVIO_AGENTE(),
                mensaje
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
        // Confirmar antes de enviar múltiples solicitudes a un agente (binarios externos).
        if (!UIUtils.confirmarAdvertencia(parentFrame,
                I18nUI.Contexto.TITULO_CONFIRMAR_ENVIO_AGENTE(),
                I18nUI.Contexto.MSG_CONFIRMAR_ENVIO_AGENTE_FLUJO(nombreAgente, seleccion.size()))) {
            return;
        }
        boolean enviada;
        try {
            PanelAgente.ResultadoInyeccion resultado = manejadorAgenteFlujo.enviar(new ArrayList<>(seleccion), contextoInvocacion);
            enviada = resultado != PanelAgente.ResultadoInyeccion.DESCARTADO;
        } catch (Exception ex) {
            api.logging().logToError(I18nUI.Contexto.LOG_ERROR_ENVIO_AGENTE(ex.getMessage()), ex);
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
                parentFrame,
                I18nUI.Contexto.TITULO_ENVIO_AGENTE(),
                mensaje,
                alertasEnviarAHabilitadas(),
                this::deshabilitarAlertasEnviarA
            );
        } else {
            // Las advertencias de fallo no son silenciables: el usuario no debe perder señales de error.
            UIUtils.mostrarAdvertencia(
                parentFrame,
                I18nUI.Contexto.TITULO_ENVIO_AGENTE(),
                mensaje
            );
        }
    }
    
    private void manejarAnalisisFlujo(List<HttpRequestResponse> seleccion, ContextoInvocacion contextoInvocacion) {
        if (seleccion == null || seleccion.size() < 2) {
            if (GraphicsEnvironment.isHeadless()) {
                return;
            }
            UIUtils.mostrarAdvertenciaConOptOutMenuContextual(
                parentFrame,
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
                parentFrame,
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
                parentFrame,
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
                parentFrame,
                I18nUI.Contexto.TITULO_FLUJO_INICIADO(),
                I18nUI.Contexto.MSG_FLUJO_INICIADO(solicitudesValidas.size()),
                alertasEnviarAHabilitadas(),
                this::deshabilitarAlertasEnviarA
            );
        }

        try {
            manejadorAnalisisFlujo.analizarFlujo(solicitudesValidas, contextoInvocacion);
        } catch (RuntimeException ex) {
            // El handler corre en el EDT; sin este catch la excepción escaparía
            // sin informar al usuario ni registrar el stack.
            api.logging().logToError(I18nUI.Contexto.MSG_ERROR_ANALISIS(ex.getMessage()), ex);
            if (!GraphicsEnvironment.isHeadless()) {
                UIUtils.mostrarError(parentFrame,
                    I18nUI.Contexto.TITULO_ERROR_ANALISIS(),
                    I18nUI.Contexto.MSG_ERROR_ANALISIS(ex.getMessage()));
            }
        }
    }

    private ContextoInvocacion construirContextoInvocacion(ContextMenuEvent evento, int cantidadSeleccionada) {
        InvocationType tipoInvocacion = InvocationType.PROXY_HISTORY;
        ToolType tipoHerramienta = ToolType.PROXY;
        if (evento != null) {
            try {
                if (evento.invocationType() != null) {
                    tipoInvocacion = evento.invocationType();
                }
            // Best-effort: invocationType() may not be available in all Burp editions
            } catch (RuntimeException ex) {
                api.logging().logToOutput(I18nUI.Contexto.LOG_INVOCATION_TYPE_NO_DISPONIBLE(ex.getMessage()));
            }
            try {
                if (evento.toolType() != null) {
                    tipoHerramienta = evento.toolType();
                }
            // Best-effort: toolType() may not be available in all Burp editions
            } catch (RuntimeException ex) {
                api.logging().logToOutput(I18nUI.Contexto.LOG_TOOL_TYPE_NO_DISPONIBLE(ex.getMessage()));
            }
        }
        return new ContextoInvocacion(tipoInvocacion, tipoHerramienta, cantidadSeleccionada);
    }

    private boolean alertasEnviarAHabilitadas() {
        return AlertasOptOutHelper.evaluarAlertaEnviarA(
            AlertasOptOutHelper.ALERTA_MENU_ENVIAR_A, config,
            config == null ? () -> true : config::alertasClickDerechoEnviarAHabilitadas);
    }

    private void deshabilitarAlertasEnviarA() {
        AlertasOptOutHelper.deshabilitarAlertaEnviarA(
            AlertasOptOutHelper.ALERTA_MENU_ENVIAR_A, config,
            config != null && config.alertasClickDerechoEnviarAHabilitadas(),
            manejadorCambioAlertasEnviarA);
    }

    private static final class RegistroClic {
        private final String hashSolicitud;
        private final String contenido;
        private final long timestampMs;

        private RegistroClic(String hashSolicitud, String contenido, long timestampMs) {
            this.hashSolicitud = hashSolicitud;
            this.contenido = contenido;
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
