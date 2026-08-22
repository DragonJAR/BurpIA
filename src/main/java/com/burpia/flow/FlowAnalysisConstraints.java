package com.burpia.flow;

import burp.api.montoya.http.message.HttpRequestResponse;
import com.burpia.util.Normalizador;

import java.util.ArrayList;
import java.util.List;

/**
 * Reglas compartidas para selección de flujos en BurpIA.
 */
public final class FlowAnalysisConstraints {
    public static final int MINIMO_PETICIONES_FLUJO = 2;
    public static final int MAXIMO_PETICIONES_FLUJO = 4;

    private FlowAnalysisConstraints() {
    }

    public static List<HttpRequestResponse> filtrarSolicitudesValidas(List<HttpRequestResponse> seleccion) {
        List<HttpRequestResponse> solicitudesValidas = new ArrayList<>();
        if (Normalizador.esVacia(seleccion)) {
            return solicitudesValidas;
        }
        for (HttpRequestResponse rr : seleccion) {
            if (esSolicitudValida(rr)) {
                solicitudesValidas.add(rr);
            }
        }
        return solicitudesValidas;
    }

    public static boolean tieneMinimoValido(List<HttpRequestResponse> seleccion) {
        return contarSolicitudesValidas(seleccion) >= MINIMO_PETICIONES_FLUJO;
    }

    public static boolean excedeMaximoValido(List<HttpRequestResponse> seleccion) {
        return contarSolicitudesValidas(seleccion) > MAXIMO_PETICIONES_FLUJO;
    }

    public static int contarSolicitudesValidas(List<HttpRequestResponse> seleccion) {
        if (Normalizador.esVacia(seleccion)) {
            return 0;
        }
        int count = 0;
        for (HttpRequestResponse rr : seleccion) {
            if (esSolicitudValida(rr)) {
                count++;
            }
        }
        return count;
    }

    private static boolean esSolicitudValida(HttpRequestResponse rr) {
        return rr != null && rr.request() != null;
    }
}
