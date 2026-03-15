package kilian1111010.wealthandfinancetracker.domain.provider;

import jakarta.persistence.*;
import kilian1111010.wealthandfinancetracker.domain.user.UserEntity;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @JoinColumn(name = "USER_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private UserEntity user;
}
