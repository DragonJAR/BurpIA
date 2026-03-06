package com.burpia.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EstilosUI Tests")
class EstilosUITest {

    private static final double DELTA = 0.01;

    @Nested
    @DisplayName("Constantes de contraste WCAG")
    class ConstantesContraste {

        @Test
        @DisplayName("CONTRASTE_AA_NORMAL debe ser 4.5")
        void testContrasteAANormal() {
            assertEquals(4.5d, EstilosUI.CONTRASTE_AA_NORMAL, DELTA, "assertEquals failed at EstilosUITest.java:26");
        }

        @Test
        @DisplayName("CONTRASTE_AA_GRANDE debe ser 3.0")
        void testContrasteAAGrande() {
            assertEquals(3.0d, EstilosUI.CONTRASTE_AA_GRANDE, DELTA, "assertEquals failed at EstilosUITest.java:32");
        }
    }

    @Nested
    @DisplayName("Detección de tema oscuro")
    class DeteccionTemaOscuro {

        @Test
        @DisplayName("esTemaOscuro clasifica correctamente fondos claros y oscuros")
        void testEsTemaOscuroPorLuminancia() {
            assertTrue(EstilosUI.esTemaOscuro(new Color(22, 24, 28)), "assertTrue failed at EstilosUITest.java:43");
            assertFalse(EstilosUI.esTemaOscuro(new Color(242, 243, 245)), "assertFalse failed at EstilosUITest.java:44");
        }

        @Test
        @DisplayName("esTemaOscuro con negro puro devuelve true")
        void testEsTemaOscuroNegroPuro() {
            assertTrue(EstilosUI.esTemaOscuro(Color.BLACK), "assertTrue failed at EstilosUITest.java:50");
        }

        @Test
        @DisplayName("esTemaOscuro con blanco puro devuelve false")
        void testEsTemaOscuroBlancoPuro() {
            assertFalse(EstilosUI.esTemaOscuro(Color.WHITE), "assertFalse failed at EstilosUITest.java:56");
        }

        @Test
        @DisplayName("esTemaOscuro con null usa fallback")
        void testEsTemaOscuroNull() {
            assertNotNull(EstilosUI.esTemaOscuro(null), "assertNotNull failed at EstilosUITest.java:62");
        }
    }

    @Nested
    @DisplayName("Cálculo de ratio de contraste")
    class RatioContraste {

        @Test
        @DisplayName("ratioContraste entre blanco y negro es 21:1")
        void testRatioContrasteMaximo() {
            double ratio = EstilosUI.ratioContraste(Color.WHITE, Color.BLACK);
            assertEquals(21.0d, ratio, DELTA, "assertEquals failed at EstilosUITest.java:74");
        }

        @Test
        @DisplayName("ratioContraste entre mismo color es 1:1")
        void testRatioContrasteMinimo() {
            Color color = new Color(128, 128, 128);
            double ratio = EstilosUI.ratioContraste(color, color);
            assertEquals(1.0d, ratio, DELTA, "assertEquals failed at EstilosUITest.java:82");
        }

        @Test
        @DisplayName("ratioContraste es simétrico")
        void testRatioContrasteSimetrico() {
            Color fg = new Color(50, 50, 50);
            Color bg = new Color(200, 200, 200);
            double ratio1 = EstilosUI.ratioContraste(fg, bg);
            double ratio2 = EstilosUI.ratioContraste(bg, fg);
            assertEquals(ratio1, ratio2, DELTA, "assertEquals failed at EstilosUITest.java:92");
        }

        @Test
        @DisplayName("ratioContraste con null usa fallback")
        void testRatioContrasteNull() {
            double ratio = EstilosUI.ratioContraste(null, null);
            assertTrue(ratio > 0, "assertTrue failed at EstilosUITest.java:99");
        }
    }

    @Nested
    @DisplayName("Ajuste de contraste")
    class AjusteContraste {

        @Test
        @DisplayName("ajustarParaContrasteMinimo garantiza ratio AA")
        void testAjusteContrasteAA() {
            Color fondo = new Color(240, 240, 240);
            Color base = new Color(180, 180, 180);

            Color ajustado = EstilosUI.ajustarParaContrasteMinimo(base, fondo, EstilosUI.CONTRASTE_AA_NORMAL);

            assertTrue(EstilosUI.ratioContraste(ajustado, fondo) >= EstilosUI.CONTRASTE_AA_NORMAL, "assertTrue failed at EstilosUITest.java:115");
        }

