package com.burpia.config;

import com.burpia.i18n.I18nUI;
import com.burpia.i18n.IdiomaUI;

import java.util.HashMap;
import java.util.Map;

public class ConfiguracionAPI {
    public static final int MAXIMO_HALLAZGOS_TABLA_DEFECTO = 1000;
    public static final int MINIMO_HALLAZGOS_TABLA = 100;
    public static final int MAXIMO_HALLAZGOS_TABLA = 50000;
    public static final int MINIMO_RETRASO_SEGUNDOS = 0;
    public static final int MAXIMO_RETRASO_SEGUNDOS = 60;
    public static final int MINIMO_MAXIMO_CONCURRENTE = 1;
    public static final int MAXIMO_MAXIMO_CONCURRENTE = 10;

    private int retrasoSegundos;
    private int maximoConcurrente;
    private int maximoHallazgosTabla;
    private boolean detallado;
    private String proveedorAI;
    private int tiempoEsperaAI;
    private String tema;
    private String idiomaUi;
    private boolean escaneoPasivoHabilitado;
    private boolean autoGuardadoIssuesHabilitado;
    private boolean autoScrollConsolaHabilitado;
    private String promptConfigurable;

    private Map<String, String> apiKeysPorProveedor;
    private Map<String, String> urlsBasePorProveedor;
    private Map<String, String> modelosPorProveedor;
    private Map<String, Integer> maxTokensPorProveedor;
    private boolean promptModificado;

    public ConfiguracionAPI() {
        this.proveedorAI = "Z.ai";
        this.retrasoSegundos = normalizarRetrasoSegundos(5);
        this.maximoConcurrente = normalizarMaximoConcurrente(3);
        this.maximoHallazgosTabla = MAXIMO_HALLAZGOS_TABLA_DEFECTO;
        this.tiempoEsperaAI = 60;
        this.detallado = false;
        this.tema = "Light";
        this.idiomaUi = IdiomaUI.porDefecto().codigo();
        this.escaneoPasivoHabilitado = true;
        this.autoGuardadoIssuesHabilitado = true;
        this.autoScrollConsolaHabilitado = true;
        this.promptModificado = false;

        this.promptConfigurable = obtenerPromptPorDefecto();

        this.apiKeysPorProveedor = new HashMap<>();
        this.urlsBasePorProveedor = new HashMap<>();
        this.modelosPorProveedor = new HashMap<>();
        this.maxTokensPorProveedor = new HashMap<>();
    }

    public String obtenerUrlApi() {
        String proveedor = obtenerProveedorAI();
        return construirUrlApiProveedor(
            proveedor,
            obtenerUrlBaseParaProveedor(proveedor),
            obtenerModeloParaProveedor(proveedor)
        );
    }

    public void establecerUrlApi(String urlApi) {
        establecerUrlBaseParaProveedor(obtenerProveedorAI(), extraerUrlBase(urlApi));
    }

    public String obtenerClaveApi() {
        return obtenerApiKeyParaProveedor(obtenerProveedorAI());
    }

    public void establecerClaveApi(String claveApi) {
        establecerApiKeyParaProveedor(obtenerProveedorAI(), claveApi);
    }

    public String obtenerModelo() {
        return obtenerModeloParaProveedor(obtenerProveedorAI());
    }

    public void establecerModelo(String modelo) {
        establecerModeloParaProveedor(obtenerProveedorAI(), modelo);
    }

    public int obtenerRetrasoSegundos() { return retrasoSegundos; }
    public void establecerRetrasoSegundos(int retrasoSegundos) {
        this.retrasoSegundos = normalizarRetrasoSegundos(retrasoSegundos);
    }

    public int obtenerMaximoConcurrente() { return maximoConcurrente; }
    public void establecerMaximoConcurrente(int maximoConcurrente) {
        this.maximoConcurrente = normalizarMaximoConcurrente(maximoConcurrente);
    }

