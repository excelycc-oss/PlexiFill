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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ReleaseToPatientView {

    private final BorderPane root;
    private final PatientLookupService service;
    private final StackPane toastRoot;
    private final SearchPanel searchPanel;

    // Patient results table
    private final TableView<Patient> patientTable = new TableView<>();

    // Medication (ready) table
    private final TableView<Prescription> medTable = new TableView<>();
    private final Button releaseBtn = new Button("Release Selected");

    // State
    private Patient currentPatient;

    // Panels
    private final BorderPane searchView;
    private final BorderPane patientView;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public ReleaseToPatientView(PatientLookupService service, StackPane toastRoot) {
        this.service   = service;
        this.toastRoot = toastRoot;

        searchPanel = new SearchPanel();
        searchView  = buildSearchView();
        patientView = buildPatientView();

        root = new BorderPane();
        root.getStyleClass().add("content-area");
        showSearchView();
    }

    // Search view

    private BorderPane buildSearchView() {
        // Title
        Label title = new Label("Release to Patient");
        title.getStyleClass().add("page-title");

        // Search callbacks
        searchPanel.setOnDobSearch(this::searchByDob);
        searchPanel.setOnLastNameSearch(raw -> searchByLastName(raw));
        searchPanel.setOnPhoneSearch(this::searchByPhone);
        searchPanel.setOnFirstLastSearch(() ->
                searchByFirstLast(searchPanel.getLastName(), searchPanel.getFirstName()));

        // Patient results table
        buildPatientTable();

        Label placeholder = new Label("Enter a search to find patients");
        placeholder.getStyleClass().add("text-muted");
        patientTable.setPlaceholder(placeholder);

        VBox tableBox = new VBox(patientTable);
        VBox.setVgrow(patientTable, Priority.ALWAYS);
        tableBox.setPadding(new Insets(0, 32, 24, 32));

        BorderPane view = new BorderPane();
        VBox top = new VBox(title, searchPanel.getRoot());
        view.setTop(top);
        view.setCenter(tableBox);
        return view;
    }

    private void buildPatientTable() {
        TableColumn<Patient, String> colLast  = col("Last Name",  p -> p.getLastName());
        TableColumn<Patient, String> colFirst = col("First Name", p -> p.getFirstName());
        TableColumn<Patient, String> colDob   = col("Date of Birth", p -> p.getFormattedDob());
        TableColumn<Patient, String> colPhone = col("Phone", p -> p.getPhoneNumber());

        colLast.setPrefWidth(200);
        colFirst.setPrefWidth(200);
        colDob.setPrefWidth(150);
        colPhone.setPrefWidth(160);

        patientTable.getColumns().addAll(colLast, colFirst, colDob, colPhone);
        patientTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        patientTable.getStyleClass().add("table-view");

        // Double-click to open patient medications
        patientTable.setRowFactory(tv -> {
            TableRow<Patient> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openPatient(row.getItem());
                }
            });
            return row;
        });
    }

    // Patient / medication view

    private BorderPane buildPatientView() {
        // Header (filled dynamically)
        VBox header = new VBox();
        header.getStyleClass().add("patient-header");

        // Medication table
        buildMedTable();
        Label medPlaceholder = new Label("No medications ready for pickup");
        medPlaceholder.getStyleClass().add("text-muted");
        medTable.setPlaceholder(medPlaceholder);

        VBox tableBox = new VBox(medTable);
        VBox.setVgrow(medTable, Priority.ALWAYS);
        tableBox.setPadding(new Insets(24, 32, 0, 32));

        // Release button bar
        releaseBtn.getStyleClass().add("btn-primary");
        releaseBtn.setDisable(true);
        releaseBtn.setOnAction(e -> releaseSelected());

        Button backBtn = new Button("← Back");
        backBtn.getStyleClass().add("btn-secondary");
        backBtn.setOnAction(e -> showSearchView());

        HBox btnBar = new HBox(12, backBtn, releaseBtn);
        btnBar.setPadding(new Insets(16, 32, 24, 32));
        btnBar.setAlignment(Pos.CENTER_LEFT);

        BorderPane view = new BorderPane();
        view.setTop(header);
        view.setCenter(tableBox);
        view.setBottom(btnBar);
        return view;
    }

    private void buildMedTable() {
        TableColumn<Prescription, String> colMed      = col("Medication", rx -> rx.getMedicationName());
        TableColumn<Prescription, String> colStrength = col("Strength",   rx -> rx.getStrength());
        TableColumn<Prescription, String> colDays     = col("Day Supply", rx -> rx.getDaySupply() + "-day");
        TableColumn<Prescription, String> colStatus   = col("Status",     rx -> rx.getStatus().getDisplayName());

        colMed.setPrefWidth(260);
        colStrength.setPrefWidth(120);
        colDays.setPrefWidth(110);
        colStatus.setPrefWidth(130);

        medTable.getColumns().addAll(colMed, colStrength, colDays, colStatus);
        medTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        medTable.getStyleClass().add("table-view");

        // Standard single-click selection; no timing-sensitive toggle logic
        medTable.setRowFactory(tv -> new TableRow<>());

        medTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> releaseBtn.setDisable(newVal == null));
    }

    private void openPatient(Patient patient) {
        this.currentPatient = patient;

        // Refresh header
        VBox header = (VBox) patientView.getTop();
        header.getChildren().clear();

        Label nameLbl   = new Label(patient.getFullName());
        nameLbl.getStyleClass().add("patient-header-name");

        Label detailLbl = new Label("DOB: " + patient.getFormattedDob()
                + "   |   Phone: " + patient.getPhoneNumber());
        detailLbl.getStyleClass().add("patient-header-detail");

        header.getChildren().addAll(nameLbl, detailLbl);

        // Load ready prescriptions
        refreshMedTable();

        root.setCenter(patientView);
    }

    private void refreshMedTable() {
        if (currentPatient == null) return;
        List<Prescription> ready = service.getReadyPrescriptions(currentPatient.getPhoneNumber());
        medTable.setItems(FXCollections.observableArrayList(ready));
        medTable.getSelectionModel().clearSelection();
        releaseBtn.setDisable(true);

        if (ready.isEmpty()) {
            showSearchView();
            Toast.show(toastRoot, "No more medications ready for " + currentPatient.getFullName());
        }
    }

    private void releaseSelected() {
        Prescription rx = medTable.getSelectionModel().getSelectedItem();
        if (rx == null) return;
        service.markPickedUp(rx, LocalDate.now());
        Toast.show(toastRoot, rx.getMedicationName() + " " + rx.getStrength()
                + " released to " + currentPatient.getFullName());
        refreshMedTable();
    }

    // Search

    private void searchByDob(String raw) {
        if (raw.isEmpty()) return;
        try {
            LocalDate dob = LocalDate.parse(raw, DATE_FMT);
            List<Patient> results = service.searchByDateOfBirth(dob);
            patientTable.setItems(FXCollections.observableArrayList(results));
            if (results.isEmpty()) Toast.showError(toastRoot, "No patients found for DOB: " + raw);
        } catch (DateTimeParseException ex) {
            Toast.showError(toastRoot, "Invalid date format. Use MM/DD/YYYY.");
        }
    }

    private void searchByLastName(String lastName) {
        if (lastName.isEmpty()) return;
        List<Patient> results = service.searchByLastName(lastName);
        patientTable.setItems(FXCollections.observableArrayList(results));
        if (results.isEmpty()) Toast.showError(toastRoot, "No patients found for last name: " + lastName.toUpperCase());
    }

    private void searchByPhone(String raw) {
        if (raw.isEmpty()) return;
        String phone = formatPhone(raw);
        if (phone == null) { Toast.showError(toastRoot, "Enter a 10-digit phone number."); return; }
        List<Patient> results = service.searchByPhone(phone);
        patientTable.setItems(FXCollections.observableArrayList(results));
        if (results.isEmpty()) Toast.showError(toastRoot, "No patients found for phone: " + phone);
    }

    private void searchByFirstLast(String lastName, String firstName) {
        if (lastName.isEmpty() || firstName.isEmpty()) {
            Toast.showError(toastRoot, "Enter both Last Name and First Name.");
            return;
        }
        List<Patient> results = service.searchByFirstAndLastName(firstName, lastName);
        patientTable.setItems(FXCollections.observableArrayList(results));
        if (results.isEmpty())
            Toast.showError(toastRoot, "No patients found for: " + firstName.toUpperCase() + " " + lastName.toUpperCase());
    }

    // Helpers

    private void showSearchView() {
        currentPatient = null;
        patientTable.getItems().clear();
        root.setCenter(searchView);
    }

    private static <T> TableColumn<T, String> col(String header, java.util.function.Function<T, String> mapper) {
        TableColumn<T, String> col = new TableColumn<>(header);
        col.setCellValueFactory(data -> new SimpleStringProperty(mapper.apply(data.getValue())));
        col.setSortable(true);
        return col;
    }

    private static String formatPhone(String input) {
        String digits = input.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            return "(" + digits.substring(0, 3) + ") " + digits.substring(3, 6) + "-" + digits.substring(6);
        }
        return null;
    }

    public void reset() {
        showSearchView();
        searchPanel.clear();
    }

    public BorderPane getRoot() { return root; }
}
