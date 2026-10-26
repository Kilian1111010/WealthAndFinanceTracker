package kilian1111010.wealthandfinancetracker.domain.quote;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "QUOTE")
@Getter
@Setter
@NoArgsConstructor
public class QuoteEntity {

    @Id
    @Column(name = "QUOTE_KEY")
    private String key;

    @Column(name = "PRICE")
    private BigDecimal price;

    @Column(name = "CURRENCY")
    private String currency;

    @Column(name = "FETCHED_AT")
    private Instant fetchedAt;
}
