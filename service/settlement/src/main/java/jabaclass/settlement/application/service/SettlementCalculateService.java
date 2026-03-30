package jabaclass.settlement.application.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.settlement.application.dto.SettlementTargetSummary;
import jabaclass.settlement.application.exception.BusinessException;
import jabaclass.settlement.application.exception.CommonErrorCode;
import jabaclass.settlement.application.usecase.SettlementCalculateUseCase;
import jabaclass.settlement.domain.model.Settlement;
import jabaclass.settlement.domain.model.SettlementFeePolicy;
import jabaclass.settlement.domain.model.SettlementHistory;
import jabaclass.settlement.domain.model.SettlementTarget;
import jabaclass.settlement.domain.repository.SettlementHistoryRepository;
import jabaclass.settlement.domain.repository.SettlementRepository;
import jabaclass.settlement.domain.repository.SettlementTargetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SettlementCalculateService implements SettlementCalculateUseCase {

	private final SettlementTargetRepository settlementTargetRepository;
	private final SettlementRepository settlementRepository;
	private final SettlementHistoryRepository settlementHistoryRepository;

	@Override
	@Transactional
	public int calculateMonthly(String settlementMonth) {
		if (settlementMonth == null || settlementMonth.isBlank()) {
			throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
		}

		List<SettlementTargetSummary> summaries =
			settlementTargetRepository.findSummaryBySettlementMonth(settlementMonth);
		Map<UUID, List<SettlementTarget>> targetsBySellerId = groupTargetsBySellerId(settlementMonth);

		List<Settlement> createdSettlements = new ArrayList<>();
		List<SettlementHistory> createdHistories = new ArrayList<>();

		for (SettlementTargetSummary summary : summaries) {
			if (settlementRepository.existsBySellerIdAndSettlementMonth(summary.sellerId(), settlementMonth)) {
				continue;
			}

			SettlementFeePolicy.SettlementAmount amount =
				SettlementFeePolicy.calculate(summary.totalSettlementAmount());

			Settlement settlement = Settlement.createReady(
				summary.sellerId(),
				settlementMonth,
				amount.originalAmount(),
				amount.feeAmount(),
				amount.feeRate(),
				amount.settlementAmount()
			);

			if (!settlement.isTransferable()) {
				settlement.hold("정산 금액이 0 이하이므로 송금 보류");
			}

			createdSettlements.add(settlement);

			List<SettlementTarget> sellerTargets = targetsBySellerId.getOrDefault(summary.sellerId(), List.of());

			createdHistories.addAll(createHistories(settlement, sellerTargets));
		}

		if (!createdSettlements.isEmpty()) {
			settlementRepository.saveAll(createdSettlements);
		}

		if (!createdHistories.isEmpty()) {
			settlementHistoryRepository.saveAll(createdHistories);
		}

		return createdSettlements.size();
	}

	private Map<UUID, List<SettlementTarget>> groupTargetsBySellerId(String settlementMonth) {
		Map<UUID, List<SettlementTarget>> targetsBySellerId = new HashMap<>();

		for (SettlementTarget target : settlementTargetRepository.findBySettlementMonth(settlementMonth)) {
			targetsBySellerId
				.computeIfAbsent(target.getSellerId(), ignored -> new ArrayList<>())
				.add(target);
		}

		return targetsBySellerId;
	}

	private List<SettlementHistory> createHistories(Settlement settlement, List<SettlementTarget> targets) {
		if (targets.isEmpty()) {
			return List.of();
		}

		BigDecimal totalOriginalAmount = targets.stream()
			.map(SettlementTarget::getSettlementAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal totalFeeAmount = settlement.getFeeAmount();
		List<SettlementHistory> histories = new ArrayList<>();

		BigDecimal accumulatedFee = BigDecimal.ZERO;

		for (int i = 0; i < targets.size(); i++) {
			SettlementTarget target = targets.get(i);

			BigDecimal historyFeeAmount;
			if (i == targets.size() - 1) {
				historyFeeAmount = totalFeeAmount.subtract(accumulatedFee);
			} else {
				historyFeeAmount = allocateFee(
					target.getSettlementAmount(),
					totalOriginalAmount,
					totalFeeAmount
				);
				accumulatedFee = accumulatedFee.add(historyFeeAmount);
			}

			BigDecimal historySettlementAmount = target.getSettlementAmount().subtract(historyFeeAmount);

			histories.add(SettlementHistory.create(
				settlement.getId(),
				target.getId(),
				settlement.getSellerId(),
				target.getProductId(),
				settlement.getSettlementMonth(),
				target.getSettlementAmount(),
				historyFeeAmount,
				historySettlementAmount,
				settlement.getStatus()
			));
		}

		return histories;
	}

	private BigDecimal allocateFee(
		BigDecimal targetAmount,
		BigDecimal totalAmount,
		BigDecimal totalFeeAmount
	) {
		if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}

		return targetAmount
			.multiply(totalFeeAmount)
			.divide(totalAmount, 2, java.math.RoundingMode.DOWN);
	}
}
