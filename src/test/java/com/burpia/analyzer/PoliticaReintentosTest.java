package com.burpia.analyzer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.net.SocketTimeoutException;
import static org.junit.jupiter.api.Assertions.assertTrue;





@DisplayName("PoliticaReintentos Tests")
class PoliticaReintentosTest {

    @Test
    @DisplayName("429 con Retry-After en segundos respeta espera del servidor")
    void dado429ConRetryAfterEnSegundos_respetaEsperaServidor() {
        long esperaMs = PoliticaReintentos.calcularEsperaMs(429, "5", 1000L, 1);
        assertTrue(esperaMs >= 5000L);
    }

    @Test
    @DisplayName("400 model required se clasifica como no reintentable")
    void dado400ModelRequired_noSeReintenta() {
        assertTrue(PoliticaReintentos.esCodigoNoReintentable(400, "model is required"));
    }

    @Test
    @DisplayName("503 se clasifica como reintentable")
    void dado503_esReintentable() {
        assertTrue(PoliticaReintentos.esCodigoReintentable(503));
    }

    @Test
    @DisplayName("SocketTimeoutException se considera reintentable")
    void socketTimeout_esReintentable() {
        assertTrue(PoliticaReintentos.esExcepcionReintentable(new SocketTimeoutException("timeout")));
    }
}
