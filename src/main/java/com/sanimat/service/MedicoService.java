package com.sanimat.service;

import com.sanimat.dao.MedicoDao;
import com.sanimat.model.HorarioMedico;
import com.sanimat.model.Medico;
import com.sanimat.util.TextNormalizer;
import com.sanimat.util.ValidationException;
import com.sanimat.util.Validator;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/**
 * Reglas de negocio para medicos.
 * Valida datos personales, matricula, usuario y horarios laborales.
 */
public class MedicoService {
    private final MedicoDao medicoDao;

    public MedicoService(MedicoDao medicoDao) {
        this.medicoDao = medicoDao;
    }

    public List<Medico> findAll(String filtro) {
        return medicoDao.findAll(filtro);
    }

    public List<HorarioMedico> findWorkSchedules(int medicoId) {
        return medicoDao.findWorkSchedules(medicoId);
    }

    public Medico findById(int medicoId) {
        return medicoDao.findById(medicoId)
                .orElseThrow(() -> new ValidationException("No se encontró el médico solicitado."));
    }

    public void save(Medico medico) {
        normalize(medico);
        validate(medico);
        if (medicoDao.existsDniExceptPersona(medico.getDni(), medico.getId())) {
            throw new ValidationException("Ya existe una persona registrada con ese DNI.");
        }
        if (medicoDao.existsMatriculaExceptMedico(medico.getMatricula(), medico.getMedicoId())) {
            throw new ValidationException("Ya existe un medico con esa matricula.");
        }
        if (medicoDao.existsUsuarioExceptMedico(medico.getUsuario(), medico.getMedicoId())) {
            throw new ValidationException("Ya existe un medico con ese usuario.");
        }
        medicoDao.save(medico);
    }

    public void delete(Medico medico) {
        if (medico == null || medico.getMedicoId() == 0) {
            throw new ValidationException("Seleccione un medico para eliminar.");
        }
        if (medicoDao.hasAppointments(medico.getMedicoId())) {
            throw new ValidationException("No se puede eliminar un medico con turnos registrados.");
        }
        medicoDao.delete(medico.getMedicoId());
    }

    private void validate(Medico medico) {
        Validator.required(medico.getNombre(), "nombre");
        Validator.required(medico.getApellido(), "apellido");
        Validator.dni(medico.getDni());
        Validator.adultBirthDate(medico.getFechaNacimiento(), "fecha de nacimiento", 18);
        Validator.matricula(medico.getMatricula());
        Validator.required(medico.getUsuario(), "usuario");
        Validator.required(medico.getContrasenia(), "contraseña");
        Validator.notNull(medico.getEspecialidadId() == 0 ? null : medico.getEspecialidadId(), "especialidad");
        Validator.email(medico.getEmail());
        Validator.phone(medico.getTelefono());
        validateWorkSchedules(medico.getHorarios());
    }

    private void validateWorkSchedules(List<HorarioMedico> horarios) {
        if (horarios == null) {
            return;
        }
        if (horarios.isEmpty()) {
            throw new ValidationException("Debe cargar al menos un horario laboral del medico.");
        }
        for (HorarioMedico horario : horarios) {
            if (horario.getDiaSemana() < 1 || horario.getDiaSemana() > 7) {
                throw new ValidationException("Seleccione un dia laboral valido.");
            }
            Validator.notNull(horario.getHoraDesde(), "hora desde");
            Validator.notNull(horario.getHoraHasta(), "hora hasta");
            if (!horario.getHoraDesde().isBefore(horario.getHoraHasta())) {
                throw new ValidationException("La hora desde debe ser menor que la hora hasta.");
            }
            validateHalfHour(horario.getHoraDesde());
            validateHalfHour(horario.getHoraHasta());
            horario.setActivo(true);
        }
        // Ordena por dia y hora para detectar rangos superpuestos de manera simple.
        List<HorarioMedico> sorted = horarios.stream()
                .sorted(Comparator.comparingInt(HorarioMedico::getDiaSemana)
                        .thenComparing(HorarioMedico::getHoraDesde))
                .toList();
        for (int i = 1; i < sorted.size(); i++) {
            HorarioMedico previous = sorted.get(i - 1);
            HorarioMedico current = sorted.get(i);
            if (previous.getDiaSemana() == current.getDiaSemana()
                    && previous.getHoraHasta().isAfter(current.getHoraDesde())) {
                throw new ValidationException("Los horarios laborales del mismo dia no deben superponerse.");
            }
        }
    }

    private void validateHalfHour(LocalTime time) {
        boolean validMinute = time.getMinute() == 0 || time.getMinute() == 30;
        boolean validPrecision = time.getSecond() == 0 && time.getNano() == 0;
        if (!validMinute || !validPrecision) {
            throw new ValidationException("Los horarios laborales deben cargarse en bloques de 30 minutos.");
        }
    }

    private void normalize(Medico medico) {
        medico.setNombre(TextNormalizer.capitalizeWords(medico.getNombre()));
        medico.setApellido(TextNormalizer.capitalizeWords(medico.getApellido()));
        medico.setDireccion(TextNormalizer.capitalizeWords(medico.getDireccion()));
    }
}
