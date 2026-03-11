# AGENTE CLAUDE (Claude Code) - BurpIA

Guía operativa para:

1. Instalar `claude` si no está instalado.
2. Configurar el MCP de Burp Suite para Claude Code.
3. Conectarlo correctamente con BurpIA.

---

## 1. Requisitos

- Burp Suite ejecutándose.
- Extensión oficial **MCP Server** de PortSwigger instalada en Burp.
- BurpIA cargado en Burp Suite.
- Claude Code CLI (`claude`) instalado y autenticado.

---

## 2. Instalar Claude Code CLI

### macOS / Linux

```bash
npm install -g @anthropic-ai/claude-code
```

Verifica instalación:

```bash
which claude
claude --version
```

### Windows

Instala Claude Code desde la documentación oficial de Anthropic. El método exacto puede variar según la versión o la política de tu entorno.

Verifica instalación:

```bat
where claude
claude --version
```

---

## 3. Primer inicio y autenticación de Claude

Inicia Claude Code una vez:

```bash
claude
```

Luego completa login:

```text
/login
```

Si tu entorno usa API keys o autenticación empresarial, sigue la política de tu organización para Anthropic.

---

## 4. Instalar MCP oficial de Burp Suite (obligatorio)

1. En Burp Suite, instala la extensión oficial **MCP Server** de PortSwigger.
2. Abre la pestaña `MCP` y activa el servidor (`Enabled`).
3. Verifica que Burp MCP esté escuchando en `http://127.0.0.1:9876`. Ese valor se reutiliza en el flag `--sse-url` del proxy `stdio`.
4. Extrae o localiza `mcp-proxy-all.jar` para el proxy `stdio` de Burp.
5. Mantén Burp abierto mientras uses Claude Code.

Notas:

- En macOS el jar suele quedar en `/Users/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- En Linux suele quedar en `/home/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- En Windows, si extraes el proxy al perfil del usuario, puede quedar bajo `%APPDATA%\\BurpSuite\\mcp-proxy\\mcp-proxy-all.jar`.

---

## 5. Configurar MCP de Burp en Claude Code

### Opción A (recomendada): agregar servidor MCP local `stdio` desde Claude CLI

macOS:

```bash
claude mcp add burp --scope user -- "/Applications/Burp Suite Professional.app/Contents/Resources/jre.bundle/Contents/Home/bin/java" "-jar" "/Users/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar" "--sse-url" "http://127.0.0.1:9876"
```

Linux:

```bash
claude mcp add burp --scope user -- "/home/USUARIO/BurpSuitePro/jre/bin/java" "-jar" "/home/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar" "--sse-url" "http://127.0.0.1:9876"
```

Windows:

```bat
claude mcp add burp --scope user -- "C:\Users\USUARIO\AppData\Local\BurpSuitePro\jre\bin\java.exe" "-jar" "C:\Users\AudiTHOR03\AppData\Roaming\BurpSuite\mcp-proxy\mcp-proxy-all.jar" "--sse-url" "http://127.0.0.1:9876"
```

Luego valida:

```bash
claude mcp list
```

Dentro de una sesión activa de Claude también puedes validar disponibilidad MCP con:

```text
/mcp
```

### Opción B: configuración manual o administrada

Si tu entorno usa configuración administrada, agrega un servidor llamado `burp` en formato `mcpServers` usando el proxy `stdio` de Burp.

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

Ajustes habituales:

- Cambia `USUARIO` por tu usuario real.
- Si usas Burp Community, la carpeta puede ser `BurpSuiteCommunity` en lugar de `BurpSuitePro`.
- En Linux la ruta `/home/USUARIO/BurpSuitePro/jre/bin/java` asume la instalación nativa habitual en el home. Si instalaste Burp en otra ruta, ajústala.
- Si extrajiste el jar en otra ruta, actualiza la entrada en `args`.
- En el ejemplo de Windows, sustituye `AudiTHOR03` por el usuario real si tu perfil de Windows es distinto.

---

## 5.2 Política de ejecución en Claude Code (recomendado)

Cuando Claude solicite permisos para ejecutar acciones o herramientas, elige la política según tu perfil de riesgo.

Para sesiones de pentesting sensibles, mantén habilitadas las confirmaciones de ejecución.

---

## 6. Configurar BurpIA para usar Claude Code

En BurpIA:

1. `Ajustes` -> pestaña `Agentes`.
2. `Seleccionar Agente`: `CLAUDE_CODE`.
3. Activar `Habilitar Agente`.
4. Configurar `Ruta del Binario`:
   - macOS/Linux: `~/.local/bin/claude --dangerously-skip-permissions`
   - Windows: `%USERPROFILE%\\.local\\bin\\claude.exe --dangerously-skip-permissions`
5. Ajustar `Espera MCP (ms)` según tu máquina.
6. Guardar ajustes.

Notas:

- BurpIA soporta comando más flags en este campo.
- Si la ruta real que te devuelve `which claude` o `where claude` es distinta, usa esa ruta real.
- Si no quieres omitir confirmaciones, elimina `--dangerously-skip-permissions` y ajusta tu flujo manual.

---

## 7. Flujo esperado en BurpIA

Con agente habilitado, BurpIA:

1. Ejecuta el comando configurado de Claude.
2. Espera el tiempo `Espera MCP (ms)` definido por el usuario.
3. Inyecta el prompt inicial pre-flight.

También reinyecta el prompt inicial cuando:

- Reinicias la consola del agente.
- Cambias de agente.

Si cuando se inserta el prompt pre-flight las herramientas MCP aún no están listas, reintenta manualmente con `Inyectar Payload`.

---

## 8. Validación rápida

1. En la consola de agente de BurpIA, valida que no aparezca error de ruta o comando.
2. En Claude, ejecuta `claude mcp list` y confirma que aparece `burp`.
3. En Burp, valida que el servidor MCP siga activo.
4. Desde BurpIA, envía un hallazgo o flujo al agente y revisa la respuesta.

---

## 9. Troubleshooting

### Error: "El binario del agente no existe en la ruta actual..."

- Corrige `Ruta del Binario` en `Ajustes > Agentes`.
- Verifica el ejecutable con `which claude` o `where claude`.
- Si usas flags, confirma primero que la ruta al ejecutable sea válida por sí sola.

### Claude inicia, pero no aparecen herramientas MCP de Burp

- Verifica que Burp MCP esté `Enabled`.
- Verifica host y puerto en `--sse-url`.
- Revisa la configuración `stdio` de `burp` y confirma que `java.exe` o `java` y `mcp-proxy-all.jar` existan en esas rutas.
- Reinicia Claude Code y revisa `claude mcp list` de nuevo.

### Burp MCP responde, pero el flujo del agente en BurpIA no ejecuta

- Confirma que BurpIA tenga `Agente habilitado`.
- Incrementa `Espera MCP (ms)` para dar más tiempo a levantar MCP.
- Usa `Reiniciar` y luego `Inyectar Payload` para forzar el pre-flight.

---

## 10. Referencias oficiales

- Claude Code docs:
  - https://docs.anthropic.com/en/docs/claude-code/getting-started
  - https://docs.anthropic.com/en/docs/claude-code/mcp
- Burp MCP Server (PortSwigger):
  - https://github.com/PortSwigger/mcp-server
- Burp y su JRE privado en instaladores nativos:
  - https://portswigger.net/burp/documentation/desktop/troubleshooting/launch-from-command-line
