package com.burpia.ui;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.util.Normalizador;

/**
 * Helper centralizado para manejar el patrón "No volver a mostrar esta alerta" (opt-out).
 *
 * <p>Patrón detectado en:</p>
 * <ul>
 *   <li>{@code FabricaMenuContextual} — alertas de menú contextual (enviar a)</li>
 *   <li>{@code PanelHallazgos} — alertas de acciones sobre hallazgos</li>
 * </ul>
 *
 * <p>El helper utiliza un {@link Map} de claves de alerta en {@link ConfiguracionAPI}
 * para persistir cuáles alertas el usuario ha desactivado.</p>
 *
 * <h3>Convenciones de nombres para claves de alerta</h3>
 * <ul>
 *   <li>Formato: {@code alerta_<contexto>_<descripcion>} en snake_case</li>
 *   <li>Ejemplos:
 *     <ul>
 *       <li>{@code alerta_menu_contextual_enviar_analisis}</li>
 *       <li>{@code alerta_menu_contextual_enviar_agente}</li>
 *       <li>{@code alerta_hallazgos_enviar_repeater}</li>
 *       <li>{@code alerta_hallazgos_enviar_intruder}</li>
 *       <li>{@code alerta_hallazgos_enviar_scanner}</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public final class AlertasOptOutHelper {

    /** Clave global para alertas de acciones "Enviar a" en menú contextual */
    public static final String ALERTA_MENU_ENVIAR_A = "alerta_menu_enviar_a";

    /** Clave global para alertas de acciones "Enviar a" desde hallazgos */
    public static final String ALERTA_HALLAZGOS_ENVIAR_A = "alerta_hallazgos_enviar_a";

    /** Clave para la alerta de análisis de flujo en menú contextual */
    public static final String ALERTA_MENU_FLUJO_INICIADO = "alerta_menu_flujo_iniciado";

    /** Clave para la alerta de análisis de solicitud individual en menú contextual */
    public static final String ALERTA_MENU_SOLICITUD_INICIADA = "alerta_menu_solicitud_iniciada";

    /** Clave para la alerta de envío de agente (solicitud) en menú contextual */
    public static final String ALERTA_MENU_ENVIO_AGENTE_SOLICITUD = "alerta_menu_envio_agente_solicitud";

    /** Clave para la alerta de envío de agente (flujo) en menú contextual */
    public static final String ALERTA_MENU_ENVIO_AGENTE_FLUJO = "alerta_menu_envio_agente_flujo";

    /** Clave para la alerta de flujo requiere múltiples solicitudes válidas */
    public static final String ALERTA_MENU_FLUJO_REQUIERE_MULTIPLES_VALIDAS = "alerta_menu_flujo_requiere_multiples_validas";

    /** Clave para la alerta de flujo excede máximo de peticiones */
    public static final String ALERTA_MENU_FLUJO_MAXIMO_PETICIONES = "alerta_menu_flujo_maximo_peticiones";

    /** Clave para la alerta de análisis de hallazgos iniciado */
    public static final String ALERTA_HALLAZGOS_ANALISIS_INICIADO = "alerta_hallazgos_analisis_iniciado";

    /** Clave para la alerta de envío a Repeater */
    public static final String ALERTA_HALLAZGOS_ENVIO_REPEATER = "alerta_hallazgos_envio_repeater";

    /** Clave para la alerta de envío a Intruder */
    public static final String ALERTA_HALLAZGOS_ENVIO_INTRUDER = "alerta_hallazgos_envio_intruder";

    /** Clave para la alerta de envío a Scanner */
    public static final String ALERTA_HALLAZGOS_ENVIO_SCANNER = "alerta_hallazgos_envio_scanner";

    /** Clave para la alerta de envío a Issues */
    public static final String ALERTA_HALLAZGOS_ENVIO_ISSUES = "alerta_hallazgos_envio_issues";

    private AlertasOptOutHelper() {
        // Utility class
    }

    /**
     * Verifica si una alerta específica debe ser mostrada.
     *
     * @param claveAlerta clave única de la alerta (ver constantes de clase)
     * @param config      configuración activa
     * @return {@code true} si la alerta debe mostrarse, {@code false} si fue desactivada
     */
    public static boolean debeMostrarAlerta(String claveAlerta, ConfiguracionAPI config) {
        if (config == null || Normalizador.esVacio(claveAlerta)) {
            return true;
        }
        if (!config.alertasHabilitadas()) {
            return false;
        }
        return !config.obtenerAlertasDeshabilitadas().containsKey(claveAlerta);
    }

    /**
     * Registra que el usuario ha desactivado una alerta específica.
     *
     * @param claveAlerta clave única de la alerta (ver constantes de clase)
     * @param config      configuración activa
     */
    public static void registrarDeshabilitacion(String claveAlerta, ConfiguracionAPI config) {
        if (config == null || Normalizador.esVacio(claveAlerta)) {
            return;
        }
        config.agregarAlertaDeshabilitada(claveAlerta);
    }

    /**
     * Combina la verificación de {@link ConfiguracionAPI#alertasHabilitadas()}
     * con la clave de opt-out individual.
     *
     * @param claveAlerta clave única de la alerta
     * @param config      configuración activa
     * @return {@code true} si la alerta debe mostrarse
     */
    public static boolean alertasEnviarAHabilitadas(String claveAlerta, ConfiguracionAPI config) {
        return debeMostrarAlerta(claveAlerta, config);
    }
}