    public int obtenerMaximoHallazgosTabla() { return maximoHallazgosTabla; }
    public void establecerMaximoHallazgosTabla(int maximoHallazgosTabla) {
        this.maximoHallazgosTabla = normalizarMaximoHallazgos(maximoHallazgosTabla);
    }

    public boolean esDetallado() { return detallado; }
    public void establecerDetallado(boolean detallado) { this.detallado = detallado; }

    public String obtenerProveedorAI() { return proveedorAI; }
    public void establecerProveedorAI(String proveedorAI) {
        this.proveedorAI = (proveedorAI != null && ProveedorAI.existeProveedor(proveedorAI)) ? proveedorAI : "Z.ai";
        asegurarMapas();
    }

    public int obtenerTiempoEsperaAI() { return tiempoEsperaAI; }
    public void establecerTiempoEsperaAI(int tiempoEsperaAI) {
        this.tiempoEsperaAI = normalizarTiempoEspera(tiempoEsperaAI);
    }

    public String obtenerTema() { return tema; }
    public void establecerTema(String tema) { this.tema = normalizarTema(tema); }

    public String obtenerIdiomaUi() { return idiomaUi; }
    public void establecerIdiomaUi(String idiomaUi) { this.idiomaUi = IdiomaUI.desdeCodigo(idiomaUi).codigo(); }

    public boolean escaneoPasivoHabilitado() { return escaneoPasivoHabilitado; }
    public void establecerEscaneoPasivoHabilitado(boolean escaneoPasivoHabilitado) {
        this.escaneoPasivoHabilitado = escaneoPasivoHabilitado;
    }

    public boolean autoGuardadoIssuesHabilitado() { return autoGuardadoIssuesHabilitado; }
    public void establecerAutoGuardadoIssuesHabilitado(boolean autoGuardadoIssuesHabilitado) {
        this.autoGuardadoIssuesHabilitado = autoGuardadoIssuesHabilitado;
    }

    public boolean autoScrollConsolaHabilitado() { return autoScrollConsolaHabilitado; }
    public void establecerAutoScrollConsolaHabilitado(boolean autoScrollConsolaHabilitado) {
        this.autoScrollConsolaHabilitado = autoScrollConsolaHabilitado;
    }

    public boolean esPromptModificado() { return promptModificado; }
    public void establecerPromptModificado(boolean modificado) { this.promptModificado = modificado; }

    public static String construirUrlApiProveedor(String proveedor, String urlBase, String modelo) {
        String baseNormalizada = normalizarUrlBase(urlBase);
        String proveedorNormalizado = proveedor != null ? proveedor : "";
        String modeloNormalizado = (modelo != null && !modelo.trim().isEmpty()) ? modelo.trim() : "gemini-1.5-pro-002";

        switch (proveedorNormalizado) {
            case "Claude":
                return baseNormalizada + "/messages";
            case "Gemini":
                return baseNormalizada + "/models/" + modeloNormalizado + ":generateContent";
            case "Ollama":
                return baseNormalizada + "/api/chat";
            case "OpenAI":
                return baseNormalizada + "/responses";
            case "Z.ai":
            case "minimax":
            default:
                return baseNormalizada + "/chat/completions";
        }
    }

    public static String extraerUrlBase(String urlConfigurada) {
        return normalizarUrlBase(urlConfigurada);
    }

    private static String normalizarUrlBase(String urlBase) {
        String base = (urlBase == null) ? "" : urlBase.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        String[] sufijos = {
            "/responses",
            "/chat/completions",
            "/completions",
            "/messages",
            "/api/chat"
        };
        for (String sufijo : sufijos) {
            if (base.endsWith(sufijo)) {
                return base.substring(0, base.length() - sufijo.length());
            }
        }

        int idxGemini = base.indexOf("/models/");
        if (idxGemini > 0) {
            return base.substring(0, idxGemini);
        }

        return base;
    }

