package kilian1111010.wealthandfinancetracker.domain.provider;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderRepository extends JpaRepository<ProviderEntity, UUID> {

    List<ProviderEntity> findAllByUserIdOrderByNameAsc(UUID userId);

    Optional<ProviderEntity> findByIdAndUserId(UUID id, UUID userId);
}
