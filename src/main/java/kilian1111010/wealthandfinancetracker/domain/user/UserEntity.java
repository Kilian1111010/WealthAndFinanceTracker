package kilian1111010.wealthandfinancetracker.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "USER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserEntity {

    @Id
    @Column(name = "ID")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "NAME")
    private String username;

    @Column(name = "PASSWORD")
    private String password;
}
