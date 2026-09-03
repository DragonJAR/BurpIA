# ANTIGRAVITY CLI AGENT - BurpIA

Operational guide to:

1. Install `agy` if it is not installed.
2. Configure Burp Suite MCP for Antigravity CLI.
3. Connect it correctly with BurpIA.

---

## 1. Requirements

- Burp Suite running.
- PortSwigger's official **MCP Server** extension installed in Burp.
- BurpIA loaded in Burp Suite.
- Antigravity CLI (`agy`) installed and authenticated.

---

## 2. Install Antigravity CLI

Official installer (from [antigravity.google/docs/cli/install](https://antigravity.google/docs/cli/install)):

### macOS / Linux

```bash
curl -fsSL https://antigravity.google/cli/install.sh | bash
```

The binary is installed at `~/.local/bin/agy`.

### Windows

**PowerShell:**

```powershell
irm https://antigravity.google/cli/install.ps1 | iex
```

The binary is registered at `C:\Users\<username>\AppData\Local\agy\bin\agy.exe`.

Verify installation:

```bash
which agy        # macOS / Linux
agy --version
```

```bat
where agy        # Windows
agy --version
```

---

## 3. First run and authentication

Start Antigravity CLI once:

```bash
agy
```

Authentication options (official docs):

- **Local sign-in:** `agy` attempts your OS keyring (Apple Keychain, Linux Secret Service, Windows Credential Manager). With a valid token profile it authenticates silently; otherwise it opens your browser for the Google OAuth flow.
- **Remote SSH:** the CLI detects the SSH environment, prints a secure authorization URL, and you complete the loop by pasting the code back into the terminal.
- **Gemini API key (headless/CI):** set `"modelProvider": "gemini"` in `~/.gemini/antigravity-cli/settings.json` and export `GEMINI_API_KEY`. Setting the env var alone has no effect.

---

## 4. Install Burp Suite official MCP (required)

1. In Burp Suite, install PortSwigger's official **MCP Server** extension.
2. Open the `MCP` tab and enable the server (`Enabled`).
3. Verify that Burp MCP is listening on `http://127.0.0.1:9876`. That value is reused in the stdio proxy through `--sse-url`.
4. Extract or locate `mcp-proxy-all.jar` for Burp's `stdio` proxy.
5. Keep Burp open while using Antigravity CLI.

Notes:

- On macOS the jar is often stored at `/Users/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- On Linux it is commonly stored at `/home/USUARIO/.BurpSuite/mcp-proxy/mcp-proxy-all.jar`.
- On Windows it can live under `%APPDATA%\BurpSuite\mcp-proxy\mcp-proxy-all.jar` if extracted into the user profile.

---

## 5. Configure Burp MCP in Antigravity CLI

Antigravity CLI reads its global settings from:

```
~/.gemini/antigravity-cli/settings.json
```

Add the `burp` MCP server entry (stdio proxy) to that file, adapting the paths to your installation:

```json
{
  "mcpServers": {
    "burp": {
      "command": "java",
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

Common adjustments:

- Replace `USUARIO` with the real user name.
- Point `command` at a full `java` executable path if `java` is not on your `PATH` (e.g. Burp's bundled JRE).
- With Burp Community the folder can be `BurpSuiteCommunity` instead of `BurpSuitePro`.
- If you extracted the jar elsewhere, update the path inside `args`.

Inside an active `agy` session, verify MCP availability with the session's MCP command before sending traffic.

---

## 6. Permissions policy in Antigravity CLI (recommended)

Antigravity CLI does **not** use a permission bypass flag. Autonomy is governed by a fine-grained permissions engine configured in the same `~/.gemini/antigravity-cli/settings.json`, with three access lists — `deny`, `ask`, `allow` — evaluated in strict precedence: **Deny > Ask > Allow**.

Relevant actions for BurpIA sessions:

| Action | Matches |
|--------|---------|
| `mcp(server/tool)` | MCP tools on a server (`mcp(burp/*)` matches all Burp MCP tools) |
| `command(prefix)` | Shell commands by token prefix |
| `read_file(path)` / `write_file(path)` | Filesystem access (`*` = everywhere) |

The default fallback for sensitive actions is **Ask** (read/write inside the workspace is auto-allowed). You can manage rules interactively with the CLI's Permissions command, or edit the lists directly. See the official permissions doc for exact syntax: <https://antigravity.google/docs/cli/permissions>.

For sensitive pentest sessions, keep the defaults (approval prompts on). For fully autonomous validation runs, grant at minimum `mcp(burp/*)`.

---

## 7. Configure BurpIA to use Antigravity CLI

In BurpIA:

1. `Settings` -> `Agents` tab.
2. `Select Agent`: `ANTIGRAVITY_CLI`.
3. Enable `Enable Agent`.
4. Configure `Binary Path`:
   - macOS / Linux: `~/.local/bin/agy`
   - Windows: `%USERPROFILE%\AppData\Local\agy\bin\agy.exe`
5. Adjust `MCP Wait (ms)` based on your machine.
6. Save settings.

Notes:

- BurpIA supports command plus flags in this field, but Antigravity needs no permission flag.
- If `which agy` or `where agy` returns a different path, use that real path.

---

## 8. Expected flow in BurpIA

With the agent enabled, BurpIA:

1. Executes the configured Antigravity command.
2. Waits for the configured `MCP Wait (ms)` value.
3. Injects the pre-flight initial prompt.

It also reinjects the initial prompt when:

- You restart the agent console.
- You switch agents.

If MCP tools are not ready when the pre-flight prompt is injected, retry manually with `Inject Payload`.

---

## 9. Quick validation

1. In BurpIA's agent console, verify no path or command error appears.
2. In Antigravity, confirm the `burp` MCP server is listed/available.
3. In Burp, confirm the MCP server remains enabled.
4. From BurpIA, send a finding or flow to the agent and review the response.

---

## 10. Troubleshooting

### Error: "The agent binary does not exist at the current path..."

- Fix `Binary Path` in `Settings > Agents`.
- Verify the executable with `which agy` or `where agy`.
- If the command includes flags, confirm the executable path is valid on its own first.

### Antigravity starts, but Burp MCP tools are missing

- Verify Burp MCP is `Enabled`.
- Verify the host and port used in `--sse-url`.
- Review `~/.gemini/antigravity-cli/settings.json` and confirm that `java` and `mcp-proxy-all.jar` exist at those paths.
- If every MCP tool call pauses with an approval prompt, add an `allow` rule for `mcp(burp/*)` (see section 6).

### Burp MCP responds, but BurpIA agent flow does not execute

- Confirm BurpIA has `Agent enabled`.
- Increase `MCP Wait (ms)` to give MCP more startup time.
- Use `Restart` and then `Inject Payload` to force pre-flight.

---

## 11. Official references

- Antigravity CLI install and auth: <https://antigravity.google/docs/cli/install>
- Antigravity CLI permissions: <https://antigravity.google/docs/cli/permissions>
- Antigravity CLI headless mode: <https://antigravity.google/docs/cli/headless>
- Burp MCP Server (PortSwigger): <https://github.com/PortSwigger/mcp-server>
- Burp native installers and bundled JRE: <https://portswigger.net/burp/documentation/desktop/troubleshooting/launch-from-command-line>
