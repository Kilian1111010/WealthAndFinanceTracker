package kilian1111010.wealthandfinancetracker.domain.accounttype;

public enum AssetClass {
    CASH,
    SAVINGS,
    INVESTMENT,
    CRYPTO,
    LIABILITY,
    OTHER;

    public boolean hasBalance() {
        return this != INVESTMENT && this != CRYPTO;
    }

    public boolean holdsPositions() {
        return this == INVESTMENT || this == CRYPTO;
    }

    public boolean isBookable() {
        return this == CASH || this == SAVINGS || this == LIABILITY;
    }

    public boolean allowsSubAccounts() {
        return this == CASH || this == SAVINGS;
    }
}
