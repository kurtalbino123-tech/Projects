import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.TableView;
import javafx.scene.control.Button;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.scene.control.Label;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javafx.scene.control.cell.PropertyValueFactory;

public class WelcomepageController {

    // FXML-linked UI components from Welcomepage.fxml
    @FXML private PieChart ageGroupChart;
    @FXML private TableView<AddResidentDialogController> residentTable; 
    @FXML private javafx.scene.control.TableColumn<AddResidentDialogController, String> colRecentName;
    @FXML private javafx.scene.control.TableColumn<AddResidentDialogController, String> colRecentClass;

    // KEYPOINT: Added Label bindings for dashboard statistic cards
    @FXML private Label lblTotalResidents;
    @FXML private Label lblMaleResidents;
    @FXML private Label lblFemaleResidents;
    @FXML private Label lblSeniorResidents;
    @FXML private Label lblMinorResidents;

    @FXML private Button btnDashboard;        // Dashboard button to stay on welcome page
    @FXML private Button btnExit;             // Exit button to return to login page
    @FXML private Button btnResidents;        // Residents button to navigate to residents page
    @FXML private Button btnAddresses;        // Addresses button to navigate to addresses page
    @FXML private Button btnDocuments;        // Documents button to navigate to documents page
    @FXML private Button btnContact;          // Contact button to navigate to contact page
    @FXML private Button Reportsbtn;          // Reports button to navigate to reports page
    @FXML private Button UsersandRolesbtn;    // Users and Roles button to navigate to users and roles page
    @FXML private Button Logsbtn;             // Logs button to navigate to logs page
    
    // Quick Access buttons
    // KEYPOINT: Defined the Quick Access buttons matching the fx:id attributes in the FXML so Java can reference them
    @FXML private Button btnQuickAddResident;
    @FXML private Button btnQuickSearchResident;
    @FXML private Button btnQuickViewAddresses;
    @FXML private Button btnQuickAddAdmin;
    @FXML private Button btnQuickViewDocuments;

    // EDITED: Added currentSelectedButton field to track which button is selected (button selection feature)
    private Button currentSelectedButton = null;

    @FXML
    public void initialize() {
        // EDITED: Set Dashboard button as selected since we're on the Welcome/Dashboard page (button selection feature)
        setButtonAsSelected(btnDashboard);

        // Setup the Recent Residents table columns
        if (colRecentName != null) colRecentName.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (colRecentClass != null) colRecentClass.setCellValueFactory(new PropertyValueFactory<>("classification"));

        // KEYPOINT: Refresh the PieChart data from the database
        updateDashboardStats();
        loadRecentResidents();
    }

