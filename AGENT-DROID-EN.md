# DROID AGENT (Factory Droid) - BurpIA

Operational guide to:

1. Install `droid` if it is not installed.
2. Configure Burp Suite MCP in Droid.
3. Connect it correctly with BurpIA.

---

## 1. Requirements

- Burp Suite running.
- PortSwigger **MCP Server** extension installed in Burp (the `MCP` tab must be visible).
- BurpIA loaded in Burp Suite.
- Factory Droid CLI (`droid`) installed.

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

Use Factory's official installer from their documentation (the method may vary by CLI version).

Verify installation:

```bat
where droid
droid --version
```

---

## 3. First run and Droid authentication

Start Droid once and complete onboarding:

```bash
droid
```

You can also authenticate through an environment variable.

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

1. In Burp Suite, open the extension store and install PortSwigger's official **MCP Server** extension.
2. Open the `MCP` tab in Burp and enable the server (`Enabled`).
3. Verify the SSE URL is available (this flow uses `http://127.0.0.1:9876`).
4. When installing the official MCP, the following file is created in your user folder:

```text
~/.BurpSuite/mcp-proxy/mcp-proxy-all.jar
```

5. If you want to save it in a different location, use the **Extract server proxy jar** button in Burp's MCP interface.
6. Keep Burp open while using Droid.

---

## 5. Configure Burp MCP in Droid

### Option A (recommended): edit `~/.factory/mcp.json`

1. Open `~/.factory/mcp.json`.
2. Add (or adjust) the `burp` entry under `mcpServers`.
3. Use this block (macOS example):

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

4. If you saved the jar in another location (using **Extract server proxy jar**), update the path in `args`.
5. If you are not on macOS, change `command` to your system's `java` path.
6. Restart Droid and validate:

```text
/mcp list
```

### Option B: ask Droid to configure it for you

You can paste the JSON into Droid and ask it to install it. Example:

```text
install this mcp and add it to ~/.factory/mcp.json:
```

Then paste the JSON block:

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

Then validate with:

```text
/mcp list
```

---

## 5.1 MCP execution control (recommended)

Depending on how much control you want over MCP actions, in the Droid agent screen press:

```text
Ctrl+L
```

With this shortcut you can switch the agent execution policy to work with more or less supervision over MCP actions.

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

---

## 7. Expected flow in BurpIA (important)

With the agent enabled, BurpIA automatically:

1. Executes the `droid` binary.
2. Waits for the user-configured `MCP Wait (ms)` time.
3. Injects the pre-flight initial prompt.

It also injects the initial prompt when:

- You restart the agent console.
- You switch agents.

If MCP tools are not loaded yet when the pre-flight prompt is injected, you can retry manually (without delay) using `Inject Payload`.

---

## 8. Quick validation checklist

1. In BurpIA's agent console, verify no binary path error appears.
2. In Droid, run `/mcp list` and confirm Burp MCP is active.
3. Run a simple MCP tool test.
4. From BurpIA, send a finding to the agent and validate the response.

---

## 9. Troubleshooting

### Error: "The agent binary does not exist at path..."

- Fix `Binary Path` in `Settings > Agents`.
- Verify with `which droid` (mac/Linux) or `where droid` (Windows).

### Droid starts, but Burp tools are missing

- Check that Burp MCP is `Enabled`.
- Verify SSE host/port in Burp.
- Check Droid `mcp.json` and restart Droid.
- If needed, retry MCP connection from Droid.

### Burp MCP responds, but agent execution does not happen

- Confirm BurpIA has `Agent enabled`.
- Increase `MCP Wait (ms)` to give MCPs more startup time.
- Use `Restart` and then `Inject Payload` to force pre-flight.

---

## 10. Official references

- Factory Droid CLI (installation, settings, MCP):
  - https://docs.factory.ai/cli/getting-started/video-walkthrough
  - https://docs.factory.ai/cli/configuration/settings
  - https://docs.factory.ai/reference/cli-reference
- Burp MCP Server (PortSwigger):
  - https://github.com/PortSwigger/mcp-server
