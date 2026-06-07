package com.sanimat.dao;

import com.sanimat.config.DatabaseConfig;
import com.sanimat.model.EstadoTurno;
import com.sanimat.model.HorarioMedico;
import com.sanimat.model.Turno;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
   Acceso JDBC para turnos.
   Incluye consultas de agenda, disponibilidad horaria y cambio de estado.
 */
public class TurnoDao {
    private final DatabaseConfig databaseConfig;

    public TurnoDao(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public List<Turno> search(String filtro, LocalDate fecha) {
        return searchInternal(filtro, fecha, null, null, false);
    }

    public List<Turno> searchForMedico(int medicoId, String filtro, LocalDate fecha) {
        return searchInternal(filtro, fecha, medicoId, null, false);
    }

    public List<Turno> searchForPaciente(int pacienteId, String filtro, LocalDate fecha) {
        return searchInternal(filtro, fecha, null, pacienteId, false);
    }

    public List<Turno> findUnpaidAppointments() {
        return searchInternal(null, null, null, null, true);
    }

    public Optional<Turno> findById(int id) {
        String sql = baseSelect() + " WHERE t.turno_id = ?";
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudo buscar el turno.", ex);
        }
    }

    public void save(Turno turno) {
        if (turno.getId() == 0) {
            insert(turno);
        } else {
            update(turno);
        }
    }

    public void cancel(int id) {
        updateStatus(id, EstadoTurno.CANCELADO);
    }

