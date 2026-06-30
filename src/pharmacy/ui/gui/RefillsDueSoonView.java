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

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

public class RefillsDueSoonView {

    private final BorderPane root;
    private final PatientLookupService service;
    private final TableView<Prescription> table = new TableView<>();
    private Runnable onSentToQueue;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public RefillsDueSoonView(PatientLookupService service) {
        this.service = service;
        root = buildLayout();
    }

    public void setOnSentToQueue(Runnable cb) { this.onSentToQueue = cb; }

    private BorderPane buildLayout() {
        Label title = new Label("Refills Due Soon");
        title.getStyleClass().add("page-title");

        Label subtitle = new Label("Prescriptions with a refill date within 3 days or overdue — double-click to refill");
        subtitle.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 13px;");

        HBox titleBar = new HBox(title);
        titleBar.setAlignment(Pos.CENTER_LEFT);

        VBox top = new VBox(8, titleBar, subtitle);
        top.setPadding(new Insets(0, 32, 20, 32));

        buildTable();

        Label placeholder = new Label("No refills due in the next 3 days");
        placeholder.getStyleClass().add("text-muted");
        table.setPlaceholder(placeholder);

        VBox tableBox = new VBox(table);
        VBox.setVgrow(table, Priority.ALWAYS);
        tableBox.setPadding(new Insets(0, 32, 24, 32));

        BorderPane layout = new BorderPane();
        layout.setTop(top);
        layout.setCenter(tableBox);
        layout.getStyleClass().add("content-area");
        return layout;
    }

    @SuppressWarnings("unchecked")
    private void buildTable() {
        // Patient name
        TableColumn<Prescription, String> colPatient = col("Patient", rx -> {
            Patient p = service.getPatientByPhone(rx.getPatientPhone());
            return p != null ? p.getFullName() : "Unknown";
        });
        colPatient.setPrefWidth(220);

        TableColumn<Prescription, String> colPhone    = col("Phone",      Prescription::getPatientPhone);
        TableColumn<Prescription, String> colMed      = col("Medication", Prescription::getMedicationName);
        TableColumn<Prescription, String> colStrength = col("Strength",   Prescription::getStrength);

        TableColumn<Prescription, String> colRefill = col("Refill Date", rx ->
                rx.getRefillDate() != null ? rx.getRefillDate().format(DATE_FMT) : "—");

        TableColumn<Prescription, String> colRefills = col("Refills Left", rx ->
                rx.getRefillsRemaining() > 0 ? String.valueOf(rx.getRefillsRemaining()) : "—");

        colPhone.setPrefWidth(150);
        colMed.setPrefWidth(220);
        colStrength.setPrefWidth(110);
        colRefill.setPrefWidth(130);
        colRefills.setPrefWidth(100);

        // Color-code refill date: today = red/urgent, tomorrow = yellow, 2-3 days = normal
        colRefill.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.equals("—")) {
                    setText(item);
                    setStyle("");
                    return;
                }
                setText(item);
                Prescription rx = getTableView().getItems().get(getIndex());
                if (rx.getRefillDate() != null) {
                    long daysUntil = java.time.LocalDate.now().until(rx.getRefillDate()).getDays();
                    if (daysUntil <= 0) {
                        setStyle("-fx-text-fill: #f85149; -fx-font-weight: bold;"); // overdue / today
                    } else if (daysUntil == 1) {
                        setStyle("-fx-text-fill: #d29922; -fx-font-weight: bold;"); // tomorrow
                    } else {
                        setStyle("-fx-text-fill: #3fb950;");
                    }
                }
            }
        });

        table.getColumns().addAll(colPatient, colPhone, colMed, colStrength, colRefill, colRefills);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getStyleClass().add("table-view");

        table.setRowFactory(tv -> {
            TableRow<Prescription> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openRefillDialog(row.getItem());
                }
            });
            return row;
        });
    }

    private void openRefillDialog(Prescription rx) {
        Patient patient = service.getPatientByPhone(rx.getPatientPhone());
        String patientName = patient != null ? patient.getFullName() : rx.getPatientPhone();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Refill Prescription");
        dialog.setHeaderText(null);

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 1; " +
                "-fx-font-family: 'Consolas', monospace;");

        Label nameLbl = new Label(patientName);
        nameLbl.setStyle("-fx-text-fill: #3fb950; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label medLbl = new Label(rx.getMedicationName() + "  " + rx.getStrength()
                + "  (" + rx.getDaySupply() + "-day)");
        medLbl.setStyle("-fx-text-fill: #e6edf3; -fx-font-size: 14px;");

        int refills = rx.getRefillsRemaining();
        Label refillLbl = new Label(refills > 0
                ? refills + " refill" + (refills == 1 ? "" : "s") + " remaining"
                : "No refills remaining");
        refillLbl.setStyle(refills > 0
                ? "-fx-text-fill: #3fb950;"
                : "-fx-text-fill: #f85149;");

        VBox content = new VBox(12, nameLbl, medLbl, new Separator(), refillLbl);
        content.setPadding(new Insets(20));
        pane.setContent(content);

        if (refills > 0) {
            ButtonType sendType   = new ButtonType("Send to Fill Queue", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            pane.getButtonTypes().addAll(sendType, cancelType);

            Button sendBtn = (Button) pane.lookupButton(sendType);
            sendBtn.getStyleClass().add("btn-primary");

            dialog.showAndWait().ifPresent(result -> {
                if (result == sendType) {
                    service.refillPrescription(rx);
                    refresh();
                    if (onSentToQueue != null) onSentToQueue.run();
                }
            });
        } else {
            pane.getButtonTypes().add(ButtonType.OK);
            dialog.showAndWait();
        }
    }

    public void refresh() {
        List<Prescription> due = service.getRefillsDueSoon();
        table.setItems(FXCollections.observableArrayList(due));
    }

    private static <T> TableColumn<T, String> col(String header, Function<T, String> mapper) {
        TableColumn<T, String> col = new TableColumn<>(header);
        col.setCellValueFactory(data -> new SimpleStringProperty(mapper.apply(data.getValue())));
        return col;
    }

    public BorderPane getRoot() { return root; }
}
