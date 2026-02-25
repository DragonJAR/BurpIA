package com.burpia.model;
import burp.api.montoya.http.message.requests.HttpRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResultadoAnalisisMultiple {
    private final String url;
    private final String marcaTiempo;
    private final List<Hallazgo> hallazgos;
    private final HttpRequest solicitudHttp;

    public ResultadoAnalisisMultiple(String url, String marcaTiempo, List<Hallazgo> hallazgos) {
        this(url, marcaTiempo, hallazgos, null);
    }

    public ResultadoAnalisisMultiple(String url, String marcaTiempo, List<Hallazgo> hallazgos, HttpRequest solicitudHttp) {
        this.url = url;
        this.marcaTiempo = marcaTiempo;
        this.hallazgos = hallazgos != null ? new ArrayList<>(hallazgos) : new ArrayList<>();
        this.solicitudHttp = solicitudHttp;
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
