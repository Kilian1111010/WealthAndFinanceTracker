package kilian1111010.wealthandfinancetracker.domain.provider;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/providers")
@RequiredArgsConstructor
class ProviderController {

    private final ProviderService providerService;

    @GetMapping
    List<ProviderResponse> list(@AuthenticationPrincipal UUID userId) {
        return this.providerService.list(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ProviderResponse create(@AuthenticationPrincipal UUID userId, @Valid @RequestBody ProviderRequest request) {
        return this.providerService.create(userId, request);
    }

    @PutMapping("/{id}")
    ProviderResponse update(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                            @Valid @RequestBody ProviderRequest request) {
        return this.providerService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        this.providerService.delete(userId, id);
    }
}
