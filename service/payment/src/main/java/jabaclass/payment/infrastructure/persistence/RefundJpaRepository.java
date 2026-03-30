package jabaclass.payment.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.payment.domain.model.Refund;

public interface RefundJpaRepository extends JpaRepository<Refund, UUID> {
}
