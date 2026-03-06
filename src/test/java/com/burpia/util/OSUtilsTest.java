package com.burpia.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OSUtils Tests")
class OSUtilsTest {

    @Nested
    @DisplayName("prepararComando")
    class PrepararComando {

        @Test
        @DisplayName("Retorna null para entrada null")
        void retornaNullParaNull() {
            assertNull(OSUtils.prepararComando(null), "assertNull failed at OSUtilsTest.java:33");
        }

        @Test
        @DisplayName("Agrega EOL a cadena vacía")
        void agregaEolACadenaVacia() {
            String resultado = OSUtils.prepararComando("");
            assertNotNull(resultado, "assertNotNull failed at OSUtilsTest.java:40");
            assertTrue(resultado.contains(OSUtils.obtenerEolTerminal()), "assertTrue failed at OSUtilsTest.java:41");
        }

        @Test
        @DisplayName("No modifica comando que ya termina con \\n")
        void noModificaComandoConSaltoLineaUnix() {
            String comando = "echo hola\n";
            assertEquals(comando, OSUtils.prepararComando(comando), "assertEquals failed at OSUtilsTest.java:48");
        }

        @Test
        @DisplayName("No modifica comando que ya termina con \\r")
        void noModificaComandoConRetornoCarro() {
            String comando = "echo hola\r";
            assertEquals(comando, OSUtils.prepararComando(comando), "assertEquals failed at OSUtilsTest.java:55");
        }

        @Test
        @DisplayName("No modifica comando que ya termina con \\r\\n")
        void noModificaComandoConEolWindows() {
            String comando = "echo hola\r\n";
            assertEquals(comando, OSUtils.prepararComando(comando), "assertEquals failed at OSUtilsTest.java:62");
        }

        @Test
        @DisplayName("Agrega EOL a comando sin salto de línea")
        void agregaEolAComandoSinSalto() {
            String comando = "echo hola";
            String resultado = OSUtils.prepararComando(comando);
            assertEquals(comando + OSUtils.obtenerEolTerminal(), resultado, "assertEquals failed at OSUtilsTest.java:70");
        }
    }

    @Nested
    @DisplayName("obtenerEolTerminal")
    class ObtenerEolTerminal {

        @Test
        @EnabledOnOs(OS.WINDOWS)
        @DisplayName("Retorna \\r\\n en Windows")
        void retornaCrLfEnWindows() {
            assertEquals("\r\n", OSUtils.obtenerEolTerminal(), "assertEquals failed at OSUtilsTest.java:82");
        }

        @Test
        @EnabledOnOs({OS.LINUX, OS.MAC})
        @DisplayName("Retorna \\n en Unix/Mac")
        void retornaLfEnUnix() {
            assertEquals("\n", OSUtils.obtenerEolTerminal(), "assertEquals failed at OSUtilsTest.java:89");
        }
    }

    @Nested
    @DisplayName("esWindows")
    class EsWindows {

        @Test
        @EnabledOnOs(OS.WINDOWS)
        @DisplayName("Retorna true en Windows")
        void retornaTrueEnWindows() {
            assertTrue(OSUtils.esWindows(), "assertTrue failed at OSUtilsTest.java:101");
        }

        @Test
        @EnabledOnOs({OS.LINUX, OS.MAC})
        @DisplayName("Retorna false en Unix/Mac")
        void retornaFalseEnUnix() {
            assertFalse(OSUtils.esWindows(), "assertFalse failed at OSUtilsTest.java:108");
        }
    }

    @Nested
    @DisplayName("esMac")
    class EsMac {

        @Test
        @EnabledOnOs(OS.MAC)
        @DisplayName("Retorna true en macOS")
        void retornaTrueEnMac() {
            assertTrue(OSUtils.esMac(), "assertTrue failed at OSUtilsTest.java:120");
        }

        @Test
        @EnabledOnOs({OS.LINUX, OS.WINDOWS})
        @DisplayName("Retorna false en no-macOS")
        void retornaFalseEnNoMac() {
            assertFalse(OSUtils.esMac(), "assertFalse failed at OSUtilsTest.java:127");
        }
    }

    @Nested
    @DisplayName("esLinux")
    class EsLinux {

