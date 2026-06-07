package com.sanimat.dao;

import com.sanimat.config.DatabaseConfig;
import com.sanimat.model.MedioPago;
import com.sanimat.model.Pago;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso JDBC para pagos registrados sobre turnos.
 */
public class PagoDao {
    private final DatabaseConfig databaseConfig;

    public PagoDao(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public List<Pago> findAll() {
        String sql = """
                SELECT pg.pago_id, pg.turno_id, pg.monto, pg.fecha_pago,
                       mp.nombre AS medio_pago,
                       CONCAT(p.apellido, ', ', p.nombre) AS paciente_nombre
                FROM pagos pg
                JOIN enum_medio_pago mp ON mp.medio_pago_id = pg.medio_pago_id
                JOIN turnos t ON t.turno_id = pg.turno_id
                JOIN pacientes pa ON pa.paciente_id = t.paciente_id
                JOIN personas p ON p.persona_id = pa.persona_id
                ORDER BY pg.fecha_pago DESC, pg.pago_id DESC
                """;
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Pago> pagos = new ArrayList<>();
            while (resultSet.next()) {
                pagos.add(map(resultSet));
            }
            return pagos;
        } catch (SQLException ex) {
            throw new DaoException("No se pudieron listar los pagos.", ex);
        }
    }

    public void save(Pago pago) {
        String sql = """
                INSERT INTO pagos(turno_id, monto, fecha_pago, medio_pago_id)
                VALUES (?, ?, ?, (SELECT medio_pago_id FROM enum_medio_pago WHERE nombre = ?))
                ON CONFLICT (turno_id) DO UPDATE
                SET monto = EXCLUDED.monto,
                    fecha_pago = EXCLUDED.fecha_pago,
                    medio_pago_id = EXCLUDED.medio_pago_id
                RETURNING pago_id
                """;
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, pago.getTurnoId());
            statement.setBigDecimal(2, pago.getMonto());
            statement.setDate(3, java.sql.Date.valueOf(pago.getFechaPago().toLocalDate()));
            statement.setString(4, pago.getMedioPago().name());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    pago.setId(resultSet.getInt("pago_id"));
                }
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudo registrar el pago.", ex);
        }
    }

    private Pago map(ResultSet resultSet) throws SQLException {
        Pago pago = new Pago();
        pago.setId(resultSet.getInt("pago_id"));
        pago.setTurnoId(resultSet.getInt("turno_id"));
        pago.setMonto(resultSet.getBigDecimal("monto"));
        pago.setMedioPago(MedioPago.valueOf(resultSet.getString("medio_pago")));
        LocalDate fechaPago = resultSet.getDate("fecha_pago").toLocalDate();
        pago.setFechaPago(fechaPago.atStartOfDay());
        pago.setPacienteNombre(resultSet.getString("paciente_nombre"));
        return pago;
    }
}
