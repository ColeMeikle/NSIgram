package com.example.application.ce.webpages.backend;

import java.util.ArrayList;
import java.util.List;

public class BannedUsernames {
    public static final List<String> bannedUsers = new ArrayList<>();

    static{
        bannedUsers.add("editProfile");
        bannedUsers.add("");
        bannedUsers.add("userLogin");
        bannedUsers.add("userSignup");
        bannedUsers.add("messages");
        bannedUsers.add("ProfileRedirect");
        bannedUsers.add("search");
    }
}
