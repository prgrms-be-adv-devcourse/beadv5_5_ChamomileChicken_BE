package jabaclass.settlement.infrastructure.kafka;

import java.time.format.DateTimeFormatter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.domain.repository.SettlementTargetRepository;
import jabaclass.settlement.infrastructure.kafka.dto.SettlementPaymentCompletedEvent;
import jabaclass.settlement.infrastructure.kafka.dto.SettlementRefundCompletedEvent;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SettlementTargetEventHandler {

	private static final DateTimeFormatter SETTLEMENT_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

	private final SettlementTargetRepository settlementTargetRepository;

	@Transactional
	public void handlePaymentCompleted(SettlementPaymentCompletedEvent event) {
		saveIgnoringDuplicateSourceEvent(SettlementTarget.forPayment(
			event.eventId(),
			toSettlementMonth(event.occurredAt()),
			event.sellerId(),
			event.orderId(),
			event.paymentId(),
			event.productId(),
			event.settlementBaseAmount(),
			event.occurredAt()
		));
	}

	@Transactional
	public void handleRefundCompleted(SettlementRefundCompletedEvent event) {
		saveIgnoringDuplicateSourceEvent(SettlementTarget.forRefund(
			event.eventId(),
			toSettlementMonth(event.occurredAt()),
			event.sellerId(),
			event.orderId(),
			event.paymentId(),
			event.refundId(),
			event.productId(),
			event.settlementBaseAmount(),
			event.occurredAt()
		));
	}

	private String toSettlementMonth(java.time.LocalDateTime occurredAt) {
		return occurredAt.format(SETTLEMENT_MONTH_FORMATTER);
	}

	private void saveIgnoringDuplicateSourceEvent(SettlementTarget target) {
		try {
			settlementTargetRepository.save(target);
		} catch (DataIntegrityViolationException e) {
			// source_event_id unique 제약에 걸린 경우 이미 적재된 동일 이벤트로 간주한다.
		}
	}
}
