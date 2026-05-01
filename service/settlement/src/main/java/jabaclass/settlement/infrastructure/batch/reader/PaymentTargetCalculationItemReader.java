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
import jabaclass.settlement.application.dto.SettlementTargetInfo;
import jabaclass.settlement.domain.model.promotion.SellerPromotion;
import jabaclass.settlement.domain.model.promotion.SettlementPromotion;
import jabaclass.settlement.domain.repository.SellerPromotionRepository;
import jabaclass.settlement.domain.repository.SettlementPromotionRepository;
import jabaclass.settlement.infrastructure.batch.dto.PaymentTargetCalculationItem;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PaymentTargetCalculationItemReader implements ItemStreamReader<PaymentTargetCalculationItem> {

	private final ItemStreamReader<SettlementTargetInfo> delegate;
	private final SellerPromotionRepository sellerPromotionRepository;
	private final SettlementPromotionRepository settlementPromotionRepository;
	private final int chunkSize;

	private final Queue<PaymentTargetCalculationItem> buffer = new ArrayDeque<>();
	private Map<UUID, AppliedPromotion> appliedPromotionByPromotionId = Map.of();

	@Override
	public PaymentTargetCalculationItem read() throws Exception {
		if (!buffer.isEmpty()) {
			return buffer.poll();
		}

		List<SettlementTargetInfo> targets = new ArrayList<>(chunkSize);
		while (targets.size() < chunkSize) {
			SettlementTargetInfo target = delegate.read();
			if (target == null) {
				break;
			}
			targets.add(target);
		}

		if (targets.isEmpty()) {
			return null;
		}

		List<UUID> sellerIds = targets.stream()
			.map(SettlementTargetInfo::sellerId)
			.distinct()
			.toList();
		LocalDateTime minOccurredAt = targets.stream()
			.map(SettlementTargetInfo::occurredAt)
			.min(LocalDateTime::compareTo)
			.orElseThrow();
		LocalDateTime maxOccurredAt = targets.stream()
			.map(SettlementTargetInfo::occurredAt)
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
		buffer.addAll(
			targets.stream()
				.map(target -> new PaymentTargetCalculationItem(
					target,
					resolveAppliedPromotion(
						sellerPromotionsBySellerId.getOrDefault(target.sellerId(), List.of()),
						appliedPromotionByPromotionId,
						target.occurredAt()
					)
				))
				.toList()
		);

		return buffer.poll();
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
		this.appliedPromotionByPromotionId = settlementPromotionRepository.findAllActive().stream()
			.collect(Collectors.toMap(
				SettlementPromotion::getId,
				promotion -> new AppliedPromotion(
					promotion.getId(),
					promotion.getPromotionType().name(),
					promotion.getFeeRate()
				),
				(existing, replacement) -> existing,
				HashMap::new
			));
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
