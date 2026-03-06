package com.burpia.config;

import com.burpia.i18n.I18nLogs;
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
            logInfo("[Configuracion] Ruta de configuracion: " + rutaConfig.toAbsolutePath());
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
                logError("[Configuracion] Archivo no es legible: " + path);
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
            logError("[Configuracion] Error de sintaxis JSON: " + e.getMessage());
            return new ConfiguracionAPI();
        } catch (IOException e) {
            logError("[Configuracion] Error de E/S al cargar: " + e.getMessage());
            return new ConfiguracionAPI();
        } catch (Exception e) {
            logError("[Configuracion] Error inesperado al cargar: " + e.getClass().getSimpleName() + " - "
                    + e.getMessage());
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
                    logError("[Configuracion] Directorio padre no existe: " + directorioPadre);
                    if (mensajeError != null) {
                        mensajeError.append(I18nUI.Configuracion.MSG_DIRECTORIO_NO_EXISTE())
                                .append(directorioPadre);
                    }
                    return false;
                }
            }

            if (directorioPadre != null && !Files.isWritable(directorioPadre)) {
                logError("[Configuracion] Directorio no es escribible: " + directorioPadre);
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

            Files.move(tempPath, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            if (archivoNuevo) {
                asegurarPermisosPrivados(path);
            }

            logInfo("[Configuracion] Configuracion guardada exitosamente en: " + path);
            return true;

        } catch (IOException e) {
            logError("[Configuracion] Error de E/S al guardar: " + e.getClass().getSimpleName() + " - "
                    + e.getMessage());
            if (mensajeError != null) {
                mensajeError.append(I18nUI.Configuracion.MSG_ERROR_IO()).append(e.getMessage());
            }
            return false;
        } catch (Exception e) {
            logError("[Configuracion] Error inesperado al guardar: " + e.getClass().getSimpleName() + " - "
                    + e.getMessage());
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

    private void logInfo(String mensaje) {
        inicializarLogging();
        gestorLogging.info("Configuracion", mensaje);
    }

    private void logError(String mensaje) {
        inicializarLogging();
        gestorLogging.error("Configuracion", mensaje);
    }

    private void asegurarPermisosPrivados(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try {
            if (!Files.getFileStore(path).supportsFileAttributeView("posix")) {
                return;
            }
            Set<PosixFilePermission> permisos = Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(path, permisos);
        } catch (Exception e) {
            logError("[Configuracion] No se pudieron ajustar permisos privados del archivo: " + e.getMessage());
        }
    }

    private void limpiarArchivoTemporal(Path tempPath) {
        if (tempPath == null) {
            return;
        }
        try {
            Files.deleteIfExists(tempPath);
        } catch (Exception e) {
            logError("[Configuracion] No se pudo eliminar archivo temporal: " + tempPath + " (" + e.getMessage() + ")");
        }
    }

    private ConfiguracionAPI construirDesdeArchivo(ArchivoConfiguracion archivo) {
        ConfiguracionAPI config = new ConfiguracionAPI();

        if (Normalizador.noEsVacio(archivo.proveedorAI) && ProveedorAI.existeProveedor(archivo.proveedorAI)) {
            config.establecerProveedorAI(archivo.proveedorAI);
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

        if (archivo.agenteHabilitado != null) {
            config.establecerAgenteHabilitado(archivo.agenteHabilitado);
        }

        if (archivo.tipoAgente != null) {
            config.establecerTipoAgente(archivo.tipoAgente);
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

        config.establecerApiKeysPorProveedor(sanitizarMapaString(archivo.apiKeysPorProveedor));
        config.establecerUrlsBasePorProveedor(sanitizarMapaString(archivo.urlsBasePorProveedor));
        config.establecerModelosPorProveedor(sanitizarMapaString(archivo.modelosPorProveedor));
        config.establecerMaxTokensPorProveedor(sanitizarMapaInt(archivo.maxTokensPorProveedor));
        config.establecerTiempoEsperaPorModelo(sanitizarMapaTimeoutPorModelo(archivo.tiempoEsperaPorModelo));

        // Multi-Proveedor Configuration
        if (archivo.multiProveedorHabilitado != null) {
            config.establecerMultiProveedorHabilitado(archivo.multiProveedorHabilitado);
        }
        if (archivo.proveedoresMultiConsulta != null && !archivo.proveedoresMultiConsulta.isEmpty()) {
            config.establecerProveedoresMultiConsulta(archivo.proveedoresMultiConsulta);
        }

        return config;
    }

    private ArchivoConfiguracion construirArchivo(ConfiguracionAPI config) {
        ArchivoConfiguracion archivo = new ArchivoConfiguracion();
        archivo.proveedorAI = config.obtenerProveedorAI();
        archivo.retrasoSegundos = config.obtenerRetrasoSegundos();
        archivo.maximoConcurrente = config.obtenerMaximoConcurrente();
        archivo.maximoHallazgosTabla = config.obtenerMaximoHallazgosTabla();
        archivo.detallado = config.esDetallado();
        archivo.tiempoEsperaAI = config.obtenerTiempoEsperaAI();
        archivo.idiomaUi = config.obtenerIdiomaUi();
        archivo.escaneoPasivoHabilitado = config.escaneoPasivoHabilitado();
        archivo.autoGuardadoIssuesHabilitado = config.autoGuardadoIssuesHabilitado();
        archivo.autoScrollConsolaHabilitado = config.autoScrollConsolaHabilitado();
        archivo.alertasHabilitadas = config.alertasHabilitadas();
        archivo.alertasClickDerechoEnviarAHabilitadas = config.alertasClickDerechoEnviarAHabilitadas();
        archivo.promptConfigurable = config.obtenerPromptConfigurable();
        archivo.promptModificado = config.esPromptModificado();
        archivo.ignorarErroresSSL = config.ignorarErroresSSL();
        archivo.soloProxy = config.soloProxy();
        archivo.agenteHabilitado = config.agenteHabilitado();
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
        archivo.rutasBinarioPorAgente = new HashMap<>(config.obtenerTodasLasRutasBinario());
        archivo.apiKeysPorProveedor = new HashMap<>(config.obtenerApiKeysPorProveedor());
        archivo.urlsBasePorProveedor = new HashMap<>(config.obtenerUrlsBasePorProveedor());
        archivo.modelosPorProveedor = new HashMap<>(config.obtenerModelosPorProveedor());
        archivo.maxTokensPorProveedor = new HashMap<>(config.obtenerMaxTokensPorProveedor());
        archivo.tiempoEsperaPorModelo = new HashMap<>(config.obtenerTiempoEsperaPorModelo());

        // Multi-Proveedor Configuration
        archivo.multiProveedorHabilitado = config.esMultiProveedorHabilitado();
        List<String> proveedores = config.obtenerProveedoresMultiConsulta();
        archivo.proveedoresMultiConsulta = proveedores != null && !proveedores.isEmpty()
            ? new ArrayList<>(proveedores)
            : null;

        return archivo;
    }

    private Map<String, String> sanitizarMapaString(Map<String, String> mapa) {
        Map<String, String> limpio = new HashMap<>();
        if (mapa == null) {
            return limpio;
        }
        for (Map.Entry<String, String> entry : mapa.entrySet()) {
            String proveedor = ProveedorAI.normalizarProveedor(entry.getKey());
            if (Normalizador.noEsVacio(proveedor) && ProveedorAI.existeProveedor(proveedor)) {
                limpio.put(proveedor, entry.getValue() != null ? entry.getValue() : "");
            }
        }
        return limpio;
    }

    private Map<String, Integer> sanitizarMapaInt(Map<String, Integer> mapa) {
        Map<String, Integer> limpio = new HashMap<>();
        if (mapa == null) {
            return limpio;
        }
        for (Map.Entry<String, Integer> entry : mapa.entrySet()) {
            String proveedor = ProveedorAI.normalizarProveedor(entry.getKey());
            if (Normalizador.noEsVacio(proveedor) && ProveedorAI.existeProveedor(proveedor)
                    && entry.getValue() != null && entry.getValue() > 0) {
                limpio.put(proveedor, entry.getValue());
            }
        }
        return limpio;
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
        if (Normalizador.esVacio(claveOriginal)) {
            return "";
        }
        String clave = claveOriginal.trim();
        int separador = clave.indexOf("::");
        if (separador <= 0) {
            return clave;
        }

        String proveedor = ProveedorAI.normalizarProveedor(clave.substring(0, separador));
        if (Normalizador.esVacio(proveedor) || !ProveedorAI.existeProveedor(proveedor)) {
            return "";
        }

        String modelo = clave.substring(separador + 2).trim();
        if (Normalizador.esVacio(modelo)) {
            return "";
        }

        return proveedor + "::" + modelo;
    }

    private static class ArchivoConfiguracion {
        private String proveedorAI;
        private Integer retrasoSegundos;
        private Integer maximoConcurrente;
        private Integer maximoHallazgosTabla;
        private Boolean detallado;
        private Integer tiempoEsperaAI;
        private String idiomaUi;
        private Boolean escaneoPasivoHabilitado;
        private Boolean autoGuardadoIssuesHabilitado;
        private Boolean autoScrollConsolaHabilitado;
        private Boolean alertasHabilitadas;
        private Boolean alertasClickDerechoEnviarAHabilitadas;
        private String promptConfigurable;
        private Boolean promptModificado;
        private Boolean ignorarErroresSSL;
        private Boolean soloProxy;
        private Boolean agenteHabilitado;
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

        // Multi-Proveedor Configuration
        private Boolean multiProveedorHabilitado;
        private List<String> proveedoresMultiConsulta;
    }
}
