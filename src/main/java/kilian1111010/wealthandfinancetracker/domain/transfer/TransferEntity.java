package kilian1111010.wealthandfinancetracker.domain.transfer;

import jakarta.persistence.*;
import kilian1111010.wealthandfinancetracker.domain.account.AccountEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "TRANSFER")
@Getter
@Setter
public class TransferEntity {

    @Id
    @Column(name = "ID")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "DATE")
    private LocalDate date;

    @JoinColumn(name = "ACCOUNT_RECEIVING_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private AccountEntity accountReceivingId;

    @JoinColumn(name = "ACCOUNT_SENDING_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private AccountEntity accountSendingId;
}
