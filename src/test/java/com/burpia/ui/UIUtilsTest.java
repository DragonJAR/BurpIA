package com.burpia.ui;

import com.burpia.i18n.I18nUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Insets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("UIUtils Tests")
class UIUtilsTest {

    private static final String IDIOMA_DEFAULT = "es";

    @BeforeEach
    void setUp() {
        I18nUI.establecerIdioma(IDIOMA_DEFAULT);
    }

    @AfterEach
    void tearDown() {
        I18nUI.establecerIdioma(IDIOMA_DEFAULT);
    }

    @Nested
    @DisplayName("extraerTextoVisibleEnlace")
    class ExtraerTextoVisibleEnlaceTests {

        @Test
        @DisplayName("mantiene texto plano sin cambios")
        void textoPlano() {
            assertEquals("Como instalar Factory Droid?", UIUtils.extraerTextoVisibleEnlace("Como instalar Factory Droid?"));
        }

        @Test
        @DisplayName("elimina anchor html")
        void conAnchor() {
            String input = "<html><a href='https://example.com'>Como instalar Factory Droid?</a></html>";
            assertEquals("Como instalar Factory Droid?", UIUtils.extraerTextoVisibleEnlace(input));
        }

        @Test
        @DisplayName("elimina etiquetas html residuales")
        void conEtiquetasHtml() {
            String input = "<b>Texto</b> <i>de enlace</i>";
            assertEquals("Texto de enlace", UIUtils.extraerTextoVisibleEnlace(input));
        }

        @Test
        @DisplayName("maneja null retornando vacio")
        void nulo() {
            assertEquals("", UIUtils.extraerTextoVisibleEnlace(null));
        }

        @Test
        @DisplayName("maneja string vacio")
        void vacio() {
            assertEquals("", UIUtils.extraerTextoVisibleEnlace(""));
        }

        @Test
        @DisplayName("maneja solo espacios en blanco")
        void soloEspacios() {
            assertEquals("", UIUtils.extraerTextoVisibleEnlace("   "));
        }
    }

    @Nested
    @DisplayName("abrirUrlConFallbackInfo")
    class AbrirUrlConFallbackInfoTests {

        @Test
        @DisplayName("retorna false con URL null")
        void urlNull() {
            assertFalse(UIUtils.abrirUrlConFallbackInfo(null, "Titulo", null, "Mensaje"));
        }

        @Test
        @DisplayName("retorna false con URL vacia")
        void urlVacia() {
            assertFalse(UIUtils.abrirUrlConFallbackInfo(null, "Titulo", "", "Mensaje"));
        }

        @Test
        @DisplayName("retorna false con URL solo espacios")
        void urlSoloEspacios() {
            assertFalse(UIUtils.abrirUrlConFallbackInfo(null, "Titulo", "   ", "Mensaje"));
        }
    }

    @Nested
    @DisplayName("normalizarPadreDialogo")
    class NormalizarPadreDialogoTests {

        @Test
        @DisplayName("usa invocador cuando recibe popup")
        void conPopup() {
            JButton invocador = new JButton("Invocador");
            JPopupMenu popup = new JPopupMenu();
            popup.setInvoker(invocador);

            assertEquals(invocador, UIUtils.normalizarPadreDialogo(popup));
        }
    }

    @Nested
    @DisplayName("envolverContenidoConIcono")
    class EnvolverContenidoConIconoTests {

        @Test
        @DisplayName("mantiene contenido y gap horizontal fijo")
        void contenidoYGap() {
            JPanel contenido = new JPanel();
            JPanel envuelto = UIUtils.envolverContenidoConIcono(contenido, JOptionPane.INFORMATION_MESSAGE);

            BorderLayout layout = (BorderLayout) envuelto.getLayout();
            assertEquals(12, layout.getHgap());
            assertEquals(contenido, layout.getLayoutComponent(BorderLayout.CENTER));
            assertNotNull(layout.getLayoutComponent(BorderLayout.WEST));
        }
    }

