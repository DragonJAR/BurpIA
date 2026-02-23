package com.burpia.model;
import burp.api.montoya.http.message.requests.HttpRequest;



public class SolicitudAnalisis {
    private final String url;
    private final String metodo;
    private final String encabezados;
    private final String cuerpo;
    private final String hashSolicitud;
    private final HttpRequest solicitudHttp;
    private final int codigoEstadoRespuesta;
    private final String encabezadosRespuesta;
    private final String cuerpoRespuesta;

    public SolicitudAnalisis(String url, String metodo, String encabezados, String cuerpo, String hashSolicitud) {
        this(url, metodo, encabezados, cuerpo, hashSolicitud, null, -1, "", "");
    }

    public SolicitudAnalisis(String url, String metodo, String encabezados, String cuerpo, String hashSolicitud, HttpRequest solicitudHttp) {
        this(url, metodo, encabezados, cuerpo, hashSolicitud, solicitudHttp, -1, "", "");
    }

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
        this.encabezados = encabezados;
        this.cuerpo = cuerpo;
        this.hashSolicitud = hashSolicitud;
        this.solicitudHttp = solicitudHttp;
        this.codigoEstadoRespuesta = codigoEstadoRespuesta;
        this.encabezadosRespuesta = encabezadosRespuesta != null ? encabezadosRespuesta : "";
        this.cuerpoRespuesta = cuerpoRespuesta != null ? cuerpoRespuesta : "";
    }

    public String obtenerUrl() { return url; }
    public String obtenerMetodo() { return metodo; }
    public String obtenerEncabezados() { return encabezados; }
    public String obtenerCuerpo() { return cuerpo; }
    public String obtenerHashSolicitud() { return hashSolicitud; }
    public HttpRequest obtenerSolicitudHttp() { return solicitudHttp; }
    public int obtenerCodigoEstadoRespuesta() { return codigoEstadoRespuesta; }
    public String obtenerEncabezadosRespuesta() { return encabezadosRespuesta; }
    public String obtenerCuerpoRespuesta() { return cuerpoRespuesta; }
}
