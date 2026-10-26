package kilian1111010.wealthandfinancetracker.domain.holding;

import kilian1111010.wealthandfinancetracker.domain.quote.Price;
import kilian1111010.wealthandfinancetracker.domain.quote.QuoteService;
import kilian1111010.wealthandfinancetracker.domain.quote.QuoteSource;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HoldingValuation {

    private final QuoteService quoteService;

    public record Value(@Nullable Price price, @Nullable BigDecimal valueEur) {

        static final Value NONE = new Value(null, null);
    }

    public Map<UUID, Value> valuate(List<HoldingEntity> holdings, boolean refresh) {
        Map<QuoteSource, Set<String>> quoteIds = new EnumMap<>(QuoteSource.class);
        for (HoldingEntity holding : holdings) {
            if (holding.quoteKey() != null) {
                quoteIds.computeIfAbsent(holding.getQuoteSource(), source -> new HashSet<>()).add(holding.getQuoteId());
            }
        }
        Map<String, Price> prices = quoteIds.isEmpty() ? Map.of() : this.quoteService.prices(quoteIds, refresh);

        Map<UUID, Value> values = new HashMap<>();
        for (HoldingEntity holding : holdings) {
            values.put(holding.getId(), value(holding, prices));
        }
        return values;
    }

    private Value value(HoldingEntity holding, Map<String, Price> prices) {
        if (holding.isManual()) {
            return new Value(null, holding.getManualValue());
        }
        String key = holding.quoteKey();
        Price price = key == null ? null : prices.get(key);
        if (price == null) {
            return Value.NONE;
        }
        BigDecimal priceEur = this.quoteService.toEur(price.amount(), price.currency());
        BigDecimal valueEur = priceEur == null
                ? null
                : priceEur.multiply(holding.getQuantity()).setScale(2, RoundingMode.HALF_UP);
        return new Value(price, valueEur);
    }
}
