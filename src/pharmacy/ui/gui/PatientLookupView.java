package pharmacy.ui.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pharmacy.model.Patient;
import pharmacy.model.Prescription;
import pharmacy.model.PrescriptionStatus;
import pharmacy.service.PatientLookupService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Function;

public class PatientLookupView {

    private final BorderPane root;
    private final PatientLookupService service;
    private final StackPane toastRoot;

    private final SearchPanel searchPanel;
    private final TableView<Patient> patientTable = new TableView<>();
    private final TableView<Prescription> rxTable = new TableView<>();

    private Patient currentPatient;

    private BorderPane searchView;
    private BorderPane profileView;
    private AddPatientView addPatientView;
    private AddMedicationView addMedicationView;

    private final Button removeBtn = new Button("Remove Medication");
    private Runnable onGoToFillQueue;
    private Runnable onGoToReleaseToPatient;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public PatientLookupView(PatientLookupService service, StackPane toastRoot) {
        this.service   = service;
        this.toastRoot = toastRoot;
        this.searchPanel = new SearchPanel();

        buildPatientTable();
        buildRxTable();

        searchView  = buildSearchView();
        profileView = buildProfileView();

        root = new BorderPane();
        root.getStyleClass().add("content-area");
        showSearchView();
    }

    // Search view

    private BorderPane buildSearchView() {
        Label title = new Label("Patient Lookup");
        title.getStyleClass().add("page-title");

        Button addPatientBtn = new Button("Add New Patient");
        addPatientBtn.getStyleClass().add("btn-primary");
        addPatientBtn.setOnAction(e -> showAddPatientForm());

        HBox titleBar = new HBox(title);
        HBox.setHgrow(title, Priority.ALWAYS);
        titleBar.getChildren().add(addPatientBtn);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(0, 32, 0, 0));

        searchPanel.setOnDobSearch(this::searchByDob);
        searchPanel.setOnLastNameSearch(this::searchByLastName);
        searchPanel.setOnPhoneSearch(this::searchByPhone);
        searchPanel.setOnFirstLastSearch(() -> searchByFirstLast(searchPanel.getLastName(), searchPanel.getFirstName()));

        Label placeholder = new Label("Enter a search to find patients");
        placeholder.getStyleClass().add("text-muted");
        patientTable.setPlaceholder(placeholder);

        VBox tableBox = new VBox(patientTable);
        VBox.setVgrow(patientTable, Priority.ALWAYS);
        tableBox.setPadding(new Insets(0, 32, 24, 32));

