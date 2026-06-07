package com.sanimat.view;

import com.sanimat.app.MainApp;
import com.sanimat.config.DatabaseConfig;
import com.sanimat.controller.LoginController;
import com.sanimat.dao.UsuarioDao;
import com.sanimat.model.Usuario;
import com.sanimat.util.Dialogs;
import com.sanimat.util.ValidationException;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URI;
import java.net.URISyntaxException;

/**
   Pantalla de ingreso al sistema.
   Solicita usuario y password y delega la validacion al controlador.
 */
public class LoginView {
    private final MainApp app;
    private final DatabaseConfig databaseConfig;
    private final LoginController controller;
    private final BorderPane root = new BorderPane();
    private final VBox formArea = new VBox();

    private final TextField usernameField = Ui.textField("Usuario");
    private final PasswordField passwordField = new PasswordField();
    private final Label statusLabel = new Label();

    private final TextField connectionHostField = Ui.textField("localhost");
    private final TextField connectionPortField = Ui.textField("5432");
    private final TextField connectionDatabaseField = Ui.textField("clinica_medica");
    private final TextField connectionUserField = Ui.textField("postgres");
    private final PasswordField connectionPasswordField = new PasswordField();
    private final Label connectionStatusLabel = new Label();
    private boolean connectionInputsConfigured;

    public LoginView(MainApp app, DatabaseConfig databaseConfig) {
        this.app = app;
        this.databaseConfig = databaseConfig;
        this.controller = new LoginController(new UsuarioDao(databaseConfig));
        build();
    }

    public Parent getRoot() {
        return root;
    }

    private void build() {
        root.getStyleClass().add("login-shell");

        formArea.getStyleClass().add("login-form-area");
        formArea.setAlignment(Pos.CENTER);
        HBox.setHgrow(formArea, Priority.ALWAYS);
        showLoginForm();

        HBox content = new HBox(buildBrandPanel(), formArea);
        root.setCenter(content);
    }

    private VBox buildBrandPanel() {
        // Panel de marca (lateral izquierdo): presentación visual del software
        VBox brand = new VBox(18);
        brand.getStyleClass().add("brand-panel");
        brand.setAlignment(Pos.CENTER_LEFT);

        Label kicker = new Label("Sistema Médico");
        kicker.getStyleClass().add("brand-kicker");
        Label title = new Label("SaniMat");
        title.getStyleClass().add("brand-title");
        Label subtitle = new Label("Gestión clínica para escritorio");
        subtitle.getStyleClass().add("brand-subtitle");
        Label detail = new Label("JavaFX + PostgreSQL + JDBC");
        detail.getStyleClass().add("brand-subtitle");

        brand.getChildren().addAll(kicker, title, subtitle, detail);
        brand.setMinWidth(430);
        brand.setPrefWidth(480);
        brand.setMaxWidth(540);
        return brand;
    }

    private void showLoginForm() {
        // Panel de formulario (lateral derecho): contenedor de los campos de entrada
        VBox form = new VBox(18);
        form.getStyleClass().add("login-card");
        form.setMinWidth(420);
        form.setPrefWidth(440);
        form.setMaxWidth(460);
        form.setMaxHeight(Region.USE_PREF_SIZE);
        form.setAlignment(Pos.CENTER_LEFT);

        Label heading = new Label("Ingresar al sistema");
        heading.getStyleClass().add("login-heading");
        Label intro = new Label("Acceso para secretaría y médicos registrados.");
        intro.getStyleClass().add("login-copy");
        intro.setWrapText(true);

        addLoginInputStyle(usernameField);
        addLoginInputStyle(passwordField);
        passwordField.setPromptText("Contraseña");
        passwordField.setMaxWidth(Double.MAX_VALUE);
        passwordField.setOnAction(event -> login());

        statusLabel.getStyleClass().setAll("status-error");
        statusLabel.setWrapText(true);

        Button loginButton = Ui.primaryButton("Acceder");
        loginButton.getStyleClass().add("login-submit");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(event -> login());

        Button connectionButton = Ui.secondaryButton("Conexión");
        connectionButton.setMaxWidth(Double.MAX_VALUE);
        connectionButton.setOnAction(event -> showConnectionForm());

        form.getChildren().setAll(
                heading,
                intro,
                Ui.formRow("Usuario", usernameField),
                Ui.formRow("Contraseña", passwordField),
                loginButton,
                connectionButton,
                statusLabel
        );
        formArea.getChildren().setAll(form);
    }

    private void showConnectionForm() {
        loadConnectionFields();

        VBox form = new VBox(16);
        form.getStyleClass().add("login-card");
        form.setMinWidth(430);
        form.setPrefWidth(460);
        form.setMaxWidth(500);
        form.setMaxHeight(Region.USE_PREF_SIZE);
        form.setAlignment(Pos.CENTER_LEFT);

        Label heading = new Label("Conexión a PostgreSQL");
        heading.getStyleClass().add("login-heading");
        Label intro = new Label("Configure los datos que usa la aplicación para conectarse a la base.");
        intro.getStyleClass().add("login-copy");
        intro.setWrapText(true);

        configureConnectionInputs();
        connectionStatusLabel.getStyleClass().setAll("status-error");
        connectionStatusLabel.setWrapText(true);

        Button testButton = Ui.secondaryButton("Probar conexión");
        testButton.setMaxWidth(Double.MAX_VALUE);
        testButton.setOnAction(event -> testConnection());

        Button saveButton = Ui.primaryButton("Guardar");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setOnAction(event -> saveConnection());

        Button backButton = Ui.secondaryButton("Volver al login");
        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setOnAction(event -> showLoginForm());

        form.getChildren().setAll(
                heading,
                intro,
                Ui.formRow("Host", connectionHostField),
                Ui.formRow("Puerto", connectionPortField),
                Ui.formRow("Base de datos", connectionDatabaseField),
                Ui.formRow("Usuario", connectionUserField),
                Ui.formRow("Contraseña", connectionPasswordField),
                testButton,
                saveButton,
                backButton,
                connectionStatusLabel
        );
        formArea.getChildren().setAll(form);
    }

