package kilian1111010.wealthandfinancetracker.domain.accounttype;

import kilian1111010.wealthandfinancetracker.domain.account.AccountRepository;
import kilian1111010.wealthandfinancetracker.domain.user.UserRepository;
import kilian1111010.wealthandfinancetracker.exception.exceptions.ConflictException;
import kilian1111010.wealthandfinancetracker.exception.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AccountTypeService {

    private final AccountTypeRepository accountTypeRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AccountTypeResponse> list(UUID userId) {
        return this.accountTypeRepository.findAllByUserIdOrderByNameAsc(userId).stream()
                .map(AccountTypeResponse::of)
                .toList();
    }

    public AccountTypeResponse create(UUID userId, AccountTypeRequest request) {
        AccountTypeEntity accountType = new AccountTypeEntity();
        accountType.setUser(this.userRepository.getReferenceById(userId));
        accountType.setAssetClass(request.assetClass());
        apply(accountType, request);
        return AccountTypeResponse.of(this.accountTypeRepository.save(accountType));
    }

    public AccountTypeResponse update(UUID userId, UUID id, AccountTypeRequest request) {
        AccountTypeEntity accountType = get(userId, id);
        if (accountType.getAssetClass() != request.assetClass()) {
            if (this.accountRepository.existsByAccountTypeId(accountType.getId())) {
                throw new ConflictException("Die Anlageklasse kann nicht geändert werden, solange Konten diesen Typ verwenden.");
            }
            accountType.setAssetClass(request.assetClass());
        }
        apply(accountType, request);
        return AccountTypeResponse.of(accountType);
    }

    public void delete(UUID userId, UUID id) {
        AccountTypeEntity accountType = get(userId, id);
        if (this.accountRepository.existsByAccountTypeId(accountType.getId())) {
            throw new ConflictException("Der Kontotyp wird noch von Konten verwendet.");
        }
        this.accountTypeRepository.delete(accountType);
    }

    public AccountTypeEntity get(UUID userId, UUID id) {
        return this.accountTypeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Kontotyp nicht gefunden."));
    }

    private void apply(AccountTypeEntity accountType, AccountTypeRequest request) {
        accountType.setName(request.name().trim());
        accountType.setIncludeInNetWorth(request.includeInNetWorth() == null || request.includeInNetWorth());
    }
}