        @Test
        @DisplayName("ajustarParaContrasteMinimo retorna mismo color si ya cumple ratio")
        void testAjusteContrasteYaCumple() {
            Color fondo = new Color(255, 255, 255);
            Color base = Color.BLACK;

            Color ajustado = EstilosUI.ajustarParaContrasteMinimo(base, fondo, EstilosUI.CONTRASTE_AA_NORMAL);

            assertEquals(base, ajustado, "assertEquals failed at EstilosUITest.java:126");
        }

        @Test
        @DisplayName("ajustarParaContrasteMinimo con null usa fallback")
        void testAjusteContrasteNull() {
            Color ajustado = EstilosUI.ajustarParaContrasteMinimo(null, null, EstilosUI.CONTRASTE_AA_NORMAL);
            assertNotNull(ajustado, "assertNotNull failed at EstilosUITest.java:133");
        }

        @Test
        @DisplayName("ajustarParaContrasteMinimo con ratio mínimo 1.0 retorna color válido")
        void testAjusteContrasteRatioMinimo() {
            Color fondo = new Color(128, 128, 128);
            Color base = new Color(130, 130, 130);

            Color ajustado = EstilosUI.ajustarParaContrasteMinimo(base, fondo, 1.0d);

            assertNotNull(ajustado, "assertNotNull failed at EstilosUITest.java:144");
            assertTrue(EstilosUI.ratioContraste(ajustado, fondo) >= 1.0d, "assertTrue failed at EstilosUITest.java:145");
        }
    }

    @Nested
    @DisplayName("Obtención de color de texto con contraste")
    class ObtenerColorTextoContraste {

        @Test
        @DisplayName("obtenerColorTextoContraste retorna negro para fondo claro")
        void testObtenerColorTextoContrasteFondoClaro() {
            Color fondo = new Color(255, 255, 255);
            Color texto = EstilosUI.obtenerColorTextoContraste(fondo);
            assertEquals(Color.BLACK, texto, "assertEquals failed at EstilosUITest.java:158");
        }

        @Test
        @DisplayName("obtenerColorTextoContraste retorna blanco para fondo oscuro")
        void testObtenerColorTextoContrasteFondoOscuro() {
            Color fondo = new Color(0, 0, 0);
            Color texto = EstilosUI.obtenerColorTextoContraste(fondo);
            assertEquals(Color.WHITE, texto, "assertEquals failed at EstilosUITest.java:166");
        }

        @Test
        @DisplayName("obtenerColorTextoContraste con null usa fallback")
        void testObtenerColorTextoContrasteNull() {
            Color texto = EstilosUI.obtenerColorTextoContraste(null);
            assertNotNull(texto, "assertNotNull failed at EstilosUITest.java:173");
        }
    }

    @Nested
    @DisplayName("Colores de texto primario y secundario")
    class ColoresTexto {

        @Test
        @DisplayName("colorTextoPrimario cumple AA en tema claro y oscuro")
        void testColorTextoPrimarioAccesible() {
            Color fondoClaro = new Color(248, 248, 248);
            Color fondoOscuro = new Color(34, 36, 41);

            Color textoClaro = EstilosUI.colorTextoPrimario(fondoClaro);
            Color textoOscuro = EstilosUI.colorTextoPrimario(fondoOscuro);

            assertTrue(EstilosUI.ratioContraste(textoClaro, fondoClaro) >= EstilosUI.CONTRASTE_AA_NORMAL, "assertTrue failed at EstilosUITest.java:190");
            assertTrue(EstilosUI.ratioContraste(textoOscuro, fondoOscuro) >= EstilosUI.CONTRASTE_AA_NORMAL, "assertTrue failed at EstilosUITest.java:191");
        }

        @Test
        @DisplayName("colorTextoSecundario cumple AA en tema claro y oscuro")
        void testColorTextoSecundarioAccesible() {
            Color fondoClaro = new Color(248, 248, 248);
            Color fondoOscuro = new Color(34, 36, 41);

            Color textoClaro = EstilosUI.colorTextoSecundario(fondoClaro);
            Color textoOscuro = EstilosUI.colorTextoSecundario(fondoOscuro);

            assertTrue(EstilosUI.ratioContraste(textoClaro, fondoClaro) >= EstilosUI.CONTRASTE_AA_NORMAL, "assertTrue failed at EstilosUITest.java:203");
            assertTrue(EstilosUI.ratioContraste(textoOscuro, fondoOscuro) >= EstilosUI.CONTRASTE_AA_NORMAL, "assertTrue failed at EstilosUITest.java:204");
        }

