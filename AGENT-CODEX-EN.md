# CODEX AGENT - BurpIA

Operational guide to:
1. Install `codex` if it is not installed.
2. Configure Burp Suite MCP for Codex.
3. Connect it correctly with BurpIA.

---

## 1. Requirements

- Burp Suite running.
- PortSwigger's official **MCP Server** extension installed in Burp.
- BurpIA loaded in Burp Suite.
- Codex CLI (`codex`) installed and authenticated.
- Node.js 18+ (for npm install).

---

## 2. Install Codex CLI

Codex CLI (from OpenAI) is installed via npm or the official installer. It self-manages into `~/.codex/`.

### macOS / Linux (common after install)

```bash
npm install -g @openai/codex
which codex
codex --version
```

If not in PATH, add `~/.local/bin` to your PATH or use the full path.

### Windows

The npm global install places the executable under the user profile. Check:

```bat
where codex
%USERPROFILE%\.local\bin\codex.exe --version
```

Alternative: download the pre-built binary from the [official releases](https://github.com/openai/codex/releases) and place it in your PATH.

Follow OpenAI's official installation instructions for your platform.

---

## 3. First run and Codex authentication

Start Codex once:

```bash
codex
```

Complete the login flow (it opens a browser for ChatGPT account login or uses an API key).

If your environment uses API keys, place them according to Codex's configuration (see `~/.codex/config.toml` or the `OPENAI_API_KEY` environment variable).

---

## 4. Install Burp Suite official MCP (required)

1. In Burp Suite, install PortSwigger's official **MCP Server** extension.
2. Open the `MCP` tab and enable the server (`Enabled`).
3. Verify that Burp MCP is listening on `http://127.0.0.1:9876`. That value is reused in the stdio proxy through `--sse-url`.
4. Extract or locate `mcp-proxy-all.jar` for Burp's `stdio` proxy.
5. Keep Burp open while using Codex.

Notes:

- On macOS the jar is often stored at `/Users/USER/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- On Linux it is commonly stored at `/home/USER/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- On Windows, if you extract it into the user profile, it can live under `%APPDATA%\BurpSuite\mcp-proxy\mcp-proxy-all.jar`.

---

## 5. Configure Burp MCP in Codex

Codex stores MCP servers in `~/.codex/config.toml` under `[mcp_servers.<name>]`.

### Option A (recommended for complex args): direct edit of `~/.codex/config.toml`

Append or create the section:

macOS (example with discovered paths):

```toml
[mcp_servers.burp]
command = "/opt/homebrew/opt/openjdk@21/bin/java"
args = ["-jar", "/Users/USER/.BurpSuite/mcp-proxy/mcp-proxy-all.jar", "--sse-url", "http://127.0.0.1:9876"]
```

Linux:

```toml
[mcp_servers.burp]
command = "/usr/bin/java"
args = ["-jar", "/home/USER/.BurpSuite/mcp-proxy/mcp-proxy-all.jar", "--sse-url", "http://127.0.0.1:9876"]
```

Windows:

```toml
[mcp_servers.burp]
command = "C:\\Users\\USER\\.local\\bin\\java.exe"
args = ["-jar", "C:\\Users\\USER\\AppData\\Roaming\\BurpSuite\\mcp-proxy\\mcp-proxy-all.jar", "--sse-url", "http://127.0.0.1:9876"]
```

> **Tip:** Use a system Java (`/opt/homebrew/opt/openjdk@21/bin/java` or `$JAVA_HOME/bin/java`) instead of Burp's embedded JRE. The embedded JRE inside the Burp Suite bundle may be killed by macOS (SIGKILL/exit 137) due to code-signing or quarantine restrictions.

### Option B: using the CLI helper

```bash
codex mcp add burp -- java -jar /Users/USER/.BurpSuite/mcp-proxy/mcp-proxy-all.jar --sse-url http://127.0.0.1:9876
```

Validate:

```bash
codex mcp list
```

Common adjustments:

- Replace `USER` with the real user name.
- If you use Burp Community, the folder can be `BurpSuiteCommunity` instead of `Burp Suite.app`.
- If you extracted the jar elsewhere, update the path inside `args`.
- After editing `config.toml`, restart Codex for the change to take effect.

---

## 5.2 Execution / Permission policy in Codex (recommended)

Codex controls tool execution permissions via `~/.codex/config.toml` and CLI flags:

```toml
approval_policy = "never"
sandbox_mode = "danger-full-access"
```

Or equivalently, pass `--yolo` on the command line (BurpIA does this by default in the configured binary path):

```bash
codex --yolo
```

`--yolo` sets `approval_policy = "never"` and `sandbox_mode = "danger-full-access"`, enabling fully autonomous operation without interactive prompts. Use with caution in production environments.

---

## 6. Configure BurpIA to use Codex

In BurpIA:

1. `Settings` -> `Agents` tab.
2. `Select Agent`: `CODEX_CLI` (visible name: "Codex CLI").
3. Enable `Enable Agent`.
4. Configure `Binary Path`:
   - macOS/Linux: `~/.local/bin/codex --yolo`
   - Windows: `%USERPROFILE%\.local\bin\codex.exe --yolo`
5. Adjust `MCP Wait (ms)` based on your machine (Codex + Java proxy can take a moment to spin up the Burp MCP bridge).
6. Save settings.

Notes:

- BurpIA supports command plus flags in this field.
- If `which codex` returns a different path, use the real full path.
- Codex picks up MCP configuration from `~/.codex/config.toml` on startup. Make sure the `burp` server is configured there.
- Permission behavior is controlled via `--yolo` flag (default) or `approval_policy`/`sandbox_mode` in `config.toml`.

---

## 7. Expected flow in BurpIA + Codex pre-flight

With the agent enabled, BurpIA:
1. Executes the configured `codex --yolo` command.
2. Waits for the configured `MCP Wait (ms)`.
3. Injects the pre-flight initial prompt.

**Codex-specific pre-flight (critical):**

Codex agents perform a strict "BURPAI CRITICAL PRE-FLIGHT CHECK" at the beginning of sessions that involve Burp:

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
2. Run `codex mcp list` — confirm `burp` appears with the correct Java + jar command.
3. In Burp, confirm the MCP Server extension is `Enabled` and listening on 9876.
4. From BurpIA, send a finding/flow to the agent. The agent should use `search_tool` (with queries like "burp", "send_http1_request", "get_scanner_issues") and then `use_tool` with names like `burp__send_http1_request`.

---

## 9. Troubleshooting

### Error: "The agent binary does not exist..."

- Fix `Binary Path` in `Settings > Agents`.
- Verify with `which codex` or direct `codex --version`.
- Use the full path if the binary is not in PATH.
- If Codex is not installed: `npm install -g @openai/codex`.

### Codex starts, but Burp MCP tools are missing / pre-flight fails

- Verify Burp MCP extension is `Enabled` and port 9876 is reachable (`curl -I http://127.0.0.1:9876` should return `text/event-stream`).
- Confirm `~/.codex/config.toml` has the correct `[mcp_servers.burp]` block.
- Run `codex mcp list` to see if "burp" is listed.
- Increase `MCP Wait (ms)` in BurpIA.
- Restart the agent console in BurpIA and force `Inject Payload`.

### Tools appear but calls fail or use wrong protocol

- The pre-flight mandates `burp__send_http1_request` + proper `\r\n` in the `content` parameter.
- Always `search_tool` first (e.g. query "send_http1_request" or "burp") to obtain the current schema before `use_tool`.
- Never call the HTTP/2 variants (`send_http2_request`, `create_repeater_tab_http2`) for primary traffic.

### Permission prompts blocking automation

- Ensure the binary path includes `--yolo`: `~/.local/bin/codex --yolo`.
- Or edit `~/.codex/config.toml` and set:

```toml
approval_policy = "never"
sandbox_mode = "danger-full-access"
```

- Restart the agent.

### MCP proxy fails with SIGKILL (exit 137) on macOS

The embedded JRE inside the Burp Suite bundle may be killed by macOS (SIGKILL/exit 137) due to code-signing or quarantine restrictions. If `codex mcp list` shows `Failed to connect` for `burp` and port 9876 is active, use a system Java instead of the embedded JRE:

```bash
# Verify that Burp's MCP port is active
curl -s -m 5 http://127.0.0.1:9876/ | head -3

# Edit ~/.codex/config.toml and change the command to system Java
# command = "/opt/homebrew/opt/openjdk@21/bin/java"
```

**Notes:**
- If you don't have Homebrew Java, install it with `brew install openjdk@21`.
- Alternatively, use the system `JAVA_HOME`: replace the command path with `$JAVA_HOME/bin/java`.

---

## 10. Official references

- Codex CLI repository: https://github.com/openai/codex
- Codex configuration reference: https://learn.chatgpt.com/docs/config-file/config-reference
- Codex config basics: https://learn.chatgpt.com/docs/config-file/config-basic
- Sandbox and approvals: https://learn.chatgpt.com/docs/agent-approvals-security
- Burp MCP Server (PortSwigger): https://github.com/PortSwigger/mcp-server
- Burp native installers and bundled JRE: https://portswigger.net/burp/documentation/desktop/troubleshooting/launch-from-command-line

---

**Remember (Codex-specific):**
Before any meaningful Burp work, the agent should execute (or you should manually trigger) the BURPAI CRITICAL PRE-FLIGHT CHECK to inventory tools, confirm the `burp` server (27 tools), and lock in the HTTP/1.1 + CRLF + `search_tool` before `use_tool` discipline. Only real tool outputs are allowed — no fabrication.
