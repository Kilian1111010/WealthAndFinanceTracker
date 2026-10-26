package kilian1111010.wealthandfinancetracker.domain.account;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
class AccountController {

    private final AccountService accountService;

    @GetMapping
    List<AccountResponse> list(@AuthenticationPrincipal UUID userId) {
        return this.accountService.list(userId);
    }

    @GetMapping("/{id}")
    AccountResponse find(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        return this.accountService.find(userId, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AccountResponse create(@AuthenticationPrincipal UUID userId, @Valid @RequestBody AccountCreateRequest request) {
        return this.accountService.create(userId, request);
    }

    @PutMapping("/{id}")
    AccountResponse update(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                           @Valid @RequestBody AccountUpdateRequest request) {
        return this.accountService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        this.accountService.delete(userId, id);
    }
}
