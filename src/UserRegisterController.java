import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Alert;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;

public class UserRegisterController {
    
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    /**
     * handleRegisterAction - Triggered when the Register button is clicked
     */
    @FXML
    public void handleRegisterAction(ActionEvent event) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Input Error", "Please enter both username and password.");
            return;
        }

        if ("admin".equalsIgnoreCase(username)) {
            showAlert("Registration Blocked", "You cannot register as the admin.");
            return;
        }

        if (isUsernameTaken(username)) {
            showAlert("Registration Failed", "This username is already taken.");
            return;
        }

        String registrationStatus = registerUser(username, password);

        if ("SUCCESS".equals(registrationStatus)) {
            showAlert("Success", "Registration successful! Welcome to the system.");
            recordLogin(username, "Male"); 
            navigateToUserWelcomePage(event, username);
        } else {
            showAlert("Registration Failed", registrationStatus);
        }
    }

    private String registerUser(String user, String pass) {
        String userSql = "INSERT INTO users (username, password) VALUES (?, ?)";
        String residentSql = "INSERT INTO residents (username_ref, full_name, classification, contact, address, birthday, email, gender, nickname) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = Database.connect()) {
            if (conn == null) return "Could not connect to database.";
            conn.setAutoCommit(false); 
            
            try (PreparedStatement pstUser = conn.prepareStatement(userSql);
                 PreparedStatement pstResident = conn.prepareStatement(residentSql)) {
                
                pstUser.setString(1, user);
                pstUser.setString(2, pass);
                pstUser.executeUpdate();
                
                pstResident.setString(1, user);         
                pstResident.setString(2, user);         
                pstResident.setString(3, "Male");       
                pstResident.setString(4, "Not Set");    
                pstResident.setString(5, "Not Set");    
                pstResident.setNull(6, Types.DATE);     
                pstResident.setString(7, "None");       
                pstResident.setString(8, "Other");      
                pstResident.setString(9, user);         
                pstResident.executeUpdate();
                
                conn.commit(); 
                return "SUCCESS";
            } catch (SQLException e) {
                conn.rollback();
                return "Database Error: " + e.getMessage();
            }
        } catch (Exception e) {
            return "System Error: " + e.getMessage();
        }
    }

    private boolean isUsernameTaken(String username) {
        String sql = "SELECT username FROM users WHERE username = ?";
        try (Connection conn = Database.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, username);
            ResultSet rs = pst.executeQuery();
            return rs.next();
        } catch (Exception e) { return false; }
    }

    private void recordLogin(String username, String role) {
        String sql = "INSERT INTO logs (username, role) VALUES (?, ?)";
        try (Connection conn = Database.connect();
             PreparedStatement pst = (conn != null) ? conn.prepareStatement(sql) : null) {
            if (pst == null) return;
            pst.setString(1, username);
            pst.setString(2, role);
            pst.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void navigateToUserWelcomePage(ActionEvent event, String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("UserWelcomepage.fxml"));
            Parent root = loader.load();
            UserWelcomepageController controller = loader.getController();
            
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
            controller.initData(username);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void handleBackToLoginClick(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("login.fxml"));
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not load login page.");
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
