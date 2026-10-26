package kilian1111010.wealthandfinancetracker.domain.quote;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

class FrankfurterExchangeRateClient implements ExchangeRateClient {

    private static final String URL = "https://api.frankfurter.dev/v1/latest?base=EUR";

    private final RestClient restClient;

    FrankfurterExchangeRateClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Map<String, BigDecimal> ratesPerEuro() {
        JsonNode body;
        try {
            body = this.restClient.get().uri(URL).retrieve().body(JsonNode.class);
        } catch (RestClientException e) {
            throw new MarketDataUnavailableException("EZB-Wechselkurse nicht erreichbar", e);
        }
        Map<String, BigDecimal> rates = new HashMap<>();
        if (body != null) {
            body.path("rates").properties().forEach(entry -> {
                if (entry.getValue().isNumber()) {
                    rates.put(entry.getKey(), entry.getValue().decimalValue());
                }
            });
        }
        rates.put("EUR", BigDecimal.ONE);
        return rates;
    }
}
