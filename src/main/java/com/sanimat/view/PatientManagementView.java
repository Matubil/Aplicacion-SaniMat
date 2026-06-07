package com.sanimat.view;

import com.sanimat.model.Paciente;
import com.sanimat.model.TipoCobertura;
import com.sanimat.service.PacienteService;
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

/**
   Pantalla de gestion de pacientes para secretaria.
   Usa modo lectura por defecto y habilita edicion solo con Nuevo o Modificar.
 */
public class PatientManagementView {
    private final PacienteService service;
    private final ObservableList<Paciente> data = FXCollections.observableArrayList();
    private final TableView<Paciente> table = new TableView<>(data);
    private final TextField searchField = Ui.textField("Buscar por nombre o DNI");
    private final TextField dniField = Ui.textField("DNI");
    private final TextField nameField = Ui.textField("Nombre");
    private final TextField lastNameField = Ui.textField("Apellido");
    private final ComboBox<String> genderCombo = new ComboBox<>(FXCollections.observableArrayList("Masculino", "Femenino", "Otro"));
    private final DatePicker birthDatePicker = new DatePicker();
    private final TextField addressField = Ui.textField("Direccion");
    private final TextField phoneField = Ui.textField("Telefono");
    private final TextField emailField = Ui.textField("Mail");
    private final ComboBox<TipoCobertura> coverageCombo = new ComboBox<>(FXCollections.observableArrayList(TipoCobertura.values()));
    private final TextField affiliateField = Ui.textField("Numero afiliado");
    private Button editButton;
    private Button saveButton;
    private Button deleteButton;
    private Paciente selected;
    private boolean editing;

    public PatientManagementView(PacienteService service) {
        this.service = service;
    }

    public Node getRoot() {
        // Inicializa la configuración de la tabla
        configureTable();
        
        // Restricciones de formato sobre DNI y teléfono
        Ui.digitsOnly(dniField, 8);
        Ui.phoneOnly(phoneField);
        
        // Listener de cobertura para habilitar o deshabilitar dinámicamente el campo de número de afiliado
        coverageCombo.valueProperty().addListener((obs, old, value) -> updateAffiliateState(editing));
        
        Button searchButton = Ui.secondaryButton("Buscar");
        searchButton.setOnAction(event -> load());
        Button newButton = Ui.secondaryButton("Nuevo");
        newButton.setOnAction(event -> startCreate());
        editButton = Ui.secondaryButton("Modificar");
        editButton.setOnAction(event -> startEdit());
        saveButton = Ui.primaryButton("Guardar");
        saveButton.setOnAction(event -> save());
        deleteButton = Ui.dangerButton("Eliminar");
        deleteButton.setOnAction(event -> delete());

        HBox filters = Ui.actions(searchField, searchButton);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        Ui.compactTable(table, 14);
        
        // Tarjeta izquierda con la grilla general de pacientes
        VBox tableCard = Ui.card(Ui.sectionTitle("Pacientes registrados"), filters, table);
        tableCard.setMinWidth(560);

        GridPane grid = formGrid();
        
        // Tarjeta derecha con el formulario y botones de acción
        VBox form = Ui.card(Ui.sectionTitle("Datos del paciente"), grid, patientActions(newButton, editButton, saveButton, deleteButton));
        form.setMinWidth(390);
        form.setPrefWidth(430);
        
        // Contenedor principal que ajusta los tamaños relativos
        HBox body = new HBox(18, tableCard, form);
        HBox.setHgrow(tableCard, Priority.ALWAYS);
        load();
        clearForm();
        return Ui.page(Ui.pageTitle("Gestion de pacientes"), body);
    }

