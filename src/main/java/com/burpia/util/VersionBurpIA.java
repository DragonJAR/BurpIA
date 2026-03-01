package com.burpia.util;

import java.util.Locale;

public final class VersionBurpIA {
    public static final String VERSION_ACTUAL = "1.0.2";
    public static final String URL_VERSION_REMOTA =
        "https://raw.githubusercontent.com/DragonJAR/BurpIA/refs/heads/main/VERSION.txt";
    public static final String URL_DESCARGA =
        "https://github.com/dragonJAR/burpIA/";

    private VersionBurpIA() {
    }

    public static String obtenerVersionActual() {
        return VERSION_ACTUAL;
    }

    public static String normalizarVersion(String version) {
        if (version == null) {
            return "";
        }
        String limpia = version.trim();
        if (limpia.isEmpty()) {
            return "";
        }

        int salto = limpia.indexOf('\n');
        if (salto >= 0) {
            limpia = limpia.substring(0, salto).trim();
        }

        if (!limpia.isEmpty() && limpia.charAt(0) == '\uFEFF') {
            limpia = limpia.substring(1).trim();
        }

        if (limpia.toLowerCase(Locale.ROOT).startsWith("v")) {
            limpia = limpia.substring(1).trim();
        }
        return limpia;
    }

    public static boolean sonVersionesDiferentes(String versionLocal, String versionRemota) {
        String local = normalizarVersion(versionLocal);
        String remota = normalizarVersion(versionRemota);
        return !local.equalsIgnoreCase(remota);
    }
}
