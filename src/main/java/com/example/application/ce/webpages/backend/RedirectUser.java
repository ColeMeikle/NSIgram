package com.example.application.ce.webpages.backend;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.VaadinSession;

public class RedirectUser {
    public static void checkUserLoggedIn(BeforeEnterEvent event){
        CookieManager.loadCurrentUserFromCookie();
        String currentUser = (String) VaadinSession.getCurrent().getAttribute("currentUser");
        if(currentUser==null){//not logged in and no user in cookies means the user needs to log in before accessing the site
            event.forwardTo("userLogin");
        }
    }
}
