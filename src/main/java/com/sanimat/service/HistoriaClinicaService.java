package com.sanimat.service;

import com.sanimat.dao.HistoriaClinicaDao;
import com.sanimat.model.HistoriaClinica;
import com.sanimat.model.Usuario;
import com.sanimat.util.Validator;

import java.util.List;

/**
   Reglas para crear, consultar y modificar historias clinicas.
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
}
