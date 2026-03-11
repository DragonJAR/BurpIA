package com.burpia.ui;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nLogs;
import com.burpia.i18n.I18nUI;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.Normalizador;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.table.TableColumn;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestor centralizado para persistencia de estado UI en BurpIA.
 * Sigue principios DRY centralizando toda la lógica de persistencia UI.
 *
 * <p>Funcionalidades soportadas:</p>
 * <ul>
 *   <li>Persistencia de filtros de búsqueda en PanelHallazgos</li>
 *   <li>Persistencia de última pestaña seleccionada en PestaniaPrincipal</li>
 *   <li>Persistencia de anchos de columna en tablas</li>
 * </ul>
 *
 * @author BurpIA Team
 * @since 1.0.3
 */
public final class UIStateManager {

    private static final String ORIGEN_LOG = "UIStateManager";
    private static final String SEPARADOR_ESTADO = "_";
    
    // Claves para estado de filtros
    private static final String CLAVE_FILTRO_BUSQUEDA = "filtroBusqueda";
    private static final String CLAVE_FILTRO_SEVERIDAD = "filtroSeveridad";
    
    // Claves para estado de pestañas
    private static final String CLAVE_PESTANIA_ACTUAL = "pestaniaActual";
    
    // Claves para anchos de columna
    private static final String CLAVE_ANCHOS_COLUMNAS = "anchosColumnas";
    
    private final ConfiguracionAPI config;
    private final GestorLoggingUnificado gestorLogging;

    /**
     * Constructor del gestor de estado UI.
     *
     * @param config Configuración API para persistencia
     * @param gestorLogging Gestor de logging unificado
     */
    public UIStateManager(ConfiguracionAPI config, GestorLoggingUnificado gestorLogging) {
        if (config == null || gestorLogging == null) {
            throw new IllegalArgumentException(I18nUI.General.ERROR_CONFIG_Y_GESTOR_LOGGING_NULOS());
        }
        this.config = config;
        this.gestorLogging = gestorLogging;
    }

