# MEJORAS — Auditoría exhaustiva de flujos de ejecución

Revisión línea por línea de todos los flujos de ejecución del plugin BurpIA,
considerando uso en inglés/español, casos límite, entradas inválidas y escenarios
de error. Cada entrada documenta el problema detectado, la solución aplicada y
su validación.

---

## Resumen ejecutivo

| Área | Hallazgos | Corregidos | Documentados (mejora futura) | Descartados |
|------|-----------|------------|------------------------------|-------------|
| Concurrencia / cancelación | 8 | 4 (C1, C2, #4, UI2) | 2 (H3, UI1) | 2 (H4, Estadisticas) |
| Pipeline LLM / parsing JSON | 3 | 2 | 1 | — |
| util/ (recursos, overflow, null) | 4 | 4 | — | — |
| config/ (validación, casing) | 2 | 2 | — | — |
| flow/ (análisis multi-request) | 2 | 1 (HIGH) | 1 | — |
| model/ (DTOs, validación) | 5 | — | 3 | 2 (limpios) |
| i18n / edge cases | 7 | 1 (#7) | 1 (findings cap) | 5 (seguros) |
| i18n (format-string crashes) | 0 | — | — | — |

**Validación final:** 1668 pruebas pasadas, PMD verde con reglas de código muerto,
JAR `BurpIA-1.6.0.jar`.

---

## Cambios aplicados

### 1. Race condition: hallazgos fantasma tras cancelación (C1)

**Archivo:** `execution/TaskExecutionManager.java`

**Problema:** Al cancelar una tarea mientras el HTTP completaba (ventana no
interrumpible entre `ejecutarAnalisisCompleto()` y `alCompletarAnalisis`), el
callback no verificaba cancelación y mostraba hallazgos fantasma + revivía la
tarea (Cancelado→Completado).

**Solución:** Re-check de cancelación al inicio de `alCompletarAnalisis`
reutilizando `GestorTareas.estaTareaCancelada(id)` (DRY).

---

### 2. Resurrección de tareas en estado final (C2)

**Archivo:** `util/GestorTareas.java`

**Problema:** `actualizarTarea` no tenía guard contra transiciones desde estado
final (COMPLETADO/ERROR/CANCELADO), permitiendo revivir tareas canceladas.
Asimetría con `marcarTareaAnalizando` que sí lo tenía.

**Solución:** Guard anti-resurrección reutilizando `Tarea.esEstadoFinal`
(DRY), permitiendo idempotencia pero no downgrade.

---

### 3. Bounds check defensivo en reparación de JSON truncado

**Archivo:** `util/ReparadorJson.java`

**Problema:** El offset `indice` podía exceder `texto.length()` con campos
truncados del LLM (`{"evidencia":` sin valor).

**Solución:** `Math.min(...)` para limitar el offset.

---

### 4. Leak de OkHttpClient en ConnectionTester (U1)

**Archivo:** `util/ConnectionTester.java`

**Problema:** `crearClienteParaConfiguracion` usaba `new OkHttpClient.Builder()`
que crea dispatcher y connection pool propios que **nunca se cierran**. Cada test
de conexión / listado de modelos acumulaba threads y conexiones huérfanas.

**Solución:** Reutilizar `clienteBase.newBuilder()` (DRY — mismo patrón que ya
usan otros métodos del archivo), que comparte el dispatcher/pool de `clienteBase`
limpiado en `cerrar()`.

---

### 5. NumberFormatException en comparación de versiones (U2)

**Archivo:** `util/ConnectionTester.java`

**Problema:** `compararVersiones` hacía `Integer.parseInt` sobre partes de
versión que podían estar vacías (`.1.0` → `["", "1", "0"]`), lanzando
`NumberFormatException` y fallando la detección de actualizaciones.

**Solución:** Helper `parsearParteVersion` con guard de empty + try/catch.

---

### 6. NPE en GestorTareas con modeloTabla null (U3)

**Archivo:** `util/GestorTareas.java`

**Problema:** El constructor toleraba `modeloTabla == null` (guard en línea 47),
pero 6 métodos lo dereferenciaban sin guard (crearTarea, actualizarFilaTabla,
limpiar, etc.), causando NPE en el primer uso.

**Solución:** `Objects.requireNonNull(modeloTabla)` en el constructor — alinea
el contrato con el uso real (ningún caller pasa null). Elimina el guard
paranoico y la clase entera de NPEs potenciales.

---

### 7. Integer overflow en presupuesto de bytes (U4)

**Archivo:** `util/HttpUtils.java`

**Problema:** `maxCaracteres * 4` podía hacer overflow a negativo con
`maxCaracteres` grande (>536M), causando truncado silencioso a 64 bytes.

**Solución:** Cómputo en `long` antes de castear a `int`.

---

### 8. Off-by-one en búsqueda de llaves balanceadas (A1)

**Archivo:** `analyzer/ParseadorRespuestasAI.java`

**Problema:** `extraerObjetosNoEstrictos` pasaba `profundidadInicial=1` a
`buscarIndiceCorcheteBalanceado`, pero el helper ya cuenta el delimitador en
`inicio`. El conteo duplicado dejaba el path principal de extracción
**efectivamente muerto** (siempre retornaba -1), forzando fallback regex menos
confiable.

**Solución:** Cambiar a `profundidadInicial=0` (consistente con línea 351 que
correctamente usa 0 para `[`/`]`).

---

### 9. Severidad heredada entre hallazgos en parser texto-plano (A2)

**Archivo:** `analyzer/ParseadorRespuestasAI.java`

**Problema:** Al detectar un nuevo título en `parsearTextoPlano`, se reseteaba
`descripcion` pero no `severidad`/`confianza`. Un hallazgo sin línea
`Severity:` heredaba la severidad del hallazgo anterior (ej: erróneamente HIGH).

**Solución:** Reset de `severidad` y `confianza` a defaults al iniciar nuevo
hallazgo.

---

### 10. Proveedor incorrecto en multi-proveedor con un solo elemento (A3)

**Archivo:** `analyzer/GestorMultiProveedor.java`

**Problema:** El branch `size()==1` delegaba a `ejecutarAnalisisProveedorUnico()`
que usa el proveedor **default** de config, ignorando el proveedor real de la
lista. Latente (callers actuales guard con `size()>1`), pero bug si se relaja.

**Solución:** Usar `ejecutarAnalisisProveedor(proveedores.get(0), modelo)`
con el proveedor real.

---

### 11. Validación de API key con lowercasing frágil (C1)

**Archivo:** `config/ConfigValidator.java`

**Problema:** `validarApiKey` hacía `.toLowerCase()` sobre el nombre canónico
del proveedor antes del switch, acoplamiento frágil: si el casing canónico
cambiaba (ej: "Z.ai"), el switch dejaba de validar prefijos silenciosamente.
Además, el case `"ollama"` era código muerto (Ollama retorna antes por no
requerir key).

**Solución:** Switch directo sobre el nombre canónico sin lowercasing, con
cases reales (`"OpenAI"`, `"Claude"`, `"Gemini"`). Eliminado case muerto y
import `Locale` huérfano.

---

### 12. Floor de timeout Moonshot saltado con casing no-canónico (C2)

**Archivo:** `config/ConfiguracionAPI.java`

**Problema:** `obtenerTiempoEsperaParaModelo` comparaba `"Moonshot (Kimi)"`
con `.equals(proveedor)` crudo. Variantes de casing/espaciado no activaban
el floor de 120s, dejando timeouts cortos para Moonshot.

**Solución:** Normalizar el proveedor antes de comparar
(`normalizarProveedor(proveedor)`).

---

### 13. Debounce de menú contextual con hashCode colisionante (UI2)

**Archivo:** `ui/FabricaMenuContextual.java`

**Problema:** El debounce usaba `String.hashCode()` como key de identidad.
`hashCode` tiene colisiones conocidas ("Aa" == "BB"), así que dos requests
**distintos** con hashCode colisionante dentro de la ventana de 500ms se
trataban como duplicados y se descartaban silenciosamente.

**Solución:** Verificar además `contenido.equals(previo.contenido)` antes de
descartar. Añadido campo `contenido` a `RegistroClic`.

---

### 14. Análisis de flujo multi-request degradaba a single-request (HIGH)

**Archivo:** `flow/FlowAnalysisRequestBuilder.java`

**Problema:** El constructor de `SolicitudAnalisis` cableaba `promptFlujo` en el
9º argumento (`cuerpoRespuesta`) en vez del 10º (`promptPreconstruido`). El
orquestador, al ver `promptPreconstruido == null`, caía en el fallback de prompt
single-request. El análisis contextual multi-request (funcionalidad central del
plugin) **silenciosamente no funcionaba** — analizaba cada request por separado.
Confirmado por `FlowAnalysisRequestBuilderTest` que fallaba.

**Solución:** Usar el constructor de 10 args para que `promptFlujo` vaya a
`promptPreconstruido`. El test ahora pasa.

---

### 15. Retry storm por clock skew en Retry-After con formato fecha (#7)

**Archivo:** `analyzer/PoliticaReintentos.java`

**Problema:** `parsearRetryAfterMs` hacía `Math.max(0L, deltaMs)` en el path de
fecha RFC-1123. Si el clock del sistema saltaba hacia atrás (NTP, migración VM),
la fecha del `Retry-After` quedaba en el pasado → deltaMs negativo → clamp a 0 →
retry inmediato. Con `MAX_INTENTOS_RETRY=5`, un 429 con Retry-After de fecha
producía un storm de 5 reintentos inmediatos contra el endpoint rate-limited.

**Solución:** Floor en `ESPERA_MINIMA_MS` (1000ms) en vez de `0L`, consistente
con el path de segundos (línea 281). Test actualizado para reflejar el
comportamiento corregido.

---

### 16. Callback a UI destruida durante reload de extensión (#4)

**Archivo:** `execution/TaskExecutionManager.java`

**Problema:** `pestaniaPrincipal` es `final` y nunca se nullea. Tras
`shutdown()` (que espera solo 5s), un worker que complete entre t=5s y el
dispose de la UI posteaba hallazgos a una `PestaniaPrincipal` ya destruida. El
guard `if (pestaniaPrincipal != null)` no protegía porque la referencia stale
seguía apuntando al objeto destruido.

**Solución:** Flag `volatile boolean cerrando` seteado al inicio de `shutdown()`,
chequeado en `alCompletarAnalisis` y `alErrorAnalisis` antes de postear al EDT.

---

## Hallazgos evaluados y NO corregidos (mejora futura)

### H3 — Tarea pausada mantiene permiso del limitador
**Archivo:** `analyzer/AnalizadorAI.java` (líneas 216-222)
**Razón:** Degradación de eficiencia (MEDIUM), no deadlock. Corregirlo requiere
refactor de concurrencia con riesgo de deadlocks peores.

### UI1 — EDT bloquea ~1.5s en destruir PanelAgente
**Archivo:** `ui/PanelAgente.java` (línea 450, `cerrarSesionActiva`)
**Razón:** Mover el `waitFor` bloqueante fuera del EDT en contexto de unload
arriesga dejar procesos PTY huérfanos si Burp cierra la extensión rápido. La
congelación breve en unload es preferible a procesos zombi.

### ReparadorJson — repair de comillas mixtas
**Archivo:** `util/ReparadorJson.java` (líneas 171-183)
**Razón:** El parser tiene fallback robusto (texto plano). Cambiar la heurística
de repair de comillas tiene alto riesgo de regresión sin batería de tests con
salidas reales de múltiples LLMs.

---

## Descartados tras verificación (no eran bugs)

- **H4** (leak al cancelar pausa): `estaTareaPausada` lee estado vivo (CANCELADO
  tras cancelar) → handler cae en branch correcto. No hay leak.
- **Estadisticas thread-safety**: usa `AtomicInteger` en todos los contadores.
- **i18n format-strings**: verificación programática, cero mismatches ES/EN.
- **TaskExecutionManager pool sizing**: orden core/max correcto en ambos branches.

---

## Áreas confirmadas limpias

- **i18n:** cero mismatch placeholders, cero `%` problemáticos.
- **Pipeline LLM:** cero `parseInt` sin try/catch, `gson.fromJson` con guard,
  acceso JSON null-safe.
- **Swing EDT:** modelos de tabla con locks + version stamps, bounds checks.
- **Recursos OkHttp:** todos los `Response` en try-with-resources (excepto U1 ya corregido).

---

## Validación

- **Build:** `BUILD SUCCESSFUL`, JAR `BurpIA-1.6.0.jar` (9.1M)
- **Pruebas:** 1668 pasadas (sin regresiones)
- **PMD:** verde (main + test) con reglas de código muerto activadas
- **Versión:** `VERSION.txt` = `VersionBurpIA.VERSION_ACTUAL` = `1.6.0`
