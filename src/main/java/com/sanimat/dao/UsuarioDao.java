package com.sanimat.dao;

import com.sanimat.config.DatabaseConfig;
import com.sanimat.model.Rol;
import com.sanimat.model.RoleName;
import com.sanimat.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
   Acceso JDBC para autenticar usuarios y resolver su rol dentro del sistema.
 */
public class UsuarioDao {
    private final DatabaseConfig databaseConfig;

    public UsuarioDao(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public Optional<Usuario> findByUsername(String username) {
        String sql = """
                SELECT s.secretario_id AS usuario_id,
                       s.usuario AS username,
                       s.contrasenia AS password_hash,
                       s.persona_id,
                       s.secretario_id,
                       NULL::INTEGER AS medico_id,
                       'SECRETARIA' AS rol_nombre,
                       CONCAT(p.apellido, ', ', p.nombre) AS nombre_completo
                FROM secretarios s
                JOIN personas p ON p.persona_id = s.persona_id
                WHERE LOWER(s.usuario) = LOWER(?)
                UNION ALL
                SELECT m.medico_id AS usuario_id,
                       m.usuario AS username,
                       m.contrasenia AS password_hash,
                       m.persona_id,
                       NULL::INTEGER AS secretario_id,
                       m.medico_id,
                       'MEDICO' AS rol_nombre,
                       CONCAT(p.apellido, ', ', p.nombre) AS nombre_completo
                FROM medicos m
                JOIN personas p ON p.persona_id = m.persona_id
                WHERE LOWER(m.usuario) = LOWER(?)
                """;
        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                Usuario usuario = new Usuario();
                usuario.setId(resultSet.getInt("usuario_id"));
                usuario.setUsername(resultSet.getString("username"));
                usuario.setPasswordHash(resultSet.getString("password_hash"));
                usuario.setPersonaId((Integer) resultSet.getObject("persona_id"));
                usuario.setMedicoId((Integer) resultSet.getObject("medico_id"));
                usuario.setSecretarioId((Integer) resultSet.getObject("secretario_id"));
                usuario.setNombreCompleto(resultSet.getString("nombre_completo"));

                Rol rol = new Rol();
                rol.setId(0);
                rol.setNombre(RoleName.valueOf(resultSet.getString("rol_nombre")));
                usuario.setRol(rol);
                return Optional.of(usuario);
            }
        } catch (SQLException ex) {
            throw new DaoException("No se pudo buscar el usuario.", ex);
        }
    }
}
