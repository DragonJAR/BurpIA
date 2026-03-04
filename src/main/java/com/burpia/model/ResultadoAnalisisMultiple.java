package com.burpia.model;
import burp.api.montoya.http.message.requests.HttpRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResultadoAnalisisMultiple {
    private final String url;
    private final List<Hallazgo> hallazgos;
    private final HttpRequest solicitudHttp;
    private final List<String> proveedoresFallidos;

    /**
     * Constructor completo con información de errores parciales durante análisis multi-proveedor.
     *
     * @param url URL analizada
     * @param hallazgos Lista de hallazgos encontrados (puede estar vacía)
     * @param solicitudHttp Solicitud HTTP analizada
     * @param proveedoresFallidos Lista de proveedores que fallaron durante el análisis (puede ser null o vacía)
     */
    public ResultadoAnalisisMultiple(String url,
                                      List<Hallazgo> hallazgos,
                                      HttpRequest solicitudHttp,
                                      List<String> proveedoresFallidos) {
        this.url = url;
        this.hallazgos = hallazgos != null ? new ArrayList<>(hallazgos) : new ArrayList<>();
        this.solicitudHttp = solicitudHttp;
        this.proveedoresFallidos = proveedoresFallidos != null
            ? new ArrayList<>(proveedoresFallidos)
            : new ArrayList<>();
    }

    /**
     * Constructor para compatibilidad con código existente.
     * Equivalentes a proveedoresFallidos = vacío (sin errores).
     */
    public ResultadoAnalisisMultiple(String url, List<Hallazgo> hallazgos, HttpRequest solicitudHttp) {
        this(url, hallazgos, solicitudHttp, null);
    }

    public String obtenerUrl() {
        return url;
    }

    public List<Hallazgo> obtenerHallazgos() {
        return Collections.unmodifiableList(hallazgos);
    }

    public HttpRequest obtenerSolicitudHttp() {
        return solicitudHttp;
    }

    /**
     * Retorna la lista inmutable de proveedores que fallaron durante el análisis multi-proveedor.
     *
     * @return Lista inmutable de nombres de proveedores que fallaron, vacía si no hubo errores
     */
    public List<String> obtenerProveedoresFallidos() {
        return Collections.unmodifiableList(proveedoresFallidos);
    }

    /**
     * Indica si hubo errores parciales durante el análisis multi-proveedor.
     *
     * @return true si al menos un proveedor falló, false en caso contrario
     */
    public boolean huboErroresParciales() {
        return !proveedoresFallidos.isEmpty();
    }

    public int obtenerNumeroHallazgos() {
        return hallazgos.size();
    }

    public String obtenerSeveridadMaxima() {
        if (hallazgos.isEmpty()) {
            return "Info";
        }

        int maxPeso = -1;
        String severidadMax = "Info";

        for (Hallazgo hallazgo : hallazgos) {
            if (hallazgo == null) {
                continue;
            }
            int peso = Hallazgo.obtenerPesoSeveridad(hallazgo.obtenerSeveridad());
            if (peso > maxPeso) {
                maxPeso = peso;
                severidadMax = hallazgo.obtenerSeveridad();
            }
        }

        return severidadMax;
    }
}
