# BurpIA

BurpIA es una extensión para Burp Suite que analiza tráfico HTTP con LLMs para ayudarte a detectar hallazgos potenciales de seguridad en flujos pasivos y manuales.

Versión actual: `1.0.0`

## Qué aporta al auditor

BurpIA no reemplaza la validación manual; la acelera.

- Revisa solicitudes y respuestas HTTP reales capturadas en Burp.
- Prioriza resultados con severidad y confianza.
- Permite pasar rápido de un hallazgo a Repeater, Intruder y Scanner Pro.
- Mantiene un flujo operativo estable bajo carga con cola, límite de concurrencia y reintentos.

## Estado actual del proyecto (v1.0.0)

- Análisis pasivo sobre eventos `HttpResponseReceived` (Montoya API).
- Análisis manual desde menú contextual: `Analizar solicitud con BurpIA`.
- Verificación estricta de `Target Scope` antes de analizar.
- Filtro de recursos estáticos comunes para reducir ruido.
- Deduplicación por hash SHA-256 con TTL (LRU).
- Cola de tareas con acciones de pausar, reanudar, cancelar, reintentar y limpiar.
- Guardado automático opcional de hallazgos en `Site Map > Issues`.
- Exportación de hallazgos a CSV y JSON.
- UI bilingüe (español/inglés), incluyendo tooltips y logs.

## Flujo funcional

### Flujo pasivo

1. BurpIA recibe una respuesta HTTP.
2. Valida scope, filtros y deduplicación.
3. Encola la tarea de análisis en segundo plano.
4. Construye el prompt con evidencia HTTP.
5. Parsea la respuesta del modelo y normaliza hallazgos.
6. Actualiza tablas/estadísticas y, si corresponde, crea `AuditIssue`.

### Flujo manual

1. Seleccionas una request desde Burp.
2. Ejecutas `Analizar solicitud con BurpIA`.
3. BurpIA analiza la request y, si está disponible, también la response asociada.

## Proveedores soportados

- Ollama
- OpenAI
- Claude
- Gemini
- Z.ai ([suscribirse con descuento](https://z.ai/subscribe?ic=FXSFEPRECU))
- Minimax ([suscribirse con descuento](https://platform.minimax.io/subscribe/coding-plan?code=GdktCUVh7E&source=link))

## Prompt y contexto HTTP

BurpIA reemplaza tokens en tiempo de ejecución.

- `{REQUEST}`: obligatorio.
- `{RESPONSE}`: recomendado (si existe respuesta HTTP asociada).

Ejemplo mínimo recomendado:

```text
Analiza la siguiente petición HTTP y su respuesta asociada.

REQUEST:
{REQUEST}

RESPONSE:
{RESPONSE}

Devuelve JSON válido con este formato:
{
  "hallazgos": [
    {
      "descripcion": "...",
      "severidad": "Critical|High|Medium|Low|Info",
      "confianza": "High|Medium|Low"
    }
  ]
}
```

## Configuración

Se persiste localmente en `~/.burpia.json`.

| Parámetro | Default | Rango | Descripción |
| --- | ---: | ---: | --- |
| `proveedorAI` | `Z.ai` | proveedores soportados | Proveedor activo. |
| `retrasoSegundos` | `5` | `0-60` | Delay entre análisis. |
| `maximoConcurrente` | `3` | `1-10` | Máximo de análisis simultáneos. |
| `maximoHallazgosTabla` | `1000` | `100-50000` | Límite de filas de hallazgos en UI. |
| `tiempoEsperaAI` | `60` | `10-300` | Timeout de llamadas a IA (segundos). |
| `escaneoPasivoHabilitado` | `true` | `true/false` | Activa o pausa captura pasiva. |
| `idiomaUi` | `es` | `es/en` | Idioma de interfaz y logs. |
| `detallado` | `false` | `true/false` | Activa logs verbosos. |

## Build e instalación

### Build local

```bash
./build-jar.sh
```

Artefacto generado:

```text
build/libs/BurpIA-1.0.0.jar
```

### Cargar en Burp

1. Abrir Burp Suite.
2. Ir a `Extensions`.
3. Click en `Add`.
4. Seleccionar `build/libs/BurpIA-1.0.0.jar`.

## Requisitos

- Java 17+
- Burp Suite (Community o Professional)
- Acceso al proveedor IA configurado (local o remoto)

## Limitaciones y seguridad

- BurpIA puede generar falsos positivos; siempre valida manualmente.
- Si un análisis manual no tiene response asociada, se analiza solo la request.
- Evita enviar datos sensibles a proveedores remotos si tu política no lo permite.

## Documentación adicional

- Contribución: `CONTRIBUTING.md`
- Referencia Montoya usada en el proyecto: `docs/PLUGINS-BURP.md`
