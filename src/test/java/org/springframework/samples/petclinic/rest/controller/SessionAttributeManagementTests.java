package org.springframework.samples.petclinic.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.samples.petclinic.model.User;
import org.springframework.samples.petclinic.service.SessionManagementService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles({"spring-data-jpa", "mysql"})
@TestPropertySource(properties = {
    "petclinic.security.enable=true",
    "petclinic.security.oauth2.enable=true",
    "spring.security.oauth2.client.registration.google.client-id=test-client-id",
    "spring.security.oauth2.client.registration.google.client-secret=test-client-secret",
    "petclinic.security.admin-emails=admin@petclinic.com,admin@example.com",
    "petclinic.security.session-timeout=1800"
})
@Transactional
public class SessionAttributeManagementTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SessionManagementService sessionManagementService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
        objectMapper = new ObjectMapper();
    }


    private void getTestUser(MockHttpSession session) {
        User user = new User();
        user.setUsername("test@example.com");
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEnabled(true);
        session.setAttribute("authenticated_user", user);
    }

    @Test
    void testSessionInfoEndpoint() throws Exception {

        MockHttpSession session = new MockHttpSession();
        getTestUser(session);
       
        // Login first
        mockMvc.perform(get("/api/auth/status")
                .session(session)
                .with(oauth2Login()))
            .andExpect(status().isOk());

        // Test session info endpoint
        mockMvc.perform(get("/api/session/info")
            .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionInfo.authenticated").value(true))
            .andExpect(jsonPath("$.sessionInfo.username").value("test@example.com"))
            .andExpect(jsonPath("$.sessionInfo.email").value("test@example.com"));
    }

}