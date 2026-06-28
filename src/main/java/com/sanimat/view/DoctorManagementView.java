package com.sanimat.view;

import com.sanimat.model.Especialidad;
import com.sanimat.model.HorarioMedico;
import com.sanimat.model.Medico;
import com.sanimat.service.EspecialidadService;
import com.sanimat.service.MedicoService;
import com.sanimat.util.Dialogs;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
   Pantalla de gestion de medicos para secretaria.
   Incluye datos personales, especialidad y horarios laborales.
 */
public class DoctorManagementView {
    private final MedicoService medicoService;
    private final EspecialidadService especialidadService;
    private final ObservableList<Medico> data = FXCollections.observableArrayList();
    private final TableView<Medico> table = new TableView<>(data);
    private final TextField searchField = Ui.textField("Buscar por nombre, DNI o matricula");
    private final TextField dniField = Ui.textField("DNI");
    private final TextField nameField = Ui.textField("Nombre");
    private final TextField lastNameField = Ui.textField("Apellido");
    private final ComboBox<String> genderCombo = new ComboBox<>(FXCollections.observableArrayList("Masculino", "Femenino", "Otro"));
    private final DatePicker birthDatePicker = new DatePicker();
    private final TextField phoneField = Ui.textField("Telefono");
    private final TextField emailField = Ui.textField("Mail");
    private final TextField addressField = Ui.textField("Direccion");
    private final TextField licenseField = Ui.textField("Matricula");
    private final TextField userField = Ui.textField("Usuario");
    private final TextField passwordField = Ui.textField("Contraseña");
    private final ComboBox<Especialidad> specialtyCombo = new ComboBox<>();
    private final ObservableList<HorarioMedico> scheduleData = FXCollections.observableArrayList();
    private final TableView<HorarioMedico> scheduleTable = new TableView<>(scheduleData);
    private final ComboBox<DayOption> scheduleDayCombo = new ComboBox<>(FXCollections.observableArrayList(DayOption.values()));
    private final ComboBox<LocalTime> scheduleStartCombo = new ComboBox<>(FXCollections.observableArrayList(timeOptions()));
    private final ComboBox<LocalTime> scheduleEndCombo = new ComboBox<>(FXCollections.observableArrayList(timeOptions()));
    private Button addScheduleButton;
    private Button removeScheduleButton;
    private Button editButton;
    private Button saveButton;
    private Button deleteButton;
    private Medico selected;
    private boolean editing;

    public DoctorManagementView(MedicoService medicoService, EspecialidadService especialidadService) {
        this.medicoService = medicoService;
        this.especialidadService = especialidadService;
    }

    public Node getRoot() {
        // Inicializa la configuración de la tabla principal y la de horarios
        configureTable();
        configureScheduleTable();
        
        // Carga especialidades en el combo y configura anchos máximos elásticos
        specialtyCombo.setItems(FXCollections.observableArrayList(especialidadService.findAll(null)));
        specialtyCombo.setMaxWidth(Double.MAX_VALUE);
        genderCombo.setMaxWidth(Double.MAX_VALUE);
        birthDatePicker.setMaxWidth(Double.MAX_VALUE);
        Ui.configureBirthDatePicker(birthDatePicker, LocalDate.now().minusYears(18));
        scheduleDayCombo.setMaxWidth(Double.MAX_VALUE);
        scheduleStartCombo.setMaxWidth(Double.MAX_VALUE);
        scheduleEndCombo.setMaxWidth(Double.MAX_VALUE);
        
        // Restricciones de entrada de datos en los campos de texto
        Ui.digitsOnly(dniField, 8);
        Ui.digitsOnly(licenseField, 9);
        Ui.phoneOnly(phoneField);

        Button searchButton = Ui.secondaryButton("Buscar");
        searchButton.setOnAction(event -> load());
        Button newButton = Ui.secondaryButton("Registrar Nuevo Medico");
        newButton.setOnAction(event -> startCreate());
        editButton = Ui.secondaryButton("Modificar");
        editButton.setOnAction(event -> startEdit());
        saveButton = Ui.primaryButton("Guardar");
        saveButton.setOnAction(event -> save());
        deleteButton = Ui.dangerButton("Eliminar");
        deleteButton.setOnAction(event -> delete());
        addScheduleButton = Ui.secondaryButton("Agregar horario");
        addScheduleButton.setOnAction(event -> addSchedule());
        removeScheduleButton = Ui.dangerButton("Quitar horario");
        removeScheduleButton.setOnAction(event -> removeSchedule());

        // Panel de estadísticas superiores (KPIs) sobre el personal
        HBox summary = new HBox(14,
                Ui.card(Ui.fieldLabel("Total de personal"), Ui.sectionTitle(String.valueOf(medicoService.findAll(null).size()))),
                Ui.card(Ui.fieldLabel("Atendiendo"), Ui.sectionTitle("0"))
        );
        
        // Panel de filtrado horizontal
        HBox filters = Ui.actions(Ui.fieldLabel("Buscar"), searchField, searchButton);
        HBox.setHgrow(searchField, Priority.ALWAYS); // Campo de búsqueda crece horizontalmente
        
        Ui.compactTable(table, 14);
        
        // Tarjeta lateral izquierda que contiene la tabla principal de médicos
        VBox tableCard = Ui.card(Ui.sectionTitle("Gestion Medica"), summary, filters, table);
        tableCard.setMinWidth(560);

        // Tarjeta lateral derecha que conforma el formulario de edición (datos personales + horarios + acciones)
        VBox form = Ui.card(Ui.sectionTitle("Datos del medico"), formGrid(), scheduleSection(), doctorActions(newButton, editButton, saveButton, deleteButton));
        form.setMinWidth(420);
        form.setPrefWidth(460);
        
        // Contenedor general que organiza la pantalla en dos columnas y ajusta el crecimiento
        HBox body = new HBox(18, tableCard, form);
        HBox.setHgrow(tableCard, Priority.ALWAYS);
        load();
        clearForm();
        return Ui.page(Ui.pageTitle("Gestion de medicos"), body);
    }

