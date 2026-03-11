package com.burpia.config;

import com.burpia.i18n.IdiomaUI;
import com.burpia.ui.EstadoProveedorUI;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.Normalizador;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Gestiona el estado del diálogo de configuración para detectar cambios no guardados.
 * Extrae la lógica de gestión de estado de DialogoConfiguracion siguiendo principios DRY.
 *
 * @author BurpIA Team
 * @since 1.0.3
 */
public final class DialogStateManager {

    private final GestorLoggingUnificado gestorLogging;
    private EstadoEdicionDialogo estadoInicial;
    private String proveedorActualUi;
    private final Map<String, String> rutasBinarioAgenteTemporal;
    private final Map<String, EstadoProveedorUI> estadoProveedorTemporal;

    public DialogStateManager(GestorLoggingUnificado gestorLogging) {
        this.gestorLogging = gestorLogging;
        this.rutasBinarioAgenteTemporal = new HashMap<>();
        this.estadoProveedorTemporal = new HashMap<>();
    }

    /**
     * Captura el estado inicial del diálogo cuando se abre.
     */
    public void capturarEstadoInicial(EstadoUIProvider uiProvider) {
        this.estadoInicial = capturarEstadoActual(uiProvider);
        this.proveedorActualUi = uiProvider.obtenerProveedorSeleccionado();
        
        if (estadoInicial != null) {
            gestorLogging.info("DialogStateManager", "Estado inicial capturado para proveedor: " + proveedorActualUi);
        }
    }

    /**
     * Captura el estado actual de la UI.
     */
    public EstadoEdicionDialogo capturarEstadoActual(EstadoUIProvider uiProvider) {
        if (uiProvider == null) {
            gestorLogging.error("DialogStateManager", "UIProvider es nulo");
            return null;
        }

        String proveedorSeleccionado = uiProvider.obtenerProveedorSeleccionado();
        String agenteSeleccionado = uiProvider.obtenerAgenteSeleccionado();

        Map<String, String> rutasAgente = new HashMap<>(rutasBinarioAgenteTemporal);
        Map<String, String> rutasTemporalesExternas = uiProvider.obtenerRutasBinarioAgenteTemporales();
        if (rutasTemporalesExternas != null) {
            rutasAgente.putAll(rutasTemporalesExternas);
        }
        if (Normalizador.noEsVacio(agenteSeleccionado)) {
            String rutaBinario = uiProvider.obtenerRutaBinarioAgente();
            if (rutaBinario != null) {
                rutasAgente.put(agenteSeleccionado, rutaBinario);
            }
        }

        Map<String, EstadoProveedorUI> borradores = new HashMap<>(estadoProveedorTemporal);
        Map<String, EstadoProveedorUI> estadosTemporalesExternos = uiProvider.obtenerEstadosProveedorTemporales();
        if (estadosTemporalesExternos != null) {
            borradores.putAll(estadosTemporalesExternos);
        }
        if (Normalizador.noEsVacio(proveedorSeleccionado)) {
            EstadoProveedorUI estadoActual = uiProvider.obtenerEstadoProveedorActual();
            if (estadoActual != null) {
                borradores.put(proveedorSeleccionado, estadoActual);
            }
        }

        Map<String, EstadoProveedorSnapshot> estadosProveedor = new HashMap<>();
        for (String proveedor : ProveedorAI.obtenerNombresProveedores()) {
            EstadoProveedorUI borrador = borradores.get(proveedor);
            estadosProveedor.put(proveedor, crearEstadoProveedorSnapshot(proveedor, borrador, uiProvider));
        }

        List<String> proveedoresSeleccionados = uiProvider.obtenerProveedoresMultiSeleccionados();

        return new EstadoEdicionDialogo(
                uiProvider.obtenerCodigoIdiomaSeleccionado(),
                proveedorSeleccionado,
                uiProvider.obtenerModeloSeleccionado(),
                uiProvider.obtenerUrlActual(),
                uiProvider.obtenerApiKeyActual(),
                uiProvider.obtenerMaxTokensTexto(),
                uiProvider.obtenerTimeoutTexto(),
                uiProvider.obtenerRetrasoTexto(),
                uiProvider.obtenerMaximoConcurrenteTexto(),
                uiProvider.obtenerMaximoHallazgosTexto(),
                uiProvider.obtenerMaximoTareasTexto(),
                uiProvider.esDetalladoSeleccionado(),
                uiProvider.esIgnorarSslSeleccionado(),
                uiProvider.esSoloProxySeleccionado(),
                uiProvider.esAlertasHabilitadasSeleccionado(),
                uiProvider.esPersistirBusquedaSeleccionado(),
                uiProvider.esPersistirSeveridadSeleccionado(),
                uiProvider.obtenerPromptActual(),
                uiProvider.esAgenteHabilitadoSeleccionado(),
                agenteSeleccionado,
                uiProvider.obtenerAgentePromptInicial(),
                uiProvider.obtenerAgentePrompt(),
                uiProvider.obtenerFuenteEstandar(),
                uiProvider.obtenerTamanioFuenteEstandar(),
                uiProvider.obtenerFuenteMono(),
                uiProvider.obtenerTamanioFuenteMono(),
                uiProvider.esMultiProveedorHabilitado(),
                rutasAgente,
                proveedoresSeleccionados,
                estadosProveedor);
    }

