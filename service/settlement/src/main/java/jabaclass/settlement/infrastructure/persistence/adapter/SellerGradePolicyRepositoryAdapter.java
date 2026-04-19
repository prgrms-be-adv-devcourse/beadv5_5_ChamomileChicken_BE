package jabaclass.settlement.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import jabaclass.settlement.domain.model.SellerGradePolicy;
import jabaclass.settlement.domain.repository.SellerGradePolicyRepository;
import jabaclass.settlement.infrastructure.persistence.SellerGradePolicyJpaRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SellerGradePolicyRepositoryAdapter implements SellerGradePolicyRepository {

	private final SellerGradePolicyJpaRepository sellerGradePolicyJpaRepository;

	@Override
	public Optional<SellerGradePolicy> findActiveApplicablePolicy(BigDecimal salesAmount) {
		return sellerGradePolicyJpaRepository.findActiveApplicablePolicies(salesAmount).stream().findFirst();
	}
}
