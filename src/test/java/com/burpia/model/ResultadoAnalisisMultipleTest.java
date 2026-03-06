package com.burpia.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests completos para ResultadoAnalisisMultiple.
 * <p>
 * Verifica construcción, inmutabilidad, cálculo de severidad máxima,
 * manejo de errores parciales y métodos de objeto.
 * </p>
 */
@DisplayName("ResultadoAnalisisMultiple Tests")
class ResultadoAnalisisMultipleTest {

    private static final String URL_TEST = "https://example.com/test";
    private static final String URL_ALTERNATIVA = "https://example.org/other";

    // ==================== CONSTRUCTOR ====================

    @Test
    @DisplayName("Constructor con parámetros nulos retorna valores por defecto")
    void testConstructorConParametrosNulos_retornaValoresPorDefecto() {
        ResultadoAnalisisMultiple resultado = new ResultadoAnalisisMultiple(
            null,
            null,
            null,
            null
        );

        assertEquals("", resultado.obtenerUrl());
        assertNotNull(resultado.obtenerHallazgos());
        assertEquals(0, resultado.obtenerNumeroHallazgos());
        assertNull(resultado.obtenerSolicitudHttp());
        assertNotNull(resultado.obtenerProveedoresFallidos());
        assertEquals(0, resultado.obtenerProveedoresFallidos().size());
        assertFalse(resultado.huboErroresParciales());
    }

    @Test
    @DisplayName("Constructor requiere lista de proveedores fallidos y la inicializa vacía")
    void testConstructorInicializaProveedoresFallidosVacio() {
        ResultadoAnalisisMultiple resultado = new ResultadoAnalisisMultiple(
            URL_TEST,
            Collections.emptyList(),
            null,
            Collections.emptyList()
        );

        assertEquals(URL_TEST, resultado.obtenerUrl());
        assertNotNull(resultado.obtenerProveedoresFallidos());
        assertEquals(0, resultado.obtenerProveedoresFallidos().size());
        assertFalse(resultado.huboErroresParciales());
    }

    @Test
    @DisplayName("Constructor copia defensivamente las listas")
    void testConstructor_copiaDefensivamenteListas() {
        List<Hallazgo> hallazgosOriginales = new ArrayList<>();
        hallazgosOriginales.add(crearHallazgo(Hallazgo.SEVERIDAD_HIGH));

        List<String> proveedoresOriginales = new ArrayList<>();
        proveedoresOriginales.add("openai");

        ResultadoAnalisisMultiple resultado = new ResultadoAnalisisMultiple(
            URL_TEST,
            hallazgosOriginales,
            null,
            proveedoresOriginales
        );

        // Modificar listas originales NO debe afectar el resultado
        hallazgosOriginales.add(crearHallazgo(Hallazgo.SEVERIDAD_LOW));
        proveedoresOriginales.add("claude");

        assertEquals(1, resultado.obtenerNumeroHallazgos());
        assertEquals(1, resultado.obtenerProveedoresFallidos().size());
    }

    // ==================== INMUTABILIDAD ====================

    @Test
    @DisplayName("Lista de hallazgos es inmutable")
    void testObtenerHallazgos_retornaListaInmutable() {
        List<Hallazgo> hallazgos = Arrays.asList(
            crearHallazgo(Hallazgo.SEVERIDAD_HIGH),
            crearHallazgo(Hallazgo.SEVERIDAD_MEDIUM)
        );

        ResultadoAnalisisMultiple resultado = new ResultadoAnalisisMultiple(
            URL_TEST,
            hallazgos,
            null,
            Collections.emptyList()
        );

        List<Hallazgo> hallazgosObtenidos = resultado.obtenerHallazgos();
        assertThrows(UnsupportedOperationException.class, () -> 
            hallazgosObtenidos.add(crearHallazgo(Hallazgo.SEVERIDAD_LOW))
        );
    }

