package com.burpia;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import com.burpia.model.Hallazgo;
import com.burpia.ui.PestaniaPrincipal;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorTareas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ExtensionBurpIA Tests")
class ExtensionBurpIATest {

    @Test
    @DisplayName("Unload limpia recursos y cierra gestores")
    void testUnloadLimpiaRecursos() throws Exception {
        ExtensionBurpIA extension = new ExtensionBurpIA();
        establecerCampo(extension, "stdout", new PrintWriter(new StringWriter(), true));
        establecerCampo(extension, "stderr", new PrintWriter(new StringWriter(), true));

        ManejadorHttpBurpIA manejador = mock(ManejadorHttpBurpIA.class);
        PestaniaPrincipal pestania = mock(PestaniaPrincipal.class);
        GestorTareas gestorTareas = mock(GestorTareas.class);
        GestorConsolaGUI gestorConsola = mock(GestorConsolaGUI.class);

        establecerCampo(extension, "manejadorHttp", manejador);
        establecerCampo(extension, "pestaniaPrincipal", pestania);
        establecerCampo(extension, "gestorTareas", gestorTareas);
        establecerCampo(extension, "gestorConsola", gestorConsola);

        extension.unload();

        verify(manejador).shutdown();
        verify(pestania).destruir();
        verify(gestorTareas).shutdown();
        verify(gestorConsola).shutdown();

        assertNull(obtenerCampo(extension, "manejadorHttp"));
        assertNull(obtenerCampo(extension, "pestaniaPrincipal"));
        assertNull(obtenerCampo(extension, "gestorTareas"));
        assertNull(obtenerCampo(extension, "gestorConsola"));
    }

    @Test
    @DisplayName("Unload es seguro sin initialize previo")
    void testUnloadSinInitializeNoFalla() {
        ExtensionBurpIA extension = new ExtensionBurpIA();
        assertDoesNotThrow(extension::unload);
    }

    @Test
    @DisplayName("Abrir configuracion es seguro sin dependencias inicializadas")
    void testAbrirConfiguracionSinDependenciasNoFalla() throws Exception {
        ExtensionBurpIA extension = new ExtensionBurpIA();
        Method abrirConfiguracion = ExtensionBurpIA.class.getDeclaredMethod("abrirConfiguracion");
        abrirConfiguracion.setAccessible(true);
        assertDoesNotThrow(() -> abrirConfiguracion.invoke(extension));
    }

    @Test
    @DisplayName("Guardar AuditIssue retorna false si SiteMap no esta disponible")
    void testGuardarAuditIssueSinSiteMap() {
        MontoyaApi api = mock(MontoyaApi.class);
        when(api.siteMap()).thenReturn(null);
        Hallazgo hallazgo = new Hallazgo("https://example.com", "Possible SQLi", "High", "High");

        boolean guardado = ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(api, hallazgo, null);

        assertFalse(guardado);
    }

    @Test
    @DisplayName("Crear AuditIssue mapea severidad Info a INFORMATION")
    void testCrearAuditIssueMapeaInfoAInformation() {
        assertEquals(
            burp.api.montoya.scanner.audit.issues.AuditIssueSeverity.INFORMATION,
            invocarConvertirSeveridad(Hallazgo.SEVERIDAD_INFO)
        );
    }

    @Test
    @DisplayName("Crear AuditIssue usa evidencia embebida en hallazgo cuando no se pasa evidencia explicita")
    void testCrearAuditIssueUsaEvidenciaEmbebida() {
        HttpRequestResponse evidencia = mock(HttpRequestResponse.class);
        Hallazgo hallazgo = new Hallazgo(
            "https://example.com",
            "Posible SQLi",
            Hallazgo.SEVERIDAD_HIGH,
            Hallazgo.CONFIANZA_ALTA,
            null,
            evidencia
        );
        assertEquals(evidencia, ExtensionBurpIA.resolverEvidenciaIssue(hallazgo, null));
        HttpRequestResponse explicita = mock(HttpRequestResponse.class);
        assertEquals(explicita, ExtensionBurpIA.resolverEvidenciaIssue(hallazgo, explicita));
    }

    private static burp.api.montoya.scanner.audit.issues.AuditIssueSeverity invocarConvertirSeveridad(String severidad) {
        try {
            Method metodo = ExtensionBurpIA.class.getDeclaredMethod("convertirSeveridad", String.class);
            metodo.setAccessible(true);
            return (burp.api.montoya.scanner.audit.issues.AuditIssueSeverity) metodo.invoke(null, severidad);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void establecerCampo(Object objetivo, String nombre, Object valor) throws Exception {
        Field field = ExtensionBurpIA.class.getDeclaredField(nombre);
        field.setAccessible(true);
        field.set(objetivo, valor);
    }

    private static Object obtenerCampo(Object objetivo, String nombre) throws Exception {
        Field field = ExtensionBurpIA.class.getDeclaredField(nombre);
        field.setAccessible(true);
        return field.get(objetivo);
    }
}
