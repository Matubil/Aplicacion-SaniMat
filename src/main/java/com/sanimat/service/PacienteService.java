package com.sanimat.service;

import com.sanimat.dao.PacienteDao;
import com.sanimat.model.Paciente;
import com.sanimat.model.TipoCobertura;
import com.sanimat.util.TextNormalizer;
import com.sanimat.util.ValidationException;
import com.sanimat.util.Validator;

import java.util.List;

/**
   Reglas de negocio para pacientes.
   Valida datos personales y controla dependencias antes de eliminar.
 */
public class PacienteService {
    private final PacienteDao pacienteDao;

    public PacienteService(PacienteDao pacienteDao) {
        this.pacienteDao = pacienteDao;
    }

    public List<Paciente> findAll(String filtro) {
        return pacienteDao.findAll(filtro);
    }

    public void save(Paciente paciente) {
        normalize(paciente);
        validate(paciente);
        if (pacienteDao.existsDniExceptPersona(paciente.getDni(), paciente.getId())) {
            throw new ValidationException("Ya existe una persona registrada con ese DNI.");
        }
        pacienteDao.save(paciente);
    }

    public void delete(Paciente paciente) {
        if (paciente == null || paciente.getPacienteId() == 0) {
            throw new ValidationException("Seleccione un paciente para eliminar.");
        }
        if (pacienteDao.hasActiveDependencies(paciente.getPacienteId())) {
            throw new ValidationException("No se puede eliminar un paciente con turnos confirmados o historia clinica.");
        }
        pacienteDao.delete(paciente.getPacienteId());
    }

    private void validate(Paciente paciente) {
        Validator.required(paciente.getNombre(), "nombre");
        Validator.required(paciente.getApellido(), "apellido");
        Validator.required(paciente.getGenero(), "genero");
        Validator.dni(paciente.getDni());
        Validator.birthDate(paciente.getFechaNacimiento(), "fecha de nacimiento");
        Validator.notNull(paciente.getTipoCobertura(), "tipo de cobertura");
        if (paciente.getTipoCobertura() == TipoCobertura.OBRA_SOCIAL) {
            Validator.required(paciente.getNumeroAfiliado(), "numero de afiliado");
        }
        Validator.email(paciente.getEmail());
        Validator.phone(paciente.getTelefono());
    }

    private void normalize(Paciente paciente) {
        paciente.setNombre(TextNormalizer.capitalizeWords(paciente.getNombre()));
        paciente.setApellido(TextNormalizer.capitalizeWords(paciente.getApellido()));
        paciente.setDireccion(TextNormalizer.capitalizeWords(paciente.getDireccion()));
    }
}
