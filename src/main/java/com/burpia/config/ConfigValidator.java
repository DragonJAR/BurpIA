package com.burpia.config;

import com.burpia.i18n.I18nUI;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.Normalizador;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static com.burpia.util.Normalizador.esVacio;
import static com.burpia.util.Normalizador.noEsVacio;

/**
 * Clase centralizada para validación de campos de configuración.
 * <p>
 * Extrae la lógica de validación de DialogoConfiguracion siguiendo el principio DRY.
 * Proporciona métodos de validación para todos los campos de configuración
 * con mensajes de error localizados y manejo robusto de errores.
 * </p>
 *
 * @see <a href="https://github.com/DragonJAR/BurpIA/blob/main/AGENTS.md">Guía de codificación AGENTS.md</a>
 */
public final class ConfigValidator {

    private static final String ORIGEN_LOG = "ConfigValidator";

    // Patrones de validación para API keys por proveedor
    private static final Pattern OPENAI_API_KEY_PATTERN = Pattern.compile("^sk-[A-Za-z0-9]{48}$");
    private static final Pattern CLAUDE_API_KEY_PATTERN = Pattern.compile("^sk-ant-[A-Za-z0-9_-]{95}$");
    private static final Pattern GEMINI_API_KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{39}$");
    
    // Patrones de validación de URLs
    private static final Pattern HTTP_URL_PATTERN = Pattern.compile("^https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+$");
    
    // Extensiones de binarios válidas para agentes
    private static final Set<String> EXTENSIONES_BINARIO_VALIDAS = new HashSet<>(Arrays.asList(
        ".exe", ".sh", ".bat", ".cmd", ".ps1", ""
    ));

    private ConfigValidator() {
        // Clase de utilidad, no instanciable
    }

    /**
     * Resultado de una operación de validación.
     */
    public static final class ValidationResult {
        private final boolean valido;
        private final String mensajeError;
        private final String campo;

        private ValidationResult(boolean valido, String mensajeError, String campo) {
            this.valido = valido;
            this.mensajeError = mensajeError;
            this.campo = campo;
        }

        /**
         * Crea un resultado de validación exitoso.
         *
         * @return ValidationResult válido
         */
        public static ValidationResult valido() {
            return new ValidationResult(true, null, null);
        }

        /**
         * Crea un resultado de validación fallido.
         *
         * @param mensajeError mensaje de error descriptivo
         * @param campo nombre del campo que falló la validación
         * @return ValidationResult inválido
         */
        public static ValidationResult invalido(String mensajeError, String campo) {
            return new ValidationResult(false, mensajeError, campo);
        }

        /**
         * Verifica si la validación fue exitosa.
         *
         * @return true si es válido, false si no
         */
        public boolean esValido() {
            return valido;
        }

        /**
         * Obtiene el mensaje de error.
         *
         * @return mensaje de error o null si es válido
         */
        public String getMensajeError() {
            return mensajeError;
        }

        /**
         * Obtiene el nombre del campo con error.
         *
         * @return nombre del campo o null si es válido
         */
        public String getCampo() {
            return campo;
        }
    }

    // ============ VALIDACIÓN DE API KEYS ============

