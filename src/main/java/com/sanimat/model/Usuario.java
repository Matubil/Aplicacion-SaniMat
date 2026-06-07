package com.sanimat.model;

/**
   Usuario autenticado en el sistema junto con su rol y posible relacion
   con medico, paciente o secretario.
 */
public class Usuario {
    private int id;
    private String username;
    private String passwordHash;
    private Rol rol;
    private Integer personaId;
    private Integer medicoId;
    private Integer secretarioId;
    private Integer pacienteId;
    private String nombreCompleto;
    private boolean activo = true;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public Integer getPersonaId() {
        return personaId;
    }

    public void setPersonaId(Integer personaId) {
        this.personaId = personaId;
    }

    public Integer getMedicoId() {
        return medicoId;
    }

    public void setMedicoId(Integer medicoId) {
        this.medicoId = medicoId;
    }

    public Integer getSecretarioId() {
        return secretarioId;
    }

    public void setSecretarioId(Integer secretarioId) {
        this.secretarioId = secretarioId;
    }

    public Integer getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(Integer pacienteId) {
        this.pacienteId = pacienteId;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean hasRole(RoleName roleName) {
        return rol != null && rol.getNombre() == roleName;
    }
}
