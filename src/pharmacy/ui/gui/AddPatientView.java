package pharmacy.ui.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pharmacy.model.Patient;
import pharmacy.service.PatientLookupService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class AddPatientView {

    private final BorderPane root;
    private final PatientLookupService service;
    private final StackPane toastRoot;
    private final Runnable onCancel;

    private final TextField lastNameField  = formField("LAST, FIRST (Last Name)");
    private final TextField firstNameField = formField("First name");
    private final TextField dobField       = formField("MM/DD/YYYY");
    private final TextField phoneField     = formField("10 digits, e.g. 9045550101");

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public AddPatientView(PatientLookupService service, StackPane toastRoot, Runnable onCancel) {
        this.service   = service;
        this.toastRoot = toastRoot;
        this.onCancel  = onCancel;

        root = buildLayout();
        autoFormatDate(dobField);
    }

    private BorderPane buildLayout() {
        // Title bar
        Label title = new Label("Add New Patient");
        title.getStyleClass().add("page-title");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> onCancel.run());

        HBox titleBar = new HBox(title);
        HBox.setHgrow(title, Priority.ALWAYS);
        titleBar.getChildren().add(cancelBtn);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(0, 32, 0, 0));

        // Form fields
        VBox form = new VBox(20);
        form.setPadding(new Insets(40, 0, 0, 0));
        form.setMaxWidth(480);
        form.setAlignment(Pos.TOP_LEFT);

        form.getChildren().addAll(
                fieldRow("LAST NAME", lastNameField),
                fieldRow("FIRST NAME", firstNameField),
                fieldRow("DATE OF BIRTH", dobField),
                fieldRow("PHONE NUMBER", phoneField)
        );

        // Submit
        Button saveBtn = new Button("Add Patient");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setPrefWidth(200);
        saveBtn.setOnAction(e -> handleSave());

        VBox submitRow = new VBox(12, saveBtn);
        submitRow.setPadding(new Insets(12, 0, 0, 0));

        form.getChildren().add(submitRow);

        VBox centerBox = new VBox(form);
        centerBox.setAlignment(Pos.TOP_CENTER);
        centerBox.setPadding(new Insets(0, 32, 32, 32));

        BorderPane layout = new BorderPane();
        layout.setTop(titleBar);
        layout.setCenter(centerBox);
        layout.getStyleClass().add("content-area");
        return layout;
    }

    private VBox fieldRow(String labelText, TextField field) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("form-label");
        VBox row = new VBox(4, lbl, field);
        return row;
    }

    private TextField formField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("form-field");
        return tf;
    }

    private void handleSave() {
        String lastName  = lastNameField.getText().trim().toUpperCase();
        String firstName = firstNameField.getText().trim().toUpperCase();
        String dobRaw    = dobField.getText().trim();
        String phoneRaw  = phoneField.getText().trim();

        if (lastName.isEmpty() || firstName.isEmpty() || dobRaw.isEmpty() || phoneRaw.isEmpty()) {
            Toast.showError(toastRoot, "All fields are required.");
            return;
        }

        LocalDate dob;
        try {
            dob = LocalDate.parse(dobRaw, DATE_FMT);
        } catch (DateTimeParseException ex) {
            Toast.showError(toastRoot, "Invalid date format. Use MM/DD/YYYY.");
            return;
        }

        String phone = formatPhone(phoneRaw);
        if (phone == null) {
            Toast.showError(toastRoot, "Phone must be exactly 10 digits.");
            return;
        }

        if (service.phoneExistsForName(phone, firstName, lastName)) {
            Toast.showError(toastRoot, firstName + " " + lastName + " is already registered with " + phone + ".");
            return;
        }

        List<Patient> dupes = service.findDuplicates(firstName, lastName, dob);
        if (!dupes.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Duplicate Patient");
            alert.setHeaderText("A patient named " + firstName + " " + lastName + " with this DOB already exists.");
            alert.setContentText("Do you still want to add this patient?");
            styleAlert(alert);
            alert.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    savePatient(firstName, lastName, dob, phone);
                }
            });
            return;
        }

        savePatient(firstName, lastName, dob, phone);
    }

    private void savePatient(String firstName, String lastName, LocalDate dob, String phone) {
        service.addPatient(firstName, lastName, dob, phone);
        Toast.show(toastRoot, "Patient " + firstName + " " + lastName + " added successfully.");
        onCancel.run();
    }

    private static String formatPhone(String input) {
        String digits = input.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            return "(" + digits.substring(0, 3) + ") " + digits.substring(3, 6) + "-" + digits.substring(6);
        }
        return null;
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

    private static void styleAlert(Alert alert) {
        alert.getDialogPane().setStyle(
                "-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 1;");
        alert.getDialogPane().lookup(".content.label").setStyle(
                "-fx-text-fill: #3fb950; -fx-font-family: 'Consolas', monospace;");
    }

    public BorderPane getRoot() { return root; }
}
