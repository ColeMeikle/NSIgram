package com.example.application.ce.webpages;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.example.application.ce.MainLayout;
import com.example.application.ce.webpages.SearchUsersView.UserDisplay;
import com.example.application.ce.webpages.backend.ChatManagement;
import com.example.application.ce.webpages.backend.RedirectUser;
import com.example.application.ce.webpages.backend.UserManagement;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
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
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinSession;

@PageTitle("Direct Messages")
@Route(value = "messages", layout = MainLayout.class)
@Menu(order = 2, icon=LineAwesomeIconUrl.PAPER_PLANE_SOLID)
public class MessagesView extends HorizontalLayout implements BeforeEnterObserver, HasUrlParameter<String> {
    private Grid<ChatDisplay> chatGrid;
    private static Grid<UserDisplay> chatMembersGrid; 
    private Grid<UserDisplay> searchUsersGrid;
    private Grid<UserDisplay> newChatUsersGrid;
    public static Map<String, ChatDisplay> chatMap = new HashMap<>();
    private String currentUser;
    private String currentChatID;
    private Timestamp oldestTimestamp;
    private Timestamp lastTimestamp;
    private String lastUser;
    private static boolean loadedAllMessages = false;
    private List<UserDisplay> allUsers = new ArrayList<>();//all users on the site
    List<UserDisplay> userList = new ArrayList<>();//all users part of currently selected chat
    List<String> newChatMembers = new ArrayList<>();//temporary chat to store new members of a created chat

