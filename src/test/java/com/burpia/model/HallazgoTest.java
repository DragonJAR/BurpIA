package com.burpia.model;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.burpia.i18n.I18nUI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Hallazgo Model Tests")
class HallazgoTest {

    @Nested
    @DisplayName("Constructores")
    class Constructores {

        @Test
        @DisplayName("Crear hallazgo con parametros basicos")
        void testCrearHallazgoBasico() {
            Hallazgo hallazgo = new Hallazgo(
                "https://example.com/test",
                "XSS",
                "XSS detectado",
                "High",
                "Medium"
            );

            assertNotNull(hallazgo);
            assertEquals("https://example.com/test", hallazgo.obtenerUrl());
            assertEquals("XSS", hallazgo.obtenerTitulo());
            assertEquals("XSS detectado", hallazgo.obtenerHallazgo());
            assertEquals("High", hallazgo.obtenerSeveridad());
            assertEquals("Medium", hallazgo.obtenerConfianza());
            assertNull(hallazgo.obtenerSolicitudHttp());
            assertNull(hallazgo.obtenerEvidenciaHttp());
            assertNull(hallazgo.obtenerEvidenciaId());
        }

        @Test
        @DisplayName("Crear hallazgo con solicitud HTTP")
        void testCrearHallazgoConSolicitud() {
            HttpRequest solicitud = mock(HttpRequest.class);
            Hallazgo hallazgo = new Hallazgo(
                "https://example.com/test",
                "SQLi",
                "SQL Injection",
                "Critical",
                "High",
                solicitud
            );

            assertNotNull(hallazgo);
            assertSame(solicitud, hallazgo.obtenerSolicitudHttp());
            assertNull(hallazgo.obtenerEvidenciaHttp());
        }

        @Test
        @DisplayName("Crear hallazgo con hora de descubrimiento")
        void testCrearHallazgoConHora() {
            Hallazgo hallazgo = new Hallazgo(
                "10:20:30",
                "https://example.com/test",
                "RCE",
                "Remote Code Execution",
                "Critical",
                "High",
                (HttpRequest) null
            );

            assertEquals("10:20:30", hallazgo.obtenerHoraDescubrimiento());
        }

        @Test
        @DisplayName("Crear hallazgo completo con todos los parametros")
        void testCrearHallazgoCompleto() {
            HttpRequest solicitud = mock(HttpRequest.class);
            HttpRequestResponse evidencia = mock(HttpRequestResponse.class);

            Hallazgo hallazgo = new Hallazgo(
                "10:20:30",
                "https://example.com/test",
                "SSRF",
                "Server-Side Request Forgery",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA,
                solicitud,
                evidencia
            );

            assertEquals("10:20:30", hallazgo.obtenerHoraDescubrimiento());
            assertEquals("https://example.com/test", hallazgo.obtenerUrl());
            assertEquals("SSRF", hallazgo.obtenerTitulo());
            assertEquals("Server-Side Request Forgery", hallazgo.obtenerHallazgo());
            assertEquals(Hallazgo.SEVERIDAD_HIGH, hallazgo.obtenerSeveridad());
            assertEquals(Hallazgo.CONFIANZA_ALTA, hallazgo.obtenerConfianza());
            assertSame(solicitud, hallazgo.obtenerSolicitudHttp());
            assertSame(evidencia, hallazgo.obtenerEvidenciaHttp());
        }

        @Test
        @DisplayName("Crear hallazgo con evidencia ID")
        void testCrearHallazgoConEvidenciaId() {
            HttpRequest solicitud = mock(HttpRequest.class);

            Hallazgo hallazgo = new Hallazgo(
                "10:20:30",
                "https://example.com/test",
                "LFI",
                "Local File Inclusion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_MEDIA,
                solicitud,
                null,
                "evidencia-123"
            );

            assertEquals("evidencia-123", hallazgo.obtenerEvidenciaId());
            assertNull(hallazgo.obtenerEvidenciaHttp());
        }

