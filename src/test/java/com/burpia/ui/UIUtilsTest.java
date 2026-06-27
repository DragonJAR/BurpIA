package com.burpia.ui;

import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
        UIUtils.configurarAlertas(null, null);
    }

    @Nested
    @DisplayName("extraerTextoVisibleEnlace")
    class ExtraerTextoVisibleEnlaceTests {

        @Test
        @DisplayName("mantiene texto plano sin cambios")
        void textoPlano() {
            assertEquals("Como instalar Factory Droid?", UIUtils.extraerTextoVisibleEnlace("Como instalar Factory Droid?"), "assertEquals failed at UIUtilsTest.java:45");
        }

        @Test
        @DisplayName("elimina anchor html")
        void conAnchor() {
            String input = "<html><a href='https://example.com'>Como instalar Factory Droid?</a></html>";
            assertEquals("Como instalar Factory Droid?", UIUtils.extraerTextoVisibleEnlace(input), "assertEquals failed at UIUtilsTest.java:52");
        }

        @Test
        @DisplayName("elimina etiquetas html residuales")
        void conEtiquetasHtml() {
            String input = "<b>Texto</b> <i>de enlace</i>";
            assertEquals("Texto de enlace", UIUtils.extraerTextoVisibleEnlace(input), "assertEquals failed at UIUtilsTest.java:59");
        }

        @Test
        @DisplayName("maneja null retornando vacio")
        void nulo() {
            assertEquals("", UIUtils.extraerTextoVisibleEnlace(null), "assertEquals failed at UIUtilsTest.java:65");
        }

        @Test
        @DisplayName("maneja string vacio")
        void vacio() {
            assertEquals("", UIUtils.extraerTextoVisibleEnlace(""), "assertEquals failed at UIUtilsTest.java:71");
        }

        @Test
        @DisplayName("maneja solo espacios en blanco")
        void soloEspacios() {
            assertEquals("", UIUtils.extraerTextoVisibleEnlace("   "), "assertEquals failed at UIUtilsTest.java:77");
        }
    }

    @Nested
    @DisplayName("abrirUrlConFallbackInfo")
    class AbrirUrlConFallbackInfoTests {

        @Test
        @DisplayName("retorna false con URL null")
        void urlNull() {
            assertFalse(UIUtils.abrirUrlConFallbackInfo(null, "Titulo", null, "Mensaje"), "assertFalse failed at UIUtilsTest.java:88");
        }

        @Test
        @DisplayName("retorna false con URL vacia")
        void urlVacia() {
            assertFalse(UIUtils.abrirUrlConFallbackInfo(null, "Titulo", "", "Mensaje"), "assertFalse failed at UIUtilsTest.java:94");
        }

        @Test
        @DisplayName("retorna false con URL solo espacios")
        void urlSoloEspacios() {
            assertFalse(UIUtils.abrirUrlConFallbackInfo(null, "Titulo", "   ", "Mensaje"), "assertFalse failed at UIUtilsTest.java:100");
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

            assertEquals(invocador, UIUtils.normalizarPadreDialogo(popup), "assertEquals failed at UIUtilsTest.java:115");
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
            assertEquals(12, layout.getHgap(), "assertEquals failed at UIUtilsTest.java:130");
            assertEquals(contenido, layout.getLayoutComponent(BorderLayout.CENTER), "assertEquals failed at UIUtilsTest.java:131");
            assertNotNull(layout.getLayoutComponent(BorderLayout.WEST), "assertNotNull failed at UIUtilsTest.java:132");
        }
    }

    @Nested
    @DisplayName("crearAreaMensajeDialogo")
    class CrearAreaMensajeDialogoTests {

        @Test
        @DisplayName("reserva margen horizontal")
        void margenHorizontal() {
            Insets margen = UIUtils.crearAreaMensajeDialogo("mensaje").getMargin();
            assertTrue(margen.left >= 2, "assertTrue failed at UIUtilsTest.java:144");
            assertTrue(margen.right >= 2, "assertTrue failed at UIUtilsTest.java:145");
        }
    }

    @Nested
    @DisplayName("Internacionalizacion")
    class InternacionalizacionTests {

        @Test
        @DisplayName("texto de checkbox no volver a mostrar se localiza en espanol e ingles")
        void textoCheckboxOptOut() {
            I18nUI.establecerIdioma("es");
            assertEquals("No volver a mostrar alertas ni notificaciones", I18nUI.General.CHECK_NO_VOLVER_MOSTRAR_ALERTA(), "assertEquals failed at UIUtilsTest.java:157");

            I18nUI.establecerIdioma("en");
            assertEquals("Stop showing alerts and notifications", I18nUI.General.CHECK_NO_VOLVER_MOSTRAR_ALERTA(), "assertEquals failed at UIUtilsTest.java:160");
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
            , "assertEquals failed at UIUtilsTest.java:171");
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
            , "assertEquals failed at UIUtilsTest.java:188");
        }
    }

    @Nested
    @DisplayName("normalizarDelayMs")
    class NormalizarDelayMsTests {

        @Test
        @DisplayName("evita valores negativos")
        void valorNegativo() {
            assertEquals(0, UIUtils.normalizarDelayMs(-5), "assertEquals failed at UIUtilsTest.java:202");
        }

        @Test
        @DisplayName("retorna cero para cero")
        void cero() {
            assertEquals(0, UIUtils.normalizarDelayMs(0), "assertEquals failed at UIUtilsTest.java:208");
        }

        @Test
        @DisplayName("retorna valor positivo sin cambios")
        void valorPositivo() {
            assertEquals(140, UIUtils.normalizarDelayMs(140), "assertEquals failed at UIUtilsTest.java:214");
        }

        @Test
        @DisplayName("maneja Integer.MAX_VALUE")
        void maximoEntero() {
            assertEquals(Integer.MAX_VALUE, UIUtils.normalizarDelayMs(Integer.MAX_VALUE), "assertEquals failed at UIUtilsTest.java:220");
        }
    }

    @Nested
    @DisplayName("crearMenuItemContextual")
    class CrearMenuItemContextualTests {

        @Test
        @DisplayName("aplica texto, tooltip y listener con estilo estándar")
        void configuraMenuItemCompleto() {
            JMenuItem item = UIUtils.crearMenuItemContextual("Reintentar", "Reintenta la acción", e -> {
            });

            assertEquals("Reintentar", item.getText(), "assertEquals failed at UIUtilsTest.java:236");
            assertEquals("Reintenta la acción", item.getToolTipText(), "assertEquals failed at UIUtilsTest.java:237");
            assertNotNull(item.getFont(), "assertNotNull failed at UIUtilsTest.java:238");
            assertEquals(1, item.getActionListeners().length, "assertEquals failed at UIUtilsTest.java:239");
        }
    }

    @Nested
    @DisplayName("AnchosColumnasTabla")
    class AnchosColumnasTablaTests {

        @Test
        @DisplayName("captura y restaura anchos preferidos usando helper centralizado")
        void capturaYRestauraAnchos() {
            JTable tabla = new JTable(1, 3);
            tabla.getColumnModel().getColumn(0).setPreferredWidth(120);
            tabla.getColumnModel().getColumn(1).setPreferredWidth(240);
            tabla.getColumnModel().getColumn(2).setPreferredWidth(360);

            int[] anchos = UIUtils.capturarAnchosColumnasTabla(tabla);

            assertArrayEquals(new int[]{120, 240, 360}, anchos,
                "assertArrayEquals failed at UIUtilsTest.java:254");

            UIUtils.restaurarAnchosColumnasTabla(tabla, 180, -1, 420);

            assertEquals(180, tabla.getColumnModel().getColumn(0).getPreferredWidth(),
                "assertEquals failed at UIUtilsTest.java:258");
            assertEquals(240, tabla.getColumnModel().getColumn(1).getPreferredWidth(),
                "assertEquals failed at UIUtilsTest.java:260");
            assertEquals(420, tabla.getColumnModel().getColumn(2).getPreferredWidth(),
                "assertEquals failed at UIUtilsTest.java:262");
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

    @Nested
    @DisplayName("convertirFilasVistaAModelo")
    class ConvertirFilasVistaAModeloTests {

        private JTable crearTablaSinFiltros(int filas) {
            javax.swing.table.DefaultTableModel modelo = new javax.swing.table.DefaultTableModel(filas, 1);
            return new JTable(modelo);
        }

        @Test
        @DisplayName("convierte filas de vista a modelo filtrando invÃ¡lidas y duplicados")
        void convierteFiltrandoInvalidasYDuplicados() {
            JTable tabla = crearTablaSinFiltros(5);
            int[] resultado = UIUtils.convertirFilasVistaAModelo(tabla, 0, 2, 2, -1, 99);
            assertArrayEquals(new int[]{0, 2}, resultado,
                "Debe filtrar Ã­ndices fuera de rango (-1, 99) y descartar duplicados (2,2)");
        }

        @Test
        @DisplayName("retorna array vacÃ­o para entrada nula o vacÃ­a")
        void retornaVacioParaEntradaNulaOVacia() {
            JTable tabla = crearTablaSinFiltros(3);
            assertArrayEquals(new int[0], UIUtils.convertirFilasVistaAModelo(tabla, (int[]) null),
                "Entrada null debe retornar array vacÃ­o");
            assertArrayEquals(new int[0], UIUtils.convertirFilasVistaAModelo(tabla),
                "Sin filas debe retornar array vacÃ­o");
        }

        @Test
        @DisplayName("retorna array vacÃ­o para tabla null")
        void retornaVacioParaTablaNull() {
            assertArrayEquals(new int[0], UIUtils.convertirFilasVistaAModelo(null, 0, 1),
                "Tabla null debe retornar array vacÃ­o de forma segura");
        }
    }

    @Nested
    @DisplayName("Empty state")
    class EmptyStateTest {

        @Test
        @DisplayName("crearEmptyState produce etiqueta centrada y oculta por defecto")
        void crearEmptyStateEsCentradaYOculta() {
            javax.swing.JLabel etiqueta = UIUtils.crearEmptyState("Sin datos");
            assertEquals(javax.swing.SwingConstants.CENTER, etiqueta.getHorizontalAlignment(),
                "assertEquals failed at UIUtilsTest.java:empty:alineacion");
            assertEquals("Sin datos", etiqueta.getText(),
                "assertEquals failed at UIUtilsTest.java:empty:texto");
            assertFalse(etiqueta.isVisible(),
                "assertFalse failed at UIUtilsTest.java:empty:oculta");
            assertFalse(etiqueta.isOpaque(),
                "assertFalse failed at UIUtilsTest.java:empty:noOpaca");
        }

        @Test
        @DisplayName("actualizarEmptyState muestra la etiqueta cuando la tabla está vacía")
        void actualizarEmptyStateMuestraEnVacio() {
            javax.swing.JTable tabla = new javax.swing.JTable(0, 1);
            javax.swing.JLabel etiqueta = UIUtils.crearEmptyState("vacio");
            UIUtils.actualizarEmptyState(tabla, etiqueta, "Sin filas");
            assertTrue(etiqueta.isVisible(),
                "assertTrue failed at UIUtilsTest.java:empty:visibleEnVacio");
            assertEquals("Sin filas", etiqueta.getText(),
                "assertEquals failed at UIUtilsTest.java:empty:mensajeVacio");
        }

        @Test
        @DisplayName("actualizarEmptyState oculta la etiqueta cuando la tabla tiene filas")
        void actualizarEmptyStateOcultaConFilas() {
            javax.swing.JTable tabla = new javax.swing.JTable(2, 1);
            javax.swing.JLabel etiqueta = UIUtils.crearEmptyState("vacio");
            etiqueta.setVisible(true);
            UIUtils.actualizarEmptyState(tabla, etiqueta, "Sin filas");
            assertFalse(etiqueta.isVisible(),
                "assertFalse failed at UIUtilsTest.java:empty:ocultaConFilas");
        }
    }

    @Nested
    @DisplayName("InputVerifiers")
    class InputVerifierTest {

        @Test
        @DisplayName("Verifier de URL acepta http/https con host y rechaza texto sin esquema")
        void verifierUrlValidaYInvalida() {
            javax.swing.InputVerifier verifier = UIUtils.crearInputVerifierUrl();
            javax.swing.JTextField valido = new javax.swing.JTextField("https://api.ejemplo.com");
            javax.swing.JTextField invalido = new javax.swing.JTextField("no-es-url");
            assertTrue(verifier.verify(valido),
                "assertTrue failed at UIUtilsTest.java:url:valida");
            assertFalse(verifier.verify(invalido),
                "assertFalse failed at UIUtilsTest.java:url:invalida");
        }

        @Test
        @DisplayName("Verifier de URL acepta vacío (la obligatoriedad se valida al guardar)")
        void verifierUrlAceptaVacio() {
            javax.swing.InputVerifier verifier = UIUtils.crearInputVerifierUrl();
            javax.swing.JTextField vacio = new javax.swing.JTextField("");
            assertTrue(verifier.verify(vacio),
                "assertTrue failed at UIUtilsTest.java:url:vacio");
        }

        @Test
        @DisplayName("Verifier numérico acepta dentro de rango y rechaza fuera de rango")
        void verifierNumericoRango() {
            javax.swing.InputVerifier verifier = UIUtils.crearInputVerifierNumerico(1, 10);
            javax.swing.JTextField dentro = new javax.swing.JTextField("5");
            javax.swing.JTextField fuera = new javax.swing.JTextField("50");
            javax.swing.JTextField noNumero = new javax.swing.JTextField("abc");
            assertTrue(verifier.verify(dentro),
                "assertTrue failed at UIUtilsTest.java:num:dentro");
            assertFalse(verifier.verify(fuera),
                "assertFalse failed at UIUtilsTest.java:num:fuera");
            assertFalse(verifier.verify(noNumero),
                "assertFalse failed at UIUtilsTest.java:num:noNumero");
        }

        @Test
        @DisplayName("Verifier de API key rechaza vacío/blancos")
        void verifierApiKeyRechazaVacio() {
            javax.swing.InputVerifier verifier = UIUtils.crearInputVerifierApiKey();
            javax.swing.JTextField vacio = new javax.swing.JTextField("   ");
            javax.swing.JTextField conValor = new javax.swing.JTextField("sk-abc123");
            assertFalse(verifier.verify(vacio),
                "assertFalse failed at UIUtilsTest.java:key:vacio");
            assertTrue(verifier.verify(conValor),
                "assertTrue failed at UIUtilsTest.java:key:conValor");
        }
    }

    @Nested
    @DisplayName("Tamaño mínimo de botón")
    class TamanoMinimoBotonTest {

        @Test
        @DisplayName("aplicarTamanoMinimoBoton fija mínimo y preferido consistentes")
        void aplicarTamanoMinimoBoton() {
            JButton boton = new JButton("→");
            UIUtils.aplicarTamanoMinimoBoton(boton, 44, 32);
            assertEquals(44, boton.getMinimumSize().width,
                "assertEquals failed at UIUtilsTest.java:tamano:minWidth");
            assertEquals(32, boton.getMinimumSize().height,
                "assertEquals failed at UIUtilsTest.java:tamano:minHeight");
            assertEquals(44, boton.getPreferredSize().width,
                "assertEquals failed at UIUtilsTest.java:tamano:prefWidth");
        }

        @Test
        @DisplayName("aplicarTamanoMinimoBoton no falla con botón null")
        void aplicarTamanoMinimoBotonNull() {
            assertDoesNotThrow(() -> UIUtils.aplicarTamanoMinimoBoton(null, 44, 32),
                "Botón null no debe lanzar excepción");
        }
    }

    @Nested
    @DisplayName("Opt-out global de alertas")
    class OptOutGlobalAlertasTests {

        @Test
        @DisplayName("alertasGloballyOn es true con config nula (por defecto se muestran)")
        void globalmenteOnConConfigNula() {
            UIUtils.configurarAlertas(null, null);
            assertTrue(UIUtils.alertasGloballyOn(),
                "Sin config instalada las alertas deben mostrarse");
        }

        @Test
        @DisplayName("alertasGloballyOn refleja el flag alertasHabilitadas de la config")
        void globalmenteOnReflejaFlag() {
            ConfiguracionAPI config = new ConfiguracionAPI();
            config.establecerAlertasHabilitadas(true);
            UIUtils.configurarAlertas(config, null);
            assertTrue(UIUtils.alertasGloballyOn(), "Con flag true debe estar on");

            config.establecerAlertasHabilitadas(false);
            assertFalse(UIUtils.alertasGloballyOn(), "Con flag false debe estar off");
        }

        @Test
        @DisplayName("aplicarOptOutGlobal baja el flag y dispara la persistencia una vez")
        void aplicarOptOutBajaFlagYPersiste() {
            ConfiguracionAPI config = new ConfiguracionAPI();
            config.establecerAlertasHabilitadas(true);
            AtomicInteger persistencias = new AtomicInteger(0);
            UIUtils.configurarAlertas(config, persistencias::incrementAndGet);

            UIUtils.aplicarOptOutGlobal();

            assertFalse(config.alertasHabilitadas(),
                "Tras opt-out el flag global debe quedar en false");
            assertEquals(1, persistencias.get(),
                "El opt-out debe disparar el guardado exactamente una vez");
        }

        @Test
        @DisplayName("aplicarOptOutGlobal es no-op seguro sin config instalada")
        void aplicarOptOutSinConfigNoLanza() {
            UIUtils.configurarAlertas(null, null);
            assertDoesNotThrow(UIUtils::aplicarOptOutGlobal);
        }
    }
}