        BorderPane view = new BorderPane();
        VBox top = new VBox(titleBar, searchPanel.getRoot());
        view.setTop(top);
        view.setCenter(tableBox);
        return view;
    }

    private void buildPatientTable() {
        TableColumn<Patient, String> colLast  = col("Last Name",     Patient::getLastName);
        TableColumn<Patient, String> colFirst = col("First Name",    Patient::getFirstName);
        TableColumn<Patient, String> colDob   = col("Date of Birth", Patient::getFormattedDob);
        TableColumn<Patient, String> colPhone = col("Phone",         Patient::getPhoneNumber);

        colLast.setPrefWidth(200);
        colFirst.setPrefWidth(200);
        colDob.setPrefWidth(150);
        colPhone.setPrefWidth(160);

        patientTable.getColumns().addAll(colLast, colFirst, colDob, colPhone);
        patientTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        patientTable.getStyleClass().add("table-view");

        patientTable.setRowFactory(tv -> {
            TableRow<Patient> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openProfile(row.getItem());
                }
            });
            return row;
        });
    }

    // Profile view

    private BorderPane buildProfileView() {
        VBox header = new VBox();
        header.getStyleClass().add("patient-header");

        Label rxPlaceholder = new Label("No medications on file");
        rxPlaceholder.getStyleClass().add("text-muted");
        rxTable.setPlaceholder(rxPlaceholder);

        VBox tableBox = new VBox(rxTable);
        VBox.setVgrow(rxTable, Priority.ALWAYS);
        tableBox.setPadding(new Insets(24, 32, 0, 32));

        Button backBtn = new Button("← Back");
        backBtn.getStyleClass().add("btn-secondary");
        backBtn.setOnAction(e -> showSearchView());

        Button addMedBtn = new Button("Add Medication");
        addMedBtn.getStyleClass().add("btn-primary");
        addMedBtn.setOnAction(e -> showAddMedicationForm());

        removeBtn.getStyleClass().add("btn-secondary");
        removeBtn.setDisable(true);
        removeBtn.setOnAction(e -> removeSelectedMedication());

        Button deletePatientBtn = new Button("Delete Patient");
        deletePatientBtn.getStyleClass().add("btn-danger");
        deletePatientBtn.setOnAction(e -> deleteCurrentPatient());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox btnBar = new HBox(12, backBtn, addMedBtn, removeBtn, spacer, deletePatientBtn);
        btnBar.setPadding(new Insets(16, 32, 24, 32));
        btnBar.setAlignment(Pos.CENTER_LEFT);

        BorderPane view = new BorderPane();
        view.setTop(header);
        view.setCenter(tableBox);
        view.setBottom(btnBar);
        return view;
    }

    private void buildRxTable() {
        TableColumn<Prescription, String> colMed      = col("Medication",  Prescription::getMedicationName);
        TableColumn<Prescription, String> colStrength = col("Strength",    Prescription::getStrength);
        TableColumn<Prescription, String> colDays     = col("Day Supply",  rx -> rx.getDaySupply() + "-day");
        TableColumn<Prescription, String> colStatus   = col("Status", rx -> {
            if (rx.getStatus() == PrescriptionStatus.PICKED_UP && rx.getPickupDate() != null) {
                return "Picked Up " + rx.getPickupDate().format(DATE_FMT);
            }
            return rx.getStatus().getDisplayName();
        });
        TableColumn<Prescription, String> colRefill   = col("Next Refill", rx ->
                rx.getRefillDate() != null ? rx.getRefillDate().format(DATE_FMT) : "—");

        TableColumn<Prescription, String> colRefills  = col("Refills", rx ->
                rx.getRefillsRemaining() > 0 ? String.valueOf(rx.getRefillsRemaining()) : "—");

        colMed.setPrefWidth(200);
        colStrength.setPrefWidth(100);
        colDays.setPrefWidth(90);
        colStatus.setPrefWidth(155);
        colRefill.setPrefWidth(110);
        colRefills.setPrefWidth(80);

        rxTable.getColumns().addAll(colMed, colStrength, colDays, colStatus, colRefill, colRefills);
        rxTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        rxTable.getStyleClass().add("table-view");

        // Track selection for remove button
        rxTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> removeBtn.setDisable(newVal == null));

        // Double-click IN_QUEUE → Fill Queue; READY → Release to Patient
        rxTable.setRowFactory(tv -> {
            TableRow<Prescription> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    Prescription rx = row.getItem();
                    if (rx.getStatus() == PrescriptionStatus.IN_QUEUE && onGoToFillQueue != null) {
                        onGoToFillQueue.run();
                    } else if (rx.getStatus() == PrescriptionStatus.READY && onGoToReleaseToPatient != null) {
                        onGoToReleaseToPatient.run();
                    }
                }
            });
            return row;
        });
    }

    public void setOnGoToFillQueue(Runnable cb)         { this.onGoToFillQueue = cb; }
    public void setOnGoToReleaseToPatient(Runnable cb)  { this.onGoToReleaseToPatient = cb; }

    private void openProfile(Patient patient) {
        this.currentPatient = patient;

        VBox header = (VBox) profileView.getTop();
        header.getChildren().clear();

        Label nameLbl   = new Label(patient.getFullName());
        nameLbl.getStyleClass().add("patient-header-name");

        Label detailLbl = new Label("DOB: " + patient.getFormattedDob()
                + "   |   Phone: " + patient.getPhoneNumber());
        detailLbl.getStyleClass().add("patient-header-detail");

        header.getChildren().addAll(nameLbl, detailLbl);

        refreshRxTable();
        root.setCenter(profileView);
    }

    private void refreshRxTable() {
        if (currentPatient == null) return;
        List<Prescription> rxList = service.getAllPrescriptions(currentPatient.getPhoneNumber());
        rxTable.setItems(FXCollections.observableArrayList(rxList));
    }

    // Add patient

    private void showAddPatientForm() {
        addPatientView = new AddPatientView(service, toastRoot, () -> {
            showSearchView();
            searchPanel.clear();
        });
        root.setCenter(addPatientView.getRoot());
    }

    // Add medication

    private void showAddMedicationForm() {
        if (currentPatient == null) return;
        addMedicationView = new AddMedicationView(service, toastRoot, currentPatient, () -> {
            openProfile(currentPatient);
            refreshRxTable();
        });
        root.setCenter(addMedicationView.getRoot());
    }

    // Delete patient

    private void deleteCurrentPatient() {
        if (currentPatient == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Patient");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete " + currentPatient.getFullName()
                + " and all of their prescriptions? This cannot be undone.");
        confirm.getDialogPane().setStyle(
                "-fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-width: 1;");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                String name = currentPatient.getFullName();
                service.deletePatient(currentPatient);
                reset();
                Toast.show(toastRoot, name + " deleted.");
            }
        });
    }

    // Remove medication

    private void removeSelectedMedication() {
        Prescription rx = rxTable.getSelectionModel().getSelectedItem();
        if (rx == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Medication");
        confirm.setHeaderText(null);
        confirm.setContentText("Remove " + rx.getMedicationName() + " " + rx.getStrength()
                + " from " + currentPatient.getFullName() + "?");
        confirm.getDialogPane().setStyle(
                "-fx-background-color: #ffffff; -fx-border-color: #cccccc; -fx-border-width: 1;");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                service.removePrescription(rx);
                Toast.show(toastRoot, rx.getMedicationName() + " " + rx.getStrength() + " removed.");
                refreshRxTable();
            }
        });
    }

    // Search

    private void searchByDob(String raw) {
        if (raw.isEmpty()) return;
        try {
            LocalDate dob = LocalDate.parse(raw, DATE_FMT);
            setResults(service.findAllByDateOfBirth(dob), "DOB: " + raw);
        } catch (DateTimeParseException ex) {
            Toast.showError(toastRoot, "Invalid date format. Use MM/DD/YYYY.");
        }
    }

    private void searchByLastName(String lastName) {
        if (lastName.isEmpty()) return;
        setResults(service.findAllByLastName(lastName), "last name: " + lastName.toUpperCase());
    }

    private void searchByPhone(String raw) {
        if (raw.isEmpty()) return;
        String phone = formatPhone(raw);
        if (phone == null) { Toast.showError(toastRoot, "Enter a 10-digit phone number."); return; }
        setResults(service.findAllByPhone(phone), "phone: " + phone);
    }

    private void searchByFirstLast(String lastName, String firstName) {
        if (lastName.isEmpty() || firstName.isEmpty()) {
            Toast.showError(toastRoot, "Enter both Last Name and First Name.");
            return;
        }
        setResults(service.findAllByFirstAndLastName(firstName, lastName),
                firstName.toUpperCase() + " " + lastName.toUpperCase());
    }

    private void setResults(List<Patient> results, String query) {
        patientTable.setItems(FXCollections.observableArrayList(results));
        if (results.isEmpty()) Toast.showError(toastRoot, "No patients found for " + query);
    }

    // Helpers

    private void showSearchView() {
        root.setCenter(searchView);
    }

    private static <T> TableColumn<T, String> col(String header, Function<T, String> mapper) {
        TableColumn<T, String> col = new TableColumn<>(header);
        col.setCellValueFactory(data -> new SimpleStringProperty(mapper.apply(data.getValue())));
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
        patientTable.getItems().clear();
        currentPatient = null;
    }

    public BorderPane getRoot() { return root; }
}
