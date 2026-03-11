# GEMINI CLI AGENT - BurpIA

Operational guide to:

1. Install `gemini` if it is not installed.
2. Configure Burp Suite MCP for Gemini CLI.
3. Connect it correctly with BurpIA.

---

## 1. Requirements

- Burp Suite running.
- PortSwigger's official **MCP Server** extension installed in Burp.
- BurpIA loaded in Burp Suite.
- Gemini CLI (`gemini`) installed and authenticated.

---

## 2. Install Gemini CLI

### macOS / Linux

```bash
npm install -g @google/gemini-cli
```

Verify installation:

```bash
which gemini
gemini --version
```

### Windows

Install Gemini CLI from Google's official docs. The exact method can vary by release or enterprise policy.

Verify installation:

```bat
where gemini
gemini --version
```

---

## 3. First run and Gemini authentication

Start Gemini CLI once:

```bash
gemini
```

Follow Google's authentication flow.

If your environment uses API keys or enterprise auth, follow your organization's Google policy.

---

## 4. Install Burp Suite official MCP (required)

1. In Burp Suite, install PortSwigger's official **MCP Server** extension.
2. Open the `MCP` tab and enable the server (`Enabled`).
3. Verify that Burp MCP is listening on `http://127.0.0.1:9876`. That value is reused in the stdio proxy through `--sse-url`.
4. Extract or locate `mcp-proxy-all.jar` for Burp's `stdio` proxy.
5. Keep Burp open while using Gemini CLI.

Notes:

- On macOS the jar is often stored at `/Users/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- On Linux it is commonly stored at `/home/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- On Windows it can live under `%APPDATA%\\BurpSuite\\mcp-proxy\\mcp-proxy-all.jar` if extracted into the user profile.

---

## 5. Configure Burp MCP in Gemini CLI

### Option A (recommended): edit `~/.gemini/settings.json`

Add or update the `burp` entry under `mcpServers` using Burp's `stdio` proxy.

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

Then validate:

```bash
gemini mcp list
```

Inside an active Gemini session, you can also verify MCP availability with:

```text
/mcp
```

### Option B: managed configuration or manual variants

If your environment uses a managed layer instead of `~/.gemini/settings.json`, reuse the same `mcpServers` block from the previous example and only adapt the config file location used by your installation.

Common adjustments:

- Replace `USUARIO` with the real user name.
- If you use Burp Community, the folder can be `BurpSuiteCommunity` instead of `BurpSuitePro`.
- On Linux, `/home/USUARIO/BurpSuitePro/jre/bin/java` assumes the common native install under the user's home directory. If Burp is installed elsewhere, update the path.
- If you extracted the jar elsewhere, update the path inside `args`.
- In the Windows example, replace `AudiTHOR03` if your Windows profile name is different.

---

## 5.2 Execution policy in Gemini CLI (recommended)

When Gemini asks for permission to run actions or tools, choose the policy that matches your risk profile.

For sensitive pentest sessions, keep approval prompts enabled.

---

## 6. Configure BurpIA to use Gemini CLI

In BurpIA:

1. `Settings` -> `Agents` tab.
2. `Select Agent`: `GEMINI_CLI`.
3. Enable `Enable Agent`.
4. Configure `Binary Path`:
   - macOS: `/opt/homebrew/bin/gemini --yolo`
   - Linux: `~/.local/bin/gemini --yolo`
   - Windows: `%USERPROFILE%\\bin\\gemini.exe --yolo`
5. Adjust `MCP Wait (ms)` based on your machine.
6. Save settings.

Notes:

- BurpIA supports command plus flags in this field.
- If `which gemini` or `where gemini` returns a different path, use that real path.
- The `--yolo` flag reduces confirmations. Remove it if you prefer manual approval.

---

## 7. Expected flow in BurpIA

With the agent enabled, BurpIA:

1. Executes the configured Gemini command.
2. Waits for the configured `MCP Wait (ms)` value.
3. Injects the pre-flight initial prompt.

It also reinjects the initial prompt when:

- You restart the agent console.
- You switch agents.

If MCP tools are not ready when the pre-flight prompt is injected, retry manually with `Inject Payload`.

---

## 8. Quick validation

1. In BurpIA's agent console, verify no path or command error appears.
2. In Gemini, run `gemini mcp list` and confirm `burp` is present.
3. In Burp, confirm the MCP server remains enabled.
4. From BurpIA, send a finding or flow to the agent and review the response.

---

## 9. Troubleshooting

### Error: "The agent binary does not exist at the current path..."

- Fix `Binary Path` in `Settings > Agents`.
- Verify the executable with `which gemini` or `where gemini`.
- If the command includes flags, confirm the executable path is valid on its own first.

### Gemini starts, but Burp MCP tools are missing

- Verify Burp MCP is `Enabled`.
- Verify the host and port used in `--sse-url`.
- Review `~/.gemini/settings.json` and confirm that `java` or `java.exe` and `mcp-proxy-all.jar` exist at those paths.
- Restart Gemini CLI and check `gemini mcp list` again.

### Burp MCP responds, but BurpIA agent flow does not execute

- Confirm BurpIA has `Agent enabled`.
- Increase `MCP Wait (ms)` to give MCP more startup time.
- Use `Restart` and then `Inject Payload` to force pre-flight.

---

## 10. Official references

- Gemini CLI docs:
  - https://github.com/google/gemini-cli
- Burp MCP Server (PortSwigger):
  - https://github.com/PortSwigger/mcp-server
- Burp native installers and bundled JRE:
  - https://portswigger.net/burp/documentation/desktop/troubleshooting/launch-from-command-line
