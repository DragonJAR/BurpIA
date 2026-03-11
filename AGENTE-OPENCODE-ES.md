# AGENTE OPENCODE (OpenCode) - BurpIA

Guía operativa para:

1. Instalar `opencode` si no está instalado.
2. Configurar el MCP de Burp Suite para OpenCode.
3. Conectarlo correctamente con BurpIA.

---

## 1. Requisitos

- Burp Suite ejecutándose.
- Extensión oficial **MCP Server** de PortSwigger instalada en Burp.
- BurpIA cargado en Burp Suite.
- OpenCode CLI (`opencode`) instalado.
- Un proveedor o cuenta configurada dentro de OpenCode.

---

## 2. Instalar OpenCode

### macOS / Linux

Instalación recomendada:

```bash
curl -fsSL https://opencode.ai/install | bash
```

Alternativa con npm:

```bash
npm install -g opencode-ai
```

Verifica instalación:

```bash
which opencode
opencode --version
```

### Windows

OpenCode recomienda usar **WSL** para la mejor experiencia general. Para integrarlo con BurpIA en Windows, esta guía documenta una instalación nativa de Windows o una instalación que exponga un binario invocable desde BurpIA.

Opciones habituales:

```powershell
choco install opencode
```

```powershell
scoop install opencode
```

```powershell
npm install -g opencode-ai
```

Verifica instalación:

```bat
where opencode
opencode --version
```

---

## 3. Primer inicio y autenticación de OpenCode

Inicia OpenCode una vez:

```bash
opencode
```

Dentro del TUI ejecuta:

```text
/connect
```

Luego:

1. Selecciona `opencode` o el proveedor LLM que usarás.
2. Completa login o pega tu API key.
3. Confirma que puedes abrir una sesión normal antes de integrarlo con BurpIA.

---

## 4. Instalar MCP oficial de Burp Suite (obligatorio)

1. En Burp Suite, instala la extensión oficial **MCP Server** de PortSwigger.
2. Abre la pestaña `MCP` y activa el servidor (`Enabled`).
3. Verifica que Burp MCP esté escuchando en `http://127.0.0.1:9876`. Ese valor se reutiliza en el flag `--sse-url` del proxy `stdio`.
4. Extrae o localiza `mcp-proxy-all.jar` para el proxy stdio.
5. Mantén Burp abierto mientras uses OpenCode.

Notas:

- En macOS el jar suele quedar en `/Users/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- En Linux suele quedar en `/home/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- En Windows puede quedar bajo `%APPDATA%\\BurpSuite\\mcp-proxy\\mcp-proxy-all.jar` si lo extraes al perfil del usuario.

---

## 5. Configurar MCP de Burp en OpenCode

### Opción A (recomendada): usar el asistente de OpenCode

Ejecuta:

```bash
opencode mcp add
```

El asistente guía la creación de servidores MCP locales o remotos. Para Burp, configura un servidor local llamado `burp` que arranque el proxy stdio usando:

- El ejecutable `java` empaquetado con Burp.
- El jar `mcp-proxy-all.jar`.
- El argumento `--sse-url http://127.0.0.1:9876`.

Luego valida:

```bash
opencode mcp list
```

### Opción B: configuración manual en `opencode.json`

OpenCode documenta la configuración MCP bajo la clave `mcp` usando `type: "local"` y un `command` en forma de array.

Ejemplo macOS:

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "burp": {
      "type": "local",
      "command": [
        "/Applications/Burp Suite Professional.app/Contents/Resources/jre.bundle/Contents/Home/bin/java",
        "-jar",
        "/Users/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar",
        "--sse-url",
        "http://127.0.0.1:9876"
      ],
      "enabled": true,
      "timeout": 10000
    }
  }
}
```

Ejemplo Linux:

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "burp": {
      "type": "local",
      "command": [
        "/home/USUARIO/BurpSuitePro/jre/bin/java",
        "-jar",
        "/home/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar",
        "--sse-url",
        "http://127.0.0.1:9876"
      ],
      "enabled": true,
      "timeout": 10000
    }
  }
}
```

Ejemplo Windows nativo:

```json
{
  "$schema": "https://opencode.ai/config.json",
  "mcp": {
    "burp": {
      "type": "local",
      "command": [
        "C:\\Users\\USUARIO\\AppData\\Local\\BurpSuitePro\\jre\\bin\\java.exe",
        "-jar",
        "C:\\Users\\AudiTHOR03\\AppData\\Roaming\\BurpSuite\\mcp-proxy\\mcp-proxy-all.jar",
        "--sse-url",
        "http://127.0.0.1:9876"
      ],
      "enabled": true,
      "timeout": 10000
    }
  }
}
```

Notas:

- OpenCode usa un `command` tipo array; no separa `command` y `args` como otros clientes.
- La configuración global de OpenCode vive en `~/.config/opencode/opencode.json`.
- Si usas WSL, edita ese archivo dentro del entorno WSL, no desde el perfil nativo de Windows.
- Si usas Burp Community, la carpeta puede ser `BurpSuiteCommunity`.
- En Linux la ruta `/home/USUARIO/BurpSuitePro/jre/bin/java` asume la instalación nativa habitual en el home. Si instalaste Burp en otra ruta, ajústala.
- En el ejemplo de Windows, sustituye `AudiTHOR03` por el usuario real si tu perfil de Windows es distinto.