        @Test
        @EnabledOnOs(OS.LINUX)
        @DisplayName("Retorna true en Linux")
        void retornaTrueEnLinux() {
            assertTrue(OSUtils.esLinux(), "assertTrue failed at OSUtilsTest.java:139");
        }

        @Test
        @EnabledOnOs({OS.MAC, OS.WINDOWS})
        @DisplayName("Retorna false en no-Linux")
        void retornaFalseEnNoLinux() {
            assertFalse(OSUtils.esLinux(), "assertFalse failed at OSUtilsTest.java:146");
        }
    }

    @Nested
    @DisplayName("extraerEjecutableComando")
    class ExtraerEjecutableComando {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Retorna null/vacío sin modificar para entrada null o vacía")
        void retornaNullOVacioSinModificar(String entrada) {
            assertEquals(entrada, OSUtils.extraerEjecutableComando(entrada), "assertEquals failed at OSUtilsTest.java:158");
        }

        @Test
        @DisplayName("Extrae ejecutable simple sin argumentos")
        void extraeEjecutableSimple() {
            assertEquals("claude", OSUtils.extraerEjecutableComando("claude"), "assertEquals failed at OSUtilsTest.java:164");
        }

        @Test
        @DisplayName("Extrae ejecutable con argumentos")
        void extraeEjecutableConArgumentos() {
            assertEquals("claude",
                OSUtils.extraerEjecutableComando("claude --dangerously-skip-permissions"), "assertEquals failed at OSUtilsTest.java:170");
        }

        @Test
        @DisplayName("Extrae ejecutable con ruta entre comillas dobles y argumentos")
        void extraeEjecutableConComillasDobles() {
            assertEquals("/opt/claude/bin/claude",
                OSUtils.extraerEjecutableComando("\"/opt/claude/bin/claude\" --dangerously-skip-permissions"), "assertEquals failed at OSUtilsTest.java:177");
        }

        @Test
        @DisplayName("Extrae ejecutable con ruta entre comillas simples y argumentos")
        void extraeEjecutableConComillasSimples() {
            assertEquals("/opt/claude/bin/claude",
                OSUtils.extraerEjecutableComando("'/opt/claude/bin/claude' --dangerously-skip-permissions"), "assertEquals failed at OSUtilsTest.java:184");
        }

        @Test
        @DisplayName("Maneja comillas sin cerrar retornando contenido después de comilla")
        void manejaComillasSinCerrar() {
            assertEquals("/path/to/cmd", OSUtils.extraerEjecutableComando("\"/path/to/cmd"), "assertEquals failed at OSUtilsTest.java:191");
            assertEquals("/path/to/cmd", OSUtils.extraerEjecutableComando("'/path/to/cmd"), "assertEquals failed at OSUtilsTest.java:192");
        }

        @Test
        @DisplayName("Maneja múltiples espacios entre ejecutable y argumentos")
        void manejaMultiplesEspacios() {
            assertEquals("claude", OSUtils.extraerEjecutableComando("claude    --flag"), "assertEquals failed at OSUtilsTest.java:198");
        }

        @Test
        @DisplayName("Recorta espacios al inicio y final")
        void recimaEspacios() {
            assertEquals("claude", OSUtils.extraerEjecutableComando("  claude  --flag  "), "assertEquals failed at OSUtilsTest.java:204");
        }

        @Test
        @DisplayName("Retorna entrada sin modificar para solo espacios (Normalizador.esVacio)")
        void retornaEntradaSinModificarParaSoloEspacios() {
            String entrada = "   ";
            assertEquals(entrada, OSUtils.extraerEjecutableComando(entrada), "assertEquals failed at OSUtilsTest.java:211");
        }
    }

    @Nested
    @DisplayName("resolverEjecutableComando")
    class ResolverEjecutableComando {

        @Test
        @DisplayName("Expande tilde a directorio home del usuario")
        void expandeTildeADirectorioHome() {
            String comando = "~/.local/bin/claude --dangerously-skip-permissions";
            String esperado = System.getProperty("user.home") + "/.local/bin/claude";
            assertEquals(esperado, OSUtils.resolverEjecutableComando(comando), "assertEquals failed at OSUtilsTest.java:224");
        }

