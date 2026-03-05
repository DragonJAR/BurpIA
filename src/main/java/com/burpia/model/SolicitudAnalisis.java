package com.burpia.model;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import com.burpia.util.HttpUtils;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Modelo que encapsula una solicitud HTTP y su respuesta para análisis de seguridad.
 * <p>
 * Esta clase proporciona acceso lazy a los componentes de la solicitud (encabezados, cuerpo)
 * y respuesta, extrayéndolos bajo demanda desde los objetos {@link HttpRequest} y
 * {@link HttpResponse} de la API Montoya.
 * </p>
 * <p>
 * Implementa thread-safety mediante double-checked locking con campos {@code volatile}
 * para la inicialización diferida de encabezados y cuerpos.
 * </p>
 *
 * @see HttpUtils Para los métodos de extracción de componentes HTTP
 */
public class SolicitudAnalisis {

    /** URL de la solicitud HTTP. */
    private final String url;

    /** Método HTTP (GET, POST, PUT, DELETE, etc.). */
    private final String metodo;

    /** Hash único para identificar esta solicitud. */
    private final String hashSolicitud;

    /** Objeto HttpRequest de la API Montoya (puede ser null si se usan strings). */
    private final HttpRequest solicitudHttp;

    /** Objeto HttpResponse de la API Montoya (puede ser null). */
    private final HttpResponse respuestaHttp;

    /** Código de estado HTTP de la respuesta (ej: 200, 404, 500). */
    private final int codigoEstadoRespuesta;

    /** Encabezados de la solicitud (inicialización lazy, thread-safe). */
    private volatile String encabezados;

    /** Cuerpo de la solicitud (inicialización lazy, thread-safe). */
    private volatile String cuerpo;

    /** Encabezados de la respuesta (inicialización lazy, thread-safe). */
    private volatile String encabezadosRespuesta;

    /** Cuerpo de la respuesta (inicialización lazy, thread-safe). */
    private volatile String cuerpoRespuesta;

    /**
     * Constructor principal para crear una solicitud de análisis desde objetos Montoya.
     * <p>
     * Los encabezados y cuerpos se extraerán de forma lazy desde los objetos
     * {@link HttpRequest} y {@link HttpResponse} cuando se accedan por primera vez.
     * </p>
     *
     * @param url URL de la solicitud HTTP
     * @param metodo Método HTTP (GET, POST, PUT, DELETE, etc.)
     * @param hashSolicitud Hash único para identificar esta solicitud
     * @param solicitudHttp Objeto HttpRequest de Burp (puede ser null)
     * @param respuestaHttp Objeto HttpResponse de Burp (puede ser null)
     * @param codigoEstadoRespuesta Código de estado HTTP de la respuesta
     */
    public SolicitudAnalisis(String url, String metodo, String hashSolicitud,
                             HttpRequest solicitudHttp, HttpResponse respuestaHttp,
                             int codigoEstadoRespuesta) {
        this.url = url;
        this.metodo = metodo;
        this.hashSolicitud = hashSolicitud;
        this.solicitudHttp = solicitudHttp;
        this.respuestaHttp = respuestaHttp;
        this.codigoEstadoRespuesta = codigoEstadoRespuesta;
    }

    /**
     * Constructor para crear una solicitud de análisis con componentes pre-extraídos.
     * <p>
     * Este constructor es útil cuando los encabezados y cuerpos ya han sido
     * extraídos como strings, evitando la necesidad de acceder a los objetos
     * HTTP originales.
     * </p>
     *
     * @param url URL de la solicitud HTTP
     * @param metodo Método HTTP (GET, POST, PUT, DELETE, etc.)
     * @param encabezados Encabezados de la solicitud ya extraídos (null se convierte en "")
     * @param cuerpo Cuerpo de la solicitud ya extraído (null se convierte en "")
     * @param hashSolicitud Hash único para identificar esta solicitud
     * @param solicitudHttp Objeto HttpRequest de Burp para referencia (puede ser null)
     * @param codigoEstadoRespuesta Código de estado HTTP de la respuesta
     * @param encabezadosRespuesta Encabezados de la respuesta ya extraídos (null se convierte en "")
     * @param cuerpoRespuesta Cuerpo de la respuesta ya extraído (null se convierte en "")
     */
    public SolicitudAnalisis(String url,
                             String metodo,
                             String encabezados,
                             String cuerpo,
                             String hashSolicitud,
                             HttpRequest solicitudHttp,
                             int codigoEstadoRespuesta,
                             String encabezadosRespuesta,
                             String cuerpoRespuesta) {
        this.url = url;
        this.metodo = metodo;
        this.hashSolicitud = hashSolicitud;
        this.solicitudHttp = solicitudHttp;
        this.respuestaHttp = null;
        this.codigoEstadoRespuesta = codigoEstadoRespuesta;
        this.encabezados = normalizarNull(encabezados);
        this.cuerpo = normalizarNull(cuerpo);
        this.encabezadosRespuesta = normalizarNull(encabezadosRespuesta);
        this.cuerpoRespuesta = normalizarNull(cuerpoRespuesta);
    }

    /**
     * Constructor simplificado para crear una solicitud de análisis básica.
     * <p>
     * Crea una solicitud con componentes pre-extraídos, sin información de respuesta HTTP.
     * </p>
     *
     * @param url URL de la solicitud HTTP
     * @param metodo Método HTTP
     * @param encabezados Encabezados de la solicitud (null se convierte en "")
     * @param cuerpo Cuerpo de la solicitud (null se convierte en "")
     * @param hashSolicitud Hash único para identificar esta solicitud
     */
    public SolicitudAnalisis(String url, String metodo, String encabezados, String cuerpo, String hashSolicitud) {
        this(url, metodo, encabezados, cuerpo, hashSolicitud, null, -1, "", "");
    }

