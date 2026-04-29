package jabaclass.settlement.infrastructure.batch.reader;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

import jabaclass.settlement.application.dto.AppliedPromotion;
import jabaclass.settlement.domain.model.promotion.SellerPromotion;
import jabaclass.settlement.domain.model.promotion.SettlementPromotion;
import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.model.settlement.SettlementTargetCalculation;
import jabaclass.settlement.domain.model.settlement.SettlementTargetType;
import jabaclass.settlement.domain.repository.SellerPromotionRepository;
import jabaclass.settlement.domain.repository.SettlementPromotionRepository;
import jabaclass.settlement.domain.repository.SettlementTargetCalculationRepository;
import jabaclass.settlement.domain.repository.SettlementTargetRepository;
import jabaclass.settlement.infrastructure.batch.dto.RefundTargetCalculationItem;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RefundTargetCalculationItemReader implements ItemStreamReader<RefundTargetCalculationItem> {

	private final ItemStreamReader<SettlementTarget> delegate;
	private final SettlementTargetRepository settlementTargetRepository;
	private final SettlementTargetCalculationRepository settlementTargetCalculationRepository;
	private final SellerPromotionRepository sellerPromotionRepository;
	private final SettlementPromotionRepository settlementPromotionRepository;
	private final int chunkSize;

	private final Queue<RefundTargetCalculationItem> buffer = new ArrayDeque<>();

	@Override
	public RefundTargetCalculationItem read() throws Exception {
		if (!buffer.isEmpty()) {
			return buffer.poll();
		}

		List<SettlementTarget> targets = new ArrayList<>(chunkSize);
		while (targets.size() < chunkSize) {
			SettlementTarget target = delegate.read();
			if (target == null) {
				break;
			}
			targets.add(target);
		}

		if (targets.isEmpty()) {
			return null;
		}

		List<UUID> paymentIds = targets.stream()
			.map(SettlementTarget::getPaymentId)
			.filter(Objects::nonNull)
			.distinct()
			.toList();
		List<UUID> sellerIds = targets.stream()
			.map(SettlementTarget::getSellerId)
			.distinct()
			.toList();
		LocalDateTime minOccurredAt = targets.stream()
			.map(SettlementTarget::getOccurredAt)
			.min(LocalDateTime::compareTo)
			.orElseThrow();
		LocalDateTime maxOccurredAt = targets.stream()
			.map(SettlementTarget::getOccurredAt)
			.max(LocalDateTime::compareTo)
			.orElseThrow();

		Map<UUID, SettlementTarget> originalPaymentTargetByPaymentId = settlementTargetRepository.findByPaymentIdsAndTargetType(
				paymentIds,
				SettlementTargetType.PAYMENT
			).stream()
			.collect(Collectors.toMap(SettlementTarget::getPaymentId, Function.identity(), (existing, replacement) -> existing));

		List<UUID> originalPaymentTargetIds = originalPaymentTargetByPaymentId.values().stream()
			.map(SettlementTarget::getId)
			.toList();
		Map<UUID, SettlementTargetCalculation> originalPaymentCalculationByTargetId =
			settlementTargetCalculationRepository.findBySettlementTargetIds(originalPaymentTargetIds)
				.stream()
				.collect(Collectors.toMap(
					SettlementTargetCalculation::getSettlementTargetId,
					Function.identity(),
					(existing, replacement) -> existing
				));

		Map<UUID, List<SellerPromotion>> sellerPromotionsBySellerId = sellerPromotionRepository.findActiveApplicablePromotions(
				sellerIds,
				minOccurredAt,
				maxOccurredAt
			).stream()
			.collect(Collectors.groupingBy(
				SellerPromotion::getSellerId,
				Collectors.collectingAndThen(
					Collectors.toList(),
					promotions -> promotions.stream()
						.sorted(Comparator.comparing(SellerPromotion::getStartedAt).reversed())
						.toList()
				)
			));
		Map<UUID, AppliedPromotion> appliedPromotionByPromotionId = loadAppliedPromotions(sellerPromotionsBySellerId);

		buffer.addAll(
			targets.stream()
				.map(target -> {
					SettlementTarget originalPaymentTarget = originalPaymentTargetByPaymentId.get(target.getPaymentId());
					SettlementTargetCalculation originalPaymentCalculation = originalPaymentTarget == null
						? null
						: originalPaymentCalculationByTargetId.get(originalPaymentTarget.getId());

					return new RefundTargetCalculationItem(
						target,
						originalPaymentTarget,
						originalPaymentCalculation,
						resolveAppliedPromotion(
							sellerPromotionsBySellerId.getOrDefault(target.getSellerId(), List.of()),
							appliedPromotionByPromotionId,
							target.getOccurredAt()
						)
					);
				})
				.toList()
		);

		return buffer.poll();
	}

	private Map<UUID, AppliedPromotion> loadAppliedPromotions(Map<UUID, List<SellerPromotion>> sellerPromotionsBySellerId) {
		Map<UUID, AppliedPromotion> appliedPromotionByPromotionId = new HashMap<>();
		sellerPromotionsBySellerId.values().stream()
			.flatMap(List::stream)
			.map(SellerPromotion::getPromotionId)
			.distinct()
			.forEach(promotionId -> settlementPromotionRepository.findById(promotionId)
				.filter(SettlementPromotion::isActive)
				.ifPresent(promotion -> appliedPromotionByPromotionId.put(
					promotionId,
					new AppliedPromotion(
						promotion.getId(),
						promotion.getPromotionType().name(),
						promotion.getFeeRate()
					)
				)));
		return appliedPromotionByPromotionId;
	}

	private AppliedPromotion resolveAppliedPromotion(
		List<SellerPromotion> sellerPromotions,
		Map<UUID, AppliedPromotion> appliedPromotionByPromotionId,
		LocalDateTime occurredAt
	) {
		return sellerPromotions.stream()
			.filter(promotion -> !promotion.getStartedAt().isAfter(occurredAt))
			.filter(promotion -> promotion.getEndedAt() == null || !promotion.getEndedAt().isBefore(occurredAt))
			.map(promotion -> appliedPromotionByPromotionId.get(promotion.getPromotionId()))
			.filter(Objects::nonNull)
			.findFirst()
			.orElseGet(AppliedPromotion::empty);
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		delegate.open(executionContext);
	}

	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		delegate.update(executionContext);
	}

	@Override
	public void close() throws ItemStreamException {
		delegate.close();
	}
}
