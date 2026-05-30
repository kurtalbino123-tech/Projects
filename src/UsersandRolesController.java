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
import java.util.Optional;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

/**
 * UsersandRolesController - Manages the Users and Roles page functionality
 * Displays users with their assigned roles and action buttons
 */
public class UsersandRolesController {

    // FXML-linked TableView and TableColumn fields for users and roles data display
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
    @FXML private Button Reportsbtn;
    @FXML private Button UsersandRolesbtn;
    @FXML private Button Logsbtn;
    @FXML private Button btnExit;
    
    // Tracking for button selection
    private Button currentSelectedButton = null;

    @FXML
    public void initialize() {
        // Set Users and Roles button as selected since we're on the Users and Roles page
        setButtonAsSelected(UsersandRolesbtn);
        
        // Bind table columns to data properties
        // KEYPOINT: Mapping to 'name' and 'classification' properties of the model
        if (colName != null) colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (colContact != null) colContact.setCellValueFactory(new PropertyValueFactory<>("classification"));

        // Setup Action Buttons cell factory
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
                            btnView.setStyle("-fx-background-color: #f1f5f9; -fx-cursor: hand; -fx-background-radius: 4;");
                            btnEdit.setStyle("-fx-background-color: #f1f5f9; -fx-cursor: hand; -fx-background-radius: 4;");
                            btnDelete.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-background-radius: 4;");

                            btnView.setOnAction(event -> System.out.println("Viewing: " + getTableView().getItems().get(getIndex()).getName()));
                            btnEdit.setOnAction(event -> System.out.println("Editing: " + getTableView().getItems().get(getIndex()).getName()));
                            btnDelete.setOnAction(event -> handleDeleteUser(getTableView().getItems().get(getIndex())));
                            
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
            colActions.setCellFactory(cellFactory);
        }

        // Load sample user data into the table
        loadUserData();
    }

    /**
     * loadUserData - Loads sample user data into the table
     */
    private void loadUserData() {
        if (addressTable != null) {
            ObservableList<AddResidentDialogController> users = FXCollections.observableArrayList();
            // KEYPOINT: Fetch both Admins and Residents for this management page
            String sql = "SELECT full_name, contact, address, classification FROM residents ORDER BY full_name ASC";
            
            try (Connection conn = Database.connect();
                 PreparedStatement pst = (conn != null) ? conn.prepareStatement(sql) : null) {
                
                if (pst == null) return;

                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        users.add(new AddResidentDialogController(
                            rs.getString("full_name"),
                            rs.getString("contact"),
                            rs.getString("address"),
                            rs.getString("classification")
                        ));
                    }
                }
                addressTable.setItems(users);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * handleDeleteUser - Deletes a user and their profile from the system
     */
    private void handleDeleteUser(AddResidentDialogController user) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete User: " + user.getName());
        alert.setContentText("This will permanently remove the user from the system. Proceed?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = Database.connect()) {
                if (conn == null) return;
                conn.setAutoCommit(false); // KEYPOINT: Use transaction for dual-table deletion

                // 1. Get the username_ref
                String findRef = "SELECT username_ref FROM residents WHERE full_name = ?";
                String usernameRef = "";
                try (PreparedStatement pst = conn.prepareStatement(findRef)) {
                    pst.setString(1, user.getName());
                    ResultSet rs = pst.executeQuery();
                    if (rs.next()) usernameRef = rs.getString("username_ref");
                }

                // 2. Delete from both tables
                String delUser = "DELETE FROM users WHERE username = ?";
                String delRes = "DELETE FROM residents WHERE full_name = ?";
                
                try (PreparedStatement pstU = conn.prepareStatement(delUser);
                     PreparedStatement pstR = conn.prepareStatement(delRes)) {
                    pstU.setString(1, usernameRef);
                    pstR.setString(1, user.getName());
                    pstU.executeUpdate();
                    pstR.executeUpdate();
                    conn.commit();
                    loadUserData(); // Refresh table
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

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
     * handleUsersandRoles - Keeps user on the Users and Roles page
     */
    @FXML
    public void handleUsersandRoles(ActionEvent event) {
        setButtonAsSelected(UsersandRolesbtn);
        System.out.println("Users and Roles button selected - refreshing page.");
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
     * handleLogs - Navigate to the Logs page
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
            System.out.println("Error: Could not load Logs page.");
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
