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

            assertNotNull(hallazgo, "assertNotNull failed at HallazgoTest.java:33");
            assertEquals("https://example.com/test", hallazgo.obtenerUrl(), "assertEquals failed at HallazgoTest.java:34");
            assertEquals("XSS", hallazgo.obtenerTitulo(), "assertEquals failed at HallazgoTest.java:35");
            assertEquals("XSS detectado", hallazgo.obtenerHallazgo(), "assertEquals failed at HallazgoTest.java:36");
            assertEquals("High", hallazgo.obtenerSeveridad(), "assertEquals failed at HallazgoTest.java:37");
            assertEquals("Medium", hallazgo.obtenerConfianza(), "assertEquals failed at HallazgoTest.java:38");
            assertNull(hallazgo.obtenerSolicitudHttp(), "assertNull failed at HallazgoTest.java:39");
            assertNull(hallazgo.obtenerEvidenciaHttp(), "assertNull failed at HallazgoTest.java:40");
            assertNull(hallazgo.obtenerEvidenciaId(), "assertNull failed at HallazgoTest.java:41");
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

            assertNotNull(hallazgo, "assertNotNull failed at HallazgoTest.java:57");
            assertSame(solicitud, hallazgo.obtenerSolicitudHttp(), "assertSame failed at HallazgoTest.java:58");
            assertNull(hallazgo.obtenerEvidenciaHttp(), "assertNull failed at HallazgoTest.java:59");
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

            assertEquals("10:20:30", hallazgo.obtenerHoraDescubrimiento(), "assertEquals failed at HallazgoTest.java:75");
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

            assertEquals("10:20:30", hallazgo.obtenerHoraDescubrimiento(), "assertEquals failed at HallazgoTest.java:95");
            assertEquals("https://example.com/test", hallazgo.obtenerUrl(), "assertEquals failed at HallazgoTest.java:96");
            assertEquals("SSRF", hallazgo.obtenerTitulo(), "assertEquals failed at HallazgoTest.java:97");
            assertEquals("Server-Side Request Forgery", hallazgo.obtenerHallazgo(), "assertEquals failed at HallazgoTest.java:98");
            assertEquals(Hallazgo.SEVERIDAD_HIGH, hallazgo.obtenerSeveridad(), "assertEquals failed at HallazgoTest.java:99");
            assertEquals(Hallazgo.CONFIANZA_ALTA, hallazgo.obtenerConfianza(), "assertEquals failed at HallazgoTest.java:100");
            assertSame(solicitud, hallazgo.obtenerSolicitudHttp(), "assertSame failed at HallazgoTest.java:101");
            assertSame(evidencia, hallazgo.obtenerEvidenciaHttp(), "assertSame failed at HallazgoTest.java:102");
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

            assertEquals("evidencia-123", hallazgo.obtenerEvidenciaId(), "assertEquals failed at HallazgoTest.java:122");
            assertNull(hallazgo.obtenerEvidenciaHttp(), "assertNull failed at HallazgoTest.java:123");
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

            assertNull(hallazgo.obtenerUrl(), "assertNull failed at HallazgoTest.java:137");
            assertNull(hallazgo.obtenerTitulo(), "assertNull failed at HallazgoTest.java:138");
            assertNull(hallazgo.obtenerHallazgo(), "assertNull failed at HallazgoTest.java:139");
            assertEquals(Hallazgo.SEVERIDAD_INFO, hallazgo.obtenerSeveridad(), "assertEquals failed at HallazgoTest.java:140"); // Valor por defecto
            assertEquals(Hallazgo.CONFIANZA_MEDIA, hallazgo.obtenerConfianza(), "assertEquals failed at HallazgoTest.java:141"); // Valor por defecto
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

            assertEquals("", hallazgo.obtenerUrl(), "assertEquals failed at HallazgoTest.java:155");
            assertEquals("", hallazgo.obtenerTitulo(), "assertEquals failed at HallazgoTest.java:156");
            assertEquals("", hallazgo.obtenerHallazgo(), "assertEquals failed at HallazgoTest.java:157");
            assertEquals(Hallazgo.SEVERIDAD_INFO, hallazgo.obtenerSeveridad(), "assertEquals failed at HallazgoTest.java:158"); // Valor por defecto
            assertEquals(Hallazgo.CONFIANZA_MEDIA, hallazgo.obtenerConfianza(), "assertEquals failed at HallazgoTest.java:159"); // Valor por defecto
        }
    }

    @Nested
    @DisplayName("Validacion de severidad")
    class ValidacionSeveridad {

        @Test
        @DisplayName("Validar severidades validas")
        void testSeveridadesValidas() {
            assertTrue(Hallazgo.esSeveridadValida(Hallazgo.SEVERIDAD_CRITICAL), "assertTrue failed at HallazgoTest.java:170");
            assertTrue(Hallazgo.esSeveridadValida(Hallazgo.SEVERIDAD_HIGH), "assertTrue failed at HallazgoTest.java:171");
            assertTrue(Hallazgo.esSeveridadValida(Hallazgo.SEVERIDAD_MEDIUM), "assertTrue failed at HallazgoTest.java:172");
            assertTrue(Hallazgo.esSeveridadValida(Hallazgo.SEVERIDAD_LOW), "assertTrue failed at HallazgoTest.java:173");
            assertTrue(Hallazgo.esSeveridadValida(Hallazgo.SEVERIDAD_INFO), "assertTrue failed at HallazgoTest.java:174");
        }

        @Test
        @DisplayName("Rechazar severidades invalidas")
        void testSeveridadesInvalidas() {
            assertFalse(Hallazgo.esSeveridadValida("Alta"), "assertFalse failed at HallazgoTest.java:180");
            assertFalse(Hallazgo.esSeveridadValida("alta"), "assertFalse failed at HallazgoTest.java:181");
            assertFalse(Hallazgo.esSeveridadValida("CRITICAL"), "assertFalse failed at HallazgoTest.java:182");
            assertFalse(Hallazgo.esSeveridadValida(null), "assertFalse failed at HallazgoTest.java:183");
            assertFalse(Hallazgo.esSeveridadValida(""), "assertFalse failed at HallazgoTest.java:184");
        }

        @Test
        @DisplayName("Obtener peso de severidad correctamente")
        void testPesoSeveridad() {
            assertEquals(5, Hallazgo.obtenerPesoSeveridad(Hallazgo.SEVERIDAD_CRITICAL), "assertEquals failed at HallazgoTest.java:190");
            assertEquals(4, Hallazgo.obtenerPesoSeveridad(Hallazgo.SEVERIDAD_HIGH), "assertEquals failed at HallazgoTest.java:191");
            assertEquals(3, Hallazgo.obtenerPesoSeveridad(Hallazgo.SEVERIDAD_MEDIUM), "assertEquals failed at HallazgoTest.java:192");
            assertEquals(2, Hallazgo.obtenerPesoSeveridad(Hallazgo.SEVERIDAD_LOW), "assertEquals failed at HallazgoTest.java:193");
            assertEquals(1, Hallazgo.obtenerPesoSeveridad(Hallazgo.SEVERIDAD_INFO), "assertEquals failed at HallazgoTest.java:194");
            assertEquals(0, Hallazgo.obtenerPesoSeveridad("Unknown"), "assertEquals failed at HallazgoTest.java:195");
            assertEquals(0, Hallazgo.obtenerPesoSeveridad(null), "assertEquals failed at HallazgoTest.java:196");
        }

        @Test
        @DisplayName("Prioridad de severidad soporta etiquetas traducidas")
        void testPrioridadSeveridadTraducida() {
            assertEquals(5, Hallazgo.obtenerPrioridadSeveridad("Crítica"), "assertEquals failed at HallazgoTest.java:202");
            assertEquals(4, Hallazgo.obtenerPrioridadSeveridad("Alta"), "assertEquals failed at HallazgoTest.java:203");
            assertEquals(3, Hallazgo.obtenerPrioridadSeveridad("Media"), "assertEquals failed at HallazgoTest.java:204");
            assertEquals(2, Hallazgo.obtenerPrioridadSeveridad("Baja"), "assertEquals failed at HallazgoTest.java:205");
            assertEquals(1, Hallazgo.obtenerPrioridadSeveridad("Información"), "assertEquals failed at HallazgoTest.java:206");
        }

        @Test
        @DisplayName("Prioridad de severidad es case-insensitive")
        void testPrioridadSeveridadCaseInsensitive() {
            assertEquals(5, Hallazgo.obtenerPrioridadSeveridad("CRITICAL"), "assertEquals failed at HallazgoTest.java:212");
            assertEquals(5, Hallazgo.obtenerPrioridadSeveridad("critical"), "assertEquals failed at HallazgoTest.java:213");
            assertEquals(4, Hallazgo.obtenerPrioridadSeveridad("HIGH"), "assertEquals failed at HallazgoTest.java:214");
            assertEquals(4, Hallazgo.obtenerPrioridadSeveridad("high"), "assertEquals failed at HallazgoTest.java:215");
            assertEquals(3, Hallazgo.obtenerPrioridadSeveridad("MEDIUM"), "assertEquals failed at HallazgoTest.java:216");
            assertEquals(2, Hallazgo.obtenerPrioridadSeveridad("LOW"), "assertEquals failed at HallazgoTest.java:217");
            assertEquals(1, Hallazgo.obtenerPrioridadSeveridad("INFO"), "assertEquals failed at HallazgoTest.java:218");
        }

        @Test
        @DisplayName("Prioridad de severidad con partial match")
        void testPrioridadSeveridadPartialMatch() {
            assertEquals(5, Hallazgo.obtenerPrioridadSeveridad("critica vulnerabilidad"), "assertEquals failed at HallazgoTest.java:224");
            assertEquals(4, Hallazgo.obtenerPrioridadSeveridad("alta severidad"), "assertEquals failed at HallazgoTest.java:225");
            assertEquals(3, Hallazgo.obtenerPrioridadSeveridad("media prioridad"), "assertEquals failed at HallazgoTest.java:226");
            assertEquals(2, Hallazgo.obtenerPrioridadSeveridad("baja urgencia"), "assertEquals failed at HallazgoTest.java:227");
            assertEquals(1, Hallazgo.obtenerPrioridadSeveridad("informational"), "assertEquals failed at HallazgoTest.java:228");
        }
    }

    @Nested
    @DisplayName("Validacion de confianza")
    class ValidacionConfianza {

        @Test
        @DisplayName("Validar confianzas validas")
        void testConfianzasValidas() {
            assertTrue(Hallazgo.esConfianzaValida(Hallazgo.CONFIANZA_ALTA), "assertTrue failed at HallazgoTest.java:239");
            assertTrue(Hallazgo.esConfianzaValida(Hallazgo.CONFIANZA_MEDIA), "assertTrue failed at HallazgoTest.java:240");
            assertTrue(Hallazgo.esConfianzaValida(Hallazgo.CONFIANZA_BAJA), "assertTrue failed at HallazgoTest.java:241");
        }

        @Test
        @DisplayName("Rechazar confianzas invalidas")
        void testConfianzasInvalidas() {
            assertFalse(Hallazgo.esConfianzaValida("Alta"), "assertFalse failed at HallazgoTest.java:247");
            assertFalse(Hallazgo.esConfianzaValida("alta"), "assertFalse failed at HallazgoTest.java:248");
            assertFalse(Hallazgo.esConfianzaValida(null), "assertFalse failed at HallazgoTest.java:249");
            assertFalse(Hallazgo.esConfianzaValida(""), "assertFalse failed at HallazgoTest.java:250");
        }

        @Test
        @DisplayName("Prioridad de confianza soporta etiquetas traducidas")
        void testPrioridadConfianzaTraducida() {
            assertEquals(3, Hallazgo.obtenerPrioridadConfianza("Alta"), "assertEquals failed at HallazgoTest.java:256");
            assertEquals(2, Hallazgo.obtenerPrioridadConfianza("Media"), "assertEquals failed at HallazgoTest.java:257");
            assertEquals(1, Hallazgo.obtenerPrioridadConfianza("Baja"), "assertEquals failed at HallazgoTest.java:258");
            assertEquals(0, Hallazgo.obtenerPrioridadConfianza("Desconocida"), "assertEquals failed at HallazgoTest.java:259");
        }

        @Test
        @DisplayName("Prioridad de confianza es case-insensitive")
        void testPrioridadConfianzaCaseInsensitive() {
            assertEquals(3, Hallazgo.obtenerPrioridadConfianza("HIGH"), "assertEquals failed at HallazgoTest.java:265");
            assertEquals(3, Hallazgo.obtenerPrioridadConfianza("high"), "assertEquals failed at HallazgoTest.java:266");
            assertEquals(2, Hallazgo.obtenerPrioridadConfianza("MEDIUM"), "assertEquals failed at HallazgoTest.java:267");
            assertEquals(2, Hallazgo.obtenerPrioridadConfianza("medium"), "assertEquals failed at HallazgoTest.java:268");
            assertEquals(1, Hallazgo.obtenerPrioridadConfianza("LOW"), "assertEquals failed at HallazgoTest.java:269");
            assertEquals(1, Hallazgo.obtenerPrioridadConfianza("low"), "assertEquals failed at HallazgoTest.java:270");
        }

        @Test
        @DisplayName("Prioridad de confianza con partial match")
        void testPrioridadConfianzaPartialMatch() {
            assertEquals(3, Hallazgo.obtenerPrioridadConfianza("alta confianza"), "assertEquals failed at HallazgoTest.java:276");
            assertEquals(2, Hallazgo.obtenerPrioridadConfianza("media certeza"), "assertEquals failed at HallazgoTest.java:277");
            assertEquals(1, Hallazgo.obtenerPrioridadConfianza("baja probabilidad"), "assertEquals failed at HallazgoTest.java:278");
            assertEquals(3, Hallazgo.obtenerPrioridadConfianza("certain"), "assertEquals failed at HallazgoTest.java:279");
            assertEquals(2, Hallazgo.obtenerPrioridadConfianza("firm"), "assertEquals failed at HallazgoTest.java:280");
            assertEquals(1, Hallazgo.obtenerPrioridadConfianza("tentative"), "assertEquals failed at HallazgoTest.java:281");
        }
    }

    @Nested
    @DisplayName("Normalizacion")
    class Normalizacion {

        @Test
        @DisplayName("Normalizar severidad con valores validos")
        void testNormalizarSeveridadValidos() {
            assertEquals(Hallazgo.SEVERIDAD_CRITICAL, Hallazgo.normalizarSeveridad("Critical"), "assertEquals failed at HallazgoTest.java:292");
            assertEquals(Hallazgo.SEVERIDAD_HIGH, Hallazgo.normalizarSeveridad("High"), "assertEquals failed at HallazgoTest.java:293");
            assertEquals(Hallazgo.SEVERIDAD_MEDIUM, Hallazgo.normalizarSeveridad("Medium"), "assertEquals failed at HallazgoTest.java:294");
            assertEquals(Hallazgo.SEVERIDAD_LOW, Hallazgo.normalizarSeveridad("Low"), "assertEquals failed at HallazgoTest.java:295");
            assertEquals(Hallazgo.SEVERIDAD_INFO, Hallazgo.normalizarSeveridad("Info"), "assertEquals failed at HallazgoTest.java:296");
        }

        @Test
        @DisplayName("Normalizar severidad con valores nulos o vacios devuelve Info")
        void testNormalizarSeveridadNulos() {
            assertEquals(Hallazgo.SEVERIDAD_INFO, Hallazgo.normalizarSeveridad(null), "assertEquals failed at HallazgoTest.java:302");
            assertEquals(Hallazgo.SEVERIDAD_INFO, Hallazgo.normalizarSeveridad(""), "assertEquals failed at HallazgoTest.java:303");
            assertEquals(Hallazgo.SEVERIDAD_INFO, Hallazgo.normalizarSeveridad("   "), "assertEquals failed at HallazgoTest.java:304");
            assertEquals(Hallazgo.SEVERIDAD_INFO, Hallazgo.normalizarSeveridad("Unknown"), "assertEquals failed at HallazgoTest.java:305");
        }

        @Test
        @DisplayName("Normalizar severidad con valores traducidos")
        void testNormalizarSeveridadTraducidos() {
            assertEquals(Hallazgo.SEVERIDAD_CRITICAL, Hallazgo.normalizarSeveridad("Crítica"), "assertEquals failed at HallazgoTest.java:311");
            assertEquals(Hallazgo.SEVERIDAD_CRITICAL, Hallazgo.normalizarSeveridad("critica"), "assertEquals failed at HallazgoTest.java:312");
            assertEquals(Hallazgo.SEVERIDAD_HIGH, Hallazgo.normalizarSeveridad("Alta"), "assertEquals failed at HallazgoTest.java:313");
            assertEquals(Hallazgo.SEVERIDAD_HIGH, Hallazgo.normalizarSeveridad("severa"), "assertEquals failed at HallazgoTest.java:314");
            assertEquals(Hallazgo.SEVERIDAD_MEDIUM, Hallazgo.normalizarSeveridad("Media"), "assertEquals failed at HallazgoTest.java:315");
            assertEquals(Hallazgo.SEVERIDAD_MEDIUM, Hallazgo.normalizarSeveridad("moderada"), "assertEquals failed at HallazgoTest.java:316");
            assertEquals(Hallazgo.SEVERIDAD_LOW, Hallazgo.normalizarSeveridad("Baja"), "assertEquals failed at HallazgoTest.java:317");
            assertEquals(Hallazgo.SEVERIDAD_INFO, Hallazgo.normalizarSeveridad("Información"), "assertEquals failed at HallazgoTest.java:318");
        }

        @Test
        @DisplayName("Normalizar confianza con valores validos")
        void testNormalizarConfianzaValidos() {
            assertEquals(Hallazgo.CONFIANZA_ALTA, Hallazgo.normalizarConfianza("High"), "assertEquals failed at HallazgoTest.java:324");
            assertEquals(Hallazgo.CONFIANZA_MEDIA, Hallazgo.normalizarConfianza("Medium"), "assertEquals failed at HallazgoTest.java:325");
            assertEquals(Hallazgo.CONFIANZA_BAJA, Hallazgo.normalizarConfianza("Low"), "assertEquals failed at HallazgoTest.java:326");
        }

        @Test
        @DisplayName("Normalizar confianza con valores nulos o vacios devuelve Medium")
        void testNormalizarConfianzaNulos() {
            assertEquals(Hallazgo.CONFIANZA_MEDIA, Hallazgo.normalizarConfianza(null), "assertEquals failed at HallazgoTest.java:332");
            assertEquals(Hallazgo.CONFIANZA_MEDIA, Hallazgo.normalizarConfianza(""), "assertEquals failed at HallazgoTest.java:333");
            assertEquals(Hallazgo.CONFIANZA_MEDIA, Hallazgo.normalizarConfianza("   "), "assertEquals failed at HallazgoTest.java:334");
            assertEquals(Hallazgo.CONFIANZA_MEDIA, Hallazgo.normalizarConfianza("Unknown"), "assertEquals failed at HallazgoTest.java:335");
        }

        @Test
        @DisplayName("Normalizar confianza con valores traducidos")
        void testNormalizarConfianzaTraducidos() {
            assertEquals(Hallazgo.CONFIANZA_ALTA, Hallazgo.normalizarConfianza("Alta"), "assertEquals failed at HallazgoTest.java:341");
            assertEquals(Hallazgo.CONFIANZA_ALTA, Hallazgo.normalizarConfianza("certain"), "assertEquals failed at HallazgoTest.java:342");
            assertEquals(Hallazgo.CONFIANZA_MEDIA, Hallazgo.normalizarConfianza("Media"), "assertEquals failed at HallazgoTest.java:343");
            assertEquals(Hallazgo.CONFIANZA_MEDIA, Hallazgo.normalizarConfianza("firm"), "assertEquals failed at HallazgoTest.java:344");
            assertEquals(Hallazgo.CONFIANZA_BAJA, Hallazgo.normalizarConfianza("Baja"), "assertEquals failed at HallazgoTest.java:345");
            assertEquals(Hallazgo.CONFIANZA_BAJA, Hallazgo.normalizarConfianza("tentative"), "assertEquals failed at HallazgoTest.java:346");
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

            assertEquals(5, fila.length, "assertEquals failed at HallazgoTest.java:367");
            assertNotNull(fila[0], "assertNotNull failed at HallazgoTest.java:368"); // hora
            assertEquals("https://example.com/test", fila[1], "assertEquals failed at HallazgoTest.java:369");
            assertEquals("SQLi", fila[2], "assertEquals failed at HallazgoTest.java:370");
            assertEquals(I18nUI.Hallazgos.TRADUCIR_SEVERIDAD(Hallazgo.SEVERIDAD_CRITICAL), fila[3], "assertEquals failed at HallazgoTest.java:371");
            assertEquals(I18nUI.Hallazgos.TRADUCIR_CONFIANZA(Hallazgo.CONFIANZA_ALTA), fila[4], "assertEquals failed at HallazgoTest.java:372");
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

            assertEquals(5, fila.length, "assertEquals failed at HallazgoTest.java:388");
            assertNotNull(fila[0], "assertNotNull failed at HallazgoTest.java:389"); // hora siempre tiene valor
            assertNull(fila[1], "assertNull failed at HallazgoTest.java:390"); // url
            assertNull(fila[2], "assertNull failed at HallazgoTest.java:391"); // titulo
            assertEquals(I18nUI.Hallazgos.TRADUCIR_SEVERIDAD(Hallazgo.SEVERIDAD_INFO), fila[3], "assertEquals failed at HallazgoTest.java:392");
            assertEquals(I18nUI.Hallazgos.TRADUCIR_CONFIANZA(Hallazgo.CONFIANZA_MEDIA), fila[4], "assertEquals failed at HallazgoTest.java:393");
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
            assertEquals("10:20:30", editado.obtenerHoraDescubrimiento(), "assertEquals failed at HallazgoTest.java:427");
            assertSame(solicitud, editado.obtenerSolicitudHttp(), "assertSame failed at HallazgoTest.java:428");
            assertSame(evidencia, editado.obtenerEvidenciaHttp(), "assertSame failed at HallazgoTest.java:429");

            // Verificar que actualiza los datos editables
            assertEquals("https://example.com/editado", editado.obtenerUrl(), "assertEquals failed at HallazgoTest.java:432");
            assertEquals("Titulo editado", editado.obtenerTitulo(), "assertEquals failed at HallazgoTest.java:433");
            assertEquals("Descripcion editada", editado.obtenerHallazgo(), "assertEquals failed at HallazgoTest.java:434");
            assertEquals(Hallazgo.SEVERIDAD_HIGH, editado.obtenerSeveridad(), "assertEquals failed at HallazgoTest.java:435");
            assertEquals(Hallazgo.CONFIANZA_ALTA, editado.obtenerConfianza(), "assertEquals failed at HallazgoTest.java:436");
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

            assertNotSame(original, editado, "assertNotSame failed at HallazgoTest.java:458");
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

            assertNull(editado.obtenerUrl(), "assertNull failed at HallazgoTest.java:485");
            assertNull(editado.obtenerTitulo(), "assertNull failed at HallazgoTest.java:486");
            assertNull(editado.obtenerHallazgo(), "assertNull failed at HallazgoTest.java:487");
            // Severidad y confianza se normalizan a valores por defecto
            assertEquals(Hallazgo.SEVERIDAD_INFO, editado.obtenerSeveridad(), "assertEquals failed at HallazgoTest.java:489");
            assertEquals(Hallazgo.CONFIANZA_MEDIA, editado.obtenerConfianza(), "assertEquals failed at HallazgoTest.java:490");
            // Mantiene solicitud original
            assertSame(solicitud, editado.obtenerSolicitudHttp(), "assertSame failed at HallazgoTest.java:492");
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

            assertNotSame(original, conEvidencia, "assertNotSame failed at HallazgoTest.java:514");
            assertSame(evidencia, conEvidencia.obtenerEvidenciaHttp(), "assertSame failed at HallazgoTest.java:515");
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

            assertSame(original, resultado, "assertSame failed at HallazgoTest.java:531");
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

            assertSame(original, resultado, "assertSame failed at HallazgoTest.java:551");
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

            assertNotSame(original, conEvidenciaId, "assertNotSame failed at HallazgoTest.java:567");
            assertEquals("evidencia-456", conEvidenciaId.obtenerEvidenciaId(), "assertEquals failed at HallazgoTest.java:568");
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

            assertSame(original, resultado, "assertSame failed at HallazgoTest.java:584");
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

            assertSame(original, resultado, "assertSame failed at HallazgoTest.java:600");
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

            assertSame(original, resultado, "assertSame failed at HallazgoTest.java:620");
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
                assertSame(evidenciaResuelta, resultado, "assertSame failed at HallazgoTest.java:652");

                // Segunda llamada usa cache
                HttpRequestResponse resultadoCacheado = hallazgo.obtenerEvidenciaHttp();
                assertSame(evidenciaResuelta, resultadoCacheado, "assertSame failed at HallazgoTest.java:656");
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

            assertNull(hallazgo.obtenerEvidenciaHttp(), "assertNull failed at HallazgoTest.java:681");
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
                assertNull(hallazgo.obtenerEvidenciaHttp(), "assertNull failed at HallazgoTest.java:707");
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

            assertSame(evidencia, hallazgo.obtenerEvidenciaHttp(), "assertSame failed at HallazgoTest.java:729");
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

            assertEquals(hallazgo, hallazgo, "assertEquals failed at HallazgoTest.java:748");
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

            assertEquals(hallazgo1, hallazgo2, "assertEquals failed at HallazgoTest.java:807");
            assertEquals(hallazgo1.hashCode(), hallazgo2.hashCode(), "assertEquals failed at HallazgoTest.java:808");
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
            assertEquals(hallazgo1, hallazgo2, "assertEquals failed at HallazgoTest.java:884");
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

            assertEquals(hallazgo1.hashCode(), hallazgo2.hashCode(), "assertEquals failed at HallazgoTest.java:914");
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

            assertEquals(hash1, hash2, "assertEquals failed at HallazgoTest.java:932");
            assertEquals(hash2, hash3, "assertEquals failed at HallazgoTest.java:933");
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

            assertTrue(resultado.contains("10:20:30"), "assertTrue failed at HallazgoTest.java:956");
            assertTrue(resultado.contains("https://example.com/test"), "assertTrue failed at HallazgoTest.java:957");
            assertTrue(resultado.contains("XSS"), "assertTrue failed at HallazgoTest.java:958");
            assertTrue(resultado.contains(Hallazgo.SEVERIDAD_HIGH), "assertTrue failed at HallazgoTest.java:959");
            assertTrue(resultado.contains(Hallazgo.CONFIANZA_ALTA), "assertTrue failed at HallazgoTest.java:960");
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

            assertNotNull(resultado, "assertNotNull failed at HallazgoTest.java:977");
            assertTrue(resultado.contains("Hallazgo"), "assertTrue failed at HallazgoTest.java:978");
        }
    }

    @Nested
    @DisplayName("Constantes")
    class Constantes {

        @Test
        @DisplayName("Constantes de severidad tienen valores correctos")
        void testConstantesSeveridad() {
            assertEquals("Critical", Hallazgo.SEVERIDAD_CRITICAL, "assertEquals failed at HallazgoTest.java:989");
            assertEquals("High", Hallazgo.SEVERIDAD_HIGH, "assertEquals failed at HallazgoTest.java:990");
            assertEquals("Medium", Hallazgo.SEVERIDAD_MEDIUM, "assertEquals failed at HallazgoTest.java:991");
            assertEquals("Low", Hallazgo.SEVERIDAD_LOW, "assertEquals failed at HallazgoTest.java:992");
            assertEquals("Info", Hallazgo.SEVERIDAD_INFO, "assertEquals failed at HallazgoTest.java:993");
        }

        @Test
        @DisplayName("Constantes de confianza tienen valores correctos")
        void testConstantesConfianza() {
            assertEquals("High", Hallazgo.CONFIANZA_ALTA, "assertEquals failed at HallazgoTest.java:999");
            assertEquals("Medium", Hallazgo.CONFIANZA_MEDIA, "assertEquals failed at HallazgoTest.java:1000");
            assertEquals("Low", Hallazgo.CONFIANZA_BAJA, "assertEquals failed at HallazgoTest.java:1001");
        }
    }
}
