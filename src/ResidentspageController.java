import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableCell;
import javafx.scene.control.Button;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Callback;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;

public class ResidentspageController {

    // ADDED: ComboBox for filtering by classification
    @FXML
    private ComboBox<String> classificationDrop;

    // ADDED: Table view bindings for resident data display
    @FXML private TableView<AddResidentDialogController> residentTable;
    @FXML private TableColumn<AddResidentDialogController, String> colName;
    @FXML private TableColumn<AddResidentDialogController, String> colContact;
    @FXML private TableColumn<AddResidentDialogController, String> colAddress;
    @FXML private TableColumn<AddResidentDialogController, String> colClassification;
    @FXML private TableColumn<AddResidentDialogController, Void> colActions;
    @FXML private Button btnDashboard;
    @FXML private Button btnResidents;
    @FXML private Button btnAddresses;
    @FXML private Button btnDocuments;
    @FXML private Button btnContact;
    @FXML private Button btnExit;
    
    // ADDED: Search field for Quick Access "Search Resident" feature
    // KEYPOINT: Mapped the text field from FXML so we can apply focus to it
    @FXML private TextField searchField;

    // EDITED: Added currentSelectedButton field to track which button is selected (button selection feature)
    private Button currentSelectedButton = null;

    @FXML
    public void initialize() {
        // EDITED: Set Residents button as selected since we're on the Residents page (button selection feature)
        setButtonAsSelected(btnResidents);
        // 2. Create the list of classification choices
        ObservableList<String> options = FXCollections.observableArrayList(
            "All Classifications",
            "Male",
            "Female",
            "Senior",
            "Minor"
        );

        // 3. Inject the options list into the ComboBox dropdown node
        if (classificationDrop != null) {
            classificationDrop.setItems(options);
            // ADDED: Listener to trigger filtering whenever a classification is selected
            classificationDrop.setOnAction(event -> loadResidentData(classificationDrop.getValue()));
        }

        // ADDED: Null checks before initializing table columns to prevent NullPointerException
        // Map columns to variables inside Resident.java model
        if (colName != null) colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (colContact != null) colContact.setCellValueFactory(new PropertyValueFactory<>("contact"));
        if (colAddress != null) colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        if (colClassification != null) colClassification.setCellValueFactory(new PropertyValueFactory<>("classification"));

        // KEYPOINT: Load real data from MySQL instead of sample data
        loadResidentData("All Classifications");

        // ADDED: Null check for colActions before creating and applying cell factory
        // Create the interactive custom action button cell factory
        if (colActions != null) {
            Callback<TableColumn<AddResidentDialogController, Void>, TableCell<AddResidentDialogController, Void>> cellFactory = new Callback<>() {
                @Override
                public TableCell<AddResidentDialogController, Void> call(final TableColumn<AddResidentDialogController, Void> param) {
                    return new TableCell<>() {
                        private final Button btnView = new Button("👁");
                        private final Button btnEdit = new Button("✏");
                        private final Button btnDelete = new Button("🗑");
                        private final HBox container = new HBox(8, btnView, btnEdit, btnDelete);

                        {
                            // Styling the buttons
                            btnView.setStyle("-fx-background-color: #f1f5f9; -fx-cursor: hand; -fx-background-radius: 4;");
                            btnEdit.setStyle("-fx-background-color: #f1f5f9; -fx-cursor: hand; -fx-background-radius: 4;");
                            btnDelete.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-background-radius: 4;");

                            btnView.setOnAction(event -> System.out.println("Viewing details for: " + getTableView().getItems().get(getIndex()).getName()));
                            btnEdit.setOnAction(event -> System.out.println("Opening edit form for: " + getTableView().getItems().get(getIndex()).getName()));
                            btnDelete.setOnAction(event -> handleDeleteResident(getTableView().getItems().get(getIndex())));
                            
                            container.setStyle("-fx-alignment: CENTER;");
                        }

                        @Override
                        protected void updateItem(Void item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty) {
                                setGraphic(null);
                            } else {
                                setGraphic(container);
                            }
                        }
                    };
                }
            };

            // ADDED: Apply cell factory to actions column
            colActions.setCellFactory(cellFactory);
        }
    }

    /**
     * loadResidentData - Fetches all residents from the database and populates the table
     * Updated to support filtering by classification category.
     * @param filter The classification string to filter by (e.g., "Male", "Female").
     */
    private void loadResidentData(String filter) {
        ObservableList<AddResidentDialogController> residents = FXCollections.observableArrayList();
        
        String sql = "SELECT full_name, contact, address, classification FROM residents WHERE classification != 'Admin'";
        boolean hasFilter = filter != null && !filter.equals("All Classifications");

        if (hasFilter) {
            // If a specific filter is used, we append it to our existing WHERE clause
            sql += " AND classification = ?";
        }
        sql += " ORDER BY full_name ASC";
        
        try (Connection conn = Database.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            if (hasFilter) {
                pst.setString(1, filter);
            }
            
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    residents.add(new AddResidentDialogController(
                        rs.getString("full_name"),
                        rs.getString("contact"),
                        rs.getString("address"),
                        rs.getString("classification")
                    ));
                }
                residentTable.setItems(residents);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * handleDeleteResident - Deletes a resident from the database after confirmation
     * @param resident The resident object to delete
     */
    private void handleDeleteResident(AddResidentDialogController resident) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Resident: " + resident.getName());
        alert.setContentText("Are you sure you want to delete this resident? This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "DELETE FROM residents WHERE full_name = ?";
            try (Connection conn = Database.connect();
                 PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setString(1, resident.getName());
                pst.executeUpdate();
                System.out.println("Deleted resident: " + resident.getName());
                loadResidentData(classificationDrop.getValue()); // Refresh table
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // ADDED: Event handler for Exit button - redirects to login page
    @FXML
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
     * handleDashboard - Triggered when the DASHBOARD button is clicked
     * Navigates back to the welcome/dashboard page from the residents page
     * 
     * @param event The ActionEvent from the DASHBOARD button click
     */
    @FXML
    public void handleDashboard(ActionEvent event) {
        try {
            // Load the welcome page FXML
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
            
            System.out.println("Navigated back to Dashboard page.");
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

    // ADDED: Clean popup launcher block for Add Resident modal dialog
    @FXML
    private void showAddResidentModal(ActionEvent event) {
        try {
            // 1. Load the separate dialog FXML layout
            FXMLLoader loader = new FXMLLoader(getClass().getResource("AddResidentDialog.fxml"));
            Parent root = loader.load();
            
            // 2. Create a clean secondary window layer (Stage)
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add New Resident Record");
            
            // 3. Make it a Modal popup (Locks the main window behind it until closed)
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.UTILITY); // Minimalist window bar frame style
            dialogStage.setResizable(false);
            
            // 4. Set the scene canvas context and display it
            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            
            // KEYPOINT: showAndWait() pauses execution here until the modal is closed.
            dialogStage.showAndWait(); 
            
            // After the modal closes, refresh the table to show the new resident
            loadResidentData(classificationDrop.getValue());
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: Could not pop open the Add Resident window layout form.");
        }
    }

    // ADDED: Event handler for Residents button in sidebar - displays resident management view
    @FXML
    private void handleResidentsButton(ActionEvent event) {
        System.out.println("Residents button clicked - Loading residents view");
        // Future: Add logic to refresh resident data, filter table, or navigate to residents management
    }

    /**
     * ADDED LINE 204-230: handleAddressesButton - Triggered when the ADDRESSES button is clicked
     * Navigates to the addresses management page from the residents page
     * 
     * @param event The ActionEvent from the ADDRESSES button click
     */
    @FXML
    private void handleAddressesButton(ActionEvent event) {
        try {
            // ADDED LINE 211: Load the addresses page FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Addresses.fxml"));
            Parent root = loader.load();
            
            // ADDED LINE 214: Get the current window (stage) from the button
            Stage stage = (Stage) btnDashboard.getScene().getWindow();
            
            // ADDED LINE 216: Create a new scene with the addresses page
            Scene scene = new Scene(root);
            
            // ADDED LINE 218-220: Set the scene and show it, keep maximized
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            
            System.out.println("Navigated to Addresses page.");
        } catch (Exception e) {
            // ADDED LINE 224-225: Print error details if navigation fails
            e.printStackTrace();
            System.out.println("Error: Could not load addresses page.");
        }
    }

    /**
     * handleDocuments - Triggered when the DOCUMENTS button is clicked
     * Navigates from the residents page to the documents page
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
     * Navigates from the residents page to the contact page
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
     * handleReports - Navigate to the Reports page (works from any controller/page)
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
            System.out.println("Error: Could not load reports page.");
        }
    }

    /**
     * handleUsersandRoles - Navigate to the Users and Roles page (works from any controller/page)
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
            System.out.println("Error: Could not load users and roles page.");
        }
    }

    /**
     * handleLogs - Navigate to the Logs page (works from any controller/page)
     */
    @FXML
    public void handleLogs(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Logs.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
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
     * focusSearchBar - Focuses the search text field
     * KEYPOINT: This is a public method. It allows external controllers (like WelcomepageController) to trigger actions in this view. This is how you pass data/commands between pages!
     * Used when navigating from the Dashboard's Quick Access "Search Resident" button
     */
    public void focusSearchBar() {
        if (searchField != null) {
            searchField.requestFocus();
        }
    }
}