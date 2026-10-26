package kilian1111010.wealthandfinancetracker.domain;

import com.jayway.jsonpath.JsonPath;
import kilian1111010.wealthandfinancetracker.TestcontainersConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.market-data.enabled=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
public abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    protected MockHttpSession newUser() throws Exception {
        String body = "{\"username\":\"user-%s\",\"password\":\"secret-password\"}".formatted(UUID.randomUUID());
        MvcResult result = this.mockMvc.perform(post("/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    protected ResultActions getJson(MockHttpSession session, String path) throws Exception {
        return this.mockMvc.perform(get(path).session(session));
    }

    protected ResultActions postJson(MockHttpSession session, String path, String body) throws Exception {
        return this.mockMvc.perform(post(path).with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    protected ResultActions putJson(MockHttpSession session, String path, String body) throws Exception {
        return this.mockMvc.perform(put(path).with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    protected ResultActions deleteJson(MockHttpSession session, String path) throws Exception {
        return this.mockMvc.perform(delete(path).with(csrf()).session(session));
    }

    protected String createdId(ResultActions actions) throws Exception {
        String content = actions.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(content, "$.id");
    }

    protected String accountType(MockHttpSession session, String name, String assetClass) throws Exception {
        return createdId(postJson(session, "/api/account-types",
                "{\"name\":\"%s\",\"assetClass\":\"%s\"}".formatted(name, assetClass)));
    }

    protected String account(MockHttpSession session, String name, String typeId, String currency, String balance)
            throws Exception {
        return createdId(postJson(session, "/api/accounts",
                "{\"name\":\"%s\",\"accountTypeId\":\"%s\",\"currency\":\"%s\",\"balance\":%s}"
                        .formatted(name, typeId, currency, balance)));
    }
}
