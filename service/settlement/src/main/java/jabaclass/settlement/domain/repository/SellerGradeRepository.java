package jabaclass.settlement.domain.repository;

import java.util.Optional;
import java.util.UUID;

import jabaclass.settlement.domain.model.grade.SellerGrade;

public interface SellerGradeRepository {

	SellerGrade save(SellerGrade sellerGrade);

	Optional<SellerGrade> findBySellerId(UUID sellerId);
}
