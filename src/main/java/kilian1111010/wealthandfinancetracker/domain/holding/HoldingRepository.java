package kilian1111010.wealthandfinancetracker.domain.holding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HoldingRepository extends JpaRepository<HoldingEntity, UUID> {

    List<HoldingEntity> findAllByAccountIdOrderBySymbolAsc(UUID accountId);

    List<HoldingEntity> findAllByAccountUserIdOrderBySymbolAsc(UUID userId);

    Optional<HoldingEntity> findByIdAndAccountUserId(UUID id, UUID userId);

    boolean existsByAccountId(UUID accountId);

    boolean existsByAccountIdAndSymbol(UUID accountId, String symbol);

    boolean existsByAccountIdAndSymbolAndIdNot(UUID accountId, String symbol, UUID id);
}
