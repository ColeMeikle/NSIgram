package com.example.application.ce.webpages;

import java.io.InputStream;
import java.util.UUID;

import com.example.application.ce.webpages.backend.CookieManager;
import com.example.application.ce.webpages.backend.ImageDatabase;
import com.example.application.ce.webpages.backend.PasswordConditions;
import com.example.application.ce.webpages.backend.RedirectUser;
import com.example.application.ce.webpages.backend.UserManagement;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

@PageTitle("Edit Profile")
@Route("editProfile")

public class EditProfileView extends VerticalLayout implements BeforeEnterObserver{
    private Image existingProfilePic;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RedirectUser.checkUserLoggedIn(event);
    }

    @SuppressWarnings("OverridableMethodCallInConstructor")
    public EditProfileView() {
        setWidthFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        loadProfile((String) VaadinSession.getCurrent().getAttribute("currentUser"));
    }

    public void loadProfile(String user){
        /*Update Profile Picture */
        Div editPfpContainer = new Div();
        editPfpContainer.addClassName("usernameContainer");
        editPfpContainer.setWidthFull();

        VerticalLayout pfpUploadLayout = new VerticalLayout();
        pfpUploadLayout.setPadding(false);
        pfpUploadLayout.setSpacing(false);
        pfpUploadLayout.setAlignItems(Alignment.START); // Aligns to left

        MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();

        Upload pfpUpload = new Upload(buffer);
        pfpUpload.addClassName("pfpUpload");
        pfpUpload.setDropAllowed(true);

        int maxFileSizeInBytes = 2 * 1024 * 1024; // 2MB
        pfpUpload.setMaxFileSize(maxFileSizeInBytes);
        pfpUpload.setAcceptedFileTypes(".jpg", "jpeg");

        pfpUpload.addFileRejectedListener(event -> {
            String errorMessage = event.getErrorMessage();

            Notification notification = Notification.show(errorMessage, 5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        });

        pfpUpload.addSucceededListener(event -> {
            String fileName = event.getFileName();
            InputStream fileData = buffer.getInputStream(fileName);
            String uniqueFileName = UUID.randomUUID().toString() + "-" + fileName;
            ImageDatabase.uploadImage(user, uniqueFileName, fileData);
             Notification.show("Succesfully update profile picture");
        });

        NativeLabel pfpUploadLabel = new NativeLabel("Upload new profile picture");
        pfpUploadLabel.addClassName("pfpUploadLabel");

        Paragraph hint = new Paragraph("Accepted file formats: JPG (.jpg)");
        hint.addClassName("pfpUploadHint");

        pfpUploadLayout.add(pfpUploadLabel, hint, pfpUpload);

        /*Image*/ existingProfilePic = new Image(UserManagement.getUserProfile(user), "Profile Picture");
        existingProfilePic.addClassName("editProfileExistingProfilePic");

        editPfpContainer.add(existingProfilePic, pfpUploadLayout);
        add(editPfpContainer);
        /*End of Update Profile Picture */

        /*Update Username */
        Div editUsernameContainer = new Div();
        editUsernameContainer.addClassName("usernameContainer");
        editUsernameContainer.setWidthFull();

        VerticalLayout usernameFieldContainer = new VerticalLayout();
        usernameFieldContainer.setSpacing(false);
        usernameFieldContainer.setPadding(false);

        TextField editUsername = new TextField("Edit username:");
        editUsername.setPlaceholder(user);
        editUsername.addClassName("editProfileInput");

        Span editUsernameError = new Span("Error");//showError updates the error message based on the situation
        editUsernameError.addClassName("editProfileError");

        usernameFieldContainer.add(editUsername, editUsernameError);

        Button editUsernameButton = new Button("Confirm");
        editUsernameButton.addClassName("editProfileButton");
        editUsernameButton.addClickListener(e -> updateUsername(user, editUsername, editUsernameError));

        editUsernameContainer.add(usernameFieldContainer, editUsernameButton);
        add(editUsernameContainer);

        /*End of update username */

        /*Update Password */
        Div editPasswordContainer = new Div();
        editPasswordContainer.addClassName("usernameContainer");
        editPasswordContainer.setWidthFull();

        VerticalLayout passwordFieldContainer = new VerticalLayout();
        passwordFieldContainer.setSpacing(false);
        passwordFieldContainer.setPadding(false);

        PasswordField editPassword = new PasswordField("Edit password:");
        editPassword.addClassName("editProfileInput");
        editPassword.setHelperText("A password must be at least 8 characters. It must have at least one letter and one digit.");

        Span editPasswordError = new Span("Error");//showError updates the error message based on the situation
        editPasswordError.addClassName("editProfileError");

        passwordFieldContainer.add(editPassword, editPasswordError);

        Button editPasswordButton = new Button("Confirm");
        editPasswordButton.addClassName("editProfileButton");
        editPasswordButton.getStyle().set("top", "-5.5%");
        editPasswordButton.addClickListener(e -> updatePassword(editPassword, editPasswordError, user));

        editPasswordContainer.add(passwordFieldContainer, editPasswordButton);
        add(editPasswordContainer);

        /*End of update password */

        /*Update Bio */

        Div editBioContainer = new Div();
        editBioContainer.addClassName("usernameContainer");
        editBioContainer.setWidthFull();

        VerticalLayout bioFieldContainer = new VerticalLayout();
        bioFieldContainer.setSpacing(false);
        bioFieldContainer.setPadding(false);

        TextArea editBio = new TextArea();
        int charLimit = 200;

        editBio.setLabel("Edit Bio:");
        editBio.setMaxLength(charLimit);
        editBio.addClassName("editBioInput");

        editBio.setHelperText("0/" + charLimit);
        editBio.setValueChangeMode(ValueChangeMode.EAGER);
        editBio.addValueChangeListener(e -> {
            e.getSource()
                    .setHelperText(e.getValue().length() + "/" + charLimit);
        });

        bioFieldContainer.add(editBio);

        Button editBioButton = new Button("Confirm");
        editBioButton.addClassName("editProfileButton");
        editBioButton.addClickListener(e -> updateBio(user, editBio));

        editBioContainer.add(bioFieldContainer, editBioButton);
        add(editBioContainer);

        /*End of update bio */

    }

    private void updateUsername(String user, TextField editUsername, Span editUsernameError){
        String newUsername = editUsername.getValue();
        if(UserManagement.userExists(newUsername)){
            editUsername.addClassName("error");
            showError(editUsernameError, "Username already taken");
            return;
        }
        if(newUsername.contains(" ")){
            editUsername.addClassName("error");
            showError(editUsernameError, "Username may not contain whitespace");
            return;
        }
        UserManagement.updateUsername(user, newUsername);
        CookieManager.storeCurrentUserCookie(newUsername);
        Notification.show("Succesfully updated username to " + newUsername);
    }

    private void updatePassword(PasswordField editPassword, Span editPasswordError, String user){
        String newPassword = editPassword.getValue();
        String checkPwdResult = PasswordConditions.CheckPwdConditions(newPassword);
        if(!checkPwdResult.equals("")){//not valid
            editPassword.addClassName("error");
            showError(editPasswordError, checkPwdResult);
            return;
        }
        UserManagement.updatePassword(user, newPassword);
        Notification.show("Succesfully updated password");
    }

    private void updateBio(String user, TextArea editBio){
        String newBio = editBio.getValue();
        UserManagement.updateBio(user, newBio);
        Notification.show("Succesfully updated bio");
    }

    private void showError(Span errorMsgText, String error){
        errorMsgText.setText(error);
        errorMsgText.addClassName("visible");//if you use setVisible, it makes elements vertically unaligned after adding a new element, so using css visibility ensures that the error msg always takes up space on the page
    }
    
}