    private static Div activeChatContainer;
    private static HorizontalLayout activeChatHeaderContainer;
    private static Image chatThumbnailLarge;
    private static H1 activeChatTitle;
    private static VerticalLayout noActiveChatContainer;
    private static VerticalLayout activeChatMessagesContainer;
    private static VerticalLayout sendMessagesOuterContainer;
    private static Dialog chatSettingsDialog;
    private static Dialog newChatDialog;

    
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RedirectUser.checkUserLoggedIn(event);
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String chatID){
        if (chatID == null || chatID.equals(currentChatID)) {
            return; // Skip if this chat is already loaded
        }
        updateCurrentChat(chatID);//error handling done inside of updateCurrentChat()
    }

    public MessagesView(){
        /*Left column (view all chats) */
        currentUser = (String) VaadinSession.getCurrent().getAttribute("currentUser");
        UI.getCurrent().getPage().executeJs("document.body.style.overflow = 'hidden'; document.documentElement.style.overflow = 'hidden';");//Disable scrolling
        Div viewAllChatsContainer = new Div();
        viewAllChatsContainer.setId("viewAllChatsContainer");

        H1 currentUserHeader = new H1("Hello, " + currentUser);
        currentUserHeader.addClassName("currentUserHeader");

        newChatDialog = new Dialog();
        VerticalLayout newChatDialogLayout = createNewChatLayout(newChatDialog);
        newChatDialog.add(newChatDialogLayout);
        newChatDialog.setWidth("500px");
        newChatDialog.setMaxWidth("100%");

        Button createNewChatButton = new Button(new Icon(VaadinIcon.PLUS));
        createNewChatButton.addClassName("createNewChatButton");

        createNewChatButton.addClickListener(e -> {
            allUsers = UserManagement.getAllUsers();//Update all users on the site
            allUsers.removeIf(user -> user.name.equals(currentUser));//don't want to be able to yourself to a chat
            newChatUsersGrid.setItems(allUsers);
            newChatDialog.open();
        });

        HorizontalLayout createNewChatsContainer = new HorizontalLayout(currentUserHeader, createNewChatButton);
        createNewChatsContainer.setWidthFull();
        createNewChatsContainer.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        chatGrid = new Grid<>();
        chatGrid.addThemeVariants(GridVariant.LUMO_NO_ROW_BORDERS);
        chatGrid.addClassName("chatGrid");

        chatGrid.addComponentColumn(chat -> {
            RouterLink link = new RouterLink("", MessagesView.class, chat.chatID);
            link.addClassName("routerLinkWrapper");

            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.addClassName("viewAllChatsRow");
            row.setAlignItems(FlexComponent.Alignment.CENTER);

            row.addClickListener(e -> {
                if(!chat.chatID.equals(currentChatID)){
                    UI.getCurrent().navigate("messages/" + chat.chatID);
                }
            });

            Image chatThumbnailSmall = new Image(chat.thumbnail, "Profile Picture");
            chatThumbnailSmall.addClassName("chatThumbnailSmall");
            chatThumbnailSmall.getElement().setAttribute("loading", "lazy");

            H1 chatNameSmall = new H1(ChatManagement.getChatName(chat.chatID, chat.chatName));
            chatNameSmall.addClassName("chatNameSmall");
            chatNameSmall.setId(chat.chatID);

            row.add(chatThumbnailSmall, chatNameSmall);
            link.add(row);
            return link;
        }).setAutoWidth(true);

        List<ChatDisplay> chatList = ChatManagement.getAllChats(currentUser);
        chatList.forEach(chat -> chatMap.put(chat.chatID, chat));
        chatGrid.setItems(chatList);

        viewAllChatsContainer.add(createNewChatsContainer, chatGrid);

        /*Right column (selected chat) */

        /*Div*/ activeChatContainer = new Div();
        activeChatContainer.setId("activeChatContainer");

        /*Horizontal Layout */ activeChatHeaderContainer = new HorizontalLayout();
        activeChatHeaderContainer.addClassName("activeChatHeaderContainer");
        activeChatHeaderContainer.setAlignItems(FlexComponent.Alignment.CENTER);

        /*Image*/ chatThumbnailLarge = new Image(UserManagement.DEFAULT_USER_PROFILE_PIC, "Profile Picture");
        chatThumbnailLarge.addClassName("chatThumbnailLarge");

        /*H1*/ activeChatTitle = new H1("Select a chat");//User should never see this text
        activeChatTitle.addClassName("activeChatTitle");

        chatSettingsDialog = new Dialog();
        VerticalLayout chatSettingsDialogLayout = createChatSettingsLayout(chatSettingsDialog);
        chatSettingsDialog.add(chatSettingsDialogLayout);
        chatSettingsDialog.setWidth("500px");
        chatSettingsDialog.setMaxWidth("100%");

        Button chatSettingsButton = new Button(new Icon(VaadinIcon.COG));
        chatSettingsButton.addClassName("chatSettingsButton");
        chatSettingsButton.addClickListener(e -> {
            chatSettingsDialog.open();
            updateChatSettingsLayout();
        });


        HorizontalLayout activeChatHeaderLeft = new HorizontalLayout(chatThumbnailLarge, activeChatTitle);
        activeChatHeaderLeft.setAlignItems(FlexComponent.Alignment.CENTER);
        activeChatHeaderLeft.setSpacing(true);
        activeChatHeaderLeft.setWidthFull();

        HorizontalLayout activeChatHeaderRight = new HorizontalLayout(chatSettingsButton);
        activeChatHeaderRight.setAlignItems(FlexComponent.Alignment.CENTER);

        activeChatHeaderContainer.setWidthFull();
        activeChatHeaderContainer.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        activeChatHeaderContainer.add(activeChatHeaderLeft, activeChatHeaderRight);


        activeChatHeaderContainer.add(activeChatHeaderLeft, activeChatHeaderRight);

        /*VerticalLayout*/ activeChatMessagesContainer = new VerticalLayout();
        activeChatMessagesContainer.addClassName("activeChatMessagesContainer");

        /*VerticalLayout*/ sendMessagesOuterContainer = new VerticalLayout();
        sendMessagesOuterContainer.addClassName("sendMessagesOuterContainer");

        HorizontalLayout sendMessagesInnerContainer = new HorizontalLayout();
        sendMessagesInnerContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        sendMessagesInnerContainer.setWidthFull();
        sendMessagesInnerContainer.addClassName("sendMessagesInnerContainer");

        TextField sendMessageInput = new TextField();
        sendMessageInput.setPlaceholder("Message...");
        sendMessageInput.addClassName("sendMessageInput");

        sendMessageInput.addKeyPressListener(Key.ENTER, e -> {
            String content = sendMessageInput.getValue();
            sendMessageInput.setValue("");//sendMessage() takes about a second to send, but want to reset text box instantly to give user a more responsive feel
            ChatManagement.sendMessage(currentChatID, content);
            newMessageReceived(UserManagement.getUserProfile(currentUser), currentUser, content, new Timestamp(System.currentTimeMillis()));
        });

        Button sendMessageButton = new Button(new Icon(VaadinIcon.ARROW_RIGHT));
        sendMessageButton.addClassName("sendMessageButton");
        sendMessageButton.addClickListener(e -> {
            ChatManagement.sendMessage(currentChatID, sendMessageInput.getValue());
            sendMessageInput.setValue("");
        });

        /*VerticalLayout*/ noActiveChatContainer = new VerticalLayout();
        noActiveChatContainer.addClassName("noActiveChatContainer");
        noActiveChatContainer.setAlignItems(FlexComponent.Alignment.CENTER);//horizontally aligns to the center

        Image noActiveChatImg = new Image("https://zxrwcyxrtjcobknxopsb.supabase.co/storage/v1/object/public/profile-pictures//Inbox-Empty-icon.png", "Empty Inbox");
        noActiveChatImg.addClassName("noActiveChatImg");

        H1 noActiveChatLabel = new H1("Your messages");
        noActiveChatLabel.addClassName("noActiveChatLabel");

        H1 noActiveChatSubtext = new H1("Select an existing chat or create a new one! ");
        noActiveChatSubtext.addClassName("noActiveChatSubtext");

        noActiveChatContainer.add(noActiveChatImg, noActiveChatLabel, noActiveChatSubtext);//only visible when there's no active chat to display
        sendMessagesInnerContainer.add(sendMessageInput, sendMessageButton);
        sendMessagesOuterContainer.add(sendMessagesInnerContainer);
        activeChatContainer.add(activeChatHeaderContainer, activeChatMessagesContainer, sendMessagesOuterContainer, noActiveChatContainer);

        /*Edit chat */

        add(viewAllChatsContainer, activeChatContainer);

        toggleActiveChatVisibility(false);

        UI.getCurrent().getPage().executeJs("""
            const container = document.querySelector('.activeChatMessagesContainer');
            if (container) {
                container.addEventListener('scroll', () => {
                    if (container.scrollTop < 100) {
                        $0.$server.loadMoreMessages();
                    }
                });
            }
        """, getElement());
    }
    
    private void updateCurrentChat(String chatID){
        activeChatMessagesContainer.removeAll();
        ChatDisplay chat = chatMap.get(chatID);
        currentChatID = chatID;

        if (ChatManagement.canUserAccessChat(chatID)) {//If the chat exists, load that chat on the right side
            chatThumbnailLarge.setSrc(chat.thumbnail);
            activeChatTitle.setText(ChatManagement.getChatName(chat.chatID, chat.chatName));
            loadedAllMessages = false;

            toggleActiveChatVisibility(true);
            loadChatMessages(chatID, null, null);//null means just load last 25 messages
        } else {//If the chat doesn't exist
            toggleActiveChatVisibility(false);
        }
    }

    @ClientCallable
    @SuppressWarnings("unused")
    private void loadMoreMessages(){
        if(currentChatID == null || oldestTimestamp == null || loadedAllMessages == true) return;
        loadChatMessages(currentChatID, oldestTimestamp, null);
    }

    private void loadChatMessages(String chatID, Timestamp loadMessagesBefore, MessageDisplay newMessage){//leave newMessage null if you want to load last 25 messages. If newMessage has a value, it will only add that message
        List<MessageDisplay> messages;
        if(newMessage != null){//If there's one specific message you want to load
            messages = new ArrayList<>();
            messages.add(newMessage);
        } else{//load 25 most recent
            messages = ChatManagement.getRecentMessages(chatID, loadMessagesBefore, 25);//chatID, before set time, limit on messages to load
            Collections.reverse(messages);//so newest messages are at the bottom
            if(messages.size() < 25) loadedAllMessages = true;
            if(messages.isEmpty()) return;
            oldestTimestamp = messages.get(0).timestamp;
        }
        
        VerticalLayout outerContainer = new VerticalLayout();
        outerContainer.setWidthFull();
        for(MessageDisplay message : messages){
            if(lastTimestamp == null || Duration.between(lastTimestamp.toInstant(), message.timestamp.toInstant()).toHours() > 24){
                Div timeHeader = new Div();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm, MMM d yyyy");
                String formattedTimestamp = message.timestamp.toLocalDateTime().format(formatter);
                timeHeader.setText(formattedTimestamp);
                timeHeader.addClassName("timeHeader");
                outerContainer.add(timeHeader);
                outerContainer.setAlignSelf(FlexComponent.Alignment.CENTER, timeHeader);
            }

            HorizontalLayout messageRow = new HorizontalLayout();
            messageRow.addClassName("messageRow");
            messageRow.setAlignItems(FlexComponent.Alignment.START);

            Image senderThumbnail = new Image(message.thumbnail, "profile picture");
            senderThumbnail.addClassName("messageThumbnail");
            messageRow.setAlignSelf(FlexComponent.Alignment.END, senderThumbnail);

            VerticalLayout messageContent = new VerticalLayout();
            messageContent.addClassName("messageContent");
            messageContent.setPadding(false);
            messageContent.setSpacing(false);

            HorizontalLayout messageTopBar = new HorizontalLayout();
            messageTopBar.setPadding(false);
            messageTopBar.setSpacing(false);
            messageTopBar.setWidthFull();
            messageTopBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

            H1 usernameLabel = new H1(message.username);
            usernameLabel.addClassName("messageUsername");

            Div timestampLabel = new Div();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String formattedTimestamp = message.timestamp.toLocalDateTime().format(formatter);
            timestampLabel.setText(formattedTimestamp);
            timestampLabel.addClassName("messageTimestamp");

            Div contentLabel = new Div();
            contentLabel.setText(message.content);
            contentLabel.addClassName("messageText");

            if(lastUser != null && lastUser.equals(message.username) && lastTimestamp != null && Duration.between(lastTimestamp.toInstant(), message.timestamp.toInstant()).toMinutes() < 5){//combine messages
                messageTopBar.addClassName("hidden");
                senderThumbnail.addClassName("hidden");
                messageContent.addClassName("continuedMessageContent");
            }

            messageTopBar.add(usernameLabel, timestampLabel);
            messageContent.add(messageTopBar, contentLabel);
            outerContainer.add(messageRow);

            if(currentUser.equals(message.username)){
                messageContent.addClassName("ownMessageContent");
                senderThumbnail.addClassName("invisible");
                usernameLabel.addClassName("invisible");
                messageRow.add(messageContent, senderThumbnail);
                outerContainer.setAlignSelf(Alignment.END, messageRow);
            } else{
                messageRow.add(senderThumbnail, messageContent);
            }

            lastTimestamp = message.timestamp;
            lastUser = message.username;
        }
        if(loadMessagesBefore == null){//we're loading the 25 most recent messages
            activeChatMessagesContainer.add(outerContainer);
        } else{
            activeChatMessagesContainer.addComponentAsFirst(outerContainer);
        }

        UI.getCurrent().getPage().executeJs(
            "const container = document.querySelector('.activeChatMessagesContainer');" +
            "if (container) container.scrollTop = container.scrollHeight;"
        );
    }

    public void newMessageReceived(String thumbnail, String username, String content, Timestamp timestamp){
        MessageDisplay newMessage = new MessageDisplay(thumbnail, username, content, timestamp);
        System.out.println(content);
        loadChatMessages(currentChatID, null, newMessage);
    }

    private static void toggleActiveChatVisibility(boolean visibility){
        if(visibility){//if we want to show a chat
            activeChatHeaderContainer.removeClassName("invisible");
            activeChatMessagesContainer.removeClassName("invisible");
            sendMessagesOuterContainer.removeClassName("invisible");
            noActiveChatContainer.addClassName("invisible");
        } else{//if there is no chat to display(none selected)
            activeChatHeaderContainer.addClassName("invisible");
            activeChatMessagesContainer.addClassName("invisible");
            sendMessagesOuterContainer.addClassName("invisible");
            noActiveChatContainer.removeClassName("invisible");
        }
    }

    private VerticalLayout createChatSettingsLayout(Dialog dialog) {
        TextField updateChatName = new TextField("Update Chat Name");
        updateChatName.addClassName("updateChatNameInput");

        Button updateChatNameButton = new Button("Confirm");
        updateChatNameButton.addClassName("updateChatNameButton");

        updateChatNameButton.addClickListener(e -> {
            String newChatName = updateChatName.getValue();
            dialog.setHeaderTitle(newChatName);
            activeChatTitle.setText(newChatName);
            UI.getCurrent().getPage().executeJs("document.getElementById($0).innerHTML = $1", currentChatID, newChatName);

            ChatManagement.updateChatName(currentChatID, newChatName);
        });

        HorizontalLayout updateChatNameContainer = new HorizontalLayout(updateChatName, updateChatNameButton);
        updateChatNameContainer.setWidthFull();

        H1 memberLabel = new H1("Members");
        memberLabel.addClassName("chatMembersLabel");

        chatMembersGrid = new Grid<>();
        chatMembersGrid.addClassName("chatMembersGrid");
        chatMembersGrid.setSizeFull();
        chatMembersGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        chatMembersGrid.addThemeVariants(GridVariant.LUMO_NO_ROW_BORDERS);

        chatMembersGrid.addComponentColumn(member -> {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setAlignItems(FlexComponent.Alignment.CENTER);

            Image pfp = new Image(member.imageUrl, "Profile Picture");
            pfp.addClassName("chatMemberPicture");

            H1 username = new H1(member.name);
            username.addClassName("chatMemberName");

            row.add(pfp, username);
            
            return row;
        }).setAutoWidth(true);

        TextField searchField = new TextField("Add users");
        searchField.setWidthFull();

        searchField.addValueChangeListener(e -> {
            String filter = e.getValue().toLowerCase();
            List<UserDisplay> filtered = allUsers.stream().filter(user -> user.name.toLowerCase().contains(filter)).toList();
            searchUsersGrid.setItems(filtered);
        });

        searchUsersGrid = new Grid<>();
        searchUsersGrid.setSizeFull();
        
        searchUsersGrid.addComponentColumn(user -> {
            HorizontalLayout userRow = new HorizontalLayout();
            userRow.setWidthFull();
            userRow.setAlignItems(FlexComponent.Alignment.CENTER);

            Image userPfp = new Image(user.imageUrl, "Profile Picture");
            userPfp.addClassName("chatMemberPicture");

            H1 username = new H1(user.name);
            username.addClassName("chatMemberName");

            Button addUserButton = new Button("Add");
            addUserButton.addClassName("addUserButton");

            for(UserDisplay u : userList){
                if(u.name.equals(user.name)){//if this user is already in the chat
                    addUserButton.setEnabled(false);
                }
            }

            addUserButton.addClickListener(e -> {
                ChatManagement.addUserToChat(user.name, currentChatID, false);
                addUserButton.setEnabled(false);
                userList.add(user);
                chatMembersGrid.setItems(userList);
            });

            HorizontalLayout searchUsersContainerLeft = new HorizontalLayout(userPfp, username);
            searchUsersContainerLeft.setAlignItems(FlexComponent.Alignment.CENTER);
            HorizontalLayout searchUsersContainerRight = new HorizontalLayout(addUserButton);
            searchUsersContainerRight.setAlignItems(FlexComponent.Alignment.CENTER);

            userRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            userRow.add(searchUsersContainerLeft, searchUsersContainerRight);

            return userRow;
        });


        VerticalLayout fieldLayout = new VerticalLayout(updateChatNameContainer, memberLabel, chatMembersGrid, searchField, searchUsersGrid);
        fieldLayout.addClassName("editChatLayout");
        fieldLayout.setSpacing(false);
        fieldLayout.setPadding(false);
        fieldLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        fieldLayout.expand(chatMembersGrid);



        return fieldLayout;
    }

    private void updateChatSettingsLayout(){
        chatSettingsDialog.setHeaderTitle(ChatManagement.getChatName(currentChatID, ChatManagement.getChatName(currentChatID)));

        userList = ChatManagement.getAllUsers(currentChatID);//all users in this chat
        chatMembersGrid.setItems(userList);

        allUsers = UserManagement.getAllUsers();//all users on the site
        searchUsersGrid.setItems(allUsers);
    }

    private VerticalLayout createNewChatLayout(Dialog dialog) {
        newChatMembers = new ArrayList<>();//reset list
        newChatMembers.add(currentUser);

        TextField updateChatName = new TextField("Chat Name (optional)");
        updateChatName.addClassName("updateChatNameInput");

        TextField searchField = new TextField("Add users");
        searchField.setWidthFull();

        searchField.addValueChangeListener(e -> {
            String filter = e.getValue().toLowerCase();
            List<UserDisplay> filtered = allUsers.stream().filter(user -> user.name.toLowerCase().contains(filter)).toList();
            newChatUsersGrid.setItems(filtered);
        });

        newChatUsersGrid = new Grid<>();
        newChatUsersGrid.setSizeFull();
        
        newChatUsersGrid.addComponentColumn(user -> {
            HorizontalLayout userRow = new HorizontalLayout();
            userRow.setWidthFull();
            userRow.setAlignItems(FlexComponent.Alignment.CENTER);

            Image userPfp = new Image(user.imageUrl, "Profile Picture");
            userPfp.addClassName("chatMemberPicture");

            H1 username = new H1(user.name);
            username.addClassName("chatMemberName");

            Button addUserButton = new Button("Add");
            addUserButton.addClassName("addUserButton");

            addUserButton.addClickListener(e -> {
                newChatMembers.add(user.name);
                addUserButton.setEnabled(false);
            });

            HorizontalLayout searchUsersContainerLeft = new HorizontalLayout(userPfp, username);
            searchUsersContainerLeft.setAlignItems(FlexComponent.Alignment.CENTER);
            HorizontalLayout searchUsersContainerRight = new HorizontalLayout(addUserButton);
            searchUsersContainerRight.setAlignItems(FlexComponent.Alignment.CENTER);

            userRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            userRow.add(searchUsersContainerLeft, searchUsersContainerRight);

            return userRow;
        });

        Button createChatButton = new Button("Create");
        createChatButton.addClassName("createChatButton");
        createChatButton.addClickListener(e -> {
            String newChatID = ChatManagement.createNewChat(UserManagement.getIDsFromUsers(newChatMembers), updateChatName.getValue());
            chatMap.put(newChatID, new ChatDisplay(ChatManagement.getChatProfilePic(currentUser, newChatID), ChatManagement.getChatName(newChatID, updateChatName.getValue()), newChatID));
            UI.getCurrent().navigate("messages/" + newChatID);//open the new chat
            dialog.close();
        });

        VerticalLayout fieldLayout = new VerticalLayout(updateChatName, searchField, newChatUsersGrid, createChatButton);
        fieldLayout.addClassName("editChatLayout");
        fieldLayout.setSpacing(false);
        fieldLayout.setPadding(false);
        fieldLayout.setAlignItems(FlexComponent.Alignment.STRETCH);

        return fieldLayout;
    }

    public static class ChatDisplay {//For display chats on the left side
        public String thumbnail;
        public String chatName;
        public String chatID;

        public ChatDisplay(String thumbnail, String chatName, String chatID) {
            this.thumbnail = thumbnail;
            this.chatName = chatName;
            this.chatID = chatID;
        }
    }

    public static class MessageDisplay {//for each individual message
        public String thumbnail;
        public String username;
        public String content;
        public Timestamp timestamp;

        public MessageDisplay(String thumbnail, String username, String content, Timestamp timestamp){
            this.thumbnail = thumbnail;
            this.username = username;
            this.content = content;
            this.timestamp = timestamp;
        }
    }
}