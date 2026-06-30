package pharmacy.ui.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pharmacy.data.CsvDataStore;
import pharmacy.service.PatientLookupService;

import java.net.URL;

public class PharmacyApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        CsvDataStore dataStore = new CsvDataStore("data");
        PatientLookupService service = new PatientLookupService(dataStore);

        MainWindow mainWindow = new MainWindow(service);

        Scene scene = new Scene(mainWindow.getRoot(), 1920, 1080);

        URL css = getClass().getResource("style.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        primaryStage.setTitle("PlexiFill");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }
}
