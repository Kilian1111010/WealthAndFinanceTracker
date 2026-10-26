package kilian1111010.wealthandfinancetracker.domain.transfer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransferRepository extends JpaRepository<TransferEntity, UUID>, JpaSpecificationExecutor<TransferEntity> {

    Optional<TransferEntity> findByIdAndSendingAccountUserId(UUID id, UUID userId);

    boolean existsBySendingAccountIdOrReceivingAccountId(UUID sendingAccountId, UUID receivingAccountId);
}
