# AGENTE GEMINI CLI - BurpIA

Guía operativa para:

1. Instalar `gemini` si no está instalado.
2. Configurar el MCP de Burp Suite para Gemini CLI.
3. Conectarlo correctamente con BurpIA.

---

## 1. Requisitos

- Burp Suite ejecutándose.
- Extensión oficial **MCP Server** de PortSwigger instalada en Burp.
- BurpIA cargado en Burp Suite.
- Gemini CLI (`gemini`) instalado y autenticado.

---

## 2. Instalar Gemini CLI

### macOS / Linux

```bash
npm install -g @google/gemini-cli
```

Verifica instalación:

```bash
which gemini
gemini --version
```

### Windows

Instala Gemini CLI desde la documentación oficial de Google. El método puede variar según la versión o la política de tu organización.

Verifica instalación:

```bat
where gemini
gemini --version
```

---

## 3. Primer inicio y autenticación de Gemini

Inicia Gemini CLI una vez:

```bash
gemini
```

Sigue el flujo de autenticación de Google.

Si tu entorno usa API keys o autenticación empresarial, sigue la política de tu organización para Google.

---

## 4. Instalar MCP oficial de Burp Suite (obligatorio)

1. En Burp Suite, instala la extensión oficial **MCP Server** de PortSwigger.
2. Abre la pestaña `MCP` y activa el servidor (`Enabled`).
3. Verifica que Burp MCP esté escuchando en `http://127.0.0.1:9876`. Ese valor se reutiliza en el flag `--sse-url` del proxy `stdio`.
4. Extrae o localiza `mcp-proxy-all.jar` para el proxy `stdio` de Burp.
5. Mantén Burp abierto mientras uses Gemini CLI.

Notas:

- En macOS el jar suele quedar en `/Users/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- En Linux suele quedar en `/home/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- En Windows puede quedar bajo `%APPDATA%\\BurpSuite\\mcp-proxy\\mcp-proxy-all.jar` si lo extraes al perfil del usuario.

---

## 5. Configurar MCP de Burp en Gemini CLI

### Opción A (recomendada): editar `~/.gemini/settings.json`

Agrega o ajusta la entrada `burp` dentro de `mcpServers` usando el proxy `stdio` de Burp.

macOS:

```json
{
  "mcpServers": {
    "burp": {
      "command": "/Applications/Burp Suite Professional.app/Contents/Resources/jre.bundle/Contents/Home/bin/java",
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

Linux:

```json
{
  "mcpServers": {
    "burp": {
      "command": "/home/USUARIO/BurpSuitePro/jre/bin/java",
      "args": [
        "-jar",
        "/home/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar",
        "--sse-url",
        "http://127.0.0.1:9876"
      ]
    }
  }
}
```

Windows:

```json
{
  "mcpServers": {
    "burp": {
      "command": "C:\\Users\\USUARIO\\AppData\\Local\\BurpSuitePro\\jre\\bin\\java.exe",
      "args": [
        "-jar",
        "C:\\Users\\AudiTHOR03\\AppData\\Roaming\\BurpSuite\\mcp-proxy\\mcp-proxy-all.jar",
        "--sse-url",
        "http://127.0.0.1:9876"
      ]
    }
  }
}
```

Luego valida:

```bash
gemini mcp list
```

Dentro de una sesión activa de Gemini también puedes validar disponibilidad MCP con:

```text
/mcp
```

### Opción B: configuración administrada o variantes manuales

Si tu entorno usa una capa administrada distinta a `~/.gemini/settings.json`, reutiliza exactamente el mismo bloque `mcpServers` del ejemplo anterior y ajusta solo la ruta del archivo de configuración que use tu instalación.

Ajustes habituales:

- Cambia `USUARIO` por tu usuario real.
- Si usas Burp Community, la carpeta puede ser `BurpSuiteCommunity`.
- En Linux la ruta `/home/USUARIO/BurpSuitePro/jre/bin/java` asume la instalación nativa habitual en el home. Si instalaste Burp en otra ruta, ajústala.
- Si extrajiste el jar en otra ruta, actualiza la entrada en `args`.
- En el ejemplo de Windows, sustituye `AudiTHOR03` por el usuario real si tu perfil de Windows es distinto.

---

## 5.2 Política de ejecución en Gemini CLI (recomendado)

Cuando Gemini solicite permisos para ejecutar acciones o herramientas, elige la política según tu perfil de riesgo.

Para sesiones de pentesting sensibles, mantén habilitadas las confirmaciones de ejecución.

---

## 6. Configurar BurpIA para usar Gemini CLI

En BurpIA:

1. `Ajustes` -> pestaña `Agentes`.
2. `Seleccionar Agente`: `GEMINI_CLI`.
3. Activar `Habilitar Agente`.
4. Configurar `Ruta del Binario`:
   - macOS: `/opt/homebrew/bin/gemini --yolo`
   - Linux: `~/.local/bin/gemini --yolo`
   - Windows: `%USERPROFILE%\\bin\\gemini.exe --yolo`
5. Ajustar `Espera MCP (ms)` según tu máquina.
6. Guardar ajustes.

Notas:

- BurpIA soporta comando más flags en este campo.
- Si `which gemini` o `where gemini` devuelven otra ruta, usa la ruta real.
- El flag `--yolo` reduce confirmaciones. Si prefieres más control manual, elimínalo.

---

## 7. Flujo esperado en BurpIA

Con agente habilitado, BurpIA:

1. Ejecuta el comando configurado de Gemini.
2. Espera el tiempo `Espera MCP (ms)` definido por el usuario.
3. Inyecta el prompt inicial pre-flight.

También reinyecta el prompt inicial cuando:

- Reinicias la consola del agente.
- Cambias de agente.

Si las herramientas MCP aún no están listas cuando entra el pre-flight, reintenta manualmente con `Inyectar Payload`.

---

## 8. Validación rápida

1. En la consola de agente de BurpIA, valida que no aparezca error de ruta o comando.
2. En Gemini, ejecuta `gemini mcp list` y confirma que aparece `burp`.
3. En Burp, valida que el servidor MCP siga activo.
4. Desde BurpIA, envía un hallazgo o flujo al agente y revisa la respuesta.

---

## 9. Troubleshooting

### Error: "El binario del agente no existe en la ruta actual..."

- Corrige `Ruta del Binario` en `Ajustes > Agentes`.
- Verifica el ejecutable con `which gemini` o `where gemini`.
- Si usas flags, confirma primero que la ruta al ejecutable sea válida por sí sola.

### Gemini inicia, pero no aparecen herramientas MCP de Burp

- Verifica que Burp MCP esté `Enabled`.
- Verifica host y puerto en `--sse-url`.
- Revisa `~/.gemini/settings.json` y confirma que `java` o `java.exe` y `mcp-proxy-all.jar` existan en esas rutas.
- Reinicia Gemini CLI y revisa `gemini mcp list` de nuevo.

### Burp MCP responde, pero el flujo del agente en BurpIA no ejecuta

- Confirma que BurpIA tenga `Agente habilitado`.
- Incrementa `Espera MCP (ms)` para dar más tiempo a levantar MCP.
- Usa `Reiniciar` y luego `Inyectar Payload` para forzar el pre-flight.

---

## 10. Referencias oficiales

- Gemini CLI docs:
  - https://github.com/google/gemini-cli
- Burp MCP Server (PortSwigger):
  - https://github.com/PortSwigger/mcp-server
- Burp y su JRE privado en instaladores nativos:
  - https://portswigger.net/burp/documentation/desktop/troubleshooting/launch-from-command-line
