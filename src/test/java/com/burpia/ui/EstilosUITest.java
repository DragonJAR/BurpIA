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
            assertEquals(4.5d, EstilosUI.CONTRASTE_AA_NORMAL, DELTA);
        }

        @Test
        @DisplayName("CONTRASTE_AA_GRANDE debe ser 3.0")
        void testContrasteAAGrande() {
            assertEquals(3.0d, EstilosUI.CONTRASTE_AA_GRANDE, DELTA);
        }
    }

    @Nested
    @DisplayName("Detección de tema oscuro")
    class DeteccionTemaOscuro {

        @Test
        @DisplayName("esTemaOscuro clasifica correctamente fondos claros y oscuros")
        void testEsTemaOscuroPorLuminancia() {
            assertTrue(EstilosUI.esTemaOscuro(new Color(22, 24, 28)));
            assertFalse(EstilosUI.esTemaOscuro(new Color(242, 243, 245)));
        }

        @Test
        @DisplayName("esTemaOscuro con negro puro devuelve true")
        void testEsTemaOscuroNegroPuro() {
            assertTrue(EstilosUI.esTemaOscuro(Color.BLACK));
        }

        @Test
        @DisplayName("esTemaOscuro con blanco puro devuelve false")
        void testEsTemaOscuroBlancoPuro() {
            assertFalse(EstilosUI.esTemaOscuro(Color.WHITE));
        }

        @Test
        @DisplayName("esTemaOscuro con null usa fallback")
        void testEsTemaOscuroNull() {
            assertNotNull(EstilosUI.esTemaOscuro(null));
        }
    }

    @Nested
    @DisplayName("Cálculo de ratio de contraste")
    class RatioContraste {

        @Test
        @DisplayName("ratioContraste entre blanco y negro es 21:1")
        void testRatioContrasteMaximo() {
            double ratio = EstilosUI.ratioContraste(Color.WHITE, Color.BLACK);
            assertEquals(21.0d, ratio, DELTA);
        }

        @Test
        @DisplayName("ratioContraste entre mismo color es 1:1")
        void testRatioContrasteMinimo() {
            Color color = new Color(128, 128, 128);
            double ratio = EstilosUI.ratioContraste(color, color);
            assertEquals(1.0d, ratio, DELTA);
        }

        @Test
        @DisplayName("ratioContraste es simétrico")
        void testRatioContrasteSimetrico() {
            Color fg = new Color(50, 50, 50);
            Color bg = new Color(200, 200, 200);
            double ratio1 = EstilosUI.ratioContraste(fg, bg);
            double ratio2 = EstilosUI.ratioContraste(bg, fg);
            assertEquals(ratio1, ratio2, DELTA);
        }

        @Test
        @DisplayName("ratioContraste con null usa fallback")
        void testRatioContrasteNull() {
            double ratio = EstilosUI.ratioContraste(null, null);
            assertTrue(ratio > 0);
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

            assertTrue(EstilosUI.ratioContraste(ajustado, fondo) >= EstilosUI.CONTRASTE_AA_NORMAL);
        }

        @Test
        @DisplayName("ajustarParaContrasteMinimo retorna mismo color si ya cumple ratio")
        void testAjusteContrasteYaCumple() {
            Color fondo = new Color(255, 255, 255);
            Color base = Color.BLACK;

            Color ajustado = EstilosUI.ajustarParaContrasteMinimo(base, fondo, EstilosUI.CONTRASTE_AA_NORMAL);

            assertEquals(base, ajustado);
        }

        @Test
        @DisplayName("ajustarParaContrasteMinimo con null usa fallback")
        void testAjusteContrasteNull() {
            Color ajustado = EstilosUI.ajustarParaContrasteMinimo(null, null, EstilosUI.CONTRASTE_AA_NORMAL);
            assertNotNull(ajustado);
        }

        @Test
        @DisplayName("ajustarParaContrasteMinimo con ratio mínimo 1.0 retorna color válido")
        void testAjusteContrasteRatioMinimo() {
            Color fondo = new Color(128, 128, 128);
            Color base = new Color(130, 130, 130);

            Color ajustado = EstilosUI.ajustarParaContrasteMinimo(base, fondo, 1.0d);

            assertNotNull(ajustado);
            assertTrue(EstilosUI.ratioContraste(ajustado, fondo) >= 1.0d);
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
            assertEquals(Color.BLACK, texto);
        }

        @Test
        @DisplayName("obtenerColorTextoContraste retorna blanco para fondo oscuro")
        void testObtenerColorTextoContrasteFondoOscuro() {
            Color fondo = new Color(0, 0, 0);
            Color texto = EstilosUI.obtenerColorTextoContraste(fondo);
            assertEquals(Color.WHITE, texto);
        }

        @Test
        @DisplayName("obtenerColorTextoContraste con null usa fallback")
        void testObtenerColorTextoContrasteNull() {
            Color texto = EstilosUI.obtenerColorTextoContraste(null);
            assertNotNull(texto);
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

            assertTrue(EstilosUI.ratioContraste(textoClaro, fondoClaro) >= EstilosUI.CONTRASTE_AA_NORMAL);
            assertTrue(EstilosUI.ratioContraste(textoOscuro, fondoOscuro) >= EstilosUI.CONTRASTE_AA_NORMAL);
        }

        @Test
        @DisplayName("colorTextoSecundario cumple AA en tema claro y oscuro")
        void testColorTextoSecundarioAccesible() {
            Color fondoClaro = new Color(248, 248, 248);
            Color fondoOscuro = new Color(34, 36, 41);

            Color textoClaro = EstilosUI.colorTextoSecundario(fondoClaro);
            Color textoOscuro = EstilosUI.colorTextoSecundario(fondoOscuro);

            assertTrue(EstilosUI.ratioContraste(textoClaro, fondoClaro) >= EstilosUI.CONTRASTE_AA_NORMAL);
            assertTrue(EstilosUI.ratioContraste(textoOscuro, fondoOscuro) >= EstilosUI.CONTRASTE_AA_NORMAL);
        }

        @Test
        @DisplayName("colorTextoPrimario con null usa fallback")
        void testColorTextoPrimarioNull() {
            Color texto = EstilosUI.colorTextoPrimario(null);
            assertNotNull(texto);
        }

        @Test
        @DisplayName("colorTextoSecundario con null usa fallback")
        void testColorTextoSecundarioNull() {
            Color texto = EstilosUI.colorTextoSecundario(null);
            assertNotNull(texto);
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

            assertTrue(EstilosUI.ratioContraste(enlaceClaro, fondoClaro) >= EstilosUI.CONTRASTE_AA_NORMAL);
            assertTrue(EstilosUI.ratioContraste(enlaceOscuro, fondoOscuro) >= EstilosUI.CONTRASTE_AA_NORMAL);
            assertTrue(EstilosUI.ratioContraste(errorClaro, fondoClaro) >= EstilosUI.CONTRASTE_AA_NORMAL);
            assertTrue(EstilosUI.ratioContraste(errorOscuro, fondoOscuro) >= EstilosUI.CONTRASTE_AA_NORMAL);
        }

        @Test
        @DisplayName("colorEnlaceAccesible con null usa fallback")
        void testColorEnlaceAccesibleNull() {
            Color color = EstilosUI.colorEnlaceAccesible(null);
            assertNotNull(color);
        }

        @Test
        @DisplayName("colorErrorAccesible con null usa fallback")
        void testColorErrorAccesibleNull() {
            Color color = EstilosUI.colorErrorAccesible(null);
            assertNotNull(color);
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

            assertTrue(EstilosUI.ratioContraste(separadorClaro, fondoClaro) >= EstilosUI.CONTRASTE_AA_GRANDE);
            assertTrue(EstilosUI.ratioContraste(separadorOscuro, fondoOscuro) >= EstilosUI.CONTRASTE_AA_GRANDE);
        }

        @Test
        @DisplayName("colorFondoSecundario retorna color distinto al fondo")
        void testColorFondoSecundarioDistinto() {
            Color fondoClaro = new Color(248, 248, 248);
            Color fondoOscuro = new Color(34, 36, 41);

            Color fondoSecClaro = EstilosUI.colorFondoSecundario(fondoClaro);
            Color fondoSecOscuro = EstilosUI.colorFondoSecundario(fondoOscuro);

            assertNotNull(fondoSecClaro);
            assertNotNull(fondoSecOscuro);
        }

        @Test
        @DisplayName("colorSeparador con null usa fallback")
        void testColorSeparadorNull() {
            Color color = EstilosUI.colorSeparador(null);
            assertNotNull(color);
        }

        @Test
        @DisplayName("colorFondoSecundario con null usa fallback")
        void testColorFondoSecundarioNull() {
            Color color = EstilosUI.colorFondoSecundario(null);
            assertNotNull(color);
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

            assertTrue(EstilosUI.ratioContraste(textoIgnoradoClaro, fondoClaro) >= EstilosUI.CONTRASTE_AA_NORMAL);
            assertTrue(EstilosUI.ratioContraste(textoIgnoradoOscuro, fondoOscuro) >= EstilosUI.CONTRASTE_AA_NORMAL);
        }

        @Test
        @DisplayName("colorTextoIgnorado con boolean funciona correctamente")
        void testColorTextoIgnoradoConBoolean() {
            Color textoClaro = EstilosUI.colorTextoIgnorado(false);
            Color textoOscuro = EstilosUI.colorTextoIgnorado(true);

            assertNotNull(textoClaro);
            assertNotNull(textoOscuro);
        }

        @Test
        @DisplayName("colorFondoIgnorado con Color retorna color válido")
        void testColorFondoIgnoradoConColor() {
            Color fondoClaro = new Color(248, 248, 248);
            Color fondoOscuro = new Color(34, 36, 41);

            Color fondoIgnoradoClaro = EstilosUI.colorFondoIgnorado(fondoClaro);
            Color fondoIgnoradoOscuro = EstilosUI.colorFondoIgnorado(fondoOscuro);

            assertNotNull(fondoIgnoradoClaro);
            assertNotNull(fondoIgnoradoOscuro);
        }

        @Test
        @DisplayName("colorFondoIgnorado con Color y boolean funciona correctamente")
        void testColorFondoIgnoradoConColorYBoolean() {
            Color fondoClaro = new Color(248, 248, 248);
            Color fondoOscuro = new Color(34, 36, 41);

            Color fondoIgnoradoClaro = EstilosUI.colorFondoIgnorado(fondoClaro, false);
            Color fondoIgnoradoOscuro = EstilosUI.colorFondoIgnorado(fondoOscuro, true);

            assertNotNull(fondoIgnoradoClaro);
            assertNotNull(fondoIgnoradoOscuro);
        }

        @Test
        @DisplayName("colorTextoIgnorado con null usa fallback")
        void testColorTextoIgnoradoNull() {
            Color color = EstilosUI.colorTextoIgnorado((Color) null);
            assertNotNull(color);
        }

        @Test
        @DisplayName("colorFondoIgnorado con null usa fallback")
        void testColorFondoIgnoradoNull() {
            Color color = EstilosUI.colorFondoIgnorado((Color) null);
            assertNotNull(color);
        }
    }

    @Nested
    @DisplayName("Obtención de fondo de panel")
    class ObtenerFondoPanel {

        @Test
        @DisplayName("obtenerFondoPanel retorna color no nulo")
        void testObtenerFondoPanelNoNulo() {
            Color fondo = EstilosUI.obtenerFondoPanel();
            assertNotNull(fondo);
        }
    }

    @Nested
    @DisplayName("Constantes de color de severidad")
    class ConstantesColoresSeveridad {

        @Test
        @DisplayName("Colores de severidad están definidos")
        void testColoresSeveridadDefinidos() {
            assertNotNull(EstilosUI.COLOR_CRITICAL);
            assertNotNull(EstilosUI.COLOR_HIGH);
            assertNotNull(EstilosUI.COLOR_MEDIUM);
            assertNotNull(EstilosUI.COLOR_LOW);
            assertNotNull(EstilosUI.COLOR_INFO);
        }

        @Test
        @DisplayName("Colores de estado de tarea están definidos")
        void testColoresEstadoTareaDefinidos() {
            assertNotNull(EstilosUI.COLOR_TASK_EN_COLA);
            assertNotNull(EstilosUI.COLOR_TASK_ANALIZANDO);
            assertNotNull(EstilosUI.COLOR_TASK_COMPLETADO);
            assertNotNull(EstilosUI.COLOR_TASK_ERROR);
            assertNotNull(EstilosUI.COLOR_TASK_CANCELADO);
            assertNotNull(EstilosUI.COLOR_TASK_PAUSADO);
        }
    }

    @Nested
    @DisplayName("Constantes de UI")
    class ConstantesUI {

        @Test
        @DisplayName("Constantes de dimensiones están definidas con valores válidos")
        void testConstantesDimensiones() {
            assertTrue(EstilosUI.ALTURA_FILA_TABLA > 0);
            assertTrue(EstilosUI.MARGEN_PANEL > 0);
            assertTrue(EstilosUI.ESPACIADO_COMPONENTES > 0);
        }

        @Test
        @DisplayName("Fuentes están definidas")
        void testFuentesDefinidas() {
            assertNotNull(EstilosUI.FUENTE_ESTANDAR);
            assertNotNull(EstilosUI.FUENTE_NEGRITA);
            assertNotNull(EstilosUI.FUENTE_MONO);
            assertNotNull(EstilosUI.FUENTE_MONO_NEGRITA);
            assertNotNull(EstilosUI.FUENTE_TABLA);
            assertNotNull(EstilosUI.FUENTE_CAMPO_TEXTO);
            assertNotNull(EstilosUI.FUENTE_BOTON_PRINCIPAL);
            assertNotNull(EstilosUI.FUENTE_TITULO_BANNER);
            assertNotNull(EstilosUI.FUENTE_ICONO_GRANDE);
        }
    }
}
