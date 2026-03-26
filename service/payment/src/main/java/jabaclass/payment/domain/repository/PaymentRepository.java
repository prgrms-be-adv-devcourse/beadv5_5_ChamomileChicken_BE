package jabaclass.payment.domain.repository;

import jabaclass.payment.domain.model.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

	Payment save(Payment payment);

	Optional<Payment> findById(UUID paymentId);

	Optional<Payment> findByOrderId(UUID orderId);
}
