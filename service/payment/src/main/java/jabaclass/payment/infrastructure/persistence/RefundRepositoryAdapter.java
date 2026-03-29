package jabaclass.payment.infrastructure.persistence;

import org.springframework.stereotype.Repository;

import jabaclass.payment.domain.model.Refund;
import jabaclass.payment.domain.repository.RefundRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RefundRepositoryAdapter implements RefundRepository {

	private final RefundJpaRepository refundJpaRepository;

	@Override
	public Refund save(Refund refund) {
		return refundJpaRepository.save(refund);
	}
}
