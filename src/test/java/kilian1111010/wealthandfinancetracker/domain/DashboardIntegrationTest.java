package kilian1111010.wealthandfinancetracker.domain;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardIntegrationTest extends ApiTestSupport {

    @Test
    void emptyDashboardForNewUser() throws Exception {
        getJson(newUser(), "/api/dashboard")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value(YearMonth.now().atDay(1).toString()))
                .andExpect(jsonPath("$.netWorth", hasSize(0)))
                .andExpect(jsonPath("$.transfers.count").value(0))
                .andExpect(jsonPath("$.monthly", hasSize(0)));
    }

    @Test
    void dashboardSummarizesWealthAndFlows() throws Exception {
        MockHttpSession session = newUser();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        String cash = accountType(session, "Giro", "CASH");
        String savings = accountType(session, "Spartopf", "SAVINGS");
        String other = accountType(session, "Sachwert", "OTHER");
        String excluded = createdId(postJson(session, "/api/account-types",
                "{\"name\":\"Fremdgeld\",\"assetClass\":\"CASH\",\"includeInNetWorth\":false}"));
        String crypto = accountType(session, "Wallet", "CRYPTO");

        String giroId = account(session, "Giro", cash, "EUR", "1000");
        String potId = createdId(postJson(session, "/api/accounts",
                "{\"name\":\"Urlaub\",\"accountTypeId\":\"%s\",\"parentAccountId\":\"%s\"}".formatted(savings, giroId)));
        account(session, "Auto", other, "EUR", "5000");
        account(session, "Verein", excluded, "EUR", "300");
        account(session, "Dollar", cash, "USD", "50");
        String walletId = createdId(postJson(session, "/api/accounts",
                "{\"name\":\"Wallet\",\"accountTypeId\":\"%s\"}".formatted(crypto)));
        postJson(session, "/api/accounts/%s/holdings".formatted(walletId), "{\"symbol\":\"BTC\",\"quoteSource\":\"COINGECKO\",\"quoteId\":\"bitcoin\",\"quantity\":0.5}")
                .andExpect(status().isCreated());

        String foodId = createdId(postJson(session, "/api/categories", "{\"name\":\"Lebensmittel\"}"));
        postJson(session, "/api/transactions",
                "{\"accountId\":\"%s\",\"amount\":2000,\"date\":\"%s\"}".formatted(giroId, now))
                .andExpect(status().isCreated());
        postJson(session, "/api/transactions",
                "{\"accountId\":\"%s\",\"categoryId\":\"%s\",\"amount\":-150,\"date\":\"%s\"}".formatted(giroId, foodId, now))
                .andExpect(status().isCreated());
        postJson(session, "/api/transactions",
                "{\"accountId\":\"%s\",\"amount\":-50,\"date\":\"%s\"}".formatted(giroId, now))
                .andExpect(status().isCreated());
        postJson(session, "/api/transfers",
                "{\"sendingAccountId\":\"%s\",\"receivingAccountId\":\"%s\",\"sentAmount\":400,\"date\":\"%s\"}"
                        .formatted(giroId, potId, now))
                .andExpect(status().isCreated());

        getJson(session, "/api/dashboard")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netWorth", hasSize(2)))
                .andExpect(jsonPath("$.netWorth[?(@.currency=='EUR')].amount").value(32800.0))
                .andExpect(jsonPath("$.netWorth[?(@.currency=='USD')].amount").value(50.0))
                .andExpect(jsonPath("$.netWorthByAssetClass[?(@.assetClass=='SAVINGS')].amount").value(400.0))
                .andExpect(jsonPath("$.netWorthByAssetClass[?(@.assetClass=='OTHER')].amount").value(5000.0))
                .andExpect(jsonPath("$.netWorthByAssetClass[?(@.assetClass=='CRYPTO')].amount").value(25000.0))
                .andExpect(jsonPath("$.holdingsValueEur").value(25000))
                .andExpect(jsonPath("$.pricesIncomplete").value(false))
                .andExpect(jsonPath("$.income[0].amount").value(2000))
                .andExpect(jsonPath("$.expenses[0].amount").value(200))
                .andExpect(jsonPath("$.net[0].amount").value(1800))
                .andExpect(jsonPath("$.transfers.count").value(1))
                .andExpect(jsonPath("$.transfers.sent[0].amount").value(400))
                .andExpect(jsonPath("$.expensesByCategory", hasSize(2)))
                .andExpect(jsonPath("$.expensesByCategory[0].categoryName").value("Lebensmittel"))
                .andExpect(jsonPath("$.expensesByCategory[0].amount").value(150))
                .andExpect(jsonPath("$.monthly", hasSize(12)))
                .andExpect(jsonPath("$.monthly[11].month").value(YearMonth.now().toString()))
                .andExpect(jsonPath("$.monthly[11].income").value(2000))
                .andExpect(jsonPath("$.monthly[11].expenses").value(200))
                .andExpect(jsonPath("$.holdings[0].symbol").value("BTC"))
                .andExpect(jsonPath("$.holdings[0].quantity").value(0.5))
                .andExpect(jsonPath("$.holdings[0].valueEur").value(25000))
                .andExpect(jsonPath("$.recentTransactions", hasSize(3)));
    }

    @Test
    void periodOutsideBookingsShowsNoFlows() throws Exception {
        MockHttpSession session = newUser();
        String cash = accountType(session, "Giro", "CASH");
        String giroId = account(session, "Giro", cash, "EUR", "0");
        postJson(session, "/api/transactions",
                "{\"accountId\":\"%s\",\"amount\":-10,\"date\":\"%s\"}".formatted(giroId, LocalDateTime.now().withNano(0)))
                .andExpect(status().isCreated());
        LocalDate lastYear = LocalDate.now().minusYears(1);

        getJson(session, "/api/dashboard?from=%s&to=%s".formatted(lastYear.withDayOfMonth(1), lastYear))
                .andExpect(jsonPath("$.expenses", hasSize(0)))
                .andExpect(jsonPath("$.netWorth[0].amount").value(-10));
        getJson(session, "/api/dashboard?from=2026-02-01&to=2026-01-01").andExpect(status().isBadRequest());
    }
}
