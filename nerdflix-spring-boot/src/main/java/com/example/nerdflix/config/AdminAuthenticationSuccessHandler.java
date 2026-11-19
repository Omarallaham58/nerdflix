package com.example.nerdflix.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AdminAuthenticationSuccessHandler implements AuthenticationSuccessHandler {//Checked

    private final SavedRequestAwareAuthenticationSuccessHandler defaultHandler;

    public AdminAuthenticationSuccessHandler(
            @Qualifier("defaultAdminAuthSuccessHandler") SavedRequestAwareAuthenticationSuccessHandler defaultHandler) {
        this.defaultHandler = defaultHandler;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(g -> g.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            // Delegate to the default handler – it will redirect to saved request or default target URL
            defaultHandler.onAuthenticationSuccess(request, response, authentication);
        } else {
            // Not an admin – log out and redirect to login with error
            SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
            logoutHandler.logout(request, response, authentication);
            response.sendRedirect("/login?error=AccessDenied");
        }
    }
}