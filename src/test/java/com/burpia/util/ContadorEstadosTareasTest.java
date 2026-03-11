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
            
            assertEquals(0, resultado.obtenerEnCola());
            assertEquals(0, resultado.obtenerAnalizando());
            assertEquals(0, resultado.obtenerPausadas());
            assertEquals(0, resultado.obtenerCompletadas());
            assertEquals(0, resultado.obtenerErrores());
            assertEquals(0, resultado.obtenerCanceladas());
            assertEquals(0, resultado.obtenerTotal());
        }

        @Test
        @DisplayName("Retorna contador vacío para lista vacía")
        void contarConListaVaciaRetornaVacio() {
            ContadorEstadosTareas resultado = ContadorEstadosTareas.contar(Collections.emptyList());
            
            assertEquals(0, resultado.obtenerTotal());
        }

        @Test
        @DisplayName("Ignora elementos null en la colección")
        void contarIgnoraNulls() {
            List<Tarea> tareas = Arrays.asList(null, null, null);
            
            ContadorEstadosTareas resultado = ContadorEstadosTareas.contar(tareas);
            
            assertEquals(0, resultado.obtenerTotal());
        }

        @Test
        @DisplayName("Cuenta correctamente tarea en cola")
        void contarTareaEnCola() {
            Tarea tarea = crearTarea(Tarea.ESTADO_EN_COLA);
            List<Tarea> tareas = Collections.singletonList(tarea);
            
            ContadorEstadosTareas resultado = ContadorEstadosTareas.contar(tareas);
            
            assertEquals(1, resultado.obtenerEnCola());
            assertEquals(1, resultado.obtenerActivasSinPausadas());
            assertEquals(1, resultado.obtenerActivas());
            assertEquals(1, resultado.obtenerTotal());
        }

        @Test
        @DisplayName("Cuenta correctamente tarea analizándose")
        void contarTareaAnalizando() {
            Tarea tarea = crearTarea(Tarea.ESTADO_ANALIZANDO);
            List<Tarea> tareas = Collections.singletonList(tarea);
            
            ContadorEstadosTareas resultado = ContadorEstadosTareas.contar(tareas);
            
            assertEquals(1, resultado.obtenerAnalizando());
            assertEquals(1, resultado.obtenerActivasSinPausadas());
            assertEquals(1, resultado.obtenerActivas());
        }

        @Test
        @DisplayName("Cuenta correctamente tarea pausada")
        void contarTareaPausada() {
            Tarea tarea = crearTarea(Tarea.ESTADO_PAUSADO);
            List<Tarea> tareas = Collections.singletonList(tarea);
            
            ContadorEstadosTareas resultado = ContadorEstadosTareas.contar(tareas);
            
            assertEquals(1, resultado.obtenerPausadas());
            assertEquals(0, resultado.obtenerActivasSinPausadas());
            assertEquals(1, resultado.obtenerActivas());
        }

        @Test
        @DisplayName("Cuenta correctamente tarea completada")
        void contarTareaCompletada() {
            Tarea tarea = crearTarea(Tarea.ESTADO_COMPLETADO);
            List<Tarea> tareas = Collections.singletonList(tarea);
            
            ContadorEstadosTareas resultado = ContadorEstadosTareas.contar(tareas);
            
            assertEquals(1, resultado.obtenerCompletadas());
            assertEquals(1, resultado.obtenerFinalizadas());
        }

        @Test
        @DisplayName("Cuenta correctamente tarea con error")
        void contarTareaError() {
            Tarea tarea = crearTarea(Tarea.ESTADO_ERROR);
            List<Tarea> tareas = Collections.singletonList(tarea);
            
            ContadorEstadosTareas resultado = ContadorEstadosTareas.contar(tareas);
            
            assertEquals(1, resultado.obtenerErrores());
            assertEquals(1, resultado.obtenerFinalizadas());
            assertEquals(1, resultado.obtenerErroresYCanceladas());
        }

        @Test
        @DisplayName("Cuenta correctamente tarea cancelada")
        void contarTareaCancelada() {
            Tarea tarea = crearTarea(Tarea.ESTADO_CANCELADO);
            List<Tarea> tareas = Collections.singletonList(tarea);
            
            ContadorEstadosTareas resultado = ContadorEstadosTareas.contar(tareas);
            
            assertEquals(1, resultado.obtenerCanceladas());
            assertEquals(1, resultado.obtenerFinalizadas());
            assertEquals(1, resultado.obtenerErroresYCanceladas());
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
            
            assertEquals(2, resultado.obtenerEnCola());
            assertEquals(1, resultado.obtenerAnalizando());
            assertEquals(1, resultado.obtenerPausadas());
            assertEquals(1, resultado.obtenerCompletadas());
            assertEquals(2, resultado.obtenerErrores());
            assertEquals(1, resultado.obtenerCanceladas());
            assertEquals(3, resultado.obtenerActivasSinPausadas()); // enCola + analizando
            assertEquals(4, resultado.obtenerActivas()); // + pausadas
            assertEquals(4, resultado.obtenerFinalizadas()); // completadas + errores + canceladas
            assertEquals(3, resultado.obtenerErroresYCanceladas()); // errores + canceladas
            assertEquals(8, resultado.obtenerTotal()); // excluye null
        }
    }

    @Nested
    @DisplayName("vacio()")
    class VacioTests {

        @Test
        @DisplayName("Retorna contador con todos los valores en cero")
        void vacioRetornaCeros() {
            ContadorEstadosTareas resultado = ContadorEstadosTareas.vacio();
            
            assertEquals(0, resultado.obtenerEnCola());
            assertEquals(0, resultado.obtenerAnalizando());
            assertEquals(0, resultado.obtenerPausadas());
            assertEquals(0, resultado.obtenerCompletadas());
            assertEquals(0, resultado.obtenerErrores());
            assertEquals(0, resultado.obtenerCanceladas());
            assertEquals(0, resultado.obtenerActivasSinPausadas());
            assertEquals(0, resultado.obtenerActivas());
            assertEquals(0, resultado.obtenerFinalizadas());
            assertEquals(0, resultado.obtenerErroresYCanceladas());
            assertEquals(0, resultado.obtenerTotal());
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
            
            assertEquals(5, contador.obtenerActivasSinPausadas()); // enCola + analizando
        }

        @Test
        @DisplayName("getActivas incluye pausadas")
        void getActivas() {
            ContadorEstadosTareas contador = new ContadorEstadosTareas(3, 2, 1, 0, 0, 0);
            
            assertEquals(6, contador.obtenerActivas()); // enCola + analizando + pausadas
        }

        @Test
        @DisplayName("getFinalizadas suma completadas + errores + canceladas")
        void getFinalizadas() {
            ContadorEstadosTareas contador = new ContadorEstadosTareas(0, 0, 0, 5, 3, 2);
            
            assertEquals(10, contador.obtenerFinalizadas()); // completadas + errores + canceladas
        }

        @Test
        @DisplayName("getErroresYCanceladas suma correctamente")
        void getErroresYCanceladas() {
            ContadorEstadosTareas contador = new ContadorEstadosTareas(0, 0, 0, 0, 4, 6);
            
            assertEquals(10, contador.obtenerErroresYCanceladas()); // errores + canceladas
        }

        @Test
        @DisplayName("getTotal suma todos los estados")
        void getTotal() {
            ContadorEstadosTareas contador = new ContadorEstadosTareas(1, 2, 3, 4, 5, 6);
            
            assertEquals(21, contador.obtenerTotal()); // 1+2+3+4+5+6
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
