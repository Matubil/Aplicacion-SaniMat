package com.sanimat.model;

/**
 * Secretario o secretaria que administra agenda, pacientes, pagos y medicos.
 */
public class Secretario extends Persona {
    private int secretarioId;
    private String usuario;
    private String contrasenia;

    public int getSecretarioId() {
        return secretarioId;
    }

    public void setSecretarioId(int secretarioId) {
        this.secretarioId = secretarioId;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getContrasenia() {
        return contrasenia;
    }

    public void setContrasenia(String contrasenia) {
        this.contrasenia = contrasenia;
    }
}
