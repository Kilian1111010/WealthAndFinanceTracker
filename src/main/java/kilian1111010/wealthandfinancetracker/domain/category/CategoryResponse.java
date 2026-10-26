package kilian1111010.wealthandfinancetracker.domain.category;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name
) {

    static CategoryResponse of(CategoryEntity entity) {
        return new CategoryResponse(entity.getId(), entity.getName());
    }
}