    /**
     * Compara el estado actual con el estado inicial para detectar cambios.
     */
    public boolean hayCambiosNoGuardados(EstadoUIProvider uiProvider) {
        if (estadoInicial == null) {
            return false;
        }

        EstadoEdicionDialogo estadoActual = capturarEstadoActual(uiProvider);
        if (estadoActual == null) {
            return false;
        }
        return !estadoInicial.equals(estadoActual);
    }

    /**
     * Obtiene los campos que han cambiado entre el estado inicial y el actual.
     */
    public List<String> obtenerCambiosDetectados(EstadoUIProvider uiProvider) {
        List<String> cambios = new ArrayList<>();
        
        if (estadoInicial == null) {
            return cambios;
        }

        EstadoEdicionDialogo estadoActual = capturarEstadoActual(uiProvider);
        if (estadoActual == null) {
            return cambios;
        }
        
        if (!Objects.equals(estadoInicial.proveedorSeleccionado(), estadoActual.proveedorSeleccionado())) {
            cambios.add("Proveedor AI");
        }
        
        if (!Objects.equals(estadoInicial.modeloSeleccionado(), estadoActual.modeloSeleccionado())) {
            cambios.add("Modelo");
        }
        
        if (!Objects.equals(estadoInicial.apiKeyActual(), estadoActual.apiKeyActual())) {
            cambios.add("API Key");
        }
        
        if (!Objects.equals(estadoInicial.urlActual(), estadoActual.urlActual())) {
            cambios.add("URL API");
        }
        
        if (!Objects.equals(estadoInicial.prompt(), estadoActual.prompt())) {
            cambios.add("Prompt");
        }
        
        if (estadoInicial.detallado() != estadoActual.detallado()) {
            cambios.add("Modo detallado");
        }
        
        if (estadoInicial.multiProveedorHabilitado() != estadoActual.multiProveedorHabilitado()) {
            cambios.add("Multi-proveedor");
        }
        
        return cambios;
    }

    /**
     * Gestiona el cambio de proveedor en la UI.
     */
    public void gestionarCambioProveedor(String nuevoProveedor, EstadoUIProvider uiProvider) {
        if (Normalizador.esVacio(nuevoProveedor)) {
            gestorLogging.error("DialogStateManager", "Proveedor nuevo es vacío");
            return;
        }

        if (Objects.equals(proveedorActualUi, nuevoProveedor)) {
            return;
        }

        String proveedorAnterior = proveedorActualUi;
        proveedorActualUi = nuevoProveedor;

        gestorLogging.info("DialogStateManager", 
            String.format("Cambiando proveedor: %s -> %s", 
                proveedorAnterior != null ? proveedorAnterior : "null", 
                nuevoProveedor));

        uiProvider.actualizarProveedorEnUI(nuevoProveedor);
    }

