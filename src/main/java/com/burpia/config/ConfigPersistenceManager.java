package com.burpia.config;

import com.burpia.i18n.I18nLogs;
import com.burpia.i18n.I18nUI;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.Normalizador;

import java.util.HashMap;
import java.util.Map;

public class ConfigPersistenceManager {
    private final GestorConfiguracion gestorConfig;
    private final GestorLoggingUnificado gestorLogging;
    private ConfiguracionAPI configuracionCargada;
    private Map<String, Object> estadoInicial;

    /**
     * Crea un nuevo gestor de persistencia de configuración.
     *
     * @param gestorConfig El gestor de configuración subyacente, no puede ser nulo
     * @param gestorLogging El gestor de logging unificado, opcional (se creará uno por defecto si es nulo)
     * @throws IllegalArgumentException si gestorConfig es nulo
     */
    public ConfigPersistenceManager(GestorConfiguracion gestorConfig, GestorLoggingUnificado gestorLogging) {
        if (gestorConfig == null) {
            throw new IllegalArgumentException(I18nUI.General.ERROR_GESTOR_CONFIG_NULO());
        }
        this.gestorConfig = gestorConfig;
        // Si gestorLogging es null, crear uno mínimo sin stdout/stderr (usa Logger interno)
        this.gestorLogging = gestorLogging != null ? gestorLogging : GestorLoggingUnificado.crearMinimal(null, null);
    }

    /**
     * Carga la configuración desde el gestor de configuración subyacente.
     * 
     * @return La configuración cargada, o una configuración por defecto si hay errores
     */
    public ConfiguracionAPI cargarConfiguracion() {
        gestorLogging.info("ConfigPersistenceManager", I18nLogs.Configuracion.CARGANDO_DESDE_GESTOR());
        
        this.configuracionCargada = gestorConfig.cargarConfiguracion();
        
        if (configuracionCargada == null) {
            gestorLogging.error("ConfigPersistenceManager", I18nLogs.Configuracion.CONFIGURACION_NULA());
            this.configuracionCargada = new ConfiguracionAPI();
        }
        
        this.estadoInicial = crearEstadoSnapshot(configuracionCargada);
        gestorLogging.info("ConfigPersistenceManager", I18nLogs.Configuracion.CARGADA_OK());
        
        return configuracionCargada;
    }

    /**
     * Guarda la configuración especificada.
     * 
     * @param configuracion La configuración a guardar, no puede ser nula
     * @return true si se guardó exitosamente, false en caso contrario
     */
    public boolean guardarConfiguracion(ConfiguracionAPI configuracion) {
        return guardarConfiguracion(configuracion, null);
    }

    /**
     * Guarda la configuración especificada con mensaje de error detallado.
     * 
     * @param configuracion La configuración a guardar, no puede ser nula
     * @param mensajeError StringBuilder opcional para almacenar mensajes de error detallados
     * @return true si se guardó exitosamente, false en caso contrario
     */
    public boolean guardarConfiguracion(ConfiguracionAPI configuracion, StringBuilder mensajeError) {
        if (configuracion == null) {
            gestorLogging.error("ConfigPersistenceManager", I18nLogs.Configuracion.NO_GUARDAR_NULA());
            if (mensajeError != null) {
                mensajeError.append(I18nUI.Configuracion.MSG_CONFIGURACION_NULA());
            }
            return false;
        }

        gestorLogging.info("ConfigPersistenceManager", I18nLogs.Configuracion.VALIDANDO_ANTES_GUARDAR());
        
        Map<String, String> errores = configuracion.validar();
        if (!errores.isEmpty()) {
            gestorLogging.error("ConfigPersistenceManager", I18nLogs.Configuracion.ERRORES_VALIDACION(errores.size()));
            if (mensajeError != null) {
                mensajeError.append(construirMensajeErroresValidacion(errores));
            }
            return false;
        }

        gestorLogging.info("ConfigPersistenceManager", I18nLogs.Configuracion.GUARDANDO_EN_GESTOR());
        
        boolean guardado = gestorConfig.guardarConfiguracion(configuracion, mensajeError);
        
        if (guardado) {
            this.configuracionCargada = configuracion;
            this.estadoInicial = crearEstadoSnapshot(configuracion);
            gestorLogging.info("ConfigPersistenceManager", I18nLogs.Configuracion.GUARDADA_OK());
        } else {
            gestorLogging.error("ConfigPersistenceManager", I18nLogs.Configuracion.ERROR_GUARDAR());
        }
        
        return guardado;
    }