### 5.1 Referencia: formato `mcpServers` del proxy stdio de Burp

Si otro cliente administrado te pide el mismo proxy con formato `mcpServers`, estos son los equivalentes manuales:

macOS:

```json
{
  "mcpServers": {
    "burp": {
      "type": "stdio",
      "command": "/Applications/Burp Suite Professional.app/Contents/Resources/jre.bundle/Contents/Home/bin/java",
      "args": [
        "-jar",
        "/Users/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar",
        "--sse-url",
        "http://127.0.0.1:9876"
      ],
      "disabled": false
    }
  }
}
```

Linux:

```json
{
  "mcpServers": {
    "burp": {
      "type": "stdio",
      "command": "/home/USUARIO/BurpSuitePro/jre/bin/java",
      "args": [
        "-jar",
        "/home/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar",
        "--sse-url",
        "http://127.0.0.1:9876"
      ],
      "disabled": false
    }
  }
}
```

Windows:

```json
{
  "mcpServers": {
    "burp": {
      "type": "stdio",
      "command": "C:\\Users\\USUARIO\\AppData\\Local\\BurpSuitePro\\jre\\bin\\java.exe",
      "args": [
        "-jar",
        "C:\\Users\\AudiTHOR03\\AppData\\Roaming\\BurpSuite\\mcp-proxy\\mcp-proxy-all.jar",
        "--sse-url",
        "http://127.0.0.1:9876"
      ],
      "disabled": false
    }
  }
}
```

---

## 6. Configurar BurpIA para usar OpenCode

En BurpIA:

1. `Ajustes` -> pestaña `Agentes`.
2. `Seleccionar Agente`: `OPEN_CODE`.
3. Activar `Habilitar Agente`.
4. Configurar `Ruta del Binario`:
   - macOS/Linux: `~/.opencode/bin/opencode`
   - Windows: `%USERPROFILE%\\.opencode\\bin\\opencode.exe`
5. Ajustar `Espera MCP (ms)` según tu máquina.
6. Guardar ajustes.

Notas:

- BurpIA soporta comando más flags en este campo.
- Si instalaste OpenCode con `npm`, `choco` o `scoop`, la ruta real puede ser distinta. Usa la salida de `which opencode` o `where opencode`.
- Si decides ejecutar OpenCode desde WSL, tendrás que configurar en BurpIA un comando invocable desde Windows que arranque esa sesión WSL.

---

## 7. Flujo esperado en BurpIA

Con agente habilitado, BurpIA:

1. Ejecuta el comando configurado de OpenCode.
2. Espera el tiempo `Espera MCP (ms)` definido por el usuario.
3. Inyecta el prompt inicial pre-flight.

También reinyecta el prompt inicial cuando:

- Reinicias la consola del agente.
- Cambias de agente.

Si cuando entra el pre-flight las herramientas MCP aún no están listas, reintenta manualmente con `Inyectar Payload`.

---

## 8. Validación rápida

1. En la consola de agente de BurpIA, valida que no aparezca error de ruta o comando.
2. Ejecuta `opencode mcp list` y confirma que `burp` aparece configurado y conectado.
3. En Burp, valida que el servidor MCP siga activo.
4. Desde BurpIA, envía un hallazgo o flujo al agente y revisa la respuesta.

---

## 9. Troubleshooting

### Error: "El binario del agente no existe en la ruta actual..."

- Corrige `Ruta del Binario` en `Ajustes > Agentes`.
- Verifica con `which opencode` o `where opencode`.
- Si configuraste WSL, confirma que el comando de lanzamiento sea invocable desde el mismo entorno donde corre Burp.

### OpenCode inicia, pero no aparecen herramientas MCP de Burp

- Verifica que Burp MCP esté `Enabled`.
- Verifica host y puerto en `--sse-url`.
- Revisa `opencode mcp list`.
- Si usas configuración manual, revisa `~/.config/opencode/opencode.json`.

### Burp MCP responde, pero el flujo del agente en BurpIA no ejecuta

- Confirma que BurpIA tenga `Agente habilitado`.
- Incrementa `Espera MCP (ms)` para dar más tiempo a levantar MCP.
- Usa `Reiniciar` y luego `Inyectar Payload` para forzar el pre-flight.

---

## 10. Referencias oficiales

- OpenCode:
  - https://opencode.ai/docs/es/
  - https://opencode.ai/docs/cli/
  - https://opencode.ai/docs/config/
  - https://opencode.ai/docs/mcp-servers/
  - https://opencode.ai/docs/windows-wsl/
- Burp MCP Server (PortSwigger):
  - https://github.com/PortSwigger/mcp-server
- Burp y su JRE privado en instaladores nativos:
  - https://portswigger.net/burp/documentation/desktop/troubleshooting/launch-from-command-line