    /**
     * Constructor simplificado con HttpRequest pero sin respuesta.
     *
     * @param url URL de la solicitud HTTP
     * @param metodo Método HTTP
     * @param encabezados Encabezados de la solicitud (null se convierte en "")
     * @param cuerpo Cuerpo de la solicitud (null se convierte en "")
     * @param hashSolicitud Hash único para identificar esta solicitud
     * @param solicitudHttp Objeto HttpRequest de Burp para referencia
     */
    public SolicitudAnalisis(String url, String metodo, String encabezados, String cuerpo, String hashSolicitud, HttpRequest solicitudHttp) {
        this(url, metodo, encabezados, cuerpo, hashSolicitud, solicitudHttp, -1, "", "");
    }

    /**
     * Normaliza un valor null a cadena vacía.
     *
     * @param valor El valor a normalizar
     * @return El valor original o "" si era null
     */
    private static String normalizarNull(String valor) {
        return valor != null ? valor : "";
    }

    /**
     * Obtiene los encabezados de la solicitud HTTP.
     * <p>
     * Si los encabezados aún no han sido extraídos, los obtiene de forma lazy
     * desde el objeto {@link HttpRequest} utilizando {@link HttpUtils#extraerEncabezados(HttpRequest)}.
     * </p>
     *
     * @return Los encabezados de la solicitud, o cadena vacía si no están disponibles
     */
    public String obtenerEncabezados() {
        return inicializarLazy(
            () -> encabezados,
            valor -> encabezados = valor,
            () -> solicitudHttp != null ? HttpUtils.extraerEncabezados(solicitudHttp) : ""
        );
    }

    /**
     * Obtiene el cuerpo de la solicitud HTTP.
     * <p>
     * Si el cuerpo aún no ha sido extraído, lo obtiene de forma lazy
     * desde el objeto {@link HttpRequest} utilizando {@link HttpUtils#extraerCuerpo(HttpRequest)}.
     * </p>
     *
     * @return El cuerpo de la solicitud, o cadena vacía si no está disponible
     */
    public String obtenerCuerpo() {
        return inicializarLazy(
            () -> cuerpo,
            valor -> cuerpo = valor,
            () -> solicitudHttp != null ? HttpUtils.extraerCuerpo(solicitudHttp) : ""
        );
    }

    /**
     * Obtiene los encabezados de la respuesta HTTP.
     * <p>
     * Si los encabezados aún no han sido extraídos, los obtiene de forma lazy
     * desde el objeto {@link HttpResponse} utilizando {@link HttpUtils#extraerEncabezados(HttpResponse)}.
     * </p>
     *
     * @return Los encabezados de la respuesta, o cadena vacía si no están disponibles
     */
    public String obtenerEncabezadosRespuesta() {
        return inicializarLazy(
            () -> encabezadosRespuesta,
            valor -> encabezadosRespuesta = valor,
            () -> respuestaHttp != null ? HttpUtils.extraerEncabezados(respuestaHttp) : ""
        );
    }

    /**
     * Obtiene el cuerpo de la respuesta HTTP.
     * <p>
     * Si el cuerpo aún no ha sido extraído, lo obtiene de forma lazy
     * desde el objeto {@link HttpResponse} utilizando {@link HttpUtils#extraerCuerpo(HttpResponse)}.
     * </p>
     *
     * @return El cuerpo de la respuesta, o cadena vacía si no está disponible
     */
    public String obtenerCuerpoRespuesta() {
        return inicializarLazy(
            () -> cuerpoRespuesta,
            valor -> cuerpoRespuesta = valor,
            () -> respuestaHttp != null ? HttpUtils.extraerCuerpo(respuestaHttp) : ""
        );
    }

    /**
     * Inicializa un valor de forma lazy utilizando double-checked locking.
     * <p>
     * Este método centraliza el patrón de inicialización diferida thread-safe,
     * siguiendo el principio DRY (Don't Repeat Yourself).
     * </p>
     *
     * @param <T> Tipo del valor a inicializar
     * @param getter Función para obtener el valor actual (puede ser null)
     * @param setter Función para establecer el valor inicializado
     * @param proveedor Función que proporciona el valor si es null
     * @return El valor (existente o recién inicializado)
     */
    private <T> T inicializarLazy(Supplier<T> getter, Consumer<T> setter, Supplier<T> proveedor) {
        T valor = getter.get();
        if (valor == null) {
            synchronized (this) {
                valor = getter.get();
                if (valor == null) {
                    valor = proveedor.get();
                    setter.accept(valor);
                }
            }
        }
        return valor;
    }

    // ==================== Getters simples ====================

    /** @return La URL de la solicitud HTTP */
    public String obtenerUrl() { return url; }

    /** @return El método HTTP (GET, POST, PUT, etc.) */
    public String obtenerMetodo() { return metodo; }

    /** @return El hash único de la solicitud */
    public String obtenerHashSolicitud() { return hashSolicitud; }

    /** @return El objeto HttpRequest de Burp, o null si no está disponible */
    public HttpRequest obtenerSolicitudHttp() { return solicitudHttp; }

    /** @return El código de estado HTTP de la respuesta */
    public int obtenerCodigoEstadoRespuesta() { return codigoEstadoRespuesta; }
}
