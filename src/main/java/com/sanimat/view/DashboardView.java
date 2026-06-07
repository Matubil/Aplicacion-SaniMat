package com.sanimat.view;

import com.sanimat.model.EstadoTurno;
import com.sanimat.model.Medico;
import com.sanimat.model.RoleName;
import com.sanimat.model.Turno;
import com.sanimat.model.Usuario;
import com.sanimat.service.MedicoService;
import com.sanimat.service.PacienteService;
import com.sanimat.service.PagoService;
import com.sanimat.service.TurnoService;
import com.sanimat.util.Dialogs;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Pantalla inicial despues del login.
 * Muestra metricas y accesos rapidos distintos segun el rol del usuario.
 */
public class DashboardView {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final Usuario usuario;
    private final TurnoService turnoService;
    private final PacienteService pacienteService;
    private final MedicoService medicoService;
    private final PagoService pagoService;
    private final Consumer<String> navigate;

    public DashboardView(
            Usuario usuario,
            TurnoService turnoService,
            PacienteService pacienteService,
            MedicoService medicoService,
            PagoService pagoService,
            Consumer<String> navigate
    ) {
        this.usuario = usuario;
        this.turnoService = turnoService;
        this.pacienteService = pacienteService;
        this.medicoService = medicoService;
        this.pagoService = pagoService;
        this.navigate = navigate;
    }

    public Node getRoot() {
        return usuario.hasRole(RoleName.MEDICO) ? doctorHome() : secretaryHome();
    }

    private Node secretaryHome() {
        List<Turno> today = turnoService.search(null, LocalDate.now(), usuario);
        List<Turno> all = turnoService.search(null, null, usuario);
        List<Medico> doctors = medicoService.findAll(null);
        long patientsWithAppointmentToday = today.stream()
                .map(Turno::getPacienteId)
                .distinct()
                .count();

        FlowPane metrics = metrics(
                metric("Total de turnos en el dia", String.valueOf(today.size())),
                metric("Pacientes con turno hoy", String.valueOf(patientsWithAppointmentToday)),
                metric("Pendientes de pago", "$ " + (pagoService.findUnpaidAppointments().size() * 5000)),
                metric("Medicos disponibles hoy", String.valueOf(countDoctorsWorkingToday(doctors))),
                metric("Total de pacientes", String.valueOf(pacienteService.findAll(null).size())),
                metric("Total de Medicos", String.valueOf(doctors.size()))
        );

        HBox actions = Ui.actions(
                action("Nuevo Turno", "TURNOS"),
                action("Nuevo Paciente", "PACIENTES"),
                futureAction("Crear Factura", "La generacion de facturas no forma parte del alcance implementado en esta version del prototipo. Queda contemplada como una posible ampliacion futura del sistema."),
                futureAction("Reportes", "La generacion de reportes no forma parte del alcance implementado en esta version del prototipo. Queda contemplada como una posible ampliacion futura del sistema.")
        );

        TableView<Turno> table = appointmentTable(all.stream().limit(12).toList());
        VBox body = new VBox(18, metrics, actions, table);
        return Ui.page(Ui.pageTitle("Bienvenida " + firstName()), body);
    }

    private Node doctorHome() {
        List<Turno> all = turnoService.search(null, null, usuario);
        List<Turno> today = turnoService.search(null, LocalDate.now(), usuario);
        long waiting = all.stream().filter(t -> t.getEstado() == EstadoTurno.CONFIRMADO).count();
        String nextPatient = all.stream()
                .filter(t -> t.getFechaHora().toLocalDate().isEqual(LocalDate.now()) || t.getFechaHora().toLocalDate().isAfter(LocalDate.now()))
                .min(Comparator.comparing(Turno::getFechaHora))
                .map(Turno::getPacienteNombre)
                .orElse("-");

        FlowPane metrics = metrics(
                metric("Total de turnos en el dia", String.valueOf(today.size())),
                metric("En espera", String.valueOf(waiting)),
                metric("Proximo paciente", nextPatient)
        );
        TableView<Turno> table = appointmentTable(all.stream().limit(12).toList());
        VBox body = new VBox(18, metrics, table);
        return Ui.page(Ui.pageTitle("Bienvenido " + firstName()), body);
    }

    private FlowPane metrics(VBox... cards) {
        FlowPane pane = new FlowPane(18, 18, cards);
        pane.setPrefWrapLength(900);
        return pane;
    }

    private VBox metric(String label, String value) {
        VBox box = Ui.card(Ui.fieldLabel(label), Ui.sectionTitle(value));
        box.getStyleClass().add("metric-card");
        box.setMinWidth(165);
        box.setPrefHeight(95);
        return box;
    }

    private long countDoctorsWorkingToday(List<Medico> doctors) {
        int today = LocalDate.now().getDayOfWeek().getValue();
        return doctors.stream()
                .filter(medico -> medicoService.findWorkSchedules(medico.getMedicoId()).stream()
                        .anyMatch(schedule -> schedule.getDiaSemana() == today))
                .count();
    }

    private Button action(String text, String key) {
        Button button = Ui.secondaryButton(text);
        button.setOnAction(event -> navigate.accept(key));
        return button;
    }

    private Button futureAction(String text, String message) {
        Button button = Ui.secondaryButton(text);
        button.getStyleClass().add("future-action-button");
        button.setOnAction(event -> Dialogs.info(text, message));
        return button;
    }

    private TableView<Turno> appointmentTable(List<Turno> turnos) {
        TableView<Turno> table = new TableView<>(FXCollections.observableArrayList(turnos));
        table.getColumns().setAll(
                Ui.column("Paciente", Turno::getPacienteNombre, 175),
                Ui.column("DNI", Turno::getPacienteDni, 105),
                Ui.column("Obra Social", Turno::getTipoCoberturaDisplay, 120),
                Ui.column("Medico", Turno::getMedicoNombre, 175),
                Ui.column("Estado", t -> t.getEstado().getDisplayName(), 115),
                Ui.column("Horario", t -> t.getFechaHora().format(TIME), 95)
        );
        Ui.compactTable(table, 10);
        return table;
    }

    private String firstName() {
        String name = usuario.getNombreCompleto();
        if (name == null || name.isBlank()) {
            return usuario.getUsername();
        }
        int comma = name.indexOf(',');
        return comma >= 0 ? name.substring(comma + 1).trim() : name;
    }
}
