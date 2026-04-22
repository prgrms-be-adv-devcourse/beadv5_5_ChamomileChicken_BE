package jabaclass.settlement.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jabaclass.settlement.domain.model.grade.SellerGrade;

public interface SellerGradeRepository {

	SellerGrade save(SellerGrade sellerGrade);

	List<SellerGrade> saveAll(List<SellerGrade> sellerGrades);

	Optional<SellerGrade> findBySellerId(UUID sellerId);

	List<SellerGrade> findBySellerIds(List<UUID> sellerIds);
}
