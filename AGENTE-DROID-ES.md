# AGENTE DROID (Factory Droid) - BurpIA

Guia operativa para:

1. Instalar `droid` si no esta instalado.
2. Configurar el MCP de Burp Suite en Droid.
3. Conectarlo correctamente con BurpIA.

---

## 1. Requisitos

- Burp Suite ejecutandose.
- Extension **MCP Server** de PortSwigger instalada en Burp (debe aparecer la pestana `MCP`).
- BurpIA cargado en Burp Suite.
- Factory Droid CLI (`droid`) instalado.

---

## 2. Instalar Droid CLI

### macOS / Linux

```bash
curl -fsSL https://app.factory.ai/cli | sh
```

Verifica instalacion:

```bash
which droid
droid --version
```

### Windows

Usa el instalador oficial de Factory desde su documentacion (el metodo puede variar por version de CLI).

Verifica instalacion:

```bat
where droid
droid --version
```

---

## 3. Primer inicio y autenticacion de Droid

Inicia Droid una vez y completa onboarding:

```bash
droid
```

Tambien puedes autenticar por variable de entorno.

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

1. En Burp Suite, ve a la tienda de extensiones y instala la extension oficial **MCP Server** de PortSwigger.
2. Abre la pestana `MCP` en Burp y activa el servidor (`Enabled`).
3. Verifica que el SSE URL este disponible (en este flujo se usa `http://127.0.0.1:9876`).
4. Al instalar el MCP oficial, se genera en tu carpeta de usuario:

```text
~/.BurpSuite/mcp-proxy/mcp-proxy-all.jar
```

5. Si quieres guardarlo en otra ruta, usa el boton **Extract server proxy jar** en la interfaz MCP de Burp.
6. Manten Burp abierto mientras uses Droid.

---

## 5. Configurar MCP de Burp en Droid

### Opcion A (recomendada): editar `~/.factory/mcp.json`

1. Abre `~/.factory/mcp.json`.
2. Agrega (o ajusta) la entrada `burp` dentro de `mcpServers`.
3. Usa este bloque:

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
5. Reinicia Droid y valida:

```text
/mcp list
```

### Opcion B: pedirle a Droid que lo configure por ti

Puedes pegarle a Droid el JSON anterior y decirle literalmente:

```text
instale este mcp
```

Luego valida con:

```text
/mcp list
```

## 5.1 Control de ejecucion del MCP (recomendado)

Dependiendo del nivel de control que quieras tener sobre lo que haga el MCP, en la pantalla del agente de Droid presiona:

```text
Ctrl+L
```

Con ese atajo puedes ir cambiando la opcion/politica de ejecucion del agente para trabajar con mas o menos supervision en las acciones MCP.

---

## 6. Configurar BurpIA para usar Factory Droid

En BurpIA:

1. `Ajustes` -> pestana `Agentes`.
2. `Seleccionar Agente`: `FACTORY_DROID`.
3. Activar `Habilitar Agente`.
4. Configurar `Ruta del Binario`:
   - macOS/Linux: `~/.local/bin/droid`
   - Windows: `%USERPROFILE%\\bin\\droid.exe`
5. Ajustar `Espera MCP (ms)` segun tu maquina.
6. Guardar ajustes.

---

## 7. Flujo esperado en BurpIA (importante)

Con agente habilitado, BurpIA hace automaticamente:

1. Ejecuta el binario `droid`.
2. Espera el tiempo `Espera MCP (ms)` configurado por usuario.
3. Inyecta el prompt inicial pre-flight.

Tambien inyecta prompt inicial cuando:

- Reinicias consola del agente.
- Cambias de agente.

Y lo inyecta manualmente (sin delay) cuando pulsas `Inyectar Payload`.

---

## 8. Validacion rapida de que todo esta OK

1. En consola de agente de BurpIA, valida que no aparezca error de binario.
2. En Droid, ejecuta `/mcp list` y confirma que Burp MCP aparezca activo.
3. Ejecuta una prueba simple de herramientas MCP.
4. Desde BurpIA, envia un hallazgo al agente y valida respuesta.

---

## 9. Troubleshooting

### Error: "El binario del agente no existe en la ruta..."

- Corrige `Ruta del Binario` en `Ajustes > Agentes`.
- Verifica con `which droid` (mac/Linux) o `where droid` (Windows).

### Droid inicia, pero no ve herramientas de Burp

- Revisa que Burp MCP este `Enabled`.
- Verifica host/puerto SSE en Burp.
- Revisa `mcp.json` de Droid y reinicia Droid.
- Si aplica, reintenta conexion MCP desde Droid.

### Burp MCP responde, pero no hay ejecucion desde el agente

- Confirma que BurpIA tenga `Agente habilitado`.
- Incrementa `Espera MCP (ms)` para dar tiempo a levantar MCPs.
- Usa `Reiniciar` y luego `Inyectar Payload` para forzar pre-flight.

---

## 10. Referencias oficiales

- Factory Droid CLI (instalacion, settings, MCP):
  - https://docs.factory.ai/cli/getting-started/video-walkthrough
  - https://docs.factory.ai/cli/configuration/settings
  - https://docs.factory.ai/reference/cli-reference
- Burp MCP Server (PortSwigger):
  - https://github.com/PortSwigger/mcp-server
