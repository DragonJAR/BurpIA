# Plan: Mejorar respuesta inmediata al pausar/cancelar tareas

## Problema Identificado

**Síntoma:** Cuando se pausa o cancela una tarea en ejecución, la siguiente tarea en cola no comienza inmediatamente.

**Causa raíz:**

1. **Llamada HTTP bloqueante:**
   - `AnalizadorAI.llamarAPIAI()` usa `clienteHttp.newCall(request).execute()`
   - Esta es una llamada **síncrona bloqueante** que espera la respuesta del servidor
   - Timeout típico: 60-120 segundos

2. **Interrupción ineficaz:**
   - Cuando se pausa/cancela: `future.cancel(true)` solo interrumpe el thread
   - OkHttp **no responde inmediatamente** a `Thread.interrupt()` en llamadas bloqueantes
   - La llamada HTTP continúa hasta que:
     - El servidor responde
     - El timeout expira

3. **Slot ocupado:**
   - El `LimitadorTasa` tiene un permiso adquirido
   - No se libera hasta que la llamada HTTP termina
   - La siguiente tarea espera por un slot disponible

**Flujo actual (PROBLEMÁTICO):**
```
Usuario hace clic en "Pausar" → Tarea marcada como PAUSADA
→ future.cancel(true) llamado
→ Thread.interrupt() llamado
→ [Llamada HTTP continúa bloqueada por hasta 60-120 segundos]
→ Llamada HTTP finalmente termina / timeout
→ Permiso de limitador liberado
→ Siguiente tarea puede comenzar
```

**Demora:** 60-120 segundos (hasta 2 minutos) antes de que la siguiente tarea inicie

---

## Solución Propuesta

### Enfoque: Usar `Call.cancel()` de OkHttp

OkHttp tiene un método `Call.cancel()` que:
- ✅ **Cierra inmediatamente** la conexión HTTP subyacente
- ✅ **Libera el socket** inmediatamente
- ✅ **Lanza `IOException`** ("Canceled") en el thread que ejecuta la llamada
- ✅ **Funciona incluso** si la llamada está bloqueada esperando respuesta

**Arquitectura:**

```
┌─────────────────────────────────────────────────────────────┐
│                    GestorTareas                              │
│  pausarTarea() / cancelarTarea()                             │
│        ↓                                                     │
│  notificarManejador(manejadorPausa, id, "pausa")             │
└────────────────┬────────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────────────┐
│                ManejadorHttpBurpIA                           │
│  cancelarEjecucionActiva(id)                                 │
│    - future.cancel(true)                                     │
│    - call.cancel()  ← NUEVO: cancelar llamada HTTP          │
└─────────────────────────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────────────┐
│                   AnalizadorAI                               │
│  llamarAPIAI()                                               │
│    - call.execute()                                          │
│    - Si call.cancel() fue llamado:                           │
│      → Lanza IOException("Canceled")                         │
│      → libera permiso de limitador                           │
│      → siguiente tarea comienza inmediatamente               │
└─────────────────────────────────────────────────────────────┘
```

---

## Plan de Implementación

### Fase 1: Almacenar referencia a Call en AnalizadorAI

**Archivo:** `src/main/java/com/burpia/analyzer/AnalizadorAI.java`

**Cambios:**
1. Agregar campo para almacenar la llamada HTTP activa:
```java
private volatile Call llamadaHttpActiva;
```

2. Modificar `llamarAPIAI()` para almacenar la referencia:
```java
Call call = clienteHttp.newCall(solicitudHttp);
llamadaHttpActiva = call;  // Almacenar antes de ejecutar
try (Response respuesta = call.execute()) {
    // ... código existente
} finally {
    llamadaHttpActiva = null;  // Limpiar referencia
}
```

### Fase 2: Crear método para cancelar llamada HTTP

**Archivo:** `src/main/java/com/burpia/analyzer/AnalizadorAI.java`

**Agregar método público:**
```java
/**
 * Cancela la llamada HTTP activa si existe.
 * Libera inmediatamente el thread y el socket.
 */
public void cancelarLlamadaHttpActiva() {
    Call call = llamadaHttpActiva;
    if (call != null && !call.isCanceled()) {
        call.cancel();
    }
}
```

### Fase 3: Crear interfaz de callback de cancelación

**Archivo:** `src/main/java/com/burpia/analyzer/AnalizadorAI.java`

