import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import java.io.File;

public class UserWelcomepageController {
    
    @FXML private Button btnAccount;
    @FXML private Button btnDocumentsUser;
    @FXML private Button btnReportsUser;
    @FXML private Button btnExit;
    @FXML private ImageView imgProfile;

    // Display Fields (Left Side)
    @FXML private Label lblSideNickname;
    @FXML private TextField txtSideContact;
    @FXML private TextField txtSideAddress;
    @FXML private TextField txtSideBirthday;
    @FXML private TextField txtSideEmail;
    @FXML private Label lblWelcomeUsername; // To display "Welcome back, [Username]"

    // Input Fields (Right Side)
    @FXML private TextField txtEditFullName;
    @FXML private TextField txtEditBirthday;
    @FXML private TextField txtEditContact;
    @FXML private TextField txtEditEmail;
    @FXML private TextField txtEditNickname;
    @FXML private TextField txtEditAddress;
    @FXML private TextField txtEditGender;
    @FXML private TextField txtEditAge;

    // Account Management Fields
    @FXML private TextField txtCurrentUsername;
    @FXML private TextField txtNewUsername;
    @FXML private PasswordField txtOldPassword;
    @FXML private PasswordField txtNewPassword;
    
    private Button currentSelectedButton = null;

    // KEYPOINT: Changed to static so the username persists when navigating between pages
    private static String loggedInUsername; 

    @FXML
    public void initialize() {
        // KEYPOINT: Added null check to prevent crash if FXML binding fails
        if (btnAccount != null) {
            setButtonAsSelected(btnAccount);
        }
        if (txtEditAge != null) txtEditAge.setEditable(false); // Make age field non-editable
        applyCircularClip();

        // If we have a username but the UI labels are empty (meaning we navigated back 
        // from another user page), reload the profile data.
        if (loggedInUsername != null && (lblWelcomeUsername == null || lblWelcomeUsername.getText().equals("User"))) {
            loadUserProfile(loggedInUsername);
        }
    }

    @FXML
    public void handleDashboard(ActionEvent event) {
        // KEYPOINT: Ensure the sidebar highlight updates when clicking Dashboard/Account
        if (btnAccount != null) {
            setButtonAsSelected(btnAccount);
            System.out.println("Account tab selected.");
        }
    }

    /**
     * applyCircularClip - Makes the profile ImageView circular regardless of the image shape.
     */
    private void applyCircularClip() {
        if (imgProfile != null) {
            // Create a circle clip based on the ImageView's size (150x150)
            Circle clip = new Circle(75, 75, 75);
            imgProfile.setClip(clip);
            System.out.println("Circular clip applied to profile image.");
        }
    }

    /**
     * handleImageClick - Opens a file chooser to change the profile picture.
     */
    @FXML
    public void handleImageClick(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        // Show the dialog
        File selectedFile = fileChooser.showOpenDialog(imgProfile.getScene().getWindow());

        if (selectedFile != null) {
            try {
                Image newImage = new Image(selectedFile.toURI().toString());
                imgProfile.setImage(newImage);
                System.out.println("Profile picture updated from: " + selectedFile.getPath());
            } catch (Exception e) {
                showAlert("Image Error", "Failed to load the selected image.");
            }
        }
    }

