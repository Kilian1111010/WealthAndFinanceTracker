package kilian1111010.wealthandfinancetracker.domain.transaction;

import jakarta.validation.Valid;
import kilian1111010.wealthandfinancetracker.domain.PageResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    PageResponse<TransactionResponse> list(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate to,
            @RequestParam(required = false) @Nullable UUID accountId,
            @RequestParam(required = false) @Nullable UUID categoryId,
            @RequestParam(required = false) TransactionFilter.@Nullable TransactionKind kind,
            @PageableDefault(size = 50) Pageable pageable) {
        TransactionFilter filter = new TransactionFilter(from, to, accountId, categoryId, kind);
        return this.transactionService.list(userId, filter, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TransactionResponse create(@AuthenticationPrincipal UUID userId, @Valid @RequestBody TransactionRequest request) {
        return this.transactionService.create(userId, request);
    }

    @PutMapping("/{id}")
    TransactionResponse update(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                               @Valid @RequestBody TransactionRequest request) {
        return this.transactionService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        this.transactionService.delete(userId, id);
    }
}
