package com.burpia.ui;

import com.burpia.config.AgenteTipo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("SubmitSequenceFactory Tests")
class SubmitSequenceFactoryTest {

    @Nested
    @DisplayName("construir para Claude Code")
    class ClaudeCodeTests {

        @Test
        @DisplayName("en Linux/Mac usa CR")
        void linuxUsaCr() {
            SubmitSequenceFactory.SubmitSequence secuencia = SubmitSequenceFactory.construir(
                AgenteTipo.CLAUDE_CODE,
                SubmitSequenceFactory.Plataforma.LINUX
            );
            assertEquals("\r", secuencia.payload(), "assertEquals failed at SubmitSequenceFactoryTest.java:27");
            assertEquals(1, secuencia.repeticiones(), "assertEquals failed at SubmitSequenceFactoryTest.java:28");
        }

        @Test
        @DisplayName("en Windows usa CRLF")
        void windowsUsaCrlf() {
            SubmitSequenceFactory.SubmitSequence secuencia = SubmitSequenceFactory.construir(
                AgenteTipo.CLAUDE_CODE,
                SubmitSequenceFactory.Plataforma.WINDOWS
            );
            assertEquals("\r\n", secuencia.payload(), "assertEquals failed at SubmitSequenceFactoryTest.java:38");
            assertEquals(1, secuencia.repeticiones(), "assertEquals failed at SubmitSequenceFactoryTest.java:39");
        }

        @Test
        @DisplayName("en Mac usa CR (igual que Linux)")
        void macUsaCr() {
            SubmitSequenceFactory.SubmitSequence secuencia = SubmitSequenceFactory.construir(
                AgenteTipo.CLAUDE_CODE,
                SubmitSequenceFactory.Plataforma.MAC
            );
            assertEquals("\r", secuencia.payload(), "assertEquals failed at SubmitSequenceFactoryTest.java:49");
            assertEquals(1, secuencia.repeticiones(), "assertEquals failed at SubmitSequenceFactoryTest.java:50");
        }
    }

    @Nested
    @DisplayName("construir para Factory Droid")
    class FactoryDroidTests {

        @Test
        @DisplayName("usa SMART_FALLBACK con cadena CR -> LF -> CRLF")
        void usaSmartFallback() {
            SubmitSequenceFactory.SubmitSequence secuencia = SubmitSequenceFactory.construir(
                AgenteTipo.FACTORY_DROID,
                SubmitSequenceFactory.Plataforma.LINUX
            );

            assertCadenaSmartFallback(secuencia);
        }
    }

    @Nested
    @DisplayName("construir para Gemini CLI")
    class GeminiCliTests {

        @Test
        @DisplayName("usa SMART_FALLBACK igual que Factory Droid")
        void usaSmartFallback() {
            SubmitSequenceFactory.SubmitSequence secuencia = SubmitSequenceFactory.construir(
                AgenteTipo.GEMINI_CLI,
                SubmitSequenceFactory.Plataforma.LINUX
            );

            assertCadenaSmartFallback(secuencia);
        }
    }

    @Nested
    @DisplayName("Casos borde y valores nulos")
    class CasosBordeTests {

        @Test
        @DisplayName("construir con tipoAgente null usa FACTORY_DROID por defecto")
        void tipoAgenteNuloUsaFactoryDroid() {
            SubmitSequenceFactory.SubmitSequence secuencia = SubmitSequenceFactory.construir(
                null,
                SubmitSequenceFactory.Plataforma.LINUX
            );

            assertCadenaSmartFallback(secuencia);
        }

        @Test
        @DisplayName("construir con plataforma null usa LINUX por defecto")
        void plataformaNulaUsaLinux() {
            SubmitSequenceFactory.SubmitSequence secuencia = SubmitSequenceFactory.construir(
                AgenteTipo.CLAUDE_CODE,
                null
            );

            assertEquals("\r", secuencia.payload(), "assertEquals failed at SubmitSequenceFactoryTest.java:109");
        }

        @Test
        @DisplayName("construir sin plataforma detecta sistema actual")
        void construirSinPlataforma() {
            SubmitSequenceFactory.SubmitSequence secuencia = SubmitSequenceFactory.construir(
                AgenteTipo.FACTORY_DROID
            );

            assertNotNull(secuencia, "assertNotNull failed at SubmitSequenceFactoryTest.java:119");
            assertNotNull(secuencia.payload(), "assertNotNull failed at SubmitSequenceFactoryTest.java:120");
        }
    }

