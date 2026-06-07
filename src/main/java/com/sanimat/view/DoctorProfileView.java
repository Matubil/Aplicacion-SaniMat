package com.sanimat.view;

import com.sanimat.model.Medico;
import com.sanimat.model.Usuario;
import com.sanimat.service.MedicoService;
import com.sanimat.util.Dialogs;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

/**
   Pantalla donde el medico edita datos basicos de su propio perfil.
 */
public class DoctorProfileView {
    private final MedicoService medicoService;
    private final Usuario usuario;
    private final Runnable afterSave;

    private final TextField dniField = readonlyField();
    private final TextField nameField = Ui.textField("Nombre");
    private final TextField lastNameField = Ui.textField("Apellido");
    private final ComboBox<String> genderCombo = new ComboBox<>(FXCollections.observableArrayList("Masculino", "Femenino", "Otro"));
    private final DatePicker birthDatePicker = new DatePicker();
    private final TextField addressField = Ui.textField("Dirección");
    private final TextField phoneField = Ui.textField("Teléfono");
    private final TextField emailField = Ui.textField("Email");
    private final TextField licenseField = readonlyField();
    private final TextField specialtyField = readonlyField();
    private final TextField userField = Ui.textField("Usuario");
    private final TextField passwordField = Ui.textField("Contraseña");

    private Medico medico;

    public DoctorProfileView(MedicoService medicoService, Usuario usuario, Runnable afterSave) {
        this.medicoService = medicoService;
        this.usuario = usuario;
        this.afterSave = afterSave;
    }

    public Node getRoot() {
        genderCombo.setMaxWidth(Double.MAX_VALUE);
        birthDatePicker.setMaxWidth(Double.MAX_VALUE);
        Ui.configureBirthDatePicker(birthDatePicker, LocalDate.now().minusYears(18));
        Ui.phoneOnly(phoneField);

        Button reloadButton = Ui.secondaryButton("Recargar");
        reloadButton.setOnAction(event -> loadProfile());
        Button saveButton = Ui.primaryButton("Guardar cambios");
        saveButton.setOnAction(event -> save());

        // Tarjeta izquierda para datos personales editables (ancho elástico)
        VBox personalCard = Ui.card(Ui.sectionTitle("Datos personales"), personalGrid());
        personalCard.setMinWidth(520);
        
        // Tarjeta derecha para datos profesionales y de cuenta (ancho fijo/preferido)
        VBox professionalCard = Ui.card(Ui.sectionTitle("Datos profesionales"), professionalGrid(), Ui.actions(saveButton, reloadButton));
        professionalCard.setMinWidth(360);
        professionalCard.setPrefWidth(420);

        // Contenedor horizontal de dos columnas
        HBox body = new HBox(18, personalCard, professionalCard);
        HBox.setHgrow(personalCard, javafx.scene.layout.Priority.ALWAYS);
        loadProfile();
        return Ui.page(Ui.pageTitle("Editar Perfil"), body);
    }

    // Grid de dos columnas para agrupar campos personales
    private GridPane personalGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.add(Ui.formRow("DNI", dniField), 0, 0);
        grid.add(Ui.formRow("Nombre", nameField), 1, 0);
        grid.add(Ui.formRow("Apellido", lastNameField), 0, 1);
        grid.add(Ui.formRow("Género", genderCombo), 1, 1);
        grid.add(Ui.formRow("Fecha nacimiento", birthDatePicker), 0, 2);
        grid.add(Ui.formRow("Teléfono", phoneField), 1, 2);
        grid.add(Ui.formRow("Email", emailField), 0, 3);
        grid.add(Ui.formRow("Dirección", addressField), 1, 3);
        return grid;
    }

    // Grid vertical de una sola columna para credenciales y datos no editables (Matrícula, Especialidad)
    private GridPane professionalGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.add(Ui.formRow("Matrícula", licenseField), 0, 0);
        grid.add(Ui.formRow("Especialidad", specialtyField), 0, 1);
        grid.add(Ui.formRow("Usuario", userField), 0, 2);
        grid.add(Ui.formRow("Contraseña", passwordField), 0, 3);
        return grid;
    }

    private void loadProfile() {
        if (usuario.getMedicoId() == null) {
            Dialogs.error("Perfil médico", "El usuario actual no tiene un médico asociado.");
            return;
        }
        medico = medicoService.findById(usuario.getMedicoId());
        dniField.setText(medico.getDni());
        nameField.setText(medico.getNombre());
        lastNameField.setText(medico.getApellido());
        genderCombo.setValue(medico.getGenero());
        birthDatePicker.setValue(medico.getFechaNacimiento());
        addressField.setText(medico.getDireccion());
        phoneField.setText(medico.getTelefono());
        emailField.setText(medico.getEmail());
        licenseField.setText(String.valueOf(medico.getMatricula()));
        specialtyField.setText(medico.getEspecialidadNombre());
        userField.setText(medico.getUsuario());
        passwordField.setText(medico.getContrasenia());
    }

    private void save() {
        if (medico == null) {
            return;
        }
        try {
            medico.setNombre(nameField.getText());
            medico.setApellido(lastNameField.getText());
            medico.setGenero(genderCombo.getValue());
            medico.setFechaNacimiento(Ui.datePickerValue(birthDatePicker, "fecha de nacimiento"));
            medico.setDireccion(addressField.getText());
            medico.setTelefono(phoneField.getText());
            medico.setEmail(emailField.getText());
            medico.setUsuario(userField.getText());
            medico.setContrasenia(passwordField.getText());
            medicoService.save(medico);
            usuario.setUsername(medico.getUsuario());
            usuario.setNombreCompleto(medico.getNombreCompleto());
            if (afterSave != null) {
                afterSave.run();
            }
            Dialogs.info("Perfil médico", "Perfil actualizado correctamente.");
        } catch (RuntimeException ex) {
            Dialogs.error("Perfil médico", ex.getMessage());
        }
    }

    private static TextField readonlyField() {
        TextField field = Ui.textField("");
        field.setEditable(false);
        return field;
    }
}
