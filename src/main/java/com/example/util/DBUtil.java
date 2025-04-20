package com.example.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBUtil {
    private static final Properties props = new Properties();
    
    static {
        try {
            // 1. Explicitly register the driver
            Class.forName("oracle.jdbc.driver.OracleDriver");
            
            // 2. Load configuration
            loadProperties();
            
            // 3. Test connection immediately
            try (Connection testConn = getConnection()) {
                System.out.println("Database connection test successful");
            }
        } catch (Exception e) {
            throw new RuntimeException("Initialization failed", e);
        }
    }
    
    private static void loadProperties() throws IOException {
        try (InputStream input = DBUtil.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (input == null) {
                throw new IOException("db.properties not found in classpath");
            }
            props.load(input);
        }
    }
    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            props.getProperty("db.url"),
            props.getProperty("db.username"),
            props.getProperty("db.password")
        );
    }
}