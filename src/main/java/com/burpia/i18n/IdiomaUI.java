package com.burpia.i18n;

public enum IdiomaUI {
    ES("es", "Espanol"),
    EN("en", "English");

    private final String codigo;
    private final String etiqueta;

    IdiomaUI(String codigo, String etiqueta) {
        this.codigo = codigo;
        this.etiqueta = etiqueta;
    }

    public String codigo() {
        return codigo;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public static IdiomaUI porDefecto() {
        return ES;
    }

    public static IdiomaUI desdeCodigo(String codigo) {
        if (codigo == null) {
            return porDefecto();
        }
        String limpio = codigo.trim().toLowerCase();
        for (IdiomaUI idioma : values()) {
            if (idioma.codigo.equals(limpio)) {
                return idioma;
            }
        }
        return porDefecto();
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
