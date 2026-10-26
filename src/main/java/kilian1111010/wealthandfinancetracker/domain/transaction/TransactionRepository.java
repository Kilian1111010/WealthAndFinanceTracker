package kilian1111010.wealthandfinancetracker.domain.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID>, JpaSpecificationExecutor<TransactionEntity> {

    Optional<TransactionEntity> findByIdAndAccountUserId(UUID id, UUID userId);

    boolean existsByAccountId(UUID accountId);

    List<TransactionEntity> findTop5ByAccountUserIdOrderByDateDescIdDesc(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update TransactionEntity t set t.category = null where t.category.id = :categoryId")
    void clearCategory(UUID categoryId);
}
