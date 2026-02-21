package com.burpia.model;

import burp.api.montoya.http.message.requests.HttpRequest;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Hallazgo {
    public static final String SEVERIDAD_CRITICAL = "Critical";
    public static final String SEVERIDAD_HIGH = "High";
    public static final String SEVERIDAD_MEDIUM = "Medium";
    public static final String SEVERIDAD_LOW = "Low";
    public static final String SEVERIDAD_INFO = "Info";

    public static final String CONFIANZA_ALTA = "High";
    public static final String CONFIANZA_MEDIA = "Medium";
    public static final String CONFIANZA_BAJA = "Low";

    private final String horaDescubrimiento;
    private final String url;
    private final String hallazgo;
    private final String severidad;
    private final String confianza;
    private final HttpRequest solicitudHttp;

    public Hallazgo(String url, String hallazgo, String severidad, String confianza) {
        this(url, hallazgo, severidad, confianza, null);
    }

    public Hallazgo(String url, String hallazgo, String severidad, String confianza, HttpRequest solicitudHttp) {
        this(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), url, hallazgo, severidad, confianza, solicitudHttp);
    }

    public Hallazgo(String horaDescubrimiento, String url, String hallazgo, String severidad, String confianza, HttpRequest solicitudHttp) {
        this.horaDescubrimiento = horaDescubrimiento;
        this.url = url;
        this.hallazgo = hallazgo;
        this.severidad = severidad;
        this.confianza = confianza;
        this.solicitudHttp = solicitudHttp;
    }

    public String obtenerHoraDescubrimiento() {
        return horaDescubrimiento;
    }

    public String obtenerUrl() {
        return url;
    }

    public String obtenerHallazgo() {
        return hallazgo;
    }

    public String obtenerSeveridad() {
        return severidad;
    }

    public String obtenerConfianza() {
        return confianza;
    }

    public HttpRequest obtenerSolicitudHttp() {
        return solicitudHttp;
    }

    public Object[] aFilaTabla() {
        return new Object[]{
            horaDescubrimiento,
            url,
            hallazgo,
            severidad,
            confianza
        };
    }

    public static Color obtenerColorSeveridad(String severidad) {
        switch (severidad) {
            case SEVERIDAD_CRITICAL:
                return new Color(128, 0, 128); // Purple
            case SEVERIDAD_HIGH:
                return new Color(204, 0, 0); // Rojo
            case SEVERIDAD_MEDIUM:
                return new Color(255, 153, 0); // Naranja
            case SEVERIDAD_LOW:
                return new Color(0, 153, 0); // Verde
            case SEVERIDAD_INFO:
                return new Color(0, 120, 215); // Azul
            default:
                return Color.GRAY;
        }
    }

    public static Color obtenerColorConfianza(String confianza) {
        switch (confianza) {
            case CONFIANZA_ALTA:
                return new Color(0, 153, 0); // Verde
            case CONFIANZA_MEDIA:
                return new Color(255, 153, 0); // Naranja
            case CONFIANZA_BAJA:
                return new Color(204, 0, 0); // Rojo
            default:
                return Color.GRAY;
        }
    }

    public static int obtenerPesoSeveridad(String severidad) {
        if (severidad == null) return 0;
        switch (severidad) {
            case SEVERIDAD_CRITICAL: return 5;
            case SEVERIDAD_HIGH: return 4;
            case SEVERIDAD_MEDIUM: return 3;
            case SEVERIDAD_LOW: return 2;
            case SEVERIDAD_INFO: return 1;
            default: return 0;
        }
    }

    public static boolean esSeveridadValida(String severidad) {
        return SEVERIDAD_CRITICAL.equals(severidad) ||
               SEVERIDAD_HIGH.equals(severidad) ||
               SEVERIDAD_MEDIUM.equals(severidad) ||
               SEVERIDAD_LOW.equals(severidad) ||
               SEVERIDAD_INFO.equals(severidad);
    }

    public static boolean esConfianzaValida(String confianza) {
        return CONFIANZA_ALTA.equals(confianza) ||
               CONFIANZA_MEDIA.equals(confianza) ||
               CONFIANZA_BAJA.equals(confianza);
    }
}