    public String obtenerApiKeyParaProveedor(String proveedor) {
        asegurarMapas();
        if (proveedor == null || proveedor.trim().isEmpty()) {
            return "";
        }
        String key = apiKeysPorProveedor.get(proveedor);
        return key != null ? key : "";
    }

    public void establecerApiKeyParaProveedor(String proveedor, String apiKey) {
        asegurarMapas();
        if (proveedor == null || proveedor.trim().isEmpty()) {
            return;
        }
        apiKeysPorProveedor.put(proveedor, apiKey);
    }

    public String obtenerUrlBaseParaProveedor(String proveedor) {
        asegurarMapas();
        if (proveedor == null || proveedor.trim().isEmpty()) {
            return "";
        }
        if (urlsBasePorProveedor.containsKey(proveedor)) {
            String urlGuardada = urlsBasePorProveedor.get(proveedor);
            if (urlGuardada != null && !urlGuardada.trim().isEmpty()) {
                return urlGuardada;
            }
        }
        String urlPorDefecto = ProveedorAI.obtenerUrlApiPorDefecto(proveedor, idiomaUi);
        return urlPorDefecto != null ? urlPorDefecto : "";
    }

    public void establecerUrlBaseParaProveedor(String proveedor, String urlBase) {
        asegurarMapas();
        if (proveedor == null || proveedor.trim().isEmpty()) {
            return;
        }
        urlsBasePorProveedor.put(proveedor, urlBase);
    }

    public String obtenerModeloParaProveedor(String proveedor) {
        asegurarMapas();
        if (proveedor == null || proveedor.trim().isEmpty()) {
            return "";
        }
        if (modelosPorProveedor.containsKey(proveedor)) {
            String modelo = modelosPorProveedor.get(proveedor);
            return modelo != null ? modelo : "";
        }
        ProveedorAI.ConfiguracionProveedor config = ProveedorAI.obtenerProveedor(proveedor);
        return config != null ? config.obtenerModeloPorDefecto() : "";
    }

    public void establecerModeloParaProveedor(String proveedor, String modelo) {
        asegurarMapas();
        if (proveedor == null || proveedor.trim().isEmpty()) {
            return;
        }
        modelosPorProveedor.put(proveedor, modelo != null ? modelo : "");
    }

    public String obtenerUrlBaseGuardadaParaProveedor(String proveedor) {
        asegurarMapas();
        if (proveedor == null || proveedor.trim().isEmpty()) {
            return null;
        }
        return urlsBasePorProveedor.get(proveedor);
    }

    public Integer obtenerMaxTokensConfiguradoParaProveedor(String proveedor) {
        asegurarMapas();
        if (proveedor == null || proveedor.trim().isEmpty()) {
            return null;
        }
        return maxTokensPorProveedor.get(proveedor);
    }

    public int obtenerMaxTokensParaProveedor(String proveedor) {
        asegurarMapas();
        if (proveedor == null || proveedor.trim().isEmpty()) {
            return 4096;
        }
        if (maxTokensPorProveedor.containsKey(proveedor)) {
            Integer valor = maxTokensPorProveedor.get(proveedor);
            if (valor != null && valor > 0) {
                return valor;
            }
        }
        return obtenerMaxTokensPorDefectoProveedor(proveedor);
    }

    public void establecerMaxTokensParaProveedor(String proveedor, int maxTokens) {
        asegurarMapas();
        if (proveedor == null || proveedor.trim().isEmpty()) {
            return;
        }
        int valorNormalizado = maxTokens > 0 ? maxTokens : obtenerMaxTokensPorDefectoProveedor(proveedor);
        maxTokensPorProveedor.put(proveedor, valorNormalizado);
    }

