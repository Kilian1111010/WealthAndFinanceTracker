package kilian1111010.wealthandfinancetracker.domain.quote;

import kilian1111010.wealthandfinancetracker.domain.accounttype.AssetClass;
import kilian1111010.wealthandfinancetracker.exception.exceptions.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class QuoteService {

    private static final Duration PRICE_TTL = Duration.ofMinutes(15);
    private static final Duration RATES_TTL = Duration.ofHours(1);
    private static final int MIN_QUERY_LENGTH = 2;

    private final Map<QuoteSource, QuoteClient> clients = new EnumMap<>(QuoteSource.class);
    private final ExchangeRateClient exchangeRateClient;
    private final QuoteCache quoteCache;

    private volatile Map<String, BigDecimal> ratesPerEuro = Map.of("EUR", BigDecimal.ONE);
    private volatile @Nullable Instant ratesFetchedAt;

    public QuoteService(List<QuoteClient> clients, ExchangeRateClient exchangeRateClient, QuoteCache quoteCache) {
        clients.forEach(client -> this.clients.put(client.source(), client));
        this.exchangeRateClient = exchangeRateClient;
        this.quoteCache = quoteCache;
    }

    public List<QuoteCandidate> search(String query, AssetClass assetClass) {
        if (!assetClass.holdsPositions()) {
            throw new InvalidRequestException("Kurse gibt es nur für Wertpapiere und Krypto.");
        }
        String trimmed = query.trim();
        if (trimmed.length() < MIN_QUERY_LENGTH) {
            return List.of();
        }
        return client(QuoteSource.forAssetClass(assetClass)).search(trimmed);
    }

    public boolean exists(QuoteSource source, String quoteId) {
        try {
            Map<String, Price> prices = client(source).prices(Set.of(quoteId));
            if (prices.isEmpty()) {
                return false;
            }
            this.quoteCache.save(Map.of(source.key(quoteId), prices.get(quoteId)));
            return true;
        } catch (MarketDataUnavailableException e) {
            log.warn("Kurs für {} konnte nicht geprüft werden: {}", source.key(quoteId), e.getMessage());
            return true;
        }
    }

    public Map<String, Price> prices(Map<QuoteSource, Set<String>> quoteIds, boolean refresh) {
        Map<String, Price> result = new HashMap<>();
        Instant freshAfter = Instant.now().minus(PRICE_TTL);
        quoteIds.forEach((source, ids) -> {
            if (ids.isEmpty()) {
                return;
            }
            Map<String, QuoteEntity> cached = this.quoteCache.find(ids.stream().map(source::key).toList());
            Set<String> missing = new HashSet<>();
            for (String id : ids) {
                QuoteEntity quote = cached.get(source.key(id));
                if (!refresh && quote != null && quote.getFetchedAt().isAfter(freshAfter)) {
                    result.put(source.key(id), toPrice(quote, false));
                } else {
                    missing.add(id);
                }
            }
            if (!missing.isEmpty()) {
                fetch(source, missing, cached, result);
            }
        });
        return result;
    }

    public @Nullable BigDecimal toEur(BigDecimal amount, String currency) {
        if ("EUR".equals(currency)) {
            return amount;
        }
        BigDecimal rate = rates().get(currency);
        if (rate == null || rate.signum() == 0) {
            return null;
        }
        return amount.divide(rate, 10, RoundingMode.HALF_UP);
    }

    private void fetch(QuoteSource source, Collection<String> ids, Map<String, QuoteEntity> cached, Map<String, Price> result) {
        try {
            Map<String, Price> fetched = client(source).prices(ids);
            Map<String, Price> byKey = new HashMap<>();
            fetched.forEach((id, price) -> byKey.put(source.key(id), price));
            this.quoteCache.save(byKey);
            result.putAll(byKey);
        } catch (MarketDataUnavailableException e) {
            log.warn("Kurse von {} nicht verfügbar: {}", source, e.getMessage());
        }
        for (String id : ids) {
            String key = source.key(id);
            QuoteEntity quote = cached.get(key);
            if (!result.containsKey(key) && quote != null) {
                result.put(key, toPrice(quote, true));
            }
        }
    }

    private Map<String, BigDecimal> rates() {
        Instant fetchedAt = this.ratesFetchedAt;
        if (fetchedAt == null || fetchedAt.isBefore(Instant.now().minus(RATES_TTL))) {
            try {
                this.ratesPerEuro = Map.copyOf(this.exchangeRateClient.ratesPerEuro());
                this.ratesFetchedAt = Instant.now();
            } catch (MarketDataUnavailableException e) {
                log.warn("Wechselkurse nicht verfügbar: {}", e.getMessage());
            }
        }
        return this.ratesPerEuro;
    }

    private QuoteClient client(QuoteSource source) {
        QuoteClient client = this.clients.get(source);
        if (client == null) {
            throw new IllegalStateException("Kein Kursdienst für " + source + " konfiguriert");
        }
        return client;
    }

    private static Price toPrice(QuoteEntity quote, boolean stale) {
        return new Price(quote.getPrice(), quote.getCurrency(), quote.getFetchedAt(), stale);
    }
}
