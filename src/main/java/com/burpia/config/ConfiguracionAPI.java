package com.burpia.config;

import java.util.HashMap;
import java.util.Map;

public class ConfiguracionAPI {
    private String urlApi;
    private String claveApi;
    private String modelo;
    private int retrasoSegundos;
    private int maximoConcurrente;
    private boolean detallado;
    private String proveedorAI;
    private int maxTokens;
    private int tiempoEsperaAI;
    private String tema;
    private boolean escaneoPasivoHabilitado;
    private String promptConfigurable;

    private Map<String, String> apiKeysPorProveedor;
    private Map<String, String> urlsBasePorProveedor;
    private Map<String, Integer> maxTokensPorProveedor;
    private boolean promptModificado;

    public ConfiguracionAPI() {
        this.proveedorAI = "Z.ai";
        this.urlApi = "https://api.z.ai/api/paas/v4/chat/completions";
        this.modelo = "glm-5";
        this.maxTokens = 16384;
        this.retrasoSegundos = 5;
        this.maximoConcurrente = 3;
        this.tiempoEsperaAI = 60;
        this.detallado = false;
        this.tema = "Light";
        this.escaneoPasivoHabilitado = true;
        this.promptModificado = false;

        // CORRECCIÓN: Inicializar promptConfigurable con valor por defecto para prevenir NullPointerException
        this.promptConfigurable = obtenerPromptPorDefecto();

        this.apiKeysPorProveedor = new HashMap<>();
        this.urlsBasePorProveedor = new HashMap<>();
        this.maxTokensPorProveedor = new HashMap<>();
    }

    public String obtenerUrlApi() { return urlApi; }
    public void establecerUrlApi(String urlApi) { this.urlApi = urlApi; }

    public String obtenerClaveApi() { return claveApi; }
    public void establecerClaveApi(String claveApi) { this.claveApi = claveApi; }

    public String obtenerModelo() { return modelo; }
    public void establecerModelo(String modelo) { this.modelo = modelo; }

    public int obtenerRetrasoSegundos() { return retrasoSegundos; }
    public void establecerRetrasoSegundos(int retrasoSegundos) { this.retrasoSegundos = retrasoSegundos; }

    public int obtenerMaximoConcurrente() { return maximoConcurrente; }
    public void establecerMaximoConcurrente(int maximoConcurrente) { this.maximoConcurrente = maximoConcurrente; }

    public boolean esDetallado() { return detallado; }
    public void establecerDetallado(boolean detallado) { this.detallado = detallado; }

    public String obtenerProveedorAI() { return proveedorAI; }
    public void establecerProveedorAI(String proveedorAI) { this.proveedorAI = proveedorAI; }

    public int obtenerMaxTokens() { return maxTokens; }
    public void establecerMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public int obtenerTiempoEsperaAI() { return tiempoEsperaAI; }
    public void establecerTiempoEsperaAI(int tiempoEsperaAI) { this.tiempoEsperaAI = tiempoEsperaAI; }

    public String obtenerTema() { return tema; }
    public void establecerTema(String tema) { this.tema = tema; }

    public boolean escaneoPasivoHabilitado() { return escaneoPasivoHabilitado; }
    public void establecerEscaneoPasivoHabilitado(boolean escaneoPasivoHabilitado) {
        this.escaneoPasivoHabilitado = escaneoPasivoHabilitado;
    }

    public boolean esPromptModificado() { return promptModificado; }
    public void establecerPromptModificado(boolean modificado) { this.promptModificado = modificado; }

    public void actualizarUrlParaProveedor() {
        ProveedorAI.ConfiguracionProveedor config = ProveedorAI.obtenerProveedor(proveedorAI);
        if (config != null) {
            this.urlApi = construirUrlApiProveedor(proveedorAI, config.obtenerUrlApi(), modelo);
        }
    }

