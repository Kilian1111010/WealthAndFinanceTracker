package kilian1111010.wealthandfinancetracker.domain.accounttype;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/account-types")
@RequiredArgsConstructor
class AccountTypeController {

    private final AccountTypeService accountTypeService;

    @GetMapping
    List<AccountTypeResponse> list(@AuthenticationPrincipal UUID userId) {
        return this.accountTypeService.list(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AccountTypeResponse create(@AuthenticationPrincipal UUID userId, @Valid @RequestBody AccountTypeRequest request) {
        return this.accountTypeService.create(userId, request);
    }

    @PutMapping("/{id}")
    AccountTypeResponse update(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                               @Valid @RequestBody AccountTypeRequest request) {
        return this.accountTypeService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        this.accountTypeService.delete(userId, id);
    }
}
