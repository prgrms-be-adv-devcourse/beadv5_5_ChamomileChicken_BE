package jabaclass.payment.infrastructure.persistence;

import jabaclass.payment.domain.model.DepositPayment;
import jabaclass.payment.domain.repository.DepositPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DepositPaymentRepositoryAdapter implements DepositPaymentRepository {

	private final DepositPaymentJpaRepository depositPaymentJpaRepository;

	@Override
	public DepositPayment save(DepositPayment depositPayment) {
		return depositPaymentJpaRepository.save(depositPayment);
	}

	@Override
	public Optional<DepositPayment> findById(UUID id) {
		return depositPaymentJpaRepository.findById(id);
	}
}
