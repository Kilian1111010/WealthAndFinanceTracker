package kilian1111010.wealthandfinancetracker.domain.dashboard;

import kilian1111010.wealthandfinancetracker.domain.accounttype.AssetClass;
import kilian1111010.wealthandfinancetracker.domain.transaction.TransactionResponse;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DashboardResponse(
        LocalDate from,
        LocalDate to,
        List<CurrencyAmount> netWorth,
        List<AssetClassAmount> netWorthByAssetClass,
        List<CurrencyAmount> income,
        List<CurrencyAmount> expenses,
        List<CurrencyAmount> net,
        TransferSummary transfers,
        List<CategoryAmount> expensesByCategory,
        List<MonthlyFlow> monthly,
        List<HoldingSummary> holdings,
        BigDecimal holdingsValueEur,
        boolean pricesIncomplete,
        List<TransactionResponse> recentTransactions
) {

    public record CurrencyAmount(String currency, BigDecimal amount) {}

    public record AssetClassAmount(AssetClass assetClass, String currency, BigDecimal amount) {}

    public record CategoryAmount(@Nullable UUID categoryId, @Nullable String categoryName, String currency,
                                 BigDecimal amount) {}

    public record MonthlyFlow(String month, String currency, BigDecimal income, BigDecimal expenses) {}

    public record TransferSummary(long count, List<CurrencyAmount> sent) {}

    public record HoldingSummary(String key, @Nullable String symbol, @Nullable String name, BigDecimal quantity,
                                 int accounts, boolean manual, @Nullable BigDecimal valueEur, boolean stale) {}
}
