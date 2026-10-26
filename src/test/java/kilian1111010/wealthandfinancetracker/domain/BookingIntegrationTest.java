package kilian1111010.wealthandfinancetracker.domain;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingIntegrationTest extends ApiTestSupport {

    private static final LocalDateTime NOW = LocalDateTime.now().withNano(0);

    private String transaction(String accountId, String amount, LocalDateTime date) {
        return "{\"accountId\":\"%s\",\"amount\":%s,\"date\":\"%s\"}".formatted(accountId, amount, date);
    }

    @Test
    void transactionsKeepBalanceInSync() throws Exception {
        MockHttpSession session = newUser();
        String typeId = accountType(session, "Giro", "CASH");
        String giroId = account(session, "Giro", typeId, "EUR", "100");
        String otherId = account(session, "Bar", typeId, "EUR", "0");

        createdId(postJson(session, "/api/transactions", transaction(giroId, "1000", NOW)));
        String expenseId = createdId(postJson(session, "/api/transactions", transaction(giroId, "-200.50", NOW)));
        getJson(session, "/api/accounts/" + giroId).andExpect(jsonPath("$.balance").value(899.5));

        putJson(session, "/api/transactions/" + expenseId, transaction(giroId, "-300", NOW))
                .andExpect(status().isOk());
        getJson(session, "/api/accounts/" + giroId).andExpect(jsonPath("$.balance").value(800));

        putJson(session, "/api/transactions/" + expenseId, transaction(otherId, "-300", NOW))
                .andExpect(status().isOk());
        getJson(session, "/api/accounts/" + giroId).andExpect(jsonPath("$.balance").value(1100));
        getJson(session, "/api/accounts/" + otherId).andExpect(jsonPath("$.balance").value(-300));

        deleteJson(session, "/api/transactions/" + expenseId).andExpect(status().isNoContent());
        getJson(session, "/api/accounts/" + otherId).andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void zeroAmountIsRejected() throws Exception {
        MockHttpSession session = newUser();
        String typeId = accountType(session, "Giro", "CASH");
        String giroId = account(session, "Giro", typeId, "EUR", "0");

        postJson(session, "/api/transactions", transaction(giroId, "0", NOW)).andExpect(status().isBadRequest());
    }

    @Test
    void transactionsCanBeFiltered() throws Exception {
        MockHttpSession session = newUser();
        String typeId = accountType(session, "Giro", "CASH");
        String giroId = account(session, "Giro", typeId, "EUR", "0");
        String categoryId = createdId(postJson(session, "/api/categories", "{\"name\":\"Miete\"}"));

        postJson(session, "/api/transactions", transaction(giroId, "2500", NOW)).andExpect(status().isCreated());
        postJson(session, "/api/transactions",
                "{\"accountId\":\"%s\",\"categoryId\":\"%s\",\"amount\":-900,\"date\":\"%s\",\"counterparty\":\"Vermieter\"}"
                        .formatted(giroId, categoryId, NOW))
                .andExpect(status().isCreated());
        postJson(session, "/api/transactions", transaction(giroId, "-50", NOW.minusYears(1)))
                .andExpect(status().isCreated());

        getJson(session, "/api/transactions")
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].currency").value("EUR"));
        getJson(session, "/api/transactions?kind=EXPENSE")
                .andExpect(jsonPath("$.totalElements").value(2));
        getJson(session, "/api/transactions?categoryId=" + categoryId)
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].categoryName").value("Miete"))
                .andExpect(jsonPath("$.content[0].counterparty").value("Vermieter"));
        getJson(session, "/api/transactions?from=" + LocalDate.now().minusDays(1))
                .andExpect(jsonPath("$.totalElements").value(2));
        getJson(session, "/api/transactions?size=1")
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void deletingCategoryKeepsTransactions() throws Exception {
        MockHttpSession session = newUser();
        String typeId = accountType(session, "Giro", "CASH");
        String giroId = account(session, "Giro", typeId, "EUR", "0");
        String categoryId = createdId(postJson(session, "/api/categories", "{\"name\":\"Freizeit\"}"));
        postJson(session, "/api/transactions",
                "{\"accountId\":\"%s\",\"categoryId\":\"%s\",\"amount\":-20,\"date\":\"%s\"}"
                        .formatted(giroId, categoryId, NOW))
                .andExpect(status().isCreated());

        deleteJson(session, "/api/categories/" + categoryId).andExpect(status().isNoContent());

        getJson(session, "/api/transactions")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].categoryId").doesNotExist());
    }

    @Test
    void transactionsOnlyOnBookableAccounts() throws Exception {
        MockHttpSession session = newUser();
        String typeId = accountType(session, "Sachwert", "OTHER");
        String carId = account(session, "Auto", typeId, "EUR", "10000");

        postJson(session, "/api/transactions", transaction(carId, "-10", NOW)).andExpect(status().isBadRequest());
    }

    @Test
    void transferBetweenSameCurrency() throws Exception {
        MockHttpSession session = newUser();
        String typeId = accountType(session, "Giro", "CASH");
        String giroId = account(session, "Giro", typeId, "EUR", "1000");
        String savingsId = account(session, "Sparen", typeId, "EUR", "0");

        String transferId = createdId(postJson(session, "/api/transfers",
                "{\"sendingAccountId\":\"%s\",\"receivingAccountId\":\"%s\",\"sentAmount\":250,\"date\":\"%s\"}"
                        .formatted(giroId, savingsId, NOW)));
        getJson(session, "/api/accounts/" + giroId).andExpect(jsonPath("$.balance").value(750));
        getJson(session, "/api/accounts/" + savingsId).andExpect(jsonPath("$.balance").value(250));
        getJson(session, "/api/transfers")
                .andExpect(jsonPath("$.content[0].receivedAmount").value(250));

        deleteJson(session, "/api/transfers/" + transferId).andExpect(status().isNoContent());
        getJson(session, "/api/accounts/" + giroId).andExpect(jsonPath("$.balance").value(1000));
        getJson(session, "/api/accounts/" + savingsId).andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void transferBetweenCurrenciesNeedsReceivedAmount() throws Exception {
        MockHttpSession session = newUser();
        String typeId = accountType(session, "Giro", "CASH");
        String eurId = account(session, "EUR", typeId, "EUR", "1000");
        String usdId = account(session, "USD", typeId, "USD", "0");
        String body = "{\"sendingAccountId\":\"%s\",\"receivingAccountId\":\"%s\",\"sentAmount\":100,%s\"date\":\"%s\"}";

        postJson(session, "/api/transfers", body.formatted(eurId, usdId, "", NOW))
                .andExpect(status().isBadRequest());
        String transferId = createdId(postJson(session, "/api/transfers",
                body.formatted(eurId, usdId, "\"receivedAmount\":108.40,", NOW)));
        getJson(session, "/api/accounts/" + eurId).andExpect(jsonPath("$.balance").value(900));
        getJson(session, "/api/accounts/" + usdId).andExpect(jsonPath("$.balance").value(108.4));

        putJson(session, "/api/transfers/" + transferId, body.formatted(eurId, usdId, "\"receivedAmount\":110,", NOW))
                .andExpect(status().isOk());
        getJson(session, "/api/accounts/" + usdId).andExpect(jsonPath("$.balance").value(110));
    }

    @Test
    void transferRejectsSameAccountAndNonPositiveAmount() throws Exception {
        MockHttpSession session = newUser();
        String typeId = accountType(session, "Giro", "CASH");
        String giroId = account(session, "Giro", typeId, "EUR", "0");
        String otherId = account(session, "Bar", typeId, "EUR", "0");
        String body = "{\"sendingAccountId\":\"%s\",\"receivingAccountId\":\"%s\",\"sentAmount\":%s,\"date\":\"%s\"}";

        postJson(session, "/api/transfers", body.formatted(giroId, giroId, "10", NOW))
                .andExpect(status().isBadRequest());
        postJson(session, "/api/transfers", body.formatted(giroId, otherId, "-10", NOW))
                .andExpect(status().isBadRequest());
    }
}