        @Test
        @DisplayName("Constructor con parametros nulos normaliza valores")
        void testConstructorConParametrosNulos() {
            Hallazgo hallazgo = new Hallazgo(
                null,
                null,
                null,
                null,
                null
            );

            assertNull(hallazgo.obtenerUrl());
            assertNull(hallazgo.obtenerTitulo());
            assertNull(hallazgo.obtenerHallazgo());
            assertEquals(Hallazgo.SEVERIDAD_INFO, hallazgo.obtenerSeveridad()); // Valor por defecto
            assertEquals(Hallazgo.CONFIANZA_MEDIA, hallazgo.obtenerConfianza()); // Valor por defecto
        }

        @Test
        @DisplayName("Constructor con parametros vacios normaliza valores")
        void testConstructorConParametrosVacios() {
            Hallazgo hallazgo = new Hallazgo(
                "",
                "",
                "",
                "",
                ""
            );

            assertEquals("", hallazgo.obtenerUrl());
            assertEquals("", hallazgo.obtenerTitulo());
            assertEquals("", hallazgo.obtenerHallazgo());
            assertEquals(Hallazgo.SEVERIDAD_INFO, hallazgo.obtenerSeveridad()); // Valor por defecto
            assertEquals(Hallazgo.CONFIANZA_MEDIA, hallazgo.obtenerConfianza()); // Valor por defecto
        }
    }

    @Nested
    @DisplayName("Validacion de severidad")
    class ValidacionSeveridad {

        @Test
        @DisplayName("Validar severidades validas")
        void testSeveridadesValidas() {
            assertTrue(Hallazgo.esSeveridadValida(Hallazgo.SEVERIDAD_CRITICAL));
            assertTrue(Hallazgo.esSeveridadValida(Hallazgo.SEVERIDAD_HIGH));
            assertTrue(Hallazgo.esSeveridadValida(Hallazgo.SEVERIDAD_MEDIUM));
            assertTrue(Hallazgo.esSeveridadValida(Hallazgo.SEVERIDAD_LOW));
            assertTrue(Hallazgo.esSeveridadValida(Hallazgo.SEVERIDAD_INFO));
        }

        @Test
        @DisplayName("Rechazar severidades invalidas")
        void testSeveridadesInvalidas() {
            assertFalse(Hallazgo.esSeveridadValida("Alta"));
            assertFalse(Hallazgo.esSeveridadValida("alta"));
            assertFalse(Hallazgo.esSeveridadValida("CRITICAL"));
            assertFalse(Hallazgo.esSeveridadValida(null));
            assertFalse(Hallazgo.esSeveridadValida(""));
        }

        @Test
        @DisplayName("Obtener peso de severidad correctamente")
        void testPesoSeveridad() {
            assertEquals(5, Hallazgo.obtenerPesoSeveridad(Hallazgo.SEVERIDAD_CRITICAL));
            assertEquals(4, Hallazgo.obtenerPesoSeveridad(Hallazgo.SEVERIDAD_HIGH));
            assertEquals(3, Hallazgo.obtenerPesoSeveridad(Hallazgo.SEVERIDAD_MEDIUM));
            assertEquals(2, Hallazgo.obtenerPesoSeveridad(Hallazgo.SEVERIDAD_LOW));
            assertEquals(1, Hallazgo.obtenerPesoSeveridad(Hallazgo.SEVERIDAD_INFO));
            assertEquals(0, Hallazgo.obtenerPesoSeveridad("Unknown"));
            assertEquals(0, Hallazgo.obtenerPesoSeveridad(null));
        }

        @Test
        @DisplayName("Prioridad de severidad soporta etiquetas traducidas")
        void testPrioridadSeveridadTraducida() {
            assertEquals(5, Hallazgo.obtenerPrioridadSeveridad("Crítica"));
            assertEquals(4, Hallazgo.obtenerPrioridadSeveridad("Alta"));
            assertEquals(3, Hallazgo.obtenerPrioridadSeveridad("Media"));
            assertEquals(2, Hallazgo.obtenerPrioridadSeveridad("Baja"));
            assertEquals(1, Hallazgo.obtenerPrioridadSeveridad("Información"));
        }