    @Nested
    @DisplayName("crearAreaMensajeDialogo")
    class CrearAreaMensajeDialogoTests {

        @Test
        @DisplayName("reserva margen horizontal")
        void margenHorizontal() {
            Insets margen = UIUtils.crearAreaMensajeDialogo("mensaje").getMargin();
            assertTrue(margen.left >= 2);
            assertTrue(margen.right >= 2);
        }
    }

    @Nested
    @DisplayName("Internacionalizacion")
    class InternacionalizacionTests {

        @Test
        @DisplayName("texto de checkbox no volver a mostrar se localiza en espanol e ingles")
        void textoCheckboxOptOut() {
            I18nUI.establecerIdioma("es");
            assertEquals("No volver a mostrar este mensaje", I18nUI.General.CHECK_NO_VOLVER_MOSTRAR_ALERTA());

            I18nUI.establecerIdioma("en");
            assertEquals("Do not show this message again", I18nUI.General.CHECK_NO_VOLVER_MOSTRAR_ALERTA());
        }

        @Test
        @DisplayName("mensaje de binario inexistente incluye comando completo cuando hay flags")
        void mensajeBinarioInexistenteConFlags() {
            String mensaje = UIUtils.construirMensajeBinarioAgenteNoEncontrado(
                "Claude Code",
                "/opt/claude/bin/claude --dangerously-skip-permissions"
            );

            assertEquals(
                "El binario de Claude Code no existe en la ruta actual: /opt/claude/bin/claude\n"
                    + "Comando configurado: /opt/claude/bin/claude --dangerously-skip-permissions",
                mensaje
            );
        }

        @Test
        @DisplayName("mensaje de binario inexistente en ingles sin flags no agrega linea extra")
        void mensajeBinarioInexistenteSinFlags() {
            I18nUI.establecerIdioma("en");

            String mensaje = UIUtils.construirMensajeBinarioAgenteNoEncontrado(
                "Factory Droid",
                "/tmp/droid"
            );

            assertEquals(
                "The Factory Droid binary does not exist at the current path: /tmp/droid",
                mensaje
            );
        }
    }

    @Nested
    @DisplayName("normalizarDelayMs")
    class NormalizarDelayMsTests {

        @Test
        @DisplayName("evita valores negativos")
        void valorNegativo() {
            assertEquals(0, UIUtils.normalizarDelayMs(-5));
        }

        @Test
        @DisplayName("retorna cero para cero")
        void cero() {
            assertEquals(0, UIUtils.normalizarDelayMs(0));
        }

        @Test
        @DisplayName("retorna valor positivo sin cambios")
        void valorPositivo() {
            assertEquals(140, UIUtils.normalizarDelayMs(140));
        }

        @Test
        @DisplayName("maneja Integer.MAX_VALUE")
        void maximoEntero() {
            assertEquals(Integer.MAX_VALUE, UIUtils.normalizarDelayMs(Integer.MAX_VALUE));
        }
    }

    @Nested
    @DisplayName("mostrarInfoConOptOutMenuContextual")
    class MostrarInfoConOptOutTests {

        @Test
        @DisplayName("respeta opt-out deshabilitado sin lanzar excepcion")
        void optOutDeshabilitadoNoLanzaExcepcion() {
            assertDoesNotThrow(() -> UIUtils.mostrarInfoConOptOutMenuContextual(
                null, "Titulo", "Mensaje", false, null
            ));
        }
    }

    @Nested
    @DisplayName("mostrarAdvertenciaConOptOutMenuContextual")
    class MostrarAdvertenciaConOptOutTests {

        @Test
        @DisplayName("respeta opt-out deshabilitado sin lanzar excepcion")
        void optOutDeshabilitadoNoLanzaExcepcion() {
            assertDoesNotThrow(() -> UIUtils.mostrarAdvertenciaConOptOutMenuContextual(
                null, "Titulo", "Mensaje", false, null
            ));
        }
    }
}