    /**
     * Valida una API key según el formato específico del proveedor.
     *
     * @param apiKey la API key a validar
     * @param proveedor el proveedor AI
     * @return resultado de la validación
     */
    public static ValidationResult validarApiKey(String apiKey, String proveedor) {
        if (esVacio(proveedor)) {
            return ValidationResult.invalido(
                I18nUI.tr("Proveedor no puede estar vacío", "Provider cannot be empty"), 
                "proveedor"
            );
        }

        ProveedorAI.ConfiguracionProveedor configProveedor = ProveedorAI.obtenerProveedor(proveedor);
        if (configProveedor == null) {
            return ValidationResult.invalido(
                I18nUI.trf("Proveedor no reconocido: %s", "Unknown provider: %s", proveedor), 
                "proveedor"
            );
        }

        if (!configProveedor.requiereClaveApi()) {
            return ValidationResult.valido();
        }

        if (esVacio(apiKey)) {
            return ValidationResult.invalido(
                I18nUI.trf("Clave API requerida para %s", "API key required for %s", proveedor), 
                "apiKey"
            );
        }

        String apiKeyTrimmed = apiKey.trim();
        
        switch (proveedor.toLowerCase()) {
            case "openai":
                if (!OPENAI_API_KEY_PATTERN.matcher(apiKeyTrimmed).matches()) {
                    return ValidationResult.invalido(
                        I18nUI.trf("Formato de API key inválido para OpenAI. Debe comenzar con 'sk-' y tener 48 caracteres", 
                                "Invalid API key format for OpenAI. Must start with 'sk-' and have 48 characters"), 
                        "apiKey"
                    );
                }
                break;
                
            case "claude":
            case "anthropic":
                if (!CLAUDE_API_KEY_PATTERN.matcher(apiKeyTrimmed).matches()) {
                    return ValidationResult.invalido(
                        I18nUI.trf("Formato de API key inválido para Claude. Debe comenzar con 'sk-ant-' y tener 95 caracteres", 
                                "Invalid API key format for Claude. Must start with 'sk-ant-' and have 95 characters"), 
                        "apiKey"
                    );
                }
                break;
                
            case "gemini":
                if (!GEMINI_API_KEY_PATTERN.matcher(apiKeyTrimmed).matches()) {
                    return ValidationResult.invalido(
                        I18nUI.trf("Formato de API key inválido para Gemini. Debe tener 39 caracteres alfanuméricos", 
                                "Invalid API key format for Gemini. Must have 39 alphanumeric characters"), 
                        "apiKey"
                    );
                }
                break;
                
            case "ollama":
                // Ollama no requiere API key format validation
                break;
                
            default:
                // Para proveedores personalizados, validación básica
                if (apiKeyTrimmed.length() < 8) {
                    return ValidationResult.invalido(
                        I18nUI.tr("API key demasiado corta. Mínimo 8 caracteres", "API key too short. Minimum 8 characters"), 
                        "apiKey"
                    );
                }
        }

        return ValidationResult.valido();
    }

    /**
     * Valida una URL de API verificando que sea HTTP/HTTPS y tenga formato válido.
     *
     * @param url la URL a validar
     * @return resultado de la validación
     */
    public static ValidationResult validarUrlApi(String url) {
        if (esVacio(url)) {
            return ValidationResult.valido(); // URL puede ser vacía para usar defaults
        }

        String urlTrimmed = url.trim();
        
        if (!HTTP_URL_PATTERN.matcher(urlTrimmed).matches()) {
            return ValidationResult.invalido(
                I18nUI.tr("Formato de URL inválido. Debe ser HTTP/HTTPS válido", "Invalid URL format. Must be valid HTTP/HTTPS"), 
                "url"
            );
        }

        if (!urlTrimmed.startsWith("https://") && !urlTrimmed.startsWith("http://localhost")) {
            return ValidationResult.invalido(
                I18nUI.tr("Se requiere HTTPS para URLs de API (excepto localhost)", "HTTPS required for API URLs (except localhost)"), 
                "url"
            );
        }

        return ValidationResult.valido();
    }

    // ============ VALIDACIÓN DE CAMPOS NUMÉRICOS ============

    /**
     * DRY: Método helper para validar enteros en un rango.
     */
    private static ValidationResult validarRangoEntero(int valor, int minimo, int maximo, 
                                                        String mensajeError, String campo) {
        if (valor < minimo || valor > maximo) {
            return ValidationResult.invalido(mensajeError, campo);
        }
        return ValidationResult.valido();
    }

    /**
     * Valida el valor de retraso en segundos.
     *
     * @param retrasoSegundos el valor a validar
     * @return resultado de la validación
     */
    public static ValidationResult validarRetrasoSegundos(int retrasoSegundos) {
        return validarRangoEntero(
            retrasoSegundos,
            ConfiguracionAPI.MINIMO_RETRASO_SEGUNDOS,
            ConfiguracionAPI.MAXIMO_RETRASO_SEGUNDOS,
            I18nUI.Configuracion.ERROR_RETRASO_RANGO(ConfiguracionAPI.MINIMO_RETRASO_SEGUNDOS, ConfiguracionAPI.MAXIMO_RETRASO_SEGUNDOS),
            "retrasoSegundos"
        );
    }

