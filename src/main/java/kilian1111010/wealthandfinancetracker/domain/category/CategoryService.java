package kilian1111010.wealthandfinancetracker.domain.category;

import kilian1111010.wealthandfinancetracker.domain.transaction.TransactionRepository;
import kilian1111010.wealthandfinancetracker.domain.user.UserRepository;
import kilian1111010.wealthandfinancetracker.exception.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(UUID userId) {
        return this.categoryRepository.findAllByUserIdOrderByNameAsc(userId).stream()
                .map(CategoryResponse::of)
                .toList();
    }

    public CategoryResponse create(UUID userId, CategoryRequest request) {
        CategoryEntity category = new CategoryEntity();
        category.setUser(this.userRepository.getReferenceById(userId));
        category.setName(request.name().trim());
        return CategoryResponse.of(this.categoryRepository.save(category));
    }

    public CategoryResponse update(UUID userId, UUID id, CategoryRequest request) {
        CategoryEntity category = get(userId, id);
        category.setName(request.name().trim());
        return CategoryResponse.of(category);
    }

    public void delete(UUID userId, UUID id) {
        CategoryEntity category = get(userId, id);
        this.transactionRepository.clearCategory(category.getId());
        this.categoryRepository.delete(category);
    }

    public CategoryEntity get(UUID userId, UUID id) {
        return this.categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Kategorie nicht gefunden."));
    }
}
