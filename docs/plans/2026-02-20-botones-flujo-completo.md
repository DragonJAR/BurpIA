# Botones y Flujos de UI Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Verificar y corregir el comportamiento de todos los botones críticos (pausar, reanudar, reintentar, cancelar, limpiar, etc.) para que funcionen correctamente y de forma consistente en todo el proyecto.

**Architecture:** Se reforzará primero la capa de dominio/control (GestorTareas + transición de estados), luego se alineará la UI (PanelTareas y paneles asociados) con esas reglas, y finalmente se añadirá una suite de pruebas de regresión a nivel unitario e integración. La estrategia evita duplicación centralizando reglas de transición y validaciones de estado.

**Tech Stack:** Java 21, Swing, Montoya API, JUnit 5, Gradle.

---

### Task 1: Auditoría de contratos de estado en tareas

**Files:**
- Modify: `src/main/java/com/burpia/model/Tarea.java`
- Modify: `src/main/java/com/burpia/util/GestorTareas.java`
- Test: `src/test/java/com/burpia/model/TareaTest.java`

**Step 1: Write failing tests for transition rules**
- Añadir tests que definan reglas explícitas:
  - `PAUSAR TODO` no debe reanudar tareas ya pausadas.
  - `REANUDAR TODO` no debe pausar tareas en análisis.
  - `REINTENTAR` aplica solo a `ERROR|CANCELADO`.

**Step 2: Run test to verify it fails**
- Run: `./gradlew test --tests "com.burpia.model.TareaTest" --tests "com.burpia.util.GestorTareasTest"`
- Expected: FAIL en casos de transición ambigua.

**Step 3: Minimal implementation**
- Separar operaciones globales en `pausarTodasActivas()` y `reanudarTodasPausadas()`.
- Mantener `reanudarTarea(id)` para `PAUSADO|ERROR|CANCELADO` (reintento) y crear método específico para reanudar pausadas si aplica.

**Step 4: Verify pass**
- Run: `./gradlew test --tests "com.burpia.model.TareaTest" --tests "com.burpia.util.GestorTareasTest"`
- Expected: PASS.

### Task 2: Corregir semántica de botones en PanelTareas

**Files:**
- Modify: `src/main/java/com/burpia/ui/PanelTareas.java`
- Test: `src/test/java/com/burpia/ui/PanelTareasActionsTest.java` (nuevo)

**Step 1: Write failing tests for UI action mapping**
- Validar que:
  - Botón principal alterna entre pausar/reanudar sin mezcla en estados mixtos.
  - Menú contextual de 1 y múltiples filas ofrece solo acciones válidas por estado.

**Step 2: Run test to verify it fails**
- Run: `./gradlew test --tests "com.burpia.ui.PanelTareasActionsTest"`

**Step 3: Minimal implementation**
- Cambiar `botonPausarReanudar` para invocar acción explícita según estado predominante:
  - Si hay pausadas -> reanudar pausadas.
  - Si no hay pausadas y hay activas -> pausar activas.
- Reusar helper DRY para obtener conteos por estado y no repetir lógica en cada handler.

**Step 4: Verify pass**
- Run: `./gradlew test --tests "com.burpia.ui.PanelTareasActionsTest"`

### Task 3: Validación integral de todos los botones de paneles

**Files:**
- Modify/Test: `src/test/java/com/burpia/ui/DialogoConfiguracionTest.java` (nuevo)
- Modify/Test: `src/test/java/com/burpia/ui/PanelHallazgosActionsTest.java` (nuevo)
- Modify/Test: `src/test/java/com/burpia/ui/PanelConsolaTest.java` (si falta cobertura)
- Modify/Test: `src/test/java/com/burpia/ui/PanelEstadisticasTest.java` (si falta cobertura)

**Step 1: Write failing integration-style tests**
- Cobertura mínima por botón:
  - Configuración: `Probar conexión`, `Guardar`, `Refresh modelos`, `Restaurar prompt`.
  - Hallazgos: `Limpiar todo`, `Exportar CSV/JSON`, acciones contextuales.
  - Consola: `Limpiar consola` y estado de autoscroll.
  - Estadísticas: `Limpiar` y `Ajustes` (callbacks conectados).

**Step 2: Run tests and capture failures**
- Run: `./gradlew test --tests "com.burpia.ui.*Test"`

**Step 3: Minimal implementation**
- Ajustar handlers que no respeten precondiciones o dejen UI inconsistente.
- Añadir guard clauses para estados inválidos.

**Step 4: Verify pass**
- Run: `./gradlew test --tests "com.burpia.ui.*Test"`

### Task 4: Verificación E2E del flujo de tareas con ManejadorHttpBurpIA

**Files:**
- Modify/Test: `src/test/java/com/burpia/ManejadorHttpBurpIAButtonsFlowIT.java` (nuevo)

**Step 1: Write failing end-to-end workflow tests**
- Escenario completo:
  - crear tareas por tráfico,
  - pausar/reanudar/cancelar desde gestor,
  - verificar que callback de analizador respeta cancelación/pausa,
  - limpiar completadas.

**Step 2: Run test to verify it fails**
- Run: `./gradlew test --tests "com.burpia.ManejadorHttpBurpIAButtonsFlowIT"`

**Step 3: Minimal implementation**
- Alinear transiciones en `ManejadorHttpBurpIA` y `GestorTareas` para evitar carrera de estados.

**Step 4: Verify pass**
- Run: `./gradlew test --tests "com.burpia.ManejadorHttpBurpIAButtonsFlowIT"`

### Task 5: Hardening final + build

**Files:**
- Modify: `docs/PLUGINS-BURP.md` (solo si hay discrepancias de comportamiento)

**Step 1: Full regression**
- Run: `./gradlew test`

**Step 2: Build artifact**
- Run: `./gradlew clean jar`
- Validate: `/Users/jaimearestrepo/Proyectos/BurpIA/build/libs/BurpIA-1.0.0.jar`

**Step 3: Smoke checklist manual (rápido)**
- Abrir UI y probar: pausar/reanudar/cancelar/reintentar/limpiar en estados mixtos.

