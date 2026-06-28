package com.sanimat.app;

import com.sanimat.config.DatabaseConfig;
import com.sanimat.config.Session;
import com.sanimat.model.Usuario;
import com.sanimat.view.LoginView;
import com.sanimat.view.MainShell;
import javafx.application.Application;
import javafx.scene.Parent;
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
        showRoot(new LoginView(this, databaseConfig).getRoot(), 1120, 700, false);
    }

    public void showMain(Usuario usuario) {
        Session.setCurrentUser(usuario);
        showRoot(new MainShell(this, databaseConfig, usuario), 1280, 760, true);
    }

    private void showRoot(Parent root, double width, double height, boolean centerWhenWindowed) {
        boolean wasMaximized = stage.isMaximized();
        boolean wasFullScreen = stage.isFullScreen();
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root, width, height);
            applyStyles(scene);
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }
        if (wasMaximized) {
            stage.setMaximized(true);
        }
        if (wasFullScreen) {
            stage.setFullScreen(true);
        }
        if (!wasMaximized && !wasFullScreen) {
            stage.setWidth(width);
            stage.setHeight(height);
            if (centerWhenWindowed) {
                stage.centerOnScreen();
            }
        }
    }

    private void applyStyles(Scene scene) {
        String css = getClass().getResource("/styles/sanimat.css").toExternalForm();
        scene.getStylesheets().add(css);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
