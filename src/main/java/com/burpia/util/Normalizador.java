package com.burpia.util;

public final class Normalizador {

    private Normalizador() {
    }

    public static String normalizarTexto(String valor) {
        return desescaparSecuenciasBasicas(valor, false).trim();
    }

    public static String normalizarTextoConControlesEnEspacio(String valor) {
        return desescaparSecuenciasBasicas(valor, true).trim();
    }

    public static boolean esVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    public static boolean noEsVacio(String valor) {
        return !esVacio(valor);
    }

    public static String sanitizarApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "[NO CONFIGURADA]";
        }
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    private static String desescaparSecuenciasBasicas(String valor, boolean controlesComoEspacio) {
        if (valor == null) {
            return "";
        }
        int len = valor.length();
        StringBuilder sb = new StringBuilder(len);
        int indice = 0;
        while (indice < len) {
            char actual = valor.charAt(indice);
            if (actual == '\\' && indice + 1 < len) {
                char siguiente = valor.charAt(indice + 1);
                if (siguiente == 'n' || siguiente == 'r' || siguiente == 't') {
                    if (controlesComoEspacio) {
                        sb.append(' ');
                    } else if (siguiente == 'n') {
                        sb.append('\n');
                    } else if (siguiente == 'r') {
                        sb.append('\r');
                    } else {
                        sb.append('\t');
                    }
                    indice += 2;
                    continue;
                }
                if (siguiente == '\"') {
                    sb.append('\"');
                    indice += 2;
                    continue;
                }
            }
            sb.append(actual);
            indice++;
        }
        return sb.toString();
    }
}