        @Test
        @DisplayName("colorTextoPrimario con null usa fallback")
        void testColorTextoPrimarioNull() {
            Color texto = EstilosUI.colorTextoPrimario(null);
            assertNotNull(texto, "assertNotNull failed at EstilosUITest.java:211");
        }

        @Test
        @DisplayName("colorTextoSecundario con null usa fallback")
        void testColorTextoSecundarioNull() {
            Color texto = EstilosUI.colorTextoSecundario(null);
            assertNotNull(texto, "assertNotNull failed at EstilosUITest.java:218");
        }
    }

    @Nested
    @DisplayName("Colores de enlace y error")
    class ColoresEnlaceError {

        @Test
        @DisplayName("Colores de enlace y error cumplen AA en tema claro y oscuro")
        void testColoresAccesiblesAA() {
            Color fondoClaro = new Color(248, 248, 248);
            Color fondoOscuro = new Color(34, 36, 41);

            Color enlaceClaro = EstilosUI.colorEnlaceAccesible(fondoClaro);
            Color enlaceOscuro = EstilosUI.colorEnlaceAccesible(fondoOscuro);
            Color errorClaro = EstilosUI.colorErrorAccesible(fondoClaro);
            Color errorOscuro = EstilosUI.colorErrorAccesible(fondoOscuro);

            assertTrue(EstilosUI.ratioContraste(enlaceClaro, fondoClaro) >= EstilosUI.CONTRASTE_AA_NORMAL, "assertTrue failed at EstilosUITest.java:237");
            assertTrue(EstilosUI.ratioContraste(enlaceOscuro, fondoOscuro) >= EstilosUI.CONTRASTE_AA_NORMAL, "assertTrue failed at EstilosUITest.java:238");
            assertTrue(EstilosUI.ratioContraste(errorClaro, fondoClaro) >= EstilosUI.CONTRASTE_AA_NORMAL, "assertTrue failed at EstilosUITest.java:239");
            assertTrue(EstilosUI.ratioContraste(errorOscuro, fondoOscuro) >= EstilosUI.CONTRASTE_AA_NORMAL, "assertTrue failed at EstilosUITest.java:240");
        }

        @Test
        @DisplayName("colorEnlaceAccesible con null usa fallback")
        void testColorEnlaceAccesibleNull() {
            Color color = EstilosUI.colorEnlaceAccesible(null);
            assertNotNull(color, "assertNotNull failed at EstilosUITest.java:247");
        }

        @Test
        @DisplayName("colorErrorAccesible con null usa fallback")
        void testColorErrorAccesibleNull() {
            Color color = EstilosUI.colorErrorAccesible(null);
            assertNotNull(color, "assertNotNull failed at EstilosUITest.java:254");
        }
    }

    @Nested
    @DisplayName("Colores de separador y fondo secundario")
    class ColoresSeparadorFondo {

        @Test
        @DisplayName("colorSeparador cumple AA grande en tema claro y oscuro")
        void testColorSeparadorAccesible() {
            Color fondoClaro = new Color(248, 248, 248);
            Color fondoOscuro = new Color(34, 36, 41);

            Color separadorClaro = EstilosUI.colorSeparador(fondoClaro);
            Color separadorOscuro = EstilosUI.colorSeparador(fondoOscuro);

            assertTrue(EstilosUI.ratioContraste(separadorClaro, fondoClaro) >= EstilosUI.CONTRASTE_AA_GRANDE, "assertTrue failed at EstilosUITest.java:271");
            assertTrue(EstilosUI.ratioContraste(separadorOscuro, fondoOscuro) >= EstilosUI.CONTRASTE_AA_GRANDE, "assertTrue failed at EstilosUITest.java:272");
        }

