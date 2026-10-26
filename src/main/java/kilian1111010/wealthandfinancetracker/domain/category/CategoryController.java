package kilian1111010.wealthandfinancetracker.domain.category;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    List<CategoryResponse> list(@AuthenticationPrincipal UUID userId) {
        return this.categoryService.list(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CategoryResponse create(@AuthenticationPrincipal UUID userId, @Valid @RequestBody CategoryRequest request) {
        return this.categoryService.create(userId, request);
    }

    @PutMapping("/{id}")
    CategoryResponse update(@AuthenticationPrincipal UUID userId, @PathVariable UUID id,
                            @Valid @RequestBody CategoryRequest request) {
        return this.categoryService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        this.categoryService.delete(userId, id);
    }
}
