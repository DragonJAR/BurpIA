package com.burpia.i18n;

import burp.api.montoya.core.ToolType;
import burp.api.montoya.ui.contextmenu.InvocationType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("I18nLogs Tests")
class I18nLogsTest {

    @BeforeEach
    void establecerIdiomaEspanol() {
        I18nUI.establecerIdioma(IdiomaUI.ES);
    }

    @AfterEach
    void restaurarIdioma() {
        I18nUI.establecerIdioma(IdiomaUI.ES);
    }

    @Nested
    @DisplayName("tr() con null y vacio")
    class TrNullYVacio {

        @Test
        @DisplayName("tr(null) retorna cadena vacia")
        void tr_null_retornaVacio() {
            assertEquals("", I18nLogs.tr(null), "assertEquals failed at I18nLogsTest.java:33");
        }

        @Test
        @DisplayName("tr(\"\") retorna cadena vacia")
        void tr_vacio_retornaVacio() {
            assertEquals("", I18nLogs.tr(""), "assertEquals failed at I18nLogsTest.java:39");
        }

        @Test
        @DisplayName("trTecnico(null) retorna cadena vacia")
        void trTecnico_null_retornaVacio() {
            assertEquals("", I18nLogs.trTecnico(null), "assertEquals failed at I18nLogsTest.java:45");
        }

        @Test
        @DisplayName("trTecnico(\"\") retorna cadena vacia")
        void trTecnico_vacio_retornaVacio() {
            assertEquals("", I18nLogs.trTecnico(""), "assertEquals failed at I18nLogsTest.java:51");
        }
    }

    @Nested
    @DisplayName("tr() traduccion Espanol a Ingles")
    class TrEspanolAIngles {

        @BeforeEach
        void establecerIngles() {
            I18nUI.establecerIdioma(IdiomaUI.EN);
        }

        @Test
        @DisplayName("traduce palabra simple")
        void traducePalabraSimple() {
            assertEquals("request", I18nLogs.tr("solicitud"), "assertEquals failed at I18nLogsTest.java:67");
            assertEquals("response", I18nLogs.tr("respuesta"), "assertEquals failed at I18nLogsTest.java:68");
            assertEquals("tasks", I18nLogs.tr("tareas"), "assertEquals failed at I18nLogsTest.java:69");
        }

        @Test
        @DisplayName("traduce frase compleja")
        void traduceFraseCompleja() {
            assertEquals("Configuration saved successfully", I18nLogs.tr("Configuracion guardada exitosamente"), "assertEquals failed at I18nLogsTest.java:75");
            assertEquals("Analysis completed", I18nLogs.tr("Analisis completado"), "assertEquals failed at I18nLogsTest.java:76");
            assertEquals("Analysis canceled", I18nLogs.tr("Analisis cancelado"), "assertEquals failed at I18nLogsTest.java:77");
        }

        @Test
        @DisplayName("traduce mensaje con URL preservando la URL")
        void traduceMensajeConUrl() {
            String resultado = I18nLogs.tr("Analisis completado: https://target.com/api");
            assertTrue(resultado.startsWith("Analysis completed"), "assertTrue failed at I18nLogsTest.java:84");
            assertTrue(resultado.contains("https://target.com/api"), "assertTrue failed at I18nLogsTest.java:85");
        }

        @Test
        @DisplayName("traduce mensaje con path de archivo")
        void traduceMensajeConPath() {
            String resultado = I18nLogs.tr("Configuracion guardada exitosamente en: /tmp/.burpia/config.json");
            assertTrue(resultado.startsWith("Configuration saved successfully"), "assertTrue failed at I18nLogsTest.java:92");
            assertTrue(resultado.contains("/tmp/.burpia/config.json"), "assertTrue failed at I18nLogsTest.java:93");
        }

        @Test
        @DisplayName("traduce mensaje largo con multiples conceptos")
        void traduceMensajeLargo() {
            String original = "Resultado descartado porque la tarea fue cancelada";
            String esperado = "Result discarded because task was canceled";
            assertTrue(I18nLogs.tr(original).startsWith(esperado), "assertTrue failed at I18nLogsTest.java:101");
        }
    }

