package com.burpia.i18n;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validación de runtime del plugin: confirma que las traducciones corregidas
 * (slots ES que estaban en inglés) devuelven los valores esperados en ambos
 * idiomas. Es la prueba empírica de que el plugin funciona como se espera
 * tras la auditoría i18n.
 */
@DisplayName("I18n Plugin Validation")
class I18nPluginValidationTest {

    private IdiomaUI idiomaPrevio;

    @BeforeEach
    void capturarIdioma() {
        idiomaPrevio = I18nUI.obtenerIdioma();
    }

    @AfterEach
    void restaurarIdioma() {
        if (idiomaPrevio != null) {
            I18nUI.establecerIdioma(idiomaPrevio.codigo());
        }
    }

    @Test
    @DisplayName("CAMBIO_API_KEY devuelve 'Clave de API' en ES y 'API Key' en EN (fix slot ES)")
    void cambioApiKeySlotEspanolCorregido() {
        I18nUI.establecerIdioma("es");
        assertEquals("Clave de API", I18nUI.Configuracion.CAMBIO_API_KEY(),
            "El slot ES de CAMBIO_API_KEY debe ser 'Clave de API' (antes estaba en inglés)");

        I18nUI.establecerIdioma("en");
        assertEquals("API Key", I18nUI.Configuracion.CAMBIO_API_KEY(),
            "El slot EN de CAMBIO_API_KEY debe ser 'API Key'");
    }

    @Test
    @DisplayName("LABEL_TIMEOUT_MODELO traduce 'Timeout' a 'Tiempo de espera' en ES (fix slot ES)")
    void labelTimeoutModeloSlotEspanolCorregido() {
        I18nUI.establecerIdioma("es");
        String valorEs = I18nUI.Configuracion.LABEL_TIMEOUT_MODELO();
        assertTrue(valorEs.contains("Tiempo de espera"),
            "El slot ES debe traducir 'Timeout' a 'Tiempo de espera'. Valor real: " + valorEs);
        assertTrue(valorEs.contains("Modelo"),
            "El slot ES debe contener 'Modelo'. Valor real: " + valorEs);

        I18nUI.establecerIdioma("en");
        String valorEn = I18nUI.Configuracion.LABEL_TIMEOUT_MODELO();
        assertTrue(valorEn.contains("Timeout"),
            "El slot EN debe contener 'Timeout'. Valor real: " + valorEn);
    }

    @Test
    @DisplayName("Entradas de diccionario nuevas de I18nLogs traducen correctamente a EN")
    void entradasDiccionarioNuevasTraducen() {
        I18nUI.establecerIdioma("en");

        assertTrue(I18nLogs.trf("Tarea creada: %s - %s", "X", "/y").contains("Task created"));
        assertTrue(I18nLogs.trf("Contenido extraído - Longitud: %d caracteres", 42).contains("characters"));
        assertTrue(I18nLogs.trf("Error en monitor de tareas atascadas").contains("stuck-tasks"));
        assertTrue(I18nLogs.trf("Analisis forzado solicitado desde menu contextual: %s %s", "GET", "/x")
                .contains("Forced analysis requested"));
        assertTrue(I18nLogs.trf("AVISO: Multi-proveedor habilitado con %d proveedor(s). Se usará proveedor único: %s",
                1, "openai").contains("WARNING: Multi-provider"));
    }
}
