package pharmacy.ui.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pharmacy.model.Patient;
import pharmacy.model.Prescription;
import pharmacy.service.PatientLookupService;

import java.util.List;
import java.util.function.Function;

public class FillQueueView {

    private final BorderPane root;
    private final PatientLookupService service;
    private final StackPane toastRoot;
    private final TableView<Prescription> table = new TableView<>();

    public FillQueueView(PatientLookupService service, StackPane toastRoot) {
        this.service   = service;
        this.toastRoot = toastRoot;
        root = buildLayout();
    }

    private BorderPane buildLayout() {
        Label title = new Label("Fill Queue");
        title.getStyleClass().add("page-title");

        HBox titleBar = new HBox(title);
        titleBar.setAlignment(Pos.CENTER_LEFT);

        buildTable();

        Label placeholder = new Label("Fill queue is empty");
        placeholder.getStyleClass().add("text-muted");
        table.setPlaceholder(placeholder);

        VBox tableBox = new VBox(table);
        VBox.setVgrow(table, Priority.ALWAYS);
        tableBox.setPadding(new Insets(0, 32, 24, 32));

        BorderPane layout = new BorderPane();
        VBox top = new VBox(titleBar);
        top.setPadding(new Insets(0, 32, 20, 32));
        layout.setTop(top);
        layout.setCenter(tableBox);
        layout.getStyleClass().add("content-area");
        return layout;
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        // Priority column — custom cell with color coding
        TableColumn<Prescription, String> colPriority = new TableColumn<>("Priority");
        colPriority.setCellValueFactory(data -> {
            Prescription rx = data.getValue();
            String text = rx.isHighPriority() ? "* HIGH" : "REGULAR";
            return new SimpleStringProperty(text);
        });
        colPriority.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else if (item.startsWith("*")) {
                    setText(item);
                    setStyle("-fx-text-fill: #3fb950; -fx-font-weight: bold;");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #8b949e;");
                }
            }
        });
        colPriority.setPrefWidth(110);

        // Patient name — looked up from phone
        TableColumn<Prescription, String> colPatient = col("Patient", rx -> {
            Patient p = service.getPatientByPhone(rx.getPatientPhone());
            return p != null ? p.getFullName() : "Unknown";
        });
        colPatient.setPrefWidth(200);

        TableColumn<Prescription, String> colPhone    = col("Phone",      Prescription::getPatientPhone);
        TableColumn<Prescription, String> colMed      = col("Medication", Prescription::getMedicationName);
        TableColumn<Prescription, String> colStrength = col("Strength",   Prescription::getStrength);
        TableColumn<Prescription, String> colDays     = col("Days",       rx -> rx.getDaySupply() + "-day");

        colPhone.setPrefWidth(150);
        colMed.setPrefWidth(240);
        colStrength.setPrefWidth(110);
        colDays.setPrefWidth(90);

        table.getColumns().addAll(colPriority, colPatient, colPhone, colMed, colStrength, colDays);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getStyleClass().add("table-view");

        table.setRowFactory(tv -> {
            TableRow<Prescription> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openFillDialog(row.getItem());
                }
            });
            return row;
        });
    }

    private void openFillDialog(Prescription rx) {
        Patient patient = service.getPatientByPhone(rx.getPatientPhone());
        String patientName = patient != null ? patient.getFullName() : rx.getPatientPhone();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Fill Prescription");
        dialog.setHeaderText(null);

        // Style the dialog pane
        DialogPane pane = dialog.getDialogPane();
        pane.setStyle(
            "-fx-background-color: #161b22; " +
            "-fx-border-color: #30363d; " +
            "-fx-border-width: 1; " +
            "-fx-font-family: 'Consolas', monospace;");

        // Content
        Label nameLbl = new Label(patientName);
        nameLbl.setStyle("-fx-text-fill: #3fb950; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label phoneLbl = new Label("Phone: " + rx.getPatientPhone());
        phoneLbl.setStyle("-fx-text-fill: #8b949e;");

        Label medLbl = new Label(rx.getMedicationName() + "  " + rx.getStrength());
        medLbl.setStyle("-fx-text-fill: #e6edf3; -fx-font-size: 14px;");

        Label daysLbl = new Label(rx.getDaySupply() + "-day supply");
        daysLbl.setStyle("-fx-text-fill: #8b949e;");

        String priorityText = rx.isHighPriority() ? "* HIGH PRIORITY" : "Regular";
        Label prioLbl = new Label(priorityText);
        prioLbl.setStyle(rx.isHighPriority()
            ? "-fx-text-fill: #3fb950; -fx-font-weight: bold;"
            : "-fx-text-fill: #8b949e;");

        VBox content = new VBox(12, nameLbl, phoneLbl, new Separator(), medLbl, daysLbl, prioLbl);
        content.setPadding(new Insets(20));

        pane.setContent(content);

        ButtonType fillType   = new ButtonType("Fill Prescription", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Don't Fill",        ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(fillType, cancelType);

        // Style the buttons
        Button fillBtn = (Button) pane.lookupButton(fillType);
        fillBtn.getStyleClass().add("btn-primary");

        Button cancelBtn = (Button) pane.lookupButton(cancelType);
        cancelBtn.getStyleClass().add("btn-secondary");

        dialog.showAndWait().ifPresent(result -> {
            if (result == fillType) {
                service.fillPrescription(rx);
                Toast.show(toastRoot, rx.getMedicationName() + " " + rx.getStrength()
                        + " filled — status set to Ready.");
                refresh();
            }
        });
    }

    public void refresh() {
        List<Prescription> queue = service.getFillQueue().getAll();
        table.setItems(FXCollections.observableArrayList(queue));
    }

    private static <T> TableColumn<T, String> col(String header, Function<T, String> mapper) {
        TableColumn<T, String> col = new TableColumn<>(header);
        col.setCellValueFactory(data -> new SimpleStringProperty(mapper.apply(data.getValue())));
        return col;
    }

    public BorderPane getRoot() { return root; }
}
