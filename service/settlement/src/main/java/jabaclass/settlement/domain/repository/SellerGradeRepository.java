package jabaclass.settlement.domain.repository;

import java.util.List;
import java.util.UUID;

import jabaclass.settlement.domain.model.grade.SellerGrade;

public interface SellerGradeRepository {

	List<SellerGrade> saveAll(List<SellerGrade> sellerGrades);

	List<SellerGrade> findBySellerIdsAndCalculatedMonth(List<UUID> sellerIds, String calculatedMonth);
}
