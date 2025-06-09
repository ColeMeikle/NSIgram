package com.example.application.ce.webpages;

import java.util.ArrayList;
import java.util.List;

import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.example.application.ce.webpages.MessagesView.ChatDisplay;
import com.example.application.ce.webpages.backend.ChatManagement;
import com.example.application.ce.webpages.backend.RedirectUser;
import com.example.application.ce.webpages.backend.UserManagement;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@PageTitle("Public Chats")
@Route("")
@Menu(order = 0, icon = LineAwesomeIconUrl.CHALKBOARD_TEACHER_SOLID)
public class PublicMessages extends VerticalLayout implements BeforeEnterObserver{
    private final String currentUser = (String) VaadinSession.getCurrent().getAttribute("currentUser");
    private final int[] chatLoadOffset = {0, 0};//chat # to start searching at. Can't be an int bc ints aren't allowed in lambda functions;
    private final int chatLoadLimit = 5;
    private final int chatLoadMax = 15;
    private final HorizontalLayout popularChatsScrollContainer;
    private final HorizontalLayout myChatsScrollContainer;
    private final List<PublicChat> loadedChats;
    private final List<PublicChat> myLoadedChats;
    private boolean completedPopularChatsScroll;
    private boolean completedMyChatsScroll;
    private Dialog newChatDialog;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RedirectUser.checkUserLoggedIn(event);
    }

    public PublicMessages() {
        UI.getCurrent().getPage().executeJs("document.body.style.overflow = 'hidden'; document.documentElement.style.overflow = 'hidden';");//Disable scrolling
        setWidthFull();
        setPadding(true);

        completedPopularChatsScroll = false;

        H1 chatHeader = new H1("Popular Chats");
        chatHeader.addClassName("popularPublicChatsHeader");

        popularChatsScrollContainer = new HorizontalLayout();//view the most popular chats
        popularChatsScrollContainer.setSpacing(true);
        popularChatsScrollContainer.addClassName("browsePublicChatsContainer");
        popularChatsScrollContainer.setId("browsePublicChatsContainer");

        H1 myChatsHeader = new H1("My Chats");
        myChatsHeader.addClassName("popularPublicChatsHeader");

        myChatsScrollContainer = new HorizontalLayout();
        myChatsScrollContainer.setSpacing(true);
        myChatsScrollContainer.addClassName("browsePublicChatsContainer");
        myChatsScrollContainer.setId("browseUserChatsContainer");

        newChatDialog = new Dialog();
        VerticalLayout newChatDialogLayout = createNewChatLayout(newChatDialog);
        newChatDialog.add(newChatDialogLayout);
        newChatDialog.setWidth("300px");
        newChatDialog.setMaxWidth("100%");

        Icon createNewChatIcon = new Icon(VaadinIcon.PLUS);
        createNewChatIcon.addClassName("createNewChatIcon");

        VerticalLayout createNewChatContainer = new VerticalLayout(createNewChatIcon);
        createNewChatContainer.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        createNewChatContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        createNewChatContainer.addClassName("publicChatContainer");
        createNewChatContainer.addClassName("createPublicChatContainer");
        myChatsScrollContainer.add(createNewChatContainer);

        createNewChatContainer.addClickListener(e -> {
            newChatDialog.open();
        });


        add(chatHeader, popularChatsScrollContainer, myChatsHeader, myChatsScrollContainer);

        loadedChats = new ArrayList<>();
        myLoadedChats = new ArrayList<>();
        chatLoadOffset[0] = 0;
        chatLoadOffset[1] = 0;//1 refers to user's own loaded chats. Must be a int array so that it can be used in a lambda function

        loadChats(popularChatsScrollContainer, loadedChats, chatLoadOffset[0], chatLoadLimit, false);
        chatLoadOffset[0] += chatLoadLimit;

        loadChats(myChatsScrollContainer, myLoadedChats, chatLoadOffset[1], chatLoadLimit, true);
        chatLoadOffset[1] += chatLoadLimit;

        UI.getCurrent().getPage().executeJs("""
            const container = document.querySelector('#browsePublicChatsContainer');
            if (container) {
                container.addEventListener('scroll', () => {
                    if (container.scrollLeft + container.clientWidth >= (container.scrollWidth - 100)) {
                        $0.$server.loadMoreChats(false);
                    }
                });
            }

            const containerTwo = document.querySelector('#browseUserChatsContainer');
            if (containerTwo) {
                containerTwo.addEventListener('scroll', () => {
                    if (containerTwo.scrollLeft + containerTwo.clientWidth >= (containerTwo.scrollWidth - 100)) {
                        $0.$server.loadMoreChats(true);
                    }
                });
            }
        """, getElement());

        getElement().setProperty("chatLoadOffset", chatLoadOffset[0]);
    }


    @ClientCallable
    @SuppressWarnings("unused")
    private void loadMoreChats(boolean isMyChat) {
        if (!isMyChat && !completedPopularChatsScroll && loadedChats.size() < chatLoadMax) {
            loadChats(popularChatsScrollContainer, loadedChats, chatLoadOffset[0], chatLoadLimit, false);
            chatLoadOffset[0] += chatLoadLimit;
        } else if(isMyChat && !completedMyChatsScroll && myLoadedChats.size() < chatLoadMax) {
            loadChats(myChatsScrollContainer, myLoadedChats, chatLoadOffset[1], chatLoadLimit, true);
            chatLoadOffset[1] += chatLoadLimit;
        }
    }

    private void loadChats(HorizontalLayout container, List<PublicChat> loadedChats, int offset, int limit, boolean isMyChat) {
        List<PublicChat> newChats = ChatManagement.getPublicChatsOrderedByMembers(offset, limit, isMyChat);
        
        if(newChats.isEmpty()){
            if(isMyChat){
                completedMyChatsScroll = true;
            } else{
                completedPopularChatsScroll = true;
            }
            return;
        }

        for (PublicChat chat : newChats) {
            VerticalLayout chatContainer = new VerticalLayout();
            chatContainer.addClassName("publicChatContainer");

            Image chatThumbnail = new Image(chat.thumbnail, "Profile Picture");
            chatThumbnail.addClassName("publicChatThumbnail");

            H1 chatTitle = new H1(chat.name);
            chatTitle.addClassName("publicChatTitle");

            HorizontalLayout chatHeaderContainer = new HorizontalLayout(chatThumbnail, chatTitle);
            chatHeaderContainer.setWidthFull();
            chatContainer.add(chatHeaderContainer);
            if(!isMyChat){
                H1 createdBy = new H1("Created By: " + UserManagement.getUserFromID(chat.creator));
                createdBy.addClassName("publicChatCreatedBy");
                chatContainer.add(createdBy);
            }

            H1 users = new H1(chat.memberCount + " users");
            users.addClassName("publicChatUserCount");

            Button joinChat = new Button("Join");
            joinChat.addClassName("publicChatJoinButton");

            joinChat.addClickListener( e -> {
                ChatManagement.addUserToChat(currentUser, chat.ID, true);//add user to chat
                UI.getCurrent().navigate("messages/" + chat.ID);//go to chat
            });

            HorizontalLayout chatFooterContainer = new HorizontalLayout(users, joinChat);
            chatFooterContainer.addClassName("publicChatFooter");
            chatFooterContainer.setWidthFull();
            chatFooterContainer.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            chatFooterContainer.setAlignItems(FlexComponent.Alignment.CENTER);
            chatContainer.add(chatFooterContainer);



            container.add(chatContainer);
            loadedChats.add(chat);
        }
    }

    private VerticalLayout createNewChatLayout(Dialog dialog){
        H1 newChatHeader = new H1("Create Public Chat");
        newChatHeader.getStyle().set("font-size", "1.5em");

        TextField newChatName = new TextField("Chat Name");
        newChatName.addClassName("updateChatNameInput");

        Button createChatButton = new Button("Create");
        createChatButton.addClassName("createChatButton");
        createChatButton.addClickListener(e -> {
            String newChatID = ChatManagement.createPublicChat(newChatName.getValue(), UserManagement.getIDFromUser(currentUser));
            MessagesView.chatMap.put(newChatID, new ChatDisplay(ChatManagement.getChatProfilePic(currentUser, newChatID), ChatManagement.getChatName(newChatID, newChatName.getValue()), newChatID));

            UI.getCurrent().navigate("messages/" + newChatID);//open the new chat
            dialog.close();
        });


        VerticalLayout fieldLayout = new VerticalLayout(newChatHeader, newChatName, createChatButton);
        fieldLayout.setSpacing(false);
        fieldLayout.setPadding(false);
        fieldLayout.setAlignItems(FlexComponent.Alignment.STRETCH);

        return fieldLayout;
    }

    public static class PublicChat{
        public String thumbnail;
        public String name;
        public String ID;
        public int memberCount;
        public String creator;

        public PublicChat(String thumbnail, String name, String ID, int memberCount, String creator){
            this.thumbnail = thumbnail;
            this.name = name;
            this.ID = ID;
            this.memberCount = memberCount;
            this.creator = creator;
        }
    }


}
