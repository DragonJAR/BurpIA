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
     * Registra que el usuario ha desactivado una alerta especÃ­fica.
     *
     * @param claveAlerta clave Ãºnica de la alerta (ver constantes de clase)
     * @param config      configuraciÃ³n activa
     */
    public static void registrarDeshabilitacion(String claveAlerta, ConfiguracionAPI config) {
        if (config == null || Normalizador.esVacio(claveAlerta)) {
            return;
        }
        config.agregarAlertaDeshabilitada(claveAlerta);
    }

    /**
     * EvalÃºa si las alertas de "enviar a" deben mostrarse, combinando el flag
     * global de config con el opt-out especÃ­fico de la clave.
     *
     * <p>Unifica el wrapper privado duplicado en FabricaMenuContextual y
     * PanelHallazgos.
     *
     * @param claveAlerta        Clave de opt-out (p.ej. {@link #ALERTA_MENU_ENVIAR_A}).
     * @param config             ConfiguraciÃ³n activa (null-safe).
     * @param flagHabilitado     Supplier que indica si el flag global de config deja
     *                           mostrar la alerta; se invoca solo si config no es null.
     * @return {@code true} si la alerta debe mostrarse.
     */
    public static boolean evaluarAlertaEnviarA(String claveAlerta, ConfiguracionAPI config,
            java.util.function.BooleanSupplier flagHabilitado) {
        boolean flag = (config == null) || flagHabilitado.getAsBoolean();
        return flag && debeMostrarAlerta(claveAlerta, config);
    }

    /**
     * Deshabilita las alertas de "enviar a" para una clave, persistiendo el opt-out
     * y bajando el flag global de config.
     *
     * <p>Unifica el wrapper privado duplicado en FabricaMenuContextual y
     * PanelHallazgos.
     *
     * @param claveAlerta        Clave de opt-out.
     * @param config             ConfiguraciÃ³n activa.
     * @param flagEstaba         Estado actual del flag global; si es false, no hace nada.
     * @param onChange           Callback a ejecutar tras deshabilitar (puede ser null).
     */
    public static void deshabilitarAlertaEnviarA(String claveAlerta, ConfiguracionAPI config,
            boolean flagEstaba, Runnable onChange) {
        if (config == null || !flagEstaba) {
            return;
        }
        registrarDeshabilitacion(claveAlerta, config);
        config.establecerAlertasClickDerechoEnviarAHabilitadas(false);
        if (onChange != null) {
            onChange.run();
        }
    }

    // alertasEnviarAHabilitadas(String, ConfiguracionAPI) removed (orphan):
    // mÃ©todo estÃ¡tico bypassed por wrappers privados (sin args) en
    // FabricaMenuContextual y PanelHallazgos. Los callers reales usan
    // debeMostrarAlerta(claveAlerta, config) directamente, lo que hace este
    // wrapper redundante.
}
