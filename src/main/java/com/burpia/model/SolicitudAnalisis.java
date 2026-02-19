package com.burpia.model;

import burp.api.montoya.http.message.requests.HttpRequest;

public class SolicitudAnalisis {
    private final String url;
    private final String metodo;
    private final String encabezados;
    private final String cuerpo;
    private final String hashSolicitud;
    private final HttpRequest solicitudHttp;

    public SolicitudAnalisis(String url, String metodo, String encabezados, String cuerpo, String hashSolicitud) {
        this(url, metodo, encabezados, cuerpo, hashSolicitud, null);
    }

    public SolicitudAnalisis(String url, String metodo, String encabezados, String cuerpo, String hashSolicitud, HttpRequest solicitudHttp) {
        this.url = url;
        this.metodo = metodo;
        this.encabezados = encabezados;
        this.cuerpo = cuerpo;
        this.hashSolicitud = hashSolicitud;
        this.solicitudHttp = solicitudHttp;
    }

    public String obtenerUrl() { return url; }
    public String obtenerMetodo() { return metodo; }
    public String obtenerEncabezados() { return encabezados; }
    public String obtenerCuerpo() { return cuerpo; }
    public String obtenerHashSolicitud() { return hashSolicitud; }
    public HttpRequest obtenerSolicitudHttp() { return solicitudHttp; }
}
