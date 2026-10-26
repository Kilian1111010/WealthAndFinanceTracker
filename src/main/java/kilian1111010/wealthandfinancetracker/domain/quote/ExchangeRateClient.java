package kilian1111010.wealthandfinancetracker.domain.quote;

import java.math.BigDecimal;
import java.util.Map;

public interface ExchangeRateClient {

    Map<String, BigDecimal> ratesPerEuro();
}
