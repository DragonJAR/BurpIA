# AGENTE CLAUDE (Claude Code) - BurpIA

Guía operativa para:

1. Instalar `claude` si no está instalado.
2. Configurar el MCP de Burp Suite para Claude Code.
3. Conectarlo correctamente con BurpIA.

---

## 1. Requisitos

- Burp Suite ejecutándose.
- Extensión **MCP Server** de PortSwigger instalada en Burp (debe aparecer la pestaña `MCP`).
- BurpIA cargado en Burp Suite.
- Claude Code CLI (`claude`) instalado.

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

Instala Claude Code desde la documentación oficial de Anthropic (el método puede variar según la versión).

Verifica instalación:

```bat
where claude
claude --version
```

---

## 3. Primer inicio y autenticación de Claude

Inicia Claude Code una vez y completa login:

```bash
claude
```

Luego ejecuta:

```text
/login
```

Si tu entorno usa API keys o autenticación empresarial, sigue la política de tu organización para Anthropic.

---

## 4. Instalar MCP oficial de Burp Suite (obligatorio)

1. En Burp Suite, ve a la tienda de extensiones y instala la extensión oficial **MCP Server** de PortSwigger.
2. Abre la pestaña `MCP` en Burp y activa el servidor (`Enabled`).
3. Verifica que el SSE URL esté disponible (en este flujo se usa `http://127.0.0.1:9876`).
4. Mantén Burp abierto mientras uses Claude Code.

---

## 5. Configurar MCP de Burp en Claude Code

### Opción A (recomendada): agregar servidor MCP desde Claude CLI

Ejecuta:

```bash
claude mcp add --scope user --transport sse burp http://127.0.0.1:9876
```

Luego valida:

```bash
claude mcp list
```

Dentro de una sesión activa de Claude también puedes validar disponibilidad MCP con:

```text
/mcp
```

### Opción B: configuración manual (avanzado)

Si tu entorno usa configuración administrada, agrega un servidor llamado `burp` con SSE URL `http://127.0.0.1:9876` en los ajustes MCP de Claude, reinicia Claude Code y valida con `claude mcp list`.

---

## 5.1 Política de ejecución en Claude Code (recomendado)

Cuando Claude solicite permisos para ejecutar acciones/herramientas, elige la política según tu perfil de riesgo.

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

- BurpIA soporta comando + flags en este campo (no solo ruta de ejecutable).
- Si tu binario `claude` está en otra ubicación, usa la ruta completa.

---

## 7. Flujo esperado en BurpIA (importante)

Con agente habilitado, BurpIA hace automáticamente:

1. Ejecuta el comando de Claude configurado.
2. Espera el tiempo `Espera MCP (ms)` configurado por usuario.
3. Inyecta el prompt inicial pre-flight.

También inyecta prompt inicial cuando:

- Reinicias consola del agente.
- Cambias de agente.

Si al insertar el prompt pre-flight los MCP aún no han cargado, reintenta manualmente (sin delay) con `Inyectar Payload`.

---

## 8. Validación rápida de que todo está OK

1. En consola de agente de BurpIA, valida que no aparezca error de ruta/comando.
2. En Claude, ejecuta `claude mcp list` y confirma que aparece `burp`.
3. En Burp, valida que el servidor MCP permanezca activo.
4. Desde BurpIA, envía un hallazgo al agente y valida la calidad de la respuesta.

---

## 9. Troubleshooting

### Error: "El binario del agente no existe en la ruta actual..."

- Corrige `Ruta del Binario` en `Ajustes > Agentes`.
- Verifica ejecutable con `which claude` (mac/Linux) o `where claude` (Windows).
- Si usas flags, confirma primero que la ruta al ejecutable sea válida.

### Claude inicia, pero no aparecen herramientas MCP de Burp

- Verifica que Burp MCP esté `Enabled`.
- Verifica host/puerto SSE en Burp.
- Reejecuta `claude mcp add --scope user --transport sse burp http://127.0.0.1:9876`.
- Reinicia Claude Code y revisa `claude mcp list` de nuevo.

### Burp MCP responde, pero el flujo del agente en BurpIA no ejecuta

- Confirma que BurpIA tenga `Agente habilitado`.
- Incrementa `Espera MCP (ms)` para dar más tiempo de arranque a MCP.
- Usa `Reiniciar` y luego `Inyectar Payload` para forzar pre-flight.

---

## 10. Referencias oficiales

- Claude Code docs:
  - https://docs.anthropic.com/en/docs/claude-code/getting-started
  - https://docs.anthropic.com/en/docs/claude-code/mcp
- Burp MCP Server (PortSwigger):
  - https://github.com/PortSwigger/mcp-server
