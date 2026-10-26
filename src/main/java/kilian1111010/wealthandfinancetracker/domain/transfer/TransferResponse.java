package kilian1111010.wealthandfinancetracker.domain.transfer;

import kilian1111010.wealthandfinancetracker.domain.account.AccountEntity;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        UUID sendingAccountId,
        String sendingAccountName,
        @Nullable String sendingCurrency,
        UUID receivingAccountId,
        String receivingAccountName,
        @Nullable String receivingCurrency,
        BigDecimal sentAmount,
        BigDecimal receivedAmount,
        LocalDateTime date,
        @Nullable String description
) {

    public static TransferResponse of(TransferEntity entity) {
        AccountEntity sending = entity.getSendingAccount();
        AccountEntity receiving = entity.getReceivingAccount();
        return new TransferResponse(
                entity.getId(),
                sending.getId(),
                sending.getName(),
                sending.getCurrency(),
                receiving.getId(),
                receiving.getName(),
                receiving.getCurrency(),
                entity.getSentAmount(),
                entity.getReceivedAmount(),
                entity.getDate(),
                entity.getDescription());
    }
}
