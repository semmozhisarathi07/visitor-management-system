package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection.java
 *
 * The ONE place where the database connection is created.
 * Never write connection code anywhere else in the project.
 *
 * ============================ IMPORTANT ============================
 * EDIT THE THREE VALUES INSIDE THE "DATABASE SETTINGS" BOX BELOW
 * TO MATCH YOUR OWN MySQL INSTALLATION.
 * ===================================================================
 */
public class DBConnection {

    // ==================== DATABASE SETTINGS ====================
    // Change these 3 values to match your computer / MySQL setup.
    private static final String DB_NAME = "visitor_management"; // database name (created by database.sql)
    private static final String USER = "root";                  // your MySQL username
    private static final String PASSWORD = "#Ferrari@123";     // your MySQL password
    // ===========================================================

    // The full JDBC address of the database.
    // 3307 is the default MySQL port for this setup.
    private static final String URL = "jdbc:mysql://127.0.0.1:3307/" + DB_NAME
        + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    // We keep one connection and reuse it (this is a small desktop application).
    private static Connection connection;

    // Private constructor: this class only has static methods.
    private DBConnection() {
    }

    /**
     * Returns a connection to the MySQL database.
     * Creates the connection the first time it is called.
     *
     * @throws SQLException if the connection cannot be created.
     *                      The message is already written in plain language.
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Load the MySQL driver (comes from mysql-connector-j in pom.xml).
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException(
                        "MySQL JDBC driver not found.\n"
                                + "Make sure mysql-connector-j is declared in pom.xml and run: mvnw clean compile", e);
            }
            try {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            } catch (SQLException e) {
                // Turn technical SQL errors into messages a normal user can understand.
                throw new SQLException(toFriendlyMessage(e), e);
            }
        }
        return connection;
    }

    /**
     * Converts common MySQL connection errors into beginner-friendly messages.
     * Normal users never see a Java stack trace.
     */
    private static String toFriendlyMessage(SQLException e) {
        String msg = String.valueOf(e.getMessage()).toLowerCase();

        if (msg.contains("access denied")) {
            return "MySQL rejected the username or password.\n"
                    + "Open DBConnection.java and check USER and PASSWORD.";
        }
        if (msg.contains("unknown database")) {
            return "Database '" + DB_NAME + "' does not exist.\n"
                    + "Run database.sql first (MySQL Workbench or: mysql -u root -p < database.sql).";
        }
        if (msg.contains("communications link failure") || msg.contains("connection refused")
                || msg.contains("connect timed out") || msg.contains("no route to host")) {
            return "Cannot connect to MySQL.\n"
                    + "Make sure MySQL Server is running and it uses port 3306.";
        }
        if (msg.contains("public key retrieval is not allowed")) {
            return "Secure connection to MySQL failed.\n"
                    + "Check the URL settings inside DBConnection.java.";
        }
        if (msg.contains("nullable target") || msg.contains("invalid connection")) {
            return "The database connection became invalid. Please restart the application.";
        }

        // Keep the technical detail in the console; users get a plain message.
        System.err.println("[DBConnection] " + e.getMessage());
        return "Database connection failed.\n"
                + "Please check that MySQL is running and that the connection settings "
                + "in DBConnection.java are correct, then start the application again.";
    }

    /**
     * Closes the shared connection (called when the application exits).
     */
    public static void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // Nothing useful to do if closing fails.
            }
            connection = null;
        }
    }
}
