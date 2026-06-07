package com.sanimat.dao;

import com.sanimat.config.DatabaseConfig;
import com.sanimat.model.Especialidad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
   Acceso JDBC para especialidades: busqueda, alta, modificacion y baja.
 */
public class EspecialidadDao {
    private final DatabaseConfig databaseConfig;

    public EspecialidadDao(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public List<Especialidad> findAll(String filtro) {
        String sql = """
                SELECT especialidad_id, nombre_especialidad, descripcion
                FROM especialidades
                WHERE (? IS NULL OR LOWER(nombre_especialidad) LIKE LOWER(?))
                ORDER BY nombre_especialidad
                """;
        String normalized = normalizeFilter(filtro);
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindOptionalFilter(statement, normalized);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Especialidad> especialidades = new ArrayList<>();
                while (resultSet.next()) {
                    especialidades.add(map(resultSet));
                }
                return especialidades;
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudieron listar las especialidades.", ex);
        }
    }

    public Optional<Especialidad> findById(int id) {
        String sql = """
                SELECT especialidad_id, nombre_especialidad, descripcion
                FROM especialidades
                WHERE especialidad_id = ?
                """;
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudo buscar la especialidad.", ex);
        }
    }

    public void save(Especialidad especialidad) {
        if (especialidad.getId() == 0) {
            insert(especialidad);
        } else {
            update(especialidad);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM especialidades WHERE especialidad_id = ?";
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DaoException("No se pudo eliminar la especialidad.", ex);
        }
    }

    public boolean existsByNameExceptId(String nombre, int id) {
        String sql = "SELECT COUNT(*) FROM especialidades WHERE LOWER(nombre_especialidad) = LOWER(?) AND especialidad_id <> ?";
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nombre);
            statement.setInt(2, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudo validar la especialidad.", ex);
        }
    }

    private void insert(Especialidad especialidad) {
        String sql = "INSERT INTO especialidades(nombre_especialidad, descripcion) VALUES (?, ?)";
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, especialidad.getNombre());
            statement.setString(2, especialidad.getDescripcion());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    especialidad.setId(keys.getInt(1));
                }
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudo crear la especialidad.", ex);
        }
    }

    private void update(Especialidad especialidad) {
        String sql = "UPDATE especialidades SET nombre_especialidad = ?, descripcion = ? WHERE especialidad_id = ?";
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, especialidad.getNombre());
            statement.setString(2, especialidad.getDescripcion());
            statement.setInt(3, especialidad.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DaoException("No se pudo actualizar la especialidad.", ex);
        }
    }

    private Especialidad map(ResultSet resultSet) throws SQLException {
        Especialidad especialidad = new Especialidad();
        especialidad.setId(resultSet.getInt("especialidad_id"));
        especialidad.setNombre(resultSet.getString("nombre_especialidad"));
        especialidad.setDescripcion(resultSet.getString("descripcion"));
        especialidad.setActivo(true);
        return especialidad;
    }

    private String normalizeFilter(String filtro) {
        return filtro == null || filtro.isBlank() ? null : "%" + filtro.trim() + "%";
    }

    private void bindOptionalFilter(PreparedStatement statement, String normalized) throws SQLException {
        statement.setString(1, normalized);
        statement.setString(2, normalized);
    }
}