    /**
     * Valida el número máximo de operaciones concurrentes.
     *
     * @param maximoConcurrente el valor a validar
     * @return resultado de la validación
     */
    public static ValidationResult validarMaximoConcurrente(int maximoConcurrente) {
        return validarRangoEntero(
            maximoConcurrente,
            ConfiguracionAPI.MINIMO_MAXIMO_CONCURRENTE,
            ConfiguracionAPI.MAXIMO_MAXIMO_CONCURRENTE,
            I18nUI.Configuracion.ERROR_MAXIMO_CONCURRENTE_RANGO(ConfiguracionAPI.MINIMO_MAXIMO_CONCURRENTE, ConfiguracionAPI.MAXIMO_MAXIMO_CONCURRENTE),
            "maximoConcurrente"
        );
    }

    /**
     * Valida el número máximo de hallazgos en tabla.
     *
     * @param maximoHallazgos el valor a validar
     * @return resultado de la validación
     */
    public static ValidationResult validarMaximoHallazgos(int maximoHallazgos) {
        return validarRangoEntero(
            maximoHallazgos,
            ConfiguracionAPI.MINIMO_HALLAZGOS_TABLA,
            ConfiguracionAPI.MAXIMO_HALLAZGOS_TABLA,
            I18nUI.Configuracion.ERROR_MAXIMO_HALLAZGOS_TABLA_RANGO(ConfiguracionAPI.MINIMO_HALLAZGOS_TABLA, ConfiguracionAPI.MAXIMO_HALLAZGOS_TABLA),
            "maximoHallazgos"
        );
    }

    /**
     * Valida el número máximo de tareas en tabla.
     *
     * @param maximoTareas el valor a validar
     * @return resultado de la validación
     */
    public static ValidationResult validarMaximoTareas(int maximoTareas) {
        return validarRangoEntero(
            maximoTareas,
            ConfiguracionAPI.MINIMO_TAREAS_TABLA,
            ConfiguracionAPI.MAXIMO_TAREAS_TABLA,
            I18nUI.trf("Máximo de tareas debe estar entre %d y %d", "Max tasks must be between %d and %d", 
                        ConfiguracionAPI.MINIMO_TAREAS_TABLA, ConfiguracionAPI.MAXIMO_TAREAS_TABLA),
            "maximoTareas"
        );
    }

    /**
     * Valida el timeout en segundos para un modelo.
     *
     * @param timeoutSegundos el valor a validar
     * @return resultado de la validación
     */
    public static ValidationResult validarTimeoutModelo(int timeoutSegundos) {
        return validarRangoEntero(
            timeoutSegundos,
            ConfiguracionAPI.TIEMPO_ESPERA_MIN_SEGUNDOS,
            ConfiguracionAPI.TIEMPO_ESPERA_MAX_SEGUNDOS,
            I18nUI.Configuracion.ERROR_TIMEOUT_RANGO(),
            "timeout"
        );
    }

    /**
     * Valida el número máximo de tokens para un modelo.
     *
     * @param maxTokens el valor a validar
     * @return resultado de la validación
     */
    public static ValidationResult validarMaxTokens(int maxTokens) {
        return validarRangoEntero(
            maxTokens,
            1,
            200000,
            I18nUI.trf("Máximo de tokens debe estar entre %d y %d", "Max tokens must be between %d and %d", 1, 200000),
            "maxTokens"
        );
    }

    /**
     * Valida el delay del agente en milisegundos.
     *
     * @param delayMs el valor a validar
     * @return resultado de la validación
     */
    public static ValidationResult validarAgenteDelay(int delayMs) {
        return validarRangoEntero(
            delayMs,
            0,
            60000,
            I18nUI.trf("El delay del agente debe estar entre %d y %d ms", "Agent delay must be between %d and %d ms", 0, 60000),
            "agenteDelay"
        );
    }

