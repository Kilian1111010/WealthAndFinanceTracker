package kilian1111010.wealthandfinancetracker;

import kilian1111010.wealthandfinancetracker.domain.quote.ExchangeRateClient;
import kilian1111010.wealthandfinancetracker.domain.quote.MarketDataUnavailableException;
import kilian1111010.wealthandfinancetracker.domain.quote.Price;
import kilian1111010.wealthandfinancetracker.domain.quote.QuoteCandidate;
import kilian1111010.wealthandfinancetracker.domain.quote.QuoteClient;
import kilian1111010.wealthandfinancetracker.domain.quote.QuoteSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@TestConfiguration(proxyBeanMethods = false)
public class FakeMarketData {

    public static final AtomicBoolean OFFLINE = new AtomicBoolean(false);

    @Bean
    QuoteClient fakeYahoo() {
        return new FakeClient(QuoteSource.YAHOO,
                List.of(new QuoteCandidate(QuoteSource.YAHOO, "EUNL.DE", "EUNL.DE", "iShares Core MSCI World", "XETRA"),
                        new QuoteCandidate(QuoteSource.YAHOO, "AAPL", "AAPL", "Apple Inc.", "NASDAQ")),
                Map.of("EUNL.DE", new Price(new BigDecimal("100"), "EUR", Instant.now(), false),
                        "AAPL", new Price(new BigDecimal("200"), "USD", Instant.now(), false)));
    }

    @Bean
    QuoteClient fakeCoinGecko() {
        return new FakeClient(QuoteSource.COINGECKO,
                List.of(new QuoteCandidate(QuoteSource.COINGECKO, "bitcoin", "BTC", "Bitcoin", null)),
                Map.of("bitcoin", new Price(new BigDecimal("50000"), "EUR", Instant.now(), false)));
    }

    @Bean
    ExchangeRateClient fakeExchangeRates() {
        return () -> Map.of("EUR", BigDecimal.ONE, "USD", new BigDecimal("1.25"));
    }

    private record FakeClient(QuoteSource source, List<QuoteCandidate> candidates, Map<String, Price> prices)
            implements QuoteClient {

        @Override
        public List<QuoteCandidate> search(String query) {
            checkOnline();
            return this.candidates.stream()
                    .filter(candidate -> candidate.symbol().contains(query.toUpperCase())
                            || candidate.name().toLowerCase().contains(query.toLowerCase()))
                    .toList();
        }

        @Override
        public Map<String, Price> prices(Collection<String> quoteIds) {
            checkOnline();
            Map<String, Price> result = new HashMap<>();
            quoteIds.forEach(id -> {
                Price price = this.prices.get(id);
                if (price != null) {
                    result.put(id, new Price(price.amount(), price.currency(), Instant.now(), false));
                }
            });
            return result;
        }

        private static void checkOnline() {
            if (OFFLINE.get()) {
                throw new MarketDataUnavailableException("offline", new IllegalStateException());
            }
        }
    }
}
