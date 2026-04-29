package jabaclass.settlement.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import jabaclass.settlement.domain.model.grade.SellerGrade;

public interface SellerGradeJpaRepository extends JpaRepository<SellerGrade, UUID> {

	List<SellerGrade> findBySellerIdInAndCalculatedMonth(List<UUID> sellerIds, String calculatedMonth);
}
