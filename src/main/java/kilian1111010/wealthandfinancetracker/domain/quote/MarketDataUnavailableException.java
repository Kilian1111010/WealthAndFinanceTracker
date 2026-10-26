package kilian1111010.wealthandfinancetracker.domain.quote;

public class MarketDataUnavailableException extends RuntimeException {

    public MarketDataUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
