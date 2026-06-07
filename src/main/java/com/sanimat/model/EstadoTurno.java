package com.sanimat.model;

/**
   Estados posibles de un turno dentro del prototipo.
 */
public enum EstadoTurno {
    CONFIRMADO("Confirmado"),
    CANCELADO("Cancelado"),
    AUSENTE("Ausente"),
    FINALIZADO("Finalizado");

    private final String displayName;

    EstadoTurno(String displayName) {
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
