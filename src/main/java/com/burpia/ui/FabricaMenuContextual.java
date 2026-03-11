package com.burpia.ui;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import com.burpia.config.AgenteTipo;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.flow.FlowAnalysisManager;
import com.burpia.flow.FlowAnalysisCallback;
import com.burpia.i18n.I18nUI;
import com.burpia.model.Hallazgo;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.LimitadorTasa;
import com.burpia.util.Normalizador;

import javax.swing.*;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.concurrent.atomic.AtomicReference;

public class FabricaMenuContextual implements ContextMenuItemsProvider {
    private final burp.api.montoya.MontoyaApi api;
    private final ConsumerSolicitud manejadorAnalisisSolicitud;
    private final Consumer<List<HttpRequestResponse>> manejadorAnalisisFlujo;
    private final ConfiguracionAPI config;
    private final Predicate<HttpRequestResponse> manejadorAgenteSolicitud;
    private final Predicate<List<HttpRequestResponse>> manejadorAgenteFlujo;
    private final Runnable manejadorCambioAlertasEnviarA;
    private final Frame parentFrame;
    private final AtomicReference<RegistroClic> ultimoClic;
    private final GestorLoggingUnificado gestorLogging;
    private final LimitadorTasa limitador;
    private final ModeloTablaHallazgos modeloTablaHallazgos;
    private FlowAnalysisManager flowAnalysisManager;
    private static final long VENTANA_DEBOUNCE_MS = 500L;

    public interface ConsumerSolicitud {
        void analizarSolicitud(HttpRequest solicitud, boolean forzarAnalisis, HttpRequestResponse solicitudRespuestaOriginal);
    }

    public FabricaMenuContextual(MontoyaApi api,
                                 ConsumerSolicitud manejadorAnalisisSolicitud,
                                 Consumer<List<HttpRequestResponse>> manejadorAnalisisFlujo,
                                 ConfiguracionAPI config,
                                 Predicate<HttpRequestResponse> manejadorAgenteSolicitud,
                                 Predicate<List<HttpRequestResponse>> manejadorAgenteFlujo,
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
        this.gestorLogging = GestorLoggingUnificado.crear(null, null, null, api, null);
        this.limitador = new LimitadorTasa(1);
        this.modeloTablaHallazgos = null;
    }
    
    public FabricaMenuContextual(MontoyaApi api,
                                 ConsumerSolicitud manejadorAnalisisSolicitud,
                                 Consumer<List<HttpRequestResponse>> manejadorAnalisisFlujo,
                                 ConfiguracionAPI config,
                                 Predicate<HttpRequestResponse> manejadorAgenteSolicitud,
                                 Predicate<List<HttpRequestResponse>> manejadorAgenteFlujo,
                                 Runnable manejadorCambioAlertasEnviarA,
                                 Frame parentFrame,
                                 GestorLoggingUnificado gestorLogging,
                                 LimitadorTasa limitador,
                                 ModeloTablaHallazgos modeloTablaHallazgos) {
        this.api = api;
        this.manejadorAnalisisSolicitud = manejadorAnalisisSolicitud;
        this.manejadorAnalisisFlujo = manejadorAnalisisFlujo;
        this.config = config;
        this.manejadorAgenteSolicitud = manejadorAgenteSolicitud;
        this.manejadorAgenteFlujo = manejadorAgenteFlujo;
        this.manejadorCambioAlertasEnviarA = manejadorCambioAlertasEnviarA;
        this.parentFrame = parentFrame;
        this.ultimoClic = new AtomicReference<>();
        this.gestorLogging = gestorLogging != null ? gestorLogging : GestorLoggingUnificado.crear(null, null, null, api, null);
        this.limitador = limitador != null ? limitador : new LimitadorTasa(1);
        this.modeloTablaHallazgos = modeloTablaHallazgos;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent evento) {
        List<Component> itemsMenu = new ArrayList<>();
        if (evento == null || Normalizador.esVacia(evento.selectedRequestResponses())) {
            return itemsMenu;
        }
        final List<HttpRequestResponse> seleccion = new ArrayList<>(evento.selectedRequestResponses());
        int cantidadSeleccionada = seleccion.size();

        if (cantidadSeleccionada == 1) {
            JMenuItem itemAnalizar = new JMenuItem(I18nUI.Contexto.ITEM_ANALIZAR_SOLICITUD());
            itemAnalizar.setFont(EstilosUI.FUENTE_ESTANDAR);
            itemAnalizar.setToolTipText(I18nUI.Tooltips.Contexto.ANALIZAR_SOLICITUD());
            itemAnalizar.addActionListener(e -> manejarAnalisisSeleccion(seleccion));
            itemsMenu.add(itemAnalizar);
        } else if (cantidadSeleccionada >= 2) {
            JMenuItem itemFlujo = new JMenuItem(I18nUI.Contexto.ITEM_ANALIZAR_FLUJO());
            itemFlujo.setFont(EstilosUI.FUENTE_ESTANDAR);
            itemFlujo.setToolTipText(I18nUI.Tooltips.Contexto.ANALIZAR_FLUJO());
            itemFlujo.addActionListener(e -> manejarAnalisisFlujo(seleccion));
            itemsMenu.add(itemFlujo);
        }

        if (config != null && config.agenteHabilitado()) {
            String nombreAgente = AgenteTipo.obtenerNombreVisible(
                config.obtenerTipoAgente(),
                I18nUI.General.AGENTE_GENERICO()
            );
            if (cantidadSeleccionada == 1 && manejadorAgenteSolicitud != null) {
                JMenuItem itemAgente = new JMenuItem(I18nUI.Contexto.ITEM_ANALIZAR_SOLICITUD_CON_AGENTE(nombreAgente));
                itemAgente.setFont(EstilosUI.FUENTE_ESTANDAR);
                itemAgente.setToolTipText(I18nUI.Tooltips.Contexto.ANALIZAR_SOLICITUD_CON_AGENTE(nombreAgente));
                itemAgente.addActionListener(e -> manejarEnvioAgenteSolicitud(seleccion, nombreAgente));
                itemsMenu.add(itemAgente);
            } else if (cantidadSeleccionada >= 2 && manejadorAgenteFlujo != null) {
                JMenuItem itemAgenteFlujo = new JMenuItem(I18nUI.Contexto.ITEM_ANALIZAR_FLUJO_CON_AGENTE(nombreAgente));
                itemAgenteFlujo.setFont(EstilosUI.FUENTE_ESTANDAR);
                itemAgenteFlujo.setToolTipText(I18nUI.Tooltips.Contexto.ANALIZAR_FLUJO_CON_AGENTE(nombreAgente));
                itemAgenteFlujo.addActionListener(e -> manejarEnvioAgenteFlujo(seleccion, nombreAgente));
                itemsMenu.add(itemAgenteFlujo);
            }
        }

        return itemsMenu;
    }

