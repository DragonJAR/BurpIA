package com.burpia.config;
import com.burpia.i18n.I18nLogs;
import com.burpia.i18n.I18nUI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GestorConfiguracion {
    private static final String NOMBRE_ARCHIVO = ".burpia.json";
    private final Path rutaConfig;
    private final Gson gson;
    private final PrintWriter out;
    private final PrintWriter err;

    public GestorConfiguracion() {
        this(null, null);
    }

    public GestorConfiguracion(PrintWriter out, PrintWriter err) {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.out = out != null ? out : new PrintWriter(System.out, true);
        this.err = err != null ? err : new PrintWriter(System.err, true);

        String userHome = System.getProperty("user.home");
        if (userHome == null || userHome.isEmpty()) {
            userHome = System.getProperty("user.dir", ".");
        }
        this.rutaConfig = Paths.get(userHome, NOMBRE_ARCHIVO);

        logInfo("[Configuracion] Ruta de configuracion: " + rutaConfig.toAbsolutePath());
    }

    public ConfiguracionAPI cargarConfiguracion() {
        try {
            Path path = rutaConfig.toAbsolutePath();

            if (!Files.exists(path)) {
                logInfo("[Configuracion] Archivo no existe, creando configuracion por defecto");
                return new ConfiguracionAPI();
            }

            asegurarPermisosPrivados(path);

            if (!Files.isReadable(path)) {
                logError("[Configuracion] Archivo no es legible: " + path);
                return new ConfiguracionAPI();
            }

            String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

            if (json == null || json.trim().isEmpty()) {
                logInfo("[Configuracion] Archivo vacio, usando configuracion por defecto");
                return new ConfiguracionAPI();
            }

            ArchivoConfiguracion archivo = gson.fromJson(json, ArchivoConfiguracion.class);
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
            logError("[Configuracion] Error inesperado al cargar: " + e.getClass().getSimpleName() + " - " + e.getMessage());
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

            Path directorioPadre = path.getParent();
            if (directorioPadre != null && !Files.exists(directorioPadre)) {
                logError("[Configuracion] Directorio padre no existe: " + directorioPadre);
                if (mensajeError != null) {
                    mensajeError.append(I18nUI.Configuracion.MSG_DIRECTORIO_NO_EXISTE())
                        .append(directorioPadre);
                }
                return false;
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
            String json = gson.toJson(archivo);

            tempPath = Paths.get(path.toString() + ".tmp");

            Files.write(tempPath, json.getBytes(StandardCharsets.UTF_8));
            asegurarPermisosPrivados(tempPath);

            try {
                Files.move(tempPath, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(tempPath, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            asegurarPermisosPrivados(path);

            logInfo("[Configuracion] Configuracion guardada exitosamente en: " + path);
            return true;

        } catch (IOException e) {
            logError("[Configuracion] Error de E/S al guardar: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            if (mensajeError != null) {
                mensajeError.append(I18nUI.Configuracion.MSG_ERROR_IO()).append(e.getMessage());
            }
            return false;
        } catch (Exception e) {
            logError("[Configuracion] Error inesperado al guardar: " + e.getClass().getSimpleName() + " - " + e.getMessage());
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
        out.println(I18nLogs.tr(mensaje));
    }

    private void logError(String mensaje) {
        err.println(I18nLogs.tr(mensaje));
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
                PosixFilePermission.OWNER_WRITE
            );
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

        if (archivo.proveedorAI != null && ProveedorAI.existeProveedor(archivo.proveedorAI)) {
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
        if (archivo.tema != null) {
            config.establecerTema(archivo.tema);
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

        // Migracion y Carga de Configuracion de Agentes
        if (archivo.agenteHabilitado != null) {
            config.establecerAgenteHabilitado(archivo.agenteHabilitado);
        } else if (archivo.agenteFactoryDroidHabilitado != null) {
            // Legacy fallback
            config.establecerAgenteHabilitado(archivo.agenteFactoryDroidHabilitado);
        }

        if (archivo.tipoAgente != null) {
            config.establecerTipoAgente(archivo.tipoAgente);
        }

        if (archivo.rutasBinarioPorAgente != null) {
            config.establecerTodasLasRutasBinario(archivo.rutasBinarioPorAgente);
        } else if (archivo.agenteFactoryDroidBinario != null) {
            // Legacy fallback
            config.establecerRutaBinarioAgente(AgenteTipo.FACTORY_DROID.name(), archivo.agenteFactoryDroidBinario);
        }
        
        if (archivo.agentePrompt != null) {
            config.establecerAgentePrompt(archivo.agentePrompt);
        } else if (archivo.agenteFactoryDroidPrompt != null) {
             // Legacy fallback
            config.establecerAgentePrompt(archivo.agenteFactoryDroidPrompt);
        }

        if (archivo.agenteDelay != null) {
            config.establecerAgenteDelay(archivo.agenteDelay);
        } else if (archivo.agenteFactoryDroidDelay != null) {
             // Legacy fallback
            config.establecerAgenteDelay(archivo.agenteFactoryDroidDelay);
        }

        config.establecerDetallado(Boolean.TRUE.equals(archivo.detallado));

        config.establecerApiKeysPorProveedor(sanitizarMapaString(archivo.apiKeysPorProveedor));
        config.establecerUrlsBasePorProveedor(sanitizarMapaString(archivo.urlsBasePorProveedor));
        config.establecerModelosPorProveedor(sanitizarMapaString(archivo.modelosPorProveedor));
        config.establecerMaxTokensPorProveedor(sanitizarMapaInt(archivo.maxTokensPorProveedor));
        config.establecerTiempoEsperaPorModelo(sanitizarMapaTimeoutPorModelo(archivo.tiempoEsperaPorModelo));

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
        archivo.tema = config.obtenerTema();
        archivo.idiomaUi = config.obtenerIdiomaUi();
        archivo.escaneoPasivoHabilitado = config.escaneoPasivoHabilitado();
        archivo.autoGuardadoIssuesHabilitado = config.autoGuardadoIssuesHabilitado();
        archivo.autoScrollConsolaHabilitado = config.autoScrollConsolaHabilitado();
        archivo.promptConfigurable = config.obtenerPromptConfigurable();
        archivo.promptModificado = config.esPromptModificado();
        archivo.ignorarErroresSSL = config.ignorarErroresSSL();
        archivo.soloProxy = config.soloProxy();
        archivo.agenteHabilitado = config.agenteHabilitado();
        archivo.tipoAgente = config.obtenerTipoAgente();
        archivo.agentePrompt = config.obtenerAgentePrompt();
        archivo.agenteDelay = config.obtenerAgenteDelay();
        archivo.rutasBinarioPorAgente = new HashMap<>(config.obtenerTodasLasRutasBinario());
        archivo.apiKeysPorProveedor = new HashMap<>(config.obtenerApiKeysPorProveedor());
        archivo.urlsBasePorProveedor = new HashMap<>(config.obtenerUrlsBasePorProveedor());
        archivo.modelosPorProveedor = new HashMap<>(config.obtenerModelosPorProveedor());
        archivo.maxTokensPorProveedor = new HashMap<>(config.obtenerMaxTokensPorProveedor());
        archivo.tiempoEsperaPorModelo = new HashMap<>(config.obtenerTiempoEsperaPorModelo());
        return archivo;
    }

    private Map<String, String> sanitizarMapaString(Map<String, String> mapa) {
        Map<String, String> limpio = new HashMap<>();
        if (mapa == null) {
            return limpio;
        }
        for (Map.Entry<String, String> entry : mapa.entrySet()) {
            if (entry.getKey() != null && ProveedorAI.existeProveedor(entry.getKey())) {
                limpio.put(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
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
            if (entry.getKey() != null && ProveedorAI.existeProveedor(entry.getKey()) && entry.getValue() != null) {
                limpio.put(entry.getKey(), entry.getValue());
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
            String clave = entry.getKey().trim();
            if (!clave.isEmpty()) {
                limpio.put(clave, entry.getValue());
            }
        }
        return limpio;
    }

    private static class ArchivoConfiguracion {
        private String proveedorAI;
        private Integer retrasoSegundos;
        private Integer maximoConcurrente;
        private Integer maximoHallazgosTabla;
        private Boolean detallado;
        private Integer tiempoEsperaAI;
        private String tema;
        private String idiomaUi;
        private Boolean escaneoPasivoHabilitado;
        private Boolean autoGuardadoIssuesHabilitado;
        private Boolean autoScrollConsolaHabilitado;
        private String promptConfigurable;
        private Boolean promptModificado;
        private Boolean ignorarErroresSSL;
        private Boolean soloProxy;
        private Boolean agenteFactoryDroidHabilitado;
        private String agenteFactoryDroidBinario;
        private String agenteFactoryDroidPrompt;
        private Integer agenteFactoryDroidDelay;
        
        private Boolean agenteHabilitado;
        private String tipoAgente;
        private String agentePrompt;
        private Integer agenteDelay;
        private Map<String, String> rutasBinarioPorAgente;
        private Map<String, String> apiKeysPorProveedor;
        private Map<String, String> urlsBasePorProveedor;
        private Map<String, String> modelosPorProveedor;
        private Map<String, Integer> maxTokensPorProveedor;
        private Map<String, Integer> tiempoEsperaPorModelo;
    }
}
