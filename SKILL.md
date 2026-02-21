---
name: burpia-maintainer
description: Standard workflow for safely evolving BurpIA (Burp extension + AI providers) with DRY code, reliable tests, and synchronized docs.
---

# BurpIA Maintainer Skill

## Objective
Deliver production-grade BurpIA changes without regressions, keeping code, tests, and docs aligned under DRY.

## When To Use
- You modify provider integrations (`OpenAI`, `Claude`, `Gemini`, `Z.ai`, `minimax`, `Ollama`).
- You touch UI behavior in `DialogoConfiguracion`, `PanelHallazgos`, `PanelTareas`, or `PestaniaPrincipal`.
- You change parsing, prompting, task flow, exports, or Burp tool integration.
- You need to audit and clean dead code, obsolete paths, and duplicated logic.

## When Not To Use
- Work is unrelated to this repository.
- The task is purely editorial outside BurpIA runtime behavior.

## Inputs
- Repository root path.
- Goal or defect description.
- Affected module(s): `config`, `analyzer`, `ui`, `util`, `model`, `docs`.
- Expected behavior (what must happen after the change).

If some input is missing, proceed with conservative defaults and state assumptions in the result.

## Progressive Context Load
Read only what is necessary, in this order:
1. `README.md`
2. `docs/PLUGINS-BURP.md`
3. `src/main/java/com/burpia/config/ProveedorAI.java`
4. `src/main/java/com/burpia/config/ConfiguracionAPI.java`
5. `src/main/java/com/burpia/util/ProbadorConexionAI.java`
6. `src/main/java/com/burpia/analyzer/AnalizadorAI.java`
7. `src/main/java/com/burpia/ui/DialogoConfiguracion.java`
8. Additional touched files and their corresponding tests under `src/test/java/com/burpia/`

## Workflow
1. Baseline
- Run compile/tests before edits.
2. Diagnose
- Reproduce and isolate root cause per module.
3. Implement
- Apply minimal DRY fix.
- Centralize provider routing and avoid duplicated branching.
- Prefer existing utilities over new helpers.
4. Clean
- Remove dead code, obsolete wrappers, and stale comments.
5. Validate
- Run targeted tests for touched modules.
- Run full test suite.
6. Sync Docs
- Update docs/contracts whenever behavior changes.

## Provider Contract Rules
- Endpoint resolution must go through `ConfiguracionAPI.construirUrlApiProveedor(...)`.
- Connection test must be isolated per provider.
- Auth headers by provider:
  - OpenAI/Z.ai/minimax: `Authorization: Bearer <key>`
  - Claude: `x-api-key`, `anthropic-version`
  - Gemini: `x-goog-api-key`
- Ollama: no API key
- UI result must display the actual endpoint tested.
- Current default models:
  - OpenAI: `gpt-5.2-pro`
  - Claude: `claude-sonnet-4-6`
  - Gemini: `gemini-3-deep-think-preview`
  - Z.ai: `glm-5`
  - Minimax: `minimax-m2.5`
  - Ollama: `gemma3:12b`

## UI Quality Rules
- Ignored findings must render clearly without raw HTML artifacts.
- Context menu actions must map to the correct Burp tools (Repeater/Intruder).
- Export logic must exclude ignored findings.
- Long text panels must remain usable at smaller sizes (scroll-safe layout).
- Statistics panel must adapt layout:
  - Horizontal (2-column) for windows ≥900px width
  - Vertical (stacked) for narrower windows
- Buttons must reposition dynamically based on layout mode.

## Mandatory Test Gates
- Endpoint builder tests for all providers ✅
- Connection tester tests for all providers and parsing variants ✅
- Parser tests for provider-specific response shapes ✅
- Task manager state transition tests ✅
- UI context menu callback tests ✅
- Renderer test for ignored findings display ✅
- Table model tests ✅
- Deduplicator tests ✅
- Console manager tests ✅
- Model validation tests (Tarea, Hallazgo) ✅

All tests pass: `./gradlew test`

## Build Contract
- Preferred commands:
  - `./gradlew --no-daemon test`
  - `./build-jar.sh --no-test`
- Required artifact:
  - `/Users/jaimearestrepo/Proyectos/BurpIA/build/libs/BurpIA-1.0.0.jar`

## Output Contract
Always report:
1. Root cause(s).
2. Files changed.
3. Tests executed and results.
4. Documentation updates.
5. Residual risks or follow-up actions.
