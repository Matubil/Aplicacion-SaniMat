package com.sanimat.view;

import com.sanimat.model.MedioPago;
import com.sanimat.model.Pago;
import com.sanimat.model.Turno;
import com.sanimat.service.PagoService;
import com.sanimat.util.Dialogs;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pantalla de pagos.
 * Lista turnos pendientes, calcula monto esperado y registra pagos completos.
 */
public class PaymentView {
    private final PagoService service;
    private final ObservableList<Pago> data = FXCollections.observableArrayList();
    private final ObservableList<Turno> pendingData = FXCollections.observableArrayList();
    private final TableView<Pago> table = new TableView<>(data);
    private final TableView<Turno> pendingTable = new TableView<>(pendingData);
    private final ComboBox<Turno> appointmentCombo = new ComboBox<>();
    private final TextField amountField = Ui.textField("Monto");
    private final ComboBox<MedioPago> paymentMethodCombo = new ComboBox<>(FXCollections.observableArrayList(
            MedioPago.EFECTIVO,
            MedioPago.TARJETA,
            MedioPago.TRANSFERENCIA
    ));
    private final DatePicker paymentDatePicker = new DatePicker(LocalDate.now());
    private final TextArea summaryArea = Ui.textArea("Seleccione un turno pendiente.");

    public PaymentView(PagoService service) {
        this.service = service;
    }

    public Node getRoot() {
        configureTable();
        configurePendingTable();
        configureAppointmentCombo();
        appointmentCombo.setMaxWidth(Double.MAX_VALUE);
        appointmentCombo.valueProperty().addListener((obs, old, value) -> selectAppointment(value));
        paymentMethodCombo.setMaxWidth(Double.MAX_VALUE);
        paymentMethodCombo.valueProperty().addListener((obs, old, value) -> updatePaymentSummary());
        paymentDatePicker.setMaxWidth(Double.MAX_VALUE);
        Ui.configureDatePicker(paymentDatePicker);
        summaryArea.setEditable(false);
        summaryArea.setFocusTraversable(false);
        summaryArea.setPrefRowCount(4);
        summaryArea.setMaxHeight(118);
        summaryArea.getStyleClass().add("payment-summary");
        Ui.decimalOnly(amountField);

        Button refreshButton = Ui.secondaryButton("Actualizar");
        refreshButton.setOnAction(event -> load());
        Button saveButton = Ui.primaryButton("Registrar pago");
        saveButton.setOnAction(event -> save());

        Ui.compactTable(pendingTable, 7);
        Ui.compactTable(table, 7);
        VBox pendingCard = Ui.card(Ui.sectionTitle("Turnos pendientes de pago"), refreshButton, pendingTable);
        VBox tableCard = Ui.card(Ui.sectionTitle("Pagos registrados"), table);
        VBox tables = new VBox(18, pendingCard, tableCard);
        VBox form = Ui.card(Ui.sectionTitle("Registro de pago"), summaryArea, formGrid(), Ui.actionStack(saveButton));
        form.setMinWidth(380);
        form.setPrefWidth(420);
        HBox body = new HBox(18, tables, form);
        HBox.setHgrow(tables, Priority.ALWAYS);
        clearForm();
        load();
        return Ui.page(Ui.pageTitle("Registro de pagos"), body);
    }

    private void configureTable() {
        table.getColumns().setAll(
                Ui.column("Turno", Pago::getTurnoId, 80),
                Ui.column("Paciente", Pago::getPacienteNombre, 220),
                Ui.column("Monto", Pago::getMonto, 110),
                Ui.column("Medio", p -> p.getMedioPago().getDisplayName(), 150),
                Ui.dateColumn("Fecha", p -> p.getFechaPago().toLocalDate(), 120)
        );
    }

    private void configurePendingTable() {
        pendingTable.getColumns().setAll(
                Ui.column("Paciente", Turno::getPacienteNombre, 160),
                Ui.column("DNI", Turno::getPacienteDni, 100),
                Ui.column("Obra Social", Turno::getTipoCoberturaDisplay, 120),
                Ui.column("Medico", Turno::getMedicoNombre, 160),
                Ui.dateColumn("Fecha", t -> t.getFechaHora().toLocalDate(), 110),
                Ui.column("Horario", t -> t.getFechaHora().toLocalTime(), 90)
        );
        pendingTable.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> selectAppointment(value));
    }

    private void configureAppointmentCombo() {
        appointmentCombo.setCellFactory(combo -> appointmentCell());
        appointmentCombo.setButtonCell(appointmentCell());
    }

    private ListCell<Turno> appointmentCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Turno turno, boolean empty) {
                super.updateItem(turno, empty);
                setText(empty || turno == null ? null : turno.toString());
                setWrapText(true);
            }
        };
    }

    private GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.add(Ui.formRow("Turno sin pago", appointmentCombo), 0, 0);
        grid.add(Ui.formRow("Monto", amountField), 0, 1);
        grid.add(Ui.formRow("Medio de pago", paymentMethodCombo), 0, 2);
        grid.add(Ui.formRow("Fecha", paymentDatePicker), 0, 3);
        return grid;
    }

    private void load() {
        data.setAll(service.findAll());
        pendingData.setAll(service.findUnpaidAppointments());
        appointmentCombo.setItems(FXCollections.observableArrayList(pendingData));
    }

    private void save() {
        try {
            Turno turno = appointmentCombo.getValue();
            Pago pago = new Pago();
            pago.setTurnoId(turno == null ? 0 : turno.getId());
            pago.setMonto(new BigDecimal(amountField.getText().replace(",", ".")));
            pago.setMedioPago(paymentMethodCombo.getValue());
            pago.setFechaPago(Ui.datePickerValue(paymentDatePicker, "fecha de pago").atStartOfDay());
            service.save(pago);
            Dialogs.info("Pago", "Pago registrado correctamente.");
            clearForm();
            load();
        } catch (NumberFormatException ex) {
            Dialogs.error("Pago", "El monto debe ser numerico.");
        } catch (RuntimeException ex) {
            Dialogs.error("Pago", ex.getMessage());
        }
    }

    private void clearForm() {
        appointmentCombo.setValue(null);
        pendingTable.getSelectionModel().clearSelection();
        amountField.clear();
        paymentMethodCombo.setValue(MedioPago.EFECTIVO);
        paymentDatePicker.setValue(LocalDate.now());
        summaryArea.setText("Seleccione un turno pendiente.");
    }

    private void selectAppointment(Turno turno) {
        if (turno == null) {
            return;
        }
        appointmentCombo.setValue(turno);
        updatePaymentSummary();
    }

    private void updatePaymentSummary() {
        Turno turno = appointmentCombo.getValue();
        if (turno == null) {
            summaryArea.setText("Seleccione un turno pendiente.");
            amountField.clear();
            return;
        }
        MedioPago medioPago = paymentMethodCombo.getValue() == null ? MedioPago.EFECTIVO : paymentMethodCombo.getValue();
        BigDecimal total = service.calcularMontoEsperado(turno, medioPago);
        amountField.setText(total.toPlainString());
        summaryArea.setText("""
                Consulta base: $25.000
                Descuento obra social: %s
                Descuento efectivo: %s
                Total a abonar: $%s
                """.formatted(
                "OBRA_SOCIAL".equals(turno.getTipoCobertura()) ? "50%" : "No aplica",
                medioPago == MedioPago.EFECTIVO ? "15%" : "No aplica",
                total.toPlainString()
        ).trim());
    }
}