    public static String construirUrlApiProveedor(String proveedor, String urlBase, String modelo) {
        String baseNormalizada = normalizarUrlBase(urlBase);
        String proveedorNormalizado = proveedor != null ? proveedor : "";
        String modeloNormalizado = (modelo != null && !modelo.trim().isEmpty()) ? modelo.trim() : "gemini-1.5-pro";

        switch (proveedorNormalizado) {
            case "Claude":
                return baseNormalizada + "/messages";
            case "Gemini":
                return baseNormalizada + "/models/" + modeloNormalizado + ":generateContent";
            case "Ollama":
                return baseNormalizada + "/api/chat";
            case "OpenAI":
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
            "/chat/completions",
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

    public String obtenerUrlApiBase() {
        ProveedorAI.ConfiguracionProveedor config = ProveedorAI.obtenerProveedor(proveedorAI);
        if (config != null) {
            return config.obtenerUrlApi();
        }
        return urlApi;
    }

    public String obtenerApiKeyParaProveedor(String proveedor) {
        if (apiKeysPorProveedor != null && apiKeysPorProveedor.containsKey(proveedor)) {
            return apiKeysPorProveedor.get(proveedor);
        }
        return claveApi;
    }

    public void establecerApiKeyParaProveedor(String proveedor, String apiKey) {
        // OPTIMIZACIÓN: Eliminar verificación de null redundante
        // apiKeysPorProveedor siempre se inicializa en el constructor
        apiKeysPorProveedor.put(proveedor, apiKey);
    }

    public String obtenerUrlBaseParaProveedor(String proveedor) {
        if (urlsBasePorProveedor != null && urlsBasePorProveedor.containsKey(proveedor)) {
            return urlsBasePorProveedor.get(proveedor);
        }
        ProveedorAI.ConfiguracionProveedor config = ProveedorAI.obtenerProveedor(proveedor);
        return config != null ? config.obtenerUrlApi() : urlApi;
    }

    public void establecerUrlBaseParaProveedor(String proveedor, String urlBase) {
        // OPTIMIZACIÓN: Eliminar verificación de null redundante
        // urlsBasePorProveedor siempre se inicializa en el constructor
        urlsBasePorProveedor.put(proveedor, urlBase);
    }

    public int obtenerMaxTokensParaProveedor(String proveedor) {
        if (maxTokensPorProveedor != null && maxTokensPorProveedor.containsKey(proveedor)) {
            return maxTokensPorProveedor.get(proveedor);
        }
        // Retornar valor por defecto del proveedor
        ProveedorAI.ConfiguracionProveedor config = ProveedorAI.obtenerProveedor(proveedor);
        return config != null ? config.obtenerMaxTokensPorDefecto() : 4096;
    }

    public void establecerMaxTokensParaProveedor(String proveedor, int maxTokens) {
        // OPTIMIZACIÓN: Eliminar verificación de null redundante
        // maxTokensPorProveedor siempre se inicializa en el constructor
        maxTokensPorProveedor.put(proveedor, maxTokens);
    }

    public Map<String, String> validar() {
        Map<String, String> errores = new HashMap<>();

        if (proveedorAI == null || proveedorAI.trim().isEmpty()) {
            errores.put("proveedorAI", "Proveedor de AI es requerido");
        } else if (!ProveedorAI.existeProveedor(proveedorAI)) {
            errores.put("proveedorAI", "Proveedor de AI no reconocido: " + proveedorAI);
        }

        ProveedorAI.ConfiguracionProveedor config = ProveedorAI.obtenerProveedor(proveedorAI);
        if (config != null && config.requiereClaveApi()) {
            String key = obtenerApiKeyParaProveedor(proveedorAI);
            if (key == null || key.trim().isEmpty()) {
                errores.put("claveApi", "Clave de API requerida para " + proveedorAI);
            }
        }

        if (modelo == null || modelo.trim().isEmpty()) {
            errores.put("modelo", "Modelo es requerido");
        }

        if (retrasoSegundos < 0 || retrasoSegundos > 60) {
            errores.put("retrasoSegundos", "Retraso debe estar entre 0 y 60 segundos");
        }

        if (maximoConcurrente < 1 || maximoConcurrente > 10) {
            errores.put("maximoConcurrente", "Maximo concurrente debe estar entre 1 y 10");
        }

        if (tiempoEsperaAI < 10 || tiempoEsperaAI > 300) {
            errores.put("tiempoEsperaAI", "Tiempo de espera debe estar entre 10 y 300 segundos");
        }

        if (!tema.equals("Light") && !tema.equals("Dark")) {
            errores.put("tema", "Tema debe ser 'Light' o 'Dark'");
        }

        if (promptConfigurable == null || promptConfigurable.trim().isEmpty()) {
            errores.put("promptConfigurable", "Prompt configurable es requerido");
        }

        return errores;
    }

    public static String obtenerPromptPorDefecto() {
        return "You are an elite offensive security researcher performing HTTP traffic analysis for a professional pentest engagement. Your goal is to identify ALL security weaknesses, not limited to OWASP Top 10.\n" +
               "\n" +
               "Analyze the following HTTP request and identify vulnerabilities including but not limited to:\n" +
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
               "SEVERITY CRITERIA:\n" +
               "- Critical: Direct code execution, authentication bypass, full data exposure\n" +
               "- High: SQL injection, stored XSS, SSRF, significant data leakage\n" +
               "- Medium: Reflected XSS, CSRF, missing security headers, cleartext transmission\n" +
               "- Low: Information disclosure, fingerprinting, minor misconfigurations\n" +
               "- Info: Observations worth noting but no direct exploitability\n" +
               "\n" +
               "CONFIDENCE CRITERIA:\n" +
               "- High: Vulnerability is directly observable in the request (e.g., no HTTPS is a fact)\n" +
               "- Medium: Strong indicators present but requires server-side confirmation\n" +
               "- Low: Possible attack surface based on structure, needs active testing to confirm\n" +
               "\n" +
               "STRICT OUTPUT RULES:\n" +
               "1. Output ONLY raw JSON. No markdown, no code blocks, no backticks, no explanation.\n" +
               "2. Start your response with { and end with }\n" +
               "3. Every finding must have exactly these three fields: \"descripcion\", \"severidad\", \"confianza\"\n" +
               "4. \"severidad\" must be exactly one of: Critical, High, Medium, Low, Info\n" +
               "5. \"confianza\" must be exactly one of: High, Medium, Low\n" +
               "6. If no vulnerabilities found, return: {\"hallazgos\":[]}\n" +
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
        return urlApi != null && !urlApi.isEmpty() &&
               modelo != null && !modelo.isEmpty() &&
               proveedorAI != null && !proveedorAI.isEmpty();
    }

    public boolean tieneApiKey() {
        return claveApi != null && !claveApi.trim().isEmpty();
    }
}
