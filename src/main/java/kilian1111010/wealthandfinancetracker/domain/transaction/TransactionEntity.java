package kilian1111010.wealthandfinancetracker.domain.transaction;

import jakarta.persistence.*;
import kilian1111010.wealthandfinancetracker.domain.account.AccountEntity;
import kilian1111010.wealthandfinancetracker.domain.category.CategoryEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "TRANSACTION")
@Getter
@Setter
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "DATE")
    private LocalDateTime date;

    @Column(name = "COUNTERPARTY")
    private @Nullable String counterparty;

    @Column(name = "DESCRIPTION")
    private @Nullable String description;

    @JoinColumn(name = "ACCOUNT_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private AccountEntity account;

    @JoinColumn(name = "CATEGORY_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private @Nullable CategoryEntity category;
}