    public Map<String, String> validar() {
        Map<String, String> errores = new HashMap<>();

        if (proveedorAI == null || proveedorAI.trim().isEmpty()) {
            errores.put("proveedorAI", I18nUI.tr("Proveedor de AI es requerido", "AI provider is required"));
        } else if (!ProveedorAI.existeProveedor(proveedorAI)) {
            errores.put(
                "proveedorAI",
                I18nUI.trf("Proveedor de AI no reconocido: %s", "Unknown AI provider: %s", proveedorAI)
            );
        }

        ProveedorAI.ConfiguracionProveedor config = ProveedorAI.obtenerProveedor(proveedorAI);
        if (config != null && config.requiereClaveApi()) {
            String key = obtenerApiKeyParaProveedor(proveedorAI);
            if (key == null || key.trim().isEmpty()) {
                errores.put(
                    "claveApi",
                    I18nUI.trf("Clave de API requerida para %s", "API key is required for %s", proveedorAI)
                );
            }
        }

        if (obtenerModelo().trim().isEmpty()) {
            errores.put("modelo", I18nUI.tr("Modelo es requerido", "Model is required"));
        }

        if (retrasoSegundos < MINIMO_RETRASO_SEGUNDOS || retrasoSegundos > MAXIMO_RETRASO_SEGUNDOS) {
            errores.put(
                "retrasoSegundos",
                I18nUI.trf(
                    "Retraso debe estar entre %d y %d segundos",
                    "Delay must be between %d and %d seconds",
                    MINIMO_RETRASO_SEGUNDOS,
                    MAXIMO_RETRASO_SEGUNDOS
                )
            );
        }

        if (maximoConcurrente < MINIMO_MAXIMO_CONCURRENTE || maximoConcurrente > MAXIMO_MAXIMO_CONCURRENTE) {
            errores.put(
                "maximoConcurrente",
                I18nUI.trf(
                    "Maximo concurrente debe estar entre %d y %d",
                    "Max concurrent must be between %d and %d",
                    MINIMO_MAXIMO_CONCURRENTE,
                    MAXIMO_MAXIMO_CONCURRENTE
                )
            );
        }

        if (maximoHallazgosTabla < MINIMO_HALLAZGOS_TABLA || maximoHallazgosTabla > MAXIMO_HALLAZGOS_TABLA) {
            errores.put("maximoHallazgosTabla",
                I18nUI.trf(
                    "Maximo de hallazgos en tabla debe estar entre %d y %d",
                    "Max findings in table must be between %d and %d",
                    MINIMO_HALLAZGOS_TABLA,
                    MAXIMO_HALLAZGOS_TABLA
                ));
        }

        if (tiempoEsperaAI < 10 || tiempoEsperaAI > 300) {
            errores.put(
                "tiempoEsperaAI",
                I18nUI.tr("Tiempo de espera debe estar entre 10 y 300 segundos",
                    "Timeout must be between 10 and 300 seconds")
            );
        }

        if (!"Light".equals(tema) && !"Dark".equals(tema)) {
            errores.put("tema", I18nUI.tr("Tema debe ser 'Light' o 'Dark'", "Theme must be 'Light' or 'Dark'"));
        }

        if (promptConfigurable == null || promptConfigurable.trim().isEmpty()) {
            errores.put(
                "promptConfigurable",
                I18nUI.tr("Prompt configurable es requerido", "Configurable prompt is required")
            );
        }

        return errores;
    }

