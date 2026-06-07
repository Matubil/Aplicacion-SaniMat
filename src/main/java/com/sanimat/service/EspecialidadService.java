package com.sanimat.service;

import com.sanimat.dao.EspecialidadDao;
import com.sanimat.model.Especialidad;
import com.sanimat.util.ValidationException;
import com.sanimat.util.Validator;

import java.util.List;

/**
   Reglas de negocio para especialidades.
   Valida datos y evita duplicados antes de guardar.
 */
public class EspecialidadService {
    private final EspecialidadDao especialidadDao;

    public EspecialidadService(EspecialidadDao especialidadDao) {
        this.especialidadDao = especialidadDao;
    }

    public List<Especialidad> findAll(String filtro) {
        return especialidadDao.findAll(filtro);
    }

    public void save(Especialidad especialidad) {
        Validator.required(especialidad.getNombre(), "nombre de especialidad");
        if (especialidadDao.existsByNameExceptId(especialidad.getNombre(), especialidad.getId())) {
            throw new ValidationException("Ya existe una especialidad con ese nombre.");
        }
        especialidadDao.save(especialidad);
    }

    public void delete(Especialidad especialidad) {
        if (especialidad == null || especialidad.getId() == 0) {
            throw new ValidationException("Seleccione una especialidad para eliminar.");
        }
        especialidadDao.delete(especialidad.getId());
    }
}