    /**
     * Guarda el estado temporal del proveedor durante la edición.
     */
    public void guardarEstadoTemporalProveedor(String proveedor, EstadoProveedorUI estado) {
        if (Normalizador.esVacio(proveedor)) {
            return;
        }
        
        if (estado != null) {
            estadoProveedorTemporal.put(proveedor, estado);
            gestorLogging.info("DialogStateManager", "Estado temporal guardado para proveedor: " + proveedor);
        } else {
            estadoProveedorTemporal.remove(proveedor);
            gestorLogging.info("DialogStateManager", "Estado temporal eliminado para proveedor: " + proveedor);
        }
    }

    /**
     * Guarda la ruta binaria del agente de forma temporal.
     */
    public void guardarRutaBinarioAgente(String agente, String ruta) {
        if (Normalizador.esVacio(agente)) {
            return;
        }
        
        if (Normalizador.noEsVacio(ruta)) {
            rutasBinarioAgenteTemporal.put(agente, ruta);
            gestorLogging.info("DialogStateManager", 
                String.format("Ruta binaria guardada para agente %s: %s", agente, ruta));
        } else {
            rutasBinarioAgenteTemporal.remove(agente);
            gestorLogging.info("DialogStateManager", "Ruta binaria eliminada para agente: " + agente);
        }
    }

    /**
     * Limpia todo el estado temporal.
     */
    public void limpiarEstadoTemporal() {
        rutasBinarioAgenteTemporal.clear();
        estadoProveedorTemporal.clear();
        gestorLogging.info("DialogStateManager", "Estado temporal limpiado");
    }

    /**
     * Obtiene el proveedor actual seleccionado en la UI.
     */
    public String obtenerProveedorActual() {
        return proveedorActualUi;
    }

    /**
     * Obtiene las rutas binarias de agentes temporales.
     */
    public Map<String, String> obtenerRutasBinarioAgenteTemporal() {
        return Collections.unmodifiableMap(rutasBinarioAgenteTemporal);
    }

    /**
     * Obtiene los estados temporales de proveedores.
     */
    public Map<String, EstadoProveedorUI> obtenerEstadoProveedorTemporal() {
        return Collections.unmodifiableMap(estadoProveedorTemporal);
    }

    /**
     * Crea un snapshot del estado de un proveedor.
     */
    private EstadoProveedorSnapshot crearEstadoProveedorSnapshot(String proveedor, 
                                                                EstadoProveedorUI borrador, 
                                                                EstadoUIProvider uiProvider) {
        if (borrador != null) {
            return new EstadoProveedorSnapshot(
                    textoSeguro(borrador.getApiKey()),
                    textoSeguro(borrador.getModelo()),
                    textoSeguro(borrador.getBaseUrl()),
                    borrador.getMaxTokens(),
                    borrador.getTimeout());
        }

        ConfiguracionAPI config = uiProvider.obtenerConfiguracion();
        String apiKey = config.obtenerApiKeyParaProveedor(proveedor);
        String modelo = config.obtenerModeloParaProveedor(proveedor);
        ProveedorAI.ConfiguracionProveedor configProveedor = ProveedorAI.obtenerProveedor(proveedor);
        if (Normalizador.esVacio(modelo) && configProveedor != null) {
            modelo = configProveedor.obtenerModeloPorDefecto();
        }

        Integer maxTokensConfigurado = config.obtenerMaxTokensConfiguradoParaProveedor(proveedor);
        int timeoutConfigurado = config.obtenerTiempoEsperaParaModelo(proveedor, modelo);

        int maxTokens = maxTokensConfigurado != null ? maxTokensConfigurado : 
                        configProveedor != null ? configProveedor.obtenerMaxTokensPorDefecto() : 0;
        int timeout = timeoutConfigurado > 0 ? timeoutConfigurado : 120;

        String baseUrl = config.obtenerUrlBaseParaProveedor(proveedor);
        if (Normalizador.esVacio(baseUrl) && configProveedor != null) {
            baseUrl = configProveedor.obtenerUrlApi();
        }

        return new EstadoProveedorSnapshot(
                textoSeguro(apiKey),
                textoSeguro(modelo),
                textoSeguro(baseUrl),
                maxTokens,
                timeout);
    }

    /**
     * Normaliza el texto para comparación segura.
     */
    private static String textoSeguro(String texto) {
        return Normalizador.normalizarTexto(texto);
    }

