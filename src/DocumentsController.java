// JavaFX and SQL imports
import javafx.fxml.FXML;  // Allows binding FXML elements to Java code
import javafx.scene.control.TableColumn;  // Column in a TableView
import javafx.scene.control.TableView;  // Table display control
import javafx.scene.control.TableCell;  // Individual cell in a table
import javafx.scene.control.Button;  // Button control for user interaction
import javafx.scene.control.cell.PropertyValueFactory;  // Maps table columns to object properties
import javafx.scene.layout.HBox;  // Horizontal layout container
import javafx.collections.FXCollections;  // Utilities for observable collections
import javafx.collections.ObservableList;  // Dynamic list that notifies observers of changes
import javafx.util.Callback;  // Functional interface for callbacks
import javafx.stage.Stage;  // Application window
import javafx.scene.Scene;  // Container for all visual elements in a window
import javafx.scene.Parent;  // Base class for all JavaFX nodes
import javafx.fxml.FXMLLoader;  // Loads FXML files and creates Java objects
import javafx.event.ActionEvent;  // Event triggered by user actions (button clicks)
import javafx.scene.input.MouseEvent;  // Event triggered by mouse movements
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

// Controller for managing the administrative Documents page
public class DocumentsController {

    // FXML Table Components
    @FXML private TableView<Address> addressTable;
    @FXML private TableColumn<Address, String> colName;
    @FXML private TableColumn<Address, String> colAddress;
    @FXML private TableColumn<Address, String> colStatus;
    @FXML private TableColumn<Address, Void> colActions;
    
    // Navigation Buttons
    @FXML private Button btnDashboard;
    @FXML private Button btnResidents;
    @FXML private Button btnAddresses;
    @FXML private Button btnDocuments;
    @FXML private Button btnContact;
    @FXML private Button btnExit;
    
    private Button currentSelectedButton = null;

