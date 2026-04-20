package jabaclass.payment.infrastructure.persistence;

import jabaclass.payment.domain.model.DepositPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DepositPaymentJpaRepository extends JpaRepository<DepositPayment, UUID> {
}
