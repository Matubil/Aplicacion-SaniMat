package com.sanimat.view;

import com.sanimat.app.MainApp;
import com.sanimat.config.DatabaseConfig;
import com.sanimat.dao.EspecialidadDao;
import com.sanimat.dao.HistoriaClinicaDao;
import com.sanimat.dao.MedicoDao;
import com.sanimat.dao.PacienteDao;
import com.sanimat.dao.PagoDao;
import com.sanimat.dao.TurnoDao;
import com.sanimat.model.RoleName;
import com.sanimat.model.Usuario;
import com.sanimat.service.EspecialidadService;
import com.sanimat.service.HistoriaClinicaService;
import com.sanimat.service.MedicoService;
import com.sanimat.service.PacienteService;
import com.sanimat.service.PagoService;
import com.sanimat.service.TurnoService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

/**
   Contenedor principal con barra lateral, barra superior y contenido central.
   Decide que vistas mostrar segun el rol del usuario logueado.
 */
public class MainShell extends BorderPane {
    private final MainApp app;
    private final Usuario usuario;
    private final PacienteService pacienteService;
    private final MedicoService medicoService;
    private final EspecialidadService especialidadService;
    private final TurnoService turnoService;
    private final PagoService pagoService;
    private final HistoriaClinicaService historiaClinicaService;
    private final Map<String, Button> navButtons = new HashMap<>();
    private Label userLabel;

    public MainShell(MainApp app, DatabaseConfig databaseConfig, Usuario usuario) {
        this.app = app;
        this.usuario = usuario;
        PacienteDao pacienteDao = new PacienteDao(databaseConfig);
        MedicoDao medicoDao = new MedicoDao(databaseConfig);
        EspecialidadDao especialidadDao = new EspecialidadDao(databaseConfig);
        TurnoDao turnoDao = new TurnoDao(databaseConfig);
        this.pacienteService = new PacienteService(pacienteDao);
        this.medicoService = new MedicoService(medicoDao);
        this.especialidadService = new EspecialidadService(especialidadDao);
        this.turnoService = new TurnoService(turnoDao);
        this.pagoService = new PagoService(new PagoDao(databaseConfig), turnoDao);
        this.historiaClinicaService = new HistoriaClinicaService(new HistoriaClinicaDao(databaseConfig));
        build();
    }

    private void build() {
        setLeft(sidebar());
        setTop(topbar());
        show("HOME");
    }

    // Construye la barra de navegación lateral con lógica de control de acceso según el rol del usuario
    private Node sidebar() {
        VBox sidebar = new VBox(10);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(230);
        sidebar.setMinWidth(190);

        Label title = new Label("SaniMat Clinic");
        title.getStyleClass().add("sidebar-title");
        Label role = new Label(" Terminal de " + usuario.getRol().getNombre().getDisplayName() );
        role.getStyleClass().add("sidebar-role");
        sidebar.getChildren().addAll(title, role, spacer(12));

        sidebar.getChildren().add(nav("Home", "HOME"));
        
        // Cada rol ve solo las opciones que puede utilizar dentro del prototipo.
        if (usuario.hasRole(RoleName.SECRETARIA)) {
            sidebar.getChildren().add(nav("Turnos", "TURNOS"));
            sidebar.getChildren().add(nav("Pacientes", "PACIENTES"));
            sidebar.getChildren().add(nav("Pagos", "PAGOS"));
            sidebar.getChildren().add(nav("Medicos", "MEDICOS"));
            sidebar.getChildren().add(nav("Especialidades", "ESPECIALIDADES"));
        }
        if (usuario.hasRole(RoleName.MEDICO)) {
            sidebar.getChildren().add(nav("Turnos", "TURNOS"));
            sidebar.getChildren().add(nav("Historial Clínico", "HISTORIAS"));
            Button recipeButton = nav("Generar Receta", "RECETA");
            recipeButton.getStyleClass().add("future-nav-button");
            sidebar.getChildren().add(recipeButton);
            sidebar.getChildren().add(nav("Editar Perfil", "PERFIL"));
        }

        // Region elástica para empujar el botón de logout al fondo de la barra
        Region fill = new Region();
        VBox.setVgrow(fill, Priority.ALWAYS);
        Button logout = navButton("Cerrar sesión");
        logout.setOnAction(event -> app.showLogin());
        sidebar.getChildren().addAll(fill, logout);
        return sidebar;
    }

    private Node topbar() {
        HBox bar = new HBox(12);
        bar.getStyleClass().add("topbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        userLabel = new Label(displayName());
        userLabel.getStyleClass().add("section-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label db = new Label("PostgreSQL / JDBC");
        db.getStyleClass().add("field-label");
        bar.getChildren().addAll(userLabel, spacer, db);
        return bar;
    }

    private Button nav(String text, String key) {
        Button button = navButton(text);
        navButtons.put(key, button);
        button.setOnAction(event -> show(key));
        return button;
    }

    // Enrutador de la aplicación: carga e inyecta dinámicamente la vista seleccionada en la zona central
    private void show(String key) {
        activate(key);
        Node content = switch (key) {
            case "PACIENTES" -> new PatientManagementView(pacienteService).getRoot();
            case "MEDICOS" -> new DoctorManagementView(medicoService, especialidadService).getRoot();
            case "ESPECIALIDADES" -> new SpecialtyManagementView(especialidadService).getRoot();
            case "TURNOS" -> new AppointmentManagementView(turnoService, pacienteService, medicoService, usuario).getRoot();
            case "PAGOS" -> new PaymentView(pagoService).getRoot();
            case "HISTORIAS" -> new ClinicalHistoryView(historiaClinicaService, pacienteService, usuario).getRoot();
            case "RECETA" -> new PlaceholderView(
                    "Generar receta",
                    "La generación de recetas no forma parte del alcance implementado en esta versión del prototipo. Queda contemplada como una posible ampliacion futura del sistema."
            ).getRoot();
            case "PERFIL" -> new DoctorProfileView(medicoService, usuario, this::refreshUserLabel).getRoot();
            default -> new DashboardView(usuario, turnoService, pacienteService, medicoService, pagoService, this::show).getRoot();
        };
        setCenter(scrollable(content));
    }

    private void refreshUserLabel() {
        if (userLabel != null) {
            userLabel.setText(displayName());
        }
    }

    private String displayName() {
        return usuario.getNombreCompleto() == null ? usuario.getUsername() : usuario.getNombreCompleto();
    }

    // Contenedor scrollable que envuelve a la vista inyectada para evitar desbordes en pantallas chicas
    private ScrollPane scrollable(Node content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.getStyleClass().add("content-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);
        return scrollPane;
    }

    private void activate(String key) {
        navButtons.values().forEach(button -> button.getStyleClass().remove("nav-button-active"));
        Button active = navButtons.getOrDefault(key, navButtons.get("HOME"));
        if (active != null && !active.getStyleClass().contains("nav-button-active")) {
            active.getStyleClass().add("nav-button-active");
        }
    }

    private Button navButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinWidth(120);
        button.setWrapText(true);
        return button;
    }

    private Region spacer(double height) {
        Region region = new Region();
        region.setMinHeight(height);
        VBox.setMargin(region, new Insets(0));
        return region;
    }
}
