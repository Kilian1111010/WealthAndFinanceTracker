package kilian1111010.wealthandfinancetracker.domain.holding;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
class HoldingController {

    private final HoldingService holdingService;

    @GetMapping("/holdings")
    List<HoldingResponse> listAll(@AuthenticationPrincipal UUID userId,
                                  @RequestParam(defaultValue = "false") boolean refresh) {
        return this.holdingService.listAll(userId, refresh);
    }

    @GetMapping("/accounts/{accountId}/holdings")
    List<HoldingResponse> list(@AuthenticationPrincipal UUID userId, @PathVariable UUID accountId) {
        return this.holdingService.list(userId, accountId);
    }

    @PostMapping("/accounts/{accountId}/holdings")
    @ResponseStatus(HttpStatus.CREATED)
    HoldingResponse create(@AuthenticationPrincipal UUID userId, @PathVariable UUID accountId,
                           @Valid @RequestBody HoldingRequest request) {
        return this.holdingService.create(userId, accountId, request);
    }

    @PutMapping("/holdings/{id}")
    HoldingResponse update(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                           @Valid @RequestBody HoldingRequest request) {
        return this.holdingService.update(userId, id, request);
    }

    @DeleteMapping("/holdings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        this.holdingService.delete(userId, id);
    }
}
