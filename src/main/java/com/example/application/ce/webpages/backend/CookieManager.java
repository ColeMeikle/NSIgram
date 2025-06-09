package com.example.application.ce.webpages.backend;

import java.util.Arrays;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;

public class CookieManager {
        public static void storeCurrentUserCookie(String username) {
            VaadinSession.getCurrent().setAttribute("currentUser", username);
            UI.getCurrent().getPage().executeJs(
                "document.cookie = 'currentUser=' + $0 + '; path=/; max-age=' + (60 * 60 * 24 * 30);",
                username
            );
        }

        public static void loadCurrentUserFromCookie() {
            UI.getCurrent().getPage().executeJs("return document.cookie")
                .then(String.class, cookieString -> {
                    if (cookieString != null) {
                        Arrays.stream(cookieString.split(";"))
                            .map(String::trim)
                            .filter(cookie -> cookie.startsWith("currentUser="))
                            .findFirst()
                            .ifPresent(cookie -> {
                                String username = cookie.substring("currentUser=".length());
                                VaadinSession.getCurrent().setAttribute("currentUser", username);
                            });
                    }
                });
        }
}