        @Test
        @DisplayName("Prioridad de severidad es case-insensitive")
        void testPrioridadSeveridadCaseInsensitive() {
            assertEquals(5, Hallazgo.obtenerPrioridadSeveridad("CRITICAL"));
            assertEquals(5, Hallazgo.obtenerPrioridadSeveridad("critical"));
            assertEquals(4, Hallazgo.obtenerPrioridadSeveridad("HIGH"));
            assertEquals(4, Hallazgo.obtenerPrioridadSeveridad("high"));
            assertEquals(3, Hallazgo.obtenerPrioridadSeveridad("MEDIUM"));
            assertEquals(2, Hallazgo.obtenerPrioridadSeveridad("LOW"));
            assertEquals(1, Hallazgo.obtenerPrioridadSeveridad("INFO"));
        }

        @Test
        @DisplayName("Prioridad de severidad con partial match")
        void testPrioridadSeveridadPartialMatch() {
            assertEquals(5, Hallazgo.obtenerPrioridadSeveridad("critica vulnerabilidad"));
            assertEquals(4, Hallazgo.obtenerPrioridadSeveridad("alta severidad"));
            assertEquals(3, Hallazgo.obtenerPrioridadSeveridad("media prioridad"));
            assertEquals(2, Hallazgo.obtenerPrioridadSeveridad("baja urgencia"));
            assertEquals(1, Hallazgo.obtenerPrioridadSeveridad("informational"));
        }
    }

    @Nested
    @DisplayName("Validacion de confianza")
    class ValidacionConfianza {

        @Test
        @DisplayName("Validar confianzas validas")
        void testConfianzasValidas() {
            assertTrue(Hallazgo.esConfianzaValida(Hallazgo.CONFIANZA_ALTA));
            assertTrue(Hallazgo.esConfianzaValida(Hallazgo.CONFIANZA_MEDIA));
            assertTrue(Hallazgo.esConfianzaValida(Hallazgo.CONFIANZA_BAJA));
        }

        @Test
        @DisplayName("Rechazar confianzas invalidas")
        void testConfianzasInvalidas() {
            assertFalse(Hallazgo.esConfianzaValida("Alta"));
            assertFalse(Hallazgo.esConfianzaValida("alta"));
            assertFalse(Hallazgo.esConfianzaValida(null));
            assertFalse(Hallazgo.esConfianzaValida(""));
        }

        @Test
        @DisplayName("Prioridad de confianza soporta etiquetas traducidas")
        void testPrioridadConfianzaTraducida() {
            assertEquals(3, Hallazgo.obtenerPrioridadConfianza("Alta"));
            assertEquals(2, Hallazgo.obtenerPrioridadConfianza("Media"));
            assertEquals(1, Hallazgo.obtenerPrioridadConfianza("Baja"));
            assertEquals(0, Hallazgo.obtenerPrioridadConfianza("Desconocida"));
        }

        @Test
        @DisplayName("Prioridad de confianza es case-insensitive")
        void testPrioridadConfianzaCaseInsensitive() {
            assertEquals(3, Hallazgo.obtenerPrioridadConfianza("HIGH"));
            assertEquals(3, Hallazgo.obtenerPrioridadConfianza("high"));
            assertEquals(2, Hallazgo.obtenerPrioridadConfianza("MEDIUM"));
            assertEquals(2, Hallazgo.obtenerPrioridadConfianza("medium"));
            assertEquals(1, Hallazgo.obtenerPrioridadConfianza("LOW"));
            assertEquals(1, Hallazgo.obtenerPrioridadConfianza("low"));
        }

        @Test
        @DisplayName("Prioridad de confianza con partial match")
        void testPrioridadConfianzaPartialMatch() {
            assertEquals(3, Hallazgo.obtenerPrioridadConfianza("alta confianza"));
            assertEquals(2, Hallazgo.obtenerPrioridadConfianza("media certeza"));
            assertEquals(1, Hallazgo.obtenerPrioridadConfianza("baja probabilidad"));
            assertEquals(3, Hallazgo.obtenerPrioridadConfianza("certain"));
            assertEquals(2, Hallazgo.obtenerPrioridadConfianza("firm"));
            assertEquals(1, Hallazgo.obtenerPrioridadConfianza("tentative"));
        }
    }

    @Nested
    @DisplayName("Normalizacion")
    class Normalizacion {