    // ============ VALIDACIÓN DE TEXTOS Y PROMPTS ============

    /**
     * Valida un texto de prompt asegurando que no esté vacío y tenga longitud razonable.
     *
     * @param prompt el prompt a validar
     * @return resultado de la validación
     */
    public static ValidationResult validarPrompt(String prompt) {
        if (esVacio(prompt)) {
            return ValidationResult.invalido(
                I18nUI.Configuracion.ERROR_PROMPT_REQUERIDO(), 
                "prompt"
            );
        }

        String promptNormalizado = Normalizador.normalizarTexto(prompt);
        
        if (promptNormalizado.length() < 10) {
            return ValidationResult.invalido(
                I18nUI.tr("El prompt es demasiado corto. Mínimo 10 caracteres", "Prompt too short. Minimum 10 characters"), 
                "prompt"
            );
        }

        if (promptNormalizado.length() > 10000) {
            return ValidationResult.invalido(
                I18nUI.tr("El prompt es demasiado largo. Máximo 10000 caracteres", "Prompt too long. Maximum 10000 characters"), 
                "prompt"
            );
        }

        return ValidationResult.valido();
    }

    /**
     * Valida un prompt de agente (preflight o principal).
     *
     * @param promptAgente el prompt del agente a validar
     * @return resultado de la validación
     */
    public static ValidationResult validarPromptAgente(String promptAgente) {
        if (esVacio(promptAgente)) {
            return ValidationResult.invalido(
                I18nUI.tr("El prompt del agente no puede estar vacío", "Agent prompt cannot be empty"), 
                "promptAgente"
            );
        }

        String promptNormalizado = Normalizador.normalizarTexto(promptAgente);
        
        if (promptNormalizado.length() < 5) {
            return ValidationResult.invalido(
                I18nUI.tr("El prompt del agente es demasiado corto. Mínimo 5 caracteres", "Agent prompt too short. Minimum 5 characters"), 
                "promptAgente"
            );
        }

        if (promptNormalizado.length() > 5000) {
            return ValidationResult.invalido(
                I18nUI.tr("El prompt del agente es demasiado largo. Máximo 5000 caracteres", "Agent prompt too long. Maximum 5000 characters"), 
                "promptAgente"
            );
        }

        return ValidationResult.valido();
    }

    // ============ VALIDACIÓN DE RUTAS DE BINARIOS ============

    /**
     * Valida la ruta al binario de un agente.
     *
     * @param rutaBinario la ruta al binario
     * @param tipoAgente el tipo de agente
     * @return resultado de la validación
     */
    public static ValidationResult validarRutaBinarioAgente(String rutaBinario, String tipoAgente) {
        if (esVacio(tipoAgente)) {
            return ValidationResult.invalido(
                I18nUI.tr("Tipo de agente no puede estar vacío", "Agent type cannot be empty"), 
                "tipoAgente"
            );
        }

        if (esVacio(rutaBinario)) {
            return ValidationResult.invalido(
                I18nUI.tr("Ruta del binario no puede estar vacía", "Binary path cannot be empty"), 
                "rutaBinario"
            );
        }

        String rutaNormalizada = rutaBinario.trim();
        
        // Validar extensión del archivo
        boolean extensionValida = EXTENSIONES_BINARIO_VALIDAS.stream()
            .anyMatch(ext -> rutaNormalizada.toLowerCase().endsWith(ext));
            
        if (!extensionValida) {
            return ValidationResult.invalido(
                I18nUI.trf("Extensión de binario inválida. Extensiones válidas: %s", 
                            "Invalid binary extension. Valid extensions: %s", 
                            String.join(", ", EXTENSIONES_BINARIO_VALIDAS)), 
                "rutaBinario"
            );
        }

        // Validar caracteres inválidos en ruta
        if (rutaNormalizada.contains("..") || rutaNormalizada.contains("~")) {
            return ValidationResult.invalido(
                I18nUI.tr("La ruta contiene caracteres inválidos (.. o ~)", "Path contains invalid characters (.. or ~)"), 
                "rutaBinario"
            );
        }

        return ValidationResult.valido();
    }

