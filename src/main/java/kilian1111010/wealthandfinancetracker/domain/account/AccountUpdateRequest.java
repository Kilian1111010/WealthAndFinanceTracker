package kilian1111010.wealthandfinancetracker.domain.account;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountUpdateRequest(
        @NotBlank @Size(max = 255) String name,
        @Nullable UUID providerId,
        @Nullable @Digits(integer = 13, fraction = 2) BigDecimal balance,
        @Nullable @Size(max = 1000) String description,
        @NotNull Boolean archived
) {}