    private void configureConnectionInputs() {
        if (connectionInputsConfigured) {
            return;
        }
        for (TextField field : new TextField[]{
                connectionHostField, connectionPortField, connectionDatabaseField, connectionUserField
        }) {
            addLoginInputStyle(field);
        }
        addLoginInputStyle(connectionPasswordField);
        connectionPasswordField.setPromptText("Contraseña");
        connectionPasswordField.setMaxWidth(Double.MAX_VALUE);
        Ui.digitsOnly(connectionPortField, 5);
        connectionInputsConfigured = true;
    }

    private void login() {
        try {
            Usuario usuario = controller.login(usernameField.getText(), passwordField.getText());
            statusLabel.setText("");
            app.showMain(usuario);
        } catch (ValidationException | IllegalStateException ex) {
            statusLabel.setText(ex.getMessage());
        } catch (RuntimeException ex) {
            statusLabel.setText("No se pudo iniciar sesión. Revise la conexión a PostgreSQL.");
        }
    }

    private void loadConnectionFields() {
        connectionStatusLabel.setText("");
        connectionUserField.setText(databaseConfig.getUser());
        connectionPasswordField.setText(databaseConfig.getPassword());

        try {
            URI uri = new URI(databaseConfig.getUrl().replaceFirst("^jdbc:", ""));
            connectionHostField.setText(uri.getHost() == null ? "localhost" : uri.getHost());
            connectionPortField.setText(uri.getPort() == -1 ? "5432" : String.valueOf(uri.getPort()));
            String path = uri.getPath();
            connectionDatabaseField.setText(path == null || path.length() <= 1 ? "clinica_medica" : path.substring(1));
        } catch (URISyntaxException | IllegalArgumentException ex) {
            connectionHostField.setText("localhost");
            connectionPortField.setText("5432");
            connectionDatabaseField.setText("clinica_medica");
        }
    }

    private void testConnection() {
        try {
            databaseConfig.testConnection(buildConnectionUrl(), connectionUserField.getText().trim(), connectionPasswordField.getText());
            connectionStatusLabel.getStyleClass().setAll("status-ok");
            connectionStatusLabel.setText("Conexión correcta.");
            Dialogs.info("Conexión", "La conexión a PostgreSQL fue exitosa.");
        } catch (RuntimeException ex) {
            connectionStatusLabel.getStyleClass().setAll("status-error");
            connectionStatusLabel.setText(ex.getMessage());
            Dialogs.error("Conexión", ex.getMessage());
        }
    }

    private void saveConnection() {
        try {
            String url = buildConnectionUrl();
            if (!Dialogs.confirm("Conexión", "¿Desea reemplazar la configuración actual de PostgreSQL?")) {
                return;
            }
            databaseConfig.saveConnection(url, connectionUserField.getText().trim(), connectionPasswordField.getText());
            connectionStatusLabel.getStyleClass().setAll("status-ok");
            connectionStatusLabel.setText("Configuración guardada en " + databaseConfig.getExternalPropertiesPath() + ".");
        } catch (RuntimeException ex) {
            connectionStatusLabel.getStyleClass().setAll("status-error");
            connectionStatusLabel.setText(ex.getMessage());
        }
    }

    private String buildConnectionUrl() {
        String host = requiredConnectionValue(connectionHostField, "host");
        String port = requiredConnectionValue(connectionPortField, "puerto");
        String database = requiredConnectionValue(connectionDatabaseField, "base de datos");
        requiredConnectionValue(connectionUserField, "usuario");
        int portNumber;
        try {
            portNumber = Integer.parseInt(port);
        } catch (NumberFormatException ex) {
            throw new ValidationException("El puerto debe ser numérico.");
        }
        if (portNumber < 1 || portNumber > 65535) {
            throw new ValidationException("El puerto debe estar entre 1 y 65535.");
        }
        if (connectionPasswordField.getText() == null || connectionPasswordField.getText().isBlank()) {
            throw new ValidationException("Complete la contraseña de PostgreSQL.");
        }
        return "jdbc:postgresql://" + host + ":" + portNumber + "/" + database;
    }

    private String requiredConnectionValue(TextField field, String label) {
        String value = field.getText();
        if (value == null || value.isBlank()) {
            throw new ValidationException("Complete el campo " + label + ".");
        }
        return value.trim();
    }

    private void addLoginInputStyle(TextField field) {
        if (!field.getStyleClass().contains("login-input")) {
            field.getStyleClass().add("login-input");
        }
    }
}