    /**
     * Valida la configuración especificada.
     * 
     * @param configuracion La configuración a validar
     * @return true si la configuración es válida, false en caso contrario
     */
    public boolean validarConfiguracion(ConfiguracionAPI configuracion) {
        if (configuracion == null) {
            gestorLogging.error("ConfigPersistenceManager", I18nLogs.Configuracion.NULA_NO_VALIDA());
            return false;
        }

        Map<String, String> errores = configuracion.validar();
        if (!errores.isEmpty()) {
            gestorLogging.info("ConfigPersistenceManager", I18nLogs.Configuracion.VALIDACION_FALLIDA(errores.size()));
            return false;
        }

        gestorLogging.info("ConfigPersistenceManager", I18nLogs.Configuracion.VALIDADA_OK());
        return true;
    }

    /**
     * Obtiene los errores de validación para una configuración.
     * 
     * @param configuracion La configuración a validar
     * @return Mapa con los errores de validación por campo
     */
    public Map<String, String> obtenerErroresValidacion(ConfiguracionAPI configuracion) {
        if (configuracion == null) {
            Map<String, String> errorNulo = new HashMap<>();
            errorNulo.put("configuracion", I18nUI.Configuracion.MSG_CONFIGURACION_NULA());
            return errorNulo;
        }

        return configuracion.validar();
    }

    /**
     * Verifica si existe una configuración cargada en memoria.
     * 
     * @return true si hay una configuración cargada, false en caso contrario
     */
    public boolean existeConfiguracionCargada() {
        return configuracionCargada != null;
    }

    /**
     * Obtiene la configuración actualmente cargada en memoria.
     * 
     * @return La configuración cargada, o null si no hay ninguna
     */
    public ConfiguracionAPI obtenerConfiguracionCargada() {
        return configuracionCargada;
    }

    /**
     * Verifica si hay cambios pendientes comparando con el estado inicial.
     * 
     * @return true si hay cambios pendientes, false en caso contrario
     */
    public boolean hayCambiosPendientes() {
        if (configuracionCargada == null || estadoInicial == null) {
            return false;
        }

        Map<String, Object> estadoActual = crearEstadoSnapshot(configuracionCargada);
        return !estadosIguales(estadoInicial, estadoActual);
    }

    /**
     * Actualiza el estado de referencia para detección de cambios.
     * Útil después de guardar exitosamente o para resetear el estado inicial.
     */
    public void actualizarEstadoReferencia() {
        if (configuracionCargada != null) {
            this.estadoInicial = crearEstadoSnapshot(configuracionCargada);
            gestorLogging.info("ConfigPersistenceManager", I18nLogs.Configuracion.ESTADO_REFERENCIA_ACTUALIZADO());
        }
    }

    /**
     * Obtiene la ruta del archivo de configuración.
     * 
     * @return La ruta absoluta del archivo de configuración
     */
    public String obtenerRutaArchivoConfiguracion() {
        return gestorConfig.obtenerRutaConfiguracion();
    }

