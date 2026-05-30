import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Button;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import javafx.scene.input.MouseEvent;

/**
 * LogsController - Manages the Logs page functionality
 * Displays system logs with user information, roles, and timestamps
 */
public class LogsController {

    // FXML-linked TableView and TableColumn fields for logs data display
    @FXML private TableView<SystemLog> addressTable;
    @FXML private TableColumn<SystemLog, String> colName;
    @FXML private TableColumn<SystemLog, String> colContact;
    @FXML private TableColumn<SystemLog, String> colTimeandDate;

    // KEYPOINT: Data model for the logs table
    public static class SystemLog {
        private final String userName;
        private final String role;
        private final String timestamp;

        public SystemLog(String userName, String role, String timestamp) {
            this.userName = userName;
            this.role = role;
            this.timestamp = timestamp;
        }

        public String getUserName() { return userName; }
        public String getRole() { return role; }
        public String getTimestamp() { return timestamp; }
    }
    
    // Navigation button bindings from sidebar
    @FXML private Button btnDashboard;
    @FXML private Button btnResidents;
    @FXML private Button btnAddresses;
    @FXML private Button btnDocuments;
    @FXML private Button Contactbtn;
    @FXML private Button Reportsbtn;
    @FXML private Button UsersandRolesbtn;
    @FXML private Button Logsbtn;
    @FXML private Button btnExit;
    
    // Tracking for button selection
    private Button currentSelectedButton = null;

    @FXML
    public void initialize() {
        // Set Logs button as selected since we're on the Logs page
        setButtonAsSelected(Logsbtn);
        
        // Bind table columns to data properties
        if (colName != null) colName.setCellValueFactory(new PropertyValueFactory<>("userName"));
        if (colContact != null) colContact.setCellValueFactory(new PropertyValueFactory<>("role"));
        if (colTimeandDate != null) colTimeandDate.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        // Load sample log data into the table
        loadLogData();
    }

    /**
     * loadLogData - Fetches login history from the logs table
     */
    private void loadLogData() {
        if (addressTable != null) {
            ObservableList<SystemLog> logs = FXCollections.observableArrayList();
            // KEYPOINT: Order by timestamp DESC so the most recent logins appear at the top
            String sql = "SELECT username, role, timestamp FROM logs ORDER BY timestamp DESC";
            
            try (Connection conn = Database.connect();
                 PreparedStatement pst = (conn != null) ? conn.prepareStatement(sql) : null) {
                if (pst == null) return;
                
                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        logs.add(new SystemLog(
                            rs.getString("username"),
                            rs.getString("role"),
                            rs.getString("timestamp")
                        ));
                    }
                }
                addressTable.setItems(logs);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    //redirects user to login page when logout button is clicked
    public void handleExit() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnExit.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            System.out.println("User logged out. Redirected to login page.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: Could not load login page.");
        }
    }

    /**
     * handleResidents - Navigate to the Residents page
     */
    @FXML
    public void handleResidents(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Residentspage.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) btnResidents.getScene().getWindow();
            Scene scene = new Scene(root);
            
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            
            System.out.println("Navigated to Residents page.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: Could not load residents page.");
        }
    }

    /**
     * handleAddresses - Navigate to the Addresses page
     */
    @FXML
    public void handleAddresses(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Addresses.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) btnAddresses.getScene().getWindow();
            Scene scene = new Scene(root);
            
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            
            System.out.println("Navigated to Addresses page.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: Could not load addresses page.");
        }
    }

    /**
     * handleDashboard - Navigate back to the Welcome/Dashboard page
     */
    @FXML
    public void handleDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Welcomepage.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) btnDashboard.getScene().getWindow();
            Scene scene = new Scene(root);
            
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            
            System.out.println("Navigated back to Dashboard page.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: Could not load dashboard page.");
        }
    }

    /**
     * handleDocuments - Navigate to the Documents page
     */
    @FXML
    public void handleDocuments(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Documents.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) btnDocuments.getScene().getWindow();
            Scene scene = new Scene(root);
            
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            
            System.out.println("Navigated to Documents page.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: Could not load documents page.");
        }
    }

    /**
     * handleContact - Navigate to the Contact page
     */
    @FXML
    public void handleContact(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Contact.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) Contactbtn.getScene().getWindow();
            Scene scene = new Scene(root);
            
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            
            System.out.println("Navigated to Contact page.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: Could not load contact page.");
        }
    }

    /**
     * handleLogs - Keeps user on the Logs page
     */
    @FXML
    public void handleLogs(ActionEvent event) {
        setButtonAsSelected(Logsbtn);
        System.out.println("Logs button selected - refreshing page.");
    }

    /**
     * handleReports - Navigate to the Reports page
     */
    @FXML
    public void handleReports(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Reports.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            System.out.println("Navigated to Reports page.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: Could not load Reports page.");
        }
    }

    /**
     * handleUsersandRoles - Navigate to the Users and Roles page
     */
    @FXML
    public void handleUsersandRoles(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("UsersandRoles.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            System.out.println("Navigated to Users and Roles page.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: Could not load Users and Roles page.");
        }
    }

    /**
     * onButtonHoverEnter - Darkens button on mouse enter
     */
    @FXML
    public void onButtonHoverEnter(MouseEvent event) {
        Button button = (Button) event.getSource();
        if (button != currentSelectedButton) {
            button.setStyle(button.getStyle().replace("rgba(255, 255, 255, 0.2)", "rgba(255, 255, 255, 0.4)"));
        }
    }

    /**
     * onButtonHoverExit - Restores button on mouse exit
     */
    @FXML
    public void onButtonHoverExit(MouseEvent event) {
        Button button = (Button) event.getSource();
        if (button != currentSelectedButton) {
            button.setStyle(button.getStyle().replace("rgba(255, 255, 255, 0.4)", "rgba(255, 255, 255, 0.2)"));
        }
    }

    /**
     * setButtonAsSelected - Sets a button as selected with special styling
     */
    private void setButtonAsSelected(Button button) {
        if (currentSelectedButton != null) {
            currentSelectedButton.setStyle(currentSelectedButton.getStyle().replace("rgba(150, 150, 150, 0.4)", "rgba(255, 255, 255, 0.2)"));
        }
        
        if (button != null) {
            button.setStyle(button.getStyle().replace("rgba(255, 255, 255, 0.2)", "rgba(150, 150, 150, 0.4)"));
            currentSelectedButton = button;
        }
    }
}
