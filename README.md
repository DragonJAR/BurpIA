# BurpIA

BurpIA es una extension para Burp Suite que analiza trafico HTTP con LLMs para ayudarte a detectar hallazgos potenciales de seguridad en menos tiempo.

Version actual: `1.0.0`

## Que obtienes con BurpIA

- Analisis pasivo y manual sobre evidencia HTTP real (`request` + `response`).
- Priorizacion de hallazgos por severidad y confianza.
- Flujo rapido de triage: enviar a Repeater, Intruder y Scanner desde la tabla.
- Control de carga con cola de tareas, deduplicacion y limite de concurrencia.
- Exportacion de hallazgos a CSV y JSON.
- Persistencia de ajustes de usuario entre reinicios del plugin.
- Interfaz bilingue (espanol/ingles), incluyendo tooltips y logs.

## Estado actual (v1.0.0)

- Captura pasiva basada en `HttpResponseReceived` (Montoya API).
- Analisis manual desde menu contextual: `Analizar solicitud con BurpIA`.
- Validacion estricta de `Target Scope` antes de analizar.
- Filtro de recursos estaticos para reducir ruido.
- Deduplicacion SHA-256 con cache LRU y expiracion TTL.
- Gestion de tareas: pausar, reanudar, cancelar, reintentar y limpiar.
- Guardado automatico opcional en `Site Map > Issues`.
- Envio manual de uno o varios hallazgos a Issues cuando el autoguardado esta desactivado.

## Inicio rapido (3 minutos)

1. Descarga el `BurpIA-1.0.0.jar`
2. Carga la extension en Burp:

- `Extensions` -> `Add`
- Selecciona `BurpIA-1.0.0.jar`

3. Configura BurpIA en la pestana del plugin:

- Proveedor LLM
- API key (si aplica)
- Modelo
- Idioma de interfaz
- Prompt personalizado

4. Usa `Probar Conexion` para validar endpoint/modelo antes de capturar trafico.

## Proveedores LLM soportados

- Ollama
- OpenAI
- Claude
- Gemini
- Z.ai
- Minimax
- Custom (compatible con APIs estilo OpenAI)

Si vas a usar Z.ai o Minimax, aqui tienes opciones de compra con descuento:

- [Z.ai con descuento](https://z.ai/subscribe?ic=FXSFEPRECU)
- [Minimax con descuento](https://platform.minimax.io/subscribe/coding-plan?code=GdktCUVh7E&source=link)

## Como funciona

### Flujo pasivo

1. BurpIA recibe una respuesta HTTP.
2. Verifica scope, filtros y deduplicacion.
3. Encola la tarea de analisis.
4. Construye el prompt con request/response.
5. Parsea la respuesta del modelo y normaliza hallazgos.
6. Actualiza tabla, estadisticas y (si aplica) guarda en Issues.

### Flujo manual

1. Seleccionas una solicitud en Burp.
2. Ejecutas `Analizar solicitud con BurpIA`.
3. BurpIA analiza la request y, si existe, su response asociada.
4. El hallazgo queda en tabla para exportar, editar o enviar a herramientas de Burp.

## Prompt personalizado

BurpIA soporta estos tokens:

- `{REQUEST}`: inserta la solicitud HTTP normalizada.
- `{RESPONSE}`: inserta la respuesta HTTP (si existe).
- `{OUTPUT_LANGUAGE}`: idioma de salida esperado para `descripcion`.

Si omites alguno de esos tokens, BurpIA agrega un bloque fallback automaticamente para mantener contexto minimo y forzar idioma de salida coherente con la configuracion del usuario.

## Requisitos

- Java 17+
- Burp Suite Community o Professional
- Conectividad al proveedor IA configurado (local o remoto)

## Compatibilidad con ediciones de Burp

- BurpIA funciona en Community y Professional.
- Algunas integraciones dependen de APIs disponibles en tu edicion (por ejemplo, Scanner).

## Buenas practicas

- Activa `Guardar automaticamente en Issues` si quieres persistencia directa en el proyecto de Burp.
- Valida manualmente cada hallazgo antes de reportarlo.
- Si usas proveedores remotos, revisa tu politica de datos antes de enviar trafico sensible.

## Limitaciones

- Puede generar falsos positivos o hallazgos incompletos; siempre requiere validacion humana.
- Si un analisis manual no tiene response asociada, se analiza solo la request.