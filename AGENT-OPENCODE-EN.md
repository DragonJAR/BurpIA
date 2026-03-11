# OPENCODE AGENT (OpenCode) - BurpIA

Operational guide to:

1. Install `opencode` if it is not installed.
2. Configure Burp Suite MCP for OpenCode.
3. Connect it correctly with BurpIA.

---

## 1. Requirements

- Burp Suite running.
- PortSwigger's official **MCP Server** extension installed in Burp.
- BurpIA loaded in Burp Suite.
- OpenCode CLI (`opencode`) installed.
- A provider or account configured inside OpenCode.

---

## 2. Install OpenCode

### macOS / Linux

Recommended installation:

```bash
curl -fsSL https://opencode.ai/install | bash
```

Alternative via npm:

```bash
npm install -g opencode-ai
```

Verify installation:

```bash
which opencode
opencode --version
```

### Windows

OpenCode recommends **WSL** for the best overall experience. For BurpIA integration on Windows, this guide documents a native Windows install or any install that exposes an `opencode` binary callable from BurpIA.

Common options:

```powershell
choco install opencode
```

```powershell
scoop install opencode
```

```powershell
npm install -g opencode-ai
```

Verify installation:

```bat
where opencode
opencode --version
```

---

## 3. First run and OpenCode authentication

Start OpenCode once:

```bash
opencode
```

Inside the TUI run:

```text
/connect
```

Then:

1. Select `opencode` or the LLM provider you plan to use.
2. Complete login or paste your API key.
3. Confirm you can open a normal session before integrating it with BurpIA.

---

## 4. Install Burp Suite official MCP (required)

1. In Burp Suite, install PortSwigger's official **MCP Server** extension.
2. Open the `MCP` tab and enable the server (`Enabled`).
3. Verify that Burp MCP is listening on `http://127.0.0.1:9876`. That value is reused in the stdio proxy through `--sse-url`.
4. Extract or locate `mcp-proxy-all.jar` for Burp's stdio proxy.
5. Keep Burp open while using OpenCode.

Notes:

- On macOS the jar is often stored at `/Users/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- On Linux it is commonly stored at `/home/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- On Windows it can live under `%APPDATA%\\BurpSuite\\mcp-proxy\\mcp-proxy-all.jar` if extracted into the user profile.

---

## 5. Configure Burp MCP in OpenCode

### Option A (recommended): use the OpenCode wizard

Run:

```bash
opencode mcp add
```

The wizard guides you through adding either a local or remote MCP server. For Burp, configure a local server named `burp` that starts the stdio proxy with:

- Burp's packaged `java` executable.
- The `mcp-proxy-all.jar` file.
- The argument `--sse-url http://127.0.0.1:9876`.

Then validate:

```bash
opencode mcp list
```

### Option B: manual configuration in `opencode.json`

OpenCode documents MCP configuration under the `mcp` key using `type: "local"` plus a `command` array.

macOS example:

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

Linux example:

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

Native Windows example:

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

Notes:

- OpenCode uses a `command` array. It does not split `command` and `args` the way some other clients do.
- OpenCode's global config lives at `~/.config/opencode/opencode.json`.
- If you use WSL, edit that file inside the WSL environment, not from the native Windows profile.
- If you use Burp Community, the folder can be `BurpSuiteCommunity`.
- On Linux, `/home/USUARIO/BurpSuitePro/jre/bin/java` assumes the common native install under the user's home directory. If Burp is installed elsewhere, update the path.
- In the Windows example, replace `AudiTHOR03` if your Windows profile name is different.

### 5.1 Reference: Burp stdio proxy in `mcpServers` format

If another managed client asks for the same proxy in `mcpServers` format, these are the manual equivalents:

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

## 6. Configure BurpIA to use OpenCode

In BurpIA:

1. `Settings` -> `Agents` tab.
2. `Select Agent`: `OPEN_CODE`.
3. Enable `Enable Agent`.
4. Configure `Binary Path`:
   - macOS/Linux: `~/.opencode/bin/opencode`
   - Windows: `%USERPROFILE%\\.opencode\\bin\\opencode.exe`
5. Adjust `MCP Wait (ms)` based on your machine.
6. Save settings.

Notes:

- BurpIA supports command plus flags in this field.
- If you installed OpenCode with `npm`, `choco`, or `scoop`, the real path may differ. Use the output from `which opencode` or `where opencode`.
- If you decide to run OpenCode from WSL, you will need a BurpIA command that is callable from Windows and launches that WSL session.

---

## 7. Expected flow in BurpIA

With the agent enabled, BurpIA:

1. Executes the configured OpenCode command.
2. Waits for the configured `MCP Wait (ms)` value.
3. Injects the pre-flight initial prompt.

It also reinjects the initial prompt when:

- You restart the agent console.
- You switch agents.

If MCP tools are not ready when the pre-flight prompt is injected, retry manually with `Inject Payload`.

---

## 8. Quick validation

1. In BurpIA's agent console, verify no path or command error appears.
2. Run `opencode mcp list` and confirm `burp` appears as configured and connected.
3. In Burp, confirm the MCP server remains enabled.
4. From BurpIA, send a finding or flow to the agent and review the response.

---

## 9. Troubleshooting

### Error: "The agent binary does not exist at the current path..."

- Fix `Binary Path` in `Settings > Agents`.
- Verify with `which opencode` or `where opencode`.
- If you configured WSL, confirm the launch command is callable from the same environment where Burp is running.

### OpenCode starts, but Burp MCP tools are missing

- Verify Burp MCP is `Enabled`.
- Verify the host and port used in `--sse-url`.
- Check `opencode mcp list`.
- If you use manual configuration, review `~/.config/opencode/opencode.json`.

### Burp MCP responds, but BurpIA agent flow does not execute

- Confirm BurpIA has `Agent enabled`.
- Increase `MCP Wait (ms)` to give MCP more startup time.
- Use `Restart` and then `Inject Payload` to force pre-flight.

---

## 10. Official references

- OpenCode:
  - https://opencode.ai/docs/
  - https://opencode.ai/docs/cli/
  - https://opencode.ai/docs/config/
  - https://opencode.ai/docs/mcp-servers/
  - https://opencode.ai/docs/windows-wsl/
- Burp MCP Server (PortSwigger):
  - https://github.com/PortSwigger/mcp-server
- Burp native installers and bundled JRE:
  - https://portswigger.net/burp/documentation/desktop/troubleshooting/launch-from-command-line
