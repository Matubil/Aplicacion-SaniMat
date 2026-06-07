package com.sanimat.dao;

import com.sanimat.config.DatabaseConfig;
import com.sanimat.model.HistoriaClinica;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
   Acceso JDBC para historias clinicas.
   Mantiene la lectura, escritura y baja de historias clinicas separada de la vista.
 */
public class HistoriaClinicaDao {
    private final DatabaseConfig databaseConfig;

    public HistoriaClinicaDao(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public List<HistoriaClinica> search(Integer pacienteId, Integer medicoId, String filtro) {
        StringBuilder sql = new StringBuilder(baseSelect());
        List<Object> params = new ArrayList<>();
        sql.append(" WHERE 1 = 1");

        if (pacienteId != null) {
            sql.append(" AND h.paciente_id = ?");
            params.add(pacienteId);
        }
        if (medicoId != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM turnos t WHERE t.paciente_id = h.paciente_id AND t.medico_id = ?)");
            params.add(medicoId);
        }
        if (filtro != null && !filtro.isBlank()) {
            sql.append("""
                     AND (
                        LOWER(h.descripcion) LIKE LOWER(?)
                        OR LOWER(pp.apellido || ' ' || pp.nombre) LIKE LOWER(?)
                     )
                    """);
            String normalized = "%" + filtro.trim() + "%";
            params.add(normalized);
            params.add(normalized);
        }
        sql.append(" ORDER BY h.fecha DESC, h.historia_clinica_id DESC");

        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<HistoriaClinica> historias = new ArrayList<>();
                while (resultSet.next()) {
                    historias.add(map(resultSet));
                }
                return historias;
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudieron listar las historias clinicas.", ex);
        }
    }

    public Optional<HistoriaClinica> findById(int id) {
        String sql = baseSelect() + " WHERE h.historia_clinica_id = ?";
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudo buscar la historia clinica.", ex);
        }
    }

    public void save(HistoriaClinica historia) {
        if (historia.getId() == 0) {
            insert(historia);
        } else {
            update(historia);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM historias_clinicas WHERE historia_clinica_id = ?";
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DaoException("No se pudo eliminar la historia clinica.", ex);
        }
    }

    private void insert(HistoriaClinica historia) {
        String sql = """
                INSERT INTO historias_clinicas(paciente_id, fecha, descripcion)
                VALUES (?, ?, ?)
                """;
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindHistoria(statement, historia);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    historia.setId(keys.getInt(1));
                }
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudo crear la historia clinica.", ex);
        }
    }

    private void update(HistoriaClinica historia) {
        String sql = """
                UPDATE historias_clinicas
                SET paciente_id = ?, fecha = ?, descripcion = ?
                WHERE historia_clinica_id = ?
                """;
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindHistoria(statement, historia);
            statement.setInt(4, historia.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DaoException("No se pudo actualizar la historia clinica.", ex);
        }
    }

    private void bindHistoria(PreparedStatement statement, HistoriaClinica historia) throws SQLException {
        statement.setInt(1, historia.getPacienteId());
        statement.setDate(2, java.sql.Date.valueOf(historia.getFechaAtencion().toLocalDate()));
        statement.setString(3, historia.getDescripcion());
    }

    private String baseSelect() {
        return """
                SELECT h.historia_clinica_id, h.paciente_id, h.fecha, h.descripcion,
                       CONCAT(pp.apellido, ', ', pp.nombre) AS paciente_nombre
                FROM historias_clinicas h
                JOIN pacientes pa ON pa.paciente_id = h.paciente_id
                JOIN personas pp ON pp.persona_id = pa.persona_id
                """;
    }

    private HistoriaClinica map(ResultSet resultSet) throws SQLException {
        HistoriaClinica historia = new HistoriaClinica();
        historia.setId(resultSet.getInt("historia_clinica_id"));
        historia.setPacienteId(resultSet.getInt("paciente_id"));
        LocalDate fecha = resultSet.getDate("fecha").toLocalDate();
        historia.setFechaAtencion(fecha.atStartOfDay());
        historia.setDescripcion(resultSet.getString("descripcion"));
        historia.setPacienteNombre(resultSet.getString("paciente_nombre"));
        return historia;
    }
}
