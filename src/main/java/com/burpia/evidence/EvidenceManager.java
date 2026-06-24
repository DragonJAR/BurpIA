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
    
    private static final GestorLoggingUnificado GESTOR_LOGGING = GestorLoggingUnificado.crearMinimal(null, null);
    
    private final AlmacenEvidenciaHttp almacenEvidencia;
    private final AtomicLong contadorEvidencias;
    private final boolean esBurpProfessional;
    
    public EvidenceManager(MontoyaApi api) {
        this.almacenEvidencia = new AlmacenEvidenciaHttp();
        this.contadorEvidencias = new AtomicLong(0);
        this.esBurpProfessional = ExtensionBurpIA.esBurpProfessional(api);
    }
    
    public String almacenarEvidencia(HttpRequestResponse evidencia) {
        if (evidencia == null) {
            GESTOR_LOGGING.warning(ORIGEN_LOG, I18nLogs.Evidence.EVIDENCIA_NULA());
            return null;
        }
        
        try {
            String evidenciaId = almacenEvidencia.guardar(evidencia);
            if (Normalizador.noEsVacio(evidenciaId)) {
                contadorEvidencias.incrementAndGet();
                GESTOR_LOGGING.info(ORIGEN_LOG, I18nLogs.Evidence.EVIDENCIA_ALMACENADA() + abreviarId(evidenciaId));
            }
            return evidenciaId;
        } catch (Exception e) {
            GESTOR_LOGGING.error(ORIGEN_LOG, I18nLogs.Evidence.ERROR_ALMACENAR(), e);
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
            GESTOR_LOGGING.error(ORIGEN_LOG, I18nLogs.Evidence.ERROR_OBTENER() + abreviarId(evidenciaId), e);
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
                GESTOR_LOGGING.info(ORIGEN_LOG, I18nLogs.Evidence.EVIDENCIA_ELIMINADA() + abreviarId(evidenciaId));
            }
        } catch (Exception e) {
            GESTOR_LOGGING.error(ORIGEN_LOG, I18nLogs.Evidence.ERROR_ELIMINAR() + abreviarId(evidenciaId), e);
        }
    }
    
    public boolean guardarHallazgoComoIssue(MontoyaApi api, Hallazgo hallazgo, String evidenciaId) {
        if (hallazgo == null) {
            GESTOR_LOGGING.warning(ORIGEN_LOG, I18nLogs.Evidence.HALLAZGO_NULO_ISSUE());
            return false;
        }
        
        if (!esBurpProfessional) {
            GESTOR_LOGGING.warning(ORIGEN_LOG, I18nLogs.Evidence.ISSUES_SOLO_PRO());
            return false;
        }

        try {
            // Resolvemos la evidencia completa si existe (request+response); si no,
            // ExtensionBurpIA.resolverEvidenciaIssue sintetiza un par desde el request
            // para que Burp ancle el issue al nodo del Site Map correspondiente.
            HttpRequestResponse evidencia = obtenerEvidenciaParaIssue(hallazgo, evidenciaId);

            boolean guardado = ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(api, hallazgo, evidencia);
            if (guardado) {
                GESTOR_LOGGING.info(ORIGEN_LOG, I18nLogs.Evidence.AUDIT_ISSUE_CREADO() + hallazgo.obtenerTitulo());
            } else {
                GESTOR_LOGGING.warning(ORIGEN_LOG, I18nLogs.Evidence.AUDIT_ISSUE_NO_CREADO());
            }
            return guardado;
        } catch (Exception e) {
            GESTOR_LOGGING.error(ORIGEN_LOG, I18nLogs.Evidence.ERROR_GUARDAR_ISSUE(), e);
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
            
            String evidenciaId = hallazgo.obtenerEvidenciaId();
            if (guardarHallazgoComoIssue(api, hallazgo, evidenciaId)) {
                guardados++;
            }
        }
        
        if (guardados > 0) {
            GESTOR_LOGGING.info(ORIGEN_LOG, I18nLogs.Evidence.AUDIT_ISSUES_CREADOS(guardados, hallazgos.size()));
        }
    }
    
    public void limpiarEvidenciasAntiguas() {
        try {
            almacenEvidencia.limpiarCacheMemoria();
            GESTOR_LOGGING.info(ORIGEN_LOG, I18nLogs.Evidence.CACHE_LIMPIADO());
        } catch (Exception e) {
            GESTOR_LOGGING.error(ORIGEN_LOG, I18nLogs.Evidence.ERROR_LIMPIAR(), e);
        }
    }
    
    public long obtenerContadorEvidencias() {
        return contadorEvidencias.get();
    }
    
    private HttpRequestResponse obtenerEvidenciaParaIssue(Hallazgo hallazgo, String evidenciaId) {
        if (hallazgo == null) {
            return null;
        }

        // Re-resolución desde el almacén (cache LRU → disco). La síntesis final a
        // partir del request cuando no hay evidencia completa la centraliza
        // ExtensionBurpIA.resolverEvidenciaIssue (DRY), que es el punto terminal.
        HttpRequestResponse evidenciaDirecta = hallazgo.obtenerEvidenciaHttp();
        if (evidenciaDirecta != null) {
            return evidenciaDirecta;
        }

        if (Normalizador.noEsVacio(evidenciaId)) {
            return obtenerEvidencia(evidenciaId);
        }

        return null;
    }
    
    private String abreviarId(String id) {
        if (Normalizador.esVacio(id)) {
            return "";
        }
        return id.length() > 8 ? id.substring(0, 8) + "..." : id;
    }
}