import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.collections.FXCollections;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class AddResidentController {

    @FXML private TextField txtName;
    @FXML private TextField txtContact;
    @FXML private TextField txtAddress;
    @FXML private TextField txtAge;
    @FXML private TextField txtGender;

    @FXML
    public void initialize() {
        // Initialization logic if needed
    }

    @FXML
    public void handleCancel(ActionEvent event) {
        // Close the modal without saving
        ((Stage) txtName.getScene().getWindow()).close();
    }

    @FXML
    public void handleSaveResident(ActionEvent event) {
        String name = txtName.getText();
        String contact = txtContact.getText();
        String address = txtAddress.getText();
        String ageStr = txtAge.getText();
        String gender = txtGender.getText();

        if (name.isEmpty() || contact.isEmpty() || address.isEmpty() || ageStr.isEmpty() || gender.isEmpty()) {
            showAlert("Input Error", "Please fill in all fields.");
            return;
        }

        // Auto-calculate classification based on age input
        String classification = gender;
        int age = 0;
        try {
            age = Integer.parseInt(ageStr);
            if (age >= 60) classification = "Senior";
            else if (age < 18) classification = "Minor";
        } catch (NumberFormatException e) {
            showAlert("Input Error", "Please enter a valid number for age.");
            return;
        }

        // KEYPOINT: Database logic to insert the new resident record
        // We use a generated 'username_ref' based on the name since there is no user account yet
        String sql = "INSERT INTO residents (username_ref, full_name, contact, address, classification, email, gender, nickname, age, birthday) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            String userRef = name.toLowerCase().replaceAll("\\s+", "_"); // Generate a unique-ish reference
            
            pst.setString(1, userRef);
            pst.setString(2, name);
            pst.setString(3, contact);
            pst.setString(4, address);
            pst.setString(5, classification);
            pst.setString(6, "N/A"); // Default email
            pst.setString(7, gender);
            pst.setString(8, name); // Default nickname
            pst.setInt(9, age);
            pst.setNull(10, Types.DATE); // Birthday is unknown, set to NULL

            pst.executeUpdate();
            showAlert("Success", "Resident added successfully!");
            
            // Close the dialog
            ((Stage) txtName.getScene().getWindow()).close();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Could not save resident: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}