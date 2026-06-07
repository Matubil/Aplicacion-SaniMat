package com.sanimat.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Turno medico agendado.
 * Combina datos propios del turno con nombres auxiliares para mostrar en tablas.
 */
public class Turno {
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int id;
    private int pacienteId;
    private int medicoId;
    private Integer secretarioId;
    private int especialidadId;
    private LocalDateTime fechaHora;
    private EstadoTurno estado = EstadoTurno.CONFIRMADO;
    private String motivo;
    private String observaciones;
    private String pacienteNombre;
    private String pacienteDni;
    private String tipoCobertura;
    private String numeroAfiliado;
    private String medicoNombre;
    private String especialidadNombre;
    private boolean pagoRegistrado;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(int pacienteId) {
        this.pacienteId = pacienteId;
    }

    public int getMedicoId() {
        return medicoId;
    }

    public void setMedicoId(int medicoId) {
        this.medicoId = medicoId;
    }

    public Integer getSecretarioId() {
        return secretarioId;
    }

    public void setSecretarioId(Integer secretarioId) {
        this.secretarioId = secretarioId;
    }

    public int getEspecialidadId() {
        return especialidadId;
    }

    public void setEspecialidadId(int especialidadId) {
        this.especialidadId = especialidadId;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public EstadoTurno getEstado() {
        return estado;
    }

    public void setEstado(EstadoTurno estado) {
        this.estado = estado;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getPacienteNombre() {
        return pacienteNombre;
    }

    public void setPacienteNombre(String pacienteNombre) {
        this.pacienteNombre = pacienteNombre;
    }

    public String getPacienteDni() {
        return pacienteDni;
    }

    public void setPacienteDni(String pacienteDni) {
        this.pacienteDni = pacienteDni;
    }

    public String getTipoCobertura() {
        return tipoCobertura;
    }

    public String getTipoCoberturaDisplay() {
        if (tipoCobertura == null || tipoCobertura.isBlank()) {
            return "";
        }
        try {
            return TipoCobertura.valueOf(tipoCobertura).getDisplayName();
        } catch (IllegalArgumentException ex) {
            String[] parts = tipoCobertura.toLowerCase().replace('_', ' ').split("\\s+");
            StringBuilder builder = new StringBuilder();
            for (String part : parts) {
                if (part.isBlank()) {
                    continue;
                }
                if (!builder.isEmpty()) {
                    builder.append(' ');
                }
                builder.append(Character.toTitleCase(part.charAt(0))).append(part.substring(1));
            }
            return builder.toString();
        }
    }

    public void setTipoCobertura(String tipoCobertura) {
        this.tipoCobertura = tipoCobertura;
    }

    public String getNumeroAfiliado() {
        return numeroAfiliado;
    }

    public void setNumeroAfiliado(String numeroAfiliado) {
        this.numeroAfiliado = numeroAfiliado;
    }

    public String getMedicoNombre() {
        return medicoNombre;
    }

    public void setMedicoNombre(String medicoNombre) {
        this.medicoNombre = medicoNombre;
    }

    public String getEspecialidadNombre() {
        return especialidadNombre;
    }

    public void setEspecialidadNombre(String especialidadNombre) {
        this.especialidadNombre = especialidadNombre;
    }

    public boolean isPagoRegistrado() {
        return pagoRegistrado;
    }

    public void setPagoRegistrado(boolean pagoRegistrado) {
        this.pagoRegistrado = pagoRegistrado;
    }

    @Override
    public String toString() {
        String nombre = pacienteNombre == null ? "Turno" : pacienteNombre;
        String fecha = fechaHora == null ? "Sin fecha" : fechaHora.format(DISPLAY_DATE_TIME);
        return "#" + id + " | " + nombre + " | " + fecha;
    }
}
