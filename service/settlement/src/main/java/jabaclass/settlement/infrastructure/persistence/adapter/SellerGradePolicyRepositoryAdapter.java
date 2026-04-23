package jabaclass.settlement.infrastructure.persistence.adapter;

import java.util.List;

import org.springframework.stereotype.Repository;

import jabaclass.settlement.domain.model.grade.SellerGradePolicy;
import jabaclass.settlement.domain.repository.SellerGradePolicyRepository;
import jabaclass.settlement.infrastructure.persistence.SellerGradePolicyJpaRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SellerGradePolicyRepositoryAdapter implements SellerGradePolicyRepository {

	private final SellerGradePolicyJpaRepository sellerGradePolicyJpaRepository;

	@Override
	public List<SellerGradePolicy> findActivePolicies() {
		return sellerGradePolicyJpaRepository.findByActiveTrueOrderByMinSalesAmountDesc();
	}
}
