package com.sanimat.view;

import com.sanimat.model.Especialidad;
import com.sanimat.service.EspecialidadService;
import com.sanimat.util.Dialogs;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
   Pantalla de gestion de especialidades para secretaria.
 */
public class SpecialtyManagementView {
    private final EspecialidadService service;
    private final ObservableList<Especialidad> data = FXCollections.observableArrayList();
    private final TableView<Especialidad> table = new TableView<>(data);
    private final TextField searchField = Ui.textField("Buscar especialidad");
    private final TextField nameField = Ui.textField("Nombre");
    private final TextArea descriptionArea = Ui.textArea("Descripcion");
    private Button editButton;
    private Button saveButton;
    private Button deleteButton;
    private Especialidad selected;
    private boolean editing;

    public SpecialtyManagementView(EspecialidadService service) {
        this.service = service;
    }

    public Node getRoot() {
        // Inicializa y enlaza la tabla de especialidades
        configureTable();
        
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

        // Barra de búsqueda con alineación horizontal
        HBox filters = Ui.actions(searchField, searchButton);
        HBox.setHgrow(searchField, Priority.ALWAYS); // Campo de búsqueda crece horizontalmente
        
        // Tarjeta derecha con el formulario de especialidades
        VBox form = Ui.card(
                Ui.sectionTitle("Datos de especialidad"),
                Ui.formRow("Nombre", nameField),
                Ui.formRow("Descripcion", descriptionArea),
                specialtyActions(newButton, editButton, saveButton, deleteButton)
        );
        Ui.compactTable(table, 12);
        
        // Tarjeta izquierda con la grilla de especialidades registradas
        VBox tableCard = Ui.card(Ui.sectionTitle("Especialidades"), filters, table);
        tableCard.setMinWidth(560);
        
        // Contenedor principal que organiza la vista en dos paneles
        HBox body = new HBox(18, tableCard, form);
        HBox.setHgrow(tableCard, Priority.ALWAYS);
        form.setMinWidth(330);
        form.setPrefWidth(360);
        
        load();
        clearForm();
        return Ui.page(Ui.pageTitle("Gestion de especialidades"), body);
    }

    private void configureTable() {
        table.getColumns().setAll(
                Ui.column("ID", Especialidad::getId, 70),
                Ui.column("Nombre", Especialidad::getNombre, 220),
                Ui.column("Descripcion", Especialidad::getDescripcion, 360)
        );
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> fillForm(value));
    }

    private void load() {
        data.setAll(service.findAll(searchField.getText()));
    }

    private VBox specialtyActions(Button newButton, Button editButton, Button saveButton, Button deleteButton) {
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

    private void save() {
        try {
            Especialidad especialidad = selected == null ? new Especialidad() : selected;
            especialidad.setNombre(nameField.getText());
            especialidad.setDescripcion(descriptionArea.getText());
            service.save(especialidad);
            Dialogs.info("Especialidad", "Datos guardados correctamente.");
            clearForm();
            load();
        } catch (RuntimeException ex) {
            Dialogs.error("Especialidad", ex.getMessage());
        }
    }

    private void delete() {
        try {
            if (selected != null && Dialogs.confirm("Especialidad", "Desea eliminar la especialidad seleccionada?")) {
                service.delete(selected);
                clearForm();
                load();
            }
        } catch (RuntimeException ex) {
            Dialogs.error("Especialidad", ex.getMessage());
        }
    }

    private void fillForm(Especialidad especialidad) {
        selected = especialidad;
        if (especialidad == null) {
            clearFields();
            setEditing(false);
            return;
        }
        nameField.setText(especialidad.getNombre());
        descriptionArea.setText(especialidad.getDescripcion());
        setEditing(false);
    }

    private void clearForm() {
        selected = null;
        table.getSelectionModel().clearSelection();
        clearFields();
        setEditing(false);
    }

    private void clearFields() {
        nameField.clear();
        descriptionArea.clear();
    }

    private void startCreate() {
        selected = null;
        table.getSelectionModel().clearSelection();
        clearFields();
        setEditing(true);
    }

    private void startEdit() {
        if (selected == null) {
            Dialogs.info("Especialidad", "Seleccione una especialidad para modificar.");
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
        Control[] controls = {nameField, descriptionArea};
        for (Control control : controls) {
            control.setDisable(!editable);
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
