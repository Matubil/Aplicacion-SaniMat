package com.sanimat.model;

/**
 * Rol asignado a un usuario.
 */
public class Rol {
    private int id;
    private RoleName nombre;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public RoleName getNombre() {
        return nombre;
    }

    public void setNombre(RoleName nombre) {
        this.nombre = nombre;
    }
}
