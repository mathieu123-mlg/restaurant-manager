package td.restaurantmanager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private Connection connection;

    public Connection getDBConnection() {
        try {
            String jdbc_url = System.getenv("JDBC_URL");
            String username = System.getenv("USERNAME");
            String password = System.getenv("PASSWORD");

            if (jdbc_url == null || username == null || password == null) {
                throw new RuntimeException("JDBC_URL, USERNAME or PASSWORD NULL");
            }

            connection = DriverManager.getConnection(jdbc_url, username, password);
            return connection;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void closeDBConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
                connection = null;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