    /**
     * updateDashboardStats - Queries the database to count residents by category and updates the PieChart.
     * KEYPOINT: We group by 'classification' and exclude 'Admin' so only resident data is visualized.
     */
    private void updateDashboardStats() {
        String sql = "SELECT classification, COUNT(*) as count FROM residents WHERE classification != 'Admin' GROUP BY classification";
        int total = 0, male = 0, female = 0, senior = 0, minor = 0;

        try (Connection conn = Database.connect();
             PreparedStatement pst = (conn != null) ? conn.prepareStatement(sql) : null) {
            
            if (pst == null) {
                System.err.println("Database connection failed. Dashboard PieChart not updated.");
                return;
            }

            try (ResultSet rs = pst.executeQuery()) {
                ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

                while (rs.next()) {
                    String classification = rs.getString("classification");
                    int count = rs.getInt("count");
                    
                    // KEYPOINT: Aggregate counts for the statistic labels
                    total += count;
                    if ("Male".equalsIgnoreCase(classification)) male = count;
                    else if ("Female".equalsIgnoreCase(classification)) female = count;
                    else if ("Senior".equalsIgnoreCase(classification)) senior = count;
                    else if ("Minor".equalsIgnoreCase(classification)) minor = count;

                    // Update PieChart slices
                    pieChartData.add(new PieChart.Data(classification, count));
                }

                if (ageGroupChart != null) {
                    ageGroupChart.setData(pieChartData);
                }

                // KEYPOINT: Set the text for each dashboard card label
                if (lblTotalResidents != null) lblTotalResidents.setText(String.valueOf(total));
                if (lblMaleResidents != null) lblMaleResidents.setText(String.valueOf(male));
                if (lblFemaleResidents != null) lblFemaleResidents.setText(String.valueOf(female));
                if (lblSeniorResidents != null) lblSeniorResidents.setText(String.valueOf(senior));
                if (lblMinorResidents != null) lblMinorResidents.setText(String.valueOf(minor));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRecentResidents() {
        if (residentTable != null) {
            Connection conn = Database.connect();
            if (conn == null) {
                System.err.println("Database connection failed. Recent residents table not loaded.");
                return;
            }

            ObservableList<AddResidentDialogController> recentResidents = FXCollections.observableArrayList();
            // KEYPOINT: Fetch the latest 5 residents
            String sql = "SELECT full_name, contact, address, classification FROM residents WHERE classification != 'Admin' ORDER BY username_ref DESC LIMIT 5";
            
            try (PreparedStatement pst = conn.prepareStatement(sql);
                 ResultSet rs = pst.executeQuery()) {
                
                while (rs.next()) {
                    recentResidents.add(new AddResidentDialogController(
                        rs.getString("full_name"),
                        rs.getString("contact"),
                        rs.getString("address"),
                        rs.getString("classification")
                    ));
                }
                residentTable.setItems(recentResidents);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try { conn.close(); } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }

    /**
     * handleExit - Triggered when the EXIT button is clicked
     * Logs out the current user and navigates back to the login page
     * 
     * @param event The ActionEvent from the EXIT button click
     */
    @FXML
    public void handleExit(ActionEvent event) {
        try {
            // Load the login page FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
            Parent root = loader.load();
            
            // Get the current window (stage) from the Exit button
            Stage stage = (Stage) btnExit.getScene().getWindow();
            
            // Create a new scene with the login page
            Scene scene = new Scene(root);
            
            // Set the scene and show it
            stage.setScene(scene);
            stage.setMaximized(true); // Keep window maximized
            stage.show();
            
            System.out.println("User logged out successfully. Redirected to login page.");
        } catch (Exception e) {
            // Print error details if navigation fails
            e.printStackTrace();
            System.out.println("Error: Could not load login page.");
        }
    }

    /**
     * handleResidents - Triggered when the RESIDENTS button is clicked
     * Navigates from the welcome page to the residents page
     * 
     * @param event The ActionEvent from the RESIDENTS button click
     */
    @FXML
    public void handleResidents(ActionEvent event) {
        try {
            // Load the residents page FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Residentspage.fxml"));
            Parent root = loader.load();
            
            // Get the current window (stage) from the Residents button
            Stage stage = (Stage) btnResidents.getScene().getWindow();
            
            // Create a new scene with the residents page
            Scene scene = new Scene(root);
            
            // Set the scene and show it
            stage.setScene(scene);
            stage.setMaximized(true); // Keep window maximized
            stage.show();
            
            System.out.println("Navigated to Residents page.");
        } catch (Exception e) {
            // Print error details if navigation fails
            e.printStackTrace();
            System.out.println("Error: Could not load residents page.");
        }
    }

    /**
     * handleAddNewAdmin - Opens the Add New Admin dialog modal
     */
    @FXML
    public void handleAddNewAdmin(ActionEvent event) {
        try {
            // KEYPOINT: Point to the actual addnewadmin.fxml file you created
            FXMLLoader loader = new FXMLLoader(getClass().getResource("addnewadmin.fxml"));
            Parent root = loader.load();
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Register New Administrator");
            
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.UTILITY); 
            dialogStage.setResizable(false);
            
            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: Could not open Add Admin dialog.");
        }
    }

    /**
     * handleQuickSearchResident - Triggered when the Quick Access "Search Resident" button is clicked
     * Navigates to the residents page and focuses the search bar
     * 
     * @param event The ActionEvent from the Search Resident button click
     */
    @FXML
    public void handleQuickSearchResident(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Residentspage.fxml"));
            Parent root = loader.load();
            
            // Get the controller and focus the search bar
            ResidentspageController controller = loader.getController();
            
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            
            // Focus the search bar after the scene is shown
            // KEYPOINT: Used Platform.runLater to delay the focus request until AFTER the UI finishes rendering the new scene. Without this, it might try to focus a field that hasn't finished drawing yet!
            Platform.runLater(() -> controller.focusSearchBar());
            System.out.println("Navigated to Residents page and focused search bar.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: Could not load residents page for searching.");
        }
    }

    /**
     * ADDED LINE 73-99: handleAddresses - Triggered when the ADDRESSES button is clicked
     * Navigates from the welcome page to the addresses page
     * 
     * @param event The ActionEvent from the ADDRESSES button click
     */
    @FXML
    public void handleAddresses(ActionEvent event) {
        try {
            // ADDED LINE 79: Load the addresses page FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Addresses.fxml"));
            Parent root = loader.load();
            
            // ADDED LINE 82: Get the current window (stage) from the addresses button
            Stage stage = (Stage) btnAddresses.getScene().getWindow();
            
            // ADDED LINE 84: Create a new scene with the addresses page
            Scene scene = new Scene(root);
            
            // ADDED LINE 86-88: Set the scene and show it, keep window maximized
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            
            System.out.println("Navigated to Addresses page.");
        } catch (Exception e) {
            // ADDED LINE 92-93: Print error details if navigation fails
            e.printStackTrace();
            System.out.println("Error: Could not load addresses page.");
        }
    }

    /**
     * handleDocuments - Triggered when the DOCUMENTS button is clicked
     * Navigates from the welcome page to the documents page
     * 
     * @param event The ActionEvent from the DOCUMENTS button click
     */
    @FXML
    public void handleDocuments(ActionEvent event) {
        try {
            // Load the documents page FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Documents.fxml"));
            Parent root = loader.load();
            
            // Get the current window (stage) from the documents button
            Stage stage = (Stage) btnDocuments.getScene().getWindow();
            
            // Create a new scene with the documents page
            Scene scene = new Scene(root);
            
            // Set the scene and show it, keep window maximized
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            
            System.out.println("Navigated to Documents page.");
        } catch (Exception e) {
            // Print error details if navigation fails
            e.printStackTrace();
            System.out.println("Error: Could not load documents page.");
        }
    }

    /**
     * handleContact - Triggered when the CONTACT button is clicked
     * Navigates from the welcome page to the contact page
     * 
     * @param event The ActionEvent from the CONTACT button click
     */
    @FXML
    public void handleContact(ActionEvent event) {
        try {
            // Load the contact page FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Contact.fxml"));
            Parent root = loader.load();
            
            // Get the current window (stage) from the contact button
            Stage stage = (Stage) btnContact.getScene().getWindow();
            
            // Create a new scene with the contact page
            Scene scene = new Scene(root);
            
            // Set the scene and show it, keep window maximized
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            
            System.out.println("Navigated to Contact page.");
        } catch (Exception e) {
            // Print error details if navigation fails
            e.printStackTrace();
            System.out.println("Error: Could not load contact page.");
        }
    }

    /**
     * handleDashboard - Triggered when the DASHBOARD button is clicked
     * Keeps the user on the welcome/dashboard page (refreshes it)
     * 
     * @param event The ActionEvent from the DASHBOARD button click
     */
    @FXML
    public void handleDashboard(ActionEvent event) {
        try {
            // Load the welcome page FXML (same as current page, essentially a refresh)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Welcomepage.fxml"));
            Parent root = loader.load();
            
            // Get the current window (stage) from the Dashboard button
            Stage stage = (Stage) btnDashboard.getScene().getWindow();
            
            // Create a new scene with the welcome page
            Scene scene = new Scene(root);
            
            // Set the scene and show it
            stage.setScene(scene);
            stage.setMaximized(true); // Keep window maximized
            stage.show();
            
            System.out.println("Refreshed Dashboard page.");
        } catch (Exception e) {
            // Print error details if navigation fails
            e.printStackTrace();
            System.out.println("Error: Could not load dashboard page.");
        }
    }

    /**
     * onButtonHoverEnter - Triggered when the mouse enters a button
     * Darkens the button background to indicate hover state
     * EDITED: Modified to skip hover effect on selected button (button selection feature)
     * 
     * @param event The MouseEvent from the button hover
     */
    @FXML
    public void onButtonHoverEnter(MouseEvent event) {
        Button button = (Button) event.getSource();
        // EDITED: Only darken on hover if it's not the currently selected button
        if (button != currentSelectedButton) {
            button.setStyle(button.getStyle().replace("rgba(255, 255, 255, 0.2)", "rgba(255, 255, 255, 0.4)"));
        }
    }

    /**
     * onButtonHoverExit - Triggered when the mouse exits a button
     * Restores the button background to normal state
     * EDITED: Modified to skip restoration on selected button (button selection feature)
     * 
     * @param event The MouseEvent from the button hover exit
     */
    @FXML
    public void onButtonHoverExit(MouseEvent event) {
        Button button = (Button) event.getSource();
        // EDITED: Only restore if it's not the currently selected button
        if (button != currentSelectedButton) {
            button.setStyle(button.getStyle().replace("rgba(255, 255, 255, 0.4)", "rgba(255, 255, 255, 0.2)"));
        }
    }

    /**
     * EDITED: Added new setButtonAsSelected() method to handle button selection styling (button selection feature)
     * Sets a button as the currently selected button with darkened styling
     * Also restores the previously selected button to normal styling
     * 
     * @param button The button to set as selected
     */
    private void setButtonAsSelected(Button button) {
        // EDITED: Restore previous selected button to normal style
        if (currentSelectedButton != null) {
            currentSelectedButton.setStyle(currentSelectedButton.getStyle().replace("rgba(150, 150, 150, 0.4)", "rgba(255, 255, 255, 0.2)"));
        }
        
        // EDITED: Set new button as selected with darkened style
        if (button != null) {
            button.setStyle(button.getStyle().replace("rgba(255, 255, 255, 0.2)", "rgba(150, 150, 150, 0.4)"));
            currentSelectedButton = button;
        }
    }

    /**
     * handleReports - Navigate to the Reports page
     */
    @FXML
    public void handleReports(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Reports.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) Reportsbtn.getScene().getWindow();
            Scene scene = new Scene(root);
            
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            
            System.out.println("Navigated to Reports page.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: Could not load reports page.");
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
            
            Stage stage = (Stage) UsersandRolesbtn.getScene().getWindow();
            Scene scene = new Scene(root);
            
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            
            System.out.println("Navigated to Users and Roles page.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: Could not load users and roles page.");
        }
    }

    /**
     * handleLogs - Navigate to the Logs page
     */
    @FXML
    public void handleLogs(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Logs.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) Logsbtn.getScene().getWindow();
            Scene scene = new Scene(root);
            
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            
            System.out.println("Navigated to Logs page.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: Could not load logs page.");
        }
    }
}