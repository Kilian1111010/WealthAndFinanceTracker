package kilian1111010.wealthandfinancetracker.domain.quote;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "app.market-data.enabled", havingValue = "true", matchIfMissing = true)
class MarketDataConfig {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";

    @Bean
    RestClient marketDataRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(TIMEOUT);
        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .build();
    }

    @Bean
    QuoteClient yahooQuoteClient(RestClient marketDataRestClient) {
        return new YahooQuoteClient(marketDataRestClient);
    }

    @Bean
    QuoteClient coinGeckoQuoteClient(RestClient marketDataRestClient) {
        return new CoinGeckoQuoteClient(marketDataRestClient);
    }

    @Bean
    ExchangeRateClient frankfurterExchangeRateClient(RestClient marketDataRestClient) {
        return new FrankfurterExchangeRateClient(marketDataRestClient);
    }
}