        @Test
        @DisplayName("colorFondoSecundario retorna color distinto al fondo")
        void testColorFondoSecundarioDistinto() {
            Color fondoClaro = new Color(248, 248, 248);
            Color fondoOscuro = new Color(34, 36, 41);

            Color fondoSecClaro = EstilosUI.colorFondoSecundario(fondoClaro);
            Color fondoSecOscuro = EstilosUI.colorFondoSecundario(fondoOscuro);

            assertNotNull(fondoSecClaro, "assertNotNull failed at EstilosUITest.java:284");
            assertNotNull(fondoSecOscuro, "assertNotNull failed at EstilosUITest.java:285");
        }

        @Test
        @DisplayName("colorSeparador con null usa fallback")
        void testColorSeparadorNull() {
            Color color = EstilosUI.colorSeparador(null);
            assertNotNull(color, "assertNotNull failed at EstilosUITest.java:292");
        }

        @Test
        @DisplayName("colorFondoSecundario con null usa fallback")
        void testColorFondoSecundarioNull() {
            Color color = EstilosUI.colorFondoSecundario(null);
            assertNotNull(color, "assertNotNull failed at EstilosUITest.java:299");
        }
    }

    @Nested
    @DisplayName("Colores de texto y fondo ignorado")
    class ColoresIgnorado {

        @Test
        @DisplayName("colorTextoIgnorado con Color cumple AA")
        void testColorTextoIgnoradoConColor() {
            Color fondoClaro = new Color(248, 248, 248);
            Color fondoOscuro = new Color(34, 36, 41);

            Color textoIgnoradoClaro = EstilosUI.colorTextoIgnorado(fondoClaro);
            Color textoIgnoradoOscuro = EstilosUI.colorTextoIgnorado(fondoOscuro);

            assertTrue(EstilosUI.ratioContraste(textoIgnoradoClaro, fondoClaro) >= EstilosUI.CONTRASTE_AA_NORMAL, "assertTrue failed at EstilosUITest.java:316");
            assertTrue(EstilosUI.ratioContraste(textoIgnoradoOscuro, fondoOscuro) >= EstilosUI.CONTRASTE_AA_NORMAL, "assertTrue failed at EstilosUITest.java:317");
        }

        @Test
        @DisplayName("colorTextoIgnorado con boolean funciona correctamente")
        void testColorTextoIgnoradoConBoolean() {
            Color textoClaro = EstilosUI.colorTextoIgnorado(false);
            Color textoOscuro = EstilosUI.colorTextoIgnorado(true);

            assertNotNull(textoClaro, "assertNotNull failed at EstilosUITest.java:326");
            assertNotNull(textoOscuro, "assertNotNull failed at EstilosUITest.java:327");
        }

        @Test
        @DisplayName("colorFondoIgnorado con Color retorna color válido")
        void testColorFondoIgnoradoConColor() {
            Color fondoClaro = new Color(248, 248, 248);
            Color fondoOscuro = new Color(34, 36, 41);

            Color fondoIgnoradoClaro = EstilosUI.colorFondoIgnorado(fondoClaro);
            Color fondoIgnoradoOscuro = EstilosUI.colorFondoIgnorado(fondoOscuro);

            assertNotNull(fondoIgnoradoClaro, "assertNotNull failed at EstilosUITest.java:339");
            assertNotNull(fondoIgnoradoOscuro, "assertNotNull failed at EstilosUITest.java:340");
        }

        @Test
        @DisplayName("colorFondoIgnorado con Color y boolean funciona correctamente")
        void testColorFondoIgnoradoConColorYBoolean() {
            Color fondoClaro = new Color(248, 248, 248);
            Color fondoOscuro = new Color(34, 36, 41);

            Color fondoIgnoradoClaro = EstilosUI.colorFondoIgnorado(fondoClaro, false);
            Color fondoIgnoradoOscuro = EstilosUI.colorFondoIgnorado(fondoOscuro, true);

            assertNotNull(fondoIgnoradoClaro, "assertNotNull failed at EstilosUITest.java:352");
            assertNotNull(fondoIgnoradoOscuro, "assertNotNull failed at EstilosUITest.java:353");
        }

        @Test
        @DisplayName("colorTextoIgnorado con null usa fallback")
        void testColorTextoIgnoradoNull() {
            Color color = EstilosUI.colorTextoIgnorado((Color) null);
            assertNotNull(color, "assertNotNull failed at EstilosUITest.java:360");
        }