**Modificar interfaz Callback:**
```java
public interface Callback {
    void alCompletarAnalisis(ResultadoAnalisisMultiple resultado);
    void alErrorAnalisis(String error);
    default void alCanceladoAnalisis() {}

    // NUEVO: callback cuando se solicita cancelación
    default void alSolicitarCancelacion() {}
}
```

### Fase 4: Conectar ManejadorHttpBurpIA con AnalizadorAI

**Archivo:** `src/main/java/com/burpia/ManejadorHttpBurpIA.java`

**1. Crear mapa para rastrear analizadores activos:**
```java
private final ConcurrentHashMap<String, AnalizadorAI> analizadoresActivos = new ConcurrentHashMap<>();
```

**2. Modificar `ejecutarAnalisisExistente()`:**
```java
// Crear analizador
AnalizadorAI analizador = new AnalizadorAI(...);

// Crear callback con referencia al analizador
AnalizadorAI.Callback callbackConCancelacion = new AnalizadorAI.Callback() {
    @Override
    public void alSolicitarCancelacion() {
        // Cancelar llamada HTTP activa
        analizador.cancelarLlamadaHttpActiva();
    }
    // ... otros métodos
};

// Almacenar referencia antes de ejecutar
String id = tareaIdRef.get();
if (id != null) {
    analizadoresActivos.put(id, analizador);
}

try {
    future = executorService.submit(analizador);
    // ...
} finally {
    // Limpiar referencia cuando termine
    if (id != null) {
        analizadoresActivos.remove(id);
    }
}
```

**3. Modificar `cancelarEjecucionActiva()`:**
```java
public void cancelarEjecucionActiva(String tareaId) {
    if (Normalizador.esVacio(tareaId)) {
        return;
    }

    // NUEVO: Cancelar llamada HTTP activa primero
    AnalizadorAI analizador = analizadoresActivos.get(tareaId);
    if (analizador != null) {
        analizador.cancelarLlamadaHttpActiva();
    }

    // Existente: cancelar Future
    Future<?> future = ejecucionesActivas.remove(tareaId);
    if (future != null) {
        boolean cancelada = future.cancel(true);
        if (cancelada) {
            rastrear("Cancelación activa aplicada para tarea: " + tareaId);
        }
    }
}
```

### Fase 5: Manejar IOException de cancelación en AnalizadorAI

**Archivo:** `src/main/java/com/burpia/analyzer/AnalizadorAI.java`

**Modificar bloque `catch` en `run()`:**
```java
catch (IOException e) {
    long duracion = System.currentTimeMillis() - tiempoInicio;
    String mensaje = e.getMessage();

    // Detectar si es cancelación de OkHttp
    if ("Canceled".equals(mensaje) || (mensaje != null && mensaje.contains("Canceled"))) {
        if (esPausada()) {
            registrar("Análisis pausado y liberando hilo (" + duracion + "ms)");
            return;
        }
        if (esCancelada()) {
            registrar("Análisis cancelado por usuario (" + duracion + "ms)");
            callback.alCanceladoAnalisis();
            return;
        }
    }

    // Manejo existente de errores
    String falloMsg = PoliticaReintentos.obtenerMensajeErrorAmigable(e);
    registrarError("Análisis fallido después de " + duracion + "ms: " + falloMsg);
    callback.alErrorAnalisis(falloMsg);
}
```

### Fase 6: Actualizar método `detener()` para limpiar

**Archivo:** `src/main/java/com/burpia/ManejadorHttpBurpIA.java`

```java
public void detener() {
    // Cancelar todas las llamadas HTTP activas
    for (AnalizadorAI analizador : analizadoresActivas.values()) {
        analizador.cancelarLlamadaHttpActiva();
    }
    analizadoresActivos.clear();

    // Existente: cancelar futures y limpiar
    for (Future<?> future : ejecucionesActivas.values()) {
        future.cancel(true);
    }
    ejecucionesActivas.clear();

    if (executorService != null && !executorService.isShutdown()) {
        executorService.shutdownNow();
    }
}
```

---

## Beneficios Esperados

### Confiabilidad ✅
- Las llamadas HTTP se cancelan **inmediatamente** (no esperan timeout)
- El thread se libera **inmediatamente**
- El permiso del limitador se libera **inmediatamente**

### Eficiencia ✅
- La siguiente tarea comienza **en milisegundos** (no en minutos)
- No se desperdician threads esperando timeouts
- No se desperdician sockets esperando respuestas

