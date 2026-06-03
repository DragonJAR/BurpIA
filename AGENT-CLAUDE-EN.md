# CLAUDE AGENT (Claude Code) - BurpIA

Operational guide to:

1. Install `claude` if it is not installed.
2. Configure Burp Suite MCP for Claude Code.
3. Connect it correctly with BurpIA.

---

## 1. Requirements

- Burp Suite running.
- PortSwigger's official **MCP Server** extension installed in Burp.
- BurpIA loaded in Burp Suite.
- Claude Code CLI (`claude`) installed and authenticated.

---

## 2. Install Claude Code CLI

For complete details and installation guidelines, visit the official website: [Claude Code](https://claude.com/product/claude-code).

### macOS / Linux

```bash
npm install -g @anthropic-ai/claude-code
```

Verify installation:

```bash
which claude
claude --version
```

### Windows

Install Claude Code from Anthropic's official docs. The exact method can vary by release or enterprise policy.

Verify installation:

```bat
where claude
claude --version
```

---

## 3. First run and Claude authentication

Start Claude Code once:

```bash
claude
```

Then complete login:

```text
/login
```

If your environment uses API keys or enterprise authentication, follow your organization's Anthropic policy.

---

## 4. Install Burp Suite official MCP (required)

1. In Burp Suite, install PortSwigger's official **MCP Server** extension.
2. Open the `MCP` tab and enable the server (`Enabled`).
3. Verify that Burp MCP is listening on `http://127.0.0.1:9876`. That value is reused in the stdio proxy through `--sse-url`.
4. Extract or locate `mcp-proxy-all.jar` for Burp's `stdio` proxy.
5. Keep Burp open while using Claude Code.

Notes:

- On macOS the jar is often stored at `/Users/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- On Linux it is commonly stored at `/home/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- On Windows, if you extract it into the user profile, it can live under `%APPDATA%\\BurpSuite\\mcp-proxy\\mcp-proxy-all.jar`.

---

## 5. Configure Burp MCP in Claude Code

### Option A (recommended): add a local `stdio` MCP server through Claude CLI

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

Then validate:

```bash
claude mcp list
```

Inside an active Claude session, you can also verify MCP availability with:

```text
/mcp
```

### Option B: manual or managed configuration

If your environment uses managed config, add a server named `burp` in `mcpServers` using Burp's `stdio` proxy.

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

Common adjustments:

- Replace `USUARIO` with the real user name.
- If you use Burp Community, the folder can be `BurpSuiteCommunity` instead of `BurpSuitePro`.
- On Linux, `/home/USUARIO/BurpSuitePro/jre/bin/java` assumes the common native install under the user's home directory. If Burp is installed elsewhere, update the path.
- If you extracted the jar elsewhere, update the path inside `args`.
- In the Windows example, replace `AudiTHOR03` if your Windows profile name is different.

---

## 5.2 Execution policy in Claude Code (recommended)

When Claude asks for permission to run actions or tools, choose the policy that matches your risk profile.

For sensitive pentest sessions, keep approval prompts enabled.

---

## 6. Configure BurpIA to use Claude Code

In BurpIA:

1. `Settings` -> `Agents` tab.
2. `Select Agent`: `CLAUDE_CODE`.
3. Enable `Enable Agent`.
4. Configure `Binary Path`:
   - macOS/Linux: `~/.local/bin/claude --dangerously-skip-permissions`
   - Windows: `%USERPROFILE%\\.local\\bin\\claude.exe --dangerously-skip-permissions`
5. Adjust `MCP Wait (ms)` based on your machine.
6. Save settings.

Notes:

- BurpIA supports command plus flags in this field.
- If `which claude` or `where claude` returns a different path, use that real path.
- If you do not want to skip confirmations, remove `--dangerously-skip-permissions` and work interactively.

---

## 7. Expected flow in BurpIA

With the agent enabled, BurpIA:

1. Executes the configured Claude command.
2. Waits for the configured `MCP Wait (ms)` value.
3. Injects the pre-flight initial prompt.

It also reinjects the initial prompt when:

- You restart the agent console.
- You switch agents.

If MCP tools are not ready when the pre-flight prompt is injected, retry manually with `Inject Payload`.

---

## 8. Quick validation

1. In BurpIA's agent console, verify no path or command error appears.
2. In Claude, run `claude mcp list` and confirm `burp` is present.
3. In Burp, confirm the MCP server remains enabled.
4. From BurpIA, send a finding or flow to the agent and review the response.

---

## 9. Troubleshooting

### Error: "The agent binary does not exist at the current path..."

- Fix `Binary Path` in `Settings > Agents`.
- Verify the executable with `which claude` or `where claude`.
- If the command includes flags, confirm the executable path is valid on its own first.

### Claude starts, but Burp MCP tools are missing

- Verify Burp MCP is `Enabled`.
- Verify the host and port used in `--sse-url`.
- Review the `stdio` server configuration and confirm that `java` or `java.exe` and `mcp-proxy-all.jar` exist at those paths.
- Restart Claude Code and check `claude mcp list` again.

### Burp MCP responds, but BurpIA agent flow does not execute

- Confirm BurpIA has `Agent enabled`.
- Increase `MCP Wait (ms)` to give MCP more startup time.
- Use `Restart` and then `Inject Payload` to force pre-flight.

---

## 10. Official references

- Claude Code docs:
  - https://docs.anthropic.com/en/docs/claude-code/getting-started
  - https://docs.anthropic.com/en/docs/claude-code/mcp
- Burp MCP Server (PortSwigger):
  - https://github.com/PortSwigger/mcp-server
- Burp native installers and bundled JRE:
  - https://portswigger.net/burp/documentation/desktop/troubleshooting/launch-from-command-line
