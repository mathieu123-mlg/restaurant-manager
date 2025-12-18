package td.restaurantmanager;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    public Connection getDBConnection() {
        try {
            AppConfig config = new AppConfig();

            String jdbc_url = config.getJDBC_URL();
            String username = config.getUSERNAME();
            String password = config.getPASSWORD();

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
