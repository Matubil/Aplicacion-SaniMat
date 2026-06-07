package com.sanimat.view;

import com.sanimat.model.EstadoTurno;
import com.sanimat.model.HorarioMedico;
import com.sanimat.model.Medico;
import com.sanimat.model.Paciente;
import com.sanimat.model.RoleName;
import com.sanimat.model.Turno;
import com.sanimat.model.Usuario;
import com.sanimat.service.MedicoService;
import com.sanimat.service.PacienteService;
import com.sanimat.service.TurnoService;
import com.sanimat.util.Dialogs;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/**
 * Pantalla de gestion de turnos.
 * Secretaria administra la agenda completa; medico solo actualiza atencion.
 */
public class AppointmentManagementView {
    private final TurnoService turnoService;
    private final PacienteService pacienteService;
    private final MedicoService medicoService;
    private final Usuario usuario;
    private final ObservableList<Turno> data = FXCollections.observableArrayList();
    private final ObservableList<Turno> closedData = FXCollections.observableArrayList();
    private final TableView<Turno> table = new TableView<>(data);
    private final TableView<Turno> closedTable = new TableView<>(closedData);
    private final TextField searchField = Ui.textField("Buscar por paciente, medico o especialidad");
    private final DatePicker filterDatePicker = new DatePicker();
    private final ComboBox<Paciente> patientCombo = new ComboBox<>();
    private final ComboBox<Medico> doctorCombo = new ComboBox<>();
    private final DatePicker datePicker = new DatePicker();
    private final ComboBox<TimeOption> timeCombo = new ComboBox<>();
    private final ComboBox<EstadoTurno> statusCombo = new ComboBox<>(FXCollections.observableArrayList(EstadoTurno.values()));
    private final ComboBox<EstadoTurno> doctorStatusCombo = new ComboBox<>(FXCollections.observableArrayList(
            EstadoTurno.FINALIZADO,
            EstadoTurno.AUSENTE
    ));
    private Button editButton;
    private Button saveButton;
    private Button cancelButton;
    private Button doctorSaveStatusButton;
    private Turno selected;
    private List<HorarioMedico> selectedDoctorSchedules = List.of();
    private boolean editing;

    public AppointmentManagementView(TurnoService turnoService, PacienteService pacienteService, MedicoService medicoService, Usuario usuario) {
        this.turnoService = turnoService;
        this.pacienteService = pacienteService;
        this.medicoService = medicoService;
        this.usuario = usuario;
    }

