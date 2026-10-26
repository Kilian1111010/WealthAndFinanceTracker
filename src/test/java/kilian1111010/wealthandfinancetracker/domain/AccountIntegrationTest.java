package kilian1111010.wealthandfinancetracker.domain;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountIntegrationTest extends ApiTestSupport {

    @Test
    void cashAccountNeedsValidCurrency() throws Exception {
        MockHttpSession session = newUser();
        String typeId = accountType(session, "Giro", "CASH");

        postJson(session, "/api/accounts", "{\"name\":\"Giro\",\"accountTypeId\":\"%s\"}".formatted(typeId))
                .andExpect(status().isBadRequest());
        postJson(session, "/api/accounts",
                "{\"name\":\"Giro\",\"accountTypeId\":\"%s\",\"currency\":\"XYZ\"}".formatted(typeId))
                .andExpect(status().isBadRequest());
        postJson(session, "/api/accounts",
                "{\"name\":\"Giro\",\"accountTypeId\":\"%s\",\"currency\":\"eur\"}".formatted(typeId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.assetClass").value("CASH"))
                .andExpect(jsonPath("$.virtual").value(false));
    }

    @Test
    void investmentAccountHasNoBalance() throws Exception {
        MockHttpSession session = newUser();
        String typeId = accountType(session, "Depot", "INVESTMENT");

        postJson(session, "/api/accounts",
                "{\"name\":\"Depot\",\"accountTypeId\":\"%s\",\"currency\":\"EUR\"}".formatted(typeId))
                .andExpect(status().isBadRequest());
        postJson(session, "/api/accounts", "{\"name\":\"Depot\",\"accountTypeId\":\"%s\"}".formatted(typeId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance").doesNotExist())
                .andExpect(jsonPath("$.currency").doesNotExist());
    }

    @Test
    void virtualAccountInheritsCurrencyAndCannotBeNested() throws Exception {
        MockHttpSession session = newUser();
        String cash = accountType(session, "Giro", "CASH");
        String savings = accountType(session, "Spartopf", "SAVINGS");
        String depot = accountType(session, "Depot", "INVESTMENT");
        String parentId = account(session, "Giro", cash, "EUR", "100");

        String virtualId = createdId(postJson(session, "/api/accounts",
                "{\"name\":\"Urlaub\",\"accountTypeId\":\"%s\",\"parentAccountId\":\"%s\"}".formatted(savings, parentId)));
        getJson(session, "/api/accounts/" + virtualId)
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.virtual").value(true))
                .andExpect(jsonPath("$.parentAccountId").value(parentId));

        postJson(session, "/api/accounts",
                "{\"name\":\"Tief\",\"accountTypeId\":\"%s\",\"parentAccountId\":\"%s\"}".formatted(savings, virtualId))
                .andExpect(status().isBadRequest());
        postJson(session, "/api/accounts",
                "{\"name\":\"USD\",\"accountTypeId\":\"%s\",\"parentAccountId\":\"%s\",\"currency\":\"USD\"}"
                        .formatted(savings, parentId))
                .andExpect(status().isBadRequest());
        postJson(session, "/api/accounts",
                "{\"name\":\"Depot\",\"accountTypeId\":\"%s\",\"parentAccountId\":\"%s\"}".formatted(depot, parentId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void accountInUseMustBeArchivedInsteadOfDeleted() throws Exception {
        MockHttpSession session = newUser();
        String typeId = accountType(session, "Giro", "CASH");
        String accountId = account(session, "Giro", typeId, "EUR", "0");
        postJson(session, "/api/transactions",
                "{\"accountId\":\"%s\",\"amount\":-10,\"date\":\"%s\"}".formatted(accountId, LocalDateTime.now()))
                .andExpect(status().isCreated());

        deleteJson(session, "/api/accounts/" + accountId).andExpect(status().isConflict());

        putJson(session, "/api/accounts/" + accountId, "{\"name\":\"Altes Giro\",\"archived\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true))
                .andExpect(jsonPath("$.balance").value(-10));
        postJson(session, "/api/transactions",
                "{\"accountId\":\"%s\",\"amount\":-10,\"date\":\"%s\"}".formatted(accountId, LocalDateTime.now()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unusedAccountCanBeDeleted() throws Exception {
        MockHttpSession session = newUser();
        String typeId = accountType(session, "Giro", "CASH");
        String accountId = account(session, "Giro", typeId, "EUR", "0");

        deleteJson(session, "/api/accounts/" + accountId).andExpect(status().isNoContent());
        getJson(session, "/api/accounts").andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void otherAssetValueCanBeCorrected() throws Exception {
        MockHttpSession session = newUser();
        String typeId = accountType(session, "Sachwert", "OTHER");
        String accountId = createdId(postJson(session, "/api/accounts",
                "{\"name\":\"Auto\",\"accountTypeId\":\"%s\",\"currency\":\"EUR\",\"balance\":15000,\"description\":\"VW Golf\"}"
                        .formatted(typeId)));

        putJson(session, "/api/accounts/" + accountId,
                "{\"name\":\"Auto\",\"balance\":12500,\"description\":\"VW Golf\",\"archived\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(12500))
                .andExpect(jsonPath("$.description").value("VW Golf"));
    }

    @Test
    void holdingsOnlyOnInvestmentAccountsWithUniqueSymbol() throws Exception {
        MockHttpSession session = newUser();
        String cash = accountType(session, "Giro", "CASH");
        String crypto = accountType(session, "Wallet", "CRYPTO");
        String giroId = account(session, "Giro", cash, "EUR", "0");
        String walletId = createdId(postJson(session, "/api/accounts",
                "{\"name\":\"Wallet\",\"accountTypeId\":\"%s\"}".formatted(crypto)));

        postJson(session, "/api/accounts/%s/holdings".formatted(giroId), "{\"symbol\":\"BTC\",\"quoteSource\":\"COINGECKO\",\"quoteId\":\"bitcoin\",\"quantity\":1}")
                .andExpect(status().isBadRequest());

        String holdingId = createdId(postJson(session, "/api/accounts/%s/holdings".formatted(walletId),
                "{\"symbol\":\" btc \",\"name\":\"Bitcoin\",\"quoteSource\":\"COINGECKO\",\"quoteId\":\"bitcoin\",\"quantity\":0.123456789012345678}"));
        getJson(session, "/api/accounts/%s/holdings".formatted(walletId))
                .andExpect(jsonPath("$[0].symbol").value("BTC"))
                .andExpect(jsonPath("$[0].quantity").value(0.123456789012345678));

        postJson(session, "/api/accounts/%s/holdings".formatted(walletId), "{\"symbol\":\"BTC\",\"quoteSource\":\"COINGECKO\",\"quoteId\":\"bitcoin\",\"quantity\":2}")
                .andExpect(status().isConflict());

        putJson(session, "/api/holdings/" + holdingId, "{\"symbol\":\"BTC\",\"quoteSource\":\"COINGECKO\",\"quoteId\":\"bitcoin\",\"quantity\":2}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(2));
        deleteJson(session, "/api/holdings/" + holdingId).andExpect(status().isNoContent());
    }

    @Test
    void foreignAccountIsNotFound() throws Exception {
        MockHttpSession owner = newUser();
        MockHttpSession stranger = newUser();
        String typeId = accountType(owner, "Giro", "CASH");
        String accountId = account(owner, "Giro", typeId, "EUR", "0");

        getJson(stranger, "/api/accounts/" + accountId).andExpect(status().isNotFound());
        postJson(stranger, "/api/transactions",
                "{\"accountId\":\"%s\",\"amount\":-10,\"date\":\"%s\"}".formatted(accountId, LocalDateTime.now()))
                .andExpect(status().isNotFound());
    }
}
