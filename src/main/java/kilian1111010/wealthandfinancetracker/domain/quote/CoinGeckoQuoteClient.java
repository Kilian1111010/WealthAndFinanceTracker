package kilian1111010.wealthandfinancetracker.domain.quote;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class CoinGeckoQuoteClient implements QuoteClient {

    private static final String BASE_URL = "https://api.coingecko.com/api/v3";
    private static final int MAX_RESULTS = 10;

    private final RestClient restClient;

    CoinGeckoQuoteClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public QuoteSource source() {
        return QuoteSource.COINGECKO;
    }

    @Override
    public List<QuoteCandidate> search(String query) {
        JsonNode body = get(BASE_URL + "/search?query={query}", query);
        List<QuoteCandidate> candidates = new ArrayList<>();
        for (JsonNode coin : body.path("coins").values()) {
            String id = coin.path("id").asString("");
            if (id.isBlank()) {
                continue;
            }
            String symbol = coin.path("symbol").asString(id).toUpperCase(Locale.ROOT);
            candidates.add(new QuoteCandidate(QuoteSource.COINGECKO, id, symbol, coin.path("name").asString(symbol), null));
            if (candidates.size() == MAX_RESULTS) {
                break;
            }
        }
        return candidates;
    }

    @Override
    public Map<String, Price> prices(Collection<String> quoteIds) {
        Map<String, Price> prices = new HashMap<>();
        if (quoteIds.isEmpty()) {
            return prices;
        }
        JsonNode body = get(BASE_URL + "/simple/price?ids={ids}&vs_currencies=eur", String.join(",", quoteIds));
        Instant now = Instant.now();
        for (String id : quoteIds) {
            JsonNode price = body.path(id).path("eur");
            if (price.isNumber()) {
                prices.put(id, new Price(price.decimalValue(), "EUR", now, false));
            }
        }
        return prices;
    }

    private JsonNode get(String uri, String variable) {
        try {
            JsonNode body = this.restClient.get().uri(uri, variable).retrieve().body(JsonNode.class);
            if (body == null) {
                throw new MarketDataUnavailableException("Leere Antwort von CoinGecko", new IllegalStateException());
            }
            return body;
        } catch (RestClientException e) {
            throw new MarketDataUnavailableException("CoinGecko nicht erreichbar", e);
        }
    }
}