    /**
     * Interfaz para proveer estado de la UI al DialogStateManager.
     */
    public interface EstadoUIProvider {
        String obtenerProveedorSeleccionado();
        String obtenerAgenteSeleccionado();
        String obtenerRutaBinarioAgente();
        EstadoProveedorUI obtenerEstadoProveedorActual();
        default Map<String, String> obtenerRutasBinarioAgenteTemporales() {
            return Collections.emptyMap();
        }
        default Map<String, EstadoProveedorUI> obtenerEstadosProveedorTemporales() {
            return Collections.emptyMap();
        }
        List<String> obtenerProveedoresMultiSeleccionados();
        String obtenerCodigoIdiomaSeleccionado();
        String obtenerModeloSeleccionado();
        String obtenerUrlActual();
        String obtenerApiKeyActual();
        String obtenerMaxTokensTexto();
        String obtenerTimeoutTexto();
        String obtenerRetrasoTexto();
        String obtenerMaximoConcurrenteTexto();
        String obtenerMaximoHallazgosTexto();
        String obtenerMaximoTareasTexto();
        boolean esDetalladoSeleccionado();
        boolean esIgnorarSslSeleccionado();
        boolean esSoloProxySeleccionado();
        boolean esAlertasHabilitadasSeleccionado();
        boolean esPersistirBusquedaSeleccionado();
        boolean esPersistirSeveridadSeleccionado();
        String obtenerPromptActual();
        boolean esAgenteHabilitadoSeleccionado();
        String obtenerAgentePromptInicial();
        String obtenerAgentePrompt();
        String obtenerFuenteEstandar();
        int obtenerTamanioFuenteEstandar();
        String obtenerFuenteMono();
        int obtenerTamanioFuenteMono();
        boolean esMultiProveedorHabilitado();
        ConfiguracionAPI obtenerConfiguracion();
        void actualizarProveedorEnUI(String nuevoProveedor);
    }

    /**
     * Snapshot del estado de edición del diálogo.
     */
    public static final class EstadoEdicionDialogo {
        private final String idiomaUi;
        private final String proveedorSeleccionado;
        private final String modeloSeleccionado;
        private final String urlActual;
        private final String apiKeyActual;
        private final String maxTokensTexto;
        private final String timeoutTexto;
        private final String retrasoTexto;
        private final String maximoConcurrenteTexto;
        private final String maximoHallazgosTexto;
        private final String maximoTareasTexto;
        private final boolean detallado;
        private final boolean ignorarSsl;
        private final boolean soloProxy;
        private final boolean alertasHabilitadas;
        private final boolean persistirBusqueda;
        private final boolean persistirSeveridad;
        private final String prompt;
        private final boolean agenteHabilitado;
        private final String tipoAgente;
        private final String agentePromptInicial;
        private final String agentePrompt;
        private final String fuenteEstandar;
        private final int tamanioFuenteEstandar;
        private final String fuenteMono;
        private final int tamanioFuenteMono;
        private final boolean multiProveedorHabilitado;
        private final Map<String, String> rutasBinarioAgente;
        private final List<String> proveedoresMultiConsulta;
        private final Map<String, EstadoProveedorSnapshot> estadosProveedor;

