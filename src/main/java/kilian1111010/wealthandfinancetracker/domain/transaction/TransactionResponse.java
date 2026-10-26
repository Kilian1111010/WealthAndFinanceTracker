package kilian1111010.wealthandfinancetracker.domain.transaction;

import kilian1111010.wealthandfinancetracker.domain.account.AccountEntity;
import kilian1111010.wealthandfinancetracker.domain.category.CategoryEntity;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID accountId,
        String accountName,
        @Nullable String currency,
        @Nullable UUID categoryId,
        @Nullable String categoryName,
        BigDecimal amount,
        LocalDateTime date,
        @Nullable String counterparty,
        @Nullable String description
) {

    public static TransactionResponse of(TransactionEntity entity) {
        AccountEntity account = entity.getAccount();
        CategoryEntity category = entity.getCategory();
        return new TransactionResponse(
                entity.getId(),
                account.getId(),
                account.getName(),
                account.getCurrency(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                entity.getAmount(),
                entity.getDate(),
                entity.getCounterparty(),
                entity.getDescription());
    }
}
