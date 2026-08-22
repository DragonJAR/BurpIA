package com.burpia.ui;

import com.burpia.config.ConfiguracionAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AlertasOptOutHelper Tests")
class AlertasOptOutHelperTest {

    private ConfiguracionAPI config;

    @BeforeEach
    void setUp() {
        config = new ConfiguracionAPI();
        // Por defecto: alertas habilitadas y "enviar a" habilitadas.
    }

    @Test
    @DisplayName("evaluarAlertaEnviarA retorna true cuando config es null")
    void evaluarAlertaEnviarARetornaTrueConConfigNull() {
        boolean resultado = AlertasOptOutHelper.evaluarAlertaEnviarA(
            AlertasOptOutHelper.ALERTA_MENU_ENVIAR_A, null, () -> true);
        assertTrue(resultado, "Con config null debe mostrar la alerta");
    }

    @Test
    @DisplayName("evaluarAlertaEnviarA respeta el flag global y el opt-out de la clave")
    void evaluarAlertaEnviarARespectaFlagYOptOut() {
        // Flag global habilitado y sin opt-out → true
        assertTrue(AlertasOptOutHelper.evaluarAlertaEnviarA(
            AlertasOptOutHelper.ALERTA_MENU_ENVIAR_A, config,
            config::alertasClickDerechoEnviarAHabilitadas),
            "Flag habilitado y sin opt-out debe mostrar la alerta");

        // Opt-out registrado → false
        config.agregarAlertaDeshabilitada(AlertasOptOutHelper.ALERTA_MENU_ENVIAR_A);
        assertFalse(AlertasOptOutHelper.evaluarAlertaEnviarA(
            AlertasOptOutHelper.ALERTA_MENU_ENVIAR_A, config,
            config::alertasClickDerechoEnviarAHabilitadas),
            "Opt-out registrado debe ocultar la alerta");
    }

    @Test
    @DisplayName("deshabilitarAlertaEnviarA baja el flag, registra opt-out y ejecuta el callback")
    void deshabilitarAlertaEnviarABajaFlagYRegistraOptOut() {
        AtomicBoolean callbackEjecutado = new AtomicBoolean(false);

        AlertasOptOutHelper.deshabilitarAlertaEnviarA(
            AlertasOptOutHelper.ALERTA_HALLAZGOS_ENVIAR_A, config,
            config.alertasClickDerechoEnviarAHabilitadas(),
            () -> callbackEjecutado.set(true));

        assertFalse(config.alertasClickDerechoEnviarAHabilitadas(),
            "El flag global debe quedar en false");
        assertTrue(config.obtenerAlertasDeshabilitadas()
                .containsKey(AlertasOptOutHelper.ALERTA_HALLAZGOS_ENVIAR_A),
            "El opt-out debe registrarse para la clave");
        assertTrue(callbackEjecutado.get(),
            "El callback onChange debe ejecutarse");
    }

    @Test
    @DisplayName("deshabilitarAlertaEnviarA no hace nada si el flag ya estaba en false")
    void deshabilitarAlertaEnviarAEsNoOpSiFlagYaFalse() {
        config.establecerAlertasClickDerechoEnviarAHabilitadas(false);
        AtomicBoolean callbackEjecutado = new AtomicBoolean(false);

        AlertasOptOutHelper.deshabilitarAlertaEnviarA(
            AlertasOptOutHelper.ALERTA_MENU_ENVIAR_A, config,
            config.alertasClickDerechoEnviarAHabilitadas(),
            () -> callbackEjecutado.set(true));

        assertFalse(callbackEjecutado.get(),
            "No debe ejecutar el callback si el flag ya estaba en false");
        assertFalse(config.obtenerAlertasDeshabilitadas()
                .containsKey(AlertasOptOutHelper.ALERTA_MENU_ENVIAR_A),
            "No debe registrar opt-out si no había nada que deshabilitar");
    }
}
