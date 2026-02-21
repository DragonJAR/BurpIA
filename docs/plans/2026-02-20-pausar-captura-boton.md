# Pausar/Reanudar Captura Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Reemplazar el botón lateral `Limpiar` por un botón `⏸️` / `▶️` que pause y reanude la captura de solicitudes HTTP dentro de scope.

**Architecture:** Se añadirá un estado explícito de captura activa (`capturaActiva`) en el flujo principal de análisis HTTP. La UI leerá/escribirá ese estado mediante callbacks de `PestaniaPrincipal`/`PanelEstadisticas`. Si la captura está pausada, `ManejadorHttpBurpIA` ignorará nuevas solicitudes (sin encolar tareas ni afectar análisis ya en ejecución).

**Tech Stack:** Java 21, Swing, Montoya API, JUnit 5.

---

### Task 1: Modelo de estado de captura (single source of truth)

**Files:**
- Modify: `src/main/java/com/burpia/ExtensionBurpIA.java`
- Modify: `src/main/java/com/burpia/ManejadorHttpBurpIA.java`

**Step 1: Añadir estado de captura y API mínima**
- En `ManejadorHttpBurpIA`:
  - `private volatile boolean capturaActiva = true;`
  - `public void pausarCaptura()`
  - `public void reanudarCaptura()`
  - `public boolean estaCapturaActiva()`
- No usar flags duplicados en otros componentes.

**Step 2: Aplicar guard clause en captura HTTP**
- En `handleHttpResponseReceived(...)`:
  - Si `!capturaActiva`, retornar `continueWith(...)` inmediatamente.
  - No crear hash, no deduplicar, no encolar tareas, no modificar estadísticas de análisis.

**Step 3: Logging claro**
- Log de transición:
  - `Captura pausada por usuario`
  - `Captura reanudada por usuario`

### Task 2: Conectar botón lateral con el estado de captura

**Files:**
- Modify: `src/main/java/com/burpia/ui/PanelEstadisticas.java`
- Modify: `src/main/java/com/burpia/ui/PestaniaPrincipal.java`
- Modify: `src/main/java/com/burpia/ExtensionBurpIA.java`

**Step 1: Reemplazar botón Limpiar por botón de captura**
- Botón lateral que hoy era limpiar:
  - estado activo: `⏸️` (tooltip: "Pausar captura de peticiones")
  - estado pausado: `▶️` (tooltip: "Reanudar captura de peticiones")

**Step 2: Callback DRY desde UI hacia extensión**
- Añadir callback único tipo `Runnable` o `Consumer<Boolean>` para toggle.
- Evitar que `PanelEstadisticas` conozca detalles de `ManejadorHttpBurpIA`.

**Step 3: Sin romper botón Ajustes**
- Mantener botón Ajustes como está.
- Misma alineación/tamaño de ambos botones.

### Task 3: Ajustar texto e indicadores visibles

**Files:**
- Modify: `src/main/java/com/burpia/ui/PanelEstadisticas.java`

**Step 1: Reflejar estado de captura**
- Añadir indicador breve en línea de `DETALLES OPERATIVOS`:
  - `⏸️ Captura: Pausada` o `▶️ Captura: Activa`
- Mantener una sola línea y spacing consistente.

**Step 2: Evitar acciones destructivas implícitas**
- Confirmar que ya no exista acción “limpiar” asociada al botón lateral.
- La limpieza de datos queda solo donde ya exista explícitamente.

### Task 4: Pruebas de regresión

**Files:**
- Modify/Test: `src/test/java/com/burpia/ManejadorHttpBurpIA...` (crear/ajustar tests)
- Modify/Test: `src/test/java/com/burpia/ui/PanelEstadisticas...` (crear/ajustar tests)

**Step 1: Test funcional de captura pausada**
- Dado `capturaActiva=false`, una respuesta dentro de scope no genera tarea ni hallazgo.

**Step 2: Test de reanudación**
- Tras `reanudarCaptura()`, nuevas respuestas dentro de scope sí se procesan.

**Step 3: Test de UI botón toggle**
- Verificar cambio de ícono/texto/tooltip al alternar.
- Verificar invocación del callback correcto.

### Task 5: Validación final y build

**Files:**
- No additional changes

**Step 1: Run full tests**
- `./gradlew test`

**Step 2: Build jar**
- `./gradlew clean jar`

**Step 3: Confirm artifact**
- `/Users/jaimearestrepo/Proyectos/BurpIA/build/libs/BurpIA-1.0.0.jar`

