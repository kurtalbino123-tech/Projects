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
import javafx.scene.input.MouseEvent;
import javafx.scene.control.TableCell;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class DocumentsuserController {

    // KEYPOINT: FXML bindings for UI elements from DocumentsUser.fxml
    @FXML private Button btnAccount;
    @FXML private Button btnDocumentsUser;
    @FXML private Button btnReportsUser;
    @FXML private Button btnExit;
    @FXML private TableView<UserDocument> addressTable;
    @FXML private TableColumn<UserDocument, String> colName;
    @FXML private TableColumn<UserDocument, String> colAddress;
    @FXML private TableColumn<UserDocument, String> colStatus;

    // KEYPOINT: Changed to static so the username persists when navigating between user pages
    private static String loggedInUsername;
    private Button currentSelectedButton = null;

    // KEYPOINT: Inner class to serve as a data model for the documents table
    public static class UserDocument {
        private final String documentName;
        private final String dateRequested;
        private final String status;

        public UserDocument(String documentName, String dateRequested, String status) {
            this.documentName = documentName;
            this.dateRequested = dateRequested;
            this.status = status;
        }

        public String getDocumentName() { return documentName; }
        public String getDateRequested() { return dateRequested; }
        public String getStatus() { return status; }
    }

    @FXML
    public void initialize() {
        // KEYPOINT: Set the current page's button as selected and populate the table
        setButtonAsSelected(btnDocumentsUser);

        colName.setCellValueFactory(new PropertyValueFactory<>("documentName"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("dateRequested"));

        // Bind and style the status column for the user panel
        if (colStatus != null) {
            colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            colStatus.setCellFactory(column -> new TableCell<UserDocument, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        if (item.equalsIgnoreCase("Approved")) setStyle("-fx-text-fill: #166534; -fx-font-weight: bold;");
                        else if (item.equalsIgnoreCase("Declined")) setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                        else setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                    }
                }
            });
        }

        // If we navigated back from another page, reload the documents automatically
        if (loggedInUsername != null) {
            loadUserDocuments();
        }
        // Data will be loaded once initData provides the username
    }

    public void initData(String username) {
        this.loggedInUsername = username;
        loadUserDocuments(); // KEYPOINT: Fetch real data for this specific user
        System.out.println("Documents user page initialized for: " + username);
    }

    /**
     * loadUserDocuments - Fetches the document requests for the logged-in user from the database.
     */
    private void loadUserDocuments() {
        if (addressTable != null && loggedInUsername != null) {
            ObservableList<UserDocument> documents = FXCollections.observableArrayList();
            String sql = "SELECT document_type, date_requested, status FROM user_documents WHERE username_ref = ? ORDER BY date_requested DESC";

            try (Connection conn = Database.connect();
                 PreparedStatement pst = (conn != null) ? conn.prepareStatement(sql) : null) {
                
                if (pst == null) return;
                pst.setString(1, loggedInUsername);

                try (ResultSet rs = pst.executeQuery()) {
                    while (rs.next()) {
                        documents.add(new UserDocument(
                            rs.getString("document_type"),
                            rs.getString("date_requested"),
                            rs.getString("status")
                        ));
                    }
                }
                addressTable.setItems(documents);
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
        // Already on this page, can be used to refresh data
        System.out.println("Already on Documents User page.");
    }

    @FXML
    public void handleReportsUser(ActionEvent event) {
        navigateTo(event, "ReportsUser.fxml");
    }

    @FXML
    public void handleExit(ActionEvent event) {
        navigateTo(event, "login.fxml");
    }

    @FXML
    public void showAddResidentModal(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("requestdocument.fxml"));
            Parent root = loader.load();
            
            RequestDocumentController controller = loader.getController();
            controller.initData(loggedInUsername);
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Request New Document");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setResizable(false);
            
            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            
            // KEYPOINT: Refresh the table after the request is made and the window is closed
            loadUserDocuments();
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
