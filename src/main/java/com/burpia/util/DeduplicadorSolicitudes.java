package com.burpia.util;

import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deduplicador de solicitudes HTTP basado en hashes con TTL y eviction LRU.
 * 
 * <p>Mantiene un registro de hashes de solicitudes ya procesadas para evitar
 * análisis duplicados. Los hashes expiran automáticamente después del TTL
 * configurado y se aplica eviction LRU cuando se alcanza el límite máximo.</p>
 * 
 * <p>Thread-safe: todas las operaciones están sincronizadas.</p>
 */
public class DeduplicadorSolicitudes {
    private static final int MAX_HASHES_POR_DEFECTO = 10000;
    private static final long TTL_MILLIS_POR_DEFECTO = Duration.ofMinutes(15).toMillis();
    private static final long INTERVALO_LIMPIEZA_MILLIS = Duration.ofSeconds(30).toMillis();
    private static final float FACTOR_CARGA = 0.75f;

    private final Object lock = new Object();
    private final int maxHashes;
    private final long ttlMillis;
    private final Map<String, Long> hashesProcesados;
    private long ultimoBarridoMillis;

    /**
     * Crea un deduplicador con configuración por defecto.
     * <ul>
     *   <li>Máximo 10,000 hashes</li>
     *   <li>TTL de 15 minutos</li>
     * </ul>
     */
    public DeduplicadorSolicitudes() {
        this(MAX_HASHES_POR_DEFECTO, TTL_MILLIS_POR_DEFECTO);
    }

    /**
     * Crea un deduplicador con configuración personalizada.
     *
     * @param maxHashes número máximo de hashes a mantener (mínimo 1)
     * @param ttlMillis tiempo de vida en milisegundos (mínimo 10ms)
     */
    public DeduplicadorSolicitudes(int maxHashes, long ttlMillis) {
        this.maxHashes = Math.max(1, maxHashes);
        this.ttlMillis = Math.max(10L, ttlMillis);
        this.hashesProcesados = new LinkedHashMap<String, Long>(this.maxHashes, FACTOR_CARGA, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                return size() > DeduplicadorSolicitudes.this.maxHashes;
            }
        };
        this.ultimoBarridoMillis = System.currentTimeMillis();
    }

    /**
     * Verifica si un hash ya fue procesado y lo agrega al registro.
     *
     * <p>Si el hash es nulo o vacío, retorna false sin agregarlo al registro.</p>
     *
     * @param hash el hash de la solicitud a verificar
     * @return true si el hash ya existía (duplicado), false si es nuevo
     */
    public boolean esDuplicadoYAgregar(String hash) {
        if (Normalizador.esVacio(hash)) {
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

    /**
     * Elimina hashes expirados según el TTL configurado.
     * 
     * <p>El LinkedHashMap está en modo access-order, por lo que los elementos
     * menos recientemente accedidos están al inicio. Esto permite que el barrido
     * sea O(n) donde n es el número de elementos expirados.</p>
     *
     * @param ahora timestamp actual en milisegundos
     */
    private void barrerExpiradosSiCorresponde(long ahora) {
        if (ahora - ultimoBarridoMillis < INTERVALO_LIMPIEZA_MILLIS) {
            return;
        }
        Iterator<Map.Entry<String, Long>> it = hashesProcesados.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (ahora - entry.getValue() > ttlMillis) {
                it.remove();
            } else {
                break;
            }
        }
        ultimoBarridoMillis = ahora;
    }
}
