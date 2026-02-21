package com.burpia;

import com.burpia.ui.PestaniaPrincipal;
import com.burpia.util.GestorConsolaGUI;
import com.burpia.util.GestorTareas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