    @Nested
    @DisplayName("SubmitSequence")
    class SubmitSequenceTests {

        @Test
        @DisplayName("descripcion incluye estrategia y repeticiones")
        void descripcion() {
            SubmitSequenceFactory.SubmitSequence secuencia = SubmitSequenceFactory.construir(
                AgenteTipo.CLAUDE_CODE,
                SubmitSequenceFactory.Plataforma.LINUX
            );

            assertEquals("AUTO_CLAUDE x1", secuencia.descripcion(), "assertEquals failed at SubmitSequenceFactoryTest.java:136");
        }

        @Test
        @DisplayName("delayEntreEnviosMs retorna valor configurado")
        void delayEntreEnviosMs() {
            SubmitSequenceFactory.SubmitSequence secuencia = SubmitSequenceFactory.construir(
                AgenteTipo.FACTORY_DROID,
                SubmitSequenceFactory.Plataforma.LINUX
            );

            assertEquals(0, secuencia.delayEntreEnviosMs(), "assertEquals failed at SubmitSequenceFactoryTest.java:147");
        }

        @Test
        @DisplayName("conFallback con payload null retorna this sin modificar")
        void conFallbackNuloRetornaThis() {
            SubmitSequenceFactory.SubmitSequence secuencia = SubmitSequenceFactory.construir(
                AgenteTipo.CLAUDE_CODE,
                SubmitSequenceFactory.Plataforma.LINUX
            );

            SubmitSequenceFactory.SubmitSequence resultado = secuencia.conFallback(null, 1, 100);

            assertSame(secuencia, resultado, "assertSame failed at SubmitSequenceFactoryTest.java:160");
            assertNull(secuencia.getFallback(), "assertNull failed at SubmitSequenceFactoryTest.java:161");
        }

        @Test
        @DisplayName("repeticiones se normaliza a minimo 1")
        void repeticionesSeNormaliza() {
            SubmitSequenceFactory.SubmitSequence secuencia = new SubmitSequenceFactory.SubmitSequence(
                "\r", 0, 0, "TEST"
            );

            assertEquals(1, secuencia.repeticiones(), "assertEquals failed at SubmitSequenceFactoryTest.java:171");
        }

        @Test
        @DisplayName("delayEntreEnviosMs se normaliza a minimo 0")
        void delaySeNormaliza() {
            SubmitSequenceFactory.SubmitSequence secuencia = new SubmitSequenceFactory.SubmitSequence(
                "\r", 1, -50, "TEST"
            );

            assertEquals(0, secuencia.delayEntreEnviosMs(), "assertEquals failed at SubmitSequenceFactoryTest.java:181");
        }
    }

    @Nested
    @DisplayName("Plataforma enum")
    class PlataformaTests {

        @Test
        @DisplayName("desdeSistemaActual retorna valor valido")
        void desdeSistemaActualRetornaValorValido() {
            SubmitSequenceFactory.Plataforma plataforma = SubmitSequenceFactory.Plataforma.desdeSistemaActual();

            assertNotNull(plataforma, "assertNotNull failed at SubmitSequenceFactoryTest.java:194");
        }
    }

    /**
     * Verifica que la secuencia tiene la cadena SMART_FALLBACK correcta:
     * CR -> LF -> CRLF
     */
    private static void assertCadenaSmartFallback(SubmitSequenceFactory.SubmitSequence secuencia) {
        assertEquals("\r", secuencia.payload(), "assertEquals failed at SubmitSequenceFactoryTest.java:203");
        assertEquals(1, secuencia.repeticiones(), "assertEquals failed at SubmitSequenceFactoryTest.java:204");

        assertNotNull(secuencia.getFallback(), "Primer fallback no debe ser null");
        assertEquals("\n", secuencia.getFallback().payload(), "assertEquals failed at SubmitSequenceFactoryTest.java:207");

        assertNotNull(secuencia.getFallback().getFallback(), "Segundo fallback no debe ser null");
        assertEquals("\r\n", secuencia.getFallback().getFallback().payload(), "assertEquals failed at SubmitSequenceFactoryTest.java:210");
    }
}
