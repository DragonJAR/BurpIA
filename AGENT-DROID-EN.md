# DROID AGENT (Factory Droid) - BurpIA

Operational guide to:

1. Install `droid` if it is not installed.
2. Configure Burp Suite MCP in Droid.
3. Connect it correctly with BurpIA.

---

## 1. Requirements

- Burp Suite running.
- PortSwigger's official **MCP Server** extension installed in Burp.
- BurpIA loaded in Burp Suite.
- Factory Droid CLI (`droid`) installed and authenticated.

---

## 2. Install Droid CLI

### macOS / Linux

```bash
curl -fsSL https://app.factory.ai/cli | sh
```

Verify installation:

```bash
which droid
droid --version
```

### Windows

Use Factory's official installer. The exact method can vary by CLI version or enterprise policy.

Verify installation:

```bat
where droid
droid --version
```

---

## 3. First run and Droid authentication

Start Droid once:

```bash
droid
```

You can also authenticate with an environment variable.

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

## 4. Install Burp Suite official MCP (required)

1. In Burp Suite, install PortSwigger's official **MCP Server** extension.
2. Open the `MCP` tab and enable the server (`Enabled`).
3. Verify that Burp MCP is listening on `http://127.0.0.1:9876`. That value is reused in the stdio proxy through `--sse-url`.
4. Extract or locate `mcp-proxy-all.jar` for Burp's stdio proxy.
5. Keep Burp open while using Droid.

Notes:

- On macOS the jar is often stored at `/Users/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- On Linux it is commonly stored at `/home/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- On Windows it can live under `%APPDATA%\\BurpSuite\\mcp-proxy\\mcp-proxy-all.jar` if extracted into the user profile.

---

## 5. Configure Burp MCP in Droid

### Option A (recommended): edit `~/.factory/mcp.json`

1. Open `~/.factory/mcp.json`.
2. Add or adjust the `burp` entry under `mcpServers`.
3. Use the block that matches your system.

macOS example:

```json
{
  "mcpServers": {
    "burp": {
      "type": "stdio",
      "command": "/Applications/Burp Suite Professional.app/Contents/Resources/jre.bundle/Contents/Home/bin/java",
      "args": [
        "-jar",
        "/Users/user/.BurpSuite/mcp-proxy/mcp-proxy-all.jar",
        "--sse-url",
        "http://127.0.0.1:9876"
      ],
      "disabled": false
    }
  }
}
```

Linux example:

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

Windows example:

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

Notes:

- Replace `USUARIO` with your real user name.
- If you use Burp Community, the folder can be `BurpSuiteCommunity`.
- On Linux, `/home/USUARIO/BurpSuitePro/jre/bin/java` assumes the common native install under the user's home directory. If Burp is installed elsewhere, update the path.
- If you extracted the jar elsewhere, update `args`.
- In the Windows example, replace `AudiTHOR03` if your Windows profile name is different.

Then restart Droid and validate:

```text
/mcp list
```

### Option B: ask Droid to configure it for you

You can paste the JSON into Droid and ask it to install it. Example:

```text
install this mcp and add it to ~/.factory/mcp.json:
```

Then paste the JSON block for your system and validate with:

```text
/mcp list
```

---

## 5.1 MCP execution control (recommended)

Depending on how much control you want over MCP actions, in the Droid agent screen press:

```text
Ctrl+L
```

That shortcut changes the execution policy so you can work with more or less supervision over MCP actions.

---

## 6. Configure BurpIA to use Factory Droid

In BurpIA:

1. `Settings` -> `Agents` tab.
2. `Select Agent`: `FACTORY_DROID`.
3. Enable `Enable Agent`.
4. Configure `Binary Path`:
   - macOS/Linux: `~/.local/bin/droid`
   - Windows: `%USERPROFILE%\\bin\\droid.exe`
5. Adjust `MCP Wait (ms)` based on your machine.
6. Save settings.

Notes:

- If `which droid` or `where droid` returns a different path, use the real path.
- BurpIA accepts command plus flags if your environment needs extra Droid parameters.

---

## 7. Expected flow in BurpIA

With the agent enabled, BurpIA:

1. Executes the configured Droid binary or command.
2. Waits for the configured `MCP Wait (ms)` value.
3. Injects the pre-flight initial prompt.

It also reinjects the initial prompt when:

- You restart the agent console.
- You switch agents.

If MCP tools are not ready when the pre-flight prompt is injected, retry manually with `Inject Payload`.

---

## 8. Quick validation

1. In BurpIA's agent console, verify no binary or command error appears.
2. In Droid, run `/mcp list` and confirm Burp MCP is active.
3. Run a simple MCP tool test.
4. From BurpIA, send a finding or flow to the agent and validate the response.

---

## 9. Troubleshooting

### Error: "The agent binary does not exist at path..."

- Fix `Binary Path` in `Settings > Agents`.
- Verify with `which droid` or `where droid`.

### Droid starts, but it cannot see Burp tools

- Check that Burp MCP is `Enabled`.
- Verify the host and port used in `--sse-url`.
- Check Droid `mcp.json` and restart Droid.
- If applicable, retry the MCP connection from Droid.

### Burp MCP responds, but agent execution does not happen

- Confirm BurpIA has `Agent enabled`.
- Increase `MCP Wait (ms)` to give MCP more startup time.
- Use `Restart` and then `Inject Payload` to force pre-flight.

---

## 10. Official references

- Factory Droid CLI:
  - https://docs.factory.ai/cli/getting-started/video-walkthrough
  - https://docs.factory.ai/cli/configuration/settings
  - https://docs.factory.ai/reference/cli-reference
- Burp MCP Server (PortSwigger):
  - https://github.com/PortSwigger/mcp-server
- Burp native installers and bundled JRE:
  - https://portswigger.net/burp/documentation/desktop/troubleshooting/launch-from-command-line
