package com.burpia.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

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

        // Usar Path para manejo multiplataforma correcto
        String userHome = System.getProperty("user.home");
        if (userHome == null || userHome.isEmpty()) {
            userHome = System.getProperty("user.dir", ".");
        }
        this.rutaConfig = Paths.get(userHome, NOMBRE_ARCHIVO);

        this.out.println("[Configuracion] Ruta de configuracion: " + rutaConfig.toAbsolutePath());
    }

    public ConfiguracionAPI cargarConfiguracion() {
        try {
            Path path = rutaConfig.toAbsolutePath();

            if (!Files.exists(path)) {
                out.println("[Configuracion] Archivo no existe, creando configuracion por defecto");
                return new ConfiguracionAPI();
            }

            if (!Files.isReadable(path)) {
                err.println("[Configuracion] Archivo no es legible: " + path);
                return new ConfiguracionAPI();
            }

            String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);

            if (json == null || json.trim().isEmpty()) {
                out.println("[Configuracion] Archivo vacio, usando configuracion por defecto");
                return new ConfiguracionAPI();
            }

            ArchivoConfiguracion archivo = gson.fromJson(json, ArchivoConfiguracion.class);
            if (archivo == null) {
                out.println("[Configuracion] Error al parsear JSON, usando configuracion por defecto");
                return new ConfiguracionAPI();
            }

            ConfiguracionAPI config = construirDesdeArchivo(archivo);

            out.println("[Configuracion] Configuracion cargada exitosamente");
            return config;

        } catch (JsonSyntaxException e) {
            err.println("[Configuracion] Error de sintaxis JSON: " + e.getMessage());
            return new ConfiguracionAPI();
        } catch (IOException e) {
            err.println("[Configuracion] Error de E/S al cargar: " + e.getMessage());
            return new ConfiguracionAPI();
        } catch (Exception e) {
            err.println("[Configuracion] Error inesperado al cargar: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return new ConfiguracionAPI();
        }
    }

    public boolean guardarConfiguracion(ConfiguracionAPI config) {
        return guardarConfiguracion(config, null);
    }

    public boolean guardarConfiguracion(ConfiguracionAPI config, StringBuilder mensajeError) {
        try {
            Path path = rutaConfig.toAbsolutePath();

            // Verificar que el directorio padre existe y es escribible
            Path directorioPadre = path.getParent();
            if (directorioPadre != null && !Files.exists(directorioPadre)) {
                err.println("[Configuracion] Directorio padre no existe: " + directorioPadre);
                if (mensajeError != null) {
                    mensajeError.append("Directorio no existe: ").append(directorioPadre);
                }
                return false;
            }

            if (directorioPadre != null && !Files.isWritable(directorioPadre)) {
                err.println("[Configuracion] Directorio no es escribible: " + directorioPadre);
                if (mensajeError != null) {
                    mensajeError.append("Directorio no escribible: ").append(directorioPadre);
                }
                return false;
            }

            ArchivoConfiguracion archivo = construirArchivo(config);
            String json = gson.toJson(archivo);

            // Usar escritura atomica con archivo temporal
            Path tempPath = Paths.get(path.toString() + ".tmp");

            // Escribir a archivo temporal primero
            Files.write(tempPath, json.getBytes(StandardCharsets.UTF_8));

            // Mover atomicamente (rename)
            try {
                Files.move(tempPath, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                // Si atomic move no es soportado, hacer move normal
                Files.move(tempPath, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            out.println("[Configuracion] Configuracion guardada exitosamente en: " + path);
            return true;

        } catch (IOException e) {
            err.println("[Configuracion] Error de E/S al guardar: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            if (mensajeError != null) {
                mensajeError.append("Error de E/S: ").append(e.getMessage());
            }
            return false;
        } catch (Exception e) {
            err.println("[Configuracion] Error inesperado al guardar: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            if (mensajeError != null) {
                mensajeError.append("Error inesperado: ").append(e.getMessage());
            }
            return false;
        }
    }

    public String obtenerRutaConfiguracion() {
        return rutaConfig.toAbsolutePath().toString();
    }

    public boolean existeConfiguracion() {
        return Files.exists(rutaConfig);
    }

    public boolean eliminarConfiguracion() {
        try {
            if (Files.exists(rutaConfig)) {
                Files.delete(rutaConfig);
                out.println("[Configuracion] Archivo eliminado: " + rutaConfig);
                return true;
            }
            return false;
        } catch (IOException e) {
            err.println("[Configuracion] Error al eliminar: " + e.getMessage());
            return false;
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
        if (archivo.escaneoPasivoHabilitado != null) {
            config.establecerEscaneoPasivoHabilitado(archivo.escaneoPasivoHabilitado);
        }
        if (archivo.promptConfigurable != null) {
            config.establecerPromptConfigurable(archivo.promptConfigurable);
        }
        if (archivo.promptModificado != null) {
            config.establecerPromptModificado(archivo.promptModificado);
        }

        // verbose off por defecto si no existe el campo
        config.establecerDetallado(Boolean.TRUE.equals(archivo.detallado));

        config.establecerApiKeysPorProveedor(sanitizarMapaString(archivo.apiKeysPorProveedor));
        config.establecerUrlsBasePorProveedor(sanitizarMapaString(archivo.urlsBasePorProveedor));
        config.establecerModelosPorProveedor(sanitizarMapaString(archivo.modelosPorProveedor));
        config.establecerMaxTokensPorProveedor(sanitizarMapaInt(archivo.maxTokensPorProveedor));

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
        archivo.escaneoPasivoHabilitado = config.escaneoPasivoHabilitado();
        archivo.promptConfigurable = config.obtenerPromptConfigurable();
        archivo.promptModificado = config.esPromptModificado();
        archivo.apiKeysPorProveedor = new HashMap<>(config.obtenerApiKeysPorProveedor());
        archivo.urlsBasePorProveedor = new HashMap<>(config.obtenerUrlsBasePorProveedor());
        archivo.modelosPorProveedor = new HashMap<>(config.obtenerModelosPorProveedor());
        archivo.maxTokensPorProveedor = new HashMap<>(config.obtenerMaxTokensPorProveedor());
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

    private static class ArchivoConfiguracion {
        private String proveedorAI;
        private Integer retrasoSegundos;
        private Integer maximoConcurrente;
        private Integer maximoHallazgosTabla;
        private Boolean detallado;
        private Integer tiempoEsperaAI;
        private String tema;
        private Boolean escaneoPasivoHabilitado;
        private String promptConfigurable;
        private Boolean promptModificado;
        private Map<String, String> apiKeysPorProveedor;
        private Map<String, String> urlsBasePorProveedor;
        private Map<String, String> modelosPorProveedor;
        private Map<String, Integer> maxTokensPorProveedor;
    }
}
