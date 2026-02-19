package com.burpia.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DeduplicadorSolicitudes Tests")
class DeduplicadorSolicitudesTest {

    private DeduplicadorSolicitudes deduplicador;

    @BeforeEach
    void setUp() {
        deduplicador = new DeduplicadorSolicitudes();
    }

    @Test
    @DisplayName("Primer hash no es duplicado")
    void testPrimerHashNoDuplicado() {
        boolean esDuplicado = deduplicador.esDuplicadoYAgregar("hash1");
        assertFalse(esDuplicado);
    }

    @Test
    @DisplayName("Segundo hash igual es duplicado")
    void testSegundoHashIgualEsDuplicado() {
        deduplicador.esDuplicadoYAgregar("hash1");
        boolean esDuplicado = deduplicador.esDuplicadoYAgregar("hash1");
        assertTrue(esDuplicado);
    }

    @Test
    @DisplayName("Hashes diferentes no son duplicados")
    void testHashesDiferentesNoDuplicados() {
        deduplicador.esDuplicadoYAgregar("hash1");
        boolean esDuplicado = deduplicador.esDuplicadoYAgregar("hash2");
        assertFalse(esDuplicado);
    }

    @Test
    @DisplayName("Contador de hashes correcto")
    void testContadorHashes() {
        deduplicador.esDuplicadoYAgregar("hash1");
        deduplicador.esDuplicadoYAgregar("hash2");
        deduplicador.esDuplicadoYAgregar("hash3");
        
        assertEquals(3, deduplicador.obtenerNumeroHashes());
    }

    @Test
    @DisplayName("Contar duplicados correctamente")
    void testContarDuplicados() {
        deduplicador.esDuplicadoYAgregar("hash1");
        deduplicador.esDuplicadoYAgregar("hash2");
        deduplicador.esDuplicadoYAgregar("hash1"); // duplicado
        deduplicador.esDuplicadoYAgregar("hash3");
        deduplicador.esDuplicadoYAgregar("hash2"); // duplicado
        
        assertEquals(3, deduplicador.obtenerNumeroHashes());
    }

    @Test
    @DisplayName("Limpiar elimina todos los hashes")
    void testLimpiar() {
        deduplicador.esDuplicadoYAgregar("hash1");
        deduplicador.esDuplicadoYAgregar("hash2");
        
        deduplicador.limpiar();
        
        assertEquals(0, deduplicador.obtenerNumeroHashes());
    }

    @Test
    @DisplayName("Contiene hash funciona correctamente")
    void testContieneHash() {
        deduplicador.esDuplicadoYAgregar("hash1");
        
        assertTrue(deduplicador.contieneHash("hash1"));
        assertFalse(deduplicador.contieneHash("hash2"));
    }

    @Test
    @DisplayName("Agregar hash manualmente")
    void testAgregarHashManualmente() {
        deduplicador.agregarHash("hash1");
        
        assertTrue(deduplicador.contieneHash("hash1"));
        assertTrue(deduplicador.esDuplicadoYAgregar("hash1")); // ahora es duplicado
    }
}
