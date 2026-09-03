# BurpIA

<p align="center">
  <img src="src/assets/logo.png" alt="BurpIA Logo" width="200"/>
</p>

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.7.0-green.svg)](VERSION.txt)
[![Burp Suite](https://img.shields.io/badge/works%20on-Community%20%26%20Pro-ff6633.svg)](https://portswigger.net/burp/communitydownload)
[![Author](https://img.shields.io/badge/author-DragonJAR-orange.svg)](https://www.DragonJAR.org)
[![Español](https://img.shields.io/badge/read%20in-Espa%C3%B1ol-blue.svg)](README.es.md)

> BurpIA turns Burp Suite into an AI-assisted web security testing environment. It analyzes real HTTP traffic with LLMs (12+ providers or local models), validates findings with autonomous CLI agents over Burp MCP, and keeps every result traceable — **works on Burp Suite Community and Professional**.

## 🎯 What BurpIA Does

This extension turns passive HTTP traffic into actionable security findings. It can:

- **Analyze traffic with LLMs:** automatic passive scanning or manual analysis via context menu, using real evidence (`request` + `response`).
- **Analyze request flows:** contextual analysis of 2–4 related requests as a single flow, ideal for state and logic vulnerabilities.
- **Validate with CLI agents:** send findings or flows to autonomous agents (Factory Droid, Claude Code, Antigravity, Open Code, Grok, Codex) integrated with Burp Suite MCP for deep manual validation.
- **Triage at speed:** send findings directly to **Burp Repeater** from the centralized results table, or save them to the Burp project Issues.
- **Manage findings intelligently:** severity/confidence prioritization, text and severity filters, CSV/JSON export.
- **Stay in control:** request deduplication (SHA-256, TTL 15 min, LRU 10k), configurable concurrency (1–10), static-resource filtering, bilingual UI (Spanish/English), native Dark/Light theme.

> **Important:** AI findings are hints, not verdicts. Always validate before reporting — false positives are possible.

## 📦 Installation

1. Download `BurpIA-1.7.0.jar` from the [releases](https://github.com/DragonJAR/BurpIA/releases) page.
2. In Burp Suite: `Extensions` tab → `Add` → select the JAR file.
3. Configure your provider in the BurpIA tab: **LLM Provider**, **API Key** (if applicable), **Model**, **Language**.
4. Use **Test Connection** to validate the endpoint before capturing traffic.

## ⚙️ Prerequisites

| Requirement | Detail |
|-------------|--------|
| Burp Suite | Community or Professional |
| Java | 17+ (bundled JRE in native Burp installers works) |
| LLM access | API key for a cloud provider, local Ollama, or LM Studio |
| CLI agent (optional) | One of: droid, claude, agy, opencode, grok, codex |

## 🚀 Quick Start (3 minutes)

1. Load the extension (see Installation).
2. Select your **LLM Provider** and enter the **API Key**.
3. Click **Test Connection** to validate endpoint and model.
4. Browse through Burp Proxy — findings appear in the BurpIA tab as traffic is analyzed.
5. Right-click any request → `Analyze request with BurpIA` (or `🤖 Analyze with {Agent}` for agent validation).

## 🔄 How It Works

### Passive flow

1. BurpIA intercepts an HTTP exchange.
2. It checks the **Scope**, applies filters, and **deduplicates**.
3. The task is queued in the analysis manager.
4. The prompt is built by injecting `request` and `response`.
5. The AI response is parsed and findings are normalized.
6. The results table, statistics, and (if enabled) Burp **Issues** are updated.

### Manual flow

1. Select one or more requests (2–4 for flow analysis) in any Burp tab.
2. Right-click:
   - **1 request:** `Analyze request with BurpIA` or `🤖 Analyze with {Agent}`.
   - **2–4 requests:** `🔍 Analyze this flow` or `🤖 Analyze this flow with {Agent}`.
3. The finding appears in the table to be edited, exported, or sent to Repeater.

## 🔌 Supported LLM Providers

| Provider | Notes |
|----------|-------|
| Ollama | Local models: Gemma 3, DeepSeek v3, Phi-4, Llama 3.3, etc. |
| Ollama Cloud | Cloud models at `ollama.com` — requires API key |
| OpenAI | o1, GPT-4o, etc. |
| Claude | Anthropic: Sonnet 3.5/3.6, Opus |
| Gemini | Google: 1.5 Pro/Flash (native support) |
| Moonshot (Kimi) | k2.5 and earlier |
| Z.ai / Minimax | GLM and MiniMax models |
| DeepSeek | v4-flash, v4-pro — OpenAI-compatible API |
| xAI Grok | grok-4.3, grok-4 — OpenAI-compatible API |
| Sakana Fugu | fugu, fugu-ultra |
| LM Studio | Local server, OpenAI-compatible |
| Custom | Up to 3 profiles for any OpenAI-compatible API |

## 🤖 CLI Agents

Autonomous validation agents integrated with Burp Suite MCP:

| Agent | Binary | Guide |
|-------|--------|-------|
| Factory Droid | `droid` | [EN](AGENT-DROID-EN.md) · [ES](AGENTE-DROID-ES.md) |
| Claude Code | `claude` | [EN](AGENT-CLAUDE-EN.md) · [ES](AGENTE-CLAUDE-ES.md) |
| Antigravity CLI | `agy` | [EN](AGENT-ANTIGRAVITY-EN.md) · [ES](AGENTE-ANTIGRAVITY-ES.md) |
| Open Code | `opencode` | [EN](AGENT-OPENCODE-EN.md) · [ES](AGENTE-OPENCODE-ES.md) |
| Grok CLI | `grok` | [EN](AGENT-GROK-EN.md) · [ES](AGENTE-GROK-ES.md) |
| Codex CLI | `codex` | [EN](AGENT-CODEX-EN.md) · [ES](AGENTE-CODEX-ES.md) |

## 🧠 Custom Prompt Tokens

- `{REQUEST}` / `{RESPONSE}`: normalized HTTP request/response.
- `{REQUEST_1}`…`{REQUEST_N}` / `{RESPONSE_1}`…`{RESPONSE_N}`: Nth element of a flow.
- `{OUTPUT_LANGUAGE}`: expected output language for finding descriptions.

If you omit these tokens, BurpIA automatically appends a security block (fallback) to keep minimum context and enforce the configured language.

## 📸 Screenshots

| Dashboard (ES) | Agent validation | Validated findings in Repeater |
|----------------|------------------|--------------------------------|
| ![BurpIA dashboard](src/assets/ES.png) | ![Agent console](src/assets/Agente.png) | ![Validated findings](src/assets/Fallos-Validados.png) |

## 🆕 What's New in v1.7.0

- **New LLM providers:** DeepSeek (v4-flash/v4-pro), xAI Grok (grok-4.3/4), Ollama Cloud, Sakana Fugu.
- **New CLI agent:** Grok CLI with binary auto-detection and controlled permissions.

## 💡 Best Practices

- Enable **Auto-save to Issues** only if you want direct persistence in the Burp project file.
- **Manually validate** every finding before reporting; AI can hallucinate.
- With cloud providers, review your data policy before sending traffic containing sensitive data.

## 📜 License

MIT — see [LICENSE](LICENSE).
