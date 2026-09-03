# BurpIA

<p align="center">
  <img src="src/assets/logo.png" alt="BurpIA Logo" width="200"/>
</p>

[![Licencia: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Versión](https://img.shields.io/badge/version-1.7.0-green.svg)](VERSION.txt)
[![Burp Suite](https://img.shields.io/badge/funciona%20en-Community%20%26%20Pro-ff6633.svg)](https://portswigger.net/burp/communitydownload)
[![Autor](https://img.shields.io/badge/author-DragonJAR-orange.svg)](https://www.DragonJAR.org)
[![English](https://img.shields.io/badge/read%20in-English-blue.svg)](README.md)

> BurpIA convierte Burp Suite en un entorno de testing web asistido por IA. Analiza tráfico HTTP real con LLMs (más de 12 proveedores o modelos locales), valida hallazgos con agentes CLI autónomos sobre Burp MCP y mantiene cada resultado trazable — **funciona en Burp Suite Community y Professional**.

## 🎯 Qué hace BurpIA

Esta extensión convierte tráfico HTTP pasivo en hallazgos de seguridad accionables. Puede:

- **Analizar tráfico con LLMs:** escaneo pasivo automático o análisis manual vía menú contextual, sobre evidencia real (`request` + `response`).
- **Analizar flujos de peticiones:** análisis contextual de 2 a 4 peticiones relacionadas como un único flujo, ideal para vulnerabilidades de estado y lógica.
- **Validar con agentes CLI:** envía hallazgos o flujos a agentes autónomos (Factory Droid, Claude Code, Antigravity, Open Code, Grok, Codex) integrados con Burp Suite MCP para validación manual profunda.
- **Triage a alta velocidad:** envía hallazgos directamente a **Burp Repeater** desde la tabla centralizada de resultados, o guárdalos en los Issues del proyecto de Burp.
- **Gestionar hallazgos inteligentemente:** priorización por severidad/confianza, filtros por texto y severidad, exportación CSV/JSON.
- **Mantener el control:** deduplicación de peticiones (SHA-256, TTL 15 min, LRU 10k), concurrencia configurable (1–10), filtrado de recursos estáticos, interfaz bilingüe (español/inglés) y tema nativo Dark/Light.

> **Importante:** los hallazgos de IA son indicios, no veredictos. Siempre valida antes de reportar — hay posibles falsos positivos.

## 📦 Instalación

1. Descarga `BurpIA-1.7.0.jar` desde la página de [releases](https://github.com/DragonJAR/BurpIA/releases).
2. En Burp Suite: pestaña `Extensiones` → `Add` → selecciona el archivo JAR.
3. Configura tu proveedor en la pestaña de BurpIA: **Proveedor LLM**, **API Key** (si aplica), **Modelo**, **Idioma**.
4. Usa **Probar conexión** para validar el endpoint antes de capturar tráfico.

## ⚙️ Requisitos previos

| Requisito | Detalle |
|-----------|---------|
| Burp Suite | Community o Professional |
| Sistema operativo | macOS, Linux o Windows |
| Java | 17+ (el JRE incluido en los instaladores nativos de Burp funciona) |
| Acceso a LLM | API key de un proveedor cloud, Ollama local, o LM Studio |
| Agente CLI (opcional) | Uno de: droid, claude, agy, opencode, grok, codex |
| Idioma de la interfaz | Español o inglés (intercambiable en ajustes) |

## 🚀 Inicio rápido (3 minutos)

1. Carga la extensión (ver Instalación).
2. Selecciona tu **Proveedor LLM** e introduce la **API Key**.
3. Pulsa **Probar conexión** para validar endpoint y modelo.
4. Navega por el Proxy de Burp — los hallazgos aparecen en la pestaña de BurpIA a medida que se analiza el tráfico.
5. Clic derecho en cualquier petición → `Analizar solicitud con BurpIA` (o `🤖 Analizar con {Agente}` para validación con agente).

## 🔄 Cómo funciona

### Flujo pasivo

1. BurpIA intercepta un intercambio HTTP.
2. Verifica el **Scope**, aplica filtros y **deduplica**.
3. La tarea se encola en el gestor de análisis.
4. Se construye el prompt inyectando `request` y `response`.
5. Se parsea la respuesta de la IA y se normalizan los hallazgos.
6. Se actualizan la tabla de resultados, las estadísticas y (si está activado) los **Issues** de Burp.

### Flujo manual

1. Selecciona una o más peticiones (2–4 para análisis de flujo) en cualquier pestaña de Burp.
2. Clic derecho:
   - **1 petición:** `Analizar solicitud con BurpIA` o `🤖 Analizar con {Agente}`.
   - **2–4 peticiones:** `🔍 Analizar este flujo` o `🤖 Analizar este flujo con {Agente}`.
3. El hallazgo aparece en la tabla para editarlo, exportarlo o enviarlo a Repeater.

## 🔌 Proveedores LLM soportados

| Proveedor | Notas |
|-----------|-------|
| Ollama | Modelos locales: Qwen 3.8, Llama 4, Gemma 4, DeepSeek v4, Phi-4, etc. |
| Ollama Cloud | Modelos cloud en `ollama.com` — requiere API key |
| OpenAI | GPT-5.6 (+ variantes Luna/Sol/Terra/Cyber) |
| Claude | Anthropic: Fable 5.1, Opus 5, Sonnet 5 |
| Gemini | Google: 3.8 Flash (GA), 3.7/3.6 Flash, 2.5 Pro |
| Moonshot (Kimi) | K3, K2.7 y anteriores |
| Z.ai / Minimax | GLM 5.3 y MiniMax H3 |
| DeepSeek | v4-pro, v4-flash — API compatible con OpenAI |
| xAI Grok | grok-4.6, grok-4.5 — API compatible con OpenAI |
| Sakana Fugu | fugu, fugu-ultra |
| LM Studio | Servidor local, compatible con OpenAI |
| Personalizado | Hasta 3 perfiles para cualquier API compatible con OpenAI |

## 🤖 Agentes CLI

Agentes de validación autónomos integrados con Burp Suite MCP:

| Agente | Binario | Guía |
|--------|---------|------|
| Factory Droid | `droid` | [ES](AGENTE-DROID-ES.md) · [EN](AGENT-DROID-EN.md) |
| Claude Code | `claude` | [ES](AGENTE-CLAUDE-ES.md) · [EN](AGENT-CLAUDE-EN.md) |
| Antigravity CLI | `agy` | [ES](AGENTE-ANTIGRAVITY-ES.md) · [EN](AGENT-ANTIGRAVITY-EN.md) |
| Open Code | `opencode` | [ES](AGENTE-OPENCODE-ES.md) · [EN](AGENT-OPENCODE-EN.md) |
| Grok CLI | `grok` | [ES](AGENTE-GROK-ES.md) · [EN](AGENT-GROK-EN.md) |
| Codex CLI | `codex` | [ES](AGENTE-CODEX-ES.md) · [EN](AGENT-CODEX-EN.md) |

## 🧠 Tokens de prompt personalizado

- `{REQUEST}` / `{RESPONSE}`: request/response HTTP normalizada.
- `{REQUEST_1}`…`{REQUEST_N}` / `{RESPONSE_1}`…`{RESPONSE_N}`: elemento N-ésimo del flujo.
- `{OUTPUT_LANGUAGE}`: idioma esperado para la descripción del hallazgo.

Si omites estos tokens, BurpIA agrega automáticamente un bloque de seguridad (fallback) para mantener el contexto mínimo y forzar el idioma configurado.

## 🚀 Ejemplo de uso

Prompt personalizado para un flujo de autenticación (2 peticiones analizadas como una sola):

```
Eres un auditor de seguridad web. Céntrate SOLO en lógica de autenticación y sesión.

Petición 1 (login):
{REQUEST_1}
Respuesta 1:
{RESPONSE_1}

Petición 2 (cambio de contraseña):
{REQUEST_2}
Respuesta 2:
{RESPONSE_2}

Reporta los hallazgos en {OUTPUT_LANGUAGE} con severidad, confianza y remediación.
```

Hallazgo típico producido por BurpIA (tal como se ve en la tabla de resultados):

```
Título:    El endpoint de cambio de contraseña acepta la contraseña antigua indefinidamente
Severidad: Alta · Confianza: Cierta
Endpoint:  POST /api/v2/account/password  (200 OK)
Evidencia: El flujo de cambio de contraseña se completó usando el token de
           sesión pre-login, lo que indica ausencia de re-autenticación y de
           rotación de sesiones activas.
Remedio:   Exigir verificación de la contraseña actual e invalidar todas las
           sesiones activas tras un cambio exitoso.
```

Desde la tabla puedes enviarlo a **Burp Repeater** para validación manual, exportarlo (CSV/JSON) o despacharlo a un agente CLI para validación más profunda vía Burp MCP.

## 📸 Capturas

| Panel principal (ES) | Validación con agente | Hallazgos validados en Repeater |
|----------------------|-----------------------|---------------------------------|
| ![Panel de BurpIA](src/assets/ES.png) | ![Consola del agente](src/assets/Agente.png) | ![Hallazgos validados](src/assets/Fallos-Validados.png) |

## 🆕 Novedades de la v1.7.0

- **Nuevos proveedores LLM:** DeepSeek (v4-flash/v4-pro), xAI Grok (grok-4.3/4), Ollama Cloud, Sakana Fugu.
- **Nuevo agente CLI:** Grok CLI con detección automática de binario y permisos controlados.

Historial completo de versiones: consulta la [página de releases](https://github.com/DragonJAR/BurpIA/releases) y los tags de git.

## 💡 Buenas prácticas

- Activa **Auto-guardado en Issues** solo si quieres persistencia directa en el archivo de proyecto de Burp.
- **Valida manualmente** cada hallazgo antes de reportarlo; la IA puede alucinar.
- Con proveedores cloud, revisa tu política de datos antes de enviar tráfico con información sensible.

## 📜 Licencia

MIT — ver [LICENSE](LICENSE).
