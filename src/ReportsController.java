import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import java.sql.*;
import javafx.scene.input.MouseEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import java.util.Optional;

public class ReportsController {
    @FXML private TableView<Address> addressTable;
    @FXML private TableColumn<Address, String> colName;
    // KEYPOINT: Matches fx:id="colContact" in Reports.fxml
    @FXML private TableColumn<Address, String> colContact;
    @FXML private TableColumn<Address, String> colStatus;
    @FXML private TableColumn<Address, Void> colActions;

    // Sidebar button bindings
    @FXML private Button btnDashboard;
    @FXML private Button btnResidents;
    @FXML private Button btnAddresses;
    @FXML private Button btnDocuments;
    @FXML private Button Contactbtn; // Matches fx:id="Contactbtn" in Reports.fxml
    @FXML private Button Reportsbtn;
    @FXML private Button UsersandRolesbtn;
    @FXML private Button Logsbtn;
    @FXML private Button btnExit;

    private Button currentSelectedButton = null;

    @FXML
    public void initialize() {
        // Highlight the Reports button in the sidebar
        if (Reportsbtn != null) {
            setButtonAsSelected(Reportsbtn);
        }

        colName.setCellValueFactory(new PropertyValueFactory<>("residentName"));
        
        if (colContact != null) {
            colContact.setCellValueFactory(new PropertyValueFactory<>("address")); // Using 'address' property for Complaint Type
        }

        if (colStatus != null) {
            colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            colStatus.setCellFactory(column -> new TableCell<Address, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        if (item.equalsIgnoreCase("Resolved")) setStyle("-fx-text-fill: #166534; -fx-font-weight: bold;");
                        else if (item.equalsIgnoreCase("Declined")) setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                        else setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                    }
                }
            });
        }

        setupActions();
        loadReports();
    }

    private void setupActions() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnView = new Button("👁");
            private final Button btnApprove = new Button("✔");
            private final Button btnDecline = new Button("🗑");
            private final HBox container = new HBox(8, btnView, btnApprove, btnDecline);
            {
                btnView.setStyle("-fx-background-color: #f1f5f9; -fx-cursor: hand;");
                btnApprove.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-cursor: hand;");
                btnDecline.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-cursor: hand;");
                
                btnView.setOnAction(e -> handleView(getTableView().getItems().get(getIndex())));
                btnApprove.setOnAction(e -> handleUpdate(getTableView().getItems().get(getIndex()), "Resolved"));
                btnDecline.setOnAction(e -> handleUpdate(getTableView().getItems().get(getIndex()), "Declined"));
                container.setStyle("-fx-alignment: CENTER;");
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void loadReports() {
        ObservableList<Address> data = FXCollections.observableArrayList();
        String sql = "SELECT id, resident_name, complaint_type, status, reason FROM user_reports ORDER BY date_reported DESC";
        
        try (Connection conn = Database.connect(); 
             PreparedStatement pst = (conn != null) ? conn.prepareStatement(sql) : null) {
            if (pst == null) return;
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    data.add(new Address(rs.getInt("id"), rs.getString("resident_name"), rs.getString("complaint_type"), rs.getString("status"), rs.getString("reason")));
                }
            }
            addressTable.setItems(data);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void handleView(Address report) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Report Details");
        alert.setHeaderText("Complaint from: " + report.getResidentName());
        alert.setContentText("Type: " + report.getAddress() + "\nReason: " + report.getReason());
        alert.showAndWait();
    }

    private void handleUpdate(Address report, String status) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Mark this report as " + status + "?", ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            try (Connection conn = Database.connect(); PreparedStatement pst = conn.prepareStatement("UPDATE user_reports SET status = ? WHERE id = ?")) {
                pst.setString(1, status);
                pst.setInt(2, report.getRequestId());
                pst.executeUpdate();
                loadReports();
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    // Navigation handlers
    @FXML public void handleDashboard(ActionEvent e) { navigateTo(e, "Welcomepage.fxml"); }
    @FXML public void handleResidents(ActionEvent e) { navigateTo(e, "Residentspage.fxml"); }
    @FXML public void handleAddresses(ActionEvent e) { navigateTo(e, "Addresses.fxml"); }
    @FXML public void handleDocuments(ActionEvent e) { navigateTo(e, "Documents.fxml"); }
    @FXML public void handleContact(ActionEvent e) { navigateTo(e, "Contact.fxml"); }
    @FXML public void handleReports(ActionEvent e) { loadReports(); } // Refresh
    @FXML public void handleUsersandRoles(ActionEvent e) { navigateTo(e, "UsersandRoles.fxml"); }
    @FXML public void handleLogs(ActionEvent e) { navigateTo(e, "Logs.fxml"); }

    @FXML
    public void handleExit(ActionEvent event) {
        navigateTo(event, "login.fxml");
    }

    private void navigateTo(ActionEvent event, String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void onButtonHoverEnter(MouseEvent event) {
        Button button = (Button) event.getSource();
        if (button != currentSelectedButton) {
            button.setStyle("-fx-background-color: rgba(255, 255, 255, 0.3); -fx-border-radius: 15; -fx-background-radius: 15; -fx-cursor: hand;");
        }
    }

    @FXML
    public void onButtonHoverExit(MouseEvent event) {
        Button button = (Button) event.getSource();
        if (button != currentSelectedButton) {
            button.setStyle("-fx-background-color: rgba(255, 255, 255, 0.2); -fx-border-radius: 15; -fx-background-radius: 15; -fx-cursor: hand;");
        }
    }

    private void setButtonAsSelected(Button button) {
        if (currentSelectedButton != null) {
            currentSelectedButton.setStyle("-fx-background-color: rgba(255, 255, 255, 0.2); -fx-border-radius: 15; -fx-background-radius: 15;");
        }
        if (button != null) {
            button.setStyle("-fx-background-color: rgba(255, 255, 255, 0.6); -fx-border-radius: 15; -fx-background-radius: 15; -fx-font-weight: bold;");
            currentSelectedButton = button;
        }
    }
}