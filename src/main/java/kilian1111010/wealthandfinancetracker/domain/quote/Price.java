package kilian1111010.wealthandfinancetracker.domain.quote;

import java.math.BigDecimal;
import java.time.Instant;

public record Price(
        BigDecimal amount,
        String currency,
        Instant fetchedAt,
        boolean stale
) {}
