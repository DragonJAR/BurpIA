package com.burpia.evidence;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import com.burpia.ExtensionBurpIA;
import com.burpia.i18n.I18nLogs;
import com.burpia.model.Hallazgo;
import com.burpia.util.AlmacenEvidenciaHttp;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.Normalizador;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class EvidenceManager {

    private static final String ORIGEN_LOG = "EvidenceManager";

    private final GestorLoggingUnificado gestorLogging;

    private final AlmacenEvidenciaHttp almacenEvidencia;
    private final AtomicLong contadorEvidencias;
    private final boolean esBurpProfessional;

    public EvidenceManager(MontoyaApi api) {
        // Logger vivo (api real) para que la ruta de evidencia/issues sea visible en
        // Extensions -> Output/Errors. Antes era crearMinimal(null,null) => api null =>
        // logToBurpApi descartaba todo (fallo silencioso, indepurable).
        this.gestorLogging = GestorLoggingUnificado.crear(null, null, null, api, null);
        this.almacenEvidencia = new AlmacenEvidenciaHttp();
        this.contadorEvidencias = new AtomicLong(0);
        this.esBurpProfessional = ExtensionBurpIA.esBurpProfessional(api);
    }
    
    public String almacenarEvidencia(HttpRequestResponse evidencia) {
        if (evidencia == null) {
            gestorLogging.warning(ORIGEN_LOG, I18nLogs.Evidence.EVIDENCIA_NULA());
            return null;
        }
        
        try {
            String evidenciaId = almacenEvidencia.guardar(evidencia);
            if (Normalizador.noEsVacio(evidenciaId)) {
                contadorEvidencias.incrementAndGet();
                gestorLogging.info(ORIGEN_LOG, I18nLogs.Evidence.EVIDENCIA_ALMACENADA() + abreviarId(evidenciaId));
            }
            return evidenciaId;
        } catch (Exception e) {
            gestorLogging.error(ORIGEN_LOG, I18nLogs.Evidence.ERROR_ALMACENAR(), e);
            return null;
        }
    }
    
    public HttpRequestResponse obtenerEvidencia(String evidenciaId) {
        if (Normalizador.esVacio(evidenciaId)) {
            return null;
        }
        
        try {
            return almacenEvidencia.obtener(evidenciaId);
        } catch (Exception e) {
            gestorLogging.error(ORIGEN_LOG, I18nLogs.Evidence.ERROR_OBTENER() + abreviarId(evidenciaId), e);
            return null;
        }
    }
    
    public void eliminarEvidencia(String evidenciaId) {
        if (Normalizador.esVacio(evidenciaId)) {
            return;
        }
        
        try {
            boolean eliminada = almacenEvidencia.eliminar(evidenciaId);
            if (eliminada) {
                contadorEvidencias.decrementAndGet();
                gestorLogging.info(ORIGEN_LOG, I18nLogs.Evidence.EVIDENCIA_ELIMINADA() + abreviarId(evidenciaId));
            }
        } catch (Exception e) {
            gestorLogging.error(ORIGEN_LOG, I18nLogs.Evidence.ERROR_ELIMINAR() + abreviarId(evidenciaId), e);
        }
    }
    
    public boolean guardarHallazgoComoIssue(MontoyaApi api, Hallazgo hallazgo) {
        if (hallazgo == null) {
            gestorLogging.warning(ORIGEN_LOG, I18nLogs.Evidence.HALLAZGO_NULO_ISSUE());
            return false;
        }

        if (!esBurpProfessional) {
            gestorLogging.warning(ORIGEN_LOG, I18nLogs.Evidence.ISSUES_SOLO_PRO());
            return false;
        }

        try {
            // El issue se arma solo con los campos editables del hallazgo (sin evidencia
            // HTTP adjunta). La evidencia sigue accesible desde la UI de BurpIA.
            boolean guardado = ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(api, hallazgo);
            if (guardado) {
                gestorLogging.info(ORIGEN_LOG, I18nLogs.Evidence.AUDIT_ISSUE_CREADO() + hallazgo.obtenerTitulo());
            } else {
                gestorLogging.warning(ORIGEN_LOG, I18nLogs.Evidence.AUDIT_ISSUE_NO_CREADO());
            }
            return guardado;
        } catch (Exception e) {
            gestorLogging.error(ORIGEN_LOG, I18nLogs.Evidence.ERROR_GUARDAR_ISSUE(), e);
            return false;
        }
    }
    
    public void guardarHallazgosComoIssues(MontoyaApi api, List<Hallazgo> hallazgos) {
        if (Normalizador.esVacia(hallazgos)) {
            return;
        }
        
        int guardados = 0;
        for (Hallazgo hallazgo : hallazgos) {
            if (hallazgo == null) {
                continue;
            }
            
            if (guardarHallazgoComoIssue(api, hallazgo)) {
                guardados++;
            }
        }
        
        if (guardados > 0) {
            gestorLogging.info(ORIGEN_LOG, I18nLogs.Evidence.AUDIT_ISSUES_CREADOS(guardados, hallazgos.size()));
        }
    }
    
    public void limpiarEvidenciasAntiguas() {
        try {
            almacenEvidencia.limpiarCacheMemoria();
            gestorLogging.info(ORIGEN_LOG, I18nLogs.Evidence.CACHE_LIMPIADO());
        } catch (Exception e) {
            gestorLogging.error(ORIGEN_LOG, I18nLogs.Evidence.ERROR_LIMPIAR(), e);
        }
    }
    
    public long obtenerContadorEvidencias() {
        return contadorEvidencias.get();
    }
    
    private String abreviarId(String id) {
        if (Normalizador.esVacio(id)) {
            return "";
        }
        return id.length() > 8 ? id.substring(0, 8) + "..." : id;
    }
}