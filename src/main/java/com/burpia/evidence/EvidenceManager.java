package com.burpia.evidence;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import com.burpia.ExtensionBurpIA;
import com.burpia.model.Hallazgo;
import com.burpia.util.AlmacenEvidenciaHttp;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.Normalizador;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class EvidenceManager {
    
    private static final Logger LOGGER = Logger.getLogger(EvidenceManager.class.getName());
    private static final GestorLoggingUnificado gestorLogging = GestorLoggingUnificado.crearConLogger(LOGGER);
    
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
            gestorLogging.warning("EvidenceManager", "Intentando almacenar evidencia nula");
            return null;
        }
        
        try {
            String evidenciaId = almacenEvidencia.guardar(evidencia);
            if (Normalizador.noEsVacio(evidenciaId)) {
                contadorEvidencias.incrementAndGet();
                gestorLogging.info("EvidenceManager", "Evidencia almacenada: " + abreviarId(evidenciaId));
            }
            return evidenciaId;
        } catch (Exception e) {
            gestorLogging.error("EvidenceManager", "Error al almacenar evidencia", e);
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
            gestorLogging.error("EvidenceManager", "Error al obtener evidencia: " + abreviarId(evidenciaId), e);
            return null;
        }
    }
    
    public void eliminarEvidencia(String evidenciaId) {
        if (Normalizador.esVacio(evidenciaId)) {
            return;
        }
        
        try {
            almacenEvidencia.eliminar(evidenciaId);
            contadorEvidencias.decrementAndGet();
            gestorLogging.info("EvidenceManager", "Evidencia eliminada: " + abreviarId(evidenciaId));
        } catch (Exception e) {
            gestorLogging.error("EvidenceManager", "Error al eliminar evidencia: " + abreviarId(evidenciaId), e);
        }
    }
    
    public boolean guardarHallazgoComoIssue(MontoyaApi api, Hallazgo hallazgo, String evidenciaId) {
        if (hallazgo == null) {
            gestorLogging.warning("EvidenceManager", "Intentando guardar hallazgo nulo como Issue");
            return false;
        }
        
        if (!esBurpProfessional) {
            gestorLogging.info("EvidenceManager", "Integración con Issues solo disponible en Burp Professional");
            return false;
        }
        
        try {
            HttpRequestResponse evidencia = obtenerEvidenciaParaIssue(hallazgo, evidenciaId);
            if (evidencia == null) {
                gestorLogging.verbose("EvidenceManager", "Hallazgo sin evidencia HTTP: no se puede crear AuditIssue");
                return false;
            }
            
            boolean guardado = ExtensionBurpIA.guardarAuditIssueDesdeHallazgo(api, hallazgo, evidencia);
            if (guardado) {
                gestorLogging.info("EvidenceManager", "AuditIssue creado para hallazgo: " + hallazgo.obtenerTitulo());
            } else {
                gestorLogging.verbose("EvidenceManager", "AuditIssue no creado: hallazgo sin datos suficientes");
            }
            return guardado;
        } catch (Exception e) {
            gestorLogging.error("EvidenceManager", "Error al guardar hallazgo como Issue", e);
            return false;
        }
    }
    
    public void guardarHallazgosComoIssues(MontoyaApi api, List<Hallazgo> hallazgos) {
        if (hallazgos == null || hallazgos.isEmpty()) {
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
            gestorLogging.info("EvidenceManager", "Se crearon " + guardados + " AuditIssues de " + hallazgos.size() + " hallazgos");
        }
    }
    
    public void limpiarEvidenciasAntiguas() {
        try {
            almacenEvidencia.limpiarCacheMemoria();
            gestorLogging.info("EvidenceManager", "Cache de memoria de evidencias limpiado");
        } catch (Exception e) {
            gestorLogging.error("EvidenceManager", "Error al limpiar evidencias antiguas", e);
        }
    }
    
    public long obtenerContadorEvidencias() {
        return contadorEvidencias.get();
    }
    
    private HttpRequestResponse obtenerEvidenciaParaIssue(Hallazgo hallazgo, String evidenciaId) {
        if (hallazgo == null) {
            return null;
        }
        
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