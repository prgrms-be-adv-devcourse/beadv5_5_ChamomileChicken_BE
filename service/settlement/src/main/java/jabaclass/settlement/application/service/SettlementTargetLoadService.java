package jabaclass.settlement.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.settlement.application.exception.BusinessException;
import jabaclass.settlement.application.exception.CommonErrorCode;
import jabaclass.settlement.application.exception.SettlementBatchErrorCode;
import jabaclass.settlement.application.dto.OrderSettlementDetail;
import jabaclass.settlement.application.dto.PaymentSettlementSource;
import jabaclass.settlement.application.dto.ProductSettlementDetail;
import jabaclass.settlement.application.dto.RefundSettlementSource;
import jabaclass.settlement.application.dto.SellerSettlementDetail;
import jabaclass.settlement.application.dto.SettlementSliceResult;
import jabaclass.settlement.application.port.outt.OrderSettlementPort;
import jabaclass.settlement.application.port.outt.PaymentSettlementPort;
import jabaclass.settlement.application.port.outt.ProductSettlementPort;
import jabaclass.settlement.application.port.outt.SellerSettlementPort;
import jabaclass.settlement.application.usecase.SettlementTargetLoadUseCase;
import jabaclass.settlement.domain.model.SettlementBatchCursor;
import jabaclass.settlement.domain.model.SettlementTarget;
import jabaclass.settlement.domain.repository.SettlementBatchCursorRepository;
import jabaclass.settlement.domain.repository.SettlementTargetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class SettlementTargetLoadService implements SettlementTargetLoadUseCase {

	private static final String PAYMENT_CURSOR_TYPE = "PAYMENT";
	private static final String REFUND_CURSOR_TYPE = "REFUND";
	private static final int DEFAULT_PAGE_SIZE = 1000;

	private final SettlementTargetRepository settlementTargetRepository;
	private final SettlementBatchCursorRepository settlementBatchCursorRepository;
	private final PaymentSettlementPort paymentSettlementPort;
	private final OrderSettlementPort orderSettlementPort;
	private final ProductSettlementPort productSettlementPort;
	private final SellerSettlementPort sellerSettlementPort;

	@Override
	public void loadDailyTargets(LocalDate targetDate) {
		if (targetDate == null) {
			throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
		}

		LocalDate actualTargetDate = targetDate == null ? LocalDate.now() : targetDate;
		LocalDateTime from = actualTargetDate.minusDays(1).atStartOfDay();
		LocalDateTime to = actualTargetDate.atStartOfDay().minusNanos(1);

		boolean replayMode = actualTargetDate.isBefore(LocalDate.now());

		loadPayments(from, to, replayMode);
		loadRefunds(from, to, replayMode);
	}

	private void loadPayments(LocalDateTime from, LocalDateTime to, boolean replayMode) {
		SettlementBatchCursor cursor = getOrCreateCursor(PAYMENT_CURSOR_TYPE, from, replayMode);

		boolean hasNext;
		do {
			SettlementSliceResult<PaymentSettlementSource> slice = fetchPayments(from, to, cursor);

			List<PaymentSettlementSource> filtered = slice.content().stream()
				.filter(payment -> "PAID".equals(payment.paymentStatus()))
				.filter(payment -> !settlementTargetRepository.existsByPaymentId(payment.paymentId()))
				.toList();

			if (!filtered.isEmpty()) {
				persistPaymentTargets(filtered);
			}

			hasNext = slice.hasNext();
			PaymentSettlementSource lastItem = slice.content().isEmpty()
				? null
				: slice.content().get(slice.content().size() - 1);
			if (lastItem != null) {
				cursor.advance(resolveNextCursorUpdatedAt(slice.nextCursorUpdatedAt(), lastItem.updatedAt()),
					resolveNextCursorId(slice.nextCursorId(), lastItem.paymentId()));
			}

			if (!replayMode) {
				settlementBatchCursorRepository.save(cursor);
			}
		} while (hasNext);
	}

	private void loadRefunds(LocalDateTime from, LocalDateTime to, boolean replayMode) {
		SettlementBatchCursor cursor = getOrCreateCursor(REFUND_CURSOR_TYPE, from, replayMode);

		boolean hasNext;
		do {
			SettlementSliceResult<RefundSettlementSource> slice = fetchRefunds(from, to, cursor);

			List<RefundSettlementSource> filtered = slice.content().stream()
				.filter(refund -> "COMPLETED".equals(refund.refundStatus()))
				.filter(refund -> !settlementTargetRepository.existsByRefundId(refund.refundId()))
				.toList();

			if (!filtered.isEmpty()) {
				persistRefundTargets(filtered);
			}

			hasNext = slice.hasNext();
			RefundSettlementSource lastItem = slice.content().isEmpty()
				? null
				: slice.content().get(slice.content().size() - 1);
			if (lastItem != null) {
				cursor.advance(resolveNextCursorUpdatedAt(slice.nextCursorUpdatedAt(), lastItem.updatedAt()),
					resolveNextCursorId(slice.nextCursorId(), lastItem.refundId()));
			}

			if (!replayMode) {
				settlementBatchCursorRepository.save(cursor);
			}
		} while (hasNext);
	}

	private SettlementBatchCursor getOrCreateCursor(String cursorType, LocalDateTime from, boolean replayMode) {
		if (replayMode) {
			return SettlementBatchCursor.initial(cursorType, from);
		}

		return settlementBatchCursorRepository.findByCursorType(cursorType)
			.orElseGet(() -> settlementBatchCursorRepository.save(SettlementBatchCursor.initial(cursorType, from)));
	}

	private SettlementSliceResult<PaymentSettlementSource> fetchPayments(
		LocalDateTime from,
		LocalDateTime to,
		SettlementBatchCursor cursor
	) {
		try {
			return paymentSettlementPort.fetchPayments(
				from, to, cursor.getLastSyncedAt(), cursor.getLastSyncedId(), DEFAULT_PAGE_SIZE
			);
		} catch (Exception e) {
			throw new BusinessException(SettlementBatchErrorCode.SETTLEMENT_SOURCE_SYNC_FAILED);
		}
	}

	private SettlementSliceResult<RefundSettlementSource> fetchRefunds(
		LocalDateTime from,
		LocalDateTime to,
		SettlementBatchCursor cursor
	) {
		try {
			return paymentSettlementPort.fetchRefunds(
				from, to, cursor.getLastSyncedAt(), cursor.getLastSyncedId(), DEFAULT_PAGE_SIZE
			);
		} catch (Exception e) {
			throw new BusinessException(SettlementBatchErrorCode.SETTLEMENT_SOURCE_SYNC_FAILED);
		}
	}

	private LocalDateTime resolveNextCursorUpdatedAt(LocalDateTime nextCursorUpdatedAt, LocalDateTime fallbackUpdatedAt) {
		return nextCursorUpdatedAt != null ? nextCursorUpdatedAt : fallbackUpdatedAt;
	}

	private UUID resolveNextCursorId(UUID nextCursorId, UUID fallbackId) {
		return nextCursorId != null ? nextCursorId : fallbackId;
	}

	@Transactional
	protected void persistPaymentTargets(List<PaymentSettlementSource> payments) {
		Set<UUID> orderIds = payments.stream()
			.map(PaymentSettlementSource::orderId)
			.collect(Collectors.toSet());

		Set<UUID> productIds = payments.stream()
			.map(PaymentSettlementSource::productId)
			.collect(Collectors.toSet());

		Map<UUID, OrderSettlementDetail> orderMap = orderSettlementPort.fetchOrders(orderIds).stream()
			.collect(Collectors.toMap(OrderSettlementDetail::orderId, Function.identity()));

		Map<UUID, ProductSettlementDetail> productMap = productSettlementPort.fetchProducts(productIds).stream()
			.collect(Collectors.toMap(ProductSettlementDetail::productId, Function.identity()));

		Set<UUID> sellerIds = productMap.values().stream()
			.map(ProductSettlementDetail::sellerId)
			.collect(Collectors.toSet());

		Map<UUID, SellerSettlementDetail> sellerMap = sellerSettlementPort.fetchSellers(sellerIds).stream()
			.collect(Collectors.toMap(SellerSettlementDetail::sellerId, Function.identity()));

		List<SettlementTarget> targets = new ArrayList<>();

		for (PaymentSettlementSource payment : payments) {
			try {
				OrderSettlementDetail order = orderMap.get(payment.orderId());
				ProductSettlementDetail product = productMap.get(payment.productId());

				if (order == null || product == null) {
					log.warn("[SETTLEMENT_LOAD_TARGETS][PAYMENT] order/product 누락 paymentId={}", payment.paymentId());
					continue;
				}

				if (!"PAID".equals(order.orderStatus())) {
					continue;
				}

				if (!"ENABLE".equals(product.productStatus())) {
					continue;
				}

				SellerSettlementDetail seller = sellerMap.get(product.sellerId());
				if (seller == null || !seller.isSeller() || !seller.hasActiveSettlementAccount()) {
					continue;
				}

				String settlementMonth = YearMonth.from(payment.occurredAt()).toString();

				// Settlement source of truth:
				// PAID payment -> positive amount, CANCELLED payment alone is never loaded as a negative source.
				targets.add(SettlementTarget.forPayment(
					settlementMonth,
					seller.sellerId(),
					order.orderId(),
					payment.paymentId(),
					payment.productId(),
					order.productScheduleId(),
					order.buyerId(),
					order.participantUserId(),
					order.quantity(),
					order.unitPrice(),
					order.orderPrice(),
					payment.occurredAt()
				));
			} catch (Exception e) {
				log.error("[SETTLEMENT_LOAD_TARGETS][PAYMENT] paymentId={} 처리 실패", payment.paymentId(), e);
			}
		}

		if (!targets.isEmpty()) {
			settlementTargetRepository.saveAll(targets);
		}
	}

	@Transactional
	protected void persistRefundTargets(List<RefundSettlementSource> refunds) {
		Set<UUID> orderIds = refunds.stream()
			.map(RefundSettlementSource::orderId)
			.collect(Collectors.toSet());

		Set<UUID> productIds = refunds.stream()
			.map(RefundSettlementSource::productId)
			.collect(Collectors.toSet());

		Map<UUID, OrderSettlementDetail> orderMap = orderSettlementPort.fetchOrders(orderIds).stream()
			.collect(Collectors.toMap(OrderSettlementDetail::orderId, Function.identity()));

		Map<UUID, ProductSettlementDetail> productMap = productSettlementPort.fetchProducts(productIds).stream()
			.collect(Collectors.toMap(ProductSettlementDetail::productId, Function.identity()));

		Set<UUID> sellerIds = productMap.values().stream()
			.map(ProductSettlementDetail::sellerId)
			.collect(Collectors.toSet());

		Map<UUID, SellerSettlementDetail> sellerMap = sellerSettlementPort.fetchSellers(sellerIds).stream()
			.collect(Collectors.toMap(SellerSettlementDetail::sellerId, Function.identity()));

		List<SettlementTarget> targets = new ArrayList<>();

		for (RefundSettlementSource refund : refunds) {
			try {
				OrderSettlementDetail order = orderMap.get(refund.orderId());
				ProductSettlementDetail product = productMap.get(refund.productId());

				if (order == null || product == null) {
					log.warn("[SETTLEMENT_LOAD_TARGETS][REFUND] order/product 누락 refundId={}", refund.refundId());
					continue;
				}

				SellerSettlementDetail seller = sellerMap.get(product.sellerId());
				if (seller == null || !seller.isSeller()) {
					continue;
				}

				String settlementMonth = YearMonth.from(refund.occurredAt()).toString();

				// Settlement source of truth:
				// COMPLETED refund -> negative amount. Refund is the only reversal source.
				targets.add(SettlementTarget.forRefund(
					settlementMonth,
					seller.sellerId(),
					order.orderId(),
					refund.paymentId(),
					refund.refundId(),
					refund.productId(),
					order.productScheduleId(),
					order.buyerId(),
					order.participantUserId(),
					order.quantity(),
					order.unitPrice(),
					order.orderPrice(),
					refund.totalRefundAmount(),
					refund.occurredAt()
				));
			} catch (Exception e) {
				log.error("[SETTLEMENT_LOAD_TARGETS][REFUND] refundId={} 처리 실패", refund.refundId(), e);
			}
		}

		if (!targets.isEmpty()) {
			settlementTargetRepository.saveAll(targets);
		}
	}
}
