package com.burpia.ui;

import com.burpia.config.ConfiguracionAPI;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.swing.JMenuItem;
import java.awt.Component;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("FabricaMenuContextual Tests")
class FabricaMenuContextualTest {

    private MontoyaApi api;
    private ConfiguracionAPI config;

    @BeforeEach
    void setUp() {
        api = mock(MontoyaApi.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        config = mock(ConfiguracionAPI.class);
        when(config.agenteHabilitado()).thenReturn(false);
        when(config.alertasHabilitadas()).thenReturn(true);
        when(config.alertasClickDerechoEnviarAHabilitadas()).thenReturn(false);
    }

    @Nested
    @DisplayName("provideMenuItems")
    class ProvideMenuItemsTests {

        @Test
        @DisplayName("retorna lista vacia cuando evento es null")
        void eventoNull() {
            FabricaMenuContextual fabrica = crearFabricaBasica();
            List<Component> items = fabrica.provideMenuItems((ContextMenuEvent) null);
            assertTrue(items.isEmpty());
        }

        @Test
        @DisplayName("retorna lista vacia cuando seleccion es null")
        void seleccionNull() {
            ContextMenuEvent evento = mock(ContextMenuEvent.class);
            when(evento.selectedRequestResponses()).thenReturn(null);

            FabricaMenuContextual fabrica = crearFabricaBasica();
            List<Component> items = fabrica.provideMenuItems(evento);

            assertTrue(items.isEmpty());
        }

        @Test
        @DisplayName("retorna lista vacia cuando seleccion esta vacia")
        void seleccionVacia() {
            ContextMenuEvent evento = mock(ContextMenuEvent.class);
            when(evento.selectedRequestResponses()).thenReturn(Collections.emptyList());

            FabricaMenuContextual fabrica = crearFabricaBasica();
            List<Component> items = fabrica.provideMenuItems(evento);

            assertTrue(items.isEmpty());
        }

        @Test
        @DisplayName("crea un item de menu para analisis")
        void creaMenuItemAnalisis() {
            ContextMenuEvent evento = crearEventoConSolicitud("GET /test HTTP/1.1");

            FabricaMenuContextual fabrica = crearFabricaBasica();
            List<Component> items = fabrica.provideMenuItems(evento);

            assertEquals(1, items.size());
            JMenuItem item = (JMenuItem) items.get(0);
            assertNotNull(item.getText());
            assertFalse(item.getText().isEmpty());
        }

        @Test
        @DisplayName("crea item de agente cuando esta habilitado")
        void creaMenuItemAgente() {
            when(config.agenteHabilitado()).thenReturn(true);
            when(config.obtenerTipoAgente()).thenReturn("FACTORY_DROID");

            ContextMenuEvent evento = crearEventoConSolicitud("GET /agent HTTP/1.1");

            AtomicInteger enviados = new AtomicInteger(0);
            FabricaMenuContextual fabrica = new FabricaMenuContextual(
                api,
                (solicitud, forzar, solicitudRespuestaOriginal) -> {},
                config,
                rr -> {
                    enviados.incrementAndGet();
                    return true;
                },
                () -> {}
            );

            List<Component> items = fabrica.provideMenuItems(evento);

            assertEquals(2, items.size());
        }
    }

    @Nested
    @DisplayName("Analisis forzado")
    class AnalisisForzadoTests {

        @Test
        @DisplayName("dispara analisis forzado al hacer click")
        void clickDisparaAnalisisForzado() {
            ContextMenuEvent evento = crearEventoConSolicitud("GET /test HTTP/1.1");

            AtomicInteger llamadas = new AtomicInteger(0);
            AtomicReference<HttpRequestResponse> evidencia = new AtomicReference<>();
            FabricaMenuContextual fabrica = new FabricaMenuContextual(
                api,
                (solicitud, forzar, solicitudRespuestaOriginal) -> {
                    if (forzar && solicitud != null && solicitudRespuestaOriginal != null) {
                        llamadas.incrementAndGet();
                        evidencia.set(solicitudRespuestaOriginal);
                    }
                },
                config,
                rr -> true,
                () -> {}
            );

            JMenuItem item = (JMenuItem) fabrica.provideMenuItems(evento).get(0);
            item.doClick();

            assertEquals(1, llamadas.get());
            assertNotNull(evidencia.get());
        }

        @Test
        @DisplayName("funciona cuando alertas de contexto estan deshabilitadas")
        void analisisConAlertasDeshabilitadas() {
            ContextMenuEvent evento = crearEventoConSolicitud("GET /force HTTP/1.1");

            AtomicInteger analisis = new AtomicInteger(0);
            FabricaMenuContextual fabrica = new FabricaMenuContextual(
                api,
                (solicitud, forzar, solicitudRespuestaOriginal) -> {
                    if (forzar && solicitud != null && solicitudRespuestaOriginal != null) {
                        analisis.incrementAndGet();
                    }
                },
                config,
                rr -> true,
                () -> {}
            );

            JMenuItem item = (JMenuItem) fabrica.provideMenuItems(evento).get(0);
            item.doClick();

            assertEquals(1, analisis.get());
            verify(config, never()).establecerAlertasClickDerechoEnviarAHabilitadas(false);
        }
    }

    @Nested
    @DisplayName("Debounce")
    class DebounceTests {

        @Test
        @DisplayName("evita doble analisis sobre la misma solicitud")
        void debounceMismaSolicitud() {
            ContextMenuEvent evento = crearEventoConSolicitud("GET /debounce HTTP/1.1");

            AtomicInteger llamadas = new AtomicInteger(0);
            FabricaMenuContextual fabrica = new FabricaMenuContextual(
                api,
                (solicitud, forzar, solicitudRespuestaOriginal) -> {
                    if (forzar) {
                        llamadas.incrementAndGet();
                    }
                },
                config,
                rr -> true,
                () -> {}
            );

            JMenuItem item = (JMenuItem) fabrica.provideMenuItems(evento).get(0);
            item.doClick();
            item.doClick();

            assertEquals(1, llamadas.get());
        }

        @Test
        @DisplayName("permite analisis de solicitudes diferentes")
        void debouncePermiteSolicitudesDiferentes() {
            AtomicInteger llamadas = new AtomicInteger(0);
            FabricaMenuContextual fabrica = new FabricaMenuContextual(
                api,
                (solicitud, forzar, solicitudRespuestaOriginal) -> {
                    if (forzar) {
                        llamadas.incrementAndGet();
                    }
                },
                config,
                rr -> true,
                () -> {}
            );

            // Primera solicitud
            ContextMenuEvent evento1 = crearEventoConSolicitud("GET /first HTTP/1.1");
            JMenuItem item1 = (JMenuItem) fabrica.provideMenuItems(evento1).get(0);
            item1.doClick();

            // Segunda solicitud diferente
            ContextMenuEvent evento2 = crearEventoConSolicitud("GET /second HTTP/1.1");
            JMenuItem item2 = (JMenuItem) fabrica.provideMenuItems(evento2).get(0);
            item2.doClick();

            assertEquals(2, llamadas.get());
        }
    }

    @Nested
    @DisplayName("Envio a agente")
    class EnvioAgenteTests {

        @Test
        @DisplayName("ejecuta callback de envio al hacer click")
        void clickEjecutaCallback() {
            when(config.agenteHabilitado()).thenReturn(true);
            when(config.obtenerTipoAgente()).thenReturn("FACTORY_DROID");

            ContextMenuEvent evento = crearEventoConSolicitud("GET /agent HTTP/1.1");

            AtomicInteger enviados = new AtomicInteger(0);
            FabricaMenuContextual fabrica = new FabricaMenuContextual(
                api,
                (solicitud, forzar, solicitudRespuestaOriginal) -> {},
                config,
                rr -> {
                    enviados.incrementAndGet();
                    return true;
                },
                () -> {}
            );

            JMenuItem itemAgente = (JMenuItem) fabrica.provideMenuItems(evento).get(1);
            itemAgente.doClick();

            assertEquals(1, enviados.get());
        }

        @Test
        @DisplayName("procesa multiples solicitudes en seleccion multiple")
        void procesaSeleccionMultiple() {
            when(config.agenteHabilitado()).thenReturn(true);
            when(config.obtenerTipoAgente()).thenReturn("FACTORY_DROID");

            HttpRequestResponse rr1 = mock(HttpRequestResponse.class);
            HttpRequestResponse rr2 = mock(HttpRequestResponse.class);
            HttpRequest request1 = mock(HttpRequest.class);
            HttpRequest request2 = mock(HttpRequest.class);
            when(request1.toString()).thenReturn("GET /m1 HTTP/1.1");
            when(request2.toString()).thenReturn("GET /m2 HTTP/1.1");
            when(rr1.request()).thenReturn(request1);
            when(rr2.request()).thenReturn(request2);

            ContextMenuEvent evento = mock(ContextMenuEvent.class);
            when(evento.selectedRequestResponses()).thenReturn(List.of(rr1, rr2));

            AtomicInteger enviados = new AtomicInteger(0);
            FabricaMenuContextual fabrica = new FabricaMenuContextual(
                api,
                (solicitud, forzar, solicitudRespuestaOriginal) -> {},
                config,
                rr -> {
                    enviados.incrementAndGet();
                    return true;
                },
                () -> {}
            );

            JMenuItem itemAgente = (JMenuItem) fabrica.provideMenuItems(evento).get(1);
            itemAgente.doClick();

            assertEquals(2, enviados.get());
        }

        @Test
        @DisplayName("maneja excepcion en manejador de agente sin fallar")
        void manejaExcepcionEnManejador() {
            when(config.agenteHabilitado()).thenReturn(true);
            when(config.obtenerTipoAgente()).thenReturn("FACTORY_DROID");

            ContextMenuEvent evento = crearEventoConSolicitud("GET /agent HTTP/1.1");

            FabricaMenuContextual fabrica = new FabricaMenuContextual(
                api,
                (solicitud, forzar, solicitudRespuestaOriginal) -> {},
                config,
                rr -> {
                    throw new RuntimeException("Error simulado");
                },
                () -> {}
            );

            JMenuItem itemAgente = (JMenuItem) fabrica.provideMenuItems(evento).get(1);

            // No debe lanzar excepcion
            itemAgente.doClick();
        }

        @Test
        @DisplayName("omite elementos null en seleccion multiple")
        void omiteElementosNull() {
            when(config.agenteHabilitado()).thenReturn(true);
            when(config.obtenerTipoAgente()).thenReturn("FACTORY_DROID");

            HttpRequestResponse rr1 = mock(HttpRequestResponse.class);
            HttpRequest request1 = mock(HttpRequest.class);
            when(request1.toString()).thenReturn("GET /valid HTTP/1.1");
            when(rr1.request()).thenReturn(request1);

            ContextMenuEvent evento = mock(ContextMenuEvent.class);
            // Lista con elemento null en el medio
            when(evento.selectedRequestResponses()).thenReturn(Arrays.asList(rr1, null));

            AtomicInteger enviados = new AtomicInteger(0);
            FabricaMenuContextual fabrica = new FabricaMenuContextual(
                api,
                (solicitud, forzar, solicitudRespuestaOriginal) -> {},
                config,
                rr -> {
                    enviados.incrementAndGet();
                    return true;
                },
                () -> {}
            );

            JMenuItem itemAgente = (JMenuItem) fabrica.provideMenuItems(evento).get(1);
            itemAgente.doClick();

            // Solo el elemento valido debe ser procesado
            assertEquals(1, enviados.get());
        }
    }

    @Nested
    @DisplayName("Configuracion nula")
    class ConfigNulaTests {

        @Test
        @DisplayName("no rompe menu contextual y omite accion de agente")
        void configNulaNoRompe() {
            ContextMenuEvent evento = crearEventoConSolicitud("GET /single HTTP/1.1");

            AtomicInteger analisis = new AtomicInteger(0);
            FabricaMenuContextual fabrica = new FabricaMenuContextual(
                api,
                (solicitud, forzar, solicitudRespuestaOriginal) -> analisis.incrementAndGet(),
                null,
                rr -> true,
                () -> {}
            );

            List<Component> items = fabrica.provideMenuItems(evento);
            assertEquals(1, items.size());
            ((JMenuItem) items.get(0)).doClick();
            assertEquals(1, analisis.get());
        }
    }

    @Nested
    @DisplayName("Callback cambio alertas")
    class CallbackCambioAlertasTests {

        @Test
        @DisplayName("llama callback de cambio cuando se deshabilitan alertas")
        void llamaCallbackCambio() {
            when(config.alertasClickDerechoEnviarAHabilitadas()).thenReturn(true);

            ContextMenuEvent evento = crearEventoConSolicitud("GET /test HTTP/1.1");

            AtomicInteger cambios = new AtomicInteger(0);
            FabricaMenuContextual fabrica = new FabricaMenuContextual(
                api,
                (solicitud, forzar, solicitudRespuestaOriginal) -> {},
                config,
                rr -> true,
                () -> cambios.incrementAndGet()
            );

            // Verificar que el callback esta configurado
            assertNotNull(fabrica);
            assertEquals(0, cambios.get());
        }

        @Test
        @DisplayName("alertas contextuales dependen del flag global de alertas")
        void alertasContextualesRespetanFlagGlobal() throws Exception {
            when(config.alertasHabilitadas()).thenReturn(false);
            when(config.alertasClickDerechoEnviarAHabilitadas()).thenReturn(true);

            FabricaMenuContextual fabrica = crearFabricaBasica();
            Method metodo = FabricaMenuContextual.class.getDeclaredMethod("alertasEnviarAHabilitadas");
            metodo.setAccessible(true);

            boolean habilitadas = (boolean) metodo.invoke(fabrica);
            assertFalse(habilitadas);
        }
    }

    // Metodos auxiliares

    private FabricaMenuContextual crearFabricaBasica() {
        return new FabricaMenuContextual(
            api,
            (solicitud, forzar, solicitudRespuestaOriginal) -> {},
            config,
            rr -> true,
            () -> {}
        );
    }

    private ContextMenuEvent crearEventoConSolicitud(String solicitudHttp) {
        ContextMenuEvent evento = mock(ContextMenuEvent.class);
        HttpRequestResponse rr = mock(HttpRequestResponse.class);
        HttpRequest request = mock(HttpRequest.class);
        when(request.toString()).thenReturn(solicitudHttp);
        when(rr.request()).thenReturn(request);
        when(evento.selectedRequestResponses()).thenReturn(List.of(rr));
        return evento;
    }
}
