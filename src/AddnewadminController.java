import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class AddnewadminController {
    
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    /**
     * handleCancel - Closes the dialog window
     */
    @FXML
    public void handleCancel(ActionEvent event) {
        ((Stage) txtUsername.getScene().getWindow()).close();
    }

    @FXML
    public void handleSaveAdmin(ActionEvent event) {
        String user = txtUsername.getText();
        String pass = txtPassword.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert("Input Error", "Please enter both username and password.");
            return;
        }

        // KEYPOINT: Logic to save Admin to database
        if (registerAdmin(user, pass)) {
            showAlert("Success", "New Administrator added successfully.");
            ((Stage) txtUsername.getScene().getWindow()).close(); // Close dialog on success
        } else {
            showAlert("Error", "Could not add admin. Username might already exist.");
        }
    }

    private boolean registerAdmin(String user, String pass) {
        String userSql = "INSERT INTO users (username, password) VALUES (?, ?)";
        String residentSql = "INSERT INTO residents (username_ref, full_name, classification, contact, address, birthday, email, gender, nickname) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = Database.connect()) {
            if (conn == null) return false;
            conn.setAutoCommit(false); // Transaction start
            
            try (PreparedStatement pstUser = conn.prepareStatement(userSql);
                 PreparedStatement pstResident = conn.prepareStatement(residentSql)) {
                
                pstUser.setString(1, user);
                pstUser.setString(2, pass);
                pstUser.executeUpdate();
                
                pstResident.setString(1, user);         
                pstResident.setString(2, user + " (Admin)"); 
                pstResident.setString(3, "Admin");   // KEYPOINT: This allows login as Admin
                pstResident.setString(4, "N/A");    
                pstResident.setString(5, "N/A");    
                pstResident.setNull(6, Types.DATE);     
                pstResident.setString(7, "N/A");       
                pstResident.setString(8, "Other");      
                pstResident.setString(9, "Admin");         
                pstResident.executeUpdate();
                
                conn.commit(); 
                return true;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