        @Test
        @DisplayName("Normalizar severidad con valores validos")
        void testNormalizarSeveridadValidos() {
            assertEquals(Hallazgo.SEVERIDAD_CRITICAL, Hallazgo.normalizarSeveridad("Critical"));
            assertEquals(Hallazgo.SEVERIDAD_HIGH, Hallazgo.normalizarSeveridad("High"));
            assertEquals(Hallazgo.SEVERIDAD_MEDIUM, Hallazgo.normalizarSeveridad("Medium"));
            assertEquals(Hallazgo.SEVERIDAD_LOW, Hallazgo.normalizarSeveridad("Low"));
            assertEquals(Hallazgo.SEVERIDAD_INFO, Hallazgo.normalizarSeveridad("Info"));
        }

        @Test
        @DisplayName("Normalizar severidad con valores nulos o vacios devuelve Info")
        void testNormalizarSeveridadNulos() {
            assertEquals(Hallazgo.SEVERIDAD_INFO, Hallazgo.normalizarSeveridad(null));
            assertEquals(Hallazgo.SEVERIDAD_INFO, Hallazgo.normalizarSeveridad(""));
            assertEquals(Hallazgo.SEVERIDAD_INFO, Hallazgo.normalizarSeveridad("   "));
            assertEquals(Hallazgo.SEVERIDAD_INFO, Hallazgo.normalizarSeveridad("Unknown"));
        }

        @Test
        @DisplayName("Normalizar severidad con valores traducidos")
        void testNormalizarSeveridadTraducidos() {
            assertEquals(Hallazgo.SEVERIDAD_CRITICAL, Hallazgo.normalizarSeveridad("Crítica"));
            assertEquals(Hallazgo.SEVERIDAD_CRITICAL, Hallazgo.normalizarSeveridad("critica"));
            assertEquals(Hallazgo.SEVERIDAD_HIGH, Hallazgo.normalizarSeveridad("Alta"));
            assertEquals(Hallazgo.SEVERIDAD_HIGH, Hallazgo.normalizarSeveridad("severa"));
            assertEquals(Hallazgo.SEVERIDAD_MEDIUM, Hallazgo.normalizarSeveridad("Media"));
            assertEquals(Hallazgo.SEVERIDAD_MEDIUM, Hallazgo.normalizarSeveridad("moderada"));
            assertEquals(Hallazgo.SEVERIDAD_LOW, Hallazgo.normalizarSeveridad("Baja"));
            assertEquals(Hallazgo.SEVERIDAD_INFO, Hallazgo.normalizarSeveridad("Información"));
        }

        @Test
        @DisplayName("Normalizar confianza con valores validos")
        void testNormalizarConfianzaValidos() {
            assertEquals(Hallazgo.CONFIANZA_ALTA, Hallazgo.normalizarConfianza("High"));
            assertEquals(Hallazgo.CONFIANZA_MEDIA, Hallazgo.normalizarConfianza("Medium"));
            assertEquals(Hallazgo.CONFIANZA_BAJA, Hallazgo.normalizarConfianza("Low"));
        }

        @Test
        @DisplayName("Normalizar confianza con valores nulos o vacios devuelve Medium")
        void testNormalizarConfianzaNulos() {
            assertEquals(Hallazgo.CONFIANZA_MEDIA, Hallazgo.normalizarConfianza(null));
            assertEquals(Hallazgo.CONFIANZA_MEDIA, Hallazgo.normalizarConfianza(""));
            assertEquals(Hallazgo.CONFIANZA_MEDIA, Hallazgo.normalizarConfianza("   "));
            assertEquals(Hallazgo.CONFIANZA_MEDIA, Hallazgo.normalizarConfianza("Unknown"));
        }

        @Test
        @DisplayName("Normalizar confianza con valores traducidos")
        void testNormalizarConfianzaTraducidos() {
            assertEquals(Hallazgo.CONFIANZA_ALTA, Hallazgo.normalizarConfianza("Alta"));
            assertEquals(Hallazgo.CONFIANZA_ALTA, Hallazgo.normalizarConfianza("certain"));
            assertEquals(Hallazgo.CONFIANZA_MEDIA, Hallazgo.normalizarConfianza("Media"));
            assertEquals(Hallazgo.CONFIANZA_MEDIA, Hallazgo.normalizarConfianza("firm"));
            assertEquals(Hallazgo.CONFIANZA_BAJA, Hallazgo.normalizarConfianza("Baja"));
            assertEquals(Hallazgo.CONFIANZA_BAJA, Hallazgo.normalizarConfianza("tentative"));
        }
    }

