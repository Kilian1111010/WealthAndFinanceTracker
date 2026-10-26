package kilian1111010.wealthandfinancetracker.domain.transaction;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionRequest(
        @NotNull UUID accountId,
        @Nullable UUID categoryId,
        @NotNull @Digits(integer = 13, fraction = 2) BigDecimal amount,
        @NotNull LocalDateTime date,
        @Nullable @Size(max = 255) String counterparty,
        @Nullable @Size(max = 1000) String description
) {}
