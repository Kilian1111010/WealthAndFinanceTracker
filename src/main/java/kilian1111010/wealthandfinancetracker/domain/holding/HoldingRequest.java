package kilian1111010.wealthandfinancetracker.domain.holding;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import kilian1111010.wealthandfinancetracker.domain.quote.QuoteSource;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

public record HoldingRequest(
        @Nullable Boolean manual,
        @Nullable @Size(max = 50) String symbol,
        @Nullable @Size(max = 255) String name,
        @Nullable QuoteSource quoteSource,
        @Nullable @Size(max = 100) String quoteId,
        @NotNull @PositiveOrZero @Digits(integer = 20, fraction = 18) BigDecimal quantity,
        @Nullable @PositiveOrZero @Digits(integer = 13, fraction = 2) BigDecimal manualValue
) {

    public boolean isManual() {
        return Boolean.TRUE.equals(this.manual);
    }
}
