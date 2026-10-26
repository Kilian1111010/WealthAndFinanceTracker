package kilian1111010.wealthandfinancetracker.domain.transfer;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransferRequest(
        @NotNull UUID sendingAccountId,
        @NotNull UUID receivingAccountId,
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal sentAmount,
        @Nullable @Positive @Digits(integer = 13, fraction = 2) BigDecimal receivedAmount,
        @NotNull LocalDateTime date,
        @Nullable @Size(max = 1000) String description
) {}
