# GROK AGENT - BurpIA

Operational guide to:
1. Install `grok` if it is not installed.
2. Configure Burp Suite MCP for Grok.
3. Connect it correctly with BurpIA.

---

## 1. Requirements

- Burp Suite running.
- PortSwigger's official **MCP Server** extension installed in Burp.
- BurpIA loaded in Burp Suite.
- Grok CLI/TUI (`grok`) installed and authenticated.

---

## 2. Install Grok CLI/TUI

Grok (from xAI) is typically installed via the official Grok desktop app / TUI installer. It self-manages into `~/.grok/`.

### macOS / Linux (common after install)

```bash
which grok
~/.grok/bin/grok --version
grok --version
```

If not in PATH, add `~/.grok/bin` to your PATH or use the full path.

### Windows

The Grok installer places the executable under the user profile. Check:

```bat
where grok
%USERPROFILE%\.grok\bin\grok.exe --version
```

Follow xAI official installation instructions for your platform.

---

## 3. First run and Grok authentication

Start Grok once:

```bash
grok
```

Complete any login / API key flow (it may open a browser or use auth.json under `~/.grok/auth.json`).

If your environment uses API keys, place them according to Grok's configuration (see `~/.grok/config.toml` or the TUI settings).

---

## 4. Install Burp Suite official MCP (required)

1. In Burp Suite, install PortSwigger's official **MCP Server** extension.
2. Open the `MCP` tab and enable the server (`Enabled`).
3. Verify that Burp MCP is listening on `http://127.0.0.1:9876`. That value is reused in the stdio proxy through `--sse-url`.
4. Extract or locate `mcp-proxy-all.jar` for Burp's `stdio` proxy.
5. Keep Burp open while using Grok.

Notes:

- On macOS the jar is often stored at `/Users/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- On Linux it is commonly stored at `/home/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- On Windows, if you extract it into the user profile, it can live under `%APPDATA%\\BurpSuite\\mcp-proxy\\mcp-proxy-all.jar`.

---

## 5. Configure Burp MCP in Grok

Grok stores MCP servers in `~/.grok/config.toml` under `[mcp_servers.<name>]`.

### Option A (recommended for complex args): direct edit of `~/.grok/config.toml`

Append or create the section:

macOS (example with discovered paths):

```toml
[mcp_servers.burp]
command = "/Applications/Burp Suite.app/Contents/Resources/jre.bundle/Contents/Home/bin/java"
args = ["-jar", "/Users/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar", "--sse-url", "http://127.0.0.1:9876"]
enabled = true
```

Linux:

```toml
[mcp_servers.burp]
command = "/home/USUARIO/BurpSuitePro/jre/bin/java"
args = ["-jar", "/home/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar", "--sse-url", "http://127.0.0.1:9876"]
enabled = true
```

Windows:

```toml
[mcp_servers.burp]
command = "C:\\Users\\USUARIO\\AppData\\Local\\BurpSuitePro\\jre\\bin\\java.exe"
args = ["-jar", "C:\\Users\\USUARIO\\AppData\\Roaming\\BurpSuite\\mcp-proxy\\mcp-proxy-all.jar", "--sse-url", "http://127.0.0.1:9876"]
enabled = true
```

### Option B: using the CLI helper

```bash
grok mcp add burp \
  --command "/Applications/Burp Suite.app/Contents/Resources/jre.bundle/Contents/Home/bin/java" \
  --args "-jar" \
  --args "/Users/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar" \
  --args "--sse-url" \
  --args "http://127.0.0.1:9876"
```

Validate:

```bash
grok mcp list
grok inspect --json   # shows loaded servers including source "configToml" for burp
```

Inside an active Grok TUI session, open the MCP management modal with:

```text
/mcps
```
or **Ctrl + L**

Then locate the `burp` server, enable it and start it if needed. After starting, tools become discoverable.

Common adjustments:

- Replace `USUARIO` with the real user name.
- If you use Burp Community, the folder can be `BurpSuiteCommunity` instead of `BurpSuitePro` / `Burp Suite.app`.
- On macOS the app folder is frequently just `Burp Suite.app` (not Professional). Adjust the `command` path accordingly.
- If you extracted the jar elsewhere, update the path inside `args`.
- After editing `config.toml`, restart Grok or reload via the /mcps modal for the change to take effect in the TUI.

---

## 5.2 Execution / Permission policy in Grok (recommended)

Grok controls tool execution permissions via `~/.grok/config.toml`:

```toml
[ui]
permission_mode = "always-approve"
yolo = false   # or true for fully automatic (use with caution)
```

For automated pentest agent sessions in BurpIA, set `permission_mode = "always-approve"` (or the equivalent non-interactive mode your Grok version supports).

You can also set this via the TUI settings.

---

## 6. Configure BurpIA to use Grok

In BurpIA:

1. `Settings` -> `Agents` tab.
2. `Select Agent`: `GROK` (or the visible name "Grok" / "Grok CLI" once registered in the AgenteTipo enum).
3. Enable `Enable Agent`.
4. Configure `Binary Path`:
   - macOS/Linux: `~/.grok/bin/grok`
   - Windows: `%USERPROFILE%\.grok\bin\grok.exe`
