package com.sanimat.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private static final String PROPERTY_URL = "db.url";
    private static final String PROPERTY_USER = "db.user";
    private static final String PROPERTY_PASSWORD = "db.password";
    private static final Path EXTERNAL_PROPERTIES_PATH = Paths.get("database.properties");

    private final Properties properties = new Properties();

    public DatabaseConfig() {
        try (InputStream input = getClass().getResourceAsStream("/database.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer database.properties", ex);
        }
        loadExternalProperties();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getUrl(), getUser(), getPassword());
    }

    public void testConnection() {
        testConnection(getUrl(), getUser(), getPassword());
    }

    public void testConnection(String url, String user, String password) {
        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement statement = connection.prepareStatement("SELECT 1");
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudo conectar a PostgreSQL", ex);
        }
    }

    public void saveConnection(String url, String user, String password) {
        properties.setProperty(PROPERTY_URL, url);
        properties.setProperty(PROPERTY_USER, user);
        properties.setProperty(PROPERTY_PASSWORD, password);
        try (OutputStream output = Files.newOutputStream(EXTERNAL_PROPERTIES_PATH)) {
            properties.store(output, "Configuracion de conexion SaniMat");
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar la configuracion de conexion.", ex);
        }
    }

    public String getUrl() {
        return value("SANIMAT_DB_URL", PROPERTY_URL, DEFAULT_URL);
    }

    public String getUser() {
        return value("SANIMAT_DB_USER", PROPERTY_USER, DEFAULT_USER);
    }

    public String getPassword() {
        return value("SANIMAT_DB_PASSWORD", PROPERTY_PASSWORD, DEFAULT_PASSWORD);
    }

    public Path getExternalPropertiesPath() {
        return EXTERNAL_PROPERTIES_PATH;
    }

    private String value(String envName, String propertyName, String defaultValue) {
        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return properties.getProperty(propertyName, defaultValue);
    }

    private void loadExternalProperties() {
        if (!Files.exists(EXTERNAL_PROPERTIES_PATH)) {
            return;
        }
        try (InputStream input = Files.newInputStream(EXTERNAL_PROPERTIES_PATH)) {
            properties.load(input);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer el archivo database.properties externo", ex);
        }
    }
}
