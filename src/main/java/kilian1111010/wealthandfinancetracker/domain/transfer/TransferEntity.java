package kilian1111010.wealthandfinancetracker.domain.transfer;

import jakarta.persistence.*;
import kilian1111010.wealthandfinancetracker.domain.account.AccountEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "TRANSFER")
@Getter
@Setter
public class TransferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "SENT_AMOUNT")
    private BigDecimal sentAmount;

    @Column(name = "RECEIVED_AMOUNT")
    private BigDecimal receivedAmount;

    @Column(name = "DATE")
    private LocalDateTime date;

    @Column(name = "DESCRIPTION")
    private @Nullable String description;

    @JoinColumn(name = "ACCOUNT_RECEIVING_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private AccountEntity receivingAccount;

    @JoinColumn(name = "ACCOUNT_SENDING_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private AccountEntity sendingAccount;
}
