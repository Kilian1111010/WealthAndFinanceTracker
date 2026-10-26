package kilian1111010.wealthandfinancetracker.domain.transfer;

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
@RequestMapping("/transfers")
@RequiredArgsConstructor
class TransferController {

    private final TransferService transferService;

    @GetMapping
    PageResponse<TransferResponse> list(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate to,
            @RequestParam(required = false) @Nullable UUID accountId,
            @PageableDefault(size = 50) Pageable pageable) {
        return this.transferService.list(userId, from, to, accountId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TransferResponse create(@AuthenticationPrincipal UUID userId, @Valid @RequestBody TransferRequest request) {
        return this.transferService.create(userId, request);
    }

    @PutMapping("/{id}")
    TransferResponse update(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                            @Valid @RequestBody TransferRequest request) {
        return this.transferService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        this.transferService.delete(userId, id);
    }
}
