package jabaclass.settlement.infrastructure.kafka;

import java.time.format.DateTimeFormatter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import jabaclass.settlement.domain.model.settlement.SettlementTarget;
import jabaclass.settlement.infrastructure.kafka.dto.SettlementPaymentCompletedEvent;
import jabaclass.settlement.infrastructure.kafka.dto.SettlementRefundCompletedEvent;
import jabaclass.settlement.infrastructure.persistence.SettlementTargetJpaRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SettlementTargetEventHandler {

	private static final DateTimeFormatter SETTLEMENT_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

	private final SettlementTargetJpaRepository settlementTargetJpaRepository;

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
			settlementTargetJpaRepository.saveAndFlush(target);
		} catch (DataIntegrityViolationException e) {
			if (!isDuplicateSourceEvent(e)) {
				throw e;
			}
		}
	}

	private boolean isDuplicateSourceEvent(DataIntegrityViolationException e) {
		Throwable current = e;
		while (current != null) {
			String message = current.getMessage();
			if (message != null && message.toLowerCase().contains("source_event_id")) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}
}
