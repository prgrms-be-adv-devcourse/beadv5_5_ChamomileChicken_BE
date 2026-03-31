package jabaclass.payment.infrastructure.persistence;

import jabaclass.payment.domain.model.Payment;
import jabaclass.payment.domain.model.PaymentStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<Payment, UUID> {
	Optional<Payment> findByOrderId(UUID orderId);

	List<Payment> findByStatusAndCreatedAtBefore(
		PaymentStatus status,
		LocalDateTime threshold
	);
}
