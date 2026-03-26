package jabaclass.payment.infrastructure.persistence;

import jabaclass.payment.domain.model.Payment;
import jabaclass.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepository {

	private final PaymentJpaRepository paymentJpaRepository;

	@Override
	public Payment save(Payment payment) {
		return paymentJpaRepository.save(payment);
	}

	@Override
	public Optional<Payment> findById(UUID id) {
		return paymentJpaRepository.findById(id);
	}

	@Override
	public Optional<Payment> findByOrderId(UUID orderId) { return paymentJpaRepository.findByOrderId(orderId); }
}
