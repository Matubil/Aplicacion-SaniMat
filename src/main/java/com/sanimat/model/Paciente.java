package com.sanimat.model;

/**
 * Paciente atendido por la clinica.
 * Extiende Persona y agrega datos de cobertura medica.
 */
public class Paciente extends Persona {
    private int pacienteId;
    private TipoCobertura tipoCobertura = TipoCobertura.PARTICULAR;
    private String numeroAfiliado;

    public int getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(int pacienteId) {
        this.pacienteId = pacienteId;
    }

    public TipoCobertura getTipoCobertura() {
        return tipoCobertura;
    }

    public void setTipoCobertura(TipoCobertura tipoCobertura) {
        this.tipoCobertura = tipoCobertura;
    }

    public String getNumeroAfiliado() {
        return numeroAfiliado;
    }

    public void setNumeroAfiliado(String numeroAfiliado) {
        this.numeroAfiliado = numeroAfiliado;
    }
}
