package com.burpia.config;

import com.burpia.util.Normalizador;
import com.burpia.util.OSUtils;
import java.util.Arrays;

public enum AgenteTipo {
    FACTORY_DROID(
            "Factory Droid",
            "~/.local/bin/droid",
            "%USERPROFILE%\\bin\\droid.exe",
            "https://github.com/DragonJAR/BurpIA/blob/main/AGENTE-DROID-ES.md",
            "https://github.com/DragonJAR/BurpIA/blob/main/AGENT-DROID-EN.md"
    ),
    CLAUDE_CODE(
            "Claude Code",
            "~/.local/bin/claude --dangerously-skip-permissions",
            "%USERPROFILE%\\.local\\bin\\claude.exe --dangerously-skip-permissions",
            "https://github.com/DragonJAR/BurpIA/blob/main/AGENTE-CLAUDE-ES.md",
            "https://github.com/DragonJAR/BurpIA/blob/main/AGENT-CLAUDE-EN.md"
    ),
    // Antigravity CLI (Google). Sin flag CLI de permisos: la autonomía se
    // configura con la lista "allow" de ~/.gemini/antigravity-cli/settings.json
    // (motor fine-grained deny>ask>allow; p.ej. mcp(*) y command(*)).
    // Rutas oficiales del instalador: ~/.local/bin/agy (Unix) y
    // %LOCALAPPDATA%\agy\bin (Windows). TUI estándar: cae al submit sequence
    // por defecto como Grok/OpenCode.
    ANTIGRAVITY_CLI(
            "Antigravity CLI",
            "~/.local/bin/agy",
            "%USERPROFILE%\\AppData\\Local\\agy\\bin\\agy.exe",
            "https://github.com/DragonJAR/BurpIA/blob/main/AGENTE-ANTIGRAVITY-ES.md",
            "https://github.com/DragonJAR/BurpIA/blob/main/AGENT-ANTIGRAVITY-EN.md"
    ),
    OPEN_CODE(
            "Open Code",
            "~/.opencode/bin/opencode",
            "%USERPROFILE%\\.opencode\\bin\\opencode.exe",
            "https://github.com/DragonJAR/BurpIA/blob/main/AGENTE-OPENCODE-ES.md",
            "https://github.com/DragonJAR/BurpIA/blob/main/AGENT-OPENCODE-EN.md"
    ),
    // Grok CLI (xAI). Sin flag CLI de permisos — Grok controla permisos vía
    // ~/.grok/config.toml (permission_mode = "always-approve"). El submit sequence
    // por defecto (Droid: \r + fallbacks \n y \r\n) le aplica; si en uso real
    // requiere comportamiento distinto, agregar branch en SubmitSequenceFactory.
    GROK_BUILD(
            "Grok CLI",
            "~/.grok/bin/grok",
            "%USERPROFILE%\\.grok\\bin\\grok.exe",
            "https://github.com/DragonJAR/BurpIA/blob/main/AGENTE-GROK-ES.md",
            "https://github.com/DragonJAR/BurpIA/blob/main/AGENT-GROK-EN.md"
    ),
    // Codex CLI (OpenAI). --yolo equivale a approval_policy="never" +
    // sandbox_mode="danger-full-access" (autonomía total sin prompts).
    // Config en ~/.codex/config.toml. TUI estándar (Rust): cae al submit
    // sequence por defecto como Grok/Antigravity/OpenCode.
    CODEX_CLI(
            "Codex CLI",
            "~/.local/bin/codex --yolo",
            "%USERPROFILE%\\.local\\bin\\codex.exe --yolo",
            "https://github.com/DragonJAR/BurpIA/blob/main/AGENTE-CODEX-ES.md",
            "https://github.com/DragonJAR/BurpIA/blob/main/AGENT-CODEX-EN.md"
    ),
    // Agente personalizado. El usuario define su propio CLI en el campo de ruta
    // del diálogo de configuración. Cae al submit sequence por defecto (SMART_FALLBACK)
    // como cualquier TUI estándar. No tiene documentación ni ruta por defecto.
    CUSTOM_AGENT(
            "Custom Agent",
            "",
            "",
            "",
            ""
    );

    private final String nombreVisible;
    private final String rutaUnix;
    private final String rutaWindows;
    private final String urlDocES;
    private final String urlDocEN;

    AgenteTipo(String nombreVisible, String rutaUnix, String rutaWindows, String urlDocES, String urlDocEN) {
        this.nombreVisible = nombreVisible;
        this.rutaUnix = rutaUnix;
        this.rutaWindows = rutaWindows;
        this.urlDocES = urlDocES;
        this.urlDocEN = urlDocEN;
    }

    public String obtenerNombreVisible() {
        return nombreVisible;
    }

    public String obtenerRutaPorDefecto() {
        return OSUtils.esWindows() ? rutaWindows : rutaUnix;
    }

    // obtenerUrlDocES/EN removed (orphan): superseded by obtenerUrlDocPorIdioma
    // que es la API canónica. Los getters por idioma directo no tenían callers.

    public String obtenerUrlDocPorIdioma(String codigoIdioma) {
        return "en".equalsIgnoreCase(codigoIdioma) ? urlDocEN : urlDocES;
    }

    public static AgenteTipo porDefecto() {
        return FACTORY_DROID;
    }

    public static String[] codigosDisponibles() {
        return Arrays.stream(values())
            .map(AgenteTipo::name)
            .toArray(String[]::new);
    }

    public static AgenteTipo siguienteCircular(AgenteTipo actual) {
        AgenteTipo[] tipos = values();
        if (tipos.length == 0) {
            return porDefecto();
        }
        AgenteTipo base = actual != null ? actual : porDefecto();
        for (int i = 0; i < tipos.length; i++) {
            if (tipos[i] == base) {
                return tipos[(i + 1) % tipos.length];
            }
        }
        return tipos[0];
    }

    public static AgenteTipo desdeCodigo(String codigo, AgenteTipo porDefecto) {
        if (Normalizador.esVacio(codigo)) {
            return porDefecto;
        }
        try {
            return valueOf(codigo.trim());
        } catch (IllegalArgumentException e) {
            return porDefecto;
        }
    }

    public static String obtenerNombreVisible(String codigo, String porDefecto) {
        AgenteTipo tipo = desdeCodigo(codigo, null);
        return tipo != null ? tipo.obtenerNombreVisible() : porDefecto;
    }

    /**
     * Wrapper null-safe de {@link #obtenerNombreVisible(String, String)} que toma
     * el código operativo desde una configuración. Unifica el patrón que antes vivía
     * duplicado (con guards de null distintos) en FabricaMenuContextual y PanelHallazgos.
     *
     * @param config       Configuración de la que extraer el tipo de agente operativo; si es
     *                     null se devuelve el valor por defecto.
     * @param porDefecto   Nombre visible a retornar si no hay agente operativo válido.
     * @return Nombre visible del agente, o {@code porDefecto}.
     */
    public static String obtenerNombreVisible(ConfiguracionAPI config, String porDefecto) {
        if (config == null) {
            return porDefecto;
        }
        return obtenerNombreVisible(config.obtenerTipoAgenteOperativo(), porDefecto);
    }
}