    public void updateStatus(int id, EstadoTurno estado) {
        String sql = """
                UPDATE turnos
                SET estado_turno_id = (SELECT estado_turno_id FROM enum_estado_turno WHERE nombre = ?)
                WHERE turno_id = ?
                """;
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, estado.name());
            statement.setInt(2, id);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DaoException("No se pudo actualizar el estado del turno.", ex);
        }
    }

    public boolean hasMedicoOverlap(int medicoId, LocalDateTime fechaHora, int excludeTurnoId) {
        String sql = """
                SELECT COUNT(*)
                FROM turnos t
                JOIN enum_estado_turno et ON et.estado_turno_id = t.estado_turno_id
                WHERE t.medico_id = ?
                  AND t.fecha = ?
                  AND t.hora = ?
                  AND t.turno_id <> ?
                  AND et.nombre = 'CONFIRMADO'
                """;
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, medicoId);
            statement.setDate(2, java.sql.Date.valueOf(fechaHora.toLocalDate()));
            statement.setTime(3, Time.valueOf(fechaHora.toLocalTime()));
            statement.setInt(4, excludeTurnoId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudo validar la disponibilidad del medico.", ex);
        }
    }

    public List<HorarioMedico> findWorkSchedules(int medicoId) {
        String sql = """
                SELECT horario_laboral_id, medico_id, dia_semana, hora_inicio, hora_fin, activo
                FROM horarios_laborales
                WHERE medico_id = ? AND activo = TRUE
                ORDER BY dia_semana, hora_inicio
                """;
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, medicoId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<HorarioMedico> schedules = new ArrayList<>();
                while (resultSet.next()) {
                    schedules.add(mapSchedule(resultSet));
                }
                return schedules;
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudieron listar los horarios laborales del medico.", ex);
        }
    }

    public List<LocalTime> findBookedTimes(int medicoId, LocalDate date, int excludeTurnoId) {
        String sql = """
                SELECT t.hora
                FROM turnos t
                JOIN enum_estado_turno et ON et.estado_turno_id = t.estado_turno_id
                WHERE t.medico_id = ?
                  AND t.fecha = ?
                  AND t.turno_id <> ?
                  AND et.nombre = 'CONFIRMADO'
                ORDER BY t.hora
                """;
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, medicoId);
            statement.setDate(2, java.sql.Date.valueOf(date));
            statement.setInt(3, excludeTurnoId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<LocalTime> bookedTimes = new ArrayList<>();
                while (resultSet.next()) {
                    bookedTimes.add(resultSet.getTime("hora").toLocalTime());
                }
                return bookedTimes;
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudieron listar los horarios ocupados del medico.", ex);
        }
    }

    public boolean isInsideWorkSchedule(int medicoId, LocalDateTime dateTime) {
        String sql = """
                SELECT COUNT(*)
                FROM horarios_laborales
                WHERE medico_id = ?
                  AND activo = TRUE
                  AND dia_semana = ?
                  AND ? >= hora_inicio
                  AND ? < hora_fin
                """;
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            Time time = Time.valueOf(dateTime.toLocalTime());
            statement.setInt(1, medicoId);
            statement.setString(2, dayName(dateTime.toLocalDate().getDayOfWeek()));
            statement.setTime(3, time);
            statement.setTime(4, time);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudo validar el horario laboral del medico.", ex);
        }
    }

    private List<Turno> searchInternal(String filtro, LocalDate fecha, Integer medicoId, Integer pacienteId, boolean onlyUnpaid) {
        StringBuilder sql = new StringBuilder(baseSelect());
        List<Object> params = new ArrayList<>();
        sql.append(" WHERE 1 = 1");

        if (filtro != null && !filtro.isBlank()) {
            sql.append("""
                     AND (
                        LOWER(pp.apellido || ' ' || pp.nombre) LIKE LOWER(?)
                        OR LOWER(pm.apellido || ' ' || pm.nombre) LIKE LOWER(?)
                        OR LOWER(e.nombre_especialidad) LIKE LOWER(?)
                        OR CAST(t.turno_id AS TEXT) LIKE ?
                     )
                    """);
            String normalized = "%" + filtro.trim() + "%";
            params.add(normalized);
            params.add(normalized);
            params.add(normalized);
            params.add(normalized);
        }
        if (fecha != null) {
            sql.append(" AND t.fecha = ?");
            params.add(fecha);
        }
        if (medicoId != null) {
            sql.append(" AND t.medico_id = ?");
            params.add(medicoId);
        }
        if (pacienteId != null) {
            sql.append(" AND t.paciente_id = ?");
            params.add(pacienteId);
        }
        if (onlyUnpaid) {
            sql.append("""
                     AND et.nombre <> 'CANCELADO'
                     AND NOT EXISTS (
                        SELECT 1
                        FROM pagos pg
                        JOIN enum_medio_pago mp ON mp.medio_pago_id = pg.medio_pago_id
                        WHERE pg.turno_id = t.turno_id
                          AND pg.monto = (
                            (CASE WHEN tc.nombre = 'OBRA_SOCIAL' THEN 12500.00 ELSE 25000.00 END)
                            * (CASE WHEN mp.nombre = 'EFECTIVO' THEN 0.85 ELSE 1.00 END)
                          )
                     )
                    """);
        }
        sql.append(" ORDER BY t.fecha DESC, t.hora DESC");

        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParams(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Turno> turnos = new ArrayList<>();
                while (resultSet.next()) {
                    turnos.add(map(resultSet));
                }
                return turnos;
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudieron listar los turnos.", ex);
        }
    }

    private void insert(Turno turno) {
        String sql = """
                INSERT INTO turnos(paciente_id, medico_id, secretario_id, fecha, hora, estado_turno_id)
                VALUES (?, ?, ?, ?, ?, (SELECT estado_turno_id FROM enum_estado_turno WHERE nombre = ?))
                """;
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindTurno(statement, turno);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    turno.setId(keys.getInt(1));
                }
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudo crear el turno.", ex);
        }
    }

    private void update(Turno turno) {
        String sql = """
                UPDATE turnos
                SET paciente_id = ?, medico_id = ?, secretario_id = ?, fecha = ?, hora = ?,
                    estado_turno_id = (SELECT estado_turno_id FROM enum_estado_turno WHERE nombre = ?)
                WHERE turno_id = ?
                """;
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindTurno(statement, turno);
            statement.setInt(7, turno.getId());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new DaoException("No se pudo actualizar el turno.", ex);
        }
    }

    private void bindTurno(PreparedStatement statement, Turno turno) throws SQLException {
        LocalDate date = turno.getFechaHora().toLocalDate();
        LocalTime time = turno.getFechaHora().toLocalTime();
        statement.setInt(1, turno.getPacienteId());
        statement.setInt(2, turno.getMedicoId());
        if (turno.getSecretarioId() == null) {
            statement.setNull(3, java.sql.Types.INTEGER);
        } else {
            statement.setInt(3, turno.getSecretarioId());
        }
        statement.setDate(4, java.sql.Date.valueOf(date));
        statement.setTime(5, Time.valueOf(time));
        statement.setString(6, turno.getEstado().name());
    }

    private void bindParams(PreparedStatement statement, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object param = params.get(i);
            int index = i + 1;
            if (param instanceof LocalDate localDate) {
                statement.setDate(index, java.sql.Date.valueOf(localDate));
            } else {
                statement.setObject(index, param);
            }
        }
    }

    private String baseSelect() {
        return """
                SELECT t.turno_id, t.paciente_id, t.medico_id, t.secretario_id, t.fecha, t.hora,
                       et.nombre AS estado_nombre,
                       CONCAT(pp.apellido, ', ', pp.nombre) AS paciente_nombre,
                       pp.dni AS paciente_dni,
                       tc.nombre AS tipo_cobertura,
                       pa.numero_afiliado,
                       CONCAT(pm.apellido, ', ', pm.nombre) AS medico_nombre,
                       e.especialidad_id, e.nombre_especialidad,
                       EXISTS (
                            SELECT 1
                            FROM pagos pg
                            JOIN enum_medio_pago mp ON mp.medio_pago_id = pg.medio_pago_id
                            WHERE pg.turno_id = t.turno_id
                              AND pg.monto = (
                                (CASE WHEN tc.nombre = 'OBRA_SOCIAL' THEN 12500.00 ELSE 25000.00 END)
                                * (CASE WHEN mp.nombre = 'EFECTIVO' THEN 0.85 ELSE 1.00 END)
                              )
                       ) AS pago_registrado
                FROM turnos t
                JOIN enum_estado_turno et ON et.estado_turno_id = t.estado_turno_id
                JOIN pacientes pa ON pa.paciente_id = t.paciente_id
                JOIN personas pp ON pp.persona_id = pa.persona_id
                JOIN enum_tipo_cobertura tc ON tc.tipo_cobertura_id = pa.tipo_cobertura_id
                JOIN medicos m ON m.medico_id = t.medico_id
                JOIN personas pm ON pm.persona_id = m.persona_id
                JOIN especialidades e ON e.especialidad_id = m.especialidad_id
                """;
    }

    private Turno map(ResultSet resultSet) throws SQLException {
        Turno turno = new Turno();
        turno.setId(resultSet.getInt("turno_id"));
        turno.setPacienteId(resultSet.getInt("paciente_id"));
        turno.setMedicoId(resultSet.getInt("medico_id"));
        turno.setSecretarioId((Integer) resultSet.getObject("secretario_id"));
        LocalDate fecha = resultSet.getDate("fecha").toLocalDate();
        LocalTime hora = resultSet.getTime("hora").toLocalTime();
        turno.setFechaHora(LocalDateTime.of(fecha, hora));
        turno.setEstado(EstadoTurno.valueOf(resultSet.getString("estado_nombre")));
        turno.setPacienteNombre(resultSet.getString("paciente_nombre"));
        turno.setPacienteDni(resultSet.getString("paciente_dni"));
        turno.setTipoCobertura(resultSet.getString("tipo_cobertura"));
        turno.setNumeroAfiliado(resultSet.getString("numero_afiliado"));
        turno.setMedicoNombre(resultSet.getString("medico_nombre"));
        turno.setEspecialidadId(resultSet.getInt("especialidad_id"));
        turno.setEspecialidadNombre(resultSet.getString("nombre_especialidad"));
        turno.setPagoRegistrado(resultSet.getBoolean("pago_registrado"));
        return turno;
    }

    private HorarioMedico mapSchedule(ResultSet resultSet) throws SQLException {
        HorarioMedico schedule = new HorarioMedico();
        schedule.setId(resultSet.getInt("horario_laboral_id"));
        schedule.setMedicoId(resultSet.getInt("medico_id"));
        schedule.setDiaSemana(dayOfWeek(resultSet.getString("dia_semana")).getValue());
        schedule.setHoraDesde(resultSet.getTime("hora_inicio").toLocalTime());
        schedule.setHoraHasta(resultSet.getTime("hora_fin").toLocalTime());
        schedule.setActivo(resultSet.getBoolean("activo"));
        return schedule;
    }

    private DayOfWeek dayOfWeek(String value) {
        return switch (value) {
            case "LUNES" -> DayOfWeek.MONDAY;
            case "MARTES" -> DayOfWeek.TUESDAY;
            case "MIERCOLES" -> DayOfWeek.WEDNESDAY;
            case "JUEVES" -> DayOfWeek.THURSDAY;
            case "VIERNES" -> DayOfWeek.FRIDAY;
            case "SABADO" -> DayOfWeek.SATURDAY;
            case "DOMINGO" -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("Dia laboral desconocido: " + value);
        };
    }

    private String dayName(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "LUNES";
            case TUESDAY -> "MARTES";
            case WEDNESDAY -> "MIERCOLES";
            case THURSDAY -> "JUEVES";
            case FRIDAY -> "VIERNES";
            case SATURDAY -> "SABADO";
            case SUNDAY -> "DOMINGO";
        };
    }
}