    // Configura columnas y listeners de selección para la tabla de médicos
    private void configureTable() {
        table.getColumns().setAll(
                Ui.column("ID", Medico::getMedicoId, 70),
                Ui.column("DNI", Medico::getDni, 100),
                Ui.column("Medico", Medico::getNombreCompleto, 200),
                Ui.column("Matricula", Medico::getMatricula, 110),
                Ui.column("Especialidad", Medico::getEspecialidadNombre, 170)
        );
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> fillForm(value));
    }

    // Configura columnas y formato visual para la tabla interna de horarios laborales del médico
    private void configureScheduleTable() {
        scheduleTable.getColumns().setAll(
                Ui.column("Dia", h -> dayLabel(h.getDiaSemana()), 110),
                Ui.column("Desde", HorarioMedico::getHoraDesde, 90),
                Ui.column("Hasta", HorarioMedico::getHoraHasta, 90)
        );
        scheduleTable.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> refreshActionState());
        Ui.compactTable(scheduleTable, 5);
    }

    // Construye un grid ordenado de dos columnas para los campos de datos del formulario del médico
    private GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.add(Ui.formRow("DNI", dniField), 0, 0);
        grid.add(Ui.formRow("Nombre", nameField), 1, 0);
        grid.add(Ui.formRow("Apellido", lastNameField), 0, 1);
        grid.add(Ui.formRow("Genero", genderCombo), 1, 1);
        grid.add(Ui.formRow("Fecha nacimiento", birthDatePicker), 0, 2);
        grid.add(Ui.formRow("Telefono", phoneField), 1, 2);
        grid.add(Ui.formRow("Mail", emailField), 0, 3);
        grid.add(Ui.formRow("Direccion", addressField), 1, 3);
        grid.add(Ui.formRow("Matricula", licenseField), 0, 4);
        grid.add(Ui.formRow("Especialidad", specialtyCombo), 1, 4);
        grid.add(Ui.formRow("Usuario", userField), 0, 5);
        grid.add(Ui.formRow("Contraseña", passwordField), 1, 5);
        return grid;
    }

    // Contenedor para agrupar y homogeneizar el ancho de los botones principales del formulario
    private VBox doctorActions(Button newButton, Button editButton, Button saveButton, Button deleteButton) {
        HBox mainActions = new HBox(10, newButton, editButton);
        mainActions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        for (Button button : new Button[]{newButton, editButton}) {
            button.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(button, Priority.ALWAYS);
        }
        saveButton.setMaxWidth(Double.MAX_VALUE);
        deleteButton.setMaxWidth(Double.MAX_VALUE);
        return new VBox(10, mainActions, saveButton, deleteButton);
    }

    // Sección interna de gestión de horarios del médico, que cuenta con su propio grid, botones y tabla compacta
    private VBox scheduleSection() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.add(Ui.formRow("Dia", scheduleDayCombo), 0, 0, 2, 1);
        grid.add(Ui.formRow("Desde", scheduleStartCombo), 0, 1);
        grid.add(Ui.formRow("Hasta", scheduleEndCombo), 1, 1);

        HBox actions = Ui.actions(addScheduleButton, removeScheduleButton);
        addScheduleButton.setMaxWidth(Double.MAX_VALUE);
        removeScheduleButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(addScheduleButton, Priority.ALWAYS);
        HBox.setHgrow(removeScheduleButton, Priority.ALWAYS);

        return new VBox(10, Ui.sectionTitle("Horarios laborales"), grid, actions, scheduleTable);
    }

    private void load() {
        data.setAll(medicoService.findAll(searchField.getText()));
    }

    private void save() {
        try {
            Medico medico = selected == null ? new Medico() : selected;
            medico.setDni(dniField.getText());
            medico.setNombre(nameField.getText());
            medico.setApellido(lastNameField.getText());
            medico.setGenero(genderCombo.getValue());
            medico.setFechaNacimiento(Ui.datePickerValue(birthDatePicker, "fecha de nacimiento"));
            medico.setTelefono(phoneField.getText());
            medico.setEmail(emailField.getText());
            medico.setDireccion(addressField.getText());
            medico.setMatricula(parseLicense());
            medico.setUsuario(userField.getText());
            medico.setContrasenia(passwordField.getText());
            Especialidad especialidad = specialtyCombo.getValue();
            medico.setEspecialidadId(especialidad == null ? 0 : especialidad.getId());
            medico.setHorarios(new ArrayList<>(scheduleData));
            medicoService.save(medico);
            Dialogs.info("Medico", "Datos guardados correctamente.");
            clearForm();
            load();
        } catch (RuntimeException ex) {
            Dialogs.error("Medico", ex.getMessage());
        }
    }

    private void delete() {
        try {
            if (selected != null && Dialogs.confirm("Medico", "Desea eliminar el medico seleccionado?")) {
                medicoService.delete(selected);
                clearForm();
                load();
            }
        } catch (RuntimeException ex) {
            Dialogs.error("Medico", ex.getMessage());
        }
    }

    private void fillForm(Medico medico) {
        selected = medico;
        if (medico == null) {
            clearFields();
            setEditing(false);
            return;
        }
        dniField.setText(medico.getDni());
        nameField.setText(medico.getNombre());
        lastNameField.setText(medico.getApellido());
        genderCombo.setValue(medico.getGenero());
        birthDatePicker.setValue(medico.getFechaNacimiento());
        phoneField.setText(medico.getTelefono());
        emailField.setText(medico.getEmail());
        addressField.setText(medico.getDireccion());
        licenseField.setText(String.valueOf(medico.getMatricula()));
        userField.setText(medico.getUsuario());
        passwordField.setText(medico.getContrasenia());
        specialtyCombo.getItems().stream()
                .filter(e -> e.getId() == medico.getEspecialidadId())
                .findFirst()
                .ifPresent(specialtyCombo::setValue);
        scheduleData.setAll(medicoService.findWorkSchedules(medico.getMedicoId()));
        clearScheduleInputs();
        setEditing(false);
    }

    private void clearForm() {
        selected = null;
        table.getSelectionModel().clearSelection();
        clearFields();
        setEditing(false);
    }

    private void clearFields() {
        dniField.clear();
        nameField.clear();
        lastNameField.clear();
        genderCombo.setValue(null);
        birthDatePicker.setValue(null);
        phoneField.clear();
        emailField.clear();
        addressField.clear();
        licenseField.clear();
        userField.clear();
        passwordField.clear();
        specialtyCombo.setValue(null);
        scheduleData.clear();
        clearScheduleInputs();
    }

    private void startCreate() {
        selected = null;
        table.getSelectionModel().clearSelection();
        clearFields();
        setEditing(true);
    }

    private void startEdit() {
        if (selected == null) {
            Dialogs.info("Medico", "Seleccione un medico para modificar.");
            return;
        }
        setEditing(true);
    }

    private void setEditing(boolean editing) {
        this.editing = editing;
        setFormEditable(editing);
        refreshActionState();
    }

    private void setFormEditable(boolean editable) {
        Control[] controls = {
                dniField, nameField, lastNameField, genderCombo, birthDatePicker, phoneField,
                emailField, addressField, licenseField, specialtyCombo, userField, passwordField,
                scheduleDayCombo, scheduleStartCombo, scheduleEndCombo
        };
        for (Control control : controls) {
            control.setDisable(!editable);
        }
        if (addScheduleButton != null) {
            addScheduleButton.setDisable(!editable);
        }
    }

    private void refreshActionState() {
        boolean hasSelection = selected != null;
        if (editButton != null) {
            editButton.setDisable(!hasSelection || editing);
        }
        if (saveButton != null) {
            saveButton.setDisable(!editing);
        }
        if (deleteButton != null) {
            deleteButton.setDisable(!hasSelection || editing);
        }
        if (removeScheduleButton != null) {
            removeScheduleButton.setDisable(!editing || scheduleTable.getSelectionModel().getSelectedItem() == null);
        }
    }

    private void addSchedule() {
        DayOption day = scheduleDayCombo.getValue();
        LocalTime start = scheduleStartCombo.getValue();
        LocalTime end = scheduleEndCombo.getValue();
        if (day == null || start == null || end == null) {
            Dialogs.info("Horarios laborales", "Seleccione dia, hora desde y hora hasta.");
            return;
        }
        if (!start.isBefore(end)) {
            Dialogs.info("Horarios laborales", "La hora desde debe ser menor que la hora hasta.");
            return;
        }

        HorarioMedico schedule = new HorarioMedico();
        schedule.setDiaSemana(day.value());
        schedule.setHoraDesde(start);
        schedule.setHoraHasta(end);
        schedule.setActivo(true);

        if (hasScheduleOverlap(schedule)) {
            Dialogs.info("Horarios laborales", "Ese rango se superpone con otro horario cargado para el mismo dia.");
            return;
        }

        scheduleData.add(schedule);
        sortScheduleData();
        clearScheduleInputs();
        refreshActionState();
    }

    private void removeSchedule() {
        HorarioMedico selectedSchedule = scheduleTable.getSelectionModel().getSelectedItem();
        if (selectedSchedule != null) {
            scheduleData.remove(selectedSchedule);
            refreshActionState();
        }
    }

    private boolean hasScheduleOverlap(HorarioMedico candidate) {
        return scheduleData.stream()
                .filter(schedule -> schedule.getDiaSemana() == candidate.getDiaSemana())
                .anyMatch(schedule -> candidate.getHoraDesde().isBefore(schedule.getHoraHasta())
                        && candidate.getHoraHasta().isAfter(schedule.getHoraDesde()));
    }

    private void sortScheduleData() {
        List<HorarioMedico> sorted = scheduleData.stream()
                .sorted(Comparator.comparingInt(HorarioMedico::getDiaSemana)
                        .thenComparing(HorarioMedico::getHoraDesde))
                .toList();
        scheduleData.setAll(sorted);
    }

    private void clearScheduleInputs() {
        scheduleDayCombo.setValue(null);
        scheduleStartCombo.setValue(null);
        scheduleEndCombo.setValue(null);
        scheduleTable.getSelectionModel().clearSelection();
    }

    private String dayLabel(int day) {
        return switch (day) {
            case 1 -> "Lunes";
            case 2 -> "Martes";
            case 3 -> "Miercoles";
            case 4 -> "Jueves";
            case 5 -> "Viernes";
            case 6 -> "Sabado";
            case 7 -> "Domingo";
            default -> "-";
        };
    }

    private static List<LocalTime> timeOptions() {
        List<LocalTime> times = new ArrayList<>();
        LocalTime time = LocalTime.of(7, 0);
        while (!time.isAfter(LocalTime.of(21, 0))) {
            times.add(time);
            time = time.plusMinutes(30);
        }
        return times;
    }

    private int parseLicense() {
        try {
            return Integer.parseInt(licenseField.getText().trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("La matricula debe ser numerica.");
        }
    }

    private enum DayOption {
        MONDAY(1, "Lunes"),
        TUESDAY(2, "Martes"),
        WEDNESDAY(3, "Miercoles"),
        THURSDAY(4, "Jueves"),
        FRIDAY(5, "Viernes"),
        SATURDAY(6, "Sabado"),
        SUNDAY(7, "Domingo");

        private final int value;
        private final String label;

        DayOption(int value, String label) {
            this.value = value;
            this.label = label;
        }

        int value() {
            return value;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
