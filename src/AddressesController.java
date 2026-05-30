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

// (ENTIRE FILE - NEW): Controller class for managing the Addresses page
public class AddressesController {

    // Table components
    @FXML private TableView<Address> addressTable;
    @FXML private TableColumn<Address, String> colName;
    @FXML private TableColumn<Address, String> colAddress;
    @FXML private TableColumn<Address, String> colCity;
    @FXML private TableColumn<Address, String> colZipCode;
    @FXML private TableColumn<Address, Void> colActions;

    // Navigation buttons
    @FXML private Button btnDashboard;
    @FXML private Button btnAddresses;
    @FXML private Button btnResidents;
    @FXML private Button btnContact;
    @FXML private Button btnExit;

    private Button currentSelectedButton = null;

    @FXML
    public void initialize() {
        setButtonAsSelected(btnAddresses);

        // Map columns to Address model properties
        if (colName != null) colName.setCellValueFactory(new PropertyValueFactory<>("residentName"));
        if (colAddress != null) colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        if (colCity != null) colCity.setCellValueFactory(new PropertyValueFactory<>("city"));
        if (colZipCode != null) colZipCode.setCellValueFactory(new PropertyValueFactory<>("zipCode"));

        // Setup action buttons in the table
        if (colActions != null) {
            Callback<TableColumn<Address, Void>, TableCell<Address, Void>> cellFactory = new Callback<>() {
                @Override
                public TableCell<Address, Void> call(final TableColumn<Address, Void> param) {
                    return new TableCell<>() {
                        private final Button btnView = new Button("👁");
                        private final Button btnEdit = new Button("✏");
                        private final Button btnDelete = new Button("🗑");
                        private final HBox container = new HBox(8, btnView, btnEdit, btnDelete);

                        {
                            btnView.setStyle("-fx-background-color: #f1f5f9; -fx-cursor: hand; -fx-background-radius: 4;");
                            btnEdit.setStyle("-fx-background-color: #f1f5f9; -fx-cursor: hand; -fx-background-radius: 4;");
                            btnDelete.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-background-radius: 4;");

                            btnView.setOnAction(event -> System.out.println("Viewing address for: " + getTableView().getItems().get(getIndex()).getResidentName()));
                            btnEdit.setOnAction(event -> System.out.println("Editing address for: " + getTableView().getItems().get(getIndex()).getResidentName()));
                            btnDelete.setOnAction(event -> handleDeleteAddress(getTableView().getItems().get(getIndex())));
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

        loadAddressData();
    }

    // Load resident addresses from database
    private void loadAddressData() {
        if (addressTable != null) {
            ObservableList<Address> addresses = FXCollections.observableArrayList();
            String sql = "SELECT full_name, address FROM residents WHERE classification != 'Admin'";
            
            try (Connection conn = Database.connect();
                 PreparedStatement pst = conn.prepareStatement(sql);
                 ResultSet rs = pst.executeQuery()) {
                
                while (rs.next()) {
                    addresses.add(new Address(
                        rs.getString("full_name"),
                        rs.getString("address"),
                        "N/A", // City Placeholder
                        "N/A"  // ZipCode Placeholder
                    ));
                }
                addressTable.setItems(addresses);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Delete address record
    private void handleDeleteAddress(Address address) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Address Entry for: " + address.getResidentName());
        alert.setContentText("Are you sure you want to delete this address entry?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "DELETE FROM residents WHERE full_name = ?";
            try (Connection conn = Database.connect();
                 PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setString(1, address.getResidentName());
                pst.executeUpdate();
                System.out.println("Deleted address entry for: " + address.getResidentName());
                loadAddressData(); // Refresh table
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Logout and redirect to login
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

    // Navigate to Residents page
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

    // Refresh current page
    @FXML
    public void handleAddresses(ActionEvent event) {
        setButtonAsSelected(btnAddresses);
        System.out.println("Addresses button selected - refreshing page.");
    }

    // Navigate to Dashboard
    @FXML
    public void handleDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Welcomepage.fxml"));
            Parent root = loader.load();
            
            // ADDED LINE 106: Get the current window (stage) from the Dashboard button
            Stage stage = (Stage) btnDashboard.getScene().getWindow();
            
            // ADDED LINE 108: Create a new scene with the welcome page
            Scene scene = new Scene(root);
            
            // ADDED LINE 110-112: Set the scene and show it, keep window maximized
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            
            System.out.println("Navigated back to Dashboard page.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: Could not load dashboard page.");
        }
    }

    // Navigate to Documents page
    @FXML
    public void handleDocuments(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Documents.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) btnResidents.getScene().getWindow();
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

    // Navigate to Contact page
    @FXML
    public void handleContact(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Contact.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) btnContact.getScene().getWindow();
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

    // Hover effect start
    @FXML
    public void onButtonHoverEnter(MouseEvent event) {
        Button button = (Button) event.getSource();
        if (button != currentSelectedButton) {
            button.setStyle(button.getStyle().replace("rgba(255, 255, 255, 0.2)", "rgba(255, 255, 255, 0.4)"));
        }
    }

    // Hover effect end
    @FXML
    public void onButtonHoverExit(MouseEvent event) {
        Button button = (Button) event.getSource();
        if (button != currentSelectedButton) {
            button.setStyle(button.getStyle().replace("rgba(255, 255, 255, 0.4)", "rgba(255, 255, 255, 0.2)"));
        }
    }

    // Highlight selected button
    private void setButtonAsSelected(Button button) {
        if (currentSelectedButton != null) {
            currentSelectedButton.setStyle(currentSelectedButton.getStyle().replace("rgba(150, 150, 150, 0.4)", "rgba(255, 255, 255, 0.2)"));
        }

        if (button != null) {
            button.setStyle(button.getStyle().replace("rgba(255, 255, 255, 0.2)", "rgba(150, 150, 150, 0.4)"));
            currentSelectedButton = button;
        }
    }

    // Navigate to Reports
    @FXML
    public void handleReports(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Reports.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) btnDashboard.getScene().getWindow();
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

    // Navigate to Users and Roles
    @FXML
    public void handleUsersandRoles(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("UsersandRoles.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) btnDashboard.getScene().getWindow();
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

    // Navigate to Logs
    @FXML
    public void handleLogs(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Logs.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) btnDashboard.getScene().getWindow();
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
