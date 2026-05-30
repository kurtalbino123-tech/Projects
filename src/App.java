import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

// ADDED: Main application entry point - extends JavaFX Application class
public class App extends Application {

    // ADDED: Override the start method - called when the application launches
    @Override
    public void start(Stage stage) throws Exception {
        // ADDED: Load the FXML file from resources and create the UI layout
        FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
        Parent root = loader.load();
        
        // ADDED: Create the scene without hardcoded dimensions to allow responsive layout
        Scene scene = new Scene(root); 
        
        // ADDED: Set the window title
        stage.setTitle("Resident Information System - Login");
        stage.setScene(scene);
        
        // ADDED: Maximize the window to fill the screen
        stage.setMaximized(true); 
            
        // ADDED: Display the stage/window
        stage.show();
    }

    // ADDED: Main entry point - starts the application
    public static void main(String[] args) {
        // ADDED: Set Windows OS scaling to 100% to match Scene Builder design
        System.setProperty("glass.win.uiScale", "100%");
        
        // ADDED: Test database connection before launching the GUI
        if (Database.connect() != null) {
            System.out.println("Successfully connected to Resident Management Database!");
        } else {
            System.out.println("Database connection failed. Is XAMPP running?");
        }
        
        // ADDED: Launch the JavaFX application
        launch(args); 
    }
}   