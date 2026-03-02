# AGENTE DROID (Factory Droid) - BurpIA

Guía operativa para:

1. Instalar `droid` si no está instalado.
2. Configurar el MCP de Burp Suite en Droid.
3. Conectarlo correctamente con BurpIA.

---

## 1. Requisitos

- Burp Suite ejecutándose.
- Extensión **MCP Server** de PortSwigger instalada en Burp (debe aparecer la pestaña `MCP`).
- BurpIA cargado en Burp Suite.
- Factory Droid CLI (`droid`) instalado.

---

## 2. Instalar Droid CLI

### macOS / Linux

```bash
curl -fsSL https://app.factory.ai/cli | sh
```

Verifica instalación:

```bash
which droid
droid --version
```

### Windows

Usa el instalador oficial de Factory desde su documentación (el método puede variar por versión de CLI).

Verifica instalación:

```bat
where droid
droid --version
```

---

## 3. Primer inicio y autenticación de Droid

Inicia Droid una vez y completa onboarding:

```bash
droid
```

También puedes autenticar por variable de entorno.

macOS/Linux:

```bash
export FACTORY_API_KEY=fk-...
```

Windows PowerShell:

```powershell
$env:FACTORY_API_KEY="fk-..."
```

Windows CMD:

```bat
set FACTORY_API_KEY=fk-...
```

---

## 4. Instalar MCP oficial de Burp Suite (obligatorio)

1. En Burp Suite, ve a la tienda de extensiones y instala la extensión oficial **MCP Server** de PortSwigger.
2. Abre la pestaña `MCP` en Burp y activa el servidor (`Enabled`).
3. Verifica que el SSE URL este disponible (en este flujo se usa `http://127.0.0.1:9876`).
4. Al instalar el MCP oficial, se genera en tu carpeta de usuario:

```text
~/.BurpSuite/mcp-proxy/mcp-proxy-all.jar
```

5. Si quieres guardarlo en otra ruta, usa el botón **Extract server proxy jar** en la interfaz MCP de Burp.
6. Mantén Burp abierto mientras uses Droid.

---

## 5. Configurar MCP de Burp en Droid

### Opción A (recomendada): editar `~/.factory/mcp.json`

1. Abre `~/.factory/mcp.json`.
2. Agrega (o ajusta) la entrada `burp` dentro de `mcpServers`.
3. Usa este bloque (ejemplo para macOS):

```json
{
  "mcpServers": {
    "burp": {
      "type": "stdio",
      "command": "/Applications/Burp Suite Professional.app/Contents/Resources/jre.bundle/Contents/Home/bin/java",
      "args": [
        "-jar",
        "/Users/usuario/.BurpSuite/mcp-proxy/mcp-proxy-all.jar",
        "--sse-url",
        "http://127.0.0.1:9876"
      ],
      "disabled": false
    }
  }
}
```

4. Si guardaste el jar en otra ruta (usando **Extract server proxy jar**), cambia la ruta en `args`.
5. Si no usas macOS, cambia `command` por tu ruta de `java` en ese sistema.
6. Reinicia Droid y valida:

```text
/mcp list
```

### Opción B: pedirle a Droid que lo configure por ti

Puedes pegarle el JSON a Droid y pedirle que lo instale. Ejemplo:

```text
instale este mcp y agrégalo a ~/.factory/mcp.json:
```

Después pega el bloque JSON:

```json
{
  "mcpServers": {
    "burp": {
      "type": "stdio",
      "command": "/Applications/Burp Suite Professional.app/Contents/Resources/jre.bundle/Contents/Home/bin/java",
      "args": [
        "-jar",
        "/Users/usuario/.BurpSuite/mcp-proxy/mcp-proxy-all.jar",
        "--sse-url",
        "http://127.0.0.1:9876"
      ],
      "disabled": false
    }
  }
}
```

Luego valida con:

```text
/mcp list
```

---

## 5.1 Control de ejecución del MCP (recomendado)

Dependiendo del nivel de control que quieras tener sobre lo que haga el MCP, en la pantalla del agente de Droid presiona:

```text
Ctrl+L
```

Con ese atajo puedes ir cambiando la opción/política de ejecución del agente para trabajar con más o menos supervisión en las acciones MCP.

---

## 6. Configurar BurpIA para usar Factory Droid

En BurpIA:

1. `Ajustes` -> pestaña `Agentes`.
2. `Seleccionar Agente`: `FACTORY_DROID`.
3. Activar `Habilitar Agente`.
4. Configurar `Ruta del Binario`:
   - macOS/Linux: `~/.local/bin/droid`
   - Windows: `%USERPROFILE%\\bin\\droid.exe`
5. Ajustar `Espera MCP (ms)` según tu máquina.
6. Guardar ajustes.

---

## 7. Flujo esperado en BurpIA (importante)

Con agente habilitado, BurpIA hace automáticamente:

1. Ejecuta el binario `droid`.
2. Espera el tiempo `Espera MCP (ms)` configurado por usuario.
3. Inyecta el prompt inicial pre-flight.

También inyecta prompt inicial cuando:

- Reinicias consola del agente.
- Cambias de agente.

Si cuando se inserta el prompt pre-flight los MCP aun no han cargado, puedes reintentar manualmente (sin delay) con `Inyectar Payload`.

---

## 8. Validación rápida de que todo está OK

1. En consola de agente de BurpIA, valida que no aparezca error de binario.
2. En Droid, ejecuta `/mcp list` y confirma que Burp MCP aparezca activo.
3. Ejecuta una prueba simple de herramientas MCP.
4. Desde BurpIA, envía un hallazgo al agente y valida respuesta.

---

## 9. Troubleshooting

### Error: "El binario del agente no existe en la ruta..."

- Corrige `Ruta del Binario` en `Ajustes > Agentes`.
- Verifica con `which droid` (mac/Linux) o `where droid` (Windows).

### Droid inicia, pero no ve herramientas de Burp

- Revisa que Burp MCP este `Enabled`.
- Verifica host/puerto SSE en Burp.
- Revisa `mcp.json` de Droid y reinicia Droid.
- Si aplica, vuelve a intentar la conexión MCP desde Droid.

### Burp MCP responde, pero no hay ejecución desde el agente

- Confirma que BurpIA tenga `Agente habilitado`.
- Incrementa `Espera MCP (ms)` para dar tiempo a levantar MCPs.
- Usa `Reiniciar` y luego `Inyectar Payload` para forzar pre-flight.

---

## 10. Referencias oficiales

- Factory Droid CLI (instalación, settings, MCP):
  - https://docs.factory.ai/cli/getting-started/video-walkthrough
  - https://docs.factory.ai/cli/configuration/settings
  - https://docs.factory.ai/reference/cli-reference
- Burp MCP Server (PortSwigger):
  - https://github.com/PortSwigger/mcp-server
