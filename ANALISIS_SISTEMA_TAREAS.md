# Análisis del Sistema de Tareas de BurpIA

**Fecha:** 2026-03-02
**Versión:** 1.0.2
**Propósito:** Documentar el comportamiento del sistema de tareas, especialmente en operaciones de pausa y cancelación

---

## 📋 Tabla de Contenidos

1. [Estados de Tarea](#estados-de-tarea)
2. [Transiciones de Estado](#transiciones-de-estado)
3. [Flujo de Vida de una Tarea](#flujo-de-vida-de-una-tarea)
4. [Operaciones de Control](#operaciones-de-control)
5. [Comportamiento al Pausar/Cancelar](#comportamiento-al-pausarcancelar)
6. [Sincronización y Thread Safety](#sincronización-y-thread-safety)
7. [Problemas Potenciales y Soluciones](#problemas-potenciales-y-soluciones)

---

## Estados de Tarea

El sistema maneja **7 estados posibles** para una tarea:

| Estado | Constante | Descripción | ¿Pausable? | ¿Cancelable? |
|--------|------------|-------------|-------------|---------------|
| **En Cola** | `ESTADO_EN_COLA` | Tarea creada, esperando ser procesada | ✅ Sí | ✅ Sí |
| **Analizando** | `ESTADO_ANALIZANDO` | Tarea siendo procesada por AI | ✅ Sí | ✅ Sí |
| **Pausado** | `ESTADO_PAUSADO` | Tarea temporalmente detenida | ❌ No | ✅ Sí (reanuda a En Cola) |
| **Completado** | `ESTADO_COMPLETADO` | Tarea finalizada exitosamente | ❌ No | ❌ No (final) |
| **Error** | `ESTADO_ERROR` | Tarea falló | ❌ No | ✅ Sí (reintentable) |
| **Cancelado** | `ESTADO_CANCELADO` | Tarea cancelada por usuario | ❌ No | ❌ No (final) |

---

## Transiciones de Estado

### Diagrama de Transiciones

```
                    ┌──────────────────┐
                    │   EN COLA        │ ◄──┐
                    └────────┬─────────┘    │ Reanudar
                             │              │ (desde Pausado)
                    Programar            │
                             ▼              │
                    ┌──────────────────┐    │
                    │   ANALIZANDO     │    │
                    └────────┬─────────┘    │
                             │              │
            ┌──────────────┼──────────────┤
            ▼              ▼              │
       ┌─────────┐   ┌─────────┐         │
       │ Pausado │   │Completado│         │
       └────┬────┘   └─────────┘         │
            │                            │
            │ Reanudar                   │
            ▼                            │
       ┌─────────┐                       │
       │  Error  │◄──────────────────────┘
       └────┬────┘
            │ Reintentar
            ▼
       ┌─────────┐
       │Cancelado│ (usuario canceló)
       └─────────┘
```

### Reglas de Transición

1. **EN_COLA → ANALIZANDO**: Cuando `LimitadorTasa` permite y `ExecutorService` tiene capacidad
2. **EN_COLA/ANALIZANDO → PAUSADO**: Usuario pausa todas las tareas activas
3. **PAUSADO → EN_COLA**: Usuario reanuda tareas pausadas
4. **EN_COLA/ANALIZANDO/PAUSADO → CANCELADO**: Usuario cancela todas las tareas activas
5. **ANALIZANDO → COMPLETADO**: Análisis AI finalizó exitosamente
6. **ANALIZANDO → ERROR**: Análisis AI falló (timeout, error de API, etc.)
7. **ERROR/CANCELADO → EN_COLA**: Si se reintenta la tarea

---

## Flujo de Vida de una Tarea

### 1. Creación de Tarea

**Entrada:** HTTP request/response (pasivo) o menú contextual (manual)

```java
// GestorTareas.java:53-68
public Tarea crearTarea(String tipo, String url, String estado, String mensajeInfo) {
    String id = UUID.randomUUID().toString();
    Tarea tarea = new Tarea(id, tipo, url, estado);
    tarea.establecerMensajeInfo(mensajeInfo);

    candado.lock();
    try {
        tareas.put(id, tarea); // Almacenar en ConcurrentHashMap
        List<String> idsPurgadas = modeloTabla.agregarTareaYObtenerIdsPurgadas(tarea);
        sincronizarTareasConPurgadoModelo(idsPurgadas, id);
        registrar("Tarea creada: " + tipo + " - " + url);
    } finally {
        candado.unlock();
    }
    return tarea;
}
```

**Estado inicial:** `ESTADO_EN_COLA`
**Ubicación:** `tareas` (ConcurrentHashMap) + `ModeloTablaTareas`

---

### 2. Programación de Ejecución

**Ubicación:** `ManejadorHttpBurpIA.programarAnalisis()` → `ejecutarAnalisisExistente()`

```java
// ManejadorHttpBurpIA.java:457-533
private void ejecutarAnalisisExistente(String tareaId, SolicitudAnalisis solicitudAnalisis, String evidenciaId) {
    // 1. Verificar que se puede iniciar
    if (!puedeIniciarAnalisis("Reintento", url)) {
        gestorTareas.actualizarTarea(tareaId, Tarea.ESTADO_ERROR, "Bloqueada por config LLM");
        return;
    }

    // 2. Crear AnalizadorAI con callbacks de estado
    AnalizadorAI analizador = new AnalizadorAI(
        solicitudAnalisis,
        config.crearSnapshot(),
        stdout, stderr, limitador,
        manejadorResultado,
        // Callback: Marcar como analizando
        () -> {
            boolean marcada = gestorTareas.marcarTareaAnalizando(id, "Analizando");
            return !marcada; // Retorna true si NO se pudo marcar (tarea cancelada/error)
        },
        gestorConsola,
        // Callback: Verificar si está cancelada
        () -> {
            return gestorTareas != null && gestorTareas.estaTareaCancelada(id);
        },
        // Callback: Verificar si está pausada
        () -> {
            return gestorTareas != null && gestorTareas.estaTareaPausada(id);
        },
        controlBackpressure
    );

    // 3. Enviar a ExecutorService
    Future<?> future = executorService.submit(analizador);
    ejecucionesActivas.put(id, future); // Rastrear Future activa
}
```

---

### 3. Durante Ejecución (Estado ANALIZANDO)

**Mientras analiza:**
- Tarea en estado `ANALIZANDO`
- `Future<?>` almacenado en `ejecucionesActivas`
- AnalizadorAI verifica periódicamente si está cancelada/pausada
- Tiempo acumulado se actualiza en tiempo real

**Verificación de cancelación durante análisis:**

```java
// AnalizadorAI ejecuta periódicamente:
if (verificadorCancelacion.getAsBoolean()) {
    // Tarea fue cancelada, interrumpir análisis
    return;
}
```

---

## Operaciones de Control

### 1. Pausar Todas las Tareas Activas

**Ubicación:** `GestorTareas.pausarTodasActivas()`

```java
public void pausarTodasActivas() {
    int pausadas = actualizarEstadosMasivo(
        tarea -> Tarea.esEstadoPausable(tarea.obtenerEstado()),
        Tarea.ESTADO_PAUSADO
    );
    registrar("Tareas pausadas: " + pausadas);
}
```

**¿Qué tareas se pausan?**
- `EN_COLA` → `PAUSADO` ✅
- `ANALIZANDO` → `PAUSADO` ✅
- `PAUSADO` → Ya está pausada (no se afecta)
- `COMPLETADO/ERROR/CANCELADO` → Final (no se pausan)

**Efecto en tareas que están ANALIZANDO:**
1. Se cambia estado a `PAUSADO`
2. `actualizarFilaTabla()` actualiza UI
3. `notificarManejador(manejadorPausa, id, "pausa")` llama a `cancelarEjecucionActiva()`

---

### 2. Reanudar Tareas Pausadas

**Ubicación:** `GestorTareas.reanudarTodasPausadas()`

```java
public void reanudarTodasPausadas() {
    int reanudadas = actualizarEstadosMasivo(
        tarea -> Tarea.esEstadoReanudable(tarea.obtenerEstado()),
        Tarea.ESTADO_EN_COLA
    );
    registrar("Tareas reanudadas: " + reanudadas);
}
```

**¿Qué tareas se reanudan?**
- `PAUSADO` → `EN_COLA` ✅
- Otras → No se afectan

**Efecto:**
1. Estado cambia a `EN_COLA`
2. `notificarManejador(manejadorReanudar, id, "reanudar")` llama a `reencolarTarea()`

---

### 3. Cancelar Todas las Tareas

**Ubicación:** `GestorTareas.cancelarTodas()`

```java
public void cancelarTodas() {
    List<String> idsCanceladas = actualizarEstadosMasivoConIds(
        Tarea::esActiva, // Filtra tareas activas
        Tarea.ESTADO_CANCELADO
    );
    registrar("Tareas canceladas: " + idsCanceladas.size());
    notificarCancelaciones(idsCanceladas); // Llama a cancelarEjecucionActiva() para cada ID
}
```

**¿Qué tareas se cancelan?**
- `EN_COLA` → `CANCELADO` ✅
- `ANALIZANDO` → `CANCELADO` ✅
- `PAUSADO` → `CANCELADO` ✅
- `COMPLETADO/ERROR/CANCELADO` → Final (no se cancelan de nuevo)

---

## Comportamiento al Pausar/Cancelar

### ⚠️ ESCENARIO 1: Tarea en Estado EN_COLA → Pausar

**Qué pasa:**
1. Estado cambia de `EN_COLA` a `PAUSADO`
2. **NO** hay `Future<?>` en `ejecucionesActivas` (todavía no se ha enviado a ExecutorService)
3. UI se actualiza para mostrar estado `PAUSADO`
4. **NO** se llama a `cancelarEjecucionActiva()` porque no hay ejecución activa

**Resultado:** ✅ Tarea simplemente cambia de estado, sin efectos colaterales

---

### ⚠️ ESCENARIO 2: Tarea en Estado ANALIZANDO → Pausar

**Qué pasa:**
1. Estado cambia de `ANALIZANDO` a `PAUSADO`
2. **SÍ** hay `Future<?>` en `ejecucionesActivas`
3. `notificarManejador(manejadorPausa, id, "pausa")` se ejecuta
4. Se llama a `cancelarEjecucionActiva(id)`:

```java
// ManejadorHttpBurpIA.java:411-422
public void cancelarEjecucionActiva(String tareaId) {
    Future<?> future = ejecucionesActivas.remove(tareaId);
    if (future != null) {
        boolean cancelada = future.cancel(true); // ⚠️ INTERRUMPIR EL HILO
        if (cancelada) {
            rastrear("Cancelación activa aplicada para tarea: " + tareaId);
        }
    }
}
```

**⚠️ PROBLEMA POTENCIAL:**
- `Future.cancel(true)` interrumpe el hilo de ejecución
- Si el AnalizadorAI está en medio de una llamada a API del LLM, la interrupción podría:
  - Dejar la conexión abierta
  - No liberar recursos
  - Causar leak de memoria o threads

**Resultado:** ⚠️ Tarea se marca como `PAUSADO` pero el hilo puede continuar ejecutándose hasta un punto de interrupción

---

### ⚠️ ESCENARIO 3: Tarea en Estado ANALIZANDO → Cancelar

**Qué pasa:**
1. Estado cambia de `ANALIZANDO` a `CANCELADO`
2. **SÍ** hay `Future<?>` en `ejecucionesActivas`
3. `notificarCancelacion(id)` se ejecuta
4. Se llama a `cancelarEjecucionActiva(id)`:

```java
Future<?> future = ejecucionesActivas.remove(tareaId); // ⚠️ SE REMUEVE DEL MAPA
future.cancel(true); // Intenta interrupción
```

**⚠️ PROBLEMA POTENCIAL:**
- La `Future` se elimina del mapa `ejecucionesActivas`
- Si el hilo NO se interrumpe inmediatamente, sigue ejecutándose
- Si completa el análisis (a pesar de estar marcado como `CANCELADO`):
  - El resultado se perderá
  - No se actualizará la tabla de hallazgos
  - Recursos (HTTP, conexión) pueden no liberarse correctamente

**Resultado:** ⚠️ Tarea marcada como `CANCELADA` pero el hilo puede continuar ejecutándose

---

### ⚠️ ESCENARIO 4: Múltiples Tareas en Cola → Cancelar Todo

**Qué pasa:**
1. Se llama a `cancelarTodas()`
2. `actualizarEstadosMasivoConIds()` itera sobre TODAS las tareas activas:
   ```java
   for (Tarea tarea : tareas.values()) {
       if (filtro.test(tarea)) { // esActiva() = esEstadoCancelable()
           tarea.establecerEstado(Tarea.ESTADO_CANCELADO);
           actualizarFilaTabla(tarea);
           idsActualizadas.add(tarea.obtenerId());
       }
   }
   ```
3. `notificarCancelaciones(idsCanceladas)` itera sobre todos los IDs:
   ```java
   for (String id : idsCanceladas) {
       notificarCancelacion(id); // Llama a cancelarEjecucionActiva()
   }
   ```

**⚠️ PROBLEMA DE RENDIMIENTO:**
- Si hay **muchas tareas** (ej: 100+ tareas), se itera dos veces sobre la colección
- Cada llamada a `cancelarEjecucionActiva()` busca en `ejecucionesActivas`
- **Bloqueo del candado** (`candado.lock()`) durante toda la operación
- UI puede congelarse temporalmente

---

## Sincronización y Thread Safety

### Candados (Locks)

**GestorTareas usa `ReentrantLock`:**

```java
private final ReentrantLock candado = new ReentrantLock();

// Operaciones que usan el candado:
- crearTarea() ✅
- actualizarTarea() ✅
- cancelarTodas() ✅
- pausarTodasActivas() ✅
- reanudarTodasPausadas() ✅
- limpiarCompletadas() ✅
- verificarTareasAtascadas() ✅
- pausarTarea() / reanudarTarea() ✅
```

**Problema:** Operaciones masivas como `cancelarTodas()` mantienen el candado durante toda la iteración.

---

### ConcurrentHashMap

```java
private final Map<String, Tarea> tareas = new ConcurrentHashMap<>();
```

**Ventaja:** Permite lecturas concurrentes sin bloqueo

**Problema:** Las operaciones de escritura (`put`, `remove`) aún pueden causar contención si hay mucha concurrencia

---

## Problemas Potenciales y Soluciones

### 🚨 PROBLEMA 1: Interrupción de Hilos No Confiable

**Síntoma:** Tarea marcada como `CANCELADA` o `PAUSADA` pero el hilo de AnalizadorAI continúa ejecutándose

**Causa:** `Future.cancel(true)` no garantiza interrupción inmediata:
- Si el hilo está en I/O bloqueante (esperando respuesta de API), `Thread.interrupt()` no siempre funciona
- El hilo puede completar el análisis antes de verificar el estado

**Evidencia en código:**

```java
// AnalizadorAI tiene verificación periódica:
if (verificadorPausa.getAsBoolean()) {
    // Si está pausada, lanza InterruptedException
    throw new InterruptedException();
}
```

**Pero esta verificación no es continua** - si el AnalizadorAI está en medio de una llamada HTTP a la API del LLM, puede no verificar el estado hasta que termine la llamada.

**Solución Propuesta:** Agregar verificación más frecuente durante operaciones I/O de larga duración.

---

### 🚨 PROBLEMA 2: Fuga de Futures en ejecucionesActivas

**Síntoma:** `ejecucionesActivas` crece indefinidamente

**Causa:**
```java
Future<?> future = ejecucionesActivas.remove(tareaId); // Se remueve del mapa
future.cancel(true); // Pero si no estaba en el mapa, no se limpia
```

**Escenario:**
1. Tarea se marca como `COMPLETADO`
2. `finalizarEjecucionActiva(id)` debería remover la `Future`
3. Pero si hay un error y no se llama, la `Future` queda en el mapa

**Evidencia en código:**

```java
// ManejadorHttpBurpIA.java:512-518
Future<?> future = executorService.submit(analizador);
String id = tareaIdRef.get();
if (id != null) {
    ejecucionesActivas.put(id, future);
}
```

**Falta:** Limpieza garantizada de `ejecucionesActivas` cuando la tarea finaliza

---

### 🚨 PROBLEMA 3: Condición de Carrera en Verificación de Cancelación

**Síntoma:** Tarea se reanuda pero sigue marcada como cancelada

**Causa:**

```java
// GestorTareas.java:347-358
public boolean estaTareaCancelada(String id) {
    candado.lock();
    try {
        Tarea tarea = tareas.get(id);
        if (tarea != null) {
            String estado = tarea.obtenerEstado();
            return Tarea.ESTADO_CANCELADO.equals(estado);
        }
        return false;
    } finally {
        candado.unlock();
    }
}
```

**Problema:** Entre el momento en que se verifica el estado y se toma la decisión, el estado puede cambiar.

**Ejemplo de race condition:**
1. Thread A (AnalizadorAI): Verifica `estaTareaCancelada()` → retorna `false`
2. Thread B (UI): Usuario hace click en "Cancelar Todo"
3. Thread A: Continúa ejecución (porque ya pasó la verificación)
4. Thread B: Marca tarea como `CANCELADA`
5. Thread A: Completa análisis y lo marca como `COMPLETADO` (sobrescribe `CANCELADA`)

---

### 🚨 PROBLEMA 4: Actualización Masiva Sin Control de Concurrency

**Síntoma:** UI se congela al pausar/cancelar muchas tareas

**Causa:**

```java
// GestorTareas.java:391-406
private List<String> actualizarEstadosMasivoConIds(Predicate<Tarea> filtro, String nuevoEstado) {
    List<String> idsActualizadas = new ArrayList<>();
    candado.lock(); // ⚠️ BLOQUEO DURANTE TODA LA ITERACIÓN
    try {
        for (Tarea tarea : tareas.values()) { // Itera sobre TODAS las tareas
            if (filtro.test(tarea)) {
                tarea.establecerEstado(nuevoEstado);
                actualizarFilaTabla(tarea); // ⚠️ ACTUALIZACIÓN UI DENTRO DEL LOCK
                idsActualizadas.add(tarea.obtenerId());
            }
        }
        return idsActualizadas;
    } finally {
        candado.unlock();
    }
}
```

**Problema:**
- Si hay 1000 tareas, el candado está mantenido por 1000 iteraciones
- `actualizarFilaTabla()` puede ser lento (actualiza Swing UI)
- Ninguna otra operación puede ejecutarse mientras tanto

---

## Recomendaciones

### 1. Limpieza Automática de ejecucionesActivas

**Problema:** Las `Future<?>` no se eliminan siempre de `ejecucionesActivas`

**Solución:** Agregar cleanup garantizado en un bloque `finally`:

```java
private void ejecutarAnalisisExistente(...) {
    final String id = tareaIdRef.get();
    Future<?> future = null;
    try {
        future = executorService.submit(analizador);
        if (id != null) {
            ejecucionesActivas.put(id, future);
        }
        // ... análisis se ejecuta
    } finally {
        // SIEMPRE limpiar la Future, sin importar el resultado
        if (id != null) {
            ejecucionesActivas.remove(id);
        }
    }
}
```

---

### 2. Verificación de Estado Más Frecuente

**Problema:** Interrupción de hilos no confiable

**Solución:** Agregar verificación de estado en puntos críticos de AnalizadorAI:

```java
// En AnalizadorAI, antes de llamadas I/O largas:
private void verificarEstadoAntesDeLlamadaAPI() throws InterruptedException {
    if (Thread.interrupted() || verificadorCancelacion.getAsBoolean()) {
        throw new InterruptedException("Tarea cancelada o interrumpida");
    }
}
```

---

### 3. Evitar Actualizaciones UI Dentro del Candado

**Problema:** Bloqueo de UI durante operaciones masivas

**Solución:** Separar adquisición de datos de actualización de UI:

```java
private List<String> actualizarEstadosMasivoConIds(...) {
    List<String> idsActualizadas = new ArrayList<>();
    List<Tarea> tareasAActualizar = new ArrayList<>();

    // FASE 1: Recoger datos (con candado)
    candado.lock();
    try {
        for (Tarea tarea : tareas.values()) {
            if (filtro.test(tarea)) {
                tarea.establecerEstado(nuevoEstado);
                idsActualizadas.add(tarea.obtenerId());
                tareasAActualizar.add(tarea); // Guardar referencia
            }
        }
    } finally {
        candado.unlock();
    }

    // FASE 2: Actualizar UI (sin candado)
    for (Tarea tarea : tareasAActualizar) {
        actualizarFilaTabla(tarea);
    }

    return idsActualizadas;
}
```

---

### 4. Marcar Tarea como Final Antes de Cancelar Future

**Problema:** Tarea completada sobrescribe estado `CANCELADA`

**Solución:** Verificar estado antes de completar:

```java
// En AnalizadorAI, antes de marcar como COMPLETADO:
if (gestorTareas.estaTareaCancelada(id) ||
    gestorTareas.estaTareaPausada(id)) {
    // No completar, mantener estado actual
    return;
}
gestorTareas.actualizarTarea(id, Tarea.ESTADO_COMPLETADO, resultado);
```

---

## Conclusión

### Fortalezas del Sistema Actual ✅

1. **Diseño modular:** Separación clara entre `GestorTareas`, `ManejadorHttpBurpIA`, y `AnalizadorAI`
2. **Thread-safe con candados:** Uso correcto de `ReentrantLock` para proteger estado compartido
3. **Verificación de estado:** Callbacks para verificar cancelación/pausa durante ejecución
4. **Retención de finalizadas:** Limpieza automática de tareas viejas (máx 2000 por defecto)
5. **Detección de tareas atascadas:** Monitorea tareas en `ANALIZANDO` por más de 5 minutos

---

### Debilidades y Riesgos ⚠️

1. **Interrupción de hilos no garantizada:** `Future.cancel(true)` puede no interumpir hilos en I/O
2. **Limpieza de `ejecucionesActivas` no confiable:** Futures pueden quedar huérfanas
3. **Race conditions:** Verificación de estado y actualización no son atómicas
4. **Bloqueo de UI en operaciones masivas:** Actualizaciones UI dentro del candado
5. **Sobrescritura de estado:** Tarea completada puede sobrescribir estado `CANCELADA`

---

### Prioridad de Mejoras

| Prioridad | Problema | Impacto | Complejidad |
|-----------|----------|---------|-------------|
| **ALTA** | Limpieza de ejecucionesActivas | Memory leak | Media |
| **ALTA** | Verificación pre-completado | Sobrescritura de estado | Baja |
| **MEDIA** | Actualizar UI fuera del candado | Congelamiento UI | Media |
| **MEDIA** | Verificación más frecuente | Hilos zombies | Media |
| **BAJA** | Atomicidad de estado | Race conditions | Alta |

---

**Fin del análisis**
