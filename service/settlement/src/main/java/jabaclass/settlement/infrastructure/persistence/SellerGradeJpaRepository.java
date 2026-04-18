package jabaclass.settlement.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.settlement.domain.model.SellerGrade;

public interface SellerGradeJpaRepository extends JpaRepository<SellerGrade, UUID> {

	Optional<SellerGrade> findBySellerId(UUID sellerId);
}