    public static String obtenerPromptPorDefecto() {
        return "You are an elite offensive security researcher performing HTTP traffic analysis for a professional pentest engagement. Your goal is to identify ALL security weaknesses, not limited to OWASP Top 10.\n" +
               "\n" +
               "Analyze the following HTTP request/response pair and identify vulnerabilities including but not limited to:\n" +
               "- Injection flaws (SQLi, XSS, XXE, SSTI, Command Injection, LDAP, XPath)\n" +
               "- Authentication and session issues\n" +
               "- Access control weaknesses\n" +
               "- Cryptographic failures (cleartext, weak ciphers, missing HSTS)\n" +
               "- Sensitive data exposure in headers, URLs, or body\n" +
               "- CSRF, SSRF, Open Redirect\n" +
               "- Business logic flaws\n" +
               "- Information disclosure (server versions, stack traces, debug params)\n" +
               "- Insecure deserialization\n" +
               "- Security misconfigurations\n" +
               "- HTTP request smuggling indicators\n" +
               "- Mass assignment / parameter pollution\n" +
               "- Dangerous HTTP methods\n" +
               "\n" +
               "REQUEST:\n" +
               "{REQUEST}\n" +
               "\n" +
               "RESPONSE:\n" +
               "{RESPONSE}\n" +
               "\n" +
               "OUTPUT LANGUAGE:\n" +
               "{OUTPUT_LANGUAGE}\n" +
               "\n" +
               "SEVERITY CRITERIA:\n" +
               "- Critical: Direct code execution, authentication bypass, full data exposure\n" +
               "- High: SQL injection, stored XSS, SSRF, significant data leakage\n" +
               "- Medium: Reflected XSS, CSRF, missing security headers, cleartext transmission\n" +
               "- Low: Information disclosure, fingerprinting, minor misconfigurations\n" +
               "- Info: Observations worth noting but no direct exploitability\n" +
               "\n" +
               "CONFIDENCE CRITERIA:\n" +
               "- High: Vulnerability is directly observable in request/response evidence\n" +
               "- Medium: Strong indicators present but still requires server-side confirmation\n" +
               "- Low: Possible attack surface based on structure, needs active testing to confirm\n" +
               "\n" +
               "STRICT OUTPUT RULES:\n" +
               "1. Output ONLY raw JSON. No markdown, no code blocks, no backticks, no explanation.\n" +
               "2. Start your response with { and end with }\n" +
               "3. Every finding must have exactly these three fields: \"descripcion\", \"severidad\", \"confianza\"\n" +
               "4. \"severidad\" must be exactly one of: Critical, High, Medium, Low, Info\n" +
               "5. \"confianza\" must be exactly one of: High, Medium, Low\n" +
               "6. Write \"descripcion\" strictly in OUTPUT LANGUAGE\n" +
               "7. If no vulnerabilities found, return: {\"hallazgos\":[]}\n" +
               "\n" +
               "{\"hallazgos\":[{\"descripcion\":\"string\",\"severidad\":\"Critical|High|Medium|Low|Info\",\"confianza\":\"High|Medium|Low\"}]}";
    }

    public String obtenerPromptConfigurable() {
        return promptConfigurable;
    }

    public void establecerPromptConfigurable(String promptConfigurable) {
        if (promptConfigurable == null || promptConfigurable.trim().isEmpty()) {
            this.promptConfigurable = obtenerPromptPorDefecto();
        } else {
            this.promptConfigurable = promptConfigurable;
        }
    }

    public boolean esConfiguracionValida() {
        return !obtenerUrlApi().isEmpty() &&
               !obtenerModelo().isEmpty() &&
               obtenerProveedorAI() != null && !obtenerProveedorAI().isEmpty();
    }

