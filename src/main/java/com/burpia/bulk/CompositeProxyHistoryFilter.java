package com.burpia.bulk;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.proxy.ProxyHttpRequestResponse;
import com.burpia.util.Normalizador;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Composite filter for proxy history items using multiple criteria.
 * <p>
 * Implements Builder pattern for flexible filter construction with AND logic
 * (all configured criteria must match for item to pass filter).
 * </p>
 */
public class CompositeProxyHistoryFilter implements ProxyHistoryFilter {
    
    private final List<CriteriaFiltro> criterios;
    private final String descripcion;
    
    private CompositeProxyHistoryFilter(Builder builder) {
        this.criterios = new ArrayList<>(builder.criterios);
        this.descripcion = builder.construirDescripcion();
    }
    
    @Override
    public boolean matches(ProxyHttpRequestResponse item) {
        if (item == null) {
            return false;
        }
        
        if (item.request() == null) {
            return false;
        }
        
        for (CriteriaFiltro criteria : criterios) {
            if (!criteria.matches(item)) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public String getDescription() {
        return descripcion;
    }
    
    /**
     * Creates a new Builder for constructing CompositeProxyHistoryFilter instances.
     *
     * @return new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for CompositeProxyHistoryFilter.
     */
    public static class Builder {
        private final List<CriteriaFiltro> criterios = new ArrayList<>();
        
        /**
         * Filters by HTTP method (e.g., "GET", "POST").
         *
         * @param metodo HTTP method to filter by
         * @return this builder for chaining
         */
        public Builder metodo(String metodo) {
            if (Normalizador.noEsVacio(metodo)) {
                criterios.add(new CriterioMetodo(metodo.trim().toUpperCase()));
            }
            return this;
        }
        
        /**
         * Filters by host pattern (regex).
         *
         * @param patronHost regex pattern for host matching
         * @return this builder for chaining
         */
        public Builder patronHost(String patronHost) {
            if (Normalizador.noEsVacio(patronHost)) {
                criterios.add(new CriterioPatronHost(patronHost.trim()));
            }
            return this;
        }
        
        /**
         * Filters by path pattern (regex).
         *
         * @param patronPath regex pattern for path matching
         * @return this builder for chaining
         */
        public Builder patronPath(String patronPath) {
            if (Normalizador.noEsVacio(patronPath)) {
                criterios.add(new CriterioPatronPath(patronPath.trim()));
            }
            return this;
        }
        
        /**
         * Filters by response status code (e.g., 200, 404, 500).
         *
         * @param codigoEstado HTTP status code to filter by
         * @return this builder for chaining
         */
        public Builder codigoEstado(int codigoEstado) {
            if (codigoEstado > 0) {
                criterios.add(new CriterioCodigoEstado(codigoEstado));
            }
            return this;
        }
        
        /**
         * Filters by response status code range (inclusive).
         *
         * @param codigoMin minimum status code (inclusive)
         * @param codigoMax maximum status code (inclusive)
         * @return this builder for chaining
         */
        public Builder rangoCodigoEstado(int codigoMin, int codigoMax) {
            if (codigoMin > 0 && codigoMax >= codigoMin) {
                criterios.add(new CriterioRangoCodigoEstado(codigoMin, codigoMax));
            }
            return this;
        }
        
        /**
         * Filters by content type in response (e.g., "application/json").
         *
         * @param tipoContenido content type to filter by
         * @return this builder for chaining
         */
        public Builder tipoContenido(String tipoContenido) {
            if (Normalizador.noEsVacio(tipoContenido)) {
                criterios.add(new CriterioTipoContenido(tipoContenido.trim().toLowerCase()));
            }
            return this;
        }
        
        /**
         * Builds the CompositeProxyHistoryFilter instance.
         *
         * @return new filter instance with configured criteria
         */
        public CompositeProxyHistoryFilter build() {
            return new CompositeProxyHistoryFilter(this);
        }
        
        private String construirDescripcion() {
            if (criterios.isEmpty()) {
                return "Sin filtros";
            }
            
            StringBuilder sb = new StringBuilder("Filtros: ");
            for (int i = 0; i < criterios.size(); i++) {
                if (i > 0) {
                    sb.append(" AND ");
                }
                sb.append(criterios.get(i).descripcion());
            }
            return sb.toString();
        }
    }
    
    private interface CriteriaFiltro {
        boolean matches(ProxyHttpRequestResponse item);
        String descripcion();
    }
    
    private static class CriterioMetodo implements CriteriaFiltro {
        private final String metodo;
        
        CriterioMetodo(String metodo) {
            this.metodo = metodo;
        }
        
        @Override
        public boolean matches(ProxyHttpRequestResponse item) {
            HttpRequest request = item.request();
            return request != null && metodo.equals(request.method());
        }
        
        @Override
        public String descripcion() {
            return "método=" + metodo;
        }
    }
    
    private static class CriterioPatronHost implements CriteriaFiltro {
        private final Pattern patron;
        private final String patronStr;
        
        CriterioPatronHost(String patronStr) {
            this.patronStr = patronStr;
            this.patron = Pattern.compile(patronStr, Pattern.CASE_INSENSITIVE);
        }
        
        @Override
        public boolean matches(ProxyHttpRequestResponse item) {
            HttpRequest request = item.request();
            if (request == null || request.httpService() == null) {
                return false;
            }
            String host = request.httpService().host();
            return host != null && patron.matcher(host).matches();
        }
        
        @Override
        public String descripcion() {
            return "host~" + patronStr;
        }
    }
    
    private static class CriterioPatronPath implements CriteriaFiltro {
        private final Pattern patron;
        private final String patronStr;
        
        CriterioPatronPath(String patronStr) {
            this.patronStr = patronStr;
            this.patron = Pattern.compile(patronStr, Pattern.CASE_INSENSITIVE);
        }
        
        @Override
        public boolean matches(ProxyHttpRequestResponse item) {
            HttpRequest request = item.request();
            if (request == null) {
                return false;
            }
            String path = request.path();
            return path != null && patron.matcher(path).matches();
        }
        
        @Override
        public String descripcion() {
            return "path~" + patronStr;
        }
    }
    
    private static class CriterioCodigoEstado implements CriteriaFiltro {
        private final int codigo;
        
        CriterioCodigoEstado(int codigo) {
            this.codigo = codigo;
        }
        
        @Override
        public boolean matches(ProxyHttpRequestResponse item) {
            HttpResponse response = item.response();
            return response != null && response.statusCode() == codigo;
        }
        
        @Override
        public String descripcion() {
            return "status=" + codigo;
        }
    }
    
    private static class CriterioRangoCodigoEstado implements CriteriaFiltro {
        private final int codigoMin;
        private final int codigoMax;
        
        CriterioRangoCodigoEstado(int codigoMin, int codigoMax) {
            this.codigoMin = codigoMin;
            this.codigoMax = codigoMax;
        }
        
        @Override
        public boolean matches(ProxyHttpRequestResponse item) {
            HttpResponse response = item.response();
            if (response == null) {
                return false;
            }
            int codigo = response.statusCode();
            return codigo >= codigoMin && codigo <= codigoMax;
        }
        
        @Override
        public String descripcion() {
            return "status=" + codigoMin + "-" + codigoMax;
        }
    }
    
    private static class CriterioTipoContenido implements CriteriaFiltro {
        private final String tipoContenido;
        
        CriterioTipoContenido(String tipoContenido) {
            this.tipoContenido = tipoContenido;
        }
        
        @Override
        public boolean matches(ProxyHttpRequestResponse item) {
            HttpResponse response = item.response();
            if (response == null) {
                return false;
            }
            String contentType = response.headerValue("Content-Type");
            if (contentType == null) {
                return false;
            }
            return contentType.toLowerCase().contains(tipoContenido);
        }
        
        @Override
        public String descripcion() {
            return "content-type~" + tipoContenido;
        }
    }
}
