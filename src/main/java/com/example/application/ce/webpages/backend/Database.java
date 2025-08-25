package com.example.application.ce.webpages.backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Database {
    // No static connection - create fresh each time
    
    private static String url;
    private static String user;
    private static String password;
    
    @Value("${spring.datasource.url}")
    public void setUrl(String url) {
        Database.url = url;
    }
    
    @Value("${spring.datasource.username}")
    public void setUser(String user) {
        Database.user = user;
    }
    
    @Value("${spring.datasource.password}")
    public void setPassword(String password) {
        Database.password = password;
    }
    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public static void init() {
        try (Connection conn = getConnection()) {
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS users (id SERIAL PRIMARY KEY, username TEXT UNIQUE, password TEXT)");
        stmt.execute("CREATE TABLE IF NOT EXISTS posts (id SERIAL PRIMARY KEY, user_id INTEGER REFERENCES users(id), content TEXT)");
        System.out.println("Database initialized.");
        } catch (SQLException e) {
        }
    }
}
