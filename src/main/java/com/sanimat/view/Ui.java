package com.sanimat.view;

import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import com.sanimat.util.ValidationException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.function.Function;

/**
 * Fabrica de componentes visuales reutilizables.
 * Mantiene estilos, botones, tablas y DatePicker consistentes entre pantallas.
 */
public final class Ui {
    private static final LocalDate MIN_DOCTOR_BIRTH_DATE = LocalDate.of(1940, 1, 1);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private Ui() {
    }

    public static Label pageTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("page-title");
        return label;
    }

    public static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    public static Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }

    public static TextField textField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    public static TextArea textArea(String prompt) {
        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setWrapText(true);
        area.setPrefRowCount(4);
        area.setMaxWidth(Double.MAX_VALUE);
        return area;
    }

    public static Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("primary-button");
        configureActionButton(button);
        return button;
    }

    public static Button secondaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("secondary-button");
        configureActionButton(button);
        return button;
    }

    public static Button dangerButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("danger-button");
        configureActionButton(button);
        return button;
    }

    public static VBox formRow(String label, Control control) {
        VBox box = new VBox(6, fieldLabel(label), control);
        VBox.setVgrow(control, Priority.NEVER);
        return box;
    }

    public static VBox card(Node... children) {
        VBox box = new VBox(14, children);
        box.getStyleClass().add("content-card");
        return box;
    }

    public static HBox actions(Node... children) {
        HBox box = new HBox(10, children);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    public static VBox actionStack(Node... children) {
        VBox box = new VBox(10, children);
        for (Node child : children) {
            if (child instanceof Button button) {
                button.setMaxWidth(Double.MAX_VALUE);
            }
        }
        return box;
    }

    public static VBox splitActions(Button left, Button right, Button danger) {
        HBox mainActions = new HBox(10, left, right);
        mainActions.setAlignment(Pos.CENTER_LEFT);
        for (Button button : new Button[]{left, right}) {
            button.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(button, Priority.ALWAYS);
        }
        danger.setMaxWidth(Double.MAX_VALUE);
        return new VBox(10, mainActions, danger);
    }

    public static void compactTable(TableView<?> table, int visibleRows) {
        table.setFixedCellSize(28);
        table.setPrefHeight(36 + (visibleRows * 28));
        table.setMinHeight(160);
        table.setMaxHeight(Region.USE_PREF_SIZE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    public static void digitsOnly(TextField field, int maxLength) {
        field.setTextFormatter(new TextFormatter<>(change -> {
            String next = change.getControlNewText();
            return next.matches("\\d*") && next.length() <= maxLength ? change : null;
        }));
    }

    public static void phoneOnly(TextField field) {
        field.setTextFormatter(new TextFormatter<>(change -> {
            String next = change.getControlNewText();
            return next.matches("[0-9+()\\s-]*") && next.length() <= 30 ? change : null;
        }));
    }

    public static void decimalOnly(TextField field) {
        field.setTextFormatter(new TextFormatter<>(change -> {
            String next = change.getControlNewText();
            return next.matches("\\d{0,10}([,.]\\d{0,2})?") ? change : null;
        }));
    }

    public static void configureDatePicker(DatePicker picker) {
        picker.setPromptText("dd/MM/aaaa");
        picker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : date.format(DATE_FORMAT);
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.isBlank()) {
                    return null;
                }
                return LocalDate.parse(text.trim(), DATE_FORMAT);
            }
        });
        picker.getEditor().setTextFormatter(new TextFormatter<>(change -> {
            if (!change.isContentChange()) {
                return change;
            }
            // Permite escribir solo digitos y arma automaticamente dd/MM/aaaa.
            String digits = change.getControlNewText().replaceAll("\\D", "");
            if (digits.length() > 8) {
                return null;
            }
            String formatted = formatDateDigits(digits);
            change.setRange(0, change.getControlText().length());
            change.setText(formatted);
            change.setCaretPosition(formatted.length());
            change.setAnchor(formatted.length());
            return change;
        }));
    }

    public static void configureBirthDatePicker(DatePicker picker, LocalDate latestAllowedDate) {
        configureBirthDatePicker(picker, MIN_DOCTOR_BIRTH_DATE, latestAllowedDate);
    }

    public static void configureBirthDatePicker(DatePicker picker, LocalDate earliestAllowedDate, LocalDate latestAllowedDate) {
        configureDatePicker(picker);
        picker.setDayCellFactory(view -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(earliestAllowedDate) || date.isAfter(latestAllowedDate));
            }
        });
    }

    public static LocalDate datePickerValue(DatePicker picker, String field) {
        String text = picker.getEditor().getText();
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.trim();
        if (!trimmed.matches("\\d{2}/\\d{2}/\\d{4}")) {
            throw new ValidationException("El campo " + field + " debe tener formato dd/mm/aaaa.");
        }
        try {
            LocalDate date = LocalDate.parse(trimmed, DATE_FORMAT);
            picker.setValue(date);
            return date;
        } catch (DateTimeParseException ex) {
            throw new ValidationException("El campo " + field + " contiene una fecha invalida.");
        }
    }

    private static String formatDateDigits(String digits) {
        StringBuilder formatted = new StringBuilder(digits);
        if (formatted.length() > 4) {
            formatted.insert(4, '/');
        }
        if (formatted.length() > 2) {
            formatted.insert(2, '/');
        }
        return formatted.toString();
    }

    public static <S, T> TableColumn<S, T> column(String title, Function<S, T> extractor, double width) {
        TableColumn<S, T> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleObjectProperty<>(extractor.apply(data.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    public static <S> TableColumn<S, String> dateColumn(String title, Function<S, LocalDate> extractor, double width) {
        // Evita mostrar fechas en formato ISO de PostgreSQL/Java, por ejemplo 2026-06-07.
        return column(title, value -> {
            LocalDate date = extractor.apply(value);
            return date == null ? "" : date.format(DATE_FORMAT);
        }, width);
    }

    public static VBox page(Node title, Node body) {
        VBox page = new VBox(18, title, body);
        page.setPadding(new Insets(22));
        VBox.setVgrow(body, Priority.ALWAYS);
        page.setMinWidth(720);
        return page;
    }

    private static void configureActionButton(Button button) {
        button.setMinWidth(105);
        button.setWrapText(true);
    }
}
