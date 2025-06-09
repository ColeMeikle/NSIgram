package com.example.application.ce.webpages;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.example.application.ce.webpages.backend.BannedUsernames;
import com.example.application.ce.webpages.backend.CookieManager;
import com.example.application.ce.webpages.backend.Database;
import com.example.application.ce.webpages.backend.PasswordConditions;
import com.example.application.ce.webpages.backend.UserManagement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@PageTitle("Sign up")
@Route("userSignup")
public class SignupView extends VerticalLayout implements BeforeEnterObserver{

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        CookieManager.loadCurrentUserFromCookie();
        String currentUser = (String) VaadinSession.getCurrent().getAttribute("currentUser");
        if(currentUser != null){
            event.forwardTo("");//if the user is already signed in, send to homepage
        }
    }

    public SignupView(){
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER); // vertical center
        setAlignItems(Alignment.CENTER);
        addClassName("login-bg");

        Div loginBox = new Div();
        loginBox.addClassName("login-box");

        H1 header = new H1("Sign Up");
        header.addClassName("login-header");

        TextField username = new TextField("Username");
        username.setPlaceholder("Username");
        username.setId("username");
        username.addClassName("login-input");
        
        PasswordField password = new PasswordField("Password");
        password.setPlaceholder("Password");
        password.setId("password");
        password.addClassName("login-input");
        password.setHelperText("A password must be at least 8 characters. It has to have at least one letter and one digit.");

        Paragraph errorMsg = new Paragraph("Invalid username or password");//this error msg will be overridden in AuthHandler.java
        errorMsg.setId("error-msg");
        errorMsg.addClassName("error-message");
        errorMsg.setVisible(false); // Hide by default

        Button loginButton = new Button("Sign Up");
        loginButton.addClassName("submit-button");
        
        loginButton.addClickListener(e -> signupUser(username, password, errorMsg));

        Paragraph signupText = new Paragraph("Already have an account?");
        signupText.addClassName("newAccount_link");

        Anchor signupLink = new Anchor("userLogin", "Log in now");
        signupLink.addClassName("newAccount_link");

        loginBox.add(header, username, password, errorMsg, loginButton, signupText, signupLink);
        add(loginBox);

    }

    private void signupUser(TextField username, PasswordField password, Paragraph errorMsg){

        //Reset error messages
        username.removeClassName("error");
        password.removeClassName("error");
        errorMsg.setVisible(false);

        String userValue = username.getValue();

        String pwdCondResult = PasswordConditions.CheckPwdConditions(password.getValue());
        if(!pwdCondResult.equals("")){//invalid
            showError(pwdCondResult, errorMsg, username, password);
            return;//exit signupUser before database is accessed
        }
        try (Connection conn = Database.getConnection()) {
            PreparedStatement checkStmt = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?");
            checkStmt.setString(1, userValue);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            if(rs.getInt(1) > 0){
                showError("Username already taken", errorMsg, username, password);
                return;
            }//if the current count for that username is not 0(username already taken), show an error and exit

            if(BannedUsernames.bannedUsers.contains(userValue)){//mostly usernames that are also existing page titles
                showError("Username already taken", errorMsg, username, password);
                return;
            }

            //if the username is not already taken, add it
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (username, password, profile, bio) VALUES (?, ?, ?, ?)");
            stmt.setString(1, username.getValue());
            stmt.setString(2, password.getValue());
            stmt.setString(3, UserManagement.DEFAULT_USER_PROFILE_PIC);
            stmt.setString(4, "");//by default the user has no bio
            stmt.executeUpdate();
            CookieManager.storeCurrentUserCookie(username.getValue());
            UI.getCurrent().navigate("");
        
        } catch (Exception e) {
            System.out.println(e);
            showError("Could not connect to database", errorMsg, username, password);
        }
    }

    private void showError(String errorValue, Paragraph errorMsg, TextField username, PasswordField password){
        errorMsg.setText(errorValue);
        errorMsg.setVisible(true);
        username.addClassName("error");
        password.addClassName("error");
    }
}
