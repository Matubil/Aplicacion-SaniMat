package com.sanimat.view;

import com.sanimat.model.HistoriaClinica;
import com.sanimat.model.Paciente;
import com.sanimat.model.Usuario;
import com.sanimat.service.HistoriaClinicaService;
import com.sanimat.service.PacienteService;
import com.sanimat.util.Dialogs;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;

/**
   Pantalla de historias clinicas.
   Permite buscar pacientes y cargar o editar el texto clinico asociado.
 */
public class ClinicalHistoryView {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter STRICT_DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private final HistoriaClinicaService historyService;
    private final PacienteService pacienteService;
    private final Usuario usuario;

    private final ObservableList<Paciente> patientData = FXCollections.observableArrayList();
    private final TableView<Paciente> patientTable = new TableView<>(patientData);
    private final TextField filterField = Ui.textField("DNI o apellido del paciente");
    private final Label patientNameLabel = Ui.sectionTitle("Seleccione un paciente");
    private final Label patientDniLabel = valueLabel("-");
    private final Label patientCoverageLabel = valueLabel("-");
    private final Label patientBirthDateLabel = valueLabel("-");
    private final Label patientContactLabel = valueLabel("-");
    private final Label patientAddressLabel = valueLabel("-");
    private final Label entryDateLabel = valueLabel("");
    private final TextArea historyArea = Ui.textArea("Historial clínico");

    private Paciente selectedPatient;
    private HistoriaClinica selectedHistory;
    private LocalDateTime newEntryDate;
    private Button createHistoryButton;
    private Button editHistoryButton;
    private Button saveHistoryButton;
    private Button cancelEditButton;
    private boolean editingHistory;
    private boolean creatingHistoryEntry;

    public ClinicalHistoryView(HistoriaClinicaService historyService, PacienteService pacienteService, Usuario usuario) {
        this.historyService = historyService;
        this.pacienteService = pacienteService;
        this.usuario = usuario;
    }

    public Node getRoot() {
        // Inicializa la configuración de columnas y selección de la tabla de pacientes
        configurePatientTable();
        
        // Configura el área de texto del historial clínico: no editable por defecto y con altura preferida
        historyArea.setEditable(false);
        historyArea.setPrefRowCount(16);
        hideEntryDate();

        Button searchButton = Ui.secondaryButton("Buscar");
        searchButton.setOnAction(event -> searchPatient());
        filterField.setOnAction(event -> searchPatient());
        Button reportButton = Ui.secondaryButton("Generar Reporte");
        reportButton.getStyleClass().add("future-action-button");
        reportButton.setOnAction(event -> showCandidateFeature("Reporte clínico"));
        Button prescriptionButton = Ui.secondaryButton("Generar Receta");
        prescriptionButton.getStyleClass().add("future-action-button");
        prescriptionButton.setOnAction(event -> showCandidateFeature("Receta médica"));
        createHistoryButton = Ui.secondaryButton("Nueva entrada");
        createHistoryButton.setOnAction(event -> startCreateHistory());
        editHistoryButton = Ui.secondaryButton("Modificar historial");
        editHistoryButton.setOnAction(event -> startEditHistory());
        saveHistoryButton = Ui.primaryButton("Guardar");
        saveHistoryButton.setOnAction(event -> saveHistory());
        cancelEditButton = Ui.secondaryButton("Cancelar");
        cancelEditButton.setOnAction(event -> cancelHistoryEdit());

        // Contenedor horizontal para la barra de búsqueda. El campo de texto se expande para ocupar todo el espacio disponible.
        HBox filter = Ui.actions(filterField, searchButton);
        HBox.setHgrow(filterField, Priority.ALWAYS);

        // Tarjeta lateral izquierda que contiene el filtro de búsqueda y la tabla de selección de pacientes
        VBox patientCard = Ui.card(Ui.sectionTitle("Pacientes"), filter, patientTable);
        patientCard.setMinWidth(360);
        patientCard.setPrefWidth(450);
        Ui.compactTable(patientTable, 12);
        
        // Tarjeta resumen del paciente seleccionado
        VBox summaryCard = Ui.card(Ui.sectionTitle("Paciente seleccionado"), patientSummary());
        
        // Tarjeta para el visor/editor del historial clínico
        VBox historyCard = Ui.card(Ui.sectionTitle("Historial Clínico"), entryDateLabel, historyArea);
        
        // Agrupación vertical de botones de acciones clínicas organizados por filas de HBox
        VBox actions = new VBox(10,
                historyActions(createHistoryButton, editHistoryButton),
                historyActions(saveHistoryButton, cancelEditButton),
                historyActions(reportButton, prescriptionButton)
        );

        // Panel contenedor derecho para el resumen del paciente, el área del historial y las acciones
        VBox historyPanel = new VBox(16, summaryCard, historyCard, actions);
        VBox.setVgrow(historyCard, Priority.ALWAYS); // Hace que la tarjeta del historial crezca verticalmente para llenar el espacio
        VBox.setVgrow(historyArea, Priority.ALWAYS); // Hace que el área de texto crezca para llenar el espacio dentro de la tarjeta
        
        // Contenedor principal que organiza la vista en dos columnas (Pacientes a la izquierda, Historial a la derecha)
        HBox body = new HBox(18, patientCard, historyPanel);
        HBox.setHgrow(historyPanel, Priority.ALWAYS);
        loadInitialPatients();
        return Ui.page(Ui.pageTitle("Historial Clínico"), body);
    }