    @Nested
    @DisplayName("Conversion a tabla")
    class ConversionTabla {

        @Test
        @DisplayName("Convertir a fila de tabla")
        void testAFilaTabla() {
            Hallazgo hallazgo = new Hallazgo(
                "https://example.com/test",
                "SQLi",
                "SQL Injection description",
                Hallazgo.SEVERIDAD_CRITICAL,
                Hallazgo.CONFIANZA_ALTA
            );

            Object[] fila = hallazgo.aFilaTabla();

            assertEquals(5, fila.length);
            assertNotNull(fila[0]); // hora
            assertEquals("https://example.com/test", fila[1]);
            assertEquals("SQLi", fila[2]);
            assertEquals(I18nUI.Hallazgos.TRADUCIR_SEVERIDAD(Hallazgo.SEVERIDAD_CRITICAL), fila[3]);
            assertEquals(I18nUI.Hallazgos.TRADUCIR_CONFIANZA(Hallazgo.CONFIANZA_ALTA), fila[4]);
        }

        @Test
        @DisplayName("Fila de tabla con valores nulos")
        void testAFilaTablaConNulos() {
            Hallazgo hallazgo = new Hallazgo(
                null,
                null,
                null,
                null,
                null
            );

            Object[] fila = hallazgo.aFilaTabla();

            assertEquals(5, fila.length);
            assertNotNull(fila[0]); // hora siempre tiene valor
            assertNull(fila[1]); // url
            assertNull(fila[2]); // titulo
            assertEquals(I18nUI.Hallazgos.TRADUCIR_SEVERIDAD(Hallazgo.SEVERIDAD_INFO), fila[3]);
            assertEquals(I18nUI.Hallazgos.TRADUCIR_CONFIANZA(Hallazgo.CONFIANZA_MEDIA), fila[4]);
        }
    }

    @Nested
    @DisplayName("Edicion de hallazgos")
    class Edicion {

        @Test
        @DisplayName("Editar mantiene evidencia y solicitud HTTP asociada")
        void testEditarMantieneEvidenciaYSolicitud() {
            HttpRequest solicitud = mock(HttpRequest.class);
            HttpRequestResponse evidencia = mock(HttpRequestResponse.class);

            Hallazgo original = new Hallazgo(
                "10:20:30",
                "https://example.com/original",
                "Titulo original",
                "Descripcion original",
                Hallazgo.SEVERIDAD_LOW,
                Hallazgo.CONFIANZA_MEDIA,
                solicitud,
                evidencia
            );

            Hallazgo editado = original.editar(
                "https://example.com/editado",
                "Titulo editado",
                "Descripcion editada",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA
            );

            // Verificar que mantiene los datos inmutables
            assertEquals("10:20:30", editado.obtenerHoraDescubrimiento());
            assertSame(solicitud, editado.obtenerSolicitudHttp());
            assertSame(evidencia, editado.obtenerEvidenciaHttp());

            // Verificar que actualiza los datos editables
            assertEquals("https://example.com/editado", editado.obtenerUrl());
            assertEquals("Titulo editado", editado.obtenerTitulo());
            assertEquals("Descripcion editada", editado.obtenerHallazgo());
            assertEquals(Hallazgo.SEVERIDAD_HIGH, editado.obtenerSeveridad());
            assertEquals(Hallazgo.CONFIANZA_ALTA, editado.obtenerConfianza());
        }

        @Test
        @DisplayName("Editar crea nuevo objeto (inmutabilidad)")
        void testEditarCreaNuevoObjeto() {
            Hallazgo original = new Hallazgo(
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_LOW,
                Hallazgo.CONFIANZA_BAJA
            );

            Hallazgo editado = original.editar(
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_LOW,
                Hallazgo.CONFIANZA_BAJA
            );

            assertNotSame(original, editado);
        }

