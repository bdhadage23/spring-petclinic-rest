package org.springframework.samples.petclinic.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.model.User;
import org.springframework.samples.petclinic.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        HttpSession session = request.getSession();
        User user = null;
        
        // Handle OidcUser (for OpenID Connect providers like Google)
        if (authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
            user = findOrCreateUserFromOidcUser(oidcUser);
        } 
        
        if (user != null) {
            // Store authenticated user in session
            session.setAttribute("authenticated_user", user);
            
            // Initialize default user preferences
            Map<String, Object> preferences = new HashMap<>();
            preferences.put("theme", "light");
            preferences.put("language", "en");
            preferences.put("notifications", true);
            session.setAttribute("user_preferences", preferences);
            
            // Store session metadata
            session.setAttribute("session_created_time", LocalDateTime.now());
            session.setAttribute("last_activity_time", LocalDateTime.now());
        }
        
        // Redirect to a success page or API endpoint
        response.sendRedirect("/petclinic/api/auth/status");
    }
    
    private User findOrCreateUserFromOidcUser(OidcUser oidcUser) {
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();
        String oauthId = oidcUser.getSubject();
        String provider = "google"; // Assuming Google for OIDC
        
        return findOrCreateUser(email, name, oauthId, provider);
    }
    
    private User findOrCreateUser(String email, String name, String oauthId, String provider) {
        if (email == null) {
            return null;
        }
        
        // Try to find existing user by OAuth ID and provider
        User user = userService.findByOauthIdAndOauthProvider(oauthId, provider);
        
        if (user == null) {
            // Try to find by email (existing user linking OAuth account)
            user = userService.findByEmail(email);
            
            if (user != null) {
                // Link existing user with OAuth account
                user.setOauthId(oauthId);
                user.setOauthProvider(provider);
            } else {
                // Create new user
                user = createNewUser(email, name, oauthId, provider);
            }
        }
        
        // Update user information from OAuth provider
        updateUserFromOAuth(user, email, name);
        
        return userService.saveUser(user);
    }
    
    private User createNewUser(String email, String name, String oauthId, String provider) {
        User user = new User();
        user.setUsername(email); // Use email as username
        user.setEmail(email);
        user.setOauthId(oauthId);
        user.setOauthProvider(provider);
        user.setEnabled(true);
        
        // Parse name into first and last name
        if (name != null) {
            String[] nameParts = name.split(" ", 2);
            user.setFirstName(nameParts[0]);
            if (nameParts.length > 1) {
                user.setLastName(nameParts[1]);
            }
        }
        
        // Assign default role
        user.addRole("USER");
        
        return user;
    }
    
    private void updateUserFromOAuth(User user, String email, String name) {
        user.setEmail(email);
        
        if (name != null) {
            String[] nameParts = name.split(" ", 2);
            user.setFirstName(nameParts[0]);
            if (nameParts.length > 1) {
                user.setLastName(nameParts[1]);
            }
        }
    }
}