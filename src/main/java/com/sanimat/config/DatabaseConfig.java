package com.sanimat.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
    Centraliza la configuracion de PostgreSQL.
    Primero intenta leer variables de entorno y luego database.properties.
 */
public class DatabaseConfig {
    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/clinica_medica";
    private static final String DEFAULT_USER = "postgres";
    private static final String DEFAULT_PASSWORD = "postgres";

    private final Properties properties = new Properties();

    public DatabaseConfig() {
        try (InputStream input = getClass().getResourceAsStream("/database.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer database.properties", ex);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getUrl(), getUser(), getPassword());
    }

    public void testConnection() {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo conectar a PostgreSQL", ex);
        }
    }

    public String getUrl() {
        return value("SANIMAT_DB_URL", "db.url", DEFAULT_URL);
    }

    public String getUser() {
        return value("SANIMAT_DB_USER", "db.user", DEFAULT_USER);
    }

    public String getPassword() {
        return value("SANIMAT_DB_PASSWORD", "db.password", DEFAULT_PASSWORD);
    }

    private String value(String envName, String propertyName, String defaultValue) {
        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return properties.getProperty(propertyName, defaultValue);
    }
}