        @Test
        @DisplayName("Editar con parametros nulos")
        void testEditarConParametrosNulos() {
            HttpRequest solicitud = mock(HttpRequest.class);

            Hallazgo original = new Hallazgo(
                "10:20:30",
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA,
                solicitud,
                null
            );

            Hallazgo editado = original.editar(
                null,
                null,
                null,
                null,
                null
            );

            assertNull(editado.obtenerUrl());
            assertNull(editado.obtenerTitulo());
            assertNull(editado.obtenerHallazgo());
            // Severidad y confianza se normalizan a valores por defecto
            assertEquals(Hallazgo.SEVERIDAD_INFO, editado.obtenerSeveridad());
            assertEquals(Hallazgo.CONFIANZA_MEDIA, editado.obtenerConfianza());
            // Mantiene solicitud original
            assertSame(solicitud, editado.obtenerSolicitudHttp());
        }
    }

    @Nested
    @DisplayName("Gestion de evidencia")
    class GestionEvidencia {

        @Test
        @DisplayName("conEvidenciaHttp con nueva evidencia crea nuevo hallazgo")
        void testConEvidenciaHttpNueva() {
            Hallazgo original = new Hallazgo(
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA
            );

            HttpRequestResponse evidencia = mock(HttpRequestResponse.class);
            Hallazgo conEvidencia = original.conEvidenciaHttp(evidencia);

            assertNotSame(original, conEvidencia);
            assertSame(evidencia, conEvidencia.obtenerEvidenciaHttp());
        }

        @Test
        @DisplayName("conEvidenciaHttp con null devuelve mismo objeto")
        void testConEvidenciaHttpNull() {
            Hallazgo original = new Hallazgo(
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA
            );

            Hallazgo resultado = original.conEvidenciaHttp(null);

            assertSame(original, resultado);
        }

        @Test
        @DisplayName("conEvidenciaHttp con misma evidencia devuelve mismo objeto")
        void testConEvidenciaHttpMisma() {
            HttpRequestResponse evidencia = mock(HttpRequestResponse.class);

            Hallazgo original = new Hallazgo(
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA,
                null,
                evidencia
            );

            Hallazgo resultado = original.conEvidenciaHttp(evidencia);

            assertSame(original, resultado);
        }

        @Test
        @DisplayName("conEvidenciaId con nuevo ID crea nuevo hallazgo")
        void testConEvidenciaIdNuevo() {
            Hallazgo original = new Hallazgo(
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA
            );

            Hallazgo conEvidenciaId = original.conEvidenciaId("evidencia-456");

            assertNotSame(original, conEvidenciaId);
            assertEquals("evidencia-456", conEvidenciaId.obtenerEvidenciaId());
        }

        @Test
        @DisplayName("conEvidenciaId con null devuelve mismo objeto")
        void testConEvidenciaIdNull() {
            Hallazgo original = new Hallazgo(
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA
            );

            Hallazgo resultado = original.conEvidenciaId(null);

            assertSame(original, resultado);
        }

        @Test
        @DisplayName("conEvidenciaId con vacio devuelve mismo objeto")
        void testConEvidenciaIdVacio() {
            Hallazgo original = new Hallazgo(
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA
            );

            Hallazgo resultado = original.conEvidenciaId("");

            assertSame(original, resultado);
        }

        @Test
        @DisplayName("conEvidenciaId con mismo ID devuelve mismo objeto")
        void testConEvidenciaIdMismo() {
            Hallazgo original = new Hallazgo(
                "10:20:30",
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA,
                null,
                null,
                "evidencia-123"
            );

            Hallazgo resultado = original.conEvidenciaId("evidencia-123");

            assertSame(original, resultado);
        }

        @Test
        @DisplayName("obtenerEvidenciaHttp resuelve lazy con resolutor")
        void testObtenerEvidenciaHttpLazyResolution() {
            HttpRequestResponse evidenciaResuelta = mock(HttpRequestResponse.class);

            // Configurar resolutor
            Hallazgo.ResolutorEvidencia resolutor = id -> {
                if ("evidencia-789".equals(id)) {
                    return evidenciaResuelta;
                }
                return null;
            };
            Hallazgo.establecerResolutorEvidencia(resolutor);

            try {
                Hallazgo hallazgo = new Hallazgo(
                    "10:20:30",
                    "https://example.com/test",
                    "Titulo",
                    "Descripcion",
                    Hallazgo.SEVERIDAD_HIGH,
                    Hallazgo.CONFIANZA_ALTA,
                    null,
                    null,
                    "evidencia-789"
                );

                // Primera llamada resuelve lazy
                HttpRequestResponse resultado = hallazgo.obtenerEvidenciaHttp();
                assertSame(evidenciaResuelta, resultado);

                // Segunda llamada usa cache
                HttpRequestResponse resultadoCacheado = hallazgo.obtenerEvidenciaHttp();
                assertSame(evidenciaResuelta, resultadoCacheado);
            } finally {
                // Limpiar resolutor
                Hallazgo.establecerResolutorEvidencia(null);
            }
        }

