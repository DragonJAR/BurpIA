package com.burpia.analyzer;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorLoggingUnificado;
import burp.api.montoya.http.message.requests.HttpRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("GestorMultiProveedor Tests")
@ExtendWith(MockitoExtension.class)
class GestorMultiProveedorTest {

    @Mock
    private ConfiguracionAPI config;

    @Mock
    private GestorConsolaGUI gestorConsola;

    @Mock
    private GestorLoggingUnificado gestorLogging;

    @Mock
    private HttpRequest solicitudHttp;

    private SolicitudAnalisis solicitud;
    private PrintWriter stdout;
    private PrintWriter stderr;

    @BeforeEach
    void setUp() {
        solicitud = new SolicitudAnalisis(
                "https://test.com",
                "GET",
                "test-hash",
                solicitudHttp,
                null,
                200
        );
        
        stdout = new PrintWriter(OutputStream.nullOutputStream(), true);
        stderr = new PrintWriter(OutputStream.nullOutputStream(), true);
    }

    @Test
    @DisplayName("Validar inicialización del gestor")
    void constructor_ConParametrosValidos_DebeInicializarCorrectamente() {
        GestorMultiProveedor gestor = new GestorMultiProveedor(
                solicitud,
                config,
                stdout,
                stderr,
                gestorConsola,
                () -> false,
                () -> false,
                gestorLogging
        );

        assertNotNull(gestor);
    }

    @Test
    @DisplayName("Validar cancelación inmediata")
    void ejecutarAnalisisMultiProveedor_CanceladoInmediato_DebeLanzarInterruptedException() {
        lenient().when(config.obtenerProveedoresMultiConsulta()).thenReturn(List.of("openai", "claude"));
        
        GestorMultiProveedor gestorCancelado = new GestorMultiProveedor(
                solicitud,
                config,
                stdout,
                stderr,
                gestorConsola,
                () -> true,
                () -> false,
                gestorLogging
        );

        assertThrows(InterruptedException.class, () -> {
            gestorCancelado.ejecutarAnalisisMultiProveedor();
        });
    }


}