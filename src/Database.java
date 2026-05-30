import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    // Note: Use the exact name you used in phpMyAdmin
    private static final String URL = "jdbc:mysql://localhost:3306/resident_information_db";
    private static final String USER = "root";
    private static final String PASS = ""; // Default XAMPP password is empty

    public static Connection connect() {
        try {
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (SQLException e) {
            System.out.println("Connection Failed! Check if XAMPP MySQL is running.");
            e.printStackTrace();
            return null;
        }
    }
}