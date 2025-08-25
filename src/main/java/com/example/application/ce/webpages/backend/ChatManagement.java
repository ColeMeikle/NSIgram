package com.example.application.ce.webpages.backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.example.application.ce.webpages.MessagesView.ChatDisplay;
import com.example.application.ce.webpages.MessagesView.MessageDisplay;
import com.example.application.ce.webpages.PublicMessages.PublicChat;
import com.example.application.ce.webpages.SearchUsersView.UserDisplay;
import com.vaadin.flow.server.VaadinSession;

public class ChatManagement {
    public static String getChatProfilePic(String user, String chatID){//returns the profile pic of the first user in a chat that is NOT the current user
        String userID = UserManagement.getIDFromUser(user);
        try (Connection conn = Database.getConnection()){
            PreparedStatement stmt = conn.prepareStatement("SELECT user_id FROM chat_members WHERE chat_id = ?::uuid");
            stmt.setString(1, chatID);
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                String firstUserID = rs.getString("user_id");//id of the first user in the chat
                if(!firstUserID.equals(userID)){
                    return UserManagement.getUserProfile(UserManagement.getUserFromID(firstUserID));
                }
                if(rs.next()){//if the first user was the current user
                    return UserManagement.getUserProfile(UserManagement.getUserFromID(rs.getString("user_id")));//profile of the second user
                }
                return UserManagement.getUserProfile(UserManagement.getUserFromID(firstUserID));//if there was only one member(the current user), still show that user's profile.
            } else{
                return UserManagement.DEFAULT_USER_PROFILE_PIC;
            }
        } catch (Exception e){
            System.out.println(e);
            return UserManagement.DEFAULT_USER_PROFILE_PIC;
        }
    }

    public static String getChatName(String chatID){
        try (Connection conn = Database.getConnection()){
            PreparedStatement stmt = conn.prepareStatement("SELECT chat_name FROM chats WHERE id = ?::uuid");
            stmt.setString(1, chatID);
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                return rs.getString("chat_name");
            }
            return null;//no chats have that chatID for some reason
        } catch (Exception e){
            System.out.println(e);
            return null;
        }
    }

    public static boolean canUserAccessChat(String chatID){//checks whether the currently signed in user has access to a certain chat
        String userID = UserManagement.getIDFromUser((String) VaadinSession.getCurrent().getAttribute("currentUser"));

        try (Connection conn = Database.getConnection()){
            PreparedStatement stmt = conn.prepareStatement("SELECT user_id FROM chat_members WHERE chat_id = ?::uuid");
            stmt.setString(1, chatID);
            ResultSet rs = stmt.executeQuery();
            while(rs.next()){
                if(rs.getString("user_id").equals(userID)){
                    return true;
                }
            }
        } catch (Exception e){
            System.out.println(e);
        }
        return false;
    }

    public static List<ChatDisplay> getAllChats(String user){//all chats that the given user has access to
        String userID = UserManagement.getIDFromUser(user);

        List<ChatDisplay> allChats = new ArrayList<>();

        try (Connection conn = Database.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT chat_id FROM chat_members WHERE user_id = ?::uuid");
            stmt.setString(1, userID);
            ResultSet rs = stmt.executeQuery();
            
            Set<String> chatIds = new HashSet<>();
            while (rs.next()) {
                chatIds.add(rs.getString("chat_id"));
            }

            Map<String, String> chatNames = getChatNames(chatIds);
            Map<String, List<String>> chatToUsers = getChatMembers(chatIds);

            Map<String, String> chatToProfileId = new HashMap<>();
            Set<String> profileIds = new HashSet<>();

            for (String chatId : chatToUsers.keySet()) {
                List<String> users = chatToUsers.get(chatId);
                String profileId = users.stream().filter(id -> !id.equals(userID)).findFirst().orElse(userID);
                chatToProfileId.put(chatId, profileId);
                profileIds.add(profileId);
            }

            Map<String, UserManagement.Pair<String, String>> userInfo = UserManagement.getUsernamesAndProfiles(profileIds);

            for (String chatId : chatIds) {
                String name = chatNames.getOrDefault(chatId, "Unknown Chat");
                String profileId = chatToProfileId.get(chatId);
                String profilePic = UserManagement.DEFAULT_USER_PROFILE_PIC;
                if (profileId != null && userInfo.containsKey(profileId)) {
                    profilePic = userInfo.get(profileId).second;
                }
                allChats.add(new ChatDisplay(profilePic, name, chatId));
            }
        } catch(Exception e) {
            System.out.println(e);
            return allChats;
        }
        return allChats;
    }

    public static Map<String, List<String>> getChatMembers(Set<String> chatIds) {
        Map<String, List<String>> chatToUsers = new HashMap<>();
        if (chatIds.isEmpty()) return chatToUsers;

        String placeholders = String.join(",", chatIds.stream().map(id -> "?::uuid").toArray(String[]::new));
        String query = "SELECT chat_id, user_id FROM chat_members WHERE chat_id IN (" + placeholders + ")";

        try (Connection conn = Database.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(query);
            int index = 1;
            for (String id : chatIds) {
                stmt.setString(index++, id);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String chatId = rs.getString("chat_id");
                String userId = rs.getString("user_id");
                chatToUsers.computeIfAbsent(chatId, k -> new ArrayList<>()).add(userId);
            }
        } catch (Exception e) {
            System.out.println("Error fetching chat members: " + e);
        }

        return chatToUsers;
    }


    public static String getChatName(String chatID, String chatName){//handles if the chat has no name, in which case the chat should show the first user that's not the current user if applicaple, otherwise if the chat has only one person then just show that one person
        if(!chatName.equals("")){
            return chatName;
        }
        String currentUserID = UserManagement.getIDFromUser((String) VaadinSession.getCurrent().getAttribute("currentUser"));
        try (Connection conn = Database.getConnection()){
            PreparedStatement stmt = conn.prepareStatement("SELECT user_id FROM chat_members WHERE chat_id = ?::uuid");//all users that are part of this chat
            stmt.setString(1, chatID);
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                String firstMember = rs.getString("user_id");
                if(!firstMember.equals(currentUserID)){//if the first member is not the current user, return them
                    return UserManagement.getUserFromID(firstMember);
                }

                if(rs.next()){
                    return UserManagement.getUserFromID(rs.getString("user_id"));//first member was the current user, and a second user exists, then return that second user
                } else{
                    return UserManagement.getUserFromID(firstMember);//if there's only one member, then just return them
                }
            } else{
                return "Chat not found";//if the user has no members then how can it show up on someone's page? Must be an error handling the uuid somewhere
            }
        } catch (Exception e){
            System.out.println(e);
            return "System error";
        }
    }

    public static void updateChatName(String chatID, String newChatName){
        try(Connection conn = Database.getConnection()){
            PreparedStatement stmt = conn.prepareStatement("UPDATE chats SET chat_name = ? WHERE id = ?::uuid");
            stmt.setString(1, newChatName);
            stmt.setString(2, chatID);
            stmt.executeQuery();
        } catch (Exception e){
            System.out.println(e);
        }
    }

    public static List<MessageDisplay> getRecentMessages(String chatID, Timestamp before, int limit){
        List<MessageDisplay> messages = new ArrayList<>();

        String query = before == null ? "SELECT * FROM messages WHERE chat_id = ?::uuid ORDER BY created_at DESC LIMIT ?" :
        "SELECT * FROM messages WHERE chat_id = ?::uuid AND created_at < ? ORDER BY created_at DESC LIMIT ?"; //if before=null, just load the 25 most recent messages

        try(Connection conn = Database.getConnection()){
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, chatID);
            if(before != null){
                stmt.setTimestamp(2, before); // return the n messages before a given time(for when user scrolls up to load more messages). When first loading a chat, before simply equals the current time
                stmt.setInt(3, limit);//how many messages we load at once(for lazy loading)
            } else{
                stmt.setInt(2, limit);
            }
            ResultSet rs = stmt.executeQuery();
            
            Set<String> senderIds = new HashSet<>();
            List<Map<String, Object>> rawMessages = new ArrayList<>();

            while (rs.next()) {
                String senderId = rs.getString("sender_id");
                senderIds.add(senderId);
                
                Map<String, Object> msgData = new HashMap<>();
                msgData.put("sender_id", senderId);
                msgData.put("content", rs.getString("content"));
                msgData.put("created_at", rs.getTimestamp("created_at"));
                rawMessages.add(msgData);
            }
            Map<String, UserManagement.Pair<String, String>> userInfo = UserManagement.getUsernamesAndProfiles(senderIds);

            for (Map<String, Object> data : rawMessages) {
                String senderId = (String) data.get("sender_id");
                UserManagement.Pair<String, String> user = userInfo.get(senderId);

                if (user != null) {
                    String username = user.first;
                    String profilePic = user.second;
                    messages.add(new MessageDisplay(profilePic, username, (String)data.get("content"), (Timestamp)data.get("created_at")));
                }
            }
            
        } catch (Exception e){
            System.out.println(e);
        }
        return messages;
    }

    public static void sendMessage(String chatID, String content){
        if(content.equals("")){
            return;//dont send empty messages
        }
        String currentUser = (String) VaadinSession.getCurrent().getAttribute("currentUser");
        try (Connection conn = Database.getConnection()){
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO messages (chat_id, sender_id, content) VALUES (?::uuid, ?::uuid, ?)");
            stmt.setString(1, chatID);
            stmt.setString(2, UserManagement.getIDFromUser(currentUser));
            stmt.setString(3, content);
            stmt.executeUpdate();
        } catch (Exception e){
            System.out.println(e);
        }
    }

    public static Map<String, String> getChatNames(Set<String> chatIds) {
        Map<String, String> names = new HashMap<>();
        if (chatIds.isEmpty()) return names;

        String placeholders = String.join(",", chatIds.stream().map(id -> "?::uuid").toArray(String[]::new));
        String query = "SELECT id, chat_name FROM chats WHERE id IN (" + placeholders + ")";

        try (Connection conn = Database.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(query);
            int index = 1;
            for (String id : chatIds) {
                stmt.setString(index++, id);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                names.put(rs.getString("id"), rs.getString("chat_name"));
            }
        } catch (Exception e) {
            System.out.println("Error fetching chat names: " + e);
        }
        return names;
    }

    public static Map<String, String> getChatThumbnails(String currentUserID, Set<String> chatIds) {
        Map<String, String> thumbnails = new HashMap<>();
        if (chatIds.isEmpty()) return thumbnails;

        String placeholders = String.join(",", chatIds.stream().map(id -> "?::uuid").toArray(String[]::new));
        String query = "SELECT chat_id, user_id FROM chat_members WHERE chat_id IN (" + placeholders + ")";

        Map<String, List<String>> chatToUsers = new HashMap<>();

        try (Connection conn = Database.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(query);
            int index = 1;
            for (String id : chatIds) {
                stmt.setString(index++, id);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String chatId = rs.getString("chat_id");
                String userId = rs.getString("user_id");
                chatToUsers.computeIfAbsent(chatId, k -> new ArrayList<>()).add(userId);
            }

            for (String chatId : chatToUsers.keySet()) {
                List<String> users = chatToUsers.get(chatId);
                String profileId = users.stream().filter(id -> !id.equals(currentUserID)).findFirst().orElse(currentUserID);
                String username = UserManagement.getUserFromID(profileId);
                String profile = UserManagement.getUserProfile(username);
                thumbnails.put(chatId, profile);
            }

        } catch (Exception e) {
            System.out.println("Error fetching chat thumbnails: " + e);
        }

        return thumbnails;
    }

    public static String createNewChat(List<String> members, String chatName){
        try(Connection conn = Database.getConnection()){
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO chats (chat_name) VALUES (?) RETURNING id");
            stmt.setString(1, chatName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()){
                String chatID = rs.getString("id");

                PreparedStatement memberStmt = conn.prepareStatement("INSERT INTO chat_members (chat_id, user_id) VALUES (?::uuid, ?::uuid)");
                for(String userID : members){
                    memberStmt.setString(1, chatID);
                    memberStmt.setString(2, userID);
                    memberStmt.addBatch();
                }
                memberStmt.executeBatch();
                return chatID;
            }


        } catch (Exception e){
            System.out.println(e);
        }
        return null;
    }

    public static String createPublicChat(String chatName, String userID){
        try(Connection conn = Database.getConnection()){
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO chats (chat_name, public, creator_id) VALUES (?, TRUE, ?::uuid) RETURNING id");
            stmt.setString(1, chatName);
            stmt.setString(2, userID);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()){
                System.out.println("test");
                String chatID = rs.getString("id");

                PreparedStatement memberStmt = conn.prepareStatement("INSERT INTO chat_members (chat_id, user_id) VALUES (?::uuid, ?::uuid)");
                memberStmt.setString(1, chatID);
                memberStmt.setString(2, userID);
                memberStmt.executeUpdate();
                return chatID;
            }


        } catch (Exception e){
            System.out.println(e);
        }
        return null;
    }

    public static List<UserDisplay> getAllUsers(String chatID){
        List<UserDisplay> users = new ArrayList<>();
        Set<String> userIDs = new HashSet<>();

        try(Connection conn = Database.getConnection()){
            PreparedStatement stmt = conn.prepareStatement("SELECT user_id FROM chat_members WHERE chat_id = ?::uuid");
            stmt.setString(1, chatID);
            ResultSet rs = stmt.executeQuery();

            while(rs.next()){
                userIDs.add(rs.getString("user_id"));
            }

            Map<String, UserManagement.Pair<String, String>> userInfo = UserManagement.getUsernamesAndProfiles(userIDs);
            for (String id : userIDs) {
                UserManagement.Pair<String, String> info = userInfo.get(id);
                if (info != null) {
                    users.add(new UserDisplay(info.first, info.second));
                }
            }

        } catch (Exception e){
            System.out.println(e);
        }

        return users;
    }

    public static boolean addUserToChat(String name, String chatID, boolean isChatPublic) {
        String userID = UserManagement.getIDFromUser(name);
        try(Connection conn = Database.getConnection()){
            PreparedStatement checkStmt = conn.prepareStatement("SELECT COUNT(*) FROM chat_members WHERE chat_id = ?::uuid AND user_id = ?::uuid");
            checkStmt.setString(1, chatID);
            checkStmt.setString(2, userID);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            if(rs.getInt(1) > 0){
                return false;//if the user is already in this chat
            }

            PreparedStatement stmt = conn.prepareStatement("INSERT INTO chat_members (chat_id, user_id) VALUES (?::uuid, ?::uuid)");
            stmt.setString(1, chatID);
            stmt.setString(2, userID);
            stmt.executeUpdate();
            if(isChatPublic){
                PreparedStatement newStmt = conn.prepareStatement("UPDATE chats SET num_members = num_members + 1 WHERE id = ?::uuid");
                newStmt.setString(1, chatID);
                newStmt.executeUpdate();
            }
            return true; // User was successfully added
        } catch (Exception e){
            System.out.println(e);
            return false;
        }
    }

    public static void removeUserFromChat(String name, String chatID){
        String userID = UserManagement.getIDFromUser(name);
        try(Connection conn = Database.getConnection()){
            PreparedStatement stmt = conn.prepareStatement("DELETE FROM chat_members WHERE user_id=?::uuid AND chat_id=?::uuid");
            stmt.setString(1, userID);
            stmt.setString(2, chatID);
            stmt.executeUpdate();

            PreparedStatement newStmt = conn.prepareStatement("UPDATE chats SET num_members = num_members - 1 WHERE id = ?::uuid");
            newStmt.setString(1, chatID);
            newStmt.executeUpdate();
        }
        catch(Exception e){
            System.out.println(e);
        }
    }

    @SuppressWarnings("ConvertToTryWithResources")
    public static List<PublicChat> getPublicChatsOrderedByMembers(int offset, int limit, boolean onlyShowUsersChats){
        List<PublicChat> publicChats = new ArrayList<>();
        String currentUser;
        String creatorID;
        String creatorThumbnail = "";

        try (Connection conn = Database.getConnection()) {
            PreparedStatement stmt;
            if(onlyShowUsersChats){
                currentUser = (String) VaadinSession.getCurrent().getAttribute("currentUser");
                creatorID = UserManagement.getIDFromUser(currentUser);
                creatorThumbnail = UserManagement.getUserProfile(currentUser);
                stmt = conn.prepareStatement("SELECT chat_name, id, num_members, creator_id FROM chats WHERE public = TRUE AND creator_id = ?::uuid ORDER BY num_members DESC OFFSET ? LIMIT ?");
                stmt.setString(1, creatorID);
                stmt.setInt(2, offset);
                stmt.setInt(3, limit);
            }else{
                stmt = conn.prepareStatement("SELECT chat_name, id, num_members, creator_id FROM chats WHERE public = TRUE ORDER BY num_members DESC OFFSET ? LIMIT ?");
                stmt.setInt(1, offset);
                stmt.setInt(2, limit);
            }
            
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String name = rs.getString("chat_name");
                String id = rs.getString("id");
                int numMembers = rs.getInt("num_members");
                String creatorId = rs.getString("creator_id");
                String thumbnail = "";

                if(onlyShowUsersChats){
                    thumbnail = creatorThumbnail;///saves a database lookup
                } else{
                    PreparedStatement userStmt = conn.prepareStatement("SELECT profile FROM users WHERE id = ?::uuid");
                    userStmt.setString(1, creatorId);
                    ResultSet userRs = userStmt.executeQuery();
                    if (userRs.next()) {
                        thumbnail = userRs.getString("profile");
                    }
                }

                publicChats.add(new PublicChat(thumbnail, name, id, numMembers, creatorId));
            }
        } catch (Exception e){
            System.out.println(e);
        }
        return publicChats;
    }

    // Optimized version with batch queries and caching
    public static List<PublicChat> getPublicChatsOrderedByMembersOptimized(int offset, int limit, boolean onlyShowUsersChats){
        List<PublicChat> publicChats = new ArrayList<>();
        
        try (Connection conn = Database.getConnection()) {
            // Use a single query with JOIN to get all data at once
            String query;
            if(onlyShowUsersChats){
                query = "SELECT c.chat_name, c.id, c.num_members, c.creator_id, u.profile " +
                        "FROM chats c " +
                        "JOIN users u ON c.creator_id = u.id " +
                        "WHERE c.public = TRUE AND c.creator_id = ?::uuid " +
                        "ORDER BY c.num_members DESC OFFSET ? LIMIT ?";
            } else {
                query = "SELECT c.chat_name, c.id, c.num_members, c.creator_id, u.profile " +
                        "FROM chats c " +
                        "JOIN users u ON c.creator_id = u.id " +
                        "WHERE c.public = TRUE " +
                        "ORDER BY c.num_members DESC OFFSET ? LIMIT ?";
            }
            
            PreparedStatement stmt = conn.prepareStatement(query);
            
            if(onlyShowUsersChats){
                String currentUser = (String) VaadinSession.getCurrent().getAttribute("currentUser");
                String creatorID = UserManagement.getIDFromUser(currentUser);
                stmt.setString(1, creatorID);
                stmt.setInt(2, offset);
                stmt.setInt(3, limit);
            } else {
                stmt.setInt(1, offset);
                stmt.setInt(2, limit);
            }
            
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String name = rs.getString("chat_name");
                String id = rs.getString("id");
                int numMembers = rs.getInt("num_members");
                String creatorId = rs.getString("creator_id");
                String thumbnail = rs.getString("profile");

                publicChats.add(new PublicChat(thumbnail, name, id, numMembers, creatorId));
            }
        } catch (Exception e){
            System.out.println(e);
            // Fallback to original method if optimized query fails
            return getPublicChatsOrderedByMembers(offset, limit, onlyShowUsersChats);
        }
        return publicChats;
    }

    // Thread-safe version that doesn't access VaadinSession
    public static List<PublicChat> getPublicChatsOrderedByMembersOptimized(int offset, int limit, boolean onlyShowUsersChats, String currentUser){
        List<PublicChat> publicChats = new ArrayList<>();
        
        try (Connection conn = Database.getConnection()) {
            // Use a single query with JOIN to get all data at once
            String query;
            if(onlyShowUsersChats){
                query = "SELECT c.chat_name, c.id, c.num_members, c.creator_id, u.profile " +
                        "FROM chats c " +
                        "JOIN users u ON c.creator_id = u.id " +
                        "WHERE c.public = TRUE AND c.creator_id = ?::uuid " +
                        "ORDER BY c.num_members DESC OFFSET ? LIMIT ?";
            } else {
                query = "SELECT c.chat_name, c.id, c.num_members, c.creator_id, u.profile " +
                        "FROM chats c " +
                        "JOIN users u ON c.creator_id = u.id " +
                        "WHERE c.public = TRUE " +
                        "ORDER BY c.num_members DESC OFFSET ? LIMIT ?";
            }
            
            PreparedStatement stmt = conn.prepareStatement(query);
            
            if(onlyShowUsersChats){
                String creatorID = UserManagement.getIDFromUser(currentUser);
                stmt.setString(1, creatorID);
                stmt.setInt(2, offset);
                stmt.setInt(3, limit);
            } else {
                stmt.setInt(1, offset);
                stmt.setInt(2, limit);
            }
            
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String name = rs.getString("chat_name");
                String id = rs.getString("id");
                int numMembers = rs.getInt("num_members");
                String creatorId = rs.getString("creator_id");
                String thumbnail = rs.getString("profile");

                publicChats.add(new PublicChat(thumbnail, name, id, numMembers, creatorId));
            }
        } catch (Exception e){
            System.out.println(e);
        }
        return publicChats;
    }
}
//thumbnail, name, ID, member count, creator
