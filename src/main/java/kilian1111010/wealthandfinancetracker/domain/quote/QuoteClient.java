package kilian1111010.wealthandfinancetracker.domain.quote;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface QuoteClient {

    QuoteSource source();

    List<QuoteCandidate> search(String query);

    Map<String, Price> prices(Collection<String> quoteIds);
}
