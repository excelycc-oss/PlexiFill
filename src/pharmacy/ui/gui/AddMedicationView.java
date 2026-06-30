package pharmacy.ui.gui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pharmacy.model.Medication;
import pharmacy.model.Patient;
import pharmacy.model.Prescription;
import pharmacy.model.PrescriptionStatus;
import pharmacy.service.PatientLookupService;

import java.util.ArrayList;
import java.util.List;

public class AddMedicationView {

    private final BorderPane root;
    private final PatientLookupService service;
    private final StackPane toastRoot;
    private final Patient patient;
    private final Runnable onDone;

    private final TextField searchField = new TextField();
    private final ListView<String> medList = new ListView<>();
    private final ListView<String> strengthList = new ListView<>();
    private final TextField daySupplyField  = new TextField();
    private final TextField refillsField    = new TextField();

    private final RadioButton fillTodayBtn   = new RadioButton("Fill Today");
    private final RadioButton saveOnlyBtn    = new RadioButton("Save to Profile Only");
    private final RadioButton highPrioBtn    = new RadioButton("High Priority");
    private final RadioButton regularPrioBtn = new RadioButton("Regular Priority");

    private final VBox prioritySection = new VBox(8, highPrioBtn, regularPrioBtn);

    private List<Medication> allMedications;
    private Medication selectedMedication;

    public AddMedicationView(PatientLookupService service, StackPane toastRoot, Patient patient, Runnable onDone) {
        this.service   = service;
        this.toastRoot = toastRoot;
        this.patient   = patient;
        this.onDone    = onDone;

        allMedications = service.getMedications();
        root = buildLayout();
    }

    private BorderPane buildLayout() {
        // Title bar
        Label title = new Label("Add Medication — " + patient.getFullName());
        title.getStyleClass().add("page-title");
        title.setStyle("-fx-font-size: 18px;");

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> onDone.run());

