package com.burpia.i18n;

import com.burpia.util.Normalizador;

import java.util.Locale;

/**
 * Enumeración que define los idiomas soportados por la interfaz de usuario.
 * <p>
 * Actualmente soporta español (ES) e inglés (EN).
 * </p>
 */
public enum IdiomaUI {
    ES("es", "Español"),
    EN("en", "English");

    private final String codigo;
    private final String etiqueta;

    IdiomaUI(String codigo, String etiqueta) {
        this.codigo = codigo;
        this.etiqueta = etiqueta;
    }

    /**
     * Obtiene el código ISO del idioma.
     *
     * @return el código del idioma (ej: "es", "en")
     */
    public String codigo() {
        return codigo;
    }

    /**
     * Obtiene la etiqueta legible del idioma en su propio idioma.
     *
     * @return la etiqueta del idioma (ej: "Español", "English")
     */
    public String etiqueta() {
        return etiqueta;
    }

    /**
     * Obtiene el idioma por defecto del sistema.
     *
     * @return el idioma por defecto (ES)
     */
    public static IdiomaUI porDefecto() {
        return ES;
    }

    /**
     * Obtiene el idioma a partir de su código ISO.
     * <p>
     * Si el código es nulo, vacío o no corresponde a ningún idioma soportado,
     * retorna el idioma por defecto.
     * </p>
     *
     * @param codigo el código ISO del idioma (ej: "es", "en")
     * @return el idioma correspondiente o el idioma por defecto si no se encuentra
     */
    public static IdiomaUI desdeCodigo(String codigo) {
        if (Normalizador.esVacio(codigo)) {
            return porDefecto();
        }
        String limpio = codigo.trim().toLowerCase(Locale.ROOT);
        for (IdiomaUI idioma : values()) {
            if (idioma.codigo.equals(limpio)) {
                return idioma;
            }
        }
        return porDefecto();
    }

    /**
     * Retorna la etiqueta del idioma como representación en cadena.
     *
     * @return la etiqueta del idioma
     */
    @Override
    public String toString() {
        return etiqueta;
    }
}
