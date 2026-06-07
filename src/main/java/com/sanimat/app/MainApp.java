package com.sanimat.app;

import com.sanimat.config.DatabaseConfig;
import com.sanimat.config.Session;
import com.sanimat.model.Usuario;
import com.sanimat.view.LoginView;
import com.sanimat.view.MainShell;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
   Punto de entrada de la aplicación. Muestra primero el login y,
   cuando el usuario ingresa correctamente, abre la pantalla principal
 */
public class MainApp extends Application {
    private final DatabaseConfig databaseConfig = new DatabaseConfig();
    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("Sistema Médico");
        showLogin();
        stage.show();
    }

    public void showLogin() {
        Session.clear();
        Scene scene = new Scene(new LoginView(this, databaseConfig).getRoot(), 1120, 700);
        applyStyles(scene);
        stage.setScene(scene);
    }

    public void showMain(Usuario usuario) {
        Session.setCurrentUser(usuario);
        Scene scene = new Scene(new MainShell(this, databaseConfig, usuario), 1280, 760);
        applyStyles(scene);
        stage.setScene(scene);
        stage.centerOnScreen();
    }

    private void applyStyles(Scene scene) {
        String css = getClass().getResource("/styles/sanimat.css").toExternalForm();
        scene.getStylesheets().add(css);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
