package com.burpia.processor;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import com.burpia.config.ConfiguracionAPI;
import com.burpia.i18n.I18nLogs;
import com.burpia.model.SolicitudAnalisis;
import com.burpia.util.FiltroContenidoAnalizable;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.HttpUtils;
import com.burpia.util.Normalizador;
import com.burpia.util.PoliticaMemoria;

public final class HttpRequestProcessor {
    
    private static final String ORIGEN_LOG = "HttpRequestProcessor";
    
    private final MontoyaApi api;
    private final ConfiguracionAPI config;
    private final GestorLoggingUnificado gestorLogging;
    
    public HttpRequestProcessor(MontoyaApi api, ConfiguracionAPI config, GestorLoggingUnificado gestorLogging) {
        this.api = api;
        this.config = config != null ? config : new ConfiguracionAPI();
        this.gestorLogging = gestorLogging;
    }
    
    public HttpRequestProcessor(MontoyaApi api, ConfiguracionAPI config) {
        this(api, config, null);
    }
    
    public boolean estaEnScope(HttpRequest solicitud) {
        if (solicitud == null) {
            return false;
        }

        try {
            String url = solicitud.url();
            if (Normalizador.esVacio(url)) {
                return false;
            }

            if (api != null && api.scope() != null) {
                return api.scope().isInScope(url);
            }

            return false;

        } catch (Exception e) {
            if (gestorLogging != null) {
                gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("Error al verificar scope"), e);
            }
            return false;
        }
    }
    
    public boolean esRecursoEstatico(String url) {
        if (Normalizador.esVacio(url)) {
            return false;
        }

        boolean esEstatico = HttpUtils.esRecursoEstatico(url);

        if (esEstatico && config.esDetallado() && gestorLogging != null) {
            gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Recurso coincidio con filtro estatico: " + url));
        }

        return esEstatico;
    }
    
    public boolean esSoloProxy(HttpResponseReceived respuestaRecibida) {
        if (respuestaRecibida == null) {
            return false;
        }
        
        return config.soloProxy() && !respuestaRecibida.toolSource().isFromTool(ToolType.PROXY);
    }
    
    public boolean esContenidoAnalizable(String contentType, String metodo, int codigoEstado) {
        return FiltroContenidoAnalizable.esAnalizable(contentType, metodo, codigoEstado);
    }
    
    public String obtenerContentTypeRespuesta(HttpResponseReceived respuestaRecibida) {
        if (respuestaRecibida == null || respuestaRecibida.headers() == null) {
            return "";
        }
        try {
            for (HttpHeader header : respuestaRecibida.headers()) {
                if (header == null || header.name() == null) {
                    continue;
                }
                if ("Content-Type".equalsIgnoreCase(header.name())) {
                    return header.value() != null ? header.value() : "";
                }
            }
        } catch (Exception e) {
            if (gestorLogging != null) {
                gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("No se pudo extraer Content-Type de respuesta"), e);
            }
        }
        return "";
    }
    
    public boolean esSolicitudValida(HttpResponseReceived respuestaRecibida) {
        if (respuestaRecibida == null) {
            if (gestorLogging != null) {
                gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("Respuesta recibida es null"));
            }
            return false;
        }

        if (respuestaRecibida.initiatingRequest() == null) {
            if (gestorLogging != null) {
                gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("Solicitud iniciadora es null"));
            }
            return false;
        }
        
        return true;
    }
    
    public SolicitudAnalisis crearSolicitudAnalisisDesdeRespuesta(HttpResponseReceived respuestaRecibida) {
        if (!esSolicitudValida(respuestaRecibida)) {
            return null;
        }
        
        String url = obtenerUrlSegura(respuestaRecibida.initiatingRequest());
        String metodo = obtenerMetodoSeguro(respuestaRecibida.initiatingRequest());
        int codigoEstado = respuestaRecibida.statusCode();
        
        String encabezadosSolicitud = HttpUtils.extraerEncabezados(respuestaRecibida.initiatingRequest());
        String cuerpoSolicitud = HttpUtils.extraerCuerpo(
                respuestaRecibida.initiatingRequest(),
                PoliticaMemoria.MAXIMO_CUERPO_ANALISIS_CARACTERES);
        String encabezadosRespuesta = HttpUtils.extraerEncabezados(respuestaRecibida);
        String cuerpoRespuesta = HttpUtils.extraerCuerpo(
                respuestaRecibida,
                PoliticaMemoria.MAXIMO_CUERPO_ANALISIS_CARACTERES);
        
        String hashSolicitud = HttpUtils.generarHashRapido(respuestaRecibida.initiatingRequest(), respuestaRecibida);
        
        return new SolicitudAnalisis(
                url,
                metodo,
                encabezadosSolicitud,
                cuerpoSolicitud,
                hashSolicitud,
                respuestaRecibida.initiatingRequest(),
                codigoEstado,
                encabezadosRespuesta,
                cuerpoRespuesta);
    }
    
    public SolicitudAnalisis crearSolicitudAnalisisForzada(HttpRequest solicitud, HttpRequestResponse solicitudRespuestaOriginal) {
        if (solicitud == null) {
            if (gestorLogging != null) {
                gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("No se pudo analizar solicitud forzada: request null"));
            }
            return null;
        }

        String url = obtenerUrlSegura(solicitud);
        String metodo = obtenerMetodoSeguro(solicitud);
        String encabezados = HttpUtils.extraerEncabezados(solicitud);
        String cuerpo = HttpUtils.extraerCuerpo(solicitud, PoliticaMemoria.MAXIMO_CUERPO_ANALISIS_CARACTERES);
        String hashSolicitud = HttpUtils.generarHashPartes(metodo, url, encabezados, cuerpo);
        
        int codigoEstadoRespuesta = -1;
        String encabezadosRespuesta = "";
        String cuerpoRespuesta = "";
        if (solicitudRespuestaOriginal != null) {
            try {
                if (solicitudRespuestaOriginal.hasResponse() && solicitudRespuestaOriginal.response() != null) {
                    codigoEstadoRespuesta = solicitudRespuestaOriginal.response().statusCode();
                    encabezadosRespuesta = HttpUtils.extraerEncabezados(solicitudRespuestaOriginal.response());
                    cuerpoRespuesta = HttpUtils.extraerCuerpo(
                            solicitudRespuestaOriginal.response(),
                            PoliticaMemoria.MAXIMO_CUERPO_ANALISIS_CARACTERES);
                }
            } catch (Exception e) {
                if (gestorLogging != null) {
                    gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("No se pudo capturar la respuesta para analisis manual"), e);
                }
            }
        }

        return new SolicitudAnalisis(
                url,
                metodo,
                encabezados,
                cuerpo,
                hashSolicitud,
                solicitud,
                codigoEstadoRespuesta,
                encabezadosRespuesta,
                cuerpoRespuesta);
    }
    
    public HttpRequestResponse construirEvidenciaHttp(HttpRequest solicitud, HttpResponse respuesta) {
        if (solicitud == null || respuesta == null) {
            return null;
        }
        try {
            return HttpRequestResponse.httpRequestResponse(solicitud, respuesta);
        } catch (Exception e) {
            if (gestorLogging != null) {
                gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("No se pudo construir HttpRequestResponse para evidencia de Issue"), e);
            }
            return null;
        }
    }
    
    public HttpRequestResponse normalizarEvidenciaManual(HttpRequest solicitud, HttpRequestResponse solicitudRespuestaOriginal) {
        if (solicitudRespuestaOriginal == null) {
            if (gestorLogging != null) {
                gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Analisis manual sin solicitud/respuesta original: se registraran hallazgos, pero no Issue."));
            }
            return null;
        }

        try {
            if (!solicitudRespuestaOriginal.hasResponse()) {
                if (gestorLogging != null) {
                    gestorLogging.info(ORIGEN_LOG, I18nLogs.tr("Analisis manual sin response asociada: se registraran hallazgos, pero no Issue."));
                }
                return null;
            }
            if (solicitudRespuestaOriginal.request() != null && solicitudRespuestaOriginal.response() != null) {
                return solicitudRespuestaOriginal;
            }
        } catch (Exception e) {
            if (gestorLogging != null) {
                gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("No se pudo reutilizar la evidencia original del analisis manual"), e);
            }
        }

        try {
            return construirEvidenciaHttp(solicitud, solicitudRespuestaOriginal.response());
        } catch (Exception e) {
            if (gestorLogging != null) {
                gestorLogging.error(ORIGEN_LOG, I18nLogs.tr("No se pudo construir evidencia desde analisis manual"), e);
            }
            return null;
        }
    }
    
    private String obtenerUrlSegura(HttpRequest solicitud) {
        String tempUrl = solicitud != null ? solicitud.url() : null;
        return tempUrl != null ? tempUrl : "[URL NULL]";
    }
    
    private String obtenerMetodoSeguro(HttpRequest solicitud) {
        String tempMetodo = solicitud != null ? solicitud.method() : null;
        return tempMetodo != null ? tempMetodo : "[METHOD NULL]";
    }
    
    public boolean puedeIniciarAnalisis() {
        ConfiguracionAPI.CodigoValidacionConsulta codigo = obtenerCodigoValidacionConsulta();
        return codigo == ConfiguracionAPI.CodigoValidacionConsulta.OK;
    }
    
    private ConfiguracionAPI.CodigoValidacionConsulta obtenerCodigoValidacionConsulta() {
        if (config == null) {
            return ConfiguracionAPI.CodigoValidacionConsulta.CONFIGURACION_NULA;
        }
        ConfiguracionAPI.CodigoValidacionConsulta codigo = config.validarCodigoParaConsultaModelo();
        return codigo != null ? codigo : ConfiguracionAPI.CodigoValidacionConsulta.CONFIGURACION_NULA;
    }
    
    public String abreviarHash(String hashSolicitud) {
        if (Normalizador.esVacio(hashSolicitud)) {
            return "";
        }
        return hashSolicitud.substring(0, Math.min(8, hashSolicitud.length()));
    }
}