    private boolean manejarClicConDebounce(HttpRequest solicitud, HttpRequestResponse solicitudRespuestaOriginal) {
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
        manejadorAnalisisSolicitud.analizarSolicitud(solicitud, true, solicitudRespuestaOriginal);

        api.logging().logToOutput(I18nUI.Contexto.LOG_ANALISIS_FORZADO());
        return true;
    }

    private void manejarAnalisisSeleccion(List<HttpRequestResponse> seleccion) {
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

    private void manejarEnvioAgenteSolicitud(List<HttpRequestResponse> seleccion, String nombreAgente) {
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
                boolean enviada = manejadorAgenteSolicitud.test(rr);
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

    private void manejarEnvioAgenteFlujo(List<HttpRequestResponse> seleccion, String nombreAgente) {
        if (Normalizador.esVacia(seleccion) || manejadorAgenteFlujo == null) {
            return;
        }
        boolean enviada;
        try {
            enviada = manejadorAgenteFlujo.test(new ArrayList<>(seleccion));
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
    
    private void manejarAnalisisFlujo(List<HttpRequestResponse> seleccion) {
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
        
        List<HttpRequestResponse> solicitudesValidas = filtrarSolicitudesValidas(seleccion);
        if (solicitudesValidas.size() < 2) {
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

        if (manejadorAnalisisFlujo != null) {
            manejadorAnalisisFlujo.accept(solicitudesValidas);
            return;
        }

        if (flowAnalysisManager == null) {
            flowAnalysisManager = new FlowAnalysisManager(
                api,
                config,
                gestorLogging,
                limitador,
                null,
                modeloTablaHallazgos
            );
        }
        
        api.logging().logToOutput(I18nUI.Contexto.LOG_FLUJO_INICIADO(solicitudesValidas.size()));
        
        if (!GraphicsEnvironment.isHeadless()) {
            UIUtils.mostrarInfoConOptOutMenuContextual(
                null,
                I18nUI.Contexto.TITULO_FLUJO_INICIADO(),
                I18nUI.Contexto.MSG_FLUJO_INICIADO(solicitudesValidas.size()),
                alertasEnviarAHabilitadas(),
                this::deshabilitarAlertasEnviarA
            );
        }
        
        FlowAnalysisCallback callback = new FlowAnalysisCallback() {
            @Override
            public void onComplete(List<Hallazgo> hallazgos, List<String> urlsFlujo) {
                api.logging().logToOutput(I18nUI.Contexto.LOG_FLUJO_COMPLETADO(hallazgos != null ? hallazgos.size() : 0));
                
                if (!GraphicsEnvironment.isHeadless()) {
                    int cantidadHallazgos = hallazgos != null ? hallazgos.size() : 0;
                    int cantidadPeticiones = urlsFlujo != null ? urlsFlujo.size() : solicitudesValidas.size();
                    UIUtils.mostrarInfoConOptOutMenuContextual(
                        null,
                        I18nUI.Contexto.TITULO_FLUJO_COMPLETADO(),
                        I18nUI.Contexto.MSG_FLUJO_COMPLETADO(cantidadHallazgos, cantidadPeticiones),
                        alertasEnviarAHabilitadas(),
                        FabricaMenuContextual.this::deshabilitarAlertasEnviarA
                    );
                }
            }
            
            @Override
            public void onError(String error) {
                api.logging().logToError(I18nUI.Contexto.MSG_FLUJO_ERROR(error));
                
                if (!GraphicsEnvironment.isHeadless()) {
                    UIUtils.mostrarAdvertenciaConOptOutMenuContextual(
                        null,
                        I18nUI.Contexto.TITULO_FLUJO_ERROR(),
                        I18nUI.Contexto.MSG_FLUJO_ERROR(error),
                        alertasEnviarAHabilitadas(),
                        FabricaMenuContextual.this::deshabilitarAlertasEnviarA
                    );
                }
            }
            
            @Override
            public void onCancelled() {
                api.logging().logToOutput(I18nUI.Contexto.MSG_FLUJO_CANCELADO());
            }
        };
        
        flowAnalysisManager.ejecutarAnalisisFlujo(solicitudesValidas, callback);
    }

    private List<HttpRequestResponse> filtrarSolicitudesValidas(List<HttpRequestResponse> seleccion) {
        List<HttpRequestResponse> solicitudesValidas = new ArrayList<>();
        if (Normalizador.esVacia(seleccion)) {
            return solicitudesValidas;
        }
        for (HttpRequestResponse rr : seleccion) {
            if (rr != null && rr.request() != null) {
                solicitudesValidas.add(rr);
            }
        }
        return solicitudesValidas;
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

}
