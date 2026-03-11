# AGENTE DROID (Factory Droid) - BurpIA

Guía operativa para:

1. Instalar `droid` si no está instalado.
2. Configurar el MCP de Burp Suite en Droid.
3. Conectarlo correctamente con BurpIA.

---

## 1. Requisitos

- Burp Suite ejecutándose.
- Extensión oficial **MCP Server** de PortSwigger instalada en Burp.
- BurpIA cargado en Burp Suite.
- Factory Droid CLI (`droid`) instalado y autenticado.

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

Usa el instalador oficial de Factory. El método puede variar por versión de la CLI o por políticas de tu entorno.

Verifica instalación:

```bat
where droid
droid --version
```

---

## 3. Primer inicio y autenticación de Droid

Inicia Droid una vez:

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

1. En Burp Suite, instala la extensión oficial **MCP Server** de PortSwigger.
2. Abre la pestaña `MCP` y activa el servidor (`Enabled`).
3. Verifica que Burp MCP esté escuchando en `http://127.0.0.1:9876`. Ese valor se reutiliza en el flag `--sse-url` del proxy `stdio`.
4. Extrae o localiza `mcp-proxy-all.jar` para el proxy stdio.
5. Mantén Burp abierto mientras uses Droid.

Notas:

- En macOS el jar suele quedar en `/Users/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- En Linux suele quedar en `/home/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- En Windows puede quedar bajo `%APPDATA%\\BurpSuite\\mcp-proxy\\mcp-proxy-all.jar` si lo extraes al perfil del usuario.

---

## 5. Configurar MCP de Burp en Droid

### Opción A (recomendada): editar `~/.factory/mcp.json`

1. Abre `~/.factory/mcp.json`.
2. Agrega o ajusta la entrada `burp` dentro de `mcpServers`.
3. Usa el bloque que corresponda a tu sistema.

Ejemplo macOS:

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

Ejemplo Linux:

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

Ejemplo Windows:

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

Notas:

- Cambia `USUARIO` por tu usuario real.
- Si usas Burp Community, la carpeta puede ser `BurpSuiteCommunity`.
- En Linux la ruta `/home/USUARIO/BurpSuitePro/jre/bin/java` asume la instalación nativa habitual en el home. Si instalaste Burp en otra ruta, ajústala.
- Si extrajiste el jar en otra ruta, actualiza `args`.
- En el ejemplo de Windows, sustituye `AudiTHOR03` por el usuario real si tu perfil de Windows es distinto.

Luego reinicia Droid y valida:

```text
/mcp list
```

### Opción B: pedirle a Droid que lo configure por ti

Puedes pegarle el JSON a Droid y pedirle que lo instale. Ejemplo:

```text
instale este mcp y agrégalo a ~/.factory/mcp.json:
```

Después pega el bloque JSON que corresponda a tu sistema y valida con:

```text
/mcp list
```

---

## 5.1 Control de ejecución del MCP (recomendado)

Dependiendo del nivel de control que quieras tener sobre lo que haga el MCP, en la pantalla del agente de Droid presiona:

```text
Ctrl+L
```

Ese atajo cambia la política de ejecución del agente para trabajar con más o menos supervisión sobre acciones MCP.

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

Notas:

- Si `which droid` o `where droid` devuelven otra ruta, usa la ruta real.
- BurpIA acepta comando más flags si en tu entorno Droid requiere parámetros adicionales.

---

## 7. Flujo esperado en BurpIA

Con agente habilitado, BurpIA:

1. Ejecuta el binario o comando configurado para Droid.
2. Espera el tiempo `Espera MCP (ms)` configurado por el usuario.
3. Inyecta el prompt inicial pre-flight.

También reinyecta el prompt inicial cuando:

- Reinicias la consola del agente.
- Cambias de agente.

Si cuando entra el pre-flight los MCP aún no están listos, reintenta manualmente con `Inyectar Payload`.

---

## 8. Validación rápida

1. En la consola de agente de BurpIA, valida que no aparezca error de binario o comando.
2. En Droid, ejecuta `/mcp list` y confirma que Burp MCP aparezca activo.
3. Ejecuta una prueba simple de herramientas MCP.
4. Desde BurpIA, envía un hallazgo o flujo al agente y valida la respuesta.

---

## 9. Troubleshooting

### Error: "El binario del agente no existe en la ruta..."

- Corrige `Ruta del Binario` en `Ajustes > Agentes`.
- Verifica con `which droid` o `where droid`.

### Droid inicia, pero no ve herramientas de Burp

- Revisa que Burp MCP esté `Enabled`.
- Verifica host y puerto en `--sse-url`.
- Revisa `mcp.json` de Droid y reinicia Droid.
- Si aplica, vuelve a intentar la conexión MCP desde Droid.

### Burp MCP responde, pero no hay ejecución desde el agente

- Confirma que BurpIA tenga `Agente habilitado`.
- Incrementa `Espera MCP (ms)` para dar tiempo a levantar MCP.
- Usa `Reiniciar` y luego `Inyectar Payload` para forzar el pre-flight.

---

## 10. Referencias oficiales

- Factory Droid CLI:
  - https://docs.factory.ai/cli/getting-started/video-walkthrough
  - https://docs.factory.ai/cli/configuration/settings
  - https://docs.factory.ai/reference/cli-reference
- Burp MCP Server (PortSwigger):
  - https://github.com/PortSwigger/mcp-server
- Burp y su JRE privado en instaladores nativos:
  - https://portswigger.net/burp/documentation/desktop/troubleshooting/launch-from-command-line