    @Nested
    @DisplayName("tr() traduccion Ingles a Espanol")
    class TrInglesAEspanol {

        @Test
        @DisplayName("en espanol, traduce de ingles a espanol")
        void traduceInglesAEspanol() {
            assertEquals("solicitud", I18nLogs.tr("request"), "assertEquals failed at I18nLogsTest.java:112");
            assertEquals("respuesta", I18nLogs.tr("response"), "assertEquals failed at I18nLogsTest.java:113");
            assertEquals("Configuracion guardada exitosamente", I18nLogs.tr("Configuration saved successfully"), "assertEquals failed at I18nLogsTest.java:114");
        }

        @Test
        @DisplayName("en espanol, mensaje espanol permanece igual")
        void mensajeEspanolPermaneceIgual() {
            String mensaje = "Analisis completado: https://target";
            assertEquals(mensaje, I18nLogs.tr(mensaje), "assertEquals failed at I18nLogsTest.java:121");
        }
    }

    @Nested
    @DisplayName("trTecnico()")
    class TrTecnico {

        @Test
        @DisplayName("preserva header HTTP sin traduccion")
        void preservaHeaderHttp() {
            I18nUI.establecerIdioma(IdiomaUI.EN);
            String header = "Access-Control-Request-Method";
            assertEquals(header, I18nLogs.trTecnico(header), "assertEquals failed at I18nLogsTest.java:134");
        }

        @Test
        @DisplayName("preserva URL sin traduccion")
        void preservaUrl() {
            I18nUI.establecerIdioma(IdiomaUI.EN);
            String url = "https://api.example.com/v1/endpoint";
            assertEquals(url, I18nLogs.trTecnico(url), "assertEquals failed at I18nLogsTest.java:142");
        }

        @Test
        @DisplayName("preserva path de archivo sin traduccion")
        void preservaPath() {
            I18nUI.establecerIdioma(IdiomaUI.EN);
            String path = "/home/user/.burpia/config.json";
            assertEquals(path, I18nLogs.trTecnico(path), "assertEquals failed at I18nLogsTest.java:150");
        }

        @Test
        @DisplayName("preserva contenido JSON sin traduccion")
        void preservaJson() {
            I18nUI.establecerIdioma(IdiomaUI.EN);
            String json = "{\"key\": \"value\", \"nested\": {\"data\": 123}}";
            assertEquals(json, I18nLogs.trTecnico(json), "assertEquals failed at I18nLogsTest.java:158");
        }

        @Test
        @DisplayName("preserva mensaje en espanol cuando idioma es ingles")
        void preservaMensajeEspanol() {
            I18nUI.establecerIdioma(IdiomaUI.EN);
            String mensaje = "Configuracion guardada exitosamente";
            assertEquals(mensaje, I18nLogs.trTecnico(mensaje), "assertEquals failed at I18nLogsTest.java:166");
        }
    }

    @Nested
    @DisplayName("tr() con idioma no soportado")
    class TrIdiomaNoSoportado {

        @Test
        @DisplayName("idioma invalido retorna mensaje original")
        void idiomaInvalido_retornaOriginal() {
            I18nUI.establecerIdioma("fr");
            String mensaje = "Analisis completado";
            assertEquals(mensaje, I18nLogs.tr(mensaje), "assertEquals failed at I18nLogsTest.java:179");
        }

        @Test
        @DisplayName("idioma null retorna mensaje original")
        void idiomaNull_retornaOriginal() {
            I18nUI.establecerIdioma((IdiomaUI) null);
            String mensaje = "Analisis completado";
            assertEquals(mensaje, I18nLogs.tr(mensaje), "assertEquals failed at I18nLogsTest.java:187");
        }
    }

    @Nested
    @DisplayName("Clase interna Inicializacion")
    class InicializacionTests {

        @BeforeEach
        void establecerIngles() {
            I18nUI.establecerIdioma(IdiomaUI.EN);
        }

