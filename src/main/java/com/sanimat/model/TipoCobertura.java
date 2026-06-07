package com.sanimat.model;

/**
 * Tipo de cobertura del paciente.
 */
public enum TipoCobertura {
    PARTICULAR("Particular"),
    OBRA_SOCIAL("Obra social");

    private final String displayName;

    TipoCobertura(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
