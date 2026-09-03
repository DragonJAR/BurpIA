# AGENTE ANTIGRAVITY CLI - BurpIA

Guía operativa para:

1. Instalar `agy` si no está instalado.
2. Configurar Burp Suite MCP para Antigravity CLI.
3. Conectarlo correctamente con BurpIA.

---

## 1. Requisitos

- Burp Suite en ejecución.
- La extensión oficial **MCP Server** de PortSwigger instalada en Burp.
- BurpIA cargado en Burp Suite.
- Antigravity CLI (`agy`) instalado y autenticado.

---

## 2. Instalar Antigravity CLI

Instalador oficial (de [antigravity.google/docs/cli/install](https://antigravity.google/docs/cli/install)):

### macOS / Linux

```bash
curl -fsSL https://antigravity.google/cli/install.sh | bash
```

El binario queda instalado en `~/.local/bin/agy`.

### Windows

**PowerShell:**

```powershell
irm https://antigravity.google/cli/install.ps1 | iex
```

El binario se registra en `C:\Users\<usuario>\AppData\Local\agy\bin\agy.exe`.

Verificar la instalación:

```bash
which agy        # macOS / Linux
agy --version
```

```bat
where agy        # Windows
agy --version
```

---

## 3. Primera ejecución y autenticación

Inicia Antigravity CLI una vez:

```bash
agy
```

Opciones de autenticación (documentación oficial):

- **Inicio de sesión local:** `agy` intenta el keyring del sistema operativo (Apple Keychain, Linux Secret Service, Windows Credential Manager). Con un perfil de token válido se autentica en silencio; si no, abre el navegador para el flujo OAuth de Google.
- **SSH remoto:** el CLI detecta el entorno SSH, imprime una URL de autorización segura y completas el flujo pegando el código de vuelta en la terminal.
- **API key de Gemini (headless/CI):** define `"modelProvider": "gemini"` en `~/.gemini/antigravity-cli/settings.json` y exporta `GEMINI_API_KEY`. Solo la variable de entorno, por sí sola, no tiene efecto.

---

## 4. Instalar el MCP oficial de Burp Suite (obligatorio)

1. En Burp Suite, instala la extensión oficial **MCP Server** de PortSwigger.
2. Abre la pestaña `MCP` y habilita el servidor (`Enabled`).
3. Verifica que Burp MCP escucha en `http://127.0.0.1:9876`. Ese valor se reutiliza en el proxy stdio vía `--sse-url`.
4. Extrae o localiza `mcp-proxy-all.jar` para el proxy `stdio` de Burp.
5. Mantén Burp abierto mientras usas Antigravity CLI.

Notas:

- En macOS el jar suele estar en `/Users/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- En Linux suele estar en `/home/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- En Windows puede estar en `%APPDATA%\BurpSuite\mcp-proxy\mcp-proxy-all.jar` si se extrajo al perfil de usuario.

---

## 5. Configurar Burp MCP en Antigravity CLI

Antigravity CLI lee su configuración global desde:

```
~/.gemini/antigravity-cli/settings.json
```

Agrega la entrada del servidor MCP `burp` (proxy stdio) en ese archivo, adaptando las rutas a tu instalación:

```json
{
  "mcpServers": {
    "burp": {
      "command": "java",
      "args": [
        "-jar",
        "/Users/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar",
        "--sse-url",
        "http://127.0.0.1:9876"
      ]
    }
  }
}
```

Ajustes comunes:

- Reemplaza `USUARIO` por el nombre de usuario real.
- Apunta `command` a la ruta completa del ejecutable `java` si no está en el `PATH` (p. ej. el JRE incluido de Burp).
- Con Burp Community la carpeta puede ser `BurpSuiteCommunity` en lugar de `BurpSuitePro`.
- Si extrajiste el jar en otra ubicación, actualiza la ruta dentro de `args`.

Dentro de una sesión activa de `agy`, verifica la disponibilidad de MCP con el comando MCP de la sesión antes de enviar tráfico.

---

## 6. Política de permisos en Antigravity CLI (recomendado)

Antigravity CLI **no** usa un flag de omisión de permisos. La autonomía se gobierna con un motor de permisos granulares configurado en el mismo `~/.gemini/antigravity-cli/settings.json`, con tres listas de acceso — `deny`, `ask`, `allow` — evaluadas con precedencia estricta: **Deny > Ask > Allow**.

Acciones relevantes para sesiones de BurpIA:

| Acción | Coincide con |
|--------|--------------|
| `mcp(servidor/herramienta)` | Herramientas MCP de un servidor (`mcp(burp/*)` cubre todas las de Burp MCP) |
| `command(prefijo)` | Comandos de shell por prefijo de tokens |
| `read_file(ruta)` / `write_file(ruta)` | Acceso al sistema de archivos (`*` = todo) |

El fallback por defecto para acciones sensibles es **Ask** (lectura/escritura dentro del workspace se auto-permite). Puedes gestionar las reglas interactivamente con el comando Permissions del CLI, o editar las listas directamente. Ve la documentación oficial de permisos para la sintaxis exacta: <https://antigravity.google/docs/cli/permissions>.

Para sesiones de pentesting sensibles, conserva los valores por defecto (aprobación interactiva). Para corridas de validación totalmente autónomas, concede como mínimo `mcp(burp/*)`.

---

## 7. Configurar BurpIA para usar Antigravity CLI

En BurpIA:

1. `Ajustes` -> pestaña `Agentes`.
2. `Selecciona el agente`: `ANTIGRAVITY_CLI`.
3. Habilita `Habilitar agente`.
4. Configura `Ruta o comando del agente`:
   - macOS / Linux: `~/.local/bin/agy`
   - Windows: `%USERPROFILE%\AppData\Local\agy\bin\agy.exe`
5. Ajusta `MCP Wait (ms)` según tu máquina.
6. Guarda los ajustes.

Notas:

- BurpIA admite comando más flags en este campo, pero Antigravity no necesita flag de permisos.
- Si `which agy` o `where agy` devuelve otra ruta, usa esa ruta real.

---

## 8. Flujo esperado en BurpIA

Con el agente habilitado, BurpIA:

1. Ejecuta el comando Antigravity configurado.
2. Espera el valor configurado de `MCP Wait (ms)`.
3. Inyecta el prompt inicial de pre-flight.

También reinyecta el prompt inicial cuando:

- Reinicias la consola del agente.
- Cambias de agente.

Si las herramientas MCP no están listas al inyectarse el prompt de pre-flight, reintenta manualmente con `Inyectar Payload`.

---

## 9. Validación rápida

1. En la consola del agente de BurpIA, verifica que no aparezca ningún error de ruta o comando.
2. En Antigravity, confirma que el servidor MCP `burp` figura como disponible.
3. En Burp, confirma que el servidor MCP sigue habilitado.
4. Desde BurpIA, envía un hallazgo o flujo al agente y revisa la respuesta.

---

## 10. Solución de problemas

### Error: "El binario del agente no existe en la ruta actual..."

- Corrige la ruta en `Ajustes > Agentes`.
- Verifica el ejecutable con `which agy` o `where agy`.
- Si el comando incluye flags, confirma primero que la ruta del ejecutable es válida por sí sola.

### Antigravity arranca, pero faltan las herramientas de Burp MCP

- Verifica que Burp MCP esté `Enabled`.
- Verifica el host y puerto usados en `--sse-url`.
- Revisa `~/.gemini/antigravity-cli/settings.json` y confirma que `java` y `mcp-proxy-all.jar` existen en esas rutas.
- Si cada llamada a herramientas MCP se pausa con un prompt de aprobación, agrega una regla `allow` para `mcp(burp/*)` (ver sección 6).

### Burp MCP responde, pero el flujo del agente de BurpIA no se ejecuta

- Confirma que BurpIA tiene el `Agente habilitado`.
- Aumenta `MCP Wait (ms)` para dar más tiempo de arranque a MCP.
- Usa `Reiniciar` y luego `Inyectar Payload` para forzar el pre-flight.

---

## 11. Referencias oficiales

- Instalación y autenticación de Antigravity CLI: <https://antigravity.google/docs/cli/install>
- Permisos de Antigravity CLI: <https://antigravity.google/docs/cli/permissions>
- Modo headless de Antigravity CLI: <https://antigravity.google/docs/cli/headless>
- Burp MCP Server (PortSwigger): <https://github.com/PortSwigger/mcp-server>
- Instaladores nativos de Burp y JRE incluido: <https://portswigger.net/burp/documentation/desktop/troubleshooting/launch-from-command-line>
