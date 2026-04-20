package jabaclass.settlement.infrastructure.persistence.adapter;

import java.util.Optional;
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
	public SellerGrade save(SellerGrade sellerGrade) {
		return sellerGradeJpaRepository.save(sellerGrade);
	}

	@Override
	public Optional<SellerGrade> findBySellerId(UUID sellerId) {
		return sellerGradeJpaRepository.findBySellerId(sellerId);
	}
}
