package td.restaurantmanager;

public class AppConfig {
    private final String JDBC_URL;
    private final String USERNAME;
    private final String PASSWORD;

    public AppConfig() {
        try {
            this.JDBC_URL = System.getenv("JDBC_URL");
            this.USERNAME = System.getenv("USERNAME");
            this.PASSWORD = System.getenv("PASSWORD");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (this.JDBC_URL == null || this.USERNAME == null || this.PASSWORD == null) {
            throw new RuntimeException("JDBC_URL, USERNAME or PASSWORD NULL");
        }
    }

    public String getJDBC_URL() {
        return JDBC_URL;
    }

    public String getUSERNAME() {
        return USERNAME;
    }

    public String getPASSWORD() {
        return PASSWORD;
    }
}
