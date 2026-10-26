package kilian1111010.wealthandfinancetracker.domain.accounttype;

import java.util.UUID;

public record AccountTypeResponse(
        UUID id,
        String name,
        AssetClass assetClass,
        boolean includeInNetWorth
) {

    static AccountTypeResponse of(AccountTypeEntity entity) {
        return new AccountTypeResponse(entity.getId(), entity.getName(), entity.getAssetClass(),
                entity.isIncludeInNetWorth());
    }
}
