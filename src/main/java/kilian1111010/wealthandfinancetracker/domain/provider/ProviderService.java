package kilian1111010.wealthandfinancetracker.domain.provider;

import kilian1111010.wealthandfinancetracker.domain.account.AccountRepository;
import kilian1111010.wealthandfinancetracker.domain.user.UserRepository;
import kilian1111010.wealthandfinancetracker.exception.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ProviderService {

    private final ProviderRepository providerRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ProviderResponse> list(UUID userId) {
        return this.providerRepository.findAllByUserIdOrderByNameAsc(userId).stream()
                .map(ProviderResponse::of)
                .toList();
    }

    public ProviderResponse create(UUID userId, ProviderRequest request) {
        ProviderEntity provider = new ProviderEntity();
        provider.setUser(this.userRepository.getReferenceById(userId));
        apply(provider, request);
        return ProviderResponse.of(this.providerRepository.save(provider));
    }

    public ProviderResponse update(UUID userId, UUID id, ProviderRequest request) {
        ProviderEntity provider = get(userId, id);
        apply(provider, request);
        return ProviderResponse.of(provider);
    }

    public void delete(UUID userId, UUID id) {
        ProviderEntity provider = get(userId, id);
        this.accountRepository.clearProvider(provider.getId());
        this.providerRepository.delete(provider);
    }

    public ProviderEntity get(UUID userId, UUID id) {
        return this.providerRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Anbieter nicht gefunden."));
    }

    private void apply(ProviderEntity provider, ProviderRequest request) {
        provider.setName(request.name().trim());
        provider.setCountry(request.country() == null ? null : request.country().toUpperCase(Locale.ROOT));
        provider.setExemptionAmount(request.exemptionAmount());
        provider.setWebsite(blankToNull(request.website()));
        provider.setLogoUrl(blankToNull(request.logoUrl()));
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
