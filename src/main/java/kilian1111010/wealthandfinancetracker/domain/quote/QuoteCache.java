package kilian1111010.wealthandfinancetracker.domain.quote;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
class QuoteCache {

    private final QuoteRepository quoteRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Map<String, QuoteEntity> find(Collection<String> keys) {
        return this.quoteRepository.findAllById(keys).stream()
                .collect(Collectors.toMap(QuoteEntity::getKey, Function.identity()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(Map<String, Price> pricesByKey) {
        pricesByKey.forEach((key, price) -> {
            QuoteEntity quote = this.quoteRepository.findById(key).orElseGet(QuoteEntity::new);
            quote.setKey(key);
            quote.setPrice(price.amount());
            quote.setCurrency(price.currency());
            quote.setFetchedAt(price.fetchedAt());
            this.quoteRepository.save(quote);
        });
    }
}
