package com.coagent4u.app.rest;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.coagent4u.agent.port.out.LLMPort;
import com.coagent4u.config.CoagentProperties;
import com.coagent4u.shared.Email;
import com.coagent4u.shared.UserId;
import com.coagent4u.user.domain.User;
import com.coagent4u.user.port.in.ConnectServiceUseCase;
import com.coagent4u.user.port.in.RegisterUserUseCase;
import com.coagent4u.user.port.out.OAuthTokenExchangePort;
import com.coagent4u.user.port.out.OAuthTokenExchangePort.OAuthTokenResult;
import com.coagent4u.user.port.out.UserPersistencePort;

/**
 * Standalone MockMvc tests for RestApiController.
 * Uses MockMvcBuilders.standaloneSetup() to avoid loading the full Spring
 * context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RestApiController Tests")
class RestApiControllerTest {

        @Mock
        private RegisterUserUseCase registerUserUseCase;

        @Mock
        private ConnectServiceUseCase connectServiceUseCase;

        @Mock
        private UserPersistencePort userPersistencePort;

        @Mock
        private OAuthTokenExchangePort oAuthTokenExchangePort;

        @Mock
        private LLMPort llmPort;

        @Mock
        private CoagentProperties coagentProperties;

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                RestApiController controller = new RestApiController(
                                registerUserUseCase,
                                connectServiceUseCase,
                                userPersistencePort,
                                oAuthTokenExchangePort,
                                llmPort,
                                coagentProperties);
                mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        }

        @Test
        @DisplayName("POST /api/users registers a new user")
        void registerUser() throws Exception {
                mockMvc.perform(post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "username": "alice",
                                                  "email": "alice@example.com",
                                                  "slackUserId": "U12345",
                                                  "workspaceId": "T12345"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(content().string("User registered successfully"));

                verify(registerUserUseCase).register(any(), eq("alice"), any(), any(), any());
        }

        @Test
        @DisplayName("GET /api/users/{id} returns user profile")
        void getUserProfile() throws Exception {
                UUID id = UUID.randomUUID();
                User mockUser = mock(User.class);
                when(mockUser.getUserId()).thenReturn(new UserId(id));
                when(mockUser.getUsername()).thenReturn("alice");
                when(mockUser.getEmail()).thenReturn(new Email("alice@example.com"));
                when(mockUser.isDeleted()).thenReturn(false);
                when(userPersistencePort.findById(new UserId(id))).thenReturn(Optional.of(mockUser));

                mockMvc.perform(get("/api/users/" + id))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.username").value("alice"))
                                .andExpect(jsonPath("$.email").value("alice@example.com"))
                                .andExpect(jsonPath("$.deleted").value(false));
        }

        @Test
        @DisplayName("GET /api/users/{id} returns 404 for unknown user")
        void getUserNotFound() throws Exception {
                UUID id = UUID.randomUUID();
                when(userPersistencePort.findById(new UserId(id))).thenReturn(Optional.empty());

                mockMvc.perform(get("/api/users/" + id))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /api/health returns OK")
        void healthCheck() throws Exception {
                mockMvc.perform(get("/api/health"))
                                .andExpect(status().isOk())
                                .andExpect(content().string("OK"));
        }

        @Test
        @DisplayName("GET /oauth2/callback with error returns 400")
        void oauthCallbackError() throws Exception {
                mockMvc.perform(get("/api/oauth2/callback").param("error", "access_denied"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /oauth2/callback with code and state exchanges tokens and connects")
        void oauthCallbackSuccess() throws Exception {
                UUID userId = UUID.randomUUID();
                OAuthTokenResult mockResult = new OAuthTokenResult(
                                "encrypted_access", "encrypted_refresh", Instant.now().plusSeconds(3600));
                when(oAuthTokenExchangePort.exchangeCode("auth_code_123")).thenReturn(mockResult);

                mockMvc.perform(get("/api/oauth2/callback")
                                .param("code", "auth_code_123")
                                .param("state", userId.toString()))
                                .andExpect(status().isOk())
                                .andExpect(content().string(
                                                "Google Calendar connected successfully! You can close this window."));

                verify(oAuthTokenExchangePort).exchangeCode("auth_code_123");
                verify(connectServiceUseCase).connect(
                                eq(new UserId(userId)),
                                eq("GOOGLE_CALENDAR"),
                                eq("encrypted_access"),
                                eq("encrypted_refresh"),
                                any(Instant.class));
        }

        @Test
        @DisplayName("GET /oauth2/callback without state returns 400")
        void oauthCallbackMissingState() throws Exception {
                mockMvc.perform(get("/api/oauth2/callback").param("code", "auth_code_123"))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("Missing state (userId)"));
        }

        @Test
        @DisplayName("GET /oauth2/authorize redirects to Google consent screen")
        void oauthAuthorizeRedirects() throws Exception {
                CoagentProperties.Google googleProps = new CoagentProperties.Google();
                googleProps.setClientId("test-client-id");
                googleProps.setRedirectUri("http://localhost:8080/oauth2/callback");
                when(coagentProperties.getGoogle()).thenReturn(googleProps);

                mockMvc.perform(get("/api/oauth2/authorize").param("userId", UUID.randomUUID().toString()))
                                .andExpect(status().isFound())
                                .andExpect(header().exists("Location"));
        }
}
