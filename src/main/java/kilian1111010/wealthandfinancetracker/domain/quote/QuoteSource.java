package kilian1111010.wealthandfinancetracker.domain.quote;

import kilian1111010.wealthandfinancetracker.domain.accounttype.AssetClass;

public enum QuoteSource {
    YAHOO,
    COINGECKO;

    public static QuoteSource forAssetClass(AssetClass assetClass) {
        return assetClass == AssetClass.CRYPTO ? COINGECKO : YAHOO;
    }

    public String key(String quoteId) {
        return name() + ":" + quoteId;
    }
}