        @Test
        @DisplayName("Retorna comando sin modificar si no tiene tilde")
        void retornaComandoSinModificarSinTilde() {
            String comando = "/usr/bin/claude --flag";
            assertEquals("/usr/bin/claude", OSUtils.resolverEjecutableComando(comando), "assertEquals failed at OSUtilsTest.java:231");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Retorna null/vacío sin modificar para entrada null o vacía")
        void retornaNullOVacioSinModificar(String entrada) {
            assertEquals(entrada, OSUtils.resolverEjecutableComando(entrada), "assertEquals failed at OSUtilsTest.java:238");
        }

        @Test
        @DisplayName("Expande solo tilde a directorio home")
        void expandeSoloTilde() {
            String esperado = System.getProperty("user.home");
            assertEquals(esperado, OSUtils.resolverEjecutableComando("~"), "assertEquals failed at OSUtilsTest.java:245");
        }
    }

    @Nested
    @DisplayName("debeCerrarVentanaAjustes")
    class DebeCerrarVentanaAjustes {

        @Test
        @DisplayName("Retorna true para DialogoConfiguracion")
        void retornaTrueParaDialogoConfiguracion() {
            assertTrue(OSUtils.debeCerrarVentanaAjustes("DialogoConfiguracion"), "assertTrue failed at OSUtilsTest.java:256");
        }

        @Test
        @DisplayName("Retorna true para nombre de clase completo de DialogoConfiguracion")
        void retornaTrueParaNombreCompleto() {
            assertTrue(OSUtils.debeCerrarVentanaAjustes("com.burpia.ui.DialogoConfiguracion"), "assertTrue failed at OSUtilsTest.java:262");
        }

        @Test
        @DisplayName("Retorna false para JDialog genérico")
        void retornaFalseParaJDialog() {
            assertFalse(OSUtils.debeCerrarVentanaAjustes("JDialog"), "assertFalse failed at OSUtilsTest.java:268");
        }

        @Test
        @DisplayName("Retorna false para MensajeBurpIA")
        void retornaFalseParaMensajeBurpIA() {
            assertFalse(OSUtils.debeCerrarVentanaAjustes("MensajeBurpIA"), "assertFalse failed at OSUtilsTest.java:274");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("Retorna false para null, vacío o solo espacios")
        void retornaFalseParaNullVacio_Espacios(String entrada) {
            assertFalse(OSUtils.debeCerrarVentanaAjustes(entrada), "assertFalse failed at OSUtilsTest.java:282");
        }
    }

    @Nested
    @DisplayName("existeBinario")
    class ExisteBinario {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Retorna false para null, vacío o solo espacios")
        void retornaFalseParaNullVacio_Espacios(String entrada) {
            assertFalse(OSUtils.existeBinario(entrada), "assertFalse failed at OSUtilsTest.java:295");
        }

        @Test
        @EnabledOnOs({OS.LINUX, OS.MAC})
        @DisplayName("Retorna false para archivo no ejecutable en Unix")
        void retornaFalseParaArchivoNoEjecutableEnUnix(@TempDir Path tempDir) throws IOException {
            Path archivo = tempDir.resolve("burpia-binario-test.tmp");
            Files.createFile(archivo);

            try {
                Files.setPosixFilePermissions(archivo, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
                ));

                Assumptions.assumeFalse(Files.isExecutable(archivo),
                    "No fue posible remover permiso de ejecución en este entorno");

                assertFalse(OSUtils.existeBinario(archivo.toString()), "assertFalse failed at OSUtilsTest.java:314");
            } finally {
                Files.deleteIfExists(archivo);
            }
        }

        @Test
        @DisplayName("Retorna true para /bin/ls en sistemas Unix")
        @EnabledOnOs({OS.LINUX, OS.MAC})
        void retornaTrueParaLsEnUnix() {
            File ls = new File("/bin/ls");
            Assumptions.assumeTrue(ls.exists(), "/bin/ls no existe en este sistema");
            assertTrue(OSUtils.existeBinario("/bin/ls"), "assertTrue failed at OSUtilsTest.java:326");
        }

        @Test
        @DisplayName("Retorna false para ruta inexistente")
        void retornaFalseParaRutaInexistente() {
            assertFalse(OSUtils.existeBinario("/ruta/inexistente/que/no/existe/binario"), "assertFalse failed at OSUtilsTest.java:332");
        }
    }
}
