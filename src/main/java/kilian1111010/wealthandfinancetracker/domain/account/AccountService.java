package kilian1111010.wealthandfinancetracker.domain.account;

import kilian1111010.wealthandfinancetracker.domain.accounttype.AccountTypeEntity;
import kilian1111010.wealthandfinancetracker.domain.accounttype.AccountTypeService;
import kilian1111010.wealthandfinancetracker.domain.accounttype.AssetClass;
import kilian1111010.wealthandfinancetracker.domain.holding.HoldingRepository;
import kilian1111010.wealthandfinancetracker.domain.provider.ProviderEntity;
import kilian1111010.wealthandfinancetracker.domain.provider.ProviderService;
import kilian1111010.wealthandfinancetracker.domain.transaction.TransactionRepository;
import kilian1111010.wealthandfinancetracker.domain.transfer.TransferRepository;
import kilian1111010.wealthandfinancetracker.domain.user.UserRepository;
import kilian1111010.wealthandfinancetracker.exception.exceptions.ConflictException;
import kilian1111010.wealthandfinancetracker.exception.exceptions.InvalidRequestException;
import kilian1111010.wealthandfinancetracker.exception.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountTypeService accountTypeService;
    private final ProviderService providerService;
    private final TransactionRepository transactionRepository;
    private final TransferRepository transferRepository;
    private final HoldingRepository holdingRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AccountResponse> list(UUID userId) {
        return this.accountRepository.findAllByUserIdOrderByNameAsc(userId).stream()
                .map(AccountResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse find(UUID userId, UUID id) {
        return AccountResponse.of(get(userId, id));
    }

    public AccountResponse create(UUID userId, AccountCreateRequest request) {
        AccountTypeEntity accountType = this.accountTypeService.get(userId, request.accountTypeId());
        AssetClass assetClass = accountType.getAssetClass();
        AccountEntity parent = request.parentAccountId() == null ? null : get(userId, request.parentAccountId());
        if (parent != null) {
            validateParent(parent, assetClass);
        }

        String currency = null;
        BigDecimal balance = null;
        if (assetClass.hasBalance()) {
            currency = resolveCurrency(request.currency(), parent);
            balance = request.balance() == null ? BigDecimal.ZERO : request.balance();
        } else if (request.currency() != null || request.balance() != null) {
            throw new InvalidRequestException(
                    "Investment- und Crypto-Konten haben keinen Kontostand, der Wert ergibt sich aus den Beständen.");
        }

        AccountEntity account = AccountEntity.builder()
                .user(this.userRepository.getReferenceById(userId))
                .name(request.name().trim())
                .accountType(accountType)
                .provider(provider(userId, request.providerId()))
                .parentAccount(parent)
                .currency(currency)
                .balance(balance)
                .description(blankToNull(request.description()))
                .build();
        return AccountResponse.of(this.accountRepository.save(account));
    }

    public AccountResponse update(UUID userId, UUID id, AccountUpdateRequest request) {
        AccountEntity account = get(userId, id);
        if (request.balance() != null) {
            if (!account.getAccountType().getAssetClass().hasBalance()) {
                throw new InvalidRequestException("Dieses Konto hat keinen Kontostand.");
            }
            account.setBalance(request.balance());
        }
        account.setName(request.name().trim());
        account.setProvider(provider(userId, request.providerId()));
        account.setDescription(blankToNull(request.description()));
        account.setArchived(request.archived());
        return AccountResponse.of(account);
    }

    public void delete(UUID userId, UUID id) {
        AccountEntity account = get(userId, id);
        UUID accountId = account.getId();
        boolean inUse = this.accountRepository.existsByParentAccountId(accountId)
                || this.transactionRepository.existsByAccountId(accountId)
                || this.transferRepository.existsBySendingAccountIdOrReceivingAccountId(accountId, accountId)
                || this.holdingRepository.existsByAccountId(accountId);
        if (inUse) {
            throw new ConflictException(
                    "Das Konto hat Buchungen, Transfers, Bestände oder Unterkonten. Archiviere es stattdessen.");
        }
        this.accountRepository.delete(account);
    }

    public AccountEntity get(UUID userId, UUID id) {
        return this.accountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Konto nicht gefunden."));
    }

    public AccountEntity getForBooking(UUID userId, UUID id) {
        AccountEntity account = get(userId, id);
        if (account.isArchived()) {
            throw new InvalidRequestException("Auf archivierte Konten kann nicht gebucht werden.");
        }
        if (!account.getAccountType().getAssetClass().isBookable()) {
            throw new InvalidRequestException(
                    "Buchungen und Transfers sind nur auf Bargeld-, Spar- und Kreditkonten möglich.");
        }
        return account;
    }

    public void changeBalance(AccountEntity account, BigDecimal delta) {
        BigDecimal balance = account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();
        account.setBalance(balance.add(delta));
    }

    private void validateParent(AccountEntity parent, AssetClass assetClass) {
        if (parent.isVirtual()) {
            throw new InvalidRequestException("Ein virtuelles Konto kann keine Unterkonten haben.");
        }
        if (parent.isArchived()) {
            throw new InvalidRequestException("Ein archiviertes Konto kann keine Unterkonten bekommen.");
        }
        if (!parent.getAccountType().getAssetClass().allowsSubAccounts() || !assetClass.allowsSubAccounts()) {
            throw new InvalidRequestException("Virtuelle Konten sind nur bei Bargeld- und Sparkonten möglich.");
        }
    }

    private String resolveCurrency(@Nullable String requested, @Nullable AccountEntity parent) {
        if (parent != null) {
            String parentCurrency = parent.getCurrency();
            if (requested != null && !requested.equalsIgnoreCase(parentCurrency)) {
                throw new InvalidRequestException("Ein virtuelles Konto hat die Währung seines Elternkontos.");
            }
            return parentCurrency;
        }
        if (requested == null) {
            throw new InvalidRequestException("Bitte eine Währung angeben.");
        }
        String code = requested.toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(code);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Unbekannte Währung: " + code);
        }
        return code;
    }

    private @Nullable ProviderEntity provider(UUID userId, @Nullable UUID providerId) {
        return providerId == null ? null : this.providerService.get(userId, providerId);
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
