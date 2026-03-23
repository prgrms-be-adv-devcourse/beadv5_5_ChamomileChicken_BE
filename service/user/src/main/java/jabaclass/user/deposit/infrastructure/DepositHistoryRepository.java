package jabaclass.user.deposit.infrastructure;

import jabaclass.user.deposit.domain.DepositHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DepositHistoryRepository extends JpaRepository<DepositHistory, UUID> {
    List<DepositHistory> findAllByUserId(UUID userId);
}
