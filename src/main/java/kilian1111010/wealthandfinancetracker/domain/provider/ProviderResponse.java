package kilian1111010.wealthandfinancetracker.domain.provider;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

public record ProviderResponse(
        UUID id,
        String name,
        @Nullable String country,
        @Nullable BigDecimal exemptionAmount,
        @Nullable String website,
        @Nullable String logoUrl
) {

    static ProviderResponse of(ProviderEntity entity) {
        return new ProviderResponse(entity.getId(), entity.getName(), entity.getCountry(),
                entity.getExemptionAmount(), entity.getWebsite(), entity.getLogoUrl());
    }
}
