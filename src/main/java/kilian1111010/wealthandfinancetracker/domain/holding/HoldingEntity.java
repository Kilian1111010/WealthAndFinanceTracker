package kilian1111010.wealthandfinancetracker.domain.holding;

import jakarta.persistence.*;
import kilian1111010.wealthandfinancetracker.domain.account.AccountEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "HOLDING")
@Getter
@Setter
public class HoldingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "SYMBOL")
    private String symbol;

    @Column(name = "NAME")
    private @Nullable String name;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @JoinColumn(name = "ACCOUNT_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private AccountEntity account;
}
