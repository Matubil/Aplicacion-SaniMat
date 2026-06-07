package com.sanimat.dao;

import com.sanimat.config.DatabaseConfig;
import com.sanimat.model.HorarioMedico;
import com.sanimat.model.Medico;
import com.sanimat.model.Persona;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acceso JDBC para medicos y sus horarios laborales.
 * Las operaciones de guardado usan transaccion porque afectan varias tablas.
 */
public class MedicoDao {
    private final DatabaseConfig databaseConfig;

    public MedicoDao(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public List<Medico> findAll(String filtro) {
        String sql = """
                SELECT p.persona_id, p.dni, p.nombre, p.apellido, p.genero, p.fecha_nacimiento,
                       p.direccion, p.telefono, p.mail,
                       m.medico_id, m.matricula, m.usuario, m.contrasenia,
                       e.especialidad_id, e.nombre_especialidad
                FROM medicos m
                JOIN personas p ON p.persona_id = m.persona_id
                JOIN especialidades e ON e.especialidad_id = m.especialidad_id
                WHERE (? IS NULL OR LOWER(p.apellido || ' ' || p.nombre) LIKE LOWER(?)
                       OR p.dni LIKE ? OR CAST(m.matricula AS TEXT) LIKE ?)
                ORDER BY p.apellido, p.nombre
                """;
        String normalized = filtro == null || filtro.isBlank() ? null : "%" + filtro.trim() + "%";
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalized);
            statement.setString(2, normalized);
            statement.setString(3, normalized);
            statement.setString(4, normalized);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Medico> medicos = new ArrayList<>();
                while (resultSet.next()) {
                    medicos.add(map(resultSet));
                }
                return medicos;
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudieron listar los medicos.", ex);
        }
    }

    public Optional<Medico> findById(int medicoId) {
        String sql = """
                SELECT p.persona_id, p.dni, p.nombre, p.apellido, p.genero, p.fecha_nacimiento,
                       p.direccion, p.telefono, p.mail,
                       m.medico_id, m.matricula, m.usuario, m.contrasenia,
                       e.especialidad_id, e.nombre_especialidad
                FROM medicos m
                JOIN personas p ON p.persona_id = m.persona_id
                JOIN especialidades e ON e.especialidad_id = m.especialidad_id
                WHERE m.medico_id = ?
                """;
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, medicoId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudo buscar el medico.", ex);
        }
    }

    public List<HorarioMedico> findWorkSchedules(int medicoId) {
        try (Connection connection = databaseConfig.getConnection()) {
            return findWorkSchedules(connection, medicoId);
        } catch (SQLException ex) {
            throw new DaoException("No se pudieron listar los horarios laborales del medico.", ex);
        }
    }

    public void save(Medico medico) {
        try (Connection connection = databaseConfig.getConnection()) {
            // Medico se guarda en personas, medicos y horarios_laborales.
            // Por eso se usa transaccion: si falla una parte, se revierte todo.
            connection.setAutoCommit(false);
            try {
                if (medico.getId() == 0) {
                    int personaId = insertPersona(connection, medico);
                    medico.setId(personaId);
                    insertMedico(connection, medico);
                } else {
                    updatePersona(connection, medico);
                    updateMedico(connection, medico);
                }
                if (medico.getHorarios() != null) {
                    replaceWorkSchedules(connection, medico);
                }
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudo guardar el medico.", ex);
        }
    }

    public void delete(int medicoId) {
        try (Connection connection = databaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int personaId = findPersonaId(connection, medicoId);
                try (PreparedStatement horarios = connection.prepareStatement("DELETE FROM horarios_laborales WHERE medico_id = ?")) {
                    horarios.setInt(1, medicoId);
                    horarios.executeUpdate();
                }
                try (PreparedStatement medicoStatement = connection.prepareStatement("DELETE FROM medicos WHERE medico_id = ?")) {
                    medicoStatement.setInt(1, medicoId);
                    medicoStatement.executeUpdate();
                }
                try (PreparedStatement personaStatement = connection.prepareStatement("DELETE FROM personas WHERE persona_id = ?")) {
                    personaStatement.setInt(1, personaId);
                    personaStatement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudo eliminar el medico.", ex);
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

    public boolean existsMatriculaExceptMedico(int matricula, int medicoId) {
        String sql = "SELECT COUNT(*) FROM medicos WHERE matricula = ? AND medico_id <> ?";
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, matricula);
            statement.setInt(2, medicoId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudo validar la matricula.", ex);
        }
    }

    public boolean existsUsuarioExceptMedico(String usuario, int medicoId) {
        String sql = "SELECT COUNT(*) FROM medicos WHERE LOWER(usuario) = LOWER(?) AND medico_id <> ?";
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, usuario);
            statement.setInt(2, medicoId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudo validar el usuario del medico.", ex);
        }
    }

    public boolean hasAppointments(int medicoId) {
        String sql = "SELECT COUNT(*) FROM turnos WHERE medico_id = ?";
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, medicoId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudieron validar los turnos del medico.", ex);
        }
    }

    private int findPersonaId(Connection connection, int medicoId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT persona_id FROM medicos WHERE medico_id = ?")) {
            statement.setInt(1, medicoId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Medico inexistente");
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

    private void insertMedico(Connection connection, Medico medico) throws SQLException {
        String sql = """
                INSERT INTO medicos(persona_id, matricula, usuario, contrasenia, especialidad_id)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, medico.getId());
            statement.setInt(2, medico.getMatricula());
            statement.setString(3, medico.getUsuario());
            statement.setString(4, medico.getContrasenia());
            statement.setInt(5, medico.getEspecialidadId());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                medico.setMedicoId(keys.getInt(1));
            }
        }
    }

    private void updateMedico(Connection connection, Medico medico) throws SQLException {
        String sql = """
                UPDATE medicos
                SET matricula = ?, usuario = ?, contrasenia = ?, especialidad_id = ?
                WHERE medico_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, medico.getMatricula());
            statement.setString(2, medico.getUsuario());
            statement.setString(3, medico.getContrasenia());
            statement.setInt(4, medico.getEspecialidadId());
            statement.setInt(5, medico.getMedicoId());
            statement.executeUpdate();
        }
    }

    private List<HorarioMedico> findWorkSchedules(Connection connection, int medicoId) throws SQLException {
        String sql = """
                SELECT horario_laboral_id, medico_id, dia_semana, hora_inicio, hora_fin, activo
                FROM horarios_laborales
                WHERE medico_id = ? AND activo = TRUE
                ORDER BY CASE dia_semana
                    WHEN 'LUNES' THEN 1
                    WHEN 'MARTES' THEN 2
                    WHEN 'MIERCOLES' THEN 3
                    WHEN 'JUEVES' THEN 4
                    WHEN 'VIERNES' THEN 5
                    WHEN 'SABADO' THEN 6
                    WHEN 'DOMINGO' THEN 7
                    ELSE 8
                END, hora_inicio
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, medicoId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<HorarioMedico> schedules = new ArrayList<>();
                while (resultSet.next()) {
                    schedules.add(mapSchedule(resultSet));
                }
                return schedules;
            }
        }
    }

    private void replaceWorkSchedules(Connection connection, Medico medico) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM horarios_laborales WHERE medico_id = ?")) {
            delete.setInt(1, medico.getMedicoId());
            delete.executeUpdate();
        }
        String sql = """
                INSERT INTO horarios_laborales(medico_id, dia_semana, hora_inicio, hora_fin, activo)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (HorarioMedico horario : medico.getHorarios()) {
                statement.setInt(1, medico.getMedicoId());
                statement.setString(2, dayName(horario.getDiaSemana()));
                statement.setTime(3, Time.valueOf(horario.getHoraDesde()));
                statement.setTime(4, Time.valueOf(horario.getHoraHasta()));
                statement.setBoolean(5, horario.isActivo());
                statement.addBatch();
            }
            statement.executeBatch();
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

    private Medico map(ResultSet resultSet) throws SQLException {
        Medico medico = new Medico();
        medico.setId(resultSet.getInt("persona_id"));
        medico.setMedicoId(resultSet.getInt("medico_id"));
        medico.setDni(resultSet.getString("dni"));
        medico.setNombre(resultSet.getString("nombre"));
        medico.setApellido(resultSet.getString("apellido"));
        medico.setGenero(resultSet.getString("genero"));
        Date fechaNacimiento = resultSet.getDate("fecha_nacimiento");
        medico.setFechaNacimiento(fechaNacimiento == null ? null : fechaNacimiento.toLocalDate());
        medico.setDireccion(resultSet.getString("direccion"));
        medico.setTelefono(resultSet.getString("telefono"));
        medico.setEmail(resultSet.getString("mail"));
        medico.setMatricula(resultSet.getInt("matricula"));
        medico.setUsuario(resultSet.getString("usuario"));
        medico.setContrasenia(resultSet.getString("contrasenia"));
        medico.setEspecialidadId(resultSet.getInt("especialidad_id"));
        medico.setEspecialidadNombre(resultSet.getString("nombre_especialidad"));
        return medico;
    }

    private HorarioMedico mapSchedule(ResultSet resultSet) throws SQLException {
        HorarioMedico horario = new HorarioMedico();
        horario.setId(resultSet.getInt("horario_laboral_id"));
        horario.setMedicoId(resultSet.getInt("medico_id"));
        horario.setDiaSemana(dayNumber(resultSet.getString("dia_semana")));
        horario.setHoraDesde(resultSet.getTime("hora_inicio").toLocalTime());
        horario.setHoraHasta(resultSet.getTime("hora_fin").toLocalTime());
        horario.setActivo(resultSet.getBoolean("activo"));
        return horario;
    }

    private String dayName(int day) {
        return switch (day) {
            case 1 -> "LUNES";
            case 2 -> "MARTES";
            case 3 -> "MIERCOLES";
            case 4 -> "JUEVES";
            case 5 -> "VIERNES";
            case 6 -> "SABADO";
            case 7 -> "DOMINGO";
            default -> throw new IllegalArgumentException("Dia laboral invalido: " + day);
        };
    }

    private int dayNumber(String day) {
        return switch (day) {
            case "LUNES" -> 1;
            case "MARTES" -> 2;
            case "MIERCOLES" -> 3;
            case "JUEVES" -> 4;
            case "VIERNES" -> 5;
            case "SABADO" -> 6;
            case "DOMINGO" -> 7;
            default -> throw new IllegalArgumentException("Dia laboral desconocido: " + day);
        };
    }
}
