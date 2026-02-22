# BurpIA

BurpIA es una extensión para Burp Suite que analiza solicitudes y respuestas HTTP con LLMs para detectar hallazgos potenciales de seguridad en modo pasivo y manual.

Versión actual: `1.0.0`

## Qué valor aporta

BurpIA no sustituye el criterio del pentester, pero sí reduce tiempo en triage y priorización.

- Revisa evidencia HTTP real (`request` + `response`) desde Burp.
- Prioriza con severidad y confianza para acelerar decisiones.
- Permite pasar de hallazgo a Repeater, Intruder y Scanner Pro en pocos clics.
- Mantiene estabilidad bajo carga con cola de tareas, deduplicación y control de concurrencia.

## Estado actual del proyecto (v1.0.0)

- Análisis pasivo con `HttpResponseReceived` (Montoya API).
- Análisis manual desde menú contextual: `Analizar solicitud con BurpIA`.
- Validación estricta de `Target Scope` antes de procesar tráfico.
- Filtro de recursos estáticos para reducir ruido.
- Deduplicación SHA-256 con cache LRU y expiración TTL.
- Cola de tareas con pausar, reanudar, cancelar, reintentar y limpiar.
- Guardado automático opcional en `Site Map > Issues`.
- Exportación de hallazgos a CSV y JSON.
- Interfaz bilingüe (español/inglés), incluyendo tooltips y logs.

## Inicio rápido (3 minutos)

1. Construye el JAR:

```bash
./build-jar.sh
```

2. Carga la extensión en Burp:

- `Extensions` -> `Add`
- Selecciona: `build/libs/BurpIA-1.0.0.jar`

3. Abre la pestaña de BurpIA y configura:

- Proveedor
- API key (si aplica)
- Modelo
- Prompt

## Proveedores LLM soportados

- Ollama
- OpenAI
- Claude
- Gemini
- Z.ai
- Minimax

Si vas a usar Z.ai o Minimax, aquí tienes opciones de compra con descuento:

- Z.ai: [comprar con descuento](https://z.ai/subscribe?ic=FXSFEPRECU)
- Minimax: [comprar con descuento](https://platform.minimax.io/subscribe/coding-plan?code=GdktCUVh7E&source=link)

## Cómo funciona

### Flujo pasivo

1. BurpIA recibe una respuesta HTTP.
2. Verifica scope, filtros y deduplicación.
3. Crea y encola la tarea de análisis.
4. Construye prompt con evidencia HTTP.
5. Parsea respuesta del modelo y normaliza hallazgos.
6. Actualiza tablas/estadísticas y, si está habilitado, crea `AuditIssue`.

### Flujo manual

1. Seleccionas una solicitud en Burp.
2. Ejecutas `Analizar solicitud con BurpIA`.
3. BurpIA analiza la request y, si existe, también su response asociada.

## Prompt recomendado

BurpIA reemplaza tokens en tiempo de ejecución:

- `{REQUEST}`: obligatorio.
- `{RESPONSE}`: recomendado para mejor contexto.

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

Se persiste localmente en: `~/.burpia.json`

| Parámetro | Default | Rango | Descripción |
| --- | ---: | ---: | --- |
| `proveedorAI` | `Z.ai` | proveedores soportados | Proveedor activo. |
| `retrasoSegundos` | `5` | `0-60` | Espera entre análisis. |
| `maximoConcurrente` | `3` | `1-10` | Máximo de análisis simultáneos. |
| `maximoHallazgosTabla` | `1000` | `100-50000` | Límite de filas de hallazgos en UI. |
| `tiempoEsperaAI` | `60` | `10-300` | Timeout de llamadas a IA (segundos). |
| `escaneoPasivoHabilitado` | `true` | `true/false` | Activa o pausa captura pasiva. |
| `idiomaUi` | `es` | `es/en` | Idioma de interfaz y logs. |
| `detallado` | `false` | `true/false` | Activa logging detallado. |

## Requisitos

- Java 17+
- Burp Suite Community o Professional
- Conectividad al proveedor IA configurado (local o remoto)

## Buenas prácticas de uso

- Activa `Guardado automático en Issues` si quieres persistencia en Burp Project.
- Valida manualmente todos los hallazgos antes de reportarlos.
- Si usas proveedores cloud, revisa tu política de datos antes de enviar tráfico sensible.

## Limitaciones

- Puede generar falsos positivos o hallazgos incompletos.
- Si un análisis manual no incluye response, se analiza solo la request.

## Documentación adicional

- Contribución: `CONTRIBUTING.md`
- Referencia Montoya usada en el proyecto: `docs/PLUGINS-BURP.md`
