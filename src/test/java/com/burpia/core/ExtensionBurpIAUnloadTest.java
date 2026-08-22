package com.burpia.core;

import com.burpia.ExtensionBurpIA;
import com.burpia.processor.HttpRequestProcessor;
import com.burpia.ui.FabricaMenuContextual;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests para el comportamiento de cleanup en {@link ExtensionBurpIA#unload()}.
 * Spec: CAT-011 — Remediation de memory leaks y ghost menu items tras unload.
 */
@DisplayName("ExtensionBurpIA Unload Tests")
class ExtensionBurpIAUnloadTest {

    /**
     * Helper: establece un campo privado en el objetivo via reflexión.
     */
    private static void establecerCampo(Object objetivo, String nombre, Object valor) throws Exception {
        Field field = ExtensionBurpIA.class.getDeclaredField(nombre);
        field.setAccessible(true);
        field.set(objetivo, valor);
    }

    /**
     * Helper: obtiene el valor de un campo privado en el objetivo via reflexión.
     */
    private static Object obtenerCampo(Object objetivo, String nombre) throws Exception {
        Field field = ExtensionBurpIA.class.getDeclaredField(nombre);
        field.setAccessible(true);
        return field.get(objetivo);
    }

    @Nested
    @DisplayName("CAT-011: Cleanup de httpRequestProcessor")
    class HttpRequestProcessorCleanupTests {

        @Test
        @DisplayName("unload() nullifica httpRequestProcessor después de cleanup")
        void testUnloadNullificaHttpRequestProcessor() throws Exception {
            ExtensionBurpIA extension = new ExtensionBurpIA();
            establecerCampo(extension, "stdout", new PrintWriter(new StringWriter(), true));
            establecerCampo(extension, "stderr", new PrintWriter(new StringWriter(), true));

            HttpRequestProcessor procesadorMock = mock(HttpRequestProcessor.class);
            establecerCampo(extension, "httpRequestProcessor", procesadorMock);

            extension.unload();

            assertNull(
                obtenerCampo(extension, "httpRequestProcessor"),
                "assertNull failed at ExtensionBurpIAUnloadTest.java:51"
            );
        }

        @Test
        @DisplayName("unload() es idempotente sobre httpRequestProcessor (no lanza segunda vez)")
        void testUnloadIdempotenteHttpRequestProcessor() throws Exception {
            ExtensionBurpIA extension = new ExtensionBurpIA();
            establecerCampo(extension, "stdout", new PrintWriter(new StringWriter(), true));
            establecerCampo(extension, "stderr", new PrintWriter(new StringWriter(), true));

            HttpRequestProcessor procesadorMock = mock(HttpRequestProcessor.class);
            establecerCampo(extension, "httpRequestProcessor", procesadorMock);

            extension.unload();

            // Segunda llamada no debe lanzar
            assertDoesNotThrow(extension::unload);

            assertNull(
                obtenerCampo(extension, "httpRequestProcessor"),
                "assertNull failed at ExtensionBurpIAUnloadTest.java:69"
            );
        }
    }

    @Nested
    @DisplayName("CAT-011: Cleanup de FabricaMenuContextual")
    class FabricaMenuContextualCleanupTests {

        @Test
        @DisplayName("unload() llama marcarDescargado() en FabricaMenuContextual cuando existe")
        void testUnloadLlamaMarcarDescargadoEnFabricaMenuContextual() throws Exception {
            ExtensionBurpIA extension = new ExtensionBurpIA();
            establecerCampo(extension, "stdout", new PrintWriter(new StringWriter(), true));
            establecerCampo(extension, "stderr", new PrintWriter(new StringWriter(), true));

            FabricaMenuContextual fabricaMock = mock(FabricaMenuContextual.class);
            establecerCampo(extension, "fabricaMenuContextual", fabricaMock);

            extension.unload();

            verify(fabricaMock).marcarDescargado();
        }

        @Test
        @DisplayName("unload() no lanza si fabricaMenuContextual es null (null-safe)")
        void testUnloadNoLanzaExcepcionSiFabricaMenuContextualEsNull() throws Exception {
            ExtensionBurpIA extension = new ExtensionBurpIA();
            establecerCampo(extension, "stdout", new PrintWriter(new StringWriter(), true));
            establecerCampo(extension, "stderr", new PrintWriter(new StringWriter(), true));

            // Explicitamente null
            establecerCampo(extension, "fabricaMenuContextual", null);

            assertDoesNotThrow(extension::unload,
                "assertDoesNotThrow failed at ExtensionBurpIAUnloadTest.java:95");
        }

        @Test
        @DisplayName("unload() es idempotente cuando fabricaMenuContextual es null")
        void testUnloadEsIdempotenteConFabricaNull() throws Exception {
            ExtensionBurpIA extension = new ExtensionBurpIA();
            establecerCampo(extension, "stdout", new PrintWriter(new StringWriter(), true));
            establecerCampo(extension, "stderr", new PrintWriter(new StringWriter(), true));

            establecerCampo(extension, "fabricaMenuContextual", null);

            extension.unload();
            assertDoesNotThrow(extension::unload);
        }
    }

    @Nested
    @DisplayName("CAT-011: Seguridad sin inicialización previa")
    class SinInicializacionPreviaTests {

        @Test
        @DisplayName("unload() es seguro sin initialize() previo")
        void testUnloadEsSeguroSinInicializacionPrevia() {
            ExtensionBurpIA extension = new ExtensionBurpIA();
            assertDoesNotThrow(extension::unload,
                "assertDoesNotThrow failed at ExtensionBurpIAUnloadTest.java:117");
        }

        @Test
        @DisplayName("unload() multiple veces es idempotente")
        void testUnloadEsIdempotente() throws Exception {
            ExtensionBurpIA extension = new ExtensionBurpIA();
            establecerCampo(extension, "stdout", new PrintWriter(new StringWriter(), true));
            establecerCampo(extension, "stderr", new PrintWriter(new StringWriter(), true));

            // JUnit considera el test exitoso si las dos llamadas a unload()
            // completan sin propagar excepción. No hace falta assertTrue(true).
            extension.unload();
            extension.unload();
        }

        @Test
        @DisplayName("unload() no deja httpRequestProcessor huérfano tras múltiples llamadas")
        void testUnloadNoDejaHttpRequestProcessorOrphan() throws Exception {
            ExtensionBurpIA extension = new ExtensionBurpIA();
            establecerCampo(extension, "stdout", new PrintWriter(new StringWriter(), true));
            establecerCampo(extension, "stderr", new PrintWriter(new StringWriter(), true));

            HttpRequestProcessor procesadorMock = mock(HttpRequestProcessor.class);
            establecerCampo(extension, "httpRequestProcessor", procesadorMock);

            extension.unload();
            extension.unload();

            assertNull(
                obtenerCampo(extension, "httpRequestProcessor"),
                "assertNull failed at ExtensionBurpIAUnloadTest.java:144"
            );
        }
    }
}