    // Configura la tabla de pacientes con sus columnas específicas y añade el listener de selección
    private void configurePatientTable() {
        patientTable.getColumns().setAll(
                Ui.column("Paciente", Paciente::getNombreCompleto, 210),
                Ui.column("DNI", Paciente::getDni, 105),
                Ui.column("Cobertura", p -> p.getTipoCobertura().getDisplayName(), 120),
                Ui.column("Telefono", Paciente::getTelefono, 130)
        );
        patientTable.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> {
            if (value != null) {
                showPatient(value);
            }
        });
    }

    // Construye un contenedor vertical con el resumen de datos personales del paciente (DNI, cobertura, nacimiento, contacto, dirección)
    private VBox patientSummary() {
        HBox firstRow = new HBox(14,
                summaryItem("DNI", patientDniLabel),
                summaryItem("Cobertura", patientCoverageLabel),
                summaryItem("Nacimiento", patientBirthDateLabel)
        );
        HBox secondRow = new HBox(14,
                summaryItem("Contacto", patientContactLabel),
                summaryItem("Dirección", patientAddressLabel)
        );
        return new VBox(12, patientNameLabel, firstRow, secondRow);
    }

    // Helper para crear ítems individuales de información del paciente con etiqueta arriba y valor abajo
    private VBox summaryItem(String label, Label value) {
        VBox box = new VBox(4, Ui.fieldLabel(label), value);
        HBox.setHgrow(box, Priority.ALWAYS); // Permite al elemento expandirse proporcionalmente
        return box;
    }

    // Configura botones para que ocupen todo el ancho horizontal disponible dentro de una fila de acciones
    private HBox historyActions(Button... buttons) {
        HBox box = Ui.actions(buttons);
        for (Button button : buttons) {
            button.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(button, Priority.ALWAYS);
        }
        return box;
    }

    private void searchPatient() {
        List<Paciente> patients = pacienteService.findAll(filterField.getText());
        patientData.setAll(patients);
        if (patients.isEmpty()) {
            clearPatient();
            Dialogs.info("Historial clínico", "No se encontró un paciente con ese filtro.");
            return;
        }
        patientTable.getSelectionModel().selectFirst();
    }

    private void loadInitialPatients() {
        List<Paciente> patients = pacienteService.findAll(null);
        patientData.setAll(patients);
        if (!patients.isEmpty()) {
            patientTable.getSelectionModel().selectFirst();
        } else {
            clearPatient();
        }
    }

    private void showPatient(Paciente paciente) {
        selectedPatient = paciente;
        patientNameLabel.setText(paciente.getNombreCompleto());
        patientDniLabel.setText(emptyToDash(paciente.getDni()));
        patientCoverageLabel.setText(paciente.getTipoCobertura().getDisplayName());
        patientBirthDateLabel.setText(paciente.getFechaNacimiento() == null ? "-" : paciente.getFechaNacimiento().format(DATE));
        patientContactLabel.setText(composeContact(paciente));
        patientAddressLabel.setText(emptyToDash(paciente.getDireccion()));
        loadHistory();
    }

    private void loadHistory() {
        if (selectedPatient == null) {
            selectedHistory = null;
            historyArea.clear();
            setHistoryEditing(false);
            return;
        }
        List<HistoriaClinica> histories = historyService.search(selectedPatient.getPacienteId(), null, usuario);
        if (histories.isEmpty()) {
            selectedHistory = null;
            historyArea.setText("No hay registros clínicos cargados para este paciente.");
            setHistoryEditing(false);
            return;
        }
        selectedHistory = histories.get(0);
        StringBuilder builder = new StringBuilder();
        for (HistoriaClinica history : histories) {
            builder.append(formatHistoryForDisplay(history))
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());
        }
        historyArea.setText(builder.toString().trim());
        setHistoryEditing(false);
    }

    private void clearPatient() {
        selectedPatient = null;
        selectedHistory = null;
        patientTable.getSelectionModel().clearSelection();
        patientNameLabel.setText("Seleccione un paciente");
        patientDniLabel.setText("-");
        patientCoverageLabel.setText("-");
        patientBirthDateLabel.setText("-");
        patientContactLabel.setText("-");
        patientAddressLabel.setText("-");
        historyArea.clear();
        setHistoryEditing(false);
    }

    private void startCreateHistory() {
        if (selectedPatient == null) {
            Dialogs.info("Historial clínico", "Seleccione un paciente para crear una entrada clínica.");
            return;
        }
        creatingHistoryEntry = true;
        newEntryDate = LocalDateTime.now();
        entryDateLabel.setText("Fecha de nueva entrada: " + newEntryDate.format(DATE));
        entryDateLabel.setVisible(true);
        entryDateLabel.setManaged(true);
        historyArea.clear();
        historyArea.setPromptText("Descripcion de la nueva entrada clinica");
        setHistoryEditing(true);
    }

    private void startEditHistory() {
        if (selectedHistory == null) {
            Dialogs.info("Historial clínico", "Seleccione un paciente con historial clínico para modificar.");
            return;
        }
        creatingHistoryEntry = false;
        hideEntryDate();
        historyArea.setPromptText("Historial clinico");
        historyArea.setText(selectedHistory.getDescripcion() == null ? "" : selectedHistory.getDescripcion());
        setHistoryEditing(true);
    }

    private void saveHistory() {
        if (selectedPatient == null) {
            Dialogs.info("Historial clínico", "Seleccione un paciente para guardar el historial.");
            return;
        }
        try {
            HistoriaClinica historia = selectedHistory == null ? new HistoriaClinica() : selectedHistory;
            historia.setPacienteId(selectedPatient.getPacienteId());
            if (historia.getFechaAtencion() == null) {
                historia.setFechaAtencion(newEntryDate == null ? LocalDateTime.now() : newEntryDate);
            }
            if (creatingHistoryEntry) {
                String entryDescription = historyArea.getText() == null ? "" : historyArea.getText().trim();
                if (entryDescription.isBlank()) {
                    Dialogs.info("Historial clínico", "Ingrese la descripcion de la nueva entrada clinica.");
                    return;
                }
                historia.setDescripcion(appendEntry(historia.getDescripcion(), newEntryDate, entryDescription));
            } else {
                if (!hasValidHistoryDates(historyArea.getText())) {
                    return;
                }
                historia.setDescripcion(historyArea.getText());
            }
            historyService.save(historia);
            Dialogs.info("Historial clínico", "Historial clínico guardado correctamente.");
            loadHistory();
        } catch (RuntimeException ex) {
            Dialogs.error("Historial clínico", ex.getMessage());
        }
    }

    private void cancelHistoryEdit() {
        loadHistory();
    }

    private void setHistoryEditing(boolean editing) {
        editingHistory = editing;
        historyArea.setEditable(editing);
        if (!editing) {
            creatingHistoryEntry = false;
            newEntryDate = null;
            historyArea.setPromptText("Historial clinico");
            hideEntryDate();
        }
        refreshHistoryActionState();
    }

    private void refreshHistoryActionState() {
        boolean hasPatient = selectedPatient != null;
        boolean hasHistory = selectedHistory != null;
        if (createHistoryButton != null) {
            createHistoryButton.setDisable(!hasPatient || editingHistory);
        }
        if (editHistoryButton != null) {
            editHistoryButton.setDisable(!hasHistory || editingHistory);
        }
        if (saveHistoryButton != null) {
            saveHistoryButton.setDisable(!editingHistory);
        }
        if (cancelEditButton != null) {
            cancelEditButton.setDisable(!editingHistory);
        }
    }

    private void showCandidateFeature(String feature) {
        if (selectedPatient == null) {
            Dialogs.info(feature, "Seleccione un paciente para continuar.");
            return;
        }
        if (feature.toLowerCase().contains("receta")) {
            Dialogs.info(feature, "La generación de recetas no forma parte del alcance implementado en esta versión del prototipo. Queda contemplada como una posible ampliación futura del sistema.");
            return;
        }
        Dialogs.info(feature, feature + " queda contemplado como funcionalidad candidata del prototipo.");
    }

    private String composeContact(Paciente paciente) {
        String email = emptyToDash(paciente.getEmail());
        String phone = emptyToDash(paciente.getTelefono());
        if ("-".equals(email)) {
            return phone;
        }
        if ("-".equals(phone)) {
            return email;
        }
        return email + " | " + phone;
    }

    private String formatHistoryForDisplay(HistoriaClinica history) {
        String description = history.getDescripcion() == null ? "" : history.getDescripcion().trim();
        if (description.matches("(?s)^\\d{2}/\\d{2}/\\d{4}.*")) {
            return description;
        }
        String date = history.getFechaAtencion() == null ? "-" : history.getFechaAtencion().toLocalDate().format(DATE);
        return date + System.lineSeparator() + description;
    }

    private String appendEntry(String currentDescription, LocalDateTime entryDate, String entryDescription) {
        String current = currentDescription == null ? "" : currentDescription.trim();
        String date = (entryDate == null ? LocalDateTime.now() : entryDate).format(DATE);
        String entry = date + System.lineSeparator() + entryDescription.trim();
        return current.isBlank()
                ? entry
                : current + System.lineSeparator() + System.lineSeparator() + entry;
    }

    private boolean hasValidHistoryDates(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        String[] lines = text.split("\\R");
        for (String line : lines) {
            String firstToken = firstToken(line);
            if (!isDateLike(firstToken)) {
                continue;
            }
            if (!firstToken.matches("\\d{2}/\\d{2}/\\d{4}")) {
                Dialogs.info("Historial clínico", "Hay una fecha incompleta: " + firstToken + ". Use el formato dd/mm/aaaa.");
                return false;
            }
            try {
                LocalDate.parse(firstToken, STRICT_DATE);
            } catch (DateTimeParseException ex) {
                Dialogs.info("Historial clínico", "Hay una fecha inválida: " + firstToken + ". Revise el día, mes y año.");
                return false;
            }
        }
        return true;
    }

    private String firstToken(String line) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        int spaceIndex = trimmed.indexOf(' ');
        return spaceIndex < 0 ? trimmed : trimmed.substring(0, spaceIndex);
    }

    private boolean isDateLike(String value) {
        return value != null && value.matches("\\d{1,2}/\\d{0,2}(/\\d{0,4})?");
    }

    private void hideEntryDate() {
        entryDateLabel.setText("");
        entryDateLabel.setVisible(false);
        entryDateLabel.setManaged(false);
    }

    private String emptyToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private Label valueLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("summary-value");
        label.setWrapText(true);
        return label;
    }
}
