package com.sanimat.model;

/**
 * Roles funcionales usados para habilitar menues y permisos.
 */
public enum RoleName {
    SECRETARIA("Secretaria"),
    MEDICO("Medico"),
    PACIENTE("Paciente");

    private final String displayName;

    RoleName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