    public Node getRoot() {
        configureTable();
        configureClosedTable();
        patientCombo.setItems(FXCollections.observableArrayList(pacienteService.findAll(null)));
        doctorCombo.setItems(FXCollections.observableArrayList(medicoService.findAll(null)));
        Ui.configureDatePicker(filterDatePicker);
        Ui.configureDatePicker(datePicker);
        configureTimeCombo();
        configureScheduleControls();
        patientCombo.setMaxWidth(Double.MAX_VALUE);
        doctorCombo.setMaxWidth(Double.MAX_VALUE);
        datePicker.setMaxWidth(Double.MAX_VALUE);
        timeCombo.setMaxWidth(Double.MAX_VALUE);
        statusCombo.setMaxWidth(Double.MAX_VALUE);
        doctorStatusCombo.setMaxWidth(Double.MAX_VALUE);

        Button searchButton = Ui.secondaryButton("Buscar");
        searchButton.setOnAction(event -> load());
        Button clearFilterButton = Ui.secondaryButton("Limpiar filtro");
        clearFilterButton.setOnAction(event -> {
            searchField.clear();
            filterDatePicker.setValue(usuario.hasRole(RoleName.MEDICO) ? LocalDate.now() : null);
            load();
        });
        HBox filters = Ui.actions(
                Ui.fieldLabel(usuario.hasRole(RoleName.MEDICO) ? "Filtrar paciente" : "Filtrar paciente/medico"),
                searchField,
                filterDatePicker,
                searchButton,
                clearFilterButton
        );
        HBox.setHgrow(searchField, Priority.ALWAYS);

        if (usuario.hasRole(RoleName.MEDICO)) {
            // El medico trabaja sobre sus turnos del dia: arriba quedan los pendientes
            // y abajo los que ya fueron cerrados por estado.
            filterDatePicker.setValue(LocalDate.now());
            Ui.compactTable(table, 7);
            Ui.compactTable(closedTable, 7);
            doctorSaveStatusButton = Ui.primaryButton("Guardar estado");
            doctorSaveStatusButton.setOnAction(event -> saveDoctorStatus());
            VBox confirmedCard = Ui.card(Ui.sectionTitle("Turnos confirmados del dia"), filters, table);
            VBox closedCard = Ui.card(Ui.sectionTitle("Turnos cerrados del dia"), closedTable);
            VBox tables = new VBox(18, confirmedCard, closedCard);
            tables.setMinWidth(560);
            VBox statusForm = Ui.card(
                    Ui.sectionTitle("Actualizar atencion"),
                    Ui.formRow("Nuevo estado", doctorStatusCombo),
                    Ui.actionStack(doctorSaveStatusButton)
            );
            statusForm.setMinWidth(300);
            statusForm.setPrefWidth(330);
            HBox body = new HBox(18, tables, statusForm);
            HBox.setHgrow(tables, Priority.ALWAYS);
            clearDoctorStatus();
            load();
            return Ui.page(Ui.pageTitle("Mis turnos"), body);
        }

        Button newButton = Ui.secondaryButton("Nuevo");
        newButton.setOnAction(event -> startCreate());
        editButton = Ui.secondaryButton("Modificar");
        editButton.setOnAction(event -> startEdit());
        saveButton = Ui.primaryButton("Guardar");
        saveButton.setOnAction(event -> save());
        cancelButton = Ui.dangerButton("Cancelar turno");
        cancelButton.setOnAction(event -> cancel());

        Ui.compactTable(table, 16);
        VBox tableCard = Ui.card(Ui.sectionTitle("Turnos"), filters, table);
        tableCard.setMinWidth(560);
        VBox form = Ui.card(Ui.sectionTitle("Datos del turno"), formGrid(), appointmentActions(newButton, editButton, saveButton, cancelButton));
        form.setMinWidth(300);
        form.setPrefWidth(330);
        HBox body = new HBox(18, tableCard, form);
        HBox.setHgrow(tableCard, Priority.ALWAYS);
        clearForm();
        load();
        return Ui.page(Ui.pageTitle("Gestion de turnos"), body);
    }

