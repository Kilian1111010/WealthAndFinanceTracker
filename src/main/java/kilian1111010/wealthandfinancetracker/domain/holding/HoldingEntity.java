package kilian1111010.wealthandfinancetracker.domain.holding;

import jakarta.persistence.*;
import kilian1111010.wealthandfinancetracker.domain.account.AccountEntity;
import kilian1111010.wealthandfinancetracker.domain.quote.QuoteSource;
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
    private @Nullable String symbol;

    @Column(name = "NAME")
    private @Nullable String name;

    @Column(name = "QUANTITY")
    private BigDecimal quantity;

    @Column(name = "MANUAL")
    private boolean manual;

    @Enumerated(EnumType.STRING)
    @Column(name = "QUOTE_SOURCE")
    private @Nullable QuoteSource quoteSource;

    @Column(name = "QUOTE_ID")
    private @Nullable String quoteId;

    @Column(name = "MANUAL_VALUE")
    private @Nullable BigDecimal manualValue;

    @JoinColumn(name = "ACCOUNT_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private AccountEntity account;

    public @Nullable String quoteKey() {
        return this.manual || this.quoteSource == null || this.quoteId == null ? null : this.quoteSource.key(this.quoteId);
    }
}
