package jabaclass.payment.domain.repository;

import jabaclass.payment.domain.model.Payment;
import jabaclass.payment.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

	Payment save(Payment payment);

	Optional<Payment> findById(UUID paymentId);

	Optional<Payment> findByOrderId(UUID orderId);

	Optional<Payment> findReusableByOrderId(UUID orderId);

	List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime threshold);

	List<SettlementPaymentSource> findSettlementPaymentSources(
		LocalDateTime from,
		LocalDateTime to,
		LocalDateTime cursorOccurredAt,
		UUID cursorId,
		int size
	);

	record SettlementPaymentSource(
		UUID paymentId,
		UUID orderId,
		UUID productId,
		String paymentStatus,
		BigDecimal totalPaymentAmount,
		LocalDateTime occurredAt
	) {
	}
}