    private void configureTable() {
        table.getColumns().setAll(
                Ui.column("ID", Paciente::getPacienteId, 70),
                Ui.column("DNI", Paciente::getDni, 100),
                Ui.column("Paciente", Paciente::getNombreCompleto, 210),
                Ui.column("Cobertura", p -> p.getTipoCobertura().getDisplayName(), 130),
                Ui.column("Telefono", Paciente::getTelefono, 130)
        );
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> fillForm(value));
    }

    // Grid que organiza los campos del formulario en dos columnas ordenadas
    private GridPane formGrid() {
        genderCombo.setMaxWidth(Double.MAX_VALUE);
        coverageCombo.setMaxWidth(Double.MAX_VALUE);
        birthDatePicker.setMaxWidth(Double.MAX_VALUE);
        Ui.configureBirthDatePicker(birthDatePicker, LocalDate.of(1900, 1, 1), LocalDate.now());
        
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
        grid.add(Ui.formRow("Cobertura", coverageCombo), 0, 4);
        grid.add(Ui.formRow("Afiliado", affiliateField), 1, 4);
        return grid;
    }

    private VBox patientActions(Button newButton, Button editButton, Button saveButton, Button deleteButton) {
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

    private void load() {
        data.setAll(service.findAll(searchField.getText()));
    }

    private void save() {
        try {
            Paciente paciente = selected == null ? new Paciente() : selected;
            paciente.setDni(dniField.getText());
            paciente.setNombre(nameField.getText());
            paciente.setApellido(lastNameField.getText());
            paciente.setGenero(genderCombo.getValue());
            paciente.setFechaNacimiento(Ui.datePickerValue(birthDatePicker, "fecha de nacimiento"));
            paciente.setDireccion(addressField.getText());
            paciente.setTelefono(phoneField.getText());
            paciente.setEmail(emailField.getText());
            paciente.setTipoCobertura(coverageCombo.getValue());
            paciente.setNumeroAfiliado(affiliateField.getText());
            service.save(paciente);
            Dialogs.info("Paciente", "Datos guardados correctamente.");
            clearForm();
            load();
        } catch (RuntimeException ex) {
            Dialogs.error("Paciente", ex.getMessage());
        }
    }

    private void delete() {
        try {
            if (selected != null && Dialogs.confirm("Paciente", "Desea eliminar el paciente seleccionado?")) {
                service.delete(selected);
                clearForm();
                load();
            }
        } catch (RuntimeException ex) {
            Dialogs.error("Paciente", ex.getMessage());
        }
    }

    private void fillForm(Paciente paciente) {
        selected = paciente;
        if (paciente == null) {
            clearFields();
            setEditing(false);
            return;
        }
        dniField.setText(paciente.getDni());
        nameField.setText(paciente.getNombre());
        lastNameField.setText(paciente.getApellido());
        genderCombo.setValue(paciente.getGenero());
        birthDatePicker.setValue(paciente.getFechaNacimiento());
        addressField.setText(paciente.getDireccion());
        phoneField.setText(paciente.getTelefono());
        emailField.setText(paciente.getEmail());
        coverageCombo.setValue(paciente.getTipoCobertura());
        affiliateField.setText(paciente.getNumeroAfiliado());
        // Seleccionar un paciente solo muestra datos. Para evitar cambios accidentales,
        // los campos se habilitan recien con el boton Modificar.
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
        addressField.clear();
        phoneField.clear();
        emailField.clear();
        coverageCombo.setValue(TipoCobertura.PARTICULAR);
        affiliateField.clear();
        updateAffiliateState(false);
    }

    private void startCreate() {
        selected = null;
        table.getSelectionModel().clearSelection();
        clearFields();
        setEditing(true);
    }

    private void startEdit() {
        if (selected == null) {
            Dialogs.info("Paciente", "Seleccione un paciente para modificar.");
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
        // Modo lectura por defecto; Nuevo y Modificar son los unicos caminos de edicion.
        Control[] controls = {
                dniField, nameField, lastNameField, genderCombo, birthDatePicker,
                addressField, phoneField, emailField, coverageCombo
        };
        for (Control control : controls) {
            control.setDisable(!editable);
        }
        updateAffiliateState(editable);
    }

    // Habilita el campo de afiliado solo si la cobertura seleccionada es 'OBRA_SOCIAL' y el formulario está en modo edición
    private void updateAffiliateState(boolean editable) {
        boolean requiresAffiliate = coverageCombo.getValue() == TipoCobertura.OBRA_SOCIAL;
        affiliateField.setPromptText(requiresAffiliate ? "Numero afiliado" : "No requiere afiliado");
        affiliateField.setDisable(!editable || !requiresAffiliate);
        if (!requiresAffiliate) {
            affiliateField.clear();
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
    }
}
