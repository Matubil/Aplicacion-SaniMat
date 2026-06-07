package com.sanimat.model;

/**
   Medios de pago admitidos por el sistema.
 */
public enum MedioPago {
    EFECTIVO("Efectivo"),
    TARJETA("Tarjeta"),
    TRANSFERENCIA("Transferencia"),
    NO_REQUIERE_PAGO("No requiere pago");

    private final String displayName;

    MedioPago(String displayName) {
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
