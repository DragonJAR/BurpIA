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

    public String getNombreVisible() {
        return nombreVisible;
    }

    public String getRutaPorDefecto() {
        return OSUtils.esWindows() ? rutaWindows : rutaUnix;
    }

    public String getUrlDocES() {
        return urlDocES;
    }

    public String getUrlDocEN() {
        return urlDocEN;
    }

    public String getUrlDocPorIdioma(String codigoIdioma) {
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
        return tipo != null ? tipo.getNombreVisible() : porDefecto;
    }
}
