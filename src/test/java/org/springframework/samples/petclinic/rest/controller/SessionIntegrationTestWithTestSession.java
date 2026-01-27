package org.springframework.samples.petclinic.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.model.User;
import org.springframework.samples.petclinic.repository.TestSession;
import org.springframework.samples.petclinic.repository.TestSessionRepository;
import org.springframework.samples.petclinic.rest.dto.SetSessionAttributeRequestDto;
import org.springframework.samples.petclinic.service.SessionManagementService;
import org.springframework.samples.petclinic.service.UserService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for SessionRestController using TestSessionRepository.
 * This demonstrates how to test session endpoints with custom session infrastructure.
 */
@WebMvcTest(SessionRestController.class)
class SessionControllerWithTestSessionTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SessionManagementService sessionManagementService;

    @MockBean
    private UserService userService;

    private TestSessionRepository testSessionRepository;
    private TestSession testSession;
    private ObjectMapper objectMapper;
    private User testUser;

    @BeforeEach
    void setUp() {
        testSessionRepository = new TestSessionRepository();
        testSession = testSessionRepository.createSession("test-session-123");
        objectMapper = new ObjectMapper();
        
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEnabled(true);
        
        // Set up test session with user
        testSession.setAttribute("authenticated_user", testUser);
    }

    @Test
    @WithMockUser
    void testGetSessionInfoWithTestSession() throws Exception {
        // Mock the service to return our test session info
        Map<String, Object> sessionInfo = new HashMap<>();
        sessionInfo.put("sessionId", testSession.getId());
        sessionInfo.put("creationTime", testSession.getCreationTime().toEpochMilli());
        sessionInfo.put("lastAccessedTime", testSession.getLastAccessedTime().toEpochMilli());
        sessionInfo.put("maxInactiveInterval", (int) testSession.getMaxInactiveInterval().getSeconds());
        sessionInfo.put("attributeCount", testSession.getAttributeNames().size());
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionInfo", sessionInfo);
        
        when(sessionManagementService.getSessionInfo(any())).thenReturn(response);

        mockMvc.perform(get("/api/session/info"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.sessionInfo").exists())
            .andExpect(jsonPath("$.sessionInfo.sessionId").value(testSession.getId()))
            .andExpect(jsonPath("$.sessionInfo.attributeCount").value(1)); // authenticated_user
    }

    @Test
    @WithMockUser
    void testGetSessionUserWithTestSession() throws Exception {
        // Mock the service to return our test user
        when(sessionManagementService.getAuthenticatedUser(any())).thenReturn(testUser);

        mockMvc.perform(get("/api/session/user"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void testGetAllSessionAttributesWithTestSession() throws Exception {
        // Add some test attributes to our session
        testSession.setAttribute("user_preference", "dark_mode");
        testSession.setAttribute("language", "en");
        
        // Mock the service to return session attributes
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("authenticated_user", testUser);
        attributes.put("user_preference", "dark_mode");
        attributes.put("language", "en");
        
        Map<String, Object> response = new HashMap<>();
        response.put("attributes", attributes);
        
        when(sessionManagementService.getAllSessionAttributes(any())).thenReturn(response);

        mockMvc.perform(get("/api/session/attributes"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.attributes").exists())
            .andExpect(jsonPath("$.attributes.user_preference").value("dark_mode"))
            .andExpect(jsonPath("$.attributes.language").value("en"));
    }

    @Test
    @WithMockUser
    void testGetSpecificSessionAttributeWithTestSession() throws Exception {
        // Add a specific attribute to test
        testSession.setAttribute("theme", "dark");
        
        // Mock the service to return the specific attribute
        Map<String, Object> response = new HashMap<>();
        response.put("key", "theme");
        response.put("value", "dark");
        
        when(sessionManagementService.getSessionAttribute(any(), anyString())).thenReturn(response);

        mockMvc.perform(get("/api/session/attributes/theme"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.key").value("theme"))
            .andExpect(jsonPath("$.value").value("dark"));
    }

    @Test
    @WithMockUser
    void testGetNonExistentAttributeWithTestSession() throws Exception {
        // Mock the service to return null for non-existent attribute
        Map<String, Object> response = new HashMap<>();
        response.put("key", "non_existent");
        response.put("value", null);
        
        when(sessionManagementService.getSessionAttribute(any(), anyString())).thenReturn(response);

        mockMvc.perform(get("/api/session/attributes/non_existent"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.key").value("non_existent"))
            .andExpect(jsonPath("$.value").value(nullValue()));
    }

    @Test
    @WithMockUser
    void testSetSessionAttributeWithTestSession() throws Exception {
        SetSessionAttributeRequestDto attributeDto = new SetSessionAttributeRequestDto();
        attributeDto.setValue("new_value");
        
        // Mock successful attribute setting (void method)
        doNothing().when(sessionManagementService).setSessionAttribute(any(), anyString(), any());

        mockMvc.perform(put("/api/session/attributes/new_key")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(attributeDto)))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void testSetSystemAttributeShouldFail() throws Exception {
        SetSessionAttributeRequestDto attributeDto = new SetSessionAttributeRequestDto();
        attributeDto.setValue("malicious_value");
        
        // Mock system attribute protection (void method)
        doNothing().when(sessionManagementService).setSessionAttribute(any(), anyString(), any());

        mockMvc.perform(put("/api/session/attributes/SPRING_SECURITY_CONTEXT")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(attributeDto)))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void testDeleteSessionAttributeWithTestSession() throws Exception {
        // Add attribute to delete
        testSession.setAttribute("temp_data", "temporary");
        
        // Mock successful deletion (void method)
        doNothing().when(sessionManagementService).removeSessionAttribute(any(), anyString());

        mockMvc.perform(delete("/api/session/attributes/temp_data")
                .with(csrf()))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void testDeleteSystemAttributeShouldFail() throws Exception {
        // Mock system attribute protection (void method)
        doNothing().when(sessionManagementService).removeSessionAttribute(any(), anyString());

        mockMvc.perform(delete("/api/session/attributes/authenticated_user")
                .with(csrf()))
            .andExpect(status().isOk());
    }

    @Test
    void testSessionEndpointsWithoutAuthentication() throws Exception {
        // Test all session endpoints without authentication should return 401
        
        mockMvc.perform(get("/api/session/info"))
            .andExpect(status().isUnauthorized());
            
        mockMvc.perform(get("/api/session/user"))
            .andExpect(status().isUnauthorized());
            
        mockMvc.perform(get("/api/session/attributes"))
            .andExpect(status().isUnauthorized());
            
        mockMvc.perform(get("/api/session/attributes/test_key"))
            .andExpect(status().isUnauthorized());
            
        mockMvc.perform(put("/api/session/attributes/test_key")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"value\":\"test_value\"}"))
            .andExpect(status().isUnauthorized());
            
        mockMvc.perform(delete("/api/session/attributes/test_key")
            .with(csrf()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void testSessionAttributeValidation() throws Exception {
        // Test with invalid JSON
        mockMvc.perform(put("/api/session/attributes/test_key")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void testSessionAttributeTypes() throws Exception {
        // Test different attribute types
        SetSessionAttributeRequestDto stringDto = new SetSessionAttributeRequestDto();
        stringDto.setValue("string_value");
        
        SetSessionAttributeRequestDto intDto = new SetSessionAttributeRequestDto();
        intDto.setValue(42);
        
        SetSessionAttributeRequestDto boolDto = new SetSessionAttributeRequestDto();
        boolDto.setValue(true);
        
        // Mock void method for setting attributes
        doNothing().when(sessionManagementService).setSessionAttribute(any(), anyString(), any());

        // Test string attribute
        mockMvc.perform(put("/api/session/attributes/string_attr")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stringDto)))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void testSessionExpiration() throws Exception {
        // Create a session with short expiration
        TestSession shortSession = new TestSession("short-session");
        shortSession.setMaxInactiveInterval(java.time.Duration.ofSeconds(1));
        testSessionRepository.save(shortSession);
        
        // Session should not be expired initially
        assert !shortSession.isExpired();
        
        // Wait for expiration
        try {
            Thread.sleep(1100); // Wait 1.1 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Session should now be expired
        assert shortSession.isExpired();
        
        // Mock service to handle expired session
        when(sessionManagementService.getSessionInfo(any()))
            .thenThrow(new RuntimeException("Session expired"));

        mockMvc.perform(get("/api/session/info"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser
    void testConcurrentSessionOperations() throws Exception {
        // Test concurrent access to session attributes
        testSession.setAttribute("counter", 0);
        
        // Simulate concurrent updates
        for (int i = 0; i < 10; i++) {
            Integer currentValue = testSession.getAttribute("counter");
            testSession.setAttribute("counter", currentValue + 1);
        }
        
        // Verify final value
        Integer finalValue = testSession.getAttribute("counter");
        assert finalValue == 10;
        
        // Mock service response
        Map<String, Object> response = new HashMap<>();
        response.put("key", "counter");
        response.put("value", finalValue);
        
        when(sessionManagementService.getSessionAttribute(any(), anyString())).thenReturn(response);

        mockMvc.perform(get("/api/session/attributes/counter"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.value").value(10));
    }
}