package kilian1111010.wealthandfinancetracker.domain;

import kilian1111010.wealthandfinancetracker.FakeMarketData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HoldingQuoteIntegrationTest extends ApiTestSupport {

    @AfterEach
    void online() {
        FakeMarketData.OFFLINE.set(false);
    }

    private String depot(MockHttpSession session) throws Exception {
        String typeId = accountType(session, "Depot", "INVESTMENT");
        return createdId(postJson(session, "/api/accounts", "{\"name\":\"Depot\",\"accountTypeId\":\"%s\"}".formatted(typeId)));
    }

    private String ticker(String symbol, String source, String quoteId, String quantity) {
        return "{\"manual\":false,\"symbol\":\"%s\",\"quoteSource\":\"%s\",\"quoteId\":\"%s\",\"quantity\":%s}"
                .formatted(symbol, source, quoteId, quantity);
    }

    @Test
    void searchReturnsCandidatesForAssetClass() throws Exception {
        MockHttpSession session = newUser();

        getJson(session, "/api/quotes/search?query=eunl&assetClass=INVESTMENT")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].quoteId").value("EUNL.DE"))
                .andExpect(jsonPath("$[0].exchange").value("XETRA"));
        getJson(session, "/api/quotes/search?query=bitcoin&assetClass=CRYPTO")
                .andExpect(jsonPath("$[0].source").value("COINGECKO"))
                .andExpect(jsonPath("$[0].symbol").value("BTC"));
        getJson(session, "/api/quotes/search?query=x&assetClass=CRYPTO").andExpect(jsonPath("$", hasSize(0)));
        getJson(session, "/api/quotes/search?query=eunl&assetClass=CASH").andExpect(status().isBadRequest());
    }

    @Test
    void tickerHoldingIsValuedInEuro() throws Exception {
        MockHttpSession session = newUser();
        String depotId = depot(session);

        postJson(session, "/api/accounts/%s/holdings".formatted(depotId), ticker("EUNL.DE", "YAHOO", "EUNL.DE", "10"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price").value(100))
                .andExpect(jsonPath("$.priceCurrency").value("EUR"))
                .andExpect(jsonPath("$.valueEur").value(1000))
                .andExpect(jsonPath("$.priceStale").value(false));
        postJson(session, "/api/accounts/%s/holdings".formatted(depotId), ticker("AAPL", "YAHOO", "AAPL", "5"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priceCurrency").value("USD"))
                .andExpect(jsonPath("$.valueEur").value(800));
    }

    @Test
    void tickerHoldingNeedsMatchingKnownQuote() throws Exception {
        MockHttpSession session = newUser();
        String depotId = depot(session);
        String path = "/api/accounts/%s/holdings".formatted(depotId);

        postJson(session, path, ticker("BTC", "COINGECKO", "bitcoin", "1")).andExpect(status().isBadRequest());
        postJson(session, path, ticker("XXX", "YAHOO", "XXX", "1")).andExpect(status().isBadRequest());
        postJson(session, path, "{\"manual\":false,\"symbol\":\"EUNL.DE\",\"quantity\":1}").andExpect(status().isBadRequest());
    }

    @Test
    void manualHoldingUsesEnteredValue() throws Exception {
        MockHttpSession session = newUser();
        String depotId = depot(session);
        String path = "/api/accounts/%s/holdings".formatted(depotId);

        postJson(session, path, "{\"manual\":true,\"quantity\":1,\"manualValue\":100}").andExpect(status().isBadRequest());
        postJson(session, path, "{\"manual\":true,\"name\":\"Betriebsrente\",\"quantity\":1}").andExpect(status().isBadRequest());
        String holdingId = createdId(postJson(session, path,
                "{\"manual\":true,\"name\":\"Betriebsrente\",\"quantity\":3.5,\"manualValue\":12000}"));

        getJson(session, path)
                .andExpect(jsonPath("$[0].manual").value(true))
                .andExpect(jsonPath("$[0].symbol").doesNotExist())
                .andExpect(jsonPath("$[0].price").doesNotExist())
                .andExpect(jsonPath("$[0].valueEur").value(12000));

        putJson(session, "/api/holdings/" + holdingId, ticker("EUNL.DE", "YAHOO", "EUNL.DE", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manual").value(false))
                .andExpect(jsonPath("$.manualValue").doesNotExist())
                .andExpect(jsonPath("$.valueEur").value(200));
    }

    @Test
    void outageFallsBackToLastKnownPrice() throws Exception {
        MockHttpSession session = newUser();
        String typeId = accountType(session, "Wallet", "CRYPTO");
        String walletId = createdId(postJson(session, "/api/accounts", "{\"name\":\"Wallet\",\"accountTypeId\":\"%s\"}".formatted(typeId)));
        postJson(session, "/api/accounts/%s/holdings".formatted(walletId), ticker("BTC", "COINGECKO", "bitcoin", "0.1"))
                .andExpect(status().isCreated());

        FakeMarketData.OFFLINE.set(true);

        getJson(session, "/api/holdings?refresh=true")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].priceStale").value(true))
                .andExpect(jsonPath("$[0].valueEur").value(5000));
        getJson(session, "/api/quotes/search?query=btc&assetClass=CRYPTO").andExpect(status().isServiceUnavailable());
        getJson(session, "/api/dashboard?refresh=true")
                .andExpect(jsonPath("$.pricesIncomplete").value(true))
                .andExpect(jsonPath("$.holdingsValueEur").value(5000));
    }
}
