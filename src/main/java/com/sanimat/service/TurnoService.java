package com.sanimat.service;

import com.sanimat.dao.TurnoDao;
import com.sanimat.model.EstadoTurno;
import com.sanimat.model.HorarioMedico;
import com.sanimat.model.RoleName;
import com.sanimat.model.Turno;
import com.sanimat.model.Usuario;
import com.sanimat.util.ValidationException;
import com.sanimat.util.Validator;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
   Reglas de negocio para turnos.
   Controla disponibilidad, estados permitidos y permisos segun rol.
 */
public class TurnoService {
    private final TurnoDao turnoDao;

    public TurnoService(TurnoDao turnoDao) {
        this.turnoDao = turnoDao;
    }

    public List<Turno> search(String filtro, LocalDate fecha, Usuario usuario) {
        if (usuario != null && usuario.hasRole(RoleName.MEDICO) && usuario.getMedicoId() != null) {
            return turnoDao.searchForMedico(usuario.getMedicoId(), filtro, fecha);
        }
        if (usuario != null && usuario.hasRole(RoleName.PACIENTE) && usuario.getPacienteId() != null) {
            return turnoDao.searchForPaciente(usuario.getPacienteId(), filtro, fecha);
        }
        return turnoDao.search(filtro, fecha);
    }

    public List<Turno> findUnpaidAppointments() {
        return turnoDao.findUnpaidAppointments();
    }

    public List<HorarioMedico> findWorkSchedules(int medicoId) {
        return turnoDao.findWorkSchedules(medicoId);
    }

    public List<LocalTime> findBookedTimes(int medicoId, LocalDate date, int excludeTurnoId) {
        return turnoDao.findBookedTimes(medicoId, date, excludeTurnoId);
    }

    public void save(Turno turno, Usuario usuario) {
        Turno savedTurno = findSavedTurno(turno);
        // El alta/modificacion completa de turnos queda reservada a secretaria.
        // El medico usa un metodo especifico que solo cambia el estado de atencion.
        if (usuario != null && usuario.hasRole(RoleName.MEDICO)) {
            throw new ValidationException("El medico solo puede actualizar el estado de sus turnos desde la pantalla Mis turnos.");
        }
        ensureEditable(savedTurno);
        validate(turno);
        validateStatusRules(turno, savedTurno);
        if (usuario != null && usuario.hasRole(RoleName.SECRETARIA)) {
            turno.setSecretarioId(usuario.getSecretarioId());
        }
        if (!turnoDao.isInsideWorkSchedule(turno.getMedicoId(), turno.getFechaHora())) {
            throw new ValidationException("El turno esta fuera del horario laboral del medico.");
        }
        if (turnoDao.hasMedicoOverlap(turno.getMedicoId(), turno.getFechaHora(), turno.getId())) {
            throw new ValidationException("El medico ya tiene un turno confirmado en esa fecha y hora.");
        }
        turnoDao.save(turno);
    }

    public void updateAttendanceStatusForMedico(Turno turno, EstadoTurno newStatus, Usuario usuario) {
        // Reglas de seguridad del rol medico: solo puede cerrar turnos propios,
        // confirmados y correspondientes al dia actual o a fechas anteriores.
        if (usuario == null || !usuario.hasRole(RoleName.MEDICO) || usuario.getMedicoId() == null) {
            throw new ValidationException("Solo un medico puede actualizar la atencion del turno.");
        }
        if (turno == null || turno.getId() == 0) {
            throw new ValidationException("Seleccione un turno para actualizar.");
        }
        if (newStatus != EstadoTurno.FINALIZADO && newStatus != EstadoTurno.AUSENTE) {
            throw new ValidationException("El medico solo puede marcar el turno como finalizado o ausente.");
        }

        Turno savedTurno = findSavedTurno(turno);
        if (savedTurno.getMedicoId() != usuario.getMedicoId()) {
            throw new ValidationException("Solo puede actualizar turnos asignados a usted.");
        }
        if (savedTurno.getEstado() != EstadoTurno.CONFIRMADO) {
            throw new ValidationException("Solo se pueden actualizar turnos confirmados.");
        }
        if (savedTurno.getFechaHora().toLocalDate().isAfter(LocalDate.now())) {
            throw new ValidationException("Solo se puede marcar un turno como ausente o finalizado si corresponde al dia actual o a una fecha anterior.");
        }
        turnoDao.updateStatus(savedTurno.getId(), newStatus);
    }