    private void configureTable() {
        configureAppointmentColumns(table);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> {
            if (value != null && usuario.hasRole(RoleName.MEDICO)) {
                closedTable.getSelectionModel().clearSelection();
            }
            fillForm(value);
        });
    }

    private void configureClosedTable() {
        configureAppointmentColumns(closedTable);
        closedTable.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> {
            if (value != null && usuario.hasRole(RoleName.MEDICO)) {
                table.getSelectionModel().clearSelection();
            }
            if (usuario.hasRole(RoleName.MEDICO)) {
                fillForm(value);
            }
        });
    }

    private void configureAppointmentColumns(TableView<Turno> target) {
        target.getColumns().setAll(
                Ui.column("Paciente", Turno::getPacienteNombre, 165),
                Ui.column("DNI", Turno::getPacienteDni, 100),
                Ui.column("Obra Social", Turno::getTipoCoberturaDisplay, 120),
                Ui.column("Medico", Turno::getMedicoNombre, 165),
                Ui.dateColumn("Fecha", t -> t.getFechaHora().toLocalDate(), 110),
                Ui.column("Horario", t -> t.getFechaHora().toLocalTime(), 90),
                Ui.column("Estado", t -> t.getEstado().getDisplayName(), 115),
                Ui.column("Abono", t -> t.isPagoRegistrado() ? "Si" : "No", 80)
        );
        // El orden se controla desde load(), para evitar que un click en el encabezado
        // mezcle fechas y horarios de forma poco clara.
        target.getColumns().forEach(column -> column.setSortable(false));
    }

    private GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.add(Ui.formRow("Paciente", patientCombo), 0, 0);
        grid.add(Ui.formRow("Medico", doctorCombo), 0, 1);
        grid.add(Ui.formRow("Fecha", datePicker), 0, 2);
        grid.add(Ui.formRow("Hora", timeCombo), 0, 3);
        grid.add(Ui.formRow("Estado", statusCombo), 0, 4);
        return grid;
    }

    private VBox appointmentActions(Button newButton, Button editButton, Button saveButton, Button cancelButton) {
        HBox mainActions = new HBox(10, newButton, editButton);
        mainActions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        for (Button button : new Button[]{newButton, editButton}) {
            button.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(button, Priority.ALWAYS);
        }
        saveButton.setMaxWidth(Double.MAX_VALUE);
        cancelButton.setMaxWidth(Double.MAX_VALUE);
        return new VBox(10, mainActions, saveButton, cancelButton);
    }

    private void load() {
        try {
            LocalDate filterDate = Ui.datePickerValue(filterDatePicker, "fecha de filtro");
            if (usuario.hasRole(RoleName.MEDICO)) {
                // Para el medico se fuerza una fecha de trabajo y se separan los turnos
                // confirmados de los que ya quedaron ausentes, cancelados o finalizados.
                LocalDate doctorDate = filterDate == null ? LocalDate.now() : filterDate;
                filterDatePicker.setValue(doctorDate);
                List<Turno> dayTurns = turnoService.search(searchField.getText(), doctorDate, usuario).stream()
                        .sorted(Comparator.comparing(Turno::getFechaHora))
                        .toList();
                data.setAll(dayTurns.stream()
                        .filter(turno -> turno.getEstado() == EstadoTurno.CONFIRMADO)
                        .toList());
                closedData.setAll(dayTurns.stream()
                        .filter(turno -> turno.getEstado() != EstadoTurno.CONFIRMADO)
                        .toList());
                refreshDoctorStatusActionState();
                return;
            }
            // Secretaria ve la agenda general ordenada desde lo mas reciente a lo mas viejo.
            List<Turno> turnos = turnoService.search(searchField.getText(), filterDate, usuario).stream()
                    .sorted(Comparator.comparing(Turno::getFechaHora).reversed())
                    .toList();
            table.getSortOrder().clear();
            data.setAll(turnos);
        } catch (RuntimeException ex) {
            Dialogs.error("Turno", ex.getMessage());
        }
    }

    private void save() {
        try {
            Paciente patient = patientCombo.getValue();
            Medico doctor = doctorCombo.getValue();
            LocalDate date = Ui.datePickerValue(datePicker, "fecha del turno");
            TimeOption timeOption = timeCombo.getValue();
            LocalTime time = timeOption == null ? null : timeOption.time();
            Turno turno = selected == null ? new Turno() : selected;
            turno.setPacienteId(patient == null ? 0 : patient.getPacienteId());
            turno.setMedicoId(doctor == null ? 0 : doctor.getMedicoId());
            turno.setEspecialidadId(doctor == null ? 0 : doctor.getEspecialidadId());
            turno.setFechaHora(date == null || time == null ? null : LocalDateTime.of(date, time));
            turno.setEstado(statusCombo.getValue());
            turnoService.save(turno, usuario);
            Dialogs.info("Turno", "Datos guardados correctamente.");
            clearForm();
            load();
        } catch (RuntimeException ex) {
            Dialogs.error("Turno", ex.getMessage());
        }
    }

    private void saveDoctorStatus() {
        try {
            EstadoTurno status = doctorStatusCombo.getValue();
            turnoService.updateAttendanceStatusForMedico(selected, status, usuario);
            Dialogs.info("Turno", "Estado del turno actualizado correctamente.");
            clearDoctorStatus();
            load();
        } catch (RuntimeException ex) {
            Dialogs.error("Turno", ex.getMessage());
        }
    }

    private void cancel() {
        try {
            turnoService.cancel(selected);
            Dialogs.info("Turno", "Turno cancelado correctamente.");
            clearForm();
            load();
        } catch (RuntimeException ex) {
            Dialogs.error("Turno", ex.getMessage());
        }
    }

    private void fillForm(Turno turno) {
        selected = turno;
        if (usuario.hasRole(RoleName.MEDICO)) {
            fillDoctorStatus(turno);
            return;
        }
        if (turno == null) {
            clearFields();
            setEditing(false);
            return;
        }
        showAllStatusOptions();
        patientCombo.getItems().stream()
                .filter(patient -> patient.getPacienteId() == turno.getPacienteId())
                .findFirst()
                .ifPresent(patientCombo::setValue);
        doctorCombo.getItems().stream()
                .filter(doctor -> doctor.getMedicoId() == turno.getMedicoId())
                .findFirst()
                .ifPresent(doctorCombo::setValue);
        datePicker.setValue(turno.getFechaHora().toLocalDate());
        updateTimeOptions();
        selectTime(turno.getFechaHora().toLocalTime());
        statusCombo.setValue(turno.getEstado());
        setEditing(false);
    }

    private void fillDoctorStatus(Turno turno) {
        selected = turno;
        doctorStatusCombo.setValue(null);
        refreshDoctorStatusActionState();
    }

    private void clearForm() {
        selected = null;
        table.getSelectionModel().clearSelection();
        clearFields();
        setEditing(false);
    }

    private void clearFields() {
        showAllStatusOptions();
        patientCombo.setValue(null);
        doctorCombo.setValue(null);
        datePicker.setValue(null);
        timeCombo.getItems().clear();
        timeCombo.setValue(null);
        statusCombo.setValue(EstadoTurno.CONFIRMADO);
    }

    private void startCreate() {
        selected = null;
        table.getSelectionModel().clearSelection();
        clearFields();
        statusCombo.setItems(FXCollections.observableArrayList(EstadoTurno.CONFIRMADO));
        statusCombo.setValue(EstadoTurno.CONFIRMADO);
        setEditing(true);
    }

    private void startEdit() {
        if (selected == null) {
            Dialogs.info("Turno", "Seleccione un turno para modificar.");
            return;
        }
        if (isFinalized(selected)) {
            Dialogs.info("Turno", "No se puede modificar un turno finalizado.");
            return;
        }
        if (selected.getEstado() == EstadoTurno.CANCELADO) {
            Dialogs.info("Turno", "No se puede modificar un turno cancelado.");
            return;
        }
        showEditableStatusOptions();
        statusCombo.setValue(selected.getEstado());
        setEditing(true);
    }

    private void setEditing(boolean editing) {
        this.editing = editing;
        setFormEditable(editing);
        refreshActionState();
    }

    private void setFormEditable(boolean editable) {
        Control[] controls = {patientCombo, doctorCombo, datePicker, timeCombo};
        for (Control control : controls) {
            control.setDisable(!editable);
        }
        statusCombo.setDisable(!editable || selected == null);
    }

    private void refreshActionState() {
        boolean hasSelection = selected != null;
        boolean closed = isFinalized(selected) || isCanceled(selected);
        if (editButton != null) {
            editButton.setDisable(!hasSelection || closed || editing);
        }
        if (saveButton != null) {
            saveButton.setDisable(!editing);
        }
        if (cancelButton != null) {
            cancelButton.setDisable(!hasSelection || closed || editing);
        }
    }

    private boolean isFinalized(Turno turno) {
        return turno != null && turno.getEstado() == EstadoTurno.FINALIZADO;
    }

    private boolean isCanceled(Turno turno) {
        return turno != null && turno.getEstado() == EstadoTurno.CANCELADO;
    }

    private void showAllStatusOptions() {
        statusCombo.setItems(FXCollections.observableArrayList(EstadoTurno.values()));
    }

    private void showEditableStatusOptions() {
        statusCombo.setItems(FXCollections.observableArrayList(
                EstadoTurno.CONFIRMADO,
                EstadoTurno.AUSENTE,
                EstadoTurno.FINALIZADO
        ));
    }

    private void clearDoctorStatus() {
        selected = null;
        table.getSelectionModel().clearSelection();
        closedTable.getSelectionModel().clearSelection();
        doctorStatusCombo.setValue(null);
        refreshDoctorStatusActionState();
    }

    private void refreshDoctorStatusActionState() {
        // El medico solo puede cerrar turnos confirmados que sean de hoy o anteriores.
        boolean canUpdate = selected != null
                && selected.getEstado() == EstadoTurno.CONFIRMADO
                && !selected.getFechaHora().toLocalDate().isAfter(LocalDate.now());
        doctorStatusCombo.setDisable(!canUpdate);
        if (doctorSaveStatusButton != null) {
            doctorSaveStatusButton.setDisable(!canUpdate);
        }
    }

    private void configureScheduleControls() {
        doctorCombo.valueProperty().addListener((obs, old, value) -> {
            loadSelectedDoctorSchedules(value);
            configureDatePickerDays();
            if (datePicker.getValue() != null && !worksOnDate(datePicker.getValue())) {
                datePicker.setValue(null);
            } else {
                updateTimeOptions();
            }
        });
        datePicker.valueProperty().addListener((obs, old, value) -> updateTimeOptions());
        configureDatePickerDays();
    }

    private void configureDatePickerDays() {
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty) {
                    return;
                }
                Medico doctor = doctorCombo.getValue();
                boolean disabled = doctor == null || date.isBefore(LocalDate.now()) || !worksOnDate(date);
                setDisable(disabled);
                setStyle("");
                if (!disabled) {
                    setStyle("-fx-background-color: #e8f3ff;");
                }
            }
        });
    }

    private void configureTimeCombo() {
        timeCombo.setCellFactory(combo -> new ListCell<>() {
            @Override
            protected void updateItem(TimeOption item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setDisable(false);
                    return;
                }
                setText(item.toString());
                setDisable(!item.available());
            }
        });
        timeCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(TimeOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        });
        timeCombo.valueProperty().addListener((obs, old, value) -> {
            if (value != null && !value.available()) {
                timeCombo.setValue(old != null && old.available() ? old : null);
            }
        });
    }

    private void loadSelectedDoctorSchedules(Medico doctor) {
        selectedDoctorSchedules = doctor == null ? List.of() : turnoService.findWorkSchedules(doctor.getMedicoId());
    }

    private boolean worksOnDate(LocalDate date) {
        return selectedDoctorSchedules.stream()
                .anyMatch(schedule -> schedule.getDiaSemana() == date.getDayOfWeek().getValue());
    }

    private void updateTimeOptions() {
        Medico doctor = doctorCombo.getValue();
        LocalDate date = datePicker.getValue();
        timeCombo.getItems().clear();
        timeCombo.setValue(null);
        if (doctor == null || date == null) {
            return;
        }
        int excludeTurnoId = selected == null ? 0 : selected.getId();
        List<LocalTime> bookedTimes = turnoService.findBookedTimes(doctor.getMedicoId(), date, excludeTurnoId);
        // Genera bloques de 30 minutos segun horarios laborales del medico.
        // Los ocupados se muestran, pero no se pueden seleccionar.
        List<TimeOption> options = selectedDoctorSchedules.stream()
                .filter(schedule -> schedule.getDiaSemana() == date.getDayOfWeek().getValue())
                .flatMap(schedule -> timeSlots(schedule).stream())
                .distinct()
                .sorted()
                .map(time -> new TimeOption(time, isSelectedTime(date, time)
                        || (!bookedTimes.contains(time) && LocalDateTime.of(date, time).isAfter(LocalDateTime.now()))))
                .toList();
        timeCombo.setItems(FXCollections.observableArrayList(options));
        options.stream()
                .filter(TimeOption::available)
                .findFirst()
                .ifPresent(timeCombo::setValue);
    }

    private boolean isSelectedTime(LocalDate date, LocalTime time) {
        return selected != null
                && selected.getFechaHora() != null
                && selected.getFechaHora().toLocalDate().equals(date)
                && selected.getFechaHora().toLocalTime().equals(time);
    }

    private List<LocalTime> timeSlots(HorarioMedico schedule) {
        java.util.ArrayList<LocalTime> slots = new java.util.ArrayList<>();
        LocalTime time = schedule.getHoraDesde();
        while (time.isBefore(schedule.getHoraHasta())) {
            slots.add(time);
            time = time.plusMinutes(30);
        }
        return slots;
    }

    private void selectTime(LocalTime time) {
        timeCombo.getItems().stream()
                .filter(option -> option.time().equals(time))
                .findFirst()
                .ifPresent(timeCombo::setValue);
    }

    private record TimeOption(LocalTime time, boolean available) {
        @Override
        public String toString() {
            return available ? time.toString() : time + " (ocupado)";
        }
    }
}