    @FXML
    public void initialize() {
        System.out.println("DocumentsController initializing...");
        
        // Highlight current page button
        if (btnDocuments != null) {
            setButtonAsSelected(btnDocuments);
            System.out.println("Documents button highlighted successfully");
        } else {
            System.err.println("ERROR: btnDocuments is null! Button binding failed!");
        }
        
        // Table Column Property Bindings
        if (colName != null) {
            colName.setCellValueFactory(new PropertyValueFactory<>("residentName"));
        }
        
        if (colAddress != null) {
            colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        }

        if (colStatus != null) {
            colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            colStatus.setCellFactory(column -> new TableCell<Address, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) { setText(null); setStyle(""); } else {
                        setText(item);
                        if (item.equalsIgnoreCase("Approved")) {
                            setStyle("-fx-text-fill: #166534; -fx-font-weight: bold;");
                        } else if (item.equalsIgnoreCase("Declined")) {
                            setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                        }
                    }
                }
            });
        }

        // Setup action buttons for row-level interactions
        if (colActions != null) {
            Callback<TableColumn<Address, Void>, TableCell<Address, Void>> cellFactory = new Callback<>() {
                @Override
                public TableCell<Address, Void> call(final TableColumn<Address, Void> param) {
                    return new TableCell<>() {
                        private final Button btnView = new Button("👁");
                        private final Button btnApprove = new Button("✔");
                        private final Button btnDecline = new Button("🗑");
                        private final HBox container = new HBox(8, btnView, btnApprove, btnDecline);

                        {
                            btnView.setStyle("-fx-background-color: #f1f5f9; -fx-cursor: hand; -fx-background-radius: 4;");
                            btnApprove.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-cursor: hand; -fx-background-radius: 4;");
                            btnDecline.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-background-radius: 4;");

                            btnView.setOnAction(event -> handleViewDocument(getTableView().getItems().get(getIndex())));
                            btnApprove.setOnAction(event -> handleUpdateStatus(getTableView().getItems().get(getIndex()), "Approved"));
                            btnDecline.setOnAction(event -> handleUpdateStatus(getTableView().getItems().get(getIndex()), "Declined"));
                            container.setStyle("-fx-alignment: CENTER;");
                        }

                        @Override
                        protected void updateItem(Void item, boolean empty) {
                            super.updateItem(item, empty);
                            setGraphic(empty ? null : container);
                        }
                    };
                }
            };
            colActions.setCellFactory(cellFactory);
        }
        
        loadSampleData();
    }

    // View detailed information about the document request
    private void handleViewDocument(Address document) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Document Request Details");
        alert.setHeaderText("Request from: " + document.getResidentName());
        alert.setContentText("Document Type: " + document.getAddress() + 
                           "\nStatus: " + document.getStatus() + 
                           "\n\nReason for Request:\n" + document.getReason());
        alert.showAndWait();
    }

    // Update request status in database
    private void handleUpdateStatus(Address document, String newStatus) {
        String action = newStatus.equals("Approved") ? "approve" : "decline";
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Action");
        alert.setHeaderText(newStatus + " Request?");
        alert.setContentText("Are you sure you want to " + action + " this document request for " + document.getResidentName() + "?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "UPDATE user_documents SET status = ? WHERE id = ?";

            try (Connection conn = Database.connect();
                 PreparedStatement pst = (conn != null) ? conn.prepareStatement(sql) : null) {
                if (pst != null) {
                    pst.setString(1, newStatus);
                    pst.setInt(2, document.getRequestId());
                    pst.executeUpdate();
                    
                    System.out.println("Status updated to " + newStatus + " for request ID: " + document.getRequestId());
                    loadSampleData(); // Refresh the table to show updated status
                }
            } catch (SQLException e) {
                e.printStackTrace();
                Alert errorAlert = new Alert(Alert.AlertType.ERROR, "Failed to update database: " + e.getMessage());
                errorAlert.show();
            }
        }
    }

    // Remove a document request from the system
    private void handleDeleteDocument(Address document) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Document Entry for: " + document.getResidentName());
        alert.setContentText("Are you sure you want to delete this document entry?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
             String sql = "DELETE d FROM user_documents d JOIN residents r ON d.username_ref = r.username_ref WHERE r.full_name = ? AND d.document_type = ? LIMIT 1";
            
            try (Connection conn = Database.connect();
                 PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setString(1, document.getResidentName());
                pst.setString(2, document.getAddress()); // In our mapping, 'address' field holds the document type
                pst.executeUpdate();
                
                System.out.println("Deleted document for: " + document.getResidentName());
                loadSampleData(); // Refresh table
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Load document requests from database
    private void loadSampleData() {
        ObservableList<Address> data = FXCollections.observableArrayList();
        
        // Join document requests with resident names
        String sql = "SELECT d.id, r.full_name, d.document_type, d.status, d.reason FROM user_documents d " +
                     "JOIN residents r ON d.username_ref = r.username_ref " +
                     "ORDER BY d.date_requested DESC";

        try (Connection conn = Database.connect();
             PreparedStatement pst = (conn != null) ? conn.prepareStatement(sql) : null;
             ResultSet rs = (pst != null) ? pst.executeQuery() : null) {
            
            while (rs != null && rs.next()) {
                data.add(new Address(rs.getInt("id"), rs.getString("full_name"), rs.getString("document_type"), rs.getString("status"), rs.getString("reason")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        addressTable.setItems(data);
    }

    // Handle button selection and highlighting
    private void setButtonAsSelected(Button button) {
        if (button == null) {
            System.out.println("setButtonAsSelected called with null button");
            return;
        }
        if (currentSelectedButton != null) {
            currentSelectedButton.setStyle("-fx-background-color: rgba(255, 255, 255, 0.2); -fx-border-radius: 15; -fx-background-radius: 15; -fx-cursor: hand;");
            System.out.println("Previous button deselected");
        }
        button.setStyle("-fx-background-color: rgba(255, 255, 255, 0.6); -fx-border-radius: 15; -fx-background-radius: 15; -fx-cursor: hand; -fx-font-weight: bold;");
        currentSelectedButton = button;
        System.out.println("New button selected and highlighted");
    }

    @FXML
    public void handleDashboard(ActionEvent event) {
        navigateTo("Welcomepage.fxml");
    }

    @FXML
    public void handleResidents(ActionEvent event) {
        System.out.println("Residents button clicked - Navigating to Residents page");
        navigateTo("Residentspage.fxml");
    }

    @FXML
    public void handleAddresses(ActionEvent event) {
        System.out.println("Addresses button clicked - Navigating to Addresses page");
        navigateTo("Addresses.fxml");
    }

    @FXML
    public void handleDocuments(ActionEvent event) {
        System.out.println("Documents button clicked - Navigating to Documents page");
        navigateTo("Documents.fxml");
    }

    @FXML
    public void handleReports(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Reports.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
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

    @FXML
    public void handleUsersandRoles(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("UsersandRoles.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
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

    @FXML
    public void handleLogs(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Logs.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
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

    @FXML
    public void handleContact(ActionEvent event) {
        System.out.println("Contact button clicked - Navigating to Contact page");
        navigateTo("Contact.fxml");
    }

    @FXML
    public void handleExit(ActionEvent event) {
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

    @FXML
    public void showAddResidentModal(ActionEvent event) {
        System.out.println("Opening add document modal...");
    }

    @FXML
    public void onButtonHoverEnter(MouseEvent event) {
        if (event.getSource() instanceof Button button) {
            button.setStyle("-fx-background-color: rgba(255, 255, 255, 0.3); -fx-border-radius: 15; -fx-background-radius: 15; -fx-cursor: hand;");
        }
    }

    @FXML
    public void onButtonHoverExit(MouseEvent event) {
        if (event.getSource() instanceof Button button && button != currentSelectedButton) {
            button.setStyle("-fx-background-color: rgba(255, 255, 255, 0.2); -fx-border-radius: 15; -fx-background-radius: 15; -fx-cursor: hand;");
        }
    }

    // Generic navigation helper
    private void navigateTo(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            Stage stage = (Stage) btnDashboard.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            System.out.println("Successfully navigated to " + fxmlFile);
        } catch (Exception e) {
            System.err.println("Error loading " + fxmlFile + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
