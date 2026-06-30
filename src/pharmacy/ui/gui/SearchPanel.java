package pharmacy.ui.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

import java.util.function.Consumer;

// Shared search panel (DOB, last name, phone, first+last) used in Release to Patient and Patient Lookup.
public class SearchPanel {

    private final GridPane grid;

    private final TextField dobField       = searchField("MM/DD/YYYY");
    private final TextField lastNameField  = searchField("Enter last name");
    private final TextField phoneField     = searchField("(XXX) XXX-XXXX");
    private final TextField firstNameField = searchField("Enter first name");

    // Callbacks — set by the host view
    private Consumer<String> onDobSearch;
    private Consumer<String> onLastNameSearch;
    private Consumer<String> onPhoneSearch;
    private Runnable         onFirstLastSearch; // reads both lastNameField + firstNameField

    public SearchPanel() {
        grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(28, 32, 20, 32));

        ColumnConstraints col0 = new ColumnConstraints();
        col0.setHgrow(Priority.ALWAYS);
        col0.setMinWidth(300);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        col1.setMinWidth(300);

        grid.getColumnConstraints().addAll(col0, col1);

        // Row 0, Col 0: Date of Birth
        grid.add(card("DATE OF BIRTH", dobField, () -> {
            if (onDobSearch != null) onDobSearch.accept(dobField.getText().trim());
        }), 0, 0);

        // Row 0, Col 1: Phone Number (beside DOB)
        grid.add(card("PHONE NUMBER", phoneField, () -> {
            if (onPhoneSearch != null) onPhoneSearch.accept(phoneField.getText().trim());
        }), 1, 0);

        // Row 1, Col 0: Last Name
        grid.add(card("LAST NAME", lastNameField, () -> {
            if (onLastNameSearch != null) onLastNameSearch.accept(lastNameField.getText().trim());
        }), 0, 1);

        // Row 1, Col 1: First Name (uses last name field too)
        grid.add(card("FIRST NAME  (uses Last Name field)", firstNameField, () -> {
            if (onFirstLastSearch != null) onFirstLastSearch.run();
        }), 1, 1);

        // Auto-format DOB as MM/DD/YYYY while typing
        autoFormatDate(dobField);

        // Allow Enter key on each field to trigger its search
        dobField.setOnAction(e -> { if (onDobSearch != null) onDobSearch.accept(dobField.getText().trim()); });
        lastNameField.setOnAction(e -> { if (onLastNameSearch != null) onLastNameSearch.accept(lastNameField.getText().trim()); });
        phoneField.setOnAction(e -> { if (onPhoneSearch != null) onPhoneSearch.accept(phoneField.getText().trim()); });
        firstNameField.setOnAction(e -> { if (onFirstLastSearch != null) onFirstLastSearch.run(); });
    }

    private VBox card(String labelText, TextField field, Runnable onSearch) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("search-label");

        Button btn = new Button("Search");
        btn.getStyleClass().add("search-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> onSearch.run());

        VBox card = new VBox(8, lbl, field, btn);
        card.getStyleClass().add("search-card");
        card.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(field, Priority.ALWAYS);
        field.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private TextField searchField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("search-field");
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private static void autoFormatDate(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            String digits = newVal.replaceAll("[^0-9]", "");
            if (digits.length() > 8) digits = digits.substring(0, 8);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < digits.length(); i++) {
                if (i == 2 || i == 4) sb.append('/');
                sb.append(digits.charAt(i));
            }
            String formatted = sb.toString();
            if (!formatted.equals(newVal)) {
                field.setText(formatted);
                field.positionCaret(formatted.length());
            }
        });
    }

    public void setOnDobSearch(Consumer<String> cb)      { this.onDobSearch = cb; }
    public void setOnLastNameSearch(Consumer<String> cb) { this.onLastNameSearch = cb; }
    public void setOnPhoneSearch(Consumer<String> cb)    { this.onPhoneSearch = cb; }
    public void setOnFirstLastSearch(Runnable cb)        { this.onFirstLastSearch = cb; }

    public String getLastName()  { return lastNameField.getText().trim(); }
    public String getFirstName() { return firstNameField.getText().trim(); }

    public GridPane getRoot() { return grid; }

    public void clear() {
        dobField.clear();
        lastNameField.clear();
        phoneField.clear();
        firstNameField.clear();
    }
}
