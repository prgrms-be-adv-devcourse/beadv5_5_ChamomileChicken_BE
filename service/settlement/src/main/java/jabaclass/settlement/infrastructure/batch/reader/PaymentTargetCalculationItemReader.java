package jabaclass.settlement.infrastructure.batch.reader;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

import jabaclass.settlement.application.dto.AppliedPromotion;
import jabaclass.settlement.domain.model.promotion.SellerPromotion;
import jabaclass.settlement.domain.model.promotion.SettlementPromotion;
import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.repository.SellerPromotionRepository;
import jabaclass.settlement.domain.repository.SettlementPromotionRepository;
import jabaclass.settlement.infrastructure.batch.dto.PaymentTargetCalculationItem;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PaymentTargetCalculationItemReader implements ItemStreamReader<PaymentTargetCalculationItem> {

	private final ItemStreamReader<SettlementTarget> delegate;
	private final SellerPromotionRepository sellerPromotionRepository;
	private final SettlementPromotionRepository settlementPromotionRepository;
	private final int chunkSize;

	private final Queue<PaymentTargetCalculationItem> buffer = new ArrayDeque<>();

	@Override
	public PaymentTargetCalculationItem read() throws Exception {
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
				.map(target -> new PaymentTargetCalculationItem(
					target,
					resolveAppliedPromotion(
						sellerPromotionsBySellerId.getOrDefault(target.getSellerId(), List.of()),
						appliedPromotionByPromotionId,
						target.getOccurredAt()
					)
				))
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
			.filter(java.util.Objects::nonNull)
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
