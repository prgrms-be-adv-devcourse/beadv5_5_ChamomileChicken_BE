package jabaclass.payment.domain.repository;

import jabaclass.payment.domain.model.DepositPayment;

import java.util.Optional;
import java.util.UUID;

public interface DepositPaymentRepository {

	DepositPayment save(DepositPayment depositPayment);

	Optional<DepositPayment> findById(UUID id);
}