        public EstadoEdicionDialogo(
                String idiomaUi,
                String proveedorSeleccionado,
                String modeloSeleccionado,
                String urlActual,
                String apiKeyActual,
                String maxTokensTexto,
                String timeoutTexto,
                String retrasoTexto,
                String maximoConcurrenteTexto,
                String maximoHallazgosTexto,
                String maximoTareasTexto,
                boolean detallado,
                boolean ignorarSsl,
                boolean soloProxy,
                boolean alertasHabilitadas,
                boolean persistirBusqueda,
                boolean persistirSeveridad,
                String prompt,
                boolean agenteHabilitado,
                String tipoAgente,
                String agentePromptInicial,
                String agentePrompt,
                String fuenteEstandar,
                int tamanioFuenteEstandar,
                String fuenteMono,
                int tamanioFuenteMono,
                boolean multiProveedorHabilitado,
                Map<String, String> rutasBinarioAgente,
                List<String> proveedoresMultiConsulta,
                Map<String, EstadoProveedorSnapshot> estadosProveedor) {
            this.idiomaUi = idiomaUi;
            this.proveedorSeleccionado = proveedorSeleccionado;
            this.modeloSeleccionado = modeloSeleccionado;
            this.urlActual = urlActual;
            this.apiKeyActual = apiKeyActual;
            this.maxTokensTexto = maxTokensTexto;
            this.timeoutTexto = timeoutTexto;
            this.retrasoTexto = retrasoTexto;
            this.maximoConcurrenteTexto = maximoConcurrenteTexto;
            this.maximoHallazgosTexto = maximoHallazgosTexto;
            this.maximoTareasTexto = maximoTareasTexto;
            this.detallado = detallado;
            this.ignorarSsl = ignorarSsl;
            this.soloProxy = soloProxy;
            this.alertasHabilitadas = alertasHabilitadas;
            this.persistirBusqueda = persistirBusqueda;
            this.persistirSeveridad = persistirSeveridad;
            this.prompt = prompt;
            this.agenteHabilitado = agenteHabilitado;
            this.tipoAgente = tipoAgente;
            this.agentePromptInicial = agentePromptInicial;
            this.agentePrompt = agentePrompt;
            this.fuenteEstandar = fuenteEstandar;
            this.tamanioFuenteEstandar = tamanioFuenteEstandar;
            this.fuenteMono = fuenteMono;
            this.tamanioFuenteMono = tamanioFuenteMono;
            this.multiProveedorHabilitado = multiProveedorHabilitado;
            this.rutasBinarioAgente = rutasBinarioAgente != null
                    ? Collections.unmodifiableMap(new HashMap<>(rutasBinarioAgente))
                    : Collections.emptyMap();
            this.proveedoresMultiConsulta = proveedoresMultiConsulta != null
                    ? Collections.unmodifiableList(new ArrayList<>(proveedoresMultiConsulta))
                    : Collections.emptyList();
            this.estadosProveedor = estadosProveedor != null
                    ? Collections.unmodifiableMap(new HashMap<>(estadosProveedor))
                    : Collections.emptyMap();
        }

