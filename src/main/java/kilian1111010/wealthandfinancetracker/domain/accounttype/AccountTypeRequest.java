package kilian1111010.wealthandfinancetracker.domain.accounttype;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record AccountTypeRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull AssetClass assetClass,
        @Nullable Boolean includeInNetWorth
) {}