    // ============ VALIDACIÓN DE CONFIGURACIÓN COMPLETA ============

    /**
     * Valida una configuración completa de proveedor.
     *
     * @param configuracion la configuración a validar
     * @return resultado de la validación
     */
    public static ValidationResult validarConfiguracionProveedor(ConfiguracionAPI configuracion) {
        if (configuracion == null) {
            return ValidationResult.invalido(
                I18nUI.Configuracion.MSG_CONFIGURACION_NULA(), 
                "configuracion"
            );
        }

        String proveedor = configuracion.obtenerProveedorAI();
        ValidationResult resultadoProveedor = validarProveedor(proveedor);
        if (!resultadoProveedor.esValido()) {
            return resultadoProveedor;
        }

        String apiKey = configuracion.obtenerClaveApi();
        ValidationResult resultadoApiKey = validarApiKey(apiKey, proveedor);
        if (!resultadoApiKey.esValido()) {
            return resultadoApiKey;
        }

        String url = configuracion.obtenerUrlApi();
        ValidationResult resultadoUrl = validarUrlApi(url);
        if (!resultadoUrl.esValido()) {
            return resultadoUrl;
        }

        String modelo = configuracion.obtenerModelo();
        ValidationResult resultadoModelo = validarModelo(modelo, proveedor);
        if (!resultadoModelo.esValido()) {
            return resultadoModelo;
        }

        return ValidationResult.valido();
    }

    /**
     * Valida que un proveedor sea válido.
     *
     * @param proveedor el nombre del proveedor
     * @return resultado de la validación
     */
    public static ValidationResult validarProveedor(String proveedor) {
        if (esVacio(proveedor)) {
            return ValidationResult.invalido(
                I18nUI.tr("Proveedor no puede estar vacío", "Provider cannot be empty"), 
                "proveedor"
            );
        }

        ProveedorAI.ConfiguracionProveedor configProveedor = ProveedorAI.obtenerProveedor(proveedor);
        if (configProveedor == null) {
            return ValidationResult.invalido(
                I18nUI.trf("Proveedor no reconocido: %s", "Unknown provider: %s", proveedor), 
                "proveedor"
            );
        }

        return ValidationResult.valido();
    }

    /**
     * Valida un modelo para un proveedor específico.
     *
     * @param modelo el nombre del modelo
     * @param proveedor el proveedor
     * @return resultado de la validación
     */
    public static ValidationResult validarModelo(String modelo, String proveedor) {
        if (esVacio(proveedor)) {
            return ValidationResult.invalido(
                I18nUI.tr("Proveedor no puede estar vacío", "Provider cannot be empty"), 
                "proveedor"
            );
        }

        ProveedorAI.ConfiguracionProveedor configProveedor = ProveedorAI.obtenerProveedor(proveedor);
        if (configProveedor == null) {
            return ValidationResult.invalido(
                I18nUI.trf("Proveedor no reconocido: %s", "Unknown provider: %s", proveedor), 
                "proveedor"
            );
        }

        if (esVacio(modelo)) {
            return ValidationResult.invalido(
                I18nUI.Configuracion.ERROR_MODELO_REQUERIDO(), 
                "modelo"
            );
        }

        String modeloNormalizado = Normalizador.normalizarTexto(modelo);
        
        if (modeloNormalizado.length() < 2) {
            return ValidationResult.invalido(
                I18nUI.tr("El modelo es demasiado corto. Mínimo 2 caracteres", "Model too short. Minimum 2 characters"), 
                "modelo"
            );
        }

        if (modeloNormalizado.length() > 100) {
            return ValidationResult.invalido(
                I18nUI.tr("El modelo es demasiado largo. Máximo 100 caracteres", "Model too long. Maximum 100 characters"), 
                "modelo"
            );
        }

        return ValidationResult.valido();
    }

    // ============ MÉTODOS UTILITARIOS ============

