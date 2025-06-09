package com.example.application.ce;

import java.util.List;

import com.example.application.ce.webpages.backend.Database;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.theme.lumo.LumoUtility;

import jakarta.annotation.PostConstruct;

/**
 * The main view is a top-level placeholder for other views.
 */
@Layout
@AnonymousAllowed
public class MainLayout extends AppLayout implements AfterNavigationObserver {

    private H1 viewTitle;
    private DrawerToggle toggle;
    private Div drawerContainer;
    private Div navbarContainer;

    public MainLayout() {
        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {
        toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H1();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        navbarContainer = new Div(toggle, viewTitle); // wrap toggle + title
        navbarContainer.setWidthFull();
        navbarContainer.getStyle().set("display", "flex");
        navbarContainer.getStyle().set("align-items", "center");
        navbarContainer.getStyle().set("gap", "1rem");

        addToNavbar(true, navbarContainer);
    }

    private void addDrawerContent() {
        drawerContainer = new Div(); // store a wrapper
        drawerContainer.setWidthFull();
        drawerContainer.setHeightFull();
        drawerContainer.getStyle().set("position" ,"relative");

        Div appName = new Div("NSIgram");
        appName.addClassName("appNameTitle");
        Header header = new Header(appName);

        Scroller scroller = new Scroller(createNavigation());

        drawerContainer.add(header, scroller, createFooter());

        addToDrawer(drawerContainer);
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();

        List<MenuEntry> menuEntries = MenuConfiguration.getMenuEntries();
        menuEntries.forEach(entry -> {
            if (entry.icon() != null) {
                nav.addItem(new SideNavItem(entry.title(), entry.path(), new SvgIcon(entry.icon())));
            } else {
                nav.addItem(new SideNavItem(entry.title(), entry.path()));
            }
        });

        return nav;
    }

    private Footer createFooter() {
        Footer layout = new Footer();

        Button signOutButton = new Button("Sign Out", event -> {
            UI.getCurrent().getPage().executeJs(//clear cookies
                "document.cookie.split(';').forEach(function(c) {" +
                " document.cookie = c.trim().split('=')[0] + '=; path=/; expires=Thu, 01 Jan 1970 00:00:00 UTC';" +
                "});"
            );
            VaadinSession.getCurrent().close();
            UI.getCurrent().getPage().setLocation("/userLogin"); // or whatever your login path is
        });
        signOutButton.addClassName("signOutButton");

        layout.add(signOutButton);
        layout.addClassName("signOutFooter");
        return layout;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        super.afterNavigation();
        viewTitle.setText(getCurrentPageTitle());

        String currentRoute = event.getLocation().getFirstSegment();

        boolean hideSidebar = currentRoute.equals("userLogin") || currentRoute.equals("userSignup");// use || for other

        drawerContainer.setVisible(!hideSidebar);
        navbarContainer.setVisible(!hideSidebar);
    }

    private String getCurrentPageTitle() {
        return MenuConfiguration.getPageHeader(getContent()).orElse("");
    }
    @PostConstruct
    public void init() {
        Database.init();
    }
}
