package com.burpia.config;

import com.burpia.util.Normalizador;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilidades de sanitización y normalización para configuración.
 * Package-private para ser compartido entre ConfiguracionAPI y GestorConfiguracion.
 */
final class ConfigSanitizers {

    private ConfigSanitizers() {
        // Utility class
    }

    /**
     * Normaliza las claves de un mapa String->String por proveedor, filtrando
     * proveedores inválidos y normalizando nombres.
     */
    static Map<String, String> normalizarMapaStringPorProveedor(Map<String, String> mapa) {
        Map<String, String> limpio = new HashMap<>();
        if (mapa == null) {
            return limpio;
        }
        for (Map.Entry<String, String> entry : mapa.entrySet()) {
            if (entry == null) {
                continue;
            }
            String proveedor = normalizarProveedor(entry.getKey());
            if (proveedor.isEmpty() || !ProveedorAI.existeProveedor(proveedor)) {
                continue;
            }
            limpio.put(proveedor, entry.getValue() != null ? entry.getValue() : "");
        }
        return limpio;
    }

    /**
     * Normaliza las claves de un mapa String->Integer por proveedor, filtrando
     * proveedores inválidos y valores no positivos.
     */
    static Map<String, Integer> normalizarMapaIntPorProveedor(Map<String, Integer> mapa) {
        Map<String, Integer> limpio = new HashMap<>();
        if (mapa == null) {
            return limpio;
        }
        for (Map.Entry<String, Integer> entry : mapa.entrySet()) {
            if (entry == null || entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            String proveedor = normalizarProveedor(entry.getKey());
            if (proveedor.isEmpty() || !ProveedorAI.existeProveedor(proveedor)) {
                continue;
            }
            limpio.put(proveedor, entry.getValue());
        }
        return limpio;
    }

    /**
     * Normaliza una clave timeout en formato "proveedor::modelo".
     * Retorna "" si la clave está vacía o malformada (sin separador "::").
     */
    static String normalizarClaveTimeoutProveedorModelo(String clave) {
        if (Normalizador.esVacio(clave)) {
            return "";
        }
        String limpia = clave.trim();
        int separador = limpia.indexOf("::");
        if (separador <= 0) {
            return "";
        }
        String proveedor = normalizarProveedor(limpia.substring(0, separador));
        if (proveedor.isEmpty() || !ProveedorAI.existeProveedor(proveedor)) {
            return "";
        }
        String modelo = limpia.substring(separador + 2).trim();
        if (Normalizador.esVacio(modelo)) {
            return "";
        }
        return proveedor + "::" + modelo;
    }

    private static String normalizarProveedor(String proveedor) {
        return ProveedorAI.normalizarProveedor(proveedor);
    }
}
