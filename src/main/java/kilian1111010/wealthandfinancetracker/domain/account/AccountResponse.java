package kilian1111010.wealthandfinancetracker.domain.account;

import kilian1111010.wealthandfinancetracker.domain.accounttype.AccountTypeEntity;
import kilian1111010.wealthandfinancetracker.domain.accounttype.AssetClass;
import kilian1111010.wealthandfinancetracker.domain.provider.ProviderEntity;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String name,
        UUID accountTypeId,
        String accountTypeName,
        AssetClass assetClass,
        boolean includeInNetWorth,
        @Nullable UUID providerId,
        @Nullable String providerName,
        @Nullable UUID parentAccountId,
        @Nullable BigDecimal balance,
        @Nullable String currency,
        @Nullable String description,
        boolean archived,
        boolean virtual
) {

    static AccountResponse of(AccountEntity entity) {
        AccountTypeEntity accountType = entity.getAccountType();
        ProviderEntity provider = entity.getProvider();
        AccountEntity parent = entity.getParentAccount();
        return new AccountResponse(
                entity.getId(),
                entity.getName(),
                accountType.getId(),
                accountType.getName(),
                accountType.getAssetClass(),
                accountType.isIncludeInNetWorth(),
                provider == null ? null : provider.getId(),
                provider == null ? null : provider.getName(),
                parent == null ? null : parent.getId(),
                entity.getBalance(),
                entity.getCurrency(),
                entity.getDescription(),
                entity.isArchived(),
                entity.isVirtual());
    }
}
