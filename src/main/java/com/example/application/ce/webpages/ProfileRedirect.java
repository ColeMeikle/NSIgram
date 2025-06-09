package com.example.application.ce.webpages;

import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.example.application.ce.webpages.backend.CookieManager;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@PageTitle("Profile")
@Route("ProfileRedirect")
@Menu(order = 10, icon = LineAwesomeIconUrl.USER_TIE_SOLID)

public class ProfileRedirect extends HorizontalLayout implements BeforeEnterObserver{
    private String currentUser;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        currentUser = (String) VaadinSession.getCurrent().getAttribute("currentUser");

        if (currentUser == null) {
            CookieManager.loadCurrentUserFromCookie();
            currentUser = (String) VaadinSession.getCurrent().getAttribute("currentUser"); // retry after loading
        }

        if (currentUser == null) {
            event.forwardTo("userLogin");
        } else {
            event.forwardTo(currentUser); // redirect to /John, etc.
        }
    }
}