    public boolean tieneApiKey() {
        String apiKey = obtenerClaveApi();
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    public String validarParaConsultaModelo() {
        asegurarMapas();

        String proveedor = obtenerProveedorAI();
        if (proveedor == null || proveedor.trim().isEmpty() || !ProveedorAI.existeProveedor(proveedor)) {
            return I18nUI.tr(
                "ALERTA: Proveedor de AI no configurado o no valido",
                "ALERT: AI provider is not configured or is invalid"
            );
        }

        String urlApi = obtenerUrlApi();
        if (urlApi == null || urlApi.trim().isEmpty()) {
            return I18nUI.tr(
                "ALERTA: URL de API no configurada",
                "ALERT: API URL is not configured"
            );
        }

        String modelo = obtenerModelo();
        if (modelo == null || modelo.trim().isEmpty()) {
            return I18nUI.trf(
                "ALERTA: Modelo no configurado para %s",
                "ALERT: Model is not configured for %s",
                proveedor
            );
        }

        ProveedorAI.ConfiguracionProveedor proveedorConfig = ProveedorAI.obtenerProveedor(proveedor);
        if (proveedorConfig != null && proveedorConfig.requiereClaveApi() && !tieneApiKey()) {
            return I18nUI.trf(
                "ALERTA: Clave de API requerida para %s",
                "ALERT: API key is required for %s",
                proveedor
            );
        }

        return "";
    }

    public Map<String, String> obtenerApiKeysPorProveedor() {
        asegurarMapas();
        return new HashMap<>(apiKeysPorProveedor);
    }

    public void establecerApiKeysPorProveedor(Map<String, String> apiKeysPorProveedor) {
        this.apiKeysPorProveedor = apiKeysPorProveedor != null ? new HashMap<>(apiKeysPorProveedor) : new HashMap<>();
    }

    public Map<String, String> obtenerUrlsBasePorProveedor() {
        asegurarMapas();
        return new HashMap<>(urlsBasePorProveedor);
    }

    public void establecerUrlsBasePorProveedor(Map<String, String> urlsBasePorProveedor) {
        this.urlsBasePorProveedor = urlsBasePorProveedor != null ? new HashMap<>(urlsBasePorProveedor) : new HashMap<>();
    }

    public Map<String, String> obtenerModelosPorProveedor() {
        asegurarMapas();
        return new HashMap<>(modelosPorProveedor);
    }

    public void establecerModelosPorProveedor(Map<String, String> modelosPorProveedor) {
        this.modelosPorProveedor = modelosPorProveedor != null ? new HashMap<>(modelosPorProveedor) : new HashMap<>();
    }

    public Map<String, Integer> obtenerMaxTokensPorProveedor() {
        asegurarMapas();
        return new HashMap<>(maxTokensPorProveedor);
    }

    public void establecerMaxTokensPorProveedor(Map<String, Integer> maxTokensPorProveedor) {
        this.maxTokensPorProveedor = maxTokensPorProveedor != null ? new HashMap<>(maxTokensPorProveedor) : new HashMap<>();
    }

    private void asegurarMapas() {
        if (apiKeysPorProveedor == null) {
            apiKeysPorProveedor = new HashMap<>();
        }
        if (urlsBasePorProveedor == null) {
            urlsBasePorProveedor = new HashMap<>();
        }
        if (modelosPorProveedor == null) {
            modelosPorProveedor = new HashMap<>();
        }
        if (maxTokensPorProveedor == null) {
            maxTokensPorProveedor = new HashMap<>();
        }
        if (proveedorAI == null || proveedorAI.trim().isEmpty() || !ProveedorAI.existeProveedor(proveedorAI)) {
            proveedorAI = "Z.ai";
        }
        idiomaUi = IdiomaUI.desdeCodigo(idiomaUi).codigo();
        tiempoEsperaAI = normalizarTiempoEspera(tiempoEsperaAI);
        tema = normalizarTema(tema);
        retrasoSegundos = normalizarRetrasoSegundos(retrasoSegundos);
        maximoConcurrente = normalizarMaximoConcurrente(maximoConcurrente);
        maximoHallazgosTabla = normalizarMaximoHallazgos(maximoHallazgosTabla);
    }

    private static int normalizarMaximoHallazgos(int valor) {
        if (valor < MINIMO_HALLAZGOS_TABLA) {
            return MINIMO_HALLAZGOS_TABLA;
        }
        if (valor > MAXIMO_HALLAZGOS_TABLA) {
            return MAXIMO_HALLAZGOS_TABLA;
        }
        return valor;
    }

    private static int normalizarTiempoEspera(int valor) {
        if (valor < 10) {
            return 10;
        }
        if (valor > 300) {
            return 300;
        }
        return valor;
    }

    private static int normalizarRetrasoSegundos(int valor) {
        if (valor < MINIMO_RETRASO_SEGUNDOS) {
            return MINIMO_RETRASO_SEGUNDOS;
        }
        if (valor > MAXIMO_RETRASO_SEGUNDOS) {
            return MAXIMO_RETRASO_SEGUNDOS;
        }
        return valor;
    }

    private static int normalizarMaximoConcurrente(int valor) {
        if (valor < MINIMO_MAXIMO_CONCURRENTE) {
            return MINIMO_MAXIMO_CONCURRENTE;
        }
        if (valor > MAXIMO_MAXIMO_CONCURRENTE) {
            return MAXIMO_MAXIMO_CONCURRENTE;
        }
        return valor;
    }

    private static String normalizarTema(String tema) {
        if ("Dark".equals(tema)) {
            return "Dark";
        }
        return "Light";
    }

    private int obtenerMaxTokensPorDefectoProveedor(String proveedor) {
        ProveedorAI.ConfiguracionProveedor config = ProveedorAI.obtenerProveedor(proveedor);
        return config != null ? config.obtenerMaxTokensPorDefecto() : 4096;
    }

    public ConfiguracionAPI crearSnapshot() {
        asegurarMapas();
        ConfiguracionAPI snapshot = new ConfiguracionAPI();
        snapshot.retrasoSegundos = this.retrasoSegundos;
        snapshot.maximoConcurrente = this.maximoConcurrente;
        snapshot.maximoHallazgosTabla = this.maximoHallazgosTabla;
        snapshot.detallado = this.detallado;
        snapshot.proveedorAI = this.proveedorAI;
        snapshot.tiempoEsperaAI = this.tiempoEsperaAI;
        snapshot.tema = this.tema;
        snapshot.idiomaUi = this.idiomaUi;
        snapshot.escaneoPasivoHabilitado = this.escaneoPasivoHabilitado;
        snapshot.autoGuardadoIssuesHabilitado = this.autoGuardadoIssuesHabilitado;
        snapshot.autoScrollConsolaHabilitado = this.autoScrollConsolaHabilitado;
        snapshot.promptConfigurable = this.promptConfigurable;
        snapshot.promptModificado = this.promptModificado;

        snapshot.apiKeysPorProveedor = new HashMap<>(this.apiKeysPorProveedor);
        snapshot.urlsBasePorProveedor = new HashMap<>(this.urlsBasePorProveedor);
        snapshot.modelosPorProveedor = new HashMap<>(this.modelosPorProveedor);
        snapshot.maxTokensPorProveedor = new HashMap<>(this.maxTokensPorProveedor);
        return snapshot;
    }

    public void aplicarDesde(ConfiguracionAPI origen) {
        if (origen == null) {
            return;
        }
        origen.asegurarMapas();

        this.retrasoSegundos = origen.retrasoSegundos;
        this.maximoConcurrente = origen.maximoConcurrente;
        this.maximoHallazgosTabla = origen.maximoHallazgosTabla;
        this.detallado = origen.detallado;
        this.proveedorAI = origen.proveedorAI;
        this.tiempoEsperaAI = origen.tiempoEsperaAI;
        this.tema = origen.tema;
        this.idiomaUi = origen.idiomaUi;
        this.escaneoPasivoHabilitado = origen.escaneoPasivoHabilitado;
        this.autoGuardadoIssuesHabilitado = origen.autoGuardadoIssuesHabilitado;
        this.autoScrollConsolaHabilitado = origen.autoScrollConsolaHabilitado;
        this.promptConfigurable = origen.promptConfigurable;
        this.promptModificado = origen.promptModificado;

        this.apiKeysPorProveedor = new HashMap<>(origen.apiKeysPorProveedor);
        this.urlsBasePorProveedor = new HashMap<>(origen.urlsBasePorProveedor);
        this.modelosPorProveedor = new HashMap<>(origen.modelosPorProveedor);
        this.maxTokensPorProveedor = new HashMap<>(origen.maxTokensPorProveedor);
        asegurarMapas();
    }

}
