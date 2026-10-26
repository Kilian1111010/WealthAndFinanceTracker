package kilian1111010.wealthandfinancetracker.domain.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {

    List<CategoryEntity> findAllByUserIdOrderByNameAsc(UUID userId);

    Optional<CategoryEntity> findByIdAndUserId(UUID id, UUID userId);
}
