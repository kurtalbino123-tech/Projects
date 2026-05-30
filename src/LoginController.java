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
import javafx.scene.Node;


/**
 * LoginController - Manages the login screen functionality
 * Handles user authentication against the database and navigation to the welcome page
 * upon successful login with valid admin credentials.
 */
public class LoginController {

    // FXML-linked UI components from login.fxml
    @FXML private TextField txtUsername;      // Input field for username entry
    @FXML private PasswordField txtPassword;  // Input field for password entry

    /**
     * handleLogin - Triggered when the LOGIN button is clicked
     * Validates user input and authenticates against the database
     * 
     * @param event The ActionEvent from the LOGIN button click
     */
    @FXML
    public void handleLogin(ActionEvent event) {
        // Retrieve username and password from input fields
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        // Check if both fields have input (prevent empty credentials)
        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Input Error", "Please enter both username and password.");
            return;
        }

        // Validate credentials against the database
        if (validateLogin(username, password)) {
            System.out.println("Login Success!");
            
            // KEYPOINT: Route based on classification from database instead of hardcoded string
            String classification = getClassification(username);
            
            // Added a safety check: if classification is Admin OR the username is "admin", go to Admin Dashboard
            if ("Admin".equalsIgnoreCase(classification) || "admin".equalsIgnoreCase(username)) {
                recordLogin(username, "Admin");
                navigateToWelcomePage(event);
            } else {
                recordLogin(username, classification);
                navigateToUserWelcomePage(event, username);
            }
        } else {
            System.out.println("Login Failed!");
            // Display error message for invalid credentials
            showAlert("Login Failed", "Invalid username or password.");
        }
    }

    /**
     * recordLogin - Inserts a new log entry whenever a user successfully logs in
     */
    private void recordLogin(String username, String role) {
        String sql = "INSERT INTO logs (username, role) VALUES (?, ?)";
        // KEYPOINT: Using try-with-resources for Connection prevents "Too Many Connections" errors
        try (Connection conn = Database.connect();
             PreparedStatement pst = (conn != null) ? conn.prepareStatement(sql) : null) {
            if (pst == null) return;
            pst.setString(1, username);
            pst.setString(2, role);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * getClassification - Helper to check if a user is an Admin or Resident
     */
    private String getClassification(String username) {
        String sql = "SELECT classification FROM residents WHERE username_ref = ?";
        try (Connection conn = Database.connect();
             PreparedStatement pst = (conn != null) ? conn.prepareStatement(sql) : null) {
            if (pst == null) return "Resident";
            pst.setString(1, username);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getString("classification");
            }
        } catch (Exception e) { e.printStackTrace(); }
        return "Resident"; // Fallback if no record is found in residents table
    }

    private boolean validateLogin(String user, String pass) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = Database.connect(); 
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, user);
            pst.setString(2, pass);
            
            // KEYPOINT: Using try-with-resources for ResultSet prevents database leaks
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Database found user: " + user);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void navigateToWelcomePage(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Welcomepage.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((javafx.scene.control.Button) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root, 1920, 1200);
            
            stage.setResizable(true);
            stage.setScene(scene);
            stage.setMaximized(true);  // Maximize window while keeping window controls visible
            stage.show();
            
            System.out.println("Successfully navigated to Welcome Page!");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Navigation Error", "Could not load welcome page.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * handleRegisterAction - Triggered when the Register button is clicked on the UserRegister page
     * Inserts the new user into the database
     * 
     * @param event The ActionEvent from the Register button click
     */
    @FXML
    public void handleRegisterAction(ActionEvent event) {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Input Error", "Please enter both username and password.");
            return;
        }

        // Prevent users from registering as the admin
        if ("admin".equalsIgnoreCase(username)) {
            showAlert("Registration Blocked", "You cannot register as the admin. Please choose a different username.");
            return;
        }

        // KEYPOINT: Check if username is actually taken before attempting registration
        // This prevents misleading "Username exists" messages during database connection errors
        if (isUsernameTaken(username)) {
            showAlert("Registration Failed", "This username is already taken. Please choose another.");
            return;
        }

        // KEYPOINT: Capture the specific status/error from the registration process
        String registrationStatus = registerUser(username, password);

        if ("SUCCESS".equals(registrationStatus)) {
            showAlert("Success", "Registration successful! Welcome to the system.");
            // KEYPOINT: Record the login event immediately after a successful registration
            recordLogin(username, "Male"); 
            navigateToUserWelcomePage(event, username); // Pass the registered username
        } else {
            // Now it shows the ACTUAL SQL error (e.g., "Unknown column 'birthday'")
            showAlert("Registration Failed", registrationStatus);
        }
    }

    private String registerUser(String user, String pass) {
        // KEYPOINT: SQL queries for both tables. We use the username as the initial Resident Name.
        String userSql = "INSERT INTO users (username, password) VALUES (?, ?)";
        // Updated to align with the new 'residents' table schema (username_ref, full_name)
        String residentSql = "INSERT INTO residents (username_ref, full_name, classification, contact, address, birthday, email, gender, nickname) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = Database.connect()) {
            if (conn == null) return "Could not connect to database. Is XAMPP running?";
            
            // KEYPOINT: Transaction Management. We turn off auto-commit to handle both inserts as one unit.
            conn.setAutoCommit(false); 
            
            try (PreparedStatement pstUser = conn.prepareStatement(userSql);
                 PreparedStatement pstResident = conn.prepareStatement(residentSql)) {
                
                // 1. Create the login account
                pstUser.setString(1, user);
                pstUser.setString(2, pass);
                pstUser.executeUpdate();
                
                // 2. Create the resident profile (matching the admin panel columns)
                pstResident.setString(1, user);         // username_ref
                pstResident.setString(2, user);         // full_name
                pstResident.setString(3, "Male");       // classification (updated from Resident)
                pstResident.setString(4, "Not Set");    // contact
                pstResident.setString(5, "Not Set");    // address
                pstResident.setNull(6, Types.DATE);     // birthday
                pstResident.setString(7, "None");       // email
                pstResident.setString(8, "Other");      // gender
                pstResident.setString(9, user);         // nickname
                pstResident.executeUpdate();
                
                conn.commit(); // Save both changes to the database
                return "SUCCESS";
            } catch (SQLException e) {
                conn.rollback(); // If either fails, undo everything
                System.err.println("SQL Error during registration: " + e.getMessage());
                return "Database Error: " + e.getMessage();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "System Error: " + e.getMessage();
        }
    }

    /**
     * isUsernameTaken - Explicitly checks the users table for a duplicate username
     */
    private boolean isUsernameTaken(String username) {
        String sql = "SELECT username FROM users WHERE username = ?";
        try (Connection conn = Database.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            if (conn == null) return false;
            
            pst.setString(1, username);
            ResultSet rs = pst.executeQuery();
            return rs.next(); // Returns true if a record was found
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // KEYPOINT: Modified to accept and pass the username to the UserWelcomepageController
    private void navigateToUserWelcomePage(ActionEvent event, String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("UserWelcomepage.fxml"));
            Parent root = loader.load();
            UserWelcomepageController controller = loader.getController(); // Get the controller instance
            
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            controller.initData(username); // Initialize the UserWelcomepage with the username
            
            System.out.println("Successfully navigated to User Welcome Page!");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Navigation Error", "Could not load user welcome page.");
        }
    }

    /**
     * handleRegisterClick - Triggered when the Register hyperlink is clicked
     * Navigates to the UserRegister page
     * 
     * @param event The ActionEvent from the Register hyperlink click
     */
    @FXML
    public void handleRegisterClick(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("UserRegister.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            
            System.out.println("Successfully navigated to User Register Page!");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Navigation Error", "Could not load register page.");
        }
    }

    /**
     * handleBackToLoginClick - Triggered when the Login hyperlink is clicked on the UserRegister page
     * Navigates back to the login page
     * 
     * @param event The ActionEvent from the Login hyperlink click
     */
    @FXML
    public void handleBackToLoginClick(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            
            System.out.println("Successfully navigated back to Login Page!");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Navigation Error", "Could not load login page.");
        }
    }
}