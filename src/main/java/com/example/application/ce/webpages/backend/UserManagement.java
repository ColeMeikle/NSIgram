package com.example.application.ce.webpages.backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.application.ce.webpages.SearchUsersView.UserDisplay;

public class UserManagement {
    final public static String DEFAULT_USER_PROFILE_PIC = "https://zxrwcyxrtjcobknxopsb.supabase.co/storage/v1/object/public/profile-pictures/default_user.jpg";

    public static boolean userExists(String user){
        try (Connection conn = Database.getConnection()) {
            PreparedStatement checkStmt = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?");
            checkStmt.setString(1, user);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
    }

    public static List<UserDisplay> getAllUsers(){
        List<UserDisplay> allUsers = new ArrayList<>();

        try (Connection conn = Database.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT username, profile FROM users");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()){
                String username = rs.getString("username");
                String profileUrl = rs.getString("profile");
                allUsers.add(new UserDisplay(username, profileUrl));
            }
        } catch (Exception e) {
            System.out.println(e);
            return allUsers;
        }
        return allUsers;
    }

    public static String getUserFromID(String userID){
        try (Connection conn = Database.getConnection()){
            PreparedStatement stmt = conn.prepareStatement("SELECT username FROM users WHERE id = ?::uuid");
            stmt.setString(1, userID);
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                return rs.getString("username");
            } else{
                return null;
            }
        } catch (Exception e){
            System.out.println(e);
            return null;
        }
    }

    public static String getIDFromUser(String user){
        try (Connection conn = Database.getConnection()){
            PreparedStatement stmt = conn.prepareStatement("SELECT id FROM users WHERE username = ?");
            stmt.setString(1, user);
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                return rs.getString("id");
            } else{
                return null;
            }
        } catch (Exception e){
            System.out.println(e);
            return null;
        }
    }

    public static List<String> getIDsFromUsers(List<String> users){//bulk version of previous method that finds ids in batches
        List<String> userIDs = new ArrayList<>();
        if (users.isEmpty()) return userIDs;

        try (Connection conn = Database.getConnection()) {
            String placeholders = String.join(",", Collections.nCopies(users.size(), "?"));
            PreparedStatement stmt = conn.prepareStatement("SELECT username, id FROM users WHERE username IN (" + placeholders + ")");
            int index = 1;
            for (String username : users) {
                stmt.setString(index++, username);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                userIDs.add(rs.getString("id"));
            }
        } catch (Exception e) {
            System.out.println("Error fetching user IDs in batch: " + e.getMessage());
        }

        return userIDs;//not necessarily in the same order as users
    }

    public static String getUserProfile(String user){
        try(Connection conn = Database.getConnection()){
            PreparedStatement stmt = conn.prepareStatement("SELECT profile FROM users WHERE username = ?");
            stmt.setString(1, user);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("profile");
            } else {
                // No such user found, return default profile
                return DEFAULT_USER_PROFILE_PIC;
            }
        }
        catch (Exception e){
            System.out.println(e);
            return DEFAULT_USER_PROFILE_PIC;
        }
    }

    public static void updateUserProfilePicture(String user, String fileName){
        try(Connection conn = Database.getConnection()){
            PreparedStatement stmt = conn.prepareStatement("UPDATE users SET profile = ? WHERE username = ?");
            stmt.setString(1, "https://zxrwcyxrtjcobknxopsb.supabase.co/storage/v1/object/public/profile-pictures//" + fileName);
            stmt.setString(2, user);

            stmt.executeUpdate();
        }
        catch (Exception e){
            System.out.println(e);
        }
    }

    public static String getUserBio(String user){
        try(Connection conn = Database.getConnection()){
            PreparedStatement stmt = conn.prepareStatement("SELECT bio FROM users WHERE username = ?");
            stmt.setString(1, user);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("bio");
            } else {
                // No such user found, return empty bio
                return "";
            }
        }
        catch (Exception e){
            System.out.println(e);
            return "";
        }
    }

    public static void updateUsername(String oldUser, String newUser){
        try (Connection conn = Database.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("UPDATE users SET username = ? WHERE username = ?");
            stmt.setString(1, newUser);
            stmt.setString(2, oldUser);

            stmt.executeUpdate();
            CookieManager.storeCurrentUserCookie(newUser);//keep user signed in with their new username
        } catch (Exception e) {
            System.out.println("Error updating username: " + e.getMessage());
        }
    }

    public static void updatePassword(String user, String newPwd){
        try (Connection conn = Database.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("UPDATE users SET password = ? WHERE username = ?");
            stmt.setString(1, newPwd);
            stmt.setString(2, user);

            stmt.executeUpdate();
        } catch (Exception e) {
            System.out.println("Error updating password: " + e.getMessage());
        }
    }

    public static void updateBio(String user, String bio){
        try (Connection conn = Database.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("UPDATE users SET bio = ? WHERE username = ?");
            stmt.setString(1, bio);
            stmt.setString(2, user);

            stmt.executeUpdate();
        } catch (Exception e) {
            System.out.println("Error updating bio: " + e.getMessage());
        }
    }

    public static Map<String, Pair<String, String>> getUsernamesAndProfiles(Set<String> userIds) {
        Map<String, Pair<String, String>> userMap = new HashMap<>();
        if (userIds.isEmpty()) return userMap;

        try (Connection conn = Database.getConnection()) {
            String placeholders = String.join(",", Collections.nCopies(userIds.size(), "?::uuid"));
            PreparedStatement stmt = conn.prepareStatement("SELECT id, username, profile FROM users WHERE id IN (" + placeholders + ")");
            int index = 1;
            stmt.setString(1, placeholders);
            for (String id : userIds) {
                stmt.setString(index++, id);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String id = rs.getString("id");
                String username = rs.getString("username");
                String profile = rs.getString("profile") != null ? rs.getString("profile") : DEFAULT_USER_PROFILE_PIC;
                userMap.put(id, new Pair<>(username, profile));
            }
        } catch (Exception e) {
            System.out.println("Error fetching user data in batch: " + e.getMessage());
        }

        return userMap;
    }
    
    public static class Pair<K, V> {
        public final K first;
        public final V second;

        public Pair(K first, V second) {
            this.first = first;
            this.second = second;
        }
    }
}