5. Adjust `MCP Wait (ms)` based on your machine (Grok + Java proxy can take a moment to spin up the Burp MCP bridge).
6. Save settings.

Notes:

- BurpIA supports command plus flags in this field if your Grok invocation needs extra arguments.
- If `which grok` returns a different path, use the real full path.
- Grok picks up MCP configuration from `~/.grok/config.toml` on startup. Make sure the `burp` server is enabled there.
- Permission behavior is controlled primarily in Grok's own `config.toml` (see section 5.2). The `--dangerously-skip-permissions` style flag is not used the same way as Claude; configure the permission_mode instead.

---

## 7. Expected flow in BurpIA + Grok pre-flight

With the agent enabled, BurpIA:
1. Executes the configured `grok` command.
2. Waits for the configured `MCP Wait (ms)`.
3. Injects the pre-flight initial prompt.

**Grok-specific pre-flight (critical):**

Grok agents perform a strict "BURPAI CRITICAL PRE-FLIGHT CHECK" at the beginning of sessions that involve Burp:

- It inventories all tools via `search_tool`.
- Confirms the `burp` server is connected and lists all 27+ tools (including `burp__send_http1_request`, `burp__get_scanner_issues`, `burp__create_repeater_tab`, `burp__get_proxy_http_history`, etc.).
- Classifies Level 1 = Burp tools (primary).
- Enforces the protocol: **always use `burp__send_http1_request`** for traffic (HTTP/1.1 only). Never `send_http2_request`.
- Requires `\r\n` (CRLF) in all HTTP `content` headers.
- **MUST** call `search_tool` first to retrieve the exact input schema before any `use_tool` call on `burp__*` tools.
- Only documents real outputs from tool calls (anti-fabrication).

The pre-flight is re-injected on restart or agent switch.

If MCP tools (especially burp ones) are not ready when the pre-flight runs, use `Inject Payload` or restart the agent console.

---

## 8. Quick validation

1. In BurpIA's agent console, verify no path/command error and that the pre-flight runs without "Burp tools missing" complaints.
2. Run `grok mcp list` and `grok inspect --json` — confirm `burp` appears with the correct Java + jar command and `source: configToml`.
3. In the Grok TUI (if opened separately), press **Ctrl + L** or `/mcps` and verify `burp` is running.
4. In Burp, confirm the MCP Server extension is `Enabled` and listening on 9876.
5. From BurpIA, send a finding/flow to the agent. The agent should use `search_tool` (with queries like "burp", "send_http1_request", "get_scanner_issues") and then `use_tool` with names like `burp__send_http1_request`.

---

## 9. Troubleshooting

### Error: "The agent binary does not exist..."

- Fix `Binary Path` in `Settings > Agents`.
- Verify with `which grok` or direct `~/.grok/bin/grok --version`.
- Use the full path if the binary is not in PATH.

### Grok starts, but Burp MCP tools are missing / pre-flight fails

- Verify Burp MCP extension is `Enabled` and port 9876 is reachable (`curl -I http://127.0.0.1:9876` should return `text/event-stream`).
- Confirm `~/.grok/config.toml` has the correct `[mcp_servers.burp]` block and `enabled = true`.
- Run `grok mcp list` and `grok inspect --json` to see if "burp" is listed.
- In a running Grok TUI, open **Ctrl + L** / `/mcps`, start the `burp` server, then retry.
- Increase `MCP Wait (ms)` in BurpIA.
- Restart the agent console in BurpIA and force `Inject Payload`.

### Tools appear but calls fail or use wrong protocol

- The pre-flight mandates `burp__send_http1_request` + proper `\r\n` in the `content` parameter.
- Always `search_tool` first (e.g. query "send_http1_request" or "burp") to obtain the current schema before `use_tool`.
- Never call the HTTP/2 variants (`send_http2_request`, `create_repeater_tab_http2`) for primary traffic.

### Permission prompts blocking automation

- Edit `~/.grok/config.toml` and set:

```toml
[ui]
permission_mode = "always-approve"
```

- Reload Grok / restart the agent.

---

## 10. Official / useful references

- Grok user guide (local after install):
  - `~/.grok/docs/user-guide/07-mcp-servers.md`
  - Other files in `~/.grok/docs/user-guide/`
- xAI / Grok resources: https://x.ai/
- Burp MCP Server (PortSwigger):
  - https://github.com/PortSwigger/mcp-server
- Burp native installers and bundled JRE:
  - https://portswigger.net/burp/documentation/desktop/troubleshooting/launch-from-command-line

---

**Remember (Grok-specific):**  
Before any meaningful Burp work, the agent should execute (or you should manually trigger) the BURPAI CRITICAL PRE-FLIGHT CHECK to inventory tools, confirm the `burp` server (27 tools), and lock in the HTTP/1.1 + CRLF + `search_tool` before `use_tool` discipline. Only real tool outputs are allowed — no fabrication.
