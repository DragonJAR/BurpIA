package com.burpia.util;
import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;



public class DeduplicadorSolicitudes {
    private static final int MAX_HASHES_POR_DEFECTO = 10000;
    private static final long TTL_MILLIS_POR_DEFECTO = Duration.ofMinutes(15).toMillis();
    private static final long INTERVALO_LIMPIEZA_MILLIS = Duration.ofSeconds(30).toMillis();

    private final Object lock = new Object();
    private final int maxHashes;
    private final long ttlMillis;
    private final LinkedHashMap<String, Long> hashesProcesados;
    private long ultimoBarridoMillis;

    public DeduplicadorSolicitudes() {
        this(MAX_HASHES_POR_DEFECTO, TTL_MILLIS_POR_DEFECTO);
    }

    public DeduplicadorSolicitudes(int maxHashes, long ttlMillis) {
        this.maxHashes = Math.max(1, maxHashes);
        this.ttlMillis = Math.max(10L, ttlMillis);
        this.hashesProcesados = new LinkedHashMap<String, Long>(this.maxHashes, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                return size() > DeduplicadorSolicitudes.this.maxHashes;
            }
        };
        this.ultimoBarridoMillis = System.currentTimeMillis();
    }

    public boolean esDuplicadoYAgregar(String hash) {
        if (hash == null || hash.isEmpty()) {
            return false;
        }

        long ahora = System.currentTimeMillis();
        synchronized (lock) {
            barrerExpiradosSiCorresponde(ahora);
            boolean duplicado = hashesProcesados.containsKey(hash);
            hashesProcesados.put(hash, ahora);
            return duplicado;
        }
    }

    public void limpiar() {
        synchronized (lock) {
            hashesProcesados.clear();
            ultimoBarridoMillis = System.currentTimeMillis();
        }
    }

    public int obtenerNumeroHashes() {
        synchronized (lock) {
            barrerExpiradosSiCorresponde(System.currentTimeMillis());
            return hashesProcesados.size();
        }
    }

    public boolean contieneHash(String hash) {
        if (hash == null || hash.isEmpty()) {
            return false;
        }
        long ahora = System.currentTimeMillis();
        synchronized (lock) {
            barrerExpiradosSiCorresponde(ahora);
            Long timestamp = hashesProcesados.get(hash);
            if (timestamp == null) {
                return false;
            }
            if (ahora - timestamp > ttlMillis) {
                hashesProcesados.remove(hash);
                return false;
            }
            return true;
        }
    }

    public void agregarHash(String hash) {
        if (hash == null || hash.isEmpty()) {
            return;
        }
        long ahora = System.currentTimeMillis();
        synchronized (lock) {
            barrerExpiradosSiCorresponde(ahora);
            hashesProcesados.put(hash, ahora);
        }
    }

    private void barrerExpiradosSiCorresponde(long ahora) {
        if (ahora - ultimoBarridoMillis < INTERVALO_LIMPIEZA_MILLIS) {
            return;
        }
        Iterator<Map.Entry<String, Long>> it = hashesProcesados.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (ahora - entry.getValue() > ttlMillis) {
                it.remove();
            }
        }
        ultimoBarridoMillis = ahora;
    }
}
