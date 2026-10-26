package kilian1111010.wealthandfinancetracker.domain.quote;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class YahooQuoteClient implements QuoteClient {

    private static final String BASE_URL = "https://query1.finance.yahoo.com";
    private static final Set<String> SUPPORTED_TYPES = Set.of("EQUITY", "ETF", "MUTUALFUND", "INDEX");
    private static final Map<String, String> MINOR_UNITS = Map.of("GBp", "GBP", "GBX", "GBP", "ZAc", "ZAR", "ILA", "ILS");

    private final RestClient restClient;

    YahooQuoteClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public QuoteSource source() {
        return QuoteSource.YAHOO;
    }

    @Override
    public List<QuoteCandidate> search(String query) {
        JsonNode body = get(BASE_URL + "/v1/finance/search?q={query}&quotesCount=10&newsCount=0&listsCount=0", query);
        List<QuoteCandidate> candidates = new ArrayList<>();
        for (JsonNode quote : body.path("quotes").values()) {
            String symbol = quote.path("symbol").asString("");
            if (symbol.isBlank() || !SUPPORTED_TYPES.contains(quote.path("quoteType").asString(""))) {
                continue;
            }
            String name = quote.path("longname").asString(quote.path("shortname").asString(symbol)).trim();
            String exchange = quote.path("exchDisp").asString("");
            candidates.add(new QuoteCandidate(QuoteSource.YAHOO, symbol, symbol, name, exchange.isBlank() ? null : exchange));
        }
        return candidates;
    }

    @Override
    public Map<String, Price> prices(Collection<String> quoteIds) {
        Map<String, Price> prices = new HashMap<>();
        for (String symbol : quoteIds) {
            JsonNode meta;
            try {
                meta = get(BASE_URL + "/v8/finance/chart/{symbol}?range=1d&interval=1d", symbol)
                        .path("chart").path("result").path(0).path("meta");
            } catch (MarketDataUnavailableException e) {
                if (e.getCause() instanceof HttpClientErrorException.NotFound) {
                    continue;
                }
                throw e;
            }
            JsonNode price = meta.path("regularMarketPrice");
            String currency = meta.path("currency").asString("");
            if (!price.isNumber() || currency.isBlank()) {
                continue;
            }
            BigDecimal amount = price.decimalValue();
            if (MINOR_UNITS.containsKey(currency)) {
                amount = amount.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
                currency = MINOR_UNITS.get(currency);
            }
            prices.put(symbol, new Price(amount, currency.toUpperCase(), Instant.now(), false));
        }
        return prices;
    }

    private JsonNode get(String uri, Object variable) {
        try {
            JsonNode body = this.restClient.get().uri(uri, variable).retrieve().body(JsonNode.class);
            if (body == null) {
                throw new MarketDataUnavailableException("Leere Antwort von Yahoo Finance", new IllegalStateException());
            }
            return body;
        } catch (RestClientException e) {
            throw new MarketDataUnavailableException("Yahoo Finance nicht erreichbar", e);
        }
    }
}
