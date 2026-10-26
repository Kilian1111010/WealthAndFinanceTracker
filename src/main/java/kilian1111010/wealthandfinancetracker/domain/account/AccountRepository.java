package kilian1111010.wealthandfinancetracker.domain.account;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {

    @EntityGraph(attributePaths = {"accountType", "provider", "parentAccount"})
    List<AccountEntity> findAllByUserIdOrderByNameAsc(UUID userId);

    Optional<AccountEntity> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByAccountTypeId(UUID accountTypeId);

    boolean existsByParentAccountId(UUID parentAccountId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AccountEntity a set a.provider = null where a.provider.id = :providerId")
    void clearProvider(UUID providerId);
}
