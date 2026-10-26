package kilian1111010.wealthandfinancetracker.domain.provider;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

public record ProviderRequest(
        @NotBlank @Size(max = 255) String name,
        @Nullable @Pattern(regexp = "[A-Za-z]{2}", message = "muss ein zweistelliger Ländercode sein") String country,
        @Nullable @PositiveOrZero @Digits(integer = 13, fraction = 2) BigDecimal exemptionAmount,
        @Nullable @Size(max = 255) String website,
        @Nullable @Size(max = 512) String logoUrl
) {}
