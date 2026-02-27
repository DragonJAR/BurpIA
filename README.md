# BurpIA

BurpIA es una extensión para Burp Suite que analiza tráfico HTTP con LLMs para ayudarte a detectar hallazgos potenciales de seguridad en menos tiempo.

**Versión actual:** `1.0.1`

English version: [README.en.md](README.en.md)

---

## Capturas clave

### 1) Vista general de BurpIA (ES)
![BurpIA en Español](src/assets/ES.png)

- Muestra el tablero central con conteo de hallazgos por severidad y estado operativo.
- Ayuda a ubicar rápidamente las pestañas principales: tareas, hallazgos, agente y consola.
- Deja claro el flujo de trabajo desde detección hasta validación manual.

### 2) Validación manual con agente
![Consola del agente en BurpIA](src/assets/Agente.png)

- Evidencia el flujo agéntico de pruebas dinámicas sobre tráfico HTTP real.
- Permite ver baseline, payloads ejecutados, observaciones y hallazgos secundarios en una sola salida.
- Facilita validar evidencia técnica antes de reportar un hallazgo.

### 3) Hallazgos validados en Repeater
![Hallazgos validados en Burp Repeater](src/assets/Fallos-Validados.png)

- Demuestra cómo BurpIA deja trazabilidad de validaciones manuales en pestañas de Repeater.
- Acelera el triage al mantener requests/responses reproducibles para cada caso validado.
- Mejora la colaboración al dejar evidencia directa lista para revisión del equipo.

---

## Qué obtienes con BurpIA

- **Análisis Híbrido con IA:** Escaneo pasivo automático o manual (vía menú contextual) sobre evidencia HTTP real (`request` + `response`).
- **Triage de Alta Velocidad:** Envío directo de hallazgos a Repeater, Intruder o Scanner desde la tabla centralizada de resultados.
- **Gestión Inteligente de Hallazgos:** Priorización por severidad/confianza con opción de envío directo al proyecto de Burp Suite.
- **Deduplicación y Control de Carga:** Sistema de colas con límite de concurrencia y hashes SHA-256 para evitar re-análisis redundantes.
- **Exportación Flexible:** Soporte para volcado de datos en formatos CSV y JSON para informes externos.
- **Experiencia de Usuario:** Interfaz bilingüe (Español/Inglés) con persistencia de ajustes entre reinicios del plugin.

---

## Estado actual (v1.0.1)

BurpIA está actualizado a `v1.0.1`.
Consulta el resumen de cambios en **Historial de versiones**.

---

## Historial de versiones

### v1.0.1 (actual)

- Ahora con pruebas manuales dinámicas agénticas, con Factory Droid, Claude Code y el MCP de Burp Suite.
- Nuevo proveedor de LLM: Moonshot.
- Mejoras en traducción y usabilidad.
- Mejoras en eficiencia y rendimiento general.

### v1.0.0

- Base funcional de análisis híbrido, gestión de tareas/hallazgos y flujo de trabajo pasivo/manual.
- Integración inicial con proveedores LLM principales y exportación de resultados.

---

## Inicio rápido (3 minutos)

1. Descarga el archivo `BurpIA-1.0.1.jar`.
2. Carga la extensión en Burp Suite:
    - Ve a la pestaña `Extensions` -> `Add`.
    - Selecciona el archivo `BurpIA-1.0.1.jar`.
3. Configura BurpIA en la pestaña del plugin:
    - Selecciona tu **Proveedor LLM**.
    - Ingresa la **API Key** (si aplica).
    - Elige el **Modelo**.
    - Configura el **Idioma de interfaz** y el **Prompt personalizado**.
4. Usa el botón **Probar Conexión** para validar el endpoint y el modelo antes de capturar tráfico.

---

## Proveedores LLM soportados

- **Ollama** (Ideal para modelos locales fine-tuneados).
- **OpenAI** (GPT-4o, GPT-3.5, etc.).
- **Claude** (Anthropic).
- **Gemini** (Google).
- **Moonshot (Kimi)**.
- **Z.ai** / **Minimax**.
- **Custom** (Cualquier API compatible con el formato de OpenAI).

> [!TIP]
> Si vas a usar Z.ai o Minimax, aquí tienes opciones de compra con descuento:
> - [Z.ai con descuento](https://z.ai/subscribe?ic=FXSFEPRECU)
> - [Minimax con descuento](https://platform.minimax.io/subscribe/coding-plan?code=GdktCUVh7E&source=link)

---

## Cómo funciona

### Flujo pasivo
1. BurpIA intercepta una respuesta HTTP.
2. Verifica el **Scope**, aplica filtros y realiza la **deduplicación**.
3. Encola la tarea en el gestor de análisis.
4. Construye el prompt inyectando la `request` y `response`.
5. Parsea la respuesta de la IA y normaliza los hallazgos.
6. Actualiza la tabla de resultados, estadísticas y (si está activo) guarda en **Issues**.

### Flujo manual
1. Seleccionas una solicitud cualquiera en cualquier pestaña de Burp.
2. Clic derecho -> `Analizar solicitud con BurpIA`.
3. BurpIA analiza la solicitud y su respuesta asociada.
4. El hallazgo aparece en la tabla para ser editado, exportado o enviado a otras herramientas.

---

## Prompt personalizado

BurpIA soporta los siguientes tokens para personalizar el análisis:

- `{REQUEST}`: Inserta la solicitud HTTP normalizada.
- `{RESPONSE}`: Inserta la respuesta HTTP (si existe).
- `{OUTPUT_LANGUAGE}`: Indica el idioma de salida esperado para la descripción del hallazgo.

*Si omites estos tokens, BurpIA aplicará un bloque de contexto mínimo para mantener consistencia y el idioma de salida configurado.*

---

## Requisitos

- **Java 17** o superior.
- **Burp Suite** (Community o Professional).
- Conectividad al proveedor de IA configurado (local o remoto).

---

## Buenas prácticas

- Activa **"Guardar automáticamente en Issues"** solo si deseas persistencia directa en el archivo de proyecto de Burp.
- **Valida manualmente** cada hallazgo antes de reportarlo; la IA puede alucinar.
- Si usas proveedores en la nube, revisa tu política de privacidad antes de enviar tráfico con datos sensibles.

---

## Limitaciones

- Puede generar falsos positivos; siempre requiere validación humana experta.
- Si un análisis manual no tiene una respuesta asociada, el modelo analizará únicamente la solicitud (`{REQUEST}`).
