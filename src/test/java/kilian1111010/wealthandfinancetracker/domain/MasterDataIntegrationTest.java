package kilian1111010.wealthandfinancetracker.domain;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MasterDataIntegrationTest extends ApiTestSupport {

    @Test
    void categoryCrud() throws Exception {
        MockHttpSession session = newUser();
        String id = createdId(postJson(session, "/api/categories", "{\"name\":\"  Lebensmittel \"}"));

        putJson(session, "/api/categories/" + id, "{\"name\":\"Einkauf\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Einkauf"));
        getJson(session, "/api/categories")
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Einkauf"));

        deleteJson(session, "/api/categories/" + id).andExpect(status().isNoContent());
        getJson(session, "/api/categories").andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void categoryRequiresName() throws Exception {
        postJson(newUser(), "/api/categories", "{\"name\":\" \"}").andExpect(status().isBadRequest());
    }

    @Test
    void providerNormalizesCountryAndValidatesInput() throws Exception {
        MockHttpSession session = newUser();
        postJson(session, "/api/providers",
                "{\"name\":\"Bank\",\"country\":\"de\",\"exemptionAmount\":500,\"website\":\" \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.country").value("DE"))
                .andExpect(jsonPath("$.exemptionAmount").value(500))
                .andExpect(jsonPath("$.website").doesNotExist());

        postJson(session, "/api/providers", "{\"name\":\"Bank\",\"country\":\"DEU\"}")
                .andExpect(status().isBadRequest());
        postJson(session, "/api/providers", "{\"name\":\"Bank\",\"exemptionAmount\":-1}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletingProviderDetachesAccounts() throws Exception {
        MockHttpSession session = newUser();
        String providerId = createdId(postJson(session, "/api/providers", "{\"name\":\"Bank\"}"));
        String typeId = accountType(session, "Giro", "CASH");
        String accountId = createdId(postJson(session, "/api/accounts",
                "{\"name\":\"Giro\",\"accountTypeId\":\"%s\",\"providerId\":\"%s\",\"currency\":\"EUR\"}"
                        .formatted(typeId, providerId)));

        deleteJson(session, "/api/providers/" + providerId).andExpect(status().isNoContent());

        getJson(session, "/api/accounts/" + accountId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerId").doesNotExist());
    }

    @Test
    void accountTypeInUseCannotBeDeletedOrChangeAssetClass() throws Exception {
        MockHttpSession session = newUser();
        String typeId = accountType(session, "Giro", "CASH");
        account(session, "Giro", typeId, "EUR", "0");

        deleteJson(session, "/api/account-types/" + typeId).andExpect(status().isConflict());
        putJson(session, "/api/account-types/" + typeId, "{\"name\":\"Giro\",\"assetClass\":\"SAVINGS\"}")
                .andExpect(status().isConflict());
        putJson(session, "/api/account-types/" + typeId,
                "{\"name\":\"Girokonto\",\"assetClass\":\"CASH\",\"includeInNetWorth\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Girokonto"))
                .andExpect(jsonPath("$.includeInNetWorth").value(false));
    }

    @Test
    void otherUsersDataIsNotVisible() throws Exception {
        MockHttpSession owner = newUser();
        MockHttpSession stranger = newUser();
        String categoryId = createdId(postJson(owner, "/api/categories", "{\"name\":\"Privat\"}"));

        getJson(stranger, "/api/categories").andExpect(jsonPath("$", hasSize(0)));
        putJson(stranger, "/api/categories/" + categoryId, "{\"name\":\"Gekapert\"}")
                .andExpect(status().isNotFound());
        deleteJson(stranger, "/api/categories/" + categoryId).andExpect(status().isNotFound());
    }

    @Test
    void apiRequiresLogin() throws Exception {
        this.mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/accounts"))
                .andExpect(status().isUnauthorized());
    }
}
