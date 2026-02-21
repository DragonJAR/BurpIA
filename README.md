# BurpIA

Extensión para **Burp Suite** que analiza tráfico HTTP mediante LLMs (Large Language Models) para detectar posibles vulnerabilidades de seguridad en flujos de trabajo pasivos y manuales.

**Versión actual:** `1.0.0`

---

## ¿Qué valor aporta este plugin?

BurpIA es un plugin nativo diseñado específicamente para integrarse con **modelos de IA locales** (sin excluir modelos comerciales). Su objetivo es ser eficiente, confiable y aportar valor real en el día a día del auditor.

Hecho por **pentesters para pentesters**, BurpIA no busca reemplazar la validación manual, sino ofrecer una "visión extra" sobre elementos que podrían pasar desapercibidos durante una auditoría convencional.

---

## Estado Actual (v1.0.0)

Actualmente, BurpIA permite:

* **Análisis pasivo:** Procesa respuestas HTTP en segundo plano mediante la `Montoya API` (`HttpHandler`).
* **Filtrado inteligente:** Solo analiza solicitudes dentro del **Scope** definido en Burp.
* **Optimización de recursos:** Omite automáticamente recursos estáticos comunes (`.js`, `.css`, `.png`, `.jpg`, `.jpeg`, `.gif`, `.svg`, `.ico`, `.woff`, `.woff2`, `.webp`, `.ttf`, `.eot`).
* **Deduplicación:** Evita análisis repetidos mediante hashing (SHA-256) y una política de TTL (Time To Live).
* **Gestión de carga:** Controla la concurrencia y gestiona una cola de tareas con límites configurables.
* **Resiliencia:** Implementa una estrategia de reintentos robusta ante fallos transitorios en las APIs de IA.
* **Interfaz integrada:** UI en Swing que muestra hallazgos, tareas, estadísticas y una consola de logs en una pestaña propia de Burp.
* **Interoperabilidad:** * Exportación de hallazgos a **CSV** y **JSON**.
    * Integración directa con **Repeater** e **Intruder**.
    * Soporte para **Scanner Pro** (en ediciones profesionales).
* **Análisis manual:** Opción en el menú contextual: *"Analizar solicitud con BurpIA"*.

---

## Funcionamiento de la Herramienta

### 1. Flujo Principal (Pasivo)
1. BurpIA intercepta el evento `HttpResponseReceived`.
2. Valida que la solicitud exista y esté dentro del **Scope**.
3. Aplica filtros de estáticos y verifica si es un duplicado.
4. Crea una tarea de análisis y la encola de forma asíncrona.
5. Construye el *prompt* inyectando `{REQUEST}` y `{RESPONSE}` (cuando esté disponible) y consulta al proveedor de IA.
6. Parsea la respuesta (formato JSON preferente con fallback robusto).
7. Registra el hallazgo y actualiza la tabla, estadísticas y el Site Map de Burp (opcional).

### 2. Flujo Manual
Desde cualquier solicitud en Burp, mediante el menú contextual:
* **"Analizar solicitud con BurpIA"**: Fuerza el análisis inmediato, ignorando los filtros del flujo pasivo.

---

## Proveedores de IA Soportados

Soporte nativo para:
* **Ollama** (Local - Recomendado)
* **OpenAI**
* **Claude**
* **Gemini**
* **Z.ai**
* **Minimax**

---

## Interfaz y Operación

La pestaña de **BurpIA** se divide en cuatro secciones clave:

* **Panel de Estadísticas:** Visualización de volumen de tráfico, severidad de hallazgos, solicitudes omitidas y errores. Incluye un botón para pausar/reanudar la captura.
* **Panel de Tareas:** Estado de la cola en tiempo real (analizando, pausadas, completadas o con error).
* **Panel de Hallazgos:** * Filtros por texto y severidad.
    * Edición de detalles mediante doble clic.
    * Opción para guardar automáticamente en el *Target Site Map*.
    * Acciones rápidas: Ignorar, borrar, exportar y envío a herramientas de Burp.
* **Consola:** Logs detallados (Info/Verbose/Error) con funciones de autoscroll y limpieza.

---

## Confiabilidad y Control de Carga

### Reintentos de IA
* **Inmediatos:** Hasta 3 intentos seguidos.
* **Backoff:** Si persiste el error, espera intervalos de 30s, 60s y 90s.
* **Límite:** Máximo 6 intentos por tarea. Los errores críticos (ej. modelo inexistente) no se reintentan.

### Gestión de Concurrencia
* Limitador mediante semáforos (`maximoConcurrente`).
* `ThreadPoolExecutor` dedicado para evitar bloqueos en la interfaz.
* Cola acotada dinámicamente: `max(50, maximoConcurrente * 20)`.

### Deduplicación
* Caché LRU (Least Recently Used) basada en hashes SHA-256.
* Capacidad: 10,000 hashes.
* Expiración automática: 15 minutos.

---

## Configuración

Los ajustes se persisten localmente en `~/.burpia.json`.

| Parámetro | Default | Rango | Descripción |
| :--- | :---: | :---: | :--- |
| `retrasoSegundos` | `5` | `0-60` | Pausa entre peticiones a la IA. |
| `maximoConcurrente` | `3` | `1-10` | Hilos de análisis simultáneos. |
| `maximoHallazgosTabla` | `1000` | `100-50000` | Límite de registros en la UI. |
| `tiempoEsperaAI` | `60` | `10-300` | Timeout de la petición API (seg). |

### Prompt Personalizable
El prompt debe incluir obligatoriamente el token `{REQUEST}` y se recomienda incluir `{RESPONSE}` para aportar contexto del servidor. BurpIA reemplaza ambos tokens automáticamente. Si `{RESPONSE}` no existe en el prompt, BurpIA agrega la respuesta al final para mantener el análisis coherente. Se recomienda que el modelo responda en formato JSON:

```json
{
  "hallazgos": [
    {
      "descripcion": "string",
      "severidad": "Critical|High|Medium|Low|Info",
      "confianza": "High|Medium|Low"
    }
  ]
}

<div align="center">

**Made with ❤️ by DragonJAR.org**

[![GitHub stars](https://img.shields.io/github/stars/jaimearestrepo/BurpIA?style=social)](https://github.com/jaimearestrepo/BurpIA/stargazers)
[![GitHub forks](https://img.shields.io/github/stars/jaimearestrepo/BurpIA?style=social)](https://github.com/jaimearestrepo/BurpIA/network/members)

</div>