package com.burpia.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DeduplicadorSolicitudes {
    private final ConcurrentHashMap<String, Boolean> hashesProcesados;
    private static final int MAX_HASHES = 10000;
    private static final int HASHES_A_ELIMINAR = 500;

    public DeduplicadorSolicitudes() {
        this.hashesProcesados = new ConcurrentHashMap<>(16, 0.75f);
    }

    public boolean esDuplicadoYAgregar(String hash) {
        Boolean valorPrevio = hashesProcesados.putIfAbsent(hash, Boolean.TRUE);

        if (hashesProcesados.size() > MAX_HASHES) {
            aplicarLimiteTamano();
        }

        return valorPrevio != null;
    }

    private void aplicarLimiteTamano() {
        if (hashesProcesados.size() <= MAX_HASHES) {
            return;
        }

        List<String> clavesAEliminar = new ArrayList<>();
        int contador = 0;

        for (String clave : hashesProcesados.keySet()) {
            if (contador++ >= HASHES_A_ELIMINAR) break;
            clavesAEliminar.add(clave);
        }

        for (String clave : clavesAEliminar) {
            hashesProcesados.remove(clave);
        }
    }

    public void limpiar() {
        hashesProcesados.clear();
    }

    public int obtenerNumeroHashes() {
        return hashesProcesados.size();
    }

    public boolean contieneHash(String hash) {
        return hashesProcesados.containsKey(hash);
    }

    public void agregarHash(String hash) {
        hashesProcesados.put(hash, Boolean.TRUE);
    }
}
