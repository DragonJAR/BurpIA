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
    @DisplayName("Contar duplicados correctamente")
    void testContarDuplicados() {
        deduplicador.esDuplicadoYAgregar("hash1");
        deduplicador.esDuplicadoYAgregar("hash2");
        deduplicador.esDuplicadoYAgregar("hash1");
        deduplicador.esDuplicadoYAgregar("hash3");
        deduplicador.esDuplicadoYAgregar("hash2");

        assertTrue(deduplicador.esDuplicadoYAgregar("hash1"));
        assertTrue(deduplicador.esDuplicadoYAgregar("hash2"));
        assertTrue(deduplicador.esDuplicadoYAgregar("hash3"));
    }

    @Test
    @DisplayName("Respeta límite máximo de hashes con eviction LRU")
    void testLimiteMaximo() {
        DeduplicadorSolicitudes deduplicadorLocal = new DeduplicadorSolicitudes(3, 60000);
        deduplicadorLocal.esDuplicadoYAgregar("h1");
        deduplicadorLocal.esDuplicadoYAgregar("h2");
        deduplicadorLocal.esDuplicadoYAgregar("h3");
        deduplicadorLocal.esDuplicadoYAgregar("h4");

        assertTrue(deduplicadorLocal.esDuplicadoYAgregar("h2"));
        assertTrue(deduplicadorLocal.esDuplicadoYAgregar("h3"));
        assertTrue(deduplicadorLocal.esDuplicadoYAgregar("h4"));
    }

    @Test
    @DisplayName("Evicción conserva elementos recientemente accedidos (LRU real)")
    void testEviccionLruReal() {
        DeduplicadorSolicitudes deduplicadorLocal = new DeduplicadorSolicitudes(3, 60000);
        deduplicadorLocal.esDuplicadoYAgregar("h1");
        deduplicadorLocal.esDuplicadoYAgregar("h2");
        deduplicadorLocal.esDuplicadoYAgregar("h3");

        assertTrue(deduplicadorLocal.esDuplicadoYAgregar("h1"));

        deduplicadorLocal.esDuplicadoYAgregar("h4");

        assertTrue(deduplicadorLocal.esDuplicadoYAgregar("h1"));
        assertTrue(deduplicadorLocal.esDuplicadoYAgregar("h3"));
        assertTrue(deduplicadorLocal.esDuplicadoYAgregar("h4"));
    }

    @Test
    @DisplayName("Hash nulo o vacío no es duplicado")
    void testHashNuloOVacio() {
        assertFalse(deduplicador.esDuplicadoYAgregar(null));
        assertFalse(deduplicador.esDuplicadoYAgregar(""));
    }
}
