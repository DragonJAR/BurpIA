package com.burpia.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GestorConfiguracion {
    private static final String ARCHIVO_CONFIG = System.getProperty("user.home") + File.separator + ".burpia.json";
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
    }

    public ConfiguracionAPI cargarConfiguracion() {
        try {
            if (Files.exists(Paths.get(ARCHIVO_CONFIG))) {
                String json = new String(Files.readAllBytes(Paths.get(ARCHIVO_CONFIG)));
                ConfiguracionAPI config = gson.fromJson(json, ConfiguracionAPI.class);

                // Si es una configuracion antigua sin proveedorAI, migrarla
                if (config.obtenerProveedorAI() == null) {
                    config.establecerProveedorAI("Z.ai");
                    config.actualizarUrlParaProveedor();
                }

                // Si no tiene promptConfigurable (configuración antigua), usar el por defecto
                if (config.obtenerPromptConfigurable() == null || config.obtenerPromptConfigurable().trim().isEmpty()) {
                    config.establecerPromptConfigurable(ConfiguracionAPI.obtenerPromptPorDefecto());
                }

                return config;
            }
        } catch (IOException e) {
            err.println("[Configuracion] Error al cargar: " + e.getMessage());
        }
        return new ConfiguracionAPI();
    }

    public void guardarConfiguracion(ConfiguracionAPI config) {
        try {
            String json = gson.toJson(config);
            Files.write(Paths.get(ARCHIVO_CONFIG), json.getBytes());
            out.println("[Configuracion] Configuracion guardada en " + ARCHIVO_CONFIG);
        } catch (IOException e) {
            err.println("[Configuracion] Error al guardar: " + e.getMessage());
        }
    }
}
