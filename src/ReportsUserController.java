import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ReportsUserController {

    // KEYPOINT: FXML bindings for UI elements from ReportsUser.fxml
    @FXML private Button btnAccount;
    @FXML private Button btnDocumentsUser;
    @FXML private Button btnReportsUser;
    @FXML private Button btnExit;
    @FXML private TableView<UserReport> addressTable;
    @FXML private TableColumn<UserReport, String> colReportUser;
    @FXML private TableColumn<UserReport, String> colAddress;
    @FXML private TableColumn<UserReport, String> colStatus;

    // KEYPOINT: Added static username to maintain session consistency
    private static String loggedInUsername;
    private Button currentSelectedButton = null;

    // KEYPOINT: Inner class to serve as a data model for the reports table
    public static class UserReport {
        private final String reportTitle;
        private final String dateReported;
        private final String status;

        public UserReport(String reportTitle, String dateReported, String status) {
            this.reportTitle = reportTitle;
            this.dateReported = dateReported;
            this.status = status;
        }

        public String getReportTitle() { return reportTitle; }
        public String getDateReported() { return dateReported; }
        public String getStatus() { return status; }
    }

    // Add initData to handle session entry
    public void initData(String username) {
        loggedInUsername = username;
        loadUserReports();
    }

    @FXML
    public void initialize() {
        // KEYPOINT: Set the current page's button as selected and populate the table
        setButtonAsSelected(btnReportsUser);

        colReportUser.setCellValueFactory(new PropertyValueFactory<>("reportTitle"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("dateReported"));

        // Bind and style the status column
        if (colStatus != null) {
            colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            colStatus.setCellFactory(column -> new TableCell<UserReport, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        // Apply colors: Green for Resolved/Approved, Red for Declined, Amber for Pending/In Progress
                        if (item.equalsIgnoreCase("Resolved") || item.equalsIgnoreCase("Approved")) {
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

        // Auto-load data if session already exists
        if (loggedInUsername != null) {
            loadUserReports();
        }
    }

    /**
     * loadUserReports - Fetches the reports submitted by the logged-in user from the database.
     */
    private void loadUserReports() {
        if (addressTable != null && loggedInUsername != null) {
            ObservableList<UserReport> reports = FXCollections.observableArrayList();
            String sql = "SELECT complaint_type, date_reported, status FROM user_reports WHERE username_ref = ? ORDER BY date_reported DESC";

            try (Connection conn = Database.connect();
                 PreparedStatement pst = (conn != null) ? conn.prepareStatement(sql) : null) {
                
                if (pst == null) return;
                pst.setString(1, loggedInUsername);

                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        reports.add(new UserReport(
                            rs.getString("complaint_type"),
                            rs.getString("date_reported"),
                            rs.getString("status")
                        ));
                    }
                }
                addressTable.setItems(reports);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // KEYPOINT: Navigation handlers to switch between different user pages
    @FXML
    public void handleDashboard(ActionEvent event) {
        navigateTo(event, "UserWelcomepage.fxml");
    }

    @FXML
    public void handleDocumentsUser(ActionEvent event) {
        navigateTo(event, "DocumentsUser.fxml");
    }

    @FXML
    public void handleReportsUser(ActionEvent event) {
        // Already on this page, can be used to refresh data
        System.out.println("Already on Reports User page.");
    }

    @FXML
    public void handleExit(ActionEvent event) {
        navigateTo(event, "login.fxml");
    }

    @FXML
    public void showAddResidentModal(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("addreport.fxml"));
            Parent root = loader.load();
            
            // Pass the logged-in user session to the report controller
            AddReportController controller = loader.getController();
            controller.initData(loggedInUsername);
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Submit Community Report");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setResizable(false);
            
            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            
            // Refresh the table once the user closes the submission form
            loadUserReports();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // KEYPOINT: Generic navigation method to reduce code duplication
    private void navigateTo(ActionEvent event, String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            
            // Get the target controller and pass the current session
            Object controller = loader.getController();
            if (controller instanceof UserWelcomepageController) {
                ((UserWelcomepageController) controller).initData(loggedInUsername);
            } else if (controller instanceof DocumentsuserController) {
                ((DocumentsuserController) controller).initData(loggedInUsername);
            } else if (controller instanceof ReportsUserController) {
                ((ReportsUserController) controller).initData(loggedInUsername);
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // KEYPOINT: UI helper methods for button hover and selection effects
    @FXML
    public void onButtonHoverEnter(MouseEvent event) {
        Button button = (Button) event.getSource();
        if (button != currentSelectedButton) {
            button.setStyle(button.getStyle().replace("rgba(255, 255, 255, 0.2)", "rgba(255, 255, 255, 0.4)"));
        }
    }

    @FXML
    public void onButtonHoverExit(MouseEvent event) {
        Button button = (Button) event.getSource();
        if (button != currentSelectedButton) {
            button.setStyle(button.getStyle().replace("rgba(255, 255, 255, 0.4)", "rgba(255, 255, 255, 0.2)"));
        }
    }

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
