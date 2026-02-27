package com.burpia.i18n;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;




@DisplayName("I18nLogs Tests")
class I18nLogsTest {

    @AfterEach
    void restaurarIdioma() {
        I18nUI.establecerIdioma(IdiomaUI.ES);
    }

    @Test
    @DisplayName("trTecnico preserva payload tecnico sin traduccion")
    void trTecnico_preservaPayloadTecnico() {
        I18nUI.establecerIdioma(IdiomaUI.EN);
        String headerTecnico = "Access-Control-Request-Method";
        assertEquals(headerTecnico, I18nLogs.trTecnico(headerTecnico));
    }

    @Test
    @DisplayName("tr aplica traduccion en mensajes convencionales")
    void tr_aplicaTraduccionConvencional() {
        I18nUI.establecerIdioma(IdiomaUI.EN);
        assertEquals("request", I18nLogs.tr("solicitud"));
    }
}
