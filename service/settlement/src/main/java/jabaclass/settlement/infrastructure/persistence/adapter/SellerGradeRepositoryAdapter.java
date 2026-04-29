package jabaclass.settlement.infrastructure.persistence.adapter;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import jabaclass.settlement.domain.model.grade.SellerGrade;
import jabaclass.settlement.domain.repository.SellerGradeRepository;
import jabaclass.settlement.infrastructure.persistence.SellerGradeJpaRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SellerGradeRepositoryAdapter implements SellerGradeRepository {

	private final SellerGradeJpaRepository sellerGradeJpaRepository;

	@Override
	public List<SellerGrade> saveAll(List<SellerGrade> sellerGrades) {
		return sellerGradeJpaRepository.saveAll(sellerGrades);
	}

	@Override
	public List<SellerGrade> findBySellerIdsAndCalculatedMonth(List<UUID> sellerIds, String calculatedMonth) {
		if (sellerIds == null || sellerIds.isEmpty()) {
			return List.of();
		}

		return sellerGradeJpaRepository.findBySellerIdInAndCalculatedMonth(sellerIds, calculatedMonth);
	}
}
