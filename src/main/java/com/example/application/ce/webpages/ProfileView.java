package com.example.application.ce.webpages;

import java.util.ArrayList;
import java.util.List;

import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.example.application.ce.webpages.MessagesView.ChatDisplay;
import com.example.application.ce.webpages.backend.ChatManagement;
import com.example.application.ce.webpages.backend.RedirectUser;
import com.example.application.ce.webpages.backend.UserManagement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@Route(":username")
@Menu(order = 1, icon = LineAwesomeIconUrl.USER_TIE_SOLID)
public class ProfileView extends HorizontalLayout implements HasDynamicTitle, HasUrlParameter<String>, BeforeEnterObserver {
    private String title = "";
    private String user;
    private String currentUser;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RedirectUser.checkUserLoggedIn(event);
    }
    
    public ProfileView() {
        setWidthFull(); // allow full width
        setJustifyContentMode(JustifyContentMode.CENTER);
    }

    @Override
    public String getPageTitle() {
        return title;
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String username) {
        user = event.getLocation().getPath();
        currentUser = (String) VaadinSession.getCurrent().getAttribute("currentUser");
        if (UserManagement.userExists(user)) {
            title = user;
            loadProfile(user, currentUser.equals(user));//username, boolean of whether this account belongs to the logged in user
        } else {
            title = "User not found";
            H1 userNotFoundError = new H1("User not found");
            userNotFoundError.addClassName("userNotFoundError");
            add(userNotFoundError);
        }
    }

    private void loadProfile(String username, Boolean isUserProfile){
        removeAll();
        Div profileContainer = new Div();
        profileContainer.addClassName("profileContainer");
        profileContainer.setWidthFull();

        String userProfile = UserManagement.getUserProfile(username);

        Image profilePic = new Image(userProfile, "Profile Picture");
        profilePic.addClassName("profilePicLarge");

        Div profileButtonContainer = new Div();
        profileButtonContainer.addClassName("profileButtonContainer");

        if(isUserProfile){//
            Button editProfileButton = new Button("Edit Profile");
            editProfileButton.addClassName("profileButton");
            editProfileButton.addClickListener(e -> UI.getCurrent().navigate("editProfile"));

            profileButtonContainer.add(editProfileButton);
        } else{
            Button messageUserButton = new Button("Message");
            messageUserButton.addClassName("profileButton");
            messageUserButton.addClickListener(e -> {
                List<String> allUserIDs = new ArrayList<>();
                allUserIDs.add(UserManagement.getIDFromUser(currentUser));
                allUserIDs.add(UserManagement.getIDFromUser(user));
                String chatID = ChatManagement.createNewChat(allUserIDs, "");
                MessagesView.chatMap.put(chatID, new ChatDisplay(userProfile, user, chatID));
                UI.getCurrent().navigate("messages/" + chatID);
            });

            profileButtonContainer.add(messageUserButton);
        }

        H1 usernameTitle = new H1(username);
        usernameTitle.addClassName("profileUserTitle");

        Paragraph userBio = new Paragraph(UserManagement.getUserBio(username));
        userBio.addClassName("profileUserBio");

        profileContainer.add(profilePic, profileButtonContainer, usernameTitle, userBio);
        add(profileContainer);
    }

}