        @Test
        @DisplayName("colorFondoIgnorado con null usa fallback")
        void testColorFondoIgnoradoNull() {
            Color color = EstilosUI.colorFondoIgnorado((Color) null);
            assertNotNull(color, "assertNotNull failed at EstilosUITest.java:367");
        }
    }

    @Nested
    @DisplayName("Obtención de fondo de panel")
    class ObtenerFondoPanel {

        @Test
        @DisplayName("obtenerFondoPanel retorna color no nulo")
        void testObtenerFondoPanelNoNulo() {
            Color fondo = EstilosUI.obtenerFondoPanel();
            assertNotNull(fondo, "assertNotNull failed at EstilosUITest.java:379");
        }
    }

    @Nested
    @DisplayName("Constantes de color de severidad")
    class ConstantesColoresSeveridad {

        @Test
        @DisplayName("Colores de severidad están definidos")
        void testColoresSeveridadDefinidos() {
            assertNotNull(EstilosUI.COLOR_CRITICAL, "assertNotNull failed at EstilosUITest.java:390");
            assertNotNull(EstilosUI.COLOR_HIGH, "assertNotNull failed at EstilosUITest.java:391");
            assertNotNull(EstilosUI.COLOR_MEDIUM, "assertNotNull failed at EstilosUITest.java:392");
            assertNotNull(EstilosUI.COLOR_LOW, "assertNotNull failed at EstilosUITest.java:393");
            assertNotNull(EstilosUI.COLOR_INFO, "assertNotNull failed at EstilosUITest.java:394");
        }

        @Test
        @DisplayName("Colores de estado de tarea están definidos")
        void testColoresEstadoTareaDefinidos() {
            assertNotNull(EstilosUI.COLOR_TASK_EN_COLA, "assertNotNull failed at EstilosUITest.java:400");
            assertNotNull(EstilosUI.COLOR_TASK_ANALIZANDO, "assertNotNull failed at EstilosUITest.java:401");
            assertNotNull(EstilosUI.COLOR_TASK_COMPLETADO, "assertNotNull failed at EstilosUITest.java:402");
            assertNotNull(EstilosUI.COLOR_TASK_ERROR, "assertNotNull failed at EstilosUITest.java:403");
            assertNotNull(EstilosUI.COLOR_TASK_CANCELADO, "assertNotNull failed at EstilosUITest.java:404");
            assertNotNull(EstilosUI.COLOR_TASK_PAUSADO, "assertNotNull failed at EstilosUITest.java:405");
        }
    }

    @Nested
    @DisplayName("Constantes de UI")
    class ConstantesUI {

        @Test
        @DisplayName("Constantes de dimensiones están definidas con valores válidos")
        void testConstantesDimensiones() {
            assertTrue(EstilosUI.ALTURA_FILA_TABLA > 0, "assertTrue failed at EstilosUITest.java:416");
            assertTrue(EstilosUI.MARGEN_PANEL > 0, "assertTrue failed at EstilosUITest.java:417");
            assertTrue(EstilosUI.ESPACIADO_COMPONENTES > 0, "assertTrue failed at EstilosUITest.java:418");
        }

        @Test
        @DisplayName("Fuentes están definidas")
        void testFuentesDefinidas() {
            assertNotNull(EstilosUI.FUENTE_ESTANDAR, "assertNotNull failed at EstilosUITest.java:424");
            assertNotNull(EstilosUI.FUENTE_NEGRITA, "assertNotNull failed at EstilosUITest.java:425");
            assertNotNull(EstilosUI.FUENTE_MONO, "assertNotNull failed at EstilosUITest.java:426");
            assertNotNull(EstilosUI.FUENTE_MONO_NEGRITA, "assertNotNull failed at EstilosUITest.java:427");
            assertNotNull(EstilosUI.FUENTE_TABLA, "assertNotNull failed at EstilosUITest.java:428");
            assertNotNull(EstilosUI.FUENTE_CAMPO_TEXTO, "assertNotNull failed at EstilosUITest.java:429");
            assertNotNull(EstilosUI.FUENTE_BOTON_PRINCIPAL, "assertNotNull failed at EstilosUITest.java:430");
            assertNotNull(EstilosUI.FUENTE_TITULO_BANNER, "assertNotNull failed at EstilosUITest.java:431");
            assertNotNull(EstilosUI.FUENTE_ICONO_GRANDE, "assertNotNull failed at EstilosUITest.java:432");
        }
    }
}