    @Test
    @DisplayName("Lista de proveedores fallidos es inmutable")
    void testObtenerProveedoresFallidos_retornaListaInmutable() {
        ResultadoAnalisisMultiple resultado = new ResultadoAnalisisMultiple(
            URL_TEST,
            null,
            null,
            Arrays.asList("openai", "claude")
        );

        List<String> proveedores = resultado.obtenerProveedoresFallidos();
        assertThrows(UnsupportedOperationException.class, () -> 
            proveedores.add("gemini")
        );
    }

    // ==================== SEVERIDAD MÁXIMA ====================

    @Test
    @DisplayName("Severidad máxima con lista vacía retorna Info")
    void testObtenerSeveridadMaxima_conListaVacia_retornaInfo() {
        ResultadoAnalisisMultiple resultado = new ResultadoAnalisisMultiple(
            URL_TEST,
            Collections.emptyList(),
            null,
            Collections.emptyList()
        );

        assertEquals(Hallazgo.SEVERIDAD_INFO, resultado.obtenerSeveridadMaxima());
    }

    @Test
    @DisplayName("Severidad máxima con hallazgos nulos los ignora")
    void testObtenerSeveridadMaxima_conHallazgosNulos_losIgnora() {
        ResultadoAnalisisMultiple resultado = new ResultadoAnalisisMultiple(
            URL_TEST,
            Arrays.asList(
                null,
                crearHallazgo(Hallazgo.SEVERIDAD_MEDIUM),
                null
            ),
            null,
            Collections.emptyList()
        );

        assertEquals(Hallazgo.SEVERIDAD_MEDIUM, resultado.obtenerSeveridadMaxima());
    }

    @Test
    @DisplayName("Severidad máxima con múltiples hallazgos retorna la más alta")
    void testObtenerSeveridadMaxima_conMultiplesHallazgos_retornaLaMasAlta() {
        ResultadoAnalisisMultiple resultado = new ResultadoAnalisisMultiple(
            URL_TEST,
            Arrays.asList(
                crearHallazgo(Hallazgo.SEVERIDAD_LOW),
                crearHallazgo(Hallazgo.SEVERIDAD_CRITICAL),
                crearHallazgo(Hallazgo.SEVERIDAD_MEDIUM),
                crearHallazgo(Hallazgo.SEVERIDAD_HIGH)
            ),
            null,
            Collections.emptyList()
        );

        assertEquals(Hallazgo.SEVERIDAD_CRITICAL, resultado.obtenerSeveridadMaxima());
    }

    @Test
    @DisplayName("Severidad máxima respeta orden: Critical > High > Medium > Low > Info")
    void testObtenerSeveridadMaxima_respetaOrdenSeveridad() {
        // Critical es la más alta
        ResultadoAnalisisMultiple conCritical = new ResultadoAnalisisMultiple(
            URL_TEST,
            Arrays.asList(
                crearHallazgo(Hallazgo.SEVERIDAD_CRITICAL),
                crearHallazgo(Hallazgo.SEVERIDAD_LOW)
            ),
            null,
            Collections.emptyList()
        );
        assertEquals(Hallazgo.SEVERIDAD_CRITICAL, conCritical.obtenerSeveridadMaxima());

        // High > Medium
        ResultadoAnalisisMultiple conHigh = new ResultadoAnalisisMultiple(
            URL_TEST,
            Arrays.asList(
                crearHallazgo(Hallazgo.SEVERIDAD_MEDIUM),
                crearHallazgo(Hallazgo.SEVERIDAD_HIGH),
                crearHallazgo(Hallazgo.SEVERIDAD_LOW)
            ),
            null,
            Collections.emptyList()
        );
        assertEquals(Hallazgo.SEVERIDAD_HIGH, conHigh.obtenerSeveridadMaxima());
    }

    // ==================== PROVEEDORES FALLIDOS ====================

    @Test
    @DisplayName("huboErroresParciales retorna false cuando no hay proveedores fallidos")
    void testHuboErroresParciales_sinProveedoresFallidos_retornaFalse() {
        ResultadoAnalisisMultiple resultado = new ResultadoAnalisisMultiple(
            URL_TEST,
            null,
            null,
            Collections.emptyList()
        );

        assertFalse(resultado.huboErroresParciales());
    }