        @Test
        @DisplayName("SEPARADOR retorna separador visual")
        void separador() {
            assertEquals("==================================================", I18nLogs.Inicializacion.SEPARADOR(), "assertEquals failed at I18nLogsTest.java:203");
        }

        @Test
        @DisplayName("SECCION_CONFIGURACION traducida")
        void seccionConfiguracion() {
            assertEquals("[Configuration]", I18nLogs.Inicializacion.SECCION_CONFIGURACION(), "assertEquals failed at I18nLogsTest.java:209");
        }

        @Test
        @DisplayName("SECCION_ENTORNO traducida")
        void seccionEntorno() {
            assertEquals("[Environment]", I18nLogs.Inicializacion.SECCION_ENTORNO(), "assertEquals failed at I18nLogsTest.java:215");
        }

        @Test
        @DisplayName("SECCION_MULTI_PROVEEDOR traducida")
        void seccionMultiProveedor() {
            assertEquals("[Multi-Provider Configuration]", I18nLogs.Inicializacion.SECCION_MULTI_PROVEEDOR(), "assertEquals failed at I18nLogsTest.java:221");
        }

        @Test
        @DisplayName("SECCION_RENDIMIENTO traducida")
        void seccionRendimiento() {
            assertEquals("[Performance]", I18nLogs.Inicializacion.SECCION_RENDIMIENTO(), "assertEquals failed at I18nLogsTest.java:227");
        }

        @Test
        @DisplayName("SECCION_AGENTE traducida")
        void seccionAgente() {
            assertEquals("[Agent]", I18nLogs.Inicializacion.SECCION_AGENTE(), "assertEquals failed at I18nLogsTest.java:233");
        }

        @Test
        @DisplayName("SI y NO traducidos")
        void siNo() {
            assertEquals("Yes", I18nLogs.Inicializacion.SI(), "assertEquals failed at I18nLogsTest.java:239");
            assertEquals("No", I18nLogs.Inicializacion.NO(), "assertEquals failed at I18nLogsTest.java:240");
        }

        @Test
        @DisplayName("TAREAS formateado correctamente")
        void tareas() {
            assertEquals("5 tasks", I18nLogs.Inicializacion.TAREAS("5"), "assertEquals failed at I18nLogsTest.java:246");
        }

        @Test
        @DisplayName("RETRASO_SEGUNDOS formateado correctamente")
        void retrasoSegundos() {
            assertEquals("delay 10s", I18nLogs.Inicializacion.RETRASO_SEGUNDOS("10"), "assertEquals failed at I18nLogsTest.java:252");
        }

        @Test
        @DisplayName("MAX_HALLAZGOS formateado correctamente")
        void maxHallazgos() {
            assertEquals("max findings: 1000", I18nLogs.Inicializacion.MAX_HALLAZGOS("1000"), "assertEquals failed at I18nLogsTest.java:258");
        }

        @Test
        @DisplayName("ENTORNO_BURP_SUITE formateado correctamente")
        void entornoBurpSuite() {
            String resultado = I18nLogs.Inicializacion.ENTORNO_BURP_SUITE("Professional", "2024.1");
            assertTrue(resultado.contains("Burp Suite:"), "assertTrue failed at I18nLogsTest.java:265");
            assertTrue(resultado.contains("Professional"), "assertTrue failed at I18nLogsTest.java:266");
            assertTrue(resultado.contains("2024.1"), "assertTrue failed at I18nLogsTest.java:267");
        }

        @Test
        @DisplayName("ENTORNO_JAVA formateado correctamente")
        void entornoJava() {
            String resultado = I18nLogs.Inicializacion.ENTORNO_JAVA("21.0.1", "Oracle");
            assertTrue(resultado.contains("Java:"), "assertTrue failed at I18nLogsTest.java:274");
            assertTrue(resultado.contains("21.0.1"), "assertTrue failed at I18nLogsTest.java:275");
            assertTrue(resultado.contains("Oracle"), "assertTrue failed at I18nLogsTest.java:276");
        }

