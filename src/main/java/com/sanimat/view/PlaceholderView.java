package com.sanimat.view;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Vista simple para funcionalidades futuras que quedan fuera del prototipo.
 */
public class PlaceholderView {
    private final String title;
    private final String message;

    public PlaceholderView(String title, String message) {
        this.title = title;
        this.message = message;
    }

    public Node getRoot() {
        Label text = Ui.sectionTitle(message);
        VBox card = Ui.card(text);
        return Ui.page(Ui.pageTitle(title), card);
    }
}
