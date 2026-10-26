package kilian1111010.wealthandfinancetracker.domain.holding;

import kilian1111010.wealthandfinancetracker.domain.quote.Price;
import kilian1111010.wealthandfinancetracker.domain.quote.QuoteSource;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HoldingResponse(
        UUID id,
        UUID accountId,
        String accountName,
        boolean manual,
        @Nullable String symbol,
        @Nullable String name,
        BigDecimal quantity,
        @Nullable QuoteSource quoteSource,
        @Nullable String quoteId,
        @Nullable BigDecimal manualValue,
        @Nullable BigDecimal price,
        @Nullable String priceCurrency,
        @Nullable Instant priceAt,
        boolean priceStale,
        @Nullable BigDecimal valueEur
) {

    static HoldingResponse of(HoldingEntity entity, HoldingValuation.Value value) {
        Price price = value.price();
        return new HoldingResponse(
                entity.getId(),
                entity.getAccount().getId(),
                entity.getAccount().getName(),
                entity.isManual(),
                entity.getSymbol(),
                entity.getName(),
                plain(entity.getQuantity()),
                entity.getQuoteSource(),
                entity.getQuoteId(),
                entity.getManualValue(),
                price == null ? null : plain(price.amount()),
                price == null ? null : price.currency(),
                price == null ? null : price.fetchedAt(),
                price != null && price.stale(),
                value.valueEur());
    }

    private static BigDecimal plain(BigDecimal value) {
        return new BigDecimal(value.stripTrailingZeros().toPlainString());
    }
}
