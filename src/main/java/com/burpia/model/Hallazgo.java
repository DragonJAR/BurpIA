package com.burpia.model;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import com.burpia.i18n.I18nUI;
import com.burpia.util.Normalizador;
import java.util.Locale;
import java.util.Objects;
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

    @FunctionalInterface
    public interface ResolutorEvidencia {
        HttpRequestResponse resolver(String evidenciaId);
    }

    private static volatile ResolutorEvidencia resolutorEvidencia;

    private final String horaDescubrimiento;
    private final String url;
    private final String titulo;
    private final String hallazgo;
    private final String severidad;
    private final String confianza;
    private final HttpRequest solicitudHttp;
    private final String evidenciaId;
    private transient volatile HttpRequestResponse evidenciaHttp;

    public static void establecerResolutorEvidencia(ResolutorEvidencia resolutor) {
        resolutorEvidencia = resolutor;
    }

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
            evidenciaHttp,
            null
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
        this(horaDescubrimiento, url, titulo, hallazgo, severidad, confianza, solicitudHttp, evidenciaHttp, null);
    }

    public Hallazgo(String horaDescubrimiento,
                    String url,
                    String titulo,
                    String hallazgo,
                    String severidad,
                    String confianza,
                    HttpRequest solicitudHttp,
                    HttpRequestResponse evidenciaHttp,
                    String evidenciaId) {
        this.horaDescubrimiento = horaDescubrimiento;
        this.url = url;
        this.titulo = titulo;
        this.hallazgo = hallazgo;
        this.severidad = normalizarSeveridad(severidad);
        this.confianza = normalizarConfianza(confianza);
        this.solicitudHttp = solicitudHttp;
        this.evidenciaHttp = evidenciaHttp;
        this.evidenciaId = evidenciaId;
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
        HttpRequestResponse evidenciaActual = evidenciaHttp;
        if (evidenciaActual != null) {
            return evidenciaActual;
        }
        String id = evidenciaId;
        ResolutorEvidencia resolutor = resolutorEvidencia;
        if (Normalizador.esVacio(id) || resolutor == null) {
            return null;
        }
        try {
            evidenciaActual = resolutor.resolver(id);
            evidenciaHttp = evidenciaActual;
            return evidenciaActual;
        } catch (Exception e) {
            return null;
        }
    }

    public String obtenerEvidenciaId() {
        return evidenciaId;
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
            evidenciaHttp,
            evidenciaId
        );
    }

    public Hallazgo conEvidenciaId(String nuevoEvidenciaId) {
        if (Normalizador.esVacio(nuevoEvidenciaId)) {
            return this;
        }
        if (nuevoEvidenciaId.equals(this.evidenciaId)) {
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
            null,
            nuevoEvidenciaId
        );
    }

    public Hallazgo editar(String nuevaUrl, String nuevoTitulo, String nuevaDescripcion, String nuevaSeveridad, String nuevaConfianza) {
        return new Hallazgo(
            horaDescubrimiento,
            nuevaUrl,
            nuevoTitulo,
            nuevaDescripcion,
            nuevaSeveridad,
            nuevaConfianza,
            solicitudHttp,
            evidenciaHttp,
            evidenciaId
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

    public static int obtenerPrioridadConfianza(String confianza) {
        switch (normalizarConfianzaParaPrioridad(confianza)) {
            case CONFIANZA_ALTA:  return 3;
            case CONFIANZA_MEDIA: return 2;
            case CONFIANZA_BAJA:  return 1;
            default:               return 0;
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
        return normalizarSeveridadInterno(severidad, SEVERIDAD_INFO);
    }

    private static String normalizarSeveridadParaPrioridad(String severidad) {
        return normalizarSeveridadInterno(severidad, "");
    }

    private static String normalizarSeveridadInterno(String severidad, String valorPorDefecto) {
        if (Normalizador.esVacio(severidad)) {
            return valorPorDefecto;
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
        return valorPorDefecto;
    }

    public static String normalizarConfianza(String confianza) {
        return normalizarConfianzaInterno(confianza, CONFIANZA_MEDIA);
    }

    private static String normalizarConfianzaParaPrioridad(String confianza) {
        return normalizarConfianzaInterno(confianza, "");
    }

    private static String normalizarConfianzaInterno(String confianza, String valorPorDefecto) {
        if (Normalizador.esVacio(confianza)) {
            return valorPorDefecto;
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
        return valorPorDefecto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Hallazgo other = (Hallazgo) o;
        return Objects.equals(horaDescubrimiento, other.horaDescubrimiento) &&
               Objects.equals(url, other.url) &&
               Objects.equals(titulo, other.titulo) &&
               Objects.equals(hallazgo, other.hallazgo) &&
               Objects.equals(severidad, other.severidad) &&
               Objects.equals(confianza, other.confianza) &&
               Objects.equals(evidenciaId, other.evidenciaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(horaDescubrimiento, url, titulo, hallazgo, severidad, confianza, evidenciaId);
    }

    @Override
    public String toString() {
        return "Hallazgo{" +
               "hora='" + horaDescubrimiento + '\'' +
               ", url='" + url + '\'' +
               ", titulo='" + titulo + '\'' +
               ", severidad='" + severidad + '\'' +
               ", confianza='" + confianza + '\'' +
               '}';
    }
}
