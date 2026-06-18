package com.burpia.config;

import com.burpia.i18n.I18nUI;
import com.burpia.util.GestorLoggingUnificado;
import com.burpia.util.Normalizador;
import com.burpia.util.RutasBurpIA;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GestorConfiguracion {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path rutaConfig;
    private final Object lockLogging = new Object();
    private PrintWriter out;
    private PrintWriter err;
    private volatile GestorLoggingUnificado gestorLogging;

    public GestorConfiguracion() {
        this(null, null);
    }

    public GestorConfiguracion(PrintWriter out, PrintWriter err) {
        this.rutaConfig = RutasBurpIA.obtenerRutaConfig();

        if (out != null && err != null) {
            this.out = out;
            this.err = err;
            this.gestorLogging = GestorLoggingUnificado.crearMinimal(out, err);
            logInfo("[Configuracion] Ruta de configuracion: %s", rutaConfig.toAbsolutePath());
        }
    }

    private void inicializarLogging() {
        if (gestorLogging == null) {
            synchronized (lockLogging) {
                if (gestorLogging == null) {
                    this.out = new PrintWriter(System.out, true);
                    this.err = new PrintWriter(System.err, true);
                    this.gestorLogging = GestorLoggingUnificado.crearMinimal(out, err);
                }
            }
        }
    }

    public ConfiguracionAPI cargarConfiguracion() {
        try {
            Path path = rutaConfig.toAbsolutePath();

            if (!Files.exists(path)) {
                logInfo("[Configuracion] Archivo no existe, creando configuracion por defecto");
                return new ConfiguracionAPI();
            }

            if (!Files.isReadable(path)) {
                logError("[Configuracion] Archivo no es legible: %s", path);
                return new ConfiguracionAPI();
            }

            String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

            if (Normalizador.esVacio(json)) {
                logInfo("[Configuracion] Archivo vacio, usando configuracion por defecto");
                return new ConfiguracionAPI();
            }

            ArchivoConfiguracion archivo = GSON.fromJson(json, ArchivoConfiguracion.class);
            if (archivo == null) {
                logInfo("[Configuracion] Error al parsear JSON, usando configuracion por defecto");
                return new ConfiguracionAPI();
            }

            ConfiguracionAPI config = construirDesdeArchivo(archivo);

            logInfo("[Configuracion] Configuracion cargada exitosamente");
            return config;

        } catch (JsonSyntaxException e) {
            logError("[Configuracion] Error de sintaxis JSON: %s", e.getMessage());
            return new ConfiguracionAPI();
        } catch (IOException e) {
            logError("[Configuracion] Error de E/S al cargar: %s", e.getMessage());
            return new ConfiguracionAPI();
        } catch (Exception e) {
            logError("[Configuracion] Error inesperado al cargar: %s - %s",
                    e.getClass().getSimpleName(), e.getMessage());
            return new ConfiguracionAPI();
        }
    }

    public boolean guardarConfiguracion(ConfiguracionAPI config) {
        return guardarConfiguracion(config, null);
    }

    public boolean guardarConfiguracion(ConfiguracionAPI config, StringBuilder mensajeError) {
        Path tempPath = null;
        try {
            if (config == null) {
                logError("[Configuracion] No se pudo guardar: configuracion nula");
                if (mensajeError != null) {
                    mensajeError.append(I18nUI.Configuracion.MSG_CONFIGURACION_NULA());
                }
                return false;
            }
            Path path = rutaConfig.toAbsolutePath();
            boolean archivoNuevo = !Files.exists(path);

            Path directorioPadre = path.getParent();
            if (directorioPadre != null && !Files.exists(directorioPadre)) {
                try {
                    Files.createDirectories(directorioPadre);
                } catch (Exception e) {
                    logError("[Configuracion] Directorio padre no existe: %s", directorioPadre);
                    if (mensajeError != null) {
                        mensajeError.append(I18nUI.Configuracion.MSG_DIRECTORIO_NO_EXISTE())
                                .append(directorioPadre);
                    }
                    return false;
                }
            }

            if (directorioPadre != null && !Files.isWritable(directorioPadre)) {
                logError("[Configuracion] Directorio no es escribible: %s", directorioPadre);
                if (mensajeError != null) {
                    mensajeError.append(I18nUI.Configuracion.MSG_DIRECTORIO_NO_ESCRIBIBLE())
                            .append(directorioPadre);
                }
                return false;
            }

            ArchivoConfiguracion archivo = construirArchivo(config);
            String json = GSON.toJson(archivo);

            tempPath = Paths.get(path.toString() + ".tmp");

            Files.write(tempPath, json.getBytes(StandardCharsets.UTF_8));

            // L6: en Windows, Files.move(REPLACE_EXISTING) puede lanzar
            // AccessDeniedException si el destino está abierto (AV, indexador,
            // otro proceso). Reintentamos con backoff corto antes de rendir,
            // evitando que un bloqueo transitorio haga perder los cambios.
            moverAtomicoConReintento(tempPath, path);

            // Aplicar permisos restrictivos en cada guardado (idempotente).
            // Esto cubre archivos creados por versiones previas sin protección o por terceros.
            asegurarPermisosPrivados(path);
            if (archivoNuevo) {
                logInfo("[Configuracion] Archivo de configuracion creado: %s", path);
            }

            logInfo("[Configuracion] Configuracion guardada exitosamente en: %s", path);
            return true;

        } catch (IOException e) {
            logError("[Configuracion] Error de E/S al guardar: %s - %s",
                    e.getClass().getSimpleName(), e.getMessage());
            if (mensajeError != null) {
                mensajeError.append(I18nUI.Configuracion.MSG_ERROR_IO()).append(e.getMessage());
            }
            return false;
        } catch (Exception e) {
            logError("[Configuracion] Error inesperado al guardar: %s - %s",
                    e.getClass().getSimpleName(), e.getMessage());
            if (mensajeError != null) {
                mensajeError.append(I18nUI.Configuracion.MSG_ERROR_INESPERADO()).append(e.getMessage());
            }
            return false;
        } finally {
            limpiarArchivoTemporal(tempPath);
        }
    }

    public String obtenerRutaConfiguracion() {
        return rutaConfig.toAbsolutePath().toString();
    }

    private void logInfo(String formato, Object... args) {
        inicializarLogging();
        gestorLogging.info("Configuracion", traducir(formato, args));
    }

    private void logWarn(String formato, Object... args) {
        // GestorLoggingUnificado no expone nivel warn; usamos info. Estos avisos
        // son informativos (entries de config ignoradas al migrar/renombrar
        // proveedores) y deben ser visibles sin instalar un canal nuevo.
        inicializarLogging();
        gestorLogging.info("Configuracion", traducir(formato, args));
    }

    /**
     * L3: avisa cuando el sanitizado descartó entries del mapa persistido
     * (proveedores renombrados/eliminados entre versiones, claves malformadas).
     * Sin esto, el usuario pierde credenciales/configuración sin enterarse.
     */
    private void avisarDescartes(Map<?, ?> origen, Map<?, ?> saneado, String concepto) {
        int antes = origen != null ? origen.size() : 0;
        int despues = saneado != null ? saneado.size() : 0;
        if (antes > despues) {
            logWarn("Config migrada: %d entradas de %s ignoradas por proveedor desconocido o clave inválida",
                    antes - despues, concepto);
        }
    }

    private void logError(String formato, Object... args) {
        inicializarLogging();
        gestorLogging.error("Configuracion", traducir(formato, args));
    }

    /**
     * DRY helper para los logs de esta clase: si no hay args, aplica
     * {@link com.burpia.i18n.I18nLogs#tr(String) tr} (diccionario);
     * si hay args, aplica {@link com.burpia.i18n.I18nLogs#trf(String, Object...) trf}
     * (formato traducido + format con args).
     */
    private static String traducir(String formato, Object... args) {
        if (args == null || args.length == 0) {
            return com.burpia.i18n.I18nLogs.tr(formato);
        }
        return com.burpia.i18n.I18nLogs.trf(formato, args);
    }

    private void asegurarPermisosPrivados(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try {
            // L5: el archivo contiene API keys en claro. En POSIX aplicamos 0600.
            // En Windows/FS no-POSIX antes se retornaba sin protección, dejando
            // el archivo legible por otros usuarios. Ahora intentamos restringir
            // vía ACL al usuario actual (AclFileAttributeView) en el fallback.
            if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
                Set<PosixFilePermission> permisos = Set.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE);
                Files.setPosixFilePermissions(path, permisos);
                return;
            }
            asegurarPermisosPrivadosAcl(path);
        } catch (Exception e) {
            logError("[Configuracion] No se pudieron ajustar permisos privados del archivo: %s", e.getMessage());
        }
    }

    /**
     * L5: fallback de protección para FS no-POSIX (Windows). Restringe el ACL a
     * solo el propietario actual usando {@link AclFileAttributeView}.
     * Best-effort: si el FS no soporta ACL, queda sin protección (lo registramos).
     */
    private void asegurarPermisosPrivadosAcl(Path path) {
        try {
            java.nio.file.attribute.AclFileAttributeView aclView =
                    Files.getFileAttributeView(path, java.nio.file.attribute.AclFileAttributeView.class);
            if (aclView == null) {
                logInfo("[Configuracion] FS sin soporte POSIX/ACL: el archivo de config no se pudo restringir");
                return;
            }
            // Conservar solo la entrada del propietario, eliminar herencia/heredados.
            java.nio.file.attribute.UserPrincipal owner = aclView.getOwner();
            java.util.List<java.nio.file.attribute.AclEntry> nuevas = new java.util.ArrayList<>();
            if (owner != null) {
                java.nio.file.attribute.AclEntry entry = java.nio.file.attribute.AclEntry.newBuilder()
                        .setType(java.nio.file.attribute.AclEntryType.ALLOW)
                        .setPrincipal(owner)
                        .setPermissions(java.nio.file.attribute.AclEntryPermission.values())
                        .build();
                nuevas.add(entry);
            }
            aclView.setAcl(nuevas);
        } catch (Exception e) {
            logInfo("[Configuracion] No se pudo aplicar ACL privada (FS no soportado): %s", e.getMessage());
        }
    }

    private void limpiarArchivoTemporal(Path tempPath) {
        if (tempPath == null) {
            return;
        }
        try {
            Files.deleteIfExists(tempPath);
        } catch (Exception e) {
            logError("[Configuracion] No se pudo eliminar archivo temporal: %s (%s)", tempPath, e.getMessage());
        }
    }

    /**
     * L6: mueve tempPath → destino con reemplazo atómico, reintentando ante
     * AccessDeniedException (Windows: AV, indexador u otro proceso con el
     * archivo abierto). En POSIX el move es atómico y no suele bloquearse.
     */
    private static void moverAtomicoConReintento(Path tempPath, Path destino) throws IOException {
        int maxIntentos = 3;
        long[] backoffMs = {0L, 100L, 250L};
        IOException ultimo = null;
        for (int intento = 0; intento < maxIntentos; intento++) {
            try {
                if (intento > 0) {
                    Thread.sleep(backoffMs[intento]);
                }
                Files.move(tempPath, destino, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IOException(I18nUI.tr("Move interrumpido", "Move interrupted"), ie);
            } catch (java.nio.file.AccessDeniedException e) {
                ultimo = e;
            }
        }
        throw ultimo != null ? ultimo
                : new IOException(I18nUI.tr("No se pudo mover el archivo tras reintentos",
                        "Could not move file after retries"));
    }

    private ConfiguracionAPI construirDesdeArchivo(ArchivoConfiguracion archivo) {
        ConfiguracionAPI config = new ConfiguracionAPI();

        if (Normalizador.noEsVacio(archivo.proveedorAI)) {
            if (ProveedorAI.existeProveedor(archivo.proveedorAI)) {
                config.establecerProveedorAI(archivo.proveedorAI);
            } else {
                // L4: antes el proveedor desconocido caía silenciosamente al
                // fallback (Z.ai) y el análisis fallaba después con un error de
                // auth confuso. Avisamos para que el usuario sepa que su
                // proveedor persistido ya no existe (rename/borrado en migración).
                logWarn("ALERTA: proveedor '%s' no reconocido; usando '%s'",
                        archivo.proveedorAI, config.obtenerProveedorAI());
            }
        }

        if (archivo.retrasoSegundos != null) {
            config.establecerRetrasoSegundos(archivo.retrasoSegundos);
        }
        if (archivo.maximoConcurrente != null) {
            config.establecerMaximoConcurrente(archivo.maximoConcurrente);
        }
        if (archivo.maximoHallazgosTabla != null) {
            config.establecerMaximoHallazgosTabla(archivo.maximoHallazgosTabla);
        }
        if (archivo.tiempoEsperaAI != null) {
            config.establecerTiempoEsperaAI(archivo.tiempoEsperaAI);
        }
        if (archivo.idiomaUi != null) {
            config.establecerIdiomaUi(archivo.idiomaUi);
        }
        if (archivo.escaneoPasivoHabilitado != null) {
            config.establecerEscaneoPasivoHabilitado(archivo.escaneoPasivoHabilitado);
        }
        if (archivo.autoGuardadoIssuesHabilitado != null) {
            config.establecerAutoGuardadoIssuesHabilitado(archivo.autoGuardadoIssuesHabilitado);
        }
        if (archivo.autoScrollConsolaHabilitado != null) {
            config.establecerAutoScrollConsolaHabilitado(archivo.autoScrollConsolaHabilitado);
        }
        if (archivo.alertasHabilitadas != null) {
            config.establecerAlertasHabilitadas(archivo.alertasHabilitadas);
        }
        if (archivo.alertasClickDerechoEnviarAHabilitadas != null) {
            config.establecerAlertasClickDerechoEnviarAHabilitadas(archivo.alertasClickDerechoEnviarAHabilitadas);
        }
        if (archivo.alertasDeshabilitadas != null) {
            config.establecerAlertasDeshabilitadas(archivo.alertasDeshabilitadas);
        }
        boolean promptModificado = Boolean.TRUE.equals(archivo.promptModificado);
        config.establecerPromptModificado(promptModificado);
        if (promptModificado && archivo.promptConfigurable != null) {
            config.establecerPromptConfigurable(archivo.promptConfigurable);
        } else {
            config.establecerPromptConfigurable(ConfiguracionAPI.obtenerPromptPorDefecto());
        }

        if (archivo.ignorarErroresSSL != null) {
            config.establecerIgnorarErroresSSL(archivo.ignorarErroresSSL);
        }

        if (archivo.soloProxy != null) {
            config.establecerSoloProxy(archivo.soloProxy);
        }

        if (archivo.tipoAgente != null) {
            config.establecerTipoAgente(archivo.tipoAgente);
        }

        if (archivo.agentesHabilitadosPorTipo != null) {
            config.establecerEstadosHabilitacionAgentes(archivo.agentesHabilitadosPorTipo);
        } else if (Boolean.TRUE.equals(archivo.agenteHabilitado)) {
            config.establecerAgenteHabilitado(config.obtenerTipoAgente(), true);
        }

        if (archivo.rutasBinarioPorAgente != null) {
            config.establecerTodasLasRutasBinario(archivo.rutasBinarioPorAgente);
        }

        if (archivo.agentePrompt != null) {
            config.establecerAgentePrompt(archivo.agentePrompt);
        }

        if (archivo.agentePreflightPrompt != null) {
            config.establecerAgentePreflightPrompt(archivo.agentePreflightPrompt);
        }

        if (archivo.agenteDelay != null) {
            config.establecerAgenteDelay(archivo.agenteDelay);
        }

        config.establecerDetallado(Boolean.TRUE.equals(archivo.detallado));

        if (archivo.nombreFuenteEstandar != null) {
            config.establecerNombreFuenteEstandar(archivo.nombreFuenteEstandar);
        }
        if (archivo.tamanioFuenteEstandar != null) {
            config.establecerTamanioFuenteEstandar(archivo.tamanioFuenteEstandar);
        }
        if (archivo.nombreFuenteMono != null) {
            config.establecerNombreFuenteMono(archivo.nombreFuenteMono);
        }
        if (archivo.tamanioFuenteMono != null) {
            config.establecerTamanioFuenteMono(archivo.tamanioFuenteMono);
        }

        if (archivo.textoFiltroHallazgos != null) {
            config.establecerTextoFiltroHallazgos(archivo.textoFiltroHallazgos);
        }
        if (archivo.filtroSeveridadHallazgos != null) {
            config.establecerFiltroSeveridadHallazgos(archivo.filtroSeveridadHallazgos);
        }
        if (archivo.persistirFiltroBusquedaHallazgos != null) {
            config.establecerPersistirFiltroBusquedaHallazgos(archivo.persistirFiltroBusquedaHallazgos);
        }
        if (archivo.persistirFiltroSeveridadHallazgos != null) {
            config.establecerPersistirFiltroSeveridadHallazgos(archivo.persistirFiltroSeveridadHallazgos);
        }
        if (archivo.maximoTareasTabla != null) {
            config.establecerMaximoTareasTabla(archivo.maximoTareasTabla);
        }

        Map<String, String> apiKeysSanitizadas = sanitizarMapaString(archivo.apiKeysPorProveedor);
        Map<String, String> urlsBaseSanitizadas = sanitizarMapaString(archivo.urlsBasePorProveedor);
        Map<String, String> modelosSanitizados = sanitizarMapaString(archivo.modelosPorProveedor);
        Map<String, Integer> maxTokensSanitizados = sanitizarMapaInt(archivo.maxTokensPorProveedor);
        Map<String, Integer> timeoutsSanitizados = sanitizarMapaTimeoutPorModelo(archivo.tiempoEsperaPorModelo);

        // L3: avisar de entries descartadas (proveedores renombrados/eliminados
        // entre versiones, claves malformadas) para que el usuario no pierda
        // credenciales/configuración sin enterarse al migrar versiones.
        avisarDescartes(archivo.apiKeysPorProveedor, apiKeysSanitizadas, "API keys");
        avisarDescartes(archivo.urlsBasePorProveedor, urlsBaseSanitizadas, "URLs base");
        avisarDescartes(archivo.modelosPorProveedor, modelosSanitizados, "modelos");
        avisarDescartes(archivo.maxTokensPorProveedor, maxTokensSanitizados, "max tokens");
        avisarDescartes(archivo.tiempoEsperaPorModelo, timeoutsSanitizados, "timeouts por modelo");

        config.establecerApiKeysPorProveedor(apiKeysSanitizadas);
        config.establecerUrlsBasePorProveedor(urlsBaseSanitizadas);
        config.establecerModelosPorProveedor(modelosSanitizados);
        config.establecerMaxTokensPorProveedor(maxTokensSanitizados);
        config.establecerTiempoEsperaPorModelo(timeoutsSanitizados);

        // Multi-Proveedor Configuration
        if (archivo.multiProveedorHabilitado != null) {
            config.establecerMultiProveedorHabilitado(archivo.multiProveedorHabilitado);
        }
        if (Normalizador.noEsVacia(archivo.proveedoresMultiConsulta)) {
            config.establecerProveedoresMultiConsulta(archivo.proveedoresMultiConsulta);
        }

        // Estado UI
        if (archivo.estadoUI != null) {
            config.establecerEstadoUI(archivo.estadoUI);
        }

        // Niveles de logging
        if (archivo.nivelErrorHabilitado != null) {
            config.establecerNivelErrorHabilitado(archivo.nivelErrorHabilitado);
        }
        if (archivo.nivelWarnHabilitado != null) {
            config.establecerNivelWarnHabilitado(archivo.nivelWarnHabilitado);
        }
        if (archivo.nivelInfoHabilitado != null) {
            config.establecerNivelInfoHabilitado(archivo.nivelInfoHabilitado);
        }
        if (archivo.nivelDebugHabilitado != null) {
            config.establecerNivelDebugHabilitado(archivo.nivelDebugHabilitado);
        }
        if (archivo.nivelTraceHabilitado != null) {
            config.establecerNivelTraceHabilitado(archivo.nivelTraceHabilitado);
        }

        return config;
    }

    private ArchivoConfiguracion construirArchivo(ConfiguracionAPI config) {
        ArchivoConfiguracion archivo = new ArchivoConfiguracion();
        archivo.proveedorAI = config.obtenerProveedorAI();
        archivo.retrasoSegundos = config.obtenerRetrasoSegundos();
        archivo.maximoConcurrente = config.obtenerMaximoConcurrente();
        archivo.maximoHallazgosTabla = config.obtenerMaximoHallazgosTabla();
        archivo.maximoTareasTabla = config.obtenerMaximoTareasTabla();
        archivo.detallado = config.esDetallado();
        archivo.tiempoEsperaAI = config.obtenerTiempoEsperaAI();
        archivo.idiomaUi = config.obtenerIdiomaUi();
        archivo.escaneoPasivoHabilitado = config.escaneoPasivoHabilitado();
        archivo.autoGuardadoIssuesHabilitado = config.autoGuardadoIssuesHabilitado();
        archivo.autoScrollConsolaHabilitado = config.autoScrollConsolaHabilitado();
        archivo.alertasHabilitadas = config.alertasHabilitadas();
        archivo.alertasClickDerechoEnviarAHabilitadas = config.alertasClickDerechoEnviarAHabilitadas();
        archivo.alertasDeshabilitadas = new HashMap<>(config.obtenerAlertasDeshabilitadas());
        archivo.promptConfigurable = config.obtenerPromptConfigurable();
        archivo.promptModificado = config.esPromptModificado();
        archivo.ignorarErroresSSL = config.ignorarErroresSSL();
        archivo.soloProxy = config.soloProxy();
        archivo.agenteHabilitado = config.agenteHabilitado();
        archivo.agentesHabilitadosPorTipo = new HashMap<>(config.obtenerEstadosHabilitacionAgentes());
        archivo.tipoAgente = config.obtenerTipoAgente();
        archivo.agentePreflightPrompt = config.obtenerAgentePreflightPrompt();
        archivo.agentePrompt = config.obtenerAgentePrompt();
        archivo.agenteDelay = config.obtenerAgenteDelay();
        archivo.nombreFuenteEstandar = config.obtenerNombreFuenteEstandar();
        archivo.tamanioFuenteEstandar = config.obtenerTamanioFuenteEstandar();
        archivo.nombreFuenteMono = config.obtenerNombreFuenteMono();
        archivo.tamanioFuenteMono = config.obtenerTamanioFuenteMono();
        archivo.textoFiltroHallazgos = config.obtenerTextoFiltroHallazgos();
        archivo.filtroSeveridadHallazgos = config.obtenerFiltroSeveridadHallazgos();
        archivo.persistirFiltroBusquedaHallazgos = config.persistirFiltroBusquedaHallazgos();
        archivo.persistirFiltroSeveridadHallazgos = config.persistirFiltroSeveridadHallazgos();
        archivo.estadoUI = new HashMap<>(config.obtenerEstadoUI());
        archivo.rutasBinarioPorAgente = new HashMap<>(config.obtenerTodasLasRutasBinario());
        archivo.apiKeysPorProveedor = new HashMap<>(config.obtenerApiKeysPorProveedor());
        archivo.urlsBasePorProveedor = new HashMap<>(config.obtenerUrlsBasePorProveedor());
        archivo.modelosPorProveedor = new HashMap<>(config.obtenerModelosPorProveedor());
        archivo.maxTokensPorProveedor = new HashMap<>(config.obtenerMaxTokensPorProveedor());
        archivo.tiempoEsperaPorModelo = new HashMap<>(config.obtenerTiempoEsperaPorModelo());

        // Multi-Proveedor Configuration
        archivo.multiProveedorHabilitado = config.esMultiProveedorHabilitado();
        List<String> proveedores = config.obtenerProveedoresMultiConsulta();
        // Siempre guardar como ArrayList (vacío si no hay proveedores) para persistir el orden
        archivo.proveedoresMultiConsulta = Normalizador.noEsVacia(proveedores)
            ? new ArrayList<>(proveedores)
            : new ArrayList<>();

        // Niveles de logging
        archivo.nivelErrorHabilitado = config.esNivelErrorHabilitado();
        archivo.nivelWarnHabilitado = config.esNivelWarnHabilitado();
        archivo.nivelInfoHabilitado = config.esNivelInfoHabilitado();
        archivo.nivelDebugHabilitado = config.esNivelDebugHabilitado();
        archivo.nivelTraceHabilitado = config.esNivelTraceHabilitado();

        return archivo;
    }

    private Map<String, String> sanitizarMapaString(Map<String, String> mapa) {
        return ConfigSanitizers.normalizarMapaStringPorProveedor(mapa);
    }

    private Map<String, Integer> sanitizarMapaInt(Map<String, Integer> mapa) {
        return ConfigSanitizers.normalizarMapaIntPorProveedor(mapa);
    }

    private Map<String, Integer> sanitizarMapaTimeoutPorModelo(Map<String, Integer> mapa) {
        Map<String, Integer> limpio = new HashMap<>();
        if (mapa == null) {
            return limpio;
        }
        for (Map.Entry<String, Integer> entry : mapa.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String clave = normalizarClaveTimeoutProveedorModelo(entry.getKey());
            int valor = entry.getValue();
            if (!clave.isEmpty() && valor > 0) {
                limpio.put(clave, valor);
            }
        }
        return limpio;
    }

    private String normalizarClaveTimeoutProveedorModelo(String claveOriginal) {
        return ConfigSanitizers.normalizarClaveTimeoutProveedorModelo(claveOriginal);
    }

    private static class ArchivoConfiguracion {
        private String proveedorAI;
        private Integer retrasoSegundos;
        private Integer maximoConcurrente;
        private Integer maximoHallazgosTabla;
        private Integer maximoTareasTabla;
        private Boolean detallado;
        private Integer tiempoEsperaAI;
        private String idiomaUi;
        private Boolean escaneoPasivoHabilitado;
        private Boolean autoGuardadoIssuesHabilitado;
        private Boolean autoScrollConsolaHabilitado;
        private Boolean alertasHabilitadas;
        private Boolean alertasClickDerechoEnviarAHabilitadas;
        private Map<String, Boolean> alertasDeshabilitadas;
        private String promptConfigurable;
        private Boolean promptModificado;
        private Boolean ignorarErroresSSL;
        private Boolean soloProxy;
        private Boolean agenteHabilitado;
        private Map<String, Boolean> agentesHabilitadosPorTipo;
        private String tipoAgente;
        private String agentePreflightPrompt;
        private String agentePrompt;
        private Integer agenteDelay;
        private Map<String, String> rutasBinarioPorAgente;
        private Map<String, String> apiKeysPorProveedor;
        private Map<String, String> urlsBasePorProveedor;
        private Map<String, String> modelosPorProveedor;
        private Map<String, Integer> maxTokensPorProveedor;
        private Map<String, Integer> tiempoEsperaPorModelo;

        private String nombreFuenteEstandar;
        private Integer tamanioFuenteEstandar;
        private String nombreFuenteMono;
        private Integer tamanioFuenteMono;

        // UI State Persistence - PanelHallazgos filters
        private String textoFiltroHallazgos;
        private String filtroSeveridadHallazgos;
        private Boolean persistirFiltroBusquedaHallazgos;
        private Boolean persistirFiltroSeveridadHallazgos;

        // UI State Persistence - Estado general
        private Map<String, String> estadoUI;

        // Niveles de logging
        private Boolean nivelErrorHabilitado;
        private Boolean nivelWarnHabilitado;
        private Boolean nivelInfoHabilitado;
        private Boolean nivelDebugHabilitado;
        private Boolean nivelTraceHabilitado;

        // Multi-Proveedor Configuration
        private Boolean multiProveedorHabilitado;
        private List<String> proveedoresMultiConsulta;
    }
}
