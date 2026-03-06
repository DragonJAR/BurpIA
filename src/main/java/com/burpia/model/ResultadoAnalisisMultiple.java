package com.burpia.model;

import burp.api.montoya.http.message.requests.HttpRequest;
import com.burpia.util.Normalizador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Resultado de un análisis de seguridad que puede incluir múltiples hallazgos.
 * <p>
 * Esta clase encapsula el resultado de analizar una solicitud HTTP con uno o más
 * proveedores de IA, incluyendo los hallazgos detectados y cualquier error parcial
 * durante el análisis multi-proveedor.
 * </p>
 * <p>
 * La clase es inmutable: todas las listas se copian defensivamente y se exponen
 * como listas no modificables.
 * </p>
 */
public class ResultadoAnalisisMultiple {
    private final String url;
    private final List<Hallazgo> hallazgos;
    private final HttpRequest solicitudHttp;
    private final List<String> proveedoresFallidos;

    /**
     * Constructor completo con información de errores parciales durante análisis multi-proveedor.
     *
     * @param url                  URL analizada (puede ser null o vacía)
     * @param hallazgos            Lista de hallazgos encontrados (puede ser null o vacía)
     * @param solicitudHttp        Solicitud HTTP analizada (puede ser null)
     * @param proveedoresFallidos  Lista de proveedores que fallaron durante el análisis (puede ser null o vacía)
     */
    public ResultadoAnalisisMultiple(String url,
                                      List<Hallazgo> hallazgos,
                                      HttpRequest solicitudHttp,
                                      List<String> proveedoresFallidos) {
        this.url = Normalizador.noEsVacio(url) ? url : "";
        this.hallazgos = hallazgos != null ? new ArrayList<>(hallazgos) : new ArrayList<>();
        this.solicitudHttp = solicitudHttp;
        this.proveedoresFallidos = proveedoresFallidos != null
            ? new ArrayList<>(proveedoresFallidos)
            : new ArrayList<>();
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

    /**
     * Obtiene la severidad máxima de todos los hallazgos.
     * <p>
     * Los hallazgos nulos en la lista son ignorados.
     * </p>
     *
     * @return La severidad máxima encontrada, o "Info" si no hay hallazgos válidos
     */
    public String obtenerSeveridadMaxima() {
        return hallazgos.stream()
            .filter(Objects::nonNull)
            .map(Hallazgo::obtenerSeveridad)
            .max((s1, s2) -> Integer.compare(
                Hallazgo.obtenerPesoSeveridad(s1),
                Hallazgo.obtenerPesoSeveridad(s2)))
            .orElse(Hallazgo.SEVERIDAD_INFO);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResultadoAnalisisMultiple other = (ResultadoAnalisisMultiple) o;
        return Objects.equals(url, other.url) &&
               Objects.equals(hallazgos, other.hallazgos) &&
               Objects.equals(proveedoresFallidos, other.proveedoresFallidos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, hallazgos, proveedoresFallidos);
    }

    @Override
    public String toString() {
        return "ResultadoAnalisisMultiple{" +
               "url='" + url + '\'' +
               ", hallazgos=" + hallazgos.size() +
               ", proveedoresFallidos=" + proveedoresFallidos +
               '}';
    }
}