    /**
     * handleEditProfile - Triggered when "Edit Profile" is clicked.
     * Synchronizes the UI and saves the changes to the MySQL database for the logged-in user.
     */
    @FXML
    public void handleEditProfile(ActionEvent event) {
        // KEYPOINT: Retrieve values from the input fields
        String fullName = txtEditFullName.getText();
        String nickname = txtEditNickname.getText();
        String contact = txtEditContact.getText();
        String address = txtEditAddress.getText();
        String birthday = txtEditBirthday.getText();
        String email = txtEditEmail.getText();
        String gender = txtEditGender.getText(); // Assuming gender is also editable
        int ageValue = 0;

        // KEYPOINT: Logic to determine Classification (Male, Female, Senior, Minor)
        String classification = gender; 
        try {
            if (birthday != null && !birthday.trim().isEmpty() && !birthday.equalsIgnoreCase("Not Set")) {
                java.time.LocalDate birthDate = java.time.LocalDate.parse(birthday);
                int age = java.time.Period.between(birthDate, java.time.LocalDate.now()).getYears();
                
                // Update the Age text field in the UI
                ageValue = age;
                if (txtEditAge != null) txtEditAge.setText(String.valueOf(age));

                if (age >= 60) {
                    classification = "Senior";
                } else if (age < 18) {
                    classification = "Minor";
                }
            }
        } catch (Exception e) {
            System.out.println("Could not calculate classification: Invalid date format.");
        }

        // Update the visual display on the left side
        if (lblSideNickname != null) lblSideNickname.setText(nickname);
        if (txtSideContact != null) txtSideContact.setText(contact);
        if (txtSideAddress != null) txtSideAddress.setText(address);
        if (txtSideBirthday != null) txtSideBirthday.setText(birthday);
        if (txtSideEmail != null) txtSideEmail.setText(email);

        // KEYPOINT: Update the database record
        // We use 'username_ref' in the WHERE clause to identify the specific resident.
        String sql = "UPDATE residents SET full_name = ?, nickname = ?, contact = ?, address = ?, birthday = ?, email = ?, gender = ?, classification = ?, age = ? WHERE username_ref = ?";
        
        try (Connection conn = Database.connect();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setString(1, fullName);
            pst.setString(2, nickname);
            pst.setString(3, contact);
            pst.setString(4, address);

            // KEYPOINT: Handle potential date truncation error by checking if the string is empty or invalid
            if (birthday == null || birthday.trim().isEmpty() || birthday.equalsIgnoreCase("Not Set")) {
                pst.setNull(5, Types.DATE);
            } else {
                pst.setString(5, birthday); // Ensure the user types in YYYY-MM-DD format
            }

            pst.setString(6, email);
            pst.setString(7, gender);
            pst.setString(8, classification); // Save the calculated classification
            pst.setInt(9, ageValue);           // KEYPOINT: Persist the calculated age to DB
            pst.setString(10, loggedInUsername); // Use the stored username for the WHERE clause
            
            int rowsAffected = pst.executeUpdate();
            if (rowsAffected > 0) {
                showAlert("Success", "Profile updated successfully!");
                System.out.println("Database updated successfully for: " + loggedInUsername);
            } else {
                showAlert("Error", "No changes were made. Please ensure your profile exists.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to update profile: " + e.getMessage());
            System.err.println("Error updating profile in database for " + loggedInUsername + ": " + e.getMessage());
        }
    }

    /**
     * handleChangeUsername - Updates the username in both users and residents tables.
     */
    @FXML
    public void handleChangeUsername(ActionEvent event) {
        String current = txtCurrentUsername.getText();
        String next = txtNewUsername.getText();

        if (current.isEmpty() || next.isEmpty()) {
            showAlert("Input Error", "Please fill in both username fields.");
            return;
        }

        if (!current.equals(loggedInUsername)) {
            showAlert("Error", "The current username entered does not match your session.");
            return;
        }

        try (Connection conn = Database.connect()) {
            conn.setAutoCommit(false); // KEYPOINT: Start transaction

            String updateUsers = "UPDATE users SET username = ? WHERE username = ?";
            String updateResidents = "UPDATE residents SET username_ref = ? WHERE username_ref = ?";

            try (PreparedStatement pst1 = conn.prepareStatement(updateUsers);
                 PreparedStatement pst2 = conn.prepareStatement(updateResidents)) {
                
                pst1.setString(1, next);
                pst1.setString(2, current);
                pst1.executeUpdate();

                pst2.setString(1, next);
                pst2.setString(2, current);
                pst2.executeUpdate();

                conn.commit();
                loggedInUsername = next;
                if (lblWelcomeUsername != null) lblWelcomeUsername.setText(next);
                showAlert("Success", "Username updated successfully!");
                txtCurrentUsername.clear();
                txtNewUsername.clear();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Database Error", "Could not change username. It might already be taken.");
        }
    }

    /**
     * handleChangePassword - Updates the password for the current user.
     */
    @FXML
    public void handleChangePassword(ActionEvent event) {
        String oldPass = txtOldPassword.getText();
        String newPass = txtNewPassword.getText();

        if (oldPass.isEmpty() || newPass.isEmpty()) {
            showAlert("Input Error", "Please fill in both password fields.");
            return;
        }

        String checkSql = "SELECT * FROM users WHERE username = ? AND password = ?";
        String updateSql = "UPDATE users SET password = ? WHERE username = ?";

        try (Connection conn = Database.connect();
             PreparedStatement checkPst = conn.prepareStatement(checkSql)) {
            
            checkPst.setString(1, loggedInUsername);
            checkPst.setString(2, oldPass);
            ResultSet rs = checkPst.executeQuery();

            if (rs.next()) {
                try (PreparedStatement updatePst = conn.prepareStatement(updateSql)) {
                    updatePst.setString(1, newPass);
                    updatePst.setString(2, loggedInUsername);
                    updatePst.executeUpdate();
                    showAlert("Success", "Password changed successfully!");
                    txtOldPassword.clear();
                    txtNewPassword.clear();
                }
            } else {
                showAlert("Error", "Incorrect old password.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "An error occurred while changing password.");
        }
    }

    /**
     * initData - Called by LoginController to pass the logged-in username.
     * KEYPOINT: This method allows data to be passed between controllers during scene transitions.
     * After receiving the username, it loads the resident's profile from the database.
     * @param username The username of the newly registered/logged-in user.
     */
    public void initData(String username) {
        this.loggedInUsername = username;
        if (lblWelcomeUsername != null) {
            lblWelcomeUsername.setText(username); // Display username in welcome message
        }
        loadUserProfile(username);
    }

    /**
     * loadUserProfile - Fetches the resident's profile from the database and populates the UI.
     * @param username The username to fetch the profile for.
     */
    private void loadUserProfile(String username) {
        // KEYPOINT: SQL query now specifically targets username_ref to link the login account
        String sql = "SELECT full_name, nickname, contact, address, birthday, email, gender, age, classification FROM residents WHERE username_ref = ?";
        
        // KEYPOINT: Using try-with-resources for the Connection ensures it closes automatically.
        // This prevents the "Too Many Connections" error in MySQL.
        try (Connection conn = Database.connect();
             PreparedStatement pst = (conn != null) ? conn.prepareStatement(sql) : null) {
            
            if (pst == null) {
                System.err.println("Database connection failed. Is XAMPP running?");
                return;
            }
            
            pst.setString(1, username);
            
            // KEYPOINT: Using try-with-resources for ResultSet ensures data is released properly
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    // Populate left-side display fields
                    if (lblSideNickname != null) lblSideNickname.setText(rs.getString("nickname"));
                    if (txtSideContact != null) txtSideContact.setText(rs.getString("contact"));
                    if (txtSideAddress != null) txtSideAddress.setText(rs.getString("address"));
                    if (txtSideBirthday != null) {
                        java.sql.Date sqlDate = rs.getDate("birthday");
                        txtSideBirthday.setText(sqlDate != null ? sqlDate.toString() : "Not Set");
                    }
                    if (txtSideEmail != null) txtSideEmail.setText(rs.getString("email"));

                    // Populate right-side editable fields
                    if (txtEditFullName != null) txtEditFullName.setText(rs.getString("full_name"));
                    if (txtEditNickname != null) txtEditNickname.setText(rs.getString("nickname"));
                    if (txtEditContact != null) txtEditContact.setText(rs.getString("contact"));
                    if (txtEditAddress != null) txtEditAddress.setText(rs.getString("address"));
                    if (txtEditBirthday != null) {
                        java.sql.Date sqlDate = rs.getDate("birthday");
                        txtEditBirthday.setText(sqlDate != null ? sqlDate.toString() : "");
                    }
                    if (txtEditEmail != null) txtEditEmail.setText(rs.getString("email"));
                    if (txtEditGender != null) txtEditGender.setText(rs.getString("gender"));
                    
                    // KEYPOINT: Recalculate age on load if the saved value is 0 but birthday exists
                    try {
                        int age = rs.getInt("age");
                        java.sql.Date bday = rs.getDate("birthday");
                        
                        // If age is not set in DB, calculate it from birthday
                        if (age == 0 && bday != null) {
                            age = java.time.Period.between(bday.toLocalDate(), java.time.LocalDate.now()).getYears();
                        }
                        
                        if (txtEditAge != null) {
                            txtEditAge.setText(String.valueOf(age));
                        }
                    } catch (SQLException e) {
                        System.err.println("Database Warning: 'age' column missing.");
                    }
                    
                    System.out.println("User profile loaded for: " + username);
                } else {
                    // KEYPOINT: If profile is missing, create a default one so the user can still use the app
                    System.out.println("Profile missing for " + username + ". Creating default entry...");
                    createDefaultProfile(username);
                    loadUserProfile(username); // Reload now that the record exists
                }
            }
            
        } catch (Exception e) {
            // This will tell you exactly which column name is wrong (e.g., "Unknown column 'age'")
            e.printStackTrace();
            showAlert("Error", "Failed to load user profile: " + e.getMessage());
            System.err.println("Error loading user profile for " + username + ": " + e.getMessage());
        }
    }

    /**
     * createDefaultProfile - Creates a skeleton resident record if the user logs in 
     * but has no corresponding data in the residents table.
     */
    private void createDefaultProfile(String username) {
        String sql = "INSERT INTO residents (username_ref, full_name, classification, contact, address, email, gender, nickname, age) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = Database.connect();
             PreparedStatement pst = (conn != null) ? conn.prepareStatement(sql) : null) {
            
            if (pst == null) return;
            
            pst.setString(1, username);         // username_ref
            pst.setString(2, username);         // full_name
            pst.setString(3, "Male");           // classification
            pst.setString(4, "Not Set");        // contact
            pst.setString(5, "Not Set");        // address
            pst.setString(6, "None");           // email
            pst.setString(7, "Other");          // gender
            pst.setString(8, username);         // nickname
            pst.setInt(9, 0);                   // age
            
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleDocumentsUser(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("DocumentsUser.fxml"));
            Parent root = loader.load();
            
            // Pass the logged-in username to the documents controller
            DocumentsuserController controller = loader.getController();
            controller.initData(loggedInUsername);
            
            Stage stage = (Stage) btnDocumentsUser.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            System.out.println("Navigated to Documents User page.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: Could not load Documents User page.");
        }
    }

    @FXML
    public void handleReportsUser(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("ReportsUser.fxml"));
            Parent root = loader.load();
            
            // Ensure the session is passed to the Reports controller if it supports it
            Object controller = loader.getController();
            if (controller instanceof ReportsUserController) {
                ((ReportsUserController) controller).initData(loggedInUsername);
            }
            
            Stage stage = (Stage) btnReportsUser.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();
            System.out.println("Navigated to Reports User page.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: Could not load Reports User page.");
        }
    }

    @FXML
    public void handleExit(ActionEvent event) {
        try {
            // KEYPOINT: Clear the static session variable so a different user can log in
            loggedInUsername = null;
            
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
