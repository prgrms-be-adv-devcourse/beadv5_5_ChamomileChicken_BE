package jabaclass.payment.infrastructure.persistence;

import jabaclass.payment.domain.model.Payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<Payment, UUID> {
}