        @Test
        @DisplayName("ENTORNO_OS formateado correctamente")
        void entornoOs() {
            String resultado = I18nLogs.Inicializacion.ENTORNO_OS("macOS", "14.0", "aarch64");
            assertTrue(resultado.contains("OS:"), "assertTrue failed at I18nLogsTest.java:283");
            assertTrue(resultado.contains("macOS"), "assertTrue failed at I18nLogsTest.java:284");
            assertTrue(resultado.contains("aarch64"), "assertTrue failed at I18nLogsTest.java:285");
        }

        @Test
        @DisplayName("URL_API formateado correctamente")
        void urlApi() {
            String url = "https://api.openai.com/v1";
            assertEquals("API URL: " + url, I18nLogs.Inicializacion.URL_API(url), "assertEquals failed at I18nLogsTest.java:292");
        }

        @Test
        @DisplayName("SSL_VERIFICACION traducido correctamente")
        void sslVerificacion() {
            assertEquals("SSL Verification: ON", I18nLogs.Inicializacion.SSL_VERIFICACION(true), "assertEquals failed at I18nLogsTest.java:298");
            assertEquals("SSL Verification: OFF", I18nLogs.Inicializacion.SSL_VERIFICACION(false), "assertEquals failed at I18nLogsTest.java:299");
        }

        @Test
        @DisplayName("MODO_SOLO_PROXY traducido correctamente")
        void modoSoloProxy() {
            assertEquals("Proxy-only mode: ON", I18nLogs.Inicializacion.MODO_SOLO_PROXY(true), "assertEquals failed at I18nLogsTest.java:305");
            assertEquals("Proxy-only mode: OFF", I18nLogs.Inicializacion.MODO_SOLO_PROXY(false), "assertEquals failed at I18nLogsTest.java:306");
        }

        @Test
        @DisplayName("INICIALIZACION_COMPLETA traducida")
        void inicializacionCompleta() {
            assertEquals("Initialization completed successfully", I18nLogs.Inicializacion.INICIALIZACION_COMPLETA(), "assertEquals failed at I18nLogsTest.java:312");
        }

        @Test
        @DisplayName("Inicializacion en espanol")
        void inicializacionEspanol() {
            I18nUI.establecerIdioma(IdiomaUI.ES);
            assertEquals("[Configuracion]", I18nLogs.Inicializacion.SECCION_CONFIGURACION(), "assertEquals failed at I18nLogsTest.java:319");
            assertEquals("Sí", I18nLogs.Inicializacion.SI(), "assertEquals failed at I18nLogsTest.java:320");
            assertEquals("No", I18nLogs.Inicializacion.NO(), "assertEquals failed at I18nLogsTest.java:321");
            assertEquals("Inicialización completada exitosamente", I18nLogs.Inicializacion.INICIALIZACION_COMPLETA(), "assertEquals failed at I18nLogsTest.java:322");
        }
    }

    @Nested
    @DisplayName("Clase interna ContextoMenu")
    class ContextoMenuTests {

        @Test
        @DisplayName("mensaje contextual incluye origen real en inglés")
        void accionIniciadaIncluyeOrigenRealEnIngles() {
            I18nUI.establecerIdioma(IdiomaUI.EN);

            String mensaje = I18nLogs.ContextoMenu.ACCION_INICIADA(
                I18nLogs.ContextoMenu.ACCION_ANALIZAR_FLUJO(),
                InvocationType.PROXY_HISTORY,
                ToolType.PROXY,
                2
            );

            assertTrue(mensaje.contains("PROXY_HISTORY"), mensaje);
            assertTrue(mensaje.contains("Proxy History"), mensaje);
            assertTrue(mensaje.contains("selected=2"), mensaje);
        }

        @Test
        @DisplayName("mensaje contextual de response ausente se localiza en español")
        void responseAusenteSeLocalizaEnEspanol() {
            I18nUI.establecerIdioma(IdiomaUI.ES);

            String mensaje = I18nLogs.ContextoMenu.RESPONSE_AUSENTE("GET", "https://example.com");

            assertTrue(mensaje.contains("sin response asociada"), mensaje);
            assertTrue(mensaje.contains("GET"), mensaje);
        }

