package kilian1111010.wealthandfinancetracker.domain.quote;

import org.jspecify.annotations.Nullable;

public record QuoteCandidate(
        QuoteSource source,
        String quoteId,
        String symbol,
        String name,
        @Nullable String exchange
) {}
