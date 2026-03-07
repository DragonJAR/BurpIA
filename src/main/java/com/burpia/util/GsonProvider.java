package com.burpia.util;

import com.google.gson.Gson;

/**
 * Proveedor singleton de Gson para eliminar instancias duplicadas.
 * Thread-safe por inicialización estática.
 */
public final class GsonProvider {
    private static final Gson INSTANCE = new Gson();

    private GsonProvider() {}

    public static Gson get() {
        return INSTANCE;
    }
}
