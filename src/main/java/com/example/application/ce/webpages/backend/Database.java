package com.example.application.ce.webpages.backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    // No static connection - create fresh each time
    public static Connection getConnection() throws SQLException {
        String url = "jdbc:postgresql://aws-0-us-west-1.pooler.supabase.com:6543/postgres?prepareThreshold=0";
        String user = "postgres.zxrwcyxrtjcobknxopsb";
        String password = "APCSDatabase101"; // Try to figure out how to hide this later
        
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