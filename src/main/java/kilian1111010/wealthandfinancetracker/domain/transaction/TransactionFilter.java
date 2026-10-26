package kilian1111010.wealthandfinancetracker.domain.transaction;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.UUID;

public record TransactionFilter(
        @Nullable LocalDate from,
        @Nullable LocalDate to,
        @Nullable UUID accountId,
        @Nullable UUID categoryId,
        @Nullable TransactionKind kind
) {

    public enum TransactionKind {
        INCOME,
        EXPENSE
    }
}
