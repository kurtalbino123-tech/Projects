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

public class AddReportController {
    
    @FXML private TextField txtName;
    @FXML private TextField txtContact; // Bound to "Type of Complaint"
    @FXML private TextField txtAddress; // Bound to "Purok"
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
    public void handleSaveResident(ActionEvent event) {
        String name = txtName.getText();
        String type = txtContact.getText();
        String location = txtAddress.getText();
        String ageStr = txtAge.getText();
        String reason = txtReason.getText();

        if (name.isEmpty() || type.isEmpty() || location.isEmpty() || ageStr.isEmpty() || reason.isEmpty()) {
            showAlert("Input Error", "Please fill in all fields.");
            return;
        }

        try (Connection conn = Database.connect();
             PreparedStatement pst = conn.prepareStatement(
                 "INSERT INTO user_reports (username_ref, resident_name, complaint_type, location, age, reason, status, date_reported) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            
            pst.setString(1, loggedInUsername);
            pst.setString(2, name);
            pst.setString(3, type);
            pst.setString(4, location);
            pst.setInt(5, Integer.parseInt(ageStr));
            pst.setString(6, reason);
            pst.setString(7, "Pending");
            pst.setDate(8, java.sql.Date.valueOf(LocalDate.now()));

            pst.executeUpdate();
            
            showAlert("Success", "Report submitted successfully!");
            ((Stage) txtName.getScene().getWindow()).close();

        } catch (NumberFormatException e) {
            showAlert("Input Error", "Age must be a valid number.");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Could not submit report: " + e.getMessage());
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
