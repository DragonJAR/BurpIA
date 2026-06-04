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

    // NOTA: Las constantes ALERTA_MENU_FLUJO_*, ALERTA_MENU_SOLICITUD_INICIADA,
    // ALERTA_MENU_ENVIO_AGENTE_*, ALERTA_HALLAZGOS_ANALISIS_INICIADO y
    // ALERTA_HALLAZGOS_ENVIO_* (Repeater/Intruder/Scanner/Issues) fueron
    // pre-declaradas para features que nunca se conectaron al sistema de
    // opt-out. Removidas en cleanup post-audit. Si en el futuro se necesita
    // un opt-out granular por acción, se vuelven a declarar.

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

    // alertasEnviarAHabilitadas(String, ConfiguracionAPI) removed (orphan):
    // método estático bypassed por wrappers privados (sin args) en
    // FabricaMenuContextual y PanelHallazgos. Los callers reales usan
    // debeMostrarAlerta(claveAlerta, config) directamente, lo que hace este
    // wrapper redundante.
}
