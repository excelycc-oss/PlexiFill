package pharmacy.ui.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import pharmacy.service.PatientLookupService;

import java.util.ArrayList;
import java.util.List;

public class MainWindow {

    private final StackPane root;
    private final StackPane contentArea;
    private final PatientLookupService service;

    private final List<Button> navButtons = new ArrayList<>();

    private ReleaseToPatientView releaseView;
    private PatientLookupView lookupView;
    private FillQueueView fillQueueView;
    private RefillsDueSoonView refillsView;

    public MainWindow(PatientLookupService service) {
        this.service = service;

        // Sidebar
        Label title = new Label("PlexiFill");
        title.getStyleClass().add("sidebar-title");

        Region divider = new Region();
        divider.getStyleClass().add("sidebar-divider");

        Button btnRelease   = navButton("Release to Patient");
        Button btnLookup    = navButton("Patient Lookup");
        Button btnFillQueue = navButton("Fill Queue");
        Button btnRefills   = navButton("Refills Due Soon");

        navButtons.add(btnRelease);
        navButtons.add(btnLookup);
        navButtons.add(btnFillQueue);
        navButtons.add(btnRefills);

        VBox sidebar = new VBox(title, divider, btnRelease, btnLookup, btnFillQueue, btnRefills);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(sidebar, Priority.ALWAYS);

        // Content area
        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");
        HBox.setHgrow(contentArea, Priority.ALWAYS);

        // Root layout
        BorderPane borderPane = new BorderPane();
        borderPane.setLeft(sidebar);
        borderPane.setCenter(contentArea);

        root = new StackPane(borderPane);
        root.setStyle("-fx-background-color: #0d1117;");

        // Init views
        releaseView   = new ReleaseToPatientView(service, root);
        lookupView    = new PatientLookupView(service, root);
        fillQueueView = new FillQueueView(service, root);
        refillsView   = new RefillsDueSoonView(service);

        // Double-click shortcuts from Patient Lookup profile
        lookupView.setOnGoToFillQueue(() -> navigate(2));
        lookupView.setOnGoToReleaseToPatient(() -> navigate(0));

        // After refilling from Refills Due Soon, jump to Fill Queue
        refillsView.setOnSentToQueue(() -> navigate(2));

        // Nav actions
        btnRelease.setOnAction(e -> navigate(0));
        btnLookup.setOnAction(e -> navigate(1));
        btnFillQueue.setOnAction(e -> navigate(2));
        btnRefills.setOnAction(e -> navigate(3));

        navigate(0); // default
    }

    private Button navButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("nav-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        return btn;
    }

    private void navigate(int index) {
        for (int i = 0; i < navButtons.size(); i++) {
            navButtons.get(i).getStyleClass().removeAll("nav-btn-active");
            if (i == index) {
                navButtons.get(i).getStyleClass().add("nav-btn-active");
            }
        }
        contentArea.getChildren().clear();
        switch (index) {
            case 0 -> { releaseView.reset(); contentArea.getChildren().add(releaseView.getRoot()); }
            case 1 -> { lookupView.reset();  contentArea.getChildren().add(lookupView.getRoot()); }
            case 2 -> { fillQueueView.refresh(); contentArea.getChildren().add(fillQueueView.getRoot()); }
            case 3 -> { refillsView.refresh(); contentArea.getChildren().add(refillsView.getRoot()); }
        }
    }

    public StackPane getRoot() { return root; }
}
