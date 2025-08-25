package com.example.application.ce.webpages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private final String currentUser;
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
    
    // Caching mechanisms
    private static final Map<String, List<PublicChat>> chatCache = new ConcurrentHashMap<>();
    private static final Map<String, String> userProfileCache = new ConcurrentHashMap<>();
    
    // Performance monitoring
    private long lastLoadTime = 0;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RedirectUser.checkUserLoggedIn(event);
    }

    public PublicMessages() {
        // Initialize currentUser from VaadinSession now that session context is available
        this.currentUser = (String) VaadinSession.getCurrent().getAttribute("currentUser");
        
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

        H1 myChatsHeader = new H1("My Public Chats");
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

        setupScrollListeners();
        
        // Load chats synchronously on first load to avoid timing issues
        loadChatsSync(popularChatsScrollContainer, loadedChats, chatLoadOffset[0], chatLoadLimit, false);
        chatLoadOffset[0] += chatLoadLimit;

        loadChatsSync(myChatsScrollContainer, myLoadedChats, chatLoadOffset[1], chatLoadLimit, true);
        chatLoadOffset[1] += chatLoadLimit;
        
        // Pre-load next batch in background
        preloadNextBatch();
    }

    private void setupScrollListeners() {
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
            loadChatsAsync(popularChatsScrollContainer, loadedChats, chatLoadOffset[0], chatLoadLimit, false);
            chatLoadOffset[0] += chatLoadLimit;
        } else if(isMyChat && !completedMyChatsScroll && myLoadedChats.size() < chatLoadMax) {
            loadChatsAsync(myChatsScrollContainer, myLoadedChats, chatLoadOffset[1], chatLoadLimit, true);
            chatLoadOffset[1] += chatLoadLimit;
        }
    }

    private void loadChatsSync(HorizontalLayout container, List<PublicChat> loadedChats, int offset, int limit, boolean isMyChat) {
        // Load chats synchronously on the main UI thread
        long startTime = System.currentTimeMillis();
        List<PublicChat> newChats = getCachedChats(offset, limit, isMyChat);
        
        if(newChats.isEmpty()){
            if(isMyChat){
                completedMyChatsScroll = true;
            } else{
                completedPopularChatsScroll = true;
            }
            return;
        }

        // Add chats directly to UI since we're on the main thread
        for (PublicChat chat : newChats) {
            addChatToContainer(container, chat, isMyChat);
            loadedChats.add(chat);
        }
        
        long loadTime = System.currentTimeMillis() - startTime;
        lastLoadTime = loadTime;
        System.out.println("Chat load time (sync): " + loadTime + "ms");
    }

    private void loadChatsAsync(HorizontalLayout container, List<PublicChat> loadedChats, int offset, int limit, boolean isMyChat) {
        // Use background thread for loading
        UI ui = UI.getCurrent();

        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            List<PublicChat> newChats = getCachedChats(offset, limit, isMyChat);
            
            if(newChats.isEmpty()){
                if(isMyChat){
                    completedMyChatsScroll = true;
                } else{
                    completedPopularChatsScroll = true;
                }
                return;
            }

            // Update UI on main thread with proper synchronization
            ui.access(() -> {
                for (PublicChat chat : newChats) {
                    addChatToContainer(container, chat, isMyChat);
                    loadedChats.add(chat);
                }
                
                long loadTime = System.currentTimeMillis() - startTime;
                lastLoadTime = loadTime;
                System.out.println("Chat load time: " + loadTime + "ms");
            });
        }).start();
    }

    private List<PublicChat> getCachedChats(int offset, int limit, boolean isMyChat) {
        String cacheKey = (isMyChat ? "my_" : "popular_") + offset + "_" + limit + "_" + currentUser;
        
        // Check cache first
        if (chatCache.containsKey(cacheKey)) {
            return chatCache.get(cacheKey);
        }
        
        // Fetch from database and cache - use thread-safe version
        List<PublicChat> chats = ChatManagement.getPublicChatsOrderedByMembersOptimized(offset, limit, isMyChat, currentUser);
        chatCache.put(cacheKey, chats);
        
        // Cache user profiles
        for (PublicChat chat : chats) {
            if (!userProfileCache.containsKey(chat.creator)) {
                userProfileCache.put(chat.creator, UserManagement.getUserFromID(chat.creator));
            }
        }
        
        return chats;
    }

    private void addChatToContainer(HorizontalLayout container, PublicChat chat, boolean isMyChat) {
        VerticalLayout chatContainer = new VerticalLayout();
        chatContainer.addClassName("publicChatContainer");

        // Lazy load images
        Image chatThumbnail = new Image();
        chatThumbnail.addClassName("publicChatThumbnail");
        chatThumbnail.setSrc("data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7"); // Placeholder
        chatThumbnail.getElement().setAttribute("data-src", chat.thumbnail);
        chatThumbnail.getElement().setAttribute("loading", "lazy");

        H1 chatTitle = new H1(chat.name);
        chatTitle.addClassName("publicChatTitle");

        HorizontalLayout chatHeaderContainer = new HorizontalLayout(chatThumbnail, chatTitle);
        chatHeaderContainer.setWidthFull();
        chatContainer.add(chatHeaderContainer);
        
        if(!isMyChat){
            // Use cached user name
            String creatorName = userProfileCache.getOrDefault(chat.creator, UserManagement.getUserFromID(chat.creator));
            H1 createdBy = new H1("Created By: " + creatorName);
            createdBy.addClassName("publicChatCreatedBy");
            chatContainer.add(createdBy);
        }

        H1 users = new H1(chat.memberCount + " users");
        users.addClassName("publicChatUserCount");

        Button joinChat = new Button("Join");
        joinChat.addClassName("publicChatJoinButton");

        joinChat.addClickListener( e -> {
            boolean wasAdded = ChatManagement.addUserToChat(currentUser, chat.ID, true);//add user to chat
            System.out.println("User was added: " + wasAdded);
            if (wasAdded) {
                // Update local member count since user was successfully added
                updateLocalMemberCount(chat.ID);
                // Update the displayed count immediately
                System.out.println(chat.name + " has " + chat.memberCount + " members.");
                users.setText((chat.memberCount) + " users");
            }
            UI.getCurrent().navigate("messages/" + chat.ID);//go to chat
        });

        HorizontalLayout chatFooterContainer = new HorizontalLayout(users, joinChat);
        chatFooterContainer.addClassName("publicChatFooter");
        chatFooterContainer.setWidthFull();
        chatFooterContainer.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        chatFooterContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        chatContainer.add(chatFooterContainer);

        container.add(chatContainer);
    }

    private void updateLocalMemberCount(String chatID) {
        // Update chats
        for (List<PublicChat> cachedChatList : chatCache.values()) {
            for (PublicChat chat : cachedChatList) {
                if (chat.ID.equals(chatID)) {
                    chat.memberCount++;
                }
            }
        }
    }

    private void preloadNextBatch() {
        // Preload next batch in background
        new Thread(() -> {
            if (!completedPopularChatsScroll && loadedChats.size() < chatLoadMax) {
                getCachedChats(chatLoadOffset[0] + chatLoadLimit, chatLoadLimit, false);
            }
            if (!completedMyChatsScroll && myLoadedChats.size() < chatLoadMax) {
                getCachedChats(chatLoadOffset[1] + chatLoadLimit, chatLoadLimit, true);
            }
        }).start();
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

    // Performance monitoring
    public long getLastLoadTime() {
        return lastLoadTime;
    }

    // Cache management
    public static void clearCache() {
        chatCache.clear();
        userProfileCache.clear();
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
