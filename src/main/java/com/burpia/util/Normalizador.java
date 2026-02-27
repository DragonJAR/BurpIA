package com.burpia.util;

import java.util.Locale;

public final class Normalizador {

    public static final Locale LOCALE_RAIZ = Locale.ROOT;

    private Normalizador() {
    }

    public static String normalizarTexto(String valor) {
        return desescaparSecuenciasBasicas(valor, false).trim();
    }

    public static String normalizarTextoConControlesEnEspacio(String valor) {
        return desescaparSecuenciasBasicas(valor, true).trim();
    }

    public static String normalizarTexto(String valor, String porDefecto) {
        String normalizado = normalizarTexto(valor);
        if (normalizado.isEmpty()) {
            return porDefecto != null ? porDefecto : "";
        }
        return normalizado;
    }

    public static String normalizarParaLog(String valor, String porDefecto) {
        if (valor == null) {
            return porDefecto != null ? porDefecto : "[NULL]";
        }
        String limpio = valor.trim();
        if (limpio.isEmpty()) {
            return porDefecto != null ? porDefecto : "[VACIO]";
        }
        return limpio;
    }

    public static String aMinusculas(String valor) {
        return valor != null ? valor.toLowerCase(LOCALE_RAIZ) : "";
    }

    public static String aMayusculas(String valor) {
        return valor != null ? valor.toUpperCase(LOCALE_RAIZ) : "";
    }

    public static boolean esVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    public static boolean noEsVacio(String valor) {
        return !esVacio(valor);
    }

    public static String truncarParaVisualizacion(String texto, int longitudMaxima) {
        if (texto == null) {
            return "";
        }
        if (longitudMaxima <= 0) {
            return "";
        }
        if (texto.length() <= longitudMaxima) {
            return texto;
        }

        int longitudReal = Math.max(3, longitudMaxima - 3);

        int corte = longitudReal;
        for (int i = longitudReal - 1; i >= Math.max(0, longitudReal - 20); i--) {
            char c = texto.charAt(i);
            if (c == ' ' || c == '/' || c == '?' || c == '&' || c == '=') {
                corte = i;
                break;
            }
        }

        return texto.substring(0, corte) + "...";
    }

    public static String truncarUrl(String url, int longitudMaxima) {
        if (url == null) {
            return "";
        }
        int maximo = longitudMaxima <= 0 ? 50 : longitudMaxima;
        if (url.length() <= maximo) {
            return url;
        }

        String esquemaDominio = "";
        int inicioPath = 0;

        int idxProtocolo = url.indexOf("://");
        if (idxProtocolo > 0) {
            int idxPath = url.indexOf('/', idxProtocolo + 3);
            if (idxPath > 0) {
                esquemaDominio = url.substring(0, idxPath);
                inicioPath = idxPath;
            } else {
                return url.length() <= maximo ? url : url.substring(0, maximo - 3) + "...";
            }
        }

        if (esquemaDominio.length() >= maximo - 10) {
            return truncarParaVisualizacion(url, maximo);
        }

        int espacioPath = maximo - esquemaDominio.length() - 3;

        if (espacioPath <= 0) {
            return esquemaDominio + "...";
        }

        String pathCompleto = url.substring(inicioPath);
        if (pathCompleto.length() <= espacioPath) {
            return url;
        }

        int mostrarInicio = Math.max(1, espacioPath / 2);
        int mostrarFinal = espacioPath - mostrarInicio;

        String inicio = pathCompleto.substring(0, mostrarInicio);
        String fin = pathCompleto.substring(pathCompleto.length() - mostrarFinal);

        return esquemaDominio + inicio + "..." + fin;
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

    public static String formatearResultadoAccion(String resumen, int exitosos, int total,
                                                   String detalle, int maxLineas) {
        StringBuilder sb = new StringBuilder();
        sb.append(resumen).append(": ").append(exitosos).append("/").append(total).append("\n\n");
        
        if (detalle != null && !detalle.isEmpty()) {
            String[] lineas = detalle.split("\n");
            int lineasMostradas = 0;
            int lineasOmitidas = 0;
            
            for (String linea : lineas) {
                if (lineasMostradas >= maxLineas) {
                    lineasOmitidas++;
                    continue;
                }

                String lineaProcesada = truncarLineaConUrl(linea, 80);
                sb.append(lineaProcesada).append("\n");
                lineasMostradas++;
            }
            
            if (lineasOmitidas > 0) {
                sb.append("\n... y ").append(lineasOmitidas).append(" líneas más");
            }
        }
        
        return sb.toString();
    }

    private static String truncarLineaConUrl(String linea, int longitudMaxima) {
        if (linea == null || linea.length() <= longitudMaxima) {
            return linea;
        }

        String prefijo = "";
        String url = linea;

        if (linea.startsWith("✅ ") || linea.startsWith("❌ ")) {
            prefijo = linea.substring(0, 2) + " ";
            url = linea.substring(2).trim();
        }

        if (url.length() > longitudMaxima - prefijo.length()) {
            return prefijo + truncarUrl(url, longitudMaxima - prefijo.length());
        }

        return linea;
    }

    public static String truncar(String valor, int maxLongitud) {
        if (valor == null) {
            return "";
        }
        if (valor.length() <= maxLongitud) {
            return valor;
        }
        return valor.substring(0, maxLongitud) + "...";
    }

    public static String truncarConIndicador(String valor, int maxLongitud, String etiqueta) {
        if (valor == null) {
            return "";
        }
        if (valor.length() <= maxLongitud) {
            return valor;
        }
        int truncados = valor.length() - maxLongitud;
        String etiquetaStr = noEsVacio(etiqueta) ? etiqueta + ": " : "";
        return valor.substring(0, maxLongitud) + 
               "\n[TRUNCADO " + etiquetaStr + "+" + truncados + " caracteres]";
    }

    public static String sanitizarParaJson(String valor) {
        if (valor == null) {
            return "";
        }
        int len = valor.length();
        StringBuilder sb = new StringBuilder(len + (len / 10));
        for (int i = 0; i < len; i++) {
            char c = valor.charAt(i);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '"') {
                sb.append("\\\"");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String sanitizarParaLog(String valor) {
        if (valor == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(valor.length());
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            if (c >= 32 || c == '\n' || c == '\r' || c == '\t') {
                sb.append(c);
            } else {
                sb.append(String.format("\\x%02x", (int) c));
            }
        }
        return sb.toString();
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

    public static String primeroNoVacio(String... valores) {
        if (valores == null) {
            return "";
        }
        for (String valor : valores) {
            if (noEsVacio(valor)) {
                return valor;
            }
        }
        return "";
    }

    public static String valorOalternativo(String valor, String alternativo) {
        return noEsVacio(valor) ? valor : (alternativo != null ? alternativo : "");
    }

    public static int normalizarRango(int valor, int min, int max) {
        return Math.max(min, Math.min(max, valor));
    }

    public static long normalizarRango(long valor, long min, long max) {
        return Math.max(min, Math.min(max, valor));
    }
}