        @Test
        @DisplayName("serialización de agente resume responses omitidas")
        void serializacionAgenteResumeResponsesOmitidas() {
            I18nUI.establecerIdioma(IdiomaUI.ES);

            String mensaje = I18nLogs.ContextoMenu.SERIALIZACION_AGENTE(2, 1, 1);

            assertTrue(mensaje.contains("requests=2"), mensaje);
            assertTrue(mensaje.contains("responses omitidas=1"), mensaje);
        }
    }

    @Nested
    @DisplayName("Clase interna Agente")
    class AgenteTests {

        @Test
        @DisplayName("ERROR_DESHABILITADO en ingles")
        void errorDeshabilitadoIngles() {
            I18nUI.establecerIdioma(IdiomaUI.EN);
            assertEquals("Factory Droid agent disabled in settings.", I18nLogs.Agente.ERROR_DESHABILITADO(), "assertEquals failed at I18nLogsTest.java:334");
        }

        @Test
        @DisplayName("ERROR_DESHABILITADO en espanol")
        void errorDeshabilitadoEspanol() {
            I18nUI.establecerIdioma(IdiomaUI.ES);
            assertEquals("Agente Factory Droid deshabilitado en ajustes.", I18nLogs.Agente.ERROR_DESHABILITADO(), "assertEquals failed at I18nLogsTest.java:341");
        }
    }

    @Nested
    @DisplayName("Casos edge y caracteres especiales")
    class CasosEdge {

        @Test
        @DisplayName("mensaje con caracteres especiales se traduce")
        void caracteresEspeciales() {
            I18nUI.establecerIdioma(IdiomaUI.EN);
            String resultado = I18nLogs.tr("Error inesperado: conexión falló");
            assertTrue(resultado.contains("Unexpected error"), "assertTrue failed at I18nLogsTest.java:354");
        }

        @Test
        @DisplayName("mensaje con multiples traducciones en una linea")
        void multiplesTraducciones() {
            I18nUI.establecerIdioma(IdiomaUI.EN);
            String resultado = I18nLogs.tr("solicitud y respuesta procesadas");
            assertTrue(resultado.contains("request"), "assertTrue failed at I18nLogsTest.java:362");
            assertTrue(resultado.contains("response"), "assertTrue failed at I18nLogsTest.java:363");
        }

        @Test
        @DisplayName("mensaje con severidad se traduce correctamente")
        void mensajeConSeveridad() {
            I18nUI.establecerIdioma(IdiomaUI.EN);
            String resultado = I18nLogs.tr("severidad maxima: Critical");
            assertTrue(resultado.contains("max severity"), "assertTrue failed at I18nLogsTest.java:371");
        }

        @Test
        @DisplayName("mensaje con hallazgos se traduce")
        void mensajeConHallazgos() {
            I18nUI.establecerIdioma(IdiomaUI.EN);
            String resultado = I18nLogs.tr("5 hallazgos encontrados");
            assertTrue(resultado.contains("findings"), "assertTrue failed at I18nLogsTest.java:379");
        }
    }

    @Nested
    @DisplayName("Consistencia bidireccional")
    class ConsistenciaBidireccional {

        @Test
        @DisplayName("traduccion ida y vuelta es consistente")
        void traduccionIdaVuelta() {
            String original = "Configuracion guardada exitosamente";

            I18nUI.establecerIdioma(IdiomaUI.EN);
            String enIngles = I18nLogs.tr(original);

            I18nUI.establecerIdioma(IdiomaUI.ES);
            String enEspanol = I18nLogs.tr(enIngles);

            assertEquals(original, enEspanol, "assertEquals failed at I18nLogsTest.java:398");
        }

        @Test
        @DisplayName("palabras simples se traducen bidireccionalmente")
        void palabrasSimplesBidireccional() {
            I18nUI.establecerIdioma(IdiomaUI.EN);
            assertEquals("request", I18nLogs.tr("solicitud"), "assertEquals failed at I18nLogsTest.java:405");

            I18nUI.establecerIdioma(IdiomaUI.ES);
            assertEquals("solicitud", I18nLogs.tr("request"), "assertEquals failed at I18nLogsTest.java:408");
        }
    }
}