        @Test
        @DisplayName("obtenerEvidenciaHttp devuelve null sin resolutor")
        void testObtenerEvidenciaHttpSinResolutor() {
            // Asegurar que no hay resolutor
            Hallazgo.establecerResolutorEvidencia(null);

            Hallazgo hallazgo = new Hallazgo(
                "10:20:30",
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA,
                null,
                null,
                "evidencia-123"
            );

            assertNull(hallazgo.obtenerEvidenciaHttp());
        }

        @Test
        @DisplayName("obtenerEvidenciaHttp maneja excepcion del resolutor")
        void testObtenerEvidenciaHttpConExcepcion() {
            // Configurar resolutor que lanza excepcion
            Hallazgo.ResolutorEvidencia resolutor = id -> {
                throw new RuntimeException("Error simulado");
            };
            Hallazgo.establecerResolutorEvidencia(resolutor);

            try {
                Hallazgo hallazgo = new Hallazgo(
                    "10:20:30",
                    "https://example.com/test",
                    "Titulo",
                    "Descripcion",
                    Hallazgo.SEVERIDAD_HIGH,
                    Hallazgo.CONFIANZA_ALTA,
                    null,
                    null,
                    "evidencia-error"
                );

                // No debe lanzar excepcion, debe devolver null
                assertNull(hallazgo.obtenerEvidenciaHttp());
            } finally {
                // Limpiar resolutor
                Hallazgo.establecerResolutorEvidencia(null);
            }
        }

        @Test
        @DisplayName("obtenerEvidenciaHttp devuelve evidencia directa si existe")
        void testObtenerEvidenciaHttpDirecta() {
            HttpRequestResponse evidencia = mock(HttpRequestResponse.class);

            Hallazgo hallazgo = new Hallazgo(
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA,
                null,
                evidencia
            );

            assertSame(evidencia, hallazgo.obtenerEvidenciaHttp());
        }
    }

    @Nested
    @DisplayName("Equals y HashCode")
    class EqualsHashCode {

        @Test
        @DisplayName("Equals devuelve true para mismo objeto")
        void testEqualsMismoObjeto() {
            Hallazgo hallazgo = new Hallazgo(
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA
            );

            assertEquals(hallazgo, hallazgo);
        }

        @Test
        @DisplayName("Equals devuelve false para null")
        void testEqualsNull() {
            Hallazgo hallazgo = new Hallazgo(
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA
            );

            assertNotEquals(null, hallazgo);
        }

        @Test
        @DisplayName("Equals devuelve false para otra clase")
        void testEqualsOtraClase() {
            Hallazgo hallazgo = new Hallazgo(
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA
            );

            assertNotEquals("string", hallazgo);
            assertNotEquals(123, hallazgo);
        }

        @Test
        @DisplayName("Equals compara todos los campos relevantes")
        void testEqualsCampos() {
            Hallazgo hallazgo1 = new Hallazgo(
                "10:20:30",
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA,
                null,
                null,
                "evidencia-123"
            );

            Hallazgo hallazgo2 = new Hallazgo(
                "10:20:30",
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA,
                null,
                null,
                "evidencia-123"
            );

            assertEquals(hallazgo1, hallazgo2);
            assertEquals(hallazgo1.hashCode(), hallazgo2.hashCode());
        }

        @Test
        @DisplayName("Equals devuelve false con hora diferente")
        void testEqualsHoraDiferente() {
            Hallazgo hallazgo1 = new Hallazgo(
                "10:20:30",
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA,
                (HttpRequest) null
            );

            Hallazgo hallazgo2 = new Hallazgo(
                "10:20:31",
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA,
                (HttpRequest) null
            );

            assertNotEquals(hallazgo1, hallazgo2);
        }

