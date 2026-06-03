# AGENTE GROK - BurpIA

Guía operativa para:
1. Instalar `grok` si no está instalado.
2. Configurar el MCP de Burp Suite para Grok.
3. Conectarlo correctamente con BurpIA.

---

## 1. Requisitos

- Burp Suite ejecutándose.
- Extensión oficial **MCP Server** de PortSwigger instalada en Burp.
- BurpIA cargado en Burp Suite.
- Grok CLI/TUI (`grok`) instalado y autenticado.

---

## 2. Instalar Grok CLI/TUI

Grok (de xAI) normalmente se instala mediante la aplicación de escritorio / TUI oficial de xAI. Se autoadministra en `~/.grok/`.

### macOS / Linux (común después de la instalación)

```bash
which grok
~/.grok/bin/grok --version
grok --version
```

Si no está en el PATH, agrega `~/.grok/bin` a tu PATH o usa la ruta completa.

### Windows

El instalador de Grok coloca el ejecutable bajo el perfil del usuario. Verifica:

```bat
where grok
%USERPROFILE%\.grok\bin\grok.exe --version
```

Sigue las instrucciones oficiales de instalación de xAI para tu plataforma.

---

## 3. Primer inicio y autenticación de Grok

Inicia Grok una vez:

```bash
grok
```

Completa cualquier flujo de login / API key (puede abrir un navegador o usar `~/.grok/auth.json`).

Si tu entorno usa API keys, colócalas según la configuración de Grok (ver `~/.grok/config.toml` o los ajustes de la TUI).

---

## 4. Instalar MCP oficial de Burp Suite (obligatorio)

1. En Burp Suite, instala la extensión oficial **MCP Server** de PortSwigger.
2. Abre la pestaña `MCP` y activa el servidor (`Enabled`).
3. Verifica que Burp MCP esté escuchando en `http://127.0.0.1:9876`. Ese valor se reutiliza en el flag `--sse-url` del proxy `stdio`.
4. Extrae o localiza `mcp-proxy-all.jar` para el proxy `stdio` de Burp.
5. Mantén Burp abierto mientras uses Grok.

Notas:

- En macOS el jar suele quedar en `/Users/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- En Linux suele quedar en `/home/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- En Windows, si extraes el proxy al perfil del usuario, puede quedar bajo `%APPDATA%\\BurpSuite\\mcp-proxy\\mcp-proxy-all.jar`.

---

## 5. Configurar MCP de Burp en Grok

Grok guarda los servidores MCP en `~/.grok/config.toml` bajo la sección `[mcp_servers.<name>]`.

### Opción A (recomendada para argumentos complejos): editar directamente `~/.grok/config.toml`

Agrega o crea la sección:

macOS (ejemplo con rutas descubiertas):

```toml
[mcp_servers.burp]
command = "/Applications/Burp Suite.app/Contents/Resources/jre.bundle/Contents/Home/bin/java"
args = ["-jar", "/Users/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar", "--sse-url", "http://127.0.0.1:9876"]
enabled = true
```

Linux:

```toml
[mcp_servers.burp]
command = "/home/USUARIO/BurpSuitePro/jre/bin/java"
args = ["-jar", "/home/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar", "--sse-url", "http://127.0.0.1:9876"]
enabled = true
```

Windows:

```toml
[mcp_servers.burp]
command = "C:\\Users\\USUARIO\\AppData\\Local\\BurpSuitePro\\jre\\bin\\java.exe"
args = ["-jar", "C:\\Users\\USUARIO\\AppData\\Roaming\\BurpSuite\\mcp-proxy\\mcp-proxy-all.jar", "--sse-url", "http://127.0.0.1:9876"]
enabled = true
```

### Opción B: usando el helper de CLI

```bash
grok mcp add burp \
  --command "/Applications/Burp Suite.app/Contents/Resources/jre.bundle/Contents/Home/bin/java" \
  --args "-jar" \
  --args "/Users/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar" \
  --args "--sse-url" \
  --args "http://127.0.0.1:9876"
```

Valida:

```bash
grok mcp list
grok inspect --json   # muestra los servidores cargados, incluyendo source "configToml" para burp
```

Dentro de una sesión activa de la TUI de Grok, abre el modal de gestión de MCP con:

```text
/mcps
```
o **Ctrl + L**

Luego localiza el servidor `burp`, habilítalo e inícialo si es necesario. Una vez iniciado, las herramientas se vuelven descubribles.

Ajustes habituales:

- Cambia `USUARIO` por tu usuario real.
- Si usas Burp Community, la carpeta puede ser `BurpSuiteCommunity` en lugar de `BurpSuitePro` / `Burp Suite.app`.
- En macOS la carpeta de la app suele ser simplemente `Burp Suite.app` (no Professional). Ajusta la ruta en `command` según corresponda.
- Si extrajiste el jar en otra ruta, actualiza la entrada en `args`.
- Después de editar `config.toml`, reinicia Grok o recarga vía el modal /mcps para que el cambio surta efecto en la TUI.

---

## 5.2 Política de ejecución / permisos en Grok (recomendado)

Grok controla los permisos de ejecución de herramientas mediante `~/.grok/config.toml`:

```toml
[ui]
permission_mode = "always-approve"
yolo = false   # o true para completamente automático (usar con precaución)
```

