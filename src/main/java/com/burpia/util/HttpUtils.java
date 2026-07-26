package com.burpia.util;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import com.burpia.i18n.I18nUI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class HttpUtils {

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    public static String convertirDigestHex(byte[] hash) {
        return convertirDigestHex(hash, hash != null ? hash.length : 0);
    }

    public static String convertirDigestHex(byte[] hash, int maxBytes) {
        if (hash == null || hash.length == 0) {
            return "";
        }
        int limite = Math.min(hash.length, Math.max(1, maxBytes));
        char[] hexChars = new char[limite * 2];
        for (int j = 0; j < limite; j++) {
            int v = hash[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

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
            throw new RuntimeException(I18nUI.General.ERROR_SHA256_NO_DISPONIBLE(), e);
        }
    });

    private static MessageDigest obtenerSha256() {
        MessageDigest md = SHA256_LOCAL.get();
        md.reset();
        return md;
    }

    public static String generarHash(byte[] datos) {
        try {
            MessageDigest md = obtenerSha256();
            if (datos != null && datos.length > 0) {
                md.update(datos);
            }
            return convertirDigestHex(md.digest());
        } finally {
            // Sin remove() el MessageDigest pinea el classloader de la extensión
            // al recargarla en Burp (los hilos sobreviven al unload).
            SHA256_LOCAL.remove();
        }
    }

    public static String generarHashPartes(String... partes) {
        try {
            MessageDigest md = obtenerSha256();
            if (partes == null || partes.length == 0) {
                return convertirDigestHex(md.digest());
            }
            for (String parte : partes) {
                if (Normalizador.noEsVacio(parte)) {
                    md.update(parte.getBytes(StandardCharsets.UTF_8));
                }
                md.update((byte) 0);
            }
            return convertirDigestHex(md.digest());
        } finally {
            SHA256_LOCAL.remove();
        }
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

        try {
            MessageDigest md = obtenerSha256();

            actualizarDigest(md, solicitud.method());
            actualizarDigest(md, solicitud.url());
            actualizarDigest(md, respuesta.statusCode());

            byte[] bytesSolicitud = obtenerBytesSolicitudSeguros(solicitud);
            if (bytesSolicitud.length > 0) {
                actualizarDigest(md, bytesSolicitud);
            } else {
                long longitudSolicitud = obtenerLongitudCuerpoSeguro(solicitud);
                if (longitudSolicitud >= 0L) {
                    actualizarDigest(md, longitudSolicitud);
                }
            }

            byte[] bytesRespuesta = obtenerBytesRespuestaSeguros(respuesta);
            if (bytesRespuesta.length > 0) {
                actualizarDigest(md, bytesRespuesta);
            } else {
                long longitudRespuesta = obtenerLongitudCuerpoSeguro(respuesta);
                if (longitudRespuesta >= 0L) {
                    actualizarDigest(md, longitudRespuesta);
                }
            }

            return convertirDigestHex(md.digest());
        } finally {
            SHA256_LOCAL.remove();
        }
    }

    private static byte[] obtenerBytesSolicitudSeguros(HttpRequest solicitud) {
        try {
            if (solicitud == null || solicitud.toByteArray() == null) {
                return new byte[0];
            }
            byte[] bytes = solicitud.toByteArray().getBytes();
            return bytes != null ? bytes : new byte[0];
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private static byte[] obtenerBytesRespuestaSeguros(HttpResponse respuesta) {
        try {
            if (respuesta == null || respuesta.body() == null) {
                return new byte[0];
            }
            byte[] bytes = respuesta.body().getBytes();
            return bytes != null ? bytes : new byte[0];
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private static long obtenerLongitudCuerpoSeguro(HttpRequest solicitud) {
        try {
            return solicitud.body() != null ? solicitud.body().length() : -1L;
        } catch (Exception e) {
            // Error al obtener longitud, se retorna -1
            return -1L;
        }
    }

    private static long obtenerLongitudCuerpoSeguro(HttpResponse respuesta) {
        try {
            return respuesta.body() != null ? respuesta.body().length() : -1L;
        } catch (Exception e) {
            // Error al obtener longitud, se retorna -1
            return -1L;
        }
    }

    private static void actualizarDigest(MessageDigest md, String valor) {
        if (Normalizador.noEsVacio(valor)) {
            md.update(valor.getBytes(StandardCharsets.UTF_8));
        }
        md.update((byte) 0);
    }

    private static void actualizarDigest(MessageDigest md, long valor) {
        md.update(String.valueOf(valor).getBytes(StandardCharsets.UTF_8));
        md.update((byte) 0);
    }

    private static void actualizarDigest(MessageDigest md, byte[] valor) {
        if (valor != null && valor.length > 0) {
            md.update(valor);
        }
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
        if (Normalizador.noEsVacio(reason)) {
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

            // Cómputo en long para evitar overflow de maxCaracteres * 4 cuando
            // maxCaracteres está cerca de Integer.MAX_VALUE/4 (wraps a negativo).
            long presupuestoBytes = Math.max(64L, (long) maxCaracteres * 4L);
            int maxBytes = (int) Math.min(body.length(), presupuestoBytes);
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

    /**
     * Determina si un hostname corresponde a loopback o a una IP privada
     * (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16) o al nombre "localhost".
     * Usado para limitar el bypass de hostname verification SSL (O3).
     * <p>
     * SECURITY (H2): los rangos privados solo aplican a IPv4 numérica estricta
     * (4 octetos 0-255). Un prefijo textual como "10.evil.com" o
     * "192.168.attacker.example" NO debe activar el bypass SSL.
     * {@code URI.getHost()} devuelve "[::1]" con corchetes: se aceptan ambas formas.
     *
     * @param hostname hostname o IP a clasificar
     * @return true si es loopback o IP privada estricta
     */
    public static boolean esLoopbackOLan(String hostname) {
        if (Normalizador.esVacio(hostname)) {
            return false;
        }
        String h = hostname.toLowerCase(Locale.ROOT);
        if ("localhost".equals(h) || h.endsWith(".localhost") || "127.0.0.1".equals(h)
                || "::1".equals(h) || "[::1]".equals(h)) {
            return true;
        }
        int[] octetos = parsearOctetosIpv4Estrictos(h);
        if (octetos == null) {
            return false;
        }
        if (octetos[0] == 10) {
            return true;
        }
        if (octetos[0] == 192 && octetos[1] == 168) {
            return true;
        }
        return octetos[0] == 172 && octetos[1] >= 16 && octetos[1] <= 31;
    }

    private static int[] parsearOctetosIpv4Estrictos(String host) {
        String[] partes = host.split("\\.", -1);
        if (partes.length != 4) {
            return null;
        }
        int[] octetos = new int[4];
        for (int i = 0; i < partes.length; i++) {
            String parte = partes[i];
            if (parte.isEmpty() || parte.length() > 3) {
                return null;
            }
            for (int j = 0; j < parte.length(); j++) {
                if (!Character.isDigit(parte.charAt(j))) {
                    return null;
                }
            }
            int valor = Integer.parseInt(parte);
            if (valor > 255) {
                return null;
            }
            octetos[i] = valor;
        }
        return octetos;
    }

    public static boolean esRecursoEstatico(String url) {
        return esRecursoEstatico(url, EXTENSIONES_ESTATICAS);
    }

    public static boolean esRecursoEstatico(String url, Set<String> extensionesEstaticas) {
        if (Normalizador.esVacio(url) || Normalizador.esVacia(extensionesEstaticas)) {
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
