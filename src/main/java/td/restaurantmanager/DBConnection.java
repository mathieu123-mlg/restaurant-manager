package td.restaurantmanager;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    public Connection getDBConnection() {
        try {
            Dotenv dotenv = Dotenv.load();

            String jdbc_url = dotenv.get("JDBC_URL");
            String username = dotenv.get("USER");
            String password = dotenv.get("PASSWORD");

            if (jdbc_url == null || username == null || password == null) {
                throw new RuntimeException("JDBC_URL, USER or PASSWORD NULL");
            }

            return DriverManager.getConnection(jdbc_url, username, password);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void closeDBConnection(Connection conn) {
        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
