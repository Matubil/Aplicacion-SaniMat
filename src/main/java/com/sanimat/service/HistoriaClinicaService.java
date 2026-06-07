package com.sanimat.service;

import com.sanimat.dao.HistoriaClinicaDao;
import com.sanimat.model.HistoriaClinica;
import com.sanimat.model.Usuario;
import com.sanimat.util.ValidationException;
import com.sanimat.util.Validator;

import java.util.List;

/**
   Reglas para crear, consultar, modificar y eliminar historias clinicas.
 */
public class HistoriaClinicaService {
    private final HistoriaClinicaDao historiaClinicaDao;

    public HistoriaClinicaService(HistoriaClinicaDao historiaClinicaDao) {
        this.historiaClinicaDao = historiaClinicaDao;
    }

    public List<HistoriaClinica> search(Integer pacienteId, String filtro, Usuario usuario) {
        return historiaClinicaDao.search(pacienteId, null, filtro);
    }

    public void save(HistoriaClinica historia) {
        Validator.notNull(historia.getPacienteId() == 0 ? null : historia.getPacienteId(), "paciente");
        Validator.notNull(historia.getFechaAtencion(), "fecha");
        Validator.required(historia.getDescripcion(), "descripcion");
        historiaClinicaDao.save(historia);
    }

    public void delete(HistoriaClinica historia) {
        if (historia == null || historia.getId() == 0) {
            throw new ValidationException("Seleccione una historia clinica para eliminar.");
        }
        historiaClinicaDao.delete(historia.getId());
    }
}
