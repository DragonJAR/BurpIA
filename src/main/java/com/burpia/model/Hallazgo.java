package com.burpia.model;
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

    // Tablas de equivalencias keyword → valor canónico (matching por contains sobre
    // el valor en minúsculas, en orden de prioridad). Incluyen formas masculinas y
    // femeninas en ES ("Alto"/"Alta") porque los LLMs no garantizan el género
    // gramatical; las raíces ("crític"/"critic") cubren ambas variantes acentuadas.
    // "alto"/"medio"/"bajo" no colisionan con ninguna keyword de otra fila.
    private static final String[][] EQUIVALENCIAS_SEVERIDAD = {
        {SEVERIDAD_CRITICAL, "critical", "crític", "critic"},
        {SEVERIDAD_HIGH, "high", "alta", "alto", "severa", "severo"},
        {SEVERIDAD_MEDIUM, "medium", "media", "medio", "moderada", "moderado"},
        {SEVERIDAD_LOW, "low", "baja", "bajo"},
        {SEVERIDAD_INFO, "info", "inform"}
    };

    private static final String[][] EQUIVALENCIAS_CONFIANZA = {
        {CONFIANZA_ALTA, "high", "alta", "alto", "certain"},
        {CONFIANZA_MEDIA, "medium", "media", "medio", "firm"},
        {CONFIANZA_BAJA, "low", "baja", "bajo", "tentative"}
    };

    private final String horaDescubrimiento;
    private final String url;
    private final String titulo;
    private final String hallazgo;
    private final String severidad;
    private final String confianza;
    private final HttpRequest solicitudHttp;

    public Hallazgo(String url, String titulo, String hallazgo, String severidad, String confianza) {
        this(url, titulo, hallazgo, severidad, confianza, null);
    }

    public Hallazgo(String url, String titulo, String hallazgo, String severidad, String confianza, HttpRequest solicitudHttp) {
        this(
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            url,
            titulo,
            hallazgo,
            severidad,
            confianza,
            solicitudHttp
        );
    }

    public Hallazgo(String horaDescubrimiento,
                    String url,
                    String titulo,
                    String hallazgo,
                    String severidad,
                    String confianza,
                    HttpRequest solicitudHttp) {
        this.horaDescubrimiento = horaDescubrimiento;
        this.url = url;
        this.titulo = titulo;
        this.hallazgo = hallazgo;
        this.severidad = normalizarSeveridad(severidad);
        this.confianza = normalizarConfianza(confianza);
        this.solicitudHttp = solicitudHttp;
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

    /**
     * Request para enviar a las herramientas de Burp (Scanner/Repeater/Intruder). Usa el
     * request real en memoria si existe (método/cabeceras/cuerpo + httpService reales); si
     * no, lo deriva de la URL del hallazgo (GET con httpService válido vía
     * {@code httpRequestFromUrl}). Devuelve null si no hay request ni URL usable.
     */
    public HttpRequest obtenerSolicitudParaBurp() {
        if (solicitudHttp != null) {
            return solicitudHttp;
        }
        if (Normalizador.esVacio(url)) {
            return null;
        }
        try {
            return HttpRequest.httpRequestFromUrl(url);
        } catch (Exception e) {
            return null; // URL inválida para derivar el httpService
        }
    }

    public Hallazgo editar(String nuevaUrl, String nuevoTitulo, String nuevaDescripcion, String nuevaSeveridad, String nuevaConfianza) {
        return new Hallazgo(
            horaDescubrimiento,
            nuevaUrl,
            nuevoTitulo,
            nuevaDescripcion,
            nuevaSeveridad,
            nuevaConfianza,
            solicitudHttp
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
        return buscarCanonico(severidad.trim().toLowerCase(Locale.ROOT), EQUIVALENCIAS_SEVERIDAD, valorPorDefecto);
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
        return buscarCanonico(confianza.trim().toLowerCase(Locale.ROOT), EQUIVALENCIAS_CONFIANZA, valorPorDefecto);
    }

    private static String buscarCanonico(String valor, String[][] equivalencias, String valorPorDefecto) {
        for (String[] fila : equivalencias) {
            for (int i = 1; i < fila.length; i++) {
                if (valor.contains(fila[i])) {
                    return fila[0];
                }
            }
        }
        return valorPorDefecto;
    }

    /**
     * Compara hallazgos por contenido semántico, no por referencia de solicitud HTTP.
     * <p>
     * Nota: solicitudHttp NO se incluye en equals/hashCode porque HttpRequest no implementa
     * equals/hashCode de forma consistente y dos hallazgos con el mismo contenido pero
     * distinto request deben considerarse iguales.
     * </p>
     */
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
               Objects.equals(confianza, other.confianza);
    }

    @Override
    public int hashCode() {
        return Objects.hash(horaDescubrimiento, url, titulo, hallazgo, severidad, confianza);
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
