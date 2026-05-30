import javafx.fxml.FXML;
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
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;

public class ContactController {
    
    // FXML-linked TableView and TableColumn fields for contact data display
    @FXML private TableView<AddResidentDialogController> addressTable;
    @FXML private TableColumn<AddResidentDialogController, String> colName;
    @FXML private TableColumn<AddResidentDialogController, String> colContact;
    @FXML private TableColumn<AddResidentDialogController, Void> colActions;
    
    // Navigation button bindings from sidebar
    @FXML private Button btnDashboard;
    @FXML private Button btnResidents;
    @FXML private Button btnAddresses;
    @FXML private Button btnDocuments;
    @FXML private Button Contactbtn;
    @FXML private Button btnExit;
    
    // Track which button is selected for styling
    private Button currentSelectedButton = null;

    @FXML
    public void initialize() {
        // Set Contact button as selected since we're on the Contact page
        setButtonAsSelected(Contactbtn);
        
        // Null checks and PropertyValueFactory bindings for table columns
        if (colName != null) colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (colContact != null) colContact.setCellValueFactory(new PropertyValueFactory<>("contact"));

        // Create the interactive custom action button cell factory
        if (colActions != null) {
            Callback<TableColumn<AddResidentDialogController, Void>, TableCell<AddResidentDialogController, Void>> cellFactory = new Callback<>() {
                @Override
                public TableCell<AddResidentDialogController, Void> call(final TableColumn<AddResidentDialogController, Void> param) {
                    return new TableCell<>() {
                        // Create three action buttons (View, Edit, Delete)
                        private final Button btnView = new Button("👁");
                        private final Button btnEdit = new Button("✏");
                        private final Button btnDelete = new Button("🗑");
                        // Container HBox to hold all action buttons in a row
                        private final HBox container = new HBox(8, btnView, btnEdit, btnDelete);

                        {
                            // Style each button with background color and hand cursor
                            btnView.setStyle("-fx-background-color: #f1f5f9; -fx-cursor: hand; -fx-background-radius: 4;");
                            btnEdit.setStyle("-fx-background-color: #f1f5f9; -fx-cursor: hand; -fx-background-radius: 4;");
                            btnDelete.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-background-radius: 4;");

                            // Add click event handlers for each button
                            btnView.setOnAction(event -> System.out.println("Viewing contact: " + getTableView().getItems().get(getIndex()).getName()));
                            btnEdit.setOnAction(event -> System.out.println("Editing contact: " + getTableView().getItems().get(getIndex()).getName()));
                            btnDelete.setOnAction(event -> handleDeleteContact(getTableView().getItems().get(getIndex())));
                            
                            // Center align buttons within container
                            container.setStyle("-fx-alignment: CENTER;");
                        }

                        // Override updateItem method to display buttons only for non-empty rows
                        @Override
                        protected void updateItem(Void item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty) {
                                // Don't display buttons for empty rows
                                setGraphic(null);
                            } else {
                                // Display button container for rows with data
                                setGraphic(container);
                            }
                        }
                    };
                }
            };

            // Apply cell factory to actions column
            colActions.setCellFactory(cellFactory);
        }

        // Load sample contact data into the table
        loadContactData();
    }

    /**
     * loadContactData - Loads sample contact data into the table
     */
    private void loadContactData() {
        if (addressTable != null) {
            ObservableList<AddResidentDialogController> contacts = FXCollections.observableArrayList();
            String sql = "SELECT full_name, contact, address, classification FROM residents WHERE classification != 'Admin'";
            
            try (Connection conn = Database.connect();
                 PreparedStatement pst = conn.prepareStatement(sql);
                 ResultSet rs = pst.executeQuery()) {
                
                while (rs.next()) {
                    contacts.add(new AddResidentDialogController(
                        rs.getString("full_name"),
                        rs.getString("contact"),
                        rs.getString("address"),
                        rs.getString("classification")
                    ));
                }
                addressTable.setItems(contacts);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * handleDeleteContact - Deletes a resident's contact entry from the database
     * @param contact The contact object to delete
     */
    private void handleDeleteContact(AddResidentDialogController contact) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Contact Entry for: " + contact.getName());
        alert.setContentText("Are you sure you want to delete this contact entry?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "DELETE FROM residents WHERE full_name = ?";
            try (Connection conn = Database.connect();
                 PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setString(1, contact.getName());
                pst.executeUpdate();
                System.out.println("Deleted contact entry for: " + contact.getName());
                loadContactData(); // Refresh table
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Event handler for Exit button - redirects to login page
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
     * handleContact - Triggered when the CONTACT button is clicked
     * Keeps the user on the contact page
     */
    @FXML
    public void handleContact(ActionEvent event) {
        setButtonAsSelected(Contactbtn);
        System.out.println("Contact button selected - refreshing page.");
    }

    /**
     * handleDashboard - Triggered when the DASHBOARD button is clicked
     * Navigates back to the welcome/dashboard page
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
     * handleResidents - Triggered when the RESIDENTS button is clicked
     * Navigates to the residents page
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
     * handleAddresses - Triggered when the ADDRESSES button is clicked
     * Navigates to the addresses page
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
     * handleDocuments - Triggered when the DOCUMENTS button is clicked
     * Navigates to the documents page
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
     * onButtonHoverEnter - Triggered when the mouse enters a button
     * Darkens the button background to indicate hover state
     */
    @FXML
    public void onButtonHoverEnter(MouseEvent event) {
        Button button = (Button) event.getSource();
        // Only darken on hover if it's not the currently selected button
        if (button != currentSelectedButton) {
            button.setStyle(button.getStyle().replace("rgba(255, 255, 255, 0.2)", "rgba(255, 255, 255, 0.4)"));
        }
    }

    /**
     * onButtonHoverExit - Triggered when the mouse exits a button
     * Restores the button background to normal state
     */
    @FXML
    public void onButtonHoverExit(MouseEvent event) {
        Button button = (Button) event.getSource();
        // Only restore if it's not the currently selected button
        if (button != currentSelectedButton) {
            button.setStyle(button.getStyle().replace("rgba(255, 255, 255, 0.4)", "rgba(255, 255, 255, 0.2)"));
        }
    }

    /**
     * setButtonAsSelected - Sets a button as the currently selected button with darkened styling
     * Also restores the previously selected button to normal styling
     */
    private void setButtonAsSelected(Button button) {
        // Restore previous selected button to normal style
        if (currentSelectedButton != null) {
            currentSelectedButton.setStyle(currentSelectedButton.getStyle().replace("rgba(150, 150, 150, 0.4)", "rgba(255, 255, 255, 0.2)"));
        }
        
        // Set new button as selected with darkened style
        if (button != null) {
            button.setStyle(button.getStyle().replace("rgba(255, 255, 255, 0.2)", "rgba(150, 150, 150, 0.4)"));
            currentSelectedButton = button;
        }
    }
}