    public void cancel(Turno turno) {
        if (turno == null || turno.getId() == 0) {
            throw new ValidationException("Seleccione un turno para cancelar.");
        }
        if (turno.getEstado() == EstadoTurno.CANCELADO) {
            throw new ValidationException("El turno ya se encuentra cancelado.");
        }
        if (turno.getEstado() == EstadoTurno.FINALIZADO) {
            throw new ValidationException("No se puede cancelar un turno finalizado.");
        }
        long hours = Duration.between(LocalDateTime.now(), turno.getFechaHora()).toHours();
        if (hours < 24) {
            throw new ValidationException("Solo se pueden cancelar turnos con al menos 24 horas de anticipacion.");
        }
        turnoDao.cancel(turno.getId());
    }

    private Turno findSavedTurno(Turno turno) {
        if (turno == null || turno.getId() == 0) {
            return null;
        }
        return turnoDao.findById(turno.getId())
                .orElseThrow(() -> new ValidationException("No se encontro el turno seleccionado."));
    }

    private void ensureEditable(Turno savedTurno) {
        if (savedTurno == null) {
            return;
        }
        if (savedTurno.getEstado() == EstadoTurno.FINALIZADO) {
            throw new ValidationException("No se puede modificar un turno finalizado.");
        }
        if (savedTurno.getEstado() == EstadoTurno.CANCELADO) {
            throw new ValidationException("No se puede modificar un turno cancelado.");
        }
    }

    private void validateStatusRules(Turno turno, Turno savedTurno) {
        if (turno.getId() == 0 && turno.getEstado() != EstadoTurno.CONFIRMADO) {
            throw new ValidationException("Un turno nuevo debe registrarse como confirmado.");
        }
        if (savedTurno != null && savedTurno.getEstado() != EstadoTurno.CANCELADO && turno.getEstado() == EstadoTurno.CANCELADO) {
            throw new ValidationException("Para cancelar un turno use el boton Cancelar turno.");
        }
        if ((turno.getEstado() == EstadoTurno.AUSENTE || turno.getEstado() == EstadoTurno.FINALIZADO)
                && turno.getFechaHora().isAfter(LocalDateTime.now())) {
            throw new ValidationException("Solo se puede marcar un turno como ausente o finalizado cuando su fecha y hora ya pasaron.");
        }
    }

    private void validate(Turno turno) {
        Validator.notNull(turno.getPacienteId() == 0 ? null : turno.getPacienteId(), "paciente");
        Validator.notNull(turno.getMedicoId() == 0 ? null : turno.getMedicoId(), "medico");
        Validator.notNull(turno.getFechaHora(), "fecha y hora");
        if (turno.getEstado() == EstadoTurno.CONFIRMADO) {
            Validator.future(turno.getFechaHora(), "fecha y hora del turno");
        }
        validateHalfHourSlot(turno.getFechaHora().toLocalTime());
        Validator.notNull(turno.getEstado(), "estado del turno");
    }

    private void validateHalfHourSlot(LocalTime time) {
        boolean validMinute = time.getMinute() == 0 || time.getMinute() == 30;
        boolean validPrecision = time.getSecond() == 0 && time.getNano() == 0;
        if (!validMinute || !validPrecision) {
            throw new ValidationException("Los turnos deben cargarse en horarios de 30 minutos, por ejemplo 08:00, 08:30 o 09:00.");
        }
    }
}