    @Test
    @DisplayName("huboErroresParciales retorna true cuando hay proveedores fallidos")
    void testHuboErroresParciales_conProveedoresFallidos_retornaTrue() {
        ResultadoAnalisisMultiple resultado = new ResultadoAnalisisMultiple(
            URL_TEST,
            null,
            null,
            Arrays.asList("openai", "claude")
        );

        assertTrue(resultado.huboErroresParciales());
        assertEquals(2, resultado.obtenerProveedoresFallidos().size());
        assertTrue(resultado.obtenerProveedoresFallidos().contains("openai"));
        assertTrue(resultado.obtenerProveedoresFallidos().contains("claude"));
    }

    // ==================== EQUALS Y HASHCODE ====================

    @Test
    @DisplayName("equals retorna true para objetos con mismos valores")
    void testEquals_conMismosValores_retornaTrue() {
        List<Hallazgo> hallazgos = Arrays.asList(
            crearHallazgo(Hallazgo.SEVERIDAD_HIGH)
        );
        List<String> proveedores = Arrays.asList("openai");

        ResultadoAnalisisMultiple resultado1 = new ResultadoAnalisisMultiple(
            URL_TEST,
            hallazgos,
            null,
            proveedores
        );

        ResultadoAnalisisMultiple resultado2 = new ResultadoAnalisisMultiple(
            URL_TEST,
            hallazgos,
            null,
            proveedores
        );

        assertEquals(resultado1, resultado2);
        assertEquals(resultado1.hashCode(), resultado2.hashCode());
    }

    @Test
    @DisplayName("equals retorna false para objetos con diferentes valores")
    void testEquals_conDiferentesValores_retornaFalse() {
        ResultadoAnalisisMultiple resultado1 = new ResultadoAnalisisMultiple(
            URL_TEST,
            Collections.emptyList(),
            null,
            Collections.emptyList()
        );

        ResultadoAnalisisMultiple resultado2 = new ResultadoAnalisisMultiple(
            URL_ALTERNATIVA,
            Collections.emptyList(),
            null,
            Collections.emptyList()
        );

        assertNotEquals(resultado1, resultado2);
    }

    @Test
    @DisplayName("equals retorna false para null y otra clase")
    void testEquals_conNullYOtraClase_retornaFalse() {
        ResultadoAnalisisMultiple resultado = new ResultadoAnalisisMultiple(
            URL_TEST,
            null,
            null,
            null
        );

        assertNotEquals(null, resultado);
        assertNotEquals("string", resultado);
    }

    @Test
    @DisplayName("equals retorna true para el mismo objeto")
    void testEquals_conMismoObjeto_retornaTrue() {
        ResultadoAnalisisMultiple resultado = new ResultadoAnalisisMultiple(
            URL_TEST,
            null,
            null,
            null
        );

        assertEquals(resultado, resultado);
    }

    // ==================== TOSTRING ====================

    @Test
    @DisplayName("toString contiene información relevante")
    void testToString_contieneInformacionRelevante() {
        ResultadoAnalisisMultiple resultado = new ResultadoAnalisisMultiple(
            URL_TEST,
            Arrays.asList(crearHallazgo(Hallazgo.SEVERIDAD_HIGH)),
            null,
            Arrays.asList("openai")
        );

        String str = resultado.toString();
        
        assertTrue(str.contains(URL_TEST));
        assertTrue(str.contains("hallazgos=1"));
        assertTrue(str.contains("proveedoresFallidos"));
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Crea un hallazgo con severidad específica para tests.
     */
    private Hallazgo crearHallazgo(String severidad) {
        return new Hallazgo(
            URL_TEST,
            "Título test",
            "Detalle test",
            severidad,
            Hallazgo.CONFIANZA_MEDIA
        );
    }
}