        @Test
        @DisplayName("Equals devuelve false con URL diferente")
        void testEqualsUrlDiferente() {
            Hallazgo hallazgo1 = new Hallazgo(
                "https://example.com/test1",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA
            );

            Hallazgo hallazgo2 = new Hallazgo(
                "https://example.com/test2",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA
            );

            assertNotEquals(hallazgo1, hallazgo2);
        }

        @Test
        @DisplayName("Equals no compara solicitud HTTP")
        void testEqualsNoComparaSolicitud() {
            HttpRequest solicitud1 = mock(HttpRequest.class);
            HttpRequest solicitud2 = mock(HttpRequest.class);

            Hallazgo hallazgo1 = new Hallazgo(
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA,
                solicitud1
            );

            Hallazgo hallazgo2 = new Hallazgo(
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA,
                solicitud2
            );

            // Deben ser iguales aunque las solicitudes sean diferentes
            assertEquals(hallazgo1, hallazgo2);
        }

        @Test
        @DisplayName("HashCode consistente con equals")
        void testHashCodeConsistente() {
            Hallazgo hallazgo1 = new Hallazgo(
                "10:20:30",
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA,
                null,
                null,
                "evidencia-123"
            );

            Hallazgo hallazgo2 = new Hallazgo(
                "10:20:30",
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA,
                null,
                null,
                "evidencia-123"
            );

            assertEquals(hallazgo1.hashCode(), hallazgo2.hashCode());
        }

        @Test
        @DisplayName("HashCode estable entre llamadas")
        void testHashCodeEstable() {
            Hallazgo hallazgo = new Hallazgo(
                "https://example.com/test",
                "Titulo",
                "Descripcion",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA
            );

            int hash1 = hallazgo.hashCode();
            int hash2 = hallazgo.hashCode();
            int hash3 = hallazgo.hashCode();

            assertEquals(hash1, hash2);
            assertEquals(hash2, hash3);
        }
    }

    @Nested
    @DisplayName("ToString")
    class ToString {

        @Test
        @DisplayName("ToString contiene campos principales")
        void testToStringContieneCampos() {
            Hallazgo hallazgo = new Hallazgo(
                "10:20:30",
                "https://example.com/test",
                "XSS",
                "Cross-Site Scripting",
                Hallazgo.SEVERIDAD_HIGH,
                Hallazgo.CONFIANZA_ALTA,
                (HttpRequest) null
            );

            String resultado = hallazgo.toString();

            assertTrue(resultado.contains("10:20:30"));
            assertTrue(resultado.contains("https://example.com/test"));
            assertTrue(resultado.contains("XSS"));
            assertTrue(resultado.contains(Hallazgo.SEVERIDAD_HIGH));
            assertTrue(resultado.contains(Hallazgo.CONFIANZA_ALTA));
        }

        @Test
        @DisplayName("ToString con valores nulos")
        void testToStringConNulos() {
            Hallazgo hallazgo = new Hallazgo(
                null,
                null,
                null,
                null,
                null,
                null
            );

            String resultado = hallazgo.toString();

            assertNotNull(resultado);
            assertTrue(resultado.contains("Hallazgo"));
        }
    }

    @Nested
    @DisplayName("Constantes")
    class Constantes {

        @Test
        @DisplayName("Constantes de severidad tienen valores correctos")
        void testConstantesSeveridad() {
            assertEquals("Critical", Hallazgo.SEVERIDAD_CRITICAL);
            assertEquals("High", Hallazgo.SEVERIDAD_HIGH);
            assertEquals("Medium", Hallazgo.SEVERIDAD_MEDIUM);
            assertEquals("Low", Hallazgo.SEVERIDAD_LOW);
            assertEquals("Info", Hallazgo.SEVERIDAD_INFO);
        }

        @Test
        @DisplayName("Constantes de confianza tienen valores correctos")
        void testConstantesConfianza() {
            assertEquals("High", Hallazgo.CONFIANZA_ALTA);
            assertEquals("Medium", Hallazgo.CONFIANZA_MEDIA);
            assertEquals("Low", Hallazgo.CONFIANZA_BAJA);
        }
    }
}
