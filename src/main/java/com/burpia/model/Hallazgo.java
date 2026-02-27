package com.burpia.model;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.burpia.i18n.I18nUI;
import com.burpia.ui.EstilosUI;
import java.awt.Color;
import java.util.Locale;
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
    private final String titulo;
    private final String hallazgo;
    private final String severidad;
    private final String confianza;
    private final HttpRequest solicitudHttp;
    private final HttpRequestResponse evidenciaHttp;

    public Hallazgo(String url, String titulo, String hallazgo, String severidad, String confianza) {
        this(url, titulo, hallazgo, severidad, confianza, (HttpRequest) null, (HttpRequestResponse) null);
    }

    public Hallazgo(String url, String titulo, String hallazgo, String severidad, String confianza, HttpRequest solicitudHttp) {
        this(url, titulo, hallazgo, severidad, confianza, solicitudHttp, null);
    }

    public Hallazgo(String url,
                    String titulo,
                    String hallazgo,
                    String severidad,
                    String confianza,
                    HttpRequest solicitudHttp,
                    HttpRequestResponse evidenciaHttp) {
        this(
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            url,
            titulo,
            hallazgo,
            severidad,
            confianza,
            solicitudHttp,
            evidenciaHttp
        );
    }

    public Hallazgo(String horaDescubrimiento, String url, String titulo, String hallazgo, String severidad, String confianza, HttpRequest solicitudHttp) {
        this(horaDescubrimiento, url, titulo, hallazgo, severidad, confianza, solicitudHttp, null);
    }

    public Hallazgo(String horaDescubrimiento,
                    String url,
                    String titulo,
                    String hallazgo,
                    String severidad,
                    String confianza,
                    HttpRequest solicitudHttp,
                    HttpRequestResponse evidenciaHttp) {
        this.horaDescubrimiento = horaDescubrimiento;
        this.url = url;
        this.titulo = titulo;
        this.hallazgo = hallazgo;
        this.severidad = severidad;
        this.confianza = confianza;
        this.solicitudHttp = solicitudHttp;
        this.evidenciaHttp = evidenciaHttp;
    }

    public String obtenerHoraDescubrimiento() {
        return horaDescubrimiento;
    }

    public String obtenerUrl() {
        return url;
    }

    public String obtenerTitulo() {
        return titulo;
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

    public HttpRequestResponse obtenerEvidenciaHttp() {
        return evidenciaHttp;
    }

    public Hallazgo conEvidenciaHttp(HttpRequestResponse evidenciaHttp) {
        if (evidenciaHttp == null || this.evidenciaHttp == evidenciaHttp) {
            return this;
        }
        return new Hallazgo(
            horaDescubrimiento,
            url,
            titulo,
            hallazgo,
            severidad,
            confianza,
            solicitudHttp,
            evidenciaHttp
        );
    }

    public Hallazgo editar(String nuevaUrl, String nuevoTitulo, String nuevaDescripcion, String nuevaSeveridad, String nuevaConfianza) {
        return new Hallazgo(
            horaDescubrimiento,
            nuevaUrl,
            nuevoTitulo,
            nuevaDescripcion,
            normalizarSeveridad(nuevaSeveridad),
            normalizarConfianza(nuevaConfianza),
            solicitudHttp,
            evidenciaHttp
        );
    }

    public Object[] aFilaTabla() {
        return new Object[]{
            horaDescubrimiento,
            url,
            titulo,
            I18nUI.Hallazgos.TRADUCIR_SEVERIDAD(severidad),
            I18nUI.Hallazgos.TRADUCIR_CONFIANZA(confianza)
        };
    }

    public static int obtenerPrioridadSeveridad(String severidad) {
        switch (normalizarSeveridadParaPrioridad(severidad)) {
            case SEVERIDAD_CRITICAL: return 5;
            case SEVERIDAD_HIGH:     return 4;
            case SEVERIDAD_MEDIUM:   return 3;
            case SEVERIDAD_LOW:      return 2;
            case SEVERIDAD_INFO:     return 1;
            default:                 return 0;
        }
    }

    public static int obtenerPesoSeveridad(String severidad) {
        return obtenerPrioridadSeveridad(severidad);
    }

    public static Color obtenerColorSeveridad(String severidad) {
        switch (normalizarSeveridadParaPrioridad(severidad)) {
            case SEVERIDAD_CRITICAL: return EstilosUI.COLOR_CRITICAL;
            case SEVERIDAD_HIGH:     return EstilosUI.COLOR_HIGH;
            case SEVERIDAD_MEDIUM:   return EstilosUI.COLOR_MEDIUM;
            case SEVERIDAD_LOW:      return EstilosUI.COLOR_LOW;
            case SEVERIDAD_INFO:     return EstilosUI.COLOR_INFO;
            default:                 return Color.GRAY;
        }
    }

    public static int obtenerPrioridadConfianza(String confianza) {
        switch (normalizarConfianzaParaPrioridad(confianza)) {
            case CONFIANZA_ALTA:  return 3;
            case CONFIANZA_MEDIA: return 2;
            case CONFIANZA_BAJA:  return 1;
            default:               return 0;
        }
    }

    public static Color obtenerColorConfianza(String confianza) {
        switch (normalizarConfianzaParaPrioridad(confianza)) {
            case CONFIANZA_ALTA:  return EstilosUI.COLOR_LOW;    
            case CONFIANZA_MEDIA: return EstilosUI.COLOR_MEDIUM; 
            case CONFIANZA_BAJA:  return EstilosUI.COLOR_HIGH;   
            default:               return Color.GRAY;
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

    public static String normalizarSeveridad(String severidad) {
        if (severidad == null || severidad.trim().isEmpty()) {
            return SEVERIDAD_INFO;
        }

        String valor = severidad.trim().toLowerCase(Locale.ROOT);
        if (valor.contains("critical") || valor.contains("critica")) {
            return SEVERIDAD_CRITICAL;
        }
        if (valor.contains("high") || valor.contains("alta") || valor.contains("severa")) {
            return SEVERIDAD_HIGH;
        }
        if (valor.contains("medium") || valor.contains("media") || valor.contains("moderada")) {
            return SEVERIDAD_MEDIUM;
        }
        if (valor.contains("low") || valor.contains("baja")) {
            return SEVERIDAD_LOW;
        }
        return SEVERIDAD_INFO;
    }

    public static String normalizarConfianza(String confianza) {
        if (confianza == null || confianza.trim().isEmpty()) {
            return CONFIANZA_MEDIA;
        }

        String valor = confianza.trim().toLowerCase(Locale.ROOT);
        if (valor.contains("high") || valor.contains("alta") || valor.contains("certain")) {
            return CONFIANZA_ALTA;
        }
        if (valor.contains("low") || valor.contains("baja") || valor.contains("tentative")) {
            return CONFIANZA_BAJA;
        }
        if (valor.contains("medium") || valor.contains("media") || valor.contains("firm")) {
            return CONFIANZA_MEDIA;
        }
        return CONFIANZA_MEDIA;
    }

    private static String normalizarSeveridadParaPrioridad(String severidad) {
        if (severidad == null || severidad.trim().isEmpty()) {
            return "";
        }
        String valor = severidad.trim().toLowerCase(Locale.ROOT);
        if (valor.contains("critical") || valor.contains("crítica") || valor.contains("critica")) {
            return SEVERIDAD_CRITICAL;
        }
        if (valor.contains("high") || valor.contains("alta") || valor.contains("severa")) {
            return SEVERIDAD_HIGH;
        }
        if (valor.contains("medium") || valor.contains("media") || valor.contains("moderada")) {
            return SEVERIDAD_MEDIUM;
        }
        if (valor.contains("low") || valor.contains("baja")) {
            return SEVERIDAD_LOW;
        }
        if (valor.contains("info") || valor.contains("inform")) {
            return SEVERIDAD_INFO;
        }
        return "";
    }

    private static String normalizarConfianzaParaPrioridad(String confianza) {
        if (confianza == null || confianza.trim().isEmpty()) {
            return "";
        }
        String valor = confianza.trim().toLowerCase(Locale.ROOT);
        if (valor.contains("high") || valor.contains("alta") || valor.contains("certain")) {
            return CONFIANZA_ALTA;
        }
        if (valor.contains("medium") || valor.contains("media") || valor.contains("firm")) {
            return CONFIANZA_MEDIA;
        }
        if (valor.contains("low") || valor.contains("baja") || valor.contains("tentative")) {
            return CONFIANZA_BAJA;
        }
        return "";
    }
}