    private Map<String, Object> crearEstadoSnapshot(ConfiguracionAPI config) {
        Map<String, Object> snapshot = new HashMap<>();
        
        snapshot.put("proveedorAI", config.obtenerProveedorAI());
        snapshot.put("retrasoSegundos", config.obtenerRetrasoSegundos());
        snapshot.put("maximoConcurrente", config.obtenerMaximoConcurrente());
        snapshot.put("maximoHallazgosTabla", config.obtenerMaximoHallazgosTabla());
        snapshot.put("maximoTareasTabla", config.obtenerMaximoTareasTabla());
        snapshot.put("detallado", config.esDetallado());
        snapshot.put("tiempoEsperaAI", config.obtenerTiempoEsperaAI());
        snapshot.put("idiomaUi", config.obtenerIdiomaUi());
        snapshot.put("escaneoPasivoHabilitado", config.escaneoPasivoHabilitado());
        snapshot.put("autoGuardadoIssuesHabilitado", config.autoGuardadoIssuesHabilitado());
        snapshot.put("autoScrollConsolaHabilitado", config.autoScrollConsolaHabilitado());
        snapshot.put("alertasHabilitadas", config.alertasHabilitadas());
        snapshot.put("alertasClickDerechoEnviarAHabilitadas", config.alertasClickDerechoEnviarAHabilitadas());
        snapshot.put("promptConfigurable", config.obtenerPromptConfigurable());
        snapshot.put("promptModificado", config.esPromptModificado());
        snapshot.put("ignorarErroresSSL", config.ignorarErroresSSL());
        snapshot.put("soloProxy", config.soloProxy());
        snapshot.put("agenteHabilitado", config.agenteHabilitado());
        snapshot.put("tipoAgente", config.obtenerTipoAgente());
        snapshot.put("agentePreflightPrompt", config.obtenerAgentePreflightPrompt());
        snapshot.put("agentePrompt", config.obtenerAgentePrompt());
        snapshot.put("agenteDelay", config.obtenerAgenteDelay());
        snapshot.put("nombreFuenteEstandar", config.obtenerNombreFuenteEstandar());
        snapshot.put("tamanioFuenteEstandar", config.obtenerTamanioFuenteEstandar());
        snapshot.put("nombreFuenteMono", config.obtenerNombreFuenteMono());
        snapshot.put("tamanioFuenteMono", config.obtenerTamanioFuenteMono());
        snapshot.put("textoFiltroHallazgos", config.obtenerTextoFiltroHallazgos());
        snapshot.put("filtroSeveridadHallazgos", config.obtenerFiltroSeveridadHallazgos());
        snapshot.put("persistirFiltroBusquedaHallazgos", config.persistirFiltroBusquedaHallazgos());
        snapshot.put("persistirFiltroSeveridadHallazgos", config.persistirFiltroSeveridadHallazgos());
        snapshot.put("multiProveedorHabilitado", config.esMultiProveedorHabilitado());
        snapshot.put("apiKeysPorProveedor", new HashMap<>(config.obtenerApiKeysPorProveedor()));
        snapshot.put("urlsBasePorProveedor", new HashMap<>(config.obtenerUrlsBasePorProveedor()));
        snapshot.put("modelosPorProveedor", new HashMap<>(config.obtenerModelosPorProveedor()));
        snapshot.put("maxTokensPorProveedor", new HashMap<>(config.obtenerMaxTokensPorProveedor()));
        snapshot.put("tiempoEsperaPorModelo", new HashMap<>(config.obtenerTiempoEsperaPorModelo()));
        snapshot.put("rutasBinarioPorAgente", new HashMap<>(config.obtenerTodasLasRutasBinario()));
        snapshot.put("estadoUI", new HashMap<>(config.obtenerEstadoUI()));
        
        return snapshot;
    }

    private boolean estadosIguales(Map<String, Object> estado1, Map<String, Object> estado2) {
        if (estado1.size() != estado2.size()) {
            return false;
        }

        for (Map.Entry<String, Object> entry : estado1.entrySet()) {
            String clave = entry.getKey();
            Object valor1 = entry.getValue();
            Object valor2 = estado2.get(clave);

            if (valor1 == null && valor2 == null) {
                continue;
            }
            
            if (valor1 == null || valor2 == null) {
                return false;
            }

            if (!valor1.equals(valor2)) {
                return false;
            }
        }

        return true;
    }

    private String construirMensajeErroresValidacion(Map<String, String> errores) {
        if (Normalizador.esVacia(errores)) {
            return "";
        }

        StringBuilder mensaje = new StringBuilder(I18nUI.Configuracion.MSG_CORRIGE_CAMPOS());
        errores.values().forEach(valor -> mensaje.append(" - ").append(valor).append("\n"));
        return mensaje.toString().trim();
    }
}
