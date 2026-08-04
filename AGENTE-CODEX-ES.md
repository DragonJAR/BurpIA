# AGENTE CODEX - BurpIA

Guía operativa para:
1. Instalar `codex` si no está instalado.
2. Configurar el MCP de Burp Suite para Codex.
3. Conectarlo correctamente con BurpIA.

---

## 1. Requisitos

- Burp Suite ejecutándose.
- Extensión oficial **MCP Server** de PortSwigger instalada en Burp.
- BurpIA cargado en Burp Suite.
- Codex CLI (`codex`) instalado y autenticado.
- Node.js 18+ (para instalación vía npm).

---

## 2. Instalar Codex CLI

Codex CLI (de OpenAI) se instala mediante npm o el instalador oficial. Se autoadministra en `~/.codex/`.

### macOS / Linux (común después de la instalación)

```bash
npm install -g @openai/codex
which codex
codex --version
```

Si no está en el PATH, agrega `~/.local/bin` a tu PATH o usa la ruta completa.

### Windows

La instalación global de npm coloca el ejecutable bajo el perfil del usuario. Verifica:

```bat
where codex
%USERPROFILE%\.local\bin\codex.exe --version
```

Alternativa: descarga el binario precompilado desde los [releases oficiales](https://github.com/openai/codex/releases) y colócalo en tu PATH.

Sigue las instrucciones oficiales de instalación de OpenAI para tu plataforma.

---

## 3. Primer inicio y autenticación de Codex

Inicia Codex una vez:

```bash
codex
```

Completa el flujo de login (abre un navegador para el login con cuenta ChatGPT o usa una API key).

Si tu entorno usa API keys, colócalas según la configuración de Codex (ver `~/.codex/config.toml` o la variable de entorno `OPENAI_API_KEY`).

---

## 4. Instalar MCP oficial de Burp Suite (obligatorio)

1. En Burp Suite, instala la extensión oficial **MCP Server** de PortSwigger.
2. Abre la pestaña `MCP` y activa el servidor (`Enabled`).
3. Verifica que Burp MCP esté escuchando en `http://127.0.0.1:9876`. Ese valor se reutiliza en el flag `--sse-url` del proxy `stdio`.
4. Extrae o localiza `mcp-proxy-all.jar` para el proxy `stdio` de Burp.
5. Mantén Burp abierto mientras usas Codex.

Notas:

- En macOS el jar suele quedar en `/Users/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- En Linux suele quedar en `/home/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- En Windows, si extraes el proxy al perfil del usuario, puede quedar bajo `%APPDATA%\BurpSuite\mcp-proxy\mcp-proxy-all.jar`.

---

## 5. Configurar MCP de Burp en Codex

Codex guarda los servidores MCP en `~/.codex/config.toml` bajo la sección `[mcp_servers.<name>]`.

### Opción A (recomendada para argumentos complejos): editar directamente `~/.codex/config.toml`

Agrega o crea la sección:

macOS (ejemplo con rutas descubiertas):

```toml
[mcp_servers.burp]
command = "/opt/homebrew/opt/openjdk@21/bin/java"
args = ["-jar", "/Users/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar", "--sse-url", "http://127.0.0.1:9876"]
```

Linux:

```toml
[mcp_servers.burp]
command = "/usr/bin/java"
args = ["-jar", "/home/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar", "--sse-url", "http://127.0.0.1:9876"]
```

Windows:

```toml
[mcp_servers.burp]
command = "C:\\Users\\USUARIO\\.local\\bin\\java.exe"
args = ["-jar", "C:\\Users\\USUARIO\\AppData\\Roaming\\BurpSuite\\mcp-proxy\\mcp-proxy-all.jar", "--sse-url", "http://127.0.0.1:9876"]
```

> **Tip:** Usa un Java del sistema (`/opt/homebrew/opt/openjdk@21/bin/java` o `$JAVA_HOME/bin/java`) en lugar del JRE embebido de Burp. El JRE embebido dentro del bundle de Burp Suite puede ser terminado por macOS (SIGKILL/exit 137) debido a restricciones de code-signing o cuarentena.

### Opción B: usando el helper de CLI

```bash
codex mcp add burp -- java -jar /Users/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar --sse-url http://127.0.0.1:9876
```

Valida:

```bash
codex mcp list
```

Ajustes habituales:

- Cambia `USUARIO` por tu usuario real.
- Si usas Burp Community, la carpeta puede ser `BurpSuiteCommunity` en lugar de `Burp Suite.app`.
- Si extrajiste el jar en otra ruta, actualiza la entrada en `args`.
- Después de editar `config.toml`, reinicia Codex para que el cambio surta efecto.

---

## 5.2 Política de ejecución / permisos en Codex (recomendado)

Codex controla los permisos de ejecución de herramientas mediante `~/.codex/config.toml` y flags CLI:

```toml
approval_policy = "never"
sandbox_mode = "danger-full-access"
```

O equivalentemente, pasa `--yolo` en la línea de comandos (BurpIA hace esto por defecto en la ruta del binario configurada):

```bash
codex --yolo
```

`--yolo` establece `approval_policy = "never"` y `sandbox_mode = "danger-full-access"`, habilitando operación completamente autónoma sin prompts interactivos. Usar con precaución en entornos de producción.

---

## 6. Configurar BurpIA para usar Codex

En BurpIA:

1. `Ajustes` -> pestaña `Agentes`.
2. `Seleccionar Agente`: `CODEX_CLI` (nombre visible: "Codex CLI").
3. Activar `Habilitar Agente`.
4. Configurar `Ruta del Binario`:
   - macOS/Linux: `~/.local/bin/codex --yolo`
   - Windows: `%USERPROFILE%\.local\bin\codex.exe --yolo`
5. Ajustar `Espera MCP (ms)` según tu máquina (Codex + el proxy Java pueden tardar un poco en levantar el bridge de Burp MCP).
6. Guardar ajustes.

Notas:

- BurpIA soporta comando más flags en este campo.
- Si `which codex` devuelve una ruta distinta, usa esa ruta real completa.
- Codex lee la configuración de MCP desde `~/.codex/config.toml` al iniciar. Asegúrate de que el servidor `burp` esté configurado allí.
- El comportamiento de permisos se controla mediante el flag `--yolo` (por defecto) o `approval_policy`/`sandbox_mode` en `config.toml`.

---

## 7. Flujo esperado en BurpIA + pre-flight de Codex

Con el agente habilitado, BurpIA:
1. Ejecuta el comando configurado de `codex --yolo`.
2. Espera el tiempo `Espera MCP (ms)` definido.
3. Inyecta el prompt inicial pre-flight.

**Pre-flight específico de Codex (crítico):**

Los agentes Codex realizan un estricto "BURPAI CRITICAL PRE-FLIGHT CHECK" al inicio de las sesiones que involucran Burp:

- Inventaría todas las herramientas mediante `search_tool`.
- Confirma que el servidor `burp` está conectado y lista las 27+ herramientas (incluyendo `burp__send_http1_request`, `burp__get_scanner_issues`, `burp__create_repeater_tab`, `burp__get_proxy_http_history`, etc.).
- Clasifica Level 1 = herramientas de Burp (primarias).
- Impone el protocolo: **siempre usar `burp__send_http1_request`** para el tráfico (solo HTTP/1.1). Nunca `send_http2_request`.
- Requiere `\r\n` (CRLF) en todos los headers del parámetro `content` HTTP.
- **DEBE** llamar primero a `search_tool` para obtener el schema exacto de entrada antes de cualquier llamada a `use_tool` sobre herramientas `burp__*`.
- Solo documenta salidas reales de llamadas a herramientas (anti-fabricación).

El pre-flight se reinyecta al reiniciar o cambiar de agente.

Si las herramientas MCP (especialmente las de burp) no están listas cuando se ejecuta el pre-flight, usa `Inyectar Payload` o reinicia la consola del agente.

---

## 8. Validación rápida

1. En la consola de agente de BurpIA, verifica que no aparezcan errores de ruta/comando y que el pre-flight se ejecute sin quejas de "Burp tools missing".
2. Ejecuta `codex mcp list` — confirma que aparece `burp` con el comando correcto de Java + jar.
3. En Burp, confirma que la extensión MCP Server esté `Enabled` y escuchando en 9876.
4. Desde BurpIA, envía un hallazgo o flujo al agente. El agente debe usar `search_tool` (con queries como "burp", "send_http1_request", "get_scanner_issues") y luego `use_tool` con nombres como `burp__send_http1_request`.

---

## 9. Troubleshooting

### Error: "El binario del agente no existe en la ruta actual..."

- Corrige `Ruta del Binario` en `Ajustes > Agentes`.
- Verifica con `which codex` o directamente `codex --version`.
- Usa la ruta completa si el binario no está en el PATH.
- Si Codex no está instalado: `npm install -g @openai/codex`.

### Codex inicia, pero no aparecen herramientas MCP de Burp / el pre-flight falla

- Verifica que la extensión MCP de Burp esté `Enabled` y que el puerto 9876 sea alcanzable (`curl -I http://127.0.0.1:9876` debe devolver `text/event-stream`).
- Confirma que `~/.codex/config.toml` tenga el bloque correcto `[mcp_servers.burp]`.
- Ejecuta `codex mcp list` para ver si "burp" aparece en la lista.
- Incrementa `Espera MCP (ms)` en BurpIA.
- Reinicia la consola del agente en BurpIA y fuerza `Inyectar Payload`.

### Las herramientas aparecen pero las llamadas fallan o usan protocolo incorrecto

- El pre-flight obliga a usar `burp__send_http1_request` + `\r\n` correcto en el parámetro `content`.
- Siempre llama primero a `search_tool` (ej. query "send_http1_request" o "burp") para obtener el schema actual antes de `use_tool`.
- Nunca llames a las variantes HTTP/2 (`send_http2_request`, `create_repeater_tab_http2`) para el tráfico principal.

### Prompts de permisos bloqueando la automatización

- Asegúrate de que la ruta del binario incluya `--yolo`: `~/.local/bin/codex --yolo`.
- O edita `~/.codex/config.toml` y configura:

```toml
approval_policy = "never"
sandbox_mode = "danger-full-access"
```

- Reinicia el agente.

### El proxy MCP falla con SIGKILL (exit 137) en macOS

El JRE embebido dentro del bundle de Burp Suite puede ser terminado por macOS (SIGKILL/exit 137) debido a restricciones de code-signing o cuarentena. Si `codex mcp list` muestra `Failed to connect` para `burp` y el puerto 9876 está activo, usa un Java del sistema en lugar del JRE embebido:

```bash
# Verificar que el puerto MCP de Burp está activo
curl -s -m 5 http://127.0.0.1:9876/ | head -3

# Editar ~/.codex/config.toml y cambiar el comando a Java del sistema
# command = "/opt/homebrew/opt/openjdk@21/bin/java"
```

**Notas:**
- Si no tienes Homebrew Java, instálalo con `brew install openjdk@21`.
- Alternativamente, usa `JAVA_HOME` del sistema: sustituye la ruta del comando por `$JAVA_HOME/bin/java`.

---

## 10. Referencias oficiales

- Repositorio de Codex CLI: https://github.com/openai/codex
- Referencia de configuración de Codex: https://learn.chatgpt.com/docs/config-file/config-reference
- Config básica de Codex: https://learn.chatgpt.com/docs/config-file/config-basic
- Sandbox y aprobaciones: https://learn.chatgpt.com/docs/agent-approvals-security
- Burp MCP Server (PortSwigger): https://github.com/PortSwigger/mcp-server
- Burp y su JRE privado en instaladores nativos: https://portswigger.net/burp/documentation/desktop/troubleshooting/launch-from-command-line

---

**Recuerda (específico de Codex):**
Antes de cualquier trabajo significativo con Burp, el agente debe ejecutar (o tú debes disparar manualmente) el BURPAI CRITICAL PRE-FLIGHT CHECK para inventariar herramientas, confirmar el servidor `burp` (27 herramientas) y fijar la disciplina de HTTP/1.1 + CRLF + `search_tool` antes de `use_tool`. Solo se permiten salidas reales de herramientas — nada de fabricación.