    /**
     * Guarda el estado de filtros del panel de hallazgos.
     *
     * @param textoBusqueda Texto de búsqueda actual
     * @param severidadSeleccionada Severidad seleccionada en el filtro
     */
    public void guardarEstadoFiltrosHallazgos(String textoBusqueda, String severidadSeleccionada) {
        if (!config.persistirFiltroBusquedaHallazgos() && 
            !config.persistirFiltroSeveridadHallazgos()) {
            return;
        }

        try {
            Map<String, String> estadoActual = obtenerEstadoUIActual();
            
            if (config.persistirFiltroBusquedaHallazgos()) {
                estadoActual.put(CLAVE_FILTRO_BUSQUEDA, textoBusqueda != null ? textoBusqueda : "");
            }
            
            if (config.persistirFiltroSeveridadHallazgos()) {
                estadoActual.put(
                    CLAVE_FILTRO_SEVERIDAD,
                    I18nUI.Hallazgos.NORMALIZAR_FILTRO_SEVERIDAD(severidadSeleccionada)
                );
            }
            
            guardarEstadoUI(estadoActual);
            
            if (config.esDetallado()) {
                gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Estado de filtros guardado: búsqueda='" + textoBusqueda +
                    "', severidad='" + severidadSeleccionada + "'"));
            }
        } catch (Exception e) {
            gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("Error al guardar estado de filtros"), e);
        }
    }

    /**
     * Restaura el estado de filtros del panel de hallazgos.
     *
     * @param panelHallazgos Panel donde se aplicarán los filtros restaurados
     */
    public void restaurarEstadoFiltrosHallazgos(PanelHallazgos panelHallazgos) {
        if (panelHallazgos == null) {
            return;
        }

        if (!config.persistirFiltroBusquedaHallazgos() && 
            !config.persistirFiltroSeveridadHallazgos()) {
            return;
        }

        try {
            Map<String, String> estadoGuardado = obtenerEstadoUIGuardado();
            
            String textoBusqueda = estadoGuardado.get(CLAVE_FILTRO_BUSQUEDA);
            String severidadSeleccionada = estadoGuardado.get(CLAVE_FILTRO_SEVERIDAD);
            
            boolean estadoRestaurado = false;
            
            if (config.persistirFiltroBusquedaHallazgos() && Normalizador.noEsVacio(textoBusqueda)) {
                panelHallazgos.establecerTextoFiltro(textoBusqueda);
                estadoRestaurado = true;
            }
            
            if (config.persistirFiltroSeveridadHallazgos() && Normalizador.noEsVacio(severidadSeleccionada)) {
                panelHallazgos.establecerFiltroSeveridad(severidadSeleccionada);
                estadoRestaurado = true;
            }
            
            if (estadoRestaurado && config.esDetallado()) {
                gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Estado de filtros restaurado: búsqueda='" + textoBusqueda +
                    "', severidad='" + severidadSeleccionada + "'"));
            }
        } catch (Exception e) {
            gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("Error al restaurar estado de filtros"), e);
        }
    }

    /**
     * Guarda la última pestaña seleccionada.
     *
     * @param tabbedPane Componente de pestañas
     * @param identificadorPestania Identificador único de la pestaña
     */
    public void guardarUltimaPestaniaSeleccionada(JTabbedPane tabbedPane, String identificadorPestania) {
        if (tabbedPane == null || Normalizador.esVacio(identificadorPestania)) {
            return;
        }

        try {
            Map<String, String> estadoActual = obtenerEstadoUIActual();
            estadoActual.put(CLAVE_PESTANIA_ACTUAL, identificadorPestania);
            guardarEstadoUI(estadoActual);
            
            if (config.esDetallado()) {
                gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Última pestaña guardada: " + identificadorPestania));
            }
        } catch (Exception e) {
            gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("Error al guardar última pestaña seleccionada"), e);
        }
    }

    /**
     * Restaura la última pestaña seleccionada.
     *
     * @param tabbedPane Componente de pestañas donde se restaurará
     * @param pestaniaPorDefecto Pestaña por defecto si no hay guardada
     */
    public void restaurarUltimaPestaniaSeleccionada(JTabbedPane tabbedPane, int pestaniaPorDefecto) {
        if (tabbedPane == null) {
            return;
        }

        try {
            Map<String, String> estadoGuardado = obtenerEstadoUIGuardado();
            String identificadorPestania = estadoGuardado.get(CLAVE_PESTANIA_ACTUAL);
            
            if (Normalizador.noEsVacio(identificadorPestania)) {
                // Buscar la pestaña por identificador
                for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                    String tituloPestania = tabbedPane.getTitleAt(i);
                    Component componente = tabbedPane.getComponentAt(i);
                    String nombreComponente = componente != null ? componente.getName() : null;
                    if (identificadorPestania.equals(nombreComponente)
                            || identificadorPestania.equals(tituloPestania)) {
                        tabbedPane.setSelectedIndex(i);
                        if (config.esDetallado()) {
                            gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Pestaña restaurada: " + identificadorPestania + " (índice: " + i + ")"));
                        }
                        return;
                    }
                }
            }
            
            // Si no se encontró, usar la pestaña por defecto
            // Validar rango para evitar IndexOutOfBoundsException
            int tabCount = tabbedPane.getTabCount();
            int indiceValido = Math.max(0, Math.min(pestaniaPorDefecto, tabCount - 1));
            tabbedPane.setSelectedIndex(indiceValido);
            
        } catch (Exception e) {
            gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("Error al restaurar última pestaña seleccionada"), e);
        }
    }

    /**
     * Guarda los anchos de columna de una tabla.
     *
     * @param tabla Tabla cuyos anchos se guardarán
     * @param identificadorTabla Identificador único de la tabla
     */
    public void guardarAnchosColumnasTabla(JTable tabla, String identificadorTabla) {
        if (tabla == null || Normalizador.esVacio(identificadorTabla)) {
            return;
        }

        try {
            Map<String, String> estadoActual = obtenerEstadoUIActual();
            StringBuilder anchos = new StringBuilder();
            
            for (int i = 0; i < tabla.getColumnCount(); i++) {
                if (i > 0) {
                    anchos.append(SEPARADOR_ESTADO);
                }
                anchos.append(tabla.getColumnModel().getColumn(i).getWidth());
            }
            
            String clave = CLAVE_ANCHOS_COLUMNAS + SEPARADOR_ESTADO + identificadorTabla;
            estadoActual.put(clave, anchos.toString());
            guardarEstadoUI(estadoActual);
            
            if (config.esDetallado()) {
                gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Anchos de columna guardados para " + identificadorTabla +
                    ": " + anchos.toString()));
            }
        } catch (Exception e) {
            gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("Error al guardar anchos de columna para " + identificadorTabla), e);
        }
    }

    /**
     * Restaura los anchos de columna de una tabla.
     *
     * @param tabla Tabla cuyos anchos se restaurarán
     * @param identificadorTabla Identificador único de la tabla
     */
    public void restaurarAnchosColumnasTabla(JTable tabla, String identificadorTabla) {
        if (tabla == null || Normalizador.esVacio(identificadorTabla)) {
            return;
        }

        try {
            Map<String, String> estadoGuardado = obtenerEstadoUIGuardado();
            String clave = CLAVE_ANCHOS_COLUMNAS + SEPARADOR_ESTADO + identificadorTabla;
            String anchosGuardados = estadoGuardado.get(clave);
            
            if (Normalizador.noEsVacio(anchosGuardados)) {
                String[] anchos = anchosGuardados.split(SEPARADOR_ESTADO);
                
                if (anchos.length == tabla.getColumnCount()) {
                    for (int i = 0; i < anchos.length && i < tabla.getColumnCount(); i++) {
                        try {
                            int ancho = Integer.parseInt(anchos[i]);
                            if (ancho > 0) {
                                TableColumn columna = tabla.getColumnModel().getColumn(i);
                                columna.setPreferredWidth(ancho);
                                columna.setWidth(ancho);
                            }
                        } catch (NumberFormatException e) {
                            gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("Ancho inválido para columna " + i +
                                " en tabla " + identificadorTabla + ": " + anchos[i]));
                        }
                    }
                    
                    if (config.esDetallado()) {
                        gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Anchos de columna restaurados para " + identificadorTabla));
                    }
                }
            }
        } catch (Exception e) {
            gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("Error al restaurar anchos de columna para " + identificadorTabla), e);
        }
    }

    /**
     * Obtiene el estado UI actual desde la configuración.
     *
     * @return Mapa con estado UI actual
     */
    private Map<String, String> obtenerEstadoUIActual() {
        Map<String, String> estado = config.obtenerEstadoUI();
        return estado != null ? new HashMap<>(estado) : new HashMap<>();
    }

    /**
     * Obtiene el estado UI guardado desde la configuración.
     *
     * @return Mapa con estado UI guardado
     */
    private Map<String, String> obtenerEstadoUIGuardado() {
        Map<String, String> estado = config.obtenerEstadoUI();
        return estado != null ? new HashMap<>(estado) : new HashMap<>();
    }

    /**
     * Guarda el estado UI en la configuración.
     *
     * @param estado Estado UI a guardar
     */
    private void guardarEstadoUI(Map<String, String> estado) {
        config.establecerEstadoUI(estado);
    }
}
