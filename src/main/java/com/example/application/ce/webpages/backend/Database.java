package com.example.application.ce.webpages.backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    // No static connection - create fresh each time
    //Run in terminal: export $(cat .env | xargs)
    public static Connection getConnection() throws SQLException {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");
        
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