# BurpIA

BurpIA is a Burp Suite extension that analyzes HTTP traffic with LLMs to help you detect potential security findings faster.

Current version: `1.0.0`

Version en español: [README.md](README.md)

## What You Get With BurpIA

- Passive and manual analysis over real HTTP evidence (`request` + `response`).
- Finding prioritization by severity and confidence.
- Fast triage flow: send findings to Repeater, Intruder, and Scanner from the table.
- Load control with task queue, deduplication, and concurrency limits.
- Findings export to CSV and JSON.
- User settings persistence across plugin restarts.
- Bilingual UI (Spanish/English), including tooltips and logs.

## Current Status (v1.0.0)

- Passive capture based on `HttpResponseReceived` (Montoya API).
- Manual analysis from context menu: `Analyze request with BurpIA`.
- Strict `Target Scope` validation before analysis.
- Static resource filtering to reduce noise.
- SHA-256 deduplication with LRU cache and TTL expiration.
- Task management: pause, resume, cancel, retry, and clear.
- Optional auto-save to `Site Map > Issues`.
- Manual send of one or multiple findings to Issues when auto-save is disabled.

## Quick Start (3 minutes)

1. Download the file `BurpIA-1.0.0.jar`

2. Load the extension in Burp:

- `Extensions` -> `Add`
- Select `BurpIA-1.0.0.jar`

3. Configure BurpIA in the plugin tab:

- LLM provider
- API key (if required)
- Model
- UI language
- Custom prompt

4. Use `Test Connection` to validate endpoint/model before capturing traffic.

## Supported LLM Providers

- Ollama
- OpenAI
- Claude
- Gemini
- Z.ai
- Minimax
- Custom (OpenAI-compatible API format)

If you plan to use Z.ai or Minimax, here are discount purchase options:

- Z.ai: [buy with discount](https://z.ai/subscribe?ic=FXSFEPRECU)
- Minimax: [buy with discount](https://platform.minimax.io/subscribe/coding-plan?code=GdktCUVh7E&source=link)

## How It Works

### Passive Flow

1. BurpIA receives an HTTP response.
2. It validates scope, filters, and deduplication.
3. It enqueues the analysis task.
4. It builds the prompt with request/response evidence.
5. It parses model output and normalizes findings.
6. It updates table, stats, and (if enabled) saves to Issues.

### Manual Flow

1. You select a request in Burp.
2. You run `Analyze request with BurpIA`.
3. BurpIA analyzes the request and, if available, its associated response.
4. The finding appears in the table for export, editing, or forwarding to Burp tools.

## Custom Prompt

BurpIA supports these tokens:

- `{REQUEST}`: inserts normalized HTTP request.
- `{RESPONSE}`: inserts HTTP response (if available).
- `{OUTPUT_LANGUAGE}`: expected output language for `descripcion`.

If any token is missing, BurpIA automatically appends a fallback block to keep minimum context and enforce output language consistency with user settings.

## Requirements

- Java 17+
- Burp Suite Community or Professional
- Connectivity to configured AI provider (local or remote)

## Burp Edition Compatibility

- BurpIA works in Community and Professional.
- Some integrations depend on APIs available in your edition (for example, Scanner).

## Best Practices

- Enable `Auto-save to Issues` if you want direct persistence in the Burp project.
- Manually validate each finding before reporting it.
- If you use remote providers, review your data policy before sending sensitive traffic.

## Limitations

- It can generate false positives or incomplete findings; human validation is always required.
- If manual analysis has no associated response, only the request is analyzed.