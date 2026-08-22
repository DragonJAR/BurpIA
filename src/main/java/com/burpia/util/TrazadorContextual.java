package com.burpia.util;

import burp.api.montoya.http.message.HttpRequestResponse;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nLogs;
import com.burpia.processor.HttpRequestProcessor;
import com.burpia.ui.FabricaMenuContextual;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Trazado verbose de acciones contextuales (menú contextual, análisis forzado,
 * envío a agente). Centraliza la lógica que antes estaba duplicada — y ya
 * divergente — entre {@code ManejadorHttpBurpIA} y {@code ExtensionBurpIA}.
 *
 * <p>La clase no tiene estado propio: la configuración vigente, el procesador
 * HTTP y el destino del log se inyectan como suppliers/consumidor para que cada
 * clase dueña conserve su fuente (p. ej. {@code configRef} en el manejador) y
 * su canal de salida (prefijos y nivel de log distintos).</p>
 *
 * <p>Todos los métodos son no-op seguros cuando la configuración es null o el
 * modo detallado está desactivado; nunca lanzan por contexto, solicitud o
 * procesador nulos.</p>
 */
public final class TrazadorContextual {

    private final Supplier<ConfiguracionAPI> proveedorConfig;
    private final Supplier<HttpRequestProcessor> proveedorProcesador;
    private final Consumer<String> consumidorTraza;

    /**
     * @param proveedorConfig  supplier de la configuración vigente (puede leer
     *                         un campo vivo; se invoca en cada traza)
     * @param proveedorProcesador supplier del procesador HTTP usado para
     *                         inspeccionar solicitudes
     * @param consumidorTraza  destino del mensaje ya validado (verbose); el
     *                         gating de modo detallado lo hace esta clase
     */
    public TrazadorContextual(Supplier<ConfiguracionAPI> proveedorConfig,
            Supplier<HttpRequestProcessor> proveedorProcesador,
            Consumer<String> consumidorTraza) {
        this.proveedorConfig = proveedorConfig;
        this.proveedorProcesador = proveedorProcesador;
        this.consumidorTraza = consumidorTraza;
    }

    public void rastrearContextual(String mensaje) {
        if (!esContextoDetalladoActivo() || Normalizador.esVacio(mensaje) || consumidorTraza == null) {
            return;
        }
        consumidorTraza.accept(mensaje);
    }

    public void registrarInicioContextualDetallado(String accion,
            FabricaMenuContextual.ContextoInvocacion contextoInvocacion) {
        // El null-check del contexto es obligatorio: hay llamadas legítimas con
        // contexto null (sobrecargas de 1 arg usadas por tests) y sin el guard
        // la evaluación de los argumentos de ACCION_INICIADA lanzaba NPE en
        // modo detallado, abortando la acción del menú vía el catch genérico.
        if (!esContextoDetalladoActivo() || contextoInvocacion == null) {
            return;
        }
        rastrearContextual(I18nLogs.ContextoMenu.ACCION_INICIADA(
            accion,
            contextoInvocacion.obtenerTipoInvocacion(),
            contextoInvocacion.obtenerTipoHerramienta(),
            contextoInvocacion.obtenerCantidadSeleccionada()
        ));
    }

    public void registrarResumenSeleccionContextualDetallado(List<HttpRequestResponse> solicitudes) {
        HttpRequestProcessor procesador = obtenerProcesadorSiDetallado();
        if (procesador == null) {
            return;
        }
        int total = solicitudes != null ? solicitudes.size() : 0;
        int sinRequest = procesador.contarSolicitudesSinRequest(solicitudes);
        int validas = Math.max(0, total - sinRequest);
        int sinResponse = procesador.contarSolicitudesSinResponse(solicitudes);
        rastrearContextual(I18nLogs.ContextoMenu.RESUMEN_SELECCION(total, validas, sinRequest, sinResponse));
    }

    public void registrarSolicitudesContextualesDetalladas(List<HttpRequestResponse> solicitudes) {
        if (!esContextoDetalladoActivo() || Normalizador.esVacia(solicitudes)) {
            return;
        }
        for (HttpRequestResponse solicitud : solicitudes) {
            registrarSolicitudContextualDetallada(solicitud);
        }
    }

    public void registrarSolicitudContextualDetallada(HttpRequestResponse solicitud) {
        if (solicitud == null) {
            return;
        }
        HttpRequestProcessor procesador = obtenerProcesadorSiDetallado();
        if (procesador == null) {
            return;
        }
        HttpRequestProcessor.ResumenSolicitudContextual resumen =
            procesador.inspeccionarSolicitudContextual(solicitud);
        if (resumen == null || !resumen.esValida()) {
            return;
        }
        for (String traza : procesador.construirTrazasDetalleContextual(resumen)) {
            rastrearContextual(traza);
        }
    }

    public void registrarBypassContextualDetallado(String mensaje) {
        rastrearContextual(mensaje);
    }

    private boolean esContextoDetalladoActivo() {
        ConfiguracionAPI config = proveedorConfig != null ? proveedorConfig.get() : null;
        return config != null && config.esDetallado();
    }

    private HttpRequestProcessor obtenerProcesadorSiDetallado() {
        if (!esContextoDetalladoActivo()) {
            return null;
        }
        return proveedorProcesador != null ? proveedorProcesador.get() : null;
    }
}
