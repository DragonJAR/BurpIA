package com.burpia.util;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class HttpUtils {

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

    public static String generarHash(byte[] datos) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            if (datos != null && datos.length > 0) {
                md.update(datos);
            }
            return convertirDigestHex(md.digest());
        } catch (Exception e) {
            throw new RuntimeException("Error al generar hash SHA-256", e);
        }
    }

    public static String generarHashPartes(String... partes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            if (partes != null) {
                for (String parte : partes) {
                    if (parte != null && !parte.isEmpty()) {
                        md.update(parte.getBytes(StandardCharsets.UTF_8));
                    }
                    md.update((byte) 0); 
                }
            }
            return convertirDigestHex(md.digest());
        } catch (Exception e) {
            throw new RuntimeException("Error al generar hash SHA-256", e);
        }
    }

    public static String convertirDigestHex(byte[] hash) {
        if (hash == null) {
            return "";
        }
        StringBuilder cadenaHexadecimal = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                cadenaHexadecimal.append('0');
            }
            cadenaHexadecimal.append(hex);
        }
        return cadenaHexadecimal.toString();
    }

    public static String extraerEncabezados(HttpRequest solicitud) {
        if (solicitud == null) {
            return "[SOLICITUD NULL]";
        }

        StringBuilder encabezados = new StringBuilder();
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

    public static String extraerEncabezados(HttpResponse respuesta) {
        if (respuesta == null) {
            return "[RESPUESTA NULL]";
        }

        StringBuilder encabezados = new StringBuilder();
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
        if (solicitud == null) {
            return "";
        }
        try {
            String cuerpo = solicitud.bodyToString();
            return cuerpo != null ? cuerpo : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    public static String extraerCuerpo(HttpResponse respuesta) {
        if (respuesta == null) {
            return "";
        }
        try {
            String cuerpo = respuesta.bodyToString();
            return cuerpo != null ? cuerpo : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    public static boolean esRecursoEstatico(String url) {
        return esRecursoEstatico(url, EXTENSIONES_ESTATICAS);
    }

    public static boolean esRecursoEstatico(String url, Set<String> extensionesEstaticas) {
        if (url == null || url.isEmpty() || extensionesEstaticas == null) {
            return false;
        }

        int startIdx = 0;
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
