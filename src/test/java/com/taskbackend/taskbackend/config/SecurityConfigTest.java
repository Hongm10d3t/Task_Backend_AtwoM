package com.taskbackend.taskbackend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void health_isPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void register_isPubliclyAccessible() throws Exception {
        // Empty body reaches the controller and fails validation (400),
        // proving Spring Security did not block it (would be 401/403 otherwise).
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_isPubliclyAccessible() throws Exception {
        // Empty body reaches the controller and fails validation (400),
        // proving Spring Security did not block it (would be 401/403 otherwise).
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_isPubliclyAccessible() throws Exception {
        // Empty body reaches the controller and fails validation (400),
        // proving Spring Security did not block it (would be 401/403 otherwise).
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tasks_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tasks_withInvalidToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/tasks").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tasks_withValidToken_isAccessible() throws Exception {
        String username = "jwt_filter_test_user_" + System.currentTimeMillis();
        String registerBody = "{\"username\":\"" + username + "\",\"password\":\"secret123\"}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = "{\"username\":\"" + username + "\",\"password\":\"secret123\"}";
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginJson.get("data").get("accessToken").asString();
        assertThat(accessToken).isNotBlank();

        mockMvc.perform(get("/api/tasks").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void refresh_withValidRefreshToken_returnsNewAccessTokenThatWorksOnTasks() throws Exception {
        String username = "jwt_refresh_test_user_" + System.currentTimeMillis();
        String registerBody = "{\"username\":\"" + username + "\",\"password\":\"secret123\"}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = "{\"username\":\"" + username + "\",\"password\":\"secret123\"}";
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String refreshToken = loginJson.get("data").get("refreshToken").asString();
        assertThat(refreshToken).isNotBlank();

        String refreshBody = "{\"refreshToken\":\"" + refreshToken + "\"}";
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode refreshJson = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newAccessToken = refreshJson.get("data").get("accessToken").asString();
        assertThat(newAccessToken).isNotBlank();

        mockMvc.perform(get("/api/tasks").header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void refresh_withUnknownRefreshToken_returnsUnauthorized() throws Exception {
        String refreshBody = "{\"refreshToken\":\"this-token-does-not-exist\"}";

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void fullFlow_loginThenRefreshThenLogoutThenRefreshAgainFails() throws Exception {
        String username = "jwt_logout_flow_user_" + System.currentTimeMillis();
        String registerBody = "{\"username\":\"" + username + "\",\"password\":\"secret123\"}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = "{\"username\":\"" + username + "\",\"password\":\"secret123\"}";
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginJson.get("data").get("accessToken").asString();
        String refreshToken = loginJson.get("data").get("refreshToken").asString();

        // 1) refresh succeeds while the token is still valid
        String refreshBody = "{\"refreshToken\":\"" + refreshToken + "\"}";
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 2) logout revokes the refresh token (requires a valid access token)
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 3) refreshing again with the now-revoked token fails
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        // 4) logging out again with the same (already-revoked) token is a safe no-op, not a 500
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 5) the still-unexpired access token itself remains usable after logout,
        // since access tokens are not blacklisted in this package.
        mockMvc.perform(get("/api/tasks").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void logout_withoutAccessToken_returnsUnauthorized() throws Exception {
        String refreshBody = "{\"refreshToken\":\"some-token\"}";

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isUnauthorized());
    }
}