        // Getters
        public String idiomaUi() { return idiomaUi; }
        public String proveedorSeleccionado() { return proveedorSeleccionado; }
        public String modeloSeleccionado() { return modeloSeleccionado; }
        public String urlActual() { return urlActual; }
        public String apiKeyActual() { return apiKeyActual; }
        public String maxTokensTexto() { return maxTokensTexto; }
        public String timeoutTexto() { return timeoutTexto; }
        public String retrasoTexto() { return retrasoTexto; }
        public String maximoConcurrenteTexto() { return maximoConcurrenteTexto; }
        public String maximoHallazgosTexto() { return maximoHallazgosTexto; }
        public String maximoTareasTexto() { return maximoTareasTexto; }
        public boolean detallado() { return detallado; }
        public boolean ignorarSsl() { return ignorarSsl; }
        public boolean soloProxy() { return soloProxy; }
        public boolean alertasHabilitadas() { return alertasHabilitadas; }
        public boolean persistirBusqueda() { return persistirBusqueda; }
        public boolean persistirSeveridad() { return persistirSeveridad; }
        public String prompt() { return prompt; }
        public boolean agenteHabilitado() { return agenteHabilitado; }
        public String tipoAgente() { return tipoAgente; }
        public String agentePromptInicial() { return agentePromptInicial; }
        public String agentePrompt() { return agentePrompt; }
        public String fuenteEstandar() { return fuenteEstandar; }
        public int tamanioFuenteEstandar() { return tamanioFuenteEstandar; }
        public String fuenteMono() { return fuenteMono; }
        public int tamanioFuenteMono() { return tamanioFuenteMono; }
        public boolean multiProveedorHabilitado() { return multiProveedorHabilitado; }
        public Map<String, String> rutasBinarioAgente() { return rutasBinarioAgente; }
        public List<String> proveedoresMultiConsulta() { return proveedoresMultiConsulta; }
        public Map<String, EstadoProveedorSnapshot> estadosProveedor() { return estadosProveedor; }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof EstadoEdicionDialogo)) {
                return false;
            }
            EstadoEdicionDialogo that = (EstadoEdicionDialogo) o;
            return detallado == that.detallado
                    && ignorarSsl == that.ignorarSsl
                    && soloProxy == that.soloProxy
                    && alertasHabilitadas == that.alertasHabilitadas
                    && persistirBusqueda == that.persistirBusqueda
                    && persistirSeveridad == that.persistirSeveridad
                    && agenteHabilitado == that.agenteHabilitado
                    && tamanioFuenteEstandar == that.tamanioFuenteEstandar
                    && tamanioFuenteMono == that.tamanioFuenteMono
                    && multiProveedorHabilitado == that.multiProveedorHabilitado
                    && Objects.equals(idiomaUi, that.idiomaUi)
                    && Objects.equals(proveedorSeleccionado, that.proveedorSeleccionado)
                    && Objects.equals(modeloSeleccionado, that.modeloSeleccionado)
                    && Objects.equals(urlActual, that.urlActual)
                    && Objects.equals(apiKeyActual, that.apiKeyActual)
                    && Objects.equals(maxTokensTexto, that.maxTokensTexto)
                    && Objects.equals(timeoutTexto, that.timeoutTexto)
                    && Objects.equals(retrasoTexto, that.retrasoTexto)
                    && Objects.equals(maximoConcurrenteTexto, that.maximoConcurrenteTexto)
                    && Objects.equals(maximoHallazgosTexto, that.maximoHallazgosTexto)
                    && Objects.equals(maximoTareasTexto, that.maximoTareasTexto)
                    && Objects.equals(prompt, that.prompt)
                    && Objects.equals(tipoAgente, that.tipoAgente)
                    && Objects.equals(agentePromptInicial, that.agentePromptInicial)
                    && Objects.equals(agentePrompt, that.agentePrompt)
                    && Objects.equals(fuenteEstandar, that.fuenteEstandar)
                    && Objects.equals(fuenteMono, that.fuenteMono)
                    && Objects.equals(rutasBinarioAgente, that.rutasBinarioAgente)
                    && Objects.equals(proveedoresMultiConsulta, that.proveedoresMultiConsulta)
                    && Objects.equals(estadosProveedor, that.estadosProveedor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    idiomaUi,
                    proveedorSeleccionado,
                    modeloSeleccionado,
                    urlActual,
                    apiKeyActual,
                    maxTokensTexto,
                    timeoutTexto,
                    retrasoTexto,
                    maximoConcurrenteTexto,
                    maximoHallazgosTexto,
                    maximoTareasTexto,
                    detallado,
                    ignorarSsl,
                    soloProxy,
                    alertasHabilitadas,
                    persistirBusqueda,
                    persistirSeveridad,
                    prompt,
                    agenteHabilitado,
                    tipoAgente,
                    agentePromptInicial,
                    agentePrompt,
                    fuenteEstandar,
                    tamanioFuenteEstandar,
                    fuenteMono,
                    tamanioFuenteMono,
                    multiProveedorHabilitado,
                    rutasBinarioAgente,
                    proveedoresMultiConsulta,
                    estadosProveedor);
        }
    }

    /**
     * Snapshot del estado de un proveedor.
     */
    public static final class EstadoProveedorSnapshot {
        private final String apiKey;
        private final String modelo;
        private final String baseUrl;
        private final int maxTokens;
        private final int timeout;

        public EstadoProveedorSnapshot(String apiKey, String modelo, String baseUrl, int maxTokens, int timeout) {
            this.apiKey = apiKey;
            this.modelo = modelo;
            this.baseUrl = baseUrl;
            this.maxTokens = maxTokens;
            this.timeout = timeout;
        }

        public String apiKey() { return apiKey; }
        public String modelo() { return modelo; }
        public String baseUrl() { return baseUrl; }
        public int maxTokens() { return maxTokens; }
        public int timeout() { return timeout; }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof EstadoProveedorSnapshot)) {
                return false;
            }
            EstadoProveedorSnapshot that = (EstadoProveedorSnapshot) o;
            return maxTokens == that.maxTokens
                    && timeout == that.timeout
                    && Objects.equals(apiKey, that.apiKey)
                    && Objects.equals(modelo, that.modelo)
                    && Objects.equals(baseUrl, that.baseUrl);
        }

        @Override
        public int hashCode() {
            return Objects.hash(apiKey, modelo, baseUrl, maxTokens, timeout);
        }
    }
}
