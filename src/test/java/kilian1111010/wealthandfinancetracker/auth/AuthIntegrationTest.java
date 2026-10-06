package kilian1111010.wealthandfinancetracker.auth;

import kilian1111010.wealthandfinancetracker.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class AuthIntegrationTest {

    private static final String PASSWORD = "secret-password";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void csrfEndpointSetsTokenCookie() throws Exception {
        this.mockMvc.perform(get("/auth/csrf"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void postWithoutCsrfTokenIsRejected() throws Exception {
        this.mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(uniqueUsername(), PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerLoginAndLogout() throws Exception {
        String username = uniqueUsername();

        this.mockMvc.perform(post("/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(username, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(username))
                .andExpect(jsonPath("$.userId").isNotEmpty());

        MvcResult login = this.mockMvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(username, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(username))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        assert session != null;
        this.mockMvc.perform(get("/api/does-not-exist").session(session))
                .andExpect(status().isNotFound());

        this.mockMvc.perform(get("/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(username));

        this.mockMvc.perform(post("/auth/logout").with(csrf()).session(session))
                .andExpect(status().isNoContent());

        this.mockMvc.perform(get("/api/does-not-exist").session(session))
                .andExpect(status().isUnauthorized());

        this.mockMvc.perform(get("/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithoutLoginIsUnauthorized() throws Exception {
        this.mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerRejectsDuplicateUsername() throws Exception {
        String username = uniqueUsername();
        register(username);

        this.mockMvc.perform(post("/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(username, PASSWORD)))
                .andExpect(status().isConflict());
    }

    @Test
    void registerRejectsInvalidInput() throws Exception {
        this.mockMvc.perform(post("/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("", "short")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        String username = uniqueUsername();
        register(username);

        this.mockMvc.perform(post("/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(username, "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiRequiresAuthentication() throws Exception {
        this.mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isUnauthorized());
    }

    private void register(String username) throws Exception {
        this.mockMvc.perform(post("/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(username, PASSWORD)))
                .andExpect(status().isCreated());
    }

    private static String uniqueUsername() {
        return "user-" + UUID.randomUUID();
    }

    private static String json(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }
}
