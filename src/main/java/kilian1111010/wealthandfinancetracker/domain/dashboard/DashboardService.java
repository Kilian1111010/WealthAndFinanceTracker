package kilian1111010.wealthandfinancetracker.domain.dashboard;

import jakarta.persistence.EntityManager;
import kilian1111010.wealthandfinancetracker.domain.account.AccountEntity;
import kilian1111010.wealthandfinancetracker.domain.account.AccountRepository;
import kilian1111010.wealthandfinancetracker.domain.accounttype.AccountTypeEntity;
import kilian1111010.wealthandfinancetracker.domain.dashboard.DashboardResponse.AssetClassAmount;
import kilian1111010.wealthandfinancetracker.domain.dashboard.DashboardResponse.CategoryAmount;
import kilian1111010.wealthandfinancetracker.domain.dashboard.DashboardResponse.CurrencyAmount;
import kilian1111010.wealthandfinancetracker.domain.dashboard.DashboardResponse.HoldingSummary;
import kilian1111010.wealthandfinancetracker.domain.dashboard.DashboardResponse.MonthlyFlow;
import kilian1111010.wealthandfinancetracker.domain.dashboard.DashboardResponse.TransferSummary;
import kilian1111010.wealthandfinancetracker.domain.accounttype.AssetClass;
import kilian1111010.wealthandfinancetracker.domain.holding.HoldingEntity;
import kilian1111010.wealthandfinancetracker.domain.holding.HoldingRepository;
import kilian1111010.wealthandfinancetracker.domain.holding.HoldingValuation;
import kilian1111010.wealthandfinancetracker.domain.transaction.TransactionRepository;
import kilian1111010.wealthandfinancetracker.domain.transaction.TransactionResponse;
import kilian1111010.wealthandfinancetracker.exception.exceptions.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardService {

    private static final int MONTHS = 12;

    private final EntityManager entityManager;
    private final AccountRepository accountRepository;
    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;
    private final HoldingValuation holdingValuation;

    private record Position(AssetClass assetClass, String currency, BigDecimal amount) {}

    public DashboardResponse dashboard(UUID userId, @Nullable LocalDate from, @Nullable LocalDate to, boolean refresh) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate periodFrom = from == null ? currentMonth.atDay(1) : from;
        LocalDate periodTo = to == null ? currentMonth.atEndOfMonth() : to;
        if (periodFrom.isAfter(periodTo)) {
            throw new InvalidRequestException("Der Zeitraum ist ungültig: Beginn liegt nach dem Ende.");
        }
        LocalDateTime start = periodFrom.atStartOfDay();
        LocalDateTime end = periodTo.plusDays(1).atStartOfDay();

        List<AccountEntity> accounts = this.accountRepository.findAllByUserIdOrderByNameAsc(userId).stream()
                .filter(account -> !account.isArchived())
                .toList();
        List<HoldingEntity> holdings = this.holdingRepository.findAllByAccountUserIdOrderBySymbolAsc(userId).stream()
                .filter(holding -> !holding.getAccount().isArchived())
                .toList();
        Map<UUID, HoldingValuation.Value> values = this.holdingValuation.valuate(holdings, refresh);
        List<Position> positions = positions(accounts, holdings, values);
        List<CurrencyAmount> income = flows(userId, start, end, true);
        List<CurrencyAmount> expenses = flows(userId, start, end, false);
        BigDecimal holdingsValue = values.values().stream()
                .map(HoldingValuation.Value::valueEur)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean pricesIncomplete = values.values().stream()
                .anyMatch(value -> value.valueEur() == null || (value.price() != null && value.price().stale()));

        return new DashboardResponse(
                periodFrom,
                periodTo,
                netWorth(positions),
                netWorthByAssetClass(positions),
                income,
                expenses,
                net(income, expenses),
                transfers(userId, start, end),
                expensesByCategory(userId, start, end),
                monthly(userId, YearMonth.from(periodTo)),
                holdingSummaries(holdings, values),
                holdingsValue,
                pricesIncomplete,
                this.transactionRepository.findTop5ByAccountUserIdOrderByDateDescIdDesc(userId).stream()
                        .map(TransactionResponse::of)
                        .toList());
    }

    private static List<Position> positions(List<AccountEntity> accounts, List<HoldingEntity> holdings,
                                            Map<UUID, HoldingValuation.Value> values) {
        List<Position> positions = new ArrayList<>();
        for (AccountEntity account : accounts) {
            AccountTypeEntity accountType = account.getAccountType();
            if (accountType.isIncludeInNetWorth() && account.getBalance() != null && account.getCurrency() != null) {
                positions.add(new Position(accountType.getAssetClass(), account.getCurrency(), account.getBalance()));
            }
        }
        for (HoldingEntity holding : holdings) {
            AccountTypeEntity accountType = holding.getAccount().getAccountType();
            BigDecimal valueEur = values.get(holding.getId()).valueEur();
            if (accountType.isIncludeInNetWorth() && valueEur != null) {
                positions.add(new Position(accountType.getAssetClass(), "EUR", valueEur));
            }
        }
        return positions;
    }

    private static List<CurrencyAmount> netWorth(List<Position> positions) {
        Map<String, BigDecimal> totals = new TreeMap<>();
        positions.forEach(position -> totals.merge(position.currency(), position.amount(), BigDecimal::add));
        return totals.entrySet().stream()
                .map(entry -> new CurrencyAmount(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static List<AssetClassAmount> netWorthByAssetClass(List<Position> positions) {
        Map<String, AssetClassAmount> totals = new LinkedHashMap<>();
        for (Position position : positions) {
            totals.merge(position.assetClass() + "|" + position.currency(),
                    new AssetClassAmount(position.assetClass(), position.currency(), position.amount()),
                    (a, b) -> new AssetClassAmount(a.assetClass(), a.currency(), a.amount().add(b.amount())));
        }
        return totals.values().stream()
                .sorted(Comparator.comparing(AssetClassAmount::assetClass).thenComparing(AssetClassAmount::currency))
                .toList();
    }

    private List<CurrencyAmount> flows(UUID userId, LocalDateTime start, LocalDateTime end, boolean income) {
        String sign = income ? "t.amount > 0" : "t.amount < 0";
        List<Object[]> rows = this.entityManager.createQuery("""
                        select a.currency, sum(t.amount)
                        from TransactionEntity t join t.account a
                        where a.user.id = :userId and t.date >= :start and t.date < :end and %s
                        group by a.currency
                        order by a.currency""".formatted(sign), Object[].class)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
        return rows.stream()
                .map(row -> new CurrencyAmount((String) row[0], ((BigDecimal) row[1]).abs()))
                .toList();
    }

    private static List<CurrencyAmount> net(List<CurrencyAmount> income, List<CurrencyAmount> expenses) {
        Map<String, BigDecimal> totals = new TreeMap<>();
        income.forEach(entry -> totals.merge(entry.currency(), entry.amount(), BigDecimal::add));
        expenses.forEach(entry -> totals.merge(entry.currency(), entry.amount().negate(), BigDecimal::add));
        return totals.entrySet().stream()
                .map(entry -> new CurrencyAmount(entry.getKey(), entry.getValue()))
                .toList();
    }

    private TransferSummary transfers(UUID userId, LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = this.entityManager.createQuery("""
                        select a.currency, count(tr), sum(tr.sentAmount)
                        from TransferEntity tr join tr.sendingAccount a
                        where a.user.id = :userId and tr.date >= :start and tr.date < :end
                        group by a.currency
                        order by a.currency""", Object[].class)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
        long count = rows.stream().mapToLong(row -> ((Number) row[1]).longValue()).sum();
        List<CurrencyAmount> sent = rows.stream()
                .map(row -> new CurrencyAmount((String) row[0], (BigDecimal) row[2]))
                .toList();
        return new TransferSummary(count, sent);
    }

    private List<CategoryAmount> expensesByCategory(UUID userId, LocalDateTime start, LocalDateTime end) {
        List<Object[]> rows = this.entityManager.createQuery("""
                        select c.id, c.name, a.currency, sum(t.amount)
                        from TransactionEntity t join t.account a left join t.category c
                        where a.user.id = :userId and t.date >= :start and t.date < :end and t.amount < 0
                        group by c.id, c.name, a.currency""", Object[].class)
                .setParameter("userId", userId)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
        return rows.stream()
                .map(row -> new CategoryAmount((UUID) row[0], (String) row[1], (String) row[2],
                        ((BigDecimal) row[3]).abs()))
                .sorted(Comparator.comparing(CategoryAmount::amount).reversed())
                .toList();
    }

    private List<MonthlyFlow> monthly(UUID userId, YearMonth lastMonth) {
        YearMonth firstMonth = lastMonth.minusMonths(MONTHS - 1L);
        List<Object[]> rows = this.entityManager.createQuery("""
                        select extract(year from t.date), extract(month from t.date), a.currency,
                               sum(case when t.amount > 0 then t.amount end),
                               sum(case when t.amount < 0 then t.amount end)
                        from TransactionEntity t join t.account a
                        where a.user.id = :userId and t.date >= :start and t.date < :end
                        group by extract(year from t.date), extract(month from t.date), a.currency""", Object[].class)
                .setParameter("userId", userId)
                .setParameter("start", firstMonth.atDay(1).atStartOfDay())
                .setParameter("end", lastMonth.plusMonths(1).atDay(1).atStartOfDay())
                .getResultList();

        Map<String, BigDecimal[]> values = new HashMap<>();
        TreeSet<String> currencies = new TreeSet<>();
        for (Object[] row : rows) {
            YearMonth month = YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
            String currency = (String) row[2];
            currencies.add(currency);
            values.put(month + "|" + currency, new BigDecimal[]{orZero(row[3]), orZero(row[4]).abs()});
        }

        List<MonthlyFlow> result = new ArrayList<>();
        for (String currency : currencies) {
            for (YearMonth month = firstMonth; !month.isAfter(lastMonth); month = month.plusMonths(1)) {
                BigDecimal[] flow = values.getOrDefault(month + "|" + currency,
                        new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                result.add(new MonthlyFlow(month.toString(), currency, flow[0], flow[1]));
            }
        }
        return result;
    }

    private static List<HoldingSummary> holdingSummaries(List<HoldingEntity> holdings,
                                                         Map<UUID, HoldingValuation.Value> values) {
        Map<String, List<HoldingEntity>> byKey = new LinkedHashMap<>();
        for (HoldingEntity holding : holdings) {
            String quoteKey = holding.quoteKey();
            String key = quoteKey != null ? quoteKey : "MANUAL:" + holding.getId();
            byKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(holding);
        }
        return byKey.entrySet().stream()
                .map(entry -> {
                    List<HoldingEntity> group = entry.getValue();
                    HoldingEntity first = group.getFirst();
                    BigDecimal quantity = group.stream()
                            .map(HoldingEntity::getQuantity)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    List<HoldingValuation.Value> groupValues = group.stream()
                            .map(holding -> values.get(holding.getId()))
                            .toList();
                    BigDecimal valueEur = groupValues.stream().anyMatch(value -> value.valueEur() == null)
                            ? null
                            : groupValues.stream()
                                    .map(HoldingValuation.Value::valueEur)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    boolean stale = groupValues.stream().anyMatch(value -> value.price() != null && value.price().stale());
                    return new HoldingSummary(entry.getKey(), first.getSymbol(), first.getName(),
                            new BigDecimal(quantity.stripTrailingZeros().toPlainString()), group.size(),
                            first.isManual(), valueEur, stale);
                })
                .sorted(Comparator.comparing(
                        (HoldingSummary summary) -> summary.valueEur() == null ? BigDecimal.ZERO : summary.valueEur())
                        .reversed())
                .toList();
    }

    private static BigDecimal orZero(@Nullable Object value) {
        return value == null ? BigDecimal.ZERO : (BigDecimal) value;
    }
}
