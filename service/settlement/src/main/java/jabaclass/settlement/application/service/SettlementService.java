package jabaclass.settlement.application.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.settlement.application.exception.BusinessException;
import jabaclass.settlement.application.exception.SettlementErrorCode;
import jabaclass.settlement.application.usecase.SettlementUseCase;
import jabaclass.settlement.domain.model.settlement.Settlement;
import jabaclass.settlement.domain.model.settlement.SettlementStatus;
import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;
import jabaclass.settlement.domain.repository.SettlementRepository;
import jabaclass.settlement.domain.repository.SettlementTargetCalculationRepository;
import jabaclass.settlement.domain.repository.SettlementTargetRepository;
import jabaclass.settlement.presentation.dto.response.SellerSettlementDetailItemResponse;
import jabaclass.settlement.presentation.dto.response.SellerSettlementDetailPageResponse;
import jabaclass.settlement.presentation.dto.response.SellerSettlementPageResponse;
import jabaclass.settlement.presentation.dto.response.SettlementResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class SettlementService implements SettlementUseCase {

	private final SettlementRepository settlementRepository;
	private final SettlementTargetCalculationRepository settlementTargetCalculationRepository;
	private final SettlementTargetRepository settlementTargetRepository;

	@Override
	public List<Settlement> getSettlementsByMonth(String settlementMonth) {
		return settlementRepository.findBySettlementMonth(settlementMonth);
	}

	@Override
	public List<Settlement> getReadySettlementsByMonth(String settlementMonth) {
		return settlementRepository.findBySettlementMonthAndStatus(
			settlementMonth,
			SettlementStatus.READY
		);
	}

	@Override
	public Settlement getSettlement(UUID settlementId) {
		return settlementRepository.findById(settlementId)
			.orElseThrow(() -> new BusinessException(SettlementErrorCode.SETTLEMENT_NOT_FOUND));
	}

	@Override
	public SellerSettlementPageResponse getSellerSettlements(UUID sellerId, int page, int size) {
		Page<Settlement> settlements = settlementRepository.findBySellerId(
			sellerId,
			pageable(page, size, Sort.by(Sort.Order.desc("settlementMonth"), Sort.Order.desc("createdAt")))
		);

		return new SellerSettlementPageResponse(
			settlements.getContent().stream()
				.map(SettlementResponse::from)
				.toList(),
			settlements.getNumber(),
			settlements.getSize(),
			settlements.getTotalElements(),
			settlements.getTotalPages(),
			settlements.hasNext()
		);
	}

	@Override
	public SellerSettlementDetailPageResponse getSellerSettlementDetails(UUID sellerId, UUID settlementId, int page, int size) {
		Settlement settlement = settlementRepository.findById(settlementId)
			.filter(it -> it.getSellerId().equals(sellerId))
			.orElseThrow(() -> new BusinessException(SettlementErrorCode.SETTLEMENT_NOT_FOUND));

		Page<SettlementTargetCalculation> calculations = settlementTargetCalculationRepository.findBySettlementMonthAndSellerId(
			settlement.getSettlementMonth(),
			sellerId,
			pageable(page, size, Sort.by(Sort.Order.desc("calculatedAt"), Sort.Order.desc("id")))
		);

		List<UUID> targetIds = calculations.getContent().stream()
			.map(SettlementTargetCalculation::getSettlementTargetId)
			.toList();
		Map<UUID, SettlementTarget> targetMap = settlementTargetRepository.findAllByIds(targetIds).stream()
			.collect(Collectors.toMap(SettlementTarget::getId, Function.identity()));

		return new SellerSettlementDetailPageResponse(
			SettlementResponse.from(settlement),
			calculations.getContent().stream()
				.map(calculation -> SellerSettlementDetailItemResponse.from(
					calculation,
					targetMap.get(calculation.getSettlementTargetId())
				))
				.toList(),
			calculations.getNumber(),
			calculations.getSize(),
			calculations.getTotalElements(),
			calculations.getTotalPages(),
			calculations.hasNext()
		);
	}

	private Pageable pageable(int page, int size, Sort sort) {
		int normalizedPage = Math.max(page, 0);
		int normalizedSize = Math.min(Math.max(size, 1), 100);
		return PageRequest.of(normalizedPage, normalizedSize, sort);
	}
}
