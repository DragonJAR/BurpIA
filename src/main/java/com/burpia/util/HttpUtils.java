package com.burpia.util;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class HttpUtils {
    private static final Logger LOGGER = Logger.getLogger(HttpUtils.class.getName());

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    public static final Set<String> EXTENSIONES_ESTATICAS;

    static {
        Set<String> extensions = new HashSet<>();
        extensions.add(".js");
        extensions.add(".css");
        extensions.add(".png");
        extensions.add(".jpg");
        extensions.add(".jpeg");
        extensions.add(".gif");
        extensions.add(".svg");
        extensions.add(".ico");
        extensions.add(".woff");
        extensions.add(".woff2");
        extensions.add(".webp");
        extensions.add(".ttf");
        extensions.add(".eot");
        EXTENSIONES_ESTATICAS = Collections.unmodifiableSet(extensions);
    }

    private HttpUtils() {

    }

    private static final ThreadLocal<MessageDigest> SHA256_LOCAL = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 no disponible", e);
        }
    });

    private static MessageDigest obtenerSha256() {
        MessageDigest md = SHA256_LOCAL.get();
        md.reset();
        return md;
    }

    public static String generarHash(byte[] datos) {
        MessageDigest md = obtenerSha256();
        if (datos != null && datos.length > 0) {
            md.update(datos);
        }
        return convertirDigestHex(md.digest());
    }

    public static String generarHashPartes(String... partes) {
        MessageDigest md = obtenerSha256();
        if (partes == null || partes.length == 0) {
            return convertirDigestHex(md.digest());
        }
        for (String parte : partes) {
            if (parte != null && !parte.isEmpty()) {
                md.update(parte.getBytes(StandardCharsets.UTF_8));
            }
            md.update((byte) 0); 
        }
        return convertirDigestHex(md.digest());
    }

    public static String convertirDigestHex(byte[] hash) {
        if (hash == null) {
            return "";
        }
        char[] hexChars = new char[hash.length * 2];
        for (int j = 0; j < hash.length; j++) {
            int v = hash[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String extraerEncabezados(HttpRequest solicitud) {
        if (solicitud == null) {
            return "[SOLICITUD NULL]";
        }

        StringBuilder encabezados = new StringBuilder(512);
        String metodo = solicitud.method();
        String url = solicitud.url();

        encabezados.append(metodo != null ? metodo : "[METHOD NULL]")
                   .append(" ")
                   .append(url != null ? url : "[URL NULL]")
                   .append("\n");

        if (solicitud.headers() != null) {
            solicitud.headers().forEach(encabezado -> {
                if (encabezado != null) {
                    encabezados.append(encabezado.name() != null ? encabezado.name() : "[NAME NULL]")
                               .append(": ")
                               .append(encabezado.value() != null ? encabezado.value() : "[VALUE NULL]")
                               .append("\n");
                }
            });
        } else {
            encabezados.append("[HEADERS NULL]\n");
        }

        return encabezados.toString();
    }

    public static String generarHashRapido(HttpRequest solicitud, HttpResponse respuesta) {
        if (solicitud == null || respuesta == null) {
            return "";
        }
        
        MessageDigest md = obtenerSha256();
        
        actualizarDigest(md, solicitud.method());
        actualizarDigest(md, solicitud.url());
        actualizarDigest(md, respuesta.statusCode());

        long longitudSolicitud = obtenerLongitudCuerpoSeguro(solicitud);
        if (longitudSolicitud >= 0L) {
            actualizarDigest(md, longitudSolicitud);
        }

        long longitudRespuesta = obtenerLongitudCuerpoSeguro(respuesta);
        if (longitudRespuesta >= 0L) {
            actualizarDigest(md, longitudRespuesta);
        }

        return convertirDigestHex(md.digest());
    }

    private static long obtenerLongitudCuerpoSeguro(HttpRequest solicitud) {
        try {
            return solicitud.body() != null ? solicitud.body().length() : -1L;
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "No se pudo obtener longitud de body request para hash rápido", e);
            }
            return -1L;
        }
    }

    private static long obtenerLongitudCuerpoSeguro(HttpResponse respuesta) {
        try {
            return respuesta.body() != null ? respuesta.body().length() : -1L;
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "No se pudo obtener longitud de body response para hash rápido", e);
            }
            return -1L;
        }
    }

    private static void actualizarDigest(MessageDigest md, String valor) {
        if (valor != null && !valor.isEmpty()) {
            md.update(valor.getBytes(StandardCharsets.UTF_8));
        }
        md.update((byte) 0);
    }

    private static void actualizarDigest(MessageDigest md, long valor) {
        md.update(String.valueOf(valor).getBytes(StandardCharsets.UTF_8));
        md.update((byte) 0);
    }

    public static String extraerEncabezados(HttpResponse respuesta) {
        if (respuesta == null) {
            return "[RESPUESTA NULL]";
        }

        StringBuilder encabezados = new StringBuilder(512);
        String version = respuesta.httpVersion() != null ? respuesta.httpVersion() : "HTTP/1.1";
        encabezados.append(version)
                   .append(" ")
                   .append(respuesta.statusCode());

        String reason = respuesta.reasonPhrase();
        if (reason != null && !reason.trim().isEmpty()) {
            encabezados.append(" ").append(reason.trim());
        }
        encabezados.append("\n");

        if (respuesta.headers() != null) {
            respuesta.headers().forEach(encabezado -> {
                if (encabezado != null) {
                    encabezados.append(encabezado.name() != null ? encabezado.name() : "[NAME NULL]")
                               .append(": ")
                               .append(encabezado.value() != null ? encabezado.value() : "[VALUE NULL]")
                               .append("\n");
                }
            });
        } else {
            encabezados.append("[HEADERS NULL]\n");
        }

        return encabezados.toString();
    }

    public static String extraerCuerpo(HttpRequest solicitud) {
        return extraerCuerpo(solicitud, Integer.MAX_VALUE);
    }

    public static String extraerCuerpo(HttpRequest solicitud, int maxCaracteres) {
        if (solicitud == null) {
            return "";
        }
        try {
            return extraerCuerpoDesdeByteArray(solicitud.body(), maxCaracteres);
        } catch (Exception ignored) {
            return "";
        }
    }

    public static String extraerCuerpo(HttpResponse respuesta) {
        return extraerCuerpo(respuesta, Integer.MAX_VALUE);
    }

    public static String extraerCuerpo(HttpResponse respuesta, int maxCaracteres) {
        if (respuesta == null) {
            return "";
        }
        try {
            return extraerCuerpoDesdeByteArray(respuesta.body(), maxCaracteres);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String extraerCuerpoDesdeByteArray(ByteArray body, int maxCaracteres) {
        if (body == null || body.length() <= 0) {
            return "";
        }
        if (maxCaracteres <= 0) {
            return "";
        }
        try {
            if (maxCaracteres == Integer.MAX_VALUE) {
                String textoCompleto = body.toString();
                return textoCompleto != null ? textoCompleto : "";
            }

            int maxBytes = Math.min(body.length(), Math.max(64, maxCaracteres * 4));
            ByteArray parcial = body.subArray(0, maxBytes);
            String textoParcial = parcial != null ? parcial.toString() : "";
            return truncarSiSupera(textoParcial != null ? textoParcial : "", maxCaracteres);
        } catch (Exception e) {
            try {
                String fallback = body.toString();
                return truncarSiSupera(fallback != null ? fallback : "", maxCaracteres);
            } catch (Exception ignored) {
                return "";
            }
        }
    }

    private static String truncarSiSupera(String texto, int maxCaracteres) {
        if (texto == null) {
            return "";
        }
        if (maxCaracteres <= 0 || texto.length() <= maxCaracteres) {
            return texto;
        }
        return texto.substring(0, maxCaracteres);
    }

    public static boolean esRecursoEstatico(String url) {
        return esRecursoEstatico(url, EXTENSIONES_ESTATICAS);
    }

    public static boolean esRecursoEstatico(String url, Set<String> extensionesEstaticas) {
        if (url == null || url.isEmpty() || extensionesEstaticas == null) {
            return false;
        }

        int queryIdx = url.indexOf('?');
        int hashIdx = url.indexOf('#');
        int endIdx = url.length();

        if (queryIdx != -1) endIdx = queryIdx;
        if (hashIdx != -1 && hashIdx < endIdx) endIdx = hashIdx;

        int dotIdx = url.lastIndexOf('.', endIdx - 1);
        if (dotIdx == -1 || dotIdx < url.lastIndexOf('/', endIdx - 1)) {
            return false;
        }

        String extension = url.substring(dotIdx, endIdx).toLowerCase(Locale.ROOT);
        return extensionesEstaticas.contains(extension);
    }
}
