# CLAUDE AGENT (Claude Code) - BurpIA

Operational guide to:

1. Install `claude` if it is not installed.
2. Configure Burp Suite MCP for Claude Code.
3. Connect it correctly with BurpIA.

---

## 1. Requirements

- Burp Suite running.
- PortSwigger **MCP Server** extension installed in Burp (the `MCP` tab must be visible).
- BurpIA loaded in Burp Suite.
- Claude Code CLI (`claude`) installed.

---

## 2. Install Claude Code CLI

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

Install Claude Code from official Anthropic docs (method may vary by release).

Verify installation:

```bat
where claude
claude --version
```

---

## 3. First run and Claude authentication

Start Claude Code once and complete login:

```bash
claude
```

Then run:

```text
/login
```

If your environment uses API keys or enterprise auth, follow your organization's Anthropic policy.

---

## 4. Install Burp Suite official MCP (required)

1. In Burp Suite, open the extension store and install PortSwigger's official **MCP Server** extension.
2. Open the `MCP` tab in Burp and enable the server (`Enabled`).
3. Verify the SSE URL is available (this flow uses `http://127.0.0.1:9876`).
4. Keep Burp open while using Claude Code.

---

## 5. Configure Burp MCP in Claude Code

### Option A (recommended): add MCP server through Claude CLI

Run:

```bash
claude mcp add --scope user --transport sse burp http://127.0.0.1:9876
```

Then validate:

```bash
claude mcp list
```

Inside an active Claude session, you can also verify MCP availability with:

```text
/mcp
```

### Option B: configure MCP manually (advanced)

If your environment uses managed config, add a server named `burp` with SSE URL `http://127.0.0.1:9876` in Claude's MCP settings, then restart Claude Code and validate with `claude mcp list`.

---

## 5.1 Execution policy in Claude Code (recommended)

When Claude asks for permission to run actions/tools, choose the policy that matches your risk profile.

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

- BurpIA supports command + flags in this field (not only plain executable path).
- If your `claude` binary is elsewhere, use the full path.

---

## 7. Expected flow in BurpIA (important)

With the agent enabled, BurpIA automatically:

1. Executes the configured Claude command.
2. Waits for the user-configured `MCP Wait (ms)` time.
3. Injects the pre-flight initial prompt.

It also injects the initial prompt when:

- You restart the agent console.
- You switch agents.

If MCP tools are not loaded yet when the pre-flight prompt is injected, retry manually (without delay) using `Inject Payload`.

---

## 8. Quick validation checklist

1. In BurpIA's agent console, verify no binary path/command error appears.
2. In Claude, run `claude mcp list` and confirm `burp` appears.
3. In Burp, confirm MCP server remains enabled.
4. From BurpIA, send a finding to the agent and validate response quality.

---

## 9. Troubleshooting

### Error: "The agent binary does not exist at the current path..."

- Fix `Binary Path` in `Settings > Agents`.
- Verify executable with `which claude` (mac/Linux) or `where claude` (Windows).
- If command includes flags, ensure executable path is valid before the flags.

### Claude starts, but Burp MCP tools are missing

- Verify Burp MCP is `Enabled`.
- Verify SSE host/port in Burp.
- Re-run `claude mcp add --scope user --transport sse burp http://127.0.0.1:9876`.
- Restart Claude Code and check `claude mcp list` again.

### Burp MCP responds, but BurpIA agent flow does not execute

- Confirm BurpIA has `Agent enabled`.
- Increase `MCP Wait (ms)` to give MCPs more startup time.
- Use `Restart` and then `Inject Payload` to force pre-flight.

---

## 10. Official references

- Claude Code docs:
  - https://docs.anthropic.com/en/docs/claude-code/getting-started
  - https://docs.anthropic.com/en/docs/claude-code/mcp
- Burp MCP Server (PortSwigger):
  - https://github.com/PortSwigger/mcp-server