Para sesiones automatizadas de pentesting con el agente en BurpIA, configura `permission_mode = "always-approve"` (o el modo no interactivo equivalente que soporte tu versión de Grok).

También puedes configurarlo desde los ajustes de la TUI.

---

## 6. Configurar BurpIA para usar Grok

En BurpIA:

1. `Ajustes` -> pestaña `Agentes`.
2. `Seleccionar Agente`: `GROK` (o el nombre visible "Grok" / "Grok CLI" una vez registrado en el enum AgenteTipo).
3. Activar `Habilitar Agente`.
4. Configurar `Ruta del Binario`:
   - macOS/Linux: `~/.grok/bin/grok`
   - Windows: `%USERPROFILE%\.grok\bin\grok.exe`
5. Ajustar `Espera MCP (ms)` según tu máquina (Grok + el proxy Java pueden tardar un poco en levantar el bridge de Burp MCP).
6. Guardar ajustes.

Notas:

- BurpIA soporta comando más flags en este campo si tu invocación de Grok necesita argumentos extra.
- Si `which grok` devuelve una ruta distinta, usa esa ruta real completa.
- Grok lee la configuración de MCP desde `~/.grok/config.toml` al iniciar. Asegúrate de que el servidor `burp` esté habilitado allí.
- El comportamiento de permisos se controla principalmente en el propio `config.toml` de Grok (ver sección 5.2). No se usa el flag estilo `--dangerously-skip-permissions` de la misma forma que Claude; configura `permission_mode` en su lugar.

---

## 7. Flujo esperado en BurpIA + pre-flight de Grok

Con agente habilitado, BurpIA:
1. Ejecuta el comando configurado de `grok`.
2. Espera el tiempo `Espera MCP (ms)` definido.
3. Inyecta el prompt inicial pre-flight.

**Pre-flight específico de Grok (crítico):**

Los agentes Grok realizan un estricto "BURPAI CRITICAL PRE-FLIGHT CHECK" al inicio de las sesiones que involucran Burp:

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
2. Ejecuta `grok mcp list` y `grok inspect --json` — confirma que aparece `burp` con el comando correcto de Java + jar y `source: configToml`.
3. En la TUI de Grok (si la abres por separado), presiona **Ctrl + L** o escribe `/mcps` y verifica que `burp` esté corriendo.
4. En Burp, confirma que la extensión MCP Server esté `Enabled` y escuchando en 9876.
5. Desde BurpIA, envía un hallazgo o flujo al agente. El agente debe usar `search_tool` (con queries como "burp", "send_http1_request", "get_scanner_issues") y luego `use_tool` con nombres como `burp__send_http1_request`.

---

## 9. Troubleshooting

### Error: "El binario del agente no existe en la ruta actual..."

- Corrige `Ruta del Binario` en `Ajustes > Agentes`.
- Verifica con `which grok` o directamente `~/.grok/bin/grok --version`.
- Usa la ruta completa si el binario no está en el PATH.

### Grok inicia, pero no aparecen herramientas MCP de Burp / el pre-flight falla

- Verifica que la extensión MCP de Burp esté `Enabled` y que el puerto 9876 sea alcanzable (`curl -I http://127.0.0.1:9876` debe devolver `text/event-stream`).
- Confirma que `~/.grok/config.toml` tenga el bloque correcto `[mcp_servers.burp]` y `enabled = true`.
- Ejecuta `grok mcp list` y `grok inspect --json` para ver si "burp" aparece en la lista.
- En una TUI de Grok en ejecución, abre **Ctrl + L** / `/mcps`, inicia el servidor `burp` y reintenta.
- Incrementa `Espera MCP (ms)` en BurpIA.
- Reinicia la consola del agente en BurpIA y fuerza `Inyectar Payload`.

### Las herramientas aparecen pero las llamadas fallan o usan protocolo incorrecto

- El pre-flight obliga a usar `burp__send_http1_request` + `\r\n` correcto en el parámetro `content`.
- Siempre llama primero a `search_tool` (ej. query "send_http1_request" o "burp") para obtener el schema actual antes de `use_tool`.
- Nunca llames a las variantes HTTP/2 (`send_http2_request`, `create_repeater_tab_http2`) para el tráfico principal.

### Prompts de permisos bloqueando la automatización

- Edita `~/.grok/config.toml` y configura:

```toml
[ui]
permission_mode = "always-approve"
```

- Recarga Grok / reinicia el agente.

---

## 10. Referencias oficiales / útiles

- Guía de usuario de Grok (local después de instalar):
  - `~/.grok/docs/user-guide/07-mcp-servers.md`
  - Otros archivos en `~/.grok/docs/user-guide/`
- Recursos xAI / Grok: https://x.ai/
- Burp MCP Server (PortSwigger):
  - https://github.com/PortSwigger/mcp-server
- Burp y su JRE privado en instaladores nativos:
  - https://portswigger.net/burp/documentation/desktop/troubleshooting/launch-from-command-line

---

**Recuerda (específico de Grok):**  
Antes de cualquier trabajo significativo con Burp, el agente debe ejecutar (o tú debes disparar manualmente) el BURPAI CRITICAL PRE-FLIGHT CHECK para inventariar herramientas, confirmar el servidor `burp` (27 herramientas) y fijar la disciplina de HTTP/1.1 + CRLF + `search_tool` antes de `use_tool`. Solo se permiten salidas reales de herramientas — nada de fabricación.
