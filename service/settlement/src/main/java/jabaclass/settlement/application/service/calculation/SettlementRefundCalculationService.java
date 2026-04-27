package jabaclass.settlement.application.service.calculation;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.settlement.application.dto.AppliedPromotion;
import jabaclass.settlement.application.exception.BusinessException;
import jabaclass.settlement.application.exception.SettlementCalculationRetryableException;
import jabaclass.settlement.application.exception.SettlementErrorCode;
import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;
import jabaclass.settlement.domain.model.settlement.SettlementTargetType;
import jabaclass.settlement.domain.repository.SettlementTargetCalculationRepository;
import jabaclass.settlement.domain.repository.SettlementTargetRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementRefundCalculationService {

	private final SettlementTargetRepository settlementTargetRepository;
	private final SettlementTargetCalculationRepository settlementTargetCalculationRepository;
	private final SettlementPromotionResolver settlementPromotionResolver;

	public SettlementTargetCalculation calculate(SettlementTarget target) {
		return settlementTargetRepository.findByPaymentIdAndTargetType(
			target.getPaymentId(),
			SettlementTargetType.PAYMENT
		).map(originalPaymentTarget -> {
			SettlementTargetCalculation originalPaymentCalculation =
				settlementTargetCalculationRepository.findBySettlementTargetId(originalPaymentTarget.getId())
					.orElseThrow(() -> new SettlementCalculationRetryableException(
						"원 결제 정산 계산 결과가 아직 생성되지 않았습니다."
					));

			return SettlementTargetCalculation.forRefund(
				target,
				originalPaymentTarget,
				originalPaymentCalculation
			);
		}).orElseGet(() -> calculateWithPromotionFallback(target));
	}

	private SettlementTargetCalculation calculateWithPromotionFallback(SettlementTarget target) {
		AppliedPromotion appliedPromotion = settlementPromotionResolver.resolve(
			target.getSellerId(),
			target.getOccurredAt()
		);

		if (appliedPromotion.exists()) {
			return SettlementTargetCalculation.forRefundWithPromotion(
				target,
				appliedPromotion.promotionId(),
				appliedPromotion.promotionType(),
				appliedPromotion.feeRate()
			);
		}

		throw new BusinessException(SettlementErrorCode.SETTLEMENT_NOT_FOUND);
	}
}
