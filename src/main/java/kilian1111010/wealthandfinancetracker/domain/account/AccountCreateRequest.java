package kilian1111010.wealthandfinancetracker.domain.account;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountCreateRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull UUID accountTypeId,
        @Nullable UUID providerId,
        @Nullable UUID parentAccountId,
        @Nullable @Pattern(regexp = "[A-Za-z]{3}", message = "muss ein dreistelliger Währungscode sein") String currency,
        @Nullable @Digits(integer = 13, fraction = 2) BigDecimal balance,
        @Nullable @Size(max = 1000) String description
) {}