### DRY ✅
- Reutiliza `cancelarEjecucionActiva()` que ya existe
- Reutiliza el mecanismo de `future.cancel()`
- No duplica lógica de cancelación

### UX ✅
- El usuario ve respuesta inmediata al pausar/cancelar
- La cola de tareas continúa fluidamente
- No hay "bloqueos" de 2 minutos

---

## Comparación: Antes vs Después

| Aspecto | Antes | Después |
|---------|-------|---------|
| **Tiempo de respuesta** | 60-120 segundos | < 1 segundo |
| **Llamada HTTP** | Espera hasta timeout | Cancelada inmediatamente |
| **Thread del Executor** | Bloqueado hasta timeout | Liberado inmediatamente |
| **Permiso de limitador** | Ocupado hasta timeout | Liberado inmediatamente |
| **Siguiente tarea** | Espera 2 minutos | Comienza inmediatamente |
| **UX del usuario** | Confusión ("¿por qué no continúa?") | Flujo fluido |

---

## Archivos a Modificar

1. `src/main/java/com/burpia/analyzer/AnalizadorAI.java`
   - Agregar campo `llamadaHttpActiva`
   - Modificar `llamarAPIAI()`
   - Agregar `cancelarLlamadaHttpActiva()`
   - Actualizar bloque `catch IOException`

2. `src/main/java/com/burpia/ManejadorHttpBurpIA.java`
   - Agregar mapa `analizadoresActivos`
   - Modificar `ejecutarAnalisisExistente()`
   - Modificar `cancelarEjecucionActiva()`
   - Modificar `detener()`

---

## Testing Plan

### Caso 1: Pausar tarea durante llamada HTTP
1. Iniciar análisis de múltiples URLs
2. Mientras primera tarea está "Analizando", hacer clic en "Pausar"
3. **Verificar:** La tarea se marca como "Pausada" inmediatamente (< 2 segundos)
4. **Verificar:** La siguiente tarea comienza inmediatamente

### Caso 2: Cancelar tarea durante llamada HTTP
1. Iniciar análisis de múltiples URLs
2. Mientras primera tarea está "Analizando", hacer clic en "Cancelar"
3. **Verificar:** La tarea se marca como "Cancelada" inmediatamente (< 2 segundos)
4. **Verificar:** La siguiente tarea comienza inmediatamente

### Caso 3: Pausar tarea pausada (reanudar)
1. Pausar una tarea
2. Reanudar la tarea
3. **Verificar:** La tarea continúa normalmente
4. **Verificar:** No hay errores ni excepciones

### Caso 4: Múltiples tareas pausadas/canceladas
1. Iniciar 10 tareas en cola
2. Pausar/cancelar 5 tareas mientras analizan
3. **Verificar:** Las 5 tareas restantes continúan sin interrupción
4. **Verificar:** No hay bloqueos ni deadlocks

---

## Riesgos y Mitigación

### Riesgo 1: Condición de carrera (race condition)
**Descripción:** `llamadaHttpActiva` podría ser null cuando se intenta cancelar
**Mitigación:** Usar `volatile` y verificación de null en `cancelarLlamadaHttpActiva()`

### Riesgo 2: Limpieza de referencia
**Descripción:** La referencia podría permanecer en el mapa después de terminar
**Mitigación:** Limpiar en bloque `finally` y en `detener()`

### Riesgo 3: Compatibilidad con código existente
**Descripción:** El cambio podría afectar otros componentes que dependen del comportamiento actual
**Mitigación:** La modificación es aditiva, no cambia el flujo normal

---

## Estimación de Esfuerzo

- **Fase 1:** 30 minutos (almacenar referencia)
- **Fase 2:** 15 minutos (método cancelar)
- **Fase 3:** 15 minutos (interfaz callback)
- **Fase 4:** 45 minutos (conectar componentes)
- **Fase 5:** 30 minutos (manejar IOException)
- **Fase 6:** 15 minutos (limpieza)
- **Testing:** 60 minutos

**Total:** ~3-4 horas de desarrollo

---

## Conclusión

Esta solución es:
- ✅ **Más confiable**: Cancelación inmediata de llamadas HTTP
- ✅ **Más eficiente**: Libera recursos inmediatamente
- ✅ **Sigue DRY**: Reutiliza mecanismos existentes
- ✅ **Minimal**: Cambios pequeños y localizados
- ✅ **Mejora UX significativamente**: Respuesta inmediata

**Recomendación:** Implementar según el plan descrito.
