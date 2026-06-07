package com.sanimat.view;

import com.sanimat.app.MainApp;
import com.sanimat.config.DatabaseConfig;
import com.sanimat.controller.LoginController;
import com.sanimat.dao.UsuarioDao;
import com.sanimat.model.Usuario;
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

/**
   Pantalla de ingreso al sistema.
   Solicita usuario y password y delega la validacion al controlador.
 */
public class LoginView {
    private final MainApp app;
    private final LoginController controller;
    private final BorderPane root = new BorderPane();
    private final TextField usernameField = Ui.textField("Usuario");
    private final PasswordField passwordField = new PasswordField();
    private final Label statusLabel = new Label();

    public LoginView(MainApp app, DatabaseConfig databaseConfig) {
        this.app = app;
        this.controller = new LoginController(new UsuarioDao(databaseConfig));
        build();
    }

    public Parent getRoot() {
        return root;
    }

    private void build() {
        root.getStyleClass().add("login-shell");

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
        usernameField.getStyleClass().add("login-input");
        passwordField.getStyleClass().add("login-input");
        passwordField.setPromptText("Contraseña");
        passwordField.setMaxWidth(Double.MAX_VALUE);
        statusLabel.getStyleClass().add("status-error");
        statusLabel.setWrapText(true);

        Button loginButton = Ui.primaryButton("Acceder");
        loginButton.getStyleClass().add("login-submit");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(event -> login());
        passwordField.setOnAction(event -> login());

        form.getChildren().addAll(
                heading,
                intro,
                Ui.formRow("Usuario", usernameField),
                Ui.formRow("Contraseña", passwordField),
                loginButton,
                statusLabel
        );

        // Contenedor secundario para centrar perfectamente el login-card a la derecha
        VBox formArea = new VBox(form);
        formArea.getStyleClass().add("login-form-area");
        formArea.setAlignment(Pos.CENTER);
        HBox.setHgrow(formArea, Priority.ALWAYS);
        
        // Organiza el lateral izquierdo (marca) y lateral derecho (formulario centrado)
        HBox content = new HBox(brand, formArea);
        root.setCenter(content);
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
}
