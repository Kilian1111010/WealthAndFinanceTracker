package kilian1111010.wealthandfinancetracker.domain.holding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface HoldingRepository extends JpaRepository<HoldingEntity, UUID> {
}
