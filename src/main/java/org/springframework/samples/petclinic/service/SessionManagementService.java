package org.springframework.samples.petclinic.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.samples.petclinic.model.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class SessionManagementService {

    public User getAuthenticatedUser(HttpSession session) {
        return (User) session.getAttribute("authenticated_user");
    }

    public void setAuthenticatedUser(HttpSession session, User user) {
        session.setAttribute("authenticated_user", user);
        updateLastActivityTime(session);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserPreferences(HttpSession session) {
        Map<String, Object> preferences = (Map<String, Object>) session.getAttribute("user_preferences");
        if (preferences == null) {
            preferences = new HashMap<>();
            session.setAttribute("user_preferences", preferences);
        }
        return preferences;
    }

    public void setUserPreference(HttpSession session, String key, Object value) {
        Map<String, Object> preferences = getUserPreferences(session);
        preferences.put(key, value);
        session.setAttribute("user_preferences", preferences);
        updateLastActivityTime(session);
    }

    public void removeUserPreference(HttpSession session, String key) {
        Map<String, Object> preferences = getUserPreferences(session);
        preferences.remove(key);
        session.setAttribute("user_preferences", preferences);
        updateLastActivityTime(session);
    }

    public LocalDateTime getSessionCreatedTime(HttpSession session) {
        return (LocalDateTime) session.getAttribute("session_created_time");
    }

    public LocalDateTime getLastActivityTime(HttpSession session) {
        return (LocalDateTime) session.getAttribute("last_activity_time");
    }

    public void updateLastActivityTime(HttpSession session) {
        session.setAttribute("last_activity_time", LocalDateTime.now());
    }

    public Map<String, Object> getSessionInfo(HttpSession session) {
        Map<String, Object> sessionInfo = new HashMap<>();
        sessionInfo.put("sessionId", session.getId());
        sessionInfo.put("createdTime", getSessionCreatedTime(session));
        sessionInfo.put("lastActivityTime", getLastActivityTime(session));
        sessionInfo.put("maxInactiveInterval", session.getMaxInactiveInterval());
        sessionInfo.put("isNew", session.isNew());
        
        User user = getAuthenticatedUser(session);
        if (user != null) {
            sessionInfo.put("authenticated", true);
            sessionInfo.put("username", user.getUsername());
            sessionInfo.put("email", user.getEmail());
        } else {
            sessionInfo.put("authenticated", false);
        }
        
        return sessionInfo;
    }

    public void invalidateSession(HttpSession session) {
        session.invalidate();
    }
}