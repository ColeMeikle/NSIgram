package com.example.application.ce.webpages;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.example.application.ce.webpages.backend.CookieManager;
import com.example.application.ce.webpages.backend.Database;
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

@PageTitle("Log in")
@Route("userLogin")
public class LoginView extends VerticalLayout implements BeforeEnterObserver{

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        CookieManager.loadCurrentUserFromCookie();
        String currentUser = (String) VaadinSession.getCurrent().getAttribute("currentUser");
        if(currentUser != null){
            event.forwardTo("");//if the user is already signed in, send to homepage
        }
    }

    public LoginView(){
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER); // vertical center
        setAlignItems(Alignment.CENTER);
        addClassName("login-bg");

        Div loginBox = new Div();
        loginBox.addClassName("login-box");

        H1 header = new H1("Login");
        header.addClassName("login-header");

        TextField username = new TextField("Username");
        username.setPlaceholder("Username");
        username.setId("username");
        username.addClassName("login-input");

        PasswordField password = new PasswordField("Password");
        password.setPlaceholder("Password");
        password.setId("password");
        password.addClassName("login-input");

        Paragraph errorMsg = new Paragraph("Invalid username or password");
        errorMsg.setId("error-msg");
        errorMsg.addClassName("error-message");
        errorMsg.setVisible(false); // Hide by default

        Button loginButton = new Button("Log In");
        loginButton.addClassName("submit-button");
        
        loginButton.addClickListener(e -> loginUser(username, password, errorMsg));

        Paragraph signupText = new Paragraph("Don't have an account?");
        signupText.addClassName("newAccount_link");

        Anchor signupLink = new Anchor("userSignup", "Sign up now");
        signupLink.addClassName("newAccount_link");

        loginBox.add(header, username, password, errorMsg, loginButton, signupText, signupLink);
        add(loginBox);

    }

    private void loginUser(TextField username, PasswordField password, Paragraph errorMsg){

        //Reset error messages
        username.removeClassName("error");
        password.removeClassName("error");
        errorMsg.setVisible(false);

        try (Connection conn = Database.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?");
            stmt.setString(1, username.getValue());
            stmt.setString(2, password.getValue());
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                CookieManager.storeCurrentUserCookie(username.getValue());
                UI.getCurrent().navigate("");//send user to home page
            }
            else{
                showError("Username or password incorrect", errorMsg, username, password);
            }
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
