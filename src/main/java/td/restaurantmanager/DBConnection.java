package td.restaurantmanager;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    public Connection getDBConnection() {
        try {
            String jdbc_url = System.getenv("JDBC_URL");
            String username = System.getenv("USERNAME");
            String password = System.getenv("PASSWORD");

            if (jdbc_url == null || username == null || password == null) {
                throw new RuntimeException("JDBC_URL, USERNAME or PASSWORD NULL");
            }

            return DriverManager.getConnection(jdbc_url, username, password);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void closeDBConnection() {
        if (this.getDBConnection() != null) {
            try {
                this.getDBConnection().close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
