package kilian1111010.wealthandfinancetracker.domain.holding;

import kilian1111010.wealthandfinancetracker.domain.account.AccountEntity;
import kilian1111010.wealthandfinancetracker.domain.account.AccountService;
import kilian1111010.wealthandfinancetracker.domain.accounttype.AssetClass;
import kilian1111010.wealthandfinancetracker.domain.quote.QuoteService;
import kilian1111010.wealthandfinancetracker.domain.quote.QuoteSource;
import kilian1111010.wealthandfinancetracker.exception.exceptions.ConflictException;
import kilian1111010.wealthandfinancetracker.exception.exceptions.InvalidRequestException;
import kilian1111010.wealthandfinancetracker.exception.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class HoldingService {

    private final HoldingRepository holdingRepository;
    private final AccountService accountService;
    private final QuoteService quoteService;
    private final HoldingValuation holdingValuation;

    @Transactional(readOnly = true)
    public List<HoldingResponse> listAll(UUID userId, boolean refresh) {
        return respond(this.holdingRepository.findAllByAccountUserIdOrderBySymbolAsc(userId), refresh);
    }

    @Transactional(readOnly = true)
    public List<HoldingResponse> list(UUID userId, UUID accountId) {
        AccountEntity account = this.accountService.get(userId, accountId);
        return respond(this.holdingRepository.findAllByAccountIdOrderBySymbolAsc(account.getId()), false);
    }

    public HoldingResponse create(UUID userId, UUID accountId, HoldingRequest request) {
        AccountEntity account = this.accountService.get(userId, accountId);
        AssetClass assetClass = account.getAccountType().getAssetClass();
        if (!assetClass.holdsPositions()) {
            throw new InvalidRequestException("Bestände sind nur bei Investment- und Crypto-Konten möglich.");
        }
        HoldingEntity holding = new HoldingEntity();
        holding.setAccount(account);
        apply(holding, assetClass, request);
        return respond(this.holdingRepository.save(holding));
    }

    public HoldingResponse update(UUID userId, UUID id, HoldingRequest request) {
        HoldingEntity holding = get(userId, id);
        apply(holding, holding.getAccount().getAccountType().getAssetClass(), request);
        return respond(holding);
    }

    public void delete(UUID userId, UUID id) {
        this.holdingRepository.delete(get(userId, id));
    }

    private HoldingEntity get(UUID userId, UUID id) {
        return this.holdingRepository.findByIdAndAccountUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Bestand nicht gefunden."));
    }

    private void apply(HoldingEntity holding, AssetClass assetClass, HoldingRequest request) {
        String symbol = normalizeSymbol(request.symbol());
        String name = blankToNull(request.name());
        if (request.isManual()) {
            if (name == null) {
                throw new InvalidRequestException("Bitte einen Namen für den Bestand angeben.");
            }
            if (request.manualValue() == null) {
                throw new InvalidRequestException("Bitte den Gesamtwert angeben.");
            }
            holding.setQuoteSource(null);
            holding.setQuoteId(null);
            holding.setManualValue(request.manualValue());
        } else {
            QuoteSource expected = QuoteSource.forAssetClass(assetClass);
            String quoteId = blankToNull(request.quoteId());
            if (request.quoteSource() == null || quoteId == null || symbol == null) {
                throw new InvalidRequestException("Bitte ein Wertpapier oder eine Kryptowährung aus der Suche auswählen.");
            }
            if (request.quoteSource() != expected) {
                throw new InvalidRequestException("Der Kurs passt nicht zur Kontoart.");
            }
            boolean changed = !quoteId.equals(holding.getQuoteId()) || holding.getQuoteSource() != expected;
            if (changed && !this.quoteService.exists(expected, quoteId)) {
                throw new InvalidRequestException("Für " + symbol + " wurde kein Kurs gefunden.");
            }
            holding.setQuoteSource(expected);
            holding.setQuoteId(quoteId);
            holding.setManualValue(null);
        }
        if (symbol != null && isDuplicate(holding, symbol)) {
            throw new ConflictException("Für " + symbol + " gibt es in diesem Konto bereits einen Bestand.");
        }
        holding.setManual(request.isManual());
        holding.setSymbol(symbol);
        holding.setName(name);
        holding.setQuantity(request.quantity());
    }

    private boolean isDuplicate(HoldingEntity holding, String symbol) {
        UUID accountId = holding.getAccount().getId();
        return holding.getId() == null
                ? this.holdingRepository.existsByAccountIdAndSymbol(accountId, symbol)
                : !Objects.equals(symbol, holding.getSymbol())
                        && this.holdingRepository.existsByAccountIdAndSymbolAndIdNot(accountId, symbol, holding.getId());
    }

    private List<HoldingResponse> respond(List<HoldingEntity> holdings, boolean refresh) {
        Map<UUID, HoldingValuation.Value> values = this.holdingValuation.valuate(holdings, refresh);
        return holdings.stream()
                .map(holding -> HoldingResponse.of(holding, values.get(holding.getId())))
                .toList();
    }

    private HoldingResponse respond(HoldingEntity holding) {
        return respond(List.of(holding), false).getFirst();
    }

    private static @Nullable String normalizeSymbol(@Nullable String symbol) {
        String value = blankToNull(symbol);
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
