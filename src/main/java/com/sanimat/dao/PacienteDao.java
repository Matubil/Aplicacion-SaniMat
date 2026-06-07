package com.sanimat.dao;

import com.sanimat.config.DatabaseConfig;
import com.sanimat.model.Paciente;
import com.sanimat.model.Persona;
import com.sanimat.model.TipoCobertura;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
   Acceso JDBC para pacientes y datos personales asociados.
 */
public class PacienteDao {
    private final DatabaseConfig databaseConfig;

    public PacienteDao(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public List<Paciente> findAll(String filtro) {
        String sql = """
                SELECT p.persona_id, p.dni, p.nombre, p.apellido, p.genero, p.fecha_nacimiento,
                       p.direccion, p.telefono, p.mail,
                       pa.paciente_id, pa.numero_afiliado,
                       tc.nombre AS tipo_cobertura
                FROM pacientes pa
                JOIN personas p ON p.persona_id = pa.persona_id
                JOIN enum_tipo_cobertura tc ON tc.tipo_cobertura_id = pa.tipo_cobertura_id
                WHERE (? IS NULL OR LOWER(p.apellido || ' ' || p.nombre) LIKE LOWER(?) OR p.dni LIKE ?)
                ORDER BY p.apellido, p.nombre
                """;
        String normalized = filtro == null || filtro.isBlank() ? null : "%" + filtro.trim() + "%";
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalized);
            statement.setString(2, normalized);
            statement.setString(3, normalized);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Paciente> pacientes = new ArrayList<>();
                while (resultSet.next()) {
                    pacientes.add(map(resultSet));
                }
                return pacientes;
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudieron listar los pacientes.", ex);
        }
    }

    public Optional<Paciente> findById(int pacienteId) {
        String sql = """
                SELECT p.persona_id, p.dni, p.nombre, p.apellido, p.genero, p.fecha_nacimiento,
                       p.direccion, p.telefono, p.mail,
                       pa.paciente_id, pa.numero_afiliado,
                       tc.nombre AS tipo_cobertura
                FROM pacientes pa
                JOIN personas p ON p.persona_id = pa.persona_id
                JOIN enum_tipo_cobertura tc ON tc.tipo_cobertura_id = pa.tipo_cobertura_id
                WHERE pa.paciente_id = ?
                """;
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, pacienteId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudo buscar el paciente.", ex);
        }
    }

    public void save(Paciente paciente) {
        try (Connection connection = databaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (paciente.getId() == 0) {
                    int personaId = insertPersona(connection, paciente);
                    paciente.setId(personaId);
                    insertPaciente(connection, paciente);
                } else {
                    updatePersona(connection, paciente);
                    updatePaciente(connection, paciente);
                }
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudo guardar el paciente.", ex);
        }
    }

    public void delete(int pacienteId) {
        try (Connection connection = databaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int personaId = findPersonaId(connection, pacienteId);
                try (PreparedStatement patientStatement = connection.prepareStatement("DELETE FROM pacientes WHERE paciente_id = ?")) {
                    patientStatement.setInt(1, pacienteId);
                    patientStatement.executeUpdate();
                }
                try (PreparedStatement personStatement = connection.prepareStatement("DELETE FROM personas WHERE persona_id = ?")) {
                    personStatement.setInt(1, personaId);
                    personStatement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudo eliminar el paciente.", ex);
        }
    }

    public boolean existsDniExceptPersona(String dni, int personaId) {
        String sql = "SELECT COUNT(*) FROM personas WHERE dni = ? AND persona_id <> ?";
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, dni);
            statement.setInt(2, personaId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudo validar el DNI.", ex);
        }
    }

    public boolean hasActiveDependencies(int pacienteId) {
        String sql = """
                SELECT
                    (SELECT COUNT(*)
                     FROM turnos t
                     JOIN enum_estado_turno et ON et.estado_turno_id = t.estado_turno_id
                     WHERE t.paciente_id = ? AND et.nombre = 'CONFIRMADO') +
                    (SELECT COUNT(*) FROM historias_clinicas WHERE paciente_id = ?) AS total
                """;
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, pacienteId);
            statement.setInt(2, pacienteId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt("total") > 0;
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudieron validar las dependencias del paciente.", ex);
        }
    }

    private int findPersonaId(Connection connection, int pacienteId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT persona_id FROM pacientes WHERE paciente_id = ?")) {
            statement.setInt(1, pacienteId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Paciente inexistente");
                }
                return resultSet.getInt("persona_id");
            }
        }
    }

    private int insertPersona(Connection connection, Persona persona) throws SQLException {
        String sql = """
                INSERT INTO personas(dni, nombre, apellido, genero, fecha_nacimiento, direccion, telefono, mail)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindPersona(statement, persona);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private void updatePersona(Connection connection, Persona persona) throws SQLException {
        String sql = """
                UPDATE personas
                SET dni = ?, nombre = ?, apellido = ?, genero = ?, fecha_nacimiento = ?, direccion = ?, telefono = ?, mail = ?
                WHERE persona_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindPersona(statement, persona);
            statement.setInt(9, persona.getId());
            statement.executeUpdate();
        }
    }

    private void insertPaciente(Connection connection, Paciente paciente) throws SQLException {
        String sql = """
                INSERT INTO pacientes(persona_id, numero_afiliado, tipo_cobertura_id)
                VALUES (?, ?, (SELECT tipo_cobertura_id FROM enum_tipo_cobertura WHERE nombre = ?))
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, paciente.getId());
            statement.setString(2, paciente.getNumeroAfiliado());
            statement.setString(3, paciente.getTipoCobertura().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                paciente.setPacienteId(keys.getInt(1));
            }
        }
    }

    private void updatePaciente(Connection connection, Paciente paciente) throws SQLException {
        String sql = """
                UPDATE pacientes
                SET numero_afiliado = ?,
                    tipo_cobertura_id = (SELECT tipo_cobertura_id FROM enum_tipo_cobertura WHERE nombre = ?)
                WHERE paciente_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, paciente.getNumeroAfiliado());
            statement.setString(2, paciente.getTipoCobertura().name());
            statement.setInt(3, paciente.getPacienteId());
            statement.executeUpdate();
        }
    }

    private void bindPersona(PreparedStatement statement, Persona persona) throws SQLException {
        statement.setString(1, persona.getDni());
        statement.setString(2, persona.getNombre());
        statement.setString(3, persona.getApellido());
        statement.setString(4, persona.getGenero());
        setDate(statement, 5, persona.getFechaNacimiento());
        statement.setString(6, persona.getDireccion());
        statement.setString(7, persona.getTelefono());
        statement.setString(8, persona.getEmail());
    }

    private void setDate(PreparedStatement statement, int index, LocalDate date) throws SQLException {
        if (date == null) {
            statement.setNull(index, Types.DATE);
        } else {
            statement.setDate(index, Date.valueOf(date));
        }
    }

    private Paciente map(ResultSet resultSet) throws SQLException {
        Paciente paciente = new Paciente();
        paciente.setId(resultSet.getInt("persona_id"));
        paciente.setPacienteId(resultSet.getInt("paciente_id"));
        paciente.setDni(resultSet.getString("dni"));
        paciente.setNombre(resultSet.getString("nombre"));
        paciente.setApellido(resultSet.getString("apellido"));
        paciente.setGenero(resultSet.getString("genero"));
        Date fechaNacimiento = resultSet.getDate("fecha_nacimiento");
        paciente.setFechaNacimiento(fechaNacimiento == null ? null : fechaNacimiento.toLocalDate());
        paciente.setDireccion(resultSet.getString("direccion"));
        paciente.setTelefono(resultSet.getString("telefono"));
        paciente.setEmail(resultSet.getString("mail"));
        paciente.setNumeroAfiliado(resultSet.getString("numero_afiliado"));
        paciente.setTipoCobertura(TipoCobertura.valueOf(resultSet.getString("tipo_cobertura")));
        return paciente;
    }
}