    /**
     * Valida un valor entero desde un string.
     *
     * @param valor el string a convertir
     * @param campo nombre del campo para mensajes de error
     * @param valorMinimo valor mínimo aceptado
     * @param valorMaximo valor máximo aceptado
     * @return resultado de la validación con el valor convertido si es válido
     */
    public static ValidationResultEntero validarEntero(String valor, String campo, int valorMinimo, int valorMaximo) {
        if (esVacio(valor)) {
            return ValidationResultEntero.invalido(
                I18nUI.trf("El campo %s es requerido", "Field %s is required", campo), 
                campo
            );
        }

        try {
            int valorEntero = Integer.parseInt(valor.trim());
            
            if (valorEntero < valorMinimo) {
                return ValidationResultEntero.invalido(
                    I18nUI.trf("El campo %s debe ser al menos %d", "Field %s must be at least %d", campo, valorMinimo), 
                    campo
                );
            }

            if (valorEntero > valorMaximo) {
                return ValidationResultEntero.invalido(
                    I18nUI.trf("El campo %s no puede exceder %d", "Field %s cannot exceed %d", campo, valorMaximo), 
                    campo
                );
            }

            return ValidationResultEntero.valido(valorEntero);
            
        } catch (NumberFormatException e) {
            return ValidationResultEntero.invalido(
                I18nUI.trf("El campo %s debe ser un número válido", "Field %s must be a valid number", campo), 
                campo
            );
        }
    }

    /**
     * Resultado de validación que incluye el valor entero convertido.
     */
    public static final class ValidationResultEntero {
        private final boolean valido;
        private final String mensajeError;
        private final String campo;
        private final Integer valor;

        private ValidationResultEntero(boolean valido, String mensajeError, String campo, Integer valor) {
            this.valido = valido;
            this.mensajeError = mensajeError;
            this.campo = campo;
            this.valor = valor;
        }

        /**
         * Crea un resultado válido con el valor convertido.
         *
         * @param valor el valor entero válido
         * @return ValidationResultEntero válido
         */
        public static ValidationResultEntero valido(int valor) {
            return new ValidationResultEntero(true, null, null, valor);
        }

        /**
         * Crea un resultado inválido.
         *
         * @param mensajeError mensaje de error
         * @param campo nombre del campo
         * @return ValidationResultEntero inválido
         */
        public static ValidationResultEntero invalido(String mensajeError, String campo) {
            return new ValidationResultEntero(false, mensajeError, campo, null);
        }

        /**
         * Verifica si la validación fue exitosa.
         *
         * @return true si es válido, false si no
         */
        public boolean esValido() {
            return valido;
        }

        /**
         * Obtiene el mensaje de error.
         *
         * @return mensaje de error o null si es válido
         */
        public String getMensajeError() {
            return mensajeError;
        }

        /**
         * Obtiene el nombre del campo con error.
         *
         * @return nombre del campo o null si es válido
         */
        public String getCampo() {
            return campo;
        }

        /**
         * Obtiene el valor entero validado.
         *
         * @return el valor o null si la validación falló
         */
        public Integer getValor() {
            return valor;
        }
    }

    /**
     * Registra un error de validación usando el sistema de logging unificado.
     *
     * @param gestorLogging el gestor de logging (puede ser null)
     * @param origen el origen del error
     * @param mensaje el mensaje de error
     */
    public static void logError(GestorLoggingUnificado gestorLogging, String origen, String mensaje) {
        if (gestorLogging != null) {
            gestorLogging.error(origen, mensaje);
        }
    }

    /**
     * Registra un error de validación con excepción usando el sistema de logging unificado.
     *
     * @param gestorLogging el gestor de logging (puede ser null)
     * @param origen el origen del error
     * @param mensaje el mensaje de error
     * @param throwable la excepción
     */
    public static void logError(GestorLoggingUnificado gestorLogging, String origen, String mensaje, Throwable throwable) {
        if (gestorLogging != null) {
            gestorLogging.error(origen, mensaje, throwable);
        }
    }
}