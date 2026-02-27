package com.burpia.model;

import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import com.burpia.util.HttpUtils;

public class SolicitudAnalisis {
    private final String url;
    private final String metodo;
    private final String hashSolicitud;
    private final HttpRequest solicitudHttp;
    private final HttpResponse respuestaHttp;
    private final int codigoEstadoRespuesta;


    private volatile String encabezados;
    private volatile String cuerpo;
    private volatile String encabezadosRespuesta;
    private volatile String cuerpoRespuesta;


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
        this.encabezados = encabezados != null ? encabezados : "";
        this.cuerpo = cuerpo != null ? cuerpo : "";
        this.encabezadosRespuesta = encabezadosRespuesta != null ? encabezadosRespuesta : "";
        this.cuerpoRespuesta = cuerpoRespuesta != null ? cuerpoRespuesta : "";
    }

    public SolicitudAnalisis(String url, String metodo, String encabezados, String cuerpo, String hashSolicitud) {
        this(url, metodo, encabezados, cuerpo, hashSolicitud, null, -1, "", "");
    }

    public SolicitudAnalisis(String url, String metodo, String encabezados, String cuerpo, String hashSolicitud, HttpRequest solicitudHttp) {
        this(url, metodo, encabezados, cuerpo, hashSolicitud, solicitudHttp, -1, "", "");
    }

    public String obtenerUrl() { return url; }
    public String obtenerMetodo() { return metodo; }
    public String obtenerHashSolicitud() { return hashSolicitud; }
    public HttpRequest obtenerSolicitudHttp() { return solicitudHttp; }
    public int obtenerCodigoEstadoRespuesta() { return codigoEstadoRespuesta; }

    public String obtenerEncabezados() {
        if (encabezados == null) {
            synchronized (this) {
                if (encabezados == null) {
                    encabezados = solicitudHttp != null ? HttpUtils.extraerEncabezados(solicitudHttp) : "";
                }
            }
        }
        return encabezados;
    }

    public String obtenerCuerpo() {
        if (cuerpo == null) {
            synchronized (this) {
                if (cuerpo == null) {
                    cuerpo = solicitudHttp != null ? HttpUtils.extraerCuerpo(solicitudHttp) : "";
                }
            }
        }
        return cuerpo;
    }

    public String obtenerEncabezadosRespuesta() {
        if (encabezadosRespuesta == null) {
            synchronized (this) {
                if (encabezadosRespuesta == null) {
                    encabezadosRespuesta = respuestaHttp != null ? HttpUtils.extraerEncabezados(respuestaHttp) : "";
                }
            }
        }
        return encabezadosRespuesta;
    }

    public String obtenerCuerpoRespuesta() {
        if (cuerpoRespuesta == null) {
            synchronized (this) {
                if (cuerpoRespuesta == null) {
                    cuerpoRespuesta = respuestaHttp != null ? HttpUtils.extraerCuerpo(respuestaHttp) : "";
                }
            }
        }
        return cuerpoRespuesta;
    }
}
