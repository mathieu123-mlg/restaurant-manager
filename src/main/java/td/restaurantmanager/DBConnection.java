package td.restaurantmanager;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private Connection connection;

    public Connection getDBConnection() {
        try {
            Dotenv dotenv = Dotenv.load();

            String jdbc_url = dotenv.get("DB_JDBC_URL");
            String username = dotenv.get("DB_USERNAME");
            String password = dotenv.get("DB_PASSWORD");

            if (jdbc_url == null || username == null || password == null) {
                throw new RuntimeException("DB_JDBC_URL, DB_USERNAME or DB_PASSWORD NULL");
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
