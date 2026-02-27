package com.burpia.ui;

import com.burpia.config.ConfiguracionAPI;
import com.jediterm.terminal.TtyConnector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.swing.SwingUtilities;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PanelAgente Transporte Tests")
class PanelAgenteTransporteTest {

    @Test
    @DisplayName("Payload corto por TTY se envia en una sola escritura")
    void testEscrituraCortaViaTtySinChunking() throws Exception {
        PanelAgente panel = crearPanelSinConsola();
        try {
            TtyConnector connector = mock(TtyConnector.class);
            when(connector.isConnected()).thenReturn(true);
            inyectarTtyConnector(panel, connector);

            boolean resultado = invocarEscrituraTty(panel, "hola");

            assertTrue(resultado);
            verify(connector, times(1)).write("hola");
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Payload largo por TTY se divide en chunks y preserva contenido")
    void testEscrituraLargaViaTtyConChunking() throws Exception {
        PanelAgente panel = crearPanelSinConsola();
        try {
            TtyConnector connector = mock(TtyConnector.class);
            when(connector.isConnected()).thenReturn(true);
            inyectarTtyConnector(panel, connector);

            String payload = "A".repeat(260);
            boolean resultado = invocarEscrituraTty(panel, payload);

            assertTrue(resultado);
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(connector, times(3)).write(captor.capture());
            String reconstruido = String.join("", captor.getAllValues());
            assertEquals(payload, reconstruido);
        } finally {
            panel.destruir();
        }
    }

    @Test
    @DisplayName("Escritura TTY falla limpio si conector no esta conectado")
    void testEscrituraTtyConectorDesconectado() throws Exception {
        PanelAgente panel = crearPanelSinConsola();
        try {
            TtyConnector connector = mock(TtyConnector.class);
            when(connector.isConnected()).thenReturn(false);
            inyectarTtyConnector(panel, connector);

            boolean resultado = invocarEscrituraTty(panel, "payload");

            assertFalse(resultado);
            verify(connector, never()).write(anyString());
        } finally {
            panel.destruir();
        }
    }

    private PanelAgente crearPanelSinConsola() throws Exception {
        final PanelAgente[] holder = new PanelAgente[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new PanelAgente(new ConfiguracionAPI(), false));
        return holder[0];
    }

    private void inyectarTtyConnector(PanelAgente panel, TtyConnector connector) throws Exception {
        Field field = PanelAgente.class.getDeclaredField("ttyConnector");
        field.setAccessible(true);
        field.set(panel, connector);
    }

    private boolean invocarEscrituraTty(PanelAgente panel, String payload) throws Exception {
        Method method = PanelAgente.class.getDeclaredMethod("escribirTextoViaTtyConnector", String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(panel, payload);
    }
}
