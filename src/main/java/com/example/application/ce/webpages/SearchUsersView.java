package com.example.application.ce.webpages;

import java.util.ArrayList;
import java.util.List;

import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.example.application.ce.webpages.backend.RedirectUser;
import com.example.application.ce.webpages.backend.UserManagement;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Search")
@Route("search")
@Menu(order = 1, icon = LineAwesomeIconUrl.SEARCH_SOLID)
public class SearchUsersView extends VerticalLayout implements BeforeEnterObserver{
    private Grid<UserDisplay> userGrid;
    private TextField searchField;
    private List<UserDisplay> allUsers = new ArrayList<>();

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RedirectUser.checkUserLoggedIn(event);
    }

    public SearchUsersView(){
        setSizeFull();
        searchField = new TextField("Search Users");
        searchField.setWidthFull();

        searchField.addValueChangeListener(e -> {
            String filter = e.getValue().toLowerCase();
            List<UserDisplay> filtered = allUsers.stream().filter(user -> user.name.toLowerCase().contains(filter)).toList();
            userGrid.setItems(filtered);
        });

        userGrid = new Grid<>();
        userGrid.setSizeFull();

        userGrid.addComponentColumn(user -> {
            Image img = new Image(user.imageUrl, "Profile Picture");
            img.addClassName("searchUserProfile");
            return img;
        }).setFlexGrow(0).setWidth("10%");

        userGrid.addComponentColumn(user -> {
            Anchor nameLink = new Anchor("/" + user.name, user.name);
            nameLink.addClassName("searchUsersName");
            return nameLink;
        }).setFlexGrow(1).setWidth("90%");

        allUsers = UserManagement.getAllUsers();

        userGrid.setItems(allUsers);

        add(searchField, userGrid);
    }


    public static class UserDisplay {
        public String name;
        public String imageUrl;

        public UserDisplay(String name, String imageUrl) {
            this.name = name;
            this.imageUrl = imageUrl;
        }
    }
}
