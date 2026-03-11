package com.burpia.util;

import com.burpia.model.Tarea;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Contador Estados Tareas Tests")
class ContadorEstadosTareasTest {

    @Nested
    @DisplayName("contar()")
    class ContarTests {

        @Test
        @DisplayName("Retorna contador vacío para colección null")
        void contarConNullRetornaVacio() {
            ContadorEstadosTareas resultado = ContadorEstadosTareas.contar(null);
            
            assertEquals(0, resultado.getEnCola());
            assertEquals(0, resultado.getAnalizando());
            assertEquals(0, resultado.getPausadas());
            assertEquals(0, resultado.getCompletadas());
            assertEquals(0, resultado.getErrores());
            assertEquals(0, resultado.getCanceladas());
            assertEquals(0, resultado.getTotal());
        }

        @Test
        @DisplayName("Retorna contador vacío para lista vacía")
        void contarConListaVaciaRetornaVacio() {
            ContadorEstadosTareas resultado = ContadorEstadosTareas.contar(Collections.emptyList());
            
            assertEquals(0, resultado.getTotal());
        }

        @Test
        @DisplayName("Ignora elementos null en la colección")
        void contarIgnoraNulls() {
            List<Tarea> tareas = Arrays.asList(null, null, null);
            
            ContadorEstadosTareas resultado = ContadorEstadosTareas.contar(tareas);
            
            assertEquals(0, resultado.getTotal());
        }

        @Test
        @DisplayName("Cuenta correctamente tarea en cola")
        void contarTareaEnCola() {
            Tarea tarea = crearTarea(Tarea.ESTADO_EN_COLA);
            List<Tarea> tareas = Collections.singletonList(tarea);
            
            ContadorEstadosTareas resultado = ContadorEstadosTareas.contar(tareas);
            
            assertEquals(1, resultado.getEnCola());
            assertEquals(1, resultado.getActivasSinPausadas());
            assertEquals(1, resultado.getActivas());
            assertEquals(1, resultado.getTotal());
        }

        @Test
        @DisplayName("Cuenta correctamente tarea analizándose")
        void contarTareaAnalizando() {
            Tarea tarea = crearTarea(Tarea.ESTADO_ANALIZANDO);
            List<Tarea> tareas = Collections.singletonList(tarea);
            
            ContadorEstadosTareas resultado = ContadorEstadosTareas.contar(tareas);
            
            assertEquals(1, resultado.getAnalizando());
            assertEquals(1, resultado.getActivasSinPausadas());
            assertEquals(1, resultado.getActivas());
        }

        @Test
        @DisplayName("Cuenta correctamente tarea pausada")
        void contarTareaPausada() {
            Tarea tarea = crearTarea(Tarea.ESTADO_PAUSADO);
            List<Tarea> tareas = Collections.singletonList(tarea);
            
            ContadorEstadosTareas resultado = ContadorEstadosTareas.contar(tareas);
            
            assertEquals(1, resultado.getPausadas());
            assertEquals(0, resultado.getActivasSinPausadas());
            assertEquals(1, resultado.getActivas());
        }

        @Test
        @DisplayName("Cuenta correctamente tarea completada")
        void contarTareaCompletada() {
            Tarea tarea = crearTarea(Tarea.ESTADO_COMPLETADO);
            List<Tarea> tareas = Collections.singletonList(tarea);
            
            ContadorEstadosTareas resultado = ContadorEstadosTareas.contar(tareas);
            
            assertEquals(1, resultado.getCompletadas());
            assertEquals(1, resultado.getFinalizadas());
        }

        @Test
        @DisplayName("Cuenta correctamente tarea con error")
        void contarTareaError() {
            Tarea tarea = crearTarea(Tarea.ESTADO_ERROR);
            List<Tarea> tareas = Collections.singletonList(tarea);
            
            ContadorEstadosTareas resultado = ContadorEstadosTareas.contar(tareas);
            
            assertEquals(1, resultado.getErrores());
            assertEquals(1, resultado.getFinalizadas());
            assertEquals(1, resultado.getErroresYCanceladas());
        }

        @Test
        @DisplayName("Cuenta correctamente tarea cancelada")
        void contarTareaCancelada() {
            Tarea tarea = crearTarea(Tarea.ESTADO_CANCELADO);
            List<Tarea> tareas = Collections.singletonList(tarea);
            
            ContadorEstadosTareas resultado = ContadorEstadosTareas.contar(tareas);
            
            assertEquals(1, resultado.getCanceladas());
            assertEquals(1, resultado.getFinalizadas());
            assertEquals(1, resultado.getErroresYCanceladas());
        }

        @Test
        @DisplayName("Cuenta múltiples tareas con estados mixtos")
        void contarMultiplesTareas() {
            List<Tarea> tareas = Arrays.asList(
                crearTarea(Tarea.ESTADO_EN_COLA),
                crearTarea(Tarea.ESTADO_EN_COLA),
                crearTarea(Tarea.ESTADO_ANALIZANDO),
                crearTarea(Tarea.ESTADO_PAUSADO),
                crearTarea(Tarea.ESTADO_COMPLETADO),
                crearTarea(Tarea.ESTADO_ERROR),
                crearTarea(Tarea.ESTADO_ERROR),
                crearTarea(Tarea.ESTADO_CANCELADO),
                null
            );
            
            ContadorEstadosTareas resultado = ContadorEstadosTareas.contar(tareas);
            
            assertEquals(2, resultado.getEnCola());
            assertEquals(1, resultado.getAnalizando());
            assertEquals(1, resultado.getPausadas());
            assertEquals(1, resultado.getCompletadas());
            assertEquals(2, resultado.getErrores());
            assertEquals(1, resultado.getCanceladas());
            assertEquals(3, resultado.getActivasSinPausadas()); // enCola + analizando
            assertEquals(4, resultado.getActivas()); // + pausadas
            assertEquals(4, resultado.getFinalizadas()); // completadas + errores + canceladas
            assertEquals(3, resultado.getErroresYCanceladas()); // errores + canceladas
            assertEquals(8, resultado.getTotal()); // excluye null
        }
    }

