package kilian1111010.wealthandfinancetracker.domain.provider;

import jakarta.persistence.*;
import kilian1111010.wealthandfinancetracker.domain.user.UserEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "PROVIDER")
@Getter
@Setter
public class ProviderEntity {

    @Id
    @Column(name = "ID")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "COUNTRY")
    private @Nullable String country;

    @Column(name = "EXEMPTION_AMOUNT")
    private @Nullable BigDecimal exemptionAmount;

    @Column(name = "WEBSITE")
    private @Nullable String website;

    @Column(name = "LOGO_URL")
    private @Nullable String logoUrl;

    @JoinColumn(name = "USER_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private UserEntity user;
}