        HBox titleBar = new HBox(title);
        HBox.setHgrow(title, Priority.ALWAYS);
        titleBar.getChildren().add(cancelBtn);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(0, 32, 0, 0));

        // ── Medication search ──────────────────────────────────────────────────
        Label medSearchLbl = new Label("MEDICATION");
        medSearchLbl.getStyleClass().add("form-label");

        searchField.setPromptText("Type medication name...");
        searchField.getStyleClass().add("form-field");
        searchField.setMaxWidth(400);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterMedications(newVal));

        medList.getStyleClass().add("list-view");
        medList.setPrefHeight(200);
        medList.setMaxWidth(400);
        medList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedMedication = allMedications.stream()
                        .filter(m -> m.getName().equals(newVal))
                        .findFirst().orElse(null);
                refreshStrengths();
            }
        });

        // Populate initial list
        filterMedications("");

        VBox medSection = new VBox(6, medSearchLbl, searchField, medList);

        // ── Strength selection ─────────────────────────────────────────────────
        Label strengthLbl = new Label("STRENGTH");
        strengthLbl.getStyleClass().add("form-label");

        strengthList.getStyleClass().add("list-view");
        strengthList.setPrefHeight(180);
        strengthList.setMaxWidth(280);

        VBox strengthSection = new VBox(6, strengthLbl, strengthList);

        // ── Day supply ─────────────────────────────────────────────────────────
        Label dayLbl = new Label("DAY SUPPLY");
        dayLbl.getStyleClass().add("form-label");

        daySupplyField.setPromptText("e.g. 30, 60, 90");
        daySupplyField.getStyleClass().add("form-field");
        daySupplyField.setMaxWidth(200);

        Label refillsLbl = new Label("NUMBER OF REFILLS");
        refillsLbl.getStyleClass().add("form-label");

        refillsField.setPromptText("0 = no refills");
        refillsField.getStyleClass().add("form-field");
        refillsField.setMaxWidth(200);

        VBox daySection = new VBox(6, dayLbl, daySupplyField);

        // ── Fill option ────────────────────────────────────────────────────────
        ToggleGroup fillGroup = new ToggleGroup();
        fillTodayBtn.setToggleGroup(fillGroup);
        saveOnlyBtn.setToggleGroup(fillGroup);
        saveOnlyBtn.setSelected(true);

        fillTodayBtn.selectedProperty().addListener((obs, was, is) ->
                prioritySection.setVisible(is));

        ToggleGroup prioGroup = new ToggleGroup();
        highPrioBtn.setToggleGroup(prioGroup);
        regularPrioBtn.setToggleGroup(prioGroup);
        regularPrioBtn.setSelected(true);

        prioritySection.setVisible(false);
        prioritySection.setPadding(new Insets(8, 0, 0, 16));

        Label fillLbl = new Label("FILL OPTION");
        fillLbl.getStyleClass().add("form-label");

        VBox fillSection = new VBox(8, fillLbl, fillTodayBtn, saveOnlyBtn, prioritySection);
        fillSection.getStyleClass().add("toggle-section");
        fillSection.setMaxWidth(320);

        // ── Confirm button ─────────────────────────────────────────────────────
        Button addBtn = new Button("Add Medication");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setPrefWidth(200);
        addBtn.setOnAction(e -> handleAdd());

        // ── Layout ────────────────────────────────────────────────────────────
        HBox selectionRow = new HBox(32, medSection, strengthSection);
        selectionRow.setAlignment(Pos.TOP_LEFT);

        VBox refillsSection = new VBox(6, refillsLbl, refillsField);

        VBox formContent = new VBox(24, selectionRow, daySection, refillsSection, fillSection, addBtn);
        formContent.setAlignment(Pos.TOP_LEFT);

        ScrollPane scrollPane = new ScrollPane(formContent);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");
        scrollPane.setPadding(new Insets(24, 32, 32, 32));

        BorderPane layout = new BorderPane();
        layout.setTop(titleBar);
        layout.setCenter(scrollPane);
        layout.getStyleClass().add("content-area");
        return layout;
    }

    private void filterMedications(String query) {
        List<String> filtered = new ArrayList<>();
        for (Medication m : allMedications) {
            if (m.getName().toLowerCase().startsWith(query.toLowerCase())) {
                filtered.add(m.getName());
            }
        }
        medList.setItems(FXCollections.observableArrayList(filtered));
        strengthList.setItems(FXCollections.observableArrayList());
        selectedMedication = null;
    }

    private void refreshStrengths() {
        if (selectedMedication == null) {
            strengthList.setItems(FXCollections.observableArrayList());
            return;
        }
        List<String> strengths = new ArrayList<>();
        for (String s : selectedMedication.getStrengths()) {
            strengths.add(s.trim());
        }
        strengthList.setItems(FXCollections.observableArrayList(strengths));
    }

    private void handleAdd() {
        if (selectedMedication == null) {
            Toast.showError(toastRoot, "Please select a medication.");
            return;
        }

        String strength = strengthList.getSelectionModel().getSelectedItem();
        if (strength == null || strength.isEmpty()) {
            Toast.showError(toastRoot, "Please select a strength.");
            return;
        }

        String dayRaw = daySupplyField.getText().trim();
        if (dayRaw.isEmpty()) {
            Toast.showError(toastRoot, "Please enter a day supply.");
            return;
        }

        int daySupply;
        try {
            daySupply = Integer.parseInt(dayRaw);
            if (daySupply <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            Toast.showError(toastRoot, "Day supply must be a positive number.");
            return;
        }

        PrescriptionStatus status;
        String priority = "";

        if (fillTodayBtn.isSelected()) {
            status   = PrescriptionStatus.IN_QUEUE;
            priority = highPrioBtn.isSelected() ? "High" : "Regular";
        } else {
            status = PrescriptionStatus.ON_FILE;
        }

        int refillsRemaining = 0;
        String refillsRaw = refillsField.getText().trim();
        if (!refillsRaw.isEmpty()) {
            try {
                refillsRemaining = Integer.parseInt(refillsRaw);
                if (refillsRemaining < 0) refillsRemaining = 0;
            } catch (NumberFormatException ex) {
                Toast.showError(toastRoot, "Number of refills must be a whole number.");
                return;
            }
        }

        Prescription rx = new Prescription(
                patient.getPhoneNumber(),
                selectedMedication.getName(),
                strength,
                daySupply,
                status,
                priority,
                null);
        rx.setRefillsRemaining(refillsRemaining);

        service.addPrescription(rx);

        String msg = selectedMedication.getName() + " " + strength + " added"
                + (status == PrescriptionStatus.IN_QUEUE ? " — sent to fill queue." : " to profile.");
        Toast.show(toastRoot, msg);
        onDone.run();
    }

    public BorderPane getRoot() { return root; }
}
