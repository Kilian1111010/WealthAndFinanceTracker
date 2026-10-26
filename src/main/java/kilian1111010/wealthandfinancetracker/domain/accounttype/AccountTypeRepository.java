package kilian1111010.wealthandfinancetracker.domain.accounttype;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountTypeRepository extends JpaRepository<AccountTypeEntity, UUID> {

    List<AccountTypeEntity> findAllByUserIdOrderByNameAsc(UUID userId);

    Optional<AccountTypeEntity> findByIdAndUserId(UUID id, UUID userId);
}
