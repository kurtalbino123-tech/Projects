import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

public class RequestDocumentController {
    
    @FXML private TextField txtName;
    @FXML private TextField txtContact; // Bound to "Type of Document" in FXML
    @FXML private TextField txtAddress;
    @FXML private TextField txtAge;
    @FXML private TextArea txtReason;

    private String loggedInUsername;

    public void initData(String username) {
        this.loggedInUsername = username;
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        ((Stage) txtName.getScene().getWindow()).close();
    }

    @FXML
    public void handleRequest(ActionEvent event) {
        String name = txtName.getText();
        String docType = txtContact.getText();
        String address = txtAddress.getText();
        String age = txtAge.getText();
        String reason = txtReason.getText();

        if (name.isEmpty() || docType.isEmpty() || address.isEmpty() || age.isEmpty()) {
            showAlert("Input Error", "Please fill in all required fields.");
            return;
        }

        // SQL logic to save document request
        String sql = "INSERT INTO user_documents (username_ref, document_type, date_requested, status, reason) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = Database.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setString(1, loggedInUsername);
            pst.setString(2, docType);
            pst.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
            pst.setString(4, "Pending");
            pst.setString(5, reason);

            pst.executeUpdate();
            
            showAlert("Success", "Your request for " + docType + " has been submitted.");
            ((Stage) txtName.getScene().getWindow()).close();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Could not submit request: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
