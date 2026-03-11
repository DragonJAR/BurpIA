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

import java.util.ArrayList;
import java.util.List;

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
        if (respuestaRecibida == null) {
            return "";
        }
        try {
            return obtenerContentTypeDesdeHeaders(respuestaRecibida.headers());
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

    public ResumenSolicitudContextual inspeccionarSolicitudContextual(HttpRequestResponse solicitudRespuesta) {
        if (solicitudRespuesta == null) {
            return ResumenSolicitudContextual.invalido();
        }

        HttpRequest solicitud = solicitudRespuesta.request();
        if (solicitud == null) {
            return ResumenSolicitudContextual.invalido();
        }

        String url = obtenerUrlSegura(solicitud);
        String metodo = obtenerMetodoSeguro(solicitud);
        int cantidadEncabezados = 0;
        try {
            if (solicitud.headers() != null) {
                cantidadEncabezados = solicitud.headers().size();
            }
        } catch (Exception e) {
            registrarErrorInspeccionContextual("No se pudo contar encabezados de solicitud", e);
        }

        boolean enScope = estaEnScope(solicitud);
        boolean recursoEstatico = esRecursoEstatico(url);

        HttpResponse respuesta = obtenerResponseSegura(solicitudRespuesta);
        boolean tieneResponse = respuesta != null;
        int codigoEstado = -1;
        String contentType = "";
        Boolean contenidoAnalizable = null;
        if (tieneResponse) {
            try {
                codigoEstado = respuesta.statusCode();
            } catch (Exception e) {
                registrarErrorInspeccionContextual("No se pudo obtener codigo de estado de respuesta", e);
            }
            try {
                contentType = obtenerContentTypeDesdeHeaders(respuesta.headers());
            } catch (Exception e) {
                registrarErrorInspeccionContextual("No se pudo extraer Content-Type de respuesta", e);
            }
            contenidoAnalizable = esContenidoAnalizable(contentType, metodo, codigoEstado);
        }

        String hashAbreviado;
        try {
            String hashCompleto = tieneResponse
                ? HttpUtils.generarHashRapido(solicitud, respuesta)
                : HttpUtils.generarHashPartes(
                    metodo,
                    url,
                    HttpUtils.extraerEncabezados(solicitud),
                    HttpUtils.extraerCuerpo(solicitud, PoliticaMemoria.MAXIMO_CUERPO_ANALISIS_CARACTERES)
                );
            hashAbreviado = abreviarHash(hashCompleto);
        } catch (Exception e) {
            registrarErrorInspeccionContextual("No se pudo calcular hash contextual", e);
            hashAbreviado = "";
        }

        return new ResumenSolicitudContextual(
            true,
            url,
            metodo,
            true,
            tieneResponse,
            codigoEstado,
            contentType,
            cantidadEncabezados,
            hashAbreviado,
            enScope,
            recursoEstatico,
            contenidoAnalizable
        );
    }

    public List<String> construirTrazasDetalleContextual(ResumenSolicitudContextual resumen) {
        List<String> trazas = new ArrayList<>();
        if (resumen == null || !resumen.tieneRequest()) {
            return trazas;
        }

        trazas.add(I18nLogs.ContextoMenu.RESPUESTA_OBSERVADA(
            resumen.obtenerMetodo(),
            resumen.obtenerUrl(),
            resumen.obtenerCodigoEstado(),
            resumen.tieneResponse()
        ));
        if (!resumen.tieneResponse()) {
            trazas.add(I18nLogs.ContextoMenu.RESPONSE_AUSENTE(resumen.obtenerMetodo(), resumen.obtenerUrl()));
        }

        trazas.add(resumen.estaEnScope()
            ? I18nLogs.ContextoMenu.SCOPE_DENTRO(resumen.obtenerMetodo(), resumen.obtenerUrl())
            : I18nLogs.ContextoMenu.SCOPE_FUERA(resumen.obtenerMetodo(), resumen.obtenerUrl()));

        if (resumen.esRecursoEstatico()) {
            trazas.add(I18nLogs.ContextoMenu.RECURSO_ESTATICO_OBSERVADO(resumen.obtenerUrl()));
        }
        if (resumen.obtenerContenidoAnalizable() != null && !resumen.obtenerContenidoAnalizable()) {
            trazas.add(I18nLogs.ContextoMenu.CONTENIDO_NO_ANALIZABLE_OBSERVADO(
                resumen.obtenerUrl(),
                Normalizador.noEsVacio(resumen.obtenerContentType()) ? resumen.obtenerContentType() : "desconocido"
            ));
        }

        trazas.add(I18nLogs.ContextoMenu.HASH_SOLICITUD(resumen.obtenerHashAbreviado()));
        trazas.add(I18nLogs.ContextoMenu.DETALLES_SOLICITUD(
            resumen.obtenerMetodo(),
            resumen.obtenerUrl(),
            resumen.obtenerCantidadEncabezados(),
            resumen.obtenerCodigoEstado()
        ));
        return trazas;
    }

    public int contarSolicitudesSinRequest(List<HttpRequestResponse> solicitudes) {
        if (Normalizador.esVacia(solicitudes)) {
            return 0;
        }
        int cantidad = 0;
        for (HttpRequestResponse solicitud : solicitudes) {
            if (solicitud == null || solicitud.request() == null) {
                cantidad++;
            }
        }
        return cantidad;
    }

    public int contarSolicitudesSinResponse(List<HttpRequestResponse> solicitudes) {
        if (Normalizador.esVacia(solicitudes)) {
            return 0;
        }
        int cantidad = 0;
        for (HttpRequestResponse solicitud : solicitudes) {
            if (solicitud == null || solicitud.request() == null) {
                continue;
            }
            if (!tieneResponseDisponible(solicitud)) {
                cantidad++;
            }
        }
        return cantidad;
    }

    public boolean tieneResponseDisponible(HttpRequestResponse solicitudRespuesta) {
        return obtenerResponseSegura(solicitudRespuesta) != null;
    }

    private HttpResponse obtenerResponseSegura(HttpRequestResponse solicitudRespuesta) {
        if (solicitudRespuesta == null) {
            return null;
        }
        try {
            if (solicitudRespuesta.hasResponse()) {
                HttpResponse respuesta = solicitudRespuesta.response();
                if (respuesta != null) {
                    return respuesta;
                }
            }
        } catch (Exception e) {
            registrarErrorInspeccionContextual("No se pudo inspeccionar response contextual", e);
        }
        try {
            return solicitudRespuesta.response();
        } catch (Exception e) {
            registrarErrorInspeccionContextual("No se pudo obtener response contextual", e);
            return null;
        }
    }

    private String obtenerContentTypeDesdeHeaders(List<HttpHeader> headers) {
        if (headers == null) {
            return "";
        }
        for (HttpHeader header : headers) {
            if (header == null || header.name() == null) {
                continue;
            }
            if ("Content-Type".equalsIgnoreCase(header.name())) {
                return header.value() != null ? header.value() : "";
            }
        }
        return "";
    }

    private void registrarErrorInspeccionContextual(String mensaje, Exception e) {
        if (gestorLogging != null) {
            gestorLogging.error(ORIGEN_LOG, I18nLogs.tr(mensaje), e);
        }
    }

    public static final class ResumenSolicitudContextual {
        private final boolean valida;
        private final String url;
        private final String metodo;
        private final boolean tieneRequest;
        private final boolean tieneResponse;
        private final int codigoEstado;
        private final String contentType;
        private final int cantidadEncabezados;
        private final String hashAbreviado;
        private final boolean enScope;
        private final boolean recursoEstatico;
        private final Boolean contenidoAnalizable;

        private ResumenSolicitudContextual(boolean valida, String url, String metodo, boolean tieneRequest,
                boolean tieneResponse, int codigoEstado, String contentType, int cantidadEncabezados,
                String hashAbreviado, boolean enScope, boolean recursoEstatico, Boolean contenidoAnalizable) {
            this.valida = valida;
            this.url = url != null ? url : "";
            this.metodo = metodo != null ? metodo : "";
            this.tieneRequest = tieneRequest;
            this.tieneResponse = tieneResponse;
            this.codigoEstado = codigoEstado;
            this.contentType = contentType != null ? contentType : "";
            this.cantidadEncabezados = cantidadEncabezados;
            this.hashAbreviado = hashAbreviado != null ? hashAbreviado : "";
            this.enScope = enScope;
            this.recursoEstatico = recursoEstatico;
            this.contenidoAnalizable = contenidoAnalizable;
        }

        private static ResumenSolicitudContextual invalido() {
            return new ResumenSolicitudContextual(false, "", "", false, false, -1, "", 0, "", false, false, null);
        }

        public boolean esValida() {
            return valida;
        }

        public String obtenerUrl() {
            return url;
        }

        public String obtenerMetodo() {
            return metodo;
        }

        public boolean tieneRequest() {
            return tieneRequest;
        }

        public boolean tieneResponse() {
            return tieneResponse;
        }

        public int obtenerCodigoEstado() {
            return codigoEstado;
        }

        public String obtenerContentType() {
            return contentType;
        }

        public int obtenerCantidadEncabezados() {
            return cantidadEncabezados;
        }

        public String obtenerHashAbreviado() {
            return hashAbreviado;
        }

        public boolean estaEnScope() {
            return enScope;
        }

        public boolean esRecursoEstatico() {
            return recursoEstatico;
        }

        public Boolean obtenerContenidoAnalizable() {
            return contenidoAnalizable;
        }
    }
}