    @Nested
    @DisplayName("vacio()")
    class VacioTests {

        @Test
        @DisplayName("Retorna contador con todos los valores en cero")
        void vacioRetornaCeros() {
            ContadorEstadosTareas resultado = ContadorEstadosTareas.vacio();
            
            assertEquals(0, resultado.getEnCola());
            assertEquals(0, resultado.getAnalizando());
            assertEquals(0, resultado.getPausadas());
            assertEquals(0, resultado.getCompletadas());
            assertEquals(0, resultado.getErrores());
            assertEquals(0, resultado.getCanceladas());
            assertEquals(0, resultado.getActivasSinPausadas());
            assertEquals(0, resultado.getActivas());
            assertEquals(0, resultado.getFinalizadas());
            assertEquals(0, resultado.getErroresYCanceladas());
            assertEquals(0, resultado.getTotal());
        }
    }

    @Nested
    @DisplayName("contarPorPredicado()")
    class ContarPorPredicadoTests {

        @Test
        @DisplayName("Retorna cero para colección null")
        void contarPorPredicadoNull() {
            int resultado = ContadorEstadosTareas.contarPorPredicado(null, Tarea::esEstadoPausable);
            
            assertEquals(0, resultado);
        }

        @Test
        @DisplayName("Retorna cero para predicado null")
        void contarPorPredicadoPredicadoNull() {
            List<Tarea> tareas = Collections.singletonList(crearTarea(Tarea.ESTADO_EN_COLA));
            
            int resultado = ContadorEstadosTareas.contarPorPredicado(tareas, null);
            
            assertEquals(0, resultado);
        }

        @Test
        @DisplayName("Cuenta correctamente cuando hay coincidencias")
        void contarPorPredicadoConCoincidencias() {
            List<Tarea> tareas = Arrays.asList(
                crearTarea(Tarea.ESTADO_EN_COLA),
                crearTarea(Tarea.ESTADO_ANALIZANDO),
                crearTarea(Tarea.ESTADO_PAUSADO),
                crearTarea(Tarea.ESTADO_COMPLETADO)
            );
            
            int pausables = ContadorEstadosTareas.contarPorPredicado(tareas, Tarea::esEstadoPausable);
            int finalizadas = ContadorEstadosTareas.contarPorPredicado(tareas, Tarea::esEstadoFinal);
            
            assertEquals(2, pausables); // EN_COLA + ANALIZANDO
            assertEquals(1, finalizadas); // COMPLETADO
        }

        @Test
        @DisplayName("Ignora elementos null en la colección")
        void contarPorPredicadoIgnoraNulls() {
            List<Tarea> tareas = Arrays.asList(
                crearTarea(Tarea.ESTADO_EN_COLA),
                null,
                crearTarea(Tarea.ESTADO_PAUSADO)
            );
            
            int reanudables = ContadorEstadosTareas.contarPorPredicado(tareas, Tarea::esEstadoReanudable);
            
            assertEquals(1, reanudables); // Solo PAUSADO
        }
    }

    @Nested
    @DisplayName("Getters calculados")
    class GettersCalculadosTests {

        @Test
        @DisplayName("getActivasSinPausadas calcula correctamente")
        void getActivasSinPausadas() {
            ContadorEstadosTareas contador = new ContadorEstadosTareas(3, 2, 0, 0, 0, 0);
            
            assertEquals(5, contador.getActivasSinPausadas()); // enCola + analizando
        }

        @Test
        @DisplayName("getActivas incluye pausadas")
        void getActivas() {
            ContadorEstadosTareas contador = new ContadorEstadosTareas(3, 2, 1, 0, 0, 0);
            
            assertEquals(6, contador.getActivas()); // enCola + analizando + pausadas
        }

        @Test
        @DisplayName("getFinalizadas suma completadas + errores + canceladas")
        void getFinalizadas() {
            ContadorEstadosTareas contador = new ContadorEstadosTareas(0, 0, 0, 5, 3, 2);
            
            assertEquals(10, contador.getFinalizadas()); // completadas + errores + canceladas
        }

        @Test
        @DisplayName("getErroresYCanceladas suma correctamente")
        void getErroresYCanceladas() {
            ContadorEstadosTareas contador = new ContadorEstadosTareas(0, 0, 0, 0, 4, 6);
            
            assertEquals(10, contador.getErroresYCanceladas()); // errores + canceladas
        }

        @Test
        @DisplayName("getTotal suma todos los estados")
        void getTotal() {
            ContadorEstadosTareas contador = new ContadorEstadosTareas(1, 2, 3, 4, 5, 6);
            
            assertEquals(21, contador.getTotal()); // 1+2+3+4+5+6
        }
    }

    // Helper para crear tareas rápidamente
    private Tarea crearTarea(String estado) {
        return new Tarea(
            java.util.UUID.randomUUID().toString(),
            "TEST",
            "http://test.com",
            estado
        );
    }
}