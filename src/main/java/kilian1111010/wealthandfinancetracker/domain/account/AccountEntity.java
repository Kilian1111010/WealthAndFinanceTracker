package kilian1111010.wealthandfinancetracker.domain.account;

import jakarta.persistence.*;
import kilian1111010.wealthandfinancetracker.domain.accounttype.AccountTypeEntity;
import kilian1111010.wealthandfinancetracker.domain.provider.ProviderEntity;
import kilian1111010.wealthandfinancetracker.domain.user.UserEntity;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ACCOUNT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "BALANCE")
    private @Nullable BigDecimal balance;

    @Column(name = "CURRENCY")
    private @Nullable String currency;

    @Column(name = "DESCRIPTION")
    private @Nullable String description;

    @Column(name = "ARCHIVED")
    private boolean archived;

    @Version
    @Column(name = "VERSION")
    private long version;

    @JoinColumn(name = "PARENT_ACCOUNT_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private @Nullable AccountEntity parentAccount;

    @JoinColumn(name = "PROVIDER_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private ProviderEntity provider;

    @JoinColumn(name = "ACCOUNT_TYPE_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private AccountTypeEntity accountType;

    @JoinColumn(name = "USER_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private UserEntity user;

    public boolean isVirtual() {
        return this.parentAccount != null;
    }
}
