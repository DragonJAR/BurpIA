package com.burpia.evidence;

import burp.api.montoya.MontoyaApi;
import com.burpia.ExtensionBurpIA;
import com.burpia.i18n.I18nLogs;
import com.burpia.model.Hallazgo;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.Normalizador;

import java.util.List;

public class EvidenceManager {

    private static final String ORIGEN_LOG = "EvidenceManager";

    private final GestorLoggingUnificado gestorLogging;
    private final boolean esBurpProfessional;

    public EvidenceManager(MontoyaApi api) {
        // Logger vivo (api real) para que la ruta de issues sea visible en
        // Extensions -> Output/Errors.
        this.gestorLogging = GestorLoggingUnificado.crear(null, null, null, api, null);
        this.esBurpProfessional = ExtensionBurpIA.esBurpProfessional(api);
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
}
