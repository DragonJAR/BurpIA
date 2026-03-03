# GEMINI CLI AGENT - BurpIA

Operational guide to:

1. Install `gemini` if it is not installed.
2. Configure Burp Suite MCP for Gemini CLI.
3. Connect it correctly with BurpIA.

---

## 1. Requirements

- Burp Suite running.
- PortSwigger **MCP Server** extension installed in Burp (the `MCP` tab must be visible).
- BurpIA loaded in Burp Suite.
- Gemini CLI (`gemini`) installed.

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

Install Gemini CLI from official Google docs (method may vary by release).

Verify installation:

```bat
where gemini
gemini --version
```

---

## 3. First run and Gemini authentication

Start Gemini CLI once and complete login:

```bash
gemini
```

Follow Google Cloud authentication instructions.

If your environment uses API keys or enterprise auth, follow your organization's Google policy.

---

## 4. Install Burp Suite official MCP (required)

1. In Burp Suite, open the extension store and install PortSwigger's official **MCP Server** extension.
2. Open the `MCP` tab in Burp and enable the server (`Enabled`).
3. Verify the SSE URL is available (this flow uses `http://127.0.0.1:9876`).
4. Keep Burp open while using Gemini CLI.

---

## 5. Configure Burp MCP in Gemini CLI

### Option A (recommended): add MCP server through Gemini CLI

Run:

```bash
gemini mcp add --scope user --transport sse burp http://127.0.0.1:9876
```

Then validate:

```bash
gemini mcp list
```

Inside an active Gemini session, you can also verify MCP availability with:

```text
/mcp
```

### Option B: configure MCP manually (advanced)

If your environment uses managed config, add a server named `burp` with SSE URL `http://127.0.0.1:9876` in Gemini's MCP settings, then restart Gemini CLI and validate with `gemini mcp list`.

---

## 5.1 Execution policy in Gemini CLI (recommended)

When Gemini asks for permission to run actions/tools, choose the policy that matches your risk profile.

For sensitive pentest sessions, keep approval prompts enabled.

---

## 6. Configure BurpIA to use Gemini CLI

In BurpIA:

1. `Settings` -> `Agents` tab.
2. `Select Agent`: `GEMINI_CLI`.
3. Enable `Enable Agent`.
4. Configure `Binary Path`:
   - macOS/Linux: `~/.local/bin/gemini --yolo`
   - Windows: `%USERPROFILE%\\bin\\gemini.exe --yolo`
5. Adjust `MCP Wait (ms)` based on your machine.
6. Save settings.

Notes:

- BurpIA supports command + flags in this field (not only plain executable path).
- If your `gemini` binary is elsewhere, use the full path.
- The `--yolo` flag is similar to Claude's `--dangerously-skip-permissions`, for automated execution.

---

## 7. Expected flow in BurpIA (important)

With the agent enabled, BurpIA automatically:

1. Executes the configured Gemini command.
2. Waits for the user-configured `MCP Wait (ms)` time.
3. Injects the pre-flight initial prompt.

It also injects the initial prompt when:

- You restart the agent console.
- You switch agents.

If MCP tools are not loaded yet when the pre-flight prompt is injected, retry manually (without delay) using `Inject Payload`.

---

## 8. Quick validation checklist

1. In BurpIA's agent console, verify no binary path/command error appears.
2. In Gemini, run `gemini mcp list` and confirm `burp` appears.
3. In Burp, confirm MCP server remains enabled.
4. From BurpIA, send a finding to the agent and validate response quality.

---

## 9. Troubleshooting

### Error: "The agent binary does not exist at the current path..."

- Fix `Binary Path` in `Settings > Agents`.
- Verify executable with `which gemini` (mac/Linux) or `where gemini` (Windows).
- If command includes flags, ensure executable path is valid before the flags.

### Gemini starts, but Burp MCP tools are missing

- Verify Burp MCP is `Enabled`.
- Verify SSE host/port in Burp.
- Re-run `gemini mcp add --scope user --transport sse burp http://127.0.0.1:9876`.
- Restart Gemini CLI and check `gemini mcp list` again.

### Burp MCP responds, but BurpIA agent flow does not execute

- Confirm BurpIA has `Agent enabled`.
- Increase `MCP Wait (ms)` to give MCPs more startup time.
- Use `Restart` and then `Inject Payload` to force pre-flight.

---

## 10. Official references

- Gemini CLI docs:
  - https://github.com/google/gemini-cli
- Burp MCP Server (PortSwigger):
  - https://github.com/PortSwigger/mcp-server
